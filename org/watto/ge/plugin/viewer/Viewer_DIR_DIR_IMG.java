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
import org.watto.ge.helper.ImageManipulator;
import org.watto.ge.plugin.AllFilesPlugin;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.ge.plugin.archive.Plugin_DIR_DIR;
import org.watto.io.FileManipulator;
import org.watto.io.converter.ByteConverter;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_DIR_DIR_IMG extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_DIR_DIR_IMG() {
    super("DIR_DIR_IMG", "Worms 2 IMG Image");
    setExtensions("img");

    setGames("Worms 2");
    setPlatforms("PC");
    setStandardFileFormat(false);
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public boolean canWrite(PreviewPanel panel) {
    if (panel instanceof PreviewPanel_Image) {
      return true;
    }
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
      if (plugin instanceof Plugin_DIR_DIR) {
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

      // 4 - Header
      String headerString = fm.readString(3);
      int headerByte = fm.readByte();

      if (headerString.equals("IMG") && headerByte == 26) {
        rating += 25;
      }
      else {
        rating = 0;
      }

      // 4 - File Size
      if (FieldValidator.checkEquals(fm.readInt(), fm.getLength())) {
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

      // 4 - Header (IMG + (byte)26)
      // 4 - File Size
      fm.skip(8);

      // X - Filename
      // 1 - null Filename Terminator
      String filename = fm.readNullString();

      int[] palette = new int[81];
      for (int i = 0; i < 81; i++) {
        // 1 - Blue
        int b = ByteConverter.unsign(fm.readByte());

        // 1 - Red
        int r = ByteConverter.unsign(fm.readByte());

        // 1 - Green
        int g = ByteConverter.unsign(fm.readByte());

        palette[i] = ((r << 16) | (g << 8) | (b) | 0xFF000000);
      }

      fm.skip(1);

      // 2 - Image Width/Height
      int imageWidth = fm.readShort();

      // 2 - Image Width/Height
      int imageHeight = fm.readShort();

      int[] data = new int[imageWidth * imageHeight];
      int numPixels = data.length;

      // X - Pixels
      for (int i = 0; i < numPixels; i++) {
        // 1 - Color Palette Index
        int colorIndex = ByteConverter.unsign(fm.readByte());

        data[i] = palette[colorIndex];
      }

      ImageResource imageResource = new ImageResource(data, imageWidth, imageHeight);

      imageResource.addProperty("Filename", filename);

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
    try {

      if (!(preview instanceof PreviewPanel_Image)) {
        return;
      }

      ImageManipulator im = new ImageManipulator((PreviewPanel_Image) preview);

      // Paletted image, 81 colors
      im.convertToPaletted();
      im.changeColorCount(81);

      int imageWidth = im.getWidth();
      int imageHeight = im.getHeight();

      if (imageWidth == -1 || imageHeight == -1) {
        return;
      }

      // Set some property defaults in case we're doing a conversion (and thus there probably isn't any properties set)
      String filename = "";

      // Now try to get the property values from the ImageResource, if they exist
      ImageResource imageResource = ((PreviewPanel_Image) preview).getImageResource();

      if (imageResource != null) {
        filename = imageResource.getProperty("Filename", "");
      }

      if (filename.equals("")) {
        filename = fm.getFile().getName();
      }

      // work out the file length
      long fileLength = 256 + filename.length() + 1 + (imageWidth * imageHeight);

      // 4 - Header (IMG + (byte)26)
      fm.writeString("IMG");
      fm.writeByte((byte) 26);

      // 4 - File Length
      fm.writeInt(fileLength);

      // X - Filename
      // 1 - null Filename Terminator
      fm.writeNullString(filename);

      // X - Palette
      int numColors = 81;
      int[] palette = im.getPalette();

      for (int i = 0; i < numColors; i++) {
        int color = palette[i];

        // 1 - Blue
        // 1 - Red
        // 1 - Green
        fm.writeByte((byte) (color));
        fm.writeByte((byte) (color >> 16));
        fm.writeByte((byte) (color >> 8));
      }

      // 1 - Unknown
      fm.writeByte(0);

      // 2 - Image Width/Height
      fm.writeShort((short) imageWidth);

      // 2 - Image Width/Height
      fm.writeShort((short) imageHeight);

      // X - Pixels
      int[] pixels = im.getPixels();
      int numPixels = pixels.length;

      for (int p = 0; p < numPixels; p++) {
        // 1 - Color Palette Index
        fm.writeByte((byte) pixels[p]);
      }

      fm.close();

    }
    catch (Throwable t) {
      logError(t);
    }
  }

}