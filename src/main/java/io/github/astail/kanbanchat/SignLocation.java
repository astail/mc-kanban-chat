package io.github.astail.kanbanchat;

import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.sign.Side;

import java.util.UUID;

/**
 * 登録された看板の位置（ワールド・座標・面）を表す不変キー。
 * Map のキーとして使うため record の equals/hashCode に依存する。
 */
public record SignLocation(UUID worldId, int x, int y, int z, Side side) {

    public static SignLocation of(Block block, Side side) {
        return new SignLocation(block.getWorld().getUID(), block.getX(), block.getY(), block.getZ(), side);
    }

    /** 一覧表示用に「ワールド名 (x, y, z, FRONT)」の形で返す。 */
    public String shortCoords(Server server) {
        World world = server.getWorld(worldId);
        String name = world != null ? world.getName() : worldId.toString();
        return name + " (" + x + ", " + y + ", " + z + ", " + side + ")";
    }
}
