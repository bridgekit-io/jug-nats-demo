package io.bridgekit.nats.pubsub;

import static io.bridgekit.nats.Utils.randomAlphanumeric;
import static io.bridgekit.nats.Utils.sleepSeconds;
import static java.nio.charset.StandardCharsets.UTF_8;
import io.bridgekit.nats.Logger;
import io.bridgekit.nats.EnterListener;
import io.nats.client.Connection;
import io.nats.client.Nats;

/**
 * Publishes random messages every 2 seconds to a standard pub/sub queue.
 *
 * <pre>
 * # Usage
 * make demo-pubsub-publisher
 * </pre>
 */
public class Publisher {
    private static final Logger logger = Logger.instance(Publisher.class);

    public static void main(String[] args) throws Exception {
        logger.info("Connecting to NATS server.");
        var nats = Nats.connect("nats://localhost:4222");

        logger.info("Press ENTER to quit.");
        var enter = new EnterListener();

        while (enter.notPressed()) {
            publishMessage(nats);
            sleepSeconds(2);
        }

        logger.info("Bye, bye!");
        nats.close();
    }

    private static void publishMessage(Connection nats) {
        var eventName = "order.placed";
        var eventData = randomAlphanumeric(5);

        logger.info("Publishing: %s -> %s", eventName, eventData);
        nats.publish(eventName, eventData.getBytes(UTF_8));
    }
}
