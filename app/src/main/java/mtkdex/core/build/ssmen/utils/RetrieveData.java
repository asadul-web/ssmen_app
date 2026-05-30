package mtkdex.core.build.ssmen.utils;

import android.net.TrafficStats;

import java.util.ArrayList;
import java.util.List;

public class RetrieveData {

    static long totalUpload = 0;
    static long totalDownload = 0;
    static long totalUpload_n = 0;
    static long totalDownload_n = 0;

    public static List<Long> findData() {
        List<Long> allData = new ArrayList<>();

        long newTotalDownload, incDownload, newTotalUpload, incUpload;
        int uid = android.os.Process.myUid();

        // Use Uid stats instead of total stats for more accuracy in VPN traffic tracking
        long currentUidRx = TrafficStats.getUidRxBytes(uid);
        long currentUidTx = TrafficStats.getUidTxBytes(uid);
        
        // Fallback to total if UID stats are not supported (though they should be)
        if (currentUidRx == TrafficStats.UNSUPPORTED) currentUidRx = TrafficStats.getTotalRxBytes();
        if (currentUidTx == TrafficStats.UNSUPPORTED) currentUidTx = TrafficStats.getTotalTxBytes();

        if (totalDownload == 0)
            totalDownload = currentUidRx;

        if (totalUpload == 0)
            totalUpload = currentUidTx;

        newTotalDownload = currentUidRx;
        incDownload = newTotalDownload - totalDownload;

        newTotalUpload = currentUidTx;
        incUpload = newTotalUpload - totalUpload;

        totalDownload = newTotalDownload;
        totalUpload = newTotalUpload;

        allData.add(incDownload);
        allData.add(incUpload);

        return allData;
    }

    public static long getNotificationData() {

        long newTotalDownload, incDownload, newTotalUpload, incUpload;
        int uid = android.os.Process.myUid();
        long currentUidRx = TrafficStats.getUidRxBytes(uid);
        long currentUidTx = TrafficStats.getUidTxBytes(uid);
        if (currentUidRx == TrafficStats.UNSUPPORTED) currentUidRx = TrafficStats.getTotalRxBytes();
        if (currentUidTx == TrafficStats.UNSUPPORTED) currentUidTx = TrafficStats.getTotalTxBytes();

        if (totalDownload_n == 0)
            totalDownload_n = currentUidRx;

        if (totalUpload_n == 0)
            totalUpload_n = currentUidTx;

        newTotalDownload = currentUidRx;
        incDownload = newTotalDownload - totalDownload_n;

        newTotalUpload = currentUidTx;
        incUpload = newTotalUpload - totalUpload_n;

        totalDownload_n = newTotalDownload;
        totalUpload_n = newTotalUpload;

        return incDownload + incUpload;
    }
}

