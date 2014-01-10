package com.example.focus;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.os.Environment;
import android.util.Log;

import com.testflightapp.lib.TestFlight;

public class Debugger {

	public static final String TESTFLIGHT_TOKEN = "f641e711-eb8e-4d6d-8fb5-57d45bc837da";

	public static SimpleDateFormat dateFormat = new SimpleDateFormat("HHmmss_ddMMyyyy", Locale.US);

	// Create date name log file every time when application start
	public static File logFolder = new File(Environment.getExternalStorageDirectory() + "/FocusLogs");
	public static boolean TESTFLIGHT = false;

	public static File logFile = new File(logFolder.getPath() + "/FocusLogs" + dateFormat.format(new Date()) + ".txt");

	public static final String TAG = "Tag";

	// default tags
	// private static String[] displayableTags = { MainActivity.LogTag,
	// DownloadItem.LogTag, LocationController.LogTag, TrafficCrud.LogTag };
	private static String[] displayableTags = { MainActivity.TAG, WorkableService.TAG, DevicesActivity.TAG };

	public static void setTags(String[] tags) {
		displayableTags = tags;
	}

	public static void d(String tag, String message) {

		Log.d(tag, message);
		if (TESTFLIGHT)
			TestFlight.passCheckpoint("Debug - " + tag + " : " + message);
		for (String tagName : displayableTags) {

			if (tagName.compareTo(tag) == 0) {

				appendLog(tag + ":\t" + message);

				break;
			}
		}
	}

	public static void e(String tag, String message) {

		Log.e(tag, message);
		if (TESTFLIGHT)
			TestFlight.passCheckpoint("Error - " + tag + " : " + message);
		appendLog("***ERROR*** " + tag + ":\t" + message);
	}

	public static void i(String tag, String message) {

		Log.i(tag, message);
		if (TESTFLIGHT)
			TestFlight.passCheckpoint("Info - " + tag + " : " + message);
		appendLog("***INFORM***" + tag + ":\t" + message);
	}

	public static void w(String tag, String message) {

		Log.w(tag, message);
		if (TESTFLIGHT)
			TestFlight.passCheckpoint("Warning - " + tag + " : " + message);
		appendLog("***WARNING***" + tag + ":\t" + message);
	}

	public static void appendLog(String text) {

		try {

			if (!logFolder.exists()) {

				logFolder.mkdir();
			}

			// BufferedWriter for performance, true to set append to file flag
			BufferedWriter buf = new BufferedWriter(new FileWriter(logFile, true));
			buf.append((new Date()).toGMTString() + "\t" + text);
			buf.newLine();
			buf.newLine();
			buf.close();

		} catch (IOException e) {

			e.printStackTrace();

		} catch (Exception e) {

			e.printStackTrace();
		}
	}
	// public static void d(String tag, String message)
	// {
	// Log.d(tag, message);
	// TestFlight.passCheckpoint("Debug - " + tag +" : " + message);
	// //TestFlight.log("Debug - " + tag +" : " + message);
	// }
	// public static void e(String tag, String message)
	// {
	// Log.e(tag, message);
	// TestFlight.passCheckpoint("Error - " + tag +" : " + message);
	// //TestFlight.log("Error- " + tag +" : " + message);
	// }
	// public static void i(String tag, String message)
	// {
	// Log.i(tag, message);
	// TestFlight.passCheckpoint("Info - " + tag +" : " + message);
	// //TestFlight.log("Info - " + tag +" : " + message);
	// }
	//
	// public static void w(String tag, String message)
	// {
	// Log.w(tag, message);
	// TestFlight.passCheckpoint("Warning - " + tag +" : " + message);
	// //TestFlight.log("Warning - " + tag +" : " + message);
	// }
	//
	// //Not usable for now.
	// public static void passCheckpoint(String checkpoint)
	// {
	// //TestFlight.passCheckpoint(checkpoint);
	// }
}
