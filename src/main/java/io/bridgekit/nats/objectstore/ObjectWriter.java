package io.bridgekit.nats.objectstore;

import java.io.File;
import java.io.FileInputStream;

import static io.bridgekit.nats.Utils.firstArg;
import static io.bridgekit.nats.Utils.mimeType;
import static io.bridgekit.nats.Utils.randomAlphanumeric;
import static io.bridgekit.nats.Utils.asString;
import static java.util.UUID.randomUUID;
import io.bridgekit.nats.Logger;
import io.nats.client.Connection;
import io.nats.client.Nats;
import io.nats.client.ObjectStore;
import io.nats.client.api.ObjectMeta;
import io.nats.client.api.ObjectStoreConfiguration;
import io.nats.client.api.StorageType;
import io.nats.client.impl.Headers;

/**
 * This demo contains a basic example of how to take a local file, and upload it to a NATS
 * object store. It simulates a common use case where some database record has a file associated with
 * it. To avoid the anti-pattern of storing files in your SQL database, you can store the file in the
 * NATS object store and maintain a reference to the original database record.
 *
 * <pre>
 * # Usage
 * make demo-objects-writer-a
 * make demo-objects-writer-b
 * make demo-objects-writer-c
 * </pre>
 */
public class ObjectWriter {
    private static final Logger logger = Logger.instance(ObjectWriter.class);

    public static void main(String[] args) throws Exception {
        logger.info("Connecting to NATS server and object store.");
        var nats = Nats.connect("nats://localhost:4222");
        var objectStore = connectObjectStore(nats, "best-favorite-animal-images");
        var fileName = firstArg(args);

        logger.info("Uploading '%s' to object store", fileName);
        var data = new FileInputStream(new File("src/main/resources", fileName));
        var meta = ObjectMeta.builder(fileName)
            .headers(new Headers()
                .put("Record-ID", randomAlphanumeric(5))
                .put("E-Tag", randomAlphanumeric(5))
                .put("Content-Type", mimeType(fileName)))
            .build();

        var objectInfo = objectStore.put(meta, data);
        logger.info("File upload complete: %s", asString(objectInfo.serialize()));

        logger.info("Bye, bye!");
        data.close();
        nats.close();
    }

    public static ObjectStore connectObjectStore(Connection nats, String storeName) throws Exception {
        nats.objectStoreManagement().create(ObjectStoreConfiguration.builder()
            .name(storeName)
            .storageType(StorageType.File)
            .compression(true)
            .build());

        return nats.objectStore(storeName);
    }
}
