package com.kurumaintown;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class TownBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, KurumaInTown.MODID);
    public static final RegistryObject<Block> GAS_STATION = BLOCKS.register("gas_station", () -> new GasStationBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(2.0F).noOcclusion()));
    private TownBlocks() {}
}
