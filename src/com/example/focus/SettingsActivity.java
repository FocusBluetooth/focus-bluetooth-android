package com.example.focus;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;
import java.util.Arrays;

import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.User;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;
import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationBuilder;
import android.app.Activity;

import android.bluetooth.BluetoothAdapter;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.Signature;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;

import android.preference.PreferenceManager;
import android.util.Base64;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.FacebookException;
import com.facebook.Request;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.model.GraphUser;
import com.facebook.widget.LoginButton;
import com.facebook.widget.LoginButton.OnErrorListener;

public class SettingsActivity extends Activity implements OnClickListener, OnItemSelectedListener {

	public final String TAG = "SettingActivity";

	ImageButton ibBT;
	ImageButton ibInfo;
	SeekBar sbCurrent;
	TextView tvCurrentLabel;
	EditText pass;
	Spinner spinner;
	TextView tvFacebook;

	private WorkableService mService = null;
	private BluetoothDevice mDevice = null;
	private BluetoothAdapter mBtAdapter = null;

	// constants for twitter login:
	static String TWITTER_CONSUMER_KEY = "c5mHCso3LzEN9t8OKhPVJA"; // place your
																	// consumer
																	// key here
	static String TWITTER_CONSUMER_SECRET = "5WYJAbcEmUYjAfjilV9Uyk93sfiUtl8KyxsjdhmYaCw"; // place
																							// your
																							// consumer
																							// secret
																							// here

	// Preference Constants
	// static final String PREF_KEY_OAUTH_TOKEN =
	// "616502507-jmpUzOvz0hPXxFtsLsyWEB8DY9EEPnxyBMg3bBBM";
	// static final String PREF_KEY_OAUTH_SECRET =
	// "j2trRe4BL4HigN19ZJbUt5XNZTd8zkb63AT6EFKvAo0";
	static final String PREF_KEY_OAUTH_TOKEN = "oauth_token";
	static final String PREF_KEY_OAUTH_SECRET = "oauth_token_secret";
	static final String PREF_KEY_TWITTER_USERNAME = "twitter_username";
	static final String PREF_KEY_TWITTER_LOGIN = "isTwitterLogedIn";

	static final String TWITTER_CALLBACK_URL = "oauth://stilldoesntcomplete.com";

	// Twitter oauth urls
	static final String URL_TWITTER_AUTH = "auth_url";
	static final String URL_TWITTER_OAUTH_VERIFIER = "oauth_verifier";
	static final String URL_TWITTER_OAUTH_TOKEN = "oauth_token";

	// Login button
	ImageButton ibLoginTwitter;
	TextView tvTwitter;

	// Twitter
	private static Twitter twitter;
	private static RequestToken requestToken;

	// Shared Preferences
	private static SharedPreferences mSharedPreferences;

	// Internet Connection detector
	private ConnectionDetector cd;

	// Alert Dialog Manager
	AlertDialogManager alert = new AlertDialogManager();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.settings);
		
		final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
		mBtAdapter = bluetoothManager.getAdapter();
		if (mBtAdapter == null) {
			Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
			finish();
			return;
		}
		Bundle bundle = getIntent().getExtras();
		if(bundle != null && mBtAdapter != null)
		{
			String deviceAddress = bundle.getString(BluetoothDevice.EXTRA_DEVICE);
 			mDevice = mBtAdapter.getRemoteDevice(deviceAddress);	
		}
		ibBT = (ImageButton) findViewById(R.id.ibBT);
		ibInfo = (ImageButton) findViewById(R.id.ibInfo);

		ibBT.setOnClickListener(this);
		ibInfo.setOnClickListener(this);
		pass = (EditText) findViewById(R.id.etPincode);

		tvCurrentLabel = (TextView) findViewById(R.id.tvCurrentLabel);

		// Shared Preferences
		mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		// Add twitter login:
		ibLoginTwitter = (ImageButton) findViewById(R.id.ibTwitter);
		tvTwitter = (TextView) findViewById(R.id.tvTwitter);

		/**
		 * Twitter login button click event will call loginToTwitter() function
		 * */
		spinner = (Spinner) findViewById(R.id.spMode);
		spinner.setOnItemSelectedListener(this);
		int selectedMode = mSharedPreferences.getInt(CommonValues.ACTIVE_MODE, 0);
		spinner.setSelection(selectedMode);
		ibLoginTwitter.setOnClickListener(this);

		if (!isTwitterLoggedInAlready()) {
			Uri uri = getIntent().getData();
			if (uri != null && uri.toString().startsWith(TWITTER_CALLBACK_URL)) {
				oAuthVerifier(uri);
			}
		} else {
			String username = mSharedPreferences.getString(PREF_KEY_TWITTER_USERNAME, "");
			tvTwitter.setText(username);
		}

		initializeFacebookButton();
		initializeSeekBar();
		
		init();
	}


	private void init() {
		Intent bindIntent = new Intent(this, WorkableService.class);
		startService(bindIntent);
		bindService(bindIntent, mServiceConnection, Context.BIND_AUTO_CREATE);
	}

	protected void initializeFacebookButton() {
		// Add Facebook login
		tvFacebook = (TextView) findViewById(R.id.tvFacebook);
		LoginButton authButton = (LoginButton) findViewById(R.id.ibFacebook);
		authButton.setOnErrorListener(new OnErrorListener() {

			@Override
			public void onError(FacebookException error) {
				Debugger.i(TAG, "Error " + error.getMessage());
			}
		});
		authButton.setReadPermissions(Arrays.asList("basic_info", "email"));
		// session state call back event
		authButton.setSessionStatusCallback(new Session.StatusCallback() {

			@Override
			public void call(Session session, SessionState state, Exception exception) {

				if (session.isOpened()) {
					Debugger.i(TAG, "Access Token" + session.getAccessToken());
					Request.executeMeRequestAsync(session, new Request.GraphUserCallback() {
						@Override
						public void onCompleted(GraphUser user, Response response) {
							if (user != null) {
								Debugger.i(TAG, "User ID " + user.getId());
								Debugger.i(TAG, "Email " + user.asMap().get("email"));
								tvFacebook.setText(user.asMap().get("email").toString());
							}
						}
					});
				}

			}
		});

		try {
			PackageInfo info = getPackageManager().getPackageInfo("com.example.focus", PackageManager.GET_SIGNATURES);
			for (Signature signature : info.signatures) {
				MessageDigest md = MessageDigest.getInstance("SHA");
				md.update(signature.toByteArray());
				String hash = Base64.encodeToString(md.digest(), Base64.DEFAULT);
				Debugger.d("KeyHash:", hash);
			}
		} catch (NameNotFoundException e) {
		} catch (NoSuchAlgorithmException e) {
		}
	}

	protected void initializeSeekBar() {
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		int value = pref.getInt(CommonValues.MAX_CURRENT_PREF, 0);

		sbCurrent = (SeekBar) findViewById(R.id.sbCurrent);
		sbCurrent.setProgress(value);
		int val = value + 20;
		float progress = (float) ((val * 0.0067 + 0.7));
		DecimalFormat df = new DecimalFormat("#.#");
		String str = df.format(progress);
		tvCurrentLabel.setText(str);
		sbCurrent.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
			}

			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				// min value should be 20;
				// seek bar should be range from 0.8 to 2.0
				// according to documentation, values that can be write to
				// device
				// can range from 20 to 200.
				int val = progress + 20;
				float value = (float) ((val * 0.0067 + 0.7));
				DecimalFormat df = new DecimalFormat("#.#");
				String str = df.format(value);
				tvCurrentLabel.setText(str);
			}
		});
	}

	protected void oAuthVerifier(Uri uri) {
		// oAuth verifier
		GetAccessToken token = new GetAccessToken();
		token.execute(uri);

	}


	@Override
	public void onClick(View v) {

		switch (v.getId()) {

		case R.id.ibBT:

			
			// Button BT
			Intent intent = new Intent(this, DevicesActivity.class);
			startActivity(intent);
			
			break;

		case R.id.ibInfo:

			// Button Info
			Toast.makeText(this, "Info", Toast.LENGTH_SHORT).show();
			break;
		case R.id.ibTwitter:

			cd = new ConnectionDetector(getApplicationContext());

			// Check if Internet present
			if (!cd.isConnectingToInternet()) {
				// Internet Connection is not present
				alert.showAlertDialog(this, "Internet Connection Error", "Please connect to working Internet connection", false);
				// stop executing code by return
				break;
			}
			// if logged in already - than log out.
			if (isTwitterLoggedInAlready()) {
				new Thread(new Runnable() {

					@Override
					public void run() {
						logoutFromTwitter();
					}
				}).start();
				return;
			}
			// Call login twitter function
			new Thread(new Runnable() {

				@Override
				public void run() {
					// Check if twitter keys are set
					if (TWITTER_CONSUMER_KEY.trim().length() == 0 || TWITTER_CONSUMER_SECRET.trim().length() == 0) {
						// Internet Connection is not present
						alert.showAlertDialog(getApplicationContext(), "Twitter oAuth tokens", "Please set your twitter oauth tokens first!", false);
						// stop executing code by return
						return;
					} else

						loginToTwitter();
				}
			}).start();

			break;
		default:
			break;
		}
	}

	@Override
	public void onBackPressed() {
		SharedPreferences.Editor pref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit();

		byte value = (byte) sbCurrent.getProgress();
		pref.putInt(CommonValues.MAX_CURRENT_PREF, value);

 		if (mService != null && mDevice != null) {
			mService.writeCharacteristic(WorkableService.MAX_CURRENT, value);

			pref.commit();
			if (pass.getText().toString().length() > 0) {
				int password = Integer.valueOf(pass.getText().toString());
				byte[] arr = ByteBuffer.allocate(4).putInt(password).array();
				WorkableService.swapArray(arr);

				mService.writeCharacteristic( WorkableService.PIN_CODE, arr);
			}
		} else {

			Debugger.d(TAG, "OnBackPressed(). Device is not connected");
//			AlertDialog.Builder builder = new AlertDialog.Builder(this);
//			builder.setTitle("Problem");
//			builder.setMessage(R.string.device_disconnected);
//			builder.setNegativeButton(R.string.ok, new DialogInterface.OnClickListener() {
//
//				@Override
//				public void onClick(DialogInterface dialog, int which) {
//					finish();
//				}
//
//			});
		}

		super.onBackPressed();
	};

	private ServiceConnection mServiceConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder rawBinder) {
			mService = ((WorkableService.LocalBinder) rawBinder).getService();
			Debugger.d(TAG, "onServiceConnected mService= " + mService);
			if (!mService.initialize()) {
                Debugger.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
			mService.setActivityHandler(mHandler);
		}

		public void onServiceDisconnected(ComponentName classname) {
			mService.disconnect();
			mService = null;
		}
	};

	public void setDevice(BluetoothDevice device) {
		mDevice = device;
	}

	private Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {

			final Bundle data = msg.getData();
			switch (msg.what) {

			case WorkableService.CONNECT_MSG:
				String text = data.getString(BluetoothDevice.EXTRA_NAME);

				break;
			case WorkableService.READY_MSG:

				// mService.readSpecificChar(mDevice,
				// WorkableService.ACTUAL_LEVEL);
				break;
			case WorkableService.DISCONNECT_MSG:
				mDevice = null;
				Debugger.d(TAG, "Device disconnected");
				break;
			// int value;
			// String name =
			// data.getString(WorkableService.EXTRA_CHARACTERISTIC);
			// if (name.equals(WorkableService.BATTERY_LEVEL.toString())) {
			// value = data.getInt(name);
			// }
			}
		}
	};

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		switch (requestCode) {

		case CommonValues.REQUEST_SELECT_DEVICE:
			if (resultCode == Activity.RESULT_OK && data != null) {
				String deviceAddress = data.getStringExtra(BluetoothDevice.EXTRA_DEVICE);
				if(mBtAdapter != null)
					mDevice = mBtAdapter.getRemoteDevice(deviceAddress);
				else
				{
					Debugger.e(TAG, "onActivityResult. Error BluetoothAdapter = null.");
					finish();
				}
				
				Debugger.d(TAG, "... onActivityResultdevice.address==" + mDevice + "mserviceValue" + mService);
				// setUiState();
				mService.connect(deviceAddress);
			}
			break;
		default:
			Debugger.e(TAG, "wrong request code");
			break;
		}
		Session.getActiveSession().onActivityResult(this, requestCode, resultCode, data);
	}

	/* *
	 * Function to login twitter
	 */
	private void loginToTwitter() {
		// Check if already logged in
		if (!isTwitterLoggedInAlready()) {
			ConfigurationBuilder builder = new ConfigurationBuilder();
			builder.setOAuthConsumerKey(TWITTER_CONSUMER_KEY);
			builder.setOAuthConsumerSecret(TWITTER_CONSUMER_SECRET);
			Configuration configuration = builder.build();

			TwitterFactory factory = new TwitterFactory(configuration);
			twitter = factory.getInstance();

			try {
				requestToken = twitter.getOAuthRequestToken(TWITTER_CALLBACK_URL);
				this.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(requestToken.getAuthenticationURL())));
			} catch (TwitterException e) {
				e.printStackTrace();
			}
		} else {
			// user already logged into twitter
			runOnUiThread(new Runnable() {

				@Override
				public void run() {
					Toast.makeText(getApplicationContext(), "Already Logged into twitter", Toast.LENGTH_LONG).show();
					String username = mSharedPreferences.getString(PREF_KEY_TWITTER_USERNAME, "");
					tvTwitter.setText(username);
				}
			});

		}
	}

	/**
	 * Function to update status
	 * */
	class GetAccessToken extends AsyncTask<Uri, Void, String> {
		private AccessToken accessToken = null;

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
		}

		@Override
		protected String doInBackground(Uri... params) {
			Uri uri = params[0];
			final String verifier = uri.getQueryParameter(URL_TWITTER_OAUTH_VERIFIER);

			try {
				// Get the access token
				accessToken = twitter.getOAuthAccessToken(requestToken, verifier);
				// Shared Preferences
				Editor e = mSharedPreferences.edit();

				// After getting access token, access token secret
				// store them in application preferences
				if (accessToken != null) {
					e.putString(PREF_KEY_OAUTH_TOKEN, accessToken.getToken());
					e.putString(PREF_KEY_OAUTH_SECRET, accessToken.getTokenSecret());
					// Store login status - true
					e.putBoolean(PREF_KEY_TWITTER_LOGIN, true);
					// save changes
					accessToken.getScreenName();
					Debugger.e("Twitter OAuth Token", "> " + accessToken.getToken());

					// Getting user details from twitter
					// For now i am getting his name only
					long userID = accessToken.getUserId();
					User user = twitter.showUser(userID);
					String username = user.getName();
					e.putString(PREF_KEY_TWITTER_USERNAME, username);
					e.commit();
					return username;

				}
			} catch (TwitterException e) {

				Debugger.e("GetAccessToken - Twitter Exception", e.getMessage());
			} catch (Exception e) {
				// Check log for login errors
				Debugger.e("Twitter Login Error", "> " + e.getMessage());
			}
			return null;
		}

		@Override
		protected void onPostExecute(String result) {
			tvTwitter.setText(result);
		}

	}

	/**
	 * Function to logout from twitter It will just clear the application shared
	 * preferences
	 * */
	private void logoutFromTwitter() {
		// Clear the shared preferences
		Editor e = mSharedPreferences.edit();
		e.remove(PREF_KEY_OAUTH_TOKEN);
		e.remove(PREF_KEY_OAUTH_SECRET);
		e.remove(PREF_KEY_TWITTER_LOGIN);
		e.commit();

		// After this take the appropriate action
		// I am showing the hiding/showing buttons again
		// You might not needed this code
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				tvTwitter.setText("");
			}
		});
		// ibLoginTwitter.setVisibility(View.VISIBLE);
	}

	/**
	 * Check user already logged in your application using twitter Login flag is
	 * fetched from Shared Preferences
	 * */
	private boolean isTwitterLoggedInAlready() {
		// return twitter login status from Shared Preferences
		return mSharedPreferences.getBoolean(PREF_KEY_TWITTER_LOGIN, false);
	}

	protected void onResume() {
		super.onResume();
	}

	@Override
	protected void onDestroy() {
		Debugger.d(TAG, "onDestroy()");
		try {
			unbindService(mServiceConnection);
		} catch (Exception ignore) {
			Debugger.e(TAG, ignore.toString());
		}
		super.onDestroy();
	};

	@Override
	public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
		if (mSharedPreferences == null) {
			mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		}
		Editor editor = mSharedPreferences.edit();
		// put current index of selected item
		editor.putInt(CommonValues.ACTIVE_MODE, arg2);
		editor.commit();

	}

	@Override
	public void onNothingSelected(AdapterView<?> arg0) {

	}
}
