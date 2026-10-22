package com.alexvahabov.nlsmeter;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SpeedThresholdsTest {
    @Test
    public void oldValuesRoundToTens() {
        assertEquals(160, SpeedThresholds.maxSpeed(159));
        assertEquals(70, SpeedThresholds.speed(SpeedThresholds.yellowStep(74, 16)));
        assertEquals(80, SpeedThresholds.speed(SpeedThresholds.redStep(80, 7, 16)));
    }

    @Test
    public void thresholdsRemainOrderedAtEveryMaximum() {
        for (int max = 40; max <= 300; max += 10) {
            int maxStep = SpeedThresholds.step(max);
            for (int requested = -20; requested <= 320; requested += 5) {
                int yellow = SpeedThresholds.yellowStep(requested, maxStep);
                int red = SpeedThresholds.redStep(requested, yellow, maxStep);
                assertTrue(yellow >= 1);
                assertTrue(red > yellow);
                assertTrue(red <= maxStep);
                assertEquals(0, SpeedThresholds.speed(yellow) % 10);
                assertEquals(0, SpeedThresholds.speed(red) % 10);
            }
        }
    }

    @Test
    public void minimumMaximumStillAllowsDistinctThresholds() {
        int maxStep = SpeedThresholds.step(40);
        assertEquals(3, SpeedThresholds.yellowStep(40, maxStep));
        assertEquals(4, SpeedThresholds.redStep(40, 3, maxStep));
    }
}
