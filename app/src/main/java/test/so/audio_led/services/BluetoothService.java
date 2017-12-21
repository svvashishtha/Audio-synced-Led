package test.so.audio_led.services;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;
import test.so.audio_led.extras.Constants;

public class BluetoothService {
    private static final UUID MY_UUID_INSECURE = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static final UUID MY_UUID_SECURE = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static final String NAME_INSECURE = "HC-05";
    private static final String NAME_SECURE = "HC-05";
    public static final int STATE_CONNECTED = 3;
    public static final int STATE_CONNECTING = 2;
    public static final int STATE_LISTEN = 1;
    public static final int STATE_NONE = 0;
    private static final String TAG = "BluetoothService";
    private final BluetoothAdapter mAdapter = BluetoothAdapter.getDefaultAdapter();
    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;
    private final Handler mHandler;
    private AcceptThread mInsecureAcceptThread;
    private int mNewState = this.mState;
    private AcceptThread mSecureAcceptThread;
    private int mState = 0;

    private class AcceptThread extends Thread {
        private String mSocketType;
        private final BluetoothServerSocket mmServerSocket;

        public AcceptThread(boolean secure) throws IOException {
            BluetoothServerSocket tmp = null;
            this.mSocketType = secure ? "Secure" : "Insecure";
            if (secure) {
                try {
                    tmp = BluetoothService.this.mAdapter.listenUsingRfcommWithServiceRecord("HC-05", BluetoothService.MY_UUID_SECURE);
                } catch (IOException e) {
                    Log.e(BluetoothService.TAG, "Socket Type: " + this.mSocketType + "listen() failed", e);
                }
            } else {
                tmp = BluetoothService.this.mAdapter.listenUsingInsecureRfcommWithServiceRecord("HC-05", BluetoothService.MY_UUID_INSECURE);
            }
            this.mmServerSocket = tmp;
            BluetoothService.this.mState = 1;
        }

        public void run() {
            Log.d(BluetoothService.TAG, "Socket Type: " + this.mSocketType + "BEGIN mAcceptThread" + this);
            setName("AcceptThread" + this.mSocketType);
            while (BluetoothService.this.mState != 3) {
                try {
                    BluetoothSocket socket = this.mmServerSocket.accept();
                    if (socket != null) {
                        synchronized (BluetoothService.this) {
                            switch (BluetoothService.this.mState) {
                                case 0:
                                case 3:
                                    try {
                                        socket.close();
                                        break;
                                    } catch (IOException e) {
                                        Log.e(BluetoothService.TAG, "Could not close unwanted socket", e);
                                        break;
                                    }
                                case 1:
                                case 2:
                                    BluetoothService.this.connected(socket, socket.getRemoteDevice(), this.mSocketType);
                                    break;
                            }
                        }
                    }
                } catch (IOException e2) {
                    Log.e(BluetoothService.TAG, "Socket Type: " + this.mSocketType + "accept() failed", e2);
                }
            }
            Log.i(BluetoothService.TAG, "END mAcceptThread, socket Type: " + this.mSocketType);
            return;
        }

        public void cancel() {
            Log.d(BluetoothService.TAG, "Socket Type" + this.mSocketType + "cancel " + this);
            try {
                this.mmServerSocket.close();
            } catch (IOException e) {
                Log.e(BluetoothService.TAG, "Socket Type" + this.mSocketType + "close() of server failed", e);
            }
        }
    }

    private class ConnectThread extends Thread {
        private String mSocketType;
        private final BluetoothDevice mmDevice;
        private final BluetoothSocket mmSocket;

        public ConnectThread(BluetoothDevice device, boolean secure) {
            this.mmDevice = device;
            BluetoothSocket tmp = null;
            this.mSocketType = secure ? "Secure" : "Insecure";
            if (secure) {
                try {
                    tmp = device.createRfcommSocketToServiceRecord(BluetoothService.MY_UUID_SECURE);
                } catch (IOException e) {
                    Log.e(BluetoothService.TAG, "Socket Type: " + this.mSocketType + "create() failed", e);
                }
            } else {
                try {
                    tmp = device.createInsecureRfcommSocketToServiceRecord(BluetoothService.MY_UUID_INSECURE);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            this.mmSocket = tmp;
            BluetoothService.this.mState = 2;
        }

        public void run() {
            Log.i(BluetoothService.TAG, "BEGIN mConnectThread SocketType:" + this.mSocketType);
            setName("ConnectThread" + this.mSocketType);
            BluetoothService.this.mAdapter.cancelDiscovery();
            try {
                this.mmSocket.connect();
                synchronized (BluetoothService.this) {
                    BluetoothService.this.mConnectThread = null;
                }
                BluetoothService.this.connected(this.mmSocket, this.mmDevice, this.mSocketType);
            } catch (IOException e) {
                try {
                    this.mmSocket.close();
                } catch (IOException e2) {
                    Log.e(BluetoothService.TAG, "unable to close() " + this.mSocketType + " socket during connection failure", e2);
                }
                BluetoothService.this.connectionFailed();
            }
        }

        public void cancel() {
            try {
                this.mmSocket.close();
            } catch (IOException e) {
                Log.e(BluetoothService.TAG, "close() of connect " + this.mSocketType + " socket failed", e);
            }
        }
    }

    private class ConnectedThread extends Thread {
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        private final BluetoothSocket mmSocket;

        public ConnectedThread(BluetoothSocket socket, String socketType) {
            Log.d(BluetoothService.TAG, "create ConnectedThread: " + socketType);
            this.mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(BluetoothService.TAG, "temp sockets not created", e);
            }
            this.mmInStream = tmpIn;
            this.mmOutStream = tmpOut;
            BluetoothService.this.mState = 3;
        }

        public void run() {
            Log.i(BluetoothService.TAG, "BEGIN mConnectedThread");
            byte[] buffer = new byte[1024];
            while (BluetoothService.this.mState == 3) {
                try {
                    BluetoothService.this.mHandler.obtainMessage(2, this.mmInStream.read(buffer), -1, buffer).sendToTarget();
                } catch (IOException e) {
                    Log.e(BluetoothService.TAG, "disconnected", e);
                    BluetoothService.this.connectionLost();
                    return;
                }
            }
        }

        public void write(byte[] buffer) {
            try {
                this.mmOutStream.write(buffer);
                BluetoothService.this.mHandler.obtainMessage(3, -1, -1, buffer).sendToTarget();
            } catch (IOException e) {
                Log.e(BluetoothService.TAG, "Exception during write", e);
            }
        }

        public void cancel() {
            try {
                this.mmSocket.close();
            } catch (IOException e) {
                Log.e(BluetoothService.TAG, "close() of connect socket failed", e);
            }
        }
    }

    public BluetoothService(Context context, Handler handler) {
        this.mHandler = handler;
    }

    private synchronized void updateUserInterfaceTitle() {
        this.mState = getState();
        Log.d(TAG, "updateUserInterfaceTitle() " + this.mNewState + " -> " + this.mState);
        this.mNewState = this.mState;
        this.mHandler.obtainMessage(1, this.mNewState, -1).sendToTarget();
    }

    public synchronized int getState() {
        return this.mState;
    }

    public synchronized void start() {
        Log.d(TAG, "start");
        if (this.mConnectThread != null) {
            this.mConnectThread.cancel();
            this.mConnectThread = null;
        }
        if (this.mConnectedThread != null) {
            this.mConnectedThread.cancel();
            this.mConnectedThread = null;
        }
        if (this.mSecureAcceptThread == null) {
            try {
                this.mSecureAcceptThread = new AcceptThread(true);
            } catch (IOException e) {
                e.printStackTrace();
            }
            this.mSecureAcceptThread.start();
        }
        if (this.mInsecureAcceptThread == null) {
            try {
                this.mInsecureAcceptThread = new AcceptThread(false);
            } catch (IOException e) {
                e.printStackTrace();
            }
            this.mInsecureAcceptThread.start();
        }
        updateUserInterfaceTitle();
    }

    public synchronized void connect(BluetoothDevice device, boolean secure) {
        Log.d(TAG, "connect to: " + device);
        if (this.mState == 2 && this.mConnectThread != null) {
            this.mConnectThread.cancel();
            this.mConnectThread = null;
        }
        if (this.mConnectedThread != null) {
            this.mConnectedThread.cancel();
            this.mConnectedThread = null;
        }
        this.mConnectThread = new ConnectThread(device, secure);
        this.mConnectThread.start();
        updateUserInterfaceTitle();
    }

    public synchronized void connected(BluetoothSocket socket, BluetoothDevice device, String socketType) {
        Log.d(TAG, "connected, Socket Type:" + socketType);
        if (this.mConnectThread != null) {
            this.mConnectThread.cancel();
            this.mConnectThread = null;
        }
        if (this.mConnectedThread != null) {
            this.mConnectedThread.cancel();
            this.mConnectedThread = null;
        }
        if (this.mSecureAcceptThread != null) {
            this.mSecureAcceptThread.cancel();
            this.mSecureAcceptThread = null;
        }
        if (this.mInsecureAcceptThread != null) {
            this.mInsecureAcceptThread.cancel();
            this.mInsecureAcceptThread = null;
        }
        this.mConnectedThread = new ConnectedThread(socket, socketType);
        this.mConnectedThread.start();
        Message msg = this.mHandler.obtainMessage(4);
        Bundle bundle = new Bundle();
        bundle.putString(Constants.DEVICE_NAME, device.getName());
        bundle.putString(Constants.DEVICE_ADDRESS, device.getAddress());
        msg.setData(bundle);
        this.mHandler.sendMessage(msg);
        updateUserInterfaceTitle();
    }

    public synchronized void stop() {
        Log.d(TAG, "stop");
        if (this.mConnectThread != null) {
            this.mConnectThread.cancel();
            this.mConnectThread = null;
        }
        if (this.mConnectedThread != null) {
            this.mConnectedThread.cancel();
            this.mConnectedThread = null;
        }
        if (this.mSecureAcceptThread != null) {
            this.mSecureAcceptThread.cancel();
            this.mSecureAcceptThread = null;
        }
        if (this.mInsecureAcceptThread != null) {
            this.mInsecureAcceptThread.cancel();
            this.mInsecureAcceptThread = null;
        }
        this.mState = 0;
        updateUserInterfaceTitle();
    }

    public void write(byte[] out) {
        synchronized (this) {
            if (this.mState != 3) {
                return;
            }
            ConnectedThread r = this.mConnectedThread;
            r.write(out);
        }
    }

    private void connectionFailed() {
        Message msg = this.mHandler.obtainMessage(5);
        Bundle bundle = new Bundle();
        bundle.putString(Constants.TOAST, "Unable to connect device");
        msg.setData(bundle);
        this.mHandler.sendMessage(msg);
        this.mState = 0;
        updateUserInterfaceTitle();
        start();
    }

    private void connectionLost() {
        Message msg = this.mHandler.obtainMessage(5);
        Bundle bundle = new Bundle();
        bundle.putString(Constants.TOAST, "Device connection was lost");
        msg.setData(bundle);
        this.mHandler.sendMessage(msg);
        this.mState = 0;
        updateUserInterfaceTitle();
        start();
    }
}
