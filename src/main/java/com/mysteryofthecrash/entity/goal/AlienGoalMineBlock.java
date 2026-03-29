package com.mysteryofthecrash.entity.goal;

import com.mysteryofthecrash.MysteryOfTheCrash;
import com.mysteryofthecrash.entity.AlienEntity;
import com.mysteryofthecrash.entity.learning.MineableBlock;
import com.mysteryofthecrash.util.BlockUtil;
import com.mysteryofthecrash.util.ChatUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.tags.FluidTags;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static net.minecraft.core.component.DataComponents.ENCHANTMENTS;

public class AlienGoalMineBlock extends Goal {

    private enum Phase { SEARCH, NAVIGATE, DIG_DOWN, CLEAR_PATH, MINE }

    private static final float  MULT_CORRECT_TOOL  = 30.0f;
    private static final float  MULT_WRONG_TOOL    = 100.0f;
    private static final float  WRONG_TOOL_PENALTY = 5.0f;

    private static final double REACH_SQ                 = 2.0 * 2.0;

    private static final int    STUCK_THRESHOLD    = 60;

    private static final int    MAX_CLEAR_ATTEMPTS = 4;

    private static final int    DIG_THRESHOLD      = 2;

    private static final int    DROP_LIMIT         = 15;

    private static final int    CLUSTER_FAIL_RADIUS = 5;

    private static final int    CLUSTER_Y_RANGE    = 20;

    private static final int    SEARCH_RADIUS_MIN  = 5;
    private static final int    SEARCH_RADIUS_STEP = 5;
    private static final int    SEARCH_RADIUS_MAX  = 100;
    private static final int    SEARCH_TICK_DELAY  = 4;

    private static final int    SCAN_BATCH         = 2000;

    private final AlienEntity alien;

    private MineableBlock miningType     = null;
    private BlockPos      currentTarget  = null;
    private BlockPos      clearTarget    = null;
    private BlockPos      activeBreakPos = null;  
    private Phase         phase          = Phase.SEARCH;
    private boolean       running        = false;
    private int           breakTimer     = 0;
    private int           stuckTimer     = 0;
    private int           clearStuck     = 0;
    private int           clearAttempts  = 0;
    private int           durationTicks  = 0;
    private int           elapsedTicks   = 0;
    private int           mineCount      = 0;
    private int           searchRadius   = SEARCH_RADIUS_MIN;
    private int           searchTick     = 0;
    private boolean       saidSearching  = false;
    private boolean       saidExpanding  = false;
    private int           sessionStartY  = 0;

    private int     cachedSurfaceY = 0;

    private boolean saidCaveNav      = false;

    private int     digDownStuckTimer = 0;

    private final Set<BlockPos> failedTargets = new HashSet<>();

    private boolean  scanActive   = false;
    private int      scanX, scanY, scanZ;
    private int      scanMinX, scanMaxX;
    private int      scanMinY, scanMaxY;
    private int      scanMinZ, scanMaxZ;
    private BlockPos scanBest     = null;
    private double   scanBestDist = Double.MAX_VALUE;

    public AlienGoalMineBlock(AlienEntity alien) {
        this.alien = alien;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    public void startMining(MineableBlock type, int durationTicks) {
        this.miningType    = type;
        this.currentTarget = null;
        this.durationTicks = durationTicks;
        this.elapsedTicks  = 0;
        this.mineCount     = 0;
        this.phase         = Phase.SEARCH;
        this.running       = true;
        this.breakTimer    = 0;
        this.stuckTimer    = 0;
        this.clearStuck    = 0;
        this.clearAttempts = 0;
        this.clearTarget   = null;
        this.activeBreakPos = null;
        this.searchRadius  = SEARCH_RADIUS_MIN;
        this.searchTick    = 0;
        this.saidSearching = false;
        this.saidExpanding = false;
        this.cachedSurfaceY = 0;
        this.saidCaveNav      = false;
        this.digDownStuckTimer = 0;
        this.scanActive    = false;
        this.failedTargets.clear();
    }

    public void cancel() {
        this.running    = false;
        this.scanActive = false;
    }

    private void markCurrentTargetFailed() {
        if (currentTarget != null) {
            failedTargets.add(currentTarget);

            if (miningType != null && alien.level() instanceof ServerLevel sl) {
                for (int dx = -CLUSTER_FAIL_RADIUS; dx <= CLUSTER_FAIL_RADIUS; dx++) {
                    for (int dz = -CLUSTER_FAIL_RADIUS; dz <= CLUSTER_FAIL_RADIUS; dz++) {
                        for (int dy = -CLUSTER_Y_RANGE; dy <= CLUSTER_Y_RANGE; dy++) {
                            BlockPos nearby = currentTarget.offset(dx, dy, dz);
                            BlockState bs = sl.getBlockState(nearby);
                            if (miningType.variants.contains(bs.getBlock())) {
                                failedTargets.add(nearby.immutable());
                            }
                        }
                    }
                }
            }
            MysteryOfTheCrash.LOGGER.info("[AlienMine] Target {} unreachable — cluster marked failed", currentTarget);
        }
        resetToSearch();
    }

    @Override
    public boolean canUse() {
        if (!running || !alien.getLifeStage().canMine) return false;
        return !alien.getStoredTool().isEmpty();
    }

    @Override
    public boolean canContinueToUse() {
        if (!running || elapsedTicks >= durationTicks || !alien.getLifeStage().canMine) return false;
        return !alien.getStoredTool().isEmpty()
                || !alien.getItemBySlot(EquipmentSlot.MAINHAND).isEmpty();
    }

    @Override
    public void start() {
        sessionStartY = alien.getBlockY();
        ItemStack tool = alien.getStoredTool();
        if (!tool.isEmpty()) alien.setItemSlot(EquipmentSlot.MAINHAND, tool);
        MysteryOfTheCrash.LOGGER.info("[AlienMine] Start — type={}, dur={}t, tool={}, Y={}",
                miningType != null ? miningType.id : "?", durationTicks,
                !tool.isEmpty() ? tool.getItem() : "NONE", sessionStartY);
        String name = miningType != null ? miningType.displayName : "...";
        say("◈ ...search...",
            "I'll look for " + name + ".",
            "Scanning for " + name + ". Give me a moment.");
    }

    @Override
    public void stop() {
        clearAllProgress();
        ItemStack activeTool = alien.getItemBySlot(EquipmentSlot.MAINHAND);
        if (!activeTool.isEmpty()) {
            alien.setStoredTool(activeTool);
            alien.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        }
        running        = false;
        currentTarget  = null;
        clearTarget    = null;
        activeBreakPos = null;
        miningType     = null;
        scanActive     = false;
        alien.getNavigation().stop();

        if (mineCount > 0)
            MysteryOfTheCrash.LOGGER.info("[AlienMine] Session ended — mined: {}", mineCount);

        if (alien.getAlienBrain() != null) {
            BlockPos returnTarget = alien.getHomePos() != null
                    ? alien.getHomePos()
                    : alien.getCrashSitePos();

            String doneMsg = mineCount > 0
                    ? "Done. Got " + mineCount + " ore(s). Heading back."
                    : "Nothing found nearby. Heading back.";
            say("◈ ...done.", doneMsg, doneMsg);

            if (returnTarget != null && !returnTarget.equals(BlockPos.ZERO)
                    && alien.level() instanceof ServerLevel sl) {
                double tpY = BlockUtil.getSafeY(sl, returnTarget);
                alien.teleportTo(returnTarget.getX() + 0.5, tpY, returnTarget.getZ() + 0.5);
                alien.getAlienBrain().commandRelease();
            } else {
                alien.getAlienBrain().commandRelease();
            }
        }
    }

    @Override
    public void tick() {
        if (!(alien.level() instanceof ServerLevel sl)) return;
        if (miningType == null) { stop(); return; }
        elapsedTicks++;

        switch (phase) {
            case SEARCH     -> tickSearch(sl);
            case NAVIGATE   -> tickNavigate(sl);
            case DIG_DOWN   -> tickDigDown(sl);
            case CLEAR_PATH -> tickClearPath(sl);
            case MINE       -> tickMine(sl);
        }
    }

    private void tickSearch(ServerLevel level) {
        alien.getNavigation().stop();
        searchTick++;
        if (searchTick < SEARCH_TICK_DELAY) return;
        searchTick = 0;

        if (!scanActive) {
            BlockPos center = alien.blockPosition();
            scanMinX = center.getX() - searchRadius;
            scanMaxX = center.getX() + searchRadius;
            int surfaceHere = level.getHeight(
                    Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, alien.getBlockX(), alien.getBlockZ());
            if (alien.getBlockY() < surfaceHere - 2) {
                scanMinY = Math.max(miningType.searchMinY, alien.getBlockY() - 20);
                scanMaxY = Math.min(miningType.searchMaxY, alien.getBlockY() + 20);
            } else {
                scanMinY = miningType.searchMinY;
                scanMaxY = miningType.searchMaxY;
            }
            scanMinZ = center.getZ() - searchRadius;
            scanMaxZ = center.getZ() + searchRadius;
            scanX = scanMinX; scanY = scanMinY; scanZ = scanMinZ;
            scanBest = null; scanBestDist = Double.MAX_VALUE;
            scanActive = true;

            if (!saidSearching) {
                saidSearching = true;
                String name = miningType.displayName;
                say("◈ ◈ ◈",
                    "Scanning radius " + searchRadius + " for " + name + "...",
                    "Checking radius " + searchRadius + " for " + name + ".");
            }
        }

        BlockPos center = alien.blockPosition();
        BlockPos.MutableBlockPos mPos = new BlockPos.MutableBlockPos();
        int processed = 0;

        scan:
        while (scanY <= scanMaxY) {
            while (scanX <= scanMaxX) {
                while (scanZ <= scanMaxZ) {
                    mPos.set(scanX, scanY, scanZ);
                    if (miningType.matches(level.getBlockState(mPos).getBlock())
                            && !failedTargets.contains(mPos.immutable())) {
                        double d = center.distSqr(mPos);
                        if (d < scanBestDist) {
                            scanBestDist = d;
                            scanBest = mPos.immutable();
                        }
                    }
                    scanZ++;
                    if (++processed >= SCAN_BATCH) break scan;
                }
                scanZ = scanMinZ;
                scanX++;
            }
            scanX = scanMinX;
            scanY++;
        }

        if (scanY > scanMaxY) {
            scanActive = false;

            if (scanBest != null) {
                currentTarget  = scanBest;
                clearAttempts  = 0;
                stuckTimer     = 0;
                cachedSurfaceY = 0;
                phase          = Phase.NAVIGATE;
                MysteryOfTheCrash.LOGGER.info("[AlienMine] Found {} at {} (radius {})",
                        miningType.id, currentTarget, searchRadius);
                String name = miningType.displayName;
                say("◈ ...found!",
                    "Found " + name + "! Going there.",
                    "Located " + name + " at depth " + currentTarget.getY() + ". On my way.");
                return;
            }

            searchRadius += SEARCH_RADIUS_STEP;
            if (searchRadius > SEARCH_RADIUS_MAX) {
                MysteryOfTheCrash.LOGGER.info("[AlienMine] {} not found within radius {}",
                        miningType.id, SEARCH_RADIUS_MAX);
                String name = miningType.displayName;
                say("◈ ...none.",
                    "I couldn't find any " + name + " in range.",
                    "No " + name + " within range. Stopping.");
                stop();
                return;
            }

            if (!saidExpanding) {
                saidExpanding = true;
                say("◈ ◈ ...further...",
                    "Not nearby. Expanding search to radius " + searchRadius + ".",
                    "Nothing at this range. Checking radius " + searchRadius + ".");
            }
        }
    }

    private void tickNavigate(ServerLevel level) {
        if (!isTargetStillValid(level)) { resetToSearch(); return; }

        alien.getLookControl().setLookAt(
                currentTarget.getX() + 0.5, currentTarget.getY() + 0.5, currentTarget.getZ() + 0.5);

        boolean underground = currentTarget.getY() <= alien.getBlockY() - DIG_THRESHOLD;

        if (underground) {

            if (cachedSurfaceY == 0) {
                cachedSurfaceY = level.getHeight(
                        Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                        currentTarget.getX(), currentTarget.getZ());
            }
            alien.getNavigation().moveTo(
                    currentTarget.getX() + 0.5, cachedSurfaceY, currentTarget.getZ() + 0.5, 1.0);
            stuckTimer++;

            int dx = Math.abs(alien.getBlockX() - currentTarget.getX());
            int dz = Math.abs(alien.getBlockZ() - currentTarget.getZ());
            if (dx <= 1 && dz <= 1) {

                cachedSurfaceY = 0;
                phase          = Phase.DIG_DOWN;
                breakTimer     = 0;
                alien.getNavigation().stop();
                say("◈ ...down.",
                    "It's underground. I'll dig down.",
                    miningType.displayName + " is below. Going down.");
                return;
            }

            if (stuckTimer >= STUCK_THRESHOLD * 2) {

                markCurrentTargetFailed();
            }

        } else {

            if (alien.blockPosition().distSqr(currentTarget) <= REACH_SQ) {
                phase       = Phase.MINE;
                breakTimer  = 0;
                stuckTimer  = 0;
                alien.getNavigation().stop();
                return;
            }

            int aboveDx = Math.abs(alien.getBlockX() - currentTarget.getX());
            int aboveDz = Math.abs(alien.getBlockZ() - currentTarget.getZ());
            if (aboveDx <= 1 && aboveDz <= 1 && alien.getBlockY() > currentTarget.getY()) {
                phase      = Phase.DIG_DOWN;
                breakTimer = 0;
                alien.getNavigation().stop();
                return;
            }

            alien.getNavigation().moveTo(
                    currentTarget.getX() + 0.5, currentTarget.getY(), currentTarget.getZ() + 0.5, 1.0);
            stuckTimer++;

            if (stuckTimer >= STUCK_THRESHOLD) {
                stuckTimer = 0;
                if (clearAttempts >= MAX_CLEAR_ATTEMPTS) {
                    markCurrentTargetFailed(); 
                    return;
                }

                BlockPos blocking = findHorizontalBlocking(level, alien.blockPosition(), currentTarget);
                if (blocking != null) {
                    clearTarget    = blocking;
                    phase          = Phase.CLEAR_PATH;
                    breakTimer     = 0;
                    clearStuck     = 0;
                    clearAttempts++;
                } else {

                    markCurrentTargetFailed();
                }
            }
        }
    }

    private void tickDigDown(ServerLevel level) {
        if (!isTargetStillValid(level)) { resetToSearch(); return; }

        alien.getNavigation().stop();

        if (alien.getBlockY() <= currentTarget.getY() + 1) {
            clearAllProgress();
            if (alien.blockPosition().distSqr(currentTarget) <= REACH_SQ + 9) {

                phase      = Phase.MINE;
                breakTimer = 0;
            } else {

                phase          = Phase.NAVIGATE;
                stuckTimer     = 0;
                cachedSurfaceY = 0;
                MysteryOfTheCrash.LOGGER.info(
                        "[AlienMine] DIG_DOWN overshot — alienY={}, targetY={}, switching to NAVIGATE",
                        alien.getBlockY(), currentTarget.getY());
            }
            return;
        }

        BlockPos digPos = alien.blockPosition().below();

        if (digPos.equals(currentTarget)) {
            clearAllProgress();
            phase      = Phase.MINE;
            breakTimer = 0;
            return;
        }

        BlockState state = level.getBlockState(digPos);
        if (state.isAir()) {

            if (alien.onGround()) {
                boolean lavaBelow = false;
                for (int drop = 1; drop <= DROP_LIMIT; drop++) {
                    BlockState bs = level.getBlockState(alien.blockPosition().below(drop));
                    if (bs.getFluidState().is(FluidTags.LAVA)) {
                        lavaBelow = true;
                        break;
                    }
                    if (!bs.isAir()) break;
                }
                if (lavaBelow) {
                    MysteryOfTheCrash.LOGGER.info(
                            "[AlienMine] Lava below at Y={} — skipping {}", alien.getBlockY(), currentTarget);
                    markCurrentTargetFailed();
                    return;
                }

                if (!saidCaveNav) {
                    saidCaveNav = true;
                    say("◈ ...cave.",
                        "There's a cave below. I'll route through it.",
                        "Open space below. Descending.");
                }
                digDownStuckTimer++;
                if (digDownStuckTimer >= STUCK_THRESHOLD) {
                    markCurrentTargetFailed();
                    return;
                }
                alien.teleportTo(alien.getX(), alien.getY() - 1.0, alien.getZ());
            } else {
                digDownStuckTimer = 0;
            }

            breakTimer = 0;
            clearAllProgress();
            return;
        }

        int breakTicks = calculateBreakTicks(state, digPos, false);
        if (breakTicks == Integer.MAX_VALUE) {
            resetToSearch();
            return;
        }

        alien.getLookControl().setLookAt(
                digPos.getX() + 0.5, digPos.getY() + 0.5, digPos.getZ() + 0.5);

        if (breakTimer == 0) equipBestToolFor(state);

        int crack = Math.min(9, breakTimer * 10 / Math.max(1, breakTicks));
        level.destroyBlockProgress(alien.getId() + 2, digPos, crack);
        activeBreakPos = digPos;

        breakTimer++;
        if (breakTimer >= breakTicks) {
            level.destroyBlockProgress(alien.getId() + 2, digPos, -1);
            activeBreakPos = null;
            collectDrops(level, digPos, state);
            level.removeBlock(digPos, false);
            level.levelEvent(2001, digPos, Block.getId(state));
            breakTimer = 0;
        }
    }

    private void tickClearPath(ServerLevel level) {
        if (clearTarget == null || level.getBlockState(clearTarget).isAir()) {
            clearTarget = null;
            phase       = Phase.NAVIGATE;
            stuckTimer  = 0;
            return;
        }

        alien.getLookControl().setLookAt(
                clearTarget.getX() + 0.5, clearTarget.getY() + 0.5, clearTarget.getZ() + 0.5);

        double distSq = alien.blockPosition().distSqr(clearTarget);
        if (distSq > REACH_SQ + 2) {
            alien.getNavigation().moveTo(
                    clearTarget.getX() + 0.5, clearTarget.getY(), clearTarget.getZ() + 0.5, 1.0);
            clearStuck++;
            if (clearStuck >= STUCK_THRESHOLD) {
                resetToSearch(); 
            }
            return;
        }

        alien.getNavigation().stop();
        BlockState state    = level.getBlockState(clearTarget);
        int        reqTicks = calculateBreakTicks(state, clearTarget, false);

        int crack = Math.min(9, breakTimer * 10 / Math.max(1, reqTicks));
        level.destroyBlockProgress(alien.getId() + 1, clearTarget, crack);

        breakTimer++;
        if (breakTimer >= reqTicks) {
            level.destroyBlockProgress(alien.getId() + 1, clearTarget, -1);
            collectDrops(level, clearTarget, state);
            level.removeBlock(clearTarget, false);
            level.levelEvent(2001, clearTarget, Block.getId(state));
            clearTarget = null;
            phase       = Phase.NAVIGATE;
            breakTimer  = 0;
            stuckTimer  = 0;
        }
    }

    private void tickMine(ServerLevel level) {
        if (!isTargetStillValid(level)) {
            clearAllProgress();
            resetToSearch();
            return;
        }

        alien.getLookControl().setLookAt(
                currentTarget.getX() + 0.5, currentTarget.getY() + 0.5, currentTarget.getZ() + 0.5);

        if (alien.blockPosition().distSqr(currentTarget) > REACH_SQ) {
            clearAllProgress();
            phase = Phase.NAVIGATE;
            return;
        }

        BlockPos obstacle = findObstacleDense(level, alien.blockPosition(), currentTarget);
        if (obstacle != null) {
            clearAllProgress();
            clearTarget    = obstacle;
            phase          = Phase.CLEAR_PATH;
            breakTimer     = 0;
            clearStuck     = 0;
            clearAttempts++;
            return;
        }

        alien.getNavigation().stop();

        if (breakTimer == 0) {
            BlockState targetState = level.getBlockState(currentTarget);
            equipBestToolFor(targetState);
            say("◈ ...break.",
                "Mining it now.",
                "Breaking it down.");
        }

        BlockState state    = level.getBlockState(currentTarget);
        int        reqTicks = calculateBreakTicks(state, currentTarget, true);

        if (reqTicks == Integer.MAX_VALUE) { clearAllProgress(); resetToSearch(); return; }

        int crack = Math.min(9, breakTimer * 10 / Math.max(1, reqTicks));
        level.destroyBlockProgress(alien.getId(), currentTarget, crack);
        activeBreakPos = currentTarget;

        breakTimer++;
        if (breakTimer < reqTicks) return;

        level.destroyBlockProgress(alien.getId(), currentTarget, -1);
        activeBreakPos = null;

        collectDrops(level, currentTarget, state);
        level.removeBlock(currentTarget, false);
        level.levelEvent(2001, currentTarget, Block.getId(state));
        alien.getMiningKnowledge().addProficiencyOnMine(miningType);
        mineCount++;

        ItemStack tool = alien.getItemBySlot(EquipmentSlot.MAINHAND);
        if (!tool.isEmpty()) {
            tool.hurtAndBreak(1, level, null, broken -> {
                MysteryOfTheCrash.LOGGER.info("[AlienMine] Tool broke: {}", broken);
                alien.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
                alien.setStoredTool(ItemStack.EMPTY);
                running = false;
            });
        }

        MysteryOfTheCrash.LOGGER.info("[AlienMine] Mined {} #{} in {}t (prof {}%)",
                miningType.id, mineCount, breakTimer,
                String.format("%.0f", alien.getMiningKnowledge().getProficiency(miningType)));

        if (mineCount % 3 == 0)
            alien.getTelepathicChat().sendRandomMessage(alien, alien.getLifeStage(), alien.getPersonality());

        resetToSearch();
    }

    private boolean isTargetStillValid(ServerLevel level) {
        return currentTarget != null
                && miningType.matches(level.getBlockState(currentTarget).getBlock());
    }

    private void resetToSearch() {
        clearAllProgress();
        currentTarget  = null;
        clearTarget    = null;
        phase          = Phase.SEARCH;
        breakTimer     = 0;
        stuckTimer     = 0;
        clearStuck     = 0;
        clearAttempts  = 0;
        searchRadius   = SEARCH_RADIUS_MIN;
        searchTick     = 0;
        saidSearching  = false;
        saidExpanding  = false;
        cachedSurfaceY = 0;
        saidCaveNav       = false;
        digDownStuckTimer = 0;
        scanActive        = false;
    }

    private void say(String childMsg, String youngMsg, String adultMsg) {
        alien.getTelepathicChat().sendDirectMessage(alien,
                ChatUtil.pickByStage(alien.getLifeStage(), childMsg, youngMsg, adultMsg));
    }

    private void equipBestToolFor(BlockState state) {
        ItemStack mainhand = alien.getItemBySlot(EquipmentSlot.MAINHAND);
        float bestSpeed = mainhand.isEmpty() ? 1.0f : mainhand.getDestroySpeed(state);
        boolean bestIsStored = false;
        int bestInvSlot = -1;

        ItemStack stored = alien.getStoredTool();
        if (!stored.isEmpty()) {
            float s = stored.getDestroySpeed(state);
            if (s > bestSpeed) { bestSpeed = s; bestIsStored = true; }
        }

        SimpleContainer inv = alien.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack item = inv.getItem(i);
            if (item.isEmpty()) continue;
            float s = item.getDestroySpeed(state);
            if (s > bestSpeed) { bestSpeed = s; bestIsStored = false; bestInvSlot = i; }
        }

        if (!bestIsStored && bestInvSlot == -1) return; 

        if (bestIsStored) {

            alien.setStoredTool(mainhand.isEmpty() ? ItemStack.EMPTY : mainhand.copy());
            alien.setItemSlot(EquipmentSlot.MAINHAND, stored.copy());
            MysteryOfTheCrash.LOGGER.info("[AlienMine] Equipped stored tool: {}", stored.getItem());
        } else {

            ItemStack newTool = inv.getItem(bestInvSlot).copy();
            inv.setItem(bestInvSlot, mainhand.isEmpty() ? ItemStack.EMPTY : mainhand.copy());
            alien.setItemSlot(EquipmentSlot.MAINHAND, newTool);
            MysteryOfTheCrash.LOGGER.info("[AlienMine] Equipped inventory tool: {}", newTool.getItem());
        }
    }

    private void clearAllProgress() {
        if (activeBreakPos == null) return;
        if (!(alien.level() instanceof ServerLevel sl)) return;
        sl.destroyBlockProgress(alien.getId(),     activeBreakPos, -1);
        sl.destroyBlockProgress(alien.getId() + 1, activeBreakPos, -1);
        sl.destroyBlockProgress(alien.getId() + 2, activeBreakPos, -1);
        activeBreakPos = null;
    }

    private int calculateBreakTicks(BlockState state, BlockPos pos, boolean applyProficiency) {
        float hardness = state.getDestroySpeed(alien.level(), pos);
        if (hardness < 0) return Integer.MAX_VALUE;
        if (hardness == 0) return 1;

        ItemStack tool       = alien.getItemBySlot(EquipmentSlot.MAINHAND);
        boolean   hasTool    = !tool.isEmpty();
        boolean   needsTool  = state.requiresCorrectToolForDrops();
        boolean   correct    = hasTool && tool.isCorrectToolForDrops(state);

        float speed = hasTool ? tool.getDestroySpeed(state) : 1.0f;
        if (needsTool && !correct) speed /= WRONG_TOOL_PENALTY;

        if (hasTool && speed > 1.0f) {
            ItemEnchantments enchs = tool.getOrDefault(ENCHANTMENTS, ItemEnchantments.EMPTY);
            for (var e : enchs.entrySet()) {
                if (e.getKey().is(Enchantments.EFFICIENCY)) {
                    int lvl = e.getIntValue();
                    speed += (float)(lvl * lvl + 1);
                    break;
                }
            }
        }

        speed = Math.max(speed, 0.001f);
        float mult     = (correct || !needsTool) ? MULT_CORRECT_TOOL : MULT_WRONG_TOOL;
        int   rawTicks = (int) Math.ceil(hardness * mult / speed);

        if (applyProficiency && miningType != null) {
            float prof     = alien.getMiningKnowledge().getProficiency(miningType);
            float profMult = 1.0f - (prof / 100.0f) * 0.3f;
            rawTicks = (int)(rawTicks * profMult);
        }
        return Math.max(1, rawTicks);
    }

    private void collectDrops(ServerLevel level, BlockPos pos, BlockState state) {
        ItemStack tool    = alien.getItemBySlot(EquipmentSlot.MAINHAND);
        boolean needsTool = state.requiresCorrectToolForDrops();
        boolean correct   = !tool.isEmpty() && tool.isCorrectToolForDrops(state);
        if (needsTool && !correct) return;

        BlockEntity     be    = level.getBlockEntity(pos);
        List<ItemStack> drops = Block.getDrops(state, level, pos, be, alien, tool);
        SimpleContainer inv   = alien.getInventory();
        for (ItemStack drop : drops) addToContainer(inv, drop, level, pos);
    }

    private static void addToContainer(SimpleContainer inv, ItemStack drop,
                                       ServerLevel level, BlockPos pos) {
        ItemStack rem = drop.copy();
        for (int i = 0; i < inv.getContainerSize() && !rem.isEmpty(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.isEmpty()) {
                inv.setItem(i, rem.copy()); rem = ItemStack.EMPTY;
            } else if (ItemStack.isSameItemSameComponents(s, rem) && s.getCount() < s.getMaxStackSize()) {
                int take = Math.min(s.getMaxStackSize() - s.getCount(), rem.getCount());
                s.grow(take); rem.shrink(take);
            }
        }
        if (!rem.isEmpty())
            level.addFreshEntity(new ItemEntity(
                    level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, rem));
    }

    private static BlockPos findHorizontalBlocking(ServerLevel level, BlockPos from, BlockPos to) {
        double dx = to.getX() - from.getX();
        double dz = to.getZ() - from.getZ();

        double hDist = Math.sqrt(dx * dx + dz * dz);
        if (hDist < 0.5) return null;

        double dy   = to.getY() - from.getY();
        double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
        int    steps = (int)(dist * 4) + 4;
        BlockPos floor = from.below();

        for (int i = 1; i <= steps; i++) {
            double   t = (double) i / steps;
            BlockPos p = BlockPos.containing(
                    from.getX() + dx * t, from.getY() + dy * t, from.getZ() + dz * t);
            if (p.equals(to) || p.equals(from) || p.equals(floor)) continue;
            BlockState st = level.getBlockState(p);
            if (!st.isAir() && st.getFluidState().isEmpty()) return p.immutable();
        }
        return null;
    }

    private static BlockPos findObstacleDense(ServerLevel level, BlockPos from, BlockPos to) {
        double dx   = to.getX() - from.getX();
        double dy   = to.getY() - from.getY();
        double dz   = to.getZ() - from.getZ();
        double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (dist < 0.5) return null;

        int steps = (int)(dist * 4) + 4;
        for (int i = 1; i <= steps; i++) {
            double   t = (double) i / steps;
            BlockPos p = BlockPos.containing(
                    from.getX() + dx * t, from.getY() + dy * t, from.getZ() + dz * t);
            if (p.equals(to) || p.equals(from)) continue;
            BlockState st = level.getBlockState(p);
            if (!st.isAir() && st.getFluidState().isEmpty()) return p.immutable();
        }
        return null;
    }

    public boolean isRunning() { return running; }
}
