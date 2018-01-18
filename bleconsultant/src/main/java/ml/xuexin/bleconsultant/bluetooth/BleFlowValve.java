package ml.xuexin.bleconsultant.bluetooth;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.LinkedBlockingQueue;

import ml.xuexin.bleconsultant.entity.WaitSendData;
import ml.xuexin.bleconsultant.tool.BleLog;

/**
 * BLE is too slowly.
 * Created by xuexin on 2017/3/9.
 */

public class BleFlowValve implements Resettable {
    private final Connector connector;
    private final LinkedBlockingQueue<WaitSendData> waitSendDataQueue = new LinkedBlockingQueue<>();
    private final Timer sendDataTimer;
    private final int dataMaxLength;

    private byte[] dataCache = new byte[1024];

    private WaitSendData lastData;

    private int cacheCount = 0;

    public BleFlowValve(Connector connector, int dataMaxLength, int timeGap) {
        this.connector = connector;
        this.dataMaxLength = dataMaxLength;
        sendDataTimer = new Timer("BleFlowValve", true);
        sendDataTimer.schedule(new SendDataTimerTask(), 0, timeGap);
    }

    public boolean sendData(WaitSendData waitSendData) {
        waitSendDataQueue.add(waitSendData);
        return true;
    }

    @Override
    public void reset() {
        waitSendDataQueue.clear();
    }

    public void destory() {
        reset();
        sendDataTimer.cancel();
    }

    private class SendDataTimerTask extends TimerTask {
        @Override
        public void run() {
            while (cacheCount < dataMaxLength && waitSendDataQueue.size() > 0) {
                WaitSendData waitSendData = waitSendDataQueue.peek();
                if (lastData == null) {
                    lastData = waitSendData;
                }
                if (lastData.isSameCharacteristic(waitSendData)) {
                    byte[] nextData = waitSendData.data;
                    System.arraycopy(nextData, 0, dataCache, cacheCount, nextData.length);
                    cacheCount += nextData.length;
                } else {
                    break;
                }
                if (waitSendData != null) {
                    lastData = waitSendDataQueue.poll();
                }
            }
            //send
            int sendCount = cacheCount > dataMaxLength ? dataMaxLength : cacheCount;
            if (sendCount == 0) {
                return;
            } else {
                byte[] data = new byte[sendCount];
                System.arraycopy(dataCache, 0, data, 0, sendCount);
                if (connector.writeToBle(lastData.serviceUuid, lastData.characteristicUuid, data)) {
                    BleLog.w(lastData.characteristicUuid + ", send:" + BleLog.parseByte(data));
                } else {
                    BleLog.w("send fail, wait to next time");
                    return;
                }
                cacheCount -= sendCount;
                if (cacheCount > 0) {
                    System.arraycopy(dataCache, sendCount, dataCache, 0, cacheCount);
                } else {
                    lastData = null;
                }
            }
        }
    }
}
