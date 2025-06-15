/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2024 wattostudios
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
import org.watto.datatype.Palette;
import org.watto.datatype.PalettedImageResource;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.helper.ImageFormatReader;
import org.watto.ge.helper.PaletteManager;
import org.watto.ge.plugin.AllFilesPlugin;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.ge.plugin.archive.Plugin_PAK;
import org.watto.io.FileManipulator;
import org.watto.io.buffer.ByteBuffer;
import org.watto.io.converter.ByteConverter;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_PAK_IMB extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_PAK_IMB() {
    super("PAK_IMB", "Railroad Tycoon 2 IMB Image");
    setExtensions("imb");

    setGames("Railroad Tycoon 2");
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
      if (plugin instanceof Plugin_PAK) {
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

      // 4 - Number of Sprites
      if (FieldValidator.checkRange(fm.readInt(), 0, 1000)) { // guess max
        rating += 5;
      }

      fm.skip(4);

      // 4 - Palette Number (PALT####.PAL)
      if (FieldValidator.checkRange(fm.readInt(), 0, 1000)) { // max is about 650 based on the main PAK file
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

      ImageResource imageResource = readImage(fm);

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
  Renamed from readThumbnail so that THIS PLUGIN DOESN'T TRY TO BUILD THUMBNAILS!
  THIS IS IMPORTANT - THE THUMBNAIL GENERATION WILL BLOW JAVA HEAP OUT OF THE WATER!
  **********************************************************************************************
  **/
  public ImageResource readImage(FileManipulator fm) {
    try {

      long arcSize = fm.getLength();

      PaletteManager.clear();
      int numPaletteFiles = 0;
      int[] paletteFiles = new int[100]; // guess max

      // 4 - Number of Sprites
      int numSprites = fm.readInt();
      FieldValidator.checkRange(numSprites, 0, 1000); // guess max

      ImageResource[] imageResources = new ImageResource[numSprites];

      for (int i = 0; i < numSprites; i++) {
        // set up an image for building up the sprite from the small components
        int maxWidth = 2400;
        int maxHeight = 2400;
        int[] spriteImage = new int[maxWidth * maxHeight];
        int xCenter = maxWidth / 2;
        int yCenter = maxHeight / 2;

        int minX = 10000;
        int maxX = -10000;
        int minY = 10000;
        int maxY = -10000;

        // 4 - Sprite Type
        fm.skip(4);

        // 4 - Palette Number (PALT####.PAL)
        int paletteNumber = fm.readInt();
        FieldValidator.checkRange(paletteNumber, 0, 1000);

        // see if the palette has already been loaded
        boolean paletteLoaded = false;
        for (int p = 0; p < numPaletteFiles; p++) {
          if (paletteFiles[p] == paletteNumber) {
            paletteLoaded = true;
            break;
          }
        }
        if (!paletteLoaded) {
          readPalette(paletteNumber);
          paletteFiles[numPaletteFiles] = paletteNumber;
          numPaletteFiles++;
        }

        int[] palette = PaletteManager.getCurrentPalette().getPalette();

        // 52 - null Padding to offset 64
        // 4 - Unknown
        fm.skip(56);

        // 4 - Number of Blocks
        int numBlocks = fm.readInt();
        FieldValidator.checkRange(numBlocks, 0, 1000); // guess max

        for (int b = 0; b < numBlocks; b++) {
          // 4 - Additional Info Size
          int additionalSize = fm.readInt();

          // 2 - Number of Sub Sprites
          int numSubSprites = fm.readShort();

          if (numSubSprites > 1) {
            fm.skip(additionalSize);
          }

          for (int s = 0; s < numSubSprites; s++) {
            // 1 - Unknown
            // 4 - Unknown
            // 4 - Unknown
            fm.skip(9);

            // 4 - X Offset
            int xOffset = fm.readInt();

            // 4 - Y Offset
            int yOffset = fm.readInt();

            // 4 - Image Width
            int width = fm.readInt();
            FieldValidator.checkWidth(width);

            // 4 - Image Height
            int height = fm.readInt();
            FieldValidator.checkHeight(height);

            // 4 - Unknown
            fm.skip(4);

            // 4 - Image Data Length
            int packedLength = fm.readInt();
            FieldValidator.checkLength(packedLength, arcSize);

            // X - Image Data (RLE Encoding)
            byte[] packedBytes = fm.readBytes(packedLength);
            readRLE(packedBytes, xOffset, yOffset, width, height, spriteImage, xCenter, yCenter, maxWidth);

            if (xOffset < minX) {
              minX = xOffset;
            }
            if (yOffset < minY) {
              minY = yOffset;
            }
            int maxXOffset = xOffset + width;
            int maxYOffset = yOffset + height;
            if (maxXOffset > maxX) {
              maxX = maxXOffset;
            }
            if (maxYOffset > maxY) {
              maxY = maxYOffset;
            }
          }
        }

        // When we're here, we've built up the image from all the pieces, so we need to resize it
        int startX = minX + xCenter;
        int endX = maxX + xCenter;
        int actualWidth = endX - startX;

        int startY = minY + yCenter;
        int endY = maxY + yCenter;
        int actualHeight = endY - startY;

        int imageSize = actualWidth * actualHeight;
        int[] resizedImage = new int[imageSize];
        int outPos = 0;
        for (int h = startY; h < endY; h++) {
          for (int w = startX; w < endX; w++) {
            resizedImage[outPos] = spriteImage[h * maxWidth + w];
            outPos++;
          }
        }

        imageResources[i] = new PalettedImageResource(resizedImage, actualWidth, actualHeight, palette);

      }

      fm.close();

      // set the previous/next images
      ImageResource imageResource = imageResources[0];
      if (numSprites > 1) {
        imageResource.setManualFrameTransition(true);

        for (int i = 0; i < numSprites; i++) {
          if (i == 0) {
            imageResources[i].setNextFrame(imageResources[i + 1]);
            imageResources[i].setPreviousFrame(imageResources[numSprites - 1]);
          }
          else if (i == numSprites - 1) {
            imageResources[i].setNextFrame(imageResources[0]);
            imageResources[i].setPreviousFrame(imageResources[i - 1]);
          }
          else {
            imageResources[i].setNextFrame(imageResources[i + 1]);
            imageResources[i].setPreviousFrame(imageResources[i - 1]);
          }
        }

      }

      return imageResource;

    }
    catch (Throwable t) {
      logError(t);
      return null;
    }
  }

  /**
  **********************************************************************************************
  Extracts a PAL resource and then gets the Palette from it
  **********************************************************************************************
  **/
  public void readPalette(int paletteNumber) {

    // find the palette file in the archive
    String paletteName = null;
    if (paletteNumber < 10) {
      paletteName = "PALT000" + paletteNumber + ".PAL";
    }
    else if (paletteNumber < 100) {
      paletteName = "PALT00" + paletteNumber + ".PAL";
    }
    else if (paletteNumber < 1000) {
      paletteName = "PALT0" + paletteNumber + ".PAL";
    }
    else {
      paletteName = "PALT" + paletteNumber + ".PAL";
    }

    Resource[] resources = Archive.getResources();
    Resource paletteResource = null;
    int numResources = resources.length;
    for (int i = 0; i < numResources; i++) {
      Resource currentResource = resources[i];
      if (currentResource.getName().equals(paletteName)) {
        // found the color palette file - need to extract it and read the colors
        paletteResource = currentResource;
        break;
      }
    }

    if (paletteResource == null) {
      return;
    }

    try {
      ByteBuffer buffer = new ByteBuffer((int) paletteResource.getLength());
      FileManipulator fm = new FileManipulator(buffer);
      paletteResource.extract(fm);

      fm.seek(0); // back to the beginning of the byte array

      // 4 - Number of Palettes
      int numPalettes = fm.readInt();
      FieldValidator.checkRange(numPalettes, 1, 100); // guess max

      // 4 - Unknown (-1)
      // 4 - Unknown (-1)
      // 4 - Unknown
      // 4 - Unknown (-1)
      // 4 - Unknown
      // 4 - Unknown
      // 4 - Unknown (-1)
      // 4 - Unknown (-1)
      // 3 - Unknown (-1)
      fm.skip(35);

      for (int p = 0; p < numPalettes; p++) {
        int[] palette = ImageFormatReader.readRGB555(fm, 1, 256).getImagePixels();
        palette[0] = 0;
        PaletteManager.addPalette(new Palette(palette));
      }

      fm.close();

    }
    catch (Throwable t) {
      logError(t);
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @SuppressWarnings("unused")
  public void readRLE(byte[] packedBytes, int xOffset, int yOffset, int width, int height, int[] spriteImage, int xCenter, int yCenter, int maxWidth) {
    int imageSize = width * height;
    byte[] result = new byte[imageSize];
    int packedLength = packedBytes.length;
    int position = 0;
    int destPos = 0;
    boolean finish = false;
    try {
      while (!finish) {
        int firstByte;
        int lineStartPos = destPos;
        //if (((firstByte = SpriteUnpacker.readByte(source, position++)) & 0x80) > 0) {
        if (((firstByte = ByteConverter.unsign(packedBytes[position++])) & 0x80) > 0) {
          ++position;
        }
        boolean lineFinished = false;
        while (!lineFinished && !finish) {
          int controlByte;
          if (position >= packedLength) {
            finish = true;
            continue;
          }
          //if (((controlByte = SpriteUnpacker.readByte(source, position++)) & 0x80) > 0) {
          if (((controlByte = ByteConverter.unsign(packedBytes[position++])) & 0x80) > 0) {
            int blockSize;
            if ((controlByte & 0x40) > 0) {
              int skipSize = controlByte & 0x3F;
              if (skipSize == 0) {
                finish = true;
                continue;
              }
              destPos += skipSize;
              continue;
            }
            if ((controlByte & 0x20) == 0) {
              blockSize = controlByte & 7;
              if (blockSize == 0) {
                //blockSize = SpriteUnpacker.readByte(source, position++);
                blockSize = ByteConverter.unsign(packedBytes[position++]);
              }
              destPos += blockSize;
              continue;
            }
            blockSize = controlByte & 0x1F;
            if (blockSize == 0) {
              //blockSize = SpriteUnpacker.readByte(source, position++);
              blockSize = ByteConverter.unsign(packedBytes[position++]);
            }
            if (position + blockSize > packedLength) {
              finish = true;
              break;
            }
            int i = 0;
            while (i < blockSize) {
              if (position >= packedLength || destPos >= imageSize) {
                finish = true;
                break;
              }
              //byte val;
              //result[destPos++] = (val = packedBytes[position++]) == 0 ? (byte) 0 : (byte) val;
              result[destPos++] = packedBytes[position++];
              ++i;
            }
            continue;
          }
          if (controlByte == 0) {
            destPos = lineStartPos + width;
            lineFinished = true;
            continue;
          }
          int i = 0;
          while (i < controlByte) {
            if (position >= packedLength || destPos >= imageSize) {
              finish = true;
              break;
            }
            result[destPos++] = packedBytes[position++];
            ++i;
          }
        }
      }
    }
    catch (Throwable t) {
      ErrorLogger.log(t);
    }

    // now we've unpacked the sprite, we need to copy it into the image at the right place

    int startX = xCenter + xOffset;
    int endX = startX + width;
    int startY = yCenter + yOffset;
    int endY = startY + height;
    int readPos = 0;

    for (int h = startY; h < endY; h++) {
      for (int w = startX; w < endX; w++) {
        int value = ByteConverter.unsign(result[readPos]);
        if (value != 0) { // first palette index = transparent
          spriteImage[h * maxWidth + w] = value;
        }
        readPos++;
      }
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