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


// Надо повесить на таймер отображение больших цифр
public class MainActivity extends AppCompatActivity {
    private TextView textView;
    private CircleView circleView;
    //private NumberView numberView;
    private GpsTracker gpsTracker;
    private BigTextView bigtextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textView = findViewById(R.id.textView);
        circleView = findViewById(R.id.circleView);
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
        // Анимация прогресса от 0 до 100% за 3 секунды
        ValueAnimator animator = ValueAnimator.ofFloat(0, 160);
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



