package de.hhu.bsinfo.dxraft;

import de.hhu.bsinfo.dxraft.server.RaftServer;
import de.hhu.bsinfo.dxraft.server.ServerConfig;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class DXRaft {

    private static final String SERVER_CONFIG_PATH = "config/server-config.json";
    private static final Logger LOGGER = LogManager.getLogger();

    private DXRaft() {}

    public static void main(String[] p_args) {
        String configPath = System.getProperty("dxraft.config");
        if (configPath == null) {
            configPath = SERVER_CONFIG_PATH;
        }

        ServerConfig config = ConfigUtils.getServerConfig(configPath);

        if (config == null) {
            return;
        }

        RaftServer server = new RaftServer(config);

        if (join(config)) {
            server.joinExistingCluster();
        } else {
            server.bootstrapNewCluster();
        }
    }

    private static boolean join(ServerConfig p_context) {
        return !p_context.getServers().contains(p_context.getRaftAddress());
    }


}
