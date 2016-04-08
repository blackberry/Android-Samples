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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.util.Base64;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateFactory;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

import static com.blackberry.bidhelper.BidConstants.BIDE_CERTIFICATE_STRING;
import static com.blackberry.bidhelper.BidConstants.CERTIFICATE;
import static com.blackberry.bidhelper.BidConstants.CERTIFICATE_AVAILABLE;
import static com.blackberry.bidhelper.BidConstants.FAILURE_REPORT_STRING;
import static com.blackberry.bidhelper.BidConstants.ID_KEY;
import static com.blackberry.bidhelper.BidConstants.JBIDE_REPORT;
import static com.blackberry.bidhelper.BidConstants.KBIDE_REPORT;
import static com.blackberry.bidhelper.BidConstants.NONCE_KEY;
import static com.blackberry.bidhelper.BidConstants.NONCE_RADIX;
import static com.blackberry.bidhelper.BidConstants.REPORT_INSERTED;
import static com.blackberry.bidhelper.BidConstants.STATUS_REPORT_STRING;
import static com.blackberry.bidhelper.BidConstants.TZ_REPORT;
import static com.blackberry.bidhelper.BidConstants.TZ_SIGNATURE;

/**
 * Android implementation of the <code>BidHelper</code> interface.
 * <p/>
 * This class provides convenience methods for obtaining status reports
 * and failure reports from the BID content provider. After a request
 * completes successfully, the caller is responsible for verifying the
 * report object before attempting to access the contents of the report.
 * An attempt to access the contents of an un-verified report will cause
 * an exception to be thrown.
 * <p/>
 * This class can also register listeners for BID-related events such
 * as the insertion of a report and the availability of a certificate.
 * <p/>
 * To facilitate porting to non-Android platforms, this class should be
 * the only class in this package that depends on Android APIs.
 * <p/>
 * Using the helper API is a simple matter of constructing the helper, registering
 * for events (optional), requesting a report, verifying the report, and destroying
 * the helper when it is no longer needed:
 * <p/>
 * If you choose not to use the helper API and would rather query the content
 * provider directly, please see <code>BidConstants</code> for all of the
 * useful string constants that you will need. For example, to query all failure
 * reports:
 * <pre>
 *     Cursor cursor = getContentResolver().query(
 *         BidConstants.FAILURE_REPORT_STRING,
 *         null, null, null, null);
 * </pre>
 * Then access the rows and columns of the result using standard database methods:
 * <pre>
 *     cursor.moveToFirst();
 *     int kbideReportIndex = cursor.getColumnIndexOrThrow(KBIDE_REPORT);
 *     String kbideReport = cursor.getString(kbideReportIndex);
 * </pre>
 * However when using this approach keep in mind that the reports are not validated,
 * and you are responsible to validate the nonce, the hashes, and the signature
 * prior to attempting to access the contents of the reports.
 * <p/>
 * If you choose not to use the helper API and would rather register manually for
 * BID broadcasts, you will need to write your own subclass of BroadcastReceiver
 * like this:
 * <pre>
 *     public class MyBroadcastReceiver extends BroadcastReceiver {
 *
 *     &#64;Override
 *     public void onReceive(Context context, Intent intent) {
 *         if (intent.getAction().equals(BidConstants.CERTIFICATE_AVAILABLE)) {
 *             //...process the event
 *         } else if (intent.getAction().equals(BidConstants.REPORT_INSERTED)) {
 *             //...process the event
 *         }
 *     }
 * </pre>
 * Also, if you choose not to use the helper API, you will need to add a
 * &lt;receiver&gt; tag like the following inside the &lt;application&gt; tag of
 * your app's AndroidManifest.xml file:
 * <pre>
 *     &lt;receiver android:name=".MyBroadcastReceiver" android:enabled="true"&gt;
 *         &lt;intent-filter&gt;
 *             &lt;action android:name="com.blackberry.bide.CERTIFICATE_AVAILABLE"/&gt;
 *             &lt;action android:name="com.blackberry.bide.REPORT_INSERTED"/&gt;
 *         &lt;/intent-filter&gt;
 *     &lt;/receiver&gt;
 * </pre>
 */
public final class BidHelperAndroid implements BidHelper {
    /**
     * Content URI for status reports.
     */
    public static final Uri STATUS_REPORT_URI = Uri.parse(STATUS_REPORT_STRING);

    /**
     * Content URI for failure reports.
     */
    public static final Uri BIDE_CERTIFICATE_URI = Uri.parse(BIDE_CERTIFICATE_STRING);

    /**
     * Content URI for failure reports.
     */
    public static final Uri FAILURE_REPORT_URI = Uri.parse(FAILURE_REPORT_STRING);

    /**
     * Projection used for content provider queries.
     */
    private static final String[] PROJECTION = {JBIDE_REPORT, KBIDE_REPORT, TZ_REPORT, TZ_SIGNATURE};

    /**
     * Bid Certificate
     */
    private X509Certificate bidCert = null;
    private Context context;
    private List<BidListener> listeners;
    private BidReceiver receiver;
    /**
     * Create a new BID helper for Android based on the specified application
     * context.
     *
     * @param context the application context.
     */
    public BidHelperAndroid(Context context) {
        this.context = context;
        listeners = new ArrayList<BidListener>();
        receiver = new BidReceiver();
        context.registerReceiver(receiver, receiver.getIntentFilter());
    }

    /**
     * Creates an un-verified status report object populated with the specified
     * values.
     *
     * @param cursor a pre-positioned cursor containing raw, un-verified values
     *               for creating the report.
     * @return an un-verified status report object populated with the specified
     * values.
     */
    private static BidStatusReport createStatusReport(Cursor cursor) {
        int jbideReportIndex = cursor.getColumnIndexOrThrow(JBIDE_REPORT);
        int kbideReportIndex = cursor.getColumnIndexOrThrow(KBIDE_REPORT);
        int tzReportIndex = cursor.getColumnIndexOrThrow(TZ_REPORT);
        int tzSignatureIndex = cursor.getColumnIndexOrThrow(TZ_SIGNATURE);
        return new BidStatusReport(cursor.getString(jbideReportIndex),
                cursor.getString(kbideReportIndex),
                cursor.getString(tzReportIndex),
                cursor.getBlob(tzSignatureIndex),
                getBase64Decoder());
    }

    /**
     * Creates an un-verified failure report object populated with the specified
     * values.
     *
     * @param cursor a pre-positioned cursor containing raw, un-verified values
     *               for creating the report.
     * @return an un-verified failure report object populated with the specified
     * values.
     */
    private static BidFailureReport createFailureReport(Cursor cursor) {
        int jbideReportIndex = cursor.getColumnIndexOrThrow(JBIDE_REPORT);
        int kbideReportIndex = cursor.getColumnIndexOrThrow(KBIDE_REPORT);
        int tzReportIndex = cursor.getColumnIndexOrThrow(TZ_REPORT);
        int tzSignatureIndex = cursor.getColumnIndexOrThrow(TZ_SIGNATURE);
        return new BidFailureReport(cursor.getString(jbideReportIndex),
                cursor.getString(kbideReportIndex),
                cursor.getString(tzReportIndex),
                cursor.getBlob(tzSignatureIndex),
                getBase64Decoder());
    }

    /**
     * Creates an un-verified certificate.
     *
     * @param cursor a pre-positioned cursor containing raw, un-verified certificate.
     * @return an un-verified certificate.
     */
    private static byte[] getCertificate(Cursor cursor) {

        int certificateIndex = cursor.getColumnIndexOrThrow(CERTIFICATE);
        return cursor.getBlob(certificateIndex);
    }

    /**
     * Returns a <code>Base64Decoder</code> implementation ready to use.
     * (This is needed due to Java's lack of a cross-platform Base64 facility).
     *
     * @return a <code>Base64Decoder</code> implementation ready to use.
     */
    private static Base64Decoder getBase64Decoder() {
        //
        // Unfortunately there is no common Base64 utility that exists
        // in both Android and desktop Java, hence the need for the
        // Base64Decoder interface here. Android has android.util.Base64.
        // Desktop Java versions 6 and 7 have methods for Base64 encoding
        // and decoding in javax.xml.bind.DatatypeConverter. Desktop Java
        // version 8 introduced java.util.Base64 but alas too late for
        // Android adoption.
        //
        return new Base64Decoder() {
            @Override
            public byte[] decode(String s) {
                byte[] result = null;
                try {
                    result = Base64.decode(s, Base64.DEFAULT);
                } catch (IllegalArgumentException iae) {
                    // do nothing - result will be null
                }
                return result;
            }
        };
    }

    @Override
    public void destroy() {
        context.unregisterReceiver(receiver);
    }

    @Override
    public void addBidListener(BidListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    @Override
    public void removeBidListener(BidListener listener) {
        listeners.remove(listener);
    }

    @Override
    public boolean isCertificateAvailable() {
        if (bidCert == null) {
            // We have not retrieved the certificate yet. Let's do that now
            byte[] certificateBytes = null;
            try {
                certificateBytes = requestBidCertificate();
            } catch (BidCertificateUnavailableException ex) {
                // Certificate is not available
                return false;
            } catch (BidRequestException bre) {
                return false;
            }

            // create an X509Certificate from the byte array
            ByteArrayInputStream bis = new ByteArrayInputStream(certificateBytes);
            try {
                CertificateFactory cf = CertificateFactory.getInstance("X.509");
                bidCert = (X509Certificate) cf.generateCertificate(bis);
            } catch (CertificateException ce) {
                return false;
            }

        }

        if (bidCert != null) {
            try {
                bidCert.checkValidity();
            } catch (CertificateExpiredException cee) {
                bidCert = null;
                return false;

            } catch (CertificateNotYetValidException cnyve) {
                return false;
            }

            return true;
        } else {
            return false;
        }
    }

    @Override
    public BidStatusReport requestStatusReport(BigInteger nonce)
            throws BidRequestException {
        try {
            //
            // Form the URI and query the content provider.
            //
            Uri.Builder builder = STATUS_REPORT_URI.buildUpon();
            builder.appendQueryParameter(NONCE_KEY, nonce.toString(NONCE_RADIX));
            Cursor cursor = context.getContentResolver().query(builder.build(), PROJECTION, null, null, null);

            //
            // Check for the unexpected.
            //
            if (cursor == null) {
                throw new BidRequestException("null cursor for query " + STATUS_REPORT_STRING);
            }
            if (cursor.getCount() != 1) {
                throw new BidRequestException("unexpected row count for query " + STATUS_REPORT_STRING);
            }
            if (!cursor.moveToFirst()) {
                throw new BidRequestException("cursor failed for query " + STATUS_REPORT_STRING);
            }

            //
            // Populate and return the report.
            //
            return createStatusReport(cursor);
        } catch (Exception e) {
            throw new BidRequestException(e);
        }
    }

    @Override
    public void verifyStatusReport(BidStatusReport report, BigInteger nonce, boolean certReqd)
            throws BidCertificateUnavailableException,
            IOException,
            BidHashMismatchException,
            BidSignatureVerificationException,
            BidNonceMismatchException,
            CertificateException {
        if (certReqd && !isCertificateAvailable()) {
            throw new BidCertificateUnavailableException("certificate not available");
        }
        // get certificate verifier - platform specific, pass in certificate
        BidCertificateVerifier bcv = new BidCertificateVerifierAndroid();
        bcv.setCertificate(bidCert);
        bcv.verifyCertificateChain();

        report.verify(bcv);
        if (!nonce.equals(report.getNonce())) {
            report.revokeVerification();
            throw new BidNonceMismatchException("reported nonce does not match expected value");
        }
    }

    @Override
    public byte[] requestBidCertificate()
            throws BidCertificateUnavailableException,
            BidRequestException {
        try {
            //
            // Form the URI and query the content provider.
            //
            Cursor cursor = context.getContentResolver().query(BIDE_CERTIFICATE_URI, PROJECTION, null, null, null);

            //
            // Check for the unexpected.
            //
            if (cursor == null) {
                throw new BidCertificateUnavailableException("null cursor for query " + BIDE_CERTIFICATE_URI);
            }
            if (cursor.getCount() != 1) {
                throw new BidRequestException("unexpected row count for query " + BIDE_CERTIFICATE_URI);
            }
            if (!cursor.moveToFirst()) {
                throw new BidRequestException("cursor failed for query " + BIDE_CERTIFICATE_URI);
            }

            //
            // Get the byte array representing the Bid certificate.
            //
            return getCertificate(cursor);
        } catch (Exception e) {
            throw new BidRequestException(e);
        }
    }

    @Override
    public BidFailureReport requestFailureReport(String reportId)
            throws BidRequestException {
        try {
            //
            // Form the URI and query the content provider.
            //
            Uri.Builder builder = FAILURE_REPORT_URI.buildUpon();
            builder.appendQueryParameter(ID_KEY, reportId);
            Cursor cursor = context.getContentResolver().query(builder.build(), PROJECTION, null, null, null);

            //
            // Check for the unexpected.
            //
            if (cursor == null) {
                throw new BidRequestException("null cursor for query " + FAILURE_REPORT_STRING);
            }
            if (cursor.getCount() != 1) {
                throw new BidRequestException("unexpected row count for query " + FAILURE_REPORT_STRING);
            }
            if (!cursor.moveToFirst()) {
                throw new BidRequestException("cursor failed for query " + FAILURE_REPORT_STRING);
            }

            //
            // Populate and return the report.
            //
            return createFailureReport(cursor);
        } catch (Exception e) {
            throw new BidRequestException(e);
        }
    }

    @Override
    public BidFailureReport[] requestAllFailureReports()
            throws BidRequestException {
        try {
            //
            // Query the content provider.
            //
            Cursor cursor = context.getContentResolver().query(FAILURE_REPORT_URI, PROJECTION, null, null, null);

            //
            // Check for the unexpected.
            //
            if (cursor == null) {
                throw new BidRequestException("null cursor for query " + FAILURE_REPORT_STRING);
            }

            //
            // Populate and return the reports.
            //
            int count = cursor.getCount();
            BidFailureReport[] result = new BidFailureReport[count];
            for (int i = 0; i < count; i++) {
                cursor.moveToPosition(i);
                result[i] = createFailureReport(cursor);
            }
            return result;
        } catch (Exception e) {
            throw new BidRequestException(e);
        }
    }

    @Override
    public void verifyFailureReport(BidFailureReport report, boolean certReqd)
            throws BidCertificateUnavailableException,
            IOException,
            BidHashMismatchException,
            BidSignatureVerificationException,
            CertificateException {
        if (certReqd && !isCertificateAvailable()) {
            throw new BidCertificateUnavailableException("certificate not available");
        }
        // get certificate verifier - platform specific, pass in certificate
        BidCertificateVerifier bcv = new BidCertificateVerifierAndroid();
        bcv.setCertificate(bidCert);
        bcv.verifyCertificateChain();

        report.verify(bcv);
    }

    /**
     * A BID broadcast receiver for firing <code>BidListener</code> events.
     */
    private final class BidReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(CERTIFICATE_AVAILABLE)) {
                for (BidListener listener : listeners) {
                    listener.certificateAvailable();
                }
            } else if (intent.getAction().equals(REPORT_INSERTED)) {
                for (BidListener listener : listeners) {
                    listener.reportInserted();
                }
            }
        }

        /**
         * Returns an intent filter corresponding to BID broadcast events,
         * used at registration to limit the scope of the receiver.
         *
         * @return an intent filter corresponding to BID broadcast events.
         */
        public IntentFilter getIntentFilter() {
            IntentFilter result = new IntentFilter();
            result.addAction(CERTIFICATE_AVAILABLE);
            result.addAction(REPORT_INSERTED);
            return result;
        }
    }
}
