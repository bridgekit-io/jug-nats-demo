package io.bridgekit.nats.eventstream;

import static io.bridgekit.nats.Utils.asString;
import static io.bridgekit.nats.Utils.firstArg;
import io.bridgekit.nats.EnterListener;
import io.bridgekit.nats.Logger;
import io.nats.client.Connection;
import io.nats.client.Message;
import io.nats.client.MessageConsumer;
import io.nats.client.MessageHandler;
import io.nats.client.Nats;
import io.nats.client.api.ConsumerConfiguration;

/**
 * This demo shows how to use NATS to perform "event streaming"; a use-case where Java
 * devs frequently reach for Kafka. It interacts with a single event stream/log
 * called ORDER_EVENTS and randomly publishes "order.placed" and "order.cancelled" events
 * with random order ids. The goal is to show how you can put all order-related events
 * in a single stream and let them be intelligently consumed by StreamConsumer instances.
 *
 * <pre>
 * # Usage
 * make demo-stream-consumer-confirm
 * make demo-stream-consumer-fulfill
 * make demo-stream-consumer-trash
 * </pre>
 *
 * Try opening multiple terminals and running these commands multiple times and concurrently.
 * This will show you how consumers load balance messages and filter out just events they care about.
 */
public class StreamConsumer {
    private static final Logger logger = Logger.instance(StreamConsumer.class);

    public static void main(String[] args) throws Exception {
        logger.info("Connecting to NATS server and the '%s' consumer group.", firstArg(args));
        var nats = Nats.connect("nats://localhost:4222");
        var consumer = consume(nats, firstArg(args));

        // Consumers are asynchronous, so just block main thread until ready to shut down.
        logger.info("Press ENTER to quit.");
        new EnterListener().awaitPressed();

        logger.info("Bye, bye!");
        consumer.close();
        nats.close();
    }

    private static void fulfillOrder(Message event) {
        logger.info("Pick/pack/ship order: %s", asString(event.getData()));
        event.ack();
    }

    private static void confirmOrder(Message event) {
        logger.info("Sending order confirmation email: %s", asString(event.getData()));
        event.ack();
    }

    private static void cancelOrder(Message event) {
        logger.info("Throwing order in the trash: %s", asString(event.getData()));
        event.ack();
    }

    private static MessageConsumer consume(Connection nats, String which) throws Exception {
        return switch (which) {
            case "CONFIRM" -> consume(nats, "CONSUMER_GROUP_CONFIRM", "order.placed", StreamConsumer::confirmOrder);
            case "FULFILL" -> consume(nats, "CONSUMER_GROUP_FULFILL", "order.placed", StreamConsumer::fulfillOrder);
            case "TRASH" -> consume(nats, "CONSUMER_GROUP_TRASH", "order.cancelled", StreamConsumer::cancelOrder);
            default -> throw new IllegalArgumentException("Invalid consumer type: " + which);
        };
    }

    private static MessageConsumer consume(Connection nats, String consumerGroup, String eventName, MessageHandler handler) throws Exception {
        final var streamName = "ORDER_EVENTS";

        // Documentation for consumer configuration:
        // https://docs.nats.io/nats-concepts/jetstream/consumers#configuration
        //
        // The "durable" option is what NATS calls their equivalent setting for Kafka's "consumer group".
        nats.jetStreamManagement().addOrUpdateConsumer(streamName, ConsumerConfiguration.builder()
            .durable(consumerGroup)
            .filterSubject(eventName)
            .build());

        // Make sure to get a JET STREAM consumer context, not a core NATS consumer context!
        return nats.jetStream().getConsumerContext(streamName, consumerGroup).consume(handler);
    }
}
