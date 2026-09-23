package com.example.animationproject;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.View;

public class NumberView extends View {
    private int currentNumber = 0;
    private final Paint textPaint = new Paint();
    private final Handler handler = new Handler();
    private final Runnable updateRunnable = new Runnable() {
        @Override
        public void run() {
            currentNumber++;
            //GlobalVars.MyTimer=currentNumber;                                        // для отладки раскоментировать 1/2
            if (currentNumber >=240*3600) {                                                  // после 10 дней счетчик обнуляется
                currentNumber=0;
            }
            invalidate();                                                                    // Перерисовываем View
            handler.postDelayed(this, 1000);                                    // Повторяем черещ секунду
        }
    };
    public NumberView(Context context) {
        super(context);
        init();
    }
    public NumberView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }
    private void init() {
        textPaint.setColor(Color.GRAY);

        // База — 13dp. На планшетах/больших экранах шрифт не станет крохотным.
        float density = getResources().getDisplayMetrics().density;
        textPaint.setTextSize(13f * density);

        textPaint.setTextAlign(Paint.Align.RIGHT);
        textPaint.setAntiAlias(true);
    }
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (w <= 0) return;

        float density = getResources().getDisplayMetrics().density;

        // Хотим, чтобы строка уложилась в ~92% ширины View.
        // Приблизительная длина строки в символах ~46, средняя ширина глифа ~0.55 * size.
        float targetByWidth = (w * 0.92f) / 46f / 0.55f;

        // Нижний предел — 12dp, верхний — 20dp
        float min = 16f * density;
        float max = 32f * density;

        float size = Math.max(min, Math.min(max, targetByWidth));
        textPaint.setTextSize(size);
    }
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        String TimerText=":";                                                       // отделитель секунд от минут
        String GPSText="";
        if ((currentNumber % 3600) % 60 < 10 ){
            TimerText = TimerText + "0";                                            // добавление ведущего нуля у секунд
        }
        TimerText = TimerText + String.valueOf(currentNumber % 60);              // добавление секунд - остатка от деления на 60
        TimerText = String.valueOf((currentNumber / 60) % 60) + TimerText;       // получаем число минут и берем от него остаток от деления на 60 - единицв иинут
        if ((currentNumber / 60) % 60 < 10) {                                      // ведущий ноль у единиц минут
            TimerText = "0" + TimerText;
        }
        TimerText = String.valueOf(currentNumber / 3600) + ":" + TimerText;     // единицы часов с отделителем от минут
        if (currentNumber / 3600 < 10) {
            TimerText = "0" + TimerText;
        }
        GPSText = " GPS:" + String.format("%.4f",  GlobalVars.currentLatitude) + "º, ";
        GPSText = GPSText + String.format("%.4f",  GlobalVars.currentLongitude) + "º";
        int width = getWidth();                                                    // Получаем размеры View
        int height = getHeight();
        canvas.drawText(                                                            // Рисуем число в центре экрана
                "Work time: " + TimerText + GPSText,
                width,
                height,
                textPaint
        );
    }
    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        handler.post(updateRunnable);                                       // Запускаем обновление при подключении к окну
    }
    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        handler.removeCallbacks(updateRunnable);                            // Останавливаем обновление при отключении от окна
    }
}