package com.alexvahabov.nlsmeter;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class SpeedScaleTest {
    @Test
    public void centerHasSelectedMagnificationAgainstBothSides() {
        for (int max = 40; max <= 300; max += 10) {
            float start = max * 60f / 160f;
            float end = max * 100f / 160f;
            for (int factor : GlobalVars.SCALE_FACTORS) {
                float left = SpeedScale.angleOffset(start, max, factor) / start;
                float middle = (SpeedScale.angleOffset(end, max, factor)
                        - SpeedScale.angleOffset(start, max, factor)) / (end - start);
                float right = (SpeedScale.angleOffset(max, max, factor)
                        - SpeedScale.angleOffset(end, max, factor)) / (max - end);
                assertEquals(factor, middle / left, 0.0001f);
                assertEquals(factor, middle / right, 0.0001f);
                assertEquals(120f, SpeedScale.angleOffset(max / 2f, max, factor), 0.0001f);
                assertEquals(0f, SpeedScale.angleOffset(-10f, max, factor), 0f);
                assertEquals(240f, SpeedScale.angleOffset(max + 10f, max, factor), 0f);
            }
        }
    }

    @Test
    public void factorOneIsLinear() {
        assertEquals(75f, SpeedScale.angleOffset(50, 160, 1), 0.0001f);
        assertEquals(135f, SpeedScale.angleOffset(90, 160, 1), 0.0001f);
    }

    @Test
    public void ticksAreRoundSpeedsAndNeverOverlapTooClosely() {
        for (int max = 40; max <= 300; max += 10) {
            for (int factor : GlobalVars.SCALE_FACTORS) {
                List<Integer> ticks = SpeedScale.tickSpeeds(max, factor, 8f);
                assertEquals(Integer.valueOf(0), ticks.get(0));
                assertTrue(ticks.contains(max / 2));
                assertEquals(Integer.valueOf(max), ticks.get(ticks.size() - 1));
                for (int i = 1; i < ticks.size(); i++) {
                    assertEquals(0, ticks.get(i) % 5);
                    float gap = SpeedScale.angleOffset(ticks.get(i), max, factor)
                            - SpeedScale.angleOffset(ticks.get(i - 1), max, factor);
                    assertTrue("tick gap: " + gap, gap >= 8f - 0.0001f);
                }
            }
        }
    }

    @Test
    public void defaultSettingsAndUnknownFactor() {
        assertEquals(160, GlobalVars.DEFAULT_MAX_SPEED);
        assertEquals(70, GlobalVars.SWarn);
        assertEquals(80, GlobalVars.SMax);
        assertEquals(4, GlobalVars.DEFAULT_SCALE_FACTOR);
        assertEquals(4, SpeedScale.validFactor(3));
    }

    @Test
    public void linear160HasFourLabelsOnEachSide() {
        assertEquals(java.util.Arrays.asList(0, 20, 40, 60, 80, 100, 120, 140, 160),
                SpeedScale.labelSpeeds(160, 1));
    }

    @Test
    public void scale150WithFactor4Includes80OnRightOfCenter() {
        List<Integer> labels = SpeedScale.labelSpeeds(150, 4);
        assertEquals(Integer.valueOf(75), labels.get(4));
        assertEquals(Integer.valueOf(80), labels.get(5));
    }

    @Test
    public void fourPerSideAndCenterAtEverySetting() {
        for (int max = 40; max <= 300; max += 10) {
            for (int factor : GlobalVars.SCALE_FACTORS) {
                List<Integer> labels = SpeedScale.labelSpeeds(max, factor);
                assertEquals(9, labels.size());
                assertEquals(Integer.valueOf(0), labels.get(0));
                assertEquals(Integer.valueOf(max / 2), labels.get(4));
                assertEquals(Integer.valueOf(max), labels.get(8));
                for (int i = 0; i < labels.size(); i++) {
                    int speed = labels.get(i);
                    assertEquals(0, speed % 5);
                    if (i > 0) assertTrue(labels.get(i - 1) < speed);
                }
            }
        }
    }
}
