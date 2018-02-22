package d.streamy.streamyapp;

        import android.content.Intent;
        import android.content.SharedPreferences;
        import android.net.wifi.WifiManager;
        import android.support.v7.app.AppCompatActivity;
        import android.os.Bundle;
        import android.text.format.Formatter;
        import android.view.View;
        import android.view.ViewGroup;
        import android.widget.Button;
        import android.widget.EditText;
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

public class MainActivity extends AppCompatActivity {
    private EditText EDITTEXT;
    private EditText Blynktext;
    public static final String PREFS_NAME = "MyPrefsFile";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        String blynkauthset = settings.getString("blynkauth", "");
        Blynktext = (EditText) findViewById(R.id.Blynktext);
        //Set service button
        Button btn = (Button) findViewById(R.id.button);

            Blynktext.setText(blynkauthset);

            if (Blynktext.getText().toString() != null && Blynktext.getText().toString() != "Blynk Auth")
            {
                btn.setEnabled(true);
            }


        EDITTEXT = (EditText) findViewById(R.id.edittext);

        WifiManager wm = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        String ip = Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress());

        if (ip == "0.0.0.0")
        {
            EDITTEXT.setText((EDITTEXT.getText().toString() +" Please connect to Wifi/LAN and restart app"));
        }
        else
        {
            EDITTEXT.setText((EDITTEXT.getText().toString() + " The LAN IP of this device is " + ip));
        }

    }

    public void testService(View view) {
        startDemo(ActivityBasicService.class);
    }

    private void startDemo(Class activity) {
        Intent i = new Intent(this, activity);
        startActivity(i);
    }

    public void SaveBlynk(View view) {
        EDITTEXT = (EditText) findViewById(R.id.edittext);

        Blynktext = (EditText) findViewById(R.id.Blynktext);
        Button btn = (Button) findViewById(R.id.button);

        //get new blynk auth
        String newblynkauth= Blynktext.getText().toString();
        RequestQueue mRequestQueue;
    // Instantiate the cache
        Cache cache = new DiskBasedCache(getCacheDir(), 1024 * 1024); // 1MB cap
    // Set up the network to use HttpURLConnection as the HTTP client.
        Network network = new BasicNetwork(new HurlStack());
    // Instantiate the RequestQueue with the cache and network.
        mRequestQueue = new RequestQueue(cache, network);
    // Start the queue
        mRequestQueue.start();
        String url ="http://blynk-cloud.com/"+newblynkauth+"/update/V15?value=verified";

    // Formulate the request and handle the response.
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        EDITTEXT.setText("Blynk auth code is working");
                        Blynktext = (EditText) findViewById(R.id.Blynktext);

                        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
                        SharedPreferences.Editor editor = settings.edit();
                        editor.putString("blynkauth",Blynktext.getText().toString());
                        editor.commit();
                        Button btn = (Button) findViewById(R.id.button);
                        btn.setEnabled(true);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        EDITTEXT.setText("Wrong code or no internet");
                        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
                        SharedPreferences.Editor editor = settings.edit();
                        editor.commit();
                        Button btn = (Button) findViewById(R.id.button);
                        btn.setEnabled(false);
                    }
                });
    // Add the request to the RequestQueue.
        mRequestQueue.add(stringRequest);
    }
}
