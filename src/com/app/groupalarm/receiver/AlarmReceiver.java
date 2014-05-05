package com.app.groupalarm.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.app.groupalarm.AlarmActivity;
import com.app.groupalarm.WakeLocker;
import com.app.groupalarm.services.SensorIntentService;
import com.facebook.Request;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.model.GraphUser;

public class AlarmReceiver extends BroadcastReceiver {

	public AlarmReceiver() {
	}

	@Override
	public void onReceive(Context context, Intent intent) {
        Intent mSIntent = new Intent(context, SensorIntentService.class);
        Long expireTime = intent.getExtras().getLong("expireTime");
        String alarmid = intent.getExtras().getString("alarmid");
        String userid = intent.getExtras().getString("userid");
        mSIntent.putExtra("expireTime", expireTime);
        mSIntent.putExtra("alarmid", alarmid);
        mSIntent.putExtra("userid", userid);
        context.startService(mSIntent);
		
		WakeLocker.acquire(context);
		
		Intent in = new Intent(context.getApplicationContext(), AlarmActivity.class);
		in.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP |Intent.FLAG_ACTIVITY_NEW_TASK);
		context.startActivity(in);
		
		WakeLocker.release();
	}
}
