package com.mysteryofthecrash.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mysteryofthecrash.entity.AlienEntity;
import com.mysteryofthecrash.entity.KnowledgeFlags;
import com.mysteryofthecrash.entity.LifeStage;
import com.mysteryofthecrash.entity.learning.MineableBlock;
import com.mysteryofthecrash.entity.learning.MiningKnowledge;
import com.mysteryofthecrash.world.AlienWorldData;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class AlienCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("alien")
                .then(Commands.literal("status")
                    .executes(AlienCommand::showStatus))
                .then(Commands.literal("locate")
                    .executes(AlienCommand::locate))
                .then(Commands.literal("tp")
                    .executes(AlienCommand::tpToAlien))
                .then(Commands.literal("grow")
                    .executes(AlienCommand::growOneStage))
                .then(Commands.literal("setage")
                    .then(Commands.argument("days", IntegerArgumentType.integer(0, 100))
                        .executes(AlienCommand::setAge)))
                .then(Commands.literal("sethome")
                    .executes(AlienCommand::setHome))
                .then(Commands.literal("keepProgress")
                    .then(Commands.argument("value", BoolArgumentType.bool())
                        .executes(AlienCommand::setKeepProgress)))
                .then(Commands.literal("debug")
                    .executes(AlienCommand::showDebug))
        );
    }

    private static int showStatus(CommandContext<CommandSourceStack> ctx) {
        AlienEntity alien = findAlien(ctx.getSource().getLevel());
        if (alien == null) {
            ctx.getSource().sendFailure(Component.literal("§cAlien not found in the world."));
            return 0;
        }

        java.util.UUID issuerId = null;
        if (ctx.getSource().getEntity() instanceof Player issuer) {
            issuerId = issuer.getUUID();
        }
        float trust = issuerId != null
                ? alien.getTrustManager().getTrust(issuerId)
                : alien.getTrustManager().getHighestTrust();
        String trustColor = trust >= 50 ? "§a" : trust >= 0 ? "§e" : trust >= -40 ? "§6" : "§c";
        String trustLabel = trust >= 50 ? "High" : trust >= 0 ? "Neutral" : trust >= -40 ? "Distrustful" : "Avoidant (flees)";

        float obedience = issuerId != null
                ? alien.getTrustManager().getObedienceChance(issuerId) * 100
                : 50f;

        String knowledge = Arrays.stream(KnowledgeFlags.values())
                .filter(alien::hasKnowledge)
                .map(k -> k.name().replace("KNOWS_", ""))
                .collect(Collectors.joining(", "));
        if (knowledge.isEmpty()) knowledge = "None yet";

        MiningKnowledge mk = alien.getMiningKnowledge();
        String miningBreakdown = mk.getKnownBlocks().isEmpty()
                ? "§7None"
                : mk.getKnownBlocks().stream()
                        .map(b -> "§f" + b.id + " §8(" + String.format("%.0f", mk.getProficiency(b)) + "§8)")
                        .collect(Collectors.joining("§7, "));

        String command = alien.getAlienBrain() != null && alien.getAlienBrain().isUnderPlayerCommand()
                ? "§aYes (active)" : "§7No (autonomous)";

        net.minecraft.world.item.ItemStack tool = alien.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND);
        String toolStr = tool.isEmpty() ? "§7None" : "§f" + tool.getItem().getDescriptionId().replace("item.minecraft.", "").replace("_", " ");

        String stageAbilities = "§7Mining: " + (alien.getLifeStage().canMine ? "§a✓" : "§c✗")
                + " §7Gear: " + (alien.getLifeStage().canEquipGear ? "§a✓" : "§c✗");

        net.minecraft.core.BlockPos crash = alien.getCrashSitePos();
        String crashStr = crash != null ? "§f" + crash.getX() + ", " + crash.getY() + ", " + crash.getZ() : "§7Unknown";
        net.minecraft.core.BlockPos home = alien.getHomePos();
        String homeStr = home != null ? "§f" + home.getX() + ", " + home.getY() + ", " + home.getZ() : "§7Not set";

        String sleepStr = String.format("%.0f%%", alien.getNeeds().sleepiness);

        String msg = "§b══ Alien Status ══\n"
                + "§7Life stage: §f" + alien.getLifeStage().name() + "  " + stageAbilities + "\n"
                + "§7Personality: §f" + alien.getPersonality().name() + "\n"
                + "§7Health: §f" + String.format("%.1f / %.1f", alien.getHealth(), alien.getMaxHealth()) + "\n"
                + "§7Bond with you: " + trustColor + trustLabel
                    + " §8(" + String.format("%.1f", trust) + "§8)\n"
                + "§7Obedience: §f" + String.format("%.0f%%", obedience) + "\n"
                + "§7Hunger: §f" + String.format("%.0f%%", alien.getNeeds().hunger) + "\n"
                + "§7Sleepiness: §f" + sleepStr + "\n"
                + "§7Curiosity: §f" + String.format("%.0f%%", alien.getNeeds().curiosity) + "\n"
                + "§7Social need: §f" + String.format("%.0f%%", alien.getNeeds().socialNeed) + "\n"
                + "§7Safety: §f" + String.format("%.0f%%", alien.getNeeds().safety) + "\n"
                + "§7Knowledge: §f" + knowledge + "\n"
                + "§7Mining known: " + miningBreakdown + "\n"
                + "§7Tool held: " + toolStr + "\n"
                + "§7Following player command: " + command + "\n"
                + "§7Spawn (crash site): " + crashStr + "\n"
                + "§7Home: " + homeStr + "\n"
                + "§7Position: §f" + (int)alien.getX() + ", " + (int)alien.getY() + ", " + (int)alien.getZ();

        ctx.getSource().sendSuccess(() -> Component.literal(msg), false);
        return 1;
    }

    private static int locate(CommandContext<CommandSourceStack> ctx) {
        AlienEntity alien = findAlien(ctx.getSource().getLevel());
        if (alien == null) {
            ctx.getSource().sendFailure(Component.literal("§cAlien not found in the world."));
            return 0;
        }

        double dist = ctx.getSource().getPosition().distanceTo(alien.position());
        ctx.getSource().sendSuccess(() -> Component.literal(
                "§bAlien is at: §f" + (int)alien.getX() + ", " + (int)alien.getY() + ", " + (int)alien.getZ()
                + " §7(§f" + String.format("%.0f", dist) + " §7blocks from you)"
        ), false);
        return 1;
    }

    private static int tpToAlien(CommandContext<CommandSourceStack> ctx) {
        AlienEntity alien = findAlien(ctx.getSource().getLevel());
        if (alien == null) {
            ctx.getSource().sendFailure(Component.literal("§cAlien not found in the world."));
            return 0;
        }

        if (ctx.getSource().getEntity() instanceof Player player) {
            player.teleportTo(alien.getX(), alien.getY(), alien.getZ());
            ctx.getSource().sendSuccess(() -> Component.literal(
                    "§bTeleported to alien at §f"
                    + (int)alien.getX() + ", " + (int)alien.getY() + ", " + (int)alien.getZ()
            ), false);
        } else {
            ctx.getSource().sendFailure(Component.literal("§cOnly players can use this command."));
        }
        return 1;
    }

    private static int growOneStage(CommandContext<CommandSourceStack> ctx) {
        AlienEntity alien = findAlien(ctx.getSource().getLevel());
        if (alien == null) {
            ctx.getSource().sendFailure(Component.literal("§cAlien not found."));
            return 0;
        }

        LifeStage current = alien.getLifeStage();
        LifeStage next = switch (current) {
            case CHILD -> LifeStage.YOUNG;
            case YOUNG -> LifeStage.ADULT;
            case ADULT -> null;
        };

        if (next == null) {
            ctx.getSource().sendSuccess(() ->
                    Component.literal("§eAlien is already ADULT — maximum stage reached."), false);
            return 1;
        }

        ServerLevel level = ctx.getSource().getLevel();
        long thresholdDay = (next == LifeStage.YOUNG) ? 7L : 21L;
        long syntheticBirthDay = (level.getGameTime() / 24000L) - thresholdDay;
        alien.setBirthDay(syntheticBirthDay);

        ctx.getSource().sendSuccess(() ->
                Component.literal("§bAlien advanced: §f" + current + " §b→ §f" + next
                        + " §7(transition occurs within 1 second)"), false);
        return 1;
    }

    private static int setAge(CommandContext<CommandSourceStack> ctx) {
        AlienEntity alien = findAlien(ctx.getSource().getLevel());
        if (alien == null) {
            ctx.getSource().sendFailure(Component.literal("§cAlien not found."));
            return 0;
        }

        int days = IntegerArgumentType.getInteger(ctx, "days");
        ServerLevel level = ctx.getSource().getLevel();
        long currentDay = level.getGameTime() / 24000L;
        long syntheticBirthDay = currentDay - days;
        alien.setBirthDay(syntheticBirthDay);

        LifeStage expected = days < 7 ? LifeStage.CHILD : days < 21 ? LifeStage.YOUNG : LifeStage.ADULT;
        ctx.getSource().sendSuccess(() ->
                Component.literal("§bAlien age set to §f" + days + " days§b."
                        + " Expected stage: §f" + expected
                        + " §7(transition occurs within 1 second)"), false);
        return 1;
    }

    private static int setHome(CommandContext<CommandSourceStack> ctx) {
        AlienEntity alien = findAlien(ctx.getSource().getLevel());
        if (alien == null) {
            ctx.getSource().sendFailure(Component.literal("§cAlien not found."));
            return 0;
        }

        net.minecraft.core.BlockPos pos = alien.blockPosition();
        alien.setHomePos(pos);

        ctx.getSource().sendSuccess(() -> Component.literal(
                "§bAlien home set to §f" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ()
                + " §7(alien will return here after mining sessions)"
        ), false);
        return 1;
    }

    private static int setKeepProgress(CommandContext<CommandSourceStack> ctx) {
        boolean value = BoolArgumentType.getBool(ctx, "value");
        AlienWorldData data = AlienWorldData.get(ctx.getSource().getLevel());
        data.setRetainProgressOnDeath(value);
        ctx.getSource().sendSuccess(() -> Component.literal(
                "§bAlien death behavior: §f" + (value
                        ? "retain all progress on respawn"
                        : "reset to baby on respawn")
        ), false);
        return 1;
    }

    private static int showDebug(CommandContext<CommandSourceStack> ctx) {
        AlienEntity alien = findAlien(ctx.getSource().getLevel());
        if (alien == null) {
            ctx.getSource().sendFailure(Component.literal("§cAlien not found."));
            return 0;
        }

        java.util.UUID issuerId = null;
        if (ctx.getSource().getEntity() instanceof Player issuer) {
            issuerId = issuer.getUUID();
        }
        float trust = issuerId != null
                ? alien.getTrustManager().getTrust(issuerId)
                : alien.getTrustManager().getHighestTrust();

        var brain = alien.getAlienBrain();
        String mode = "AUTONOMOUS";
        if (brain != null) {
            if (brain.isMining())             mode = "MINE";
            else if (brain.isReturningHome()) mode = "RETURN_HOME";
            else if (brain.isFollowing())     mode = "FOLLOW_PLAYER";
            else if (brain.isExploring())     mode = "EXPLORE";
            else if (brain.isResting())       mode = "REST";
        }

        var needs = alien.getNeeds();
        MiningKnowledge mk = alien.getMiningKnowledge();

        String knownFlags = Arrays.stream(KnowledgeFlags.values())
                .filter(alien::hasKnowledge)
                .map(k -> k.name().replace("KNOWS_", ""))
                .collect(Collectors.joining(", "));
        if (knownFlags.isEmpty()) knownFlags = "none";

        String miningProf = mk.getKnownBlocks().isEmpty()
                ? "none"
                : mk.getKnownBlocks().stream()
                        .map(b -> b.id + "=" + String.format("%.0f", mk.getProficiency(b)) + "%")
                        .collect(Collectors.joining(", "));

        net.minecraft.world.item.ItemStack stored = alien.getStoredTool();
        String toolStr = stored.isEmpty() ? "none"
                : stored.getItem().getDescriptionId().replace("item.minecraft.", "").replace("_", " ");

        int usedSlots = 0;
        var inv = alien.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (!inv.getItem(i).isEmpty()) usedSlots++;
        }

        String msg = "§b══ Alien Debug ══\n"
                + "§7Bond: §f" + String.format("%.1f", trust)
                + "  §7Hunger: §f" + String.format("%.0f", needs.hunger)
                + "  §7Boredom: §f" + String.format("%.0f", needs.boredom) + "\n"
                + "§7Social: §f" + String.format("%.0f", needs.socialNeed)
                + "  §7Safety: §f" + String.format("%.0f", needs.safety)
                + "  §7Sleep: §f" + String.format("%.0f", needs.sleepiness) + "\n"
                + "§7Personality: §f" + alien.getPersonality()
                + "  §7Stage: §f" + alien.getLifeStage()
                + "  §7Mode: §f" + mode + "\n"
                + "§7Known: §f" + knownFlags + "\n"
                + "§7Mining: §f" + miningProf + "\n"
                + "§7Tool: §f" + toolStr + "  §7Inventory: §f" + usedSlots + "/27";

        ctx.getSource().sendSuccess(() -> Component.literal(msg), false);
        return 1;
    }

    private static AlienEntity findAlien(ServerLevel level) {
        List<AlienEntity> found = level.getEntitiesOfClass(
                AlienEntity.class,
                new AABB(-30_000_000, level.getMinBuildHeight(),
                         -30_000_000,  30_000_000, level.getMaxBuildHeight(), 30_000_000));
        return found.isEmpty() ? null : found.get(0);
    }
}
