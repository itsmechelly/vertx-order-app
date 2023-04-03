package com.vertx;

import com.hazelcast.config.Config;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.eventbus.EventBusOptions;
import io.vertx.core.spi.cluster.ClusterManager;
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;


public class Main {

    private static final Logger log = Logger.getLogger(Main.class.getPackageName());

    /**
     * To run the application in a cluster mode, the main class will use Vert.x implementation of Hazelcast as a cluster manager.
     * This method will generate the hazelcast configuration and set the destination address.
     */
    public static void main(String[] args) {
        log.info("OrderVerticle Module= main.main: going to create and use new instance of Hazelcast Config object");
        Config hazelcastConfig = new Config();
        ClusterManager mgr = new HazelcastClusterManager(hazelcastConfig);

        log.info("OrderVerticle Module= main.main: going to set cluster address to EventBusOptions Object");
        VertxOptions options = new VertxOptions().setClusterManager(mgr);
        EventBusOptions ebOptions = new EventBusOptions().setHost(getAddress()).setClustered(true);
        options.setEventBusOptions(ebOptions);

        log.info("OrderVerticle Module= main.main: address set successfully  to= " + getAddress());
        Vertx.clusteredVertx(options, result -> {
            log.info("OrderVerticle Module= main.main: going to deployVerticle()");
            if (result.succeeded()) {
                result.result().deployVerticle(OrderVerticle.class, new DeploymentOptions());
            }
        });
    }

    /**
     * This method will use the NetworkInterface to locate and filter the IP addresses.
     * The relevant IP address will be sent back to the main method.
     */
    private static String getAddress() {
        try {
            log.info("OrderVerticle Module= main.getAddress: going to use NetworkInterface to locate IP addresses");
            List<NetworkInterface> networkInterfaces = new ArrayList<>();
            NetworkInterface.getNetworkInterfaces()
                    .asIterator().forEachRemaining(networkInterfaces::add);

            log.info("OrderVerticle Module= main.getAddress: going to filter the IP addresses");
            return networkInterfaces.stream()
                    .flatMap(result -> result.inetAddresses()
                            .filter(entry -> entry.getAddress().length == 4)
                            .filter(entry -> !entry.isLoopbackAddress())
                            .filter(entry -> entry.getAddress()[0] != Integer.valueOf(10).byteValue())
                            .map(InetAddress::getHostAddress))
                    .findFirst().orElse(null);
        } catch (SocketException e) {
            return null;
        }
    }
}
