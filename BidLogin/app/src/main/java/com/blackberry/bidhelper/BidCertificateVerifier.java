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

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

/**
 * Common interface of convenience methods for verifying that a certificate chains up to the
 * BlackBerry CA and that the private key associated to the certificate actually signed a
 * blob of data.
 * <p/>
 * This interface defines the minimum set of methods that a platform-specific
 * implementation must provide.
 */
public interface BidCertificateVerifier {

    public static final String BIDE_CERT_PATH[] =
            {
                    // Subject: C=CA, O=Research In Motion Limited, OU=BlackBerry, CN=RIM BlackBerry Core PKI Intermediate CA 1
                    // Issuer: C=CA, O=Research In Motion Limited, OU=BlackBerry, CN=RIM BlackBerry Core PKI Root CA 1
                    "-----BEGIN CERTIFICATE-----\n" +
                            "MIICrTCCAgygAwIBAgIFAKV3GiowDAYIKoZIzj0EAwMFADBzMQswCQYDVQQGEwJD\n" +
                            "QTEjMCEGA1UECgwaUmVzZWFyY2ggSW4gTW90aW9uIExpbWl0ZWQxEzARBgNVBAsM\n" +
                            "CkJsYWNrQmVycnkxKjAoBgNVBAMMIVJJTSBCbGFja0JlcnJ5IENvcmUgUEtJIFJv\n" +
                            "b3QgQ0EgMTAeFw0xMjA4MDExNzEwNTVaFw0zMjA4MDExNzEwNTVaMHsxCzAJBgNV\n" +
                            "BAYTAkNBMSMwIQYDVQQKDBpSZXNlYXJjaCBJbiBNb3Rpb24gTGltaXRlZDETMBEG\n" +
                            "A1UECwwKQmxhY2tCZXJyeTEyMDAGA1UEAwwpUklNIEJsYWNrQmVycnkgQ29yZSBQ\n" +
                            "S0kgSW50ZXJtZWRpYXRlIENBIDEwdjAQBgcqhkjOPQIBBgUrgQQAIgNiAAQCDrKS\n" +
                            "k9O78lvLfTt8HLANXkPfrIVYe9BlJP+E0+t1K/ioxvUhl0KpbIknB/kqucqMY/Cj\n" +
                            "JFrsyNs8EVFRmxDSAiYsK6dWOnQyU0tmJ6zb7jV6QEZB9+ws8NBmZgXoMe2jZjBk\n" +
                            "MBIGA1UdEwEB/wQIMAYBAf8CAQAwDgYDVR0PAQH/BAQDAgEGMB0GA1UdDgQWBBSS\n" +
                            "EO8HgtdWcmgBiIxaq7XQZXERLDAfBgNVHSMEGDAWgBR+9Iag4JH520lvA+lTBGft\n" +
                            "EL01VDAMBggqhkjOPQQDAwUAA4GMADCBiAJCAVqPVxtwfqXm3VtbbJSk+YJHDYrR\n" +
                            "JwCyJdGDOn9rf1nu6Yee0Uvbeqyk2RbS0MJU8YrxXpbhJ8o1qGvB1PJEjLE8AkIB\n" +
                            "aTp8CDWMuK2Ng9jFpsL+6ojXgxgbr/vRRTBdyBosHjErPeP77a2DeBnFd4dpJ6g1\n" +
                            "UebLTwfGKj5omtEQS64I4Pg=\n" +
                            "-----END CERTIFICATE-----",

                    // Subject: C=CA, O=Research In Motion Limited, OU=BlackBerry, CN=RIM BlackBerry Core PKI Root CA 1
                    // Issuer: C=CA, O=Research In Motion Limited, OU=BlackBerry, CN=RIM BlackBerry Core PKI Root CA 1
                    "-----BEGIN CERTIFICATE-----\n" +
                            "MIICyjCCAimgAwIBAgIEM4hiXTAMBggqhkjOPQQDBAUAMHMxCzAJBgNVBAYTAkNB\n" +
                            "MSMwIQYDVQQKDBpSZXNlYXJjaCBJbiBNb3Rpb24gTGltaXRlZDETMBEGA1UECwwK\n" +
                            "QmxhY2tCZXJyeTEqMCgGA1UEAwwhUklNIEJsYWNrQmVycnkgQ29yZSBQS0kgUm9v\n" +
                            "dCBDQSAxMB4XDTEyMDcyNjE3NTkzMVoXDTM3MDcyNjE3NTkzMVowczELMAkGA1UE\n" +
                            "BhMCQ0ExIzAhBgNVBAoMGlJlc2VhcmNoIEluIE1vdGlvbiBMaW1pdGVkMRMwEQYD\n" +
                            "VQQLDApCbGFja0JlcnJ5MSowKAYDVQQDDCFSSU0gQmxhY2tCZXJyeSBDb3JlIFBL\n" +
                            "SSBSb290IENBIDEwgZswEAYHKoZIzj0CAQYFK4EEACMDgYYABAEdHwaE0ppB1qIa\n" +
                            "YZeQpamxf4VkE24IECMSP+aUhHwkGwQbbbR7NcpXHgTaUtnhxYWszZaYS7JVHmvk\n" +
                            "SDe7Co8z7AHk+XlP6zV/VVZtskcWbT3olfTVzFpb9ZSCKwDdohiOv8rsFmN6iy17\n" +
                            "9EafjqC2HwkDQh3dTZS5/O3q2oGfNkRe1aNmMGQwEgYDVR0TAQH/BAgwBgEB/wIB\n" +
                            "/zAOBgNVHQ8BAf8EBAMCAQYwHQYDVR0OBBYEFH70hqDgkfnbSW8D6VMEZ+0QvTVU\n" +
                            "MB8GA1UdIwQYMBaAFH70hqDgkfnbSW8D6VMEZ+0QvTVUMAwGCCqGSM49BAMEBQAD\n" +
                            "gYwAMIGIAkIBHOuRd/TTuk7XZQC2nrdVJ2fE7WuU1DZFMbZK/3rJ9JyhNXckTs/p\n" +
                            "MnVUn8yRUOoEIhwT/tOnoAXPwMZUdomz2wUCQgCwwUVphFXj9oMjelrMUlhVu3sa\n" +
                            "p5T/oIKXJt8dk7asItNRuDw1WbkTCh2xf0CW4oSbm7fw7FmfghJOqaJRKP2lAg==\n" +
                            "-----END CERTIFICATE-----"
            };

    /**
     * Set a certificate to be used in the verification.
     *
     * @param certificate certificate to be used by the verifier
     */
    public void setCertificate(X509Certificate certificate);

    /**
     * Verify that the certificate chains up to the CA root certificate.
     *
     * @throws java.lang.IllegalStateException         if the certificate was not
     *                                                 yet set on the verifier
     * @throws java.security.cert.CertificateException if the certificate validation
     *                                                 failed
     */
    public void verifyCertificateChain()
            throws IllegalStateException,
            CertificateException;

    /**
     * Verify that the report was signed by the given certificate.
     *
     * @param tzReport  the report for which we are checking the signature.
     * @param signature the singature to verify.
     * @return <code>true</code> if the report signatures  <code>false</code> otherwise.
     */
    public boolean verifyReport(byte[] tzReport, byte[] signature)
            throws CertificateException;

}
