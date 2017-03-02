package com.example.shaun.shaunandroidtest;

import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;
import android.app.Activity;

/**
 * Created by truedemon on 2/3/2017.
 */

public class AppServiceController extends Application {

    private Context mContext;
    private static AppServiceController mInstance;
    private MyService mService; //= new MyService(mInstance);
//    Intent intent = new Intent(this, MyService.class);
    Intent mIntent;
    public static synchronized AppServiceController getInstance() {
        return mInstance;
    }

    @Override
    public void onCreate() {
//        mContext = this;
        mInstance = this;
        mService = new MyService();
        // TODO Auto-generated method stub
        super.onCreate();
    }

    public void startService(){
        //start your service

        mService.startService();
        mIntent = new Intent(this, MyService.class);
        bindService(mIntent, mConnection, Context.BIND_AUTO_CREATE);
    }
    public void stopService(){
        //stop service
    }
    public MyService getService(){
        return mService;
    }
    private boolean mBound = false;
    private static final String TAG = "Batman service controller";
    private ServiceConnection mConnection = new ServiceConnection() {
        // Called when the connection with the service is established
        public void onServiceConnected(ComponentName className, IBinder service) {
            // Because we have bound to an explicit
            // service that is running in our own process, we can
            // cast its IBinder to a concrete class and directly access it.
            MyService.BTBinder binder = (MyService.BTBinder) service;
            mService = binder.getService();
            mBound = true;
        }

        // Called when the connection with the service disconnects unexpectedly
        public void onServiceDisconnected(ComponentName className) {
//            Log.e(TAG, "onServiceDCed");
            mBound = false;
        }
    };
}
