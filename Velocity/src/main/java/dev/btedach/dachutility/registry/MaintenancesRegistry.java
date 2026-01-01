package dev.btedach.dachutility.registry;

import com.velocitypowered.api.proxy.server.RegisteredServer;
import dev.btedach.dachutility.DACHUtility;
import dev.btedach.dachutility.maintenance.Maintenance;
import lombok.Getter;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;

public class MaintenancesRegistry {

    private final DACHUtility plugin;
    @Getter
    private final Map<String, Maintenance> maintenances;
    private final File maintenancesFile;
    private final JSONArray json;

    public MaintenancesRegistry(DACHUtility plugin, Path dataDirectoryPath, String fileName) {
        this.plugin = plugin;
        this.maintenances = Collections.synchronizedMap(new HashMap<>());

        this.maintenancesFile = new File(dataDirectoryPath.toFile(), fileName);
        JSONArray jsonArray = new JSONArray();
        try {
            if(!maintenancesFile.getParentFile().exists()) maintenancesFile.getParentFile().mkdir();
            if(!maintenancesFile.exists()) maintenancesFile.createNewFile();

            BufferedReader br = new BufferedReader(new FileReader(maintenancesFile));
            String line = br.readLine();
            if(line != null) {
                jsonArray = new JSONArray(line);
            }
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.json = jsonArray;
    }

    public void loadMaintenances() {
        for(int i = 0; i < this.json.length(); i++) {
            JSONObject object = this.json.getJSONObject(i);
            JSONObject timeJSON = object.getJSONObject("time");
            JSONArray serversJSON = object.getJSONArray("servers");

            String name = object.getString("name");
            ZonedDateTime time = LocalDateTime.of(
                            timeJSON.getInt("year"),
                            timeJSON.getInt("month"),
                            timeJSON.getInt("day"),
                            timeJSON.getInt("hour"),
                            timeJSON.getInt("minute"))
                    .atZone(ZoneId.of("Europe/Berlin"));
            Set<RegisteredServer> servers = new HashSet<>();

            boolean proxy = false;
            for(int j = 0; j < serversJSON.length(); j++) {
                if(serversJSON.getString(j).equals("Proxy-1")) {
                    proxy = true;
                    continue;
                }
                Optional<RegisteredServer> registeredServerOptional = DACHUtility.getInstance().getServer().getServer(serversJSON.getString(j));
                if (registeredServerOptional.isEmpty()) {
                    continue;
                }
                servers.add(registeredServerOptional.get());
            }

            Maintenance maintenance = new Maintenance(name, servers, time, proxy);
            this.maintenances.put(name, maintenance);
        }
    }

    public void register(Maintenance maintenance) {
        this.maintenances.put(maintenance.name(), maintenance);

        JSONArray arrayServers = new JSONArray();
        maintenance.servers().forEach(registeredServer -> {
            if(registeredServer != null) {
                arrayServers.put(registeredServer.getServerInfo().getName());
            }
        });
        if(maintenance.proxy()) {
            arrayServers.put("Proxy-1");
        }

        JSONObject object = new JSONObject()
                .put("name", maintenance.name())
                .put("servers", arrayServers)
                .put("time", new JSONObject()
                        .put("day", maintenance.time().getDayOfMonth())
                        .put("month", maintenance.time().getMonthValue())
                        .put("year", maintenance.time().getYear())
                        .put("hour", maintenance.time().getHour())
                        .put("minute",maintenance.time().getMinute()));
        this.json.put(object);
        this.saveJson();
        this.plugin.scheduleMaintenances(this);
    }

    public void unregister(String name) {
        this.maintenances.remove(name);
        for(int i = 0; i < this.json.length(); i++) {
            JSONObject object = this.json.getJSONObject(i);
            if(object.getString("name").equals(name)) {
                this.json.remove(i);
                this.saveJson();
                this.plugin.scheduleMaintenances(this);
                return;
            }
        }
    }

    private void saveJson() {
        try {
            FileWriter fw = new FileWriter(this.maintenancesFile);
            fw.write(this.json.toString());
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}