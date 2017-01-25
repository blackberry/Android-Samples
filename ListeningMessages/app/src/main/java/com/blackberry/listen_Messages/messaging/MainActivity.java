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

package com.blackberry.listen_Messages.messaging;

import android.Manifest;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.Button;

import com.blackberry.listen_Messages.R;

public class MainActivity extends Activity {

    private Button start;
    private Button stop;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        start= (Button) findViewById(R.id.button2);
        stop= (Button) findViewById(R.id.button);
          enableControls();
        stop.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                stopListening();
            }
        });
    }


    private void enableControls() {

        start.setEnabled(true);
        stop.setEnabled(false);

    }

    public void startlistening() {
        startService(new Intent(this, MyService.class));
        start.setEnabled(false);
        stop.setEnabled(true);
    }


    private void stopListening() {
        stopService(new Intent(this, MyService.class));
        start.setEnabled(true);
        stop.setEnabled(false);
    }


    private static final int PERMISSION_REQUEST_CODE = 1;
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startlistening();

                } else {
                }
                break;
        }
    }

    public void startService(View view) {

        if (!checkPermission()) {

            requestPermission();

        } else {

            startlistening();

        }
    }

    private boolean checkPermission(){
        int result = ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.READ_SMS);
        if (result == PackageManager.PERMISSION_GRANTED){

            return true;

        } else {

            return false;

        }
    }

    private void requestPermission(){

        if (ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.READ_SMS)){

            showDialogOK("SMS Services Permission required for this app",
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            switch (which) {
                                case DialogInterface.BUTTON_POSITIVE:
                                    ActivityCompat.requestPermissions(MainActivity.this,new String[]{Manifest.permission.READ_SMS},PERMISSION_REQUEST_CODE);
                                    break;
                                case DialogInterface.BUTTON_NEGATIVE:

                                    break;
                            }
                        }
                    });

        } else {

            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.READ_SMS},PERMISSION_REQUEST_CODE);
        }
    }

    private void showDialogOK(String message, DialogInterface.OnClickListener okListener) {
        new android.app.AlertDialog.Builder(this)
                .setMessage(message)
                .setPositiveButton("OK", okListener)
                .setNegativeButton("Cancel", okListener)
                .create()
                .show();
    }


}




