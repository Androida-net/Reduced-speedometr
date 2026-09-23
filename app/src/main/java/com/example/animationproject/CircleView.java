package com.example.animationproject;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
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
    private final int[] speedMarkers = {0, 20, 40, 50, 60, 65, 70, 75, 80, 85, 90, 100, 120, 140, 160};
    private final int[] scalePlus    = {140, 140, 160, 185, 210, 240, 255, 275, 290, 305, 300, 275, 254, 240, 240};

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

    // ---------- Константы геометрии (общие для onMeasure и recomputeGeometry) ----------
    private static final float NUMBER_OFFSET = 50f;   // цифра на radius + 50
    private static final float NUMBER_SIZE   = 40f;   // textPaint.setTextSize(40)
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
        textPaint.setTextSize(40);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setAntiAlias(true);

        needlePaint.setColor(Color.YELLOW);
        needlePaint.setStyle(Paint.Style.FILL_AND_STROKE);
        needlePaint.setAntiAlias(true);

        // --- Базовый отступ (без insets — они теперь на корневом FrameLayout) ---
        final float density = getResources().getDisplayMetrics().density;
        final int basePad = (int) (8 * density);
        setPadding(basePad, basePad, basePad, basePad);

        refreshRunnable = new Runnable() {
            @Override
            public void run() {
                if (GlobalVars.MyTimer < GlobalVars.SWarn) {
                    circlePaint.setColor(Color.GREEN);
                } else if (GlobalVars.MyTimer < GlobalVars.SMax) {
                    circlePaint.setColor(Color.YELLOW);
                } else {
                    circlePaint.setColor(Color.RED);
                }
                setProgressPointPosition(GlobalVars.MyTimer);
                handler.postDelayed(this, REFRESH_INTERVAL);
            }
        };
    }

    // ============================================================
    //  onMeasure — сообщаем wrap_content реальную высоту циферблата
    // ============================================================
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width  = MeasureSpec.getSize(widthMeasureSpec);
        int wMode  = MeasureSpec.getMode(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        int hMode  = MeasureSpec.getMode(heightMeasureSpec);

        // Если ширина не задана явно — считать нечего, отдаём решение родителю
        if (wMode != MeasureSpec.EXACTLY) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            return;
        }

        // Радиус, ограниченный шириной — та же формула, что в recomputeGeometry()
        float availW = width - getPaddingLeft() - getPaddingRight();
        float r = (availW - 2f * MARGIN) / 1.732f;
        if (r < 0f) r = 0f;

        // Высота, нужная циферблату: 1.5r (вертикальный габарит сектора) + 2*margin под цифры
        int desiredHeight = (int) (1.5f * r + 2f * MARGIN
                + getPaddingTop() + getPaddingBottom());

        if (hMode == MeasureSpec.AT_MOST) {
            height = Math.min(height, desiredHeight);
        } else if (hMode == MeasureSpec.UNSPECIFIED) {
            height = desiredHeight;
        }
        // hMode == EXACTLY — уважаем то, что просил родитель

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

        float maxRbyH = (availH - 2f * MARGIN) / 1.5f;

        radius = Math.min(maxRbyW, maxRbyH);
        if (radius < 10f) radius = 10f;

        centerX = getPaddingLeft() + availW / 2f;
        centerY = getPaddingTop() + availH / 2f + 0.25f * radius;
    }

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
        invalidate();
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);

        if (radius <= 0f) return;

        if (shouldDrawAllElements) {
            // а) Засечки и цифры
            for (int i = 0; i < speedMarkers.length; i++) {
                float angle = START_ANGLE + (SWEEP_ANGLE * i / (speedMarkers.length - 1));
                float cosA = (float) Math.cos(Math.toRadians(angle));
                float sinA = (float) Math.sin(Math.toRadians(angle));

                float startX = centerX + radius * cosA;
                float startY = centerY + radius * sinA;

                float stopX, stopY;
                if (i % 2 == 0) {
                    stopX = centerX + (radius - 80f) * cosA;
                    stopY = centerY + (radius - 80f) * sinA;
                } else {
                    stopX = centerX + (radius - 50f) * cosA;
                    stopY = centerY + (radius - 50f) * sinA;
                }
                canvas.drawLine(startX, startY, stopX, stopY, markerPaint);

                if (i % 2 == 0) {
                    float textX = centerX + (radius + 50f) * cosA;
                    float textY = centerY + (radius + 50f) * sinA;
                    canvas.drawText(String.valueOf(speedMarkers[i]), textX, textY + 15f, textPaint);
                }
            }

            // б) Стрелка
            float maxValue = speedMarkers[speedMarkers.length - 1];
            float needleAngle = START_ANGLE + (SWEEP_ANGLE * PointPosition / maxValue);
            for (int i = 0; i < speedMarkers.length; i++) {
                if (GlobalVars.MyTimer > speedMarkers[i]) {
                    needleAngle = START_ANGLE + (scalePlus[i] * PointPosition / maxValue);
                }
            }

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

            // в) Центральный кружок
            canvas.drawCircle(centerX, centerY, 25f, needlePaint);
        }

        // Дуга прогресса
        float maxValue = speedMarkers[speedMarkers.length - 1];
        float sweep = SWEEP_ANGLE * progress / maxValue;
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