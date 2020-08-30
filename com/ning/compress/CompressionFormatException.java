package com.ning.compress;

import java.io.IOException;

/**
 * Base exception used by compression codecs when encountering a problem
 * with underlying data format, usually due to data corruption.
 */
public class CompressionFormatException extends IOException
{
    private static final long serialVersionUID = 1L;

    protected CompressionFormatException(String message) {
        super(message);
    }

    protected CompressionFormatException(Throwable t) {
        super();
        initCause(t);
    }

    protected CompressionFormatException(String message, Throwable t) {
        super(message);
        initCause(t);
    }
}
