package com.ning.compress.lzf;

import com.ning.compress.CompressionFormatException;

public class LZFException extends CompressionFormatException
{
    private static final long serialVersionUID = 1L;

    public LZFException(String message) {
        super(message);
    }

    public LZFException(Throwable t) {
        super(t);
    }

    public LZFException(String message, Throwable t) {
        super(message, t);
    }
}
