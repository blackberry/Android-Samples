/*
 * Copyright (c) 2011-2015 BlackBerry Limited.
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

package blackberry.msohm.com.sliderdemo;

import android.content.res.Configuration;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

import java.util.Date;

public class MainActivity extends AppCompatActivity
{

    private TextView m_tvKeyboard;
    private TextView m_tvKeyboardHidden;
    private TextView m_tvHardKeyboardHidden;
    private TextView m_tvEventLog;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        m_tvKeyboard = (TextView)findViewById(R.id.valueKeyboard);
        m_tvKeyboard.setText("");

        m_tvKeyboardHidden = (TextView)findViewById(R.id.valueKeyboardHidden);
        m_tvKeyboardHidden.setText("");

        m_tvHardKeyboardHidden = (TextView)findViewById(R.id.valueHardKeyboardHidden);
        m_tvHardKeyboardHidden.setText("");

        m_tvEventLog = (TextView)findViewById(R.id.eventlog);
    }

    void readConfiguration(){
        Configuration conf = getResources().getConfiguration();

        String keyboardValue;
        switch(conf.keyboard) {
            case Configuration.KEYBOARD_NOKEYS: keyboardValue = "KEYBOARD_NOKEYS"; break;
            case Configuration.KEYBOARD_12KEY: keyboardValue = "KEYBOARD_12KEY"; break;
            case Configuration.KEYBOARD_QWERTY: keyboardValue = "KEYBOARD_QWERTY"; break;
            default: keyboardValue = "Unknown";
        }
        if(!m_tvKeyboard.getText().toString().isEmpty() && (m_tvKeyboard.getText().toString() != keyboardValue))
            m_tvEventLog.append("    'keyboard':" + m_tvKeyboard.getText().toString() + "-->" + keyboardValue + "\n");
        m_tvKeyboard.setText(keyboardValue);

        String keyboardHiddenValue = (conf.keyboardHidden == Configuration.KEYBOARDHIDDEN_NO)?"KEYBOARDHIDDEN_NO":"KEYBOARDHIDDEN_YES";
        if(!m_tvKeyboardHidden.getText().toString().isEmpty() && (m_tvKeyboardHidden.getText().toString() != keyboardHiddenValue))
            m_tvEventLog.append("    'keyboardHidden':" + m_tvKeyboardHidden.getText().toString() + "-->" + keyboardHiddenValue + "\n");
        m_tvKeyboardHidden.setText(keyboardHiddenValue);

        String hardKeyboardHiddenValue = (conf.hardKeyboardHidden == Configuration.HARDKEYBOARDHIDDEN_NO) ? "HARDKEYBOARDHIDDEN_NO" : "HARDKEYBOARDHIDDEN_YES";
        if(!m_tvHardKeyboardHidden.getText().toString().isEmpty() && (m_tvHardKeyboardHidden.getText().toString() != hardKeyboardHiddenValue))
            m_tvEventLog.append("    'hardKeyboardHidden':" + m_tvHardKeyboardHidden.getText().toString() + "-->" + hardKeyboardHiddenValue + "\n");
        m_tvHardKeyboardHidden.setText(hardKeyboardHiddenValue);
    }

    @Override
    protected void onResume() {
        super.onResume();
        m_tvEventLog.append("Activity started \n");
        readConfiguration();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        m_tvEventLog.append((new Date()).toString() + " ConfigChange : \n--------------------------\n");
        readConfiguration();

    }

}
