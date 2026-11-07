package com.dooo.android.utils;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import android.util.Log;

import androidx.collection.ArraySet;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.dooo.android.AppConfig;
import com.dooo.android.Player;
import com.dooo.android.list.WebSeriesList;
import com.dooo.android.list.YTStreamList;
import com.google.android.gms.cast.RemoteMediaPlayer;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.yausername.youtubedl_android.YoutubeDL;
import com.yausername.youtubedl_android.YoutubeDLException;
import com.yausername.youtubedl_android.YoutubeDLRequest;
import com.yausername.youtubedl_android.mapper.VideoInfo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class Yts {

    private static CompositeDisposable compositeDisposable = new CompositeDisposable();

    public interface VolleyCallback {
        void onSuccess(List<YTStreamList> result);
        void onError(VolleyError error);
    }
    public interface VolleyCallback2 {
        void onSuccess(String result);
        void onError(VolleyError error);
    }


    public static void getlinks(Context context, String url, final VolleyCallback callback) {
        List<YTStreamList> qualityItems = new ArrayList<>();



        Disposable disposable = Observable.fromCallable(() -> {
            YoutubeDL.getInstance().init(context);
            return YoutubeDL.getInstance().getInfo(url);
        })
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(streamInfo -> {
                    int listSize =  streamInfo.getFormats().size();
                    for (int i = 0; i<listSize; i++) {
                        if(streamInfo.getFormats().get(i).getFormatId().equals("17")
                                || streamInfo.getFormats().get(i).getFormatId().equals("18")
                                || streamInfo.getFormats().get(i).getFormatId().equals("22")) {
                            qualityItems.add(new YTStreamList(streamInfo.getFormats().get(i).getFormatNote(), streamInfo.getFormats().get(i).getUrl(), streamInfo.getFormats().get(i).getFormat()));

                        }
                    }
                    if(!qualityItems.isEmpty()) {
                        callback.onSuccess(qualityItems);
                    } else {
                        callback.onError(new VolleyError("Something Went Wrong!"));
                    }

                }, e -> {
                    callback.onError((VolleyError) e);
                });
        compositeDisposable.add(disposable);
    }

    public static void getStreamLinks(Context context, String k, String vid, final VolleyCallback2 callback) {
        callback.onSuccess(vid);
    }
}
