package com.zmm.chatroom.route;

import com.zmm.chatroom.util.IDUtils;
import com.zmm.chatroom.util.JWTUtils;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.handler.JWTAuthHandler;
import org.apache.ignite.IgniteCache;

/**
 * @author zmm
 * @date 2021/7/21 9:47
 */
public class RoomRoute extends BaseRoute{

    public static void init(){
        createRoom();
        enterRoom();
        roomLeave();
        getRoom();
        getUsersInRoom();
        getRooms();
    }

    public static void createRoom(){
        mainRouter.post("/room")
                  .consumes("application/json")
                  .handler(JWTAuthHandler.create(JWTUtils.jwt))
                  .handler(http -> {
                      JsonObject jsonObject = http.getBodyAsJson();
                      if (jsonObject == null){
                          http.response().setStatusCode(400).end();
                      }else{
                          String name = jsonObject.getString("name");
                          if (name == null || name.length() == 0){
                              http.response().setStatusCode(400).end();
                          }else{
                              String id = "" + IDUtils.nextId();
                              jsonObject.put("id", id);

                              IgniteCache<String, Object> room = ignite.cache("room");
                              vertx.executeBlocking(block -> {
                                  room.putAsync(id, name);
                                  JsonArray rooms = (JsonArray) room.get("allRoom");
                                  if (rooms == null){
                                      rooms = new JsonArray();
                                  }
                                  rooms.add(0, jsonObject);
                                  room.put("allRoom", rooms);
                                  http.response().setStatusCode(200).end(id);
                              });
                          }
                      }
                  })
                  .failureHandler(resp -> resp.response().setStatusCode(400).end());
    }

    public static void enterRoom(){
        mainRouter.put("/room/:roomid/enter")
                  .handler(JWTAuthHandler.create(JWTUtils.jwt))
                  .handler(http -> {
                      String roomId = http.request().getParam("roomid");
                      if (roomId == null){
                          http.response().setStatusCode(400).end();
                      }else{
                          String username = http.user().principal().getString("username");
                          vertx.executeBlocking(block -> {
                              IgniteCache<String, Object> room = ignite.cache("room");
                              Object name = room.get(roomId);
                              if (name == null){
                                  http.response().setStatusCode(400).end();
                              }else{
                                  IgniteCache<String, String> userCache = ignite.cache("userCache");
                                  String oldRoomId = userCache.get(username);
                                  if (!roomId.equals(oldRoomId)){
                                      userCache.putAsync(username, roomId);
                                      IgniteCache<String, JsonArray> usersCache = ignite.cache("usersCache");
                                      if (oldRoomId != null){
                                          JsonArray oldUsers = usersCache.get(oldRoomId);
                                          oldUsers.remove(username);
                                          usersCache.put(oldRoomId, oldUsers);
                                      }
                                      JsonArray users = usersCache.get(roomId);
                                      if (users == null){
                                          users = new JsonArray();
                                      }
                                      users.add(0, username);
                                      usersCache.put(roomId, users);
                                  }
                                  http.response().setStatusCode(200).end();
                              }
                          });
                      }
                  })
                  .failureHandler(resp -> resp.response().setStatusCode(400).end());
    }

    public static void roomLeave(){
        mainRouter.put("/roomLeave")
                  .handler(JWTAuthHandler.create(JWTUtils.jwt))
                  .handler(http -> {
                      String username = http.user().principal().getString("username");

                      IgniteCache<String, String> userCache = ignite.cache("userCache");
                      vertx.executeBlocking(block -> {
                          String roomId = userCache.get(username);
                          if (roomId != null){
                              userCache.removeAsync(username);
                              IgniteCache<String, JsonArray> usersCache = ignite.cache("usersCache");
                              JsonArray users = usersCache.get(roomId);
                              users.remove(username);
                              usersCache.put(roomId, users);
                          }
                          http.response().setStatusCode(200).end();
                      });
                  })
                  .failureHandler(resp -> {
                      resp.response().setStatusCode(400).end();
                  });
    }

    public static void getRoom(){
        mainRouter.get("/room/:roomid")
                  .handler(http -> {
                      String roomId = http.request().getParam("roomid");
                      if (roomId == null){
                          http.response().setStatusCode(400).end();
                      }else{
                          IgniteCache<String, Object> room = ignite.cache("room");
                          vertx.executeBlocking(block -> {
                              String name = (String) room.get(roomId);
                              if (name == null){
                                  http.response().setStatusCode(400).end();
                              }else{
                                  http.response().setStatusCode(200).end(name);
                              }
                          });
                      }
                  })
                  .failureHandler(resp -> resp.response().setStatusCode(400).end());
    }

    public static void getUsersInRoom(){
        mainRouter.get("/room/:roomid/users")
                  .produces("application/json")
                  .handler(http -> {
                      String roomId = http.request().getParam("roomid");
                      if (roomId == null){
                          http.response()
                              .putHeader("content-type","application/json")
                              .setStatusCode(400)
                              .end();
                      }else{
                          IgniteCache<String, JsonArray> usersCache = ignite.cache("usersCache");

                          vertx.executeBlocking(block -> {
                              JsonArray users = usersCache.get(roomId);
                              if (users == null){
                                  if (ignite.cache("room").get(roomId) == null){
                                      http.response()
                                              .putHeader("content-Type", "application/json")
                                              .setStatusCode(400)
                                              .end();
                                  }else{
                                      http.response()
                                              .putHeader("content-Type", "application/json")
                                              .setStatusCode(200)
                                              .end(new JsonArray().encode());
                                  }
                              }else{
                                  http.response()
                                          .putHeader("content-Type", "application/json")
                                          .setStatusCode(200)
                                          .end(users.encode());
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

    public static void getRooms(){
        mainRouter.post("/roomList")
                  .consumes("application/json")
                  .produces("application/json")
                  .handler(http -> {
                      JsonObject page = http.getBodyAsJson();
                      Integer index = page.getInteger("pageIndex");
                      Integer size = page.getInteger("pageSize");
                      if (index == null || index < 0 ||
                            size == null || size <= 0){
                          http.response()
                              .putHeader("content-type","application/json")
                              .setStatusCode(400).end();
                      }else{
                          vertx.executeBlocking(block -> {
                              JsonArray rooms = (JsonArray) ignite.cache("room").get("allRoom");
                              if (rooms == null){
                                  http.response()
                                      .putHeader("content-type","application/json")
                                      .setStatusCode(200)
                                      .end(new JsonArray().encode());
                              }else{
                                  int len = Math.min(index + size, rooms.size());
                                  http.response()
                                      .putHeader("content-type","application/json")
                                      .setStatusCode(200)
                                      .end(new JsonArray(rooms.getList().subList(index, len)).encode());
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
