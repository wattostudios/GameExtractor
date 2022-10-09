/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2021 wattostudios
 *
 * License Information:
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License
 * published by the Free Software Foundation; either version 2 of the License, or (at your option) any later versions. This
 * program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranties
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License at http://www.gnu.org for more
 * details. For further information on this application, refer to the authors' website.
 */

package org.watto.ge.helper;

import org.watto.datatype.jna.JNA_UINT8;
import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.NativeLibrary;
import com.sun.jna.Pointer;

public interface NativeOodleDecomp32 extends Library {

  public static final String JNA_LIBRARY_NAME = "oo32";

  public static final NativeLibrary JNA_NATIVE_LIB = NativeLibrary.getInstance(NativeOodleDecomp32.JNA_LIBRARY_NAME);

  @SuppressWarnings("deprecation")
  public static final NativeOodleDecomp32 INSTANCE = (NativeOodleDecomp32) Native.loadLibrary(NativeOodleDecomp32.JNA_LIBRARY_NAME, NativeOodleDecomp32.class, NativeOodleDecomp32FunctionMapper.buildLibraryMap());

  int OodleLZ_Compress(int codec, JNA_UINT8 src_buf, long src_len, JNA_UINT8 dst_buf, int level, Pointer opts, long offs, long unused, Pointer scratch, long scratch_size);

  int OodleLZ_Decompress(JNA_UINT8 src_buf, int src_len, JNA_UINT8 dst, long dst_size, int fuzz, int crc, int verbose, JNA_UINT8 dst_base, long e, Pointer cb, Pointer cb_ctx, Pointer scratch, long scratch_size, int threadPhase);

}
