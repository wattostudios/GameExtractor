/* Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
 * file except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS
 * OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package com.ning.compress.lzf;

import java.util.concurrent.atomic.AtomicReference;

import com.ning.compress.lzf.util.ChunkDecoderFactory;

/**
 * Decoder that handles decoding of sequence of encoded LZF chunks,
 * combining them into a single contiguous result byte array.
 * This class has been mostly replaced by
 * {@link ChunkDecoder}, although static methods are left here
 * and may still be used for convenience.
 * All static methods use {@link ChunkDecoderFactory#optimalInstance}
 * to find actual {@link ChunkDecoder} instance to use.
 * 
 * @author Tatu Saloranta (tatu.saloranta@iki.fi)
 * 
 * @see com.ning.compress.lzf.ChunkDecoder
 */
public class LZFDecoder
{
    /**
     * Lazily initialized "fast" instance that may use <code>sun.misc.Unsafe</code>
     * to speed up decompression
     */
    protected final static AtomicReference<ChunkDecoder> _fastDecoderRef = new AtomicReference<ChunkDecoder>();

    /**
     * Lazily initialized "safe" instance that DOES NOT use <code>sun.misc.Unsafe</code>
     * for decompression, just standard JDK functionality.
     */
    protected final static AtomicReference<ChunkDecoder> _safeDecoderRef = new AtomicReference<ChunkDecoder>();

    /*
    ///////////////////////////////////////////////////////////////////////
    // Factory methods for ChunkDecoders
    ///////////////////////////////////////////////////////////////////////
     */

    /**
     * Accessor method that can be used to obtain {@link ChunkDecoder}
     * that uses all possible optimization methods available, including
     * <code>sun.misc.Unsafe</code> for memory access.
     */
    public static ChunkDecoder fastDecoder() {
        // race conditions are ok here, we don't really mind
        ChunkDecoder dec = _fastDecoderRef.get();
        if (dec == null) { // 
            dec = ChunkDecoderFactory.optimalInstance();
            _fastDecoderRef.compareAndSet(null, dec);
        }
        return dec;
    }

    /**
     * Accessor method that can be used to obtain {@link ChunkDecoder}
     * that only uses standard JDK access methods, and should work on
     * all Java platforms and JVMs.
     */
    public static ChunkDecoder safeDecoder() {
        // race conditions are ok here, we don't really mind
        ChunkDecoder dec = _safeDecoderRef.get();
        if (dec == null) { // 
            dec = ChunkDecoderFactory.safeInstance();
            _safeDecoderRef.compareAndSet(null, dec);
        }
        return dec;
    }
    
    /*
    ///////////////////////////////////////////////////////////////////////
    // Basic API, general
    ///////////////////////////////////////////////////////////////////////
     */

    /**
     * Helper method that checks resulting size of an LZF chunk, regardless of
     * whether it contains compressed or uncompressed contents.
     */
    public static int calculateUncompressedSize(byte[] data, int offset, int length) throws LZFException {
        return ChunkDecoder.calculateUncompressedSize(data, length, length);
    }

    /*
    ///////////////////////////////////////////////////////////////////////
    // Basic API, fast decode methods
    ///////////////////////////////////////////////////////////////////////
     */
    
    public static byte[] decode(final byte[] inputBuffer) throws LZFException {
        return fastDecoder().decode(inputBuffer, 0, inputBuffer.length);
    }
    
    public static byte[] decode(final byte[] inputBuffer, int offset, int length) throws LZFException {
        return fastDecoder().decode(inputBuffer, offset, length);
    }
    
    public static int decode(final byte[] inputBuffer, final byte[] targetBuffer) throws LZFException {
        return fastDecoder().decode(inputBuffer, 0, inputBuffer.length, targetBuffer);
    }

    public static int decode(final byte[] sourceBuffer, int offset, int length, final byte[] targetBuffer)
            throws LZFException {
        return fastDecoder().decode(sourceBuffer, offset, length, targetBuffer);        
    }

    /*
    ///////////////////////////////////////////////////////////////////////
    // Basic API, "safe" decode methods
    ///////////////////////////////////////////////////////////////////////
     */

    public static byte[] safeDecode(final byte[] inputBuffer) throws LZFException {
        return safeDecoder().decode(inputBuffer, 0, inputBuffer.length);
    }

    public static byte[] safeDecode(final byte[] inputBuffer, int offset, int length) throws LZFException {
        return safeDecoder().decode(inputBuffer, offset, length);
    }

    public static int safeDecode(final byte[] inputBuffer, final byte[] targetBuffer) throws LZFException {
        return safeDecoder().decode(inputBuffer, 0, inputBuffer.length, targetBuffer);
    }

    public static int safeDecode(final byte[] sourceBuffer, int offset, int length, final byte[] targetBuffer)
            throws LZFException {
        return safeDecoder().decode(sourceBuffer, offset, length, targetBuffer);        
    }
}
