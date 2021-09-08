package com.zmm.chatroom;

import com.zmm.chatroom.route.BaseRoute;
import com.zmm.chatroom.route.MessageRoute;
import com.zmm.chatroom.route.RoomRoute;
import com.zmm.chatroom.route.UserRoute;
import com.zmm.chatroom.shareable.CustomShareable;
import com.zmm.chatroom.util.JWTUtils;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import org.apache.ignite.Ignite;


/**
 * @author zmm
 * @date 2021/7/21 9:15
 */
public class MainVerticle extends AbstractVerticle {

    @SuppressWarnings("unchecked")
    public void myServerInit(){
        BaseRoute.mainRouter = Router.router(vertx);

        JWTUtils.init(vertx);
        BaseRoute.event = vertx.eventBus();
        BaseRoute.ignite = ((CustomShareable<Ignite>) vertx.sharedData().getLocalMap("ip").get("ignite")).getData();
        BaseRoute.vertx = vertx;
        BaseRoute.mainRouter.route().handler(BodyHandler.create());
        BaseRoute.mainRouter.get("/checkCluster")
                            .handler(http -> http.response().setStatusCode(200).end())
                            .failureHandler(resp -> resp.response().setStatusCode(400).end());
        UserRoute.init();
        RoomRoute.init();
        MessageRoute.init();
    }

    @Override
    public void start(Promise<Void> startPromise) {
        this.myServerInit();
        this.vertx.createHttpServer(new HttpServerOptions().setIdleTimeout(5))
             .requestHandler(BaseRoute.mainRouter)
             .listen(8080)
             .onSuccess(msg -> System.out.println("start server 8080!"));
    }
}
