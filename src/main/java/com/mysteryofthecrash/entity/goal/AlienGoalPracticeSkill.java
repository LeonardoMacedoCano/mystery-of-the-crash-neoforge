package com.mysteryofthecrash.entity.goal;

import com.mysteryofthecrash.entity.AlienEntity;
import com.mysteryofthecrash.entity.KnowledgeFlags;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

public class AlienGoalPracticeSkill extends Goal {

    private final AlienEntity alien;
    private boolean enabled = false;
    private int ticksRunning = 0;
    private int maxDuration = 100;

    public AlienGoalPracticeSkill(AlienEntity alien) {
        this.alien = alien;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    public void setEnabled(boolean e) { this.enabled = e; }

    @Override
    public boolean canUse() {
        if (!enabled) return false;
        return alien.hasAnyKnowledge();
    }

    @Override
    public boolean canContinueToUse() {
        return enabled && ticksRunning < maxDuration;
    }

    @Override
    public void start() {
        ticksRunning = 0;
        float avgSkill = (alien.getLearner().miningSkill
                        + alien.getLearner().farmingSkill
                        + alien.getLearner().logisticsSkill) / 3f;
        maxDuration = (int)(60 + avgSkill * 1.4f);
    }

    @Override
    public void tick() {
        ticksRunning++;

        if (ticksRunning % 40 == 0) {
            alien.getLearner().onExperiment(alien);

            if (alien.hasKnowledge(KnowledgeFlags.KNOWS_MINING)) {
                alien.getLearner().miningSkill = Math.min(100f,
                        alien.getLearner().miningSkill + 0.3f);
            }
            if (alien.hasKnowledge(KnowledgeFlags.KNOWS_PLANTING)) {
                alien.getLearner().farmingSkill = Math.min(100f,
                        alien.getLearner().farmingSkill + 0.3f);
            }
            if (alien.hasKnowledge(KnowledgeFlags.KNOWS_STORAGE)) {
                alien.getLearner().logisticsSkill = Math.min(100f,
                        alien.getLearner().logisticsSkill + 0.3f);
            }
        }
    }
}
