package com.mysteryofthecrash.entity.brain;

import com.mysteryofthecrash.entity.AlienEntity;
import com.mysteryofthecrash.entity.goal.*;
import com.mysteryofthecrash.entity.learning.MineableBlock;
import com.mysteryofthecrash.util.ChatUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.player.Player;

public class AlienBrain {

    private final AlienEntity            alien;
    private final AlienGoalEscapeWater   escapeWaterGoal;
    private final AlienGoalAvoidThreat   avoidThreatGoal;
    private final AlienGoalFollowPlayer  followPlayerGoal;
    private final AlienGoalEat           eatGoal;
    private final AlienGoalMineBlock     mineBlockGoal;
    private final AlienGoalReturnHome    returnHomeGoal;
    private final AlienGoalExplore       exploreGoal;
    private final AlienGoalPracticeSkill practiceSkillGoal;
    private final AlienGoalCollectItems  collectItemsGoal;
    private final AlienGoalRest          restGoal;

    private int commandOverrideTicks = 0;

    private static final int   COMMAND_OVERRIDE_DURATION = 600;
    private static final float TP_FOLLOW_THRESHOLD       = 50.0f;

    public AlienBrain(AlienEntity alien, GoalSelector goalSelector) {
        this.alien = alien;
        escapeWaterGoal   = new AlienGoalEscapeWater(alien);
        avoidThreatGoal   = new AlienGoalAvoidThreat(alien);
        followPlayerGoal  = new AlienGoalFollowPlayer(alien);
        eatGoal           = new AlienGoalEat(alien);
        mineBlockGoal     = new AlienGoalMineBlock(alien);
        returnHomeGoal    = new AlienGoalReturnHome(alien);
        exploreGoal       = new AlienGoalExplore(alien);
        practiceSkillGoal = new AlienGoalPracticeSkill(alien);
        collectItemsGoal  = new AlienGoalCollectItems(alien);
        restGoal          = new AlienGoalRest(alien);

        goalSelector.addGoal(0, escapeWaterGoal);  
        goalSelector.addGoal(0, avoidThreatGoal);
        goalSelector.addGoal(1, followPlayerGoal);
        goalSelector.addGoal(1, eatGoal);           
        goalSelector.addGoal(1, mineBlockGoal);
        goalSelector.addGoal(1, returnHomeGoal);
        goalSelector.addGoal(2, exploreGoal);
        goalSelector.addGoal(3, practiceSkillGoal);
        goalSelector.addGoal(4, collectItemsGoal);
        goalSelector.addGoal(5, restGoal);

        followPlayerGoal.setEnabled(true);
        eatGoal.setEnabled(true);               
        exploreGoal.setEnabled(false);
        practiceSkillGoal.setEnabled(false);
        collectItemsGoal.setEnabled(false);
        restGoal.setEnabled(false);
    }

    public void commandFollow(Player requester) {
        setAllGoals(false);
        followPlayerGoal.setEnabled(true);
        eatGoal.setEnabled(true);
        commandOverrideTicks = COMMAND_OVERRIDE_DURATION;

        if (requester != null && alien.distanceTo(requester) > TP_FOLLOW_THRESHOLD) {
            alien.teleportTo(requester.getX(), requester.getY(), requester.getZ());
            alien.getTelepathicChat().sendDirectMessage(alien,
                ChatUtil.pickByStage(alien.getLifeStage(), "◈ ...!", "Coming to you.", "On my way. Teleporting."));
        }
    }

    public void commandStay() {
        setAllGoals(false);
        eatGoal.setEnabled(true);
        commandOverrideTicks = COMMAND_OVERRIDE_DURATION;
    }

    public void commandMine(MineableBlock block, int durationTicks) {
        setAllGoals(false);
        mineBlockGoal.startMining(block, durationTicks);
        commandOverrideTicks = durationTicks + 100;
    }

    public void commandReturnHome(int sessionStartY, BlockPos target) {
        setAllGoals(false);
        returnHomeGoal.startReturning(sessionStartY, target);
        eatGoal.setEnabled(true);

    }

    public void commandRelease() {
        commandOverrideTicks = 0;
    }

    public void tick() {
        if (commandOverrideTicks > 0) commandOverrideTicks--;
    }

    public boolean isUnderPlayerCommand() { return commandOverrideTicks > 0; }

    public void applyDecision(DecisionEngine.Action action) {
        if (commandOverrideTicks > 0) return;

        setAllGoals(false);
        eatGoal.setEnabled(true); 

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
        eatGoal.setEnabled(enabled);
        exploreGoal.setEnabled(enabled);
        practiceSkillGoal.setEnabled(enabled);
        collectItemsGoal.setEnabled(enabled);
        restGoal.setEnabled(enabled);
        if (!enabled) {
            returnHomeGoal.cancel();
            mineBlockGoal.cancel(); 
        }
    }

    public boolean isFollowing()     { return followPlayerGoal.isActive(); }
    public boolean isExploring()     { return exploreGoal.isActive(); }
    public boolean isResting()       { return restGoal.isActive(); }
    public boolean isMining()        { return mineBlockGoal.isRunning(); }
    public boolean isReturningHome() { return returnHomeGoal.isRunning(); }
}
