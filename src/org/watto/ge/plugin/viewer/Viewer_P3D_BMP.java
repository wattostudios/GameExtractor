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

import java.awt.Image;
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
import org.watto.ge.plugin.archive.Plugin_P3D;
import org.watto.io.FileManipulator;
import org.watto.io.buffer.ByteBuffer;
import org.watto.io.converter.ByteConverter;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_P3D_BMP extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_P3D_BMP() {
    super("P3D_BMP", "NHL Championship 2000 BMP Image");
    setExtensions("bmp");

    setGames("NHL Championship 2000");
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
      if (plugin instanceof Plugin_P3D) {
        rating += 50;
      }
      else if (!(plugin instanceof AllFilesPlugin)) {
        return 0;
      }

      if (FieldValidator.checkExtension(fm, extensions)) {
        rating += 24; // only 24, not 25, because we don't want to match to normal BMP images
      }
      else {
        return 0;
      }

      // 2 - Code
      if (fm.readShort() == -252) {
        rating += 5;
      }

      // 4 - File Length
      if (fm.readInt() == fm.getLength()) {
        rating += 5;
      }
      else {
        rating = 0;
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

      short width = 0;
      short height = 0;
      int[] palette = null;

      while (fm.getOffset() < arcSize) {
        // 2 - Code (12320)
        short code = fm.readShort();

        // 4 - Block Length (including these 6 bytes)
        int blockLength = fm.readInt() - 6;
        FieldValidator.checkLength(blockLength, arcSize);

        // X - Block Data
        if (code == 12353) {
          // IMAGE BLOCK

          // 1 - Name Length
          int nameLength = ByteConverter.unsign(fm.readByte());

          // X - Name
          fm.skip(nameLength);

          // 4 - Unknown (640)
          // 4 - Unknown (480)
          fm.skip(8);

          // 1 - Name Length
          nameLength = ByteConverter.unsign(fm.readByte());

          // X - Name
          fm.skip(nameLength);
        }
        else if (code == 12304) {
          // IMAGE DETAILS BLOCK

          // 1 - Name Length
          int nameLength = ByteConverter.unsign(fm.readByte());

          // X - Name
          fm.skip(nameLength);

          // 4 - null
          fm.skip(4);

          // 2 - Image Width
          width = fm.readShort();
          FieldValidator.checkWidth(width);

          // 2 - Image Height
          height = fm.readShort();
          FieldValidator.checkHeight(height);

          // 2 - Bit Depth? (8)
          fm.skip(2);
        }
        else if (code == 12305) {
          // COLOR PALETTE BLOCK

          // for each color (256)
          //   3 - Color (RGB)
          palette = ImageFormatReader.readPaletteRGB(fm, 256);

        }
        else if (code == 12306) {
          // IMAGE DATA

          // for each pixel
          //   1 - Palette Index
          if (width == 0 || height == 0 || palette == null) {
            return null;
          }

          ImageResource imageResource = ImageFormatReader.read8BitPaletted(fm, width, height, palette);
          imageResource = ImageFormatReader.flipVertically(imageResource);

          fm.close();

          return imageResource;

        }
        else if (code == 12313) {
          // COMPACT DETAILS BLOCK

          // 1 - String Length
          int nameLength = ByteConverter.unsign(fm.readByte());

          // X - String
          fm.skip(nameLength);

          // 4 - null
          fm.skip(4);

          // 2 - Image Width
          width = fm.readShort();
          FieldValidator.checkWidth(width);

          // 2 - Image Height
          height = fm.readShort();
          FieldValidator.checkHeight(height);

          // 2 - Bit Depth? (8)
          fm.skip(2);
        }
        else {
          fm.skip(blockLength);
        }
      }

      fm.close();

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

      // need to flip the image back to being upside-down
      imageResource = ImageFormatReader.flipVertically(imageResource);

      // Extract the original resource into a byte[] array, so we can reference it
      int srcLength = (int) resourceBeingReplaced.getDecompressedLength();

      byte[] srcBytes = new byte[(int) resourceBeingReplaced.getDecompressedLength()];
      FileManipulator src = new FileManipulator(new ByteBuffer(srcBytes));
      resourceBeingReplaced.extract(src);
      src.seek(0);

      // Build the new file using the src[] and adding in the new image content

      ImageManipulator im = new ImageManipulator(imageResource);
      im.changeColorCount(256);

      String filename = resourceBeingReplaced.getName();
      int filenameLength = filename.length();

      // Work out the length of each block (and the file overall)
      int imageDataBlockLength = 6 + (width * height);
      int paletteBlockLength = 6 + (256 * 3);
      int imageDetailsBlockLength = 6 + 1 + filenameLength + 10 + paletteBlockLength + imageDataBlockLength;
      int imageBlockLength = 6 + 1 + filenameLength + 8 + 1 + filenameLength + imageDetailsBlockLength;

      while (src.getOffset() < srcLength) {
        // 2 - Code
        short code = src.readShort();
        fm.writeShort(code);

        // 4 - Block Length
        int blockLength = src.readInt() - 6;

        if (code == 12353) {
          // IMAGE BLOCK
          fm.writeInt(imageBlockLength);

          // 1 - Name Length
          int nameLength = ByteConverter.unsign(src.readByte());
          fm.writeByte(nameLength);

          // X - Name
          fm.writeBytes(src.readBytes(nameLength));

          // 4 - Unknown (640)
          // 4 - Unknown (480)
          fm.writeBytes(src.readBytes(8));

          // 1 - Name Length
          nameLength = ByteConverter.unsign(src.readByte());
          fm.writeByte(nameLength);

          // X - Name
          fm.writeBytes(src.readBytes(nameLength));
        }
        else if (code == 12304) {
          // IMAGE DETAILS BLOCK
          fm.writeInt(imageDetailsBlockLength);

          // 1 - Name Length
          int nameLength = ByteConverter.unsign(src.readByte());
          fm.writeByte(nameLength);

          // X - Name
          fm.writeBytes(src.readBytes(nameLength));

          // 4 - null
          fm.writeBytes(src.readBytes(4));

          // 2 - Image Width
          src.skip(2);
          fm.writeShort(width);

          // 2 - Image Height
          src.skip(2);
          fm.writeShort(height);

          // 2 - Bit Depth? (8)
          fm.writeBytes(src.readBytes(2));
        }
        else if (code == 12305) {
          // COLOR PALETTE BLOCK
          fm.writeInt(paletteBlockLength);

          // for each color (256)
          //   3 - Color (RGB)
          src.skip(blockLength);

          ImageFormatWriter.writePaletteRGB(fm, im.getPalette());

        }
        else if (code == 12306) {
          // IMAGE DATA
          fm.writeInt(imageDataBlockLength);

          // for each pixel
          //   1 - Palette Index
          src.skip(blockLength);

          int[] pixels = im.getPixels();
          int numPixels = pixels.length;
          for (int p = 0; p < numPixels; p++) {
            fm.writeByte(pixels[p]);
          }

        }
        else if (code == 12313) {
          // COMPACT DETAILS BLOCK

          // 1 - Name Length
          int nameLength = ByteConverter.unsign(src.readByte());
          fm.writeByte(nameLength);

          // X - Name
          fm.writeBytes(src.readBytes(nameLength));

          // 4 - null
          fm.writeBytes(src.readBytes(4));

          // 2 - Image Width
          src.skip(2);
          fm.writeShort(width);

          // 2 - Image Height
          src.skip(2);
          fm.writeShort(height);

          // 2 - Bit Depth? (8)
          fm.writeBytes(src.readBytes(2));
        }
        else {
          fm.writeInt(blockLength + 6);

          fm.writeBytes(src.readBytes(blockLength));
        }

      }
      fm.close();

    }
    catch (Throwable t) {
      logError(t);
    }
  }

}