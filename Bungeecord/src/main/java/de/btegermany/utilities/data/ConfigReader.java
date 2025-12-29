package de.btegermany.utilities.data;

import de.btegermany.utilities.BTEGUtilitiesBungee;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class ConfigReader {

    private static final String CONFIG = "config.yaml";
    private final BTEGUtilitiesBungee plugin;

    public ConfigReader(BTEGUtilitiesBungee plugin) {
        this.plugin = plugin;
    }

    public PortainerConfig readPortainerConfig() {
        ConfigurationProvider provider = ConfigurationProvider.getProvider(YamlConfiguration.class);
        File dir = plugin.getDataFolder();
        if (!dir.getParentFile().exists()) dir.getParentFile().mkdir();
        if (!dir.exists()) dir.mkdir();
        File configFile = new File(dir, CONFIG);

        try {
            if (!configFile.exists()) {
                try (InputStream inputStream = plugin.getResourceAsStream(configFile.getName())) {
                    FileUtils.copyInputStreamToFile(inputStream, configFile);
                }
            }
            Configuration config = provider.load(configFile);

            String environmentIdEnv = System.getenv("PORTAINER_ENVIRONMENT_ID");

            Integer environmentId = config.get("portainer.environment-id", (environmentIdEnv != null ? Integer.parseInt(environmentIdEnv) : null));
            String accessToken = config.getString("portainer.access-token", null);

            if (environmentId == null || accessToken == null) {
                this.plugin.getLogger().warning("Portainer configuration is invalid");
            }

            return new PortainerConfig(environmentId, accessToken);

        } catch (IOException e) {
            plugin.getLogger().warning("Config unter \"" + configFile.getPath() + "\" konnte nicht geladen werden!");
            e.printStackTrace();
        }
        return null;
    }

}
