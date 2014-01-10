package com.example.focus;

public class CommonValues {

	public final static String MAX_CURRENT_PREF = "MAX_CURRENT";
	public final static String ACTIVE_MODE = "ACTIVE_MODE";
	public final static String PASS = "PASS";
	public final static int REQUEST_SELECT_DEVICE = 1;
	public final static int REQUEST_PAIRING = 3;
	
    public static final int STATE_DISCONNECTED = 0;
    public static final int STATE_CONNECTING = 1;
    public static final int STATE_CONNECTED = 2;
	
	// battery states
	public final static int BATTERY_EMPTY = 0;
	public final static int BATTERY_ALMOST_EMPTY = 10;
	public final static int BATTERY_LESS_HALF = 30;
	public final static int BATTERY_HALF = 50;
	public final static int BATTERY_MORE_HALF = 70;
	public final static int BATTERY_ALMOST_FULL = 90;
	
	// time to read actual level characteristic 
	public final static int READ_TIME = 200;
	
	// available modes
	public static final int OFF = 0;
	public static final int PAIRED = 1;
	public static final int CONTINUOUS = 2;
	public static final int PULSE = 3;
	public static final int SINUS = 4;
	public static final int NOISE = 5;
	public static final int FAKE = 6;
	
	
	
}
