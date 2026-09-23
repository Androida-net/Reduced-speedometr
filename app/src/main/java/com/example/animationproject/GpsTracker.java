package com.example.animationproject;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;

import java.util.Arrays;

public class GpsTracker {

    // ---------- Настройки фильтров ----------
    /** Точки с погрешностью хуже этой — выбрасываем. */
    private static final float MAX_ACCURACY_M = 40f;

    /** Минимальный интервал между обрабатываемыми точками (мс). */
    private static final long MIN_TIME_DELTA_MS = 500;

    /** Размер окна медианного фильтра (нечётное число). */
    private static final int MEDIAN_WINDOW = 5;

    /** Коэффициент EMA: 0..1, меньше — плавнее. 0.3 — хороший старт. */
    private static final float EMA_ALPHA = 0.3f;

    /** Максимальное реальное ускорение, км/ч за секунду (8 ≈ 0–100 за 3.5 с). */
    private static final float MAX_ACCEL_KMH_PER_S = 8f;

    /** Скорости ниже этого — считаем стоянием. */
    private static final float NOISE_FLOOR_KMH = 3f;

    // ---------- Состояние ----------
    private final LocationManager locationManager;
    private final LocationListener locationListener;
    private final Handler handler = new Handler();
    private final Context context;

    private Location previousLocation;
    private long lastProcessedTime = 0L;
    private float filteredSpeed = 0f;   // итоговая сглаженная скорость

    // Кольцевой буфер для медианного фильтра
    private final float[] medianBuffer = new float[MEDIAN_WINDOW];
    private int medianCount = 0;
    private int medianIndex = 0;

    public GpsTracker(Context context) {
        this.context = context;
        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                GlobalVars.currentLatitude  = location.getLatitude();
                GlobalVars.currentLongitude = location.getLongitude();
                processLocation(location);
            }

            @Override public void onStatusChanged(String provider, int status, Bundle extras) {}
            @Override public void onProviderEnabled(String provider) {}
            @Override public void onProviderDisabled(String provider) {}
        };
    }

    public void startTracking() {
        try {
            locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    1000,   // minTime: 1 с
                    5,      // minDistance: 5 м
                    locationListener);
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    public void stopTracking() {
        locationManager.removeUpdates(locationListener);
        handler.removeCallbacksAndMessages(null);
    }

    // ============================================================
    //  Основной конвейер обработки одной точки
    // ============================================================
    private void processLocation(Location location) {

        // --- 1. Отсев по точности ---
        if (location.hasAccuracy() && location.getAccuracy() > MAX_ACCURACY_M) {
            return;
        }

        long now = location.getTime();

        // --- 2. Первая валидная точка — только запоминаем ---
        if (lastProcessedTime == 0L) {
            lastProcessedTime = now;
            previousLocation = location;
            return;
        }

        long dtMs = now - lastProcessedTime;

        // --- 3. Слишком частые точки — игнор ---
        if (dtMs < MIN_TIME_DELTA_MS) return;

        // --- 4. «Сырая» скорость ---
        float rawSpeedKmh;
        if (location.hasSpeed()) {
            // Аппаратная скорость (по Доплеру) — самая надёжная.
            // На 95% устройств доступна и не зависит от джиттера координат.
            rawSpeedKmh = location.getSpeed() * 3.6f;
        } else {
            // Fallback: считаем через distanceTo / dt
            float distM = previousLocation.distanceTo(location);
            rawSpeedKmh = (distM / (dtMs / 1000f)) * 3.6f;
        }

        // --- 5. Медианный фильтр: гасит одиночные выбросы ---
        float medianSpeed = pushMedian(rawSpeedKmh);

        // --- 6. Ограничение ускорения: не быстрее физически возможного ---
        float dtSec = dtMs / 1000f;
        float maxDelta = MAX_ACCEL_KMH_PER_S * dtSec;
        float diff = medianSpeed - filteredSpeed;
        if (diff >  maxDelta) diff =  maxDelta;
        if (diff < -maxDelta) diff = -maxDelta;
        float accelLimited = filteredSpeed + diff;

        // --- 7. EMA-сглаживание ---
        filteredSpeed = EMA_ALPHA * accelLimited + (1f - EMA_ALPHA) * filteredSpeed;

        // --- 8. Мёртвая зона около нуля ---
        if (filteredSpeed < NOISE_FLOOR_KMH) filteredSpeed = 0f;

        // --- 9. Публикуем результат ---
        GlobalVars.MyTimer = Math.round(filteredSpeed);

        // Готовимся к следующей итерации
        previousLocation = location;
        lastProcessedTime = now;
    }

    /** Кольцевой буфер + медиана. Устойчив к одиночным выбросам. */
    private float pushMedian(float value) {
        medianBuffer[medianIndex] = value;
        medianIndex = (medianIndex + 1) % MEDIAN_WINDOW;
        if (medianCount < MEDIAN_WINDOW) medianCount++;

        float[] copy = Arrays.copyOf(medianBuffer, medianCount);
        Arrays.sort(copy);
        if (medianCount % 2 == 0) {
            return 0.5f * (copy[medianCount / 2 - 1] + copy[medianCount / 2]);
        } else {
            return copy[medianCount / 2];
        }
    }
}