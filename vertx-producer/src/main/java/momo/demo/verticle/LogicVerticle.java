package momo.demo.verticle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import momo.demo.constant.MomoConstant;

public class LogicVerticle extends AbstractVerticle {
  private final Logger LOGGER = LoggerFactory.getLogger(LogicVerticle.class);

  @Override
  public void start(Promise<Void> startPromise) {
    Router router = Router.router(vertx);
    router.post("/orderTaxi").consumes("*/json").handler(BodyHandler.create())
        .handler(this::orderTaxi);

    vertx.createHttpServer().requestHandler(router).listen(8080).onSuccess(
        rc -> LOGGER.info("Producer: Http server producer is started success on port 8080"))
        .onFailure(rc -> System.out
            .println("Http server producer is started failure on port 8080"));

  }

  /**
   * POST: http://localhost:[port]/orderTaxi
   *
   * @param rc
   */
  private void orderTaxi(RoutingContext rc) {
    String contentJson = rc.getBodyAsString();
    LOGGER.info("Producer: orderTaxi - contentJson: " + contentJson);
    vertx.eventBus().<String>consumer(MomoConstant.RESPONSE_EVENT_BUS,
        msg1 -> {
          rc.response().end("Data return from Consumer: " + msg1.body());
          msg1.reply(
              "Producer: onSuccess: orderTaxi - Request Order Taxi Successs");
        });

    vertx.eventBus().<String>request(MomoConstant.REQUEST_EVENT_BUS, contentJson)
        .onSuccess(s1 -> {
          LOGGER.info("Producer: onSuccess: orderTaxi - Request Order Taxi Success");
        })
        .onFailure(f -> {
          LOGGER.info("Producer: onSuccess: orderTaxi - Request Order Taxi Failed");
          f.printStackTrace();
          rc.response().end(f.getMessage());
        })
        .onComplete(c1 -> {
          LOGGER.info("Producer: onComplete: orderTaxi - Request Order Taxi Complete");
        });

  }
}
