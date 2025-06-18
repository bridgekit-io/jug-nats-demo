package io.bridgekit.nats.sampleapp;

import java.io.Closeable;
import java.io.IOException;
import java.util.function.Consumer;

import static io.bridgekit.nats.Utils.marshalJSON;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.time.Duration.ofDays;
import io.bridgekit.nats.Logger;
import io.bridgekit.nats.Utils;
import io.nats.client.Connection;
import io.nats.client.JetStream;
import io.nats.client.JetStreamApiException;
import io.nats.client.JetStreamManagement;
import io.nats.client.Message;
import io.nats.client.MessageHandler;
import io.nats.client.Nats;
import io.nats.client.api.ConsumerConfiguration;
import io.nats.client.api.DeliverPolicy;
import io.nats.client.api.StorageType;
import io.nats.client.api.StreamConfiguration;

/**
 * EventBroker manages the NATS connection used to facilitate our event-streaming functionality. It
 * lets services publish events to NATS as well as registering routes/consumers to trigger work
 * when certain events are fired.
 */
public class EventGateway implements Closeable {
    private final Logger logger;
    private final Connection nats;
    private final JetStream jetStream;
    private final JetStreamManagement jetStreamManagement;

    public EventGateway(String host, int port) {
        try {
            this.logger = Logger.instance(getClass());
            this.nats = Nats.connect(String.format("nats://%s:%d", host, port));
            this.jetStreamManagement = nats.jetStreamManagement();
            this.jetStream = nats.jetStream();
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * The URL used to connect to NATS.
     */
    public String url() {
        return nats.getConnectedUrl();
    }

    /**
     * Okay... this doesn't really "start" anything. I just wanted parity with the ApiGateway.
     */
    public void start() {
        logger.info("Now running: %s", url());
    }

    /**
     * Returns an instance of Publisher than can be used to post service events to NATS.
     */
    public Publisher publisher() {
        return (eventName, payload) -> {
            try {
                logger.info("Publishing event: %s", eventName);
                jetStream.publish(eventName, marshalJSON(payload).getBytes(UTF_8));
            }
            catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
    }

    /**
     * Sets up the NATS event stream and provides routing for matching events.
     *
     * @param streamName     The name of the top-level stream/log where NATS will write the events.
     * @param subjectPattern NATS will only write events to the stream that match this pattern.
     * @return A new stream/router you can use to set up event-based service routes.
     */
    public EventStream stream(String streamName, String subjectPattern) {
        try {
            logger.info("Setting up event consumer: %s -> %s", streamName, subjectPattern);
            return new EventStream(streamName, subjectPattern);
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void close() throws IOException {
        Utils.closeQuietly(nats);
    }

    /**
     * Provides the ability to set up routes/consumers for events written to a NATS stream.
     */
    public class EventStream {
        private final String streamName;

        private EventStream(String streamName, String subjectPattern) throws Exception {
            this.streamName = streamName;

            // Yep, copy/pasted from the StreamPublisher demo.
            var streamConfig = StreamConfiguration.builder()
                .name(streamName)
                .subjects(subjectPattern)
                .storageType(StorageType.File)
                .maxMessages(10)
                .build();

            // Admittedly, this kind of sucks. The Go client has a convenient "createOrUpdateStream()" method
            // that does this logic for you. Maybe they'll add it to the Java client, eventually.
            try {
                jetStreamManagement.updateStream(streamConfig);
            }
            catch (JetStreamApiException e) {
                if (e.getErrorCode() != 404) {
                    throw e;
                }
                jetStreamManagement.addStream(streamConfig);
            }
        }

        /**
         * Registers an event-based route/consumer. When NATS receives a matching event on in this stream, the
         * event gateway will invoke your handler.
         *
         * @param eventName     The event to listen for. Can include wildcards (e.g. "user.>" or "*.created.>")
         * @param consumerGroup If running multiple instances only 1 in this group will get the event.
         * @param handler       The unit of work to execute when the gateway receives a matching event.
         * @return this
         */
        public EventStream on(String eventName, String consumerGroup, Consumer<Message> handler) {
            try {
                jetStreamManagement.addOrUpdateConsumer(streamName, ConsumerConfiguration.builder()
                    .durable(consumerGroup)
                    .deliverPolicy(DeliverPolicy.New)
                    .inactiveThreshold(ofDays(14))
                    .filterSubject(eventName)
                    .build());

                MessageHandler messageHandler = msg -> {
                    try {
                        logger.info("Handling event: %s/%s", eventName, consumerGroup);
                        handler.accept(msg);
                    }
                    catch (Exception e) {
                        logger.error(e, "Error handling event: %s: %s", eventName, e.getMessage());
                    }
                    finally {
                        // In a *real* distributed system, you'd probably want to have some sort of retry logic.
                        msg.ack();
                    }
                };

                // Normally, you'd capture the consumer and close() it, but the only time we close it is when
                // we shut down the program. Closing the NATS connection on shutdown will take care of this anyway.
                jetStream.getConsumerContext(streamName, consumerGroup).consume(messageHandler);
                return this;
            }
            catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * The minimal interface required to let services post events to NATS.
     */
    public interface Publisher {
        void publish(String eventName, Object payload);
    }
}
