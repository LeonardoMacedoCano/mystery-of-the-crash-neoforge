package com.mysteryofthecrash.entity.brain;

import com.mysteryofthecrash.entity.AlienEntity;
import com.mysteryofthecrash.entity.goal.*;
import net.minecraft.world.entity.ai.goal.GoalSelector;

public class AlienBrain {

    private final AlienGoalAvoidThreat   avoidThreatGoal;
    private final AlienGoalFollowPlayer  followPlayerGoal;
    private final AlienGoalExplore       exploreGoal;
    private final AlienGoalPracticeSkill practiceSkillGoal;
    private final AlienGoalCollectItems  collectItemsGoal;
    private final AlienGoalRest          restGoal;

    private int commandOverrideTicks = 0;

    private static final int COMMAND_OVERRIDE_DURATION = 600;

    public AlienBrain(AlienEntity alien, GoalSelector goalSelector) {
        avoidThreatGoal   = new AlienGoalAvoidThreat(alien);
        followPlayerGoal  = new AlienGoalFollowPlayer(alien);
        exploreGoal       = new AlienGoalExplore(alien);
        practiceSkillGoal = new AlienGoalPracticeSkill(alien);
        collectItemsGoal  = new AlienGoalCollectItems(alien);
        restGoal          = new AlienGoalRest(alien);

        goalSelector.addGoal(0, avoidThreatGoal);
        goalSelector.addGoal(1, followPlayerGoal);
        goalSelector.addGoal(2, exploreGoal);
        goalSelector.addGoal(3, practiceSkillGoal);
        goalSelector.addGoal(4, collectItemsGoal);
        goalSelector.addGoal(5, restGoal);

        followPlayerGoal.setEnabled(true);
        exploreGoal.setEnabled(false);
        practiceSkillGoal.setEnabled(false);
        collectItemsGoal.setEnabled(false);
        restGoal.setEnabled(false);
    }

    public void commandFollow() {
        setAllGoals(false);
        followPlayerGoal.setEnabled(true);
        commandOverrideTicks = COMMAND_OVERRIDE_DURATION;
    }

    public void commandStay() {
        setAllGoals(false);
        commandOverrideTicks = COMMAND_OVERRIDE_DURATION;
    }

    public void tick() {
        if (commandOverrideTicks > 0) commandOverrideTicks--;
    }

    public boolean isUnderPlayerCommand() { return commandOverrideTicks > 0; }

    public void applyDecision(DecisionEngine.Action action) {
        if (commandOverrideTicks > 0) return;

        setAllGoals(false);

        switch (action) {
            case FOLLOW_PLAYER  -> followPlayerGoal.setEnabled(true);
            case EXPLORE        -> exploreGoal.setEnabled(true);
            case PRACTICE_SKILL -> practiceSkillGoal.setEnabled(true);
            case COLLECT_ITEMS,
                 ORGANIZE_AREA  -> collectItemsGoal.setEnabled(true);
            case REST           -> restGoal.setEnabled(true);
            case AVOID_THREAT   -> { }
        }
    }

    private void setAllGoals(boolean enabled) {
        followPlayerGoal.setEnabled(enabled);
        exploreGoal.setEnabled(enabled);
        practiceSkillGoal.setEnabled(enabled);
        collectItemsGoal.setEnabled(enabled);
        restGoal.setEnabled(enabled);
    }

    public boolean isFollowing()  { return followPlayerGoal.isActive(); }
    public boolean isExploring()  { return exploreGoal.isActive(); }
    public boolean isResting()    { return restGoal.isActive(); }
}
