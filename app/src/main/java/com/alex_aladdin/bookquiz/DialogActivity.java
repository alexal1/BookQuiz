package com.alex_aladdin.bookquiz;

import android.content.Intent;
import android.graphics.Typeface;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.vk.sdk.VKAccessToken;
import com.vk.sdk.VKCallback;
import com.vk.sdk.VKSdk;
import com.vk.sdk.api.VKError;

public class DialogActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dialog);

        //Подключаем шрифты
        final TextView textTitle = (TextView)findViewById(R.id.dialog_title);
        textTitle.setTypeface(Typeface.createFromAsset(getAssets(), "fonts/DearType - Lifehack Sans.otf"));
        final TextView textMessage = (TextView)findViewById(R.id.dialog_message);
        textMessage.setTypeface(Typeface.createFromAsset(getAssets(), "fonts/DearType - Lifehack Sans.otf"));
        final Button buttonNext = (Button) findViewById(R.id.dialog_next);
        buttonNext.setTypeface(Typeface.createFromAsset(getAssets(), "fonts/DearType - Lifehack Sans.otf"));
        final Button buttonShare = (Button) findViewById(R.id.dialog_share);
        buttonShare.setTypeface(Typeface.createFromAsset(getAssets(), "fonts/DearType - Lifehack Sans.otf"));

        //Получаем строковые значения от вызывающего эту активность объекта класса
        String title = getIntent().getStringExtra("title");
        String message = getIntent().getStringExtra("message");
        //Вставляем полученный текст в текстовые поля
        textTitle.setText(title);
        textMessage.setText(message);

        //Если стоит соответствующий флаг, показываем кнопку "Поделиться"
        if (getIntent().getBooleanExtra("share", false))
            buttonShare.setVisibility(View.VISIBLE);

        //Если стоит соответствующий флаг, включаем апплодисменты
        if (getIntent().getBooleanExtra("applause", false)) {
            MediaPlayer mp = MediaPlayer.create(this, R.raw.applause);
            //Вешаем слушатель окончания звука, который выгружает файл из памяти
            mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mediaPlayer) {
                    //Выгружаем файл из памяти
                    mediaPlayer.release();
                    //Убираем слушатель
                    mediaPlayer.setOnCompletionListener(null);
                }
            });
            mp.start();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        //Если стоит соответствующий флаг, загружаем следующую страницу
        if (getIntent().getBooleanExtra("nextPage", false))
            MainActivity.mContentLoader.nextPage();
    }

    public void onButtonNextClick(View view) {
        finish();
    }

    public void onButtonShareClick(View view) {
        //Авторизация
        VKSdk.login(this, "wall");
    }

    //Метод, вызываемый после авторизации
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (!VKSdk.onActivityResult(requestCode, resultCode, data, new VKCallback<VKAccessToken>() {
            @Override
            public void onResult(VKAccessToken res) {
                //Пользователь успешно авторизовался
                Log.i("VK_SDK", "Auth OK");

                VKApplication.getInstance().share(res);
            }

            @Override
            public void onError(VKError error) {
                //Ошибка авторизации
                Log.i("VK_SDK", String.valueOf(error));

                Toast toast = Toast.makeText(getApplicationContext(), getString(R.string.share_error_auth), Toast.LENGTH_SHORT);
                toast.show();
            }
        })) {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }
}