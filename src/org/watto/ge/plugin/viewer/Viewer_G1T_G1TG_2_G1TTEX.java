/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2026 wattostudios
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
import org.watto.ge.plugin.archive.Plugin_G1T_G1TG_2;
import org.watto.io.FileManipulator;
import org.watto.io.buffer.ByteBuffer;
import org.watto.io.converter.ByteConverter;
import org.watto.io.converter.IntConverter;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_G1T_G1TG_2_G1TTEX extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_G1T_G1TG_2_G1TTEX() {
    super("G1T_G1TG_2_G1TTEX", "G1T_G1TG_2_G1TTEX");
    setExtensions("g1t_tex");

    setGames("Fist of the North Star: Ken's Rage");
    setPlatforms("XBox 360");
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
  public int getMatchRating(FileManipulator fm) {
    try {

      int rating = 0;

      ArchivePlugin plugin = Archive.getReadPlugin();
      if (plugin instanceof Plugin_G1T_G1TG_2) {
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

      fm.skip(8);

      if (IntConverter.changeFormat(fm.readInt()) == 12) {
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

      // 1 - Packed Info (4bits = Unknown, 4bits = Mipmap Count)
      fm.skip(1);

      // 1 - Image Format
      int imageFormat = fm.readByte();

      // 1 - Packed Dimensions (4bits = Width [2*], 4bits = Height [2*])
      int dimensions = ByteConverter.unsign(fm.readByte());

      int width = (int) Math.pow(2, (dimensions >> 4));
      FieldValidator.checkWidth(width);

      int height = (int) Math.pow(2, (dimensions & 15));
      FieldValidator.checkHeight(height);

      // 4 - Flags
      fm.skip(4);

      // 1 - Extra Data Version
      if (fm.readByte() != 0) {
        // 4 - Extra Data Length (including this field) (12)
        int extraLength = IntConverter.changeFormat(fm.readInt()) - 4;
        FieldValidator.checkLength(extraLength, arcSize);

        // 4 - Unknown
        // 4 - Unknown
        fm.skip(extraLength);
      }

      // X - Image Data
      // Format Ref = https://github.com/VitaSmith/gust_tools/blob/master/gust_g1t.c#L831
      ImageResource imageResource = null;
      if (imageFormat == 1) { // R8G8B8A8
        imageResource = ImageFormatReader.readRGBA(fm, width, height);
      }
      else if (imageFormat == 2) { // B8G8R8A8
        imageResource = ImageFormatReader.readBGRA(fm, width, height);
      }
      else if (imageFormat == 6) { // DXT1 with XBox360 Byte Swapping
        int numPixels = width * height / 2;
        byte[] pixelBytes = fm.readBytes(numPixels);
        pixelBytes = ImageFormatReader.byteSwapXbox360(pixelBytes);
        fm.close();
        fm = new FileManipulator(new ByteBuffer(pixelBytes));

        imageResource = ImageFormatReader.readDXT1(fm, width, height);
      }
      else if (imageFormat == 7) { // DXT3 with XBox360 Byte Swapping
        int numPixels = width * height;
        byte[] pixelBytes = fm.readBytes(numPixels);
        pixelBytes = ImageFormatReader.byteSwapXbox360(pixelBytes);
        fm.close();
        fm = new FileManipulator(new ByteBuffer(pixelBytes));

        imageResource = ImageFormatReader.readDXT3(fm, width, height);
      }
      else if (imageFormat == 8) { // DXT5 with XBox360 Byte Swapping
        int numPixels = width * height;
        byte[] pixelBytes = fm.readBytes(numPixels);
        pixelBytes = ImageFormatReader.byteSwapXbox360(pixelBytes);
        fm.close();
        fm = new FileManipulator(new ByteBuffer(pixelBytes));

        imageResource = ImageFormatReader.readDXT5(fm, width, height);
      }
      else if (imageFormat == 89) { // DXT1
        imageResource = ImageFormatReader.readDXT1(fm, width, height);
      }
      else if (imageFormat == 90) { // DXT3
        imageResource = ImageFormatReader.readDXT3(fm, width, height);
      }
      else if (imageFormat == 91) { // DXT5
        imageResource = ImageFormatReader.readDXT5(fm, width, height);
      }
      else if (imageFormat == 92) { // BC4
        imageResource = ImageFormatReader.readBC4(fm, width, height);
      }
      else if (imageFormat == 94) { // BC6H
        imageResource = ImageFormatReader.readBC6H(fm, width, height);
      }
      else if (imageFormat == 95) { // BC7
        imageResource = ImageFormatReader.readBC7(fm, width, height);
      }

      else {
        ErrorLogger.log("[Viewer_G1T_G1TG_2_G1TTEX] Unknown Image Format: " + imageFormat);
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