package com.mysteryofthecrash.registry;

import com.mysteryofthecrash.MysteryOfTheCrash;
import com.mysteryofthecrash.inventory.AlienInventoryMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModMenuTypes {

    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, MysteryOfTheCrash.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<AlienInventoryMenu>> ALIEN_INVENTORY =
            MENUS.register("alien_inventory",
                    () -> IMenuTypeExtension.create(AlienInventoryMenu::fromNetwork));

    public static void register(IEventBus bus) {
        MENUS.register(bus);
    }
}
