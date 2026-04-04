package com.mysteryofthecrash.world;

import com.mysteryofthecrash.MysteryOfTheCrash;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import javax.annotation.Nullable;

import java.util.UUID;

public class AlienWorldData extends SavedData {

    private static final String DATA_NAME = "mysteryofthecrash_world";

    private boolean     crashSiteGenerated    = false;
    private BlockPos    crashSitePos          = BlockPos.ZERO;
    private BlockPos    respawnPos            = BlockPos.ZERO;
    private boolean     needsRespawn          = false;
    private UUID        alienUUID             = null;
    private boolean     retainProgressOnDeath = false;
    @Nullable
    private CompoundTag savedAlienProgress    = null;
    private BlockPos    lastKnownPos          = BlockPos.ZERO;

    private static final SavedData.Factory<AlienWorldData> FACTORY =
            new SavedData.Factory<>(AlienWorldData::new,
                    (tag, registries) -> AlienWorldData.load(tag),
                    null);

    public static AlienWorldData get(ServerLevel level) {
        ServerLevel overworld = level.getServer().overworld();
        return overworld.getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putBoolean("crashSiteGenerated", crashSiteGenerated);
        tag.putInt("crashSiteX", crashSitePos.getX());
        tag.putInt("crashSiteY", crashSitePos.getY());
        tag.putInt("crashSiteZ", crashSitePos.getZ());
        tag.putInt("respawnX",   respawnPos.getX());
        tag.putInt("respawnY",   respawnPos.getY());
        tag.putInt("respawnZ",   respawnPos.getZ());
        tag.putBoolean("needsRespawn", needsRespawn);
        if (alienUUID != null) {
            tag.putUUID("alienUUID", alienUUID);
        }
        tag.putBoolean("retainProgressOnDeath", retainProgressOnDeath);
        if (savedAlienProgress != null) {
            tag.put("savedAlienProgress", savedAlienProgress);
        }
        tag.putInt("lastKnownX", lastKnownPos.getX());
        tag.putInt("lastKnownY", lastKnownPos.getY());
        tag.putInt("lastKnownZ", lastKnownPos.getZ());
        return tag;
    }

    public static AlienWorldData load(CompoundTag tag) {
        AlienWorldData data = new AlienWorldData();
        data.crashSiteGenerated = tag.getBoolean("crashSiteGenerated");
        data.crashSitePos = new BlockPos(
                tag.getInt("crashSiteX"),
                tag.getInt("crashSiteY"),
                tag.getInt("crashSiteZ"));
        data.respawnPos = new BlockPos(
                tag.getInt("respawnX"),
                tag.getInt("respawnY"),
                tag.getInt("respawnZ"));
        data.needsRespawn = tag.getBoolean("needsRespawn");
        if (tag.hasUUID("alienUUID")) {
            data.alienUUID = tag.getUUID("alienUUID");
        }
        data.retainProgressOnDeath = tag.getBoolean("retainProgressOnDeath");
        if (tag.contains("savedAlienProgress")) {
            data.savedAlienProgress = tag.getCompound("savedAlienProgress");
        }
        data.lastKnownPos = new BlockPos(
                tag.getInt("lastKnownX"), tag.getInt("lastKnownY"), tag.getInt("lastKnownZ"));
        return data;
    }

    public boolean isCrashSiteGenerated()               { return crashSiteGenerated; }
    public void    setCrashSiteGenerated(boolean v)     { crashSiteGenerated = v; setDirty(); }

    public BlockPos getCrashSitePos()                   { return crashSitePos; }
    public void     setCrashSitePos(BlockPos pos)       { crashSitePos = pos; respawnPos = pos; setDirty(); }

    public BlockPos getRespawnPos()                     { return respawnPos; }
    public void     setRespawnPos(BlockPos pos)         { respawnPos = pos; setDirty(); }

    public boolean  isNeedsRespawn()                    { return needsRespawn; }
    public void     setNeedsRespawn(boolean v)          { needsRespawn = v; setDirty(); }

    public UUID     getAlienUUID()                      { return alienUUID; }
    public void     setAlienUUID(UUID uuid)             { alienUUID = uuid; setDirty(); }

    public boolean     isRetainProgressOnDeath()              { return retainProgressOnDeath; }
    public void        setRetainProgressOnDeath(boolean v)    { retainProgressOnDeath = v; setDirty(); }

    @Nullable
    public CompoundTag getSavedAlienProgress()                { return savedAlienProgress; }
    public void        setSavedAlienProgress(@Nullable CompoundTag t) { savedAlienProgress = t; setDirty(); }

    public BlockPos getLastKnownPos()                        { return lastKnownPos; }
    public void     setLastKnownPos(BlockPos pos)            { lastKnownPos = pos; setDirty(); }
}
