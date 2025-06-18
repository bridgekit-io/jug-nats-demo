package io.bridgekit.nats.sampleapp;

import static io.bridgekit.nats.Utils.asString;
import static io.bridgekit.nats.Utils.closeOnShutdown;
import static io.bridgekit.nats.Utils.firstArgOptional;
import static io.bridgekit.nats.Utils.unmarshalJSON;
import io.bridgekit.nats.EnterListener;
import io.bridgekit.nats.Logger;
import io.bridgekit.nats.sampleapp.analytics.AnalyticsService;
import io.bridgekit.nats.sampleapp.analytics.AnalyticsService.TrackEventRequest;
import io.bridgekit.nats.sampleapp.analytics.AnalyticsServiceHandler;
import io.bridgekit.nats.sampleapp.notifications.NotificationService;
import io.bridgekit.nats.sampleapp.notifications.NotificationService.OrderNotificationRequest;
import io.bridgekit.nats.sampleapp.notifications.NotificationServiceHandler;
import io.bridgekit.nats.sampleapp.orders.OrderService;
import io.bridgekit.nats.sampleapp.orders.OrderService.CancelOrderRequest;
import io.bridgekit.nats.sampleapp.orders.OrderService.GetOrderRequest;
import io.bridgekit.nats.sampleapp.orders.OrderService.PlaceOrderRequest;
import io.bridgekit.nats.sampleapp.orders.OrderService.SearchOrdersRequest;
import io.bridgekit.nats.sampleapp.orders.OrderService.ShipOrderRequest;
import io.bridgekit.nats.sampleapp.orders.OrderServiceHandler;
import io.bridgekit.nats.sampleapp.payments.PaymentService;
import io.bridgekit.nats.sampleapp.payments.PaymentService.ChargebackRequest;
import io.bridgekit.nats.sampleapp.payments.PaymentService.GetTransactionRequest;
import io.bridgekit.nats.sampleapp.payments.PaymentService.RefundRequest;
import io.bridgekit.nats.sampleapp.payments.PaymentService.SearchTransactionsCriteria;
import io.bridgekit.nats.sampleapp.payments.PaymentServiceHandler;

/**
 * This demo runs a set of services simulating a very basic order management system.
 *
 * <ul>
 *   <li>OrderService: Tracks the item, quantity, pricing, and status info for orders.</li>
 *   <li>PaymentService: Manages the financial transactions associated with orders.</li>
 *   <li>NotificationService: Delivers messages to customers regarding their orders.</li>
 *   <li>AnalyticsService: Internal service that just sucks in all events for reporting purposes.</li>
 * </ul>
 *
 * None of these services *really* do any of that. They're basically just stubbed out methods for specific
 * workflow tasks that write some logs. The real purpose of this demonstration is to show how you can
 * very easily build event-based workflows and integrate NATS into your architecture with very little effort.
 * <p>
 * You have two options for running this demo. The first is to just run everything in a single process for simplicity.
 * The other is to run the API in one terminal and the event-based service routes in another.
 * <pre>
 * # Option 1: Run everything in one terminal/process:
 * make demo-app
 *
 * # Option 2: Run the API in one terminal/process:
 * make demo-app-api
 * # Option 2: Run the event-based services in another:
 * make demo-app-events
 * </pre>
 * When using option 2, you can run <code>make demo-app-events</code> in multiple consoles to see events
 * being load-balanced between instances.
 */
public class Main {
    private static final Logger logger = Logger.instance(Main.class);

    public static void main(String[] args) throws Exception {
        // Some service methods are invoked via the API; others through event consumption. Both, however, need
        // access to the NATS broker. Event-based routes need it to register consumers, and all services need
        // it to be able to publish events. The "publish" use-case is why all service handlers accept the
        // publisher as a dependency.
        var broker = new EventGateway("localhost", 4222);
        var publisher = broker.publisher();

        // Raw business logic service instances that are all oblivious to request/response transport.
        var services = new Services();
        services.paymentService = new PaymentServiceHandler(publisher);
        services.orderService = new OrderServiceHandler(publisher, services.paymentService);
        services.notificationService = new NotificationServiceHandler(publisher);
        services.analyticsService = new AnalyticsServiceHandler(); // no publishing... it does its work in secret!

        // Determine which of these you ran:
        //
        //   make demo-app-api
        //   make demo-app-events
        //   make demo-app
        //
        switch (firstArgOptional(args)) {
        case "API":
            startApiGateway(services);
            break;
        case "EVENT":
        case "EVENTS":
            startEventGateway(services, broker);
            break;
        default:
            // If you don't supply an arg, just run everything in one VM.
            startEventGateway(services, broker);
            startApiGateway(services);
            break;
        }

        logger.info("Press ENTER to exit.");
        new EnterListener().awaitPressed();

        logger.info("Bye, bye! I miss you already!");
        System.exit(0); // Javalin prevents shutdown at end of main, so we need to explicitly quit.
    }

    /**
     * Registers the routes for our REST API and starts the HTTP server. Not all services/methods are
     * exposed in the API; just the tasks we want to provide some external UX for.
     *
     * @param services Our collection of raw business-logic-only service handlers that do the *real* work.
     */
    private static void startApiGateway(Services services) {
        var api = new ApiGateway("localhost", 7222)
            .GET("/order", ctx -> {
                var req = unmarshalJSON(ctx.body(), SearchOrdersRequest.class);
                return services.orderService.searchOrders(req);
            })
            .PUT("/order", ctx -> {
                var req = unmarshalJSON(ctx.body(), PlaceOrderRequest.class);
                return services.orderService.placeOrder(req);
            })
            .GET("/order/{orderID}", ctx -> {
                var req = new GetOrderRequest();
                req.orderID = ctx.pathParam("orderID");
                return services.orderService.getOrder(req);
            })
            .DELETE("/order/{orderID}", ctx -> {
                var req = new CancelOrderRequest();
                req.orderID = ctx.pathParam("orderID");
                return services.orderService.cancelOrder(req);
            })
            .POST("/order/{orderID}/shipping", ctx -> {
                var req = new ShipOrderRequest();
                req.orderID = ctx.pathParam("orderID");
                return services.orderService.shipOrder(req);
            })
            .GET("/transaction", ctx -> {
                var req = new SearchTransactionsCriteria();
                return services.paymentService.searchTransactions(req);
            })
            .GET("/transaction/{transactionID}", ctx -> {
                var req = new GetTransactionRequest();
                req.transactionID = ctx.pathParam("transactionID");
                return services.paymentService.getTransaction(req);
            })
            .POST("/transaction/{transactionID}/chargeback", ctx -> {
                var req = new ChargebackRequest();
                req.transactionID = ctx.pathParam("transactionID");
                return services.paymentService.chargeback(req);
            });

        api.start();
        closeOnShutdown(api);
    }

    /**
     * Sets up the NATS event streams and consumers that enable services to asynchronously listen for
     * events elsewhere in the system to trigger the next task in our ordering workflows.
     *
     * @param services Our collection of raw business-logic-only service handlers that do the *real* work.
     * @param gateway  Our managed connection to the NATS message broker.
     */
    private static void startEventGateway(Services services, EventGateway gateway) {
        gateway.stream("EVENT_GATEWAY_ORDERS", "order.>")
            //
            // Send confirmation email when an order is placed.
            //
            .on("order.placed", "group_notification_order_placed", msg -> {
                var req = unmarshalJSON(msg.getData(), OrderNotificationRequest.class);
                services.notificationService.sendOrderPlacedMessage(req);
            })
            //
            // Send confirmation email when an order is cancelled.
            //
            .on("order.cancelled", "group_notification_order_cancelled", msg -> {
                var req = unmarshalJSON(msg.getData(), OrderNotificationRequest.class);
                services.notificationService.sendOrderCancelledMessage(req);
            })
            //
            // Refund the payment when an order is cancelled.
            //
            .on("order.cancelled", "group_payment_refund", msg -> {
                var req = unmarshalJSON(msg.getData(), RefundRequest.class);
                services.paymentService.refund(req);
            })
            //
            // Everything goes to the analytics service.
            //
            .on("order.>", "group_analytics_trackEvent", msg -> {
                var req = new TrackEventRequest();
                req.event = msg.getSubject();
                req.json = asString(msg.getData());
                services.analyticsService.trackEvent(req);
            });

        gateway.stream("EVENT_GATEWAY_PAYMENTS", "payment.>")
            //
            // When CC company notifies payment service of a chargeback, cancel the order.
            //
            .on("payment.chargeback", "group_order_cancel", msg -> {
                var req = unmarshalJSON(msg.getData(), CancelOrderRequest.class);
                services.orderService.cancelOrder(req);
            })
            //
            // Everything goes to the analytics service.
            //
            .on("payment.>", "group_analytics_payments", msg -> {
                var req = new TrackEventRequest();
                req.event = msg.getSubject();
                req.json = asString(msg.getData());
                services.analyticsService.trackEvent(req);
            });

        gateway.stream("EVENT_GATEWAY_NOTIFICATIONS", "notification.>")
            //
            // Everything goes to the analytics service.
            //
            .on("notification.>", "group_analytics_notifications", msg -> {
                var req = new TrackEventRequest();
                req.event = msg.getSubject();
                req.json = asString(msg.getData());
                services.analyticsService.trackEvent(req);
            });

        gateway.start();
        closeOnShutdown(gateway);
        logger.info("Event gateway now running: %s", gateway.url());
    }

    /**
     * A simple data structure that wrangles the raw business-logic-only service handlers for all
     * services in the system. This keeps us from having to write methods that accept a boat-load
     * of parameters; instead letting you accept one in order to create routes for any service/method.
     */
    private static class Services {
        public OrderService orderService;
        public PaymentService paymentService;
        public NotificationService notificationService;
        public AnalyticsService analyticsService;
    }
}
