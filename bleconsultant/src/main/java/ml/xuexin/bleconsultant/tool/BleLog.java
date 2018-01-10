package ml.xuexin.bleconsultant.tool;

import android.util.Log;

/**
 * Created by xuexin on 2017/3/7.
 */

public class BleLog {
    public static boolean DEBUG = false;
    public static final String TAG = "BleConsultant";

    public static void e(String msg) {
        if (msg != null)
            Log.e(TAG, msg);
    }

    public static void d(String msg) {
        if (msg != null)
            if (DEBUG) Log.d(TAG, msg);
    }

    public static void w(String msg) {
        if (msg != null)
            if (DEBUG) Log.w(TAG, msg);
    }

    public static void i(String msg) {
        if (msg != null)
            if (DEBUG) Log.i(TAG, msg);
    }


    /**
     * parse byte[] to hex string
     *
     * @param data
     * @return
     */
    public static String parseByte(byte[] data) {
        String s = "";
        for (int i = 0; i < data.length; ++i) {
            s += String.format(" %02x", data[i] & 0x0ff);
        }
        return s;
    }
}
