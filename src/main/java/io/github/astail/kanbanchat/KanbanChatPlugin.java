package io.github.astail.kanbanchat;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

/**
 * KanbanChat 本体。
 * 指定した看板の内容を、プレイヤーのログイン時に本人のチャットへ流す。
 * 看板の指定は「コマンド登録（/kanbanchat set）」と「マーカー行（[login]）」の両対応。
 */
public final class KanbanChatPlugin extends JavaPlugin implements Listener {

    private static final PlainTextComponentSerializer PLAIN = PlainTextComponentSerializer.plainText();

    private SignRegistry registry;
    private MarkerMatcher markerMatcher;
    private boolean active;
    private int joinDelayTicks;
    private boolean skipEmptyLines;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadSettings();

        registry = new SignRegistry(getDataFolder(), getLogger());
        registry.load();

        register();

        getLogger().info("KanbanChat を有効化しました（登録看板: " + registry.size()
                + " 枚 / 状態: " + (active ? "ON" : "OFF") + "）。");
    }

    @Override
    public void onDisable() {
        if (registry != null) {
            registry.save();
        }
    }

    /** plugin.yml のコマンドとリスナーを登録する。失敗時はプラグインを無効化する。 */
    private void register() {
        PluginCommand command = getCommand("kanbanchat");
        if (command == null) {
            getLogger().severe("plugin.yml に kanbanchat コマンドが定義されていません。プラグインを無効化します。");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        KanbanChatCommand handler = new KanbanChatCommand(this, registry);
        command.setExecutor(handler);
        command.setTabCompleter(handler);
        getServer().getPluginManager().registerEvents(this, this);
    }

    /** config.yml を読み直して設定値を反映する。 */
    private void loadSettings() {
        reloadConfig();
        FileConfiguration config = getConfig();
        active = config.getBoolean("enabled", true);
        joinDelayTicks = Math.max(0, config.getInt("join-delay-ticks", 30));
        skipEmptyLines = config.getBoolean("skip-empty-lines", true);

        List<String> markers = config.getStringList("markers");
        if (markers.isEmpty()) {
            markers = List.of("[login]", "[ログイン]");
        }
        markerMatcher = MarkerMatcher.fromTokens(markers);
    }

    // ───────────────────────── イベント ─────────────────────────

    /** ログイン時、本人のチャットへ看板メッセージを送る（少し遅らせてバニラ参加メッセージの後に出す）。 */
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (!active) {
            return;
        }
        Player player = event.getPlayer();
        if (!player.hasPermission("kanbanchat.see") || registry.isEmpty()) {
            return;
        }
        getServer().getScheduler().runTaskLater(this, () -> {
            if (!player.isOnline()) {
                return;
            }
            Component message = buildMessage();
            if (message != null) {
                player.sendMessage(message);
            }
        }, joinDelayTicks);
    }

    /** 看板の編集を検知してレジストリへ反映する（マーカーの登録・解除／コマンド登録看板の本文更新）。 */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSignChange(SignChangeEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        Side side = event.getSide();

        List<Component> lines = new ArrayList<>(4);
        for (int i = 0; i < 4; i++) {
            Component line = event.line(i);
            lines.add(line == null ? Component.empty() : line);
        }

        SignLocation location = SignLocation.of(block, side);
        MarkerMatcher.Match match = markerMatcher.match(plain(lines.get(0)));

        if (match.matched()) {
            // ── マーカー方式 ──
            if (!player.hasPermission("kanbanchat.admin")) {
                player.sendMessage(Component.text("看板をログイン掲示板にする権限がありません。", NamedTextColor.RED));
                return;
            }
            int order = (match.order() != null && match.order() >= 1)
                    ? match.order()
                    : (registry.contains(location) ? registry.get(location).order() : registry.nextOrder());
            List<Component> content = markerContent(lines);
            registry.put(new SignEntry(location, order, SignEntry.Source.MARKER, content));
            registry.save();
            player.sendMessage(Component.text(
                    "ログイン看板 #" + order + " を登録しました（マーカー方式・本文 " + content.size() + " 行）。",
                    NamedTextColor.GREEN));
            return;
        }

        // ── マーカーが無い場合 ──
        SignEntry existing = registry.get(location);
        if (existing == null) {
            return;
        }
        if (existing.source() == SignEntry.Source.MARKER) {
            // マーカーが外された → 登録解除
            registry.remove(location);
            registry.save();
            player.sendMessage(Component.text(
                    "マーカーが外れたため、ログイン看板 #" + existing.order() + " の登録を解除しました。",
                    NamedTextColor.YELLOW));
        } else {
            // コマンド登録看板 → 本文を更新
            List<Component> content = filterLines(lines);
            registry.put(existing.withLines(content));
            registry.save();
            player.sendMessage(Component.text(
                    "ログイン看板 #" + existing.order() + " の本文を更新しました（" + content.size() + " 行）。",
                    NamedTextColor.GREEN));
        }
    }

    /** 登録済みの看板が壊されたら登録を解除する。 */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (!(block.getState() instanceof Sign)) {
            return;
        }
        boolean removed = false;
        int order = -1;
        for (Side side : Side.values()) {
            SignLocation location = SignLocation.of(block, side);
            SignEntry entry = registry.get(location);
            if (entry != null) {
                order = entry.order();
                registry.remove(location);
                removed = true;
            }
        }
        if (removed) {
            registry.save();
            event.getPlayer().sendMessage(Component.text(
                    "ログイン看板 #" + order + " が壊されたため登録を解除しました。", NamedTextColor.YELLOW));
        }
    }

    // ───────────────────────── 公開ヘルパー ─────────────────────────

    /** 登録看板を order 昇順で連結したメッセージ。1 行も無ければ null。 */
    public Component buildMessage() {
        List<Component> all = new ArrayList<>();
        for (SignEntry entry : registry.ordered()) {
            all.addAll(entry.lines());
        }
        if (all.isEmpty()) {
            return null;
        }
        return Component.join(JoinConfiguration.newlines(), all);
    }

    /** 空行を取り除いた行リストを返す（skip-empty-lines が false ならそのまま）。 */
    public List<Component> filterLines(List<Component> input) {
        if (!skipEmptyLines) {
            return new ArrayList<>(input);
        }
        List<Component> out = new ArrayList<>();
        for (Component line : input) {
            if (!plain(line).isEmpty()) {
                out.add(line);
            }
        }
        return out;
    }

    /** マーカー行（1 行目）を除いた本文（2〜4 行目）を返す。 */
    public List<Component> markerContent(List<Component> fourLines) {
        List<Component> body = new ArrayList<>();
        for (int i = 1; i < fourLines.size(); i++) {
            body.add(fourLines.get(i));
        }
        return filterLines(body);
    }

    /** その行がマーカー（[login] 等）かどうか。 */
    public boolean isMarkerLine(Component line) {
        return markerMatcher.match(plain(line)).matched();
    }

    /** config / signs.yml を読み直し、ロード済みチャンクの看板を再同期する。 */
    public void reloadAll() {
        loadSettings();
        registry.load();
        resyncLoadedSigns();
    }

    /** ロード済みチャンクにある登録看板の本文をワールドから読み直す。戻り値は再同期の対象になった枚数。 */
    public int resyncLoadedSigns() {
        int changed = 0;
        for (SignEntry entry : registry.ordered()) {
            SignLocation location = entry.location();
            World world = getServer().getWorld(location.worldId());
            if (world == null || !world.isChunkLoaded(location.x() >> 4, location.z() >> 4)) {
                continue;
            }
            Block block = world.getBlockAt(location.x(), location.y(), location.z());
            if (!(block.getState() instanceof Sign sign)) {
                registry.remove(location);
                changed++;
                continue;
            }
            List<Component> lines = new ArrayList<>(4);
            for (int i = 0; i < 4; i++) {
                lines.add(sign.getSide(location.side()).line(i));
            }
            List<Component> content = entry.source() == SignEntry.Source.MARKER
                    ? markerContent(lines)
                    : filterLines(lines);
            registry.put(entry.withLines(content));
            changed++;
        }
        if (changed > 0) {
            registry.save();
        }
        return changed;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean value) {
        this.active = value;
        getConfig().set("enabled", value);
        saveConfig();
    }

    private static String plain(Component component) {
        return PLAIN.serialize(component).trim();
    }
}
