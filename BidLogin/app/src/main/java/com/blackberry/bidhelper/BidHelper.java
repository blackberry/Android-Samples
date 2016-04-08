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

import java.io.IOException;
import java.math.BigInteger;
import java.security.cert.CertificateException;

/**
 * Common interface of convenience methods for obtaining status reports
 * and failure reports from the BID content provider. After a request
 * completes successfully, the caller is responsible for verifying the
 * report object before attempting to access the contents of the report.
 * An attempt to access the contents of an un-verified report will cause
 * an exception to be thrown.
 * <p/>
 * This class can also register listeners for BID-related events such
 * as the insertion of a report and the availability of a certificate.
 * <p/>
 * This interface defines the minimum set of methods that a platform-specific
 * implementation must provide.
 */
public interface BidHelper {

    /**
     * Call this method when you are finished with the helper and no longer
     * require its services. Failing to do so could lead to memory leaks.
     */
    public void destroy();

    /**
     * Adds a listener for events such as the insertion of a report
     * and the availability of a certificate.
     *
     * @param listener a listener for BID-related events.
     */
    public void addBidListener(BidListener listener);

    /**
     * Removes a listener that was previously added.
     *
     * @param listener a listener for BID-related events.
     */
    public void removeBidListener(BidListener listener);

    /**
     * Checks whether a non-expired certificate is available, fetches it and returns
     * <code>true</code> if it is, otherwise returns <code>false</code>.
     *
     * @return <code>true</code> if a non-expired certificate is available,
     * <code>false</code> otherwise.
     */
    public boolean isCertificateAvailable();

    /**
     * Requests a BID status report based on the specified nonce.
     *
     * @param nonce a nonce value to be included in the report for
     *              security purposes.
     * @return the BID status report in an un-verified state.
     * The caller is responsible for verifying the report using the methods
     * provided before attempting to access the contents of the report.
     * An attempt to access the contents of an un-verified report will cause
     * an exception to be thrown.
     * @throws BidRequestException if an error occurred while attempting to fetch the report.
     */
    public BidStatusReport requestStatusReport(BigInteger nonce)
            throws BidRequestException;

    /**
     * Verifies the specified BID status report. Any failure to verify will
     * result in an exception.
     *
     * @param report   the report to be verified.
     * @param nonce    the same nonce value used in the request.
     * @param certReqd indicates whether a certificate is required for verification.
     *                 Use a value of <code>true</code> to require that the certificate be
     *                 available; this should be the norm especially when the caller is remote.
     *                 Specifying a value of <code>false</code> will cause the certificate check
     *                 to be bypassed which is only appropriate when the caller resides on
     *                 the same device as BID itself.
     * @throws BidCertificateUnavailableException      if the certificate is not available and <code>certReqd</code>
     *                                                 is <code>true</code>.
     * @throws java.io.IOException                     if an i/o error occurs.
     * @throws BidHashMismatchException                if the hash values do not match.
     * @throws BidSignatureVerificationException       if the signature verification fails.
     * @throws BidNonceMismatchException               if the nonce values do not match.
     * @throws java.security.cert.CertificateException if the certificate chain part of the report is invalid
     */
    public void verifyStatusReport(BidStatusReport report, BigInteger nonce, boolean certReqd)
            throws BidCertificateUnavailableException,
            IOException,
            BidHashMismatchException,
            BidSignatureVerificationException,
            BidNonceMismatchException,
            CertificateException;

    /**
     * Requests a specific BID failure report.
     *
     * @param reportId the report ID as given in the status report.
     * @return the BID failure report in an un-verified state.
     * The caller is responsible for verifying the report using the methods
     * provided before attempting to access the contents of the report.
     * An attempt to access the contents of an un-verified report will cause
     * an exception to be thrown.
     * @throws BidRequestException if an error occurred while attempting to fetch the report.
     */
    public BidFailureReport requestFailureReport(String reportId)
            throws BidRequestException;

    /**
     * Requests all BID failure reports.
     *
     * @return an array of the BID failure reports in an un-verified state.
     * The caller is responsible for verifying the report using the methods
     * provided before attempting to access the contents of the report.
     * An attempt to access the contents of an un-verified report will cause
     * an exception to be thrown.
     * @throws BidRequestException if an error occurred while attempting to fetch the reports.
     */
    public BidFailureReport[] requestAllFailureReports()
            throws BidRequestException;

    /**
     * Verifies the specified BID failure report. Any failure to verify will
     * result in an exception.
     *
     * @param report   the report to be verified.
     * @param certReqd indicates whether a certificate is required for verification.
     *                 Use a value of <code>true</code> to require that the certificate be
     *                 available; this should be the norm especially when the caller is remote.
     *                 Specifying a value of <code>false</code> will cause the certificate check
     *                 to be bypassed which is only appropriate when the caller resides on
     *                 the same device as BID itself.
     * @throws BidCertificateUnavailableException      if the certificate is not available and <code>certReqd</code>
     *                                                 is <code>true</code>.
     * @throws java.io.IOException                     if an i/o error occurs.
     * @throws BidHashMismatchException                if the hash values do not match.
     * @throws BidSignatureVerificationException       if the signature verification fails.
     * @throws java.security.cert.CertificateException if the certificate chain part of the report is invalid
     */
    public void verifyFailureReport(BidFailureReport report, boolean certReqd)
            throws BidCertificateUnavailableException,
            IOException,
            BidHashMismatchException,
            BidSignatureVerificationException,
            CertificateException;

    /**
     * Request the BID certificate.
     *
     * @return the BID certificate which may be used to verify the failure reports.
     * @throws BidCertificateUnavailableException if the BID
     *                                            Certificate is not available.
     * @throws BidRequestException                if there was an error encountered
     *                                            in the query for the BID Certificate.
     */

    public byte[] requestBidCertificate()
            throws BidCertificateUnavailableException,
            BidRequestException;
}
