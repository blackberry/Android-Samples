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
package com.blackberry.bidhelper;

/**
 * Encapsulates the detailed information in a BID failure.
 */
public final class BidFailure {
    private final String sensorName;
    private final int severity;
    private final String details;

    /**
     * Creates a new instance based on the specified values.
     *
     * @param sensorName the name of the sensor.
     * @param severity   the severity level assigned to this failure.
     * @param details    the detailed message.
     */
    BidFailure(String sensorName, int severity, String details) {
        this.sensorName = sensorName;
        this.severity = severity;
        this.details = details;
    }

    /**
     * Returns the sensor name for this failure.
     *
     * @return the sensor name for this failure.
     */
    public final String getSensorName() {
        return sensorName;
    }

    /**
     * Returns the severity level for this failure.
     *
     * @return the severity level for this failure.
     */
    public final int getSeverity() {
        return severity;
    }

    /**
     * Returns the detailed message for this failure.
     *
     * @return the detailed message for this failure.
     */
    public final String getDetails() {
        return details;
    }
}