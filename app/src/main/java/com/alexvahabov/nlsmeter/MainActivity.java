package com.alexvahabov.nlsmeter;
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
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import androidx.appcompat.app.AlertDialog;

// Надо повесить на таймер отображение больших цифр
public class MainActivity extends AppCompatActivity {
    private TextView textView;
    private CircleView circleView;
    private GpsTracker gpsTracker;
    private BigTextView bigtextView;

    private void showMaxSpeedDialog() {
        final int steps = (GlobalVars.MAX_MAX_SPEED - GlobalVars.MIN_MAX_SPEED)
                / GlobalVars.STEP_MAX_SPEED;

        float density = getResources().getDisplayMetrics().density;
        int pad = (int) (16 * density);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(pad, pad, pad, pad);
        ScrollView scrollView = new ScrollView(this);
        scrollView.addView(layout);

        // --- Максимум шкалы ---
        final TextView label = new TextView(this);
        label.setTextSize(22f);
        label.setPadding(0, 0, 0, pad / 2);
        layout.addView(label);

        SeekBar sb = new SeekBar(this);
        sb.setMax(steps);
        sb.setProgress((GlobalVars.MAX_SPEED - GlobalVars.MIN_MAX_SPEED)
                / GlobalVars.STEP_MAX_SPEED);
        layout.addView(sb);

        final TextView yellowLabel = new TextView(this);
        yellowLabel.setTextSize(18f);
        yellowLabel.setPadding(0, pad, 0, pad / 2);
        layout.addView(yellowLabel);
        SeekBar yellowBar = new SeekBar(this);
        yellowBar.setMax(SpeedThresholds.step(GlobalVars.MAX_SPEED));
        yellowBar.setProgress(SpeedThresholds.yellowStep(GlobalVars.SWarn, yellowBar.getMax()));
        layout.addView(yellowBar);

        final TextView redLabel = new TextView(this);
        redLabel.setTextSize(18f);
        redLabel.setPadding(0, pad, 0, pad / 2);
        layout.addView(redLabel);
        SeekBar redBar = new SeekBar(this);
        redBar.setMax(SpeedThresholds.step(GlobalVars.MAX_SPEED));
        redBar.setProgress(SpeedThresholds.redStep(GlobalVars.SMax,
                yellowBar.getProgress(), redBar.getMax()));
        layout.addView(redBar);

        Runnable updateLabels = () -> {
            label.setText("Максимум шкалы: " + (GlobalVars.MIN_MAX_SPEED
                    + sb.getProgress() * GlobalVars.STEP_MAX_SPEED) + " км/ч");
            yellowLabel.setText("Жёлтый цвет с "
                    + SpeedThresholds.speed(yellowBar.getProgress()) + " км/ч");
            redLabel.setText("Красный цвет с "
                    + SpeedThresholds.speed(redBar.getProgress()) + " км/ч");
        };
        sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean fromUser) {
                if (!fromUser) return;
                int max = GlobalVars.MIN_MAX_SPEED + p * GlobalVars.STEP_MAX_SPEED;
                int maxStep = SpeedThresholds.step(max);
                yellowBar.setMax(maxStep);
                redBar.setMax(maxStep);
                yellowBar.setProgress(SpeedThresholds.yellowStep(
                        SpeedThresholds.speed(yellowBar.getProgress()), maxStep));
                redBar.setProgress(SpeedThresholds.redStep(
                        SpeedThresholds.speed(redBar.getProgress()),
                        yellowBar.getProgress(), maxStep));
                updateLabels.run();
            }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) {}
        });

        yellowBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean fromUser) {
                if (!fromUser) return;
                int yellow = SpeedThresholds.clamp(p, 1, yellowBar.getMax() - 1);
                yellowBar.setProgress(yellow);
                if (redBar.getProgress() <= yellow) redBar.setProgress(yellow + 1);
                updateLabels.run();
            }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) {}
        });

        redBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean fromUser) {
                if (!fromUser) return;
                int red = SpeedThresholds.clamp(p, 2, redBar.getMax());
                redBar.setProgress(red);
                if (yellowBar.getProgress() >= red) yellowBar.setProgress(red - 1);
                updateLabels.run();
            }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) {}
        });
        updateLabels.run();

        // --- Коэффициент растяжения центральной зоны ---
        TextView factorLabel = new TextView(this);
        factorLabel.setText("Коэффициент шкалы в центре (1 — линейная)");
        factorLabel.setTextSize(18f);
        factorLabel.setPadding(0, pad, 0, pad / 2);
        layout.addView(factorLabel);
        Spinner factorSpinner = new Spinner(this);
        String[] factorOptions = {"1", "2", "4", "8", "16"};
        ArrayAdapter<String> factorAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, factorOptions);
        factorAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        factorSpinner.setAdapter(factorAdapter);
        for (int i = 0; i < GlobalVars.SCALE_FACTORS.length; i++) {
            if (GlobalVars.SCALE_FACTORS[i] == GlobalVars.SCALE_FACTOR) {
                factorSpinner.setSelection(i);
                break;
            }
        }
        layout.addView(factorSpinner);

        // --- Диалог ---
        new AlertDialog.Builder(this)
                .setTitle("Настройки шкалы")
                .setView(scrollView)
                .setPositiveButton("ОК", (d, w) -> {
                    int val = GlobalVars.MIN_MAX_SPEED
                            + sb.getProgress() * GlobalVars.STEP_MAX_SPEED;
                    applyScaleSettings(val,
                            SpeedThresholds.speed(yellowBar.getProgress()),
                            SpeedThresholds.speed(redBar.getProgress()),
                            GlobalVars.SCALE_FACTORS[factorSpinner.getSelectedItemPosition()]);
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    /** Применить максимум шкалы, цветовые пороги и коэффициент центральной зоны. */
    private void applyScaleSettings(int newMax, int yellowSpeed, int redSpeed, int factor) {
        GlobalVars.MAX_SPEED = SpeedThresholds.maxSpeed(newMax);
        GlobalVars.SCALE_FACTOR = SpeedScale.validFactor(factor);
        int maxStep = SpeedThresholds.step(GlobalVars.MAX_SPEED);
        int yellowStep = SpeedThresholds.yellowStep(yellowSpeed, maxStep);
        GlobalVars.SWarn = SpeedThresholds.speed(yellowStep);
        GlobalVars.SMax = SpeedThresholds.speed(
                SpeedThresholds.redStep(redSpeed, yellowStep, maxStep));

        getSharedPreferences(GlobalVars.PREFS_NAME, MODE_PRIVATE)
                .edit()
                .putInt(GlobalVars.KEY_MAX_SPEED, GlobalVars.MAX_SPEED)
                .putInt(GlobalVars.KEY_SWARN,     GlobalVars.SWarn)
                .putInt(GlobalVars.KEY_SMAX,      GlobalVars.SMax)
                .putInt(GlobalVars.KEY_SCALE_FACTOR, GlobalVars.SCALE_FACTOR)
                .apply();

        circleView.setMaxSpeed(GlobalVars.MAX_SPEED);
        circleView.setScaleFactor(GlobalVars.SCALE_FACTOR);
        circleView.setProgress(GlobalVars.MAX_SPEED);
        circleView.setProgressPointPosition(GlobalVars.MyTimer);
        bigtextView.invalidate();
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textView = findViewById(R.id.textView);
        circleView = findViewById(R.id.circleView);
        // --- Восстанавливаем настройки из SharedPreferences ---
        SharedPreferences prefs = getSharedPreferences(GlobalVars.PREFS_NAME, MODE_PRIVATE);
        GlobalVars.MAX_SPEED = SpeedThresholds.maxSpeed(
                prefs.getInt(GlobalVars.KEY_MAX_SPEED, GlobalVars.DEFAULT_MAX_SPEED));
        int maxStep = SpeedThresholds.step(GlobalVars.MAX_SPEED);
        int yellowStep = SpeedThresholds.yellowStep(
                prefs.getInt(GlobalVars.KEY_SWARN, GlobalVars.SWarn), maxStep);
        GlobalVars.SWarn = SpeedThresholds.speed(yellowStep);
        GlobalVars.SMax = SpeedThresholds.speed(SpeedThresholds.redStep(
                prefs.getInt(GlobalVars.KEY_SMAX, GlobalVars.SMax), yellowStep, maxStep));
        int oldFactor = prefs.contains(GlobalVars.KEY_NONLINEAR)
                ? (prefs.getBoolean(GlobalVars.KEY_NONLINEAR, true) ? 2 : 1)
                : GlobalVars.DEFAULT_SCALE_FACTOR;
        GlobalVars.SCALE_FACTOR = SpeedScale.validFactor(
                prefs.getInt(GlobalVars.KEY_SCALE_FACTOR, oldFactor));

        circleView.setMaxSpeed(GlobalVars.MAX_SPEED);
        circleView.setScaleFactor(GlobalVars.SCALE_FACTOR);

        findViewById(R.id.settingsButton).setOnClickListener(v -> showMaxSpeedDialog());
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
