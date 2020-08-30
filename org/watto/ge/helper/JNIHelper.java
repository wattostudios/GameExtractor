/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2020 wattostudios
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

/**
**********************************************************************************************
Helper class for JNI Handling
**********************************************************************************************
**/
public class JNIHelper {

  public static void loadLibrary(String libName) {
    try {
      String basePath = new File("").getAbsolutePath() + File.separatorChar + "jni" + File.separatorChar;
      if (System.getProperty("os.arch").equals("x86")) {
        //System.loadLibrary(libName + "_32");
        System.load(basePath + libName + "_32.dll");
      }
      else {
        //System.loadLibrary(libName + "_64"); //64bit
        System.load(basePath + libName + "_64.dll");
      }
    }
    catch (Throwable t) {
      ErrorLogger.log(t);
    }
  }

}