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
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import java.io.IOException;
import java.io.StringReader;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Arrays;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

import static com.blackberry.bidhelper.BidConstants.TAG_DEVICEMODEL;
import static com.blackberry.bidhelper.BidConstants.TAG_JBIDEHASH;
import static com.blackberry.bidhelper.BidConstants.TAG_KBIDEHASH;
import static com.blackberry.bidhelper.BidConstants.TAG_OSVERSION;

/**
 * Base class for BID reports that need to be verified prior to use.
 * <p/>
 * Initially the report is un-verified. To verify a report, call the
 * appropriate method in the <code>BidHelper</code> class for your
 * platform.
 * <p/>
 * An attempt to access the contents of an un-verified report will cause
 * an exception to be thrown.
 */
public abstract class BidVerifiable {

    private final String jbideReport;
    private final byte[] jbideReportHash;
    private final String kbideReport;
    private final byte[] kbideReportHash;
    private final String tzReport;
    private final byte[] tzReportHash;
    private final byte[] tzSignature;
    private final Base64Decoder base64Decoder;
    // for safety do NOT expose a method that blindly sets isVerified to true
    private boolean isVerified;
    private boolean allowReadUnverified;
    private String devicemodel;
    private String osversion;
    /**
     * Creates a new instance based on the specified raw, un-verified data.
     *
     * @param jbideReport   the un-verified JBIDE report.
     * @param kbideReport   the un-verified KBIDE report.
     * @param tzReport      the un-verified TZ report.
     * @param tzSignature   the un-verified TZ signature.
     * @param base64Decoder a Base64 decoder to use as needed.
     */
    BidVerifiable(String jbideReport, String kbideReport,
                  String tzReport, byte[] tzSignature,
                  Base64Decoder base64Decoder) {
        this.jbideReport = jbideReport;
        this.jbideReportHash = (jbideReport == null) ? null : getSha256(jbideReport);
        this.kbideReport = kbideReport;
        this.kbideReportHash = getSha256(kbideReport);
        this.tzReport = tzReport;
        this.tzReportHash = getSha256(tzReport);
        this.tzSignature = Arrays.copyOf(tzSignature, tzSignature.length);
        this.base64Decoder = base64Decoder;
        this.allowReadUnverified = false;
    }

    /**
     * Returns the SHA-256 hash of the specified string.
     *
     * @param s the string whose hash value is desired.
     * @return the SHA-256 hash of the specified string.
     */
    private static byte[] getSha256(String s) {
        byte[] result;
        try {
            MessageDigest digester = MessageDigest.getInstance("SHA-256");
            result = digester.digest(s.getBytes());
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is ubiquitous and required in all Java implementations.
            // Nonetheless, force a mismatch.
            result = new byte[0];
        }
        return result;
    }

    /**
     * Returns the device model information or null if none.
     * Throws an exception if this report is currently un-verified.
     *
     * @return the device model information or null if none.
     * @throws java.lang.IllegalStateException if the status of this report is set to un-verified.
     */
    public final String getDeviceModel() {
        checkVerified();
        return devicemodel;
    }

    /**
     * Returns the OS version information or null if none.
     * Throws an exception if this report is currently un-verified.
     *
     * @return the OS version information or null if none.
     * @throws java.lang.IllegalStateException if the status of this report is set to un-verified.
     */
    public final String getOsVersion() {
        checkVerified();
        return osversion;
    }

    /**
     * Returns the raw JBIDE report or null if none.
     * Throws an exception if this report is currently un-verified.
     *
     * @return the raw JBIDE report or null if none.
     * @throws java.lang.IllegalStateException if the status of this report is set to un-verified.
     */
    public final String getJbideReport() {
        checkVerified();
        return jbideReport;
    }

    /**
     * Returns the raw KBIDE report.
     * Throws an exception if this report is currently un-verified.
     *
     * @return the raw KBIDE report.
     * @throws java.lang.IllegalStateException if the status of this report is set to un-verified.
     */
    public final String getKbideReport() {
        checkVerified();
        return kbideReport;
    }

    /**
     * Returns the raw TZ report.
     * Throws an exception if this report is currently un-verified.
     *
     * @return the raw TZ report.
     * @throws java.lang.IllegalStateException if the status of this report is set to un-verified.
     */
    public final String getTzReport() {
        checkVerified();
        return tzReport;
    }

    /**
     * Creates and configures a parser, begins the BIDE document set,
     * and parses the JBIDE, KBIDE, and TZ XML firing SAX events to
     * the handler provided by <code>getBideXmlHandler()</code>.
     * The <code>isVerified</code> flag is set true only on success.
     *
     * @throws java.io.IOException                     if an i/o error occurs.
     * @throws BidHashMismatchException                if the hash values do not match.
     * @throws BidSignatureVerificationException       if the signature verification fails.
     * @throws java.security.cert.CertificateException if the certificate chain part of the report is invalid
     */
    final void verify(BidCertificateVerifier certVerifier)
            throws IOException,
            BidHashMismatchException,
            BidSignatureVerificationException,
            CertificateException {
        isVerified = false;
        BidXmlHandler handler = getBideXmlHandler();
        Verifier verifier = new Verifier(handler);

        try {
            // Create and configure the parser
            SAXParserFactory factory = SAXParserFactory.newInstance();
            XMLReader parser = factory.newSAXParser().getXMLReader();
            parser.setEntityResolver(verifier);
            parser.setDTDHandler(verifier);
            parser.setContentHandler(verifier);
            parser.setErrorHandler(verifier);
            parser.setFeature("http://xml.org/sax/features/namespaces", true);
            parser.setFeature("http://xml.org/sax/features/namespace-prefixes", false);

            handler.startDocumentSet();

            if (jbideReport != null) {
                // Parse JBIDE report
                parse(jbideReport, BidXmlHandler.JBIDE_XML_TYPE, handler, parser);
            }

            // Parse KBIDE report
            if (kbideReport.length() > 0) {
                parse(kbideReport, BidXmlHandler.KBIDE_XML_TYPE, handler, parser);
            }

            // Parse TZ report
            if (tzReport.length() > 0) {
                parse(tzReport, BidXmlHandler.TZ_XML_TYPE, handler, parser);
            }

            handler.endDocumentSet();

            // Verify signature
            certVerifier.verifyCertificateChain();

            // Verify report
            if (certVerifier.verifyReport(tzReport.getBytes(), tzSignature)) {
                isVerified = true;
            } else {
                throw new BidSignatureVerificationException("Report Verification Failed.");
            }


        } catch (SAXException se) {

            // Un-box and throw any nested BidHashMismatchException
            // otherwise wrap in an IOException.
            Throwable t = se.getCause();
            if (t instanceof BidHashMismatchException) {
                throw (BidHashMismatchException) t;
            } else {
                throw new IOException(se);
            }
        } catch (ParserConfigurationException pce) {
            throw new IOException(pce);
        }
    }

    /**
     * Sets the input source of the given parser based on the specified XML report,
     * sets the XML type of the given handler based on the specified integer value,
     * and runs the SAX parser. The parser is assumed to already have had the
     * various SAX event handlers configured (see <code>Verifier</code>).
     *
     * @param xmlReport the XML report to be parsed.
     * @param xmlType   the integer value representing the type of the XML report.
     * @param handler   the handler to set the XML type for.
     * @param parser    the pre-configured parser to be run on the XML input.
     * @throws java.io.IOException      if an i/o error occurs.
     * @throws BidHashMismatchException if the hash values do not match.
     * @throws org.xml.sax.SAXException if a parsing error occurs.
     */
    private void parse(String xmlReport, int xmlType, BidXmlHandler handler, XMLReader parser)
            throws IOException,
            BidHashMismatchException,
            SAXException {
        InputSource in = new InputSource(new StringReader(xmlReport));
        handler.setXmlType(xmlType);
        parser.parse(in);
    }

    /**
     * Revokes (clears) the verified status of this report.
     */
    final void revokeVerification() {
        isVerified = false;
    }

    /**
     * Returns <code>true</code> if this report has been verified,
     * <code>false</code> otherwise.
     *
     * @return <code>true</code> if this report has been verified,
     * <code>false</code> otherwise.
     */
    public final boolean isVerified() {
        return isVerified;
    }

    /**
     * Checks the verified status of this report and throws an exception if
     * un-verified, unless it is specifically bypassed.
     *
     * @throws java.lang.IllegalStateException if the status of this report is set to un-verified.
     */
    final void checkVerified() {
        if (!isVerified && !allowReadUnverified) {
            throw new IllegalStateException("attempt to access an unverified report");
        }
    }

    /**
     * Allow reading of report without verification.
     * <p/>
     * Call this function if you want to read this particular report
     * without verification.
     */
    public final void bypassVerification() {
        allowReadUnverified = true;
    }

    /**
     * Returns a handler for this type of report. The handler uses parser events
     * to verify hash values and to populate the report object.
     *
     * @return a handler for this type of report.
     */
    abstract BidXmlHandler getBideXmlHandler();

    /**
     * Instances of this class handle SAX parsing events for the purpose of
     * validating and extracting the contents of the various BIDE XML reports.
     * The <code>BidXmlHandler</code> specified in the constructor is used
     * to determine the type of XML being parsed (JBIDE, KBIDE, TZ). Also, the
     * implementations of the various <code>DefaultHandler</code> methods call
     * the corresponding method in <code>BidXmlHandler</code> after performing
     * any processing of their own. Any BIDE-specific exceptions that occur are
     * wrapped in a <code>SAXException</code> and then un-boxed below in the
     * <code>verify()</code> method of the outer class.
     */
    private final class Verifier extends DefaultHandler {
        private final BidXmlHandler handler;

        /**
         * Creates a new Verifier with the specified handler.
         *
         * @param handler the <code>BidXmlHandler</code> used to determine
         *                the type of XML being parsed (JBIDE, KBIDE, TZ). Also the
         *                target of all calls to <code>DefaultHandler</code> methods.
         */
        public Verifier(BidXmlHandler handler) {
            this.handler = handler;
        }

        @Override
        public InputSource resolveEntity(String publicId, String systemId)
                throws IOException, SAXException {
            handler.resolveEntity(publicId, systemId);
            return null;
        }

        @Override
        public void notationDecl(String name, String publicId, String systemId)
                throws SAXException {
            handler.notationDecl(name, publicId, systemId);
        }

        @Override
        public void unparsedEntityDecl(String name, String publicId, String systemId, String notationName)
                throws SAXException {
            handler.unparsedEntityDecl(name, publicId, systemId, notationName);
        }

        @Override
        public void setDocumentLocator(Locator locator) {
            handler.setDocumentLocator(locator);
        }

        @Override
        public void startDocument()
                throws SAXException {
            handler.startDocument();
        }

        @Override
        public void endDocument()
                throws SAXException {
            handler.endDocument();
        }

        @Override
        public void startPrefixMapping(String prefix, String uri)
                throws SAXException {
            handler.startPrefixMapping(prefix, uri);
        }

        @Override
        public void endPrefixMapping(String prefix)
                throws SAXException {
            handler.endPrefixMapping(prefix);
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes atts)
                throws SAXException {
            handler.push(localName, atts);
            handler.startElement(uri, localName, qName, atts);
        }

        @Override
        public void endElement(String uri, String localName, String qName)
                throws SAXException {
            handler.endElement(uri, localName, qName);
            handler.pop();
        }

        @Override
        public void characters(char[] ch, int start, int length)
                throws SAXException {
            if (handler.isTypeAndTag(BidXmlHandler.KBIDE_XML_TYPE, TAG_JBIDEHASH)) {
                String s = new String(ch, start, length);
                byte[] candidate = base64Decoder.decode(s);
                if (candidate == null || !Arrays.equals(jbideReportHash, candidate)) {
                    Exception e = new BidHashMismatchException("JBIDE report hash mismatch");
                    throw new SAXException(e);
                }
            } else if (handler.isTypeAndTag(BidXmlHandler.TZ_XML_TYPE, TAG_KBIDEHASH)) {
                String s = new String(ch, start, length);
                byte[] candidate = base64Decoder.decode(s);
                if (candidate == null || !Arrays.equals(kbideReportHash, candidate)) {
                    Exception e = new BidHashMismatchException("KBIDE report hash mismatch");
                    throw new SAXException(e);
                }
            } else if (handler.isTypeAndTag(BidXmlHandler.JBIDE_XML_TYPE, TAG_DEVICEMODEL)) {
                devicemodel = new String(ch, start, length);
            } else if (handler.isTypeAndTag(BidXmlHandler.JBIDE_XML_TYPE, TAG_OSVERSION)) {
                osversion = new String(ch, start, length);
            }

            handler.characters(ch, start, length);
        }

        @Override
        public void ignorableWhitespace(char[] ch, int start, int length)
                throws SAXException {
            handler.ignorableWhitespace(ch, start, length);
        }

        @Override
        public void processingInstruction(String target, String data)
                throws SAXException {
            handler.processingInstruction(target, data);
        }

        @Override
        public void skippedEntity(String name)
                throws SAXException {
            handler.skippedEntity(name);
        }

        @Override
        public void warning(SAXParseException e)
                throws SAXException {
            handler.warning(e);
        }

        @Override
        public void error(SAXParseException e)
                throws SAXException {
            handler.error(e);
        }

        @Override
        public void fatalError(SAXParseException e)
                throws SAXException {
            handler.fatalError(e);
            throw e;
        }
    }
}
