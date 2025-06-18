package io.bridgekit.nats.sampleapp;

import java.io.Closeable;
import java.util.NoSuchElementException;
import java.util.function.Function;

import static io.bridgekit.nats.Utils.marshalJSON;
import static java.lang.String.format;
import io.bridgekit.nats.Logger;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.Handler;

/**
 * Provides routing, startup, and shutdown of the HTTP server that provides access to our REST API. It
 * utilizes Javalin/Jetty to do the heavy lifting. This provides some helpers that reduce some of the
 * boilerplate associated with setting up HTTP routes.
 */
public class ApiGateway implements Closeable {
    private static final Logger logger = Logger.instance(ApiGateway.class);

    private final Javalin javalin;
    private final String host;
    private final int port;

    public ApiGateway(String host, int port) {
        this.host = host;
        this.port = port;
        this.javalin = Javalin
            .create(config -> config.showJavalinBanner = false)
            .error(404, ctx -> respondError(ctx, 404, "Endpoint not found"));
    }

    /**
     * The base URL for all API endpoints.
     */
    public String url() {
        return format("%s://%s:%d", "http", host, port);
    }

    /**
     * Activates the HTTP server to start listening for API calls. The server runs in another
     * thread, so this call does NOT block.
     */
    public void start() {
        this.javalin.start(host, port);
        logger.info("Now running: %s", url());
    }

    /**
     * Registers an HTTP GET route.
     *
     * @param pathPattern The HTTP path that will route requests to this handler (e.g. "/user/{userID}")
     * @param handler
     *     The raw unit of work to invoke when the server receives a request matching the path
     *     pattern. It should return a raw value representing what you want to send back in the
     *     HTTP response. Don't worry about status codes or serializing JSON because the ApiGateway
     *     will automatically take care of that for you.
     */
    public ApiGateway GET(String pathPattern, Function<Context, ?> handler) {
        javalin.get(pathPattern, respond(handler));
        return this;
    }

    /**
     * Registers an HTTP PUT route.
     *
     * @param pathPattern The HTTP path that will route requests to this handler (e.g. "/user/{userID}")
     * @param handler
     *     The raw unit of work to invoke when the server receives a request matching the path
     *     pattern. It should return a raw value representing what you want to send back in the
     *     HTTP response. Don't worry about status codes or serializing JSON because the ApiGateway
     *     will automatically take care of that for you.
     */
    public ApiGateway PUT(String pathPattern, Function<Context, ?> handler) {
        javalin.put(pathPattern, respond(handler));
        return this;
    }

    /**
     * Registers an HTTP POST route.
     *
     * @param pathPattern The HTTP path that will route requests to this handler (e.g. "/user/{userID}")
     * @param handler
     *     The raw unit of work to invoke when the server receives a request matching the path
     *     pattern. It should return a raw value representing what you want to send back in the
     *     HTTP response. Don't worry about status codes or serializing JSON because the ApiGateway
     *     will automatically take care of that for you.
     */
    public ApiGateway POST(String pathPattern, Function<Context, ?> handler) {
        javalin.post(pathPattern, respond(handler));
        return this;
    }

    /**
     * Registers an HTTP DELETE route.
     *
     * @param pathPattern The HTTP path that will route requests to this handler (e.g. "/user/{userID}")
     * @param handler
     *     The raw unit of work to invoke when the server receives a request matching the path
     *     pattern. It should return a raw value representing what you want to send back in the
     *     HTTP response. Don't worry about status codes or serializing JSON because the ApiGateway
     *     will automatically take care of that for you.
     */
    public ApiGateway DELETE(String pathPattern, Function<Context, ?> handler) {
        javalin.delete(pathPattern, respond(handler));
        return this;
    }

    /**
     * Decorates the given handler with our standard request handling functionality. It invokes the
     * handler, serializes the result to JSON, and applies that to the underlying Javalin context. This
     * also catches exceptions and responds in a standard fashion.
     *
     * @param handler The route handler to decorate
     * @return A ready-to-rock handler that can be registered with Javalin.
     */
    private Handler respond(Function<Context, ?> handler) {
        return ctx -> {
            try {
                logger.info("%s %s", ctx.method(), ctx.path());
                var result = handler.apply(ctx);
                ctx.result(marshalJSON(result));
            }
            catch (SecurityException e) {
                respondError(ctx, 403, e.getMessage());
            }
            catch (NoSuchElementException e) {
                respondError(ctx, 404, e.getMessage());
            }
            catch (Exception e) {
                respondError(ctx, 500, e.getMessage());
            }
        };
    }

    private void respondError(Context ctx, int status, String message) {
        final var errorJSON = "{\"status\": %d, \"message\": \"%s\"}";
        ctx.status(status).result(format(errorJSON, status, message));
    }

    @Override
    public void close() {
        javalin.stop();
    }
}
