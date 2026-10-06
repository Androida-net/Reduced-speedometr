package com.example.animationproject;

public class GlobalVars {
    public static int MyTimer = 0;                              // Скорость по GPS
    public static int SWarn = 74;                               // Предупреждение (желтым)
    public static int SMax = 80;                                // превышение (красным)

    public static double currentLatitude;                       // Широта (-90.0 до +90.0)
    public static double currentLongitude;                      // Долгота (-180.0 до +180.0)

    // ---------- Настройки шкалы ----------
    public static final String PREFS_NAME    = "gps_prefs";
    public static final String KEY_MAX_SPEED = "max_speed";
    public static final String KEY_SWARN     = "swarn";
    public static final String KEY_SMAX      = "smax";

    public static final int DEFAULT_MAX_SPEED = 160;
    public static final int MIN_MAX_SPEED     = 40;
    public static final int MAX_MAX_SPEED     = 300;
    public static final int STEP_MAX_SPEED    = 10;

    /** Текущий максимум шкалы (км/ч). */
    public static int MAX_SPEED = DEFAULT_MAX_SPEED;
    public static final String KEY_NONLINEAR = "nonlinear";

    /** true — нелинейная шкала (подробнее в средней зоне), false — линейная. */
    public static boolean NONLINEAR = true;
}