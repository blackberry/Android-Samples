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

import org.xml.sax.SAXException;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.blackberry.bidhelper.BidConstants.NONCE_RADIX;
import static com.blackberry.bidhelper.BidConstants.TAG_FAILED_REPORTS;
import static com.blackberry.bidhelper.BidConstants.TAG_HIGH_TOKENS;
import static com.blackberry.bidhelper.BidConstants.TAG_LOW_TOKENS;
import static com.blackberry.bidhelper.BidConstants.TAG_MEDIUM_TOKENS;
import static com.blackberry.bidhelper.BidConstants.TAG_NONCE;
import static com.blackberry.bidhelper.BidConstants.TAG_REPORTID;
import static com.blackberry.bidhelper.BidConstants.TAG_SEVERITY;
import static com.blackberry.bidhelper.BidConstants.TAG_SIGNATURE_TOKEN_VALUE;
import static com.blackberry.bidhelper.BidConstants.TAG_STATUS;
import static com.blackberry.bidhelper.BidConstants.TAG_TIME;
import static com.blackberry.bidhelper.BidConstants.TAG_TOKEN;

/**
 * This class contains methods for accessing the contents of a BID
 * status report.
 * <p/>
 * Initially the report is un-verified. To verify a report, call the
 * appropriate method in the <code>BidHelper</code> class for your
 * platform.
 * <p/>
 * An attempt to access the contents of an un-verified report will cause
 * an exception to be thrown.
 */
public final class BidStatusReport extends BidVerifiable {

    public enum Severity {
        HIGH, MEDIUM, LOW
    };

    /**
     * A handler specific to BID status reports. The handler uses parser events
     * to verify hash values and to populate the report object.
     */
    private class StatusXmlHandler extends BidXmlHandler {
        private List<String> idList;
        private List<String> highSecurityTokenList;
        private List<String> mediumSecurityTokenList;
        private List<String> lowSecurityTokenList;

        @Override
        public void startDocumentSet()
                throws SAXException {
            idList = new ArrayList<String>();
            highSecurityTokenList = new ArrayList<String>();
            mediumSecurityTokenList = new ArrayList<String>();
            lowSecurityTokenList = new ArrayList<String>();
        }

        @Override
        public void characters(char[] ch, int start, int length)
                throws SAXException {
            if (isTypeAndTag(KBIDE_XML_TYPE, TAG_TIME)) {
                time = new String(ch, start, length);
            } else if (isTypeAndTag(JBIDE_XML_TYPE, TAG_NONCE)) {
                try {
                    String s = new String(ch, start, length);
                    nonce = new BigInteger(s, NONCE_RADIX);
                } catch (NumberFormatException nfe) {
                    throw new SAXException(nfe);
                }
            } else if (isTypeAndTag(TZ_XML_TYPE, TAG_STATUS)) {
                String s = new String(ch, start, length);
                // Specify an exact match for "pass" so that
                // we error on the side of safety (i.e., in
                // reporting a "fail" status if the exact match
                // fails).
                hasTZFailure = !s.equals("0x0000");
            } else if (isTypeAndTag(JBIDE_XML_TYPE, TAG_STATUS)) {
                String s = new String(ch, start, length);
                // Specify an exact match for "pass" so that
                // we error on the side of safety (i.e., in
                // reporting a "fail" status if the exact match
                // fails).
                hasJBIDEFailure = !s.equals("pass");
            } else if (isTypeAndTag(JBIDE_XML_TYPE, TAG_SEVERITY)) {
                try {
                    String s = new String(ch, start, length);
                    maxSeverity = Integer.parseInt(s);
                } catch (NumberFormatException nfe) {
                    throw new SAXException(nfe);
                }
            } else if (isTypeAndTag(JBIDE_XML_TYPE, TAG_SIGNATURE_TOKEN_VALUE)) {
                softwareSignatureType = new String(ch, start, length);
            } else if (isTypeTagAndParent(JBIDE_XML_TYPE, TAG_TOKEN, TAG_HIGH_TOKENS)) {
                highSecurityTokenList.add(new String(ch, start, length));
            } else if (isTypeTagAndParent(JBIDE_XML_TYPE, TAG_TOKEN, TAG_MEDIUM_TOKENS)) {
                mediumSecurityTokenList.add(new String(ch, start, length));
            } else if (isTypeTagAndParent(JBIDE_XML_TYPE, TAG_TOKEN, TAG_LOW_TOKENS)) {
                lowSecurityTokenList.add(new String(ch, start, length));
            } else if (isTypeTagAndParent(JBIDE_XML_TYPE, TAG_REPORTID, TAG_FAILED_REPORTS)) {
                idList.add(new String(ch, start, length));
            }
        }

        @Override
        public void endDocumentSet()
                throws SAXException {
            int size = idList.size();
            reportIds = idList.toArray(new String[size]);

            size = highSecurityTokenList.size();
            highSecurityTokens = highSecurityTokenList.toArray(new String[size]);

            size = mediumSecurityTokenList.size();
            mediumSecurityTokens = mediumSecurityTokenList.toArray(new String[size]);

            size = lowSecurityTokenList.size();
            lowSecurityTokens = lowSecurityTokenList.toArray(new String[size]);
        }
    }

    private String time;
    private BigInteger nonce;
    private boolean hasTZFailure;
    private boolean hasJBIDEFailure;
    private int maxSeverity;
    private String[] reportIds;
    private String[] highSecurityTokens;
    private String[] mediumSecurityTokens;
    private String[] lowSecurityTokens;
    private String softwareSignatureType;

    /**
     * Creates a new instance based on the specified raw, un-verified data.
     *
     * @param jbideReport   the un-verified JBIDE report.
     * @param kbideReport   the un-verified KBIDE report.
     * @param tzReport      the un-verified TZ report.
     * @param tzSignature   the un-verified TZ signature.
     * @param base64Decoder a Base64 decoder to use as needed.
     */
    BidStatusReport(String jbideReport, String kbideReport,
                    String tzReport, byte[] tzSignature,
                    Base64Decoder base64Decoder) {
        super(jbideReport, kbideReport, tzReport, tzSignature, base64Decoder);
    }

    @Override
    BidXmlHandler getBideXmlHandler() {
        return new StatusXmlHandler();
    }

    /**
     * Returns the time value as extracted from the report.
     * Throws an exception if this report is currently un-verified.
     *
     * @return the time value as extracted from the report.
     * @throws java.lang.IllegalStateException if the status of this report is set to un-verified.
     */
    public final String getTime() {
        checkVerified();
        return time;
    }

    /**
     * Returns the nonce value as extracted from the report.
     * Throws an exception if this report is currently un-verified.
     *
     * @return the nonce value as extracted from the report.
     * @throws java.lang.IllegalStateException if the status of this report is set to un-verified.
     */
    public final BigInteger getNonce() {
        checkVerified();
        return nonce;
    }

    /**
     * Returns overall status as extracted from the report.
     * Throws an exception if this report is currently un-verified.
     *
     * @return overall status as extracted from the report, which is
     * <code>false</code> in the absence of failures or <code>true</code>
     * otherwise.
     * @throws java.lang.IllegalStateException if the status of this report is set to un-verified.
     */
    public final boolean hasFailure() {
        checkVerified();
        return (hasJBIDEFailure || hasTZFailure);
    }

    /**
     * Returns maximum severity as extracted from the report.
     * Throws an exception if this report is currently un-verified.
     *
     * @return maximum severity as extracted from the report, which is
     * the maximum of the integer severity values of the individual reports.
     * @throws java.lang.IllegalStateException if the status of this report is set to un-verified.
     */
    public final int getMaxSeverity() {
        checkVerified();
        return maxSeverity;
    }

    /**
     * Returns a copy of the report IDs as extracted from the report.
     * Throws an exception if this report is currently un-verified.
     *
     * @return a copy of the report IDs as extracted from the report.
     * @throws java.lang.IllegalStateException if the status of this report is set to un-verified.
     */
    public final String[] getReportIds() {
        checkVerified();
        if ((reportIds != null) && (reportIds.length > 0)) {
            return Arrays.copyOf(reportIds, reportIds.length);
        } else {
            return new String[0];
        }
    }

    /**
     * Returns a copy of the security tokens as extracted from the report.
     * Throws an exception if this report is currently un-verified.
     *
     * @return a copy of the security tokens as extracted from the report.
     * @throws java.lang.IllegalStateException if the status of this report is set to un-verified.
     */
    public final String[] getSecurityTokens(Severity severity) {
        checkVerified();
        if (severity == Severity.HIGH) {
            if ((highSecurityTokens != null) && (highSecurityTokens.length > 0)) {
                return Arrays.copyOf(highSecurityTokens, highSecurityTokens.length);
            }
        } else if ((mediumSecurityTokens != null) && (severity == Severity.MEDIUM)) {
            if (mediumSecurityTokens.length > 0) {
                return Arrays.copyOf(mediumSecurityTokens, mediumSecurityTokens.length);
            }
        } else {
            if ((lowSecurityTokens != null) && (lowSecurityTokens.length > 0)) {
                return Arrays.copyOf(lowSecurityTokens, lowSecurityTokens.length);
            }
        }
        return new String[0];
    }

    /**
     * Returns the build signature the report was created on.
     *
     * @return a string representing the type of build signature; or null if
     * it was not in the status report.
     * @throws java.lang.IllegalStateException if the status of this report is set to un-verified.
     */
    public final String getSoftwareSignatureType() {
        checkVerified();
        return softwareSignatureType;
    }
}
