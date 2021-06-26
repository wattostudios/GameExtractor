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
import org.watto.ge.plugin.archive.Plugin_CAM_CYLBPC_3;
import org.watto.ge.plugin.archive.Plugin_CAM_CYLBPC_4;
import org.watto.io.FileManipulator;
import org.watto.io.converter.ByteConverter;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_CAM_CYLBPC_3_4_PICT extends ViewerPlugin {

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Viewer_CAM_CYLBPC_3_4_PICT() {
    super("CAM_CYLBPC_3_4_PICT", "Playboy: The Mansion PICT Images");
    setExtensions("pict");

    setGames("Playboy: The Mansion");
    setPlatforms("PC");
    setStandardFileFormat(false);

    setEnabled(false); // Doesn't currently work, PICT has lots of different formats that need more analyzing

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
      if (plugin instanceof Plugin_CAM_CYLBPC_3 || plugin instanceof Plugin_CAM_CYLBPC_4) {
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

      // 4 - Version (1)
      fm.skip(4);

      // 2 - Number Of Colors (256)
      int numColors = fm.readShort();

      // 4 - Unknown (1)
      // 4 - Unknown (8193)
      // 2 - null
      fm.skip(10);

      // 2 - Width (512)
      int imageWidth = fm.readShort();

      // 2 - Height (256)
      int imageHeight = fm.readShort();

      // 2 - Bit Depth (8) ie paletted 256 color
      fm.skip(2);

      int[] colors = new int[numColors];

      // read the color palette
      for (int i = 0; i < numColors; i++) {
        // 1 - Red
        int r = ByteConverter.unsign(fm.readByte());

        // 1 - Green
        int g = ByteConverter.unsign(fm.readByte());

        // 1 - Blue
        int b = ByteConverter.unsign(fm.readByte());

        // 1 - Alpha
        int a = ByteConverter.unsign(fm.readByte());

        colors[i] = ((a << 24) | (r) | (g << 8) | (b << 16));
      }

      int[] data = new int[imageWidth * imageHeight];
      int numPixels = data.length - 1;
      for (int i = numPixels; i >= 0; i--) {
        // 1 - Palette Color Index
        int palIndex = ByteConverter.unsign(fm.readByte());

        data[i] = colors[palIndex];
      }

      return new ImageResource(data, imageWidth, imageHeight);

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
      im.convertToPaletted(); // This format is a paletted image
      im.changeColorCount(256);// Reduce to 256 colors, if necessary

      int imageWidth = im.getWidth();
      int imageHeight = im.getHeight();

      if (imageWidth == -1 || imageHeight == -1) {
        return;
      }

      // 4 - Version (1)
      fm.writeInt(1);

      // 2 - Number Of Colors (256)
      fm.writeShort((short) 256);

      // 4 - Unknown (1)
      fm.writeInt(1);

      // 4 - Unknown (8193)
      fm.writeInt(8193);

      // 2 - null
      fm.writeShort((short) 0);

      // 2 - Width (512)
      fm.writeShort((short) imageWidth);

      // 2 - Height (256)
      fm.writeShort((short) imageHeight);

      // 2 - Bit Depth (8) ie paletted 256 color
      fm.writeShort((short) 8);

      int numColors = 256;
      int[] palette = im.getPalette();

      for (int i = 0; i < numColors; i++) {
        int color = palette[i];

        // 1 - Red
        // 1 - Green
        // 1 - Blue
        // 1 - Alpha
        fm.writeByte((byte) color);
        fm.writeByte((byte) color >> 8);
        fm.writeByte((byte) color >> 16);
        fm.writeByte((byte) color >> 24);
      }

      // X - Pixels
      int[] pixels = im.getPixels();
      int numPixels = pixels.length;

      for (int i = numPixels - 1; i >= 0; i--) {
        // 1 - Color Palette Index
        fm.writeByte((byte) pixels[i]);
      }

      fm.close();

    }
    catch (Throwable t) {
      logError(t);
    }
  }

}