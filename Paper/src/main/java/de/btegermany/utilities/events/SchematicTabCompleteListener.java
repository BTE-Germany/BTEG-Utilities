package de.btegermany.utilities.events;

import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.LocalConfiguration;
import com.destroystokyo.paper.event.server.AsyncTabCompleteEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.TabCompleteEvent;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

/** Adds schematic name and folder suggestions to FAWE's schematic commands. */
public final class SchematicTabCompleteListener implements Listener {

    @EventHandler
    public void onTabComplete(TabCompleteEvent event) {
        List<String> matches = getSuggestions(event.getBuffer());
        if (matches != null) {
            event.setCompletions(matches);
        }
    }

    /** FAWE registers its commands with Brigadier, so Paper reports their completions here. */
    @EventHandler
    public void onAsyncTabComplete(AsyncTabCompleteEvent event) {
        if (!event.isCommand()) {
            return;
        }
        List<String> matches = getSuggestions(event.getBuffer());
        if (matches != null) {
            event.setCompletions(matches);
        }
    }

    private static List<String> getSuggestions(String buffer) {
        String[] parts = buffer.trim().split("\\s+");
        if (parts.length < 2 || !isSchematicCommand(parts[0])) {
            return null;
        }

        String action = parts[1].toLowerCase(Locale.ROOT);
        boolean trailingSpace = !buffer.isEmpty() && Character.isWhitespace(buffer.charAt(buffer.length() - 1));
        int argumentIndex = trailingSpace ? parts.length : parts.length - 1;
        boolean foldersOnly;
        int filenameIndex;
        if (action.equals("load") || action.equals("delete")) {
            foldersOnly = false;
            filenameIndex = 2;
        } else if (action.equals("save")) {
            foldersOnly = true;
            filenameIndex = parts.length > 2 && parts[2].equalsIgnoreCase("-f") ? 3 : 2;
        } else {
            return null;
        }

        // Suggestions only apply while the filename argument is being entered.
        if (argumentIndex != filenameIndex || parts.length > filenameIndex + (trailingSpace ? 0 : 1)) {
            return null;
        }

        String prefix = trailingSpace ? "" : parts[argumentIndex].replace('\\', '/');
        Path schematicDirectory = getSchematicDirectory();
        if (schematicDirectory == null || !Files.isDirectory(schematicDirectory)) {
            return null;
        }

        int lastSlash = prefix.lastIndexOf('/');
        String directoryPrefix = lastSlash >= 0 ? prefix.substring(0, lastSlash + 1) : "";
        String namePrefix = lastSlash >= 0 ? prefix.substring(lastSlash + 1) : prefix;
        Path root = schematicDirectory.toAbsolutePath().normalize();
        Path currentDirectory = root.resolve(directoryPrefix).normalize();
        if (!currentDirectory.startsWith(root) || !Files.isDirectory(currentDirectory)) {
            return List.of();
        }

        try (Stream<Path> children = Files.list(currentDirectory)) {
            return children
                    .map(path -> {
                        String name = path.getFileName().toString();
                        if (Files.isDirectory(path)) {
                            return directoryPrefix + name + "/";
                        }
                        if (!foldersOnly && Files.isRegularFile(path)) {
                            return directoryPrefix + withoutExtension(name);
                        }
                        return null;
                    })
                    .filter(name -> name != null && name.regionMatches(true, directoryPrefix.length(), namePrefix, 0, namePrefix.length()))
                    .sorted(String.CASE_INSENSITIVE_ORDER)
                    .toList();
        } catch (IOException ignored) {
            // Keep FAWE's own completion behavior if the schematic directory cannot be read.
            return null;
        }
    }

    private static boolean isSchematicCommand(String command) {
        String normalized = command.toLowerCase(Locale.ROOT);
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        return normalized.equals("schem") || normalized.equals("schematic")
                || normalized.equals("fawe:schem") || normalized.equals("fawe:schematic")
                || normalized.equals("worldedit:schem") || normalized.equals("worldedit:schematic");
    }

    private static Path getSchematicDirectory() {
        try {
            WorldEdit worldEdit = WorldEdit.getInstance();
            LocalConfiguration configuration = worldEdit.getConfiguration();
            return worldEdit.getWorkingDirectoryPath(configuration.saveDir);
        } catch (IllegalStateException | LinkageError exception) {
            return null;
        }
    }

    private static String withoutExtension(String name) {
        int slash = name.lastIndexOf('/');
        int dot = name.lastIndexOf('.');
        return dot > slash ? name.substring(0, dot) : name;
    }
}
