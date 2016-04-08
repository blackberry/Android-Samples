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

import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cms.CMSException;
import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.SignerInformation;
import org.bouncycastle.cms.SignerInformationStore;
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.util.Store;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertPath;
import java.security.cert.CertPathValidator;
import java.security.cert.CertPathValidatorException;
import java.security.cert.CertPathValidatorResult;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.PKIXParameters;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public final class BidCertificateVerifierAndroid implements BidCertificateVerifier {
    /**
     * Bid Certificate
     */
    private X509Certificate bidCert = null;

    /**
     * State of the verifier
     */
    private boolean certificateVerified = false;

    /**
     * Create a new BID Certificate Cerifier
     */
    public BidCertificateVerifierAndroid() {
    }

    ;

    private static final String LOG_TAG = BidCertificateVerifierAndroid.class.getSimpleName();

    @Override
    public void setCertificate(X509Certificate certificate) {
        this.bidCert = certificate;
    }

    public static final List<Certificate> loadCertificates(String[] pemEncodedCerts) throws CertificateException {

        List<Certificate> certList = new ArrayList<>();
        CertificateFactory certFactory = CertificateFactory.getInstance("X.509");

        for (String certPem : pemEncodedCerts) {
            InputStream certIn = new ByteArrayInputStream(certPem.getBytes());
            Certificate cert = certFactory.generateCertificate(certIn);
            certList.add(cert);
        }

        return certList;
    }

    @Override
    public void verifyCertificateChain() throws IllegalStateException, CertificateException {

        if (this.bidCert == null) {
            throw new IllegalStateException("Certificate not yet set");
        }

        List<Certificate> bideCertList = loadCertificates(BidCertificateVerifier.BIDE_CERT_PATH);
        Certificate entityCert = this.bidCert;
        Certificate rootCert = bideCertList.get(1);

        Set<TrustAnchor> trustAnchorSet = new HashSet<>();
        trustAnchorSet.add(new TrustAnchor((X509Certificate) rootCert, null));
        PKIXParameters params;
        try {
            params = new PKIXParameters(trustAnchorSet);
            params.setRevocationEnabled(false);
        } catch (InvalidAlgorithmParameterException e) {
            throw new CertificateException("Failed to initialize PKIXParameters", e);
        }

        // Build the certificate chain
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        CertPath certPath;
        try {
            List<Certificate> certChain = new ArrayList<>();

            // Add the bide certifcate
            certChain.add((X509Certificate) entityCert);

            // Add the intermediate certificate
            certChain.add((X509Certificate) bideCertList.get(0));

            certPath = cf.generateCertPath(certChain);
        } catch (CertificateException ce) {
            //android.util.Log.d(LOG_TAG, "Failed to use Build cert chain: " + ce.toString());
            throw ce;
        }

        CertPathValidator validator;
        try {
            validator = CertPathValidator.getInstance("PKIX");
        } catch (NoSuchAlgorithmException e) {
            //android.util.Log.d(LOG_TAG, "NoSuchAlgorithmException thrown: " + e.toString());
            throw new CertificateException("Failed to initialize CertPathValidator", e);
        }

        CertPathValidatorResult result;
        try {
            result = validator.validate(certPath, params);
        } catch (InvalidAlgorithmParameterException e) {
            throw new CertificateException("Validation failed", e);
        } catch (CertPathValidatorException e) {
            throw new CertificateException("Validation failed", e);
        }
    }

    @Override
    public boolean verifyReport(byte[] tzReport, byte[] signature)
            throws CertificateException {
        if (this.bidCert == null) {
            throw new IllegalStateException("Certificate not yet set");
        }

        try {
            CMSSignedData cms = new CMSSignedData(new CMSProcessableByteArray(tzReport), signature);

            Store certStore = cms.getCertificates();
            SignerInformationStore signers = cms.getSignerInfos();
            Collection c = signers.getSigners();
            Iterator it = c.iterator();

            if (c.size() != 1) {
                return false;
            }

            while (it.hasNext()) {
                SignerInformation signer = (SignerInformation) it.next();
                Collection certCollection = certStore.getMatches(signer.getSID());
                Iterator certIt = certCollection.iterator();

                // If there is no certificate part of the signature then the report may have been created before 
                // the certificate was cut.
                if (certCollection.size() == 0) {
                    return signer.verify(new JcaSimpleSignerInfoVerifierBuilder().setProvider("BC").build(this.bidCert));
                }

                X509CertificateHolder certHolder = (X509CertificateHolder) certIt.next();
                X509Certificate cert = new JcaX509CertificateConverter().setProvider("BC").getCertificate(certHolder);
                return signer.verify(new JcaSimpleSignerInfoVerifierBuilder().setProvider("BC").build(cert));
            }
        } catch (CMSException e) {
            throw new CertificateException(e.toString());
        } catch (OperatorCreationException oce) {
            throw new CertificateException(oce.toString());
        } catch (Exception ex) {
            throw ex;
        }

        return false;
    }
}
