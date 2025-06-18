package io.bridgekit.nats.sampleapp.notifications;

import io.bridgekit.nats.Logger;
import io.bridgekit.nats.sampleapp.EventGateway.Publisher;

/**
 * The notification service pretends to fire off emails or text messages to users for
 * various reasons; an order was placed, an order was cancelled, etc.
 */
public class NotificationServiceHandler implements NotificationService{
    private final Logger logger;
    private final Publisher publisher;

    public NotificationServiceHandler(Publisher eventPublisher) {
        this.logger = Logger.instance(NotificationService.class);
        this.publisher = eventPublisher;
    }

    /**
     * @inheritDoc
     */
    @Override
    public void sendOrderPlacedMessage(OrderNotificationRequest req) {
        logger.info("sendOrderPlacedMessage(OrderID: %s)", req.orderID);

        //
        // Pretend we looked up the order/customer and fired off an email/text/whatever.
        //
        publisher.publish("notification.orderPlaced", req);
    }

    /**
     * @inheritDoc
     */
    @Override
    public void sendOrderCancelledMessage(OrderNotificationRequest req) {
        logger.info("sendOrderCancelledMessage(OrderID: %s)", req.orderID);

        //
        // Pretend we looked up the order/customer and fired off an email/text/whatever.
        //
        publisher.publish("notification.orderCancelled", req);
    }
}
