package momo.demo.verticle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.rabbitmq.RabbitMQClient;
import io.vertx.rabbitmq.RabbitMQOptions;
import momo.demo.constant.MomoConstant;

public class TestEventBusVerticle extends AbstractVerticle {
    private final Logger LOGGER = LoggerFactory.getLogger(TestEventBusVerticle.class);

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
                    .onComplete(promise);
        });

        this.createConnectRabbitMQWithUri()
                .onSuccess(h1 -> {
                    Buffer message = Buffer.buffer("Test message !!!");
                    rabbitMQClient.basicPublish("",
                            MomoConstant.REQUEST_ROUTING_KEY,
                            message, ar -> {
                                if (ar.succeeded()) {
                                    LOGGER.info("Push success ");
                                }
                                else {
                                    LOGGER.info("Push failed");
                                }
                            });
                });
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
                LOGGER.info("Producer: Fail to connect to RabbitMQ "
                        + asyncResult.cause().getMessage());
                createConnectPromise.fail(asyncResult.cause());
            }
        });

        return createConnectPromise.future();
    }

    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();
        vertx.deployVerticle(new TestEventBusVerticle());
    }

}
