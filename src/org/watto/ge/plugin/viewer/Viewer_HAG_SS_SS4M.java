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

import java.util.Arrays;
import org.watto.ErrorLogger;
import org.watto.component.PreviewPanel;
import org.watto.component.PreviewPanel_Image;
import org.watto.datatype.Archive;
import org.watto.datatype.ImageResource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.helper.PaletteGenerator;
import org.watto.ge.plugin.AllFilesPlugin;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.ge.plugin.archive.Plugin_HAG;
import org.watto.io.FileManipulator;
import org.watto.io.converter.ByteConverter;
import org.watto.io.converter.IntConverter;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_HAG_SS_SS4M extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_HAG_SS_SS4M() {
    super("HAG_SS_SS4M", "The Riddle Of Master Lu SS Image");
    setExtensions("ss", "ssb", "ssc");

    setGames("Orion Burger",
        "Ripley's Believe It or Not!: The Riddle Of Master Lu");
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
      if (plugin instanceof Plugin_HAG) {
        rating += 50;
      }
      else if (!(plugin instanceof AllFilesPlugin)) {
        return 0;
      }

      if (FieldValidator.checkExtension(fm, extensions)) {
        rating += 25;
      }

      // 4 - Header
      String header = fm.readString(4);
      if (header.equals("SS4M") || header.equals("M4SS")) {
        rating += 50;
      }
      else {
        rating = 0;
      }

      if (fm.readInt() == 101) {
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

  @SuppressWarnings("unused")
  @Override
  public ImageResource readThumbnail(FileManipulator fm) {
    try {

      long arcSize = fm.getLength();

      // 4 - Header (SS4M)
      String headerString = fm.readString(4);
      boolean swapOrder = false;
      if (headerString.equals("M4SS")) {
        swapOrder = true;
      }

      // 4 - Unknown (101)
      fm.skip(4);

      int[] palette = null;
      ImageResource image = null;

      while (fm.getOffset() < arcSize) {
        String header = fm.readString(4);

        int blockLength = fm.readInt();
        if (swapOrder) {
          blockLength = IntConverter.changeFormat(blockLength);
        }
        blockLength -= 8;

        FieldValidator.checkLength(blockLength, arcSize);

        if (header.equals("LAP ") || header.equals(" PAL")) {
          long nextOffset = fm.getOffset() + blockLength;

          // Palette

          // 4 - Number of Colors
          int numColors = fm.readInt();
          if (swapOrder) {
            numColors = IntConverter.changeFormat(numColors);
          }
          FieldValidator.checkNumColors(numColors);

          // X - Palette (BGRA)
          //int[] paletteIn = ImageFormatReader.readPaletteBGRA(fm, numColors);
          palette = new int[256];
          Arrays.fill(palette, 0);

          if (swapOrder) {
            // swapped order
            for (int c = 0; c < numColors; c++) {

              // 1 - Palette Index
              int paletteIndex = ByteConverter.unsign(fm.readByte());

              // 3 - RGB
              int rPixel = ByteConverter.unsign(fm.readByte()) << 2;
              int gPixel = ByteConverter.unsign(fm.readByte()) << 2;
              int bPixel = ByteConverter.unsign(fm.readByte()) << 2;
              int aPixel = 255;

              //pixels[i] = ((fm.readByte() << 16) | (fm.readByte() << 8) | fm.readByte() | (((byte) 255) << 24));
              int color = ((rPixel << 16) | (gPixel << 8) | bPixel | (aPixel << 24));

              palette[paletteIndex] = color;
            }
          }
          else {
            // normal order
            for (int c = 0; c < numColors; c++) {
              // 3 - RGB
              int bPixel = ByteConverter.unsign(fm.readByte()) << 2;
              int gPixel = ByteConverter.unsign(fm.readByte()) << 2;
              int rPixel = ByteConverter.unsign(fm.readByte()) << 2;
              int aPixel = 255;

              //pixels[i] = ((fm.readByte() << 16) | (fm.readByte() << 8) | fm.readByte() | (((byte) 255) << 24));
              int color = ((rPixel << 16) | (gPixel << 8) | bPixel | (aPixel << 24));

              // 1 - Palette Index
              int paletteIndex = ByteConverter.unsign(fm.readByte());
              palette[paletteIndex] = color;
            }
          }

          fm.relativeSeek(nextOffset); // just in case
        }
        else if (header.equals("SS  ") || header.equals("  SS")) {
          long nextOffset = fm.getOffset() + blockLength;

          // Image Data

          // 4 - Unknown
          // 4 - Unknown
          // 4 - Unknown
          fm.skip(12);

          // 4 - Maximum Width
          int maxWidth = fm.readInt();
          if (swapOrder) {
            maxWidth = IntConverter.changeFormat(maxWidth);
          }
          FieldValidator.checkWidth(maxWidth + 1); // allow 0 width

          // 4 - Maximum Height
          int maxHeight = fm.readInt();
          if (swapOrder) {
            maxHeight = IntConverter.changeFormat(maxHeight);
          }
          FieldValidator.checkHeight(maxHeight + 1); // allow 0 width

          // 4 - Unknown
          // 4 - Unknown
          // 4 - Unknown
          // 4 - Unknown
          // 4 - Unknown
          // 4 - Unknown
          fm.skip(24);

          // 4 - Number of Frames
          int numFrames = fm.readInt();
          if (swapOrder) {
            numFrames = IntConverter.changeFormat(numFrames);
          }
          FieldValidator.checkRange(numFrames, 1, 1000); // guess

          int baseOffset = (int) (fm.getOffset() + (numFrames * 4));
          int[] frameOffsets = new int[numFrames];

          for (int f = 0; f < numFrames; f++) {
            // 4 - Frame Offset (relative to the start of the Image Data)
            int frameOffset = fm.readInt();
            if (swapOrder) {
              frameOffset = IntConverter.changeFormat(frameOffset);
            }
            frameOffset += baseOffset;
            FieldValidator.checkOffset(frameOffset, arcSize);
            frameOffsets[f] = frameOffset;
          }

          ImageResource[] imageFrames = new ImageResource[numFrames];

          for (int f = 0; f < numFrames; f++) {
            fm.relativeSeek(frameOffsets[f]); // just in case

            // 4 - Unknown
            // 4 - Unknown
            fm.skip(8);

            // 4 - XPos?
            // 4 - YPos?
            int xPos = fm.readInt();
            int yPos = fm.readInt();
            //System.out.println("Frame " + f + ": XPos = " + xPos + "  YPos = " + yPos);

            // 4 - Frame Width
            int width = fm.readInt();
            if (swapOrder) {
              width = IntConverter.changeFormat(width);
            }
            FieldValidator.checkWidth(width + 1); // allow 0 width

            // 4 - Frame Height
            int height = fm.readInt();
            if (swapOrder) {
              height = IntConverter.changeFormat(height);
            }
            FieldValidator.checkHeight(height + 1); // allow 0 height

            //System.out.println("\tWidth " + width + ": MaxWidth = " + maxWidth);
            //System.out.println("\tHeight " + height + ": MaxHeight = " + maxHeight);

            // 4 - Unknown
            // 4 - Unknown
            // 4 - Unknown
            // 4 - Unknown
            // 4 - Unknown
            // 4 - Unknown
            // 4 - Unknown
            // 4 - Unknown
            // 4 - Unknown
            fm.skip(36);

            // X - Frame Image Data (RLE)
            int numPixels = width * height;
            int[] pixels = new int[numPixels];

            int outPos = 0;

            // read the pixels
            while (outPos < numPixels && fm.getOffset() < arcSize) {
              int b = ByteConverter.unsign(fm.readByte());

              if (b == 0) {
                b = ByteConverter.unsign(fm.readByte());
                if (b > 0) {
                  for (int i = 0; i < b; i++) {
                    pixels[outPos] = ByteConverter.unsign(fm.readByte());
                    outPos++;
                    if (outPos >= numPixels) {
                      break;
                    }
                  }
                }
              }
              else {
                int c = ByteConverter.unsign(fm.readByte());
                for (int i = 0; i < b; i++) {
                  pixels[outPos] = c;
                  outPos++;
                  if (outPos >= numPixels) {
                    break;
                  }
                }
              }
            }

            if (palette == null) {
              // default greyscale palette for images with no palette specified?
              palette = PaletteGenerator.getGrayscale();
            }

            // apply the palette
            if (palette != null) {
              for (int i = 0; i < numPixels; i++) {
                pixels[i] = palette[pixels[i]];
              }
            }

            // create the frame
            ImageResource imageResource = new ImageResource(pixels, width, height);
            imageFrames[f] = imageResource;
          }

          // go through and set previous/next
          image = imageFrames[0];
          if (numFrames > 1) {
            for (int i = 0; i < numFrames - 1; i++) {
              imageFrames[i].setNextFrame(imageFrames[i + 1]);
            }
            for (int i = 1; i < numFrames; i++) {
              imageFrames[i].setPreviousFrame(imageFrames[i - 1]);
            }
            imageFrames[0].setPreviousFrame(imageFrames[numFrames - 1]);
            imageFrames[numFrames - 1].setNextFrame(imageFrames[0]);

            image.setManualFrameTransition(true);
          }

          fm.relativeSeek(nextOffset); // just in case
        }
        else {
          ErrorLogger.log("[Viewer_HAG_SS_SS4M] Unknown block type: " + header);
          fm.skip(blockLength);
        }

      }

      if (palette == null || image == null) {
        return null;
      }

      fm.close();

      return image;

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