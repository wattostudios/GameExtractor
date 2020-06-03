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
import org.watto.ge.helper.ImageFormatReader;
import org.watto.ge.helper.ImageFormatWriter;
import org.watto.ge.helper.ImageManipulator;
import org.watto.ge.plugin.AllFilesPlugin;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.ge.plugin.archive.Plugin_PAK_27;
import org.watto.io.FileManipulator;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_PAK_27_DXT extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_PAK_27_DXT() {
    super("PAK_27_DXT", "Cars DXT Image");
    setExtensions("dxt");

    setGames("Cars");
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
      if (plugin instanceof Plugin_PAK_27) {
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

      // 4 - Unknown (2)
      if (fm.readInt() == 2) {
        rating += 4;
      }

      // 4 - Unknown (54 = DXT Compression, 55 = Paletted)
      int imageFormat = fm.readInt();
      if (imageFormat == 54 || imageFormat == 55) {
        rating += 5;
      }

      // 4 - Number of Colors (0 if not paletted, 256 if paletted)
      if (FieldValidator.checkRange(fm.readInt(), 0, 256)) {
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

      // 4 - Unknown (2)
      fm.skip(4);

      // 4 - Unknown (54 = DXT Compression, 55 = Paletted)
      fm.skip(4);

      // 4 - Number of Colors (0 if not paletted, 256 if paletted)
      int colorCount = fm.readInt();

      // X - Color Palette (if NumColors != 0){
      // RGBA format
      int[] palette = null;
      if (colorCount != 0) {
        FieldValidator.checkNumColors(colorCount);
        palette = ImageFormatReader.readPaletteRGBA(fm, colorCount);
      }

      // 4 - Number Of Mipmaps
      int mipmapCount = fm.readInt();

      // 4 - Image Width
      // 4 - Image Height
      fm.skip(8);

      // 4 - Image Width
      int width = fm.readInt();
      FieldValidator.checkWidth(width);

      // 4 - Image Height
      int height = fm.readInt();
      FieldValidator.checkHeight(height);

      // 4 - Data Length
      int length = fm.readInt();
      FieldValidator.checkLength(length, fm.getLength());

      // X - Pixels
      ImageResource imageResource = null;
      if (colorCount != 0) {
        imageResource = ImageFormatReader.read8BitPaletted(fm, width, height, palette);
        imageResource.setProperty("ImageFormat", "8BitPaletted");
      }
      else if ((width * height) == length) {
        imageResource = ImageFormatReader.readDXT3(fm, width, height);
        imageResource.setProperty("ImageFormat", "DXT3");
      }
      else {
        imageResource = ImageFormatReader.readDXT1(fm, width, height);
        imageResource.setProperty("ImageFormat", "DXT1");
      }

      imageResource.addProperty("MipmapCount", "" + mipmapCount);

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

      int mipmapCount = 1;

      // Set some property defaults in case we're doing a conversion (and thus there probably isn't any properties set)
      String imageFormat = "DXT3";

      // Now try to get the property values from the ImageResource, if they exist
      ImageResource imageResource = ((PreviewPanel_Image) preview).getImageResource();

      if (imageResource != null) {
        mipmapCount = imageResource.getProperty("MipmapCount", mipmapCount);
        imageFormat = imageResource.getProperty("ImageFormat", "DXT3");
      }

      if (!(imageFormat.equals("DXT1") || imageFormat.equals("DXT3") || imageFormat.equals("DXT5") || imageFormat.equals("8BitPaletted"))) {
        // a different image format not allowed in this image - change to DXT3
        imageFormat = "DXT3";
      }

      // Generate all the mipmaps of the image
      ImageResource[] mipmapsTrue = null;
      ImageManipulator[] mipmapsPaletted = null;
      if (imageFormat.equals("8BitPaletted")) {

        // Paletted, 256 colors
        im.convertToPaletted();
        im.changeColorCount(256);

        mipmapsPaletted = im.generatePalettedMipmaps();

        if (mipmapCount > mipmapsPaletted.length) {
          mipmapCount = mipmapsPaletted.length;
        }

      }
      else {
        mipmapsTrue = im.generateMipmaps();

        if (mipmapCount > mipmapsTrue.length) {
          mipmapCount = mipmapsTrue.length;
        }
      }

      // calculate the file length
      long fileLength = 36;
      if (imageFormat.equals("8BitPaletted")) {
        // Color Palette
        fileLength += im.getNumColors() * 4;
        // Pixels
        for (int i = 0; i < mipmapCount; i++) {
          ImageManipulator mipmap = mipmapsPaletted[i];
          fileLength += mipmap.getNumPixels();
        }
      }
      else {
        for (int i = 0; i < mipmapCount; i++) {
          ImageResource mipmap = mipmapsTrue[i];
          int calcWidth = mipmap.getWidth();
          int calcHeight = mipmap.getHeight();

          int length = calcWidth * calcHeight;
          if (imageFormat.equals("DXT1")) {
            length /= 2; // 0.5bytes per pixel for DXT1, 1byte per pixel for DXT3/5
          }
          fileLength += length;
        }
      }

      // 4 - Unknown (2)
      fm.writeInt(2);

      if (imageFormat.equals("8BitPaletted")) {
        // 4 - Unknown (54/55)
        fm.writeInt(55);

        int[] palette = im.getPalette();
        int numColors = im.getNumColors();

        // 4 - Number of Colors (0 if not paletted, 256 if paletted)
        fm.writeInt(numColors);

        // X - Color Palette (if NumColors != 0){
        // RGBA format
        ImageFormatWriter.writePaletteRGBA(fm, palette);
      }
      else {
        // 4 - Unknown (54/55)
        fm.writeInt(54);

        // 4 - Number of Colors (0 if not paletted, 256 if paletted)
        fm.writeInt(0);
      }

      // 4 - Number Of Mipmaps
      fm.writeInt(mipmapCount);

      // 4 - Image Width
      fm.writeInt(imageWidth);

      // 4 - Image Height
      fm.writeInt(imageHeight);

      // 4 - Image Width
      fm.writeInt(imageWidth);

      // 4 - Image Height
      fm.writeInt(imageHeight);

      // 4 - Data Length
      fm.writeInt(fileLength);

      // X - Mipmaps
      for (int i = 0; i < mipmapCount; i++) {
        if (imageFormat.equals("8BitPaletted")) {
          ImageManipulator mipmap = mipmapsPaletted[i];

          int[] indexes = mipmap.getPixels();
          int numIndexes = indexes.length;

          // X - Palette Index
          for (int p = 0; p < numIndexes; p++) {
            fm.writeByte(indexes[p]);
          }
        }
        else {

          ImageResource mipmap = mipmapsTrue[i];

          // X - Pixels
          if (imageFormat.equals("DXT1")) {
            ImageFormatWriter.writeDXT1(fm, mipmap);
          }
          else if (imageFormat.equals("DXT3")) {
            ImageFormatWriter.writeDXT3(fm, mipmap);
          }
          else if (imageFormat.equals("DXT5")) {
            ImageFormatWriter.writeDXT5(fm, mipmap);
          }
        }
      }

      fm.close();

    }
    catch (Throwable t) {
      logError(t);
    }
  }

}