package org.essentialss.implementation.world;

import org.essentialss.api.world.SWorldData;
import org.essentialss.api.world.points.warp.SWarp;
import org.essentialss.api.world.points.warp.SWarpBuilder;
import org.essentialss.implementation.EssentialsSMain;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.api.Sponge;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.hocon.HoconConfigurationLoader;
import org.spongepowered.math.vector.Vector3d;

import java.io.File;

public class SWorldDataSerializer {

    public static void load(@NotNull SWorldData worldData) throws ConfigurateException {
        File folder = Sponge.configManager().pluginConfig(EssentialsSMain.plugin().container()).directory().toFile();
        File file = new File(folder, worldData.identifier() + ".conf");
        HoconConfigurationLoader loader = HoconConfigurationLoader.builder().file(file).build();
        CommentedConfigurationNode root = loader.load();

        root.node("points").childrenList().parallelStream().forEach(node -> {
            String type = node.node("type").getString();
            if (null == type) {
                return;
            }
            double x = node.node("x").getDouble();
            double y = node.node("y").getDouble();
            double z = node.node("z").getDouble();
            if (type.equalsIgnoreCase("Warp")) {
                String warpName = node.node("name").getString();
                if (null == warpName) {
                    return;
                }
                worldData.register(new SWarpBuilder().name(warpName).point(new Vector3d(x, y, z)), false, null);
                return;
            }
        });
    }

    public static void save(@NotNull SWorldData worldData) throws ConfigurateException {
        File folder = Sponge.configManager().pluginConfig(EssentialsSMain.plugin().container()).directory().toFile();
        File file = new File(folder, worldData.identifier() + ".conf");
        HoconConfigurationLoader loader = HoconConfigurationLoader.builder().file(file).build();
        CommentedConfigurationNode root = loader.load();

        CommentedConfigurationNode pointNode = root.node("points");
        for (SWarp warp : worldData.warps()) {
            CommentedConfigurationNode points = pointNode.appendListNode();
            points.node("name").set(warp.identifier());
            points.node("type").set("Warp");
            points.node("x").set(warp.position().x());
            points.node("y").set(warp.position().y());
            points.node("z").set(warp.position().z());
        }

        loader.save(root);

    }

}
