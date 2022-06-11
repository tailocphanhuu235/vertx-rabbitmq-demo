package momo.demo;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import momo.demo.verticle.LogicVerticle;
import momo.demo.verticle.RabbitMQClientVerticle;

public class Main {
  public static void main(String[] args) {
    Vertx vertx = Vertx.vertx();

    vertx.deployVerticle(new RabbitMQClientVerticle());
    vertx.deployVerticle(new LogicVerticle());
  }
}
