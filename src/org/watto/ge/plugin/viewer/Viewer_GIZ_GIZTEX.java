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
import org.watto.component.PreviewPanel;
import org.watto.component.PreviewPanel_Image;
import org.watto.datatype.Archive;
import org.watto.datatype.ImageResource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.helper.ImageFormatReader;
import org.watto.ge.plugin.AllFilesPlugin;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.ge.plugin.archive.Plugin_GIZ;
import org.watto.io.FileManipulator;
import org.watto.io.buffer.ByteBuffer;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_GIZ_GIZTEX extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_GIZ_GIZTEX() {
    super("GIZ_GIZTEX", "GIZ_GIZTEX Image");
    setExtensions("giz_tex");

    setGames("Rails Across America");
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
      if (plugin instanceof Plugin_GIZ) {
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

      // 1 - Image Type (0=Raw Image , 1=JPEG Image, 2=Animation Frame)
      int imageType = fm.readByte();
      if (imageType == 0 || imageType == 1 || imageType == 2) {
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

      // 1 - Image Type (0=Raw Image , 1=JPEG Image, 2=Animation Frame)
      int imageType = fm.readByte();

      if (imageType == 1) {
        // JPEG Image

        ImageResource imageResource = new Viewer_JPEG_JFIF().readThumbnail(fm);

        // Swap red and blue, and flip vertically
        imageResource = ImageFormatReader.swapRedAndBlue(imageResource);
        imageResource = ImageFormatReader.flipVertically(imageResource);

        // 
        return imageResource;
      }
      else if (imageType == 0) {
        // Raw Image

        // 4 - Image Width
        int width = fm.readInt();
        FieldValidator.checkWidth(width);

        // 4 - Image Height
        int height = fm.readInt();
        FieldValidator.checkHeight(height);

        // 4 - Image Data Length
        fm.skip(4);

        // 4 - Bits Per Pixel (8/24)
        int bpp = fm.readInt();

        // X - Pixels
        ImageResource imageResource = null;
        if (bpp == 24) {
          if ((width * 3) % 4 == 0) {
            // read normally
            imageResource = ImageFormatReader.readBGR(fm, width, height);
            imageResource = ImageFormatReader.flipVertically(imageResource);
          }
          else {
            // each line is padded to a 4-byte multiple, so we need to strip the padding out before reading it

            int paddingSize = 4 - ((width * 3) % 4);

            int numBytes = width * height * 3;
            byte[] pixelBytes = new byte[numBytes];
            int outPos = 0;

            for (int h = 0; h < height; h++) {
              for (int w = 0; w < width; w++) {
                pixelBytes[outPos++] = fm.readByte(); // B
                pixelBytes[outPos++] = fm.readByte(); // G
                pixelBytes[outPos++] = fm.readByte(); // R
              }

              // skip the padding bytes at the end of the line
              fm.skip(paddingSize);
            }

            fm.close();
            fm = new FileManipulator(new ByteBuffer(pixelBytes));

            imageResource = ImageFormatReader.readBGR(fm, width, height);
            imageResource = ImageFormatReader.flipVertically(imageResource);
          }
        }
        else if (bpp == 8) {
          if (width % 4 == 0) {
            // read normally
            imageResource = ImageFormatReader.read8BitPaletted(fm, width, height);
            imageResource = ImageFormatReader.flipVertically(imageResource);
          }
          else {
            // each line is padded to a 4-byte multiple, so we need to strip the padding out before reading it

            int paddingSize = 4 - (width % 4);

            int numBytes = width * height;
            byte[] pixelBytes = new byte[numBytes];
            int outPos = 0;

            for (int h = 0; h < height; h++) {
              for (int w = 0; w < width; w++) {
                pixelBytes[outPos++] = fm.readByte(); // palette index
              }

              // skip the padding bytes at the end of the line
              fm.skip(paddingSize);
            }

            fm.close();
            fm = new FileManipulator(new ByteBuffer(pixelBytes));

            imageResource = ImageFormatReader.read8BitPaletted(fm, width, height);
            imageResource = ImageFormatReader.flipVertically(imageResource);
          }
        }
        else {
          ErrorLogger.log("[Viewer_GIZ_GIZTEX] Unknown Image Depth for Type 0 Image: " + bpp);
          return null;
        }

        fm.close();

        return imageResource;

      }
      else {
        // 4 - Number of Frames?
        // 4 - Next Frame?
        // 4 - Unknown
        // 4 - Unknown
        // 4 - Unknown
        // 4 - Unknown
        fm.skip(24);

        // 4 - Image Width
        int width = fm.readInt();
        FieldValidator.checkWidth(width);

        // 4 - Image Height
        int height = fm.readInt();
        FieldValidator.checkHeight(height);

        // 4 - Image Data Length
        fm.skip(4);

        // 4 - Bits Per Pixel (8/24)
        int bpp = fm.readInt();

        // X - Pixels
        ImageResource imageResource = null;
        if (bpp == 24) {
          if ((width * 3) % 4 == 0) {
            // read normally
            imageResource = ImageFormatReader.readBGR(fm, width, height);
            imageResource = ImageFormatReader.flipVertically(imageResource);
          }
          else {
            // each line is padded to a 4-byte multiple, so we need to strip the padding out before reading it

            int paddingSize = 4 - ((width * 3) % 4);

            int numBytes = width * height * 3;
            byte[] pixelBytes = new byte[numBytes];
            int outPos = 0;

            for (int h = 0; h < height; h++) {
              for (int w = 0; w < width; w++) {
                pixelBytes[outPos++] = fm.readByte(); // B
                pixelBytes[outPos++] = fm.readByte(); // G
                pixelBytes[outPos++] = fm.readByte(); // R
              }

              // skip the padding bytes at the end of the line
              fm.skip(paddingSize);
            }

            fm.close();
            fm = new FileManipulator(new ByteBuffer(pixelBytes));

            imageResource = ImageFormatReader.readBGR(fm, width, height);
            imageResource = ImageFormatReader.flipVertically(imageResource);
          }
        }
        else if (bpp == 8) {
          if (width % 4 == 0) {
            // read normally
            imageResource = ImageFormatReader.read8BitPaletted(fm, width, height);
            imageResource = ImageFormatReader.flipVertically(imageResource);
          }
          else {
            // each line is padded to a 4-byte multiple, so we need to strip the padding out before reading it

            int paddingSize = 4 - (width % 4);

            int numBytes = width * height;
            byte[] pixelBytes = new byte[numBytes];
            int outPos = 0;

            for (int h = 0; h < height; h++) {
              for (int w = 0; w < width; w++) {
                pixelBytes[outPos++] = fm.readByte(); // palette index
              }

              // skip the padding bytes at the end of the line
              fm.skip(paddingSize);
            }

            fm.close();
            fm = new FileManipulator(new ByteBuffer(pixelBytes));

            imageResource = ImageFormatReader.read8BitPaletted(fm, width, height);
            imageResource = ImageFormatReader.flipVertically(imageResource);
          }
        }
        else {
          ErrorLogger.log("[Viewer_GIZ_GIZTEX] Unknown Image Depth for Type 0 Image: " + bpp);
          return null;
        }

        fm.close();

        return imageResource;
      }

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