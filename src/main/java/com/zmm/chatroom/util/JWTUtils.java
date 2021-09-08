package com.zmm.chatroom.util;

import io.vertx.core.Vertx;
import io.vertx.ext.auth.JWTOptions;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.JWTAuthOptions;

/**
 * @author zmm
 * @date 2021/7/21 13:02
 */
public class JWTUtils {

    public static JWTAuth jwt;

    public static void init(Vertx vertx){
        JWTOptions jwtOptions = new JWTOptions().setExpiresInMinutes(60);
        JWTAuthOptions authConfig = new JWTAuthOptions().setJWTOptions(jwtOptions);
        jwt = JWTAuth.create(vertx , authConfig);
    }

}
