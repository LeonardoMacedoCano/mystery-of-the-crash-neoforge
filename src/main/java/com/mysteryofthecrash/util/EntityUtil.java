package com.mysteryofthecrash.util;

import com.mysteryofthecrash.entity.AlienEntity;
import com.mysteryofthecrash.world.AlienWorldData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.util.Optional;
import java.util.UUID;

public final class EntityUtil {

    private EntityUtil() {}

    public static Optional<AlienEntity> findAlienGlobal(ServerLevel level) {
        UUID uuid = AlienWorldData.get(level).getAlienUUID();
        if (uuid == null) return Optional.empty();
        Entity entity = level.getEntity(uuid);
        if (entity instanceof AlienEntity alien && alien.isAlive()) {
            return Optional.of(alien);
        }
        return Optional.empty();
    }

    public static Optional<AlienEntity> findNearbyAlien(ServerLevel level, Player player, double radius) {
        return level.getEntitiesOfClass(AlienEntity.class,
                player.getBoundingBox().inflate(radius))
                .stream()
                .findFirst();
    }
}
