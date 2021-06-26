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
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.ge.plugin.archive.PluginGroup_U;
import org.watto.ge.plugin.archive.Plugin_U_118;
import org.watto.ge.plugin.archive.Plugin_U_Generic;
import org.watto.io.FileManipulator;
import org.watto.io.buffer.ByteBuffer;
import org.watto.io.converter.ByteConverter;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_U_Texture_Generic_DDS extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_U_Texture_Generic_DDS() {
    super("U_Texture_Generic_DDS", "Unreal Engine DDS Texture Image");
    setExtensions("texture"); // MUST BE LOWER CASE

    setGames("Unreal Engine",
        "Devastation",
        "Land Of The Dead: Road To Fiddlers Green",
        "Lemony Snicket's A Series of Unfortunate Events",
        "Pariah",
        "Postal 2",
        "Rainbow Six 3: Raven Shield",
        "Redneck Kentucky and the Next Generation Chickens",
        "Shadow Ops: Red Mercury",
        "Splinter Cell",
        "Splinter Cell: Pandora Tomorrow",
        "Star Wars: Republic Commando",
        "Tribes Vengeance",
        "Unreal 2: The Awakening",
        "Unreal Tournament 2003",
        "Unreal Tournament 2004",
        "Warpath",
        "XIII");
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

      if (Archive.getReadPlugin() instanceof PluginGroup_U) {
        rating += 51; // 51, so that it's tried before the paletted one is tried
      }

      if (Archive.getReadPlugin() instanceof Plugin_U_Generic || Archive.getReadPlugin() instanceof Plugin_U_118) {
        rating += 10;
      }

      if (FieldValidator.checkExtension(fm, extensions)) {
        rating += 25;
      }
      else {
        rating = 0;
      }

      // 1 - Number Of Mipmaps (9)
      int numMipmaps = ByteConverter.unsign(fm.readByte());
      if (numMipmaps == 0) {
        numMipmaps = ByteConverter.unsign(fm.readByte()); // Unreal 2 - The Awakening
      }

      if (numMipmaps < 50) {
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

      /*
      // 1 - Number Of Mipmaps (5)
      int numMipmaps = ByteConverter.unsign(fm.readByte());
      if (numMipmaps == 0) {
        numMipmaps = ByteConverter.unsign(fm.readByte()); // Unreal 2 - The Awakening
      }
      */
      int numMipmaps = 1;
      try {
        // 1 - Number Of Mipmaps (9)
        numMipmaps = ByteConverter.unsign(fm.readByte());
        FieldValidator.checkLength(numMipmaps, 20);
      }
      catch (Throwable t) {
        // skip another 3 bytes and try again
        fm.skip(3);

        numMipmaps = ByteConverter.unsign(fm.readByte());
        FieldValidator.checkLength(numMipmaps, 20);
      }

      // for each mipmap
      for (int i = 0; i < numMipmaps; i++) {
        // 4 - Unknown
        fm.skip(4);

        // 1-5 - Image Data Length
        int dataLength = (int) PluginGroup_U.readIndex(fm);
        if (dataLength == 0) {
          dataLength = (int) PluginGroup_U.readIndex(fm);
        }
        FieldValidator.checkLength(dataLength, fm.getLength());

        // X - Texture Data (DXT1=W*H/2, DXT3/5=W*H)
        byte[] textureData = fm.readBytes(dataLength);

        // 4 - Width
        int width = fm.readInt();
        FieldValidator.checkNumFiles(width);

        // 4 - Height
        int height = fm.readInt();
        FieldValidator.checkNumFiles(height);

        // 1 - ID
        // 1 - ID
        fm.skip(2);

        if (dataLength > 0) {
          //fm.seek(fm.getOffset() - 10 - dataLength); // not needed, as we have already read the textureData into a byte[]

          // Now open the already-read texture data from above, so we can read the image
          fm.close();
          fm = new FileManipulator(new ByteBuffer(textureData));

          ImageResource imageResource = null;
          if (width * height == dataLength) {
            imageResource = ImageFormatReader.readDXT3(fm, width, height);
            imageResource.setProperty("ImageFormat", "DXT3");
          }
          else if (width * height == dataLength / 4) {
            imageResource = ImageFormatReader.readRGBA(fm, width, height);
            imageResource.setProperty("ImageFormat", "RGBA");
          }
          else {
            imageResource = ImageFormatReader.readDXT1(fm, width, height);
            imageResource.setProperty("ImageFormat", "DXT1");
          }

          fm.close();

          imageResource.setProperty("MipmapCount", "" + numMipmaps);

          return imageResource;

        }

      }

      return null;

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
      String imageFormat = "DXT3";

      // Now try to get the property values from the ImageResource, if they exist
      ImageResource imageResource = ((PreviewPanel_Image) preview).getImageResource();

      if (imageResource != null) {
        imageFormat = imageResource.getProperty("ImageFormat", "DXT3");
        mipmapCount = imageResource.getProperty("MipmapCount", 1);
      }

      if (!(imageFormat.equals("DXT1") || imageFormat.equals("DXT3") || imageFormat.equals("DXT5") || imageFormat.equals("RGBA"))) {
        // a different image format not allowed in this image - change to DXT3
        imageFormat = "DXT3";
      }
      if (mipmapCount > mipmaps.length) {
        mipmapCount = mipmaps.length;
      }
      if (imageFormat.equals("RGBA")) {
        mipmapCount = 1;
      }

      // 4 - Number Of MipMaps
      fm.writeInt(mipmapCount);

      // X - Mipmaps
      for (int i = 0; i < mipmapCount; i++) {
        ImageResource mipmap = mipmaps[i];

        if (!imageFormat.equals("RGBA")) {
          int mipmapHeight = mipmap.getHeight();
          int resizedHeight = mipmapHeight;
          if (resizedHeight < 4) {
            resizedHeight = 4;
          }

          int mipmapWidth = mipmap.getWidth();
          int resizedWidth = mipmapWidth;
          if (resizedWidth < 4) {
            resizedWidth = 4;
          }

          int pixelCount = resizedWidth * resizedHeight;

          int pixelLength = mipmap.getNumPixels();
          if (pixelLength < pixelCount) {
            // one of the smallest mipmaps (eg 1x1 or 2x2) --> needs to be resized to 4x4
            int[] oldPixels = mipmap.getImagePixels();
            int[] newPixels = new int[pixelCount]; // minimum of 4x4, but if one dimension is already > 4, can be larger

            for (int h = 0; h < resizedHeight; h++) {
              for (int w = 0; w < resizedWidth; w++) {
                if (h < mipmapHeight && w < mipmapWidth) {
                  // copy the pixel from the original
                  newPixels[h * resizedWidth + w] = oldPixels[h * mipmapWidth + w];
                }
                else {
                  newPixels[h * resizedWidth + w] = 0;
                }
              }
            }
            mipmap.setPixels(newPixels);
            mipmap.setWidth(resizedWidth);
            mipmap.setHeight(resizedHeight);
          }
        }

        int width = mipmap.getWidth();
        int height = mipmap.getHeight();

        int length = width * height;
        if (imageFormat.equals("DXT1")) {
          length /= 2;
        }
        else if (imageFormat.equals("RGBA")) {
          length *= 4;
        }

        // 4 - Unknown
        fm.writeInt(length);

        // 4 - Image Data Length
        fm.writeInt(length);

        // X - Texture Data (DXT1=W*H/2, DXT3/5=W*H, RGBA=W*H*4)
        if (imageFormat.equals("DXT1")) {
          ImageFormatWriter.writeDXT1(fm, mipmap);
        }
        else if (imageFormat.equals("DXT3")) {
          ImageFormatWriter.writeDXT3(fm, mipmap);
        }
        else if (imageFormat.equals("DXT5")) {
          ImageFormatWriter.writeDXT5(fm, mipmap);
        }
        else if (imageFormat.equals("RGBA")) {
          ImageFormatWriter.writeRGBA(fm, mipmap);
        }

        // 4 - Width
        fm.writeInt(width);

        // 4 - Height
        fm.writeInt(height);

        // 1 - ID
        fm.writeByte(mipmapCount - i);

        // 1 - ID
        fm.writeByte(mipmapCount - i);

      }

      fm.close();

    }
    catch (Throwable t) {
      logError(t);
    }
  }

}