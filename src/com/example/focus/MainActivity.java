package com.example.focus;

import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Random;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.focus.circularPB.CircularProgressBar;
import com.testflightapp.lib.TestFlight;

public class MainActivity extends Activity implements OnClickListener {

	// **********************************************RotaryKnob**********************************************

	private ImageView ivGauge;
	private TextView tvValue;
	private TextView tvText;
	private RelativeLayout diagramLayout;
	private ImageView ivGaugeDiagram;

	private DrawDiagram dd;

	private ImageView ivKnob;

	// constants

	static final float initRotateAngle = 190f;

	static final float angleDiapazon = 340f;

	// measure of value = 10 seconds
	static final float minValue = 30f; // 5 minutes
	static final float maxValue = 240f; // 40 minutes

	static final long measure = 10000L; // 10 seconds = 10000 milliseconds

	// constants

	private static Bitmap imageOriginal, imageScaled;
	private static Matrix matrix;

	private int dialerHeight, dialerWidth;

	private double startAngle;
	private double currentAngle;

	// division of scale
	private float division;

	private float startValue = minValue;
	private float currentValue = minValue;

	private SimpleDateFormat dateFormat;

	private boolean knobOn = false;

	private float topY;

	private float bottomY;

	private float touchX;

	private float touchY;

	private float touchLayoutX;

	private float touchLayoutY;

	private boolean isRotate = false;

	// **********************************************RotaryKnob**********************************************

	private enum State {
		CONNECTED, DISCONNECTED, WORKING, WAITING, KNOB_ROTATING, BONDED, UNBONDED
	};

	private State previousState;
	private State currentState = State.DISCONNECTED;
	public static final String TAG = "MainActivity";

	private static final int REQUEST_ENABLE_BT = 2;

	ImageButton ibSettings;
	ImageButton ibBT;
	ImageButton ibChat;

	ImageView ivBattery;

	TextView device;
	Button discoverServices;
	Button connect;
	Button disconnect;
	Button unpair;
	private static boolean WITH_BUTTONS = true;

	// **********************************************Gauge
	// Start*********************************************
	private static final long SECOND = 1000;
	private static final int DEGREES_IN_CIRCLE = 360;
	private static final float ONE_HOUR = 360f;

	private static final int START_ANGLE = 109;
	private static final int END_ANGLE = 431;
	ImageView gauge;

	// **********************************************Gauge
	// Start*********************************************

	Timer timer;
	ChangeProgress task;
	Intent intent;

	private WorkableService mService = null;
	private BluetoothDevice mDevice = null;
	private BluetoothAdapter mBtAdapter = null;
	private SharedPreferences mSharedPreferences = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.main);

		// **********************************************RotaryKnob**********************************************

		initializeRotatyKnob();

		// **********************************************RotaryKnob**********************************************

		ibSettings = (ImageButton) findViewById(R.id.ibSettings);
		ibBT = (ImageButton) findViewById(R.id.ibBT);
		ibChat = (ImageButton) findViewById(R.id.ibChat);

		ivBattery = (ImageView) findViewById(R.id.ivBattery);

		device = (TextView) findViewById(R.id.textView2);
		discoverServices = (Button) findViewById(R.id.button1);
		connect = (Button) findViewById(R.id.button2);
		disconnect = (Button) findViewById(R.id.button3);
		unpair = (Button) findViewById(R.id.button4);

		if (WITH_BUTTONS) {
			connect.setOnClickListener(this);
			disconnect.setOnClickListener(this);
			discoverServices.setOnClickListener(this);
			unpair.setOnClickListener(this);
		} else {
			connect.setVisibility(View.INVISIBLE);
			discoverServices.setVisibility(View.INVISIBLE);
			device.setVisibility(View.INVISIBLE);
			disconnect.setVisibility(View.INVISIBLE);
			unpair.setVisibility(View.INVISIBLE);
		}

		// // --------------------------------------------------------------

		ibSettings.setOnClickListener(this);
		ibBT.setOnClickListener(this);
		ibChat.setOnClickListener(this);
		final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
		mBtAdapter = bluetoothManager.getAdapter();
		if (mBtAdapter == null) {
			Toast.makeText(this, "OnCreate() >>>> Bluetooth is not available",
					Toast.LENGTH_LONG).show();
			finish();
			return;
		}
		init();
		setState(State.DISCONNECTED);
		if (Debugger.TESTFLIGHT) {
			// initialize the TestFlight
			Application app = getApplication();
			TestFlight.takeOff(app, Debugger.TESTFLIGHT_TOKEN);
		}
	}

	/**
	 * Do actions when knob turn on or turn off
	 */
	private void knobStateChange() {

		// Debug.startMethodTracing("focus");
		if (knobOn == true) {
			Debugger.d(TAG, "KnobStateChange() >>>> KnobOn");

			// displayImageView();
			// Convert time to byte array and write into array
			short time = (short) (currentValue * 10);
			byte[] value = ByteBuffer.allocate(2).putShort(time).array();
			//	putInt(progress).array();
			byte temp = value[0];
			value[0] = value[1];
			value[1] = temp;
			mService.writeCharacteristic(WorkableService.TIME_IN_ACTIVE_MODE,
					value);
			setState(State.WORKING);
			// changeOrangeGauge(false);
			// knob is turned on;
			final CircularProgressBar progress;
			progress = createProgressBar();
			// this should be placed here. without it, displays incorrect
			gauge.setImageDrawable(getApplicationContext().getResources()
					.getDrawable(R.drawable.gauge_blue_empty));
			timer = new Timer();
			final int delay = (int) (currentValue * measure / (END_ANGLE - START_ANGLE));
			if (timer != null) {
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						timer.schedule(new ReadActualLevelCharacteristic(),
								CommonValues.READ_TIME, CommonValues.READ_TIME);
						task = new ChangeProgress(gauge, progress, START_ANGLE,
								END_ANGLE, true);
						timer.scheduleAtFixedRate(task, delay, delay);
						// change timer label every second
						timer.scheduleAtFixedRate(new ChangeTimeLabel(tvValue,
								(long) currentValue * measure), SECOND, SECOND);
						gauge.setVisibility(View.VISIBLE);
					}
				});

				if (mSharedPreferences != null) {
					int mode = mSharedPreferences.getInt(
							CommonValues.ACTIVE_MODE, 0);
					if (mode >= 0)
						mService.writeCharacteristic(WorkableService.MODE,
								(byte) (mode + 2));
				}
			}
			// startDrawingDiagram();
		} else {
			Debugger.d(TAG, "KnobStateChange() >>>> KnobOff");
			if (timer != null) {
				task.setCanceled(true);
				// timer.cancel(); // will do in setState.
				Debugger.d(TAG, "KnobStateChange() >>>> timer.Cancel()");
			}
			setState(State.CONNECTED);
			// resetTime();
			if (mService != null) {
				Debugger.d(TAG,
						"KnobStateChange() >>>> Set device mode to PAIRED(1)");
				mService.writeCharacteristic(WorkableService.MODE,
						(byte) CommonValues.PAIRED);
			} else {
				Debugger.e(TAG,
						"KnobStateChange() >>>> KnobOff() -  mService == null");
			}
			// stopDrawingDiagram();
		}
	}

	// two methods to emulate diagram drawing
	// private void stopDrawingDiagram() {
	// // TODO Auto-generated method stub
	// if (timer2 != null)
	// timer2.cancel();
	// }
	//
	// Timer timer2 = null;
	//
	// private void startDrawingDiagram() {
	// // TODO Auto-generated method stub
	// if (timer2 == null)
	// timer2 = new Timer();
	// timer2.scheduleAtFixedRate(new TimerTask() {
	//
	// @Override
	// public void run() {
	// // TODO Auto-generated method stub
	// Random rand = new Random();
	// short value = (short) rand.nextInt(150);
	// dd.drawLine(value);
	// }
	// }, 0, 100);
	// }

	protected CircularProgressBar createProgressBar() {
		final CircularProgressBar progress;
		Drawable d = gauge.getDrawable();
		int width = gauge.getWidth();
		int height = gauge.getHeight();
		d.setBounds(0, 0, width, height);

		Bitmap b = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
		Canvas c = new Canvas(b);
		d.draw(c);
		progress = new CircularProgressBar(b, (int) width / 2,
				(int) height / 2, (int) width / 2, (int) (width * 0.5 * 0.6));
		return progress;
		// return null;
	}

	private void init() {
		Intent bindIntent = new Intent(this, WorkableService.class);
		startService(bindIntent);
		bindService(bindIntent, mServiceConnection, Context.BIND_AUTO_CREATE);

		IntentFilter filter = new IntentFilter();
		filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
		filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
		this.registerReceiver(proximityStatusChangeReceiver, filter);
		mSharedPreferences = PreferenceManager
				.getDefaultSharedPreferences(getApplicationContext());
	}

	@Override
	public void onClick(View v) {

		switch (v.getId()) {

		case R.id.ibSettings:

			// Button Settings
			intent = new Intent(this, SettingsActivity.class);
			if (mDevice != null) {
				intent.putExtra(BluetoothDevice.EXTRA_DEVICE,
						mDevice.getAddress());
			}
			startActivity(intent);
			break;

		case R.id.ibBT:

			// Button BT
			if (!mBtAdapter.isEnabled()) {
				Debugger.e(TAG, "onClick() >>>> BT not enabled yet");
				intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
				startActivityForResult(intent, REQUEST_ENABLE_BT);
			} else {
				if (mDevice == null) {
					Intent newIntent = new Intent(this, DevicesActivity.class);
					startActivityForResult(newIntent,
							CommonValues.REQUEST_SELECT_DEVICE);
				} else {
					if (currentState == State.CONNECTED && mService != null) {
						Debugger.d(TAG,
								"onClick() >>>>  Bluetooth Button. state - Connected. Disconnecting...");
						mService.disconnect();
						mDevice = null;
					}
					if (mDevice != null && mService != null
							&& currentState != State.CONNECTED) {
						mService.connect(mDevice);
					}
				}
			}

			break;

		case R.id.ibChat:

			// Button Chat

			break;
		case R.id.button1:
			mService.discoverServices();
			break;

		case R.id.button2:
			mService.connect(mDevice);
			break;
		case R.id.button3:
			mService.disconnect();
			break;
		case R.id.button4:
			removeBond(mDevice);
		default:
			break;
		}
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {

		case CommonValues.REQUEST_SELECT_DEVICE:
			if (resultCode == Activity.RESULT_OK && data != null) {
				String deviceAddress = data
						.getStringExtra(BluetoothDevice.EXTRA_DEVICE);
				if (mBtAdapter != null) {
					mDevice = mBtAdapter.getRemoteDevice(deviceAddress);
					// mService.connect(mDevice);
					int state = mDevice.getBondState();
					if (state == BluetoothDevice.BOND_NONE) {
						Debugger.d(TAG,
								"onActivityResult() >>>> Device is not BONDED.. PAIRING...");
						pairDevice(mDevice);
					} else

					if (state == BluetoothDevice.BOND_BONDED) {
						Debugger.d(TAG,
								"onActivityResult() >>>> Device is already bonded. Trying to connect");
						if (mService.connect(mDevice))
							Debugger.d(TAG,
									"onActivityResult() >>>> Connecting to device successful. ");
					}
				} else {
					Debugger.e(TAG,
							"onActivityResult() >>>>  BluetoothAdapter = null.");
					finish();
				}
				Debugger.d(TAG,
						"onActivityResult() >>>> ... onActivityResultdevice.address=="
								+ mDevice + "mserviceValue" + mService);
				// setUiState();

			}
			break;
		case REQUEST_ENABLE_BT:
			// When the request to enable Bluetooth returns
			if (resultCode == Activity.RESULT_OK) {
				Toast.makeText(this, "Bluetooth has turned on ",
						Toast.LENGTH_SHORT).show();

			} else {
				// User did not enable Bluetooth or an error occurred
				Debugger.d(TAG, "onActivityResult() >>>>  BT not enabled");
				Toast.makeText(this, "Problem in BT Turning ON ",
						Toast.LENGTH_SHORT).show();
				finish();
			}
			break;
		case CommonValues.REQUEST_PAIRING:
			Debugger.d(TAG,
					" onActivityResult() >>>> REQUEST_PAIRING. ResultCode = "
							+ resultCode);
			break;
		default:
			Debugger.e(TAG, " onActivityResult() >>>> wrong request code");
			break;
		}
	}

	private ServiceConnection mServiceConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className,
				IBinder rawBinder) {
			mService = ((WorkableService.LocalBinder) rawBinder).getService();
			Debugger.d(TAG, "onServiceConnected() >>>> mService= " + mService);
			if (!mService.initialize()) {
				Debugger.e(TAG,
						"onServiceConnected() >>>> Unable to initialize Bluetooth");
				finish();
			}
			mService.setActivityHandler(mHandler);
		}

		public void onServiceDisconnected(ComponentName classname) {
			mService.disconnect();
			mService = null;
		}
	};

	private Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			final Bundle data = msg.getData();
			switch (msg.what) {

			case WorkableService.CONNECT_MSG:
				setState(State.CONNECTED);
				mService.readSpecificChar(WorkableService.BATTERY_LEVEL);
				// String text = data.getString(BluetoothDevice.EXTRA_NAME);
				// device.setText("Device: " + text);
				// pairDevice(mDevice);
				// initialize Knob for rotating and touching
				// ivKnob.setOnTouchListener(new MyOnTouchListener());
				break;
			case WorkableService.READY_MSG:
				setState(State.CONNECTED);
				mService.readSpecificChar(WorkableService.BATTERY_LEVEL);
				try {
					Thread.sleep(200);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				mService.readSpecificChar(WorkableService.ACTUAL_LEVEL);
				break;
			case WorkableService.DISCONNECT_MSG:
				if (timer != null)
					timer.cancel();

				// setState(States.DISCONNECTED);
				String message = data.getString(BluetoothDevice.EXTRA_NAME);
				if (message != null) {
					Toast.makeText(getApplicationContext(),
							R.string.device_disconnected + ":" + message,
							Toast.LENGTH_SHORT).show();
				} else
					Toast.makeText(getApplicationContext(),
							R.string.device_disconnected, Toast.LENGTH_SHORT)
							.show();
				break;
			case WorkableService.VALUE_MSG:
				UUID name = UUID.fromString(data
						.getString(WorkableService.EXTRA_CHARACTERISTIC));
				if (name.equals(WorkableService.BATTERY_LEVEL)) {
					byte value = data.getByte(name.toString());
					if (value >= CommonValues.BATTERY_ALMOST_FULL)
						// set image baterry_full
						ivBattery.setImageResource(R.drawable.battery_full);
					else if (value < CommonValues.BATTERY_ALMOST_FULL
							&& value > CommonValues.BATTERY_MORE_HALF)
						// set image battery_80
						ivBattery.setImageResource(R.drawable.battery_80);
					else if (value < CommonValues.BATTERY_MORE_HALF
							&& value > CommonValues.BATTERY_HALF)
						// set image baterry_60;
						ivBattery.setImageResource(R.drawable.battery_60);
					else if (value < CommonValues.BATTERY_HALF
							&& value > CommonValues.BATTERY_LESS_HALF)
						// set image battery_40
						ivBattery.setImageResource(R.drawable.battery_40);
					else if (value < CommonValues.BATTERY_LESS_HALF
							&& value > CommonValues.BATTERY_ALMOST_EMPTY)
						// set image battery_20
						ivBattery.setImageResource(R.drawable.battery_20);
					else if (value < CommonValues.BATTERY_ALMOST_EMPTY
							&& value > CommonValues.BATTERY_EMPTY)
						// set image battery_empty
						ivBattery.setImageResource(R.drawable.battery_empty);
				}
				if (name.equals(WorkableService.ACTUAL_LEVEL)) {
					byte value = data.getByte(name.toString());
					// Toast.makeText(getApplicationContext(), "value = " +
					// value, Toast.LENGTH_SHORT).show();
					Debugger.d(TAG, "Handler() >>>> Actual Level  = " + value);
					dd.drawLine((short) value);
				}
				if (name.equals(WorkableService.TIME_IN_ACTIVE_MODE)) {
					byte[] value = data.getByteArray(name.toString());
					Debugger.d(TAG, "Handler() >>>> time in active mode   = "
							+ value[0] + value[1]);
				}
				if (name.equals(WorkableService.PIN_CODE)) {
					byte[] value = data.getByteArray(name.toString());
					Debugger.d(TAG, "Handler() >>>> pincode   = " + value[0]
							+ value[1] + value[2] + value[3]);
				}
				if (name.equals(WorkableService.CURRENT_OFFSET)) {
					byte value = data.getByte(name.toString());
					Debugger.d(TAG, " Handler() >>>> Current Offset   = "
							+ value);
				}
				if (name.equals(WorkableService.ELECTRODES_MODE)) {
					byte value = data.getByte(name.toString());
					Debugger.d(TAG, " Handler() >>>> Electrodes modes   = "
							+ value);
				}
				break;
			case WorkableService.CHARACTERISTIC_CHANGED:
				UUID uuid = UUID.fromString(data
						.getString(WorkableService.EXTRA_CHARACTERISTIC));
				if (uuid.equals(WorkableService.ACTUAL_LEVEL))
					mService.readSpecificChar(WorkableService.ACTUAL_LEVEL);
				if (uuid.equals(WorkableService.BATTERY_LEVEL))
					mService.readSpecificChar(WorkableService.BATTERY_LEVEL);
				break;
			default:
				super.handleMessage(msg);
			}
		}

	};

	public void pairDevice(BluetoothDevice device) {
		// startActivity(intent);
		// connect(device);
		Debugger.d(TAG,
				"pairDevice() >>>> Pairing to device:" + device.getAddress());
		try {
			createBond(device);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private final BroadcastReceiver proximityStatusChangeReceiver = new BroadcastReceiver() {

		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();

			final Intent mIntent = intent;
			if (action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)) {
				BluetoothDevice device = mIntent
						.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				int state = mIntent.getIntExtra(
						BluetoothDevice.EXTRA_BOND_STATE, 0);
				// int prevState =
				// mIntent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE,
				// 0);
				Debugger.d(TAG,
						"onReceive()  >>>> BluetoothDevice.ACTION_BOND_STATE_CHANGED. State = "
								+ state);
				if (device.equals(mDevice)) {
					if (state == BluetoothDevice.BOND_BONDED) {
						setState(State.BONDED);
						// DONT connect right after bonding. Device should be
						// disconnected first.

						// mService.disconnect();
						if (mService.connect(device)) {
							setState(State.CONNECTED);
							Toast.makeText(context, "Device was bonded",
									Toast.LENGTH_LONG).show();
						}
					}

					if (state == BluetoothDevice.BOND_NONE)
						setState(State.UNBONDED);
				}
			}
			if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
				final int state = mIntent.getIntExtra(
						BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
				Debugger.d(TAG,
						"onReceive() >>>> BluetoothAdapter.ACTION_STATE_CHANGED."
								+ "state is" + state);
				runOnUiThread(new Runnable() {
					public void run() {
						// TODO make some function that
						// setState(States.DISCONNECTED);
					}
				});
			}
		}
	};

	@Override
	protected void onResume() {
		if (mService != null)
			mService.setActivityHandler(mHandler);

		TestFlight.endSession();
		super.onResume();
	}

	@Override
	public void onDestroy() {
		Debugger.d(TAG, "onDestroy()");
		try {
			unregisterReceiver(proximityStatusChangeReceiver);

			unbindService(mServiceConnection);
			stopService(new Intent(this, WorkableService.class));
			if (timer != null)
				timer.cancel();
		} catch (Exception ignore) {
			Debugger.e(TAG, ignore.toString());
		}
		super.onDestroy();
	}

	@Override
	public void onBackPressed() {

		// super.onBackPressed();
		if (currentState != State.WORKING)
			new AlertDialog.Builder(this)
					.setIcon(android.R.drawable.ic_dialog_alert)
					.setTitle(R.string.popup_title)
					.setMessage(R.string.popup_message)
					.setPositiveButton(R.string.popup_yes,
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog,
										int which) {
									finish();
								}
							}).setNegativeButton(R.string.popup_no, null)
					.show();
	}

	protected void setState(final State state) {

		previousState = currentState;
		currentState = state;

		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				switch (state) {
				case BONDED:
					tvText.setVisibility(View.VISIBLE);
					tvText.setText(R.string.bonded);
					tvValue.setVisibility(View.INVISIBLE);
					tvValue.setText(dateFormat.format(new Date(
							(long) currentValue * measure)));
					Debugger.d(TAG, "setState() >>>> State changed to bonded.");

				case UNBONDED:
					setState(State.DISCONNECTED);
					tvText.setText(R.string.unbonded);
					Debugger.d(TAG,
							"setState() >>>> State changed to unbonded.");

				case CONNECTED:

					tvText.setVisibility(View.VISIBLE);
					tvText.setText(R.string.press_message);
					tvValue.setVisibility(View.INVISIBLE);
					tvValue.setText(dateFormat.format(new Date(
							(long) currentValue * measure)));

					ivKnob.setOnTouchListener(new MyOnTouchListener());

					gauge.setVisibility(View.INVISIBLE);
					ivGauge.setImageDrawable(getApplicationContext()
							.getResources().getDrawable(
									R.drawable.gauge_blue_bg));

					if (timer != null)
						timer.cancel();

					Debugger.d(TAG,
							"setState() >>>> State changed to connected.");
					break;
				case DISCONNECTED:

					tvText.setVisibility(View.VISIBLE);
					tvText.setText(R.string.disconnected);
					tvValue.setVisibility(View.INVISIBLE);
					ivBattery.setImageDrawable(getApplicationContext()
							.getResources().getDrawable(
									R.drawable.battery_empty));

					setKnobState(false);
					gauge.setVisibility(View.INVISIBLE);
					ivGauge.setImageDrawable(getApplicationContext()
							.getResources().getDrawable(
									R.drawable.gauge_blue_bg));

					ivKnob.setOnTouchListener(null);
					Debugger.d(TAG,
							"setState() >>>> State changed to disconnected.");
					if (previousState == State.BONDED) {
						Debugger.d(TAG,
								" setState() >>>> Previous state == State.BONDED");

						// if state was changed from Bonded, try reconnect.
						if (mDevice != null && mService != null)
							mService.connect(mDevice);
					}
					break;

				case WAITING:

					tvValue.setText(dateFormat.format(new Date((long) ONE_HOUR
							* measure)));
					ivKnob.setOnTouchListener(null);
					ivGauge.setImageDrawable(getApplicationContext()
							.getResources().getDrawable(
									R.drawable.gauge_orange_bg2));
					gauge.setImageDrawable(getApplicationContext()
							.getResources().getDrawable(
									R.drawable.gauge_orange_empty2));
					gauge.setVisibility(View.VISIBLE);

					Debugger.d(TAG, "setState() >>>> State changed to waiting.");
					break;
				case WORKING:

					tvText.setVisibility(View.INVISIBLE);
					tvValue.setVisibility(View.VISIBLE);

					ivGauge.setImageDrawable(getApplicationContext()
							.getResources().getDrawable(
									R.drawable.gauge_blue_bg2));
					gauge.setImageDrawable(getApplicationContext()
							.getResources().getDrawable(
									R.drawable.gauge_blue_empty));
					gauge.setVisibility(View.VISIBLE);

					Debugger.d(TAG, "setState() >>>> State changed to working.");
					break;
				case KNOB_ROTATING:
					tvText.setVisibility(View.INVISIBLE);
					tvText.setText(R.string.press_message);
					tvValue.setVisibility(View.VISIBLE);

					Debugger.d(TAG,
							" setState() >>>> State changed to Knob_rotating.");
					break;
				}

			}
		});
	}

	/** Reads Actual Level Characteristic every 200 milliseconds. */
	class ReadActualLevelCharacteristic extends TimerTask {

		@Override
		public void run() {
			if (mService != null)
				mService.readSpecificChar(WorkableService.ACTUAL_LEVEL);
			else {
				Debugger.d(
						TAG,
						"Timer. ReadActualLevelCharacteristic cannot be started. Make sure Service is not null");
			}
		}

	}

	/** Changes Progress - fade in/ out one degree in the circularProgressBar. */
	class ChangeProgress extends TimerTask {
		CircularProgressBar progress;
		int current;
		int end_angle;
		ImageView gauge;
		boolean clockwise = false;
		boolean isCanceled;

		public ChangeProgress(ImageView g, CircularProgressBar p, int start,
				int end, boolean clock) {
			progress = p;
			current = start;
			end_angle = end;
			gauge = g;
			clockwise = clock;
			isCanceled = false;
		}

		public void setCanceled(boolean value) {
			isCanceled = value;
			Debugger.d(TAG, "ChangeProgress >>>>  setCanceled()");
		}

		@Override
		public void run() {
			if (!isCanceled) {
				if (clockwise) {
					if (current < end_angle) {
						progress.disappearSector(current % DEGREES_IN_CIRCLE,
								(current % DEGREES_IN_CIRCLE) + 1, 0);
						current++;
						displayImageView();
					} else {
						// Debug.stopMethodTracing();
						this.cancel();
						setState(State.WAITING);
						startNewTimer();
					}
				}

				else {
					if (current > end_angle) {
						if (current % DEGREES_IN_CIRCLE == 0)
							progress.disappearSector(DEGREES_IN_CIRCLE - 1,
									DEGREES_IN_CIRCLE, 0);
						else
							progress.disappearSector(
									(current % DEGREES_IN_CIRCLE) - 1, current
											% DEGREES_IN_CIRCLE, 0);
						current--;
						displayImageView();
					} else {
						// this.cancel(); // don't need this, because timer is
						// canceled in setState() method
						setState(State.CONNECTED);

					}
				}
			}
		}

		protected void displayImageView() {
			runOnUiThread(new Runnable() {

				@Override
				public void run() {
					if (!isCanceled) {
						// height sets as the width, because if set real
						// width - it display the image not correct. For now
						// have no idea why this happens.
						int height = progress.getBitmap().getHeight();
						gauge.setImageBitmap(Bitmap.createScaledBitmap(
								progress.getBitmap(), height, height, false));
						Debugger.d(TAG,
								"ChangeProgress >>>> DisplayImageView()");
					}
				}
			});
		}
	}

	/** Changes Label */
	class ChangeTimeLabel extends TimerTask {
		TextView label;
		long currentTime;
		long min;

		public ChangeTimeLabel(TextView l, long ct) {
			label = l;
			currentTime = ct;
			min = (long) minValue * measure;
		}

		@Override
		public void run() {
			if (currentTime >= 0) {
				runOnUiThread(new Runnable() {

					@Override
					public void run() {
						label.setText(dateFormat.format(new Date(currentTime)));
						currentTime -= SECOND;
					}
				});

			} else {
				this.cancel();

			}
		}

	}

	public void startNewTimer() {
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				Debugger.d(TAG, "startNewTimer");

				setKnobState(false);
				ivKnob.setOnTouchListener(null);
				Debugger.d(TAG, "StartNewTimer()->Set device mode to PAIRED(1)");
				mService.writeCharacteristic(WorkableService.MODE,
						(byte) CommonValues.PAIRED);

				final CircularProgressBar progress = createProgressBar();
				if (timer != null)
					timer.cancel();
				timer = new Timer();
				// 360 - One hour to wait until start working again
				final int delay = (int) (ONE_HOUR * measure / (END_ANGLE - START_ANGLE));
				if (timer != null) {
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							// TODO change to real after testing

							timer.scheduleAtFixedRate(new ChangeProgress(gauge,
									progress, END_ANGLE, END_ANGLE - 2, false),
									delay, delay);
							timer.scheduleAtFixedRate(new ChangeTimeLabel(
									tvValue, 360 * measure), SECOND, SECOND);
						}
					});
				}
			}
		});
	}

	// ****************************************************RotatyKnob*************************************************************
	protected void initializeIvKnob() {

		ivKnob.getViewTreeObserver().addOnGlobalLayoutListener(
				new OnGlobalLayoutListener() {

					@Override
					public void onGlobalLayout() {

						// method called more than once, but the values only
						// need to be
						// initialized one time
						if (dialerHeight == 0 || dialerWidth == 0) {

							dialerHeight = ivKnob.getHeight();
							dialerWidth = ivKnob.getWidth();

							// resize
							Matrix resize = new Matrix();

							resize.postScale(
									(float) Math.min(dialerWidth, dialerHeight)
											/ (float) imageOriginal.getWidth(),
									(float) Math.min(dialerWidth, dialerHeight)
											/ (float) imageOriginal.getHeight());

							imageScaled = Bitmap.createBitmap(imageOriginal, 0,
									0, imageOriginal.getWidth(),
									imageOriginal.getHeight(), resize, false);

							// translate to the image view's center
							float translateX = dialerWidth / 2
									- imageScaled.getWidth() / 2;
							float translateY = dialerHeight / 2
									- imageScaled.getHeight() / 2;

							matrix.postTranslate(translateX, translateY);

							ivKnob.setImageBitmap(imageScaled);
							ivKnob.setImageMatrix(matrix);

							rotateDialer(initRotateAngle);

						} else if (dialerHeight != ivKnob.getHeight()) {

							dialerHeight = ivKnob.getHeight();
							dialerWidth = ivKnob.getWidth();

							// resize
							Matrix resize = new Matrix();

							resize.postScale(
									(float) Math.min(dialerWidth, dialerHeight)
											/ (float) imageOriginal.getWidth(),
									(float) Math.min(dialerWidth, dialerHeight)
											/ (float) imageOriginal.getHeight());

							imageScaled = Bitmap.createBitmap(imageOriginal, 0,
									0, imageOriginal.getWidth(),
									imageOriginal.getHeight(), resize, false);

							// translate to the image view's center
							// float translateX = dialerWidth / 2 -
							// imageScaled.getWidth() / 2;
							// float translateY = dialerHeight / 2 -
							// imageScaled.getHeight() / 2;
							//
							// matrix.postTranslate(translateX, translateY);

							ivKnob.setImageBitmap(imageScaled);

							matrix.reset();

							ivKnob.setImageMatrix(matrix);

							rotateDialer(initRotateAngle);

							rotateDialer((currentValue - minValue)
									* angleDiapazon / (maxValue - minValue));

							if (knobOn == true) {
								ivKnob.setY(tvValue.getY() + ivKnob.getHeight());
							}
						}
					}
				});
	}

	// **********************************************RotaryKnob**********************************************

	protected void initializeRotatyKnob() {

		ivGauge = (ImageView) findViewById(R.id.ivGauge);
		gauge = (ImageView) findViewById(R.id.gauge_fill);
		// gauge.setVisibility(View.INVISIBLE);
		tvValue = (TextView) findViewById(R.id.tvValue);
		tvText = (TextView) findViewById(R.id.tvText);
		diagramLayout = (RelativeLayout) findViewById(R.id.diagramLayout);
		ivGaugeDiagram = (ImageView) findViewById(R.id.ivGaugeDiagram);
		ivKnob = (ImageView) findViewById(R.id.ivKnob);
		ivBattery = (ImageView) findViewById(R.id.ivBattery);

		initializeIvKnob();

		// int pixels = (int) (dp * scale + 0.5f);

		float scale = getResources().getDisplayMetrics().density;

		// Checks the orientation of the screen
		if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {

			((RelativeLayout.LayoutParams) ivGauge.getLayoutParams()).height = (int) (210 * scale + 0.5f);
			((RelativeLayout.LayoutParams) ivGauge.getLayoutParams()).bottomMargin = (int) (10 * scale + 0.5f);

			((RelativeLayout.LayoutParams) tvValue.getLayoutParams()).topMargin = (int) (50 * scale + 0.5f);
			tvValue.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);

			((RelativeLayout.LayoutParams) diagramLayout.getLayoutParams()).width = (int) (80 * scale + 0.5f);
			((RelativeLayout.LayoutParams) diagramLayout.getLayoutParams()).height = (int) (35 * scale + 0.5f);

			((RelativeLayout.LayoutParams) ivGaugeDiagram.getLayoutParams()).height = (int) (210 * scale + 0.5f);
			((RelativeLayout.LayoutParams) ivGaugeDiagram.getLayoutParams()).bottomMargin = (int) (10 * scale + 0.5f);

			((RelativeLayout.LayoutParams) ivKnob.getLayoutParams()).height = (int) (90 * scale + 0.5f);
			((RelativeLayout.LayoutParams) ivKnob.getLayoutParams()).width = (int) (90 * scale + 0.5f);

			((RelativeLayout.LayoutParams) ivBattery.getLayoutParams()).height = (int) (20 * scale + 0.5f);
			((RelativeLayout.LayoutParams) ivBattery.getLayoutParams()).width = (int) (20 * scale + 0.5f);
		}

		// DrawDiagram dd = new DrawDiagram(this);

		dd = (com.example.focus.DrawDiagram) findViewById(R.id.drawDiagram);

		// dd.setZOrderOnTop(true);
		// dd.getHolder().setFormat(PixelFormat.TRANSPARENT);

		// load the image only once
		if (imageOriginal == null) {
			imageOriginal = BitmapFactory.decodeResource(getResources(),
					R.drawable.knob_high);
		}

		// initialize the matrix only once
		if (matrix == null) {
			matrix = new Matrix();
		} else {
			// not needed, you can also post the matrix immediately to restore
			// the old state
			matrix.reset();
		}

		division = (maxValue - minValue) / angleDiapazon;

		dateFormat = new SimpleDateFormat("HH:mm:ss", Locale.US);

		dateFormat.setTimeZone(TimeZone.getTimeZone("GMT+00:00"));

		tvValue.setText(dateFormat.format(new Date((long) currentValue
				* measure)));
		tvValue.setVisibility(View.INVISIBLE);
		tvText.setVisibility(View.VISIBLE);
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);

		// int pixels = (int) (dp * scale + 0.5f);

		float scale = getResources().getDisplayMetrics().density;

		// Checks the orientation of the screen
		if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {

			((RelativeLayout.LayoutParams) ivGauge.getLayoutParams()).height = (int) (210 * scale + 0.5f);
			((RelativeLayout.LayoutParams) ivGauge.getLayoutParams()).bottomMargin = (int) (10 * scale + 0.5f);

			((RelativeLayout.LayoutParams) tvValue.getLayoutParams()).topMargin = (int) (50 * scale + 0.5f);
			tvValue.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);

			((RelativeLayout.LayoutParams) diagramLayout.getLayoutParams()).width = (int) (80 * scale + 0.5f);
			((RelativeLayout.LayoutParams) diagramLayout.getLayoutParams()).height = (int) (35 * scale + 0.5f);

			((RelativeLayout.LayoutParams) ivGaugeDiagram.getLayoutParams()).height = (int) (210 * scale + 0.5f);
			((RelativeLayout.LayoutParams) ivGaugeDiagram.getLayoutParams()).bottomMargin = (int) (10 * scale + 0.5f);

			((RelativeLayout.LayoutParams) ivKnob.getLayoutParams()).height = (int) (90 * scale + 0.5f);
			((RelativeLayout.LayoutParams) ivKnob.getLayoutParams()).width = (int) (90 * scale + 0.5f);

			((RelativeLayout.LayoutParams) ivBattery.getLayoutParams()).height = (int) (20 * scale + 0.5f);
			((RelativeLayout.LayoutParams) ivBattery.getLayoutParams()).width = (int) (20 * scale + 0.5f);

		} else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {

			((RelativeLayout.LayoutParams) ivGauge.getLayoutParams()).height = (int) (330 * scale + 0.5f);
			((RelativeLayout.LayoutParams) ivGauge.getLayoutParams()).bottomMargin = (int) (30 * scale + 0.5f);

			((RelativeLayout.LayoutParams) tvValue.getLayoutParams()).topMargin = (int) (80 * scale + 0.5f);
			tvValue.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);

			((RelativeLayout.LayoutParams) diagramLayout.getLayoutParams()).width = (int) (120 * scale + 0.5f);
			((RelativeLayout.LayoutParams) diagramLayout.getLayoutParams()).height = (int) (60 * scale + 0.5f);

			((RelativeLayout.LayoutParams) ivGaugeDiagram.getLayoutParams()).height = (int) (330 * scale + 0.5f);
			((RelativeLayout.LayoutParams) ivGaugeDiagram.getLayoutParams()).bottomMargin = (int) (30 * scale + 0.5f);

			((RelativeLayout.LayoutParams) ivKnob.getLayoutParams()).height = (int) (150 * scale + 0.5f);
			((RelativeLayout.LayoutParams) ivKnob.getLayoutParams()).width = (int) (150 * scale + 0.5f);

			((RelativeLayout.LayoutParams) ivBattery.getLayoutParams()).height = (int) (30 * scale + 0.5f);
			((RelativeLayout.LayoutParams) ivBattery.getLayoutParams()).width = (int) (30 * scale + 0.5f);
		}
	}

	/**
	 * Simple implementation of an {@link OnTouchListener} for registering the
	 * dialer's touch events.
	 */
	private class MyOnTouchListener implements OnTouchListener {

		@Override
		public boolean onTouch(View v, MotionEvent event) {

			switch (event.getAction()) {

			case MotionEvent.ACTION_DOWN:

				topY = tvValue.getY();

				bottomY = tvValue.getY() + ivKnob.getHeight();

				touchX = event.getX();

				touchY = event.getY();

				touchLayoutX = ivKnob.getX() + touchX;

				touchLayoutY = ivKnob.getY() + touchY;

				startAngle = getAngle(event.getX(), event.getY());

				break;

			case MotionEvent.ACTION_MOVE:

				if (knobOn == false) {

					float partX = ivKnob.getWidth() / 4;

					float partY = ivKnob.getHeight() / 4;

					if ((touchLayoutX < tvValue.getX() + partX)
							|| (touchLayoutX > tvValue.getX() + 3 * partX)
							|| (touchLayoutY < tvValue.getY() + partY)
							|| (touchLayoutY > tvValue.getY() + 3 * partY)) {

						currentAngle = getAngle(event.getX(), event.getY());

						getRotateValue();

						isRotate = true;

						break;
					}
				}

				float tempY = ivKnob.getY() + event.getY() - touchY;

				if ((tempY >= topY) && (tempY <= bottomY)) {

					ivKnob.setY(tempY);
				}

				break;

			case MotionEvent.ACTION_UP:

				if (isRotate == true) {

					isRotate = false;

				} else {

					if ((event.getX() < touchX + 10)
							&& (event.getX() > touchX - 10)
							&& (event.getY() < touchY + 10)
							&& (event.getY() > touchY - 10)) {

						if (knobOn == true) {

							ivKnob.setY(topY);

							ivKnob.setBackgroundResource(R.drawable.knob_low);

							knobOn = false;

							knobStateChange();

						} else {

							ivKnob.setY(bottomY);

							ivKnob.setBackgroundResource(R.drawable.knob_low_off);

							knobOn = true;

							knobStateChange();
						}
					} else {

						if ((bottomY - ivKnob.getY()) >= (ivKnob.getY() - topY)) {

							ivKnob.setY(topY);

							if (knobOn == true) {

								ivKnob.setBackgroundResource(R.drawable.knob_low);

								knobOn = false;

								knobStateChange();
							}

						} else {

							ivKnob.setY(bottomY);

							if (knobOn == false) {

								ivKnob.setBackgroundResource(R.drawable.knob_low_off);

								knobOn = true;

								knobStateChange();
							}
						}
					}
				}

				break;
			}

			return true;
		}
	}

	/**
	 * Calculate rotate value of dialer
	 * 
	 */
	private void getRotateValue() {

		/*
		 * Bugs: 1. long rotate to min side when startValue == minValue; 2. long
		 * rotate to max side when startValue == maxValue;
		 */

		if ((startAngle >= 0) && (startAngle <= 90) && (currentAngle >= 270)
				&& (currentAngle < 360)) {

			currentValue += (float) (360 - currentAngle + startAngle)
					* division;

			if (currentValue > maxValue) {

				double t = startAngle - (maxValue - startValue) / division;

				currentAngle = (t >= 0) ? t : 360 + t;

				currentValue = maxValue;
			}

		} else if ((startAngle >= 270) && (startAngle < 360)
				&& (currentAngle >= 0) && (currentAngle <= 90)) {

			currentValue -= (float) (360 - startAngle + currentAngle)
					* division;

			if (currentValue < minValue) {

				double t = startAngle + (startValue - minValue) / division;

				currentAngle = (t < 360) ? t : t - 360;

				currentValue = minValue;
			}

		} else {

			currentValue += (float) (startAngle - currentAngle) * division;

			if (currentValue < minValue) {

				currentAngle = startAngle + (startValue - minValue) / division;

				currentValue = minValue;
			} else if (currentValue > maxValue) {

				currentAngle = startAngle - (maxValue - startValue) / division;

				currentValue = maxValue;
			}
		}

		rotateDialer((float) (startAngle - currentAngle));

		tvValue.setText(dateFormat.format(new Date((long) currentValue
				* measure)));
		if (currentState != State.KNOB_ROTATING)
			setState(State.KNOB_ROTATING);

		startValue = currentValue;

		startAngle = currentAngle;
	}

	/**
	 * @return The angle of the unit circle with the image view's center
	 */
	private double getAngle(double xTouch, double yTouch) {
		double x = xTouch - (dialerWidth / 2d);
		double y = dialerHeight - yTouch - (dialerHeight / 2d);
		switch (getQuadrant(x, y)) {
		case 1:
			return Math.asin(y / Math.hypot(x, y)) * 180 / Math.PI;
		case 2:
			return 180 - Math.asin(y / Math.hypot(x, y)) * 180 / Math.PI;
		case 3:
			return 180 + (-1 * Math.asin(y / Math.hypot(x, y)) * 180 / Math.PI);
		case 4:
			return 360 + Math.asin(y / Math.hypot(x, y)) * 180 / Math.PI;
		default:
			return 0;
		}
	}

	/**
	 * @return The selected quadrant.
	 */
	private static int getQuadrant(double x, double y) {
		if (x >= 0) {
			return y >= 0 ? 1 : 4;
		} else {
			return y >= 0 ? 2 : 3;
		}
	}

	/**
	 * Rotate the dialer.
	 * 
	 * @param degrees
	 *            The degrees, the dialer should get rotated.
	 */
	private void rotateDialer(float degrees) {
		matrix.postRotate(degrees, dialerWidth / 2, dialerHeight / 2);
		ivKnob.setImageMatrix(matrix);
	}

	private void setKnobState(boolean state) {

		if (state == true) {

			ivKnob.setY(tvValue.getY() + ivKnob.getHeight());

			ivKnob.setBackgroundResource(R.drawable.knob_low_off);

			knobOn = true;

		} else {

			ivKnob.setY(tvValue.getY());

			ivKnob.setBackgroundResource(R.drawable.knob_low);

			knobOn = false;
		}
	}

	// **********************************************RotaryKnob**********************************************

	private Boolean connect(BluetoothDevice bdDevice) {
		Boolean bool = false;
		try {
			Debugger.i("Log", "service method is called ");
			Class cl = Class.forName("android.bluetooth.BluetoothDevice");
			Class[] par = {};
			Method method = cl.getMethod("createBond", par);
			Object[] args = {};
			bool = (Boolean) method.invoke(bdDevice);// , args);// this invoke
														// creates the detected
														// devices paired.
			// Log.i("Log", "This is: "+bool.booleanValue());
			// Log.i("Log", "devicesss: "+bdDevice.getName());
		} catch (Exception e) {
			Debugger.i("Log", "Inside catch of serviceFromDevice Method");
			e.printStackTrace();
		}
		return bool.booleanValue();
	};

	public boolean createBond(BluetoothDevice btDevice) throws Exception {
		Class class1 = Class.forName("android.bluetooth.BluetoothDevice");
		Method createBondMethod = class1.getMethod("createBond");
		Boolean returnValue = (Boolean) createBondMethod.invoke(btDevice);
		return returnValue.booleanValue();
	}

	private void removeBond(BluetoothDevice device) {
		try {
			Method m = device.getClass()
					.getMethod("removeBond", (Class[]) null);
			m.invoke(device, (Object[]) null);
		} catch (Exception e) {
			Debugger.e(TAG, e.getMessage());
		}
	}
}
