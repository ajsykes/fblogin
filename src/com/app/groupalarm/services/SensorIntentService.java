package com.app.groupalarm.services;

import java.util.Calendar;

import android.app.IntentService;
import android.app.Service;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

import com.app.groupalarm.sensors.AccelerometerState;
import com.app.groupalarm.sensors.LightState;
import com.firebase.client.Firebase;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 * TODO: Customize class - update intent actions, extra parameters and static
 * helper methods.
 */
public class SensorIntentService extends IntentService implements SensorEventListener{
	private SensorManager sensorManager;
	private Sensor accelerometerSensor;
	private Sensor lightSensor;
	
	private Firebase firebaseRef;
	
	private AccelerometerState accelerometerState;
	private LightState lightState;
	private Calendar cal;
	
	private float previousLightLux;
	private long responseIndex = 0;
	
	private int currentDownTime = 0;
	private int currentUpTime = 0;
	private long totalUpTime = 1;
	private long totalDownTime = 1;
	private final float MOVEMENTTHRESHOLD = (float) 0.1;
	private final int DOWNTIMETHRESHOLD = 7;
	private final int UPTIMETHRESHOLD = 10;
	
	private Long expireTime;
	private String alarmid;
	private String userid;
	
	public SensorIntentService() {
		super("SensorIntentService");
	}


	@Override
	protected void onHandleIntent(Intent intent) {
		if (intent != null) {
			expireTime = intent.getExtras().getLong("expireTime");
			alarmid = intent.getExtras().getString("alarmid");
			userid = intent.getExtras().getString("userid");
			firebaseRef = new Firebase("https://brilliant-fire-1384.firebaseio.com/alarms/" + alarmid + "/users/" + userid);
			sensorManager = (SensorManager) getSystemService(Service.SENSOR_SERVICE);
			startAccelerometer();
			startLightSensor();
		}
	}

	/**
	 * Handle action Foo in the provided background thread with the provided
	 * parameters.
	 */
	private void handleActionFoo(String param1, String param2) {
		throw new UnsupportedOperationException("Not yet implemented");
	}

	/**
	 * Handle action Baz in the provided background thread with the provided
	 * parameters.
	 */
	private void handleActionBaz(String param1, String param2) {
		throw new UnsupportedOperationException("Not yet implemented");
	}
	
	private void startAccelerometer() {
		accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
		sensorManager.registerListener(this, accelerometerSensor, SensorManager.SENSOR_DELAY_NORMAL);
		
		setIsMoving(false);
	}
	
	private void startLightSensor(){
		lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
		sensorManager.registerListener(this, lightSensor, SensorManager.SENSOR_DELAY_UI);
		
		previousLightLux = (float) -1.0;
		
		setIsLight(LightState.UNDECIDED);
	}

	@Override
	public void onAccuracyChanged(Sensor arg0, int arg1) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		cal = Calendar.getInstance();
		Long time = event.timestamp/1000000;
		if (time >= expireTime + 1000*60*60) {
			finishSensorService();
		}
		
		int sensorType = event.sensor.getType();
		
		if(sensorType == Sensor.TYPE_LINEAR_ACCELERATION){
			accelerometerHandler(event);
		}
		else if(sensorType == Sensor.TYPE_LIGHT){
			lightHandler(event);
		}
		
		responseIndex++;
		
		if(responseIndex >= Integer.MAX_VALUE){
			responseIndex = 1;
		}
		
		if(totalUpTime > 100 && getTotalUptimeToDowntimeRatio() > 2.0){
			firebaseRef.child("sigMove").setValue(1);
		}
		
	}

	private void lightHandler(SensorEvent event) {
		float luxValue = event.values[0];
		
		if(responseIndex % 50 == 0 || responseIndex == 0){
			previousLightLux = luxValue;
		}
		else if((luxValue > 15.0 && luxValue > previousLightLux*2) || luxValue > 35.0){
			setIsLight(LightState.ON);
		}
		else if(luxValue < 10.0 && lightState != LightState.UNDECIDED){
			setIsLight(LightState.OFF);
		}
		
	}

	private void accelerometerHandler(SensorEvent event) {
		
		float force_x = event.values[0];
		float force_y = event.values[1];
		float force_z = event.values[2];
		
		if(accelerometerState == AccelerometerState.IDLE){
			totalDownTime++;

			if(isCurrentlyMoving(force_x, force_y, force_z)){
				currentUpTime++;
			}
			else{
				currentUpTime = 0;
			}
			if(currentUpTime >= UPTIMETHRESHOLD){
				setIsMoving(true);
			}
		}
		else if(accelerometerState == AccelerometerState.MOVING){

			totalUpTime++;
			
			if(isCurrentlyMoving(force_x, force_y, force_z)){
				currentDownTime = 0;
			}
			else{
				currentDownTime++;
			}
			if(currentDownTime >= DOWNTIMETHRESHOLD){
				setIsMoving(false);
			}
		}
		
		if(totalUpTime >= Integer.MAX_VALUE || totalDownTime >= Integer.MAX_VALUE){
			totalDownTime = totalDownTime / 10;
			totalUpTime = totalUpTime / 10;
		}
	}
	
	private boolean isCurrentlyMoving(float forceX, float forceY, float forceZ){
		if(forceX > MOVEMENTTHRESHOLD || forceY > MOVEMENTTHRESHOLD || forceZ > MOVEMENTTHRESHOLD){
			return true;
		}
		if(forceX < MOVEMENTTHRESHOLD*-1 || forceY < MOVEMENTTHRESHOLD*-1 || forceZ < MOVEMENTTHRESHOLD*-1){
			return true;
		}
		return false;
	}
	
	private void setIsMoving(boolean isMoving){
		if(isMoving){
			firebaseRef.child("moving").setValue(1);
			accelerometerState = AccelerometerState.MOVING;
			Log.d("moving", "I am now moving");
		}
		else{
			firebaseRef.child("moving").setValue(0);
			accelerometerState = AccelerometerState.IDLE;
		}
	}
	
	private void setIsLight(LightState state){
		if(state == LightState.ON){
			firebaseRef.child("light").setValue(1);
			lightState = LightState.ON;
		}
		else if (state == LightState.OFF){
			firebaseRef.child("light").setValue(0);
			lightState = LightState.OFF;
		}
		else if(state == LightState.UNDECIDED){
			firebaseRef.child("light").setValue(-1);
			lightState = LightState.UNDECIDED;
		}
	}
	
	private double getTotalUptimeToDowntimeRatio(){
		return totalUpTime/totalDownTime;
	}
	
	private void finishSensorService(){
		sensorManager.unregisterListener(this,accelerometerSensor);
		sensorManager.unregisterListener(this,lightSensor);
		
		firebaseRef.child("sigMove").setValue(0);
		firebaseRef.child("light").setValue(0);
		firebaseRef.child("moving").setValue(0);
		firebaseRef.child("opt out").setValue(0);
		firebaseRef.child("snoozed").setValue(0);
		firebaseRef.child("stopped").setValue(0);
	}
}
