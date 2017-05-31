package ml.xuexin.bleconsultant.exception;

/**
 * Created by xuexin on 2017/5/31.
 */

public class ThreadException extends RuntimeException {
    public ThreadException() {
        super("Please call init on main thread");
    }
}
