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

import org.watto.Settings;
import org.watto.component.WSPluginException;
import org.watto.io.FileManipulator;
import org.watto.io.FilenameSplitter;

public class FieldValidator {

  static int maxNumFiles2 = Settings.getInt("MaxNumberOfFiles2");
  static int maxNumFiles4 = Settings.getInt("MaxNumberOfFiles4");
  static int maxFilenameLength = Settings.getInt("MaxFilenameLength");

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public static boolean checkColorDepth(long colorDepth) throws Exception {
    return colorDepth(colorDepth);
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public static boolean checkEquals(long length1, long length2) throws Exception {
    return equals(length1, length2);
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public static boolean checkEquals(String text1, String text2) throws Exception {
    return text1.equals(text2);
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public static boolean checkExtension(FileManipulator fm, String... extensions) throws Exception {
    String ext = FilenameSplitter.getExtension(fm.getFile()).toLowerCase();

    for (int i = 0; i < extensions.length; i++) {
      if (ext.equals(extensions[i])) {
        return true;
      }
    }
    return false;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public static boolean checkFilename(String filename) throws Exception {
    return filename(filename);
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public static boolean checkFilenameLength(int filenameLength) throws Exception {
    return filenameLength(filenameLength);
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public static boolean checkFilenameLength(String filename) throws Exception {
    return filenameLength(filename);
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public static boolean checkHeight(long height) throws Exception {
    return height(height);
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public static boolean checkLength(long length) throws Exception {
    return length(length);
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public static boolean checkLength(long length, long arcSize) throws Exception {
    return length(length, arcSize);
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public static boolean checkLength(long length, long offset, long arcSize) throws Exception {
    return length(length, offset, arcSize);
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public static boolean checkNumColors(long numColors) throws Exception {
    return numColors(numColors);
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public static boolean checkNumColors(long numColors, long maxNumColors) throws Exception {
    return numColors(numColors, maxNumColors);
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public static boolean checkNumFiles(int numFiles) throws Exception {
    return numFiles(numFiles);
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public static boolean checkNumFiles(short numFiles) throws Exception {
    return numFiles(numFiles);
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public static boolean checkOffset(long offset) throws Exception {
    return offset(offset);
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public static boolean checkOffset(long offset, long arcSize) throws Exception {
    return offset(offset, arcSize);
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public static boolean checkPositive(long value) throws Exception {
    return positive(value);
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public static boolean checkRange(int value, int lowRange, int highRange) throws Exception {
    if (value >= lowRange && value <= highRange) {
      return true;
    }
    throw new WSPluginException("Value " + value + " is not in the range " + lowRange + "-" + highRange);
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public static boolean checkWidth(long width) throws Exception {
    return width(width);
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public static boolean colorDepth(long colorDepth) throws Exception {
    if (colorDepth <= 0 || colorDepth > 128) {
      throw new WSPluginException("Invalid Color Depth: " + colorDepth);
    }
    return true;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public static boolean equals(long length1, long length2) throws Exception {
    if (length1 != length2) {
      throw new WSPluginException("Unequal Sizes: " + length1 + "!=" + length2);
    }
    return true;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public static boolean filename(String filename) throws Exception {
    if (filename == null || filename.length() <= 0 || filename.equals("")) {
      throw new WSPluginException("Invalid Filename: " + filename);
    }
    return true;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public static boolean filenameLength(int filenameLength) throws Exception {
    if (filenameLength <= 0 || filenameLength > maxFilenameLength) {
      throw new WSPluginException("Invalid Filename Length: " + filenameLength);
    }
    return true;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public static boolean filenameLength(String filename) throws Exception {
    int filenameLength = filename.length();
    if (filenameLength <= 0 || filenameLength > maxFilenameLength) {
      throw new WSPluginException("Invalid Filename Length: " + filenameLength);
    }
    return true;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public static boolean height(long height) throws Exception {
    if (height <= 0 || height > 100000) {
      throw new WSPluginException("Invalid Height: " + height);
    }
    return true;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public static boolean length(long length) throws Exception {
    if (length < 0) {
      throw new WSPluginException("Invalid Length: " + length);
    }
    return true;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public static boolean length(long length, long arcSize) throws Exception {
    if (length < 0 || length > arcSize) {
      throw new WSPluginException("Invalid Length: " + length + " <> " + arcSize);
    }
    return true;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public static boolean length(long length, long offset, long arcSize) throws Exception {
    if (length < 0 || offset < 0 || length > arcSize || offset >= arcSize || offset + length > arcSize) {
      throw new WSPluginException("Invalid Length: " + length + "+" + offset + " <> " + arcSize);
    }
    return true;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public static boolean numColors(long numColors) throws Exception {
    if (numColors <= 0) {
      throw new WSPluginException("Invalid Number Of Colors: " + numColors);
    }
    return true;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public static boolean numColors(long numColors, long maxNumColors) throws Exception {
    if (numColors <= 0 || numColors > maxNumColors) {
      throw new WSPluginException("Invalid Number Of Colors: " + numColors);
    }
    return true;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public static boolean numFiles(int numFiles) throws Exception {
    if (numFiles <= 0 || numFiles > maxNumFiles4) {
      throw new WSPluginException("Invalid Number Of Files: " + numFiles);
    }
    return true;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public static boolean numFiles(short numFiles) throws Exception {
    if (numFiles <= 0 || numFiles > maxNumFiles2) {
      throw new WSPluginException("Invalid Number Of Files: " + numFiles);
    }
    return true;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public static boolean offset(long offset) throws Exception {
    if (offset < 0) {
      throw new WSPluginException("Invalid Offset: " + offset);
    }
    return true;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public static boolean offset(long offset, long arcSize) throws Exception {
    if (offset < 0 || offset >= arcSize) {
      throw new WSPluginException("Invalid Offset: " + offset + " <> " + arcSize);
    }
    return true;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public static boolean positive(long value) throws Exception {
    if (value < 0) {
      throw new WSPluginException("Not positive: " + value);
    }
    return true;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public static boolean width(long width) throws Exception {
    if (width <= 0 || width > 100000) {
      throw new WSPluginException("Invalid Width: " + width);
    }
    return true;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public FieldValidator() {
  }

}