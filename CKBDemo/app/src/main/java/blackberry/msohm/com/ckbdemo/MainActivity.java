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

package blackberry.msohm.com.ckbdemo;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity
{

    private final String LOG_TAG = MainActivity.class.getSimpleName();

    private TextView m_tvEventLog;
    private int logCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        m_tvEventLog = (TextView)findViewById(R.id.eventlog);
    }



    // Used to get touchpad events.
    @Override
    public boolean onGenericMotionEvent(MotionEvent event) {

        if (event.getSource() == InputDevice.SOURCE_TOUCHPAD) {
            dumpEvent(event);
        }
        return false;
    }

/** Show an event in the LogCat view, for debugging */
private void dumpEvent(MotionEvent event) {
    String names[] = { "DOWN" , "UP" , "MOVE" , "CANCEL" , "OUTSIDE" ,
            "POINTER_DOWN" , "POINTER_UP" , "7?" , "8?" , "9?" };
    StringBuilder sb = new StringBuilder();
    final int action = event.getAction();
    final int actionCode = action & MotionEvent.ACTION_MASK;
    final int pointerCount = event.getPointerCount();


    if (logCount > 100)
    {
        m_tvEventLog.setText("TextView Cleared...\n");
        logCount = 0;
    }

    // Move events can contain historical data representing batching of events.
    // We should consume the historical data first.
    if (actionCode == MotionEvent.ACTION_MOVE) {
        final int historySize = event.getHistorySize();
        for (int h = 0; h < historySize; h++) {
            sb.append("event ACTION_MOVE[" );
            for (int i = 0; i < pointerCount; i++) {
                sb.append("#" ).append(i);
                sb.append("(pid " ).append(event.getPointerId(i));
                sb.append(")=" ).append((int) event.getHistoricalX(i, h));
                sb.append("," ).append((int) event.getHistoricalY(i, h));
                if (i + 1 < pointerCount)
                    sb.append(";" );
            }
            sb.append("] - historical, at time ");
            sb.append(event.getHistoricalEventTime(h));
            Log.d(LOG_TAG, sb.toString());
            sb.setLength(0);
        }
    }
    sb.append("event ACTION_" ).append(names[actionCode]);
    // Down and up events still contain current coordinates for all active pointers, but only one ID
    // matches the action
    if (actionCode == MotionEvent.ACTION_POINTER_DOWN || actionCode == MotionEvent.ACTION_POINTER_UP) {
        sb.append("(pid ");
        sb.append(event.getPointerId(event.getActionIndex()));
        sb.append(")" );
    }
    sb.append("[" );
    for (int i = 0; i < pointerCount; i++) {
        sb.append("#" ).append(i);
        sb.append("(pid " ).append(event.getPointerId(i));
        sb.append(")=" ).append((int) event.getX(i));
        sb.append("," ).append((int) event.getY(i));
        if (i + 1 < pointerCount)
            sb.append(";" );
    }
    sb.append("] - max " );

    // We shouldn't really need to get the device's maximum range for every event, since it's
    // unlikely to change.

    final InputDevice device = InputDevice.getDevice(event.getDeviceId());
    sb.append((int) device.getMotionRange(MotionEvent.AXIS_X).getMax()).append("," );
    sb.append((int) device.getMotionRange(MotionEvent.AXIS_Y).getMax());
    sb.append(", at time ").append(event.getEventTime());
    sb.append("\n");
    Log.d(LOG_TAG, sb.toString());

    m_tvEventLog.setText(sb.toString() + m_tvEventLog.getText());
    logCount++;
}

}
