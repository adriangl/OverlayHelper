package com.adriangl.overlayhelper;

import android.app.AppOpsManager;
import android.content.Context;
import android.os.Build;
import android.provider.Settings;
import androidx.annotation.RequiresApi;
import androidx.annotation.RequiresPermission;

/**
 * Interface used to implement custom overlay checking behaviour depending on device version.
 */
interface OverlayDelegate {
    /**
     * Starts watching overlay setting's changes.
     */
    void startWatching();

    /**
     * Stops watching overlay setting's changes.
     */
    void stopWatching();

    /**
     * Checks if overlays can be drawn in the app. You will have to manually check if the
     * user has permissions to do so.
     *
     * @return true if the user can draw overlays.
     */
    @RequiresPermission(android.Manifest.permission.SYSTEM_ALERT_WINDOW)
    boolean canDrawOverlays();

    /**
     * Implementation of {@link OverlayDelegate} for devices with API level < 23.
     */
    class LollipopOverlayDelegate implements OverlayDelegate {
        @Override
        public void startWatching() {
            // No-op
        }

        @Override
        public void stopWatching() {
            // No-op
        }

        @Override
        @RequiresPermission(android.Manifest.permission.SYSTEM_ALERT_WINDOW)
        public boolean canDrawOverlays() {
            return true;
        }
    }

    /**
     * Implementation of {@link OverlayDelegate} for devices with API level >= 23.
     */
    @RequiresApi(api = Build.VERSION_CODES.M)
    class MarshmallowOverlayDelegate implements OverlayDelegate {
        private final Context context;

        MarshmallowOverlayDelegate(Context context) {
            this.context = context;
        }

        @Override public void startWatching() {
            // No-op
        }

        @Override public void stopWatching() {
            // No-op
        }

        @Override
        @RequiresPermission(android.Manifest.permission.SYSTEM_ALERT_WINDOW)
        public boolean canDrawOverlays() {
            return Settings.canDrawOverlays(context);
        }
    }

    /**
     * Implementation of {@link OverlayDelegate} for devices with API level >= 26.
     * <p>
     * The implementation of {@link Settings#canDrawOverlays(Context)} in Android O is bugged, in the sense that, if we launch the settings
     * activity to enable "Draw over other apps" permission, after returning from said activity, the method
     * {@link Settings#canDrawOverlays(Context)} will not return a proper value after a few seconds. This can cause issues in most apps,
     * as they expect said value to be changed immediately after returning from it.
     * <p>
     * This implementation solves it by using the {@link AppOpsManager}: when the setting has been changed, toggle the value stored in the
     * delegate.
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    class OreoOverlayDelegate implements OverlayDelegate {
        private final Context context;
        private final AppOpsManager opsManager;
        private final AppOpsManager.OnOpChangedListener onOpChangedListener = new AppOpsManager.OnOpChangedListener() {
            @Override
            public void onOpChanged(String op, String packageName) {
                String myPackageName = context.getPackageName();
                if (myPackageName.equals(packageName) &&
                    AppOpsManager.OPSTR_SYSTEM_ALERT_WINDOW.equals(op)) {
                    canDrawOverlays = !canDrawOverlays;
                }
            }
        };

        private boolean canDrawOverlays = false;

        OreoOverlayDelegate(Context context) {
            this.context = context;
            this.opsManager = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
        }

        @Override public void startWatching() {
            this.canDrawOverlays = Settings.canDrawOverlays(context);

            if (opsManager != null) {
                opsManager.startWatchingMode(AppOpsManager.OPSTR_SYSTEM_ALERT_WINDOW,
                    null, onOpChangedListener);
            }
        }

        @Override public void stopWatching() {
            if (opsManager != null) {
                opsManager.stopWatchingMode(onOpChangedListener);
            }
        }

        @Override
        @RequiresPermission(android.Manifest.permission.SYSTEM_ALERT_WINDOW)
        public boolean canDrawOverlays() {
            return canDrawOverlays;
        }
    }

    /**
     * Implementation of {@link OverlayDelegate} for devices with API level >= 28.
     */
    @RequiresApi(api = Build.VERSION_CODES.P)
    class PieOverlayDelegate implements OverlayDelegate {
        private final Context context;

        PieOverlayDelegate(Context context) {
            this.context = context;
        }

        @Override public void startWatching() {
            // No-op
        }

        @Override public void stopWatching() {
            // No-op
        }

        @Override public boolean canDrawOverlays() {
            return Settings.canDrawOverlays(context);
        }
    }
}
