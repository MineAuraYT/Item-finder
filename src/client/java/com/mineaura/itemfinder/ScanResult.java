package com.mineaura.itemfinder;

import net.minecraft.core.BlockPos;

public record ScanResult(String containerName, BlockPos position, String itemName, int count) { }
