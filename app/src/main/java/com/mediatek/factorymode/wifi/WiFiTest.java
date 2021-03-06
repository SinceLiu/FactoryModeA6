
package com.mediatek.factorymode.wifi;

import java.util.List;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.LocationManager;
import android.net.wifi.ScanResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import com.mediatek.factorymode.AppDefine;
import com.mediatek.factorymode.BaseTestActivity;
import com.mediatek.factorymode.R;
import com.mediatek.factorymode.Utils;

import android.app.ActionBar;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.widget.LinearLayout;

public class WiFiTest extends BaseTestActivity implements OnClickListener {
    public static final int WIFI_STATE_DISABLING = 0;

    public static final int WIFI_STATE_DISABLED = 1;

    public static final int WIFI_STATE_ENABLING = 2;

    public static final int WIFI_STATE_ENABLED = 3;

    public static final int TIMEOUT = 15;

    private TextView mTvInfo = null;

    private TextView mTvResult = null;

    private TextView mTvCon = null;

    private TextView mTvResInfo = null;

    private Button mBtOk = null;

    private Button mBtFailed = null;

    private List<ScanResult> mWifiList = null;

    private SharedPreferences mSp = null;

    private String mNetWorkName = "";

    boolean mResult = false;

    boolean isLoactionOpen;
    boolean isWifiOpen;
    boolean mFlag = false;

    boolean mListFlag = false;

    boolean mWifiScan = false;

    private WiFiTools mWifiTools;

    private int mCount = 0;

    HandlerThread mWifiThread = new HandlerThread("wifiThread");

    WifiHandler mHandler;

    private final static int WIFI_STATE = 0;

    private final static int WIFI_LIST = 1;

    private final static int WIFI_CONNECTED = 2;

    private final static int WIFI_CONNECTING = 3;

    private final static int WIFI_FAILED = 4;

    private final static int WIFI_NOTFOUND_OPENAP = 5;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActionBar.LayoutParams lp = new ActionBar.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                Gravity.CENTER);

        View mView = LayoutInflater.from(this).inflate(R.layout.title, new LinearLayout(this), false);
        TextView mTextView = (TextView) mView.findViewById(R.id.action_bar_title);
        getActionBar().setCustomView(mView, lp);

        mTextView.setText(getTitle());

        getActionBar().setDisplayShowHomeEnabled(false);
        getActionBar().setDisplayShowTitleEnabled(false);
        getActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        getActionBar().setDisplayShowCustomEnabled(true);
        getActionBar().setBackgroundDrawable(new ColorDrawable(Color.BLACK));

        setContentView(R.layout.wifi_test);
        mSp = getSharedPreferences("FactoryMode", Context.MODE_PRIVATE);
        mTvInfo = (TextView) findViewById(R.id.wifi_state_id);
        mTvResult = (TextView) findViewById(R.id.wifi_result_id);
        mTvCon = (TextView) findViewById(R.id.wifi_con_id);
        mTvResInfo = (TextView) findViewById(R.id.wifi_resinfo_id);
        mBtOk = (Button) findViewById(R.id.wifi_bt_ok);
        mBtFailed = (Button) findViewById(R.id.wifi_bt_failed);
        mBtOk.setOnClickListener(this);
        mBtFailed.setOnClickListener(this);

        //判断是否开启位置，android 8.0需要开启位置，wifiManager才能正常工作
        LocationManager locationManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
        boolean networkProvider = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        boolean gpsProvider = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        if (!networkProvider && !gpsProvider) {
            isLoactionOpen = false;
            Settings.Secure.putInt(getContentResolver(), Settings.Secure.LOCATION_MODE, 1);  //开启位置
        } else {
            isLoactionOpen = true;
        }
        mWifiTools = new WiFiTools(this);
        isWifiOpen = !mWifiTools.openWifi();
        mWifiThread.start();
        mHandler = new WifiHandler(mWifiThread.getLooper());
        mHandler.post(wifirunnable);
    }

    class WifiHandler extends Handler {
        public WifiHandler() {
        }

        public WifiHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        /*mWifiTools.closeWifi();*/
        if (!isLoactionOpen) {
            Settings.Secure.putInt(getContentResolver(), Settings.Secure.LOCATION_MODE, 0);  //关闭位置
        }
        if (!isWifiOpen) {
            mWifiTools.closeWifi();
        }
        mHandler.removeCallbacks(wifirunnable);
    }

    Runnable wifirunnable = new Runnable() {
        @Override
        public void run() {
            if (mCount >= TIMEOUT) {
                UiHandler.sendEmptyMessage(WIFI_FAILED);
                /*mWifiTools.closeWifi();*/
                mHandler.removeCallbacks(this);
            }
            if (mFlag == false) {
                boolean res = StartWifi();
                if (res == false && mListFlag == true) {
                    UiHandler.sendEmptyMessage(WIFI_NOTFOUND_OPENAP);
                    mHandler.removeCallbacks(this);
                    return;
                } else if (res != false) {
                    mFlag = true;
                    UiHandler.sendEmptyMessage(WIFI_CONNECTING);
                }
                mHandler.postDelayed(this, 3000);
            } else {
                if (mWifiTools.IsConnection()) {
                    UiHandler.sendEmptyMessage(WIFI_CONNECTED);
                } else {
                    mHandler.postDelayed(this, 3000);
                }
            }
            mCount++;
        }
    };

    Handler UiHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case WIFI_STATE:
                    mTvInfo.setText(mWifiTools.GetState());
                    mTvResult.setText(R.string.WiFi_scaning);
                    break;
                case WIFI_LIST:
                    mTvResult.setText(mNetWorkName);
                    break;
                case WIFI_CONNECTED:
                    mTvResInfo.setText(mWifiTools.GetWifiInfo().toString());
                    mTvCon.setText(getString(R.string.WiFi_success));
                    break;
                case WIFI_CONNECTING:
                    mTvCon.setText(getString(R.string.WiFi_connecting));
                    mTvCon.setTextColor(WiFiTest.this.getResources().getColor(R.color.Green));
                    break;
                case WIFI_FAILED:
                    mTvCon.setText(getString(R.string.WiFi_failed));
                    mTvCon.setTextColor(WiFiTest.this.getResources().getColor(R.color.Red));
                    break;
                case WIFI_NOTFOUND_OPENAP:
                    mTvCon.setText(getString(R.string.WiFi_notfound_openap));
                    mTvCon.setTextColor(WiFiTest.this.getResources().getColor(R.color.Red));
                    break;
            }
        }
    };

    public boolean StartWifi() {
        UiHandler.sendEmptyMessage(WIFI_STATE);
        mWifiList = mWifiTools.scanWifi();
//        if(AllTest.begin_auto_test&&mWifiList!=null&&mWifiList.size()>0){
//            Utils.SetPreferences(WiFiTest.this, mSp, R.string.wifi_name, AppDefine.FT_SUCCESS);
//            finish();
//        }
        mNetWorkName = "";
        if (mWifiList == null || mWifiList.size() <= 0) {
            return false;
        }
        if (mWifiList.size() > 0) {
            for (int i = 0; i < mWifiList.size(); i++) {
                ScanResult sr = mWifiList.get(i);
                mNetWorkName += sr.SSID + "------" + sr.level + "\n";
            }
            UiHandler.sendEmptyMessage(WIFI_LIST);
            for (int j = 0; j < mWifiList.size(); j++) {
                ScanResult sr = mWifiList.get(j);
                if (sr.capabilities.equals("[WPS]") || sr.capabilities.equals("")) {
                    mResult = mWifiTools.addWifiConfig(mWifiList, sr, "");
                    if (mResult == true) {
                        return true;
                    }
                }
            }
            mListFlag = true;
            return false;
        }
        return false;
    }

    @Override
    public void onClick(View v) {
        Utils.SetPreferences(this, mSp, R.string.wifi_name,
                (v.getId() == mBtOk.getId()) ? AppDefine.FT_SUCCESS : AppDefine.FT_FAILED);
        finish();
    }
}
