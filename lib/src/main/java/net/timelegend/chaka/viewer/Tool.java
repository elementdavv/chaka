package net.timelegend.chaka.viewer;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Tool
{
    public final static String TAG = "Chaka";
    public static Context mContext;

    public final static <T> void i(T v) {
        Log.i(TAG, v.toString());
    }

    public final static <T> void w(T v) {
        Log.w(TAG, v.toString());
    }

    public final static <T> void e(T v) {
        Log.e(TAG, v.toString());
    }

    public final static <T> void d(T v) {
        Log.d(TAG, v.toString());
    }

    public final static <T> void v(T v) {
        Log.v(TAG, v.toString());
    }

    public static String getResourceString(int id) {
       return mContext.getResources().getString(id);
    }

    public static void toastFromResource(int id) {
        Toast.makeText(mContext, getResourceString(id), Toast.LENGTH_SHORT).show();
    }

    public static File getDataDir(String folder) {
        return mContext.getExternalFilesDir(folder);
    }

    public static <T> void toast(T t) {
        String s = null;
        if (t instanceof String) {
            s = (String)t;
        }
        else {
            s = String.valueOf(t);
        }
        Toast.makeText(mContext, s, Toast.LENGTH_SHORT).show();
    }

	public static String toHex(byte[] digest) {
		StringBuilder builder = new StringBuilder(2 * digest.length);

		for (byte b : digest)
			builder.append(String.format("%02x", b));

		return builder.toString();
	}

    @SuppressWarnings("deprecation")
    public static void fullScreen(Window window) {
        // below android 11 (api30)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            final WindowInsetsController controller = window.getInsetsController();

            if(controller != null) {
                controller.hide(WindowInsets.Type.statusBars());
                controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
		    window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
    }

    public static String getDigest(ContentResolver cr, Uri uri) {
        String digest = null;

        try {
            MessageDigest complete = MessageDigest.getInstance("MD5");
            InputStream is = cr.openInputStream(uri);
            byte[] buffer = new byte[1024];
            int numRead;
            numRead = is.read(buffer);

            if (numRead > 0) {
                complete.update(buffer, 0, numRead);
            }

            is.close();
            byte[] b = complete.digest();
            digest = Tool.toHex(b);
        }
        catch (IOException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        return digest;
    }

    public static void saveBitmap(Bitmap bm, String fn) {
        File dir = getDataDir(null);
        File f = new File(dir, fn);
        i("downloadpath:"+f.getAbsolutePath());
        f.delete();
        try (FileOutputStream fos = new FileOutputStream(f)) {
            bm.compress(Bitmap.CompressFormat.PNG, 100, fos);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String colorHex(int color) {
        String hex = Integer.toHexString(ContextCompat.getColor(mContext, color) & 0x00ffffff);
        return hex;
    }

}
