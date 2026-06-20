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
import org.watto.datatype.ImageResource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.helper.ImageFormatReader;
import org.watto.ge.helper.ImageSwizzler;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.io.FileManipulator;
import org.watto.io.buffer.ByteBuffer;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_GIM_MIG extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_GIM_MIG() {
    super("GIM_MIG", "Playstation GIM Image");
    setExtensions("gim", "mig");

    setGames("Playstation 2", "Playstation Portable");
    setPlatforms("PS2", "PSP");
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

      /*
      ArchivePlugin plugin = Archive.getReadPlugin();
      if (plugin instanceof _Plugin_XXX) {
        rating += 50;
      }
      else if (!(plugin instanceof AllFilesPlugin)) {
        return 0;
      }
      */

      if (FieldValidator.checkExtension(fm, extensions)) {
        rating += 25;
      }

      // 4 - Header
      if (fm.readString(8).equals("MIG.00.1")) {
        rating += 50;
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

      // 4 - Header ("MIG.")
      // 4 - Version ("00.1")
      // 4 - Platform ("PSP" + null)
      // 4 - null
      fm.skip(16);

      int[] palette = null;

      while (fm.getOffset() < arcSize) {
        long startOffset = fm.getOffset();

        // 2 - Block Type (1=block, 2=root, 3=picture, 4=image, 5=palette, 6=animation, 255=File Info)
        int blockType = fm.readShort();

        // 2 - null
        // 4 - Block Length (including child blocks and these header fields)
        fm.skip(6);

        // 4 - Next Block Offset (relative to the start of this block)
        long nextOffset = startOffset + fm.readInt();
        FieldValidator.checkOffset(nextOffset, arcSize + 1); // allow the last offset to be the arcSize

        // 4 - Block Data Offset (relative to the start of this block)
        long dataOffset = startOffset + fm.readInt();
        FieldValidator.checkOffset(dataOffset, arcSize + 1); // allow the last offset to be the arcSize

        // X - Block Data
        if (blockType == 5) {
          // Palette

          startOffset = fm.getOffset();

          // 4 - Palette Header Length (48)
          int headerLength = fm.readInt();
          FieldValidator.checkLength(headerLength, arcSize);

          // 2 - Palette Format (refer to image formats above)
          int paletteFormat = fm.readShort();

          // 2 - null
          // 2 - Number of Colors? (256)
          // 2 - Unknown (1)
          // 2 - Palette Width (16)
          // 2 - Palette Height (16)
          // 2 - Unknown (1)
          // 2 - Unknown (2)
          // 4 - null
          // 4 - Offset Data Offset (relative to the start of the Image Block) (48)
          // 4 - Palette Data Offset (relative to the start of the Palette Block) (64)
          // 4 - Palette Block Length (block data only)
          // 4 - null
          // 2 - Unknown (2)
          // 2 - Unknown (1)
          // 2 - Unknown (3)
          // 2 - Number of Palettes? (1)
          fm.relativeSeek(startOffset + headerLength);

          // 4 - Palette Data Offset (relative to the start of the Palette Block) (64)
          long paletteOffset = startOffset + fm.readInt();
          FieldValidator.checkOffset(paletteOffset, arcSize);

          fm.relativeSeek(paletteOffset);

          // X - Palette Data
          if (paletteFormat == 0) {
            // RGB565
            palette = ImageFormatReader.readBGR565(fm, 256, 1).getImagePixels();
          }
          else if (paletteFormat == 1) {
            // RGBA5551
            palette = ImageFormatReader.readABGR1555(fm, 256, 1).getImagePixels();
          }
          else if (paletteFormat == 2) {
            // RGBA4444
            palette = ImageFormatReader.readABGR4444(fm, 256, 1).getImagePixels();
          }
          else if (paletteFormat == 3) {
            // RGBA
            palette = ImageFormatReader.readABGR(fm, 256, 1).getImagePixels();
          }
          else {
            ErrorLogger.log("[Viewer_GIM_MIG] Unknown Palette Format: " + paletteFormat);
            return null;
          }

        }
        else if (blockType == 4) {
          // Image

          startOffset = fm.getOffset();

          // 4 - Image Header Length (48)
          int headerLength = fm.readInt();
          FieldValidator.checkLength(headerLength, arcSize);

          // 2 - Image Format (refer to image formats above)
          int imageFormat = fm.readShort();

          // 2 - Pixel Order (0=normal, 1=Swizzled)
          int pixelOrder = fm.readShort();

          // 2 - Image Width
          int width = fm.readShort();
          FieldValidator.checkWidth(width);

          // 2 - Image Height
          int height = fm.readShort();
          FieldValidator.checkHeight(height);

          // 2 - Bits Per Pixel
          // 2 - Image Width Alignment
          // 2 - Image Height Alignment
          // 2 - Number of Dimensions (2)
          // 4 - null
          // 4 - Offset Data Offset (relative to the start of the Image Block) (48)
          // 4 - Plane Data Offset (relative to the start of the Image Block) (64)
          // 4 - Image Block Length (block data only)
          // 4 - Plane Mask
          // 2 - Level Type (0=generic, 1=mipmap reduced size, 2=mipmap fixed size, 3=sequence)
          // 2 - Level or Mipmap Count
          // 2 - Frame Type
          fm.skip(34);

          // 2 - Number of Frames
          int numFrames = fm.readShort();
          if (numFrames != 1) {
            ErrorLogger.log("[Viewer_GIM_MIG] Only rendering the first frame, but there are actually: " + numFrames);
          }

          fm.relativeSeek(startOffset + headerLength);

          // 4 - Frame Offset (relative to the start of the Image Block) (64)
          long frameOffset = startOffset + fm.readInt();
          FieldValidator.checkOffset(frameOffset, arcSize);

          fm.relativeSeek(frameOffset);

          // X - Image Data (in the format specified in the header)
          ImageResource imageResource = null;

          if (imageFormat == 0) {
            // RGB565
            if (pixelOrder == 1) {
              int numPixels = width * height * 2;
              byte[] pixelBytes = fm.readBytes(numPixels);
              pixelBytes = ImageSwizzler.unswizzlePSP32Bit(pixelBytes, width, height); // don't have a 16-bit version
              fm.close();
              fm = new FileManipulator(new ByteBuffer(pixelBytes));
            }
            imageResource = ImageFormatReader.readBGR565(fm, width, height);
          }
          else if (imageFormat == 1) {
            // RGBA5551
            if (pixelOrder == 1) {
              int numPixels = width * height * 2;
              byte[] pixelBytes = fm.readBytes(numPixels);
              pixelBytes = ImageSwizzler.unswizzlePSP32Bit(pixelBytes, width, height); // don't have a 16-bit version
              fm.close();
              fm = new FileManipulator(new ByteBuffer(pixelBytes));
            }
            imageResource = ImageFormatReader.readABGR1555(fm, width, height);
          }
          else if (imageFormat == 2) {
            // RGBA4444
            if (pixelOrder == 1) {
              int numPixels = width * height * 2;
              byte[] pixelBytes = fm.readBytes(numPixels);
              pixelBytes = ImageSwizzler.unswizzlePSP32Bit(pixelBytes, width, height); // don't have a 16-bit version
              fm.close();
              fm = new FileManipulator(new ByteBuffer(pixelBytes));
            }
            imageResource = ImageFormatReader.readABGR4444(fm, width, height);
          }
          else if (imageFormat == 3) {
            // RGBA
            if (pixelOrder == 1) {
              int numPixels = width * height * 4;
              byte[] pixelBytes = fm.readBytes(numPixels);
              pixelBytes = ImageSwizzler.unswizzlePSP32Bit(pixelBytes, width, height);
              fm.close();
              fm = new FileManipulator(new ByteBuffer(pixelBytes));
            }
            imageResource = ImageFormatReader.readABGR(fm, width, height);
          }
          else if (imageFormat == 4) {
            // 4bit paletted
            if (pixelOrder == 1) {
              int numPixels = width * height / 2;
              byte[] pixelBytes = fm.readBytes(numPixels);
              pixelBytes = ImageSwizzler.unswizzlePSP4Bit(pixelBytes, width, height);
              fm.close();
              fm = new FileManipulator(new ByteBuffer(pixelBytes));
            }
            imageResource = ImageFormatReader.read4BitPaletted(fm, width, height, palette);
          }
          else if (imageFormat == 5) {
            // 8bit paletted
            if (pixelOrder == 1) {
              int numPixels = width * height;
              byte[] pixelBytes = fm.readBytes(numPixels);
              pixelBytes = ImageSwizzler.unswizzlePSP8Bit(pixelBytes, width, height);
              fm.close();
              fm = new FileManipulator(new ByteBuffer(pixelBytes));
            }
            imageResource = ImageFormatReader.read8BitPaletted(fm, width, height, palette);
          }
          else if (imageFormat == 6) {
            // 4bit paletted (with alpha)
            if (pixelOrder == 1) {
              int numPixels = width * height / 2;
              byte[] pixelBytes = fm.readBytes(numPixels);
              pixelBytes = ImageSwizzler.unswizzlePSP4Bit(pixelBytes, width, height);
              fm.close();
              fm = new FileManipulator(new ByteBuffer(pixelBytes));
            }
            imageResource = ImageFormatReader.read4BitPaletted(fm, width, height, palette);
          }
          else if (imageFormat == 7) {
            // 8bit paletted (with alpha)
            if (pixelOrder == 1) {
              int numPixels = width * height;
              byte[] pixelBytes = fm.readBytes(numPixels);
              pixelBytes = ImageSwizzler.unswizzlePSP8Bit(pixelBytes, width, height);
              fm.close();
              fm = new FileManipulator(new ByteBuffer(pixelBytes));
            }
            imageResource = ImageFormatReader.read8BitPaletted(fm, width, height, palette);
          }
          else if (imageFormat == 8) {
            // DXT1
            if (pixelOrder == 1) {
              int numPixels = width * height / 2;
              byte[] pixelBytes = fm.readBytes(numPixels);
              pixelBytes = ImageSwizzler.unswizzlePSP4Bit(pixelBytes, width, height);
              fm.close();
              fm = new FileManipulator(new ByteBuffer(pixelBytes));
            }
            imageResource = ImageFormatReader.readDXT1(fm, width, height);
          }
          else if (imageFormat == 9) {
            // DXT3
            if (pixelOrder == 1) {
              int numPixels = width * height;
              byte[] pixelBytes = fm.readBytes(numPixels);
              pixelBytes = ImageSwizzler.unswizzlePSP8Bit(pixelBytes, width, height);
              fm.close();
              fm = new FileManipulator(new ByteBuffer(pixelBytes));
            }
            imageResource = ImageFormatReader.readDXT3(fm, width, height);
          }
          else if (imageFormat == 10) {
            // DXT5
            if (pixelOrder == 1) {
              int numPixels = width * height;
              byte[] pixelBytes = fm.readBytes(numPixels);
              pixelBytes = ImageSwizzler.unswizzlePSP8Bit(pixelBytes, width, height);
              fm.close();
              fm = new FileManipulator(new ByteBuffer(pixelBytes));
            }
            imageResource = ImageFormatReader.readDXT5(fm, width, height);
          }
          else if (imageFormat == 264) {
            // DXT1EXT
            if (pixelOrder == 1) {
              int numPixels = width * height / 2;
              byte[] pixelBytes = fm.readBytes(numPixels);
              pixelBytes = ImageSwizzler.unswizzlePSP4Bit(pixelBytes, width, height);
              fm.close();
              fm = new FileManipulator(new ByteBuffer(pixelBytes));
            }
            imageResource = ImageFormatReader.readDXT1(fm, width, height);
          }
          else if (imageFormat == 265) {
            // DXT3EXT
            if (pixelOrder == 1) {
              int numPixels = width * height;
              byte[] pixelBytes = fm.readBytes(numPixels);
              pixelBytes = ImageSwizzler.unswizzlePSP8Bit(pixelBytes, width, height);
              fm.close();
              fm = new FileManipulator(new ByteBuffer(pixelBytes));
            }
            imageResource = ImageFormatReader.readDXT3(fm, width, height);
          }
          else if (imageFormat == 266) {
            // DXT5EXT
            if (pixelOrder == 1) {
              int numPixels = width * height;
              byte[] pixelBytes = fm.readBytes(numPixels);
              pixelBytes = ImageSwizzler.unswizzlePSP8Bit(pixelBytes, width, height);
              fm.close();
              fm = new FileManipulator(new ByteBuffer(pixelBytes));
            }
            imageResource = ImageFormatReader.readDXT5(fm, width, height);
          }
          else {
            ErrorLogger.log("[Viewer_GIM_MIG] Unknown Image Format: " + imageFormat);
            return null;
          }

          fm.close();
          return imageResource;

        }
        else {
          // don't care about anything else, just read the next block
        }

        fm.relativeSeek(nextOffset);

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
  }

}