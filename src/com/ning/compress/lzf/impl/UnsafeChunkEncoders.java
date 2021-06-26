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

package com.ning.compress.lzf.impl;

import com.ning.compress.BufferRecycler;
import java.nio.ByteOrder;


/**
 * Class that handles actual encoding of individual chunks.
 * Resulting chunks can be compressed or non-compressed; compression
 * is only used if it actually reduces chunk size (including overhead
 * of additional header bytes)
 * 
 * @author Tatu Saloranta (tatu.saloranta@iki.fi)
 */
public final class UnsafeChunkEncoders
{
    private final static boolean LITTLE_ENDIAN = (ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN);
    
    public static UnsafeChunkEncoder createEncoder(int totalLength) {
        if (LITTLE_ENDIAN) {
            return new UnsafeChunkEncoderLE(totalLength);
        }
        return new UnsafeChunkEncoderBE(totalLength);
    }

    public static UnsafeChunkEncoder createNonAllocatingEncoder(int totalLength) {
        if (LITTLE_ENDIAN) {
            return new UnsafeChunkEncoderLE(totalLength, false);
        }
        return new UnsafeChunkEncoderBE(totalLength, false);
    }

    public static UnsafeChunkEncoder createEncoder(int totalLength, BufferRecycler bufferRecycler) {
        if (LITTLE_ENDIAN) {
            return new UnsafeChunkEncoderLE(totalLength, bufferRecycler);
        }
        return new UnsafeChunkEncoderBE(totalLength, bufferRecycler);
    }

    public static UnsafeChunkEncoder createNonAllocatingEncoder(int totalLength, BufferRecycler bufferRecycler) {
        if (LITTLE_ENDIAN) {
            return new UnsafeChunkEncoderLE(totalLength, bufferRecycler, false);
        }
        return new UnsafeChunkEncoderBE(totalLength, bufferRecycler, false);
    }
}
