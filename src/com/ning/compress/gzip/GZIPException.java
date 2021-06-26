package com.ning.compress.gzip;

import com.ning.compress.CompressionFormatException;

public class GZIPException extends CompressionFormatException
{
    private static final long serialVersionUID = 1L;

    public GZIPException(String message) {
        super(message);
    }

    public GZIPException(Throwable t) {
        super(t);
    }

    public GZIPException(String message, Throwable t) {
        super(message, t);
    }
}
