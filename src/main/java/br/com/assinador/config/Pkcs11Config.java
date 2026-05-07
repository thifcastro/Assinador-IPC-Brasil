package br.com.assinador.config;

public class Pkcs11Config {
    private final String libraryPath;
    private final char[] pin;

    public Pkcs11Config(String libraryPath, char[] pin) {
        this.libraryPath = libraryPath;
        this.pin = pin;
    }

    public String getLibraryPath() {
        return libraryPath;
    }

    public char[] getPin() {
        return pin;
    }
}
