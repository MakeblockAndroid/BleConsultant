package ml.xuexin.bleconsultant.tool;

import android.os.Handler;
import android.os.Looper;

/**
 * Created by xuexin on 2017/3/8.
 */

public class ThreadUtil {
    public static boolean isMainThread() {
        return Thread.currentThread() == Looper.getMainLooper().getThread();
    }
}
