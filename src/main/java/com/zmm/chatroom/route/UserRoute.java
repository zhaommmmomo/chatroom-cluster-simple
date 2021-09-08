package com.zmm.chatroom.route;

import com.zmm.chatroom.util.JWTUtils;
import io.vertx.core.json.JsonObject;
import org.apache.ignite.IgniteCache;

/**
 * @author zmm
 * @date 2021/7/21 9:29
 */
public class UserRoute extends BaseRoute{

    public static void init() {
        createUser();
        userLogin();
        findUser();
    }

    public static void createUser() {
        mainRouter.post("/user")
                  .consumes("application/json")
                  .handler(http -> {
                      JsonObject json = http.getBodyAsJson();
                      if (json != null){
                          String username = json.getString("username");
                          String password = json.getString("password");
                          if (username != null && username.length() != 0
                                  && password != null && password.length() != 0) {
                              json.remove("username");
                              vertx.executeBlocking(block -> {
                                  ignite.cache("user").put(username, json);
                                  http.response().setStatusCode(200).end();
                              });
                          }else{
                              http.response().setStatusCode(200).end();
                          }
                      }else{
                          http.response().setStatusCode(200).end();
                      }
                  })
                  .failureHandler(resp -> resp.response().setStatusCode(200).end());
    }

    public static void userLogin(){
        mainRouter.get("/userLogin")
                  .handler(http -> {
                      String username = http.request().getParam("username");
                      String password = http.request().getParam("password");
                      if (username == null || password == null){
                          http.response().setStatusCode(400).end();
                      }else{
                          vertx.executeBlocking(block -> {
                              IgniteCache<String, JsonObject> user = ignite.cache("user");
                              JsonObject userInfo = user.get(username);
                              if (userInfo == null || !userInfo.getString("password").equals(password)){
                                  http.response().setStatusCode(400).end();
                              }else{
                                  http.response()
                                          .setStatusCode(200)
                                          .end(JWTUtils.jwt.generateToken(new JsonObject().put("username", username)));
                              }
                          });
                      }
                  })
                  .failureHandler(res -> res.response().setStatusCode(400).end());
    }

    public static void findUser(){
        mainRouter.get("/user/:username")
                  .produces("application/json")
                  .handler(http -> {
                      String username = http.request().getParam("username");
                      if (username == null){
                          http.response()
                              .putHeader("content-type","application/json")
                              .setStatusCode(400).end();
                      }else{
                          IgniteCache<String, JsonObject> user = ignite.cache("user");
                          vertx.executeBlocking(block -> {
                              JsonObject userInfo = user.get(username);
                              if (userInfo == null){
                                  http.response()
                                          .putHeader("content-type","application/json")
                                          .setStatusCode(400).end();
                              }else{
                                  userInfo.remove("password");
                                  http.response()
                                          .putHeader("content-type","application/json")
                                          .setStatusCode(200)
                                          .end(userInfo.encode());
                              }
                          });
                      }
                  })
                  .failureHandler(http -> http.response()
                                              .putHeader("content-type","application/json")
                                              .setStatusCode(400).end());
    }
}
