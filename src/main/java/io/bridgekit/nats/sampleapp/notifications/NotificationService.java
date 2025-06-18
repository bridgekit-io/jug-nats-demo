package io.bridgekit.nats.sampleapp.notifications;

/**
 * The notification service pretends to fire off emails or text messages to users for
 * various reasons; an order was placed, an order was cancelled, etc.
 */
public interface NotificationService {
    /**
     * Fired after an order has been placed. It sends a notification to the user letting them
     * know that the order has been received, and we're going to start processing it.
     */
    void sendOrderPlacedMessage(OrderNotificationRequest req);

    /**
     * Fires after a customer cancels an order. It sends a notification confirming that the
     * cancellation process is underway.
     */
    void sendOrderCancelledMessage(OrderNotificationRequest req);

    /**
     * Provides the necessary context for an order-based notification.
     */
    class OrderNotificationRequest {
        public String orderID;
    }
}
