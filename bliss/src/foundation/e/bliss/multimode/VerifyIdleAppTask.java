/*
 * Copyright © MURENA SAS 2023.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 */
package foundation.e.bliss.multimode;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.LauncherActivityInfo;
import android.content.pm.LauncherApps;
import android.os.UserHandle;
import android.util.Pair;

import androidx.annotation.WorkerThread;

import com.android.launcher3.LauncherAppState;
import com.android.launcher3.R;
import com.android.launcher3.model.BgDataModel;
import com.android.launcher3.model.ItemInstallQueue;
import com.android.launcher3.model.data.AppInfo;
import com.android.launcher3.model.data.ItemInfo;
import com.android.launcher3.util.ComponentKey;
import com.android.launcher3.util.ItemInfoMatcher;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

import foundation.e.bliss.utils.Logger;

@WorkerThread
public class VerifyIdleAppTask implements Runnable {
    private static final String TAG = "VerifyIdleAppTask";

    private final List<String> mBlacklistedApps;

    private final Context mContext;
    private final Collection<AppInfo> mApps;
    private final String mPackageNames;
    private final UserHandle mUser;
    private final boolean mIsAddPackage;
    private boolean mIgnoreLoaded;

    private BgDataModel mBgdataModel;

    public VerifyIdleAppTask(Context context, Collection<AppInfo> apps, String packageNames, UserHandle user,
            boolean isAdd, BgDataModel bgDataModel) {
        mContext = context;
        mApps = apps;
        mPackageNames = packageNames;
        mUser = user;
        mIsAddPackage = isAdd;
        mBgdataModel = bgDataModel;
        mBlacklistedApps = Arrays.asList(context.getResources().getStringArray(R.array.blacklisted_apps));
    }

    private static void verifyShortcutHighRes(Context context, AppInfo appInfo) {
        if (appInfo != null) {
            if (appInfo.usingLowResIcon()) {
                LauncherAppState.getInstance(context).getIconCache().getTitleAndIcon(appInfo, false);
            }
        }
    }

    @Override
    public void run() {
        if (!MultiModeController.isSingleLayerMode()) {
            return;
        }

        final Map<ComponentKey, Object> map = new HashMap<>();
        if (mApps != null && mApps.size() > 0) {
            // All apps loading, we ignore loaded.
            mIgnoreLoaded = true;
            for (AppInfo app : mApps) {
                if (mBlacklistedApps.stream().noneMatch(
                        pkg -> pkg.equals(Objects.requireNonNull(app.getTargetPackage()).trim().toLowerCase()))) {
                    map.put(new ComponentKey(app.componentName, app.user), app);
                }
            }
        } else if (mPackageNames != null && mUser != null) {
            // App add or update, should not ignore loaded.
            mIgnoreLoaded = false;
            final LauncherApps launcherApps = mContext.getSystemService(LauncherApps.class);
            if (mIsAddPackage || launcherApps.isPackageEnabled(mPackageNames, mUser)) {
                final List<LauncherActivityInfo> infos = launcherApps.getActivityList(mPackageNames, mUser);
                for (LauncherActivityInfo info : infos) {
                    map.put(new ComponentKey(info.getComponentName(), info.getUser()), info);
                }
            }
        }

        verifyAllApps(mContext, map, mIsAddPackage);
    }

    List<Pair<ItemInfo, Object>> verifyAllApps(Context context, Map<ComponentKey, Object> map, boolean animated) {
        List<Pair<ItemInfo, Object>> newItems = new ArrayList<>();
        synchronized (mBgdataModel) {
            for (Map.Entry<ComponentKey, Object> entry : map.entrySet()) {
                ComponentKey componentKey = entry.getKey();
                HashSet<ComponentName> components = new HashSet<>(1);
                components.add(componentKey.componentName);
                Predicate<ItemInfo> matcher = ItemInfoMatcher.ofComponents(components, componentKey.user);
                if (mBgdataModel.workspaceItems.stream().noneMatch(matcher)) {
                    Object obj = entry.getValue();
                    if (obj instanceof AppInfo) {
                        verifyShortcutHighRes(context, (AppInfo) obj);
                        newItems.add(Pair.create((AppInfo) obj, null));
                    } else if (obj instanceof LauncherActivityInfo) {
                        LauncherActivityInfo info = (LauncherActivityInfo) obj;
                        newItems.add(new ItemInstallQueue.PendingInstallShortcutInfo(
                                info.getApplicationInfo().packageName, info.getUser()).getItemInfo(context));
                    }
                    Logger.d(TAG, "will bind " + componentKey.componentName + " to workspace.");
                }
            }
        }

        LauncherAppState appState = LauncherAppState.getInstanceNoCreate();
        if (appState != null && newItems.size() > 0) {
            appState.getModel().addAndBindAddedWorkspaceItems(newItems, animated, mIgnoreLoaded);
        }
        return newItems;
    }
}