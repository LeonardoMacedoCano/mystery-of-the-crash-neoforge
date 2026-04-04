package com.mysteryofthecrash.event;

import com.mysteryofthecrash.MysteryOfTheCrash;
import com.mysteryofthecrash.command.AlienCommand;
import com.mysteryofthecrash.entity.AlienEntity;
import com.mysteryofthecrash.registry.ModEntities;
import com.mysteryofthecrash.util.BlockUtil;
import com.mysteryofthecrash.util.EntityUtil;
import com.mysteryofthecrash.chat.PlayerCommands;
import com.mysteryofthecrash.world.AlienWorldData;
import com.mysteryofthecrash.world.CrashSiteGenerator;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.nbt.CompoundTag;
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

        bootstrapAlienChunk(event.getServer(), overworld, data);
    }

    private static void bootstrapAlienChunk(net.minecraft.server.MinecraftServer server,
            ServerLevel overworld, AlienWorldData data) {
        BlockPos lastPos = data.getLastKnownPos();
        if (lastPos.equals(BlockPos.ZERO)) lastPos = data.getCrashSitePos();
        if (lastPos.equals(BlockPos.ZERO)) return;

        ChunkPos bootChunk = new ChunkPos(lastPos);
        overworld.getChunkSource().addRegionTicket(TicketType.FORCED, bootChunk, 2, bootChunk);
        MysteryOfTheCrash.LOGGER.info("[ModEvents] Bootstrap force-loading chunk {} for alien", bootChunk);

        server.tell(new net.minecraft.server.TickTask(server.getTickCount() + 60,
                () -> overworld.getChunkSource().removeRegionTicket(TicketType.FORCED, bootChunk, 2, bootChunk)));
    }

    @SubscribeEvent
    public static void onEntityJoinLevel(net.neoforged.neoforge.event.entity.EntityJoinLevelEvent event) {
        if (!(event.getEntity() instanceof AlienEntity alien)) return;
        if (event.getLevel().isClientSide()) return;
        ServerLevel level = (ServerLevel) event.getLevel();
        AlienWorldData data = AlienWorldData.get(level);
        UUID activeUUID = data.getAlienUUID();
        if (activeUUID != null && !alien.getUUID().equals(activeUUID)) {
            MysteryOfTheCrash.LOGGER.warn("[ModEvents] Orphaned AlienEntity blocked from joining: {}", alien.getUUID());
            event.setCanceled(true);
        }
    }

    private static void ensureAlienExists(ServerLevel level, AlienWorldData data) {
        UUID uuid = data.getAlienUUID();
        if (uuid == null) {
            MysteryOfTheCrash.LOGGER.warn("[ModEvents] No alien UUID in world data — regenerating.");
            CrashSiteGenerator.generate(level);
            return;
        }

        if (EntityUtil.findAlienGlobal(level).isPresent()) {
            MysteryOfTheCrash.LOGGER.info("[ModEvents] Alien found alive: {}", uuid);
            killOrphanedAliens(level, uuid);
            return;
        }

        if (data.isNeedsRespawn()) {
            MysteryOfTheCrash.LOGGER.info("[ModEvents] Alien missing — respawning at {}",
                    data.getRespawnPos());
            spawnAlienAt(level, data);
        }
        killOrphanedAliens(level, uuid);
    }

    private static void killOrphanedAliens(ServerLevel level, UUID activeUUID) {
        for (Entity e : level.getAllEntities()) {
            if (e instanceof AlienEntity orphan
                    && orphan.isAlive()
                    && !orphan.getUUID().equals(activeUUID)) {
                MysteryOfTheCrash.LOGGER.warn("[ModEvents] Removing orphaned AlienEntity: {}", orphan.getUUID());
                orphan.remove(Entity.RemovalReason.DISCARDED);
            }
        }
    }

    private static void spawnAlienAt(ServerLevel level, AlienWorldData data) {
        if (EntityUtil.findAlienGlobal(level).isPresent()) {
            MysteryOfTheCrash.LOGGER.warn("[ModEvents] spawnAlienAt aborted — alien already exists.");
            return;
        }
        AlienEntity alien = ModEntities.ALIEN.get().create(level);
        if (alien == null) return;

        var pos = data.getRespawnPos();
        alien.setCrashSitePos(data.getCrashSitePos());
        int safeY = BlockUtil.getSafeYBlocking(level, pos);
        alien.moveTo(pos.getX() + 0.5, safeY, pos.getZ() + 0.5, 0f, 0f);
        alien.finalizeSpawn(level,
                level.getCurrentDifficultyAt(pos),
                net.minecraft.world.entity.MobSpawnType.EVENT,
                null);

        if (data.isRetainProgressOnDeath() && data.getSavedAlienProgress() != null) {
            CompoundTag wrapper = new CompoundTag();
            wrapper.put("AlienData", data.getSavedAlienProgress());
            alien.readAdditionalSaveData(wrapper);

            alien.moveTo(pos.getX() + 0.5, safeY, pos.getZ() + 0.5, 0f, 0f);
            alien.setHealth(alien.getMaxHealth());
            data.setSavedAlienProgress(null);
            MysteryOfTheCrash.LOGGER.info("[ModEvents] Alien respawned with full progression retained.");
        }

        level.addFreshEntity(alien);
        data.setAlienUUID(alien.getUUID());
        data.setNeedsRespawn(false);
        data.setDirty();
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) return;
        Player player = event.getPlayer();

        EntityUtil.findNearbyAlien(serverLevel, player, 12.0).ifPresent(alien ->
            alien.getLearner().onPlayerBreakBlock(alien,
                    event.getState().getBlock(), player, event.getPos()));
    }

    @SubscribeEvent
    public static void onContainerOpen(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide()) return;
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) return;

        var be = serverLevel.getBlockEntity(event.getPos());
        if (!(be instanceof ChestBlockEntity) && !(be instanceof BarrelBlockEntity)) return;

        EntityUtil.findNearbyAlien(serverLevel, event.getEntity(), 12.0).ifPresent(alien ->
            alien.getLearner().onPlayerAccessStorage(alien, event.getEntity()));
    }

    @SubscribeEvent
    public static void onPlayerCraft(PlayerEvent.ItemCraftedEvent event) {
        Player player = event.getEntity();
        if (!(player.level() instanceof ServerLevel serverLevel)) return;

        EntityUtil.findNearbyAlien(serverLevel, player, 12.0).ifPresent(alien ->
            alien.getLearner().onPlayerCraft(alien, player, null));
    }

    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) return;

        EntityUtil.findNearbyAlien(serverLevel, player, 12.0).ifPresent(alien ->
            alien.getLearner().onPlayerFarm(alien, event.getState().getBlock(), player));
    }

    @SubscribeEvent
    public static void onPlayerChat(ServerChatEvent event) {
        String message = event.getMessage().getString();
        String lower = message.toLowerCase().trim();
        if (!lower.equals(PlayerCommands.FOLLOW)
                && !lower.equals(PlayerCommands.STAY)
                && !lower.equals(PlayerCommands.MINE_LIST)
                && !lower.startsWith(PlayerCommands.MINE_PREFIX)) return;

        event.setCanceled(true);

        ServerLevel level = event.getPlayer().serverLevel();
        Player player = event.getPlayer();
        var server = player.getServer();
        assert server != null;

        net.minecraft.network.chat.Component playerDisplay =
                net.minecraft.network.chat.Component.literal("<")
                        .append(player.getDisplayName())
                        .append(net.minecraft.network.chat.Component.literal("> " + message));

        for (var p : server.getPlayerList().getPlayers()) {
            p.sendSystemMessage(playerDisplay);
        }

        server.tell(new net.minecraft.server.TickTask(
                server.getTickCount() + 1,
                () -> EntityUtil.findAlienGlobal(level).ifPresent(alien ->
                        alien.getTelepathicChat().interpretPlayerMessage(
                                alien, player, message,
                                alien.getLifeStage(), alien.getPersonality()))));
    }

    @SubscribeEvent
    public static void onEntityDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof AlienEntity alien)) return;
        if (event.getEntity().level().isClientSide()) return;

        ServerLevel level = (ServerLevel) event.getEntity().level();
        AlienWorldData data = AlienWorldData.get(level);

        if (!alien.getUUID().equals(data.getAlienUUID())) {
            MysteryOfTheCrash.LOGGER.warn("[ModEvents] Orphaned AlienEntity died — ignoring: {}", alien.getUUID());
            return;
        }

        data.setNeedsRespawn(true);

        if (data.isRetainProgressOnDeath()) {
            CompoundTag wrapper = new CompoundTag();
            alien.addAdditionalSaveData(wrapper);
            if (wrapper.contains("AlienData")) {
                data.setSavedAlienProgress(wrapper.getCompound("AlienData"));
                MysteryOfTheCrash.LOGGER.info("[ModEvents] Alien progress saved for respawn.");
            }
        }

        data.setDirty();
        MysteryOfTheCrash.LOGGER.info("[ModEvents] Alien died — flagged for respawn at {}",
                data.getRespawnPos());

        var server = event.getEntity().getServer();
        if (server != null) {
            server.tell(new net.minecraft.server.TickTask(
                    server.getTickCount() + 100,
                    () -> {
                        AlienWorldData freshData = AlienWorldData.get(level);
                        if (freshData.isNeedsRespawn() && EntityUtil.findAlienGlobal(level).isEmpty()) {
                            spawnAlienAt(level, freshData);
                        }
                    }));
        }

        net.minecraft.network.chat.Component deathMsg = net.minecraft.network.chat.Component.literal(
                "§c[Telepathy] §7...the signal faded. The alien is gone. It will return.");
        for (var player : level.players()) {
            player.sendSystemMessage(deathMsg);
        }
    }
}
