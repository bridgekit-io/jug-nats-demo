package io.bridgekit.nats.kvcache;

import java.io.IOException;

import static io.bridgekit.nats.Utils.randomInt;
import static io.bridgekit.nats.Utils.sleepSeconds;
import io.bridgekit.nats.EnterListener;
import io.bridgekit.nats.Logger;
import io.nats.client.Connection;
import io.nats.client.JetStreamApiException;
import io.nats.client.KeyValue;
import io.nats.client.Nats;
import io.nats.client.api.KeyValueConfiguration;
import io.nats.client.api.StorageType;

/**
 * Technically this is both reads and writes to a NATS key/value store. This simulates a very primitive
 * mechanism for tracking the number of API calls made by different users. Maybe you're trying to throttle
 * usage and need a fast way to access/update the call counts.
 *
 * <pre>
 * # Usage
 * make demo-kv-cache-writer
 * </pre>
 */
public class CacheWriter {
    private static final Logger logger = Logger.instance(CacheWriter.class);

    public static void main(String[] args) throws Exception {
        logger.info("Connecting to NATS server and ephemeral K/V store.");
        var nats = Nats.connect("nats://localhost:4222");
        var kv = connectKeyValueStore(nats, "account-api-calls");

        logger.info("Press ENTER to quit.");
        var enter = new EnterListener();

        while (enter.notPressed()) {
            incrementRandomAccount(kv);
            sleepSeconds(1);
        }

        logger.info("Bye, bye!");
        nats.close();
    }

    /**
     * In this fake example, there are 5 accounts. This randomly picks one of those accounts and
     * increments their API request count by one.
     */
    private static void incrementRandomAccount(KeyValue kv) throws JetStreamApiException, IOException {
        // Pick one of our 5 random accounts.
        var accountKey = "account." + randomInt(0, 5) + ".request-total";

        // Fetch the current total and increment it. Null entry means it wasn't in the store.
        var entry = kv.get(accountKey);
        var newTotal = entry == null ? 1 : (entry.getValueAsLong() + 1);

        // Write the new total to the K/V store.
        logger.info("Incrementing %s -> %d", accountKey, newTotal);
        kv.put(accountKey, newTotal); // Value can be a String, Number, or byte[]
    }

    public static KeyValue connectKeyValueStore(Connection nats, String storeName) throws Exception {
        nats.keyValueManagement().create(KeyValueConfiguration.builder()
            .name(storeName)
            .storageType(StorageType.Memory)
            .build());

        return nats.keyValue(storeName);
    }
}
