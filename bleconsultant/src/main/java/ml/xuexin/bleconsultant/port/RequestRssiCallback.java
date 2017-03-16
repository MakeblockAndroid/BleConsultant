package ml.xuexin.bleconsultant.port;

/**
 * Created by xuexin on 2017/3/13.
 */

public interface RequestRssiCallback extends OvertimeInterface {

    void onReadRemoteRssi(int rssi, int status);
}
