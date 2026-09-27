package com.example.animationproject;
import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;

import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.util.Log;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.widget.TextView;

// Для замены макс.скорости и диалога ее установки и хранения.
import android.content.SharedPreferences;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import androidx.appcompat.app.AlertDialog;

// Надо повесить на таймер отображение больших цифр
public class MainActivity extends AppCompatActivity {
    private TextView textView;
    private CircleView circleView;
    private GpsTracker gpsTracker;
    private BigTextView bigtextView;

    private void showMaxSpeedDialog() {                 // описанеие диалога установки макс скорости
        final int steps = (GlobalVars.MAX_MAX_SPEED - GlobalVars.MIN_MAX_SPEED)
                / GlobalVars.STEP_MAX_SPEED;

        float density = getResources().getDisplayMetrics().density;
        int pad = (int) (16 * density);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(pad, pad, pad, pad);

        final TextView label = new TextView(this);
        label.setTextSize(22f);
        label.setText(GlobalVars.MAX_SPEED + " км/ч");
        label.setPadding(0, 0, 0, pad / 2);
        layout.addView(label);

        SeekBar sb = new SeekBar(this);
        sb.setMax(steps);
        sb.setProgress((GlobalVars.MAX_SPEED - GlobalVars.MIN_MAX_SPEED)
                / GlobalVars.STEP_MAX_SPEED);
        sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean fromUser) {
                int val = GlobalVars.MIN_MAX_SPEED + p * GlobalVars.STEP_MAX_SPEED;
                label.setText(val + " км/ч");
            }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) {}
        });
        layout.addView(sb);

        new AlertDialog.Builder(this)
                .setTitle("Максимальная скорость")
                .setView(layout)
                .setPositiveButton("ОК", (d, w) -> {
                    int val = GlobalVars.MIN_MAX_SPEED
                            + sb.getProgress() * GlobalVars.STEP_MAX_SPEED;
                    applyMaxSpeed(val);
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void applyMaxSpeed(int newMax) {
        GlobalVars.MAX_SPEED = newMax;

        // Пороги SWarn/SMax — пропорционально новому максимуму.
        // При MAX=160 даёт ровно 74 и 80, как было.
        GlobalVars.SWarn = Math.round(newMax * (74f / 160f));
        GlobalVars.SMax  = Math.round(newMax * (80f / 160f));

        getSharedPreferences(GlobalVars.PREFS_NAME, MODE_PRIVATE)
                .edit()
                .putInt(GlobalVars.KEY_MAX_SPEED, GlobalVars.MAX_SPEED)
                .putInt(GlobalVars.KEY_SWARN,     GlobalVars.SWarn)
                .putInt(GlobalVars.KEY_SMAX,      GlobalVars.SMax)
                .apply();

        circleView.setMaxSpeed(newMax);
        circleView.setProgress(newMax);          // перерисовка дуги под новый максимум
        circleView.invalidate();
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textView = findViewById(R.id.textView);
        circleView = findViewById(R.id.circleView);
        // --- Восстанавливаем настройки из SharedPreferences ---
        SharedPreferences prefs = getSharedPreferences(GlobalVars.PREFS_NAME, MODE_PRIVATE);
        GlobalVars.MAX_SPEED = prefs.getInt(GlobalVars.KEY_MAX_SPEED,
                GlobalVars.DEFAULT_MAX_SPEED);
        GlobalVars.SWarn = prefs.getInt(GlobalVars.KEY_SWARN, GlobalVars.SWarn);
        GlobalVars.SMax  = prefs.getInt(GlobalVars.KEY_SMAX,  GlobalVars.SMax);
        circleView.setMaxSpeed(GlobalVars.MAX_SPEED);

// Долгое нажатие на циферблат открывает настройку
        circleView.setOnLongClickListener(v -> {
            showMaxSpeedDialog();
            return true;
        });
        bigtextView = findViewById(R.id.bigtextView);
        bigtextView.setVisibility(BigTextView.INVISIBLE );

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);                   // Удерживать экран включенным

        // Проверка и запрос разрешений к GPS
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }
        gpsTracker = new GpsTracker(this);
        gpsTracker.startTracking();
        startTextAnimation();                                       // Запуск заставки (анимации "Go")
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        gpsTracker.stopTracking();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            gpsTracker.startTracking();
        }
    }

    private void startTextAnimation() {
    //    circleView.setVisibility(CircleView.GONE);                  // Выключаем форму с кругом

        // Анимация масштабирования текста
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(textView, "scaleX", 0f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(textView, "scaleY", 0f, 1f);

        scaleX.setDuration(1000);
        scaleY.setDuration(1000);
        scaleX.setInterpolator(new AccelerateInterpolator());
        scaleY.setInterpolator(new AccelerateInterpolator());

        // Запуск анимаций вместе
        scaleX.start();
        scaleY.start();

        // После завершения запускаем анимацию круга
        scaleX.addListener(new android.animation.Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(android.animation.Animator animation) {}

            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                textView.setVisibility(TextView.GONE);                      // Выключаем изображение первой анимамации.
                circleView.setVisibility(circleView.VISIBLE);               //

                BigTextView bigTextView = findViewById(R.id.bigtextView);   // получаем ссылку на view с большими цифрами
                bigTextView.setVisibility(View.VISIBLE);                    // восстанавливаем ее видимость (в описании ее видимость описана как Gone)
                //circleView.setTextColor(Color.WHITE);
                startCircleAnimation();
            }
            @Override
            public void onAnimationCancel(android.animation.Animator animation) {}
            @Override
            public void onAnimationRepeat(android.animation.Animator animation) {}
        });
    }
    private void startCircleAnimation() {
        // Анимация прогресса от 0 до 100% за 1 секунду
        ValueAnimator animator = ValueAnimator.ofFloat(0, GlobalVars.MAX_SPEED);
        animator.setDuration(1000);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float progress = (float) animation.getAnimatedValue();
                circleView.setProgress(progress);
            }
        });
        animator.start();
    }
}



