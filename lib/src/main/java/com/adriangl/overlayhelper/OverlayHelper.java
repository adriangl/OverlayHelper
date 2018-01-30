package com.adriangl.overlayhelper;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresPermission;
import android.support.annotation.StringRes;

import java.util.Arrays;
import java.util.List;

/**
 * Helper class that takes care of querying and requesting permissions for drawing over other apps.
 * <p>
 * Its use is as follows:
 * 1- Create a new {@link OverlayHelper} with a {@link OverlayPermissionChangedListener}.
 * 2- Start watching settings changes with {@link OverlayHelper#startWatching()}, for example in {@link Activity#onCreate}.
 * Stop watching changes with {@link OverlayHelper#stopWatching()} in {@link Activity#onDestroy}.
 * 3- Call {@link OverlayHelper#onRequestDrawOverlaysPermissionResult(int)} in the activity's
 * {@link Activity#onActivityResult(int, int, Intent)}, so the helper can retrieve the needed data.
 * 4- Call {@link OverlayHelper#requestDrawOverlaysPermission} wherever you want in your activity.
 * 5- You'll receive your permission results in the {@link OverlayPermissionChangedListener} that you registered when creating the helper.
 */
public class OverlayHelper {
    private static final int DRAW_OVERLAYS_REQUEST_CODE = 9876;

    @Nullable
    private final OverlayPermissionChangedListener overlayPermissionChangedListener;
    @NonNull
    private final Context context;
    @NonNull
    private final OverlayDelegate overlayDelegate;

    private boolean isWatching = false;

    /**
     * Creates a new {@link OverlayHelper}.
     *
     * @param ctx      A {@link Context} reference that will be kept during the helper's life.
     * @param listener Listener used to notify about permission granting or revoking. Only needed when requesting the SYSTEM_ALERT_WINDOW
     *                 permission.
     */
    public OverlayHelper(@NonNull Context ctx, @Nullable OverlayPermissionChangedListener listener) {
        this.context = ctx.getApplicationContext();
        this.overlayPermissionChangedListener = listener;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // API 26+
            this.overlayDelegate = new OverlayDelegate.OreoOverlayDelegate(context);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // API 23+: check if the user has explicitly enabled overlays
            this.overlayDelegate = new OverlayDelegate.MarshmallowOverlayDelegate(context);
        } else {
            // API < 23: always true if permission SYSTEM_ALERT_WINDOW is enabled
            this.overlayDelegate = new OverlayDelegate.LollipopOverlayDelegate();
        }
    }

    /**
     * Starts watching overlay settings changes.
     */
    public void startWatching() {
        if (!isWatching) {
            overlayDelegate.startWatching();
            isWatching = true;
        }
    }

    /**
     * Stops watching overlay settings changes.
     */
    public void stopWatching() {
        if (isWatching) {
            overlayDelegate.stopWatching();
            isWatching = false;
        }
    }

    /**
     * Checks if the current app can draw over other apps.
     *
     * @return True if the app can draw over other apps, false otherwise.
     */
    @RequiresPermission(Manifest.permission.SYSTEM_ALERT_WINDOW)
    public boolean canDrawOverlays() {
        checkSystemAlertWindowPermission(context);
        checkArgument(isWatching, "You must call startWatching() before performing this operation");

        return overlayDelegate.canDrawOverlays();
    }

    /**
     * Requests the needed permission to draw over other apps. It will publish the result of this query
     * in {@link OverlayPermissionChangedListener#onOverlayPermissionGranted()},
     * {@link OverlayPermissionChangedListener#onOverlayPermissionDenied()}
     * or {@link OverlayPermissionChangedListener#onOverlayPermissionCancelled()}
     *
     * @param activity           The {@link Activity} to use to display the dialog to request permissions.
     * @param dialogTitle        Request permission dialog title.
     * @param dialogMessage      Request permission dialog message.
     * @param positiveButtonText Request permission dialog positive button text.
     * @param negativeButtonText Request permission dialog negative button text.
     */
    @RequiresPermission(Manifest.permission.SYSTEM_ALERT_WINDOW)
    public void requestDrawOverlaysPermission(@NonNull final Activity activity,
                                              @Nullable String dialogTitle,
                                              @Nullable String dialogMessage,
                                              @Nullable String positiveButtonText,
                                              @Nullable String negativeButtonText) {
        checkSystemAlertWindowPermission(context);
        checkArgument(isWatching, "You must call startWatching() before performing this operation");

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder
            .setTitle(dialogTitle)
            .setMessage(dialogMessage)
            .setPositiveButton(positiveButtonText, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    openDrawOverlaysActivityForResult(activity);
                }
            })
            .setNegativeButton(negativeButtonText, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (overlayPermissionChangedListener != null) {
                        overlayPermissionChangedListener.onOverlayPermissionCancelled();
                    }
                    dialog.dismiss();
                }
            })
            .setCancelable(false)
            .show();
    }

    /**
     * Requests the needed permission to draw over other apps. It will publish the result of this query
     * in {@link OverlayPermissionChangedListener#onOverlayPermissionGranted()},
     * {@link OverlayPermissionChangedListener#onOverlayPermissionDenied()}
     * or {@link OverlayPermissionChangedListener#onOverlayPermissionCancelled()}
     *
     * @param activity             The {@link Activity} to use to display the dialog to request permissions.
     * @param dialogTitleId        Request permission dialog title ID.
     * @param dialogMessageId      Request permission dialog message ID.
     * @param positiveButtonTextId Request permission dialog positive button text ID.
     * @param negativeButtonTextId Request permission dialog negative button text ID.
     */
    @RequiresPermission(Manifest.permission.SYSTEM_ALERT_WINDOW)
    public void requestDrawOverlaysPermission(final Activity activity,
                                              @StringRes int dialogTitleId,
                                              @StringRes int dialogMessageId,
                                              @StringRes int positiveButtonTextId,
                                              @StringRes int negativeButtonTextId) {
        requestDrawOverlaysPermission(
            activity,
            activity.getString(dialogTitleId),
            activity.getString(dialogMessageId),
            activity.getString(positiveButtonTextId),
            activity.getString(negativeButtonTextId));
    }

    /**
     * Checks the activity result to see if the "draw over other apps" permission has been granted
     * or not.
     * It will publish its result in {@link OverlayPermissionChangedListener#onOverlayPermissionGranted()}
     * or {@link OverlayPermissionChangedListener#onOverlayPermissionDenied()}
     * <p>
     * It must be called in {@link Activity#onActivityResult(int, int, Intent)} to work.
     *
     * @param requestCode The requestCode received in {@link Activity#onActivityResult(int, int, Intent)}
     */
    public void onRequestDrawOverlaysPermissionResult(int requestCode) {
        checkSystemAlertWindowPermission(context);
        checkArgument(isWatching, "You must call startWatching() before performing this operation");

        if (overlayPermissionChangedListener != null) {
            if (requestCode == DRAW_OVERLAYS_REQUEST_CODE) {
                if (canDrawOverlays()) {
                    overlayPermissionChangedListener.onOverlayPermissionGranted();
                } else {
                    overlayPermissionChangedListener.onOverlayPermissionDenied();
                }
            }
        }
    }

    private void openDrawOverlaysActivityForResult(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // For API 23+: launch permission activity
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + activity.getPackageName()));
            activity.startActivityForResult(intent, DRAW_OVERLAYS_REQUEST_CODE);
        } else {
            // For API < 23: always return "permission granted"
            if (overlayPermissionChangedListener != null) {
                overlayPermissionChangedListener.onOverlayPermissionGranted();
            }
        }
    }

    private void checkSystemAlertWindowPermission(Context context) {
        PackageManager pm = context.getPackageManager();
        boolean result = false;
        try {
            PackageInfo packageInfo = pm.getPackageInfo(
                context.getPackageName(),
                PackageManager.GET_PERMISSIONS);

            String[] requestedPermissions = null;
            if (packageInfo != null) {
                requestedPermissions = packageInfo.requestedPermissions;
            }

            if (requestedPermissions != null) {
                if (requestedPermissions.length > 0) {
                    List<String> requestedPermissionsList = Arrays.asList(requestedPermissions);
                    result = requestedPermissionsList.contains("android.permission.SYSTEM_ALERT_WINDOW");
                }
            }
        } catch (PackageManager.NameNotFoundException ignored) {
            // No-op
        }

        if (!result) {
            throw new SecurityException("You must add \"android.permission.SYSTEM_ALERT_WINDOW\" " +
                "permission to your Manifest file to use overlays");
        }
    }

    /**
     * Ensures that an expression checking an argument is true.
     *
     * @param expression   the expression to check
     * @param errorMessage the exception message to use if the check fails; will
     *                     be converted to a string using {@link String#valueOf(Object)}
     * @throws IllegalArgumentException if {@code expression} is false
     */
    private void checkArgument(boolean expression, @Nullable final String errorMessage) {
        if (!expression) {
            throw new IllegalArgumentException(errorMessage != null ? errorMessage : "");
        }
    }

    /**
     * Listener used to notify permission changes in {@link OverlayHelper}.
     */
    public interface OverlayPermissionChangedListener {
        /**
         * Triggered when the user cancels requesting the permission.
         */
        void onOverlayPermissionCancelled();

        /**
         * Triggered when the user grants the permission.
         */
        void onOverlayPermissionGranted();

        /**
         * Triggered when the user denies the permission.
         */
        void onOverlayPermissionDenied();
    }
}
