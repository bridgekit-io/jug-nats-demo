package io.bridgekit.nats.objectstore;

import java.io.File;
import java.io.FileOutputStream;

import static io.bridgekit.nats.Utils.asFile;
import static io.bridgekit.nats.Utils.firstArg;
import static io.bridgekit.nats.objectstore.ObjectWriter.connectObjectStore;
import io.bridgekit.nats.Logger;
import io.nats.client.Nats;

/**
 * This demo attempts to read an object/file from the NATS object store added by previously running
 * the ObjectWriter demo. It will download the file from the object store into the "data/out" directory
 * and print the JSON for the entire metadata structure.
 *
 * <pre>
 * # Usage
 * make demo-objects-reader-a
 * make demo-objects-reader-b
 * make demo-objects-reader-c
 * </pre>
 */
public class ObjectReader {
    private static final Logger logger = Logger.instance(ObjectReader.class);

    public static void main(String[] args) throws Exception {
        logger.info("Connecting to NATS server and object store.");
        var nats = Nats.connect("nats://localhost:4222");
        var objectStore = connectObjectStore(nats, "best-favorite-animal-images");
        var fileName = firstArg(args);

        try (var downloadStream = new FileOutputStream(asFile("data/downloads", fileName))) {
            var objectInfo = objectStore.get(fileName, downloadStream);
            var recordID = objectInfo.getObjectMeta().getHeaders().getFirst("Record-ID");
            logger.info("Downloaded data/downloads/%s for record %s", fileName, recordID);
        }
        catch (IllegalArgumentException e) { // An admittedly clunky exception for this case...
            logger.info("Object key not found: %s", fileName);
        }

        logger.info("Bye, bye!");
        nats.close();
    }
}
