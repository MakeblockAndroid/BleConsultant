package ml.xuexin.bleconsultant.port;

/**
 * Created by xuexin on 2017/3/14.
 */

public interface ReadCallback extends OvertimeInterface {
    void onCharacteristicRead(int status, byte[] data);
}
