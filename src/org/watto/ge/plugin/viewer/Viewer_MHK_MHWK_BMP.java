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

import org.watto.component.PreviewPanel;
import org.watto.component.PreviewPanel_Image;
import org.watto.datatype.Archive;
import org.watto.datatype.ImageResource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.AllFilesPlugin;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.ge.plugin.archive.Plugin_MHK_MHWK;
import org.watto.io.FileManipulator;
import org.watto.io.converter.ShortConverter;

/**
**********************************************************************************************
Based on scummvm-1.4.1/engines/mohawk/bitmap.cpp
**********************************************************************************************
**/
public class Viewer_MHK_MHWK_BMP extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_MHK_MHWK_BMP() {
    super("MHK_MHWK_BMP", "Mohawk Games BMP Image [MHK_MHWK_BMP]");
    setExtensions("bmp");

    setGames("Green Eggs and Ham");
    setPlatforms("PC");
    setStandardFileFormat(false);

    setEnabled(false);// not working
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
      if (plugin instanceof Plugin_MHK_MHWK) {
        rating += 50;
      }
      else if (!(plugin instanceof AllFilesPlugin)) {
        return 0;
      }

      if (FieldValidator.checkExtension(fm, extensions)) {
        rating += 25;
      }
      else {
        return 0;
      }

      fm.skip(4);

      // 2 - Image Height
      if (FieldValidator.checkHeight(ShortConverter.changeFormat(fm.readShort()))) {
        rating += 5;
      }

      // 2 - Image Width
      if (FieldValidator.checkWidth(ShortConverter.changeFormat(fm.readShort()))) {
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

  @SuppressWarnings("unused")
  @Override
  public ImageResource readThumbnail(FileManipulator fm) {
    try {

      long arcSize = fm.getLength();

      // 2 - Image Format
      short imageFormat = ShortConverter.changeFormat(fm.readShort());

      // 2 - Bytes per Row
      short bytesPerRow = ShortConverter.changeFormat(fm.readShort());

      // 2 - Width
      short width = ShortConverter.changeFormat(fm.readShort());
      FieldValidator.checkWidth(width);

      // 2 - Height
      short height = ShortConverter.changeFormat(fm.readShort());
      FieldValidator.checkHeight(height);

      // 2 - X Offset
      // 2 - Y Offset
      fm.skip(4);

      int numPixels = width * height;
      int[] pixels = new int[numPixels];

      int outputPos = 0;
      for (int i = 0; i < height; i++) {
        //int rowByteCount = isLE ? _data->readUint16LE() : _data->readUint16BE();
        int rowByteCount = ShortConverter.unsign(ShortConverter.changeFormat(fm.readShort()));
        int startPos = outputPos;

        //byte *dst = (byte *)surface->pixels + i * _header.width;
        int remaining = width;
        while (remaining > 0) {
          byte code = fm.readByte();
          int runLen = (code & 0x7F) + 1;

          if (runLen > remaining) {
            runLen = remaining;
          }

          if ((code & 0x80) == 0x80) {
            byte val = fm.readByte();
            for (int p = 0; p < runLen; p++) {
              pixels[outputPos + p] = val;
            }
          }
          else {
            for (int p = 0; p < runLen; p++) {
              pixels[outputPos + p] = fm.readByte();
            }
          }

          outputPos += runLen;
          remaining -= runLen;
        }

        //_data->seek(startPos + rowByteCount);
      }

      fm.close();

      // TODO temporary - convert each pixel to a color value
      for (int i = 0; i < numPixels; i++) {
        int pixel = pixels[i];
        pixels[i] = 255 << 24 | pixel << 16 | pixel << 8 | pixel;
      }

      ImageResource imageResource = new ImageResource(pixels, width, height);
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