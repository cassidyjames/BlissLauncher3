/*
 * Copyright © MURENA SAS 2023.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 */
package foundation.e.bliss;

import static android.content.pm.ActivityInfo.CONFIG_LOCALE;
import static android.content.pm.ActivityInfo.CONFIG_ORIENTATION;
import static android.content.pm.ActivityInfo.CONFIG_SCREEN_SIZE;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.LauncherApps;
import android.content.pm.ShortcutInfo;
import android.database.sqlite.SQLiteDatabase;
import android.os.UserHandle;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.launcher3.LauncherPrefs;
import com.android.launcher3.model.data.AppInfo;
import com.android.launcher3.InvariantDeviceProfile;
import com.android.launcher3.Launcher;
import com.android.launcher3.Utilities;
import com.android.launcher3.util.MainThreadInitializedObject;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class LauncherAppMonitor extends LauncherApps.Callback
        implements
            SharedPreferences.OnSharedPreferenceChangeListener,
            LauncherAppMonitorCallback {

    // We do not need any synchronization for this variable as its only written on
    // UI thread.
    public static final MainThreadInitializedObject<LauncherAppMonitor> INSTANCE = new MainThreadInitializedObject<>(
            LauncherAppMonitor::new);

    private final ArrayList<WeakReference<LauncherAppMonitorCallback>> mCallbacks = new ArrayList<>();

    private Launcher mLauncher;

    // private MultiModeController mMultiModeController = null;

    public static LauncherAppMonitor getInstance(final Context context) {
        return INSTANCE.get(context.getApplicationContext());
    }

    public static LauncherAppMonitor getInstanceNoCreate() {
        return INSTANCE.getNoCreate();
    }

    // return null while launcher activity isn't running
    public Launcher getLauncher() {
        return mLauncher;
    }

    /**
     * Remove the given observer's callback.
     *
     * @param callback
     *            The callback to remove
     */
    public void unregisterCallback(LauncherAppMonitorCallback callback) {
        for (int i = mCallbacks.size() - 1; i >= 0; i--) {
            if (mCallbacks.get(i).get() == callback) {
                synchronized (mCallbacks) {
                    mCallbacks.remove(i);
                }
            }
        }
    }

    /**
     * Register to receive notifications about general Launcher app information
     * 
     * @param callback
     *            The callback to register
     */
    public void registerCallback(LauncherAppMonitorCallback callback) {
        // Prevent adding duplicate callbacks
        unregisterCallback(callback);
        synchronized (mCallbacks) {
            mCallbacks.add(new WeakReference<>(callback));
        }
    }

    private LauncherAppMonitor(Context context) {
        LauncherPrefs.getPrefs(context).registerOnSharedPreferenceChangeListener(this);
        // mMultiModeController = new MultiModeController(context, this);
    }

    @Override
    public void onLauncherPreCreate(Launcher launcher) {
        if (mLauncher != null) {
            onLauncherDestroy(mLauncher);
        }
        mLauncher = launcher;

        for (int i = 0; i < mCallbacks.size(); i++) {
            LauncherAppMonitorCallback cb = mCallbacks.get(i).get();
            if (cb != null) {
                cb.onLauncherPreCreate(launcher);
            }
        }
    }

    @Override
    public void onLauncherCreated() {
        for (int i = 0; i < mCallbacks.size(); i++) {
            LauncherAppMonitorCallback cb = mCallbacks.get(i).get();
            if (cb != null) {
                cb.onLauncherCreated();
            }
        }
    }

    @Override
    public void onLauncherPreResume() {
        for (int i = 0; i < mCallbacks.size(); i++) {
            LauncherAppMonitorCallback cb = mCallbacks.get(i).get();
            if (cb != null) {
                cb.onLauncherPreResume();
            }
        }
    }

    @Override
    public void onLauncherResumed() {
        for (int i = 0; i < mCallbacks.size(); i++) {
            LauncherAppMonitorCallback cb = mCallbacks.get(i).get();
            if (cb != null) {
                cb.onLauncherResumed();
            }
        }
    }

    @Override
    public void onLauncherPrePause() {
        for (int i = 0; i < mCallbacks.size(); i++) {
            LauncherAppMonitorCallback cb = mCallbacks.get(i).get();
            if (cb != null) {
                cb.onLauncherPrePause();
            }
        }
    }

    @Override
    public void onLauncherPaused() {
        for (int i = 0; i < mCallbacks.size(); i++) {
            LauncherAppMonitorCallback cb = mCallbacks.get(i).get();
            if (cb != null) {
                cb.onLauncherPaused();
            }
        }
    }

    @Override
    public void onLauncherStart() {
        for (int i = 0; i < mCallbacks.size(); i++) {
            LauncherAppMonitorCallback cb = mCallbacks.get(i).get();
            if (cb != null) {
                cb.onLauncherStart();
            }
        }
    }

    @Override
    public void onLauncherStop() {
        for (int i = 0; i < mCallbacks.size(); i++) {
            LauncherAppMonitorCallback cb = mCallbacks.get(i).get();
            if (cb != null) {
                cb.onLauncherStop();
            }
        }
    }

    @Override
    public void onLauncherDestroy(@NonNull Launcher launcher) {
        if (launcher != mLauncher) {
            return;
        }

        for (int i = 0; i < mCallbacks.size(); i++) {
            LauncherAppMonitorCallback cb = mCallbacks.get(i).get();
            if (cb != null) {
                cb.onLauncherDestroy(launcher);
            }
        }
        mLauncher = null;
    }

    @Override
    public void onLauncherRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        for (int i = 0; i < mCallbacks.size(); i++) {
            LauncherAppMonitorCallback cb = mCallbacks.get(i).get();
            if (cb != null) {
                cb.onLauncherRequestPermissionsResult(requestCode, permissions, grantResults);
            }
        }
    }

    @Override
    public void onLauncherFocusChanged(boolean hasFocus) {
        for (int i = 0; i < mCallbacks.size(); i++) {
            LauncherAppMonitorCallback cb = mCallbacks.get(i).get();
            if (cb != null) {
                cb.onLauncherFocusChanged(hasFocus);
            }
        }
    }

    @Override
    public void onReceiveHomeIntent() {
        for (int i = 0; i < mCallbacks.size(); i++) {
            LauncherAppMonitorCallback cb = mCallbacks.get(i).get();
            if (cb != null) {
                cb.onReceiveHomeIntent();
            }
        }
    }

    @Override
    public void onLauncherWorkspaceBindingFinish() {
        for (int i = 0; i < mCallbacks.size(); i++) {
            LauncherAppMonitorCallback cb = mCallbacks.get(i).get();
            if (cb != null) {
                cb.onLauncherWorkspaceBindingFinish();
            }
        }
    }

    @Override
    public void onLauncherAllAppBindingFinish(@NonNull AppInfo[] apps) {
        for (int i = 0; i < mCallbacks.size(); i++) {
            LauncherAppMonitorCallback cb = mCallbacks.get(i).get();
            if (cb != null) {
                cb.onLauncherAllAppBindingFinish(apps);
            }
        }
    }

    @Override
    public void onPackageRemoved(String packageName, UserHandle user) {
        for (int i = 0; i < mCallbacks.size(); i++) {
            LauncherAppMonitorCallback cb = mCallbacks.get(i).get();
            if (cb != null) {
                cb.onPackageRemoved(packageName, user);
            }
        }
    }

    @Override
    public void onPackageAdded(String packageName, UserHandle user) {
        for (int i = 0; i < mCallbacks.size(); i++) {
            LauncherAppMonitorCallback cb = mCallbacks.get(i).get();
            if (cb != null) {
                cb.onPackageAdded(packageName, user);
            }
        }
    }

    @Override
    public void onPackageChanged(String packageName, UserHandle user) {
        for (int i = 0; i < mCallbacks.size(); i++) {
            LauncherAppMonitorCallback cb = mCallbacks.get(i).get();
            if (cb != null) {
                cb.onPackageChanged(packageName, user);
            }
        }
    }

    @Override
    public void onPackagesAvailable(String[] packageNames, UserHandle user, boolean replacing) {
        for (int i = 0; i < mCallbacks.size(); i++) {
            LauncherAppMonitorCallback cb = mCallbacks.get(i).get();
            if (cb != null) {
                cb.onPackagesAvailable(packageNames, user, replacing);
            }
        }
    }

    @Override
    public void onPackagesUnavailable(String[] packageNames, UserHandle user, boolean replacing) {
        for (int i = 0; i < mCallbacks.size(); i++) {
            LauncherAppMonitorCallback cb = mCallbacks.get(i).get();
            if (cb != null) {
                cb.onPackagesUnavailable(packageNames, user, replacing);
            }
        }
    }

    @Override
    public void onPackagesSuspended(String[] packageNames, UserHandle user) {
        for (int i = 0; i < mCallbacks.size(); i++) {
            LauncherAppMonitorCallback cb = mCallbacks.get(i).get();
            if (cb != null) {
                cb.onPackagesSuspended(packageNames, user);
            }
        }
    }

    @Override
    public void onPackagesUnsuspended(String[] packageNames, UserHandle user) {
        for (int i = 0; i < mCallbacks.size(); i++) {
            LauncherAppMonitorCallback cb = mCallbacks.get(i).get();
            if (cb != null) {
                cb.onPackagesUnsuspended(packageNames, user);
            }
        }
    }

    @Override
    public void onShortcutsChanged(String packageName, List<ShortcutInfo> shortcuts, UserHandle user) {
        for (int i = 0; i < mCallbacks.size(); i++) {
            LauncherAppMonitorCallback cb = mCallbacks.get(i).get();
            if (cb != null) {
                cb.onShortcutsChanged(packageName, shortcuts, user);
            }
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        for (int i = 0; i < mCallbacks.size(); i++) {
            LauncherAppMonitorCallback cb = mCallbacks.get(i).get();
            if (cb != null) {
                cb.onAppSharedPreferenceChanged(key);
            }
        }
    }

    public void onReceive(Intent intent) {
        for (int i = 0; i < mCallbacks.size(); i++) {
            LauncherAppMonitorCallback cb = mCallbacks.get(i).get();
            if (cb != null) {
                cb.onReceive(intent);
            }
        }
    }

    public void onAppCreated(Context context) {
        for (int i = 0; i < mCallbacks.size(); i++) {
            LauncherAppMonitorCallback cb = mCallbacks.get(i).get();
            if (cb != null) {
                cb.onAppCreated(context);
            }
        }
    }

    public void onUIConfigChanged() {
        for (int i = 0; i < mCallbacks.size(); i++) {
            LauncherAppMonitorCallback cb = mCallbacks.get(i).get();
            if (cb != null) {
                cb.onUIConfigChanged();
            }
        }
    }

    public void onThemeChanged() {
        for (int i = 0; i < mCallbacks.size(); i++) {
            LauncherAppMonitorCallback cb = mCallbacks.get(i).get();
            if (cb != null) {
                cb.onThemeChanged();
            }
        }
    }

    public void onAllAppsListUpdated(List<? extends AppInfo> apps) {
        for (int i = 0; i < mCallbacks.size(); i++) {
            LauncherAppMonitorCallback cb = mCallbacks.get(i).get();
            if (cb != null) {
                cb.onAllAppsListUpdated(apps);
            }
        }
    }

    public void onLauncherConfigurationChanged(int diff) {
        boolean localChanged = (diff & CONFIG_LOCALE) != 0;
        boolean orientationChanged = (diff & CONFIG_ORIENTATION) != 0;
        boolean screenSizeChanged = (diff & CONFIG_SCREEN_SIZE) != 0;

        for (int i = 0; i < mCallbacks.size(); i++) {
            LauncherAppMonitorCallback cb = mCallbacks.get(i).get();
            if (cb != null) {
                if (localChanged) {
                    cb.onLauncherLocaleChanged();
                }
                if (orientationChanged) {
                    cb.onLauncherOrientationChanged();
                }
                if (screenSizeChanged) {
                    cb.onLauncherScreensizeChanged();
                }
            }
        }
    }

    public void onLoadAllAppsEnd(ArrayList<AppInfo> apps) {
        for (int i = 0; i < mCallbacks.size(); i++) {
            LauncherAppMonitorCallback cb = mCallbacks.get(i).get();
            if (cb != null) {
                cb.onLoadAllAppsEnd(apps);
            }
        }
    }

    @Override
    public void onLauncherStyleChanged(@NonNull String style) {
        for (int i = 0; i < mCallbacks.size(); i++) {
            LauncherAppMonitorCallback cb = mCallbacks.get(i).get();
            if (cb != null) {
                cb.onLauncherStyleChanged(style);
            }
        }
    }

    @Override
    public void onLauncherDbUpgrade(@NonNull SQLiteDatabase db, int oldVersion, int newVersion) {
        for (int i = 0; i < mCallbacks.size(); i++) {
            LauncherAppMonitorCallback cb = mCallbacks.get(i).get();
            if (cb != null) {
                cb.onLauncherDbUpgrade(db, oldVersion, newVersion);
            }
        }
    }

    @Override
    public void dump(@NonNull String prefix, @NonNull FileDescriptor fd, @NonNull PrintWriter w, String[] args) {
        boolean isAll = args.length > 0 && TextUtils.equals(args[0], "--all");

        if (mLauncher != null) {
            w.println();
            w.println(prefix + " DeviceProfile: " + InvariantDeviceProfile.INSTANCE.get(mLauncher));
        }

        for (int i = 0; i < mCallbacks.size(); i++) {
            LauncherAppMonitorCallback cb = mCallbacks.get(i).get();
            if (cb != null) {
                cb.dump(prefix, fd, w, isAll);
            }
        }
    }

    @Override
    public void dump(@Nullable String prefix, @Nullable FileDescriptor fd, @Nullable PrintWriter w, boolean dumpAll) {
    }

    @Override
    public void onLauncherLocaleChanged() {
    }

    @Override
    public void onLauncherOrientationChanged() {
    }

    @Override
    public void onLauncherScreensizeChanged() {
    }

    @Override
    public void onAppSharedPreferenceChanged(@Nullable String key) {
    }
}
