package de.btegermany.utilities.util;

import org.bukkit.entity.Player;

public enum Direction {
    NORTH (false),
    SOUTH (false),
    EAST (true),
    WEST (true),
    UP (false),
    DOWN (false);

    private final boolean horizontal;

    Direction(boolean horizontal) {
        this.horizontal = horizontal;
    }

    public boolean isHorizontal() {
        return horizontal;
    }

    public static Direction ofPlayer(Player player) {
        float yaw = player.getLocation().getYaw();
        if (yaw < 0) {
            yaw += 360;
        }
        if (yaw >= 315 || yaw < 45) {
            return SOUTH;
        } else if (yaw < 135) {
            return WEST;
        } else if (yaw < 225) {
            return NORTH;
        } else if (yaw < 315) {
            return EAST;
        }
        return NORTH;
    }

    public static Direction fromInput(String input) {
        return switch (input) {
            case "n" -> NORTH;
            case "e" -> EAST;
            case "s" -> SOUTH;
            case "w" -> WEST;
            case "u" -> UP;
            case "d" -> DOWN;
            default -> throw new IllegalStateException("Unexpected value: " + input);
        };
    }

}