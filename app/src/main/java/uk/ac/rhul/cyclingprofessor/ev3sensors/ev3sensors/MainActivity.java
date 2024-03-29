package uk.ac.rhul.cyclingprofessor.ev3sensors.ev3sensors;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;

import org.opencv.android.CameraBridgeViewBase;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity implements ActivityCompat.OnRequestPermissionsResultCallback {
    private static final String TAG = "EV3Sensors::Activity";
    private static final int SHOWN_IPV4 = 1;
    private static final int PERMISSION_REQUEST_CODE = 0;

    private ActivityResultLauncher<Intent> someActivityResultLauncher = null;

    /**
     * Permissions that need to be explicitly requested from end user.
     */
    private static final String[] REQUIRED_SDK_PERMISSIONS = new String[]{
            Manifest.permission.CAMERA, Manifest.permission.INTERNET};

    // Layout Views
    private CameraBridgeViewBase mOpenCvCameraView;
    private EditText mSend;
    private final List<String> receivedItems = new ArrayList<>();

    private Server server;
    private ArrayAdapter<String> listAdapter;


    public MainActivity() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar); // Attaching the layout to the toolbar object
        setSupportActionBar(toolbar);

        Button mSendButton = findViewById(R.id.button_send);
        mSend = findViewById(R.id.send);
        ListView mReceived = findViewById(R.id.received);
        listAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                receivedItems);
        mReceived.setAdapter(listAdapter);
        mHandler.init();

        mOpenCvCameraView = findViewById(R.id.open_cv_activity_view);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCameraPermissionGranted();
        mOpenCvCameraView.setCameraIndex(CameraBridgeViewBase.CAMERA_ID_ANY);
        mOpenCvCameraView.enableView();

        checkPermissions();

        mSendButton.setOnClickListener(v -> {
            // Send a message using content of the edit text widget
            String message = mSend.getText().toString();
            sendMessage(message);
        });
        someActivityResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        // There are no request codes
                        Intent data = result.getData();
                        // TODO Make the activity return the clicked IP address.
                    }
                });

    }


    @Override
    public void onPause() {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
        server.stop();
    }

    @Override
    public void onResume() {
        super.onResume();
        System.loadLibrary("opencv_java4");
        server.start();
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Toolbar tb = findViewById(R.id.toolbar);
        tb.inflateMenu(R.menu.menu_connect);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.show_addresses) {// Launch the DeviceListActivity to see devices and do scan
            Intent serverIntent = new Intent(this, ConnectionListActivity.class);
            someActivityResultLauncher.launch(serverIntent);
            return true;
        } else if (itemId == R.id.about_app) {
            AlertDialog.Builder alert = new AlertDialog.Builder(this);
            alert.setTitle("EV3 Sensors App?").setMessage(getString(R.string.about_message)).show();
            return true;
        } else if (itemId == R.id.exit_app) {
            mOpenCvCameraView.disableView();
            finish();
            moveTaskToBack(true);
            return true;
        }
        return false;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if ((requestCode== SHOWN_IPV4) && (resultCode != Activity.RESULT_OK)) {
            showExitDialog(getString(R.string.no_network_available));
        }
    }

    private void showExitDialog(String text) {
        FragmentManager fm = getSupportFragmentManager();
        ExitWithMessageDialogFragment alertDialog = ExitWithMessageDialogFragment.newInstance(text);
        alertDialog.show(fm, "exit_fragment");

    }

    public void exitApplication() {
        //Just finish when the dialogs end.
        mOpenCvCameraView.disableView();
        finish();
    }

    /**
     * Checks the dynamically-controlled permissions and requests missing permissions from end user.
     * <p>
     * Modified from developer.here.com
     */
    private void checkPermissions() {
        final List<String> missingPermissions = new ArrayList<>();
        // check all required dynamic permissions
        for (final String permission : REQUIRED_SDK_PERMISSIONS) {
            final int result = ContextCompat.checkSelfPermission(this, permission);
            if (result != PackageManager.PERMISSION_GRANTED) {
                missingPermissions.add(permission);
            }
        }
        if (!missingPermissions.isEmpty()) {
            // request required (and missing) permissions
            final String[] permissions = missingPermissions
                    .toArray(new String[0]);
            requestPermissions(permissions, PERMISSION_REQUEST_CODE);
        } else {
            // We already have them all - whoopee
            final int[] grantResults = new int[REQUIRED_SDK_PERMISSIONS.length];
            Arrays.fill(grantResults, PackageManager.PERMISSION_GRANTED);
            onRequestPermissionsResult(PERMISSION_REQUEST_CODE, REQUIRED_SDK_PERMISSIONS, grantResults);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            for (int index = permissions.length - 1; index >= 0; --index) {
                if (grantResults[index] != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Required permission '" + permissions[index]
                            + "' not granted, exiting", Toast.LENGTH_LONG).show();
                    finish();
                    return;
                }
            }
            initialize();
        }
    }

    private void initialize() {
        mOpenCvCameraView.setCvCameraViewListener(new CV_Camera(this));
        mOpenCvCameraView.setCameraPermissionGranted();
        server = new Server(this, mHandler);
    }

    /**
     * Updates the status on the action bar.
     *
     * @param subTitle status
     */
    private void setStatus(CharSequence subTitle) {
        final ActionBar actionBar = getSupportActionBar();
        if (null == actionBar) {
            return;
        }
        actionBar.setSubtitle(subTitle);
    }

    /**
     * Thanks to Alex Lockwood - Android Design Patterns for the heads up about the huge memory leak by using an anonymous Handler inner class.
     * https://www.androiddesignpatterns.com/2013/01/inner-class-handler-memory-leak.html
     **/
    private static class ServerHandler extends Handler {
        private final WeakReference<MainActivity> mActivity;
        private String mConnMsg = null;

        ServerHandler(MainActivity activity) {
            super(Looper.getMainLooper());
            mActivity = new WeakReference<>(activity);
        }

        void init() {
            mConnMsg = mActivity.get().getString(R.string.title_not_connected);
        }

        @Override
        public void handleMessage(Message msg) {
            MainActivity activity = mActivity.get();
            if (activity == null) {
                // We cannot handle events if there is no-one who cares about them.
                return;
            }
            switch (msg.what) {
                case Constants.MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case Server.STATE_CONNECTED:
                            activity.setStatus(mConnMsg);
                            break;
                        case Server.STATE_LISTEN:
                        case Server.STATE_NONE:
                            activity.setStatus(activity.getString(R.string.title_not_connected));
                            break;
                    }
                    break;
                case Constants.MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    // construct a string from the valid bytes in the buffer
                    String readMessage;
                    try {
                        readMessage = new String(readBuf, 0, msg.arg1);
                    } catch (StringIndexOutOfBoundsException e) {
                        Log.e(TAG, "could not read string, is the program running?", e);
                        activity.setStatus("No EV3 Connected");
                        System.exit(-1);
                        break;
                    }

                    activity.receivedItems.add(0, readMessage);
                    activity.listAdapter.notifyDataSetChanged();
                    break;
                case Constants.MESSAGE_DEVICE_NAME:
                    String device = msg.getData().getString(Constants.DEVICE_NAME);
                    mConnMsg = activity.getString(R.string.title_connected_to, device);
                    Toast.makeText(activity, mConnMsg, Toast.LENGTH_SHORT).show();
                    break;
                case Constants.MESSAGE_TOAST:
                    Toast.makeText(activity, msg.getData().getString(Constants.TOAST), Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    }

    /**
     * The Handler that gets information back from the Server object
     */
    private final ServerHandler mHandler = new ServerHandler(this);



    /**
     * Sends a message.
     *
     * @param message A string of text to send.
     */
    void sendMessage(String message) {
        // Check that we're actually connected before trying anything
        if (server.getState() != Server.STATE_CONNECTED) {
            // Silently ignore messages.
            return;
        }

        // Check that there's actually something to send
        if (message.length() > 0) {
            // Get the message bytes and tell the BluetoothChatService to write
            byte[] send = message.getBytes();
            server.write(send);
        }
    }


}