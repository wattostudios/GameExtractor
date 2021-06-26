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

package org.watto.ge.plugin.viewer;

import org.watto.ErrorLogger;
import org.watto.component.PreviewPanel;
import org.watto.component.PreviewPanel_Image;
import org.watto.datatype.Archive;
import org.watto.datatype.ImageResource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.helper.ImageFormatReader;
import org.watto.ge.plugin.AllFilesPlugin;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.ge.plugin.archive.Plugin_KIT_KIT;
import org.watto.io.FileManipulator;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_KIT_KIT_RYHP extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_KIT_KIT_RYHP() {
    super("KIT_KIT_RYHP", "KIT_KIT_RYHP");
    setExtensions("ryhp");

    setGames("Unravel");
    setPlatforms("PC");
    setStandardFileFormat(false);
  }

  /**
  **********************************************************************************************
  Calculates the size of the padding, when the <code>length</code> needs to be a multiple of
  <code>multiple</code>
  @return the padding amount
  **********************************************************************************************
  **/
  public int calculatePadding(int length, int multiple) {
    int padding = length % multiple;
    if (padding == 0) {
      return 0;
    }

    return multiple - padding;
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
  public int getMatchRating(FileManipulator fm) {
    try {

      int rating = 0;

      ArchivePlugin plugin = Archive.getReadPlugin();
      if (plugin instanceof Plugin_KIT_KIT) {
        rating += 50;
      }
      else if (!(plugin instanceof AllFilesPlugin)) {
        return 0;
      }

      // 4 - Header
      if (fm.readString(4).equals("RYHP")) {
        rating += 50;
      }
      else {
        rating = 0;
      }

      long arcSize = fm.getLength();

      // 4 - Header Size (84)
      if (FieldValidator.checkLength(fm.readInt(), arcSize)) {
        rating += 5;
      }

      // 4 - Descriptor Length
      if (FieldValidator.checkLength(fm.readInt(), arcSize)) {
        rating += 5;
      }

      return rating;

    }
    catch (Throwable t) {
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

      long arcSize = fm.getLength();

      // 4 - Header (RYHP)
      fm.skip(4);

      // 4 - Header Size (84)
      int headerSize = fm.readInt();
      FieldValidator.checkLength(headerSize, arcSize);

      // 4 - Descriptor Length
      int descriptorSize = fm.readInt();
      FieldValidator.checkLength(descriptorSize, arcSize);

      // 72 - Unknown
      // X - Descriptors
      fm.seek(headerSize + descriptorSize);

      // 4 - Unknown (1)
      // 4 - Unknown (1)
      // 4 - Filename Offset (relative to the start of the ImageDetails) [+20]
      // 4 - Unknown (4)
      // 4 - Filename Offset (relative to the start of the ImageDetails) [+24]
      // 4 - null
      // 4 - Unknown (1)
      // 4 - Unknown (2)
      // 4 - null
      // 4 - Unknown (10)
      // 4 - Unknown (1)
      // 4 - Unknown (22)
      // 4 - Unknown (22)
      // 12 - null
      // 4 - Unknown (1)
      // 8 - null
      fm.skip(76);

      // X - Filename
      // 1 - null Filename Terminator
      fm.readNullString();

      // 0-3 - null Padding to a multiple of 4 bytes
      // [OPTIONAL] 4 - null
      // 1 - Unknown
      for (int p = 0; p < 100; p++) {
        if (fm.readByte() != 0) {
          break;
        }
      }

      // 4 - Number of Mipmaps?
      // 4 - Number of Mipmaps?
      // 4 - null
      fm.skip(12);

      // 4 - Image Width
      int width = fm.readInt();
      if (width <= 0 || width > 8192) {
        return null; // not an image
      }
      FieldValidator.checkWidth(width);

      // 4 - Image Height
      int height = fm.readInt();
      FieldValidator.checkHeight(height);

      // 10 - Image Header (PTexture2D)
      // 1 - null Image Header Terminator
      String imageHeader = fm.readNullString();
      if (!imageHeader.equals("PTexture2D")) {
        return null; // not an image
      }

      // X - Image Format (BC4/BC5/DXT5)
      // 1 - null Image Format Terminator
      String imageFormat = fm.readNullString();

      // 4 - Unknown (8)
      fm.skip(4);

      // 4 - Length of Extra Data (11)
      int extraLength = fm.readInt();
      FieldValidator.checkLength(extraLength, arcSize);

      // 4 - null
      // 4 - Unknown (2)
      // 4 - Unknown (4/5)
      // 4 - Length of Extra Data (11)
      fm.skip(16);

      // 11 - Unknown
      fm.skip(extraLength);

      // 2 - Unknown (1)
      fm.skip(2);

      // X - Pixels
      ImageResource imageResource = null;

      if (imageFormat.equals("DXT5")) {
        imageResource = ImageFormatReader.readDXT5(fm, width, height);
      }
      else if (imageFormat.equals("DXT1")) {
        imageResource = ImageFormatReader.readDXT1(fm, width, height);
      }
      else if (imageFormat.equals("BC4")) {
        imageResource = ImageFormatReader.readBC4(fm, width, height);
      }
      else if (imageFormat.equals("BC5")) {
        imageResource = ImageFormatReader.readBC5(fm, width, height);
      }
      else if (imageFormat.equals("ARGB8")) {
        imageResource = ImageFormatReader.readRGBA(fm, width, height);
      }
      else {
        ErrorLogger.log("[Viewer_KIT_KIT_RYHP]: Unknown Image Format: " + imageFormat);
      }

      if (imageResource == null) {
        return null;
      }

      // All images are vertically flipped
      imageResource = ImageFormatReader.flipVertically(imageResource);

      fm.close();

      //ColorConverter.convertToPaletted(resource);

      imageResource.addProperty("ImageFormat", imageFormat);

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