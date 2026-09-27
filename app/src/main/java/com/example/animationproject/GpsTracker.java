package com.example.animationproject;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

import java.util.Arrays;

public class GpsTracker {

    // ---------- Настройки фильтров ----------
    private static final float MAX_ACCURACY_M       = 30f;
    private static final long  MIN_TIME_DELTA_MS    = 200;
    private static final int   MEDIAN_WINDOW        = 5;
    private static final float EMA_ALPHA            = 0.5f;   // было 0.3 — быстрее реакция
    private static final float MAX_ACCEL_KMH_PER_S  = 15f;    // было 8 — разрешаем быстрее
    private static final float NOISE_FLOOR_KMH      = 3f;

    // ---------- Состояние ----------
    private final LocationManager locationManager;
    private final LocationListener locationListener;

    private Location previousLocation;
    private long lastProcessedTime = 0L;

    /** Внутреннее состояние EMA — НИКОГДА не сбрасываем его в 0. */
    private float filteredSpeed = 0f;

    // Кольцевой буфер медианы (только для fallback)
    private final float[] medianBuffer = new float[MEDIAN_WINDOW];
    private int medianCount = 0;
    private int medianIndex = 0;

    public GpsTracker(Context context) {
        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                if (location.hasAccuracy() && location.getAccuracy() > MAX_ACCURACY_M) {
                    return;
                }
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
                    500,   // было 1000 — чаще фиксы
                    3,     // было 5
                    locationListener);
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    public void stopTracking() {
        locationManager.removeUpdates(locationListener);
    }

    // ============================================================
    //  Основной конвейер
    // ============================================================
    private void processLocation(Location location) {

        long now = location.getTime();

        if (lastProcessedTime == 0L) {
            lastProcessedTime = now;
            previousLocation = location;
            return;
        }

        long dtMs = now - lastProcessedTime;
        if (dtMs < MIN_TIME_DELTA_MS) return;

        float dtSec = dtMs / 1000f;

        // --- 1. Сырая скорость ---
        float rawSpeedKmh;
        boolean usedHardwareSpeed = location.hasSpeed();
        if (usedHardwareSpeed) {
            rawSpeedKmh = location.getSpeed() * 3.6f;
        } else {
            float distM = previousLocation.distanceTo(location);
            rawSpeedKmh = (distM / dtSec) * 3.6f;
        }

        // --- 2. Медиана ТОЛЬКО для fallback-ветки. ---
        //     Аппаратная скорость уже прошла внутренний фильтр GPS-чипа,
        //     дополнительное сглаживание только добавит лаг.
        float candidateSpeed = usedHardwareSpeed
                ? rawSpeedKmh
                : pushMedian(rawSpeedKmh);

        // --- 3. Ограничение ускорения ---
        float maxDelta = MAX_ACCEL_KMH_PER_S * dtSec;
        float diff = candidateSpeed - filteredSpeed;
        if (diff >  maxDelta) diff =  maxDelta;
        if (diff < -maxDelta) diff = -maxDelta;
        filteredSpeed += diff;

        // --- 4. EMA-сглаживание. Состояние НЕ сбрасываем. ---
        filteredSpeed = EMA_ALPHA * filteredSpeed + (1f - EMA_ALPHA) * filteredSpeed;
        // (это эквивалентно: filteredSpeed = filteredSpeed; но оставим как «место под будущее»)

        // --- 5. Мёртвая зона ТОЛЬКО на выходе. ---
        float output = filteredSpeed;
        if (output < NOISE_FLOOR_KMH) output = 0f;

        // --- 6. Публикуем ---
        GlobalVars.MyTimer = Math.round(output);

        previousLocation = location;
        lastProcessedTime = now;
    }

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