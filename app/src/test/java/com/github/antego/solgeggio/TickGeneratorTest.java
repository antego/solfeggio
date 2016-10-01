package com.github.antego.solgeggio;

import com.github.antego.solfeggio.TickGenerator;

import org.junit.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertTrue;

public class TickGeneratorTest {
    @Test
    public void tickGeneratorTest() {
        TickGenerator generator = new TickGenerator();
        Map<Float, String> ticks = generator.generateTicks(22050);
        Set<Float> coords = ticks.keySet();
        int posCoords = 0, negCoords = 0;
        for (Float coord : coords) {
            if (coord >= 0) {
                posCoords++;
            } else {
                negCoords++;
            }
        }
        assertTrue(posCoords == negCoords);
    }
}
