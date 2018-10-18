package de.hhu.bsinfo.dxraft;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import de.hhu.bsinfo.dxraft.client.ClientConfig;
import de.hhu.bsinfo.dxraft.server.ServerConfig;
import de.hhu.bsinfo.dxutils.JsonUtil;
import lombok.extern.log4j.Log4j2;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;

@Log4j2
public final class ConfigUtils {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting()
        .excludeFieldsWithoutExposeAnnotation().create();

    private ConfigUtils() {

    }

    public static JsonElement getConfig(String p_configPath) {
        File file = new File(p_configPath);
        if (!file.exists()) {
            System.out.println("No config file exists, creating default config file and exiting...");
            ClientConfig config = new ClientConfig();
            String configString = GSON.toJson(config);

            try {
                PrintWriter writer = new PrintWriter(file);
                writer.print(configString);
                writer.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }

            return null;
        }

        JsonElement element;
        Gson gson = new GsonBuilder().setPrettyPrinting().excludeFieldsWithoutExposeAnnotation().create();

        try {
            element = gson.fromJson(new String(Files.readAllBytes(Paths.get(p_configPath))), JsonElement.class);
        } catch (final Exception e) {
            log.error("Could not load configuration '%s': %s", p_configPath, e.getMessage());
            return null;
        }

        if (element == null) {
            log.error("Could not load configuration '%s': empty configuration file", p_configPath);
            return null;
        }

        JsonUtil.override(element, System.getProperties(), "dxraft.", Collections.singletonList("dxram.config"));

        return element;
    }

    public static ClientConfig getClientConfig(String p_configPath) {
        JsonElement el = ConfigUtils.getConfig(p_configPath);

        if (el == null) {
            return null;
        }

        ClientConfig config;

        try {
            config = GSON.fromJson(el, ClientConfig.class);
        } catch (final Exception e) {
            log.error("Loading configuration '%s' failed: %s", p_configPath, e.getMessage());
            return null;
        }

        if (config == null) {
            log.error("Loading configuration '%s' failed: context null", p_configPath);
            return null;
        }

        return config;
    }

    public static ServerConfig getServerConfig(String p_configPath) {
        JsonElement el = ConfigUtils.getConfig(p_configPath);

        if (el == null) {
            return null;
        }

        ServerConfig config;

        try {
            config = GSON.fromJson(el, ServerConfig.class);
        } catch (final Exception e) {
            log.error("Loading configuration '%s' failed: %s", p_configPath, e.getMessage());
            return null;
        }

        if (config == null) {
            log.error("Loading configuration '%s' failed: context null", p_configPath);
            return null;
        }

        return config;
    }
}
