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
package com.writenfctag;

import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.io.IOException;

/**
 * This activity helps in writing NFC Codes on tags.
 */
public class MainActivity extends AppCompatActivity {

    public static final String TAG = "WriteNFCTagActivity";
    public static final boolean DEBUG = true;
    boolean mWriteMode = false;
    private NfcAdapter mNfcAdapter;
    private PendingIntent mNfcPendingIntent;
    private AlertDialog alert;
    private EditText startingTag;
    private EditText endingTag;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button go = ((Button) findViewById(R.id.button));
        go.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                enableTagWriteMode();

                alert = new AlertDialog.Builder(MainActivity.this).setMessage("Touch Tag To Write")
                        .setOnCancelListener(new DialogInterface.OnCancelListener() {
                            @Override
                            public void onCancel(DialogInterface dialog) {
                                disableTagWriteMode();
                            }
                        }).create();
                alert.show();

            }
        });
    }

    private void enableTagWriteMode() {
        if (DEBUG) {
            Log.i(TAG, "enableTagWriteMode entered");
        }
        mWriteMode = true;
    }

    private void disableTagWriteMode() {
        if (DEBUG) {
            Log.i(TAG, "disableTagWriteMode entered");
        }
        mWriteMode = false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        mNfcAdapter = NfcAdapter.getDefaultAdapter(MainActivity.this);
        mNfcPendingIntent = PendingIntent.getActivity(MainActivity.this, 0,
                new Intent(MainActivity.this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);

        IntentFilter tagDetected = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
        IntentFilter[] mWriteTagFilters = new IntentFilter[]{tagDetected};
        mNfcAdapter.enableForegroundDispatch(this, mNfcPendingIntent, mWriteTagFilters, null);
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        // Tag writing mode
        startingTag = (EditText) findViewById(R.id.nfcInfo1);
        endingTag = (EditText) findViewById(R.id.nfcInfo2);

        String majorType = startingTag.getText().toString();
        String minorType = endingTag.getText().toString();
        String mimeType = null;
        if (DEBUG) {
            Log.i(TAG, "onNewIntent and value of intent : " + intent
                    + "and mWriteMode is:" + mWriteMode + "," + majorType.trim().length());
        }

        if (majorType.trim().length() > 0 && mWriteMode
                && NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction())) {
            Tag detectedTag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            if (minorType.trim().length() == 0) {
                mimeType = majorType;
            } else {
                mimeType = majorType + "/" + minorType;
            }
            //NdefRecord record = NdefRecord.createExternal(domain, type, payload);
            NdefRecord record = NdefRecord.createMime(mimeType, "nfctag".getBytes());
            NdefMessage message = new NdefMessage(new NdefRecord[]{record});
            if (writeTag(message, detectedTag)) {
                Toast.makeText(this, "Success", Toast.LENGTH_LONG)
                        .show();
            }
        } else if (majorType.trim().length() == 0) {
            Toast.makeText(this, "Invalid Major Mimetype", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Click On Write NFC Tag Button", Toast.LENGTH_SHORT).show();
        }
        if (alert != null) {
            alert.dismiss();
        }
        disableTagWriteMode();
    }

    /**
     * Writes an NdefMessage to a NFC tag
     *
     * @param message NdefMessage that need to be written
     * @param tag     The tag that needs to be written.
     * @return true in case it was successful else false.
     */
    public boolean writeTag(NdefMessage message, Tag tag) {
        int size = message.toByteArray().length;
        try {
            Ndef ndef = Ndef.get(tag);
            if (ndef != null) {
                ndef.connect();
                if (!ndef.isWritable()) {
                    Toast.makeText(getApplicationContext(),
                            "Error: tag not writable",
                            Toast.LENGTH_SHORT).show();
                    return false;
                }
                if (ndef.getMaxSize() < size) {
                    Toast.makeText(getApplicationContext(),
                            "Error: tag too small",
                            Toast.LENGTH_SHORT).show();
                    return false;
                }
                ndef.writeNdefMessage(message);
                return true;
            } else {
                NdefFormatable format = NdefFormatable.get(tag);
                if (format != null) {
                    try {
                        format.connect();
                        format.format(message);
                        return true;
                    } catch (IOException e) {
                        return false;
                    }
                } else {
                    return false;
                }
            }
        } catch (Exception e) {
            return false;
        }
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
}
