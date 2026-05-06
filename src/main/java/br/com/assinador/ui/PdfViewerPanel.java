package br.com.assinador.ui;

import br.com.assinador.pdf.SignaturePosition;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;

import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;

public class PdfViewerPanel extends JPanel {

    private static final float RENDER_SCALE = 1.5f;
    private BufferedImage renderedPage;
    private PDDocument document;
    private int pageIndex;

    private Rectangle selectionRect;
    private Point dragStart;

    public PdfViewerPanel() {
        setBackground(Color.DARK_GRAY);
        MouseAdapter adapter = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (renderedPage == null) {
                    return;
                }
                dragStart = e.getPoint();
                selectionRect = new Rectangle(dragStart);
                repaint();
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (dragStart == null) {
                    return;
                }
                selectionRect = createRect(dragStart, e.getPoint());
                repaint();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (dragStart == null) {
                    return;
                }
                selectionRect = createRect(dragStart, e.getPoint());
                dragStart = null;
                repaint();
            }
        };
        addMouseListener(adapter);
        addMouseMotionListener(adapter);
    }

    public void loadDocument(PDDocument document) throws IOException {
        this.document = document;
        this.pageIndex = 0;
        renderCurrentPage();
    }

    public void setPageIndex(int pageIndex) throws IOException {
        this.pageIndex = pageIndex;
        renderCurrentPage();
    }

    public int getPageIndex() {
        return pageIndex;
    }


    public boolean hasComplexPageGeometry() {
        if (document == null) {
            return false;
        }
        var page = document.getPage(pageIndex);
        boolean rotated = page.getRotation() != 0;
        boolean cropDiffers = !page.getCropBox().equals(page.getMediaBox());
        return rotated || cropDiffers;
    }

    public String getGeometryWarningMessage() {
        if (document == null) {
            return "";
        }
        var page = document.getPage(pageIndex);
        boolean rotated = page.getRotation() != 0;
        boolean cropDiffers = !page.getCropBox().equals(page.getMediaBox());
        if (!rotated && !cropDiffers) {
            return "";
        }
        return "Aviso: esta página possui rotação e/ou CropBox diferente do MediaBox. A posição visual da assinatura pode exigir ajuste manual.";
    }

    public SignaturePosition getSignaturePosition() {
        if (document == null || renderedPage == null || selectionRect == null || selectionRect.width < 2 || selectionRect.height < 2) {
            return null;
        }

        float pageWidth = document.getPage(pageIndex).getMediaBox().getWidth();
        float pageHeight = document.getPage(pageIndex).getMediaBox().getHeight();

        float scaleX = pageWidth / renderedPage.getWidth();
        float scaleY = pageHeight / renderedPage.getHeight();

        float x = selectionRect.x * scaleX;
        float width = selectionRect.width * scaleX;
        float height = selectionRect.height * scaleY;
        float yFromTop = selectionRect.y * scaleY;
        float y = pageHeight - yFromTop - height;

        return new SignaturePosition(pageIndex, x, y, width, height);
    }

    private void renderCurrentPage() throws IOException {
        if (document == null) {
            return;
        }
        PDFRenderer renderer = new PDFRenderer(document);
        renderedPage = renderer.renderImage(pageIndex, RENDER_SCALE);
        selectionRect = null;
        setPreferredSize(new Dimension(renderedPage.getWidth(), renderedPage.getHeight()));
        revalidate();
        repaint();
    }

    private Rectangle createRect(Point a, Point b) {
        int x = Math.min(a.x, b.x);
        int y = Math.min(a.y, b.y);
        int w = Math.abs(a.x - b.x);
        int h = Math.abs(a.y - b.y);
        return new Rectangle(x, y, w, h);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (renderedPage == null) {
            return;
        }
        g.drawImage(renderedPage.getScaledInstance(renderedPage.getWidth(), renderedPage.getHeight(), Image.SCALE_SMOOTH), 0, 0, null);
        if (selectionRect != null) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setColor(new Color(30, 144, 255, 80));
            g2.fill(selectionRect);
            g2.setColor(Color.BLUE);
            g2.setStroke(new BasicStroke(2f));
            g2.draw(selectionRect);
            g2.dispose();
        }
    }
}
