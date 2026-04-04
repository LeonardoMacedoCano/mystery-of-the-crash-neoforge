package com.mysteryofthecrash.entity;

import com.mysteryofthecrash.MysteryOfTheCrash;
import com.mysteryofthecrash.chat.TelepathicChat;
import com.mysteryofthecrash.entity.brain.AlienBrain;
import com.mysteryofthecrash.entity.brain.DecisionEngine;
import com.mysteryofthecrash.entity.growth.GrowthTickHandler;
import com.mysteryofthecrash.entity.learning.LearningObserver;
import com.mysteryofthecrash.entity.learning.MiningKnowledge;
import com.mysteryofthecrash.entity.personality.PersonalityResolver;
import com.mysteryofthecrash.entity.trust.TrustManager;
import com.mysteryofthecrash.inventory.AlienInventoryMenu;
import com.mysteryofthecrash.world.AlienWorldData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.Set;

public class AlienEntity extends PathfinderMob implements MenuProvider {

    private static final EntityDataAccessor<Integer> DATA_LIFE_STAGE =
            SynchedEntityData.defineId(AlienEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_IS_EXPLORING =
            SynchedEntityData.defineId(AlienEntity.class, EntityDataSerializers.BOOLEAN);

    private final SimpleContainer     inventory         = new SimpleContainer(27);
    private final AlienNeeds          needs             = new AlienNeeds();
    private final PersonalityResolver personalityResolver = new PersonalityResolver();
    private final TrustManager        trustManager      = new TrustManager();
    private final LearningObserver    learner           = new LearningObserver();
    private final MiningKnowledge     miningKnowledge   = new MiningKnowledge();
    private final GrowthTickHandler   growthHandler     = new GrowthTickHandler();
    private final DecisionEngine      decisionEngine    = new DecisionEngine();
    private final TelepathicChat      telepathicChat    = new TelepathicChat();

    private AlienBrain alienBrain;

    private ChunkPos selfForcedChunk = null;

    private ItemStack storedTool = ItemStack.EMPTY;

    private LifeStage   lifeStage   = LifeStage.CHILD;
    private Personality personality = Personality.CURIOUS;
    private long        birthDay    = 0L;
    private BlockPos    crashSitePos = BlockPos.ZERO;
    @Nullable
    private BlockPos    homePos     = null;  
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

        this.setPersistenceRequired(); 

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

        updateSelfForcedChunk(serverLevel);
        if (tickCount % 200 == 0) {
            AlienWorldData.get(serverLevel).setLastKnownPos(blockPosition());
        }

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
    protected int decreaseAirSupply(int currentAir) {
        return getMaxAirSupply(); 
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float multiplier, DamageSource source) {
        return false; 
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean result = super.hurt(source, amount);
        if (!result) return false;

        if (source.getEntity() instanceof Player attacker) {
            trustManager.onPlayerAttacked(attacker.getUUID());
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
        if (level().isClientSide || !(level() instanceof ServerLevel serverLevel)) return;
        AlienWorldData data = AlienWorldData.get(serverLevel);
        if (!getUUID().equals(data.getAlienUUID())) {
            MysteryOfTheCrash.LOGGER.warn("[Alien] Orphaned ET died — not flagging respawn: {}", getUUID());
            return;
        }
        data.setNeedsRespawn(true);
        data.setDirty();
        MysteryOfTheCrash.LOGGER.info("[Alien] Died. Will respawn at {}", data.getRespawnPos());
    }

    @Override
    public void onAddedToLevel() {
        super.onAddedToLevel();
        if (level() instanceof ServerLevel sl) {
            updateSelfForcedChunk(sl);
        }
    }

    @Override
    public void onRemovedFromLevel() {
        if (level() instanceof ServerLevel sl && selfForcedChunk != null) {
            sl.getChunkSource().removeRegionTicket(TicketType.FORCED, selfForcedChunk, 2, selfForcedChunk);
            selfForcedChunk = null;
        }
        super.onRemovedFromLevel();
    }

    private void updateSelfForcedChunk(ServerLevel level) {
        ChunkPos current = new ChunkPos(blockPosition());
        if (current.equals(selfForcedChunk)) return;
        if (selfForcedChunk != null) {
            level.getChunkSource().removeRegionTicket(TicketType.FORCED, selfForcedChunk, 2, selfForcedChunk);
        }
        selfForcedChunk = current;
        level.getChunkSource().addRegionTicket(TicketType.FORCED, selfForcedChunk, 2, selfForcedChunk);
    }

    @Override
    public net.minecraft.world.InteractionResult mobInteract(Player player,
                                                              net.minecraft.world.InteractionHand hand) {
        var itemInHand = player.getItemInHand(hand);

        if (itemInHand.has(net.minecraft.core.component.DataComponents.FOOD)) {
            if (!level().isClientSide) {
                float trustBefore = trustManager.getTrust(player.getUUID());
                needs.feed(15f);
                needs.socialNeed = Math.max(0, needs.socialNeed - 10f);
                trustManager.onPlayerFed(player.getUUID());
                personalityResolver.recordKindnessEvent(3f);
                telepathicChat.sendRandomMessage(this, lifeStage, personality);
                MysteryOfTheCrash.LOGGER.info("[Alien] Fed by {}. Bond: {} -> {}. Hunger: {}",
                        player.getName().getString(),
                        String.format("%.1f", trustBefore),
                        String.format("%.1f", trustManager.getTrust(player.getUUID())),
                        String.format("%.0f", needs.hunger));
                if (!player.getAbilities().instabuild) {
                    itemInHand.shrink(1);
                }
            }
            return net.minecraft.world.InteractionResult.sidedSuccess(level().isClientSide);
        }

        if (lifeStage.canEquipGear && itemInHand.getItem() instanceof DiggerItem) {
            if (!level().isClientSide) {

                if (!storedTool.isEmpty()) this.spawnAtLocation(storedTool);
                storedTool = itemInHand.copy();
                if (!player.getAbilities().instabuild) itemInHand.shrink(1);
                telepathicChat.sendRandomMessage(this, lifeStage, personality);
                MysteryOfTheCrash.LOGGER.info("[Alien] Tool stored (will equip when mining): {}", storedTool.getItem());
            }
            return net.minecraft.world.InteractionResult.sidedSuccess(level().isClientSide);
        }

        if (lifeStage.canEquipGear && itemInHand.getItem() instanceof ArmorItem armorItem) {
            if (!level().isClientSide) {
                EquipmentSlot slot    = armorItem.getEquipmentSlot();
                ItemStack     current = this.getItemBySlot(slot);
                if (!current.isEmpty()) this.spawnAtLocation(current);
                this.setItemSlot(slot, itemInHand.copy());
                this.setDropChance(slot, 1.0f);
                if (!player.getAbilities().instabuild) itemInHand.shrink(1);
                telepathicChat.sendRandomMessage(this, lifeStage, personality);
                MysteryOfTheCrash.LOGGER.info("[Alien] Equipped armor: {} in {}", itemInHand.getItem(), slot);
            }
            return net.minecraft.world.InteractionResult.sidedSuccess(level().isClientSide);
        }

        if (!level().isClientSide) {
            player.openMenu(this, buf -> buf.writeInt(this.getId()));
        }
        return net.minecraft.world.InteractionResult.sidedSuccess(level().isClientSide);
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
        alienTag.put("mining",          miningKnowledge.save());

        HolderLookup.Provider registries = this.level().registryAccess();
        ListTag invTag = new ListTag();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.isEmpty()) {
                CompoundTag slotTag = new CompoundTag();
                slotTag.putByte("Slot", (byte) i);
                slotTag.put("Item", stack.save(registries));
                invTag.add(slotTag);
            }
        }
        alienTag.put("inventory", invTag);

        if (!storedTool.isEmpty()) {
            alienTag.put("storedTool", storedTool.save(registries));
        }

        if (homePos != null) {
            alienTag.putInt("homePosX", homePos.getX());
            alienTag.putInt("homePosY", homePos.getY());
            alienTag.putInt("homePosZ", homePos.getZ());
        }

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
        miningKnowledge.load(alienTag.getCompound("mining"));

        if (alienTag.contains("inventory")) {
            HolderLookup.Provider registries = this.level().registryAccess();
            ListTag invTag = alienTag.getList("inventory", Tag.TAG_COMPOUND);
            for (int i = 0; i < invTag.size(); i++) {
                CompoundTag slotTag = invTag.getCompound(i);
                int slot = slotTag.getByte("Slot") & 0xFF;
                if (slot < inventory.getContainerSize()) {
                    ItemStack.parse(registries, slotTag.getCompound("Item"))
                            .ifPresent(s -> inventory.setItem(slot, s));
                }
            }
        }

        if (alienTag.contains("storedTool")) {
            HolderLookup.Provider registries = this.level().registryAccess();
            ItemStack.parse(registries, alienTag.getCompound("storedTool"))
                    .ifPresent(s -> storedTool = s);
        }

        if (alienTag.contains("homePosX")) {
            homePos = new BlockPos(
                    alienTag.getInt("homePosX"),
                    alienTag.getInt("homePosY"),
                    alienTag.getInt("homePosZ"));
        }

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
    public MiningKnowledge      getMiningKnowledge()      { return miningKnowledge; }
    public SimpleContainer      getInventory()            { return inventory; }
    public TelepathicChat       getTelepathicChat()       { return telepathicChat; }
    public AlienBrain           getAlienBrain()           { return alienBrain; }
    public ItemStack            getStoredTool()           { return storedTool; }
    public void                 setStoredTool(ItemStack s){ storedTool = s; }
    @Nullable
    public BlockPos             getHomePos()              { return homePos; }
    public void                 setHomePos(BlockPos pos)  { this.homePos = pos; }

    @Override
    public Component getDisplayName() {
        return Component.literal("Alien's Pack");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInv, Player player) {
        return new AlienInventoryMenu(containerId, playerInv, this);
    }

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
