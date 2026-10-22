package com.alexvahabov.nlsmeter;

import java.util.ArrayList;
import java.util.List;

/** Геометрия шкалы в градусах; не зависит от Android и используется стрелкой и отметками. */
final class SpeedScale {
    static final float SWEEP_ANGLE = 240f;
    // Три внутренние позиции в каждой половине дуги (концы и центр добавляются отдельно).
    private static final float[] LEFT_LABEL_ANGLES = {30f, 60f, 90f};
    private static final float[] RIGHT_LABEL_ANGLES = {150f, 180f, 210f};
    // При максимуме 160 детальная зона 60–100: по 60 км/ч остаётся с обоих краёв.
    private static final float DETAIL_START = 60f / 160f;
    private static final float DETAIL_END = 100f / 160f;

    private SpeedScale() {}

    static int validFactor(int factor) {
        for (int option : GlobalVars.SCALE_FACTORS) {
            if (factor == option) return factor;
        }
        return GlobalVars.DEFAULT_SCALE_FACTOR;
    }

    static float angleOffset(float speed, int maxSpeed, int factor) {
        if (maxSpeed <= 0 || speed <= 0f) return 0f;
        if (speed >= maxSpeed) return SWEEP_ANGLE;

        float start = DETAIL_START * maxSpeed;
        float end = DETAIL_END * maxSpeed;
        int weight = validFactor(factor);
        float total = start + weight * (end - start) + maxSpeed - end;
        float weightedSpeed;
        if (speed <= start) {
            weightedSpeed = speed;
        } else if (speed <= end) {
            weightedSpeed = start + weight * (speed - start);
        } else {
            weightedSpeed = start + weight * (end - start) + speed - end;
        }
        return SWEEP_ANGLE * weightedSpeed / total;
    }

    /** Каждая потенциальная отметка соответствует круглой скорости, а не доле угла. */
    static List<Integer> tickSpeeds(int maxSpeed, int factor, float minAngleGap) {
        List<Integer> ticks = new ArrayList<>();
        if (maxSpeed <= 0) return ticks;
        ticks.add(0);
        float previousAngle = 0f;
        // Настройка максимума кратна 10, поэтому половина всегда круглая (0 или 5).
        boolean roundCenter = maxSpeed % 10 == 0;
        int centerSpeed = maxSpeed / 2;
        for (int speed = 5; speed < maxSpeed; speed += 5) {
            float angle = angleOffset(speed, maxSpeed, factor);
            if (roundCenter && speed != centerSpeed
                    && Math.abs(angle - SWEEP_ANGLE / 2f) < minAngleGap) continue;
            if (angle - previousAngle >= minAngleGap
                    && SWEEP_ANGLE - angle >= minAngleGap) {
                ticks.add(speed);
                previousAngle = angle;
            }
        }
        ticks.add(maxSpeed);
        return ticks;
    }

    /** Ровно четыре круглые подписи с каждой стороны и одна посередине. */
    static List<Integer> labelSpeeds(int maxSpeed, int factor) {
        List<Integer> labels = new ArrayList<>();
        int center = maxSpeed / 2;
        labels.add(0);
        addSideLabels(labels, maxSpeed, factor, 5, center, LEFT_LABEL_ANGLES);
        labels.add(center);
        addSideLabels(labels, maxSpeed, factor, center + 5, maxSpeed, RIGHT_LABEL_ANGLES);
        labels.add(maxSpeed);
        return labels;
    }

    private static void addSideLabels(List<Integer> labels, int maxSpeed, int factor,
                                      int first, int limit, float[] targets) {
        float bestScore = Float.MAX_VALUE;
        int firstBest = -1, secondBest = -1, thirdBest = -1;
        for (int a = first; a < limit - 10; a += 5) {
            for (int b = a + 5; b < limit - 5; b += 5) {
                for (int c = b + 5; c < limit; c += 5) {
                    float score = labelCost(a, targets[0], maxSpeed, factor)
                            + labelCost(b, targets[1], maxSpeed, factor)
                            + labelCost(c, targets[2], maxSpeed, factor);
                    if (score < bestScore) {
                        bestScore = score;
                        firstBest = a;
                        secondBest = b;
                        thirdBest = c;
                    }
                }
            }
        }
        if (firstBest >= 0) {
            labels.add(firstBest);
            labels.add(secondBest);
            labels.add(thirdBest);
        }
    }

    private static float labelCost(int speed, float targetAngle, int maxSpeed, int factor) {
        float deviation = angleOffset(speed, maxSpeed, factor) - targetAngle;
        // Предпочитаем десятки, но используем пятёрки, если они заметно лучше размещаются.
        return deviation * deviation + (speed % 10 == 0 ? 0f : 144f);
    }
}
