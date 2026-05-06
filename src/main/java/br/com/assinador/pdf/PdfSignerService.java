package br.com.assinador.pdf;

import br.com.assinador.certificate.CertificateInfo;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.ExternalSigningSupport;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.SignatureOptions;
import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.CMSSignedDataGenerator;
import org.bouncycastle.cms.jcajce.JcaSignerInfoGeneratorBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.security.Security;
import java.util.Calendar;
import java.util.List;

public class PdfSignerService {

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    public void signPdf(File inputFile, File outputFile, CertificateInfo certInfo, SignaturePosition pos, boolean lockAfterSigning) throws Exception {
        if (inputFile.equals(outputFile)) {
            throw new IllegalArgumentException("O arquivo de saída deve ser diferente do arquivo original.");
        }

        try (PDDocument document = Loader.loadPDF(inputFile)) {
            PDSignature signature = new PDSignature();
            signature.setFilter(PDSignature.FILTER_ADOBE_PPKLITE);
            signature.setSubFilter(PDSignature.SUBFILTER_ADBE_PKCS7_DETACHED);
            signature.setName(certInfo.getCertificate().getSubjectX500Principal().getName());
            signature.setReason("Assinatura digital ICP-Brasil");
            signature.setSignDate(Calendar.getInstance());

            if (lockAfterSigning) {
                applyCertificationLock(document, signature);
            }

            try (SignatureOptions options = new SignatureOptions()) {
                options.setPage(pos.getPageIndex());
                options.setVisualSignature(createVisualSignatureTemplate(document, certInfo, pos));
                document.addSignature(signature, options);

                try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                    ExternalSigningSupport externalSigning = document.saveIncrementalForExternalSigning(fos);
                    byte[] content = externalSigning.getContent().readAllBytes();
                    byte[] cms = sign(content, certInfo);
                    externalSigning.setSignature(cms);
                }
            }
        }
    }

    private void applyCertificationLock(PDDocument document, PDSignature signature) {
        COSDictionary sigDict = signature.getCOSObject();

        COSDictionary transformParams = new COSDictionary();
        transformParams.setItem(COSName.TYPE, COSName.TRANSFORM_PARAMS);
        transformParams.setName(COSName.V, "1.2");
        transformParams.setInt(COSName.P, 1);

        COSDictionary sigRef = new COSDictionary();
        sigRef.setItem(COSName.TYPE, COSName.SIG_REF);
        sigRef.setItem(COSName.TRANSFORM_METHOD, COSName.DOCMDP);
        sigRef.setItem(COSName.DIGEST_METHOD, COSName.getPDFName("SHA256"));
        sigRef.setItem(COSName.TRANSFORM_PARAMS, transformParams);

        COSArray referenceArray = new COSArray();
        referenceArray.add(sigRef);
        sigDict.setItem(COSName.REFERENCE, referenceArray);

        COSDictionary permsDict = new COSDictionary();
        permsDict.setItem(COSName.DOCMDP, signature);
        PDDocumentCatalog catalog = document.getDocumentCatalog();
        catalog.getCOSObject().setItem(COSName.PERMS, permsDict);
    }

    private byte[] sign(byte[] content, CertificateInfo certInfo) throws Exception {
        CMSSignedDataGenerator gen = new CMSSignedDataGenerator();
        ContentSigner sha256Signer = new JcaContentSignerBuilder("SHA256withRSA")
                .setProvider(certInfo.getPrivateKey().getProvider())
                .build(certInfo.getPrivateKey());
        gen.addSignerInfoGenerator(new JcaSignerInfoGeneratorBuilder(
                new JcaDigestCalculatorProviderBuilder().setProvider("BC").build())
                .build(sha256Signer, certInfo.getCertificate()));
        List<java.security.cert.X509Certificate> chain = certInfo.getCertificateChain();
        List<java.security.cert.X509Certificate> toEmbed = (chain == null || chain.isEmpty())
                ? List.of(certInfo.getCertificate())
                : chain;
        gen.addCertificates(new JcaCertStore(toEmbed));
        CMSSignedData signedData = gen.generate(new CMSProcessableByteArray(content), false);
        return signedData.getEncoded();
    }

    private InputStream createVisualSignatureTemplate(PDDocument src, CertificateInfo certInfo, SignaturePosition pos) throws Exception {
        try (PDDocument visualDoc = new PDDocument()) {
            PDPage page = new PDPage(src.getPage(pos.getPageIndex()).getMediaBox());
            visualDoc.addPage(page);

            BufferedImage image = new BufferedImage((int) Math.max(200, pos.getWidth()), (int) Math.max(80, pos.getHeight()), BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = image.createGraphics();
            g.setColor(new Color(255, 255, 255, 220));
            g.fillRect(0, 0, image.getWidth(), image.getHeight());
            g.setColor(Color.BLACK);
            g.drawRect(0, 0, image.getWidth() - 1, image.getHeight() - 1);
            g.drawString("Assinado digitalmente", 10, 20);
            g.drawString(certInfo.getCertificate().getSubjectX500Principal().getName(), 10, 40);
            g.drawString(Calendar.getInstance().getTime().toString(), 10, 60);
            g.dispose();

            var pdImage = LosslessFactory.createFromImage(visualDoc, image);
            try (PDPageContentStream cs = new PDPageContentStream(visualDoc, page, PDPageContentStream.AppendMode.APPEND, true)) {
                cs.drawImage(pdImage, pos.getX(), pos.getY(), pos.getWidth(), pos.getHeight());
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            visualDoc.save(baos);
            return new ByteArrayInputStream(baos.toByteArray());
        }
    }
}
