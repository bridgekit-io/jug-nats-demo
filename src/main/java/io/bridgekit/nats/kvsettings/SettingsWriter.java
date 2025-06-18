package io.bridgekit.nats.kvsettings;

import static io.bridgekit.nats.Utils.randomAlphanumeric;
import static io.bridgekit.nats.Utils.randomInt;
import static io.bridgekit.nats.Utils.sleepSeconds;
import io.bridgekit.nats.EnterListener;
import io.bridgekit.nats.Logger;
import io.nats.client.Connection;
import io.nats.client.KeyValue;
import io.nats.client.Nats;
import io.nats.client.api.KeyValueConfiguration;
import io.nats.client.api.StorageType;

/**
 * Another Key/Value store demo - but this one simulates using it as a mechanism for distributed settings/config
 * instead of reaching for something like etcd or consul. The "writer" side of the demo periodically changes the
 * configuration data stored in the Key/Value store to new values. Look at the SettingsReader class to see how
 * these changes are automatically broadcast to other processes/services that need to consume them.
 *
 * <pre>
 * # Usage
 * make demo-kv-settings-writer
 * </pre>
 */
public class SettingsWriter {
    private static final Logger logger = Logger.instance(SettingsWriter.class);

    public static void main(String[] args) throws Exception {
        logger.info("Connecting to NATS server and K/V store.");
        var nats = Nats.connect("nats://localhost:4222");
        var kv = createKeyValueStore(nats, "centralized_config");

        logger.info("Press ENTER to quit.");
        var enter = new EnterListener();

        while (enter.notPressed()) {
            changeSetting(kv, "settings.db.url", "localhost:" + randomInt(3000, 4000));
            sleepSeconds(2);

            changeSetting(kv, "settings.s3.key", randomAlphanumeric(5));
            sleepSeconds(2);
        }

        logger.info("Bye, bye!");
        nats.close();
    }

    private static void changeSetting(KeyValue kv, String setting, String newValue) throws Exception {
        logger.info("Changing setting: %s -> %s", setting, newValue);
        kv.put(setting, newValue);
    }

    public static KeyValue createKeyValueStore(Connection nats, String storeName) throws Exception {
        nats.keyValueManagement().create(KeyValueConfiguration.builder()
            .name(storeName)
            .storageType(StorageType.File)
            .build());

        return nats.keyValue(storeName);
    }
}
