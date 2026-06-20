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
import org.watto.SingletonManager;
import org.watto.TemporarySettings;
import org.watto.component.PreviewPanel;
import org.watto.component.PreviewPanel_Image;
import org.watto.datatype.Archive;
import org.watto.datatype.ImageResource;
import org.watto.datatype.Palette;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.helper.ImageFormatReader;
import org.watto.ge.helper.ImageSwizzler;
import org.watto.ge.helper.PaletteManager;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.ge.plugin.archive.Plugin_LZ;
import org.watto.io.FileManipulator;
import org.watto.io.buffer.ByteBuffer;
import org.watto.io.converter.ShortConverter;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_NCGR_RGCN extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_NCGR_RGCN() {
    super("NCGR_RGCN", "Nintendo DS NCGR Image");
    setExtensions("ncgr", "rgcn");

    setGames("Nintendo DS",
        "Custom Robo Arena",
        "Wizards of Waverly Place");
    setPlatforms("NDS");
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
      if (plugin instanceof Plugin_LZ) {
        rating += 20;
      }

      if (FieldValidator.checkExtension(fm, extensions)) {
        rating += 25;
      }
      else {
        return 0;
      }

      // 4 - Header (RGCN)
      if (fm.readString(4).equals("RGCN")) {
        rating += 50;
      }
      else {
        return 0;
      }

      fm.skip(4);

      // 4 - Archive Length
      if (fm.readInt() == fm.getLength()) {
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
  Extracts a PALT resource and then gets the Palette from it
  **********************************************************************************************
  **/
  public void extractAndAddPalette(Resource paletteResource) {
    try {
      int paletteLength = (int) paletteResource.getDecompressedLength();

      ByteBuffer buffer = new ByteBuffer(paletteLength);
      FileManipulator fm = new FileManipulator(buffer);
      paletteResource.extract(fm);

      fm.seek(0); // back to the beginning of the byte array

      // 4 - Header (RLCN)
      // 2 - Byte Order
      // 2 - Version
      // 4 - Archive Length
      // 2 - TTLP Offset
      // 2 - Number of Blocks (1)
      // 4 - Header (TTLP)
      // 4 - Block Length
      fm.skip(24);

      // 4 - Color Depth (3=4bpp, 4=8bpp)
      int imageFormat = fm.readInt();

      // 4 - null
      fm.skip(4);

      // 4 - Palette Data Length
      int dataLength = fm.readInt();
      FieldValidator.checkLength(dataLength, paletteLength);

      // 4 - Palette Data Offset (relative to the start of the TTLP Block)
      fm.skip(4);

      int numColors = 256;
      if (imageFormat == 3) {
        numColors = 16;
      }
      else if (imageFormat == 4) {
        // 256
      }
      else {
        return;
      }

      int paletteSize = numColors * 2; // each color is 16bit BGR555
      int numPalettes = dataLength / paletteSize;

      for (int p = 0; p < numPalettes; p++) {

        // X Bytes - Pixel Data
        int[] palette = new int[256]; // force 256 colors, even if we only read 16

        int firstColor = -1;
        int numColorsFound = 0;
        for (int i = 0; i < numColors; i++) {
          int pixel = ShortConverter.unsign(fm.readShort());

          int bPixel = ((pixel >> 10) & 31) * 8;
          int gPixel = ((pixel >> 5) & 31) * 8;
          int rPixel = (pixel & 31) * 8;
          int aPixel = 255;

          // OUTPUT = ARGB
          int color = ((rPixel << 16) | (gPixel << 8) | bPixel | (aPixel << 24));
          palette[i] = color;

          if (color != firstColor) {
            numColorsFound++;
            firstColor = color;
          }
        }

        if (numColorsFound > 1) { // only add a palette if there are more than 1 colors in it (to exclude blank palettes)
          PaletteManager.addPalette(new Palette(palette));
        }
      }

      fm.close();

    }
    catch (Throwable t) {
      logError(t);
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

      // 4 - Header (RGCN)
      // 2 - Byte Order
      // 2 - Version
      // 4 - Archive Length
      // 2 - RAHC Offset
      // 2 - Number of Blocks (1)
      // 4 - Header (RAHC)
      // 4 - Block Length
      // 2 - Tile Data Length (in KBs)
      // 2 - Unknown
      fm.skip(28);

      // 4 - Color Depth (3=4bpp, 4=8bpp)
      int imageFormat = fm.readInt();
      if (imageFormat != 3 && imageFormat != 4) {
        return null;
      }

      // 2 - null
      // 2 - null
      // 4 - null
      fm.skip(8);

      // 4 - Tile Data Length
      int dataLength = fm.readInt();
      FieldValidator.checkLength(dataLength, arcSize);

      // 4 - Tile Data Offset (relative to the start of the RAHC Block)
      fm.skip(4);

      // X - Palette Index (size depends on color depth)
      byte[] pixelData = fm.readBytes(dataLength);

      // get all the color Palettes in the archive
      if (PaletteManager.getNumPalettes() <= 0) {
        Resource[] resources = Archive.getResources();

        int numResources = resources.length;
        for (int i = 0; i < numResources; i++) {
          Resource currentResource = resources[i];
          String extension = currentResource.getExtension();
          if (extension.equalsIgnoreCase("NCLR") || extension.equalsIgnoreCase("RLCN")) {
            // found a color palette file - need to extract it and read the colors
            extractAndAddPalette(resources[i]);
          }
        }
      }

      // Find the preferred palette number
      Object resourceObject = SingletonManager.get("CurrentResource");
      if (resourceObject == null || !(resourceObject instanceof Resource)) {
        return null;
      }
      Resource resource = (Resource) resourceObject;

      try {
        int paletteID = Integer.parseInt(resource.getProperty("PaletteID"));
        PaletteManager.setCurrentPalette(paletteID);
      }
      catch (Throwable t) {
      }

      int[] palette = PaletteManager.getCurrentPalette().getPalette();

      int numColors = palette.length;
      if (numColors <= 0) {
        ErrorLogger.log("[Viewer_NCGR_RGCN] Invalid number of colors: " + numColors);
        return null;
      }

      // Now convert the pixelData into an image
      int numPixels = dataLength;
      if (imageFormat == 3) {
        numPixels *= 2;
      }

      int width = 256;
      int height = numPixels / width;

      //pixelData = ImageFormatReader.unswizzle(pixelData, width, height, 16);

      /*
      fm.close();
      fm = new FileManipulator(new ByteBuffer(pixelData));
      
      int[] pixels = new int[numPixels];
      
      if (imageFormat == 3) {
        // 4bpp
        for (int i = 0; i < numPixels; i += 2) {
          int pixel = ByteConverter.unsign(fm.readByte());
      
          int pixel2 = (pixel >> 4) & 15;
          int pixel1 = (pixel & 15);
      
          pixels[i] = pixel1;
          pixels[i + 1] = pixel2;
        }
      }
      else {
        // 8bpp
        for (int i = 0; i < numPixels; i++) {
          pixels[i] = ByteConverter.unsign(fm.readByte());
        }
      }
      
      pixels = ImageFormatReader.reorderPixelBlocks(new ImageResource(pixels, width, height), 8, 8).getPixels();
      */

      // This format doesn't store the image dimensions. So, what we'll do, is we'll generate them as frames, where each frame is a different
      // width from 256,128,64,32,16 and that will let the user choose which one is correct in each case.
      int[] widths = new int[] { 256, 128, 64, 32, 16 };
      int numFrames = widths.length;

      ImageResource[] frames = new ImageResource[numFrames];
      for (int f = 0; f < numFrames; f++) {

        width = widths[f];
        height = numPixels / width;

        byte[] sizedPixelData = null;

        if (imageFormat == 3) {
          sizedPixelData = ImageSwizzler.unswizzleBC4Bit(pixelData, width, height);
        }
        else {
          sizedPixelData = ImageSwizzler.unswizzleBC8Bit(pixelData, width, height);
        }

        fm.close();
        fm = new FileManipulator(new ByteBuffer(sizedPixelData));

        ImageResource frame = null;
        if (imageFormat == 3) {
          // 4bpp
          frame = ImageFormatReader.read4BitPaletted(fm, width, height, true);
        }
        else {
          // 8bpp
          frame = ImageFormatReader.read8BitPaletted(fm, width, height, true);
        }

        frames[f] = frame;
      }

      // set the frame transitions
      ImageFormatReader.createFrameTransitions(frames);

      TemporarySettings.set("PreviewPanel_Image_RetainCurrentFrame", true); // so that the next image will be the same dimensions

      /*
      // Get the currently-chosen frame (aka Dimension)
      int currentFrameNum = Settings.getInt("PreviewPanel_Image_CurrentFrame");
      if (currentFrameNum < 0 || currentFrameNum >= numFrames) {
        currentFrameNum = 0;
      }
      
      ImageResource imageResource = frames[currentFrameNum];
      */
      ImageResource imageResource = frames[0];
      imageResource.setManualFrameTransition(true);

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