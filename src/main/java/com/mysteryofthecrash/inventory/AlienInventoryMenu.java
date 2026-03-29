package com.mysteryofthecrash.inventory;

import com.mysteryofthecrash.entity.AlienEntity;
import com.mysteryofthecrash.registry.ModMenuTypes;
import com.mojang.datafixers.util.Pair;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class AlienInventoryMenu extends AbstractContainerMenu {

    public static final int EQUIP_SLOTS = 5;
    public static final int ALIEN_SLOTS = 27;

    private final AlienEntity alien;

    public AlienInventoryMenu(int containerId, Inventory playerInv, AlienEntity alien) {
        super(ModMenuTypes.ALIEN_INVENTORY.get(), containerId);
        this.alien = alien;

        addSlot(new ArmorEquipSlot(alien, EquipmentSlot.HEAD,  8,  18));
        addSlot(new ArmorEquipSlot(alien, EquipmentSlot.CHEST, 26, 18));
        addSlot(new ArmorEquipSlot(alien, EquipmentSlot.LEGS,  44, 18));
        addSlot(new ArmorEquipSlot(alien, EquipmentSlot.FEET,  62, 18));
        addSlot(new StoredToolSlot(alien,                      80, 18));

        SimpleContainer bag = alien.getInventory();
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(bag, col + row * 9, 8 + col * 18, 36 + row * 18));
            }
        }

        addPlayerSlots(playerInv);
    }

    private AlienInventoryMenu(int containerId, Inventory playerInv) {
        super(ModMenuTypes.ALIEN_INVENTORY.get(), containerId);
        this.alien = null;

        SimpleContainer dummy = new SimpleContainer(EQUIP_SLOTS + ALIEN_SLOTS);
        for (int i = 0; i < EQUIP_SLOTS + ALIEN_SLOTS; i++) {
            addSlot(new Slot(dummy, i, 0, 0));
        }

        addPlayerSlots(playerInv);
    }

    private void addPlayerSlots(Inventory playerInv) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInv, col + row * 9 + 9, 8 + col * 18, 102 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInv, col, 8 + col * 18, 160));
        }
    }

    public boolean isValid() {
        return alien != null;
    }

    public static AlienInventoryMenu fromNetwork(int containerId, Inventory playerInv, FriendlyByteBuf buf) {
        if (buf != null) {
            int entityId = buf.readInt();
            Level level = playerInv.player.level();
            if (level.getEntity(entityId) instanceof AlienEntity alien) {
                return new AlienInventoryMenu(containerId, playerInv, alien);
            }
        }
        return new AlienInventoryMenu(containerId, playerInv);
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;

        ItemStack stack    = slot.getItem();
        ItemStack original = stack.copy();

        int bagStart    = EQUIP_SLOTS;
        int bagEnd      = EQUIP_SLOTS + ALIEN_SLOTS;
        int playerStart = bagEnd;
        int playerEnd   = this.slots.size();

        if (index < EQUIP_SLOTS) {

            if (!moveItemStackTo(stack, bagStart, bagEnd, false)) {
                if (!moveItemStackTo(stack, playerStart, playerEnd, false)) {
                    return ItemStack.EMPTY;
                }
            }
        } else if (index < bagEnd) {

            if (!moveItemStackTo(stack, playerStart, playerEnd, true)) {
                return ItemStack.EMPTY;
            }
        } else {

            ItemStack toMove = slot.getItem();
            boolean moved = false;
            if (toMove.getItem() instanceof ArmorItem) {
                moved = moveItemStackTo(toMove, 0, 4, false);
            } else if (toMove.getItem() instanceof DiggerItem) {
                moved = moveItemStackTo(toMove, 4, 5, false);
            }
            if (!moved) {
                if (!moveItemStackTo(stack, bagStart, bagEnd, false)) {
                    return ItemStack.EMPTY;
                }
            }
        }

        if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();

        return original;
    }

    private static class ArmorEquipSlot extends Slot {
        private final AlienEntity alien;
        private final EquipmentSlot equipSlot;

        ArmorEquipSlot(AlienEntity alien, EquipmentSlot equipSlot, int x, int y) {
            super(new SimpleContainer(1), 0, x, y);
            this.alien     = alien;
            this.equipSlot = equipSlot;
        }

        @Override public ItemStack getItem()          { return alien.getItemBySlot(equipSlot); }
        @Override public boolean   hasItem()          { return !getItem().isEmpty(); }
        @Override public int       getMaxStackSize()  { return 1; }

        @Override
        public void set(ItemStack stack) {
            alien.setItemSlot(equipSlot, stack);
            this.setChanged();
        }

        @Override
        public ItemStack remove(int amount) {
            ItemStack current = alien.getItemBySlot(equipSlot);
            if (!current.isEmpty()) {
                alien.setItemSlot(equipSlot, ItemStack.EMPTY);
                return current;
            }
            return ItemStack.EMPTY;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return stack.getItem() instanceof ArmorItem armor
                    && armor.getEquipmentSlot() == equipSlot;
        }

        @Override
        public Pair<ResourceLocation, ResourceLocation> getNoItemIcon() {
            ResourceLocation sprite = switch (equipSlot) {
                case HEAD  -> ResourceLocation.withDefaultNamespace("item/empty_armor_slot_helmet");
                case CHEST -> ResourceLocation.withDefaultNamespace("item/empty_armor_slot_chestplate");
                case LEGS  -> ResourceLocation.withDefaultNamespace("item/empty_armor_slot_leggings");
                case FEET  -> ResourceLocation.withDefaultNamespace("item/empty_armor_slot_boots");
                default    -> null;
            };
            return sprite != null ? Pair.of(InventoryMenu.BLOCK_ATLAS, sprite) : null;
        }
    }

    private static class StoredToolSlot extends Slot {
        private final AlienEntity alien;

        StoredToolSlot(AlienEntity alien, int x, int y) {
            super(new SimpleContainer(1), 0, x, y);
            this.alien = alien;
        }

        @Override public ItemStack getItem()         { return alien.getStoredTool(); }
        @Override public boolean   hasItem()         { return !alien.getStoredTool().isEmpty(); }
        @Override public int       getMaxStackSize() { return 1; }

        @Override
        public void set(ItemStack stack) {
            alien.setStoredTool(stack);
            this.setChanged();
        }

        @Override
        public ItemStack remove(int amount) {
            ItemStack current = alien.getStoredTool();
            if (!current.isEmpty()) {
                alien.setStoredTool(ItemStack.EMPTY);
                return current;
            }
            return ItemStack.EMPTY;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return stack.getItem() instanceof DiggerItem;
        }

        @Override
        public Pair<ResourceLocation, ResourceLocation> getNoItemIcon() {

            return Pair.of(InventoryMenu.BLOCK_ATLAS,
                    ResourceLocation.withDefaultNamespace("item/empty_armor_slot_shield"));
        }
    }
}
