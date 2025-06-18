package io.bridgekit.nats.sampleapp;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static io.bridgekit.nats.Utils.hasText;
import static io.bridgekit.nats.Utils.marshalJSON;
import static io.bridgekit.nats.Utils.unmarshalJSON;
import io.nats.client.Connection;
import io.nats.client.JetStreamApiException;
import io.nats.client.KeyValue;
import io.nats.client.Nats;
import io.nats.client.api.KeyValueConfiguration;
import io.nats.client.api.StorageType;

/**
 * In a real application, you'd probably want to store your persistent data in an actual database. For this
 * demo, we don't want the complexity of setting up Postgres or anything like that. This is also a demo about
 * how to use NATS, so this utilizes NATS's key/value storage capabilities to provide a simple datastore.
 * <p>
 * Each instance of this class builds a NATS Key/Value store that maps a record id to a JSON document
 * representing that record's current state.
 */
public class KeyValueStore<T> {
    private final Class<T> entityType;
    private final Connection nats;
    private final KeyValue keyValue;

    public KeyValueStore(Class<T> entityType, String bucketName) {
        try {
            this.entityType = entityType;
            this.nats = Nats.connect("nats://localhost:4222");
            this.keyValue = connectKeyValueStore(bucketName);
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns true if this bucket/store does not contain ANY records. We use this internally to determine
     * if we need to fill in the fake order/transaction data.
     */
    public boolean isEmpty() {
        try {
            // In a production app, asking for "all keys" is probably a bad idea since it doesn't scale
            // even remotely well. I'm doing this here because I know the sample data set is a handful of
            // keys. If you do need to iterate all keys, you can use `keyValue.consumeKeys()` to create
            // a cursor and stream the keys.
            return keyValue.keys().isEmpty();
        }
        catch (IOException | JetStreamApiException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Fetches all entries from store and unmarshalls their JSON data back into raw record instances.
     */
    public List<T> values() {
        try {
            // Again, this is terribly inefficient, but for our 3-record database it's fine.
            return keyValue.keys().stream()
                .map(this::get)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .distinct() // some repos list the same record under multiple keys, so only show unique records
                .toList();
        }
        catch (IOException | JetStreamApiException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Looks up a single record from the key/value store.
     *
     * @param id The lookup key/id you used when previously adding the record to the datastore.
     * @return An Optional wrapping the record. This will be a non-null, but empty if the record doesn't exist.
     */
    public Optional<T> get(String id) {
        try {
            return Optional.ofNullable(hasText(id) ? keyValue.get(id) : null)
                .map(entry -> entry.getValueAsString())
                .map(json -> unmarshalJSON(json, entityType));
        }
        catch (IOException | JetStreamApiException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Writes the record to the key/value store. The record will be marshalled to JSON for storage.
     *
     * @param id     The lookup key you can use to retrieve this record later.
     * @param record The raw record you want to write to the store.
     */
    public void put(String id, T record) {
        try {
            keyValue.put(id, marshalJSON(record));
        }
        catch (IOException | JetStreamApiException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * This is a copy/paste of the k/v setup code used in both the CacheWriter and SettingsWriter
     * demos from earlier. Nothing fancy.
     */
    private KeyValue connectKeyValueStore(String bucketName) throws Exception {
        nats.keyValueManagement().create(KeyValueConfiguration.builder()
            .name(bucketName)
            .storageType(StorageType.File)
            .build());

        return nats.keyValue(bucketName);
    }
}
