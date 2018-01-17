package ml.xuexin.bleconsultant.port;

import java.util.List;

import ml.xuexin.bleconsultant.entity.BleClient;

/**
 * Created by xuexin on 2017/3/3.
 */

public interface ScanClientsHelper {

    /**
     * This method will be always called at UI thread.
     * @param bleClientList List of filtered devices
     */
    void reportClients(List<BleClient> bleClientList);

    /**
     *
     * @param bleClient
     * @return return false will ignore the device
     */
    boolean clientFilter(BleClient bleClient);


    /**
     * per report time(ms)
     * @return
     */
    long getReportPeriod();

}
