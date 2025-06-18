package io.bridgekit.nats.sampleapp.orders;

import java.util.List;
import java.util.NoSuchElementException;

import static io.bridgekit.nats.Utils.randomAlphanumeric;
import static io.bridgekit.nats.sampleapp.orders.OrderService.Order.STATUS_FULFILLED;
import static io.bridgekit.nats.sampleapp.orders.OrderService.Order.STATUS_PLACED;
import static io.bridgekit.nats.sampleapp.orders.OrderService.Order.STATUS_SHIPPED;
import io.bridgekit.nats.Logger;
import io.bridgekit.nats.sampleapp.KeyValueStore;
import io.bridgekit.nats.sampleapp.orders.OrderService.Order;

/**
 * Manages the database read/write operations for our Orders table. In reality, there's no "database".
 * Under the hood, we're actually just using a NATS key/value store to store the data.
 *
 * @see KeyValueStore For more info on how we store the fake data.
 */
public class OrderRepo {
    private final Logger logger;
    private final KeyValueStore<Order> store;

    public OrderRepo() {
        this.logger = Logger.instance(OrderRepo.class);
        this.store = new KeyValueStore<>(Order.class, "fake-db-orders");
        populateFakeDataIfEmpty();
    }

    /**
     * Returns all Order records in our datastore. Use your imagination to pretend that this can
     * filter based on a customer id, status, date, etc.
     */
    public List<Order> search() {
        return store.values();
    }

    /**
     * Fetches the Order with the given id.
     *
     * @param orderID The id of the order record to grab.
     * @return The matching record/entity.
     *
     * @throws NoSuchElementException If there's no record associated with this id.
     */
    public Order get(String orderID) {
        return store.get(orderID).orElseThrow(() -> new NoSuchElementException("Order not found: " + orderID));
    }

    /**
     * Assigns a super sophisticated unique id to the order, and writes it to the underlying datastore.
     *
     * @return The order record, now with its 'orderID' assigned.
     */
    public Order create(Order order) {
        order.orderID = randomAlphanumeric(4);
        store.put(order.orderID, order);
        return order;
    }

    /**
     * Writes the updated values over the existing order record.
     *
     * @param order The updated order data to persist.
     * @return The order param, as-is.
     */
    public Order update(Order order) {
        store.put(order.orderID, order);
        return order;
    }

    /**
     * Rather than starting with an empty database, this will seed our datastore with some fake
     * orders that you can start to play with. It only writes these records if it doesn't look like
     * the NATS K/V store has any order records already.
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
            logger.info("Fake order data already present.");
            return;
        }

        logger.info("Loading fake order data.");
        var order = new Order();
        order.orderID = "123";
        order.itemID = "ABC";
        order.itemName = "Do-it-yourself Brain Surgery Kit";
        order.quantity = 1;
        order.price = 1999;
        order.total = order.price * order.quantity;
        order.status = STATUS_SHIPPED;
        order.trackingNumber = randomAlphanumeric(5);
        update(order);

        order = new Order();
        order.orderID = "456";
        order.itemID = "DEF";
        order.itemName = "Rectangular Basketball";
        order.quantity = 3;
        order.price = 900;
        order.total = order.price * order.quantity;
        order.status = STATUS_PLACED;
        update(order);

        order = new Order();
        order.orderID = "789";
        order.itemID = "GHI";
        order.itemName = "The Internet";
        order.quantity = 3;
        order.price = 14999;
        order.total = order.price * order.quantity;
        order.status = STATUS_FULFILLED;
        update(order);
    }
}
