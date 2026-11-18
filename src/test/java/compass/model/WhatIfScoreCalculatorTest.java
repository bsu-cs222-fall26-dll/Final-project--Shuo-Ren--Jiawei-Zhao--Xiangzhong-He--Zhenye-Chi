package compass.model;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WhatIfScoreCalculatorTest {
    @Test void calculatesTheAssignmentPercentage() {
        assertEquals(new BigDecimal("85.00"), calculate("85", "100"));
    }

    @Test void roundsRepeatingPercentagesToTwoDecimalPlaces() {
        assertEquals(new BigDecimal("33.33"), calculate("1", "3"));
    }

    @Test void acceptsDecimalScoresAndPossiblePoints() {
        assertEquals(new BigDecimal("75.00"), calculate("7.5", "10"));
    }

    @Test void acceptsZeroEarnedPoints() {
        assertEquals(new BigDecimal("0.00"), calculate("0", "100"));
    }

    @Test void permitsExtraCreditAboveOneHundredPercent() {
        assertEquals(new BigDecimal("110.00"), calculate("11", "10"));
    }

    @Test void rejectsNegativeScores() {
        assertThrows(IllegalArgumentException.class, () -> calculate("-1", "100"));
    }

    @Test void rejectsAssignmentsWithZeroPossiblePoints() {
        assertThrows(IllegalArgumentException.class, () -> calculate("1", "0"));
    }

    @Test void rejectsMissingPossiblePoints() {
        assertThrows(IllegalArgumentException.class,
                () -> WhatIfScoreCalculator.percentage(BigDecimal.ONE, null));
    }

    private BigDecimal calculate(String earned, String possible) {
        return WhatIfScoreCalculator.percentage(new BigDecimal(earned), new BigDecimal(possible));
    }
}