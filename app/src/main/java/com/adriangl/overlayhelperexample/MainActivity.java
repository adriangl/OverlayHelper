package com.adriangl.overlayhelperexample;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.adriangl.overlayhelper.OverlayHelper;

/**
 * Main {@link android.app.Activity} to test the library.
 */
public class MainActivity extends AppCompatActivity {

    @SuppressWarnings("FieldCanBeLocal")
    private Button requestDrawOverlaysPermissionsButton;
    private TextView drawOverlaysPermissionStatusTextView;
    private OverlayHelper overlayHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        requestDrawOverlaysPermissionsButton = findViewById(R.id.request_draw_overlays_button);
        drawOverlaysPermissionStatusTextView = findViewById(R.id.draw_overlays_status_value);

        requestDrawOverlaysPermissionsButton.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                overlayHelper.requestDrawOverlaysPermission(
                    MainActivity.this,
                    "Request draw overlays permission?",
                    "You have to enable the draw overlays permission for this app to work",
                    "Enable",
                    "Cancel");
            }
        });

        overlayHelper = new OverlayHelper(this.getApplicationContext(), new OverlayHelper.OverlayPermissionChangedListener() {
            @Override public void onOverlayPermissionCancelled() {
                Toast.makeText(MainActivity.this, "Draw overlay permissions request canceled", Toast.LENGTH_SHORT).show();
            }

            @Override public void onOverlayPermissionGranted() {
                Toast.makeText(MainActivity.this, "Draw overlay permissions request granted", Toast.LENGTH_SHORT).show();
            }

            @Override public void onOverlayPermissionDenied() {
                Toast.makeText(MainActivity.this, "Draw overlay permissions request denied", Toast.LENGTH_SHORT).show();
            }
        });

        overlayHelper.startWatching();

        updateOverlayStatusText();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        overlayHelper.onRequestDrawOverlaysPermissionResult(requestCode);
        updateOverlayStatusText();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        overlayHelper.stopWatching();
    }

    private void updateOverlayStatusText() {
        drawOverlaysPermissionStatusTextView.setText(overlayHelper.canDrawOverlays() ? "Enabled" : "Disabled");
    }
}
