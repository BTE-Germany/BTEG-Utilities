package de.btegermany.utilities.commands;

import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.io.BuiltInClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardWriter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockTypes;
import de.btegermany.utilities.BTEGUtilities;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Saves analyzed tree selections into configurable treepack directory layouts.
 */
public final class TreePackCommand implements TabExecutor {
    private final BTEGUtilities plugin;
    private final Map<UUID, Pending> pending = new ConcurrentHashMap<>();
    private final File configFile;

    public TreePackCommand(BTEGUtilities plugin) {
        this.plugin = plugin;
        configFile = new File(plugin.getDataFolder(), "treepack.yml");
        if (!configFile.exists()) plugin.saveResource("treepack.yml", false);
        migrateDensityOptions();
        validateConfiguredBlocks();
    }

    private void migrateDensityOptions() {
        YamlConfiguration configuration = config();
        if (configuration.contains("manual.densities")) return;
        var oldDensitySection = configuration.getConfigurationSection("density");
        List<String> densityOptions = oldDensitySection == null
                ? List.of("dense", "moderate", "sparse", "airy")
                : new ArrayList<>(oldDensitySection.getKeys(false));
        configuration.set("manual.densities", densityOptions);
        configuration.set("density", null);
        try {
            configuration.save(configFile);
            plugin.getLogger().info("Migrated treepack density categories to manual.densities.");
        } catch (IOException exception) {
            plugin.getLogger().warning("Could not migrate treepack density options: " + exception.getMessage());
        }
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String @NotNull [] args) {
        if (!(sender instanceof Player player)) return true;
        if (!player.hasPermission("bteg.builder")) {
            player.sendMessage(BTEGUtilities.PREFIX + "§cNo permission.");
            return true;
        }
        if (args.length == 1 && args[0].equalsIgnoreCase("confirm")) {
            Pending request = pending.remove(player.getUniqueId());
            if (request == null || request.expiresAt < System.currentTimeMillis()) {
                player.sendMessage("§cThere is no pending tree type confirmation.");
                return true;
            }
            save(player, request.type, request.trunk, request.shape, request.density);
            return true;
        }
        if (args.length != 5 || !args[0].equalsIgnoreCase("tree")) {
            player.sendMessage("§cUsage: /addnorm tree <type> <trunk> <shape> <density> or /addnorm confirm");
            return true;
        }
        String type = clean(args[1]), trunk = args[2], shape = args[3], density = args[4];
        YamlConfiguration cfg = config();
        if (!values(cfg, "manual.trunks").contains(trunk)) {
            player.sendMessage("§cUnknown trunk. Options: " + String.join(", ", values(cfg, "manual.trunks")));
            return true;
        }
        if (!values(cfg, "manual.shapes").contains(shape)) {
            player.sendMessage("§cUnknown shape. Options: " + String.join(", ", values(cfg, "manual.shapes")));
            return true;
        }
        if (!values(cfg, "manual.densities").contains(density)) {
            player.sendMessage("§cUnknown density. Options: " + String.join(", ", values(cfg, "manual.densities")));
            return true;
        }
        if (!knownTypes(cfg).contains(type.toLowerCase(Locale.ROOT))) {
            pending.put(player.getUniqueId(), new Pending(type, trunk, shape, density, System.currentTimeMillis() + 15000));
            Bukkit.getScheduler().runTaskLater(plugin, () -> pending.remove(player.getUniqueId()), 300);
            player.sendMessage("§eTree type '" + type + "' does not exist. Run §f/addnorm confirm §ewithin 15 seconds to create it.");
            return true;
        }
        save(player, type, trunk, shape, density);
        return true;
    }

    private void save(Player player, String type, String trunk, String shape, String density) {
        try {
            YamlConfiguration c = config();
            var local = WorldEdit.getInstance().getSessionManager().get(BukkitAdapter.adapt(player));
            var region = local.getSelection(local.getSelectionWorld());
            if (!(region instanceof CuboidRegion cuboid)) {
                player.sendMessage("§cSelect a cuboid first.");
                return;
            }
            BlockVector3 min = region.getMinimumPoint(), max = region.getMaximumPoint();
            Map<BlockVector3, BlockState> blocks = new HashMap<>();
            BlockVector3 pos1 = cuboid.getPos1(), pos2 = cuboid.getPos2();
            int high = max.y(), low = min.y();
            for (int y = min.y(); y <= max.y(); y++)
                for (int x = min.x(); x <= max.x(); x++)
                    for (int z = min.z(); z <= max.z(); z++) {
                        BlockVector3 p = BlockVector3.at(x, y, z);
                        if (!region.contains(p) || p.equals(pos1) || p.equals(pos2)) continue;
                        BlockState b = Objects.requireNonNull(region.getWorld()).getBlock(p);
                        if (!b.isAir())
                            blocks.put(p, b);
                    }
            if (blocks.isEmpty()) throw new IllegalArgumentException("Selection contains no tree blocks.");
            BlockVector3 origin = findOrigin(blocks, c, low, min, max);
            if (origin == null) {
                player.sendMessage("§cNo configured trunk block was found on the selection's bottom layer. Nothing was saved.");
                return;
            }
            while (high > low) {
                boolean occupied = false;
                for (BlockVector3 p : blocks.keySet())
                    if (p.y() == high) {
                        occupied = true;
                        break;
                    }
                if (occupied) break;
                high--;
            }
            int height = high - low + 1;
            Set<String> leaves = leafBlocks(c);
            int trunkHeight = 0;
            for (int y = low; y <= high; y++) {
                final int yy = y;
                if (blocks.entrySet().stream().anyMatch(e -> e.getKey().y() == yy && matchesAny(e.getValue(), leaves)))
                    break;
                trunkHeight++;
            }
            String heightType = rangeCategory(c, "height-types", height);
            String trunkHeightType = rangeCategory(c, "trunk-heights", trunkHeight);
            String colour = scoreCategory(c, "colours", blocks.values());
            List<BlockState> base = blocks.entrySet().stream().filter(e -> e.getKey().y() == low).map(Map.Entry::getValue).toList();
            String trunkColour = scoreCategory(c, "trunk-colours", base);
            String thickness = thickness(base, c);
            Map<String, String> vars = new HashMap<>();
            vars.put("type", type);
            vars.put("height", height + "m");
            vars.put("height_type", heightType);
            vars.put("trunk_height", trunkHeight + "m");
            vars.put("trunk", trunk);
            vars.put("shape", shape);
            vars.put("density", density);
            vars.put("colour", colour);
            vars.put("trunk_colour", trunkColour);
            vars.put("thickness", thickness);
            List<String> paths = values(c, "paths");
            if (paths.isEmpty()) throw new IllegalArgumentException("No output paths configured.");
            Path root = plugin.getDataFolder().toPath().resolve(c.getString("output-root", "treepack")).normalize();
            int count = nextCount(root, type + height + "_");
            String name = type + height + "_" + count + ".schem";
            for (String template : paths) {
                String relative = render(template, vars).replace("\\", "/");
                if (relative.endsWith("tree.schem"))
                    relative = relative.substring(0, relative.length() - "tree.schem".length()) + name;
                Path target = root.resolve(relative).normalize();
                if (!target.startsWith(root))
                    throw new IllegalArgumentException("Configured path escapes output root.");
                if (!relative.endsWith(".schem")) target = target.resolve(name);
                Files.createDirectories(target.getParent());
                var clipboard = new BlockArrayClipboard(new CuboidRegion(region.getWorld(), min, max));
                clipboard.setOrigin(origin);
                for (var entry : blocks.entrySet())
                    clipboard.setBlock(entry.getKey(), entry.getValue());
                try (ClipboardWriter writer = BuiltInClipboardFormat.SPONGE_V3_SCHEMATIC.getWriter(Files.newOutputStream(target))) {
                    writer.write(clipboard);
                }
            }
            player.sendMessage("§aSaved §f" + name + " §ato " + paths.size() + " treepack location(s).");
            sendUncategorizedBlocks(player, c, blocks.values());
        } catch (Exception ex) {
            player.sendMessage("§cCould not save tree schematic: " + ex.getMessage());
            plugin.getLogger().warning("Treepack save failed: " + ex);
        }
    }

    private YamlConfiguration config() {
        YamlConfiguration configuration = new YamlConfiguration();
        try {
            configuration.load(configFile);
            return configuration;
        } catch (IOException | InvalidConfigurationException exception) {
            Path backup = configFile.toPath().resolveSibling(configFile.getName() + ".invalid");
            try {
                Files.copy(configFile.toPath(), backup, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                try (var defaults = plugin.getResource("treepack.yml")) {
                    if (defaults == null) throw new IOException("Bundled treepack.yml resource is missing");
                    Files.copy(defaults, configFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                }
                plugin.getLogger().warning("Invalid treepack.yml was backed up to " + backup.getFileName()
                        + " and replaced with the bundled defaults. Edit the repaired file to restore custom settings.");
                configuration.load(configFile);
                return configuration;
            } catch (IOException | InvalidConfigurationException repairException) {
                plugin.getLogger().severe("Could not repair invalid treepack.yml: " + repairException.getMessage());
                return new YamlConfiguration();
            }
        }
    }

    private List<String> values(YamlConfiguration c, String path) {
        return c.getStringList(path);
    }

    private Set<String> knownTypes(YamlConfiguration c) {
        Set<String> out = new TreeSet<>();
        Path root = plugin.getDataFolder().toPath().resolve(c.getString("output-root", "treepack"));
        for (String pattern : values(c, "paths")) {
            int i = pattern.indexOf("<type>");
            if (i < 0) continue;
            String prefix = pattern.substring(0, i).replace("[", "").replace("]", "");
            Path dir = root.resolve(prefix);
            while (dir.getParent() != null && !Files.exists(dir)) dir = dir.getParent();
            if (Files.isDirectory(dir)) try (var stream = Files.list(dir)) {
                stream.filter(Files::isDirectory).forEach(p -> out.add(p.getFileName().toString().toLowerCase(Locale.ROOT)));
            } catch (IOException ignored) {
            }
        }
        return out;
    }

    private Set<String> leafBlocks(YamlConfiguration c) {
        Set<String> s = new HashSet<>();
        for (String key : Objects.requireNonNull(c.getConfigurationSection("colours")).getKeys(false)) {
            s.addAll(c.getStringList("colours." + key + ".blocks"));
            s.addAll(c.getStringList("colours." + key + ".others"));
        }
        return s;
    }

    private List<String> trunkBlocks(YamlConfiguration c) {
        List<String> patterns = new ArrayList<>();
        var section = c.getConfigurationSection("trunk-colours");
        if (section == null) return patterns;
        for (String category : section.getKeys(false)) {
            patterns.addAll(c.getStringList("trunk-colours." + category + ".blocks"));
            patterns.addAll(c.getStringList("trunk-colours." + category + ".walls"));
            patterns.addAll(c.getStringList("trunk-colours." + category + ".fences"));
            patterns.addAll(c.getStringList("trunk-colours." + category + ".other"));
        }
        return patterns;
    }

    private BlockVector3 findOrigin(Map<BlockVector3, BlockState> blocks, YamlConfiguration c, int bottomY,
                                    BlockVector3 min, BlockVector3 max) {
        List<String> trunkPatterns = trunkBlocks(c);
        BlockVector3 best = null;
        int bestRank = -1;
        double bestDistance = Double.POSITIVE_INFINITY;
        double centerX = (min.x() + max.x()) / 2.0;
        double centerZ = (min.z() + max.z()) / 2.0;
        for (var entry : blocks.entrySet()) {
            BlockVector3 position = entry.getKey();
            if (position.y() != bottomY || !matchesAny(entry.getValue(), trunkPatterns)) continue;
            int rank = trunkThicknessRank(entry.getValue(), c);
            double dx = position.x() - centerX, dz = position.z() - centerZ;
            double distance = dx * dx + dz * dz;
            if (best == null || rank > bestRank || (rank == bestRank && distance < bestDistance)
                    || (rank == bestRank && distance == bestDistance
                    && (position.x() < best.x() || (position.x() == best.x() && position.z() < best.z())))) {
                best = position;
                bestRank = rank;
                bestDistance = distance;
            }
        }
        return best;
    }

    private int trunkThicknessRank(BlockState state, YamlConfiguration c) {
        if (matchesAny(state, trunkPatterns(c, "blocks"))) return 4; // full block
        if (blockId(state).equals("pointed_dripstone") && matchesAny(state, trunkPatterns(c, "walls"))) return 2;
        if (matchesAny(state, trunkPatterns(c, "walls"))) return 3; // wall
        if (matchesAny(state, trunkPatterns(c, "fences"))) return 1; // fence
        if (matchesAny(state, trunkPatterns(c, "other"))) return 0;
        return 0; // other configured trunk material
    }

    private List<String> trunkPatterns(YamlConfiguration c, String group) {
        List<String> patterns = new ArrayList<>();
        var section = c.getConfigurationSection("trunk-colours");
        if (section == null) return patterns;
        for (String category : section.getKeys(false))
            patterns.addAll(c.getStringList("trunk-colours." + category + "." + group));
        return patterns;
    }

    private void sendUncategorizedBlocks(Player player, YamlConfiguration c, Collection<BlockState> blocks) {
        List<String> leafPatterns = new ArrayList<>(leafBlocks(c));
        List<String> trunkPatterns = trunkBlocks(c);
        Map<String, Integer> counts = new TreeMap<>();
        for (BlockState block : blocks) {
            if (matchesAny(block, leafPatterns) || matchesAny(block, trunkPatterns)) continue;
            counts.merge(block.getBlockType().id(), 1, Integer::sum);
        }
        if (counts.isEmpty()) {
            player.sendMessage("§7All blocks in the schematic match a configured leaf or trunk category.");
            return;
        }
        player.sendMessage("§eUncategorized blocks (ID × count):");
        List<String> entries = counts.entrySet().stream().map(e -> e.getKey() + " × " + e.getValue()).toList();
        for (int start = 0; start < entries.size(); start += 5) {
            player.sendMessage("§7" + String.join(", ", entries.subList(start, Math.min(start + 5, entries.size()))));
        }
    }

    private String rangeCategory(YamlConfiguration c, String section, int value) {
        var sec = c.getConfigurationSection(section);
        if (sec != null) for (String k : sec.getKeys(false))
            if (value >= sec.getInt(k + ".min", 0) && value <= sec.getInt(k + ".max", Integer.MAX_VALUE)) return k;
        return "unknown";
    }

    private String scoreCategory(YamlConfiguration c, String section, Collection<BlockState> input) {
        var sec = c.getConfigurationSection(section);
        if (sec == null) return "unknown";
        String winner = "unknown";
        double max = 0;
        for (String k : sec.getKeys(false)) {
            List<String> patterns = new ArrayList<>(c.getStringList(section + "." + k + ".blocks"));
            patterns.addAll(c.getStringList(section + "." + k + ".others"));
            patterns.addAll(c.getStringList(section + "." + k + ".walls"));
            patterns.addAll(c.getStringList(section + "." + k + ".fences"));
            patterns.addAll(c.getStringList(section + "." + k + ".other"));
            double score = input.stream().filter(x -> matchesAny(x, patterns)).count() * c.getDouble(section + "." + k + ".multiplier", 1);
            if (score > max) {
                max = score;
                winner = k;
            }
        }
        return winner;
    }

    private String thickness(List<BlockState> blocks, YamlConfiguration c) {
        if (matchesTrunkGroup(blocks, c, "blocks")) return "full_block";
        if (matchesTrunkGroup(blocks, c, "walls", true)) return "wall";
        if (blocks.stream().anyMatch(b -> blockId(b).equals("pointed_dripstone"))) return "pointed_dripstone";
        if (matchesTrunkGroup(blocks, c, "fences")) return "fence";
        if (matchesTrunkGroup(blocks, c, "other")) return "other";
        return "other";
    }

    private boolean matchesTrunkGroup(List<BlockState> states, YamlConfiguration c, String group) {
        return matchesTrunkGroup(states, c, group, false);
    }

    private boolean matchesTrunkGroup(List<BlockState> states, YamlConfiguration c, String group, boolean excludePointedDripstone) {
        var section = c.getConfigurationSection("trunk-colours");
        if (section == null) return false;
        for (String category : section.getKeys(false)) {
            List<String> patterns = c.getStringList("trunk-colours." + category + "." + group);
            if (excludePointedDripstone) patterns = patterns.stream().filter(p -> !blockId(p).equals("pointed_dripstone")).toList();
            if (matchesAny(states, patterns)) return true;
        }
        return false;
    }

    private boolean matchesAny(BlockState state, Collection<String> patterns) {
        return patterns.stream().anyMatch(pattern -> matches(state, pattern));
    }

    private boolean matchesAny(Collection<BlockState> states, Collection<String> patterns) {
        return states.stream().anyMatch(state -> matchesAny(state, patterns));
    }

    /** Matches WorldEdit-style block IDs and optional property filters, including comma-separated allowed values. */
    private boolean matches(BlockState state, String pattern) {
        int bracket = pattern.indexOf('[');
        String id = normalizeId(bracket < 0 ? pattern : pattern.substring(0, bracket));
        if (!id.equals(normalizeId(state.getBlockType().id()))) return false;
        if (bracket < 0) return true;
        int end = pattern.lastIndexOf(']');
        if (end < bracket) return false;
        Map<String, List<String>> conditions = new LinkedHashMap<>();
        String currentProperty = null;
        for (String part : pattern.substring(bracket + 1, end).split(",")) {
            String[] pair = part.split("=", 2);
            if (pair.length == 2) {
                currentProperty = pair[0].trim();
                conditions.computeIfAbsent(currentProperty, ignored -> new ArrayList<>()).add(pair[1].trim());
            } else if (currentProperty != null) {
                conditions.get(currentProperty).add(part.trim());
            } else {
                return false;
            }
        }
        for (var condition : conditions.entrySet()) {
            var property = state.getBlockType().getProperty(condition.getKey());
            if (property == null) return false;
            Object actual = state.getState(property);
            if (actual == null || condition.getValue().stream().noneMatch(value -> value.equalsIgnoreCase(actual.toString()))) return false;
        }
        return true;
    }

    private String blockId(BlockState state) {
        return blockId(state.getBlockType().id());
    }

    private String blockId(String id) {
        int bracket = id.indexOf('[');
        if (bracket >= 0) id = id.substring(0, bracket);
        return id.substring(id.indexOf(':') + 1);
    }

    private int nextCount(Path root, String prefix) throws IOException {
        int n = 1;
        if (!Files.exists(root)) return n;
        if (!Files.isDirectory(root)) throw new IOException("Treepack output root is not a directory: " + root);
        try (var stream = Files.walk(root)) {
            for (Path p : stream.filter(Files::isRegularFile).toList()) {
                String f = p.getFileName().toString();
                if (f.startsWith(prefix) && f.endsWith(".schem")) {
                    try {
                        n = Math.max(n, Integer.parseInt(f.substring(prefix.length(), f.length() - 6)) + 1);
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        }
        return n;
    }

    private String render(String path, Map<String, String> vars) {
        for (var e : vars.entrySet()) {
            path = path.replace("[<" + e.getKey() + ">]", e.getValue()).replace("<" + e.getKey() + ">", e.getKey() + "/" + e.getValue());
        }
        return path;
    }

    private String clean(String s) {
        return s.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_-]", "");
    }

    private Set<String> normalizedIds(Collection<String> ids) {
        return ids.stream().map(this::normalizeId).collect(Collectors.toSet());
    }

    private void validateConfiguredBlocks() {
        YamlConfiguration c = config();
        Set<String> patterns = new LinkedHashSet<>();
        addCategoryPatterns(c, patterns, "colours", "blocks", "others");
        addCategoryPatterns(c, patterns, "trunk-colours", "blocks", "walls", "fences", "other");
        List<String> invalid = patterns.stream().filter(pattern -> !isValidBlockPattern(pattern)).sorted().toList();
        if (!invalid.isEmpty()) {
            plugin.getLogger().warning("Invalid Minecraft block IDs or blockstates in treepack.yml:\n - " + String.join("\n - ", invalid));
        }
    }

    private void addCategoryPatterns(YamlConfiguration c, Set<String> patterns, String sectionName, String... fields) {
        var section = c.getConfigurationSection(sectionName);
        if (section == null) return;
        for (String category : section.getKeys(false))
            for (String field : fields)
                patterns.addAll(c.getStringList(sectionName + "." + category + "." + field));
    }

    private boolean isValidBlockPattern(String pattern) {
        int bracket = pattern.indexOf('[');
        String rawId = bracket < 0 ? pattern : pattern.substring(0, bracket);
        String id = normalizeId(rawId);
        if (!id.startsWith("minecraft:")) return false;
        var blockType = BlockTypes.get(id);
        if (blockType == null) return false;
        if (bracket < 0) return true;
        int end = pattern.lastIndexOf(']');
        if (end < bracket || end != pattern.length() - 1) return false;
        String currentProperty = null;
        for (String part : pattern.substring(bracket + 1, end).split(",")) {
            String[] pair = part.split("=", 2);
            if (pair.length == 2) {
                currentProperty = pair[0].trim();
                var property = blockType.getProperty(currentProperty);
                if (property == null || !isValidPropertyValue(property, pair[1].trim())) return false;
            } else if (currentProperty != null) {
                var property = blockType.getProperty(currentProperty);
                if (property == null || !isValidPropertyValue(property, part.trim())) return false;
            } else {
                return false;
            }
        }
        return true;
    }

    private boolean isValidPropertyValue(com.sk89q.worldedit.registry.state.Property<?> property, String value) {
        try {
            property.getValueFor(value);
            return true;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private String normalizeId(String id) {
        String normalized = id.toLowerCase(Locale.ROOT).trim();
        return normalized.contains(":") ? normalized : "minecraft:" + normalized;
    }

    @Override
    public @NonNull List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String @NotNull [] args) {
        if (args.length == 1) {
            List<String> options = args[0].toLowerCase(Locale.ROOT).startsWith("c") ? List.of("confirm") : List.of("tree", "confirm");
            return options.stream().filter(x -> x.startsWith(args[0].toLowerCase(Locale.ROOT))).toList();
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("tree"))
            return knownTypes(config()).stream().filter(x -> x.startsWith(args[1].toLowerCase(Locale.ROOT))).toList();
        if (args.length == 3 && args[0].equalsIgnoreCase("tree"))
            return values(config(), "manual.trunks").stream().filter(x -> x.startsWith(args[2])).toList();
        if (args.length == 4 && args[0].equalsIgnoreCase("tree"))
            return values(config(), "manual.shapes").stream().filter(x -> x.startsWith(args[3])).toList();
        if (args.length == 5 && args[0].equalsIgnoreCase("tree"))
            return values(config(), "manual.densities").stream().filter(x -> x.startsWith(args[4])).toList();
        return Collections.emptyList();
    }

    private record Pending(String type, String trunk, String shape, String density, long expiresAt) {
    }
}
