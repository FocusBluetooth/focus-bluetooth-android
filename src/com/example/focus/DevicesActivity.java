package com.example.focus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class DevicesActivity extends Activity {

	// ListView lvDevices;

	private BluetoothAdapter mBtAdapter;
	private TextView mEmptyList;
	public static final String TAG = "DeviceListActivity";
	private WorkableService mService = null;
	List<BluetoothDevice> deviceList;
	private BluetoothManager mBluetoothManager;
	private DeviceAdapter deviceAdapter;
	private ServiceConnection onService = null;

	String[] devices = new String[] {};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Debugger.d(TAG, "onCreate");
		mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBtAdapter = mBluetoothManager.getAdapter();
		// getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE,
		// R.layout.title_bar);
		setContentView(R.layout.device_list);


		// getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE,
		// R.layout.title_bar);
		setContentView(R.layout.device_list);


		onService = new ServiceConnection() {
			public void onServiceConnected(ComponentName className, IBinder rawBinder) {
				mService = ((WorkableService.LocalBinder) rawBinder).getService();
				if (mService != null) {
					mService.setDeviceListHandler(mHandler);
				}
				populateList();
			}



			public void onServiceDisconnected(ComponentName classname) {
				mService = null;
			}
		};



		// start service, if not already running (but it is)
		startService(new Intent(this, WorkableService.class));
		Intent bindIntent = new Intent(this, WorkableService.class);
		bindService(bindIntent, onService, Context.BIND_AUTO_CREATE);

		// mBtAdapter = BluetoothAdapter.getDefaultAdapter();
		mEmptyList = (TextView) findViewById(R.id.empty);
		Button cancelButton = (Button) findViewById(R.id.btn_cancel);
		cancelButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				finish();
			}
		});

	}

	// @Override
	// public void onItemClick(AdapterView<?> parent, View view, int position,
	// long id) {
	//
	// Toast.makeText(this, "Device: " + ((TextView)view).getText(),
	// Toast.LENGTH_SHORT).show();
	// }
	//

	private Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			// Gatt device found message.
			case WorkableService.GATT_DEVICE_FOUND_MSG:
				Bundle data = msg.getData();
				final BluetoothDevice device = data.getParcelable(BluetoothDevice.EXTRA_DEVICE);
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						addDevice(device);
					}
				});
				break;
			default:
				super.handleMessage(msg);
			}
		}
	};


	private void populateList() {
		/* Initialize device list container */
		Debugger.d(TAG, "populateList");
		deviceList = new ArrayList<BluetoothDevice>();
		deviceAdapter = new DeviceAdapter(this, deviceList);
		// devRssiValues = new HashMap<String, Integer>();


		ListView newDevicesListView = (ListView) findViewById(R.id.new_devices);
		newDevicesListView.setAdapter(deviceAdapter);
		newDevicesListView.setOnItemClickListener(mDeviceClickListener);

		Set<BluetoothDevice> pairedDevices = mBtAdapter.getBondedDevices();
		for (BluetoothDevice pairedDevice : pairedDevices)
			addDevice(pairedDevice);

		mService.scan(true);
	}

	private void addDevice(BluetoothDevice device) {
		boolean deviceFound = false;

		for (BluetoothDevice listDev : deviceList) {
			if (listDev.getAddress().equals(device.getAddress())) {
				deviceFound = true;
				break;
			}
		}
		// devRssiValues.put(device.getAddress(), rssi);
		if (!deviceFound) {
			mEmptyList.setVisibility(View.GONE);
			deviceList.add(device);
			deviceAdapter.notifyDataSetChanged();
		}
	}


	@Override
	public void onStart() {
		super.onStart();
		Debugger.d(TAG, "onStart mService= " + mService);


		IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
		filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
		filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
		this.registerReceiver(mReceiver, filter);

	}

	@Override
	public void onStop() {
		super.onStop();
		unregisterReceiver(mReceiver);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		unbindService(onService);
		mService.scan(false);
	}


	private OnItemClickListener mDeviceClickListener = new OnItemClickListener() {
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			BluetoothDevice device = deviceList.get(position);
			if (mBluetoothManager.getConnectionState(device,BluetoothGatt.GATT_SERVER) == BluetoothProfile.STATE_CONNECTED) {
				Debugger.i(TAG, "connected device");
				showMessage("device already connected");
				return;
			}
			mService.scan(false);
			Bundle b = new Bundle();
			b.putString(BluetoothDevice.EXTRA_DEVICE, deviceList.get(position).getAddress());


			Intent result = new Intent();
			result.putExtras(b);

			setResult(Activity.RESULT_OK, result);
			finish();
		}
	};

	/**
	 * The BroadcastReceiver that listens for discovered devices and changes the
	 * title when discovery is finished.
	 */
	private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();

			if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
				setProgressBarIndeterminateVisibility(false);
				setTitle(R.string.select_device);
				if (deviceList.size() == 0) {
					mEmptyList.setText(R.string.no_ble_devices);
				}
			}
			if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
				if (!mBtAdapter.isEnabled())
					finish();
			}
		}
	};

	class DeviceAdapter extends BaseAdapter {
		Context context;
		List<BluetoothDevice> devices;
		LayoutInflater inflater;

		public DeviceAdapter(Context context, List<BluetoothDevice> devices) {
			this.context = context;
			inflater = LayoutInflater.from(context);
			this.devices = devices;
		}

		@Override
		public int getCount() {
			return devices.size();
		}

		@Override
		public Object getItem(int position) {
			return devices.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ViewGroup vg;

			if (convertView != null) {
				vg = (ViewGroup) convertView;
			} else {
				vg = (ViewGroup) inflater.inflate(R.layout.device_element, null);
			}

			BluetoothDevice device = devices.get(position);
			final TextView tvadd = ((TextView) vg.findViewById(R.id.address));
			final TextView tvname = ((TextView) vg.findViewById(R.id.name));
			final TextView tvpaired = (TextView) vg.findViewById(R.id.paired);


			tvname.setText(device.getName());
			tvadd.setText(device.getAddress());
			if (device.getBondState() == BluetoothDevice.BOND_BONDED) {
				Debugger.i(TAG, "device::" + device.getName());
				tvname.setTextColor(Color.GRAY);
				tvadd.setTextColor(Color.GRAY);
				tvpaired.setTextColor(Color.GRAY);
				tvpaired.setVisibility(View.VISIBLE);
				tvpaired.setText(R.string.paired);
			} else {
				tvname.setTextColor(Color.WHITE);
				tvadd.setTextColor(Color.WHITE);
				tvpaired.setVisibility(View.GONE);




			}
			try
			{
			int state =  mBluetoothManager.getConnectionState(device, BluetoothGatt.GATT);
			if (state == BluetoothProfile.STATE_CONNECTED) {
				Debugger.i(TAG, "connected device::" + device.getName());
				tvname.setTextColor(Color.WHITE);
				tvadd.setTextColor(Color.WHITE);
				tvpaired.setVisibility(View.VISIBLE);
				tvpaired.setText(R.string.connected);
			}
			}
			catch(Exception ex)
			{
				Debugger.e(TAG, ex.getMessage());
			}
			
			// else if (mService.mBluetoothGattServer.getConnectionState(device)
			// == BluetoothProfile.STATE_CONNECTED) {
			// Log.i(TAG, "connected device::gatt server"+device.getName());
			// tvname.setTextColor(Color.WHITE);
			// tvadd.setTextColor(Color.WHITE);
			// tvpaired.setVisibility(View.VISIBLE);
			// tvpaired.setText(R.string.connected);
			// }
			return vg;
		}
	}


	private void showMessage(String msg) {
		Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
	}
}
