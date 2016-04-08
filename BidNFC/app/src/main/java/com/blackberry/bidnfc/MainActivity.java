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
package com.blackberry.bidnfc;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.blackberry.bidhelper.BidHelperAndroid;
import com.blackberry.bidhelper.BidListener;
import com.blackberry.bidhelper.BidSignatureVerificationException;
import com.blackberry.bidhelper.BidStatusReport;

import java.math.BigInteger;
import java.util.Random;

/**
 * This class how to use the BID in NFC transaction scenario.
 * In this class it is being done by a dummy transaction.
 */
public class MainActivity extends AppCompatActivity implements BidListener {

    public static final int GET_BID_REPORT = 1;
    public static final int DEVICE_COMPROMISED = 2;
    public static final int DEVICE_SAFE = 3;

    private final HandlerThread mHandlerThread = new HandlerThread("MainActivity", Process.THREAD_PRIORITY_BACKGROUND);
    private BidReportHandler mHandler;
    private Handler mUIHandler;

    private BidHelperAndroid mHelper;

    RelativeLayout progressBarLayout;
    RelativeLayout transactionLayout;

    ProgressBar progress;
    TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (Build.MANUFACTURER.equalsIgnoreCase("BlackBerry") && (Build.MODEL).startsWith("STV")) {
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1) {
                mHelper = new BidHelperAndroid(this);
                mHelper.addBidListener(this);

                progressBarLayout = (RelativeLayout) findViewById(R.id.progressView);
                transactionLayout = (RelativeLayout) findViewById(R.id.transcation);

                progress = (ProgressBar) findViewById(R.id.progressBar1);
                textView = (TextView) findViewById(R.id.textView1);
                progressBarLayout.setVisibility(View.VISIBLE);
                transactionLayout.setVisibility(View.INVISIBLE);

                mUIHandler = new Handler() {
                    public void handleMessage(Message msg) {
                        final int what = msg.what;
                        switch (what) {
                            case DEVICE_SAFE:
                                safeView();
                                break;
                            case DEVICE_COMPROMISED:
                                UnsafeView();
                                break;
                        }
                    }
                };
                mHandlerThread.start();
                mHandler = new BidReportHandler(mHandlerThread.getLooper());
                mHandler.sendEmptyMessage(GET_BID_REPORT);
            } else {
                Toast.makeText(this, R.string.version_msg, Toast.LENGTH_LONG).show();
            }
        } else {
            Toast.makeText(this, R.string.warning_msg, Toast.LENGTH_LONG).show();
        }


    }
    /**
     * In case the device is in compromised state. The activity will not
     * complete the transaction and will move tot ErrorActivity.
     */
    private void UnsafeView() {
        Intent i = new Intent(this, ErrorActivity.class);
        startActivity(i);
    }

    /**
     * In case the device is secure, the transaction completes successfully.
     */
    private void safeView() {
        progressBarLayout.setVisibility(View.GONE);
        transactionLayout.setVisibility(View.VISIBLE);
        Toast.makeText(this, "Transaction Completed", Toast.LENGTH_SHORT).show();
    }

    /**
     * This function do a fake transaction.
     */
    public void startTransaction() {
        // do something long
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i <= 10; i++) {
                    final int value = i;
                    doFakeWork();
                    progress.post(new Runnable() {
                        @Override
                        public void run() {
                            textView.setText("Transaction In Progress....");
                            progress.setProgress(value);
                        }
                    });
                }
                mUIHandler.sendEmptyMessage(DEVICE_SAFE);
            }
        };
        new Thread(runnable).start();
    }

    // Simulating something time consuming
    private void doFakeWork() {
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mHelper != null) {
            mHelper.destroy();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        //super.onNewIntent(intent);
        Log.i(PaymentInfo.TAG, "Foreground Dispatch Main Activity: " + intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
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
     * A handler class to handle the queue.
     */
    private class BidReportHandler extends Handler {
        public BidReportHandler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            switch (msg.what) {
                case GET_BID_REPORT:
                    // We will first query the status and if the device is
                    // safe only then continue.
                    if (queryStatus()) {
                        startTransaction();
                    } else {
                        Toast.makeText(getApplicationContext(), "Transaction Failed", Toast.LENGTH_LONG).show();
                    }

            }
        }
    }

    /**
     * Call to the BID framework to get the status of the device.
     * @return the status of the device (safe or compromised).
     */
    private boolean queryStatus() {
        boolean safe = false;
        try {
            BigInteger bi = new BigInteger(256, new Random());
            BidStatusReport report = mHelper.requestStatusReport(bi);
            try {
                mHelper.verifyStatusReport(report, bi, true);
            } catch (BidSignatureVerificationException ex) {
                report.bypassVerification();
            }
            if (report.hasFailure()) {
                Toast.makeText(getApplicationContext(), "Device has been compromised and the severity is: " + report.getMaxSeverity(), Toast.LENGTH_LONG).show();
            } else {
                safe = true;
                Toast.makeText(getApplicationContext(), "Device is safe.", Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return safe;
    }
}
