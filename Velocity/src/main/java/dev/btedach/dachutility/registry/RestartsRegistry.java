package dev.btedach.dachutility.registry;

import dev.btedach.dachutility.restart.Restart;
import dev.btedach.dachutility.restart.RestartsIDsManager;
import lombok.Getter;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class RestartsRegistry {

    private final RestartsIDsManager restartsIDsManager;
    @Getter
    private final Map<Integer, Restart> restarts;

    public RestartsRegistry(RestartsIDsManager restartsIDsManager) {
        this.restartsIDsManager = restartsIDsManager;
        this.restarts = Collections.synchronizedMap(new HashMap<>());
    }

    public void register(Restart restart) {
        this.restarts.put(restart.getId(), restart);
    }

    public void unregister(int id) {
        this.restarts.remove(id);
        this.restartsIDsManager.releaseId(id);
    }

    public void unregister(Restart restart) {
        this.unregister(restart.getId());
    }

    public Restart getRestart(int id) {
        return this.restarts.get(id);
    }

}
