/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2025 wattostudios
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
import org.watto.ge.helper.ColorSplitAlpha;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.helper.ImageFormatReader;
import org.watto.ge.helper.ImageFormatWriter;
import org.watto.ge.helper.ImageManipulator;
import org.watto.ge.plugin.AllFilesPlugin;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.ge.plugin.archive.Plugin_ARC_ARC_2;
import org.watto.io.FileManipulator;
import org.watto.io.converter.IntConverter;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_ARC_ARC_2_TEX_TEX extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_ARC_ARC_2_TEX_TEX() {
    super("ARC_ARC_2_TEX_TEX", "Lost Planet TEX Image");
    setExtensions("tex");

    setGames("Lost Planet",
        "Capcom Fighting Collection");
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
      if (plugin instanceof Plugin_ARC_ARC_2) {
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
      if (fm.readString(4).equals("TEX" + (char) 0)) {
        rating += 50;
      }
      else {
        rating = 0;
      }

      // 2 - Unknown (112)
      fm.skip(2);

      // 2 - Unknown (2/34)
      int imageFormat = fm.readShort();
      if (imageFormat == 2 || imageFormat == 34) {
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

      // 4 - Header (TEX + null)
      // 2 - Unknown (112)
      fm.skip(6);

      // 2 - Unknown (2/34)
      int dataFormat = fm.readShort();

      // 4 - Unknown (257)
      fm.skip(4);

      ImageResource imageResource = null;

      try {
        // 2 - Width
        int width = fm.readShort();
        FieldValidator.checkWidth(width);

        // 2 - Height
        int height = fm.readShort();
        FieldValidator.checkHeight(height);

        // 4 - null
        fm.skip(4);

        // 4 - Direct X Format Header (DXT5)
        String fourCC = fm.readString(4);

        // 4 - Unknown (1065353216)
        // 4 - Unknown (1065353216)
        // 4 - Unknown (1065353216)
        // 4 - Unknown (1065353216)
        fm.skip(16);

        // 4 - Header Length (44/80)
        int headerLength = fm.readInt();
        FieldValidator.checkLength(headerLength, fm.getLength());

        int skipSize = (int) (headerLength - fm.getOffset());
        if (skipSize < 0) {
          return null;
        }
        fm.skip(skipSize);
        //fm.seek(headerLength);

        // X - Image Data
        if (fourCC.equals("DXT3")) {
          imageResource = ImageFormatReader.readDXT3(fm, width, height);
          imageResource.addProperty("ImageFormat", "DXT3");
        }
        else if (fourCC.equals("DXT5")) {
          imageResource = ImageFormatReader.readDXT5(fm, width, height);
          imageResource.addProperty("ImageFormat", "DXT5");
        }
        else if (fourCC.equals("DXT1")) {
          imageResource = ImageFormatReader.readDXT1(fm, width, height);
          imageResource.addProperty("ImageFormat", "DXT1");
        }
        else if (dataFormat == 2) {
          imageResource = ImageFormatReader.readBGRA(fm, width, height);
          imageResource.setProperty("ImageFormat", "BGRA");
        }
        else {
          fm.close();
          return null;
        }
      }
      catch (Throwable t) {
        // try for the BC7 from Capcom Fighting Collection

        fm.relativeSeek(8);

        // 4 - Image Width and Height (HHHHHHHH,HHHHHWWW,WWWWWWWW,WWXXXXXX)
        long widthHeight = IntConverter.unsign(fm.readInt());
        int height = (int) (widthHeight >> 19);
        int width = (int) ((widthHeight >> 6) & 8191);

        FieldValidator.checkHeight(height);
        FieldValidator.checkWidth(width);

        // 4 - Unknown
        // 4 - Image Data Offset (24)
        // 4 - null
        fm.skip(12);

        imageResource = ImageFormatReader.readBC7(fm, width, height);
        imageResource.setProperty("ImageFormat", "BC7");
        //imageResource = ImageFormatReader.swapGBARtoARGB(imageResource);

        // Apply Capcom MT Framework pixel filter
        int[] pixels = imageResource.getPixels();
        int numPixels = pixels.length;

        for (int i = 0; i < numPixels; i++) {
          int pixel = pixels[i];

          ColorSplitAlpha split = new ColorSplitAlpha(pixel);

          /*
          // close
          a *= 2;
          r = (split.getBlue() * 2 + a) / 3;
          g = (split.getGreen() + a) / 3;
          b = (split.getRed() * 2 + a) / 3;
          a = split.getGreen();
          */

          int a = split.getAlpha() * 2;
          int r = ((split.getBlue() * 2) + a) / 3;
          int g = ((split.getGreen()) + a) / 3;
          int b = ((split.getRed() * 2) + a) / 3;

          // Increase contrast, reduce brightness
          int value = (r + g + b) / 3;

          int largeSaturation = 25;
          int smallSaturation = 10;
          int brightness = 35;

          r -= brightness;
          g -= brightness;
          b -= brightness;

          if (value >= 192) {
            r += largeSaturation;
            g += largeSaturation;
            b += largeSaturation;
          }
          else if (value >= 128) {
            r += smallSaturation;
            g += smallSaturation;
            b += smallSaturation;
          }
          else if (value >= 64) {
            r -= smallSaturation;
            g -= smallSaturation;
            b -= smallSaturation;
          }
          else {
            r -= largeSaturation;
            g -= largeSaturation;
            b -= largeSaturation;
          }

          a = split.getGreen();

          if (r > 255) {
            r = 255;
          }
          if (g > 255) {
            g = 255;
          }
          if (b > 255) {
            b = 255;
          }

          if (r < 0) {
            r = 0;
          }
          if (g < 0) {
            g = 0;
          }
          if (b < 0) {
            b = 0;
          }

          split.setRed(r);
          split.setGreen(g);
          split.setBlue(b);
          split.setAlpha(a);
          pixels[i] = split.getColor();

        }

        imageResource.setPixels(pixels);

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

      // Set some property defaults in case we're doing a conversion (and thus there probably isn't any properties set)
      String imageFormat = "DXT3";

      // Now try to get the property values from the ImageResource, if they exist
      ImageResource imageResource = ((PreviewPanel_Image) preview).getImageResource();

      if (imageResource != null) {
        imageFormat = imageResource.getProperty("ImageFormat", "DXT3");
      }

      if (!(imageFormat.equals("DXT1") || imageFormat.equals("DXT3") || imageFormat.equals("DXT5") || imageFormat.equals("BGRA"))) {
        // a different image format not allowed in this image - change to DXT3
        imageFormat = "DXT3";
      }

      // 4 - Header (TEX + null)
      fm.writeString("TEX");
      fm.writeByte(0);

      // 2 - Unknown(112)
      fm.writeShort((short) 112);

      // 2 - Format (34=DDS, 2=RGBA)
      if (imageFormat.equals("BGRA")) {
        fm.writeShort((short) 2);
      }
      else {
        fm.writeShort((short) 34);
      }

      // 4 - Unknown (257)
      fm.writeInt(257);

      // 2 - Width
      fm.writeShort((short) imageWidth);

      // 2 - Height
      fm.writeShort((short) imageHeight);

      // 4 - null
      fm.writeInt(0);

      // 4 - Direct X Format Header (DXT5)
      if (imageFormat.equals("BGRA")) {
        fm.writeInt(21);
      }
      else {
        fm.writeString(imageFormat);
      }

      // 4 - Unknown (1065353216)
      fm.writeInt(1065353216);

      // 4 - Unknown (1065353216)
      fm.writeInt(1065353216);

      // 4 - Unknown (1065353216)
      fm.writeInt(1065353216);

      // 4 - Unknown (1065353216)
      fm.writeInt(1065353216);

      // 4 - Header Length (44)
      fm.writeInt(44);

      if (imageFormat.equals("BGRA")) {
        ImageFormatWriter.writeBGRA(fm, imageResource);
      }
      else if (imageFormat.equals("DXT1")) {
        ImageFormatWriter.writeDXT1(fm, imageResource);
      }
      else if (imageFormat.equals("DXT3")) {
        ImageFormatWriter.writeDXT3(fm, imageResource);
      }
      else if (imageFormat.equals("DXT5")) {
        ImageFormatWriter.writeDXT5(fm, imageResource);
      }

      fm.close();

    }
    catch (Throwable t) {
      logError(t);
    }
  }

}