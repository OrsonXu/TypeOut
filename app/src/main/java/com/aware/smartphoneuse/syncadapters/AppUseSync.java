package com.aware.smartphoneuse.syncadapters;

import android.app.Service;
import android.content.Intent;
import android.net.Uri;
import android.os.IBinder;
import androidx.annotation.Nullable;

import com.aware.smartphoneuse.Provider;
import com.aware.syncadapters.AwareSyncAdapter;

/**
 * Created by denzilferreira on 01/09/2017.
 *
 * This class tells what data is synched to the server. The Uri[] needs to be in the same order as the database tables and tables fields (due to the index in the array).
 */
public class AppUseSync extends Service {
    private AwareSyncAdapter sSyncAdapter = null;
    private static final Object sSyncAdapterLock = new Object();

    @Override
    public void onCreate() {
        super.onCreate();
        synchronized (sSyncAdapterLock) {
            if (sSyncAdapter == null) {
                sSyncAdapter = new AwareSyncAdapter(getApplicationContext(), true, true);
                sSyncAdapter.init(
                        Provider.DATABASE_TABLES, Provider.TABLES_FIELDS,
                        new Uri[]{
                                Provider.Applications_Diff.CONTENT_URI,
                                Provider.Applications_Interv.CONTENT_URI,
                                Provider.Applications_Interv_No_Permissions.CONTENT_URI,
                                Provider.Applications_Interv_App_End.CONTENT_URI,
                                Provider.Applications_Switch_Permit.CONTENT_URI,
                                Provider.Applications_Summary.CONTENT_URI,
                                Provider.Applications_Whitelist.CONTENT_URI,
                                Provider.Applications_Phases.CONTENT_URI
                        }
                );
            }
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return sSyncAdapter.getSyncAdapterBinder();
    }
}
