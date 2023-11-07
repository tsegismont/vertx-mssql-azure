package com.example.starter;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.healthchecks.HealthCheckHandler;
import io.vertx.ext.healthchecks.Status;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.ErrorHandler;
import io.vertx.mssqlclient.MSSQLConnectOptions;
import io.vertx.mssqlclient.MSSQLPool;
import io.vertx.sqlclient.PoolOptions;

public class MainVerticle extends AbstractVerticle {

  private static final int HTTP_PORT = Integer.parseInt(System.getenv().getOrDefault("HTTP_PORT", "8080"));
  private static final String DB_HOST = System.getenv().getOrDefault("DB_HOST", "localhost");
  private static final int DB_PORT = Integer.parseInt(System.getenv().getOrDefault("DB_PORT", "1433"));
  private static final String DB_NAME = System.getenv().getOrDefault("DB_NAME", "vertx");
  private static final String DB_USER = System.getenv().getOrDefault("DB_USER", "vertx");
  private static final String DB_PASSWORD = System.getenv().getOrDefault("DB_PASSWORD", "vertx");

  @Override
  public void start(Promise<Void> startPromise) throws Exception {

    MSSQLConnectOptions connectOptions = new MSSQLConnectOptions()
      .setHost(DB_HOST)
      .setPort(DB_PORT)
      .setDatabase(DB_NAME)
      .setUser(DB_USER)
      .setPassword(DB_PASSWORD);

    PoolOptions poolOptions = new PoolOptions()
      .setMaxSize(5);

    MSSQLPool client = MSSQLPool.pool(vertx, connectOptions, poolOptions);

    Router router = Router.router(vertx);

    router.get("/").respond(rc -> Future.succeededFuture("<!doctype html>\n" +
      "<html lang=\"en\">\n" +
      "  <head>\n" +
      "    <meta charset=\"UTF-8\" />\n" +
      "    <title>Hello World!</title>\n" +
      "  </head>\n" +
      "  <body>Hello World!</body>\n" +
      "</html>\n"));

    router.get("/config").respond(rc -> {
      JsonObject json = connectOptions.toJson();
      json.remove("password");
      return Future.succeededFuture(json);
    });

    router.get("/test").respond(rc -> client.query("SELECT 1 ").execute().map("OK"));

    HealthCheckHandler healthCheckHandler = HealthCheckHandler.create(vertx);
    healthCheckHandler.register("my-procedure-name", promise -> promise.complete(Status.OK()));
    router.get("/health*").handler(healthCheckHandler);

    router.route().failureHandler(ErrorHandler.create(vertx, true));


    vertx.createHttpServer().requestHandler(router)
      .listen(HTTP_PORT)
      .<Void>mapEmpty()
      .onComplete(startPromise);
  }
}
