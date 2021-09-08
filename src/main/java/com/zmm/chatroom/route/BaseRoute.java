package com.zmm.chatroom.route;

import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.ext.web.Router;
import org.apache.ignite.Ignite;

/**
 * @author zmm
 * @date 2021/7/21 13:37
 */
public abstract class BaseRoute {

    /**
     * 本地数据：
     *     user:  用来记录用户信息
     *     * LocalMap<String, JsonObject>
     *     * key: username              value: 用户基本信息，不包括用户名
     *     room:  用来记录房间信息
     *     * LocalMap<String, Object>
     *     * key: id                    value: 房间信息。房间id、房间名、时间戳(用来保证存储在LevelDB中不会乱序)
     *     * 特殊：(key: allRoom         value: 所有房间信息列表JsonArray)
     *     msg:   用来记录历史消息
     *     * LocalMap<String, JsonArray>
     *     * key: id(房间id)             value: 历史消息列表
     *     userCache:  用来将用户与房间号对应
     *     *
     *     *
     *     usersCache: 用来将记录房间中的用户列表
     *     *
     */

    public static Router mainRouter;

    public static EventBus event;

    public static Ignite ignite;

    public static Vertx vertx;
}
