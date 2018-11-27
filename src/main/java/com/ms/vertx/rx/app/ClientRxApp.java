package com.ms.vertx.rx.app;


import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.core.buffer.Buffer;
import io.vertx.reactivex.core.http.HttpClient;
import io.vertx.reactivex.core.http.HttpClientRequest;

import static io.reactivex.internal.operators.observable.ObservableBlockingSubscribe.subscribe;

public class ClientRxApp extends AbstractVerticle {

    public static void main (String[] args) {
        String action = args.length > 0 ? args[0].toLowerCase() : "getall";
        String pname = args.length > 1 ? args[1] : "";
        String price = args.length > 2 ? args[2] : "";

        Vertx vertx = Vertx.vertx();
        vertx.deployVerticle(ClientRxApp.class.getName(), new DeploymentOptions().setConfig(new JsonObject().put("action", action).put("name", pname).put("price",price)),
            res -> {
            System.exit(0);
            });
    }

    @Override
    public void start(Future<Void> startFuture){

        HttpClient httpClient = vertx.createHttpClient();
        JsonObject params = config();
        String action = params.getString("action");
        HttpClientRequest req;
        Buffer statusCode = Buffer.buffer();
        String pjson = String.format("{ \"name\" : \"%s\", \"price\" : %s}", params.getString("name"), params.getString("price"));
        if (action.equals("add")) {
            req = httpClient.post(8080, "localhost", "/addproduct");
        }
        else  if (action.equals("fetchone"))
                req = httpClient.get(8080, "localhost", "/getproductbyid/" + params.getString("name"));
        else
                req = httpClient.get(8080, "localhost", "/showproducts");
        req
            .toFlowable()
            .flatMap(httpClientResponse -> { statusCode.appendInt(httpClientResponse.statusCode()); return httpClientResponse.toFlowable();})
            .reduce(Buffer::appendBuffer)
            .subscribe(data -> { System.out.printf("Status Code: %d\nData: %s\n",statusCode.getInt(0),data.toString()); startFuture.complete();});

        if (action.equals("add"))  {
            req
                .putHeader("Content-Length", String.valueOf(pjson.length()))
                .write(pjson);
        }

        req.end();


    }

}
