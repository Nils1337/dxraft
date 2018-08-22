package de.hhu.bsinfo.dxraft;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

import de.hhu.bsinfo.dxraft.context.RaftAddress;
import de.hhu.bsinfo.dxraft.server.RaftServer;
import de.hhu.bsinfo.dxraft.server.RaftServerContext;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class DXRaft {

    private static final String CONFIG_FILE_PATH = "config.json";
    private static final Logger LOGGER = LogManager.getLogger();

    private DXRaft() {}

    public static void main(String[] args) {
        String idString = System.getProperty("server.id");

        if (idString == null) {
            System.out.println("Id must be provided. Exiting...");
            return;
        }

        int id = RaftAddress.INVALID_ID;

        try {
            id = Integer.parseInt(idString);
        } catch (NumberFormatException e) {
            System.out.println("Could not parse id. Exiting...");
            return;
        }

        if (id == RaftAddress.INVALID_ID) {
            System.out.println("Invalid id. Exiting...");
        }

        JsonParser parser = new JsonParser();
        Gson gson = new Gson();
        JsonObject config;

        InputStream configStream = DXRaft.class.getClassLoader().getResourceAsStream(CONFIG_FILE_PATH);

        if (configStream == null) {
            LOGGER.error("Config file could not be found. Exiting...");
            return;
        }

        Reader reader = new InputStreamReader(configStream);
        config = parser.parse(reader).getAsJsonObject();

        JsonArray servers = config.getAsJsonArray("servers");
        Type type = new TypeToken<ArrayList<RaftAddress>>(){}.getType();
        List<RaftAddress> addresses = gson.fromJson(servers, type);

        final int localId = id;
        Optional<RaftAddress> localAddress = addresses.stream().filter(addr -> addr.getId() == localId).findAny();

        if (localAddress.isPresent()) {
            RaftServerContext context = RaftServerContext.RaftServerContextBuilder.aRaftServerContext()
                .withRaftServers(addresses)
                .withLocalAddress(localAddress.get())
                .build();

            RaftServer server = RaftServer.RaftServerBuilder.aRaftServer().withContext(context).build();
            server.bootstrapNewCluster();
        } else {
            String ip = System.getProperty("server.ip");
            String portString = System.getProperty("server.port");
            if (ip == null || portString == null) {
                LOGGER.error("Server not in configuration and no ip or no port given. Exiting...");
                return;
            }

            int port;

            try {
                port = Integer.parseInt(idString);
            } catch (NumberFormatException e) {
                System.out.println("Could not parse port. Exiting...");
                return;
            }

            RaftServerContext context = RaftServerContext.RaftServerContextBuilder.aRaftServerContext()
                .withRaftServers(addresses)
                .withLocalAddress(new RaftAddress(id, ip, port))
                .build();

            RaftServer server = RaftServer.RaftServerBuilder.aRaftServer().withContext(context).build();
            server.joinExistingCluster();
        }
    }


}
