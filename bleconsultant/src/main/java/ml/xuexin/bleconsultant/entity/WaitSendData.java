package ml.xuexin.bleconsultant.entity;

import java.util.UUID;

/**
 * Created by xuexin on 2017/3/9.
 */

public class WaitSendData {
    public final byte[] data;
    public final UUID serviceUuid;
    public final UUID characteristicUuid;

    public WaitSendData(byte[] data, UUID serviceUuid, UUID characteristicUuid) {
        this.data = data;
        this.serviceUuid = serviceUuid;
        this.characteristicUuid = characteristicUuid;
    }

    public boolean isSameCharacteristic(WaitSendData waitSendData) {
        if (waitSendData == null)
            return false;
        if (waitSendData == this)
            return true;
        return waitSendData.serviceUuid.equals(this.serviceUuid) &&
                waitSendData.characteristicUuid.equals(this.characteristicUuid);

    }
}
