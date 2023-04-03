package com.vertx;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.FileSystem;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.Session;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.SessionHandler;
import io.vertx.ext.web.sstore.LocalSessionStore;

import java.util.logging.Logger;

public class RestVerticle extends AbstractVerticle {

    private static final Logger log = Logger.getLogger(RestVerticle.class.getPackageName());

    public static final Integer PORT_LISTENER = 8080;
    public static final String JSON_LOGIN_FILE = "users.json";
    public static final String REST_VERTICAL_SERVICE = "rest-verticle-service";

    private static final String ROUTER_GREETING = "/";
    private static final String ROUTER_LOGIN = "/login";
    private static final String ROUTER_LOGOUT = "/logout";
    private static final String ROUTER_ADD_ORDER = "/add-order";
    private static final String ROUTER_GET_ORDERS = "/get-orders";

    public static final Integer HTTP_STATUS_OKAY = 200;
    public static final Integer HTTP_STATUS_NOT_FOUND = 404;
    public static final Integer HTTP_STATUS_BAD_REQUEST = 400;
    public static final Integer HTTP_STATUS_UNAUTHORIZED = 401;
    public static final Integer HTTP_STATUS_INTERNAL_SERVER_ERROR = 500;

    /**
     * This method starts an HTTP server:
     * The method create a Vert.x  HTTP server and then handle every request using the router generated from the createRouter() method (listening to port 8080).
     */
    @Override
    public void start(Promise<Void> startPromise) {
        log.info("restVerticle.start: going to startPromise");
        vertx.createHttpServer()
                .requestHandler(createRouter())
                .listen(PORT_LISTENER, httpInstance -> {
                    if (httpInstance.succeeded()) {
                        startPromise.complete();
                        log.info("HttpServer was created from " + REST_VERTICAL_SERVICE + " and lessening to port " + PORT_LISTENER);
                    } else {
                        startPromise.fail(httpInstance.cause());
                        log.info("HttpServer creation decline " + REST_VERTICAL_SERVICE + " cause: " + httpInstance.cause());
                    }
                });
    }

    /**
     * This method creates a Vert.x Web Router (the object used to route HTTP requests to specific request handlers).
     *
     * @return Vert.x Web Router
     */
    private Router createRouter() {
        log.info("restVerticle.createRouter: going to create all routes handlers");
        final Router router = Router.router(vertx);
        router.route().handler(SessionHandler.create(LocalSessionStore.create(vertx)));
        router.route().handler(BodyHandler.create());

        router.get(ROUTER_GREETING).handler(this::greetingHandler);
        router.post(ROUTER_LOGIN).handler(this::loginHandler);
        router.post(ROUTER_LOGOUT).handler(this::sessionAuth).handler(this::logoutHandler);
        router.post(ROUTER_ADD_ORDER).handler(this::sessionAuth).handler(this::addOrderHandler);
        router.get(ROUTER_GET_ORDERS).handler(this::sessionAuth).handler(this::getOrdersHandler);
        return router;
    }

    /**
     * POST: loginHandler(RoutingContext context) – this method will use the RoutingContext interface to get the username and password from the user, and check if the user can be logged in (username and password will be saved in local JSON file).
     * In the background the module should open a session for each user that logged in.
     */
    private void loginHandler(RoutingContext context) {
        log.info("restVerticle.loginHandler: going to login for session= " + context.session());
        JsonObject jsonFromContext = context.getBodyAsJson();
        FileSystem fileSystem = vertx.fileSystem();

        log.info("restVerticle.loginHandler: going to read from json file");
        fileSystem.readFile(JSON_LOGIN_FILE, asyncResult -> {
            if (asyncResult.succeeded()) {
                Buffer buffer = asyncResult.result();
                JsonArray jsonArrayFromFile = buffer.toJsonArray();

                for (Object json : jsonArrayFromFile) {
                    JsonObject jsonFromFile = (JsonObject) json;
                    log.info("restVerticle.loginHandler: going to check login details: username and password");
                    if (jsonFromFile.getValue("username").equals(jsonFromContext.getValue("username")) && jsonFromFile.getValue("password").equals(jsonFromContext.getValue("password"))) {
                        Session session = context.session();
                        session.put("sessionAuth", true);

                        log.info("restVerticle.loginHandler: login successfully for session= " + context.session());
                        contextResponse(context, "false", "login successfully", HTTP_STATUS_OKAY);
                    } else {
                        log.info("restVerticle.loginHandler: login failed - username or password incorrect for session= " + context.session());
                        contextResponse(context, "username or password incorrect", "false", HTTP_STATUS_UNAUTHORIZED);
                    }
                    return;
                }
            } else {
                log.info("restVerticle.loginHandler: login failed - can't find/ can't read from " + JSON_LOGIN_FILE + " for session= " + context.session());
                contextResponse(context, "something went wrong", "false", HTTP_STATUS_INTERNAL_SERVER_ERROR);
            }
        });
    }

    /**
     * Helper method to check if users session is permitted.
     */
    private void sessionAuth(RoutingContext context) {
        log.info("restVerticle.sessionAuth: going to check authentication for session= " + context.session());
        Session session = context.session();
        Boolean isAuth = session.get("sessionAuth");
        if (isAuth != null && isAuth) {
            context.next();
        } else {
            context.fail(HTTP_STATUS_UNAUTHORIZED);
        }
    }

    /**
     * This method will be used to log out from the user session, his session will be destroyed.
     */
    private void logoutHandler(RoutingContext context) {
        log.info("restVerticle.logoutHandler: going to logout, session= " + context.session());
        Session session = context.session();
        session.put("sessionAuth", false);
        session.destroy();
        contextResponse(context, "false", "true", HTTP_STATUS_OKAY);
    }

    /**
     * This method greeting the user at the main endpoint.
     */
    private void greetingHandler(RoutingContext context) {
        log.info("restVerticle.greetingHandler: request pass with status " + HTTP_STATUS_OKAY);
        context.response()
                .putHeader("content-type", "text/plain")
                .end("Welcome to " + REST_VERTICAL_SERVICE + " let's have some fun!");
    }

    /**
     * This method will add an order to the user.
     * By using Vert.x Event Bus, the order will be sent to the OrderVerticle module.
     */
    private void addOrderHandler(RoutingContext context) {
        JsonObject jsonBody = context.getBodyAsJson();
        log.info("restVerticle.addOrderHandler: going to use eventBus, adding data to: orderVerticle.addOrder");

        vertx.eventBus().request("addOrder", jsonBody, handler -> {
            if (handler.succeeded()) {
                log.info("restVerticle.addOrderHandler: STATUS 200 OKAY - data added to orderVerticle.addOrder");
                context.response()
                        .putHeader("content-type", "application/json")
                        .setStatusCode(HTTP_STATUS_OKAY)
                        .end(Json.encodePrettily(handler.result().body()));
            } else {
                log.info("restVerticle.addOrderHandler: STATUS 400 BAD REQUEST - something went wrong while adding data to orderVerticle.addOrder");
                context.response().setStatusCode(HTTP_STATUS_BAD_REQUEST)
                        .end(handler.cause().getMessage());
            }
        });
    }

    /**
     * This method will return all user orders.
     * The request to get the data will be sent by Vert.x Event Bus to the OrderVerticle module.
     */
    private void getOrdersHandler(RoutingContext context) {
        log.info("restVerticle.getOrdersHandler: going to use eventBus, requesting data from order-vertical-service: orderVerticle.getOrders");

        vertx.eventBus().request("getOrders", null, handler -> {
            if (handler.succeeded()) {
                log.info("restVerticle.getOrdersHandler: STATUS 200 OKAY - data received from orderVerticle.getOrders");
                context.response()
                        .putHeader("content-type", "application/json")
                        .setStatusCode(HTTP_STATUS_OKAY)
                        .end(Json.encodePrettily(handler.result().body()));
            } else {
                log.info("restVerticle.getOrdersHandler: STATUS 404 NOT FOUND - data not found from orderVerticle.getOrders");
                context.response().setStatusCode(HTTP_STATUS_NOT_FOUND)
                        .end(handler.cause().getMessage());
            }
        });
    }

    /**
     * Helper method to print error values in case one of the endpoints collapse, or get runtime error.
     * This method used in other methods exist in this java class, I added it for clean code.
     */
    private void contextResponse(RoutingContext context, String errorValue, String loginValue, Integer httpStatus) {
        JsonObject jsonResult = new JsonObject();
        jsonResult.put("error", errorValue);
        jsonResult.put("login", loginValue);

        context.response()
                .putHeader("content-type", "application/json")
                .setStatusCode(httpStatus)
                .end(Json.encodePrettily(jsonResult));
    }
}
