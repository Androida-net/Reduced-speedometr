package com.alexvahabov.nlsmeter;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

public class CircleView extends View {

    // ---------- Геометрия (считается в onSizeChanged) ----------
    private float centerX;
    private float centerY;
    private float radius;

    // Центральная зона 60–100 из 160 масштабируется вместе с максимумом.
    private int scaleFactor = GlobalVars.DEFAULT_SCALE_FACTOR;
    private List<Integer> speedLabels = SpeedScale.labelSpeeds(GlobalVars.MAX_SPEED, scaleFactor);

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

    // ---------- Константы геометрии ----------
    private static final float NUMBER_OFFSET = 50f;
    private static final float NUMBER_SIZE   = 54f;
    private static final float MIN_NUMBER_SIZE = 12f;
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

    private float speedToAngleOffset(float speed) {
        return SpeedScale.angleOffset(speed, GlobalVars.MAX_SPEED, scaleFactor);
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
        this.progress = Math.min(progress, GlobalVars.MAX_SPEED);
        invalidate();
    }

    public void setProgressPointPosition(float PointPosition) {
        this.PointPosition = Math.min(PointPosition, GlobalVars.MAX_SPEED);
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
     * Границы «подробной» зоны масштабируются пропорционально.
     */
    public void setMaxSpeed(int newMax) {
        newMax = Math.max(GlobalVars.MIN_MAX_SPEED,
                Math.min(GlobalVars.MAX_MAX_SPEED, newMax));
        GlobalVars.MAX_SPEED = newMax;
        speedLabels = SpeedScale.labelSpeeds(newMax, scaleFactor);

        if (PointPosition > newMax) PointPosition = newMax;
        if (progress     > newMax) progress     = newMax;
        invalidate();
    }
    /** 1 — линейная шкала; остальные значения увеличивают масштаб центральной зоны. */
    public void setScaleFactor(int factor) {
        scaleFactor = SpeedScale.validFactor(factor);
        GlobalVars.SCALE_FACTOR = scaleFactor;
        speedLabels = SpeedScale.labelSpeeds(GlobalVars.MAX_SPEED, scaleFactor);
        invalidate();
    }

    private RectF labelBounds(int speed, Paint.FontMetrics metrics) {
        float angle = START_ANGLE + speedToAngleOffset(speed);
        float x = centerX + (radius + NUMBER_OFFSET)
                * (float) Math.cos(Math.toRadians(angle));
        float baseline = centerY + (radius + NUMBER_OFFSET)
                * (float) Math.sin(Math.toRadians(angle))
                - (metrics.ascent + metrics.descent) / 2f;
        float halfWidth = textPaint.measureText(String.valueOf(speed)) / 2f;
        return new RectF(x - halfWidth - 2f, baseline + metrics.ascent - 2f,
                x + halfWidth + 2f, baseline + metrics.descent + 2f);
    }

    /** Подгоняет размер цифр под девять подписей, не меняя их выбранные скорости. */
    private void fitLabelText(List<Integer> labels) {
        for (float size = NUMBER_SIZE; size >= MIN_NUMBER_SIZE; size -= 2f) {
            textPaint.setTextSize(size);
            Paint.FontMetrics metrics = textPaint.getFontMetrics();
            List<RectF> occupied = new ArrayList<>();
            boolean overlaps = false;
            for (int speed : labels) {
                RectF bounds = labelBounds(speed, metrics);
                for (RectF other : occupied) {
                    if (RectF.intersects(bounds, other)) {
                        overlaps = true;
                        break;
                    }
                }
                if (overlaps) break;
                occupied.add(bounds);
            }
            if (!overlaps) return;
        }
    }


    // ============================================================
    //  Отрисовка
    // ============================================================
    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);

        if (radius <= 0f) return;

        if (shouldDrawAllElements) {
            // Круглые значения через 5 км/ч; в сжатых областях убираем тесные засечки.
            float minAngleGap = (float) Math.toDegrees(16f / radius);
            List<Integer> labels = speedLabels;
            fitLabelText(labels);
            TreeSet<Integer> ticks = new TreeSet<>(SpeedScale.tickSpeeds(GlobalVars.MAX_SPEED,
                    scaleFactor, minAngleGap));
            ticks.addAll(labels);
            for (int speed : ticks) {
                boolean labeled = labels.contains(speed);
                float angleOffset = speedToAngleOffset(speed);
                float angle = START_ANGLE + angleOffset;
                float cosA = (float) Math.cos(Math.toRadians(angle));
                float sinA = (float) Math.sin(Math.toRadians(angle));

                float startX = centerX + radius * cosA;
                float startY = centerY + radius * sinA;

                float stopX, stopY;
                if (labeled) {
                    stopX = centerX + (radius - 80f) * cosA;
                    stopY = centerY + (radius - 80f) * sinA;
                } else {
                    stopX = centerX + (radius - 50f) * cosA;
                    stopY = centerY + (radius - 50f) * sinA;
                }
                canvas.drawLine(startX, startY, stopX, stopY, markerPaint);

                if (labeled) {
                    float textX = centerX + (radius + NUMBER_OFFSET) * cosA;
                    float textY = centerY + (radius + NUMBER_OFFSET) * sinA;
                    Paint.FontMetrics metrics = textPaint.getFontMetrics();
                    canvas.drawText(String.valueOf(speed), textX,
                            textY - (metrics.ascent + metrics.descent) / 2f, textPaint);
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
