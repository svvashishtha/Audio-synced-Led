package test.so.audio_led;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;

import test.so.audio_led.extras.Constants;
import test.so.audio_led.fft.AnalyzerParameters;
import test.so.audio_led.fft.RecordingThread;
import test.so.audio_led.services.BluetoothService;
import test.so.audio_led.utils.WaveformView;

public class MainActivity extends AppCompatActivity implements AudioDataReceivedListener, Constants,
        GetPermissionDialogFragment.DialogInteractionListener {

    private static String mConnectedDeviceName;
    private static String mConnectedDeviceAddress;
    private final int REQUEST_CONNECT_DEVICE_INSECURE = 2;
    private final int REQUEST_CONNECT_DEVICE_SECURE = 1;
    private final int REQUEST_ENABLE_BT = 3;
    private final Handler mHandler = new CustomHandler(this);
    public Context context;
    public JSONArray soundValuesJson;
    public ImageView stopButton;
    public RecordingThread mRecordingThread;
    WaveformView waveformView;
    private AnalyzerParameters analyzerParam;
    private BluetoothAdapter mBluetoothAdapter;
    private ImageView recordButton;
    private BluetoothService mService;
    private String TAG = "MainActivity";
    private TextView status;

    public void setStatus(int resId) {
        status.setText(resId);
    }

    public void setStatus(String msg) {
        status.setText(msg);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context = this;
        recordButton = findViewById(R.id.record_button);
        stopButton = findViewById(R.id.stop_button);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        waveformView = findViewById(R.id.waveform_view);
        setUpRecorder();
        setUpCLickListeners();
    }

    private final void setUpConnectionHc05() {
        Intent intent = new Intent();
        intent.putExtra(DEVICE_NAME, "HC-05");
        intent.putExtra(DEVICE_ADDRESS, "98:D3:32:30:B8:F0");
        connectDevice(intent, true);
    }

    /**
     * Set up the UI and background operations for chat.
     */
    private void setupCommunication() {
        Log.d(TAG, "setupChat()");

        mService = new BluetoothService(context, mHandler);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), REQUEST_ENABLE_BT);
        } else if (this.mService == null) {
            setupCommunication();
            setUpConnectionHc05();
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.bluetooth_chat, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.secure_connect_scan: {
                // Launch the DeviceListActivity to see devices and do scan
                Intent serverIntent = new Intent(context, DeviceListActivity.class);
                startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_SECURE);
                return true;
            }
            case R.id.insecure_connect_scan: {
                // Launch the DeviceListActivity to see devices and do scan
                Intent serverIntent = new Intent(context, DeviceListActivity.class);
                startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_INSECURE);
                return true;
            }
            case R.id.discoverable: {
                // Ensure this device is discoverable by others
                ensureDiscoverable();
                return true;
            }
        }
        return false;
    }

    /**
     * Makes this device discoverable for 300 seconds (5 minutes).
     */
    private void ensureDiscoverable() {
        if (mBluetoothAdapter.getScanMode() !=
                BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(discoverableIntent);
        }
    }

    private void setUpRecorder() {
        loadPreferences();
        soundValuesJson = new JSONArray();
        mRecordingThread = new RecordingThread(this, this.analyzerParam);
    }


    private void setUpCLickListeners() {
        recordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mRecordingThread.recording()) {
                    startAudioRecordingSafe();
                    stopButton.setEnabled(true);
                    recordButton.setEnabled(false);
                }

            }
        });
        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mRecordingThread.recording()) {
                    mRecordingThread.stopRecording();
                    recordButton.setEnabled(true);
                    stopButton.setEnabled(false);
                }
            }
        });
    }

    private void startAudioRecordingSafe() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) +
                    ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                mRecordingThread.startRecording();
                return;
            }
            checkPermissions();
        } else {
            mRecordingThread.startRecording();
        }
    }

    private void checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.RECORD_AUDIO) + ContextCompat
                    .checkSelfPermission(this,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale
                        (this, Manifest.permission.RECORD_AUDIO)) {
                    GetPermissionDialogFragment getPermissionDialogFragment = GetPermissionDialogFragment.newInstance(Constants.PERMISSION_RECORD_AUDIO);
                    getPermissionDialogFragment.show(getFragmentManager(), "recordAudio");
                    return;
                } else if (ActivityCompat.shouldShowRequestPermissionRationale
                        (this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    GetPermissionDialogFragment getPermissionDialogFragment = GetPermissionDialogFragment.newInstance(PERMISSION_STORAGE);
                    getPermissionDialogFragment.show(getFragmentManager(), "storage");
                    return;
                }
                requestPermissions();
            }

    }

    private void requestPermissions() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                PERMISSIONS_REQUEST);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST: {
                for (int i = 0; i < permissions.length; i++) {
                    if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                        checkPermissions();
                        break;
                    }
                }
                break;
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

    }

    private void loadPreferences() {
        analyzerParam = new AnalyzerParameters(getResources());
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        if (sharedPref.getBoolean("keepScreenOn", true)) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
        analyzerParam.setAudioSourceId(Integer.parseInt(sharedPref.getString("audioSource", Integer.toString(analyzerParam.getRECORDER_AGC_OFF()))));
        analyzerParam.setWndFuncName(sharedPref.getString("windowFunction", "Hanning"));
        analyzerParam.setSpectrogramDuration(Double.parseDouble(sharedPref.getString("spectrogramDuration", Double.toString(6.0d))));
        analyzerParam.setOverlapPercent(Double.parseDouble(sharedPref.getString("fft_overlap_percent", "50.0")));
        analyzerParam.setHopLen((int) ((analyzerParam.getFftLen() * (1 - (analyzerParam.getOverlapPercent() / ((double) 100)))) + 0.5d));
    }

    @Override
    public void onAudioDataReceived(short[] sArr) {
        waveformView.setSamples(sArr);
    }

    @Override
    public void sampleValues(double maxAmpDB, double maxAmpFreq, double rms, double rmsFromFFt) {
        Log.d(TAG, "acknowledging values");
        JSONObject value = new JSONObject();
        try {
            value.put("amp", Math.abs(maxAmpDB));
            value.put("rms", rms);
            value.put("freq", maxAmpFreq);
            value.put("rmsFromFt", rmsFromFFt);
            soundValuesJson.put(value);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void exitApp() {
        finish();
    }

    /**
     * Establish connection with other device
     *
     * @param data   An {@link Intent} with {@link DeviceListActivity#EXTRA_DEVICE_ADDRESS} extra.
     * @param secure Socket Security type - Secure (true) , Insecure (false)
     */
    private void connectDevice(Intent data, boolean secure) {
        // Get the device MAC address
        String address = data.getExtras()
                .getString(DEVICE_ADDRESS);
        // Get the BluetoothDevice object
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        // Attempt to connect to the device
        mService.connect(device, secure);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CONNECT_DEVICE_SECURE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    connectDevice(data, true);
                }
                break;
            case REQUEST_CONNECT_DEVICE_INSECURE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    connectDevice(data, false);
                }
                break;
            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    // Bluetooth is now enabled, so set up a chat session
                    setupCommunication();
                    setUpConnectionHc05();
                } else {
                    // User did not enable Bluetooth or an error occurred
                    Log.d(TAG, "BT not enabled");
                    Toast.makeText(context, R.string.bt_not_enabled_leaving,
                            Toast.LENGTH_SHORT).show();

                }
        }
    }

    @Override
    public void requestPermissionAgain(String permissionType) {
        requestPermissions();
    }

    private static class CustomHandler extends Handler {
        private final WeakReference<MainActivity> mActivity;
        String TAG = "CustomHandler";

        private CustomHandler(MainActivity mActivity) {
            this.mActivity = new WeakReference<MainActivity>(mActivity);
        }

        @Override
        public void handleMessage(Message msg) {
            MainActivity mainActivity = mActivity.get();
            switch (msg.what) {
                case Constants.MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {

                        case BluetoothService.STATE_CONNECTED:
                            mainActivity.setStatus(mainActivity.getString(R.string.title_connected_to, mConnectedDeviceName));
                            break;
                        case BluetoothService.STATE_CONNECTING:
                            mainActivity.setStatus(R.string.title_connecting);
                            break;
                        case BluetoothService.STATE_LISTEN:
                        case BluetoothService.STATE_NONE:
                            mainActivity.setStatus(R.string.title_not_connected);
                            break;
                    }
                    break;
                case Constants.MESSAGE_WRITE:
                    byte[] writeBuf = (byte[]) msg.obj;
                    // construct a string from the buffer
                    String writeMessage = new String(writeBuf);
                    Log.d(TAG, "message sent: " + writeMessage);
                    break;
                case Constants.MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    // construct a string from the valid bytes in the buffer
                    String readMessage = new String(readBuf, 0, msg.arg1);
                    Log.d(TAG, "message read: " + readMessage);
                    break;
                case Constants.MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    mConnectedDeviceName = msg.getData().getString(Constants.DEVICE_NAME);
                    mConnectedDeviceAddress = msg.getData().getString(Constants.DEVICE_ADDRESS);

                    if (null != mainActivity) {
                        Toast.makeText(mainActivity, "Connected to "
                                + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                    }
                    break;
                case Constants.MESSAGE_TOAST:
                    if (null != mainActivity) {
                        Toast.makeText(mainActivity, msg.getData().getString(Constants.TOAST),
                                Toast.LENGTH_SHORT).show();
                    }
                    break;
            }
        }

    }
}
