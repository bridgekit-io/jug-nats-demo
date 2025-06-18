package io.bridgekit.nats.eventstream;

import java.time.Duration;

import static io.bridgekit.nats.Utils.randomAlphanumeric;
import static io.bridgekit.nats.Utils.randomBoolean;
import static io.bridgekit.nats.Utils.sleepSeconds;
import static java.nio.charset.StandardCharsets.UTF_8;
import io.bridgekit.nats.EnterListener;
import io.bridgekit.nats.Logger;
import io.nats.client.Connection;
import io.nats.client.JetStream;
import io.nats.client.JetStreamApiException;
import io.nats.client.Nats;
import io.nats.client.api.StorageType;
import io.nats.client.api.StreamConfiguration;

/**
 * This demo shows how to use NATS to perform "event streaming"; a use-case where Java
 * devs frequently reach for Kafka. It interacts with a single event stream/log
 * called ORDER_EVENTS and randomly publishes "order.placed" and "order.cancelled" events
 * with random order ids. The goal is to show how you can put all order-related events
 * in a single stream and let them be intelligently consumed by StreamConsumer instances.
 *
 * <pre>
 * # Usage
 * make demo-stream-publisher
 * </pre>
 */
public class StreamPublisher {
    private static final Logger logger = Logger.instance(StreamPublisher.class);

    public static void main(String[] args) throws Exception {
        logger.info("Connecting to NATS server and event stream/log.");
        var nats = Nats.connect("nats://localhost:4222");
        var jetStream = nats.jetStream();
        createOrUpdateStream(nats);

        logger.info("Press ENTER to quit.");
        var enter = new EnterListener();

        while (enter.notPressed()) {
            publishMessage(jetStream);
            sleepSeconds(2);
        }

        logger.info("Bye, bye!");
        nats.close();
    }

    private static void publishMessage(JetStream jetStream) throws Exception {
        // Randomly choose one of the two events we emit and make up an id/payload for it.
        var eventName = randomBoolean() ? "order.placed" : "order.cancelled";
        var orderID = randomAlphanumeric(5);

        logger.info("Publishing event: %s -> %s", eventName, orderID);
        jetStream.publish(eventName, orderID.getBytes(UTF_8));
    }

    public static void createOrUpdateStream(Connection nats) throws Exception {
        var streamName = "ORDER_EVENTS";
        var eventSubject = "order.>";

        var streamConfig = StreamConfiguration.builder()
            .name(streamName)
            .subjects(eventSubject)
            .storageType(StorageType.File)
            .maxMessages(100)
            .build();

        // Admittedly, this kind of sucks. The Go client has a convenient "createOrUpdateStream()" method
        // that does this logic for you. Maybe they'll add it to the Java client, eventually.
        try {
            nats.jetStreamManagement().updateStream(streamConfig);
            logger.info("Updated existing %s stream.", streamName);
        }
        catch (JetStreamApiException e) {
            if (e.getErrorCode() != 404) {
                throw e;
            }
            nats.jetStreamManagement().addStream(streamConfig);
            logger.info("Created a new %s stream.", streamName);
        }
    }
}
