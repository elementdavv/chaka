package net.timelegend.chaka.viewer;

import android.app.ActionBar;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.MenuItem;
import android.view.View;

import androidx.activity.ComponentActivity;
import androidx.core.content.ContextCompat;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.net.URL;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

import javax.net.ssl.HttpsURLConnection;

import com.mittsu.markedview.MarkedView;

public class HelpActivity extends ComponentActivity
{
    private Handler handler;

    @Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        ActionBar ab = getActionBar();
        ab.setDisplayOptions(ActionBar.DISPLAY_SHOW_TITLE | ActionBar.DISPLAY_HOME_AS_UP,
                ActionBar.DISPLAY_SHOW_TITLE | ActionBar.DISPLAY_HOME_AS_UP | ActionBar.DISPLAY_USE_LOGO);
        ab.setTitle(R.string.help);
        Tool.fullScreen(getWindow());
        setContentView(R.layout.help_activity);

        handler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                costMessage(msg);
            }
        };

        gotFile();
    }

    private void gotFile() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(new Runnable() {
            @Override
            public void run() {
                Message msg = handler.obtainMessage();

                try {
                    URL url = new URL(getResources().getString(R.string.readme));
                    HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
                    conn.setDoInput(true);
                    conn.connect();
                    InputStream is = conn.getInputStream();
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(is));

                    String readText = "";
                    StringBuilder stringBuilder = new StringBuilder();

                    while ((readText = bufferedReader.readLine()) != null) {
                        stringBuilder.append(readText);
                        stringBuilder.append("\n");
                    }

                    is.close();
                    conn.disconnect();
                    Bundle bundle = new Bundle();
                    bundle.putCharSequence("key", stringBuilder.toString());
                    msg.what = 1;
                    msg.setData(bundle);
                }
                catch (IOException e) {
                    e.printStackTrace();
                    msg.what = 0;
                }
                finally {
                    msg.sendToTarget();
                    executor.shutdown();
                }
            }
        });
    }

    private void costMessage(Message msg) {
        MarkedView mvHelp = (MarkedView)findViewById(R.id.mvHelp);
        mvHelp.chColor("white");
        mvHelp.chBackgroundColor("#" + Integer.toHexString(ContextCompat.getColor(this, R.color.toolbar) & 0x00ffffff));
        mvHelp.init();
        String mdText;

        if (msg.what == 1)
            mdText = msg.getData().getCharSequence("key").toString();
        else
            mdText = "Network Error";

        mvHelp.setMDText(mdText);

        handler.postDelayed(new Runnable() {
            public void run() {
                mvHelp.setVisibility(View.VISIBLE);

                handler.postDelayed(new Runnable() {
                    public void run() {
                        View mvOverlay = (View)findViewById(R.id.mvOverlay);
                        mvOverlay.setVisibility(View.INVISIBLE);
                        if (Tool.delay > 300)
                            Tool.delay -= 300;
                        else if (Tool.delay > 200)
                            Tool.delay -= 200;
                    }
                }, Tool.delay);
            }
        }, 200);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        // Respond to the action bar's Up/Home button
        case android.R.id.home:
            super.onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

}
