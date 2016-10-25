package com.alex_aladdin.bookquiz;

import android.content.ContentValues;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import java.io.IOException;

//Вспомогательный класс для открытия/создания базы данных
//Наследуемся от класса SQLiteOpenHelper, с помощью которого можно создавать, открывать и обновлять базы данных
class DatabaseHelper extends SQLiteOpenHelper {

    private final Context fContext; //Переменная final может быть установлена только однажды
    private static final String DATABASE_NAME = "bookquiz_database.db"; //К переменной static можно обращаться через название класса
    private static final int DATABASE_VERSION = 1;
    static final String TABLE_NAME = "content";

    DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        fContext = context;
    }

    //Если при создании экземпляра класса не существует БД с именем DATABASE_NAME, то вызывается метод onCreate
    @Override
    @SuppressWarnings("TryFinallyCanBeTryWithResources")
    public void onCreate(SQLiteDatabase db) {
        //Передаем запрос для создания таблицы
        db.execSQL("CREATE TABLE " + TABLE_NAME + " (_id INTEGER PRIMARY KEY, title TEXT, author TEXT, character TEXT, " +
                "tip TEXT, text TEXT, stars INT DEFAULT 0);");
        //ContentValues это некая обертка над данными, которые будут записаны в БД
        ContentValues values = new ContentValues();
        //Получаем xml-файл из ресурсов
        Resources res = fContext.getResources();
        XmlResourceParser _xml = res.getXml(R.xml.books_records);

        try {
            //Ищем конец документа
            int eventType = _xml.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                //Ищем теги record
                if ((eventType == XmlPullParser.START_TAG) && (_xml.getName().equals("record"))) {
                    //Тег record найден, теперь получим его атрибуты и вставим в таблицу
                    String title = _xml.getAttributeValue(0);
                    String author = _xml.getAttributeValue(1);
                    String character = _xml.getAttributeValue(2);
                    String tip = _xml.getAttributeValue(3);
                    String text = _xml.getAttributeValue(4);

                    values.put("title", title);
                    values.put("author", author);
                    values.put("character", character);
                    values.put("tip", tip);
                    values.put("text", text);

                    //Вставляем новую запись в БД
                    db.insert(TABLE_NAME, null, values);
                }
                eventType = _xml.next();
            }
        }
        //Отлавливаем ошибки
        catch (XmlPullParserException | IOException e) {
            Log.e("SQLite", e.getMessage(), e);
        }
        finally {
            //Закрываем xml файл
            _xml.close();
        }
    }

    //Метод вызывается при несовпадении версий
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        //Просто удаляем существующую таблицу и заменяем её на новую
        Log.w("SQLite", "Обновляемся с версии " + oldVersion + " на версию " + newVersion);
        db.execSQL("DROP TABLE IF IT EXISTS " + TABLE_NAME);
        onCreate(db);
    }
}