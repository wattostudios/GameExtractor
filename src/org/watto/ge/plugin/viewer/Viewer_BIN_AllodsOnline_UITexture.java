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

package org.watto.ge.plugin.viewer;

import org.watto.ErrorLogger;
import org.watto.component.PreviewPanel;
import org.watto.component.PreviewPanel_Image;
import org.watto.datatype.ImageResource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.helper.ImageFormatReader;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.ge.plugin.exporter.Exporter_ZLib_CompressedSizeOnly;
import org.watto.io.FileManipulator;
import org.watto.io.buffer.ByteBuffer;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_BIN_AllodsOnline_UITexture extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_BIN_AllodsOnline_UITexture() {
    super("BIN_AllodsOnline_UITexture", "Allods Online BIN UI Image");
    setExtensions("bin");

    setGames("Allods Online");
    setPlatforms("PC");
    setStandardFileFormat(false);
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public boolean canWrite(PreviewPanel panel) {
    return false;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public boolean canReplace(PreviewPanel panel) {
    return false;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public int getMatchRating(FileManipulator fm) {
    try {

      int rating = 0;

      if (FieldValidator.checkExtension(fm, extensions)) {
        rating += 25;
      }

      if (fm.getFile().getName().contains("(UITexture)")) {
        rating += 25;
      }
      else {
        rating = 0;
      }

      String header = fm.readString(1);
      if (header.equals("x")) {
        rating += 5;
      }

      return rating;

    }
    catch (

    Throwable t) {
      return 0;
    }
  }

  /**
  **********************************************************************************************
  Reads a resource from the FileManipulator, and generates a PreviewPanel for it. The FileManipulator
  is an extracted temp file, not the original archive!
  **********************************************************************************************
  **/
  @Override
  public PreviewPanel read(FileManipulator fm) {
    try {

      ImageResource imageResource = readThumbnail(fm);

      if (imageResource == null) {
        return null;
      }

      PreviewPanel_Image preview = new PreviewPanel_Image(imageResource);

      return preview;

    }
    catch (Throwable t) {
      logError(t);
      return null;
    }
  }

  /**
  **********************************************************************************************
  Reads a resource from the FileManipulator, and generates a Thumbnail for it (generally, only
  an Image ViewerPlugin will do this, but others can do it if they want). The FileManipulator is
  an extracted temp file, not the original archive!
  **********************************************************************************************
  **/

  @Override
  public ImageResource readThumbnail(FileManipulator fm) {
    try {

      String filename = fm.getFile().getName();
      String filepath = fm.getFile().getAbsolutePath();

      int arcSize = (int) fm.getLength();

      Exporter_ZLib_CompressedSizeOnly exporter = Exporter_ZLib_CompressedSizeOnly.getInstance();
      exporter.open(fm, arcSize, arcSize);

      byte[] fileData = new byte[arcSize * 10]; // guess max 10* compression

      int decompWritePos = 0;
      while (exporter.available()) { // make sure we read the next bit of data, if required
        fileData[decompWritePos++] = (byte) exporter.read();
      }

      fm.close();
      fm = new FileManipulator(new ByteBuffer(fileData));

      // 4 - Unknown (0)
      fm.skip(4);

      // 4 - Data Length
      int dataLength = fm.readInt();

      int width = 0;
      int height = 0;
      String imageFormat = "DXT1";

      if (dataLength == 2048) {
        if (filename.equals("HaloSampleActive.(UITexture).bin") || ((filepath.contains("Currency") || filepath.contains("MapTracks")) && !(filename.contains("Ice_Shard") || filename.contains("Horse")))) {
          imageFormat = "DXT3";
          width = 32;
          height = 64;
        }
        else {
          width = 64;
          height = 64;
          imageFormat = "DXT1";
        }
      }
      else if (dataLength == 4096) {
        if (filename.startsWith("Context")) {
          imageFormat = "DXT1";
          width = 64;
          height = 128;
        }
        else {
          imageFormat = "DXT3";
          width = 64;
          height = 64;
        }
      }
      else if (dataLength == 32768) {
        width = 256;
        height = 256;
        imageFormat = "DXT1";
      }
      else if (dataLength == 16384) {
        width = 128;
        height = 128;
        imageFormat = "DXT3";
      }
      else if (dataLength == 1024) {
        width = 32;
        height = 32;
        imageFormat = "DXT3";
      }
      else {
        ErrorLogger.log("[Viewer_BIN_AllodsOnline_UITexture] Unknown Width/Height for Data Size: " + dataLength);
        return null;
      }

      // X - Image Data
      ImageResource imageResource = null;
      if (imageFormat.equals("DXT1")) {
        imageResource = ImageFormatReader.readDXT1(fm, width, height);
      }
      else if (imageFormat.equals("DXT3")) {
        imageResource = ImageFormatReader.readDXT3(fm, width, height);
      }

      fm.close();

      return imageResource;

    }
    catch (Throwable t) {
      logError(t);
      return null;
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public void write(PreviewPanel preview, FileManipulator fm) {

  }

}