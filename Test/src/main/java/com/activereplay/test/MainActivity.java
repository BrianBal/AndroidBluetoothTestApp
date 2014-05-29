package com.activereplay.test;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;


public class MainActivity extends Activity {

    public static final UUID TRACE_BREDR_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    public static final UUID TRACE_SERVICE_UUID = UUID.fromString("0000A100-8501-11e3-ba12-0002a5d5c51b");
    public static final UUID STATUS_UUID = UUID.fromString("0000A102-8501-11e3-ba12-0002a5d5c51b");
    public static final UUID RECORDING_CONTROL_UUID  = UUID.fromString("0000A101-8501-11e3-ba12-0002a5d5c51b");
    public static final UUID BATTERY_CHARGE_UUID = UUID.fromString("0000a103-8501-11e3-ba12-0002a5d5c51b");
    public static final UUID FILES_AVAILABLE_UUID = UUID.fromString("0000a104-8501-11e3-ba12-0002a5d5c51b");
    public static final UUID CLEAR_DATA_UUID = UUID.fromString("0000a105-8501-11e3-ba12-0002a5d5c51b");
    public static final UUID SPACE_AVAILABLE_UUID = UUID.fromString("0000a106-8501-11e3-ba12-0002a5d5c51b");
    public static final UUID BLUETOOTH_2_1_STATUS_UUID = UUID.fromString("0000a107-8501-11e3-ba12-0002a5d5c51b");
    public static final UUID BLUETOOTH_2_1_CONTROL_UUID = UUID.fromString("0000a108-8501-11e3-ba12-0002a5d5c51b");
    public static final UUID FIRMWARE_VERSION_UUID = UUID.fromString("0000a109-8501-11e3-ba12-0002a5d5c51b");
    public static final UUID REBOOT_UUID = UUID.fromString("0000a110-8501-11e3-ba12-0002a5d5c51b");

    BluetoothManager bluetoothManager;
    BluetoothAdapter bluetoothAdapter;
    boolean scanning = false;
    Handler handler;

    BluetoothDevice bluetoothDevice;
    BluetoothGatt bluetoothGatt;
    BluetoothSocket btSocket;

    TextView txtLog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
        handler = new Handler(getMainLooper());

        txtLog = (TextView)findViewById(R.id.txtLog);

        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(btReceiver, filter);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        switch ( id )
        {
            case R.id.action_ble_connect:
                discoverBLEDevices();
                return true;
            case R.id.action_ble_connect_paired:
                discoverPairedBLEDevices();
                return true;
            case R.id.action_ble_read_battery:
                readBattery();
                return true;
            case R.id.action_ble_read_files:
                readFilesAvailable();
                return true;
            case R.id.action_ble_disconnect:
                closeBLEConnection();
                return true;
            case R.id.action_ble_on_edr:
                activateBREDR();
                return true;
            case R.id.action_ble_off_edr:
                deactivateBREDR();
                return true;
            case R.id.action_bredr_connect:
                connectBREDR();
                return true;
            case R.id.action_bredr_disconnect:
                closeBREDRDevice();
                return true;
            case R.id.action_bt_reset:
                resetBluetooth();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void logMessage(final String msg)
    {
        runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                // Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
                String log = txtLog.getText().toString();
                log = msg + "\n" + log;
                txtLog.setText(log);
            }
        });
    }

    public void readStatus()
    {
        readCharacteristic(STATUS_UUID);
    }

    public void readBattery()
    {
        readCharacteristic(BATTERY_CHARGE_UUID);
    }

    public void readFilesAvailable()
    {
        readCharacteristic(FILES_AVAILABLE_UUID);
    }

    public void readFirmware()
    {
        readCharacteristic(FIRMWARE_VERSION_UUID);
    }

    public void writeClearFiles()
    {

    }

    public void readCharacteristic(UUID uuid)
    {
        BluetoothGattService service = bluetoothGatt.getService(TRACE_SERVICE_UUID);
        if ( service != null )
        {
            BluetoothGattCharacteristic characteristic = service.getCharacteristic(uuid);
            if ( characteristic != null )
            {
                bluetoothGatt.readCharacteristic(characteristic);
            }
        }
    }

    public void activateBREDR()
    {
        writeCharacteristic(BLUETOOTH_2_1_CONTROL_UUID, new byte[] { (byte)0x01 } );
    }

    public void deactivateBREDR()
    {
        writeCharacteristic(BLUETOOTH_2_1_CONTROL_UUID, new byte[] { (byte)0x00 } );
    }

    public void writeCharacteristic(UUID uuid, byte[] data)
    {
        BluetoothGattService service = bluetoothGatt.getService(TRACE_SERVICE_UUID);
        if ( service != null )
        {
            BluetoothGattCharacteristic characteristic = service.getCharacteristic(uuid);
            if ( characteristic != null )
            {
                characteristic.setValue(data);
                bluetoothGatt.writeCharacteristic(characteristic);
            }
        }
    }

    public void discoverBLEDevices()
    {
        if ( ! scanning )
        {
            bluetoothAdapter.startLeScan(scanCallback);
        }
    }

    public void resetBluetooth()
    {
        bluetoothAdapter.disable();
    }

    public void discoverPairedBLEDevices()
    {
        Set<BluetoothDevice> bondedDevices = bluetoothAdapter.getBondedDevices();
        for (BluetoothDevice d : bondedDevices)
        {
            String name = d.getName();
            if ( name != null && name.equalsIgnoreCase("trace") )
            {
                logMessage("bonded device found " + d.getAddress());
                connectToBLEDevice(d);
                break;
            }
        }
    }

    public void connectToBLEDevice(final BluetoothDevice btDevice)
    {
        bluetoothAdapter.stopLeScan(scanCallback);

        //this need to be called on the UI thread to avoid a Samsung BLE threading issue:  http://stackoverflow.com/questions/20069507/solved-gatt-callback-fails-to-register/20507449#20507449
        handler.post(new Runnable()
        {
            @Override
            public void run()
            {
                if (bluetoothAdapter == null || btDevice == null)
                {
                    logMessage("BluetoothAdapter not initialized or unspecified address.");
                    return;
                }

                if ( bluetoothGatt != null )
                {
                    logMessage("Trying to reconnect.");
                    bluetoothGatt.connect();
                }
                else
                {
                    logMessage("Trying to create a new connection.");
                    bluetoothDevice = btDevice;
                    bluetoothGatt = bluetoothDevice.connectGatt(MainActivity.this, false, gattCallback);
                }
            }
        });
    }

    public void closeBLEConnection()
    {
        handler.post(new Runnable()
        {
            @Override
            public void run()
            {
                if ( bluetoothGatt != null )
                {
                    bluetoothGatt.close();
                    bluetoothGatt = null;
                    logMessage("closing ble connection");
                }
            }
        });
    }

    public void connectBREDR()
    {
        try
        {
            new Thread(new Runnable()
            {
                public void run()
                {
                    bluetoothAdapter.cancelDiscovery();
                    boolean val = false;

                    if ( bluetoothDevice == null )
                    {
                        logMessage("BR/EDR Connection failed no bluetooth device found");
                        return;
                    }

                    BluetoothSocket btSocket = null;
                    try
                    {
                        btSocket = bluetoothDevice.createInsecureRfcommSocketToServiceRecord(TRACE_BREDR_UUID);
                        btSocket.connect();
                        if ( btSocket.isConnected() )
                        {
                            logMessage("BR/EDR connected");
                        }
                    }
                    catch ( Exception e )
                    {
                        e.printStackTrace();
                        logMessage("BR/EDR Connection failed " + e.getMessage());
                        if ( btSocket != null )
                        {
                            try
                            {
                                btSocket.close();
                            }
                            catch ( IOException e1 )
                            {
                                e1.printStackTrace();
                            }
                        }
                    }
                }
            }).start();
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }
    }

    public void closeBREDRDevice()
    {
        if ( btSocket != null )
        {
            try
            {
                btSocket.close();
            }
            catch ( IOException e )
            {
                e.printStackTrace();
            }
            btSocket = null;
        }
        logMessage("BR/EDR connection closed");
    }

    private List<UUID> parseUUIDs(final byte[] advertisedData)
    {
        List<UUID> uuids = new ArrayList<UUID>();

        int offset = 0;
        while (offset < (advertisedData.length - 2))
        {
            int len = advertisedData[offset++];
            if (len == 0)
            {
                break;
            }

            int type = advertisedData[offset++];
            switch (type)
            {
                case 0x02: // Partial list of 16-bit UUIDs
                case 0x03: // Complete list of 16-bit UUIDs
                    while (len > 1)
                    {
                        int uuid16 = advertisedData[offset++];
                        uuid16 += (advertisedData[offset++] << 8);
                        len -= 2;
                        uuids.add(UUID.fromString(String.format("%08x-0000-1000-8000-00805f9b34fb", uuid16)));
                    }
                    break;
                case 0x06:// Partial list of 128-bit UUIDs
                case 0x07:// Complete list of 128-bit UUIDs
                    // Loop through the advertised 128-bit UUID's.
                    while (len >= 16)
                    {
                        try
                        {
                            // Wrap the advertised bits and order them.
                            ByteBuffer buffer = ByteBuffer.wrap(advertisedData, offset++, 16).order(ByteOrder.LITTLE_ENDIAN);
                            long mostSignificantBit = buffer.getLong();
                            long leastSignificantBit = buffer.getLong();
                            uuids.add(new UUID(leastSignificantBit, mostSignificantBit));
                        }
                        catch (IndexOutOfBoundsException e)
                        {
                            // Defensive programming.
                            continue;
                        }
                        finally
                        {
                            // Move the offset to read the next uuid.
                            offset += 15;
                            len -= 16;
                        }
                    }
                    break;
                default:
                    offset += (len - 1);
                    break;
            }
        }

        return uuids;
    }


    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {

        @Override
        public void onConnectionStateChange(final BluetoothGatt gatt, int status, int newState)
        {
            if (newState == BluetoothProfile.STATE_CONNECTED )
            {
                logMessage("ble connected");
                logMessage("attempting to discover ble services");
                logMessage("...");
                gatt.discoverServices();
            }
            else if (newState == BluetoothProfile.STATE_DISCONNECTED)
            {
                logMessage("ble disconnected");
                bluetoothGatt.close();
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status)
        {
            if ( status == BluetoothGatt.GATT_SUCCESS )
            {
                logMessage("ble services discovered");
            }
            else
            {
                logMessage("ble service discovery failed");
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status)
        {
            byte[] data = characteristic.getValue();
            if ( BATTERY_CHARGE_UUID.equals(characteristic.getUuid()) )
            {
                int battery = (int)data[0];
                logMessage("battery: " + battery);
            }
            if ( FILES_AVAILABLE_UUID.equals(characteristic.getUuid()) )
            {
                int files = (int)data[0];
                logMessage("files available: " + files);
            }
            if ( FIRMWARE_VERSION_UUID.equals(characteristic.getUuid()) )
            {
                int major = data[0];
                int minor = data[1];
                int build = data[2];
                String firmware = String.format("%s.%s.%s", major, minor, build);
                logMessage("firmware: " + firmware);
            }
        }


        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status)
        {
            String msg = "failed ";
            if ( status == BluetoothGatt.GATT_SUCCESS )
            {
                msg = "success ";
            }

            if ( BLUETOOTH_2_1_CONTROL_UUID.equals(characteristic.getUuid()) )
            {
                msg = msg + " writing 2.1 control characteristic to " + characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_SINT32, 0);
            }

            logMessage(msg);
        }


        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic)
        {
        }
    };

    BluetoothAdapter.LeScanCallback scanCallback = new BluetoothAdapter.LeScanCallback()
    {
        @Override
        public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord)
        {
            logMessage("device found: " + device.getAddress());
            List<UUID> uuids = parseUUIDs(scanRecord);
            for (UUID uuid : uuids)
            {
                if (uuid.equals(TRACE_SERVICE_UUID))
                {
                    connectToBLEDevice(device);
                    break;
                }
            }
        }
    };

    private final BroadcastReceiver btReceiver = new BroadcastReceiver()
    {
        public void onReceive(Context context, Intent intent)
        {
            String action = intent.getAction();
            // When discovery finds a device
            if ( BluetoothAdapter.ACTION_STATE_CHANGED.equals(action) )
            {
                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -999);
                if ( state == BluetoothAdapter.STATE_OFF )
                {
                    logMessage("bluetooth is off");
                    bluetoothAdapter.enable();
                }
                if ( state == BluetoothAdapter.STATE_ON )
                {
                    logMessage("bluetooth is on");
                }
            }
        }
    };

}
