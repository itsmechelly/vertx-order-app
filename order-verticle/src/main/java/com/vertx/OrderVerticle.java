package com.vertx;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.Message;
import io.vertx.core.file.FileSystem;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.logging.Logger;

public class OrderVerticle extends AbstractVerticle {

    private static final Logger log = Logger.getLogger(OrderVerticle.class.getPackageName());

    public static final String JSON_ORDERS_FILE = "orders.json";
    public static final String ORDER_VERTICAL_SERVICE = "order-verticle-service";

    @Override
    public void start(Promise<Void> promise) {
        log.info("orderVerticle.start: going to startPromise from " + ORDER_VERTICAL_SERVICE);
        vertx.eventBus().consumer("addOrder", message -> {
            JsonObject jsonObject = (JsonObject) message.body();
            addOrder(message, jsonObject.getString("orderID"), jsonObject.getString("orderName"), jsonObject.getString("orderDate"));
        });
        vertx.eventBus().consumer("getOrders", this::getOrders);
        promise.complete();
    }

    private void addOrder(Message<Object> message, String orderId, String orderName, String orderDate) {
        log.info("orderVerticle.addOrder: going to answer eventBus consumer, adding new order to= " + JSON_ORDERS_FILE);
        FileSystem fileSystem = vertx.fileSystem();

        log.info("orderVerticle.addOrder: going to read existing data from= " + JSON_ORDERS_FILE);
        fileSystem.readFile(JSON_ORDERS_FILE, res -> {
            if (res.succeeded()) {
                Buffer bufferReader = res.result();
                JsonObject newOrder = new JsonObject();
                JsonArray jsonArray = bufferReader.toJsonArray();

                newOrder.put("orderId", orderId);
                newOrder.put("orderName", orderName);
                newOrder.put("orderDate", orderDate);
                jsonArray.add(newOrder);

                log.info("orderVerticle.addOrder: going to write new order data from= " + JSON_ORDERS_FILE);
                Buffer bufferWriter = jsonArray.toBuffer();
                vertx.fileSystem().writeFile(JSON_ORDERS_FILE, bufferWriter, asyncResult -> {
                    if (asyncResult.succeeded()) {
                        log.info("orderVerticle.addOrder: write new order - done");
                        messageResponse(message, "false", "order data succeed");
                    } else {
                        log.info("orderVerticle.addOrder: write new order - failed");
                        messageResponse(message, "true", "something went wrong while writing to fileSystem");
                    }
                });
            } else {
                messageResponse(message, "true", "something went wrong while reading from fileSystem");
            }
        });
    }

    private void getOrders(Message<Object> message) {
        log.info("orderVerticle.getOrders: going to answer eventBus consumer");

        FileSystem fileSystem = vertx.fileSystem();
        fileSystem.readFile(JSON_ORDERS_FILE, res -> {
            if (res.succeeded()) {
                Buffer buffer = res.result();
                JsonArray jsonArray = buffer.toJsonArray();
                message.reply(jsonArray);
            } else {
                message.reply("order list not found or empty");
            }
        });
    }

    private void messageResponse(Message<Object> message, String errorValue, String insertValue) {
        JsonObject jsonResult = new JsonObject();
        jsonResult.put("error", errorValue);
        jsonResult.put("insert", insertValue);
        message.reply(jsonResult);
    }
}
