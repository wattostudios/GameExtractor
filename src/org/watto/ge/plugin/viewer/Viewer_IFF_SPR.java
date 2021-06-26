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
import org.watto.Settings;
import org.watto.component.PreviewPanel;
import org.watto.component.PreviewPanel_Image;
import org.watto.datatype.Archive;
import org.watto.datatype.ImageResource;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.AllFilesPlugin;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.ge.plugin.archive.Plugin_IFF;
import org.watto.ge.plugin.resource.Resource_FileID;
import org.watto.io.FileManipulator;
import org.watto.io.buffer.ByteBuffer;
import org.watto.io.converter.ByteConverter;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_IFF_SPR extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_IFF_SPR() {
    super("IFF_SPR", "The Sims SPR#/SPR2 Image");
    setExtensions("spr#", "spr2");

    setGames("The Sims");
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
      if (plugin instanceof Plugin_IFF) {
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

      // 4 - Version
      int version = fm.readInt();
      if (version == 502 || version == 503 || version == 504 || version == 505 || version == 1000) {
        rating += 50;
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
      ByteBuffer buffer = new ByteBuffer((int) paltResource.getLength());
      FileManipulator fm = new FileManipulator(buffer);
      paltResource.extract(fm);

      fm.seek(0); // back to the beginning of the byte array

      // 4 - Version
      fm.skip(4);

      // 4 - Number of Colors
      int numColors = fm.readInt();
      FieldValidator.checkNumColors(numColors);

      // 4 - null
      // 4 - null
      fm.skip(8);

      int[] palette = new int[numColors];
      for (int i = 0; i < numColors; i++) {
        // 3 - RGB
        int rPixel = ByteConverter.unsign(fm.readByte());
        int gPixel = ByteConverter.unsign(fm.readByte());
        int bPixel = ByteConverter.unsign(fm.readByte());
        int aPixel = 255;

        palette[i] = ((rPixel << 16) | (gPixel << 8) | bPixel | (aPixel << 24));
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
  Reads a single SPR# frame
  **********************************************************************************************
  **/
  public ImageResource readImageSPR(FileManipulator fm, int[] palette) {
    try {

      // 4 - null
      fm.skip(4);

      // 2 - Height
      short height = fm.readShort();
      FieldValidator.checkHeight(height);

      // 2 - Width
      short width = fm.readShort();
      FieldValidator.checkWidth(width);

      // 2 - Unknown
      fm.skip(2);

      // read the image
      int numPixels = width * height;
      int[] pixels = new int[numPixels];
      int currentPixel = 0;
      for (int i = 0; i < height; i++) {
        // 1 - Encoding
        int encoding = ByteConverter.unsign(fm.readByte());

        // 1 - Count
        int count = ByteConverter.unsign(fm.readByte());

        if (encoding == 9) {
          // count rows of transparent
          for (int r = 0; r < count; r++) {
            for (int w = 0; w < width; w++) {
              pixels[currentPixel] = 0;
              currentPixel++;
            }
          }
          i += (count - 1); // no headers for these rows
        }
        else if (encoding == 4) {
          // pixel data
          count -= 2; // skip the 2 header bytes we just read

          for (int c = 0; c < count;) {
            // 1 - Format Code
            int formatCode = ByteConverter.unsign(fm.readByte());
            c++;

            // 1 - Format Count
            int formatCount = ByteConverter.unsign(fm.readByte());
            c++;

            if (formatCode == 1) {
              // Background Pixels
              for (int w = 0; w < formatCount; w++) {
                pixels[currentPixel] = 0;
                currentPixel++;
              }
            }
            else if (formatCode == 2) {
              // RLE encoding

              // 1 - Palette Index
              int paletteIndex = ByteConverter.unsign(fm.readByte());
              int paletteValue = palette[paletteIndex];

              c++;

              // 1 - Unknown
              fm.skip(1);
              c++;

              for (int w = 0; w < formatCount; w++) {
                pixels[currentPixel] = paletteValue;
                currentPixel++;
              }
            }
            else if (formatCode == 3) {
              // Copy Pixels

              for (int w = 0; w < formatCount; w++) {
                // 1 - Palette Index
                int paletteIndex = ByteConverter.unsign(fm.readByte());
                int paletteValue = palette[paletteIndex];
                c++;

                pixels[currentPixel] = paletteValue;
                currentPixel++;
              }

              if (formatCount % 2 == 1) {
                // 1 - padding byte
                fm.skip(1);
                c++;
              }
            }
            else {
              ErrorLogger.log("[VIEWER_IFF_SPR] Unknown SPR Format Code: " + formatCode);
              return null;
            }
          }

          // the rest of the current row is filled with transparency
          int nextOffset = (i + 1) * width;
          int transparentCount = nextOffset - currentPixel;
          for (int w = 0; w < transparentCount; w++) {
            pixels[currentPixel] = 0;
            currentPixel++;
          }
        }
        else {
          ErrorLogger.log("[VIEWER_IFF_SPR] Unknown SPR Row Encoding: " + encoding);
          return null;
        }

      }

      return new ImageResource(pixels, width, height);
    }
    catch (Throwable t) {
      logError(t);
      return null;
    }
  }

  /**
   **********************************************************************************************
   Reads a single SPR2 frame
   **********************************************************************************************
   **/
  public ImageResource readImageSPR2(FileManipulator fm, int[] paletteIn) {
    try {

      // 2 - Width
      short width = fm.readShort();
      FieldValidator.checkWidth(width);

      // 2 - Height
      short height = fm.readShort();
      FieldValidator.checkHeight(height);

      // 2 - Flags
      fm.skip(2);

      // 2 - null
      // 2 - Palette ID (again)
      fm.skip(4);

      // 2 - Transparent Pixel ID
      short transID = fm.readShort();

      int numColors = paletteIn.length;
      FieldValidator.checkRange(transID, 0, numColors);

      int[] palette = new int[numColors];
      System.arraycopy(paletteIn, 0, palette, 0, numColors);
      palette[transID] = 0;

      // 2 - Y Location
      // 2 - X Location
      fm.skip(4);

      // read the image
      int numPixels = width * height;
      int[] pixels = new int[numPixels];
      int currentPixel = 0;
      for (int i = 0; i < height; i++) {
        // 2 - Encoding and Count
        int byte1 = ByteConverter.unsign(fm.readByte());
        int byte2 = ByteConverter.unsign(fm.readByte());

        int encoding = (byte2 >> 5);
        int count = (((byte2 & 31) << 8) | byte1);

        if (encoding == 4) {
          // count rows of transparent
          for (int r = 0; r < count; r++) {
            for (int w = 0; w < width; w++) {
              pixels[currentPixel] = 0;
              currentPixel++;
            }
          }
          i += (count - 1); // no headers for these rows
        }
        else if (encoding == 0) {
          // pixel data
          count -= 2; // skip the 2 header bytes we just read

          for (int c = 0; c < count;) {
            // 2 - Format Code and Count
            byte1 = ByteConverter.unsign(fm.readByte());
            byte2 = ByteConverter.unsign(fm.readByte());
            c += 2;

            int formatCode = (byte2 >> 5);
            int formatCount = (((byte2 & 31) << 8) | byte1);

            if (formatCode == 3) {
              // Background Pixels
              for (int w = 0; w < formatCount; w++) {
                pixels[currentPixel] = 0;
                currentPixel++;
              }
            }
            else if (formatCode == 1) {
              // Copy Pixels (copy two channels (z-buffer and color))

              for (int w = 0; w < formatCount; w++) {
                // 1 - Z-Buffer
                fm.skip(1);

                // 1 - Palette Index
                int paletteIndex = ByteConverter.unsign(fm.readByte());
                int paletteValue = palette[paletteIndex];
                c += 2;

                pixels[currentPixel] = paletteValue;
                currentPixel++;
              }

            }
            else if (formatCode == 2) {
              // Copy Pixels (copy three channels (z-buffer, color, and alpha))

              for (int w = 0; w < formatCount; w++) {
                // 1 - Z-Buffer
                fm.skip(1);

                // 1 - Palette Index
                int paletteIndex = ByteConverter.unsign(fm.readByte());
                int paletteValue = palette[paletteIndex];

                // 1 - Alpha
                int alphaValue = ByteConverter.unsign(fm.readByte());

                c += 3;

                paletteValue = (((paletteValue << 8) >> 8) | (alphaValue << 24));

                pixels[currentPixel] = paletteValue;
                currentPixel++;
              }

              if (formatCount % 2 == 1) {
                // 1 - padding byte
                fm.skip(1);
                c++;
              }
            }
            else if (formatCode == 6) {
              // Copy Pixels (copy one channel (color))

              for (int w = 0; w < formatCount; w++) {
                // 1 - Palette Index
                int paletteIndex = ByteConverter.unsign(fm.readByte());
                int paletteValue = palette[paletteIndex];
                c++;

                pixels[currentPixel] = paletteValue;
                currentPixel++;
              }

              if (formatCount % 2 == 1) {
                // 1 - padding byte
                fm.skip(1);
                c++;
              }
            }
            else {
              ErrorLogger.log("[VIEWER_IFF_SPR] Unknown SPR2 Format Code: " + formatCode);
              return null;
            }
          }

          // the rest of the current row is filled with transparency
          int nextOffset = (i + 1) * width;
          int transparentCount = nextOffset - currentPixel;
          for (int w = 0; w < transparentCount; w++) {
            pixels[currentPixel] = 0;
            currentPixel++;
          }

        }
        else {
          ErrorLogger.log("[VIEWER_IFF_SPR] Unknown SPR2 Row Encoding: " + encoding);
          return null;
        }

      }

      return new ImageResource(pixels, width, height);
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

      // 4 - Version
      int version = fm.readInt();

      // 4 - Frame Count
      int numImages = fm.readInt();
      FieldValidator.checkRange(numImages, 1, 50);

      // 4 - Palette ID
      int paletteID = fm.readInt();

      int[] offsets = new int[numImages];
      for (int i = 0; i < numImages; i++) {
        // 4 - Image Offset
        int offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);
        offsets[i] = offset;
      }

      // get the Palette
      int[] palette = new int[0];
      Resource[] resources = Archive.getResources();
      int numResources = resources.length;
      for (int i = 0; i < numResources; i++) {
        Resource_FileID currentResource = (Resource_FileID) resources[i];
        if (currentResource.getID() == paletteID && currentResource.getExtension().equalsIgnoreCase("PALT")) {
          // found the color palette file - need to extract it and read the colors

          palette = extractPalette(resources[i]);

          break;
        }
      }

      int numColors = palette.length;
      if (numColors <= 0) {
        ErrorLogger.log("[VIEWER_IFF_SPR] Invalid number of colors: " + numColors);
        return null;
      }

      ImageResource[] images = new ImageResource[numImages];
      for (int i = 0; i < numImages; i++) {
        fm.seek(offsets[i]);

        ImageResource image = null;
        if (version == 502 || version == 503 || version == 504 || version == 505) {
          image = readImageSPR(fm, palette);
        }
        else if (version == 1000) {
          image = readImageSPR2(fm, palette);
        }

        if (image == null) {
          ErrorLogger.log("[VIEWER_IFF_SPR] Error reading image at index " + i);
          return null;
        }
        images[i] = image;
      }

      fm.close();

      // Now covert all the images into a single image with frames in it, then return it
      for (int i = 0; i < numImages; i++) {
        ImageResource currentImage = images[i];
        if (i != 0) {
          currentImage.setPreviousFrame(images[i - 1]);
        }
        if (i != numImages - 1) {
          currentImage.setNextFrame(images[i + 1]);
        }

        currentImage.setManualFrameTransition(true);
      }

      // So we can display the current image number by default, when loading the next image (ie always show image number 3, not always resetting to 0 for each preview)
      ImageResource imageResource = images[0];
      int currentImageNumber = Settings.getInt("PreviewPanel_Image_CurrentFrame");
      if (currentImageNumber > 0 && currentImageNumber < numImages) {
        imageResource = images[currentImageNumber];
      }
      else {
        currentImageNumber = 0;
        Settings.set("PreviewPanel_Image_CurrentFrame", 0);
      }

      imageResource.addProperty("FrameCount", "" + numImages);

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