package com.example.starter;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.ext.healthchecks.HealthCheckHandler;
import io.vertx.ext.healthchecks.Status;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.ErrorHandler;
import io.vertx.mssqlclient.MSSQLConnectOptions;
import io.vertx.mssqlclient.MSSQLPool;
import io.vertx.sqlclient.PoolOptions;

public class MainVerticle extends AbstractVerticle {

  private static final String HOST = System.getenv().getOrDefault("HOST", "localhost");
  private static final Integer PORT = Integer.valueOf(System.getenv().getOrDefault("PORT", "1433"));
  private static final String DB = System.getenv().getOrDefault("DB", "vertx");
  private static final String USER = System.getenv().getOrDefault("USER", "vertx");
  private static final String PASSWORD = System.getenv().getOrDefault("PASSWORD", "vertx");

  @Override
  public void start(Promise<Void> startPromise) throws Exception {

    MSSQLConnectOptions connectOptions = new MSSQLConnectOptions()
      .setHost(HOST)
      .setPort(PORT)
      .setDatabase(DB)
      .setUser(USER)
      .setPassword(PASSWORD);

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

    router.get("/test").respond(rc -> client.query("SELECT 1 ").execute().map("OK"));

    HealthCheckHandler healthCheckHandler = HealthCheckHandler.create(vertx);
    healthCheckHandler.register("my-procedure-name", promise -> promise.complete(Status.OK()));
    router.get("/health*").handler(healthCheckHandler);

    router.route().failureHandler(ErrorHandler.create(vertx, true));


    vertx.createHttpServer().requestHandler(router)
      .listen(8080)
      .<Void>mapEmpty()
      .onComplete(startPromise);
  }
}
