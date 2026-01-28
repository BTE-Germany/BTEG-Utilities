package dev.btedach.dachutility.data;

import org.slf4j.Logger;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.hocon.HoconConfigurationLoader;
import org.spongepowered.configurate.serialize.SerializationException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

public class ConfigReader {

    private static final String CONFIG = "config.conf";
    private final Logger logger;
    private CommentedConfigurationNode rootNode;

    public ConfigReader(Path dataDirectoryPath, Logger logger) {
        this.logger = logger;

        final HoconConfigurationLoader configLoader = HoconConfigurationLoader.builder()
                .path(new File(dataDirectoryPath.toFile(), CONFIG).toPath())
                .build();

        try {
            this.rootNode = configLoader.load();
        } catch (IOException e) {
            this.logger.error("Loading config {} failed", CONFIG, e);
            this.rootNode = null;
        }
    }

    public PortainerConfig readPortainerConfig() {
        try {
            String environmentIdEnv = System.getenv("PORTAINER_ENVIRONMENT_ID");

            ConfigurationNode portainerNode = this.rootNode.node("portainer");
            Integer environmentId = portainerNode.node("environment-id").get(Integer.class, (environmentIdEnv != null ? Integer.parseInt(environmentIdEnv) : null));
            String accessToken = portainerNode.node("access-token").getString();

            if (environmentId == null || accessToken == null) {
                this.logger.warn("Portainer configuration is invalid");
            }

            return new PortainerConfig(environmentId, accessToken);

        } catch (SerializationException e) {
            this.logger.warn("Config unter \"{}\" konnte nicht geladen werden. Ungültige environment-id", CONFIG);
            e.printStackTrace();
        }
        return null;
    }

}
