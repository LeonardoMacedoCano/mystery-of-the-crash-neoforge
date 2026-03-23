package com.mysteryofthecrash.entity;

import com.mysteryofthecrash.MysteryOfTheCrash;
import com.mysteryofthecrash.chat.TelepathicChat;
import com.mysteryofthecrash.entity.brain.AlienBrain;
import com.mysteryofthecrash.entity.brain.DecisionEngine;
import com.mysteryofthecrash.entity.growth.GrowthTickHandler;
import com.mysteryofthecrash.entity.learning.LearningObserver;
import com.mysteryofthecrash.entity.personality.PersonalityResolver;
import com.mysteryofthecrash.entity.trust.TrustManager;
import com.mysteryofthecrash.world.AlienWorldData;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.Set;

public class AlienEntity extends PathfinderMob {

    private static final EntityDataAccessor<Integer> DATA_LIFE_STAGE =
            SynchedEntityData.defineId(AlienEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_IS_EXPLORING =
            SynchedEntityData.defineId(AlienEntity.class, EntityDataSerializers.BOOLEAN);

    private final AlienNeeds          needs             = new AlienNeeds();
    private final PersonalityResolver personalityResolver = new PersonalityResolver();
    private final TrustManager        trustManager      = new TrustManager();
    private final LearningObserver    learner           = new LearningObserver();
    private final GrowthTickHandler   growthHandler     = new GrowthTickHandler();
    private final DecisionEngine      decisionEngine    = new DecisionEngine();
    private final TelepathicChat      telepathicChat    = new TelepathicChat();

    private AlienBrain alienBrain;

    private LifeStage   lifeStage   = LifeStage.CHILD;
    private Personality personality = Personality.CURIOUS;
    private long        birthDay    = 0L;
    private BlockPos    crashSitePos = BlockPos.ZERO;
    private final Set<KnowledgeFlags> knowledgeFlags = EnumSet.noneOf(KnowledgeFlags.class);

    private int secondTickAccumulator = 0;
    private int chatTickAccumulator   = 0;
    private static final int CHAT_INTERVAL_TICKS = 1200;

    public AlienEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH,           8.0)
                .add(Attributes.MOVEMENT_SPEED,       0.18)
                .add(Attributes.FOLLOW_RANGE,         32.0)
                .add(Attributes.STEP_HEIGHT,          1.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.1);
    }

    @Override
    protected void registerGoals() {
        alienBrain = new AlienBrain(this, this.goalSelector);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_LIFE_STAGE,   LifeStage.CHILD.ordinal());
        builder.define(DATA_IS_EXPLORING, false);
    }

    @Override
    @Nullable
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                        MobSpawnType spawnType, @Nullable SpawnGroupData spawnGroupData) {
        SpawnGroupData data = super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData);

        if (level instanceof ServerLevel serverLevel) {
            birthDay = serverLevel.getGameTime() / 24000L;
        }

        applyStageAttributes(LifeStage.CHILD);
        this.setHealth(this.getMaxHealth());

        MysteryOfTheCrash.LOGGER.info("[Alien] Spawned. Birth day: {}", birthDay);
        return data;
    }

    @Override
    public void aiStep() {
        super.aiStep();

        if (level().isClientSide) return;
        ServerLevel serverLevel = (ServerLevel) level();

        if (++secondTickAccumulator >= 20) {
            secondTickAccumulator = 0;
            growthHandler.onSecondTick(this, serverLevel);
        }

        if (alienBrain != null) {
            alienBrain.tick();
        }

        DecisionEngine.Action action = decisionEngine.tick(this);
        if (action != null && alienBrain != null) {
            alienBrain.applyDecision(action);
        }

        if (++chatTickAccumulator >= CHAT_INTERVAL_TICKS) {
            chatTickAccumulator = 0;
            if (serverLevel.getNearestPlayer(this, 32) != null) {
                telepathicChat.sendRandomMessage(this, lifeStage, personality);
            }
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean result = super.hurt(source, amount);
        if (!result) return false;

        if (source.getEntity() instanceof Player) {
            trustManager.onPlayerAttacked();
            personalityResolver.recordAggressionEvent(5f);
        }

        if (lifeStage == LifeStage.CHILD) {
            personalityResolver.recordDangerTick(2f);
        }
        trustManager.onDangerExposure();

        return true;
    }

    @Override
    public void die(DamageSource cause) {
        super.die(cause);
        if (!level().isClientSide && level() instanceof ServerLevel serverLevel) {
            AlienWorldData data = AlienWorldData.get(serverLevel);
            data.setNeedsRespawn(true);
            data.setDirty();
            MysteryOfTheCrash.LOGGER.info("[Alien] Died. Will respawn at {}", data.getRespawnPos());
        }
    }

    @Override
    public net.minecraft.world.InteractionResult mobInteract(Player player,
                                                              net.minecraft.world.InteractionHand hand) {
        var itemInHand = player.getItemInHand(hand);

        if (itemInHand.has(net.minecraft.core.component.DataComponents.FOOD)) {
            if (!level().isClientSide) {
                float trustBefore = trustManager.getTrust();
                needs.feed(15f);
                needs.socialNeed = Math.max(0, needs.socialNeed - 10f);
                trustManager.onPlayerFed();
                personalityResolver.recordKindnessEvent(3f);
                telepathicChat.sendRandomMessage(this, lifeStage, personality);
                MysteryOfTheCrash.LOGGER.info("[Alien] Fed by {}. Bond: {} -> {}. Hunger: {}",
                        player.getName().getString(),
                        String.format("%.1f", trustBefore),
                        String.format("%.1f", trustManager.getTrust()),
                        String.format("%.0f", needs.hunger));
                if (!player.getAbilities().instabuild) {
                    itemInHand.shrink(1);
                }
            }
            return net.minecraft.world.InteractionResult.sidedSuccess(level().isClientSide);
        }

        return super.mobInteract(player, hand);
    }

    public void respawnAt(BlockPos pos) {
        this.teleportTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        this.setHealth(this.getMaxHealth());
        MysteryOfTheCrash.LOGGER.info("[Alien] Respawned at {}", pos);
    }

    public void applyStageAttributes(LifeStage stage) {
        var maxHp = this.getAttribute(Attributes.MAX_HEALTH);
        var speed = this.getAttribute(Attributes.MOVEMENT_SPEED);
        if (maxHp != null) maxHp.setBaseValue(stage.maxHealth);
        if (speed != null) speed.setBaseValue(stage.moveSpeed);

        if (this.getHealth() > stage.maxHealth) {
            this.setHealth(stage.maxHealth);
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        CompoundTag alienTag = new CompoundTag();

        alienTag.putString("lifeStage",   lifeStage.name());
        alienTag.putString("personalityName", personality.name());
        alienTag.putLong("birthDay",         birthDay);
        alienTag.putInt("crashSiteX",     crashSitePos.getX());
        alienTag.putInt("crashSiteY",     crashSitePos.getY());
        alienTag.putInt("crashSiteZ",     crashSitePos.getZ());

        int flagBits = 0;
        for (KnowledgeFlags flag : KnowledgeFlags.values()) {
            if (knowledgeFlags.contains(flag)) {
                flagBits |= (1 << flag.ordinal());
            }
        }
        alienTag.putInt("knowledgeFlags", flagBits);

        alienTag.put("needs",           needs.save());
        alienTag.put("personalityData", personalityResolver.save());
        alienTag.put("trust",           trustManager.save());
        alienTag.put("learning",        learner.save());

        tag.put("AlienData", alienTag);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (!tag.contains("AlienData")) return;

        CompoundTag alienTag = tag.getCompound("AlienData");

        try {
            lifeStage   = LifeStage.valueOf(alienTag.getString("lifeStage"));
        } catch (IllegalArgumentException e) {
            lifeStage = LifeStage.CHILD;
        }
        try {
            personality = Personality.valueOf(alienTag.getString("personalityName"));
        } catch (IllegalArgumentException e) {
            personality = Personality.CURIOUS;
        }

        birthDay     = alienTag.getLong("birthDay");
        crashSitePos = new BlockPos(
                alienTag.getInt("crashSiteX"),
                alienTag.getInt("crashSiteY"),
                alienTag.getInt("crashSiteZ"));

        int flagBits = alienTag.getInt("knowledgeFlags");
        knowledgeFlags.clear();
        for (KnowledgeFlags flag : KnowledgeFlags.values()) {
            if ((flagBits & (1 << flag.ordinal())) != 0) {
                knowledgeFlags.add(flag);
            }
        }

        needs.load(alienTag.getCompound("needs"));
        personalityResolver.load(alienTag.getCompound("personalityData"));
        trustManager.load(alienTag.getCompound("trust"));
        learner.load(alienTag.getCompound("learning"));

        applyStageAttributes(lifeStage);

        this.entityData.set(DATA_LIFE_STAGE, lifeStage.ordinal());
    }

    public boolean hasKnowledge(KnowledgeFlags flag) {
        return knowledgeFlags.contains(flag);
    }

    public boolean hasAnyKnowledge() {
        return !knowledgeFlags.isEmpty();
    }

    public void learnKnowledge(KnowledgeFlags flag) {
        knowledgeFlags.add(flag);
    }

    public LifeStage   getLifeStage()          { return lifeStage; }
    public void        setLifeStage(LifeStage s) {
        this.lifeStage = s;
        this.entityData.set(DATA_LIFE_STAGE, s.ordinal());
    }

    public Personality getPersonality()               { return personality; }
    public void        setPersonality(Personality p)  { this.personality = p; }

    public long        getBirthDay()                  { return birthDay; }
    public void        setBirthDay(long day)          { this.birthDay = day; }
    public BlockPos    getCrashSitePos()              { return crashSitePos; }
    public void        setCrashSitePos(BlockPos pos)  { this.crashSitePos = pos; }

    public AlienNeeds           getNeeds()                { return needs; }
    public PersonalityResolver  getPersonalityResolver()  { return personalityResolver; }
    public TrustManager         getTrustManager()         { return trustManager; }
    public LearningObserver     getLearner()              { return learner; }
    public TelepathicChat       getTelepathicChat()       { return telepathicChat; }
    public AlienBrain           getAlienBrain()           { return alienBrain; }

    public boolean isCurrentGoalExploring() {
        return alienBrain != null && alienBrain.isExploring();
    }

    public boolean isCurrentGoalResting() {
        return alienBrain != null && alienBrain.isResting();
    }

    public boolean isIdle() {
        return getNavigation().isDone()
                && (alienBrain == null || !alienBrain.isExploring());
    }

    public void setExploring(boolean exploring) {
        this.entityData.set(DATA_IS_EXPLORING, exploring);
    }

    public boolean isExploring() {
        return this.entityData.get(DATA_IS_EXPLORING);
    }

    public int getLifeStageOrdinal() {
        return this.entityData.get(DATA_LIFE_STAGE);
    }

    public void setChanged() {
    }
}
