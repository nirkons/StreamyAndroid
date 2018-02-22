package d.streamy.streamyapp;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.EditText;
import android.widget.Toast;

import com.android.volley.Cache;
import com.android.volley.Network;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.BasicNetwork;
import com.android.volley.toolbox.DiskBasedCache;
import com.android.volley.toolbox.HurlStack;
import com.android.volley.toolbox.StringRequest;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.net.URL;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.TimeUnit;

import jcifs.smb.SmbFile;
import okhttp3.Credentials;
//import okhttp3.Request;
//import okhttp3.Response;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;


public class BasicService extends Service {

    private NotificationManager notificationManager;
    private ThreadGroup basicServiceThreads = new ThreadGroup("BasicServiceGroup");
    private boolean isShowingForegroundNotification;
    private Thread thread;
    public static final String PREFS_NAME = "MyPrefsFile";
    private ServerSocket serverSocket;
    Thread Thread1 = null;
    private EditText EDITTEXT;
    public static final int SERVERPORT = 13000;

    public BasicService() {
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
    }

    @Override
    public IBinder onBind(Intent intent) {
        Toast.makeText(this, "onBind Called", Toast.LENGTH_SHORT).show();
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Toast.makeText(this, "onCreate Called", Toast.LENGTH_SHORT).show();
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        Toast.makeText(this, "onStartCommand Called", Toast.LENGTH_SHORT).show();
        if (intent.hasExtra("foreground")) {
            //Foreground Service Demo
            if (isShowingForegroundNotification) {
                stopImportantJob();
                stopSelf();//you have to stop it still, it is not enough with stopforeground
            } else
                doImportatJob();
        }
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Toast.makeText(this, "onDestroy Called", Toast.LENGTH_SHORT).show();
        notificationManager.cancelAll();
        if (Thread1 != null)
        {
            try {
                this.serverSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            Thread1.interrupt();
        }
        if (thread != null)
            thread.interrupt();
        if (isShowingForegroundNotification)
            stopImportantJob();

    }

    @Override
    public boolean onUnbind(Intent intent) {
        Toast.makeText(this, "onUnbind Called", Toast.LENGTH_SHORT).show();
        return super.onUnbind(intent);
    }

    @Override
    public void onRebind(Intent intent) {
        Toast.makeText(this, "onRebind Called", Toast.LENGTH_SHORT).show();
        super.onRebind(intent);
    }

    private void displayNotificationMessage(String message) {

        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, ActivityBasicService.class), 0);

        Notification notification = new NotificationCompat.Builder(this)
                .setContentTitle(message)
                .setContentText("Touch to turn off service")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setTicker("Starting up!!!")
                .setContentIntent(contentIntent)
                .setOngoing(false) //by default false
                .build();

        notificationManager.notify(0, notification);
    }


    void doImportatJob() {
        //...  perform important job
        //make this service a foreground service, so it will be as important as the Activity

        this.Thread1 = new Thread(new Thread1());
        this.Thread1.start();

        PendingIntent contentIntent =
                PendingIntent.getActivity(this, 0,
                        new Intent(this, ActivityBasicService.class), 0);
        Notification notification = new NotificationCompat.Builder(this)
                .setContentTitle("Streamy service started, listening for commands")
                .setContentText("Touch to open Streamy")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setTicker("Starting up!!!")
                .setContentIntent(contentIntent)
                .setOngoing(false) //Always true in startForeground
                .build();
        startForeground(1992, notification); //notification can not be dismissed until detached,// or stopped service or stopForeground()
        isShowingForegroundNotification = true;
    }

    private void stopImportantJob() {
        //... Stop your work
        stopForeground(true);
        isShowingForegroundNotification = false;
        if (false) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_DETACH); //now you can dismiss the notification
                stopForeground(STOP_FOREGROUND_REMOVE);
            }
        }

        try {
            this.Thread1.interrupt();
            this.serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    class Thread1 implements Runnable {

        public void run() {
            Socket socket = null;

            try {
                serverSocket = new ServerSocket(SERVERPORT);
            } catch (IOException e) {
                e.printStackTrace();
            }
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    socket = serverSocket.accept();

                    Thread2 commThread = new Thread2(socket);
                    new Thread(commThread).start();
                    return;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    class Thread2 implements Runnable {

        private Socket clientSocket;
        private BufferedReader input;


        public Thread2(Socket clientSocket) {
            this.clientSocket = clientSocket;
            try {
                this.input = new BufferedReader(new InputStreamReader(this.clientSocket.getInputStream()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void run() {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    String read = input.readLine();

                    if (read != null) {
                        if (read.equals("1")) {

                            //get video service
                            //https://api.thingspeak.com/channels/418232/fields/1.xml?api_key=FDDXM9I0TKKIFHVL&results=1

                            //get third party video service name from the cloud
                            String videoservicecloud = "https://api.thingspeak.com/channels/418232/fields/1.xml?api_key=FDDXM9I0TKKIFHVL&results=1";
                            OkHttpClient videoclient = new OkHttpClient.Builder()
                                    .connectTimeout(3, TimeUnit.SECONDS)
                                    .build();
                            okhttp3.Request videoservicerequest = new okhttp3.Request.Builder()
                                    .url(videoservicecloud)
                                    .build();
                            okhttp3.Response videoresponse = videoclient.newCall(videoservicerequest).execute();

                            String resultstring = videoresponse.body().string();
                            final String videoservicename = resultstring.substring(resultstring.lastIndexOf("<field1>")+8, resultstring.lastIndexOf("</field1>"));


                            try {
                                Thread.sleep(1500);
                            }
                            catch (Exception exs)
                            {

                            }

                            SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
                            String blynkauthset = settings.getString("blynkauth", "");

                            RequestQueue mRequestQueue;
                            // Instantiate the cache
                            Cache cache = new DiskBasedCache(getCacheDir(), 1024 * 1024); // 1MB cap
                            // Set up the network to use HttpURLConnection as the HTTP client.
                            Network network = new BasicNetwork(new HurlStack());
                            // Instantiate the RequestQueue with the cache and network.
                            mRequestQueue = new RequestQueue(cache, network);
                            // Start the queue
                            mRequestQueue.start();
                            String url ="http://blynk-cloud.com/"+blynkauthset+"/get/V4";
                            // Formulate the request and handle the response.
                            StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                                    new Response.Listener<String>() {
                                        @Override
                                        public void onResponse(String response) {
                                            //extract only integers from response string, I.E the show ID
                                            String nfxID = response.replaceAll("\\D+","");;

                                            /*//fallback if I decide to send all the IDs instead of just the latest
                                            if (nfxID.length() != 8)
                                            {
                                                nfxID = nfxID.substring(0,7);
                                            }*/

                                            String watchUrl = "http://www."+videoservicename+".com/watch/"+nfxID;

                                            try {

                                                Intent intent = new Intent(Intent.ACTION_VIEW);
                                                intent.setClassName("com."+videoservicename+".mediaclient", "com."+videoservicename+".mediaclient.ui.launch.UIWebViewActivity");
                                                intent.setData(Uri.parse(watchUrl));
                                                startActivity(intent);

                                                //Other way of launching video service
                                                /*
                                                Intent nfxID = new Intent();
                                                nfx.setAction(Intent.ACTION_VIEW);
                                                nfx.setData(Uri.parse("http://www."+videoservicename+".com/watch/"+"nfxID"));
                                                nfx.putExtra("source","30"); // careful: String, not int
                                                nfx.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                                startActivity(nfx);*/
                                            }
                                            catch(Exception e)
                                            {
                                            }

                                        }
                                    },
                                    new Response.ErrorListener() {
                                        @Override
                                        public void onErrorResponse(VolleyError error) {

                                        }
                                    });
                            // Add the request to the RequestQueue.
                            mRequestQueue.add(stringRequest);
                        }
                        else
                        {   //music services
                            if (read.equals("2"))
                            {
                                // get music service
                                //https://api.thingspeak.com/channels/432248/fields/1.xml?api_key=K69YT0LN7V9ONPPT&results=1

                                //get third party video service name from the cloud
                                String musicservicecloud = "https://api.thingspeak.com/channels/432248/fields/1.xml?api_key=K69YT0LN7V9ONPPT&results=1";
                                OkHttpClient musicclient = new OkHttpClient.Builder()
                                        .connectTimeout(3, TimeUnit.SECONDS)
                                        .build();
                                okhttp3.Request musicservicerequest = new okhttp3.Request.Builder()
                                        .url(musicservicecloud)
                                        .build();
                                okhttp3.Response musicresponse = musicclient.newCall(musicservicerequest).execute();

                                String resultstring = musicresponse.body().string();
                                final String musicservicename = resultstring.substring(resultstring.lastIndexOf("<field1>")+8, resultstring.lastIndexOf("</field1>"));

                                try {
                                    Thread.sleep(1000);
                                }
                                catch (Exception exs)
                                {

                                }


                                SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
                                String blynkauthset = settings.getString("blynkauth", "");
                                String url ="http://blynk-cloud.com/"+blynkauthset+"/get/V2";

                                try {

                                    OkHttpClient client = new OkHttpClient();

                                    okhttp3.Request request = new okhttp3.Request.Builder()
                                            .url(url)
                                            .build();

                                    okhttp3.Response response = client.newCall(request).execute();
                                    String resp = response.body().string();

                                    String[] ips = new String[3];
                                    //Clean response and get only the IPs
                                    String ipresponse = resp.substring(2,resp.length()-2);
                                    //get index of fourth dot
                                    int fourth = ordinalIndexOf(ipresponse, ".",4);
                                    //get first IP
                                    ips[0] = ipresponse.substring(0,fourth);
                                    Log.d("ip 1", ips[0]);
                                    //get index of eighth dot
                                    int eighth = ordinalIndexOf(ipresponse, ".",8);
                                    //get second IP
                                    ips[1] = ipresponse.substring(fourth+1,eighth);
                                    Log.d("ip 2", ips[1]);
                                    int end = ipresponse.length();
                                    //get third IP
                                    ips[2] = ipresponse.substring(eighth+1,end);
                                    Log.d("ip 3", ips[2]);;

                                    for (int p = 0; p<ips.length; p++)
                                    {
                                        try
                                        {
                                            //try to stop music streaming if its running on remote computer
                                            String spoturl = "http://"+ips[p]+":13000?4-EOF-";
                                            OkHttpClient stopMusic = new OkHttpClient.Builder()
                                                    .connectTimeout(2, TimeUnit.SECONDS)
                                                    .build();
                                            okhttp3.Request spotrequest = new okhttp3.Request.Builder()
                                                    .url(spoturl)
                                                    .build();
                                            okhttp3.Response spotresponse = stopMusic.newCall(spotrequest).execute();
                                        }
                                        catch (Exception xe)
                                        {

                                        }

                                    }

                                }
                                catch (Exception spe)
                                {

                                }
                                try {
                                    Thread.sleep(1500);
                                }
                                catch (Exception exs)
                                {

                                }
                                //press the play button
                                Intent i = new Intent(Intent.ACTION_MEDIA_BUTTON);
                                i.setComponent(new ComponentName("com."+musicservicename+".music", "com."+musicservicename+".music.internal.receiver.MediaButtonReceiver"));
                                i.putExtra(Intent.EXTRA_KEY_EVENT,new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY));
                                sendOrderedBroadcast(i, null);

                                i = new Intent(Intent.ACTION_MEDIA_BUTTON);
                                i.setComponent(new ComponentName("com."+musicservicename+".music", "com."+musicservicename+".music.internal.receiver.MediaButtonReceiver"));
                                i.putExtra(Intent.EXTRA_KEY_EVENT,new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PLAY));
                                sendOrderedBroadcast(i, null);
                                    try {
                                        Thread.sleep(800);
                                    }
                                    catch (Exception e)
                                    {

                                    }
                                    if (IsAudioRunning() == false)
                                    {
                                        i = new Intent(Intent.ACTION_MEDIA_BUTTON);
                                        i.setComponent(new ComponentName("com."+musicservicename+".music", "com."+musicservicename+".music.internal.receiver.MediaButtonReceiver"));
                                        i.putExtra(Intent.EXTRA_KEY_EVENT,new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY));
                                        sendOrderedBroadcast(i, null);

                                        i = new Intent(Intent.ACTION_MEDIA_BUTTON);
                                        i.setComponent(new ComponentName("com."+musicservicename+".music", "com."+musicservicename+".music.internal.receiver.MediaButtonReceiver"));
                                        i.putExtra(Intent.EXTRA_KEY_EVENT,new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PLAY));
                                        sendOrderedBroadcast(i, null);

                                        try {
                                            Thread.sleep(500);
                                        }
                                        catch (Exception e)
                                        {

                                        }
                                    }

                                try {
                                    Thread.sleep(800);
                                }
                                catch (Exception e)
                                {

                                }

                                if (IsAudioRunning() == false)
                                {
                                    Intent launchIntent = getPackageManager().getLaunchIntentForPackage("com."+musicservicename+".music");
                                    if (launchIntent != null) {
                                        startActivity(launchIntent);//null pointer check in case package name was not found
                                    }
                                    try {
                                        Thread.sleep(500);
                                    }
                                    catch (Exception e)
                                    {

                                    }

                                    i = new Intent(Intent.ACTION_MEDIA_BUTTON);
                                    i.setComponent(new ComponentName("com."+musicservicename+".music", "com."+musicservicename+".music.internal.receiver.MediaButtonReceiver"));
                                    i.putExtra(Intent.EXTRA_KEY_EVENT,new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY));
                                    sendOrderedBroadcast(i, null);

                                    i = new Intent(Intent.ACTION_MEDIA_BUTTON);
                                    i.setComponent(new ComponentName("com."+musicservicename+".music", "com."+musicservicename+".music.internal.receiver.MediaButtonReceiver"));
                                    i.putExtra(Intent.EXTRA_KEY_EVENT,new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PLAY));
                                    sendOrderedBroadcast(i, null);
                                }


                            }
                            else
                            {   //vlc media player
                                if (read.equals("3"))
                                {

                                    SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
                                    String blynkauthset = settings.getString("blynkauth", "");
                                    Log.d("blynkauth",blynkauthset);

                                    RequestQueue mRequestQueue;
                                    // Instantiate the cache
                                    Cache cache = new DiskBasedCache(getCacheDir(), 1024 * 1024); // 1MB cap
                                    // Set up the network to use HttpURLConnection as the HTTP client.
                                    Network network = new BasicNetwork(new HurlStack());
                                    // Instantiate the RequestQueue with the cache and network.
                                    mRequestQueue = new RequestQueue(cache, network);
                                    // Start the queue
                                    mRequestQueue.start();
                                    String url ="http://blynk-cloud.com/"+blynkauthset+"/get/V2";
                                    // Formulate the request and handle the response.
                                    StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                                            new Response.Listener<String>() {
                                                @Override
                                                public void onResponse(String response) {
                                                    String[] ips = new String[3];
                                                    //Clean response and get only the IPs
                                                    String ipresponse = response.substring(2,response.length()-2);
                                                    //get index of fourth dot
                                                    int fourth = ordinalIndexOf(ipresponse, ".",4);
                                                    //get first IP
                                                    ips[0] = ipresponse.substring(0,fourth);
                                                    Log.d("ip 1", ips[0]);
                                                    //get index of eighth dot
                                                    int eighth = ordinalIndexOf(ipresponse, ".",8);
                                                    //get second IP
                                                    ips[1] = ipresponse.substring(fourth+1,eighth);
                                                    Log.d("ip 2", ips[1]);
                                                    //get third IP
                                                    ips[2] = ipresponse.substring(eighth+1,ipresponse.length());
                                                    Log.d("ip 3", ips[2]);;
                                                    //get current position of video response
                                                    String gotresponse = "";
                                                    //get file location response
                                                    String gotresponse2 = "";
                                                    //figure out where VLC is running at the moment
                                                    for (int i=0; i<ips.length; i++)
                                                    {
                                                        OkHttpClient client = new OkHttpClient.Builder()
                                                                .addInterceptor(new BasicAuthInterceptor("", "streamyarduino2018"))
                                                                .build();

                                                        okhttp3.Request request = new okhttp3.Request.Builder()
                                                                .url("http://"+ips[i]+":8080/requests/status.xml")
                                                                .build();
                                                        try {
                                                            okhttp3.Response responses = client.newCall(request).execute();
                                                            gotresponse = responses.body().string();
                                                            if (gotresponse != "" || gotresponse != null)
                                                            {
                                                                //get current time in milliseconds
                                                                int currtime = (Integer.parseInt(gotresponse.substring(gotresponse.indexOf("<time>")+6,gotresponse.indexOf("</time>"))))*1000;
                                                                //get location of file
                                                                OkHttpClient client2 = new OkHttpClient.Builder()
                                                                        .addInterceptor(new BasicAuthInterceptor("", "streamyarduino2018"))
                                                                        .build();

                                                                okhttp3.Request request2 = new okhttp3.Request.Builder()
                                                                        .url("http://"+ips[i]+":8080/requests/playlist.xml")
                                                                        .build();
                                                                try {
                                                                    okhttp3.Response responses2 = client.newCall(request2).execute();
                                                                    gotresponse2 = responses2.body().string();
                                                                    //Log.d("second response is", gotresponse2);
                                                                    //parse location of file
                                                                    if ((gotresponse2.lastIndexOf("file:///") != -1) && (gotresponse2.indexOf("current=") != -1))
                                                                    {
                                                                        String clean = gotresponse2.substring(gotresponse2.lastIndexOf("file:///")+11,gotresponse2.indexOf("current=")-2);
                                                                        //URI with IP
                                                                        String fileuri = ips[i]+ "/"+ clean;
                                                                        //path without IP
                                                                        String filename = clean.substring(clean.lastIndexOf("/"),clean.length());

                                                                        Log.d("clean is", clean);
                                                                        Log.d("fileurl is", fileuri);
                                                                        Log.d("filname is", filename);

                                                                        //subtitles are not really auto loading even though VLC settings are enabled, also need to check if subtitles file exist
                                                                        //before putting it in vlcIntent.... so I turned this feature off
                                                                        //String subtitles = fileuri.substring(0, fileuri.lastIndexOf(".")) + ".srt";
                                                                        //subtitles = subtitles.replace("/","//");
                                                                        int count = fileuri.length() - fileuri.replaceAll("/","").length();
                                                                        //fileuri = fileuri.replace("/","//");

                                                                        //try all the path files to see which one is the correct file, this saves the user from configuring the folder every time.
                                                                            try {
                                                                                SmbFile sFile = new SmbFile("smb://" + fileuri.replace("/", "//"));
                                                                                if (sFile.exists()) {
                                                                                    fileuri = fileuri.replace("/", "//");
                                                                                }
                                                                            } catch (Exception e) {
                                                                                for (int x=0; x<count-1; x++) {
                                                                                    try {
                                                                                        SmbFile sFile = new SmbFile("smb://"+ips[i]+"//"+clean.replace("/","//"));
                                                                                        if (sFile.exists()) {
                                                                                            fileuri = ips[i]+"//"+clean.replace("/","//");
                                                                                            x = count;

                                                                                        }
                                                                                    } catch (Exception ex) {
                                                                                        clean = clean.substring(clean.indexOf("/")+1,clean.length());
                                                                                    }
                                                                                }
                                                                            }
                                                                        //launch intent
                                                                        Uri uri = Uri.parse("smb://"+fileuri);
                                                                        Intent vlcIntent = new Intent(Intent.ACTION_VIEW);
                                                                        vlcIntent.setPackage("org.videolan.vlc");
                                                                        vlcIntent.setDataAndTypeAndNormalize(uri, "video/*");
                                                                        //vlcIntent.putExtra("from_start", false);
                                                                        vlcIntent.putExtra("position", currtime);
                                                                        //vlcIntent.putExtra("subtitles_location", "smb://"+subtitles);
                                                                        //Intent launchIntent = getPackageManager().getLaunchIntentForPackage("org.videolan.vlc");
                                                                        if (vlcIntent != null) {
                                                                            startActivity(vlcIntent);//null pointer check in case package name was not found
                                                                        }
                                                                    }


                                                                } catch (IOException e) {
                                                                    e.printStackTrace();
                                                                }


                                                                //stop the loop
                                                                i = ips.length;
                                                            }

                                                        } catch (IOException e) {
                                                            e.printStackTrace();
                                                        }

                                                    }

                                                }
                                            },
                                            new Response.ErrorListener() {
                                                @Override
                                                public void onErrorResponse(VolleyError error) {

                                                }
                                            });
                                    // Add the request to the RequestQueue.
                                    mRequestQueue.add(stringRequest);
                                }
                            }
                        }
                    } else {
                        Thread1 = new Thread(new Thread1());
                        Thread1.start();
                        return;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public int ordinalIndexOf(String str, String substr, int n) {
        int pos = str.indexOf(substr);
        while (--n > 0 && pos != -1)
            pos = str.indexOf(substr, pos + 1);
        return pos;
    }

    public class BasicAuthInterceptor implements Interceptor {

        private String credentials;

        public BasicAuthInterceptor(String user, String password) {
            this.credentials = Credentials.basic(user, password);
        }

        @Override
        public okhttp3.Response intercept(Chain chain) throws IOException {
            okhttp3.Request request = chain.request();
            okhttp3.Request authenticatedRequest = request.newBuilder()
                    .header("Authorization", credentials).build();
            return chain.proceed(authenticatedRequest);
        }

    }

    public boolean IsAudioRunning()
    {
        AudioManager myAudioManager;
        myAudioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);

        if (myAudioManager.isMusicActive() == true)
        {
            return true;
        }
        else {
            return false;
        }
    }

}
