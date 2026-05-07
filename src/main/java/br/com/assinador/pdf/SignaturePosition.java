package br.com.assinador.pdf;

public class SignaturePosition {
    private final int pageIndex;
    private final float x;
    private final float y;
    private final float width;
    private final float height;

    public SignaturePosition(int pageIndex, float x, float y, float width, float height) {
        this.pageIndex = pageIndex;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public int getPageIndex() {
        return pageIndex;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public float getWidth() {
        return width;
    }

    public float getHeight() {
        return height;
    }
}
