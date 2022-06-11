package momo.demo.verticle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import momo.demo.constant.MomoConstant;

public class LogicVerticle extends AbstractVerticle {
    private final Logger LOGGER = LoggerFactory.getLogger(LogicVerticle.class);

    @Override
    public void start(Promise<Void> startPromise) {
        LOGGER.info("Consumer: start");
        vertx.eventBus().consumer(MomoConstant.REQUEST_EVENT_BUS, msg -> {
            LOGGER.info("Consumer: REQUEST_EVENT_BUS is received message: " + msg.address() + ": "
                    + msg.body().toString());
            vertx.eventBus().request(MomoConstant.RESPONSE_EVENT_BUS,
                    MomoConstant.STUB_DATA_FROM_CONSUMER,
                    result -> {
                        if (result.succeeded()) {
                            LOGGER.info("Consumer: onSuccess: start - Response event bus Success");
                            startPromise.complete();
                        }
                        else {
                            LOGGER.info("Consumer: onFailure: start - Response event bus Failed");
                            result.cause().printStackTrace();
                            startPromise.fail(result.cause());
                        }
                    });
            msg.reply("Consumer: REQUEST_EVENT_BUS is received message");
        });
    }

}
