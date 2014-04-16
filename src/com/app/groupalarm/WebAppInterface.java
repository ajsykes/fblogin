package com.app.groupalarm;

import com.facebook.Session;
import com.facebook.Session.StatusCallback;
import com.facebook.SessionState;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

public class WebAppInterface {
	
	Context mContext;
	String token;
	
	public WebAppInterface(Context c){
		
		mContext = c;
	}
	
	@JavascriptInterface
	public void facebookLogin(){
		
		// start Facebook Login
	    Session.openActiveSession((Activity) mContext, true, new StatusCallback() {
			
			@Override
			public void call(Session session, SessionState state, Exception exception) {
				if(session.isOpened()){
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
	
	private void setToken(String s){
		token = s;
	}
	
}
