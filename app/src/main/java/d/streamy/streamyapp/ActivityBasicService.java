package d.streamy.streamyapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class ActivityBasicService extends AppCompatActivity {

    public static final String TAG = "BASICSERVICE";
    public static int count = 0;
    private EditText EDITTEXT;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_basic_service);

        EDITTEXT = (EditText)findViewById(R.id.edittext);
    }


    @Override
    protected void onDestroy() {
        if (stopService(new Intent(ActivityBasicService.this, StartedServiceDemo.class)))
            Log.d(TAG, "stopped Succesfully");
        else
            Log.d(TAG, "stopped");
        super.onDestroy();
    }

    public void startForeground(View view) {
        startService(new Intent(this, BasicService.class)
                .putExtra("foreground", true));
    }

    public void stopForeground(View view) {
        startService(new Intent(this, BasicService.class).putExtra("foreground", true));
    }
}


