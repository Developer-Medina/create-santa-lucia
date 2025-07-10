package com.medinadev.createsantalucia.item;

import com.medinadev.createsantalucia.CreateSantaLucia;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(CreateSantaLucia.MOD_ID);

    //register item

    public static final DeferredItem<Item> CRYSTAL_KEY = ITEMS.register("crystal_key",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> EMERALD_KEY = ITEMS.register("emerald_key",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> DIAMOND_KEY = ITEMS.register("diamond_key",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MASTER_KEY = ITEMS.register("master_key",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> SOUL_NUGGET = ITEMS.register("soul_nugget",
            () -> new Item(new Item.Properties()));


    //method
    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }


}
