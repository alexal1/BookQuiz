package com.alex_aladdin.bookquiz;

import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.media.MediaPlayer;
import android.support.v4.content.ContextCompat;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.Arrays;

//Класс, обрабатывающий ответы и управляющий тем, что видит пользователь в текстовом поле для ответа
class AnswerChecker {

    private final Activity fActivity;
    private String mTitle; //Правильный ответ
    private String mMask; //Текущая маска, видимая пользователю
    private int mStars; //Количество заработанных звезд
    private int mPrevStars; //Количество звезд, которые уже были заработаны до этого

    AnswerChecker(Activity activity) {
        fActivity = activity;
    }

    void init(String title, int stars) {
        //Договоримся использовать верхний регистр
        mTitle = title.toUpperCase();
        //Переводим в массив символов
        char[] title_array = mTitle.toCharArray();
        //Создаем маску, содержащую знаки вопроса вместо букв
        char[] mask_array = new char[title.length()];
        for (int i = 0; i < title.length(); i++) {
            if (title_array[i] == ' ')
                mask_array[i] = ' ';
            else
                mask_array[i] = '?';
        }
        mMask = new String(mask_array);
        //Передаем полученную маску текстовому полю
        TextView textMask = (TextView)this.fActivity.findViewById(R.id.mask);
        textMask.setText(mMask);
        //Изначально у нас три звезды, с каждой ошибкой их будет меньше
        mStars = 3;
        //При этом отображается на экране столько звезд, сколько записано в базе данных
        mPrevStars = stars;
        showStars(mPrevStars);
    }

    void check(char letter) {
        //Используем верхний регистр
        letter = Character.toUpperCase(letter);
        //Если эта буква уже есть, игнорируем
        if (mMask.contains(String.valueOf(letter))) return;
        //Переводим название и маску в массивы символов
        char[] title_array = mTitle.toCharArray();
        char[] mask_array = mMask.toCharArray();
        //Если в названии встречается соответствующая буква, "открываем" на этой же позиции букву в маске
        for (int i = 0; i < mTitle.length(); i++)
            if (title_array[i] == letter)
                mask_array[i] = letter;

        //Если после перебора всех букв ни одна новая не открылась, то есть новая маска совпадает со старой, результат отрицательный
        if (Arrays.equals(mMask.toCharArray(), mask_array)) {
            //Звезд меньше на одну
            if (mStars == 3) {
                mStars = 2;
                showMessageTip();
            }
            else
                mStars = 1;

            indicateResult(false);
        }
        //Иначе -- положительный
        else
            indicateResult(true);

        //Сохраняем новую маску
        mMask = new String(mask_array);

        TextView textMask = (TextView)this.fActivity.findViewById(R.id.mask);
        textMask.setText(mMask);

        //Если оказались открыты все буквы
        if (Arrays.equals(mTitle.toCharArray(), mask_array)) {
            //Заносим достижение в базу данных, если оно больше предыдущего
            writeStarsNumber(Math.max(mStars, mPrevStars));
            //Отображаем столько звезд, сколько максимально было когда-либо набрано на этой странице
            showStars(Math.max(mStars, mPrevStars));
            //Выводим сообщение
            showMessageCongrats();
        }
    }

    //Метод, записывающий в базу данных число заработанных звезд
    private void writeStarsNumber(int stars) {
        //Инициализируем наш класс-обертку
        DatabaseHelper dbh = new DatabaseHelper(fActivity);
        //База нам нужна для записи и чтения
        SQLiteDatabase sqdb = dbh.getWritableDatabase();

        //Получаем ID
        int id = MainActivity.mContentLoader.getCurrentID();
        //Обновляем строку в базе
        ContentValues values = new ContentValues();
        values.put("stars", stars);
        sqdb.update(DatabaseHelper.TABLE_NAME, values, "_id = ?", new String[] {Integer.toString(id)});

        //Закрываем соединения с базой данных
        sqdb.close();
        dbh.close();
    }

    //Метод, отображающий визуально и звуком правильность/неправильность ответа
    private void indicateResult(Boolean answer) {
        //Правильный ответ
        if (answer) {
            //Звук
            MediaPlayer mp = MediaPlayer.create(fActivity, R.raw.correct);
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
            //Анимация
            TextView textMask = (TextView)this.fActivity.findViewById(R.id.mask);
            int colorStart = ContextCompat.getColor(fActivity, R.color.colorTrue);
            int colorEnd = ContextCompat.getColor(fActivity, R.color.colorTransparentTrue);
            ValueAnimator animator = ObjectAnimator.ofInt(textMask, "backgroundColor", colorStart, colorEnd);
            animator.setDuration(1000);
            animator.setEvaluator(new ArgbEvaluator());
            animator.start();
        }
        //Неправильный ответ
        else {
            //Звук
            MediaPlayer mp = MediaPlayer.create(fActivity, R.raw.incorrect);
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
            //Анимация
            TextView textMask = (TextView)this.fActivity.findViewById(R.id.mask);
            int colorStart = ContextCompat.getColor(fActivity, R.color.colorDecorative);
            int colorEnd = ContextCompat.getColor(fActivity, R.color.colorTransparentDecorative);
            ValueAnimator animator = ObjectAnimator.ofInt(textMask, "backgroundColor", colorStart, colorEnd);
            animator.setDuration(1000);
            animator.setEvaluator(new ArgbEvaluator());
            animator.start();
        }
    }

    //Метод, отображающий картинки звезд на экране в соответствии с переменной mStars
    private void showStars(int stars) {
        ImageView imageStar1 = (ImageView)this.fActivity.findViewById(R.id.star1);
        ImageView imageStar2 = (ImageView)this.fActivity.findViewById(R.id.star2);
        ImageView imageStar3 = (ImageView)this.fActivity.findViewById(R.id.star3);

        switch (stars) {
            case 0:
                imageStar1.setAlpha(0.2f);
                imageStar2.setAlpha(0.2f);
                imageStar3.setAlpha(0.2f);
                break;
            case 1:
                imageStar1.setAlpha(0.8f);
                imageStar2.setAlpha(0.2f);
                imageStar3.setAlpha(0.2f);
                break;
            case 2:
                imageStar1.setAlpha(0.8f);
                imageStar2.setAlpha(0.8f);
                imageStar3.setAlpha(0.2f);
                break;
            case 3:
                imageStar1.setAlpha(0.8f);
                imageStar2.setAlpha(0.8f);
                imageStar3.setAlpha(0.8f);
                break;
        }
    }

    //Показ поздравительного сообщения
    private void showMessageCongrats() {
        //Формируем текст сообщения
        String message;
        switch (mStars) {
            case 1:
                message = fActivity.getResources().getString(R.string.dialog_congrats_message_part1_star1);
                break;
            case 2:
                message = fActivity.getResources().getString(R.string.dialog_congrats_message_part1_star2);
                break;
            case 3:
                message = fActivity.getResources().getString(R.string.dialog_congrats_message_part1_star3);
                break;
            default:
                message = "";
                break;
        }
        message += " " + MainActivity.mContentLoader.getCurrentCharacter();
        message += " " + fActivity.getResources().getString(R.string.dialog_congrats_message_part2);
        message += " \u00AB" + MainActivity.mContentLoader.getCurrentTitle() + "\u00BB ";
        message += MainActivity.mContentLoader.getCurrentAuthor() + ".";

        Intent intent = new Intent(fActivity, DialogActivity.class);
        intent.putExtra("title", fActivity.getResources().getString(R.string.dialog_congrats_title));
        intent.putExtra("message", message);
        intent.putExtra("nextPage", true);
        intent.putExtra("applause", true);
        intent.putExtra("share", true);
        fActivity.startActivity(intent);
    }

    //Показ сообщения с подсказкой
    private void showMessageTip() {
        Intent intent = new Intent(fActivity, DialogActivity.class);
        intent.putExtra("title", fActivity.getResources().getString(R.string.dialog_tip_title));
        intent.putExtra("message", MainActivity.mContentLoader.getCurrentTip());
        fActivity.startActivity(intent);
    }
}