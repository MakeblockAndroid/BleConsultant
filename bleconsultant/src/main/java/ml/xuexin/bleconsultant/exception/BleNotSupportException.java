package ml.xuexin.bleconsultant.exception;

/**
 * Created by xuexin on 2017/5/31.
 */

public class BleNotSupportException extends RuntimeException {
    public BleNotSupportException() {
        super("Device don't support BLE");
    }
}
