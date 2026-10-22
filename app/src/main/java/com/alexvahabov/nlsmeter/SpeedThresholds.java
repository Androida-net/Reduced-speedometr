package com.alexvahabov.nlsmeter;

/** Шаги движков настройки скоростей: один шаг равен 10 км/ч. */
final class SpeedThresholds {
    private SpeedThresholds() {}

    static int step(int speed) {
        return Math.round(speed / (float) GlobalVars.STEP_MAX_SPEED);
    }

    static int speed(int step) {
        return step * GlobalVars.STEP_MAX_SPEED;
    }

    static int maxSpeed(int requested) {
        int step = step(requested);
        return speed(clamp(step, step(GlobalVars.MIN_MAX_SPEED),
                step(GlobalVars.MAX_MAX_SPEED)));
    }

    static int yellowStep(int speed, int maxStep) {
        return clamp(step(speed), 1, maxStep - 1);
    }

    static int redStep(int speed, int yellowStep, int maxStep) {
        return clamp(step(speed), yellowStep + 1, maxStep);
    }

    static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(value, max));
    }
}
