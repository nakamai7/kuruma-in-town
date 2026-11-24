package com.kurumaintown;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class TownItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, KurumaInTown.MODID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, KurumaInTown.MODID);
    public static final RegistryObject<Item> TABLET = ITEMS.register("tablet", () -> new TabletItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> GARAGE_ADMIN = ITEMS.register("garage_admin", () -> new GarageAdminItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> GARAGE = ITEMS.register("garage", () -> new GarageItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> CUSTOM_TOOL = ITEMS.register("custom_tool", () -> new CustomToolItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> REPAIR_KIT = ITEMS.register("repair_kit", () -> new RepairKitItem(new Item.Properties().stacksTo(16)));
    public static final RegistryObject<Item> COLOR_SPRAY = ITEMS.register("color_spray", () -> new ColorSprayItem(new Item.Properties().stacksTo(16)));
    public static final RegistryObject<Item> GAS_STATION = ITEMS.register("gas_station", () -> new BlockItem(TownBlocks.GAS_STATION.get(), new Item.Properties().stacksTo(16)));
    public static final RegistryObject<CreativeModeTab> TAB = TABS.register("items", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.kuruma_in_town.items"))
            .icon(() -> TABLET.get().getDefaultInstance())
            .displayItems((parameters, output) -> { output.accept(TABLET.get()); output.accept(REPAIR_KIT.get()); output.accept(CUSTOM_TOOL.get()); output.accept(GARAGE.get()); output.accept(GARAGE_ADMIN.get()); output.accept(COLOR_SPRAY.get()); output.accept(GAS_STATION.get()); })
            .build());
    private TownItems() {}
}
