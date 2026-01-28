package dev.btedach.dachutility.restart;

import java.util.ArrayList;
import java.util.List;

public class RestartsIDsManager {

    private final List<Integer> idsAssigned;
    private int idNext;

    public RestartsIDsManager() {
        this.idsAssigned = new ArrayList<>();
        this.idNext = 0;
    }

    public synchronized int getAndClaimNextId() {
        int id = this.idNext;
        this.idsAssigned.add(id, id);
        this.idNext = this.idsAssigned.size();
        return id;
    }

    public synchronized void releaseId(int id) {
        this.idsAssigned.remove(Integer.valueOf(id));
        if(this.idsAssigned.isEmpty()) {
            this.idNext = 0;
            return;
        }
        int lastAssigned = this.idsAssigned.get(this.idsAssigned.size() - 1);
        this.idNext = id - 1 > lastAssigned ? lastAssigned + 1 : id;
    }

}
