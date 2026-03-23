package com.mysteryofthecrash.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mysteryofthecrash.entity.AlienEntity;
import com.mysteryofthecrash.entity.KnowledgeFlags;
import com.mysteryofthecrash.entity.LifeStage;
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
        );
    }

    private static int showStatus(CommandContext<CommandSourceStack> ctx) {
        AlienEntity alien = findAlien(ctx.getSource().getLevel());
        if (alien == null) {
            ctx.getSource().sendFailure(Component.literal("§cAlien not found in the world."));
            return 0;
        }

        float trust = alien.getTrustManager().getTrust();
        String trustColor = trust >= 50 ? "§a" : trust >= 0 ? "§e" : trust >= -40 ? "§6" : "§c";
        String trustLabel = trust >= 50 ? "High" : trust >= 0 ? "Neutral" : trust >= -40 ? "Distrustful" : "Avoidant (flees)";

        String knowledge = Arrays.stream(KnowledgeFlags.values())
                .filter(alien::hasKnowledge)
                .map(k -> k.name().replace("KNOWS_", ""))
                .collect(Collectors.joining(", "));
        if (knowledge.isEmpty()) knowledge = "None yet";

        String command = alien.getAlienBrain() != null && alien.getAlienBrain().isUnderPlayerCommand()
                ? "§aYes (active)" : "§7No (autonomous)";

        String msg = "§b══ Alien Status ══\n"
                + "§7Life stage: §f" + alien.getLifeStage().name() + "\n"
                + "§7Personality: §f" + alien.getPersonality().name() + "\n"
                + "§7Health: §f" + String.format("%.1f / %.1f", alien.getHealth(), alien.getMaxHealth()) + "\n"
                + "§7Bond: " + trustColor + trustLabel
                    + " §8(" + String.format("%.1f", trust) + "§8)\n"
                + "§7Obedience: §f" + String.format("%.0f%%", alien.getTrustManager().getObedienceChance() * 100) + "\n"
                + "§7Hunger: §f" + String.format("%.0f%%", alien.getNeeds().hunger) + "\n"
                + "§7Curiosity: §f" + String.format("%.0f%%", alien.getNeeds().curiosity) + "\n"
                + "§7Social need: §f" + String.format("%.0f%%", alien.getNeeds().socialNeed) + "\n"
                + "§7Safety: §f" + String.format("%.0f%%", alien.getNeeds().safety) + "\n"
                + "§7Knowledge: §f" + knowledge + "\n"
                + "§7Following player command: " + command + "\n"
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

    private static AlienEntity findAlien(ServerLevel level) {
        List<AlienEntity> found = level.getEntitiesOfClass(
                AlienEntity.class,
                new AABB(-30_000_000, level.getMinBuildHeight(),
                         -30_000_000,  30_000_000, level.getMaxBuildHeight(), 30_000_000));
        return found.isEmpty() ? null : found.get(0);
    }
}
