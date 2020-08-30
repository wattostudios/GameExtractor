package com.ning.compress.lzf.util;

import com.ning.compress.BufferRecycler;
import com.ning.compress.lzf.ChunkEncoder;
import com.ning.compress.lzf.LZFChunk;
import com.ning.compress.lzf.impl.UnsafeChunkEncoders;
import com.ning.compress.lzf.impl.VanillaChunkEncoder;

/**
 * Simple helper class used for loading
 * {@link ChunkEncoder} implementations, based on criteria
 * such as "fastest available" or "safe to run anywhere".
 */
public class ChunkEncoderFactory
{
    /*
    ///////////////////////////////////////////////////////////////////////
    // Public API
    ///////////////////////////////////////////////////////////////////////
     */

    /**
     * Convenience method, equivalent to:
     *<code>
     *   return optimalInstance(LZFChunk.MAX_CHUNK_LEN);
     *</code>
     */
    public static ChunkEncoder optimalInstance() {
        return optimalInstance(LZFChunk.MAX_CHUNK_LEN);
    }

    /**
     * Method to use for getting compressor instance that uses the most optimal
     * available methods for underlying data access. It should be safe to call
     * this method as implementations are dynamically loaded; however, on some
     * non-standard platforms it may be necessary to either directly load
     * instances, or use {@link #safeInstance}.
     *
	 * <p/>Uses a ThreadLocal soft-referenced BufferRecycler instance.
	 * 
     * @param totalLength Expected total length of content to compress; only matters
     *    for content that is smaller than maximum chunk size (64k), to optimize
     *    encoding hash tables
     */
    public static ChunkEncoder optimalInstance(int totalLength) {
        try {
            return UnsafeChunkEncoders.createEncoder(totalLength);
        } catch (Exception e) {
            return safeInstance(totalLength);
        }
    }

    /**
     * Factory method for constructing encoder that is always passed buffer
     * externally, so that it will not (nor need) allocate encoding buffer.
     * <p>
     * Uses a ThreadLocal soft-referenced BufferRecycler instance.
     */
    public static ChunkEncoder optimalNonAllocatingInstance(int totalLength) {
        try {
            return UnsafeChunkEncoders.createNonAllocatingEncoder(totalLength);
        } catch (Exception e) {
            return safeNonAllocatingInstance(totalLength);
        }
    }

    /**
     * Convenience method, equivalent to:
     *<code>
     *   return safeInstance(LZFChunk.MAX_CHUNK_LEN);
     *</code>
     */
    public static ChunkEncoder safeInstance() {
        return safeInstance(LZFChunk.MAX_CHUNK_LEN);
    }
	
    /**
     * Method that can be used to ensure that a "safe" compressor instance is loaded.
     * Safe here means that it should work on any and all Java platforms.
     * <p>
     * Uses a ThreadLocal soft-referenced BufferRecycler instance.
     *
     * @param totalLength Expected total length of content to compress; only matters
     *    for content that is smaller than maximum chunk size (64k), to optimize
     *    encoding hash tables
     */
    public static ChunkEncoder safeInstance(int totalLength) {
        return new VanillaChunkEncoder(totalLength);
    }

    /**
     * Factory method for constructing encoder that is always passed buffer
     * externally, so that it will not (nor need) allocate encoding buffer.
	*<p>Uses a ThreadLocal soft-referenced BufferRecycler instance.
     */
    public static ChunkEncoder safeNonAllocatingInstance(int totalLength) {
        return VanillaChunkEncoder.nonAllocatingEncoder(totalLength);
    }

    /**
     * Convenience method, equivalent to:
     *<code>
     *   return optimalInstance(LZFChunk.MAX_CHUNK_LEN, bufferRecycler);
     *</code>
     */
    public static ChunkEncoder optimalInstance(BufferRecycler bufferRecycler) {
        return optimalInstance(LZFChunk.MAX_CHUNK_LEN, bufferRecycler);
    }

    /**
     * Method to use for getting compressor instance that uses the most optimal
     * available methods for underlying data access. It should be safe to call
     * this method as implementations are dynamically loaded; however, on some
     * non-standard platforms it may be necessary to either directly load
     * instances, or use {@link #safeInstance}.
	 * 
     * @param totalLength Expected total length of content to compress; only matters
     *    for content that is smaller than maximum chunk size (64k), to optimize
     *    encoding hash tables
	 * @param bufferRecycler The BufferRecycler instance
     */
    public static ChunkEncoder optimalInstance(int totalLength, BufferRecycler bufferRecycler) {
        try {
            return UnsafeChunkEncoders.createEncoder(totalLength, bufferRecycler);
        } catch (Exception e) {
            return safeInstance(totalLength, bufferRecycler);
        }
    }

    /**
     * Factory method for constructing encoder that is always passed buffer
     * externally, so that it will not (nor need) allocate encoding buffer.
     */
    public static ChunkEncoder optimalNonAllocatingInstance(int totalLength, BufferRecycler bufferRecycler) {
        try {
            return UnsafeChunkEncoders.createNonAllocatingEncoder(totalLength, bufferRecycler);
        } catch (Exception e) {
            return safeNonAllocatingInstance(totalLength, bufferRecycler);
        }
    }

    /**
     * Convenience method, equivalent to:
     *<code>
     *   return safeInstance(LZFChunk.MAX_CHUNK_LEN, bufferRecycler);
     *</code>
     */
    public static ChunkEncoder safeInstance(BufferRecycler bufferRecycler) {
        return safeInstance(LZFChunk.MAX_CHUNK_LEN, bufferRecycler);
    }
    /**
     * Method that can be used to ensure that a "safe" compressor instance is loaded.
     * Safe here means that it should work on any and all Java platforms.
     *
     * @param totalLength Expected total length of content to compress; only matters
     *    for content that is smaller than maximum chunk size (64k), to optimize
     *    encoding hash tables
	 * @param bufferRecycler The BufferRecycler instance
     */
    public static ChunkEncoder safeInstance(int totalLength, BufferRecycler bufferRecycler) {
        return new VanillaChunkEncoder(totalLength, bufferRecycler);
    }

    /**
     * Factory method for constructing encoder that is always passed buffer
     * externally, so that it will not (nor need) allocate encoding buffer.
     */
    public static ChunkEncoder safeNonAllocatingInstance(int totalLength, BufferRecycler bufferRecycler) {
        return VanillaChunkEncoder.nonAllocatingEncoder(totalLength, bufferRecycler);
    }
}
