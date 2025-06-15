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
import org.watto.ge.plugin.archive.Plugin_ZIP_PK;
import org.watto.io.FileManipulator;
import org.watto.io.converter.ByteConverter;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_ZIP_PK_TEX_TEXL001 extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_ZIP_PK_TEX_TEXL001() {
    super("ZIP_PK_TEX_TEXL001", "ZIP_PK_TEX_TEXL001 Image");
    setExtensions("tex");

    setGames("Encore Classic Puzzle and Board Games");
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
      if (plugin instanceof Plugin_ZIP_PK) {
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
      if (fm.readString(4).equals("tex ")) {
        rating += 25;
      }
      else {
        rating = 0;
      }

      // 4 - Header
      String header = fm.readString(4);
      if (header.equals("L001") || header.equals("1001") || header.equals("0001")) {
        rating += 25;
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
  Reads a resource from the FileManipulator, and generates a Thumbnail for it (generally, only
  an Image ViewerPlugin will do this, but others can do it if they want). The FileManipulator is
  an extracted temp file, not the original archive!
  **********************************************************************************************
  **/

  @Override
  public ImageResource readThumbnail(FileManipulator fm) {
    ImageResource imageResource = readImage(fm);

    // remove all the frames except the first one
    imageResource.setNextFrame(null);
    imageResource.setPreviousFrame(null);

    return imageResource;

  }

  /**
  **********************************************************************************************
  Does the actual reading for both read() methods.
  **********************************************************************************************
  **/
  @SuppressWarnings("unused")
  public ImageResource readImage(FileManipulator fm) {
    try {

      long arcSize = fm.getLength();

      // 8 - Header ("tex L001")
      fm.skip(8);

      // COLOR PALETTE
      // for each color (256)
      //   3 - Color (RGB)
      int[] palette = ImageFormatReader.readPaletteRGB(fm, 256);

      // 2 - Number of Images? (1)
      fm.skip(2);

      // 2 - Number of Frames? (10)
      int numFrames = fm.readShort();
      FieldValidator.checkRange(numFrames, 1, 1024); // guess

      ImageResource[] images = new ImageResource[numFrames];

      int maxWidth = 0;
      int maxHeight = 0;
      int[] widths = new int[numFrames];
      int[] heights = new int[numFrames];
      int[] lefts = new int[numFrames];
      int[] tops = new int[numFrames];

      // for each frame
      for (int f = 0; f < numFrames; f++) {

        //System.out.println("Frame " + f + " of " + numFrames + "\t" + fm.getOffset());

        // 2 - Image Width
        short width = fm.readShort();
        FieldValidator.checkWidth(width + 1); // allow 0 width
        widths[f] = width;

        // 2 - Image Height
        short height = fm.readShort();
        FieldValidator.checkHeight(height + 1); // allow 0 width
        heights[f] = height;

        // 2 - Unknown (0)
        short unknown1 = fm.readShort();
        // 2 - Unknown (0)
        short unknown2 = fm.readShort();
        //fm.skip(4);

        // 2 - Left Position
        int leftOffset = fm.readShort();
        FieldValidator.checkWidth(leftOffset + 1);
        lefts[f] = leftOffset;

        // 2 - Top Position
        int topOffset = fm.readShort();
        FieldValidator.checkHeight(topOffset + 1);
        tops[f] = topOffset;

        //fm.skip(8);
        //System.out.println(width + "\t" + height + "\t" + unknown1 + "\t" + unknown2 + "\t" + leftOffset + "\t" + topOffset);

        if (width + leftOffset > maxWidth) {
          maxWidth = width + leftOffset;
        }
        if (height + topOffset > maxHeight) {
          maxHeight = height + topOffset;
        }

        int numPixels = width * height;
        int[] pixels = new int[numPixels];

        int outPos = 0;
        for (int h = 0; h < height; h++) {
          // 2 - Number of Bytes for the Line Data
          short byteCount = fm.readShort();

          //System.out.println((fm.getOffset() - 2) + "\t" + byteCount);

          FieldValidator.checkLength(byteCount, arcSize);

          int readBytes = 0;
          while (readBytes < byteCount) {

            // 1 - Control Character
            int control = fm.readByte();
            readBytes++;

            if (control == 1) {
              // 2 - Number of Transparent Pixels
              short count = fm.readShort();
              FieldValidator.checkPositive(count);
              readBytes += 2;

              for (int c = 0; c < count; c++) {
                pixels[outPos] = 0;
                outPos++;
              }

            }
            else if (control == 2) {
              // 2 - Number of Pixels to Copy (paletted indexes)
              short count = fm.readShort();
              FieldValidator.checkPositive(count);
              readBytes += 2;

              // X - Pixels (1 byte per pixel)
              for (int c = 0; c < count; c++) {
                pixels[outPos] = palette[ByteConverter.unsign(fm.readByte())];
                outPos++;
              }
              readBytes += count;

            }
            else if (control == 4) {
              // 2 - Number of Pixels to copy (2-byte Colors)
              short count = fm.readShort();
              FieldValidator.checkPositive(count);
              readBytes += 2;

              // X - Pixels (2 byte per pixel)
              for (int c = 0; c < count; c++) {
                // 1 - Palette Index
                int color = palette[ByteConverter.unsign(fm.readByte())];
                // 1 - Alpha
                int alpha = ByteConverter.unsign(fm.readByte());

                pixels[outPos] = (color & 16777215) | (alpha << 24);
                outPos++;
              }
              readBytes += count * 2;

            }
            else if (control == 8) {
              // 2 - Number of Pixels to repeat
              short count = fm.readShort();
              FieldValidator.checkPositive(count);
              readBytes += 2;

              int color = palette[ByteConverter.unsign(fm.readByte())];
              readBytes++;

              for (int c = 0; c < count; c++) {
                pixels[outPos] = color;
                outPos++;
              }

            }
            else if (control == 16) { // not sure how to apply alpha here
              // 2 - Number of Pixels to Copy (paletted indexes)
              short count = fm.readShort();
              FieldValidator.checkPositive(count);
              readBytes += 2;

              // X - Pixels (1 byte per pixel)
              for (int c = 0; c < count; c++) {
                pixels[outPos] = palette[ByteConverter.unsign(fm.readByte())];
                outPos++;
              }
              readBytes += count;

            }
            else if (control == 32) { // not sure how to apply alpha here
              // 2 - Number of Pixels to Copy (paletted indexes)
              short count = fm.readShort();
              FieldValidator.checkPositive(count);
              readBytes += 2;

              // X - Pixels (1 byte per pixel)
              for (int c = 0; c < count; c++) {
                pixels[outPos] = palette[ByteConverter.unsign(fm.readByte())];
                outPos++;
              }
              readBytes += count;

            }
            else {
              ErrorLogger.log("[Viewer_ZIP_PK_TEX_TEXL001] Unknown control character: " + control);
              return null;
            }
          }

        }

        ImageResource imageResource = new ImageResource(pixels, width, height);
        images[f] = imageResource;
      }

      //System.out.println("endpos" + "\t" + fm.getOffset());

      // Try to work out if the last frame is supposed to be the background piece, and all the other images just fit in it.
      // We guess this if the last image is the max size, and all the other images are significantly smaller.
      boolean commonFrame = false;
      if (numFrames == 1) {
        // don't analyze
      }
      else {
        if (maxWidth == (widths[numFrames - 1] + lefts[numFrames - 1]) && maxHeight == (heights[numFrames - 1] + tops[numFrames - 1])) {
          // last frame is the largest

          // check if all the other frames are significantly smaller
          commonFrame = true;
          for (int f = 0; f < numFrames - 1; f++) { // -1 to skip the last frame
            if (lefts[f] > 5 && tops[f] > 5 && maxWidth - widths[f] > 20 && maxHeight - heights[f] > 20) {
              // much smaller
            }
            else {
              // kinda similar sized, so probably not supposed to retain the last frame
              commonFrame = false;
              break;
            }
          }

        }
      }

      int[] backgroundFrame = null;
      if (commonFrame) {
        // take the last frame, resize it, and use that as the basis for all other frames.
        //ImageResource lastFrame = images[numFrames - 1];

        int width = widths[numFrames - 1];
        int height = heights[numFrames - 1];
        int left = lefts[numFrames - 1];
        int top = tops[numFrames - 1];

        if (width != maxWidth || height != maxHeight) {
          // needs to be resized

          int[] oldPixels = images[numFrames - 1].getImagePixels();

          int maxPixels = maxWidth * maxHeight;
          int[] pixels = new int[maxPixels];

          // image is aligned to the top-left
          int leftPos = maxWidth - (width + left);
          int topPos = maxHeight - (height + top);

          int readPos = 0;
          int outPos = (topPos + top) * maxWidth;
          //int outPos = 0;
          for (int h = 0; h < height; h++) {
            outPos += left; // left padding (according to frame details)

            for (int w = 0; w < width; w++) {
              pixels[outPos] = oldPixels[readPos];
              outPos++;
              readPos++;
            }

            outPos += leftPos; // add the padding to the right of the resized line
          }

          backgroundFrame = pixels;
        }

        // shrink the frames array to remove the common one at the end
        numFrames--;
        ImageResource[] oldFrames = images;
        images = new ImageResource[numFrames];
        System.arraycopy(oldFrames, 0, images, 0, numFrames);
      }

      // resize the frames to the maxWidth/maxHeight
      int maxPixels = maxWidth * maxHeight;
      for (int f = 0; f < numFrames; f++) {
        int width = widths[f];
        int height = heights[f];
        int left = lefts[f];
        int top = tops[f];

        if (backgroundFrame != null || (width != maxWidth || height != maxHeight)) {
          // needs to be resized (or we need to apply a background image)

          int[] oldPixels = images[f].getImagePixels();
          int[] pixels = new int[maxPixels];

          // if we have a background frame, copy it across here before we apply the new pixels on top of it
          if (backgroundFrame != null) {
            System.arraycopy(backgroundFrame, 0, pixels, 0, maxPixels);
          }

          // image is aligned to the top-left
          int leftPos = maxWidth - (width + left);
          int topPos = maxHeight - (height + top);

          int readPos = 0;
          //int outPos = (topPos + top) * maxWidth;
          int outPos = top * maxWidth;
          //int outPos = 0;
          for (int h = 0; h < height; h++) {
            outPos += left; // left padding (according to frame details)

            for (int w = 0; w < width; w++) {
              pixels[outPos] = oldPixels[readPos];
              outPos++;
              readPos++;
            }

            outPos += leftPos; // add the padding to the right of the resized line
          }

          images[f] = new ImageResource(pixels, maxWidth, maxHeight);
          //images[f].setPixels(pixels);
        }
      }

      ImageResource imageResource = images[0];

      // set the transitions
      if (numFrames != 1) {
        for (int f = 0; f < numFrames; f++) {
          if (f == 0) {
            images[f].setPreviousFrame(images[numFrames - 1]);
            images[f].setNextFrame(images[f + 1]);
          }
          else if (f == numFrames - 1) {
            images[f].setPreviousFrame(images[f - 1]);
            images[f].setNextFrame(images[0]);
          }
          else {
            images[f].setPreviousFrame(images[f - 1]);
            images[f].setNextFrame(images[f + 1]);
          }
        }
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

}