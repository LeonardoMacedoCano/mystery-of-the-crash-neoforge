package com.mysteryofthecrash.entity;

public enum Personality {

    CURIOUS(
        0.7f,
        1.8f,
        1.3f,
        0.5f,
        1.0f,
        0.5f
    ),

    LOYAL(
        1.8f, 0.6f, 0.9f, 1.0f, 1.7f, 0.3f
    ),

    INDEPENDENT(
        0.4f, 1.5f, 1.4f, 1.2f, 0.6f, 0.4f
    ),

    CHAOTIC(
        1.0f, 1.2f, 0.8f, 0.5f, 0.7f, 2.5f
    ),

    LAZY(
        0.9f, 0.4f, 0.5f, 2.0f, 0.4f, 0.3f
    ),

    PROTECTIVE(
        1.4f, 0.7f, 1.0f, 0.8f, 1.5f, 0.3f
    );

    public final float followWeight;
    public final float exploreWeight;
    public final float practiceWeight;
    public final float restWeight;
    public final float helpWeight;
    public final float chaosWeight;

    Personality(float follow, float explore, float practice, float rest, float help, float chaos) {
        this.followWeight   = follow;
        this.exploreWeight  = explore;
        this.practiceWeight = practice;
        this.restWeight     = rest;
        this.helpWeight     = help;
        this.chaosWeight    = chaos;
    }
}
