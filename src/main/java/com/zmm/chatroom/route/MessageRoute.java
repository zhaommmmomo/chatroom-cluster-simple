package com.zmm.chatroom.route;

import com.zmm.chatroom.util.JWTUtils;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.handler.JWTAuthHandler;
import org.apache.ignite.IgniteCache;

/**
 * @author zmm
 * @date 2021/7/21 9:47
 */
public class MessageRoute extends BaseRoute{

    public static void init() {
        sendMessage();
        retrieve();
    }

    public static void sendMessage(){
        mainRouter.post("/message/send")
                  .consumes("application/json")
                  .handler(JWTAuthHandler.create(JWTUtils.jwt))
                  .handler(http -> {
                      JsonObject message = http.getBodyAsJson();
                      String id = message.getString("id");
                      String text = message.getString("text");
                      if (id == null || text == null){
                          http.response().setStatusCode(400).end();
                      }else{
                          String username = http.user().principal().getString("username");

                          IgniteCache<String, String> userCache = ignite.cache("userCache");
                          vertx.executeBlocking(block -> {
                              String roomId = userCache.get(username);
                              if (roomId != null){
                                  String timestamp = "" + System.currentTimeMillis();
                                  message.put("timestamp", timestamp);

                                  IgniteCache<String, JsonArray> allMsg = ignite.cache("msg");
                                  JsonArray msg = allMsg.get(roomId);
                                  if (msg == null){
                                      msg = new JsonArray();
                                  }
                                  msg.add(0, message);
                                  allMsg.put(roomId, msg);
                                  http.response().setStatusCode(200).end();
                              }else{
                                  http.response().setStatusCode(400).end();
                              }
                          });
                      }
                  })
                  .failureHandler(resp -> {
                      resp.response().setStatusCode(400).end();
                  });
    }

    public static void retrieve(){
        mainRouter.post("/message/retrieve")
                  .produces("application/json")
                  .consumes("application/json")
                  .handler(JWTAuthHandler.create(JWTUtils.jwt))
                  .handler(http -> {
                      JsonObject page = http.getBodyAsJson();
                      Integer index = page.getInteger("pageIndex");
                      Integer size = page.getInteger("pageSize");
                      if (index == null || index >= 0 ||
                            size == null || size <= 0){
                          http.response()
                              .putHeader("content-type","application/json")
                              .setStatusCode(400).end();
                      }else{
                          String username = http.user().principal().getString("username");

                          IgniteCache<String, String> userCache = ignite.cache("userCache");
                          vertx.executeBlocking(block -> {
                              String roomId = userCache.get(username);
                              if (roomId == null){
                                  http.response()
                                          .putHeader("content-type","application/json")
                                          .setStatusCode(400).end();
                              }else{

                                  IgniteCache<String, JsonArray> allMsg = ignite.cache("msg");
                                  JsonArray msg = allMsg.get(roomId);
                                  if (msg == null){
                                      http.response()
                                              .putHeader("content-type","application/json")
                                              .setStatusCode(200)
                                              .end(new JsonArray().encode());
                                  }else{
                                      int i = -index - 1;
                                      int len = Math.min(size + i, msg.size());
                                      http.response()
                                              .putHeader("content-type","application/json")
                                              .setStatusCode(200)
                                              .end(new JsonArray(msg.getList().subList(i, len)).encode());
                                  }
                              }
                          });
                      }
                  })
                  .failureHandler(resp -> {
                      resp.response()
                          .putHeader("content-type","application/json")
                          .setStatusCode(400).end();
                  });
    }
}
