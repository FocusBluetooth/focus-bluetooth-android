/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.example.focus;

import java.util.List;
import java.util.UUID;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

public class WorkableService extends Service {
	public static final String TAG = "WorkableService";

	public static final UUID SERVICE = UUID.fromString("0000fff0-0000-1000-8000-00805f9b34fb");
	public static final UUID BATTERY_LEVEL = UUID.fromString("0000fff1-0000-1000-8000-00805f9b34fb");
	public static final UUID ACTUAL_LEVEL = UUID.fromString("0000fff2-0000-1000-8000-00805f9b34fb");
	public static final UUID MODE = UUID.fromString("0000fff3-0000-1000-8000-00805f9b34fb");
	public static final UUID ELECTRODES_MODE = UUID.fromString("0000fff4-0000-1000-8000-00805f9b34fb");
	public static final UUID TIME_IN_ACTIVE_MODE = UUID.fromString("0000fff6-0000-1000-8000-00805f9b34fb");
	public static final UUID FW_VERSION = UUID.fromString("0000fff7-0000-1000-8000-00805f9b34fb");
	public static final UUID FW_DATA_BUFFER = UUID.fromString("0000fff8-0000-1000-8000-00805f9b34fb");
	public static final UUID PIN_CODE = UUID.fromString("0000fff9-0000-1000-8000-00805f9b34fb");
	public static final UUID MAX_CURRENT = UUID.fromString("0000fff5-0000-1000-8000-00805f9b34fb");
	public static final UUID CURRENT_OFFSET = UUID.fromString("0000fffa-0000-1000-8000-00805f9b34fb");
	public static final UUID CURRENT_RISE_FALL_TIME = UUID.fromString("0000fffb-0000-1000-8000-00805f9b34fb");
	public static final UUID PULSE_WIDTH = UUID.fromString("0000fffc-0000-1000-8000-00805f9b34fb");
	public static final UUID PULSE_PERIOD = UUID.fromString("0000fffd-0000-1000-8000-00805f9b34fb");

	public static final UUID ACTUAL_LEVEL_DESCRIPTOR = UUID.fromString("00002901-0000-1000-8000-00805f9b34fb");
	public static final UUID BATTERY_LEVEL_DESCRIPTOR = UUID.fromString("00002901-0000-1000-8000-00805f9b34fb"); // yes
																													// it
																													// is
																													// the
																													// //
																													// same
																													// as
																													// Actual
																													// level
	public static final int CONNECT_MSG = 21;
	public static final int DISCONNECT_MSG = 22;
	public static final int READY_MSG = 23;
	public static final int VALUE_MSG = 24;
	public static final int GATT_DEVICE_FOUND_MSG = 25;
	public static final int GATT_CHARACTERISTIC_RSSI_MSG = 26;
	public static final int CHARACTERISTIC_CHANGED = 27;

	/** Source of device entries in the device list */
	public static final int DEVICE_SOURCE_SCAN = 10;
	public static final int DEVICE_SOURCE_BONDED = 11;
	public static final int DEVICE_SOURCE_CONNECTED = 12;

	/** Intent extras */
	public static final String EXTRA_DEVICE = "DEVICE";
	public static final String EXTRA_RSSI = "RSSI";
	public static final String EXTRA_SOURCE = "SOURCE";
	public static final String EXTRA_ADDR = "ADDRESS";
	public static final String EXTRA_CONNECTED = "CONNECTED";
	public static final String EXTRA_STATUS = "STATUS";
	public static final String EXTRA_UUID = "UUID";
	public static final String EXTRA_VALUE = "VALUE";
	public static final String EXTRA_CHARACTERISTIC = "CHARACTERISTIC";
	public static final String EXTRA_MESSAGE = "MESSAGE";

	private int mConnectionState = CommonValues.STATE_DISCONNECTED;

	// Stops scanning after 10 seconds.
	private static final long SCAN_PERIOD = 10000;

	private BluetoothManager mBluetoothManager;
	private BluetoothAdapter mBtAdapter = null;
	public BluetoothGatt mBluetoothGatt = null;
	// public BluetoothGattServer mBluetoothGattServer = null;
	private boolean mScanning;
	private String mBluetoothDeviceAddress;
	private Handler mHandler;
	private Handler mActivityHandler = null;
	private Handler mDeviceListHandler = null;
	public boolean isNoti = false;

	/**
	 * Profile service connection listener
	 */
	public class LocalBinder extends Binder {
		WorkableService getService() {
			return WorkableService.this;
		}
	}

	@Override
	public IBinder onBind(Intent arg0) {
		return binder;
	}

	private final IBinder binder = new LocalBinder();

	@Override
	public void onCreate() {
		Debugger.d(TAG, "onCreate()");
		if (mBtAdapter == null) {
			final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
			mBtAdapter = bluetoothManager.getAdapter();
			if (mBtAdapter == null)
				return;
			mHandler = new Handler();
		}

	}

	public boolean initialize() {
		// For API level 18 and above, get a reference to BluetoothAdapter
		// through
		// BluetoothManager.
		if (mBluetoothManager == null) {
			mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
			if (mBluetoothManager == null) {
				Debugger.e(TAG, "initialize() >>>> Unable to initialize BluetoothManager.");
				return false;
			}
		}

		mBtAdapter = mBluetoothManager.getAdapter();
		if (mBtAdapter == null) {
			Debugger.e(TAG, "initialize() >>>> Unable to obtain a BluetoothAdapter.");
			return false;
		}

		return true;
	}

	void setActivityHandler(Handler mHandler) {
		Debugger.d(TAG, "Activity Handler set");
		mActivityHandler = mHandler;
	}

	public void setDeviceListHandler(Handler mHandler) {
		Debugger.d(TAG, "Device List Handler set");
		mDeviceListHandler = mHandler;
	}

	@Override
	public void onDestroy() {
		Debugger.d(TAG, "onDestroy()");
		if (mBluetoothGatt == null) {
			return;
		}
		mBluetoothGatt.close();
		mBluetoothGatt = null;
		super.onDestroy();
	}

	// API 18 method that is called when device is found
	private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {

		@Override
		public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
			Debugger.d(TAG, "onLeScan() - device=" + device + ", rssi=" + rssi);

			if (!checkIfBroadcastMode(scanRecord)) {
				// set mBluetoothGatt
				// mBluetoothGatt = device.connectGatt(getApplicationContext(),
				// false, mGattCallbacks);
				Bundle mBundle = new Bundle();
				Message msg = Message.obtain(mDeviceListHandler, GATT_DEVICE_FOUND_MSG);
				mBundle.putParcelable(BluetoothDevice.EXTRA_DEVICE, device);
				mBundle.putInt(EXTRA_RSSI, rssi);
				mBundle.putInt(EXTRA_SOURCE, DEVICE_SOURCE_SCAN);
				msg.setData(mBundle);
				msg.sendToTarget();
			} else
				Debugger.i(TAG, "device =" + device + " is in Broadcast mode, hence not displaying");
		}
	};
	/**
	 * GATT client callbacks
	 */
	private BluetoothGattCallback mGattCallbacks = new BluetoothGattCallback() {

		@Override
		public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
			if (status == BluetoothGatt.GATT_SUCCESS) {
				BluetoothDevice device = gatt.getDevice();
				// int state = device.getBondState();
				Debugger.d(TAG, "onConnectionStateChange() >>>> newState = " + newState);
				//
//				 if(newState == BluetoothDevice.BOND_BONDED)
//				 {
				 boolean result = mBluetoothGatt.discoverServices();
				 if (!result)
				 showMessage("Error when discovering services");
//				 //writeCharacteristic(WorkableService.MODE, (byte) 2);
//				 }
				Debugger.d(TAG, "onConnectionStateChange() >>>> Client onConnectionStateChange (" + device.getAddress() + ")");
				// Device has been connected - start service discovery
				if (newState == BluetoothProfile.STATE_CONNECTED && mBluetoothGatt != null) {

					mConnectionState = CommonValues.STATE_CONNECTED;
					// pairDevice(device);
					Bundle mBundle = new Bundle();
					Message msg = Message.obtain(mActivityHandler, CONNECT_MSG);
					mBundle.putString(BluetoothDevice.EXTRA_DEVICE, device.getAddress());
					mBundle.putString(BluetoothDevice.EXTRA_NAME, device.getName());
					msg.setData(mBundle);
					msg.sendToTarget();
					// if (mBluetoothGatt.getService(SERVICE) != null) {
					// Message message = Message.obtain(mActivityHandler,
					// READY_MSG);
					// message.sendToTarget();
					// }
//					boolean result = mBluetoothGatt.discoverServices();
//					if (!result)
//						// else
//						showMessage("Error when discovering services");
				}
				if (newState == BluetoothProfile.STATE_DISCONNECTED && mBluetoothGatt != null) {
					mConnectionState = CommonValues.STATE_DISCONNECTED;
					Bundle mBundle = new Bundle();
					Message msg = Message.obtain(mActivityHandler, DISCONNECT_MSG);
					mBundle.putString(BluetoothDevice.EXTRA_DEVICE, device.getAddress());
					msg.setData(mBundle);
					msg.sendToTarget();
				}
				if (status == 141) {
					Debugger.d(TAG, "onConnectionStateChange() >>>> Status == 141. Gatt already open");
				}
			}
		}

		@Override
		public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
			Debugger.d(TAG, "onCharacteristicChanged() >>>>  characteristic = characteristic");
			if (characteristic.getUuid().equals(ACTUAL_LEVEL)) {
				Debugger.d(TAG, "onCharacteristicChanged() >>>> Actual Level was changed");
				Bundle mBundle = new Bundle();
				Message msg = Message.obtain(mActivityHandler, CHARACTERISTIC_CHANGED);
				mBundle.putString(EXTRA_CHARACTERISTIC, characteristic.getUuid().toString());
				msg.setData(mBundle);
				msg.sendToTarget();
			}
			if (characteristic.getUuid().equals(BATTERY_LEVEL)) {
				Debugger.d(TAG, "onCharacteristicChanged() >>>> Battery Level was changed");
				Bundle mBundle = new Bundle();
				Message msg = Message.obtain(mActivityHandler, CHARACTERISTIC_CHANGED);
				mBundle.putString(EXTRA_CHARACTERISTIC, characteristic.getUuid().toString());
				msg.setData(mBundle);
				msg.sendToTarget();
				// readCharacteristic(characteristic);
			}

		}

		@Override
		public void onServicesDiscovered(BluetoothGatt gatt, int status) {
			if (status == BluetoothGatt.GATT_SUCCESS) {
				Debugger.d(TAG, "onServicesDiscovered()");
				Message msg = Message.obtain(mActivityHandler, READY_MSG);
				msg.sendToTarget();
			} else {
				Debugger.w(TAG, "onServicesDiscovered() >>>> received: " + status);
			}

		}

		@Override
		public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic arg0, int arg1) {
			Debugger.d(TAG, "OnCharacteristicWrite() " + arg0);
		}

		@Override
		public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
			Debugger.d(TAG, "onCharacteristicRead() >>>> charac = " + characteristic);

			UUID uuid = characteristic.getUuid();
			byte[] value = characteristic.getValue();
			Bundle mBundle = new Bundle();
			Message msg = Message.obtain(mActivityHandler, VALUE_MSG);

			if (uuid.equals(BATTERY_LEVEL)) {
				//List<BluetoothGattDescriptor> list = characteristic.getDescriptors();
				mBundle.putString(EXTRA_CHARACTERISTIC, BATTERY_LEVEL.toString());
				mBundle.putByte(BATTERY_LEVEL.toString(), value[0]);
			}
			if (uuid.equals(ACTUAL_LEVEL)) {
				// setCharacteristicNotification(characteristic, true);
				mBundle.putString(EXTRA_CHARACTERISTIC, ACTUAL_LEVEL.toString());
				mBundle.putByte(ACTUAL_LEVEL.toString(), value[0]);
			}
			if (uuid.equals(MODE)) {
				mBundle.putString(EXTRA_CHARACTERISTIC, MODE.toString());
				mBundle.putByte(MODE.toString(), value[0]);
			}
			if (uuid.equals(ELECTRODES_MODE)) {
				mBundle.putString(EXTRA_CHARACTERISTIC, ELECTRODES_MODE.toString());
				mBundle.putByte(ELECTRODES_MODE.toString(), value[0]);
			}
			if (uuid.equals(TIME_IN_ACTIVE_MODE)) {
				mBundle.putString(EXTRA_CHARACTERISTIC, TIME_IN_ACTIVE_MODE.toString());
				mBundle.putByteArray(TIME_IN_ACTIVE_MODE.toString(), value);
			}
			if (uuid.equals(FW_VERSION)) {
				mBundle.putString(EXTRA_CHARACTERISTIC, FW_VERSION.toString());
				mBundle.putByteArray(FW_VERSION.toString(), value);
			}
			if (uuid.equals(PIN_CODE)) {
				mBundle.putString(EXTRA_CHARACTERISTIC, PIN_CODE.toString());
				mBundle.putByteArray(PIN_CODE.toString(), value);
			}
			if (uuid.equals(MAX_CURRENT)) {
				mBundle.putString(EXTRA_CHARACTERISTIC, MAX_CURRENT.toString());
				mBundle.putByte(MAX_CURRENT.toString(), value[0]);
			}
			if (uuid.equals(CURRENT_OFFSET)) {
				mBundle.putString(EXTRA_CHARACTERISTIC, CURRENT_OFFSET.toString());
				mBundle.putByte(CURRENT_OFFSET.toString(), value[0]);
			}
			if (uuid.equals(CURRENT_RISE_FALL_TIME)) {
				mBundle.putString(EXTRA_CHARACTERISTIC, CURRENT_RISE_FALL_TIME.toString());
				mBundle.putByte(CURRENT_RISE_FALL_TIME.toString(), value[0]);
			}
			if (uuid.equals(PULSE_WIDTH)) {
				mBundle.putString(EXTRA_CHARACTERISTIC, PULSE_WIDTH.toString());
				mBundle.putByteArray(PULSE_WIDTH.toString(), value);
			}
			if (uuid.equals(PULSE_PERIOD)) {
				mBundle.putString(EXTRA_CHARACTERISTIC, PULSE_PERIOD.toString());
				mBundle.putByteArray(PULSE_PERIOD.toString(), value);
			}
			msg.setData(mBundle);
			msg.sendToTarget();
		}

		public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
			Debugger.i(TAG, "onDescriptorRead()");
			//
			// BluetoothGattCharacteristic mTxPowerccc =
			// descriptor.getCharacteristic();
			// Debugger.i(TAG, "Registering for notification");
			//
			// boolean isenabled = enableNotification(mTxPowerccc, true);
			// Debugger.i(TAG, "Notification status =" + isenabled);
		}

		public void onReadRemoteRssi(BluetoothGatt gatt, BluetoothDevice device, int rssi, int status) {
			Debugger.i(TAG, "onRssiRead() >>>> rssi value is " + rssi);

			Bundle mBundle = new Bundle();
			Message msg = Message.obtain(mActivityHandler, GATT_CHARACTERISTIC_RSSI_MSG);
			mBundle.putParcelable(EXTRA_DEVICE, device);
			mBundle.putInt(EXTRA_RSSI, rssi);
			mBundle.putInt(EXTRA_STATUS, status);
			msg.setData(mBundle);
			msg.sendToTarget();
		}

	};

	public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic, boolean enabled) {
		if (mBtAdapter == null || mBluetoothGatt == null) {
			Log.w(TAG, "BluetoothAdapter not initialized");
			return;
		}
		mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);

		// This is specific to Actual Level.
		if (ACTUAL_LEVEL.equals(characteristic.getUuid())) {
			BluetoothGattDescriptor descriptor = characteristic.getDescriptor(ACTUAL_LEVEL_DESCRIPTOR);
			descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
			mBluetoothGatt.writeDescriptor(descriptor);
		}

		if (BATTERY_LEVEL.equals(characteristic.getUuid())) {
			BluetoothGattDescriptor descriptor = characteristic.getDescriptor(BATTERY_LEVEL_DESCRIPTOR);
			descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
			mBluetoothGatt.writeDescriptor(descriptor);
		}
	}

	/*
	 * Broadcast mode checker API
	 */
	public boolean checkIfBroadcastMode(byte[] scanRecord) {
		int offset = 0;
		while (offset < (scanRecord.length - 2)) {
			int len = scanRecord[offset++];
			if (len == 0)
				break; // Length == 0 , we ignore rest of the packet
			// TODO: Check the rest of the packet if get len = 0

			int type = scanRecord[offset++];
			switch (type) {
			case 0x01:

				if (len >= 2) {
					// The usual scenario(2) and More that 2 octets scenario.
					// Since this data will be in Little endian format, we
					// are interested in first 2 bits of first byte
					byte flag = scanRecord[offset++];
					/*
					 * 00000011(0x03) - LE Limited Discoverable Mode and LE
					 * General Discoverable Mode
					 */
					if ((flag & 0x03) > 0)
						return false;
					else
						return true;
				} else if (len == 1) {
					continue;// ignore that packet and continue with the rest
				}
			default:
				offset += (len - 1);
				break;
			}
		}
		return false;
	}

	public boolean connect(final String address) {
		if (mBtAdapter == null || address == null) {
			Debugger.w(TAG, "connect() >>>> BluetoothAdapter not initialized or unspecified address.");
			return false;
		}

		// Previously connected device. Try to reconnect.
		if (mBluetoothDeviceAddress != null && address.equals(mBluetoothDeviceAddress) && mBluetoothGatt != null) {
			Debugger.d(TAG, "connect() >>>> Trying to use an existing mBluetoothGatt for connection.");

			mBluetoothGatt.getDevice().connectGatt(this, false, mGattCallbacks);
			mConnectionState = CommonValues.STATE_CONNECTING;
			return true;
		}

		final BluetoothDevice device = mBtAdapter.getRemoteDevice(address);
		if (device == null) {
			Debugger.w(TAG, " connect() >>>> Device not found.  Unable to connect.");
			return false;

		}
		// new connection
		Debugger.d(TAG, "connect() >>>> Trying to create a new connection.");
		mBluetoothGatt = device.connectGatt(this, false, mGattCallbacks);
		mBluetoothDeviceAddress = address;
		mConnectionState = CommonValues.STATE_CONNECTING;
		return true;
	}

	public boolean connect(BluetoothDevice device) {
		if (mBtAdapter == null) {
			Debugger.w(TAG, "connect() >>>> BluetoothAdapter not initialized or unspecified address.");
			return false;
		}

		// Previously connected device. Try to reconnect.
		// if (mBluetoothDeviceAddress != null &&
		// device.getAddress().equals(mBluetoothDeviceAddress)
		// && mBluetoothGatt != null) {
		// Debugger.d(TAG,
		// "Trying to use an existing mBluetoothGatt for connection.");
		//
		// mBluetoothGatt.getDevice().connectGatt(this, false, mGattCallbacks);
		// mConnectionState = CommonValues.STATE_CONNECTING;
		// return true;
		// }

		// /final BluetoothDevice device = mBtAdapter.getRemoteDevice(device);
		if (device == null) {
			Debugger.w(TAG, "connect() >>>> Device not found.  Unable to connect.");
			return false;

		}
		// new connection
		Debugger.d(TAG, "connect() >>>> Trying to create a new connection.");
		if (mBluetoothGatt != null)

		{
			mBluetoothGatt.close();
			mBluetoothGatt = null;
		}
		mBluetoothGatt = device.connectGatt(this, true, mGattCallbacks);
		mBluetoothDeviceAddress = device.getAddress();
		mConnectionState = CommonValues.STATE_CONNECTING;
		return true;
	}

	public void disconnect() {
		if (mBtAdapter == null || mBluetoothGatt == null) {
			Debugger.w(TAG, "connect() >>>> BluetoothAdapter not initialized");
			return;
		}
		mBluetoothGatt.disconnect();
	}

	// API 18

	public void scan(final boolean enable) {
		if (enable) {
			// Stops scanning after a pre-defined scan period.

			// mHandler.postDelayed(new Runnable() {
			// @Override
			// public void run() {
			// mScanning = false;
			// mBtAdapter.stopLeScan(mLeScanCallback);
			//
			// }
			// }, SCAN_PERIOD);

			mScanning = true;
			mBtAdapter.startLeScan(mLeScanCallback);
		} else {
			mScanning = false;
			mBtAdapter.stopLeScan(mLeScanCallback);
		}
	}

	public BluetoothGattService getService(UUID ServiceUUID) {
		if (mBluetoothGatt != null) {

			Debugger.d(TAG, "getService() >>>> ServiceUUID=" + ServiceUUID);
			return mBluetoothGatt.getService(ServiceUUID);
		}
		return null;
	}

	public boolean readCharacteristic(BluetoothGattCharacteristic Char) {
		boolean result = false;
		if (mBluetoothGatt != null) {
			result = mBluetoothGatt.readCharacteristic(Char);
			Debugger.d(TAG, "readCharacteristic() >>>> Char=" + Char);
			return result;
		}
		return false;
	}

	public void discoverServices() {
		boolean result = mBluetoothGatt.discoverServices();
		if (!result)
			// else
			showMessage("Error when discovering services");
	}

	private void showMessage(String message) {
		Debugger.e(TAG, message);

		mConnectionState = CommonValues.STATE_DISCONNECTED;
		Bundle mBundle = new Bundle();
		Message msg = Message.obtain(mActivityHandler, DISCONNECT_MSG);
		mBundle.putString(BluetoothDevice.EXTRA_DEVICE, mBluetoothGatt.getDevice().getAddress());
		mBundle.putString(EXTRA_MESSAGE, message);
		msg.setData(mBundle);
		msg.sendToTarget();
	}

	public void readSpecificChar(UUID charUUID) {
		byte[] value;
		BluetoothGattService service = mBluetoothGatt.getService(SERVICE);
		if (service == null) {
			showMessage("Service not found!");
			return;
		}

		BluetoothGattCharacteristic characteristic = service.getCharacteristic(charUUID);
		if (characteristic == null) {
			showMessage("readSpecificChar() >>>> Charateristic not found!");
			return;
		}

		boolean result = mBluetoothGatt.readCharacteristic(characteristic);
		if (result == false) {
			showMessage("readSpecificChar() >>>> Reading is failed!");
			return;
		}
		value = characteristic.getValue();
		if (value != null) {
			Debugger.d(TAG, "readSpecificChar() >>>>  getValue. value[] = " + value[0]);
		}
		return;
	}

	public void writeCharacteristic(UUID charUUID, byte value) {

		BluetoothGattService service = mBluetoothGatt.getService(SERVICE);
		if (service == null) {
			showMessage("writeCharacteristic() >>>> Service not found!");
			return;
		}
		BluetoothGattCharacteristic characteristic = service.getCharacteristic(charUUID);
		if (characteristic == null) {
			showMessage("writeCharacteristic() >>>> Charateristic not found!");
			return;
		}
		boolean status = false;
		int storedLevel = characteristic.getWriteType();
		Debugger.d(TAG, "writeCharacteristic() >>>>  - storedLevel=" + storedLevel);
		characteristic.setValue(value, BluetoothGattCharacteristic.FORMAT_UINT8, 0);
		characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
		status = mBluetoothGatt.writeCharacteristic(characteristic);
		Debugger.d(TAG, "writeCharacteristic() >>>> status=" + status);
	}

	public void writeCharacteristic(UUID charUUID, byte[] value) {

		BluetoothGattService service = mBluetoothGatt.getService(SERVICE);
		if (service == null) {
			showMessage("writeCharacteristic() >>>> Service not found!");
			return;
		}
		BluetoothGattCharacteristic characteristic = service.getCharacteristic(charUUID);
		if (characteristic == null) {
			showMessage("writeCharacteristic() >>>> Charateristic not found!");
			return;
		}
		boolean status = false;
		int storedLevel = characteristic.getWriteType();

		Debugger.d(TAG, "writeCharacteristic() >>>> storedLevel=" + storedLevel);

		characteristic.setValue(value);
		characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
		status = mBluetoothGatt.writeCharacteristic(characteristic);
		Debugger.d(TAG, "writeCharacteristic() >>>> status=" + status);
	}

	private void updateFirmware(BluetoothDevice device) {

	}

	public static void swapArray(byte[] array) {
		for (int i = 0; i < (int) (array.length / 2); i++) {
			byte temp = array[i];
			array[i] = array[array.length - i - 1];
			array[array.length - i - 1] = temp;
		}
	}
}
