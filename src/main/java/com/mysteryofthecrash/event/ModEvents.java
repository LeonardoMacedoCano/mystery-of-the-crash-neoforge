package com.mysteryofthecrash.event;

import com.mysteryofthecrash.MysteryOfTheCrash;
import com.mysteryofthecrash.command.AlienCommand;
import com.mysteryofthecrash.entity.AlienEntity;
import com.mysteryofthecrash.registry.ModEntities;
import com.mysteryofthecrash.world.AlienWorldData;
import com.mysteryofthecrash.world.CrashSiteGenerator;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

import java.util.UUID;

public class ModEvents {

    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.ALIEN.get(), AlienEntity.createAttributes().build());
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        AlienCommand.register(event.getDispatcher());
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        ServerLevel overworld = event.getServer().overworld();
        AlienWorldData data = AlienWorldData.get(overworld);

        if (!data.isCrashSiteGenerated()) {
            MysteryOfTheCrash.LOGGER.info("[ModEvents] First start — generating crash site.");
            CrashSiteGenerator.generate(overworld);
        } else {
            ensureAlienExists(overworld, data);
        }
    }

    private static void ensureAlienExists(ServerLevel level, AlienWorldData data) {
        UUID uuid = data.getAlienUUID();
        if (uuid == null) {
            MysteryOfTheCrash.LOGGER.warn("[ModEvents] No alien UUID in world data — regenerating.");
            CrashSiteGenerator.generate(level);
            return;
        }

        Entity entity = level.getEntity(uuid);
        if (entity instanceof AlienEntity alien && alien.isAlive()) {
            MysteryOfTheCrash.LOGGER.info("[ModEvents] Alien found alive: {}", uuid);
            return;
        }

        if (data.isNeedsRespawn() || entity == null || !entity.isAlive()) {
            MysteryOfTheCrash.LOGGER.info("[ModEvents] Alien missing — respawning at {}",
                    data.getRespawnPos());
            spawnAlienAt(level, data);
        }
    }

    private static void spawnAlienAt(ServerLevel level, AlienWorldData data) {
        AlienEntity alien = ModEntities.ALIEN.get().create(level);
        if (alien == null) return;

        var pos = data.getRespawnPos();
        alien.setCrashSitePos(data.getCrashSitePos());
        alien.moveTo(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, 0f, 0f);
        alien.finalizeSpawn(level,
                level.getCurrentDifficultyAt(pos),
                net.minecraft.world.entity.MobSpawnType.EVENT,
                null);
        level.addFreshEntity(alien);
        data.setAlienUUID(alien.getUUID());
        data.setNeedsRespawn(false);
        data.setDirty();
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) return;
        Player player = event.getPlayer();

        findNearbyAlien(serverLevel, player, 12.0).ifPresent(alien ->
            alien.getLearner().onPlayerBreakBlock(alien,
                    event.getState().getBlock(), player, event.getPos()));
    }

    @SubscribeEvent
    public static void onContainerOpen(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide()) return;
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) return;

        var be = serverLevel.getBlockEntity(event.getPos());
        if (!(be instanceof ChestBlockEntity) && !(be instanceof BarrelBlockEntity)) return;

        findNearbyAlien(serverLevel, event.getEntity(), 12.0).ifPresent(alien ->
            alien.getLearner().onPlayerAccessStorage(alien, event.getEntity()));
    }

    @SubscribeEvent
    public static void onPlayerCraft(PlayerEvent.ItemCraftedEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!(player.level() instanceof ServerLevel serverLevel)) return;

        findNearbyAlien(serverLevel, player, 12.0).ifPresent(alien ->
            alien.getLearner().onPlayerCraft(alien, player, null));
    }

    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) return;

        findNearbyAlien(serverLevel, player, 12.0).ifPresent(alien ->
            alien.getLearner().onPlayerFarm(alien, event.getState().getBlock(), player));
    }

    @SubscribeEvent
    public static void onPlayerChat(ServerChatEvent event) {
        ServerLevel level = event.getPlayer().serverLevel();
        Player player = event.getPlayer();
        String message = event.getMessage().getString();

        findNearbyAlien(level, player, 32.0).ifPresent(alien ->
            alien.getTelepathicChat().interpretPlayerMessage(
                    alien, player, message,
                    alien.getLifeStage(), alien.getPersonality()));
    }

    @SubscribeEvent
    public static void onEntityDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof AlienEntity alien)) return;
        if (event.getEntity().level().isClientSide()) return;

        ServerLevel level = (ServerLevel) event.getEntity().level();
        AlienWorldData data = AlienWorldData.get(level);
        data.setNeedsRespawn(true);
        data.setDirty();
        MysteryOfTheCrash.LOGGER.info("[ModEvents] Alien died — flagged for respawn at {}",
                data.getRespawnPos());
    }

    private static java.util.Optional<AlienEntity> findNearbyAlien(
            ServerLevel level, Player player, double radius) {
        return level.getEntitiesOfClass(AlienEntity.class,
                player.getBoundingBox().inflate(radius))
                .stream()
                .findFirst();
    }
}
