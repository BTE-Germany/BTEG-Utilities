package de.btegermany.utilities.registry;

import de.btegermany.utilities.restart.Restart;
import de.btegermany.utilities.restart.RestartsIDsManager;

import java.util.*;

public class RestartsRegistry {

    private final RestartsIDsManager restartsIDsManager;
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

    public Map<Integer, Restart> getRestarts() {
        return restarts;
    }
}
