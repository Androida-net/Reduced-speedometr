package com.example.animationproject;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class CircleView extends View {

    // ---------- Геометрия (считается в onSizeChanged) ----------
    private float centerX;
    private float centerY;
    private float radius;

    // ---------- Данные шкалы ----------

    /** Базовые метки для MAX_SPEED = 160. При другом максимуме масштабируются пропорционально. */

    private static final int BASE_MAX = 160;

    /** Набор для нелинейного режима: сгущение в зоне 60–90. */
    private static final int[] BASE_MARKERS_NL =
            {0, 20, 40, 60, 65, 70, 75, 80, 85, 90, 100, 120, 140, 160};

    /** Набор для линейного режима: только круглые значения с равным шагом. */
    private static final int[] BASE_MARKERS_LIN =
            {0, 20, 40, 60, 80, 100, 120, 140, 160};

    /** Текущие метки (пересобираются в setMaxSpeed). */
    private int[] speedMarkers = BASE_MARKERS_NL.clone();

    // ============================================================
    //  НЕЛИНЕЙНАЯ ШКАЛА (кусочно-линейное отображение скорость → угол)
    // ============================================================
    //  Средняя треть дуги отводится диапазону 0.375…0.5625 от MAX_SPEED
    //  (для MAX_SPEED = 160 это 60…90 км/ч) — получается 2× подробнее,
    //  чем в первой и последней трети.
    //
    //  Чтобы сдвинуть «интересную зону», меняй дроби в SPEED_FRACTIONS:
    //    0.30 / 0.55 — подробно с 48 до 88 км/ч при MAX_SPEED = 160
    //    0.45 / 0.70 — подробно с 72 до 112 км/ч
    // ============================================================

    // --- Набор для нелинейной шкалы ---
    private static final float[] SPEED_FRACTIONS_NL = {0f, 0.375f, 0.5625f, 1f};
    private static final float[] ANGLE_FRACTIONS_NL = {0f, 1f / 3f, 2f / 3f, 1f};

    // --- Набор для линейной шкалы (одна прямая 0..MAX) ---
    private static final float[] SPEED_FRACTIONS_LIN = {0f, 1f};
    private static final float[] ANGLE_FRACTIONS_LIN = {0f, 1f};

    /** Текущий режим шкалы. Управляется из Activity через setNonlinear(). */
    private boolean nonlinear = true;

    // ---------- Состояние ----------
    private float progress = 0f;
    private float PointPosition = 0f;
    private boolean shouldDrawAllElements = false;

    // ---------- Paint ----------
    private final Paint circlePaint = new Paint();
    private final Paint markerPaint = new Paint();
    private final Paint textPaint   = new Paint();
    private final Paint needlePaint = new Paint();

    // ---------- Анимация ----------
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable refreshRunnable;
    private static final int REFRESH_INTERVAL  = 2000;
    private static final int ELEMENTS_DELAY_MS = 2000;

    // ---------- Константы шкалы ----------
    private static final float START_ANGLE = 150f;
    private static final float SWEEP_ANGLE = 240f;

    // ---------- Константы геометрии ----------
    private static final float NUMBER_OFFSET = 50f;
    private static final float NUMBER_SIZE   = 54f;
    private static final float MARGIN        = NUMBER_OFFSET + NUMBER_SIZE;

    // ---------- Конструкторы ----------
    public CircleView(Context context) {
        super(context);
        init();
    }
    public CircleView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }
    public CircleView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        circlePaint.setColor(Color.GREEN);
        circlePaint.setStyle(Paint.Style.STROKE);
        circlePaint.setStrokeWidth(20);
        circlePaint.setAntiAlias(true);
        circlePaint.setStrokeCap(Paint.Cap.ROUND);

        markerPaint.setColor(Color.WHITE);
        markerPaint.setStyle(Paint.Style.STROKE);
        markerPaint.setStrokeWidth(3);
        markerPaint.setAntiAlias(true);

        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(NUMBER_SIZE);
        textPaint.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setAntiAlias(true);

        needlePaint.setColor(Color.YELLOW);
        needlePaint.setStyle(Paint.Style.FILL_AND_STROKE);
        needlePaint.setAntiAlias(true);

        final float density = getResources().getDisplayMetrics().density;
        final int basePad = (int) (8 * density);
        setPadding(basePad, basePad, basePad, basePad);

        refreshRunnable = new Runnable() {
            @Override
            public void run() {
                setProgressPointPosition(GlobalVars.MyTimer);
                handler.postDelayed(this, REFRESH_INTERVAL);
            }
        };
    }

    // ============================================================
    //  Кусочно-линейное отображение: скорость → угол (от START_ANGLE)
    // ============================================================
    /**
     * @param speed значение в км/ч
     * @return угловое смещение от START_ANGLE, в градусах (0..SWEEP_ANGLE)
     */
    private float speedToAngleOffset(float speed) {
        float maxSpeed = GlobalVars.MAX_SPEED;
        if (maxSpeed <= 0f) return 0f;
        if (speed <= 0f)        return 0f;
        if (speed >= maxSpeed)  return SWEEP_ANGLE;

        float[] speedFractions = nonlinear ? SPEED_FRACTIONS_NL : SPEED_FRACTIONS_LIN;
        float[] angleFractions = nonlinear ? ANGLE_FRACTIONS_NL : ANGLE_FRACTIONS_LIN;

        for (int i = 0; i < speedFractions.length - 1; i++) {
            float sLo = speedFractions[i]     * maxSpeed;
            float sHi = speedFractions[i + 1] * maxSpeed;
            if (speed <= sHi) {
                float aLo = angleFractions[i]     * SWEEP_ANGLE;
                float aHi = angleFractions[i + 1] * SWEEP_ANGLE;
                if (sHi <= sLo) return aLo;
                return aLo + (aHi - aLo) * (speed - sLo) / (sHi - sLo);
            }
        }
        return SWEEP_ANGLE;
    }

    // ============================================================
    //  onMeasure
    // ============================================================
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width  = MeasureSpec.getSize(widthMeasureSpec);
        int wMode  = MeasureSpec.getMode(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        int hMode  = MeasureSpec.getMode(heightMeasureSpec);

        if (wMode != MeasureSpec.EXACTLY) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            return;
        }

        float availW = width - getPaddingLeft() - getPaddingRight();
        float r = (availW - 2f * MARGIN) / 1.732f;
        if (r < 0f) r = 0f;

        int desiredHeight = (int) (1.5f * r + 2f * MARGIN
                + getPaddingTop() + getPaddingBottom());

        if (hMode == MeasureSpec.AT_MOST) {
            height = Math.min(height, desiredHeight);
        } else if (hMode == MeasureSpec.UNSPECIFIED) {
            height = desiredHeight;
        }
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        handler.post(refreshRunnable);
        handler.postDelayed(() -> {
            shouldDrawAllElements = true;
            invalidate();
        }, ELEMENTS_DELAY_MS);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        handler.removeCallbacksAndMessages(null);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        recomputeGeometry(w, h);
    }

    private void recomputeGeometry(int w, int h) {
        float availW = w - getPaddingLeft() - getPaddingRight();
        float availH = h - getPaddingTop()  - getPaddingBottom();
        if (availW <= 0 || availH <= 0) return;

        float maxRbyWarc = (availW - 2f * MARGIN) / 1.732f;
        float maxRbyWnum = availW / 2f - MARGIN;
        float maxRbyW    = Math.min(maxRbyWarc, maxRbyWnum);
        float maxRbyH    = (availH - 2f * MARGIN) / 1.5f;

        radius = Math.min(maxRbyW, maxRbyH);
        if (radius < 10f) radius = 10f;

        centerX = getPaddingLeft() + availW / 2f;
        centerY = getPaddingTop() + availH / 2f + 0.25f * radius;
    }

    // ============================================================
    //  Публичный API
    // ============================================================
    public void setTextColor(int colorNum, int ColorCir) {
        circlePaint.setColor(ColorCir);
        textPaint.setColor(colorNum);
        markerPaint.setColor(colorNum);
        invalidate();
    }

    public void setProgress(float progress) {
        this.progress = Math.min(progress, speedMarkers[speedMarkers.length - 1]);
        invalidate();
    }

    public void setProgressPointPosition(float PointPosition) {
        this.PointPosition = Math.min(PointPosition, speedMarkers[speedMarkers.length - 1]);
        if (GlobalVars.MyTimer < GlobalVars.SWarn) {
            circlePaint.setColor(Color.GREEN);
        } else if (GlobalVars.MyTimer < GlobalVars.SMax) {
            circlePaint.setColor(Color.YELLOW);
        } else {
            circlePaint.setColor(Color.RED);
        }
        invalidate();
    }

    /**
     * Задать максимальную скорость шкалы.
     * Метки масштабируются пропорционально; границы «подробной» зоны тоже.
     */
    public void setMaxSpeed(int newMax) {
        newMax = Math.max(GlobalVars.MIN_MAX_SPEED,
                Math.min(GlobalVars.MAX_MAX_SPEED, newMax));
        GlobalVars.MAX_SPEED = newMax;

        rebuildMarkers();

        if (PointPosition > newMax) PointPosition = newMax;
        if (progress     > newMax) progress     = newMax;
        invalidate();
    }
    /** Пересобирает speedMarkers под текущие nonlinear + GlobalVars.MAX_SPEED. */
    private void rebuildMarkers() {
        int[] base = nonlinear ? BASE_MARKERS_NL : BASE_MARKERS_LIN;
        int newMax = GlobalVars.MAX_SPEED;
        if (newMax <= 0) newMax = BASE_MAX;

        int[] scaled = new int[base.length];
        for (int i = 0; i < base.length; i++) {
            scaled[i] = Math.round(base[i] * (float) newMax / BASE_MAX);
        }
        scaled[scaled.length - 1] = newMax;
        speedMarkers = scaled;
    }
    /**
     * Включить/выключить нелинейную шкалу.
     * Пересобирает набор меток под выбранный режим и перерисовывает циферблат.
     */
    public void setNonlinear(boolean nonlinear) {
        this.nonlinear = nonlinear;
        GlobalVars.NONLINEAR = nonlinear;
        rebuildMarkers();
        invalidate();
    }


    // ============================================================
    //  Отрисовка
    // ============================================================
    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);

        if (radius <= 0f) return;

        if (shouldDrawAllElements) {
            // ----- а) Засечки и цифры (нелинейно по speedToAngleOffset) -----
            for (int i = 0; i < speedMarkers.length; i++) {
                float angleOffset = speedToAngleOffset(speedMarkers[i]);
                float angle = START_ANGLE + angleOffset;
                float cosA = (float) Math.cos(Math.toRadians(angle));
                float sinA = (float) Math.sin(Math.toRadians(angle));

                float startX = centerX + radius * cosA;
                float startY = centerY + radius * sinA;

                float stopX, stopY;
                if (i % 2 == 0 || i == speedMarkers.length - 1)  {
                    stopX = centerX + (radius - 80f) * cosA;
                    stopY = centerY + (radius - 80f) * sinA;
                } else {
                    stopX = centerX + (radius - 50f) * cosA;
                    stopY = centerY + (radius - 50f) * sinA;
                }
                canvas.drawLine(startX, startY, stopX, stopY, markerPaint);

                if (!nonlinear || i % 2 == 0 || i == speedMarkers.length - 1) {
                    float textX = centerX + (radius + NUMBER_OFFSET) * cosA;
                    float textY = centerY + (radius + NUMBER_OFFSET) * sinA;
                    canvas.drawText(String.valueOf(speedMarkers[i]), textX, textY + 15f, textPaint);
                }
            }

            // ----- б) Стрелка (тот же кусочно-линейный маппинг) -----
            float needleAngle = START_ANGLE + speedToAngleOffset(PointPosition);
            float needleLength = radius - 10f;

            Path needlePath = new Path();
            needlePath.moveTo(centerX, centerY);
            needlePath.lineTo(
                    centerX + needleLength * (float) Math.cos(Math.toRadians(needleAngle)),
                    centerY + needleLength * (float) Math.sin(Math.toRadians(needleAngle))
            );
            float backAngle1 = needleAngle + 150f;
            float backAngle2 = needleAngle - 150f;
            needlePath.lineTo(
                    centerX + 25f * (float) Math.cos(Math.toRadians(backAngle1)),
                    centerY + 25f * (float) Math.sin(Math.toRadians(backAngle1))
            );
            needlePath.lineTo(
                    centerX + 25f * (float) Math.cos(Math.toRadians(backAngle2)),
                    centerY + 25f * (float) Math.sin(Math.toRadians(backAngle2))
            );
            needlePath.close();

            needlePaint.setStrokeWidth(3f);
            canvas.drawPath(needlePath, needlePaint);

            // ----- в) Центральный кружок -----
            canvas.drawCircle(centerX, centerY, 25f, needlePaint);
        }

        // ----- Дуга прогресса (тот же маппинг) -----
        float sweep = speedToAngleOffset(progress);
        canvas.drawArc(
                centerX - radius,
                centerY - radius,
                centerX + radius,
                centerY + radius,
                START_ANGLE,
                sweep,
                false,
                circlePaint
        );
    }
}
