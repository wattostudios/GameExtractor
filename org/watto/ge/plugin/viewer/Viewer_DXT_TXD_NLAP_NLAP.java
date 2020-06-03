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
import org.watto.ge.plugin.archive.Plugin_DXT_TXD;
import org.watto.ge.plugin.archive.Plugin_PAL_LAP;
import org.watto.io.FileManipulator;
import org.watto.io.converter.ByteConverter;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_DXT_TXD_NLAP_NLAP extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_DXT_TXD_NLAP_NLAP() {
    super("DXT_TXD_NLAP_NLAP", "Hitman: Codename 47 NLAP Image");
    setExtensions("nlap");

    setGames("Hitman: Codename 47");
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
      if (plugin instanceof Plugin_DXT_TXD || plugin instanceof Plugin_PAL_LAP) {
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
      if (fm.readString(4).equals("NLAP")) {
        rating += 50;
      }
      else {
        rating = 0;
      }

      // 4 - File Length
      if (fm.readInt() == fm.getLength()) {
        rating += 5;
      }

      fm.skip(4);

      // 2 - Image Height
      if (FieldValidator.checkHeight(fm.readShort())) {
        rating += 5;
      }

      // 2 - Image Width
      if (FieldValidator.checkWidth(fm.readShort())) {
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

      // 4 - Header (NLAP)
      fm.skip(4);

      // 4 - File Length (including all these header fields)
      FieldValidator.checkLength(fm.readInt(), arcSize);

      // 4 - File ID
      int fileID = fm.readInt();

      // 2 - Image Height
      short height = fm.readShort();
      FieldValidator.checkHeight(height);

      // 2 - Image Width
      short width = fm.readShort();
      FieldValidator.checkWidth(width);

      // 4 - Number Of Mipmaps
      int numMipmaps = fm.readInt();

      // 4 - File Type (20)
      fm.skip(4);

      // 4 - Hash?
      int hash = fm.readInt();

      // X - Filename
      // 1 - null Filename Terminator
      String filename = fm.readNullString();

      // 4 - Data Length
      FieldValidator.checkLength(fm.readInt(), arcSize);

      // X - Pixels
      int numPixels = width * height;
      int[] pixels = new int[numPixels];
      for (int p = 0; p < numPixels; p++) {
        // 1 - Color Palette Index
        pixels[p] = ByteConverter.unsign(fm.readByte());
      }

      // skip the other mipmaps
      for (int i = 1; i < numMipmaps; i++) {
        // 4 - Data Length
        int length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // X - Pixels
        fm.skip(length);
      }

      // 4 - Number Of Colors
      int numColors = fm.readInt();
      FieldValidator.checkNumColors(numColors);

      // X - Palette
      int[] palette = new int[numColors];
      for (int i = 0; i < numColors; i++) {
        // 1 - Red
        // 1 - Green
        // 1 - Blue
        // 1 - Alpha
        int r = ByteConverter.unsign(fm.readByte());
        int g = ByteConverter.unsign(fm.readByte());
        int b = ByteConverter.unsign(fm.readByte());
        int a = ByteConverter.unsign(fm.readByte());

        palette[i] = ((a << 24) | (r << 16) | (g << 8) | (b));
      }

      // go through and change the pixels to palette values
      for (int i = 0; i < numPixels; i++) {
        pixels[i] = palette[pixels[i]];
      }

      fm.close();

      ImageResource imageResource = new ImageResource(pixels, width, height);

      imageResource.addProperty("MipmapCount", "" + numMipmaps);
      imageResource.addProperty("Hash", "" + hash);
      imageResource.addProperty("FileID", "" + fileID);
      imageResource.addProperty("Filename", filename);
      imageResource.addProperty("ColorCount", "" + numColors);

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

      // Paletted, 256 Colors
      im.convertToPaletted();
      im.changeColorCount(256);

      int imageWidth = im.getWidth();
      int imageHeight = im.getHeight();

      if (imageWidth == -1 || imageHeight == -1) {
        return;
      }

      // Generate all the PALETTED mipmaps of the image
      ImageManipulator[] mipmaps = im.generatePalettedMipmaps();
      int mipmapCount = mipmaps.length;

      // Set some property defaults in case we're doing a conversion (and thus there probably isn't any properties set)
      int fileID = 0;
      int hash = 0;
      String filename = "";
      int colorCount = 256;

      // Now try to get the property values from the ImageResource, if they exist
      ImageResource imageResource = ((PreviewPanel_Image) preview).getImageResource();

      if (imageResource != null) {
        mipmapCount = imageResource.getProperty("MipmapCount", mipmapCount);
        fileID = imageResource.getProperty("FileID", 0);
        hash = imageResource.getProperty("Hash", 0);
        filename = imageResource.getProperty("Filename", "");
      }

      if (filename.equals("")) {
        filename = fm.getFile().getName();
      }
      if (mipmapCount > mipmaps.length) {
        mipmapCount = mipmaps.length;
      }

      // work out the file length
      long fileLength = 28 + filename.length() + 1 + (mipmapCount * 4);
      for (int i = 0; i < mipmapCount; i++) {
        // Paletted is 1 byte per pixel
        int byteCount = mipmaps[i].getNumPixels();
        fileLength += byteCount;
      }
      // add on the palette
      fileLength += 4 + (colorCount * 4);

      // 4 - Header (NLAP)
      fm.writeString("NLAP");

      // 4 - File Length (including all these header fields)
      fm.writeInt(fileLength);

      // 4 - File ID
      fm.writeInt(fileID);

      // 2 - Image Height
      fm.writeShort((short) imageHeight);

      // 2 - Image Width
      fm.writeShort((short) imageWidth);

      // 4 - Number Of Mipmaps
      fm.writeInt(mipmapCount);

      // 4 - File Type? (20)
      fm.writeInt(20);

      // 4 - Hash?
      fm.writeInt(hash);

      // X - Filename
      // 1 - null Filename Terminator
      fm.writeString(filename);
      fm.writeByte(0);

      // X - Mipmaps
      for (int i = 0; i < mipmapCount; i++) {
        ImageManipulator mipmap = mipmaps[i];

        int[] indexes = mipmap.getPixels();
        int pixelCount = mipmap.getNumPixels();

        // 4 - Data Length
        fm.writeInt(pixelCount); // Paletted is 1 byte per pixel

        // X - Pixels
        for (int p = 0; p < pixelCount; p++) {
          fm.writeByte(indexes[p]);
        }
      }

      int[] palette = im.getPalette();
      int numColors = palette.length;

      // 4 - Number Of Colors
      fm.writeInt(numColors);

      // X - Palette
      for (int i = 0; i < numColors; i++) {
        int color = palette[i];

        // 1 - Red
        // 1 - Green
        // 1 - Blue
        // 1 - Alpha
        fm.writeByte(color >> 16);
        fm.writeByte(color >> 8);
        fm.writeByte(color);
        fm.writeByte(color >> 24);
      }

      fm.close();

    }
    catch (Throwable t) {
      logError(t);
    }
  }

}