package com.alex_aladdin.bookquiz;

import android.app.Application;
import android.util.Log;
import android.widget.Toast;

import com.vk.sdk.VKAccessToken;
import com.vk.sdk.VKSdk;
import com.vk.sdk.api.VKApi;
import com.vk.sdk.api.VKApiConst;
import com.vk.sdk.api.VKError;
import com.vk.sdk.api.VKParameters;
import com.vk.sdk.api.VKRequest;
import com.vk.sdk.api.VKResponse;
import com.vk.sdk.api.model.VKApiLink;
import com.vk.sdk.api.model.VKApiPhoto;
import com.vk.sdk.api.model.VKAttachments;
import com.vk.sdk.api.model.VKWallPostResult;

import org.json.JSONArray;

//Класс-singleton, то есть доступный из любого участка кода, в котором собраны все методы для работы с VK SDK
public class VKApplication extends Application {
    private static VKApplication singleton;
    private int mID, mSex; //id и пол пользователя

    //Возвращает экземпляр данного класса
    public static VKApplication getInstance() {
        return singleton;
    }

    @Override
    public final void onCreate() {
        super.onCreate();
        singleton = this;

        VKSdk.initialize(this);
    }

    //Метод, который запускается кнопкой из DialogActivity
    //Возвращает, удалось ли запостить пост
    public void share(VKAccessToken vkAccessToken) {
        //Определяем id по токену
        mID = Integer.parseInt(vkAccessToken.userId);
        Log.i("VK_SDK", "id = " + mID);
        //Определяем пол
        getMySex();
    }

    //Определение пола
    private void getMySex() {
        VKParameters parameters = new VKParameters();
        parameters.put(VKApiConst.OWNER_ID, String.valueOf(mID));
        parameters.put(VKApiConst.FIELDS, VKApiConst.SEX);
        VKRequest request = VKApi.users().get(parameters);
        request.executeWithListener(new VKRequest.VKRequestListener() {
            @Override
            public void onComplete(VKResponse response) {
                super.onComplete(response);

                //В ответ мы получили данные в формате JSON, вынимаем из них информацию про пол
                try {
                    JSONArray array = response.json.getJSONArray("response");
                    mSex = array.getJSONObject(0).getInt("sex");
                    Log.i("VK_SDK", "sex = " + mSex);

                    //Делаем пост
                    makePost();

                } catch (org.json.JSONException e) {
                    Log.i("VK_SDK", String.valueOf(e));
                    showResult(false);
                }
            }

            @Override
            public void onError(VKError error) {
                super.onError(error);

                Log.i("VK_SDK", String.valueOf(error));
                showResult(false);
            }
        });
    }

    //Создание поста
    private void makePost() {
        final String appPackageName = getPackageName();

        VKAttachments attachments = new VKAttachments();
        attachments.add(new VKApiPhoto(getString(R.string.photo_id)));
        attachments.add(new VKApiLink("https://play.google.com/store/apps/details?id=" + appPackageName));

        VKParameters parameters = new VKParameters();
        parameters.put(VKApiConst.OWNER_ID, String.valueOf(mID));
        parameters.put(VKApiConst.MESSAGE, prepareMessage());
        parameters.put(VKApiConst.ATTACHMENTS, attachments);
        VKRequest post = VKApi.wall().post(parameters);
        post.setModelClass(VKWallPostResult.class);
        post.executeWithListener(new VKRequest.VKRequestListener() {
            @Override
            public void onComplete(VKResponse response) {
                super.onComplete(response);

                Log.i("VK_SDK", "Post was added");
                showResult(true);
            }

            @Override
            public void onError(VKError error) {
                super.onError(error);

                Log.i("VK_SDK", String.valueOf(error));
                showResult(false);
            }
        });
    }

    //Подготовка текста сообщения
    private String prepareMessage() {
        String message;

        //1. Угадал/угадала
        if (mSex == 1) {
            message = getString(R.string.share_female_guessed);
        } else {
            message = getString(R.string.share_male_guessed);
        }
        //2. Книга
        message += " \u00AB" + MainActivity.mContentLoader.getCurrentTitle() + "\u00BB ";
        //3. По рассказу
        message += getString(R.string.share_by_character) + " ";
        //4. Персонаж
        message += MainActivity.mContentLoader.getCurrentCharacter() + ".";
        //5. Теги
        message += "\n" + getString(R.string.share_tags);

        return message;
    }

    private void showResult(Boolean success) {
        if (success) {
            //Берем строку из prepareMessage, за исключением тегов
            String message = getString(R.string.share_success) + " \"" +
                    prepareMessage().substring(0, prepareMessage().lastIndexOf('\n')) + "\"";
            Toast toast = Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG);
            toast.show();
        } else {
            Toast toast = Toast.makeText(getApplicationContext(), getString(R.string.share_error_other), Toast.LENGTH_SHORT);
            toast.show();
        }
    }
}