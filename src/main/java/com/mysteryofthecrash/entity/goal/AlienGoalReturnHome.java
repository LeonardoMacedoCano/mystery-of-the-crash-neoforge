package com.mysteryofthecrash.entity.goal;

import com.mysteryofthecrash.MysteryOfTheCrash;
import com.mysteryofthecrash.entity.AlienEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

public class AlienGoalReturnHome extends Goal {

    private enum Phase { FLY_UP, NAVIGATE }

    private static final double ARRIVED_DIST_SQ = 3.5 * 3.5;
    private static final double LIFT_FORCE       = 0.18;
    private static final double MAX_LIFT_SPEED   = 0.55;
    private static final int    NAVIGATE_RETRY   = 40;
    private static final int    FLY_BELOW_THRESH = 2;

    private final AlienEntity alien;
    private boolean  running     = false;
    private Phase    phase       = Phase.NAVIGATE;
    private int      surfaceY    = 0;
    private int      tickCounter = 0;
    private BlockPos target      = null;

    public AlienGoalReturnHome(AlienEntity alien) {
        this.alien = alien;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    public void startReturning(int sessionStartY, BlockPos returnTarget) {
        this.running     = true;
        this.surfaceY    = sessionStartY;
        this.target      = returnTarget;
        this.tickCounter = 0;
        this.phase = (alien.getBlockY() < sessionStartY - FLY_BELOW_THRESH)
                ? Phase.FLY_UP : Phase.NAVIGATE;
        MysteryOfTheCrash.LOGGER.info("[AlienHome] Return triggered — currentY={}, surfaceY={}, target={}, phase={}",
                alien.getBlockY(), surfaceY, returnTarget, phase);
    }

    public void cancel() { this.running = false; }
    public boolean isRunning() { return running; }

    @Override public boolean canUse()           { return running && target != null; }
    @Override public boolean canContinueToUse() { return running && target != null; }

    @Override
    public void start() {
        tickCounter = 0;
        if (phase == Phase.NAVIGATE) navigateTo(target);
    }

    @Override
    public void stop() {
        running = false;
        alien.setNoGravity(false);
        alien.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (!running || target == null) { running = false; return; }
        tickCounter++;
        switch (phase) {
            case FLY_UP   -> tickFlyUp();
            case NAVIGATE -> tickNavigate();
        }
    }

    private void tickFlyUp() {
        if (alien.getBlockY() >= surfaceY - 1) {
            alien.setNoGravity(false);
            phase = Phase.NAVIGATE;
            tickCounter = 0;
            navigateTo(target);
            MysteryOfTheCrash.LOGGER.info("[AlienHome] Reached surface Y={}, navigating to {}", surfaceY, target);
            return;
        }
        alien.setNoGravity(true);
        alien.getNavigation().stop();
        Vec3 motion = alien.getDeltaMovement();
        alien.setDeltaMovement(
                motion.x * 0.4,
                Math.min(motion.y + LIFT_FORCE, MAX_LIFT_SPEED),
                motion.z * 0.4);
    }

    private void tickNavigate() {
        if (alien.blockPosition().distSqr(target) <= ARRIVED_DIST_SQ) {
            running = false;
            alien.getNavigation().stop();
            MysteryOfTheCrash.LOGGER.info("[AlienHome] Arrived at {}", target);
            return;
        }
        if (tickCounter % NAVIGATE_RETRY == 0 && alien.getNavigation().isDone()) {
            navigateTo(target);
        }
    }

    private void navigateTo(BlockPos pos) {
        alien.getNavigation().moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 1.0);
    }
}
