package io.github.astail.kanbanchat;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.bukkit.block.sign.Side;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * 登録済みログイン看板の一覧を保持し、signs.yml へ永続化する。
 * 位置をキーに重複を排除し、order の昇順（同値は y, x, z 順）で表示用リストを返す。
 */
public final class SignRegistry {

    private static final GsonComponentSerializer GSON = GsonComponentSerializer.gson();

    private final File file;
    private final Logger logger;
    private final Map<SignLocation, SignEntry> entries = new LinkedHashMap<>();

    public SignRegistry(File dataFolder, Logger logger) {
        this.file = new File(dataFolder, "signs.yml");
        this.logger = logger;
    }

    // ───────────── 参照 ─────────────

    /** order 昇順（同値は y, x, z 順）に並べた表示・連結用リスト。 */
    public List<SignEntry> ordered() {
        List<SignEntry> list = new ArrayList<>(entries.values());
        list.sort(Comparator
                .comparingInt(SignEntry::order)
                .thenComparingInt(e -> e.location().y())
                .thenComparingInt(e -> e.location().x())
                .thenComparingInt(e -> e.location().z()));
        return list;
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }

    public int size() {
        return entries.size();
    }

    public SignEntry get(SignLocation location) {
        return entries.get(location);
    }

    public boolean contains(SignLocation location) {
        return entries.containsKey(location);
    }

    /** 次の自動採番番号（現在の最大 order + 1、空なら 1）。 */
    public int nextOrder() {
        return entries.values().stream().mapToInt(SignEntry::order).max().orElse(0) + 1;
    }

    // ───────────── 更新 ─────────────

    public void put(SignEntry entry) {
        entries.put(entry.location(), entry);
    }

    public boolean remove(SignLocation location) {
        return entries.remove(location) != null;
    }

    // ───────────── 永続化 ─────────────

    public void load() {
        entries.clear();
        if (!file.exists()) {
            return;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        for (Map<?, ?> raw : yaml.getMapList("signs")) {
            try {
                UUID world = UUID.fromString(String.valueOf(raw.get("world")));
                int x = ((Number) raw.get("x")).intValue();
                int y = ((Number) raw.get("y")).intValue();
                int z = ((Number) raw.get("z")).intValue();
                Object sideRaw = raw.get("side");
                Side side = Side.valueOf(sideRaw != null ? String.valueOf(sideRaw) : "FRONT");
                Object orderRaw = raw.get("order");
                int order = orderRaw instanceof Number n ? n.intValue() : 1;
                Object sourceRaw = raw.get("source");
                SignEntry.Source source = SignEntry.Source.valueOf(
                        sourceRaw != null ? String.valueOf(sourceRaw) : "COMMAND");

                List<Component> lines = new ArrayList<>();
                if (raw.get("lines") instanceof List<?> rawLines) {
                    for (Object line : rawLines) {
                        try {
                            lines.add(GSON.deserialize(String.valueOf(line)));
                        } catch (RuntimeException ex) {
                            logger.warning("signs.yml の行を読み込めませんでした（スキップします）: " + ex.getMessage());
                        }
                    }
                }

                SignLocation location = new SignLocation(world, x, y, z, side);
                entries.put(location, new SignEntry(location, order, source, lines));
            } catch (RuntimeException ex) {
                logger.warning("signs.yml のエントリを読み込めませんでした: " + ex.getMessage());
            }
        }
    }

    public void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        List<Map<String, Object>> list = new ArrayList<>();
        for (SignEntry entry : ordered()) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("world", entry.location().worldId().toString());
            map.put("x", entry.location().x());
            map.put("y", entry.location().y());
            map.put("z", entry.location().z());
            map.put("side", entry.location().side().name());
            map.put("order", entry.order());
            map.put("source", entry.source().name());

            List<String> lines = new ArrayList<>();
            for (Component line : entry.lines()) {
                lines.add(GSON.serialize(line));
            }
            map.put("lines", lines);
            list.add(map);
        }
        yaml.set("signs", list);
        try {
            yaml.save(file);
        } catch (IOException ex) {
            logger.warning("signs.yml の保存に失敗しました: " + ex.getMessage());
        }
    }
}
