package br.com.assinador.certificate;

import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.List;

public class CertificateInfo {
    private final String alias;
    private final X509Certificate certificate;
    private final PrivateKey privateKey;
    private final List<X509Certificate> certificateChain;

    public CertificateInfo(String alias, X509Certificate certificate, PrivateKey privateKey, List<X509Certificate> certificateChain) {
        this.alias = alias;
        this.certificate = certificate;
        this.privateKey = privateKey;
        this.certificateChain = certificateChain;
    }

    public String getAlias() {
        return alias;
    }

    public X509Certificate getCertificate() {
        return certificate;
    }

    public PrivateKey getPrivateKey() {
        return privateKey;
    }

    public List<X509Certificate> getCertificateChain() {
        return certificateChain;
    }

    public String getDisplayName() {
        String subject = certificate.getSubjectX500Principal().getName();
        String issuer = certificate.getIssuerX500Principal().getName();
        return subject + " | Emissor: " + issuer;
    }

    @Override
    public String toString() {
        return getDisplayName();
    }
}
