package com.medinadev.createsantalucia.item;

import com.medinadev.createsantalucia.CreateSantaLucia;
import com.medinadev.createsantalucia.block.ModBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModCreativeModeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TAB =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, CreateSantaLucia.MOD_ID);

    public static final Supplier<CreativeModeTab> CREATE_SANTA_LUCIA_ITEMS = CREATIVE_MODE_TAB.register("create_santa_lucia_items_tab",
            () -> CreativeModeTab.builder().icon(() -> new ItemStack(ModItems.MASTER_KEY.get()))
                    .title(Component.translatable("creativetab.createsantalucia.create_santa_lucia_items"))
                    .displayItems((itemDisplayParameters, output) -> {
                        output.accept(ModItems.CRYSTAL_KEY);
                        output.accept(ModItems.EMERALD_KEY);
                        output.accept(ModItems.DIAMOND_KEY);
                        output.accept(ModItems.MASTER_KEY);
                        output.accept(ModItems.SOUL_NUGGET);
                    }).build());

    public static final Supplier<CreativeModeTab> CREATE_SANTA_LUCIA_BLOCKS = CREATIVE_MODE_TAB.register("create_santa_lucia_blocks_tab",
            () -> CreativeModeTab.builder().icon(() -> new ItemStack(ModBlocks.CHARCOAL_BLOCK.get()))
                    .withTabsBefore(ResourceLocation.fromNamespaceAndPath(CreateSantaLucia.MOD_ID, "create_santa_lucia_items_tab"))
                    .title(Component.translatable("creativetab.createsantalucia.create_santa_lucia_blocks"))
                    .displayItems((itemDisplayParameters, output) -> {
                        output.accept(ModBlocks.CHARCOAL_BLOCK);
                    }).build());




    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TAB.register(eventBus);
    }

}
