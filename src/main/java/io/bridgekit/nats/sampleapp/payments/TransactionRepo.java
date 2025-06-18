package io.bridgekit.nats.sampleapp.payments;

import java.util.List;
import java.util.NoSuchElementException;

import static io.bridgekit.nats.Utils.randomAlphanumeric;
import static io.bridgekit.nats.sampleapp.payments.PaymentService.Transaction.PROCESSOR_APPLE_PAY;
import static io.bridgekit.nats.sampleapp.payments.PaymentService.Transaction.PROCESSOR_STRIPE;
import static io.bridgekit.nats.sampleapp.payments.PaymentService.Transaction.STATUS_AUTHORIZED;
import static io.bridgekit.nats.sampleapp.payments.PaymentService.Transaction.STATUS_CHARGED;
import static java.lang.String.format;
import io.bridgekit.nats.Logger;
import io.bridgekit.nats.Utils;
import io.bridgekit.nats.sampleapp.KeyValueStore;
import io.bridgekit.nats.sampleapp.payments.PaymentService.Transaction;

/**
 * Manages the database read/write operations for our Transactions table. In reality, there's no "database".
 * Under the hood, we're actually just using a NATS key/value store to store the data.
 *
 * @see KeyValueStore For more info on how we store the fake data.
 */
public class TransactionRepo {
    private final Logger logger;
    private final KeyValueStore<Transaction> store;

    public TransactionRepo() {
        this.logger = Logger.instance(TransactionRepo.class);
        this.store = new KeyValueStore<>(Transaction.class, "fake-db-transactions");
        populateFakeDataIfEmpty();
    }

    /**
     * Returns a list of the unique transactions in the DB. Pretend that we're filtering based on customer :)
     */
    public List<Transaction> search() {
        return store.values();
    }

    /**
     * Fetches a transaction given either its actual transaction id or the associated order id.
     *
     * @param transactionID The id of the actual transaction
     * @param orderID       The id of the order associated with the transaction
     * @return The matching transaction record
     * @throws NoSuchElementException If neither id matches a transaction in the DB.
     */
    public Transaction get(String transactionID, String orderID) {
        var transaction = Utils.optional(
            () -> store.get(transactionID),
            () -> store.get(orderID));

        return transaction.orElseThrow(() -> {
            var message = format("Transaction not found: %s/%s", transactionID, orderID);
            return new NoSuchElementException(message);
        });
    }

    /**
     * Assigns a super sophisticated unique id to the transaction, and writes it to the underlying datastore.
     *
     * @param t The state of the new transaction
     * @return The transaction parameter, now with its 'transactionID' assigned.
     */
    public Transaction create(Transaction t) {
        t.transactionID = randomAlphanumeric(4);
        store.put(t.transactionID, t);
        store.put(t.orderID, t);
        return t;
    }

    /**
     * Writes the updated values over the existing transaction record.
     *
     * @param t The updated transaction data to persist.
     * @return The transaction param, as-is.
     */
    public Transaction update(Transaction t) {
        store.put(t.transactionID, t);
        store.put(t.orderID, t);
        return t;
    }

    /**
     * Rather than starting with an empty database, this will seed our datastore with some fake
     * transactions that you can play with. It only writes these records if it doesn't look like
     * the NATS K/V store has any transaction records already.
     * <p>
     * To reset the state so this will actually populate again, you'll need to wipe the NATS data:
     * <pre>
     *     # In terminal A
     *     make nats-clean
     *     make nats
     *
     *     # In terminal B (will populate fake data again)
     *     make demo-app
     * </pre>
     */
    private void populateFakeDataIfEmpty() {
        if (!store.isEmpty()) {
            logger.info("Fake transaction data already present.");
            return;
        }

        logger.info("Loading fake transaction data.");
        var transaction = new Transaction();
        transaction.transactionID = "X";
        transaction.orderID = "123";
        transaction.total = 1999;
        transaction.status = STATUS_CHARGED;
        transaction.processorID = PROCESSOR_STRIPE;
        transaction.processorToken = randomAlphanumeric(8);
        update(transaction);

        transaction = new Transaction();
        transaction.transactionID = "Y";
        transaction.orderID = "456";
        transaction.total = 2700;
        transaction.status = STATUS_AUTHORIZED;
        transaction.processorID = PROCESSOR_STRIPE;
        transaction.processorToken = randomAlphanumeric(8);
        update(transaction);

        transaction = new Transaction();
        transaction.transactionID = "Z";
        transaction.orderID = "789";
        transaction.total = 44997;
        transaction.status = STATUS_CHARGED;
        transaction.processorID = PROCESSOR_APPLE_PAY;
        transaction.processorToken = randomAlphanumeric(8);
        update(transaction);
    }
}
