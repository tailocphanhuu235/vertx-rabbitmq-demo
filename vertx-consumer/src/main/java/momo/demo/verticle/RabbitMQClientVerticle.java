package momo.demo.verticle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;
import io.vertx.rabbitmq.QueueOptions;
import io.vertx.rabbitmq.RabbitMQClient;
import io.vertx.rabbitmq.RabbitMQConsumer;
import io.vertx.rabbitmq.RabbitMQOptions;
import momo.demo.constant.MomoConstant;

public class RabbitMQClientVerticle extends AbstractVerticle {
    private final Logger LOGGER = LoggerFactory.getLogger(RabbitMQClientVerticle.class);

    private RabbitMQClient rabbitMQClient;

    @Override
    public void start(Promise<Void> startPromise) throws Exception {
        LOGGER.info("Consumer: start()");
        RabbitMQOptions config = new RabbitMQOptions();
//        config.setUri(MomoConstant.RABBIT_URI);
        config.setUser("momo");
        config.setPassword("momo");
        config.setHost("localhost");
        config.setPort(5672);
        config.setVirtualHost("vhost1");
        config.setConnectionTimeout(6000); // in milliseconds
        config.setRequestedHeartbeat(60); // in seconds
        config.setHandshakeTimeout(6000); // in milliseconds
        config.setRequestedChannelMax(5);
        config.setNetworkRecoveryInterval(500); // in milliseconds
        config.setAutomaticRecoveryEnabled(true);

        this.rabbitMQClient = RabbitMQClient.create(vertx, config);

        this.createConnectRabbitMQWithUri()
                .onSuccess(h1 -> {
                    LOGGER.info("Consumer: onSuccess: RabbitMQ already!");
                    this.consumeResponseEventBus();
                    this.consumeRequestQueue();
                })
                .onFailure(Throwable::printStackTrace);

    }

    private Future<Void> createConnectRabbitMQWithUri() {
        LOGGER.info("Consumer: createConnectRabbitMQWithUri()");
        Promise<Void> createConnectPromise = Promise.promise();

        this.rabbitMQClient.start(asyncResult -> {
            if (asyncResult.succeeded()) {
                LOGGER.info("Consumer: RabbitMQ successfully connected!");
                createConnectPromise.complete();
            }
            else {
                LOGGER.info("Consumer: Fail to connect to RabbitMQ "
                        + asyncResult.cause().getMessage());
                createConnectPromise.fail(asyncResult.cause());
            }
        });

        return createConnectPromise.future();
    }

    private Future<Void> consumeRequestQueue() {
        LOGGER.info("Consumer: consumeRequestQueue()");
        Promise<Void> consumeRequestQueue = Promise.promise();
        rabbitMQClient.basicConsumer(MomoConstant.REQUEST_QUEUE,
                new QueueOptions().setAutoAck(true), consumerResult -> {
                    if (consumerResult.succeeded()) {
                        LOGGER.info("Consumer: RabbitMQ consumer MomoConstant.REQUEST_QUEUE created !");
                        RabbitMQConsumer consumer = consumerResult.result();

                        consumer.handler(msg -> {
                            String json = msg.body().toString();
                            LOGGER.info("Consumer: Got response message from MomoConstant.REQUEST_QUEUE: " + json);
                            vertx.eventBus().request(MomoConstant.REQUEST_EVENT_BUS, json,
                                    result -> {
                                        LOGGER.info("Consumer: Send to request event bus: " + json);
                                        if (result.succeeded()) {
                                            consumeRequestQueue.tryComplete();
                                        }
                                        else {
                                            result.cause().printStackTrace();
                                            consumeRequestQueue.tryFail(result.cause());
                                        }
                                    });
                        });
                    }
                    else {
                        consumerResult.cause().printStackTrace();
                        consumeRequestQueue.fail(consumerResult.cause());
                    }
                });

        return consumeRequestQueue.future();
    }

    private Future<Void> consumeResponseEventBus() {
        LOGGER.info("Consumer: consumeResponseEventBus()");
        Promise<Void> consumeRequestEventBusPromise = Promise.promise();
        vertx.eventBus().consumer(MomoConstant.RESPONSE_EVENT_BUS, msg -> {
            LOGGER.info("Consumer: RESPONSE_EVENT_BUS is received message : " + msg.address() + ": "
                    + msg.body());
            Buffer message = Buffer.buffer((String) msg.body());
            rabbitMQClient.basicPublish(MomoConstant.TAXI_EXCHANGE, MomoConstant.RESPONSE_ROUTING_KEY,
                    message, pubResult -> {
                        if (pubResult.succeeded()) {
                            LOGGER.info("Consumer: Message published to RESPONSE_QUEUE !");
                            consumeRequestEventBusPromise.complete();
                        }
                        else {
                            LOGGER.info("Consumer: Message publish to RESPONSE_QUEUE failed !");
                            pubResult.cause().printStackTrace();
                            consumeRequestEventBusPromise.fail(pubResult.cause());
                        }
                    });
            msg.reply("Consumer: RESPONSE_EVENT_BUS is received message");
        });

        return consumeRequestEventBusPromise.future();
    }

}
