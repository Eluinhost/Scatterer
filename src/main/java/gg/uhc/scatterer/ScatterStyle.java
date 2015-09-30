package gg.uhc.scatterer;

import com.publicuhc.scatterlib.logic.RandomCircleScatterLogic;
import com.publicuhc.scatterlib.logic.RandomSquareScatterLogic;
import com.publicuhc.scatterlib.logic.StandardScatterLogic;

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