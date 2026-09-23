package com.example.animationproject;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.View;

public class BigTextView extends View {

    private final Paint bigtextPaint = new Paint();
    private final Handler handler = new Handler();

    private final Runnable updateRunnable = new Runnable() {
        @Override
        public void run() {
            invalidate();
            handler.postDelayed(this, 1000);
        }
    };

    // --- конструкторы ---
    public BigTextView(Context context) {
        super(context);
        initBigText();
    }
    public BigTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initBigText();
    }
    public BigTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initBigText();
    }

    private void initBigText() {
        bigtextPaint.setColor(Color.GREEN);
        // размер задаётся в onSizeChanged — здесь только дефолт
        bigtextPaint.setTextAlign(Paint.Align.CENTER);
        bigtextPaint.setAntiAlias(true);
    }

    /**
     * Пересчитываем размер шрифта под фактическую высоту View.
     * 0.75 — с запасом под ascent/descent, чтобы глиф не вылезал за границы.
     */
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (h <= 0 || w <= 0) return;

        float density = getResources().getDisplayMetrics().density;

        // Нижняя граница — от высоты View (как было), верхняя — фикс в dp.
        float byHeight = h * 0.75f;
        float maxDp    = 200f * density;   // ← верхний предел (~120dp)
        float byWidth  = w / 1.6f;         // чтобы даже "999" не вылезало по ширине

        float size = Math.min(byHeight, Math.min(maxDp, byWidth));
        bigtextPaint.setTextSize(size);
    }
    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        handler.post(updateRunnable);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        handler.removeCallbacks(updateRunnable);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Цвет в зависимости от скорости
        if (GlobalVars.MyTimer >= GlobalVars.SWarn && GlobalVars.MyTimer < GlobalVars.SMax) {
            bigtextPaint.setColor(Color.YELLOW);
        } else if (GlobalVars.MyTimer > GlobalVars.SMax) {
            bigtextPaint.setColor(Color.RED);
        } else {
            bigtextPaint.setColor(Color.GREEN);
        }

        int width  = getWidth();
        int height = getHeight();

        // Точная вертикальная центровка глифа внутри View
        float yPos = height / 2f - (bigtextPaint.descent() + bigtextPaint.ascent()) / 2f;

        canvas.drawText(
                String.valueOf(GlobalVars.MyTimer),
                width / 2f,
                yPos,
                bigtextPaint
        );
    }
}