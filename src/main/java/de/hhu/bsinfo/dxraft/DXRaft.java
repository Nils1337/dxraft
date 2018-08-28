package de.hhu.bsinfo.dxraft;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Optional;

import com.google.gson.Gson;

import de.hhu.bsinfo.dxraft.net.RaftAddress;
import de.hhu.bsinfo.dxraft.server.RaftServer;
import de.hhu.bsinfo.dxraft.server.ServerContext;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class DXRaft {

    private static final String DEFAULT_CONFIG_FILE_PATH = "config.json";
    private static final Logger LOGGER = LogManager.getLogger();

    private DXRaft() {}

    public static void main(String[] p_args) {
        Gson gson = new Gson();
        InputStream configStream;
        ServerContext context;

        String configPath = System.getProperty("config");
        if (configPath == null || !new File(configPath).exists()) {
            System.out.println("No config path given or file does not exist, using default config file");
            configStream = DXRaft.class.getClassLoader().getResourceAsStream(DEFAULT_CONFIG_FILE_PATH);

            if (configStream == null) {
                System.out.println("Default config file could not be found. Exiting...");
                return;
            }

            Reader reader = new InputStreamReader(configStream);
            context = gson.fromJson(reader, ServerContext.class);
        } else {
            System.out.println("Reading " + configPath + "...");
            try {
                context = gson.fromJson(new String(Files.readAllBytes(Paths.get(configPath))), ServerContext.class);
            } catch (IOException e) {
                System.out.println("Error reading given config file. Exiting...");
                return;
            }
        }

        overwriteLocalAddress(context);

        if (context.getLocalAddress() == null) {
            System.out.println("Address of server could not be determined. Exiting...");
            return;
        }

        RaftServer server = RaftServer.RaftServerBuilder.aRaftServer().withContext(context).build();

        if (join(context)) {
            server.joinExistingCluster();
        } else {
            server.bootstrapNewCluster();
        }
    }

    private static void overwriteLocalAddress(ServerContext p_context) {
        String idString = System.getProperty("server.id");
        int id;

        if (idString != null) {
            try {
                id = Integer.parseInt(idString);
            } catch (NumberFormatException e) {
                System.out.println("Warning: Could not parse id.");
                return;
            }

            if (id == RaftAddress.INVALID_ID) {
                System.out.println("Warning: Invalid id in parameter.");
                return;
            }

            // overwrite local address from config with the id provided as parameter
            final int localId = id;
            Optional<RaftAddress> localAddress = p_context.getServers().stream()
                .filter(addr -> addr.getId() == localId).findAny();

            localAddress.ifPresent(p_context::setLocalAddress);
        }
    }

    private static boolean join(ServerContext p_context) {
        return !p_context.getServers().contains(p_context.getLocalAddress());
    }


}
