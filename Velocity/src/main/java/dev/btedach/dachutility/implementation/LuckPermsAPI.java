package dev.btedach.dachutility.implementation;

import dev.btedach.dachutility.DACHUtility;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.query.QueryOptions;

import java.util.Objects;
import java.util.UUID;

public class LuckPermsAPI {
    public DACHUtility instance;

    public LuckPermsAPI(DACHUtility instance){
        this.instance = instance;
    }

    public String getHightestRole(UUID uuid){
        int highestGroup = 0;
        String highestGroupName = "";
        for(Group group : Objects.requireNonNull(instance.getLuckPerms().getUserManager().getUser(uuid)).getInheritedGroups(QueryOptions.nonContextual())){
            if(group.getWeight().isPresent()){
                if(highestGroup < group.getWeight().getAsInt()){
                    highestGroup = group.getWeight().getAsInt();
                    highestGroupName = group.getName();
                }
            }

        }
        return highestGroupName;
    }
}
