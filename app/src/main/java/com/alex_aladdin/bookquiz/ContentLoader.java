package com.alex_aladdin.bookquiz;


import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import android.widget.EditText;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Random;

//Класс, предназначенный для загрузки контента из базы данных и снабжения им остальных классов
class ContentLoader {

    private final Activity fActivity;
    private int mMaxPage; //Число текстов в базе данных
    private int[] mGlobalOrder; //Массив, содержащий порядок показа страниц
    private int mCurrentPage; //Текущий номер в массиве mGlobalOrder

    private String mTitle;
    private String mAuthor;
    private String mCharacter;
    private String mTip;

    ContentLoader(Activity activity) {
        fActivity = activity;
        setOrder();
        nextPage();
    }

    void nextPage() {
        //Инициализируем наш класс-обертку
        DatabaseHelper dbh = new DatabaseHelper(fActivity);
        //База нам нужна для записи и чтения
        SQLiteDatabase sqdb = dbh.getWritableDatabase();

        //Переходим к следующей странице и получаем соответствующий ей id
        mCurrentPage++;
        mCurrentPage = mCurrentPage % mMaxPage;
        int id = mGlobalOrder[mCurrentPage];
        Log.i("ContentLoader", "Загружаем страницу с индексом " + mCurrentPage + ", id = " + id);

        //Читаем из базы все ячейки строки с нужным id
        Cursor cursor = sqdb.query(DatabaseHelper.TABLE_NAME, null, "_id = " + id, null, null, null, null);
        //Переходим к первой записи
        cursor.moveToFirst();
        //Сохраняем данные в соответствующие переменные
        String title = cursor.getString(cursor.getColumnIndex("title"));
        String author = cursor.getString(cursor.getColumnIndex("author"));
        String character = cursor.getString(cursor.getColumnIndex("character"));
        String tip = cursor.getString(cursor.getColumnIndex("tip"));
        String text = cursor.getString(cursor.getColumnIndex("text"));
        int stars = cursor.getInt(cursor.getColumnIndex("stars"));

        TextView textBase = (TextView)this.fActivity.findViewById(R.id.base);
        textBase.setText(text);
        EditText textAnswer = (EditText)this.fActivity.findViewById(R.id.answer);
        textAnswer.setText("");
        mTitle = title;
        mAuthor = author;
        mCharacter = character;
        mTip = tip;

        MainActivity.mAnswerChecker.init(title, stars);

        cursor.close();

        //Закрываем соединения с базой данных
        sqdb.close();
        dbh.close();

        //Отображаем номер страницы на верхней панели
        String page_number = fActivity.getResources().getString(R.string.page);
        page_number += " " + String.valueOf(mCurrentPage + 1) + "/" + String.valueOf(mMaxPage);
        TextView textPage = (TextView)fActivity.findViewById(R.id.page);
        textPage.setText(page_number);
    }

    //Метод, устанавливающий порядок загрузки страниц
    private void setOrder() {
        //Инициализируем наш класс-обертку
        DatabaseHelper dbh = new DatabaseHelper(fActivity);
        //База нам нужна для записи и чтения
        SQLiteDatabase sqdb = dbh.getWritableDatabase();
        //Читаем из базы поля "_id" и "stars" всех строк
        Cursor cursor = sqdb.query(DatabaseHelper.TABLE_NAME, new String[] {"_id", "stars"}, null, null, null, null, null);
        //Узнаем число строк
        mMaxPage = cursor.getCount();
        //Объявляем вспомогательные массивы переменной длины
        ArrayList<Integer> order_complete = new ArrayList<>(); //все id строк, где одна или больше звезд
        ArrayList<Integer> order_incomplete = new ArrayList<>(); //все id строк без звезд (не пройденные тексты)
        //Двигаемся по всем строкам
        while (cursor.moveToNext()) {
            int id = cursor.getInt(cursor.getColumnIndex("_id"));
            int stars = cursor.getInt(cursor.getColumnIndex("stars"));
            //В зависимости от наличия или отсутствия звезд раскидываем в тот или иной массив
            if (stars > 0)
                order_complete.add(id);
            else
                order_incomplete.add(id);
        }
        cursor.close();

        Log.i("ContentLoader", "В базе данных " + mMaxPage + " строк, из них " + order_complete.size() + " со звездами и " +
        order_incomplete.size() + " без звезд");

        //Перемешиваем массивы
        shuffleArray(order_complete);
        shuffleArray(order_incomplete);

        //Собираем глобальный массив из этих двух: сначала сделанные, потом не сделанные
        mGlobalOrder = new int[mMaxPage];
        for (int i = 0; i < order_complete.size(); i++)
            mGlobalOrder[i] = order_complete.get(i);

        for (int i = 0; i < order_incomplete.size(); i++)
            mGlobalOrder[order_complete.size() + i] = order_incomplete.get(i);

        //Текущая страница -- первая не сделанная, либо (если все сделаны) первая в массиве
        //Отнимаем единицу, т.к. nextPage() её прибавит
        mCurrentPage = order_complete.size() % mMaxPage - 1;

        //Отчет в лог
        String report = "Порядок показа страниц: " + mGlobalOrder[0];
        for (int i = 1; i < mMaxPage; i++)
            report += ", " + mGlobalOrder[i];

        Log.i("ContentLoader", report + "; начинаем со страницы №" + (mCurrentPage + 1));

        //Закрываем соединения с базой данных
        sqdb.close();
        dbh.close();

        //Если до сих пор не была сделана ни одна страница (приложение запущено в первый раз), показываем приветствие
        if (order_complete.isEmpty()) showMessageHello();
    }

    //Случайное перемешивание массива
    private void shuffleArray(ArrayList<Integer> array) {
        Random rnd = new Random();
        for (int i = array.size() - 1; i > 0; i--) {
            int index = rnd.nextInt(i + 1);
            //Меняем местами элементы i и index
            int t = array.get(index);
            array.set(index, array.get(i));
            array.set(i, t);
        }
    }

    //Показ приветственного сообщения
    private void showMessageHello() {
        Intent intent = new Intent(fActivity, DialogActivity.class);
        intent.putExtra("title", fActivity.getResources().getString(R.string.dialog_hello_title));
        intent.putExtra("message", fActivity.getResources().getString(R.string.dialog_hello_message));
        fActivity.startActivity(intent);
    }

    //Получить id строки в базе данных, с которой мы в данный момент работаем
    int getCurrentID() {
        return mGlobalOrder[mCurrentPage];
    }

    //Возвращает название книги
    String getCurrentTitle() { return mTitle; }

    //Возвращает автора
    String getCurrentAuthor() { return mAuthor; }

    //Возвращает персонажа
    String getCurrentCharacter() { return mCharacter; }

    //Возвращает подсказку
    String getCurrentTip() { return mTip; }
}