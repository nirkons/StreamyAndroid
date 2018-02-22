package d.streamy.streamyapp;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayDeque;

public class StartedServiceDemo extends Service {

    public static final String EXTRA_STRING = "extra_string";
    public static String ACTION_REMOVE = "action_remove";
    public static String ACTION_STOP = "action_stop";
    final ArrayDeque<String> messagesQue = new ArrayDeque<>();
    volatile boolean shouldStop;

    public StartedServiceDemo() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        shouldStop = false;
        Log.d("LOG", "Starting service");
        new Thread(new RunnableWorker(), "BackgroundThread-1")
                .start();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        Log.d("DEBUG", "Service Started");
        String message = intent.getStringExtra(EXTRA_STRING);
        Log.v("StartedServiceDemo", "onStartCommand() called, message = " + message + ", startId = " + startId);

        synchronized (messagesQue) {
            if (intent.getAction().equals(ACTION_STOP))
                shouldStop = true;
            else if (intent.getAction().equals(ACTION_REMOVE))
                if (messagesQue.contains(intent.getStringExtra(EXTRA_STRING)))
                    messagesQue.remove(intent.getStringExtra(EXTRA_STRING));
        }
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d("DEBUG", "onDestroy Called");
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d("DEBUG", "onUnbind Called");
        return super.onUnbind(intent);
    }

    @Override
    public void onRebind(Intent intent) {
        Toast.makeText(this, "onRebind Called", Toast.LENGTH_SHORT).show();
        super.onRebind(intent);
    }

    class RunnableWorker implements Runnable {
        @Override
        public void run() {

            while (!shouldStop) {
                String message = null;
                synchronized (messagesQue) {

                    message = messagesQue.poll();
                    //Do your operations here ... i.e Logging to a file or whateverh
                    if (message != null)
                        Log.d("DEBUG", "New message consumed : " + message);
                }
            }
            Log.d("DEBUG", "Finish Loop");
            stopSelf();
        }
    }
}
