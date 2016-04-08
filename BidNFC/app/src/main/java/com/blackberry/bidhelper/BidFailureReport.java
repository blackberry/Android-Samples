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

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.blackberry.bidhelper.BidConstants.ATT_SENSOR;
import static com.blackberry.bidhelper.BidConstants.ATT_SEVERITY;
import static com.blackberry.bidhelper.BidConstants.TAG_FAILURE;
import static com.blackberry.bidhelper.BidConstants.TAG_REPORTID;
import static com.blackberry.bidhelper.BidConstants.TAG_TIME;

/**
 * This class contains methods for accessing the contents of a BID
 * failure report.
 * <p/>
 * Initially the report is un-verified. To verify a report, call the
 * appropriate method in the <code>BidHelper</code> class for your
 * platform.
 * <p/>
 * An attempt to access the contents of an un-verified report will cause
 * an exception to be thrown.
 */
public final class BidFailureReport extends BidVerifiable {
    /**
     * A handler specific to BID failure reports. The handler uses parser events
     * to verify hash values and to populate the report object.
     */
    private class FailureXmlHandler extends BidXmlHandler {
        private List<BidFailure> failureList;
        private Attributes failureAtts;

        @Override
        public void startDocumentSet()
                throws SAXException {
            failureList = new ArrayList<BidFailure>();
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes atts)
                throws SAXException {
            if (isFailureTag()) {
                failureAtts = getAttributes();
            }
        }

        @Override
        public void endElement(String uri, String localName, String qName)
                throws SAXException {
            if (isFailureTag() && failureAtts != null) {
                // tag was empty
                addFailure("(none)");
            }
        }

        @Override
        public void characters(char[] ch, int start, int length)
                throws SAXException {
            if (isTypeAndTag(KBIDE_XML_TYPE, TAG_TIME)) {
                time = new String(ch, start, length);
            } else if (isTypeAndTag(TZ_XML_TYPE, TAG_REPORTID)) {
                id = new String(ch, start, length);
            } else if (failureAtts != null) {
                addFailure(new String(ch, start, length));
            }
        }

        /**
         * Returns true if this is a failure tag inside a JBIDE or KBIDE report,
         * false otherwise.
         *
         * @return true if this is a failure tag inside a JBIDE or KBIDE report,
         * false otherwise.
         */
        private boolean isFailureTag() {
            return isTypeAndTag(JBIDE_XML_TYPE | KBIDE_XML_TYPE, TAG_FAILURE);
        }

        /**
         * Adds a BID failure to this handler's list of failures and
         * then clear the failure attributes marker.
         *
         * @param details failure details.
         * @throws org.xml.sax.SAXException if a parse error occurs.
         */
        private void addFailure(String details)
                throws SAXException {
            String sensorName = failureAtts.getValue("", ATT_SENSOR);
            int severity;
            try {
                String s = failureAtts.getValue("", ATT_SEVERITY);
                severity = Integer.parseInt(s);
            } catch (NumberFormatException nfe) {
                throw new SAXException(nfe);
            }
            failureList.add(new BidFailure(sensorName, severity, details));
            failureAtts = null;
        }

        @Override
        public void endDocumentSet()
                throws SAXException {
            int size = failureList.size();
            failures = failureList.toArray(new BidFailure[size]);
        }
    }

    private String time;
    private String id;
    private BidFailure[] failures;

    /**
     * Creates a new instance based on the specified raw, un-verified data.
     *
     * @param jbideReport   the un-verified JBIDE report.
     * @param kbideReport   the un-verified KBIDE report.
     * @param tzReport      the un-verified TZ report.
     * @param tzSignature   the un-verified TZ signature.
     * @param base64Decoder a Base64 decoder to use as needed.
     */
    BidFailureReport(String jbideReport, String kbideReport,
                     String tzReport, byte[] tzSignature,
                     Base64Decoder base64Decoder) {
        super(jbideReport, kbideReport, tzReport, tzSignature, base64Decoder);
    }

    @Override
    BidXmlHandler getBideXmlHandler() {
        return new FailureXmlHandler();
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
     * Returns the ID value as extracted from the report.
     * Throws an exception if this report is currently un-verified.
     *
     * @return the ID value as extracted from the report.
     * @throws java.lang.IllegalStateException if the status of this report is set to un-verified.
     */
    public final String getId() {
        checkVerified();
        return id;
    }

    /**
     * Returns a copy of the failures as extracted from the report.
     * Throws an exception if this report is currently un-verified.
     *
     * @return a copy of the failures as extracted from the report.
     * @throws java.lang.IllegalStateException if the status of this report is set to un-verified.
     */
    public final BidFailure[] getFailures() {
        checkVerified();
        return Arrays.copyOf(failures, failures.length);
    }
}