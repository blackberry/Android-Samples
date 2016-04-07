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
 * Constants used for BIDE requests and report parsing.
 */
public interface BidConstants {
    /**
     * Intent string for broadcasts of the certificate available event.
     */
    public static final String CERTIFICATE_AVAILABLE = "com.blackberry.bide.CERTIFICATE_AVAILABLE";

    /**
     * Intent string for broadcasts of the report inserted event.
     */
    public static final String REPORT_INSERTED = "com.blackberry.bide.REPORT_INSERTED";

    /**
     * Content URI string for status reports.
     */
    public static final String STATUS_REPORT_STRING = "content://com.blackberry.bide/status";

    /**
     * Name of the nonce query parameter for status reports.
     */
    public static final String NONCE_KEY = "nonce";

    /**
     * Radix for the nonce (16, for hexadecimal).
     */
    public static final int NONCE_RADIX = 16;

    /**
     * Content URI string for failure reports.
     */
    public static final String FAILURE_REPORT_STRING = "content://com.blackberry.bide/reports";

    /**
     * Content URI string for failure reports.
     */
    public static final String BIDE_CERTIFICATE_STRING = "content://com.blackberry.bide/certificate";

    /**
     * Name of the query parameter for report ID.
     */
    public static final String ID_KEY = "id";

    /**
     * Column name for date received.
     */
    public static final String DATE_RECEIVED = "DATE_RECEIVED";

    /**
     * Column name for JBIDE report.
     */
    public static final String JBIDE_REPORT = "JBIDE_REPORT";

    /**
     * Column name for KBIDE report.
     */
    public static final String KBIDE_REPORT = "KBIDE_REPORT";

    /**
     * Column name for TZ report.
     */
    public static final String TZ_REPORT = "TZ_REPORT";

    /**
     * Column name for TZ signature.
     */
    public static final String TZ_SIGNATURE = "TZ_SIGNATURE";

    /**
     * Column name for TZ report ID.
     */
    public static final String TZ_REPORT_ID = "TZ_REPORT_ID";

    /**
     * Column name for severity number.
     */
    public static final String SEVERITY_NUMBER = "SEVERITY_NUMBER";

    /**
     * Column name for Certificate.
     */
    static final String CERTIFICATE = "CERTIFICATE";


    /**
     * Name of the XML bidestatus tag.
     */
    public static final String TAG_BIDE_STATUS = "bidestatus";

    /**
     * Name of the XML devicemodel tag.
     */
    public static final String TAG_DEVICEMODEL = "devicemodel";

    /**
     * Name of the XML failedreports tag.
     */
    public static final String TAG_FAILED_REPORTS = "failedreports";

    /**
     * Name of the XML failure tag.
     */
    public static final String TAG_FAILURE = "failure";

    /**
     * Name of the XML jbidehash tag.
     */
    public static final String TAG_JBIDEHASH = "jbidehash";

    /**
     * Name of the XML kbidehash tag.
     */
    public static final String TAG_KBIDEHASH = "kbidehash";

    /**
     * Name of the XML nonce tag.
     */
    public static final String TAG_NONCE = "nonce";

    /**
     * Name of the XML osversion tag.
     */
    public static final String TAG_OSVERSION = "osversion";

    /**
     * Name of the XML tag for report ID in the TZ report.
     */
    public static final String TAG_REPORTID = "reportid";

    /**
     * Name of the XML severity tag.
     */
    public static final String TAG_SEVERITY = "severity";

    /**
     * Name of the XML status tag.
     */
    public static final String TAG_STATUS = "status";

    /**
     * Name of the XML status tag.
     */
    public static final String TAG_SIGNATURE_TOKEN_VALUE = "sigtokenvalue";

    /**
     * Name of the XML time tag.
     */
    public static final String TAG_TIME = "time";

    /**
     * Name of the XML sensor attribute.
     */
    public static final String ATT_SENSOR = "sensor";

    /**
     * Name of the XML severity attribute.
     */
    public static final String ATT_SEVERITY = "severity";
    /**
     * Name of the XML High Security device tokens tag.
     */
    static final String TAG_HIGH_TOKENS = "highsecuritytokens";

    /**
     * Name of the XML Medium Security device tokens tag.
     */
    static final String TAG_MEDIUM_TOKENS = "mediumsecuritytokens";

    /**
     * Name of the XML Low Security device tokens tag.
     */
    static final String TAG_LOW_TOKENS = "lowsecuritytokens";

    /**
     * Name of the XML devicetoken tag.
     */
    static final String TAG_TOKEN = "token";
}
