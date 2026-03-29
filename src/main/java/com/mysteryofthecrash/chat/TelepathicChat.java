package com.mysteryofthecrash.chat;

import com.mysteryofthecrash.entity.AlienEntity;
import com.mysteryofthecrash.entity.LifeStage;
import com.mysteryofthecrash.entity.Personality;
import com.mysteryofthecrash.entity.learning.MineableBlock;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

public class TelepathicChat {

    private static final Random RNG = new Random();

    private static final String[] CHILD_MESSAGES = {
        "...◈...",
        "▲ ▲ warm?",
        "§o...here... yes...",
        "◈ ◈ ◈",
        "...you... friend?",
        "§o[something like fear]",
        "...bright... hurts...",
        "◈ remember... nothing.",
        "...cold before. warm now.",
        "§o[images: fire, stars, falling]"
    };

    private static final String[] YOUNG_MESSAGES = {
        "I want to see what is there.",
        "Something is wrong nearby. I feel it.",
        "I learned something today.",
        "You were gone too long.",
        "I am trying to understand this place.",
        "Show me again. I will remember.",
        "I do not like it when you do that.",
        "There is danger close. Be careful.",
        "I collected something. It felt right.",
        "I rested. I feel better now."
    };

    private static final Map<Personality, String[]> ADULT_MESSAGES = Map.of(
        Personality.CURIOUS, new String[]{
            "Did you notice the pattern in those caves? Fascinating.",
            "I've been mapping everything. There's so much I still don't know.",
            "What does this do? I want to try.",
            "I went further than usual. Don't worry — I found something interesting."
        },
        Personality.LOYAL, new String[]{
            "I stayed close. I didn't want you to be alone.",
            "You looked tired. I kept watch.",
            "Wherever you go, I'll go. That's just how it is.",
            "I noticed you were in danger. I didn't like that feeling."
        },
        Personality.INDEPENDENT, new String[]{
            "I handled it myself. You didn't need to be involved.",
            "I'll decide what to do next. Trust me.",
            "I appreciate the suggestion. I'm going to do it differently.",
            "I've been thinking. There's a better way."
        },
        Personality.CHAOTIC, new String[]{
            "I moved some things around. It made more sense that way.",
            "Rules are just starting points. I improved on them.",
            "You looked too comfortable. I fixed that.",
            "Interesting what happens when you rearrange everything at once."
        },
        Personality.LAZY, new String[]{
            "I was going to do that. Later.",
            "It can wait. Everything can wait.",
            "I found a good spot. I'm going to stay here a while.",
            "You seem very busy. That's fine. I'm not."
        },
        Personality.PROTECTIVE, new String[]{
            "There were monsters nearby last night. I dealt with it.",
            "Stay behind me when we go there. It's not safe.",
            "I'm watching the perimeter. You focus on your work.",
            "I don't like how close that was. Please be more careful."
        }
    );

    private static final Map<String, String> TRANSITION_MESSAGES = Map.of(
        "young_transition", "§o...something has settled inside me. I know what I am now.",
        "adult_transition", "§o I understand this world now. I'll decide my own path."
    );

    public void sendRandomMessage(LivingEntity alien, LifeStage stage, Personality personality) {
        String message = pickMessage(stage, personality);
        broadcastToNearby(alien, message);
    }

    public void sendTransitionMessage(LivingEntity alien, String key, Personality personality) {
        String base = TRANSITION_MESSAGES.getOrDefault(key, "...");
        broadcastToNearby(alien, base);
    }

    public void sendDirectMessage(LivingEntity alien, String message) {
        broadcastToNearby(alien, message);
    }

    public boolean interpretPlayerMessage(AlienEntity alien, Player player,
                                          String message, LifeStage stage,
                                          Personality personality) {
        String lower = message.toLowerCase();

        if (lower.contains("come") || lower.contains("follow") || lower.contains("here")) {

            if (alien.getTrustManager().isAvoidantToAll()) {
                respondTo(alien, stage, personality,
                        "◈ ...no.",
                        "I don't want to.",
                        "You don't get to tell me what to do.");
            } else {
                if (alien.getAlienBrain() != null) {
                    alien.getAlienBrain().commandFollow(player);
                }
                respondTo(alien, stage, personality,
                        "...coming.",
                        "Okay. I'm coming.",
                        "On my way.");
            }
            return true;
        }

        if (lower.contains("stay") || lower.contains("wait") || lower.contains("stop")) {

            if (alien.getAlienBrain() != null) {
                alien.getAlienBrain().commandStay();
            }
            respondTo(alien, stage, personality,
                    "...still.",
                    "I'll wait here.",
                    "Fine. I'll stay.");
            return true;
        }

        if (lower.startsWith("mining:")) {
            String rest = lower.substring(7).trim();
            String[] parts = rest.split("\\s+", 2);
            String blockId = parts[0];
            int durationSeconds = 300;
            if (parts.length > 1) {
                try { durationSeconds = Integer.parseInt(parts[1]); } catch (NumberFormatException ignored) {}
            }
            handleMineCommand(alien, player, stage, personality, blockId, durationSeconds * 20);
            return true;
        }

        if (lower.equals("mining")) {
            handleMineList(alien, player, stage, personality);
            return true;
        }

        if (lower.contains("mine") || lower.contains("dig")) {
            respondTo(alien, stage, personality,
                    "◈ ...break?",
                    "I can try that.",
                    "I know how to do that. I'll handle it.");
            return true;
        }

        if (lower.contains("farm") || lower.contains("plant") || lower.contains("grow")) {
            respondTo(alien, stage, personality,
                    "...seeds?",
                    "I've seen you do that. I can try.",
                    "I'll take care of the planting.");
            return true;
        }

        if (lower.contains("help")) {
            respondTo(alien, stage, personality,
                    "◈ ◈",
                    "I want to help. Tell me how.",
                    "What do you need?");
            return true;
        }

        if (lower.contains("eat") || lower.contains("food") || lower.contains("hungry")) {
            respondTo(alien, stage, personality,
                    "...hungry... yes.",
                    "Yes. I'm hungry.",
                    "I was going to say something about that.");
            return true;
        }

        return false;
    }

    private void handleMineList(AlienEntity alien, Player player,
                                LifeStage stage, Personality personality) {
        if (!alien.getLifeStage().canMine) {
            respondTo(alien, stage, personality,
                    "◈ ...not yet.",
                    "I'm not ready for that.",
                    "Later.");
            return;
        }

        List<MineableBlock> known = alien.getMiningKnowledge().getKnownBlocks();

        if (known.isEmpty()) {
            respondTo(alien, stage, personality,
                    "◈ ...nothing.",
                    "I haven't learned to mine anything yet.",
                    "I don't know enough about mining.");
            return;
        }

        String blockList = known.stream()
                .map(b -> b.id + " (" + String.format("%.0f", alien.getMiningKnowledge().getProficiency(b)) + ")")
                .collect(Collectors.joining(", "));

        if (player instanceof ServerPlayer sp) {
            sp.sendSystemMessage(Component.literal("§b[Telepathy] §7Known blocks: §f" + blockList));
        }
    }

    private void handleMineCommand(AlienEntity alien, Player player,
                                   LifeStage stage, Personality personality,
                                   String blockId, int durationTicks) {
        if (!alien.getLifeStage().canMine) {
            respondTo(alien, stage, personality,
                    "◈ ...not yet.",
                    "I'm not ready for that.",
                    "Later.");
            return;
        }

        Optional<MineableBlock> opt = MineableBlock.fromId(blockId);
        if (opt.isEmpty()) {
            if (player instanceof ServerPlayer sp) {
                sp.sendSystemMessage(Component.literal(
                        "§c[Telepathy] Unknown block: §f" + blockId
                        + " §7— try: mining"));
            }
            return;
        }

        MineableBlock target = opt.get();

        if (!alien.getMiningKnowledge().isKnown(target)) {
            respondTo(alien, stage, personality,
                    "◈ ...don't know that.",
                    "I haven't learned to mine " + target.displayName + " yet.",
                    "My proficiency with " + target.displayName + " isn't there yet.");
            return;
        }

        if (!(alien.level() instanceof ServerLevel)) return;

        if (alien.getStoredTool().isEmpty()) {
            respondTo(alien, stage, personality,
                    "◈ ...need tool.",
                    "I need a pickaxe for that. Give me one.",
                    "I don't have a mining tool. Give me one first.");
            return;
        }

        if (alien.getAlienBrain() != null) {
            alien.getAlienBrain().commandMine(target, durationTicks);
        }

        int seconds = durationTicks / 20;
        respondTo(alien, stage, personality,
                "...break.",
                "On it. Mining " + target.displayName + " for " + seconds + "s.",
                "I'll handle it. " + seconds + " seconds.");
    }

    private String pickMessage(LifeStage stage, Personality personality) {
        return switch (stage) {
            case CHILD -> CHILD_MESSAGES[RNG.nextInt(CHILD_MESSAGES.length)];
            case YOUNG -> YOUNG_MESSAGES[RNG.nextInt(YOUNG_MESSAGES.length)];
            case ADULT -> {
                String[] pool = ADULT_MESSAGES.getOrDefault(personality, YOUNG_MESSAGES);
                yield pool[RNG.nextInt(pool.length)];
            }
        };
    }

    private void respondTo(LivingEntity alien, LifeStage stage, Personality personality,
                            String childReply, String youngReply, String adultReply) {
        String reply = switch (stage) {
            case CHILD -> childReply;
            case YOUNG -> youngReply;
            case ADULT -> adultReply;
        };
        broadcastToNearby(alien, reply);
    }

    private void broadcastToNearby(LivingEntity alien, String message) {
        if (!(alien.level() instanceof ServerLevel serverLevel)) return;

        List<ServerPlayer> nearby = serverLevel.getPlayers(
                p -> p.distanceTo(alien) <= 32.0f);

        Component component = Component.literal("§b[Telepathy] §r" + message);
        for (ServerPlayer player : nearby) {
            player.sendSystemMessage(component);
        }
    }
}
