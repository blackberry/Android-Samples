/* Copyright (c) 2011-2016 BlackBerry Limited.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.blackberry.bidlogin;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.blackberry.bidhelper.BidHelperAndroid;
import com.blackberry.bidhelper.BidListener;
import com.blackberry.bidhelper.BidSignatureVerificationException;
import com.blackberry.bidhelper.BidStatusReport;

import java.math.BigInteger;
import java.util.Random;


/**
 * This class is a login Activity. It shows the call back to BID for generating
 * report and then verify it using Bouncy Castle library.
 */
public class BidLoginActivity extends Activity implements BidListener {

    public static final boolean DEBUG = false;
    public static final int GET_BID_REPORT = 1;
    public static final int DEVICE_COMPROMISED = 2;
    public static final int DEVICE_SAFE = 3;
    private static final String TAG = "BlackBerryBidTestApp";
    private final HandlerThread mHandlerThread = new HandlerThread("BidLoginActivity", android.os.Process.THREAD_PRIORITY_BACKGROUND);
    private BidReportHandler mHandler;
    private Handler mUIHandler;

    private Context context;

    private Button login;
    private EditText username;
    private EditText password;

    private BidHelperAndroid mHelper;
    private OnClickListener mLoginListener = new OnClickListener() {
        public void onClick(View v) {
            String user = username.getText().toString();
            String pass = password.getText().toString();

            if (user.equals(R.string.username) && pass.equals(R.string.password)) {
                Intent intent = new Intent(context, Welcome.class);
                startActivity(intent);
            } else {
                Toast.makeText(context, R.string.fail_login, Toast.LENGTH_SHORT).show();
            }


        }
    };

    private void setupButton(int id, View.OnClickListener l) {
        Button button = (Button) findViewById(id);
        button.setOnClickListener(l);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (DEBUG) {
            Log.i(TAG, "onCreate received");
        }

        context = this;
        mHelper = new BidHelperAndroid(this);
        mHelper.addBidListener(this);

        setContentView(R.layout.activity_login_screen);
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        username = (EditText) findViewById(R.id.username);
        password = (EditText) findViewById(R.id.password);
        login = (Button) findViewById(R.id.login_button);

        setupButton(R.id.login_button, mLoginListener);
        disableControls();

        if (Build.MANUFACTURER.equalsIgnoreCase("BlackBerry") && (Build.MODEL).startsWith("STV")) {
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1) {
                getBidReport();
            } else {
                Toast.makeText(this, R.string.version_msg, Toast.LENGTH_LONG).show();
            }
        } else {
            Toast.makeText(this, R.string.warning_msg, Toast.LENGTH_LONG).show();
        }
    }

    /**
     * gets the report by sending a message
     * GET_BID_REPORT to the message queue.
     */
    private void getBidReport() {
        mUIHandler = new Handler() {
            public void handleMessage(Message msg) {
                final int what = msg.what;
                switch (what) {
                    case DEVICE_SAFE:
                        enableControls();
                        break;
                    default:
                        break;
                }
            }
        };
        mHandlerThread.start();
        mHandler = new BidReportHandler(mHandlerThread.getLooper());
        mHandler.sendEmptyMessage(GET_BID_REPORT);
    }

    /**
     * It enables the controls once the verification completes
     */
    private void enableControls() {
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        username.setEnabled(true);
        password.setEnabled(true);
        login.setEnabled(true);
    }

    /**
     * It disables the control untill the verification
     * has been done.
     */
    private void disableControls() {
        username.setEnabled(false);
        password.setEnabled(false);
        login.setEnabled(false);
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.i(TAG, "onResume received");
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.i(TAG, "onPause received");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mHelper.destroy();
    }

    @Override
    public void certificateAvailable() {
        Toast.makeText(this, R.string.cert_available, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void reportInserted() {
        Toast.makeText(this, R.string.report_inserted, Toast.LENGTH_SHORT).show();
    }

    /**
     * It allows for requesting the status report and verifying it.
     * The report generated should be verified before opening.
     */
    private void queryStatus() {
        try {
            BigInteger bi = new BigInteger(256, new Random());
            BidStatusReport report = mHelper.requestStatusReport(bi);
            try {
                mHelper.verifyStatusReport(report, bi, true);
            } catch (BidSignatureVerificationException ex) {
                Log.e(TAG, "verification failed.");
                /**
                 * You can read a particular report without verifying
                 * it by using the below call. THIS IS NOT RECOMMENDED.
                 */
                //report.bypassVerification();
            }
            if (report.hasFailure()) {
                Toast.makeText(context, "Device has been compromised and the severity is: " + report.getMaxSeverity(), Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(context, "Device is safe.", Toast.LENGTH_LONG).show();
                mUIHandler.sendEmptyMessage(DEVICE_SAFE);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * class handles the calls for getting the BID report.
     */
    private class BidReportHandler extends Handler {
        public BidReportHandler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            switch (msg.what) {
                case GET_BID_REPORT:
                    //do the processing and get the device state
                    queryStatus();
            }
        }
    }
}
