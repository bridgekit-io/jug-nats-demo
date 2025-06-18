package io.bridgekit.nats.pubsub;

import static io.bridgekit.nats.Utils.asString;
import io.bridgekit.nats.EnterListener;
import io.bridgekit.nats.Logger;
import io.nats.client.Message;
import io.nats.client.Nats;

/**
 * Subscribes to a NATS event topic and prints the message payload to the console.
 *
 * <pre>
 * # Usage
 * make demo-pubsub-subscriber
 * </pre>
 */
public class Subscriber {
    private static final Logger logger = Logger.instance(Subscriber.class);

    public static void main(String[] args) throws Exception {
        logger.info("Connecting to NATS server and subscribing.");
        var nats = Nats.connect("nats://localhost:4222");

        // Subject matching wildcards: https://docs.nats.io/nats-concepts/subjects#wildcards
        nats.createDispatcher(Subscriber::handleMessage).subscribe("order.>");

        logger.info("Press ENTER to quit.");
        new EnterListener().awaitPressed();

        logger.info("Bye, bye!");
        nats.close();
    }

    private static void handleMessage(Message msg) {
        logger.info("Received: %s -> %s", msg.getSubject(), asString(msg.getData()));
    }
}
