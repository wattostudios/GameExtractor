/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2024 wattostudios
 *
 * License Information:
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License
 * published by the Free Software Foundation; either version 2 of the License, or (at your option) any later versions. This
 * program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranties
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License at http://www.gnu.org for more
 * details. For further information on this application, refer to the authors' website.
 */

package org.watto.ge.helper;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.NativeLibrary;
import com.sun.jna.Pointer;

public interface NativeQuickBMS extends Library {

  public static final String JNA_LIBRARY_NAME = "quickbms";

  public static final NativeLibrary JNA_NATIVE_LIB = NativeLibrary.getInstance(NativeQuickBMS.JNA_LIBRARY_NAME);

  @SuppressWarnings("deprecation")
  public static final NativeQuickBMS INSTANCE = (NativeQuickBMS) Native.loadLibrary(NativeQuickBMS.JNA_LIBRARY_NAME, NativeQuickBMS.class);

  int quickbms_compression(String algo, Pointer in, int zsize, Pointer out, int size);

}
