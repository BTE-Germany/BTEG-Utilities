package dev.btedach.dachutility.utils;

import dev.btedach.dachutility.DACHUtility;
import net.kyori.adventure.text.Component;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class FileManager {




    public void checkConfigFiles() throws IOException {
        File configFile = new File(Constants.JAR_PATH+"DACH-Utils/config.yml");
        if(!configFile.exists()){
            configFile.createNewFile();
            try (PrintWriter writer = new PrintWriter(new FileWriter(configFile, false))) {
                    writer.println("token" + Constants.DELIMITER);
                writer.println("mainServerID" + Constants.DELIMITER + 0);
                writer.println("allChannelID" + Constants.DELIMITER + 0);
                writer.println("builderChatID" + Constants.DELIMITER + 0);
                DACHUtility.getInstance().getLogger().info("You need to start your Proxy again to add the Token, ChannelID and ServerId in den config.yml file1");
                DACHUtility.getInstance().getLogger().info("Stopping the Server!");
                DACHUtility.instance.getServer().shutdown(Component.text("No Token + ChannelID + ServerID"));
            } catch (IOException e) {
                DACHUtility.getInstance().getLogger().error("Failed to write to config.yml file", e);
            }
        }
    }




    public Object fetchStringFromConfig(File file, String config) {
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] s = line.split("=");
                if(s[0].toLowerCase().startsWith(config)){
                    return s[1];
                }
            }
        } catch (IOException e) {
            DACHUtility.getInstance().getLogger().error("Failed to read from config. {}", e.getMessage());
        }
        return null;
    }

    public Object fetchStringFromConfig(FILETYPE filetype, String config) {
        try (BufferedReader reader = new BufferedReader(new FileReader(filetype.getFilePath()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] s = line.split("=");
                if(s[0].toLowerCase().startsWith(config.toLowerCase())){
                    return s[1];
                }
            }
        } catch (IOException e) {
            DACHUtility.getInstance().getLogger().error("Failed to read from config. {}", e.getMessage());
        }
        return null;
    }

    public enum FILETYPE {
        CONFIG(Constants.JAR_PATH+"DACH-Utils/config.yml");

        private String filePath;

        FILETYPE(String filePath) {
            this.filePath = filePath;
        }

        public String getFilePath() {
            return filePath;
        }
    }

    public static File getFileFromResource(String fileName) throws URISyntaxException {
        ClassLoader classLoader = DACHUtility.class.getClassLoader();
        URL resource = classLoader.getResource(fileName);
        if (resource == null) {
            throw new IllegalArgumentException("file not found! " + fileName);
        } else {
            File configFile=new File(classLoader.getResource(fileName).getFile());
            return configFile;
        }

    }


    public static List getFileAsList(String fileName) throws IOException {
        List<String> lines = new ArrayList<>();
        InputStream is =  getFileFromResourceAsStream(fileName);
        InputStreamReader streamReader = new InputStreamReader(is, StandardCharsets.UTF_8);
        BufferedReader reader = new BufferedReader(streamReader);
        String line;
        while ((line = reader.readLine()) != null) {
            lines.add(line);
        }
        return lines;
    }


    public static String getStringFromFile(String filename, String input) throws IOException {
        InputStream is =  getFileFromResourceAsStream(filename);
        InputStreamReader streamReader =
                new InputStreamReader(is, StandardCharsets.UTF_8);
        BufferedReader reader = new BufferedReader(streamReader);

        String line;
        while ((line = reader.readLine()) != null) {
            if(line.startsWith(input)){
                return line;
            }
        }
        return "";
    }

    private static InputStream getFileFromResourceAsStream(String fileName) {
        ClassLoader classLoader = DACHUtility.class.getClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream(fileName);
        if (inputStream == null) {
            DACHUtility.getInstance().getLogger().info(("file not found! " + fileName));
            return null;
        } else {
            return inputStream;
        }
    }
}
