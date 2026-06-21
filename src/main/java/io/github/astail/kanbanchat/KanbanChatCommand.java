package io.github.astail.kanbanchat;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.block.sign.SignSide;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/** /kanbanchat コマンドの実処理とタブ補完。 */
public final class KanbanChatCommand implements CommandExecutor, TabCompleter {

    private static final PlainTextComponentSerializer PLAIN = PlainTextComponentSerializer.plainText();
    private static final int REACH = 6;

    private final KanbanChatPlugin plugin;
    private final SignRegistry registry;

    public KanbanChatCommand(KanbanChatPlugin plugin, SignRegistry registry) {
        this.plugin = plugin;
        this.registry = registry;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendStatus(sender);
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "set" -> requirePlayer(sender, player -> doSet(player, args));
            case "remove", "unset", "delete" -> requirePlayer(sender, player -> doRemove(player, args));
            case "list" -> doList(sender);
            case "test", "preview" -> doTest(sender);
            case "reload" -> doReload(sender);
            case "on" -> setActive(sender, true);
            case "off" -> setActive(sender, false);
            case "status" -> sendStatus(sender);
            default -> sendUsage(sender);
        }
        return true;
    }

    // ───────────── サブコマンド ─────────────

    private void doSet(Player player, String[] args) {
        Sign sign = targetSign(player);
        if (sign == null) {
            player.sendMessage(error("登録したい看板を " + REACH + " ブロック以内で見ながら実行してください。"));
            return;
        }
        Side side = pickSide(sign, findSideArg(args));
        SignLocation location = SignLocation.of(sign.getBlock(), side);

        List<Component> raw = readSide(sign, side);
        List<Component> content = plugin.isMarkerLine(raw.get(0))
                ? plugin.markerContent(raw)
                : plugin.filterLines(raw);
        if (content.isEmpty()) {
            player.sendMessage(error("その面（" + side + "）には文字が書かれていません。先に看板へ文字を書いてください。"));
            return;
        }

        Integer number = findNumber(args);
        if (number != null && number < 1) {
            player.sendMessage(error("番号は 1 以上を指定してください。"));
            return;
        }
        int order = (number != null)
                ? number
                : (registry.contains(location) ? registry.get(location).order() : registry.nextOrder());

        registry.put(new SignEntry(location, order, SignEntry.Source.COMMAND, content));
        registry.save();
        player.sendMessage(ok("ログイン看板 #" + order + " を登録しました（コマンド方式・" + side
                + " 面・本文 " + content.size() + " 行）。"));
    }

    private void doRemove(Player player, String[] args) {
        Integer number = findNumber(args);
        if (number != null) {
            List<SignLocation> targets = new ArrayList<>();
            for (SignEntry entry : registry.ordered()) {
                if (entry.order() == number) {
                    targets.add(entry.location());
                }
            }
            if (targets.isEmpty()) {
                player.sendMessage(error("番号 #" + number + " の看板は登録されていません。"));
                return;
            }
            for (SignLocation location : targets) {
                registry.remove(location);
            }
            registry.save();
            player.sendMessage(ok("ログイン看板 #" + number + " を " + targets.size() + " 枚解除しました。"));
            return;
        }

        Sign sign = targetSign(player);
        if (sign == null) {
            player.sendMessage(error("解除したい看板を見ながら実行するか、/kanbanchat remove <番号> で指定してください。"));
            return;
        }
        boolean removed = false;
        int order = -1;
        for (Side side : Side.values()) {
            SignLocation location = SignLocation.of(sign.getBlock(), side);
            SignEntry entry = registry.get(location);
            if (entry != null) {
                order = entry.order();
                registry.remove(location);
                removed = true;
            }
        }
        if (removed) {
            registry.save();
            player.sendMessage(ok("ログイン看板 #" + order + " の登録を解除しました。"));
        } else {
            player.sendMessage(error("その看板は登録されていません。"));
        }
    }

    private void doList(CommandSender sender) {
        if (registry.isEmpty()) {
            sender.sendMessage(info("登録された看板はありません。"));
            return;
        }
        sender.sendMessage(info("登録看板（" + registry.size() + " 枚）— order 昇順で連結して送信します:"));
        for (SignEntry entry : registry.ordered()) {
            String first = entry.lines().isEmpty()
                    ? "(本文なし)"
                    : PLAIN.serialize(entry.lines().get(0));
            sender.sendMessage(Component.text(
                    "  #" + entry.order() + " [" + entry.source() + "] "
                            + entry.location().shortCoords(plugin.getServer()) + " : " + first,
                    NamedTextColor.GRAY));
        }
    }

    private void doTest(CommandSender sender) {
        Component message = plugin.buildMessage();
        if (message == null) {
            sender.sendMessage(info("登録された看板がありません。"));
            return;
        }
        sender.sendMessage(info("── ログインメッセージ プレビュー ──"));
        sender.sendMessage(message);
    }

    private void doReload(CommandSender sender) {
        plugin.reloadAll();
        sender.sendMessage(ok("設定と看板を再読み込みしました（登録看板: " + registry.size() + " 枚）。"));
    }

    private void setActive(CommandSender sender, boolean value) {
        plugin.setActive(value);
        sender.sendMessage(ok("ログインメッセージを " + (value ? "ON" : "OFF") + " にしました。"));
    }

    private void sendStatus(CommandSender sender) {
        sender.sendMessage(info("KanbanChat: " + (plugin.isActive() ? "ON" : "OFF")
                + " / 登録看板 " + registry.size() + " 枚"));
        sendUsage(sender);
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(info("/kanbanchat set [番号] [front|back]  - 見ている看板を登録"));
        sender.sendMessage(info("/kanbanchat remove [番号]            - 看板の登録を解除（番号 or 視線）"));
        sender.sendMessage(info("/kanbanchat list                     - 登録一覧"));
        sender.sendMessage(info("/kanbanchat test                     - 自分にプレビュー表示"));
        sender.sendMessage(info("/kanbanchat reload                   - 設定 / 看板を再読込"));
        sender.sendMessage(info("/kanbanchat on | off | status        - 全体の有効 / 無効 / 状態"));
    }

    // ───────────── 補助 ─────────────

    private void requirePlayer(CommandSender sender, Consumer<Player> action) {
        if (sender instanceof Player player) {
            action.accept(player);
        } else {
            sender.sendMessage(error("このサブコマンドは、プレイヤーが看板を見ながら実行してください。"));
        }
    }

    private Sign targetSign(Player player) {
        Block block = player.getTargetBlockExact(REACH);
        if (block != null && block.getState() instanceof Sign sign) {
            return sign;
        }
        return null;
    }

    private List<Component> readSide(Sign sign, Side side) {
        SignSide signSide = sign.getSide(side);
        List<Component> lines = new ArrayList<>(4);
        for (int i = 0; i < 4; i++) {
            lines.add(signSide.line(i));
        }
        return lines;
    }

    private Side pickSide(Sign sign, String explicit) {
        if (explicit != null) {
            return explicit.equalsIgnoreCase("back") ? Side.BACK : Side.FRONT;
        }
        if (isSideEmpty(sign, Side.FRONT) && !isSideEmpty(sign, Side.BACK)) {
            return Side.BACK;
        }
        return Side.FRONT;
    }

    private boolean isSideEmpty(Sign sign, Side side) {
        SignSide signSide = sign.getSide(side);
        for (int i = 0; i < 4; i++) {
            if (!PLAIN.serialize(signSide.line(i)).trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private Integer findNumber(String[] args) {
        for (int i = 1; i < args.length; i++) {
            try {
                return Integer.parseInt(args[i]);
            } catch (NumberFormatException ignored) {
                // 数字以外はスキップ
            }
        }
        return null;
    }

    private String findSideArg(String[] args) {
        for (int i = 1; i < args.length; i++) {
            if (args[i].equalsIgnoreCase("front") || args[i].equalsIgnoreCase("back")) {
                return args[i];
            }
        }
        return null;
    }

    private static Component ok(String text) {
        return Component.text(text, NamedTextColor.GREEN);
    }

    private static Component error(String text) {
        return Component.text(text, NamedTextColor.RED);
    }

    private static Component info(String text) {
        return Component.text(text, NamedTextColor.GRAY);
    }

    // ───────────── タブ補完 ─────────────

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return prefix(List.of("set", "remove", "list", "test", "reload", "on", "off", "status"), args[0]);
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        if (sub.equals("set")) {
            if (args.length == 2) {
                List<String> options = new ArrayList<>();
                options.add(String.valueOf(registry.nextOrder()));
                options.add("front");
                options.add("back");
                return prefix(options, args[1]);
            }
            if (args.length == 3) {
                return prefix(List.of("front", "back"), args[2]);
            }
        }
        if ((sub.equals("remove") || sub.equals("unset") || sub.equals("delete")) && args.length == 2) {
            List<String> numbers = new ArrayList<>();
            for (SignEntry entry : registry.ordered()) {
                numbers.add(String.valueOf(entry.order()));
            }
            return prefix(numbers, args[1]);
        }
        return List.of();
    }

    private static List<String> prefix(List<String> options, String token) {
        String lower = token.toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        for (String option : options) {
            if (option.toLowerCase(Locale.ROOT).startsWith(lower)) {
                out.add(option);
            }
        }
        return out;
    }
}
