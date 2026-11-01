package tn.nightbeam.betterwitchhuts.platform;

import java.nio.file.Path;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;

/** What common code needs from the loader. Each loader passes an implementation to {@code Witchbound.init}. */
public interface Platform {
    Path configDir();

    /** {@code new MenuType<>(...)}: the constructor is only opened up on the loaders. */
    <T extends AbstractContainerMenu> MenuType<T> menuType(MenuFactory<T> factory);

    @FunctionalInterface
    interface MenuFactory<T extends AbstractContainerMenu> {
        T create(int containerId, Inventory inventory);
    }
}
