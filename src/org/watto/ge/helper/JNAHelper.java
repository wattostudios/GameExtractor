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

import java.io.File;
import org.watto.ErrorLogger;
import com.sun.jna.NativeLibrary;

/**
**********************************************************************************************
Helper class for JNA Handling
**********************************************************************************************
**/
public class JNAHelper {

  static boolean pathsAdded = false;

  public static void setLibraryPaths() {
    if (!pathsAdded) {
      try {
        String basePath = new File("").getAbsolutePath() + File.separatorChar + "jni" + File.separatorChar;

        NativeLibrary.addSearchPath(NativeOodleDecomp32.JNA_LIBRARY_NAME, basePath);
        NativeLibrary.addSearchPath(NativeOodleDecomp64.JNA_LIBRARY_NAME, basePath);

        pathsAdded = true;
      }
      catch (Throwable t) {
        ErrorLogger.log(t);
      }
    }
  }

}