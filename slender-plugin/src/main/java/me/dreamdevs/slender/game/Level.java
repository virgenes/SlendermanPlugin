package me.dreamdevs.slender.game;

import lombok.Getter;

@Getter
public class Level {

    private final int requireExp;
    private final java.util.List<String> rewards;

    public Level(int requireExp, java.util.List<String> rewards) {
        this.requireExp = requireExp;
        this.rewards = rewards;
    }

}