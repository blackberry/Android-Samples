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
import android.app.Service;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

public class MyService extends Service {



    private static final String TAG = "SMSTRACKER";
    MyObserver myObserver;
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // TODO Auto-generated method stub
        System.out.println("Inside service");
        Log.e(TAG, "inside onStartCommand");
        Toast.makeText(this, "Service started", Toast.LENGTH_LONG).show();

        myObserver = new MyObserver(new Handler());
        ContentResolver contentResolver = this.getApplicationContext().getContentResolver();
        contentResolver.registerContentObserver(Uri.parse("content://sms"), true, myObserver);
        return START_STICKY;
    }


    @Override
    public IBinder onBind(Intent arg0) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        getContentResolver().unregisterContentObserver(myObserver);
        Toast.makeText(this, "MyService Completed or Stopped.", Toast.LENGTH_SHORT).show();
    }

    class MyObserver extends ContentObserver {

        public MyObserver(Handler handler) {
            super(handler);
            Log.e(TAG, "inside observer constructor");
        }

        @Override
        public void onChange(boolean selfChange) {

            System.out.println("Inside onChange");
            Log.e(TAG, "inside onChange");
            Toast.makeText(getApplicationContext(), "Message listened", Toast.LENGTH_SHORT).show();
            super.onChange(selfChange);
            Uri uriSMSURI = Uri.parse("content://sms");
            Cursor cur = getContentResolver().query(uriSMSURI, null, null, null, null);
            cur.moveToNext();
            String content = cur.getString(cur.getColumnIndex("body"));
            Log.e(TAG, "inside content==="+content);
            String smsNumber = cur.getString(cur.getColumnIndex("address"));
            Log.e(TAG, "inside number"+smsNumber);
            if (smsNumber == null || smsNumber.length() <= 0) {
                smsNumber = "Unknown";
            }
            cur.close();
        }
    }
}

