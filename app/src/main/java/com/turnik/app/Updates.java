package com.turnik.app;

import android.app.*;
import android.content.*;
import android.net.Uri;
import android.os.*;
import android.widget.Toast;
import androidx.core.content.FileProvider;
import org.json.*;
import java.io.*;
import java.net.*;
import java.util.*;

public final class Updates {
    private static final String API="https://api.github.com/repos/vladislavliub-stack/Turnik-Android/releases/latest";
    private final Activity activity;
    Updates(Activity a){activity=a;}
    void check(boolean manual){
        new Thread(()->{
            try{
                HttpURLConnection c=(HttpURLConnection)new URL(API).openConnection();
                c.setConnectTimeout(10000);c.setReadTimeout(10000);
                c.setRequestProperty("Accept","application/vnd.github+json");
                c.setRequestProperty("User-Agent","Turnik-Android");
                int code=c.getResponseCode();
                if(code==404){message(manual?"Обновлений пока нет":null);return;}
                if(code!=200)throw new IOException("HTTP "+code);
                StringBuilder sb=new StringBuilder();
                try(BufferedReader br=new BufferedReader(new InputStreamReader(c.getInputStream()))){String s;while((s=br.readLine())!=null)sb.append(s);}
                JSONObject release=new JSONObject(sb.toString());
                String tag=release.optString("tag_name");
                int newest=version(tag);
                int current=activity.getPackageManager().getPackageInfo(activity.getPackageName(),0).versionCode;
                if(newest<=current){message(manual?"Установлена последняя версия":null);return;}
                JSONArray assets=release.getJSONArray("assets");String url=null;
                for(int i=0;i<assets.length();i++){JSONObject a=assets.getJSONObject(i);if(a.optString("name").endsWith(".apk")){url=a.getString("browser_download_url");break;}}
                if(url==null)throw new IOException("APK не найден в релизе");
                final String apkUrl=url;
                activity.runOnUiThread(()->new AlertDialog.Builder(activity)
                    .setTitle("Обновление «Турника»")
                    .setMessage("Доступна версия "+tag+". Скачать и установить?")
                    .setNegativeButton("Позже",null)
                    .setPositiveButton("Скачать",(d,w)->download(apkUrl)).show());
            }catch(Exception e){message(manual?"Не удалось проверить обновления: "+e.getMessage():null);}
        }).start();
    }
    private int version(String tag){try{String[] p=tag.replaceFirst("^[vV]","").split("\\.");return Integer.parseInt(p[0])*10000+(p.length>1?Integer.parseInt(p[1])*100:0)+(p.length>2?Integer.parseInt(p[2]):0);}catch(Exception e){return 0;}}
    private void message(String s){if(s!=null)activity.runOnUiThread(()->Toast.makeText(activity,s,Toast.LENGTH_LONG).show());}
    private void download(String url){
        try{
            DownloadManager dm=(DownloadManager)activity.getSystemService(Context.DOWNLOAD_SERVICE);
            DownloadManager.Request req=new DownloadManager.Request(Uri.parse(url));
            req.setTitle("Турник: обновление");
            req.setDescription("Загрузка новой версии");
            req.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            req.setDestinationInExternalFilesDir(activity,Environment.DIRECTORY_DOWNLOADS,"turnik-update.apk");
            long id=dm.enqueue(req);
            message("Скачиваем обновление");
            new Thread(()->{
                for(int i=0;i<240;i++){
                    try{Thread.sleep(1000);DownloadManager.Query q=new DownloadManager.Query().setFilterById(id);
                        try(android.database.Cursor cursor=dm.query(q)){
                            if(cursor!=null&&cursor.moveToFirst()){
                                int status=cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS));
                                if(status==DownloadManager.STATUS_SUCCESSFUL){activity.runOnUiThread(this::install);return;}
                                if(status==DownloadManager.STATUS_FAILED){message("Ошибка скачивания APK");return;}
                            }
                        }
                    }catch(Exception e){message("Ошибка обновления: "+e.getMessage());return;}
                }
                message("Загрузка продолжается. Проверь уведомления.");
            }).start();
        }catch(Exception e){message("Не удалось начать загрузку: "+e.getMessage());}
    }
    private void install(){
        try{
            File f=new File(activity.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),"turnik-update.apk");
            Uri uri=FileProvider.getUriForFile(activity,activity.getPackageName()+".provider",f);
            Intent i=new Intent(Intent.ACTION_VIEW);
            i.setDataAndType(uri,"application/vnd.android.package-archive");
            i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_ACTIVITY_NEW_TASK);
            activity.startActivity(i);
        }catch(Exception e){message("Не удалось открыть установщик: "+e.getMessage());}
    }
}
