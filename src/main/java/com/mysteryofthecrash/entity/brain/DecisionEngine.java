package com.mysteryofthecrash.entity.brain;

import com.mysteryofthecrash.MysteryOfTheCrash;
import com.mysteryofthecrash.entity.AlienEntity;
import com.mysteryofthecrash.entity.AlienNeeds;
import com.mysteryofthecrash.entity.KnowledgeFlags;
import com.mysteryofthecrash.entity.Personality;
import com.mysteryofthecrash.entity.trust.TrustManager;

import java.util.EnumMap;
import java.util.Map;
import java.util.Random;

public class DecisionEngine {

    private final Random RNG = new Random();

    public static final int DECISION_INTERVAL = 60;

    public enum Action {
        FOLLOW_PLAYER,
        EXPLORE,
        PRACTICE_SKILL,
        COLLECT_ITEMS,
        ORGANIZE_AREA,
        REST,
        AVOID_THREAT
    }

    private Action currentAction = Action.FOLLOW_PLAYER;
    private int ticksSinceDecision = 0;

    public Action tick(AlienEntity alien) {
        ticksSinceDecision++;
        if (ticksSinceDecision < DECISION_INTERVAL) return null;
        ticksSinceDecision = 0;

        var nearP  = alien.level().getNearestPlayer(alien, 32.0);
        Action previous = currentAction;
        currentAction = evaluate(alien, nearP);

        if (currentAction != previous) {
            float logTrust = (nearP != null)
                    ? alien.getTrustManager().getTrust(nearP.getUUID())
                    : alien.getTrustManager().getHighestTrust();
            MysteryOfTheCrash.LOGGER.info("[ET/Decision] {} -> {} | Bond={} Hunger={} SocNeed={} Sleep={}",
                    previous, currentAction,
                    String.format("%.0f", logTrust),
                    String.format("%.0f", alien.getNeeds().hunger),
                    String.format("%.0f", alien.getNeeds().socialNeed),
                    String.format("%.0f", alien.getNeeds().sleepiness));
        }

        return currentAction;
    }

    public Action getCurrentAction() { return currentAction; }

    private Action evaluate(AlienEntity alien, net.minecraft.world.entity.player.Player nearestPlayer) {
        AlienNeeds   needs       = alien.getNeeds();
        Personality  personality = alien.getPersonality();
        TrustManager trust       = alien.getTrustManager();

        float trustLevel = (nearestPlayer != null)
                ? trust.getTrust(nearestPlayer.getUUID())
                : trust.getHighestTrust();
        java.util.UUID nearestId = (nearestPlayer != null) ? nearestPlayer.getUUID() : null;

        Map<Action, Float> scores = new EnumMap<>(Action.class);

        float normalizedTrust = (trustLevel + 100f) / 2f;
        float followScore = (normalizedTrust * 0.65f + needs.socialNeed * 0.35f)
                          * personality.followWeight;
        scores.put(Action.FOLLOW_PLAYER, followScore);

        float exploreScore = (needs.curiosity * 0.6f + needs.boredom * 0.4f)
                           * personality.exploreWeight;
        scores.put(Action.EXPLORE, exploreScore);

        float practiceScore = 0f;
        if (alien.hasAnyKnowledge()) {
            float skillUrge = needs.boredom * 0.5f + needs.curiosity * 0.3f;
            practiceScore = skillUrge * personality.practiceWeight;
        }
        scores.put(Action.PRACTICE_SKILL, practiceScore);

        float collectScore = 0f;
        if (alien.hasKnowledge(KnowledgeFlags.KNOWS_MINING)
                || alien.hasKnowledge(KnowledgeFlags.KNOWS_PLANTING)) {
            collectScore = needs.boredom * 0.4f * personality.helpWeight;
            boolean assists = (nearestId != null) ? trust.willAssistAutonomously(nearestId)
                    : !trust.isAvoidantToAll();
            if (assists) collectScore *= 1.5f;
        }
        scores.put(Action.COLLECT_ITEMS, collectScore);

        float organizeScore = 0f;
        if (alien.hasKnowledge(KnowledgeFlags.KNOWS_STORAGE)) {
            organizeScore = needs.boredom * 0.35f * personality.helpWeight;
        }
        scores.put(Action.ORGANIZE_AREA, organizeScore);

        float restScore = (100f - needs.safety) * 0.3f * personality.restWeight
                        + needs.sleepiness * 0.25f;
        restScore *= (1f - needs.hunger / 200f);
        scores.put(Action.REST, restScore);

        float avoidScore = needs.safety * personality.followWeight
                         * (alien.getLifeStage().ordinal() == 0 ? 2f : 1f);
        scores.put(Action.AVOID_THREAT, avoidScore);

        if (personality == Personality.CHAOTIC) {
            for (Action a : Action.values()) {
                float variance = 0.5f + RNG.nextFloat() * personality.chaosWeight;
                scores.merge(a, 0f, (old, zero) -> old * variance);
            }
        }

        boolean avoidant = (nearestId != null) ? trust.isAvoidant(nearestId) : trust.isAvoidantToAll();
        if (avoidant) {
            scores.merge(Action.FOLLOW_PLAYER, 0f, (old, zero) -> old * 0.1f);
            scores.merge(Action.EXPLORE,       0f, (old, zero) -> old * 1.4f);
        }

        return scores.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(Action.FOLLOW_PLAYER);
    }
}
