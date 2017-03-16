package ml.xuexin.bleconsultant.port;

/**
 * This listener will be always hold, beware of memory leaks
 * Created by xuexin on 2017/3/10.
 */

public interface ConnectionStateListener {
    //state is as same as ConnectCallback
    void onStateChange(int state);
}
