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
import org.watto.ge.plugin.AllFilesPlugin;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.ge.plugin.archive.Plugin_DAT_66;
import org.watto.io.FileManipulator;
import org.watto.io.buffer.ByteBuffer;
import org.watto.io.converter.ByteConverter;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_DAT_66_BITMAP extends ViewerPlugin {

  static int[] palette = null;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_DAT_66_BITMAP() {
    super("DAT_66_BITMAP", "Rollercoaster Tycoon Classic Bitmap Image");
    setExtensions("bitmap", "compressed_bitmap");

    setGames("Rollercoaster Tycoon Classic");
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
      if (plugin instanceof Plugin_DAT_66) {
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
  public int[] extractPalette(Resource paltResource) {
    try {
      int paltLength = (int) paltResource.getLength();

      ByteBuffer buffer = new ByteBuffer(paltLength);
      FileManipulator fm = new FileManipulator(buffer);
      paltResource.extract(fm);

      fm.seek(0); // back to the beginning of the byte array

      int numColors = paltLength / 3;

      int startPos = 0;
      try {
        startPos = Integer.parseInt(paltResource.getProperty("XOffset"));
      }
      catch (Throwable t) {
      }

      palette = new int[256];
      for (int i = 0; i < numColors; i++) {
        // 3 - BGR
        int bPixel = ByteConverter.unsign(fm.readByte());
        int gPixel = ByteConverter.unsign(fm.readByte());
        int rPixel = ByteConverter.unsign(fm.readByte());
        int aPixel = 255;

        palette[startPos] = ((rPixel << 16) | (gPixel << 8) | bPixel | (aPixel << 24));
        startPos++;
      }

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

      long arcSize = fm.getLength();

      int height = 32;
      int width = 32;

      // get the width/height from the properties of the image resource, which were read by the ArchivePlugin
      Object resourceObject = SingletonManager.get("CurrentResource");
      if (resourceObject == null || !(resourceObject instanceof Resource)) {
        return null;
      }
      Resource resource = (Resource) resourceObject;

      try {
        height = Integer.parseInt(resource.getProperty("Height"));
        width = Integer.parseInt(resource.getProperty("Width"));
      }
      catch (Throwable t) {
        //
      }

      // get a Palette
      if (palette == null) {
        palette = new int[0];
        Resource[] resources = Archive.getResources();
        int numResources = resources.length;
        for (int i = 0; i < numResources; i++) {
          Resource currentResource = resources[i];
          if (currentResource.getExtension().equalsIgnoreCase("palette")) {
            // found a color palette file - need to extract it and read the colors
            palette = extractPalette(resources[i]);
            break;
          }
        }
      }

      if (palette == null) {
        ErrorLogger.log("[VIEWER_DAT_66] Failure loading color palette");
        return null;
      }

      int numColors = palette.length; // should always be 256
      if (numColors <= 0) {
        ErrorLogger.log("[VIEWER_DAT_66] Invalid number of colors: " + numColors);
        return null;
      }

      // now read a file
      ImageResource imageResource = null;

      String extension = resource.getExtension();
      if (extension.equalsIgnoreCase("bitmap")) {
        // direct bitmap - width x height palette values
        int numPixels = width * height;
        int[] pixels = new int[numPixels];

        for (int i = 0; i < numPixels; i++) {
          pixels[i] = palette[ByteConverter.unsign(fm.readByte())];
        }

        imageResource = new ImageResource(pixels, width, height);
      }
      else if (extension.equalsIgnoreCase("compressed_bitmap")) {
        // compressed bitmap

        // read the scanline offsets
        int[] offsets = new int[height];
        for (int i = 0; i < height; i++) {
          // 2 - scanline offset (relative to the start of the file)
          short offset = fm.readShort();
          FieldValidator.checkOffset(offset, arcSize);
          offsets[i] = offset;
        }

        // determine the length of each scanline data
        int[] lengths = new int[height];
        for (int i = 1; i < height; i++) {
          lengths[i - 1] = offsets[i] - offsets[i - 1];
        }
        lengths[height - 1] = (int) (arcSize - offsets[height - 1]);

        // now read each scanline
        int numPixels = width * height;
        int[] pixels = new int[numPixels];

        for (int i = 0; i < height; i++) {
          fm.seek(offsets[i]); // just in case

          int lineLength = lengths[i];
          int position = 0;
          while (position < lineLength) {
            // 1 - Number of Bytes of Data (top bit is also a flag --> 0=more scan elements after this one, 1=this is the last scan element for this line)
            int numBytes = ByteConverter.unsign(fm.readByte());
            int lastScanElement = (numBytes >> 7);
            numBytes = (numBytes & 63);

            // 1 - X Offset where these pixels appear in the image
            int xOffset = ByteConverter.unsign(fm.readByte());

            // X - Data Bytes
            int arrayStart = (i * width) + xOffset;
            for (int b = 0; b < numBytes; b++) {
              pixels[arrayStart] = palette[ByteConverter.unsign(fm.readByte())];
              arrayStart++;
            }

            position += (2 + numBytes);
            if (lastScanElement == 1) {
              break;
            }
          }

        }

        imageResource = new ImageResource(pixels, width, height);
      }

      fm.close();

      return imageResource;

    }
    catch (

    Throwable t) {
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