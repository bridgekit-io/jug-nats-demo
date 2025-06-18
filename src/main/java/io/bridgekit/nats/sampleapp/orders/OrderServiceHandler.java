package io.bridgekit.nats.sampleapp.orders;

import java.util.List;

import static io.bridgekit.nats.Utils.randomAlphanumeric;
import static io.bridgekit.nats.sampleapp.orders.OrderService.Order.STATUS_CANCELLED;
import static io.bridgekit.nats.sampleapp.orders.OrderService.Order.STATUS_SHIPPED;
import io.bridgekit.nats.Logger;
import io.bridgekit.nats.sampleapp.EventGateway.Publisher;
import io.bridgekit.nats.sampleapp.payments.PaymentService;

/**
 * Provides operations to let customers place, track, and cancel orders. It's also used by the
 * warehouse to mark when items ship.
 */
public class OrderServiceHandler implements OrderService {
    private final Logger logger;
    private final OrderRepo orderRepo;
    private final Publisher eventPublisher;
    private final PaymentService paymentService;

    public OrderServiceHandler(Publisher eventPublisher, PaymentService paymentService) throws Exception {
        this.logger = Logger.instance(OrderService.class);
        this.orderRepo = new OrderRepo();
        this.eventPublisher = eventPublisher;
        this.paymentService = paymentService;
    }

    /**
     * @inheritDoc
     */
    @Override
    public List<Order> searchOrders(SearchOrdersRequest req) {
        logger.info("Searching orders for customer");
        return orderRepo.search();
    }

    /**
     * @inheritDoc
     */
    @Override
    public Order getOrder(GetOrderRequest req) {
        logger.info("Fetching order: %s", req.orderID);
        return orderRepo.get(req.orderID);
    }

    /**
     * @inheritDoc
     */
    @Override
    public Order placeOrder(PlaceOrderRequest req) {
        logger.info("Placing order for '%s' x%d", req.itemName, req.quantity);

        var order = new Order();
        order.status = "PLACED";
        order.itemID = req.itemID;
        order.itemName = req.itemName;
        order.quantity = req.quantity;
        order.price = req.price;
        order.total = order.price * order.quantity;
        order = orderRepo.create(order);

        var payment = new PaymentService.AuthorizeRequest();
        payment.orderID = order.orderID;
        payment.total = order.total;
        payment.processorID = req.processorID;
        payment.processorToken = req.processorToken;
        paymentService.authorize(payment);

        eventPublisher.publish("order.placed", order);
        return order;
    }

    /**
     * @inheritDoc
     */
    @Override
    public Order shipOrder(ShipOrderRequest req) {
        var order = orderRepo.get(req.orderID);

        switch (order.status) {
        case STATUS_CANCELLED:
            throw new IllegalStateException("Can't ship cancelled order: " + order.orderID);
        case STATUS_SHIPPED:
            logger.info("Order already shipped: %s", order);
            return order;
        default:
            logger.info("Shipping order: %s", req.orderID);
            order.status = "SHIPPED";
            order.trackingNumber = randomAlphanumeric(5);
            order = orderRepo.update(order);
        }

        eventPublisher.publish("order.shipped", order);
        return order;
    }

    /**
     * Invoked by the warehouse once the order is on the delivery truck.
     */
    @Override
    public Order cancelOrder(CancelOrderRequest req) {
        var order = orderRepo.get(req.orderID);

        switch (order.status) {
        case STATUS_CANCELLED:
            logger.info("Order already cancelled: %s", order);
            return order;
        default:
            logger.info("Cancelling order: %s", order);
            order.status = "CANCELLED";
            order = orderRepo.update(order);
        }

        eventPublisher.publish("order.cancelled", order);
        return order;
    }
}
