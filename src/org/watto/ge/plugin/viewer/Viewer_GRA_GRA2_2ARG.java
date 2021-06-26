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
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.helper.ImageFormatReader;
import org.watto.ge.helper.ImageFormatWriter;
import org.watto.ge.helper.ImageManipulator;
import org.watto.ge.plugin.AllFilesPlugin;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.ge.plugin.archive.Plugin_GRA;
import org.watto.io.FileManipulator;
import org.watto.io.buffer.ByteBuffer;
import org.watto.io.converter.ByteConverter;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_GRA_GRA2_2ARG extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_GRA_GRA2_2ARG() {
    super("GRA_GRA2_2ARG", "Scrabble 2 GRA1/2 Image");
    setExtensions("gra2", "gra1");

    setGames("Scrabble 2");
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
  public int getMatchRating(FileManipulator fm) {
    try {

      int rating = 0;

      ArchivePlugin plugin = Archive.getReadPlugin();
      if (plugin instanceof Plugin_GRA) {
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
      String header = fm.readString(4);
      if (header.equals("2ARG") || header.equals("1ARG")) {
        rating += 50;
      }
      else {
        rating = 0;
      }

      // 2 - Image Width
      if (FieldValidator.checkWidth(fm.readShort())) {
        rating += 5;
      }

      // 2 - Image Height
      if (FieldValidator.checkHeight(fm.readShort())) {
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
  Extracts a PALT resource and then gets the Palette from it
  **********************************************************************************************
  **/
  public int[] extractPalette(Resource paltResource) {
    try {
      ByteBuffer buffer = new ByteBuffer((int) paltResource.getLength());
      FileManipulator fm = new FileManipulator(buffer);
      paltResource.extract(fm);

      fm.seek(0); // back to the beginning of the byte array

      int[] palette = ImageFormatReader.readPaletteBGRA(fm, 256);

      fm.close();

      return palette;
    }
    catch (Throwable t) {
      logError(t);
      return new int[0];
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

      // 4 - Header (2ARG)
      String header = fm.readString(4);

      // 2 - Image Width
      short width = fm.readShort();
      FieldValidator.checkWidth(width);

      // 2 - Image Height
      short height = fm.readShort();
      FieldValidator.checkHeight(height);

      ImageResource imageResource = null;

      if (header.equals("1ARG")) {
        // X - Unknown
        fm.skip(32);

        // X - Pixels (RGB)
        imageResource = ImageFormatReader.readRGB(fm, width, height);
      }
      else {

        // 4 - null
        // 4 - Unknown
        // 4 - Unknown
        // 4 - Unknown
        fm.skip(16);

        // 4 - Unknown (31)
        int imageFormat = fm.readInt();

        // 4 - null
        // 4 - null
        fm.skip(8);

        // 4 - Palette Number (-1 for no palette)
        int paletteID = fm.readInt();

        if (paletteID != -1) {
          // paletted

          int numPixels = width * height;
          byte[] pixelIndexes = fm.readBytes(numPixels);

          // get the Palette
          int[] palette = new int[0];
          Resource[] resources = Archive.getResources();
          int numResources = resources.length;
          if (resources[paletteID].getExtension().equalsIgnoreCase("PAL1")) {
            // found the correct palette file
            palette = extractPalette(resources[paletteID]);
          }
          else {
            // try to find any palette

            for (int i = 0; i < numResources; i++) {
              Resource currentResource = (Resource) resources[i];
              if (currentResource.getExtension().equalsIgnoreCase("PAL1")) {
                // found the color palette file - need to extract it and read the colors
                palette = extractPalette(resources[i]);
                break;
              }
            }
          }

          int numColors = palette.length;
          if (numColors <= 0) {
            ErrorLogger.log("[VIEWER_GRA_GRA2_2ARG] Invalid number of colors: " + numColors);
            return null;
          }

          // X - Pixels
          //imageResource = ImageFormatReader.read8BitPaletted(fm, width, height, palette);
          int[] pixels = new int[numPixels];
          for (int i = 0; i < numPixels; i++) {
            pixels[i] = palette[ByteConverter.unsign(pixelIndexes[i])];
          }
          imageResource = new ImageResource(pixels, width, height);

        }
        else {
          if (imageFormat == 255 || fm.getLength() == ((width * height * 3) + 40)) {
            // X - Pixels (RGB)
            imageResource = ImageFormatReader.readRGB(fm, width, height);
          }
          else {
            // X - Pixels (RGBA5551)
            imageResource = ImageFormatReader.readRGBA5551(fm, width, height);
          }
        }
      }

      fm.close();

      //ColorConverter.convertToPaletted(resource);

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

      int imageWidth = im.getWidth();
      int imageHeight = im.getHeight();

      if (imageWidth == -1 || imageHeight == -1) {
        return;
      }

      // Generate all the mipmaps of the image
      ImageResource[] mipmaps = im.generateMipmaps();
      int mipmapCount = mipmaps.length;

      // Set some property defaults in case we're doing a conversion (and thus there probably isn't any properties set)
      int fileID = 0;
      int hash = 0;
      String filename = "";

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
        // ABGR is 4 bytes per pixel
        int byteCount = mipmaps[i].getNumPixels() * 4;
        fileLength += byteCount;
      }

      // 4 - Header (ABGR)
      fm.writeString("ABGR");

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

      // 4 - File Type? (28)
      fm.writeInt(28);

      // 4 - Hash?
      fm.writeInt(hash);

      // X - Filename
      // 1 - null Filename Terminator
      fm.writeString(filename);
      fm.writeByte(0);

      // X - Mipmaps
      for (int i = 0; i < mipmapCount; i++) {
        ImageResource mipmap = mipmaps[i];

        int pixelCount = mipmap.getNumPixels();

        // 4 - Data Length
        fm.writeInt(pixelCount * 4); // ABGR is 4 bytes per pixel

        // X - Pixels
        ImageFormatWriter.writeBGRA(fm, mipmap);
      }

      fm.close();

    }
    catch (Throwable t) {
      logError(t);
    }
  }

}