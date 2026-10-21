package edu.example.library;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class FinePolicyTest {
    private final FinePolicy policy = new CappedDailyFinePolicy();

    @ParameterizedTest
    @CsvSource({"0,0", "1,50", "19,950", "20,1000", "21,1000",
            "2147483648,1000", "9223372036854775807,1000"})
    @DisplayName("R14 AC31 AC33: fine boundaries and overflow safety")
    void fineAtCapBoundaries(long days, int expected) {
        assertEquals(expected, policy.calculateFine(days));
    }

    @Test
    void negativeDayInputIsRejected() {
        assertThrowsExactly(IllegalArgumentException.class, () -> policy.calculateFine(-1));
    }
}
