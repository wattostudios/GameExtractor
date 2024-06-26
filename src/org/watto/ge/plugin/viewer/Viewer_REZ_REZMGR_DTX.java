/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2022 wattostudios
 *
 * License Information:
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License
 * published by the Free Software Foundation; either version 2 of the License, or (at your option) any later versions. This
 * program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranties
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License at http://www.gnu.org for more
 * details. For further information on this application, refer to the authors' website.
 */

package org.watto.ge.plugin.viewer;

import java.awt.Image;
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
import org.watto.ge.helper.PaletteGenerator;
import org.watto.ge.plugin.AllFilesPlugin;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.ge.plugin.archive.Plugin_REZ_REZMGR;
import org.watto.io.FileManipulator;
import org.watto.io.buffer.ByteBuffer;
import org.watto.io.converter.ByteConverter;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_REZ_REZMGR_DTX extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_REZ_REZMGR_DTX() {
    super("REZ_REZMGR_DTX", "REZ_REZMGR_DTX");
    setExtensions("dtx");

    setGames("Alien Vs Predator 2",
        "Blood 2",
        "KISS Psycho Circus: The Nightmare Child",
        "Schizm 2",
        "Mysterious Journey 2");
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
      if (plugin instanceof Plugin_REZ_REZMGR) {
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

      // 4 - null
      // 4 - Unknown (-5)
      fm.skip(8);

      // 2 - Image Width
      if (FieldValidator.checkWidth(fm.readShort() + 1)) { // +1 to allow "grayscale" images which have width/height in a different place
        rating += 5;
      }

      // 2 - Image Height
      if (FieldValidator.checkHeight(fm.readShort() + 1)) { // +1 to allow "grayscale" images which have width/height in a different place
        rating += 5;
      }

      // 2 - Number of Mipmaps
      if (FieldValidator.checkRange(fm.readShort(), 0, 20)) {
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

      String imageFormat = "DXT5";
      short numMipmaps = 4;

      // 4 - null
      fm.skip(4);

      // 4 - Unknown (-5)
      int imageVersion = fm.readInt();

      // 2 - Image Width
      short width = fm.readShort();

      // 2 - Image Height
      short height = fm.readShort();

      if (width == 0 && height == 0) {
        imageFormat = "Grayscale";

        // 6 - null
        fm.skip(6);

        // 2 - Image Width
        width = fm.readShort();

        // 2 - Image Height
        height = fm.readShort();
      }
      else {
        // 2 - Number of Mipmaps
        numMipmaps = fm.readShort();
        FieldValidator.checkRange(numMipmaps, 0, 20);

        // 2 - null
        fm.skip(2);

        // 4 - Unknown
        // 4 - Unknown
        // 2 - Unknown
        fm.skip(10);

        // 1 - Image Format (3=RGBA, 4=DXT1, 6=DXT5)
        int imageType = ByteConverter.unsign(fm.readByte());
        if (imageType == 0) {
          if (imageVersion == -5) {
            imageFormat = "RGBA";
          }
          else if (imageVersion == -2 || imageVersion == -3) {
            imageFormat = "8bitPaletted";
          }
        }
        else if (imageType == 3) {
          imageFormat = "RGBA";
        }
        else if (imageType == 4) {
          imageFormat = "DXT1";
        }
        else if (imageType == 5) {
          imageFormat = "DXT3";
        }
        else if (imageType == 6) {
          imageFormat = "DXT3"; // think it's actually DXT3 (some as DXT5 are just all blank)
        }
        else if (imageType == 136 || imageType == 236) {
          imageFormat = "8bitPaletted";
        }
        else {
          ErrorLogger.log("REZ_REZMGR_DTX: Unknown Image Type: " + imageType);
          return null;
        }
      }

      FieldValidator.checkWidth(width);
      FieldValidator.checkHeight(height);

      ImageResource imageResource = null;

      if (imageFormat.equals("8bitPaletted")) {
        if (imageVersion == -2) {
          // 1 - Unknown
          // 8 - null
          // 4 - Unknown
          // 4 - Unknown
          fm.skip(17);
        }
        else if (imageVersion == -3) {
          // 1 - Unknown
          // 4 - Unknown
          // 4 - null
          // 128 - null Padding
          // 4 - Unknown
          // 4 - Unknown
          fm.seek(172);
        }

        // X - Color Palette (ARGB)
        int numColors = 256;
        int[] palette = new int[numColors];

        for (int i = 0; i < numColors; i++) {
          // 1 - Alpha
          // 1 - Red
          // 1 - Green
          // 1 - Blue
          int a = ByteConverter.unsign(fm.readByte());
          int r = ByteConverter.unsign(fm.readByte());
          int g = ByteConverter.unsign(fm.readByte());
          int b = ByteConverter.unsign(fm.readByte());
          palette[i] = (a << 24 | r << 16 | g << 8 | b);
        }

        // X - Color Palette Indexes
        int numPixels = width * height;
        int[] pixels = new int[numPixels];

        for (int i = 0; i < numPixels; i++) {
          pixels[i] = palette[ByteConverter.unsign(fm.readByte())];
        }
        imageResource = new ImageResource(pixels, width, height);

      }
      else if (imageFormat.equals("Grayscale")) {
        int[] palette = PaletteGenerator.getGrayscale();

        // X - Color Palette Indexes
        int numPixels = width * height;
        int[] pixels = new int[numPixels];

        for (int i = 0; i < numPixels; i++) {
          pixels[i] = palette[ByteConverter.unsign(fm.readByte())];
        }

        imageResource = new ImageResource(pixels, width, height);
      }
      else {
        // 1 - Unknown
        // 8 - null
        // 128 - Description, Padding, other Junk Data
        fm.seek(164);

        // X - Pixels
        if (imageFormat.equals("DXT5")) {
          imageResource = ImageFormatReader.readDXT5(fm, width, height);
        }
        else if (imageFormat.equals("RGBA")) {
          imageResource = ImageFormatReader.readRGBA(fm, width, height);
          imageResource = ImageFormatReader.removeAlphaIfAllInvisible(imageResource);
        }
        else if (imageFormat.equals("DXT1")) {
          imageResource = ImageFormatReader.readDXT1(fm, width, height);
        }
        else if (imageFormat.equals("DXT3")) {
          imageResource = ImageFormatReader.readDXT3(fm, width, height);
        }
      }

      fm.close();

      if (imageResource == null) {
        return null;
      }

      imageResource.addProperty("MipmapCount", "" + numMipmaps);
      imageResource.addProperty("ImageFormat", "" + imageFormat);

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

  /**
  **********************************************************************************************
  We can't WRITE these files from scratch, but we can REPLACE some of the images with new content  
  **********************************************************************************************
  **/
  public void replace(Resource resourceBeingReplaced, PreviewPanel preview, FileManipulator fm) {
    try {

      if (!(preview instanceof PreviewPanel_Image)) {
        return;
      }

      PreviewPanel_Image ivp = (PreviewPanel_Image) preview;
      Image image = ivp.getImage();
      int width = ivp.getImageWidth();
      int height = ivp.getImageHeight();

      if (width == -1 || height == -1) {
        return;
      }

      // Try to get the existing ImageResource (if it was stored), otherwise build a new one
      ImageResource imageResource = ((PreviewPanel_Image) preview).getImageResource();
      if (imageResource == null) {
        imageResource = new ImageResource(image, width, height);
      }

      // Extract the original resource into a byte[] array, so we can reference it
      int srcLength = (int) resourceBeingReplaced.getDecompressedLength();
      if (srcLength > 1280) {
        srcLength = 1280; // allows enough reading for the header and color palette, but not much of the original image data
      }
      byte[] srcBytes = new byte[(int) resourceBeingReplaced.getDecompressedLength()];
      FileManipulator src = new FileManipulator(new ByteBuffer(srcBytes));
      resourceBeingReplaced.extract(src);
      src.seek(0);

      // Build the new file using the src[] and adding in the new image content

      // 4 - null
      fm.writeBytes(src.readBytes(4));

      // 4 - Unknown (-5)
      int imageVersion = src.readInt();
      fm.writeInt(imageVersion);

      // 2 - Image Width
      short srcWidth = src.readShort();

      // 2 - Image Height
      short srcHeight = src.readShort();

      String imageFormat = null;
      int numMipmaps = 1;

      if (srcWidth == 0 && srcHeight == 0) {
        fm.writeShort(0);
        fm.writeShort(0);

        imageFormat = "Grayscale";

        // 6 - null
        fm.writeBytes(src.readBytes(6));

        // 2 - Image Width
        srcWidth = src.readShort();
        fm.writeShort(width);

        // 2 - Image Height
        srcHeight = src.readShort();
        fm.writeShort(height);
      }
      else {
        fm.writeShort(width);
        fm.writeShort(height);

        // 2 - Number of Mipmaps
        numMipmaps = src.readShort();
        fm.writeShort(numMipmaps);

        // 2 - null
        // 4 - Unknown
        // 4 - Unknown
        // 2 - Unknown
        fm.writeBytes(src.readBytes(12));

        // 1 - Image Format (3=RGBA, 4=DXT1, 6=DXT5)
        int imageType = ByteConverter.unsign(src.readByte());
        fm.writeByte(imageType);
        if (imageType == 0) {
          if (imageVersion == -5) {
            imageFormat = "RGBA";
          }
          else if (imageVersion == -2 || imageVersion == -3) {
            imageFormat = "8bitPaletted";
          }
        }
        else if (imageType == 3) {
          imageFormat = "RGBA";
        }
        else if (imageType == 4) {
          imageFormat = "DXT1";
        }
        else if (imageType == 5) {
          imageFormat = "DXT3";
        }
        else if (imageType == 6) {
          imageFormat = "DXT3"; // think it's actually DXT3 (some as DXT5 are just all blank)
        }
        else if (imageType == 136 || imageType == 236) {
          imageFormat = "8bitPaletted";
        }
        else {
          ErrorLogger.log("REZ_REZMGR_DTX: Unknown Image Type: " + imageType);
          return;
        }
      }

      if (imageFormat.equals("8bitPaletted")) {
        if (imageVersion == -2) {
          // 1 - Unknown
          // 8 - null
          // 4 - Unknown
          // 4 - Unknown
          fm.writeBytes(src.readBytes(17));
        }
        else if (imageVersion == -3) {
          // 1 - Unknown
          // 4 - Unknown
          // 4 - null
          // 128 - null Padding
          // 4 - Unknown
          // 4 - Unknown
          fm.writeBytes(src.readBytes(172 - (int) fm.getLength()));
        }

        // X - Color Palette (ARGB)
        ImageManipulator im = new ImageManipulator(imageResource);
        im.changeColorCount(256);

        int[] palette = im.getPalette();
        ImageFormatWriter.writePaletteARGB(fm, palette);

        // X - Color Palette Indexes
        ImageManipulator[] mipmaps = im.generatePalettedMipmaps();
        for (int m = 0; m < numMipmaps; m++) {
          int[] pixels = mipmaps[m].getPixels();
          int numPixels = pixels.length;

          for (int i = 0; i < numPixels; i++) {
            fm.writeByte(pixels[i]);
          }
        }

      }
      else if (imageFormat.equals("Grayscale")) {
        ImageManipulator im = new ImageManipulator(imageResource);
        im.changeColorCount(256);

        // change to grayscale palette
        int[] palette = PaletteGenerator.getGrayscale();
        im.setPalette(palette);

        // X - Color Palette Indexes
        ImageManipulator[] mipmaps = im.generatePalettedMipmaps();
        for (int m = 0; m < numMipmaps; m++) {
          int[] pixels = mipmaps[m].getPixels();
          int numPixels = pixels.length;

          for (int i = 0; i < numPixels; i++) {
            fm.writeByte(pixels[i]);
          }
        }

      }
      else {
        // 1 - Unknown
        // 8 - null
        // 128 - Description, Padding, other Junk Data
        fm.writeBytes(src.readBytes(164 - (int) fm.getLength()));

        // X - Pixels
        ImageManipulator im = new ImageManipulator(imageResource);
        ImageResource[] mipmaps = im.generateMipmaps();

        for (int m = 0; m < numMipmaps; m++) {
          if (imageFormat.equals("DXT5")) {
            ImageFormatWriter.writeDXT5(fm, mipmaps[m]);
          }
          else if (imageFormat.equals("RGBA")) {
            ImageFormatWriter.writeRGBA(fm, mipmaps[m]);
          }
          else if (imageFormat.equals("DXT1")) {
            ImageFormatWriter.writeDXT1(fm, mipmaps[m]);
          }
          else if (imageFormat.equals("DXT3")) {
            ImageFormatWriter.writeDXT3(fm, mipmaps[m]);
          }
        }
      }

      src.close();
      fm.close();

    }
    catch (Throwable t) {
      logError(t);
    }
  }

}