package br.com.assinador.certificate;

import br.com.assinador.config.Pkcs11Config;

import java.security.Key;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class Pkcs11CertificateProvider {

    private Provider provider;
    private KeyStore keyStore;

    public List<CertificateInfo> loadCertificates(Pkcs11Config config) throws Exception {
        unloadProvider();
        Provider baseProvider = Security.getProvider("SunPKCS11");
        if (baseProvider == null) {
            throw new IllegalStateException("Provider SunPKCS11 não encontrado na JVM.");
        }

        String pkcs11Config = "name=A3Token\n" +
                "library=" + config.getLibraryPath() + "\n";

        provider = baseProvider.configure(pkcs11Config);
        Security.addProvider(provider);

        keyStore = KeyStore.getInstance("PKCS11", provider);
        keyStore.load(null, config.getPin());

        List<CertificateInfo> result = new ArrayList<>();
        Enumeration<String> aliases = keyStore.aliases();
        while (aliases.hasMoreElements()) {
            String alias = aliases.nextElement();
            if (!keyStore.isKeyEntry(alias)) {
                continue;
            }
            Key key = keyStore.getKey(alias, null);
            if (!(key instanceof PrivateKey privateKey)) {
                continue;
            }
            Certificate cert = keyStore.getCertificate(alias);
            if (cert instanceof X509Certificate x509Certificate) {
                Certificate[] chain = keyStore.getCertificateChain(alias);
                List<X509Certificate> x509Chain = chain == null ? List.of() : Arrays.stream(chain)
                        .filter(X509Certificate.class::isInstance)
                        .map(X509Certificate.class::cast)
                        .collect(Collectors.toList());
                result.add(new CertificateInfo(alias, x509Certificate, privateKey, x509Chain, provider));
            }
        }

        return result;
    }

    public void unloadProvider() {
        if (provider != null) {
            Security.removeProvider(provider.getName());
            provider = null;
            keyStore = null;
        }
    }
}
