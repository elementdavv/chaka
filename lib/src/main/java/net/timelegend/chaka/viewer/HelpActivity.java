package net.timelegend.chaka.viewer;

import android.app.ActionBar;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.MenuItem;
import android.view.View;

import androidx.activity.ComponentActivity;
import androidx.core.content.ContextCompat;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HttpsURLConnection;

import org.json.JSONException;
import org.json.JSONObject;

import com.mittsu.markedview.MarkedView;

public class HelpActivity extends ComponentActivity
{
    public static int delay;

    @Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        ActionBar ab = getActionBar();
        ab.setDisplayOptions(ActionBar.DISPLAY_SHOW_TITLE | ActionBar.DISPLAY_HOME_AS_UP,
                ActionBar.DISPLAY_SHOW_TITLE | ActionBar.DISPLAY_HOME_AS_UP | ActionBar.DISPLAY_USE_LOGO);
        ab.setTitle(R.string.help);
        setContentView(R.layout.help_activity);
        showReadme();
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        Tool.fullScreen(getWindow());
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

    private void showReadme() {
        MarkedView mvHelp = (MarkedView)findViewById(R.id.mvHelp);
        mvHelp.chColor("#" + Tool.colorHex(android.R.color.white));
        mvHelp.chBackgroundColor("#" + Tool.colorHex(R.color.toolbar));
        mvHelp.init();
        File dir = getExternalFilesDir(null);
        File f = new File(dir, README);
        mvHelp.loadMDFile(f);

        Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(new Runnable() {
            public void run() {
                mvHelp.setVisibility(View.VISIBLE);

                handler.postDelayed(new Runnable() {
                    public void run() {
                        View mvOverlay = (View)findViewById(R.id.mvOverlay);
                        mvOverlay.setVisibility(View.INVISIBLE);
                        if (delay > 300)
                            delay -= 300;
                        else if (delay > 200)
                            delay -= 200;
                    }
                }, delay);
            }
        }, 200);
    }

    private static String README = "README.md";
    private static String tag, published;

    // called when book layout completed
    public static void updateReadme(Context context) {
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

        executor.schedule(new Runnable() {
            @Override
            public void run() {
                try {
                    File dir = context.getExternalFilesDir(null);
                    File f = new File(dir, README);

                    if (testReadme(f)) return;
                    getApi(context);
                    getReadme(context, f);
                }
                catch (MalformedURLException e) {
                    e.printStackTrace();
                }
                catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }, 1, TimeUnit.SECONDS);

        executor.shutdown();
    }

    private static boolean testReadme(File f) {
        if (f.exists() && f.length() != 0L) {
            long mod = f.lastModified();
            long curr = System.currentTimeMillis();
            long aweek = 7 * 24 * 60 * 60 * 1000;
            if (curr - mod < aweek)
                return true;
        }

        return false;
    }

    private static void getApi(Context context)
            throws MalformedURLException, IOException {
        URL url = new URL(context.getResources().getString(R.string.api));
        HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
        conn.setDoInput(true);
        conn.connect();
        InputStream is = conn.getInputStream();
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(is));
        String line = "";

        try {
            line = bufferedReader.readLine();
            JSONObject jo = new JSONObject(line);
            tag = jo.getString("tag_name").substring(1);
            published = jo.getString("published_at").substring(0, 10);
        }
        catch (JSONException e) {
            e.printStackTrace();
            tag = "";
            published = "";
        }
        is.close();
        conn.disconnect();
    }

    private static void getReadme(Context context, File f)
            throws MalformedURLException, FileNotFoundException, IOException {
        f.delete();
        URL url = new URL(context.getResources().getString(R.string.readme));
        HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
        conn.setDoInput(true);
        conn.connect();
        InputStream is = conn.getInputStream();
        FileOutputStream fos = new FileOutputStream(f);
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));
        String line = "";
        int ln = 0;

        while ((line = br.readLine()) != null) {
            bw.write(line);
            bw.newLine();

            if (ln == 0) {
                ln++;
                line = getVersionLine();
                bw.write(line);
                bw.newLine();
                bw.newLine();
            }
        }

        bw.flush();
        bw.close();
        fos.close();
        is.close();
        conn.disconnect();
    }

    private static String getVersionLine() {
        String cv = BuildConfig.VERSION_NAME;
        String res = "```release " + cv + "```";
        int cp = cv.compareTo(tag);

        if (cp == -1) {
            res = "```release " + cv + "(update available: " + tag + ", published " + published + ")```";
        }
        else if (cp == 1) {
            res = "```unknown release```";
        }

        return res;
    }
}
