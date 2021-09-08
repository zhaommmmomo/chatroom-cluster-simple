package com.zmm.chatroom;

import com.zmm.chatroom.shareable.CustomShareable;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Launcher;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.file.FileSystem;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.shareddata.LocalMap;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import org.apache.ignite.Ignite;
import org.apache.ignite.Ignition;
import org.apache.ignite.cache.CacheMode;
import org.apache.ignite.cache.CacheWriteSynchronizationMode;
import org.apache.ignite.cluster.ClusterState;
import org.apache.ignite.configuration.*;
import org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi;
import org.apache.ignite.spi.discovery.tcp.ipfinder.vm.TcpDiscoveryVmIpFinder;

import javax.cache.expiry.CreatedExpiryPolicy;
import javax.cache.expiry.Duration;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Arrays;
import java.util.Enumeration;

/**
 * 启动类
 * @author zmm
 * @date 2021/7/22 14:32
 */
public class Application extends Launcher {

    private volatile static JsonArray ip = new JsonArray();

    private static Ignite ignite;

    public static void main(String[] args) {
        new Application().dispatch(args);
    }

    @Override
    public void beforeStartingVertx(VertxOptions options) {

        Vertx vertx = Vertx.vertx();
        preStart(vertx);

        while (ip.size() == 0){

        }

        System.out.println("vertx关闭...");

        TcpDiscoverySpi spi = new TcpDiscoverySpi();

        TcpDiscoveryVmIpFinder ipFinder = new TcpDiscoveryVmIpFinder();
        ipFinder.setAddresses(Arrays.asList(ip.getString(0),
                                            ip.getString(1),
                                            ip.getString(2)));

        spi.setIpFinder(ipFinder);

        // 持久化
        DataStorageConfiguration storageCfg = new DataStorageConfiguration();
        storageCfg.setWalMode(WALMode.BACKGROUND);
        storageCfg.setWalSegmentSize(256 * 1024 * 1024);
        storageCfg.setDefaultWarmUpConfiguration(new LoadAllWarmUpConfiguration());
        storageCfg.setPageSize(4096);

        DataRegionConfiguration defaultData = storageCfg.getDefaultDataRegionConfiguration();
        defaultData.setPersistenceEnabled(true)
                   .setMaxSize((long) (3.5 * 1024 * 1024 * 1024));

        DataRegionConfiguration cache = new DataRegionConfiguration();
        cache.setPersistenceEnabled(false);
        cache.setName("cache");
        cache.setMaxSize(1024 * 1024 * 1024);
        storageCfg.setDataRegionConfigurations(cache);

        CacheConfiguration<Object, Object> userCache = new CacheConfiguration<>();
        userCache.setName("userCache");
        //userCache.setBackups(2);
        userCache.setDataRegionName("cache");
        userCache.setCacheMode(CacheMode.PARTITIONED);
        userCache.setExpiryPolicyFactory(CreatedExpiryPolicy.factoryOf(Duration.ETERNAL));
        userCache.setWriteSynchronizationMode(CacheWriteSynchronizationMode.FULL_SYNC);

        CacheConfiguration<Object, Object> usersCache = new CacheConfiguration<>();
        usersCache.setName("usersCache");
        //userCache.setBackups(2);
        usersCache.setDataRegionName("cache");
        usersCache.setCacheMode(CacheMode.PARTITIONED);
        usersCache.setExpiryPolicyFactory(CreatedExpiryPolicy.factoryOf(Duration.ETERNAL));
        usersCache.setWriteSynchronizationMode(CacheWriteSynchronizationMode.FULL_SYNC);

        CacheConfiguration<String, Object> user = new CacheConfiguration<>();
        user.setName("user");
        //user.setBackups(2);
        user.setDataRegionName("default");
        user.setCacheMode(CacheMode.PARTITIONED);
        user.setExpiryPolicyFactory(CreatedExpiryPolicy.factoryOf(Duration.ETERNAL));
        user.setWriteSynchronizationMode(CacheWriteSynchronizationMode.FULL_SYNC);


        CacheConfiguration<String, Object> room = new CacheConfiguration<>();
        room.setName("room");
        //room.setBackups(2);
        room.setDataRegionName("default");
        room.setCacheMode(CacheMode.PARTITIONED);
        room.setExpiryPolicyFactory(CreatedExpiryPolicy.factoryOf(Duration.ETERNAL));
        room.setWriteSynchronizationMode(CacheWriteSynchronizationMode.FULL_SYNC);

        CacheConfiguration<String, Object> msg = new CacheConfiguration<>();
        msg.setName("msg");
        //msg.setBackups(2);
        msg.setDataRegionName("default");
        msg.setCacheMode(CacheMode.PARTITIONED);
        msg.setExpiryPolicyFactory(CreatedExpiryPolicy.factoryOf(Duration.ETERNAL));
        msg.setWriteSynchronizationMode(CacheWriteSynchronizationMode.FULL_SYNC);

        IgniteConfiguration cfg = new IgniteConfiguration();
        cfg.setDiscoverySpi(spi).setDataStorageConfiguration(storageCfg);
        cfg.setClusterStateOnStart(ClusterState.ACTIVE);

        cfg.setCacheConfiguration(userCache, usersCache, user, room, msg);

        ignite = Ignition.start(cfg);
        ignite.active(true);
        ignite.cluster().setBaselineTopology(ignite.cluster().forServers().nodes());


        //ignite.cluster().baselineAutoAdjustEnabled(true);
        //ignite.cluster().baselineAutoAdjustTimeout(30000);

        //options.setClusterManager(new IgniteClusterManager(ignite)).setEventLoopPoolSize(8);

        options.setEventLoopPoolSize(8).setWorkerPoolSize(350);
    }

    @Override
    public void beforeDeployingVerticle(DeploymentOptions deploymentOptions) {
        // 设置部署多少个Verticle实例
        deploymentOptions.setInstances(100);
        //deploymentOptions.setInstances(1);
    }

    @Override
    public void afterStartingVertx(Vertx vertx) {

        LocalMap<Object, Object> data = vertx.sharedData().getLocalMap("ip");
        data.put("ip", Application.ip);
        data.put("ignite", new CustomShareable<>(ignite));
    }

    private void preStart(Vertx vertx){

        FileSystem fileSystem = vertx.fileSystem();
        String path = "/root/ip.json";
        if (fileSystem.existsBlocking(path)){
            ip = vertx.fileSystem().readFileBlocking(path).toJsonArray();
            return;
        }

        HttpServer httpServer = vertx.createHttpServer();

        Router router = Router.router(vertx);
        router.route().handler(BodyHandler.create());
        router.post("/updateCluster")
              .handler(http -> {
                  JsonArray ips = http.getBodyAsJsonArray();
                  if (ips.size() != 3){
                      http.response().setStatusCode(400).end();
                  }else{
                      try {
                          for (Enumeration ifaces = NetworkInterface.getNetworkInterfaces(); ifaces.hasMoreElements();) {
                              NetworkInterface iface = (NetworkInterface) ifaces.nextElement();
                              // 在所有的接口下再遍历IP
                              for (Enumeration inetAddrs = iface.getInetAddresses(); inetAddrs.hasMoreElements(); ) {
                                  InetAddress inetAddr = (InetAddress) inetAddrs.nextElement();
                                  String str = inetAddr.toString().substring(1);
                                  if (ips.contains(str)) {
                                      // 如果是本机ip
                                      ips.remove(str);
                                      ips.add(0, str);

                                      // 保存ip
                                      vertx.fileSystem().writeFile(path, ips.toBuffer());
                                      break;
                                  }
                              }
                          }
                      }catch (SocketException e) {
                          e.printStackTrace();
                      }
                      ip = ips;
                      http.response().setStatusCode(200).end();
                      httpServer.close();
                      vertx.close();
                  }
              })
              .failureHandler(resp -> resp.response().setStatusCode(200).end());

        router.get("/checkCluster")
              .handler(http -> http.response().setStatusCode(400).end())
              .failureHandler(resp -> resp.response().setStatusCode(400).end());

        httpServer.requestHandler(router)
                  .listen(8080, ok -> {
                      System.out.println("监听ip...");
                  });
    }
}
