/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.bluetoothchat;

import android.app.ActionBar;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.android.common.logger.Log;

//
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Environment;
//import android.util.Log;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
//

/**
 * This fragment controls Bluetooth to communicate with other devices.
 */
public class BluetoothChatFragment extends Fragment {

    private static final String TAG = "BluetoothChatFragment";

    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
    private static final int REQUEST_CONNECT_DEVICE_INSECURE = 2;
    private static final int REQUEST_ENABLE_BT = 3;

    // Layout Views
    private ListView mConversationView;
    private EditText mOutEditText;
    private Button mSendButton;
    private Button btnStreamStart;
    private Button btnStreamStop;
    private Button btnLstStart;
    private Button btnLstStop;
    private Button btnPlayFile;
    private CheckBox cbWriteToFile;


    /**
     * Name of the connected device
     */
    private String mConnectedDeviceName = null;

    /**
     * Array adapter for the conversation thread
     */
    private ArrayAdapter<String> mConversationArrayAdapter;

    /**
     * String buffer for outgoing messages
     */
    private StringBuffer mOutStringBuffer;

    /**
     * Local Bluetooth adapter
     */
    private BluetoothAdapter mBluetoothAdapter = null;

    /**
     * Member object for the chat services
     */
    private BluetoothChatService mChatService = null;

    /**
     * START RecordAudio
     */
    private static final int RECORDER_SAMPLERATE = 44100;
    private static final int RECORDER_BPP = 16;
    private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_STEREO;//CHANNEL_IN_MONO;
    private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    private AudioRecord recorder = null;
    private Thread recordingThread = null;
    private boolean isStreaming = false;
    private boolean isListening = false;
    private int bufferSize = 0;
    private AudioTrack at;
    /**
     * END RecordAudio
     */

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        // Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            FragmentActivity activity = getActivity();
            Toast.makeText(activity, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            activity.finish();
        }


        //* START RecordAudio
        //
        //setButtonHandlers(getView());
        //enableButtons(false, getView());

        bufferSize = AudioRecord.getMinBufferSize(RECORDER_SAMPLERATE,
                RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING);
        at = new AudioTrack(AudioManager.STREAM_MUSIC, 44100, AudioFormat.CHANNEL_IN_STEREO,
                AudioFormat.ENCODING_PCM_16BIT, bufferSize, AudioTrack.MODE_STREAM);
        at.play();
        //* END RecordAudio


    }

    private void setButtonHandlers(View view) {
        ((Button) view.findViewById(R.id.btnStart)).setOnClickListener(btnClick);
        ((Button) view.findViewById(R.id.btnStop)).setOnClickListener(btnClick);
        ((Button) view.findViewById(R.id.btnLstStart)).setOnClickListener(btnClick);
        ((Button) view.findViewById(R.id.btnLstStop)).setOnClickListener(btnClick);
        ((Button) view.findViewById(R.id.btnPlayFile)).setOnClickListener(btnClick);
    }

    private void enableButton(int id, boolean isEnable, View view) {
        ((Button) view.findViewById(id)).setEnabled(isEnable);
    }

    private void enableButtons(boolean isStreaming, boolean isListening, View view) {
        //enableButton(R.id.btnStart, !isStreaming, view);
        //enableButton(R.id.btnStop, isStreaming, view);
        btnStreamStart.setEnabled(!isStreaming);
        btnStreamStop.setEnabled(isStreaming);
        btnLstStart.setEnabled(!isListening);
        btnLstStop.setEnabled(isListening);
    }

    private View.OnClickListener btnClick = new View.OnClickListener() {
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.btnStart: {
                    startStreaming();
                    enableButtons(isStreaming, isListening, v);
                    break;
                }
                case R.id.btnStop: {
                    stopStreaming();
                    enableButtons(isStreaming, isListening, v);
                    break;
                }
                case R.id.btnLstStart: {
                    //enableButtons(false, v);
                    isListening = true;
                    enableButtons(isStreaming, isListening, v);
                    //cbWriteToFile.setEnabled(isListening);
                    break;
                }
                case R.id.btnLstStop: {
                    //enableButtons(false, v);
                    isListening = false;
                    enableButtons(isStreaming, isListening, v);
                    //cbWriteToFile.setEnabled(isListening);
                    break;
                }
                case R.id.btnPlayFile: {
                    String filePathWave =  Environment.getExternalStorageDirectory()+"/Download/voice8K16bitmono.raw";
                    try {
                        PlayAudioFileViaAudioTrack(filePathWave, bufferSize);
                    } catch (IOException e) {
                        e.printStackTrace();
                        Toast.makeText(getActivity(), e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                    //enableButtons(false);
                    //stopRecording();
                    break;
                }
                /*case R.id.btnPlayBuffer: {
                    String filePathWave =  Environment.getExternalStorageDirectory()+"/Download/voice8K16bitmono.wav";
                    try {
                        PlayAudioFileViaAudioTrack(filePathWave, bufferSize);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    //enableButtons(false);
                    //stopRecording();
                    break;
                }*/
            }
        }
    };

    private void startStreaming() {

        recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                RECORDER_SAMPLERATE, RECORDER_CHANNELS,
                RECORDER_AUDIO_ENCODING, bufferSize);//BufferElements2Rec * BytesPerElement);

        recorder.startRecording();
        isStreaming = true;

        recordingThread = new Thread(new Runnable() {
            public void run() {
                streamAudioDataViaBluetooth();
                //writeAudioDataToWaveFile();
            }
        }, "AudioRecorder Thread");
        recordingThread.start();
    }


    private void stopStreaming() {
        // stops the recording activity
        if (null != recorder) {
            isStreaming = false;
            recorder.stop();
            recorder.release();
            recorder = null;
            recordingThread = null;
        }
    }

    private void streamAudioDataViaBluetooth() {
        // Write the output audio in byte

        /*String filePath =  Environment.getExternalStorageDirectory()+"/Download/voice8K16bitmono.raw";
        String filePathWave =  Environment.getExternalStorageDirectory()+"/Download/voice8K16bitmono.wav";*/
        //short sData[] = new short[BufferElements2Rec];

        byte bdata[] = new byte[bufferSize];

        /*FileOutputStream os = null;
        try {
            os = new FileOutputStream(filePath);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }*/
        int read = 0;
        while (isStreaming) {
            // gets the voice output from microphone to byte format
            read = recorder.read(bdata, 0, bufferSize);
            //sendBytes(bdata);
            if (bdata.length > 0) {
                // Get the message bytes and tell the BluetoothChatService to write
                //byte[] send = message.getBytes();
                mChatService.write(bdata);
            }
            //if(AudioRecord.ERROR_INVALID_OPERATION != read){
            /*try {
                os.write(bdata);
            } catch (IOException e) {
                e.printStackTrace();
            }*/
            //}
            /*recorder.read(sData, 0, BufferElements2Rec);
            //System.out.println("Short wirting to file" + sData.toString());
            try {
                // // writes the data to file from buffer
                // // stores the voice buffer
                byte bData[] = short2byte(sData);
                os.write(bData, 0, BufferElements2Rec * BytesPerElement);
            } catch (IOException e) {
                e.printStackTrace();
            }*/

        }
        /*try {
            os.close();
            copyWaveFile(filePath, filePathWave);
        } catch (IOException e) {
            e.printStackTrace();
        }*/
    }


    @Override
    public void onStart() {
        super.onStart();
        // If BT is not on, request that it be enabled.
        // setupChat() will then be called during onActivityResult
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
            // Otherwise, setup the chat session
        } else if (mChatService == null) {
            setupChat();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mChatService != null) {
            mChatService.stop();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        // Performing this check in onResume() covers the case in which BT was
        // not enabled during onStart(), so we were paused to enable it...
        // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
        if (mChatService != null) {
            // Only if the state is STATE_NONE, do we know that we haven't started already
            if (mChatService.getState() == BluetoothChatService.STATE_NONE) {
                // Start the Bluetooth chat services
                mChatService.start();
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_bluetooth_chat, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        //mConversationView = (ListView) view.findViewById(R.id.in);
        //mOutEditText = (EditText) view.findViewById(R.id.edit_text_out);
        //mSendButton = (Button) view.findViewById(R.id.button_send);
        btnStreamStart = (Button) view.findViewById(R.id.btnStart);
        btnStreamStop  = (Button) view.findViewById(R.id.btnStop);
        btnLstStart    = (Button) view.findViewById(R.id.btnLstStart);
        btnLstStop     = (Button) view.findViewById(R.id.btnLstStop);
        cbWriteToFile  = (CheckBox) view.findViewById(R.id.cbWriteToFile);
        setButtonHandlers(view);
        enableButtons(isStreaming, isListening, view);
    }

    /**
     * Set up the UI and background operations for chat.
     */
    private void setupChat() {
        Log.d(TAG, "setupChat()");

        // Initialize the array adapter for the conversation thread
       /* mConversationArrayAdapter = new ArrayAdapter<String>(getActivity(), R.layout.message);

        mConversationView.setAdapter(mConversationArrayAdapter);

        // Initialize the compose field with a listener for the return key
        mOutEditText.setOnEditorActionListener(mWriteListener);*/

        // Initialize the send button with a listener that for click events
        /*mSendButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Send a message using content of the edit text widget
                View view = getView();
                if (null != view) {
                    TextView textView = (TextView) view.findViewById(R.id.edit_text_out);
                    String message = textView.getText().toString();
                    sendMessage(message);
                }
            }
        });*/

        // Initialize the BluetoothChatService to perform bluetooth connections
        mChatService = new BluetoothChatService(getActivity(), mHandler);

        // Initialize the buffer for outgoing messages
        mOutStringBuffer = new StringBuffer("");
    }

    /**
     * Makes this device discoverable.
     */
    private void ensureDiscoverable() {
        if (mBluetoothAdapter.getScanMode() !=
                BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(discoverableIntent);
        }
    }

    /**
     * Sends a message.
     *
     * @param message A string of text to send.
     */
    private void sendMessage(String message) {
        // Check that we're actually connected before trying anything
        if (mChatService.getState() != BluetoothChatService.STATE_CONNECTED) {
            Toast.makeText(getActivity(), R.string.not_connected, Toast.LENGTH_SHORT).show();
            return;
        }

        // Check that there's actually something to send
        if (message.length() > 0) {
            // Get the message bytes and tell the BluetoothChatService to write
            byte[] send = message.getBytes();
            mChatService.write(send);

            // Reset out string buffer to zero and clear the edit text field
            mOutStringBuffer.setLength(0);
            mOutEditText.setText(mOutStringBuffer);
        }
    }

    /**
     * The action listener for the EditText widget, to listen for the return key
     */
    private TextView.OnEditorActionListener mWriteListener
            = new TextView.OnEditorActionListener() {
        public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
            // If the action is a key-up event on the return key, send the message
            if (actionId == EditorInfo.IME_NULL && event.getAction() == KeyEvent.ACTION_UP) {
                String message = view.getText().toString();
                sendMessage(message);
            }
            return true;
        }
    };

    /**
     * Updates the status on the action bar.
     *
     * @param resId a string resource ID
     */
    private void setStatus(int resId) {
        FragmentActivity activity = getActivity();
        if (null == activity) {
            return;
        }
        final ActionBar actionBar = activity.getActionBar();
        if (null == actionBar) {
            return;
        }
        actionBar.setSubtitle(resId);
    }

    /**
     * Updates the status on the action bar.
     *
     * @param subTitle status
     */
    private void setStatus(CharSequence subTitle) {
        FragmentActivity activity = getActivity();
        if (null == activity) {
            return;
        }
        final ActionBar actionBar = activity.getActionBar();
        if (null == actionBar) {
            return;
        }
        actionBar.setSubtitle(subTitle);
    }

    /**
     * The Handler that gets information back from the BluetoothChatService
     */
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            FragmentActivity activity = getActivity();
            switch (msg.what) {
                case Constants.MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case BluetoothChatService.STATE_CONNECTED:
                            setStatus(getString(R.string.title_connected_to, mConnectedDeviceName));
                            mConversationArrayAdapter.clear();
                            break;
                        case BluetoothChatService.STATE_CONNECTING:
                            setStatus(R.string.title_connecting);
                            break;
                        case BluetoothChatService.STATE_LISTEN:
                        case BluetoothChatService.STATE_NONE:
                            setStatus(R.string.title_not_connected);
                            break;
                    }
                    break;
                case Constants.MESSAGE_WRITE:
                    byte[] writeBuf = (byte[]) msg.obj;
                    // construct a string from the buffer
                    String writeMessage = new String(writeBuf);
                    if (!isStreaming) {
                        mConversationArrayAdapter.add("Me:  " + writeMessage);
                    }
                    break;
                case Constants.MESSAGE_READ:
                    if (!isListening) {
                        break;
                    }
                    byte[] readBuf = (byte[]) msg.obj;
                    // construct a string from the valid bytes in the buffer
                    //String readMessage = new String(readBuf, 0, msg.arg1);
                    //mConversationArrayAdapter.add(mConnectedDeviceName + ":  " + readMessage);
                    //
                    if (cbWriteToFile.isChecked()) {
                        SaveBluetoothBytesToFile(readBuf, bufferSize);
                    }else try {
                            PlayAudioFromByteBuffer(readBuf, bufferSize);
                        } catch (IOException e) {
                        e.printStackTrace();
                    }

                    //
                    break;
                case Constants.MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    mConnectedDeviceName = msg.getData().getString(Constants.DEVICE_NAME);
                    if (null != activity) {
                        Toast.makeText(activity, "Connected to "
                                + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                    }
                    break;
                case Constants.MESSAGE_TOAST:
                    if (null != activity) {
                        Toast.makeText(activity, msg.getData().getString(Constants.TOAST),
                                Toast.LENGTH_SHORT).show();
                    }
                    break;
            }
        }
    };

    private void SaveBluetoothBytesToFile(byte[] byteData, int size) {

        String filePath =  Environment.getExternalStorageDirectory()+"/Download/voice8K16bitmono.raw";

        //byte bdata[] = new byte[bufferSize];

        FileOutputStream os = null;
        try {
            os = new FileOutputStream(filePath);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        try {
                os.write(byteData);
        } catch (IOException e) {
                e.printStackTrace();
          }
    }

    private void PlayAudioFromByteBuffer(byte[] byteData, int MinBufferSize) throws IOException {
        // We keep temporarily filePath globally as we have only two sample sounds now..

//        if (filePath == null)
//            return;

        /*AudioTrack at = new AudioTrack(AudioManager.STREAM_MUSIC, 44100, AudioFormat.CHANNEL_IN_STEREO,
                AudioFormat.ENCODING_PCM_16BIT, MinBufferSize, AudioTrack.MODE_STREAM);*/


        if (at == null) {
            Log.d("TCAudio", "audio track is not initialised ");
            return;
        }

        //int bufferCount = 192 * 1024; // 192 kb

        /*byte[] byteData = null;
        File file = null;
        file = new File(filePath);

        FileInputStream in = null;
        try {
            in = new FileInputStream(file);

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }*/
        //int firstByte = 44;
        //int bytesread = 0, ret = 0;
        //int size = (int) in.getChannel().size();//(int) file.length();
        // int byteCount = bufferCount;

        //byteData = new byte[size];
        //at.play();
        //ret = in.read(byteData, firstByte, byteData.length-firstByte);
        at.write(byteData, 0, byteData.length);

        /*if (byteCount > size) {
            byteCount = size;
        }
        byteData = new byte[byteCount];
        at.play();
        while (firstByte < size) {
            ret = in.read(byteData, firstByte, byteCount);
            at.write(byteData, 0, ret);
            firstByte =+ byteCount;
        }*/

        //in.close();
        //at.stop();
        //at.release();


    }

    private void PlayAudioFileViaAudioTrack(String filePath, int MinBufferSize) throws IOException {
        // We keep temporarily filePath globally as we have only two sample sounds now..

//        if (filePath == null)
//            return;

        AudioTrack at = new AudioTrack(AudioManager.STREAM_MUSIC, 44100, AudioFormat.CHANNEL_IN_STEREO,
                AudioFormat.ENCODING_PCM_16BIT, MinBufferSize, AudioTrack.MODE_STREAM);


        if (at == null) {
            Log.d("TCAudio", "audio track is not initialised ");
            return;
        }

        int count = 512 * 1024; // 512 kb
//Reading the file..
        byte[] byteData = null;
        File file = null;
        //try {
            file = new File(filePath);
        /*} catch (IOException e) {
            e.printStackTrace();
            //Toast.makeText(BluetoothChatFragment.this, "file not founf", Toast.LENGTH_SHORT).show();
            return;
        }*/



        byteData = new byte[(int) count];
        FileInputStream in = null;
        try {
            in = new FileInputStream(file);

        } catch (FileNotFoundException e) {
// TODO Auto-generated catch block
            e.printStackTrace();
            return;
        }
        int firstByte = 0;//44;
        int bytesread = 0, ret = 0;
        int size = (int) file.length()-firstByte;
        at.play();
        //while (bytesread < size) {
        //ret = in.read(byteData, firstByte, count-firstByte);
        ret = in.read(byteData, firstByte, byteData.length-firstByte);
        at.write(byteData,0, ret);
        //if (ret != -1) {
        // Write the byte array to the track
        //at.write(byteData,0, ret);
        //bytesread += ret;
        //} else break;
        //} in.close(); at.stop(); at.release();
        //


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
                    setupChat();
                } else {
                    // User did not enable Bluetooth or an error occurred
                    Log.d(TAG, "BT not enabled");
                    Toast.makeText(getActivity(), R.string.bt_not_enabled_leaving,
                            Toast.LENGTH_SHORT).show();
                    getActivity().finish();
                }
        }
    }

    /**
     * Establish connection with other divice
     *
     * @param data   An {@link Intent} with {@link DeviceListActivity#EXTRA_DEVICE_ADDRESS} extra.
     * @param secure Socket Security type - Secure (true) , Insecure (false)
     */
    private void connectDevice(Intent data, boolean secure) {
        // Get the device MAC address
        String address = data.getExtras()
                .getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
        // Get the BluetoothDevice object
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        // Attempt to connect to the device
        mChatService.connect(device, secure);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.bluetooth_chat, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.secure_connect_scan: {
                // Launch the DeviceListActivity to see devices and do scan
                Intent serverIntent = new Intent(getActivity(), DeviceListActivity.class);
                startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_SECURE);
                return true;
            }
            case R.id.insecure_connect_scan: {
                // Launch the DeviceListActivity to see devices and do scan
                Intent serverIntent = new Intent(getActivity(), DeviceListActivity.class);
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

}
