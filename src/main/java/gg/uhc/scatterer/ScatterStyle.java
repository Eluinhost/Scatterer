package gg.uhc.scatterer;

import gg.uhc.scatterlib.logic.RandomCircleScatterLogic;
import gg.uhc.scatterlib.logic.RandomSquareScatterLogic;
import gg.uhc.scatterlib.logic.StandardScatterLogic;

import java.util.Random;

public enum ScatterStyle {
    CIRCULAR {
        @Override
        public StandardScatterLogic provide() {
            return new RandomCircleScatterLogic(RANDOM);
        }
    },
    SQUARE {
        @Override
        public StandardScatterLogic provide() {
            return new RandomSquareScatterLogic(RANDOM);
        }
    };

    static Random RANDOM = new Random();

    public abstract StandardScatterLogic provide();
}