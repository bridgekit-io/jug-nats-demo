package io.bridgekit.nats.sampleapp.orders;

import java.util.List;

/**
 * Provides operations to let customers place, track, and cancel orders. It's also used by the
 * warehouse to mark when items ship.
 */
public interface OrderService {
    /**
     * Fetches all orders matching the given criteria.
     */
    List<Order> searchOrders(SearchOrdersRequest req);

    /**
     * Fetches the current state of the specified order.
     */
    Order getOrder(GetOrderRequest req);

    /**
     * Creates a new order with the given details.
     */
    Order placeOrder(PlaceOrderRequest req);

    /**
     * Invoked by the warehouse once the order is on the delivery truck.
     */
    Order shipOrder(ShipOrderRequest req);

    /**
     * Begins the cancellation process of the order. This is asynchronous,
     * so some workflow tasks like refunding the transaction may still be
     * pending when this operation completes.
     */
    Order cancelOrder(CancelOrderRequest req);

    /**
     * The DTO describing the current state of an order in the system.
     */
    class Order {
        public static final String STATUS_PLACED = "PLACED";
        public static final String STATUS_SHIPPED = "SHIPPED";
        public static final String STATUS_FULFILLED = "FULFILLED";
        public static final String STATUS_CANCELLED = "CANCELLED";

        public String orderID;
        public String itemID;
        public String itemName;
        public long quantity;
        public long price;
        public long total;
        public String status;
        public String trackingNumber;

        @Override
        public String toString() {
            return "Order[ID:" + orderID + ", Status:" + status + "]";
        }

        @Override
        public int hashCode() {
            return orderID.hashCode();
        }

        @Override
        public boolean equals(Object other) {
            if (other instanceof Order otherOrder) {
                return orderID.equals(otherOrder.orderID);
            }
            return false;
        }
    }

    class SearchOrdersRequest {
    }

    class GetOrderRequest {
        public String orderID;
    }

    class PlaceOrderRequest {
        public String itemID;
        public String itemName;
        public long quantity;
        public long price;
        public String processorID;
        public String processorToken;
    }

    class ShipOrderRequest {
        public String orderID;
    }

    class CancelOrderRequest {
        public String orderID;
    }
}
