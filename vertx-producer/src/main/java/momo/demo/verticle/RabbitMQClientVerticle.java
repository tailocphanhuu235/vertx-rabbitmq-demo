package momo.demo.verticle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;
import io.vertx.rabbitmq.RabbitMQClient;
import io.vertx.rabbitmq.RabbitMQConsumer;
import io.vertx.rabbitmq.RabbitMQOptions;
import momo.demo.constant.MomoConstant;

public class RabbitMQClientVerticle extends AbstractVerticle {
    private final Logger LOGGER = LoggerFactory.getLogger(RabbitMQClientVerticle.class);

    private RabbitMQClient rabbitMQClient;

    @Override
    public void start(Promise<Void> startPromise) throws Exception {
        LOGGER.info("Producer: start()");
        RabbitMQOptions config = new RabbitMQOptions();
        config.setUri(MomoConstant.RABBIT_URI);

        this.rabbitMQClient = RabbitMQClient.create(vertx, config);
        this.rabbitMQClient.addConnectionEstablishedCallback(promise -> {
            this.rabbitMQClient.exchangeDeclare(MomoConstant.TAXI_EXCHANGE,
                    MomoConstant.DIRECT_EXCHANGE_TYPE, true, true).compose(c1 -> {
                        return rabbitMQClient.queueDeclare(MomoConstant.REQUEST_QUEUE, true, false,
                                true);
                    }).compose(c2 -> {
                        return rabbitMQClient.queueBind(c2.getQueue(), MomoConstant.TAXI_EXCHANGE,
                                MomoConstant.REQUEST_ROUTING_KEY);
                    }).compose(c3 -> {
                        return rabbitMQClient.queueDeclare(MomoConstant.RESPONSE_QUEUE, true, false,
                                true);
                    }).compose(c4 -> {
                        return rabbitMQClient.queueBind(c4.getQueue(), MomoConstant.TAXI_EXCHANGE,
                                MomoConstant.RESPONSE_ROUTING_KEY);
                    })
                    // .compose(c5 -> this.consumeRequestEventBus())//TODO
                    // .compose(c6 -> this.consumeRequestQueue())//TODO
                    .onComplete(promise);
        });

        this.createConnectRabbitMQWithUri()
                .onSuccess(h1 -> {
                    LOGGER.info("Producer: onSuccess: RabbitMQ already!");
                    this.consumeRequestEventBus();
                    this.consumeRequestQueue();
                })
                .onFailure(Throwable::printStackTrace);

    }

    private Future<Void> createConnectRabbitMQWithUri() {
        LOGGER.info("Producer: createConnectRabbitMQWithUri()");
        Promise<Void> createConnectPromise = Promise.promise();

        this.rabbitMQClient.start(asyncResult -> {
            if (asyncResult.succeeded()) {
                LOGGER.info("Producer: RabbitMQ successfully connected!");
                createConnectPromise.complete();
            }
            else {
                LOGGER.info("Producer: Fail to connect to RabbitMQ " + asyncResult.cause().getMessage());
                createConnectPromise.fail(asyncResult.cause());
            }
        });

        return createConnectPromise.future();
    }

    private Future<Void> consumeRequestEventBus() {
        LOGGER.info("Producer: consumeRequestEventBus()");
        Promise<Void> consumeRequestEventBusPromise = Promise.promise();
        vertx.eventBus().consumer(MomoConstant.REQUEST_EVENT_BUS, msg -> {
            LOGGER.info("Producer: REQUEST_EVENT_BUS is received message : " + msg.address() + ": "
                    + msg.body());
            Buffer message = Buffer.buffer((String) msg.body());
            rabbitMQClient.basicPublish(MomoConstant.TAXI_EXCHANGE, MomoConstant.REQUEST_QUEUE,
                    message, pubResult -> {
                        if (pubResult.succeeded()) {
                            LOGGER.info("Producer: Message published !");
                            consumeRequestEventBusPromise.tryComplete();
                        }
                        else {
                            LOGGER.info("Producer: Message publish failed !");
                            pubResult.cause().printStackTrace();
                            consumeRequestEventBusPromise.fail(pubResult.cause());
                        }
                    });
            msg.reply("Producer: REQUEST_EVENT_BUS is received message");
        });

        return consumeRequestEventBusPromise.future();
    }

    private Future<Void> consumeRequestQueue() {
        LOGGER.info("Producer: consumeRequestQueue()");
        Promise<Void> consumeRequestQueue = Promise.promise();
        rabbitMQClient.basicConsumer(MomoConstant.RESPONSE_QUEUE, consumerResult -> {
            if (consumerResult.succeeded()) {
                LOGGER.info("Producer: RabbitMQ consumer created !");
                RabbitMQConsumer consumer = consumerResult.result();

                consumer.handler(msg -> {
                    String data = msg.body().toString();
                    LOGGER.info("Producer: Got response message: " + data);
                    vertx.eventBus().request(MomoConstant.RESPONSE_EVENT_BUS, data, result -> {
                        LOGGER.info("Producer: Return client: " + data);
                        if (result.succeeded()) {
                            consumeRequestQueue.tryComplete();
                        }
                        else {
                            consumeRequestQueue.fail(result.cause());
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

}
