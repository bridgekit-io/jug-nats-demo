package io.bridgekit.nats.kvsettings;

import static io.bridgekit.nats.kvsettings.SettingsWriter.createKeyValueStore;
import io.bridgekit.nats.EnterListener;
import io.bridgekit.nats.Logger;
import io.nats.client.Nats;
import io.nats.client.api.KeyValueWatcher;

/**
 * This is the consumer side of the SettingsWriter demo. Backend services typically need to be fed some sort
 * of configuration environment/file/etc. This uses a NATS Key/Value store to fetch configuration values and
 * registers event listeners that allow your service to react to changes in those values - allowing you to
 * change configuration on the fly without requiring service restarts!
 *
 * <pre>
 * # Usage
 * make demo-kv-settings-reader
 * </pre>
 */
public class SettingsReader {
    private static final Logger logger = Logger.instance(SettingsReader.class);

    public static void main(String[] args) throws Exception {
        logger.info("Connecting to NATS server and K/V store.");
        var nats = Nats.connect("nats://localhost:4222");
        var kv = createKeyValueStore(nats, "centralized_config");

        logger.info("Setting up watchers to detect configuration changes.");
        var watcherA = kv.watch("settings.db.url", (SettingsListener) entry -> {
            logger.info("Reconnecting to database at: %s", entry.getValueAsString());
        });
        var watcherB = kv.watch("settings.s3.key", (SettingsListener) entry -> {
            logger.info("Rotating S3 secret access key: %s", entry.getValueAsString());
        });

        logger.info("Press ENTER to quit.");
        new EnterListener().awaitPressed();

        logger.info("Bye, bye!");
        watcherA.close();
        watcherB.close();
        nats.close();
    }

    private interface SettingsListener extends KeyValueWatcher {
        @Override
        default void endOfData() {}
    }
}
