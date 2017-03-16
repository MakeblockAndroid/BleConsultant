package ml.xuexin.bleconsultant.port;

/**
 * Created by xuexin on 2017/3/9.
 */

public interface ConnectCallback extends OvertimeInterface{

     int STATE_UNINIT = -1;

    /** The profile is in disconnected state */
     int STATE_DISCONNECTED  = 0;
    /** The profile is in connecting state */
     int STATE_CONNECTING    = 1;
    /** The profile is in connected state */
     int STATE_CONNECTED     = 2;
    /** The profile is in disconnecting state */
     int STATE_DISCONNECTING = 3;

     int STATE_SERVICES_DISCOVERED = 4;

     int STATE_CONNECT_OVERTIME = 5;

     int STATE_SERVICES_DISCOVERED_FAIL = 6;


    void onStateChange(int state);
}
