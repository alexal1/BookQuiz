package com.alex_aladdin.bookquiz;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

@SuppressLint("StaticFieldLeak")
public class MainActivity extends AppCompatActivity {

    public static AnswerChecker mAnswerChecker;
    public static ContentLoader mContentLoader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        //Создаем объекты классов
        mAnswerChecker = new AnswerChecker(this);
        mContentLoader = new ContentLoader(this);

        //Подключаем шрифты
        final TextView textBase = (TextView)findViewById(R.id.base);
        textBase.setTypeface(Typeface.createFromAsset(getAssets(), "fonts/DearType - Lifehack Italic.otf"));
        final TextView textPage = (TextView)findViewById(R.id.page);
        textPage.setTypeface(Typeface.createFromAsset(getAssets(), "fonts/DearType - Lifehack Sans.otf"));
        final TextView textMask = (TextView)findViewById(R.id.mask);
        textMask.setTypeface(Typeface.createFromAsset(getAssets(), "fonts/DearType - Lifehack Sans.otf"));
        final Button buttonRate = (Button)findViewById(R.id.rate);
        buttonRate.setTypeface(Typeface.createFromAsset(getAssets(), "fonts/DearType - Lifehack Sans.otf"));

        //Вешаем на textAnswer обработчик изменения текста
        final EditText textAnswer = (EditText)findViewById(R.id.answer);
        textAnswer.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                //Проверяем что поле не пусто (оно обнуляется в методе nextPage ContentLoader'а)
                if (s.length() == 0) return;
                //Берем последнюю вбитую в EditText букву и отправляем её на проверку в AnswerChecker
                char letter = s.charAt(s.length() - 1);
                mAnswerChecker.check(letter);
            }
        });
    }

    //Показываем/убираем клавиатуру по щелчку на экране
    public void onScreenClick(View view) {
        InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
    }

    //Открываем Google Play по щелчку на кнопке rate
    public void onButtonRateClick(View view) {
        final String appPackageName = getPackageName();
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName)));
        } catch (android.content.ActivityNotFoundException exception) {
            //На случай если не установлен Play Store
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + appPackageName)));
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        //Блокируем EditText, чтобы убрать клавиатуру
        EditText textAnswer = (EditText)findViewById(R.id.answer);
        textAnswer.setEnabled(false);
    }

    @Override
    public void onResume() {
        super.onResume();

        //Разблокируем EditText
        EditText textAnswer = (EditText)findViewById(R.id.answer);
        textAnswer.setEnabled(true);

        //Показываем клавиатуру
        InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
    }
}