package ml.xuexin.bleconsultant.exception;

/**
 * Created by xuexin on 2017/5/31.
 */

public class InitBluetoothException extends RuntimeException {
    public InitBluetoothException() {
        super("Init BluetoothAdapter fail");
    }
}
