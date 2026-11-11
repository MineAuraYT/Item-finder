package com.mineaura.itemfinder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/** Scans already-loaded server chunks in an integrated single-player world. */
public final class ContainerScanner {
    public static final int DEFAULT_RADIUS = 50;

    private ContainerScanner() { }

    public static void scan(MinecraftServer server, net.minecraft.resources.ResourceKey<Level> dimension,
                            BlockPos origin, String query, int radius,
                            Consumer<List<ScanResult>> callback) {
        server.execute(() -> {
            ServerLevel level = server.getLevel(dimension);
            if (level == null) {
                callback.accept(List.of());
                return;
            }

            String needle = query.trim().toLowerCase(Locale.ROOT);
            if (needle.isEmpty()) {
                callback.accept(List.of());
                return;
            }

            int minChunkX = Math.floorDiv(origin.getX() - radius, 16);
            int maxChunkX = Math.floorDiv(origin.getX() + radius, 16);
            int minChunkZ = Math.floorDiv(origin.getZ() - radius, 16);
            int maxChunkZ = Math.floorDiv(origin.getZ() + radius, 16);
            double radiusSquared = (double) radius * radius;
            List<ScanResult> found = new ArrayList<>();

            for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
                for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                    LevelChunk chunk = level.getChunkSource().getChunkNow(chunkX, chunkZ);
                    if (chunk == null) continue; // Never force-load chunks.

                    for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
                        if (!(blockEntity instanceof Container container)) continue;
                        if (!isSupportedContainer(blockEntity)) continue;

                        BlockPos pos = blockEntity.getBlockPos();
                        double dx = pos.getX() + 0.5 - (origin.getX() + 0.5);
                        double dy = pos.getY() + 0.5 - (origin.getY() + 0.5);
                        double dz = pos.getZ() + 0.5 - (origin.getZ() + 0.5);
                        if (dx * dx + dy * dy + dz * dz > radiusSquared) continue;

                        int total = 0;
                        String matchedName = null;
                        for (int slot = 0; slot < container.getContainerSize(); slot++) {
                            ItemStack stack = container.getItem(slot);
                            if (stack.isEmpty() || !matches(stack, needle)) continue;
                            total += stack.getCount();
                            if (matchedName == null) matchedName = stack.getHoverName().getString();
                        }

                        if (total > 0) {
                            found.add(new ScanResult(containerName(blockEntity), pos.immutable(), matchedName, total));
                        }
                    }
                }
            }

            found.sort(Comparator.comparingDouble(result -> result.position().distToCenterSqr(
                    origin.getX() + 0.5, origin.getY() + 0.5, origin.getZ() + 0.5)));
            callback.accept(List.copyOf(found));
        });
    }

    private static boolean isSupportedContainer(BlockEntity entity) {
        return entity instanceof ChestBlockEntity
                || entity instanceof BarrelBlockEntity
                || entity instanceof ShulkerBoxBlockEntity;
    }

    private static String containerName(BlockEntity entity) {
        if (entity instanceof ChestBlockEntity) return "Chest";
        if (entity instanceof BarrelBlockEntity) return "Barrel";
        if (entity instanceof ShulkerBoxBlockEntity) return "Shulker Box";
        return "Container";
    }

    private static boolean matches(ItemStack stack, String query) {
        String registryName = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        String displayName = stack.getHoverName().getString();
        return registryName.toLowerCase(Locale.ROOT).contains(query)
                || displayName.toLowerCase(Locale.ROOT).contains(query);
    }
}
