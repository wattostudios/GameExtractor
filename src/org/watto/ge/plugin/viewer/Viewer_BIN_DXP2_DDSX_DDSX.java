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
import org.watto.SingletonManager;
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
import org.watto.ge.plugin.archive.Plugin_BIN_DXP2;
import org.watto.ge.plugin.archive.Plugin_BIN_DXP2_2;
import org.watto.ge.plugin.exporter.Exporter_ZLib_CompressedSizeOnly;
import org.watto.io.FileManipulator;
import org.watto.io.buffer.ByteBuffer;
import org.watto.io.converter.IntConverter;
import org.watto.io.converter.StringConverter;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_BIN_DXP2_DDSX_DDSX extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_BIN_DXP2_DDSX_DDSX() {
    super("BIN_DXP2_DDSX_DDSX", "Wings of Prey DDSX Image");
    setExtensions("ddsx");

    setGames("Wings of Prey");
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
      if (plugin instanceof Plugin_BIN_DXP2 || plugin instanceof Plugin_BIN_DXP2_2) {
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
      if (fm.readString(4).equals("DDSx")) {
        rating += 50;

        fm.skip(8);

        // 2 - Image Width
        if (FieldValidator.checkWidth(fm.readShort())) {
          rating += 5;
        }

        // 2 - Image Height
        if (FieldValidator.checkHeight(fm.readShort())) {
          rating += 5;
        }
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

      int width = 0;
      int height = 0;
      String imageFormat = "";

      // get the width/height from the properties of the image resource, which were read by the ArchivePlugin
      Object resourceObject = SingletonManager.get("CurrentResource");
      if (resourceObject == null || !(resourceObject instanceof Resource)) {
        return null;
      }
      Resource resource = (Resource) resourceObject;

      try {
        height = Integer.parseInt(resource.getProperty("Height"));
        width = Integer.parseInt(resource.getProperty("Width"));
        imageFormat = resource.getProperty("ImageFormat");
      }
      catch (Throwable t) {
        //
      }

      if (width == 0 && height == 0) {
        // we're probably reading from a DDSX file, not one extracted from an archive

        // 4 - Header (DDSx)
        fm.skip(4);

        // 4 - DDS Format (DXT1/DXT3/DXT5/(byte)21/(byte)50)
        byte[] imageFormatBytes = fm.readBytes(4);
        imageFormat = StringConverter.convertLittle(imageFormatBytes);
        if (imageFormat.equals("DXT1") || imageFormat.equals("DXT3") || imageFormat.equals("DXT5") || imageFormat.equals("BC7 ") || imageFormat.equals("ATI2") || imageFormat.equals("ATI1")) {
          //
        }
        else {
          imageFormat = "" + IntConverter.convertLittle(imageFormatBytes);
        }

        // 4 - Unknown
        fm.skip(4);

        // 2 - Image Width
        width = fm.readShort();
        FieldValidator.checkWidth(width);

        // 2 - Image Height
        height = fm.readShort();
        FieldValidator.checkHeight(height);

        // 2 - Number Of Mipmaps?
        // 2 - null
        // 2 - Header Length (32)
        // 4 - null
        // 2 - Header Length (32)
        fm.skip(12);

        // 4 - Compressed Image Data Length
        int length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // X - Pixel Data (ZLib Compression)
        int maxSize = width * height * 4; // dont care if there's more mipmaps, only want the first one

        Exporter_ZLib_CompressedSizeOnly exporter = Exporter_ZLib_CompressedSizeOnly.getInstance();
        exporter.open(fm, length, length);

        byte[] decompBytes = new byte[maxSize];
        for (int i = 0; i < maxSize; i++) {
          if (exporter.available()) { // make sure we read the next bit of data, if required
            decompBytes[i] = (byte) exporter.read();
          }
          else {
            break;
          }
        }

        exporter.close();

        // open the decompressed file data for processing
        fm.close();
        fm = new FileManipulator(new ByteBuffer(decompBytes));
      }

      if (Archive.getReadPlugin() instanceof Plugin_BIN_DXP2_2) {
        // the biggest mipmap is at the end of the file, not the beginning, so need to skip to it.

        int decompLength = 0;
        if (imageFormat.equals("DXT1")) {
          decompLength = width * height / 2;
        }
        else if (imageFormat.equals("DXT3") || imageFormat.equals("DXT5") || imageFormat.equals("ATI1") || imageFormat.equals("ATI2") || imageFormat.equals("BC7 ") || imageFormat.equals("50")) {
          decompLength = width * height;
        }
        else if (imageFormat.equals("21")) {
          decompLength = width * height * 4;
        }

        if (decompLength != 0) {
          int skipSize = (int) (fm.getLength() - decompLength - fm.getOffset());
          if (skipSize > 0) {
            fm.skip(skipSize);
          }
        }
      }

      // X - Pixel Data
      ImageResource imageResource = null;

      if (imageFormat.equals("DXT1")) {
        imageResource = ImageFormatReader.readDXT1(fm, width, height);
      }
      else if (imageFormat.equals("DXT3")) {
        imageResource = ImageFormatReader.readDXT3(fm, width, height);
      }
      else if (imageFormat.equals("DXT5")) {
        imageResource = ImageFormatReader.readDXT5(fm, width, height);
      }
      else if (imageFormat.equals("BC7 ")) {
        imageResource = ImageFormatReader.readBC7(fm, width, height);
      }
      else if (imageFormat.equals("ATI1")) {
        imageResource = ImageFormatReader.readATI1(fm, width, height);
      }
      else if (imageFormat.equals("ATI2")) {
        imageResource = ImageFormatReader.readATI2(fm, width, height);
      }
      else if (imageFormat.equals("21")) {
        imageResource = ImageFormatReader.readRGBA(fm, width, height);
      }
      else if (imageFormat.equals("50")) {
        imageResource = ImageFormatReader.read8BitPaletted(fm, width, height);
      }
      else {
        ErrorLogger.log("[Viewer_BIN_DXP2_DDSX_DDSX] Unknown Image Format: " + imageFormat + " width:" + width + " height:" + height);
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
      int width = ivp.getImageWidth();
      int height = ivp.getImageHeight();

      String imageFormat = "";

      // get the width/height from the properties of the image resource, which were read by the ArchivePlugin
      try {
        imageFormat = resourceBeingReplaced.getProperty("ImageFormat");
      }
      catch (Throwable t) {
        //
      }

      // Try to write the file out in the right format (same as the source)

      // Some will need to be changed (can't write everything)
      if (imageFormat.equals("BC7 ") || imageFormat.equals("ATI1") || imageFormat.equals("ATI2") || imageFormat.equals("")) {
        imageFormat = "DXT5";
      }

      ImageManipulator im = new ImageManipulator(ivp.getImageResource());
      ImageResource[] mipmaps = im.generateMipmaps();

      if (Archive.getReadPlugin() instanceof Plugin_BIN_DXP2_2) {
        // smallest to largest
        for (int m = mipmaps.length - 1; m >= 0; m--) {
          ImageResource imageResource = mipmaps[m];

          if (imageFormat.equals("DXT1")) {
            ImageFormatWriter.writeDXT1(fm, imageResource);
          }
          else if (imageFormat.equals("DXT3")) {
            ImageFormatWriter.writeDXT3(fm, imageResource);
          }
          else if (imageFormat.equals("DXT5")) {
            ImageFormatWriter.writeDXT5(fm, imageResource);
          }
          else if (imageFormat.equals("21")) {
            ImageFormatWriter.writeRGBA(fm, imageResource);
          }
          else if (imageFormat.equals("50")) {
            int[] pixels = imageResource.getPixels();
            int numPixels = pixels.length;

            for (int i = 0; i < numPixels; i++) {
              fm.writeByte(pixels[i]);
            }
          }
        }
      }
      else {
        // largest to smallest
        for (int m = 0; m < mipmaps.length; m++) {
          ImageResource imageResource = mipmaps[m];

          if (imageFormat.equals("DXT1")) {
            ImageFormatWriter.writeDXT1(fm, imageResource);
          }
          else if (imageFormat.equals("DXT3")) {
            ImageFormatWriter.writeDXT3(fm, imageResource);
          }
          else if (imageFormat.equals("DXT5")) {
            ImageFormatWriter.writeDXT5(fm, imageResource);
          }
          else if (imageFormat.equals("21")) {
            ImageFormatWriter.writeRGBA(fm, imageResource);
          }
          else if (imageFormat.equals("50")) {
            int[] pixels = imageResource.getPixels();
            int numPixels = pixels.length;

            for (int i = 0; i < numPixels; i++) {
              fm.writeByte(pixels[i]);
            }
          }
        }
      }

      // set the new properties on the resource
      resourceBeingReplaced.setProperty("Width", "" + width);
      resourceBeingReplaced.setProperty("Height", "" + height);
      resourceBeingReplaced.setProperty("ImageFormat", "" + imageFormat);

      fm.close();

    }
    catch (Throwable t) {
      logError(t);
    }
  }

}