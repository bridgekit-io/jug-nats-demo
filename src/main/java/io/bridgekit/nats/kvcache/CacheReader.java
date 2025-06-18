package io.bridgekit.nats.kvcache;

import java.io.IOException;

import static io.bridgekit.nats.Utils.sleepSeconds;
import static io.bridgekit.nats.kvcache.CacheWriter.connectKeyValueStore;
import io.bridgekit.nats.EnterListener;
import io.bridgekit.nats.Logger;
import io.nats.client.JetStreamApiException;
import io.nats.client.KeyValue;
import io.nats.client.Nats;

/**
 * This simulates remote, read-only access to the cache entries updated by the CacheWriter. Every 5
 * seconds it fetches all 5 users' cached totals and prints them to the console.
 *
 * <pre>
 * # Usage
 * make demo-kv-cache-reader
 * </pre>
 */
public class CacheReader {
    private static final Logger logger = Logger.instance(CacheReader.class);

    public static void main(String[] args) throws Exception {
        logger.info("Connecting to NATS server and ephemeral K/V store.");
        var nats = Nats.connect("nats://localhost:4222");
        var kv = connectKeyValueStore(nats, "account-api-calls");

        logger.info("Press ENTER to quit.");
        var enter = new EnterListener();

        while (enter.notPressed()) {
            printUserTotals(kv);
            sleepSeconds(5);
        }

        logger.info("Bye, bye!");
        nats.close();
    }

    /**
     * Looks up the current request totals for all 5 accounts and writes their values to the console.
     */
    private static void printUserTotals(KeyValue kv) throws JetStreamApiException, IOException {
        logger.info("");

        for (int i = 0; i < 5; i++) {
            var accountKey = "account." + i + ".request-total";
            var entry = kv.get(accountKey);
            var currentTotal = entry == null ? 0 : entry.getValueAsLong();

            logger.info("%s -> %d", accountKey, currentTotal);
        }
    }
}
