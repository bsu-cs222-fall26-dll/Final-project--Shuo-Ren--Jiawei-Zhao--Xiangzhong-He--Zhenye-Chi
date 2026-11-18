package compass.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class WhatIfScoreCalculator {
    private WhatIfScoreCalculator() { }
    public static BigDecimal percentage(BigDecimal score, BigDecimal possible) {
        if (score == null || score.signum() < 0) {
            throw new IllegalArgumentException("Expected score must be zero or greater.");
        }
        if (possible == null || possible.signum() <= 0) {
            throw new IllegalArgumentException("This assignment has no positive points possible.");
        }
        return score.multiply(BigDecimal.valueOf(100)).divide(possible, 2, RoundingMode.HALF_UP);
    }

}