package com.app.groupalarm;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.widget.Toast;

import com.app.groupalarm.receiver.AlarmReceiver;
import com.facebook.Session;
import com.facebook.Session.StatusCallback;
import com.facebook.SessionState;
import com.firebase.client.Firebase;
import com.firebase.simplelogin.FirebaseSimpleLoginError;
import com.firebase.simplelogin.FirebaseSimpleLoginUser;
import com.firebase.simplelogin.SimpleLogin;
import com.firebase.simplelogin.SimpleLoginAuthenticatedHandler;

public class WebAppInterface {
	
	Context mContext;
	String token;
	Firebase ref;
	SimpleLogin authClient;
	AlarmManager am;
	
	public WebAppInterface(Context c){
		
		mContext = c;
		ref = new Firebase("https://brilliant-fire-1384.firebaseio.com/");
		authClient = new SimpleLogin(ref, mContext);
		am = (AlarmManager)c.getSystemService(Context.ALARM_SERVICE);
	}
	
	@JavascriptInterface
	public void login(){
		
		// start Facebook Login reason to reload
	    Session.openActiveSession((Activity) mContext, true, new StatusCallback() {
			
			@Override
			public void call(Session session, SessionState state, Exception exception) {
				if(session.isOpened()){
					authClient.loginWithFacebook("715464251850018", session.getAccessToken(), new SimpleLoginAuthenticatedHandler() {
						@Override
						public void authenticated(FirebaseSimpleLoginError error,
								FirebaseSimpleLoginUser user) {
							if(error != null){
								
							}
							else{
								
								//Login with Facebook
							}
							
						}
					});
					
					MainActivity main = (MainActivity) mContext;
					Log.i("FB Success", "Success");
					main.webview.loadUrl("javascript:processToken('" + session.getAccessToken() + "')");
				}
			}
		});
	    
	}
	
	@JavascriptInterface
	private String getToken(){
		Log.i("Token", Session.getActiveSession().getAccessToken());
		return token;
	}
	
	/**
	 * Cancel's an alarm using the alarmid to create a replicate of the PendingIntent of the real alarm
	 * 
	 * @param alarmid Alarm id from Javascript side
	 */
	@JavascriptInterface
	public void cancel(String alarmid) {
		Intent intent = new Intent(mContext,AlarmReceiver.class);
		PendingIntent pendingIntent = PendingIntent.getBroadcast(mContext, Integer.parseInt(alarmid), intent, 0);
		am.cancel(pendingIntent);
		Toast.makeText(mContext, "Alarm Cancelled", Toast.LENGTH_SHORT).show();
	}
	
	
	
	/**
     * Set's an alarm via the input parameters, using the current date of the year as a reference. 
     * 
     * Currently the alarm works if the device is awake, but should work when device is asleep. 
     * See note on line 67
     * 
     * @param time Time of alarm ex. "7:30 am"
     * @param day Day of week for alarm repeat
     * @param offset Minutes backwards from time that alarm will be set. 
     */
    @JavascriptInterface
    public void set(String time, String day, String offset, String alarmid, String userid, String updated) {
		try {
			if (day == null) {
				String[] days = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday" };
				for (String d : days) {
					int id = (d + alarmid).hashCode();
					Intent intent = new Intent(mContext, AlarmReceiver.class);
					PendingIntent pendingIntent = PendingIntent.getBroadcast(
							mContext, id, intent, 0);
					am.cancel(pendingIntent);
				}
			}
			String[] days = day.split(",");
			for (String d : days) {
				int id = (d + alarmid).hashCode();
				int minuteOffset = -(Integer.parseInt(offset));
				SimpleDateFormat sdf = new SimpleDateFormat(
						"hh:mm aa EEEEEE");
				SimpleDateFormat printer = new SimpleDateFormat(
						"hh:mm aa EEEEEE's' MMM dd yyyy");
				if (time.indexOf(Character.getNumericValue(':')) == 1) {
					time = "0" + time;
				}

				// alarm is needed to maintain month, date, year for alarm,
				// otherwise the alarm will be set for 1970 and go off
				// immediately
				// timeRef is only needed for the time and day of the week
				Calendar alarm = Calendar.getInstance();
				Calendar timeRef = Calendar.getInstance();

				// alarm.setTime(sdf.parse("01:42 pm" + " " + "Thursdays"));
				String firstletter = d.substring(0, 1);
				firstletter = firstletter.toUpperCase();
				d = firstletter + d.substring(1, d.length());
				
				timeRef.setTime(sdf.parse(time + " " + d));

				alarm.set(Calendar.DAY_OF_WEEK,
						timeRef.get(Calendar.DAY_OF_WEEK));
				alarm.set(Calendar.HOUR, timeRef.get(Calendar.HOUR));
				alarm.set(Calendar.AM_PM, timeRef.get(Calendar.AM_PM));
				alarm.set(Calendar.MINUTE, timeRef.get(Calendar.MINUTE));
				alarm.set(Calendar.SECOND, 0);

				Intent intent = new Intent(mContext, AlarmReceiver.class);
				intent.putExtra("expireTime", alarm.getTimeInMillis());
				intent.putExtra("alarmid", alarmid);
				intent.putExtra("userid", userid);
				PendingIntent pendingIntent = PendingIntent.getBroadcast(
						mContext, id, intent, 0);
				alarm.add(Calendar.MINUTE, minuteOffset);
				//am.set(AlarmManager.RTC_WAKEUP, alarm.getTimeInMillis(),
				//		pendingIntent);
				// Toast.makeText(mContext, "Alarm set for " +
				// String.valueOf(timePicker.getCurrentHour()) +
				// ":" + String.valueOf(timePicker.getCurrentMinute()),
				// Toast.LENGTH_LONG).show();

				// Set one-time
				// am.set(AlarmManager.RTC_WAKEUP, alarm.getTimeInMillis(), pi);

				// AlarmManager.RTC_WAKEUP should give the new alarm permission
				// to wake the device up.
				// This didn't work on the virtual device; hasn't been tested on
				// real device.
				am.setRepeating(AlarmManager.RTC_WAKEUP, alarm.getTimeInMillis(), AlarmManager.INTERVAL_DAY*7, pendingIntent);
				System.out.println("repeating alarm set " +
						printer.format(new Date(alarm.getTimeInMillis())));
				Log.d("repeating alarm set",
						printer.format(new Date(alarm.getTimeInMillis())));
			}
		} catch (Exception e) {
			e.printStackTrace();
			Log.e("interface error", "err", e);
		}
		if (updated == "true")
			Toast.makeText(mContext, "Alarm Updated", Toast.LENGTH_SHORT).show();
		else
			Toast.makeText(mContext, "Alarm Set", Toast.LENGTH_SHORT).show();
    }
	
	private void setToken(String s){
		token = s;
	}
	
}
