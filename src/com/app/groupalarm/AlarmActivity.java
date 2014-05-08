package com.app.groupalarm;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import com.app.groupalarm.fragments.AlarmFragment;
import com.app.groupalarm.receiver.AlarmReceiver;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;

public class AlarmActivity extends Activity {
	
	Ringtone r;
	Firebase ref;
	AlarmManager am;
	String alarmid;
	String userid;
	String expireTime;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.activity_alarm);
        
        am = (AlarmManager)getApplicationContext().getSystemService(Context.ALARM_SERVICE);
        
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            alarmid = extras.getString("alarmid");
            userid = extras.getString("userid");
            expireTime = extras.getString("expireTime");
        }
        /* can't test this on emulator...
         * uncomment the lines below and also 'r.stop()' in the snooze() and stop() methods
         */
        Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        if (notification == null)
        	notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
        r = RingtoneManager.getRingtone(getApplicationContext(), notification);
        r.play();
        ref = new Firebase("https://brilliant-fire-1384.firebaseio.com/alarms/" + alarmid + "/users/" + userid);
		
        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(R.id.container, new AlarmFragment())
                    .commit();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.alarm, menu);
        return true;
    }
    
    public void snooze() {
    	//r.stop();
    	final Firebase snoozed = ref.child("snoozed");
    	snoozed.addListenerForSingleValueEvent(new ValueEventListener() {
    	     @Override
    	     public void onDataChange(DataSnapshot snapshot) {
    	         int current = snapshot.getValue(Integer.class);
    	         snoozed.setValue(current + 1);
    	     }

    	     @Override
    	     public void onCancelled(FirebaseError error) {
    	         System.err.println("Listener was cancelled");
    	     }
    	});
    	Calendar now = Calendar.getInstance();
    	now.add(Calendar.MINUTE, 8);
    	Intent intent = new Intent(getApplicationContext(), AlarmReceiver.class);
		intent.putExtra("expireTime", now.getTimeInMillis());
		intent.putExtra("alarmid", alarmid);
		intent.putExtra("userid", userid);
		int id = ("snoozed" + alarmid).hashCode();
		PendingIntent pendingIntent = PendingIntent.getBroadcast(
				getApplicationContext(), id, intent, 0);
		am.set(AlarmManager.RTC_WAKEUP, now.getTimeInMillis(), pendingIntent);
		Intent homeIntent = new Intent(getBaseContext(), MainActivity.class);
		getApplicationContext().startActivity(homeIntent);
    }
    
    public void stop() {
    	//r.stop();
    	ref.child("stopped").setValue(1);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

}
