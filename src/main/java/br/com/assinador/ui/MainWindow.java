package br.com.assinador.ui;

import br.com.assinador.certificate.CertificateInfo;
import br.com.assinador.certificate.Pkcs11CertificateProvider;
import br.com.assinador.config.Pkcs11Config;
import br.com.assinador.pdf.PdfSignerService;
import br.com.assinador.pdf.SignaturePosition;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.ButtonGroup;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;

public class MainWindow extends JFrame {

    private final JButton openButton = new JButton("Abrir PDF");
    private final JButton prevButton = new JButton("Página anterior");
    private final JButton nextButton = new JButton("Próxima página");
    private final JButton configureA3Button = new JButton("Configurar A3");
    private final JButton loadCertsButton = new JButton("Carregar certificados");
    private final JButton signButton = new JButton("Assinar PDF");
    private final JLabel pageLabel = new JLabel("Página: -");
    private final JComboBox<CertificateInfo> certCombo = new JComboBox<>();
    private final JTextArea logArea = new JTextArea(8, 80);
    private final JRadioButton allowMoreSignaturesRadio = new JRadioButton("Permitir outras assinaturas depois", true);
    private final JRadioButton lockDocumentRadio = new JRadioButton("Bloquear documento após esta assinatura");
    private final PdfViewerPanel viewerPanel = new PdfViewerPanel();

    private File currentPdfFile;
    private PDDocument currentDocument;
    private Pkcs11Config pkcs11Config;

    private final Pkcs11CertificateProvider certificateProvider = new Pkcs11CertificateProvider();
    private final PdfSignerService signerService = new PdfSignerService();

    public MainWindow() {
        super("Assinador PDF ICP-Brasil A3");
        setSize(1200, 800);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        setLayout(new BorderLayout());
        add(buildTopPanel(), BorderLayout.NORTH);
        add(new JScrollPane(viewerPanel), BorderLayout.CENTER);
        add(buildBottomPanel(), BorderLayout.SOUTH);

        logArea.setEditable(false);
        registerListeners();
    }

    private JPanel buildTopPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.add(openButton);
        panel.add(prevButton);
        panel.add(nextButton);
        panel.add(pageLabel);
        panel.add(configureA3Button);
        panel.add(loadCertsButton);
        panel.add(certCombo);
        ButtonGroup signatureModeGroup = new ButtonGroup();
        signatureModeGroup.add(allowMoreSignaturesRadio);
        signatureModeGroup.add(lockDocumentRadio);
        panel.add(allowMoreSignaturesRadio);
        panel.add(lockDocumentRadio);
        panel.add(signButton);
        return panel;
    }

    private JPanel buildBottomPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Log"));
        panel.add(new JScrollPane(logArea), BorderLayout.CENTER);
        return panel;
    }

    private void registerListeners() {
        openButton.addActionListener(e -> openPdf());
        prevButton.addActionListener(e -> changePage(-1));
        nextButton.addActionListener(e -> changePage(1));
        configureA3Button.addActionListener(e -> configureA3());
        loadCertsButton.addActionListener(e -> loadCertificates());
        signButton.addActionListener(e -> signPdf());
    }

    private void openPdf() {
        JFileChooser chooser = new JFileChooser();
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        try {
            if (currentDocument != null) {
                currentDocument.close();
            }
            currentPdfFile = chooser.getSelectedFile();
            currentDocument = Loader.loadPDF(currentPdfFile);
            viewerPanel.loadDocument(currentDocument);
            updatePageLabel();
            log("PDF aberto: " + currentPdfFile.getAbsolutePath());
        } catch (Exception ex) {
            logError("Erro ao abrir PDF", ex);
        }
    }

    private void changePage(int delta) {
        if (currentDocument == null) {
            return;
        }
        int next = viewerPanel.getPageIndex() + delta;
        if (next < 0 || next >= currentDocument.getNumberOfPages()) {
            return;
        }
        try {
            viewerPanel.setPageIndex(next);
            updatePageLabel();
        } catch (IOException ex) {
            logError("Erro ao renderizar página", ex);
        }
    }

    private void updatePageLabel() {
        if (currentDocument == null) {
            pageLabel.setText("Página: -");
            return;
        }
        pageLabel.setText("Página: " + (viewerPanel.getPageIndex() + 1) + "/" + currentDocument.getNumberOfPages());
    }

    private void configureA3() {
        JTextField dllField = new JTextField(40);
        JPasswordField pinField = new JPasswordField(20);

        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.add(new JLabel("Caminho da DLL PKCS#11:"));
        form.add(dllField);
        form.add(new JLabel("PIN:"));
        form.add(pinField);

        int option = JOptionPane.showConfirmDialog(this, form, "Configurar A3", JOptionPane.OK_CANCEL_OPTION);
        if (option == JOptionPane.OK_OPTION) {
            pkcs11Config = new Pkcs11Config(dllField.getText().trim(), pinField.getPassword());
            log("Configuração A3 atualizada.");
        }
    }

    private void loadCertificates() {
        if (pkcs11Config == null) {
            log("Configure o A3 antes de carregar certificados.");
            return;
        }
        try {
            certCombo.removeAllItems();
            List<CertificateInfo> certs = certificateProvider.loadCertificates(pkcs11Config);
            for (CertificateInfo cert : certs) {
                certCombo.addItem(cert);
            }
            log(certs.size() + " certificado(s) carregado(s).");
        } catch (Exception ex) {
            logError("Erro ao carregar certificados", ex);
        }
    }

    private void signPdf() {
        if (currentPdfFile == null || currentDocument == null) {
            log("Abra um PDF primeiro.");
            return;
        }
        CertificateInfo cert = (CertificateInfo) certCombo.getSelectedItem();
        if (cert == null) {
            log("Selecione um certificado.");
            return;
        }
        SignaturePosition position = viewerPanel.getSignaturePosition();
        if (position == null) {
            log("Selecione a área da assinatura com o mouse.");
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File(currentPdfFile.getParentFile(), currentPdfFile.getName().replace(".pdf", "") + "-assinado.pdf"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        try {
            boolean lockAfterSigning = lockDocumentRadio.isSelected();
            List<PDSignature> signatures = currentDocument.getSignatureDictionaries();
            if (lockAfterSigning && signatures != null && !signatures.isEmpty()) {
                log("Este PDF já possui assinatura anterior. O modo de bloqueio/certificação só pode ser usado na primeira assinatura do documento. Use o modo normal para adicionar nova assinatura.");
                return;
            }
            if (viewerPanel.hasComplexPageGeometry()) {
                log(viewerPanel.getGeometryWarningMessage());
            }
            if (cert.getCertificateChain() == null || cert.getCertificateChain().isEmpty()) {
                log("Aviso: cadeia de certificados do token não disponível para este alias. Será usado apenas o certificado do assinante.");
            }

            signerService.signPdf(currentPdfFile, chooser.getSelectedFile(), cert, position, lockAfterSigning);
            log("Modo de assinatura: " + (lockAfterSigning ? "Bloqueio/Certificação" : "Normal (permite assinaturas futuras)"));
            log("PDF assinado salvo em: " + chooser.getSelectedFile().getAbsolutePath());
        } catch (Exception ex) {
            logError("Erro ao assinar PDF", ex);
        }
    }

    private void log(String message) {
        logArea.append(message + System.lineSeparator());
    }

    private void logError(String message, Exception ex) {
        log(message + ": " + ex.getMessage());
        ex.printStackTrace();
    }
}
