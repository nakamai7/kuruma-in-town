package com.kurumaintown;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(KurumaInTown.MODID)
public final class KurumaInTown {
    public static final String MODID = "kuruma_in_town";
    public KurumaInTown() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        TownBlocks.BLOCKS.register(bus);
        TownItems.ITEMS.register(bus);
        TownItems.TABS.register(bus);
        bus.addListener(this::commonSetup);
    }
    private void commonSetup(FMLCommonSetupEvent event) { event.enqueueWork(TownNetwork::register); }
}
