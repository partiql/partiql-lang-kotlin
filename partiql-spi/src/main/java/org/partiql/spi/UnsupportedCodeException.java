package org.partiql.spi;

/**
 * This is specifically thrown when an operation is not implemented for a particular {@link Enum#code()}.
 * @see Enum#name()
 */
public class UnsupportedCodeException extends RuntimeException {
    public UnsupportedCodeException(int code) {
        super("Code not supported: " + code + ".");
    }
}
