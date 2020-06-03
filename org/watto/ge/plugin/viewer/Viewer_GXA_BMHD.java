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
import org.watto.datatype.ImageResource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.io.FileManipulator;
import org.watto.io.converter.ByteConverter;
import org.watto.io.converter.IntConverter;
import org.watto.io.converter.ShortConverter;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_GXA_BMHD extends ViewerPlugin {

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Viewer_GXA_BMHD() {
    super("GXA_BMHD", "Redguard: Elder Scrolls Adventures GXA Image");
    setExtensions("gxa");

    setGames("Redguard: Elder Scrolls Adventures");
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

      if (FieldValidator.checkExtension(fm, extensions)) {
        rating += 25;
      }
      else {
        return 0;
      }

      // 4 - Header
      if (fm.readString(4).equals("BMHD")) {
        rating += 50;
      }
      else {
        rating = 0;
      }

      // 4 - Color Palette Offset (34) (relative to the end of this field)
      if (FieldValidator.checkOffset(IntConverter.changeFormat(fm.readInt()), 256)) {
        rating += 5;
      }

      fm.skip(32);

      // 2 - Number Of Colors
      if (FieldValidator.checkNumColors(ShortConverter.changeFormat(fm.readShort()))) {
        rating += 5;
      }

      // 4 - Header
      if (fm.readString(4).equals("BPAL")) {
        rating += 5;
      }

      // 4 - Color Palette Length (768)
      if (FieldValidator.checkLength(IntConverter.changeFormat(fm.readInt()), 769)) {
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

      long arcSize = fm.getLength();

      // 4 - Header (BMHD)
      // 4 - Color Palette Offset (34) (relative to the end of this field) (BIG)
      // 32 - Description (GXlib image conversion          )
      fm.skip(40);

      // 2 - Number Of Frames (LITTLE)
      short numFrames = fm.readShort();
      FieldValidator.checkNumColors(numFrames);

      // 4 - Header (BPAL)
      // 4 - Color Palette Length (768)
      fm.skip(8);

      // X - Palette
      int numColors = 256;
      int[] palette = new int[numColors];
      for (int i = 0; i < numColors; i++) {
        // 1 - Red
        // 1 - Green
        // 1 - Blue
        int r = ByteConverter.unsign(fm.readByte());
        int g = ByteConverter.unsign(fm.readByte());
        int b = ByteConverter.unsign(fm.readByte());

        palette[i] = ((255 << 24) | (r << 16) | (g << 8) | (b));
      }

      // 4 - Header (BBMP)
      fm.skip(4);

      // 4 - Pixel Data Length (BIG) (including all the fields after this one)
      int pixelLength = IntConverter.changeFormat(fm.readInt());
      FieldValidator.checkLength(pixelLength, arcSize);

      ImageResource[] frames = new ImageResource[numFrames];
      for (int i = 0; i < numFrames; i++) {
        // 2 - Number of Colors (256) (BIG)
        fm.skip(2);

        // 2 - Image Width (LITTLE)
        short width = fm.readShort();
        FieldValidator.checkWidth(width);

        // 2 - Image Height (LITTLE)
        short height = fm.readShort();
        FieldValidator.checkHeight(height);

        // 4 - null
        fm.skip(4);

        // 4 - RLE Encoding? (0=no encoding, 1=encoding)
        int encodedRLE = fm.readInt();

        // 4 - null
        fm.skip(4);

        if (encodedRLE == 0) {
          // No Encoding

          // X - Pixels
          int numPixels = width * height;
          int[] pixels = new int[numPixels];
          for (int p = 0; p < numPixels; p++) {
            // 1 - Color Palette Index
            pixels[p] = palette[ByteConverter.unsign(fm.readByte())];
          }

          // X - Pixels
          ImageResource imageResource = new ImageResource(pixels, width, height);
          frames[i] = imageResource;
        }
        else if (encodedRLE == 1) {
          // RLE Encoding (still a work in progress)
          /*
          
          // 4 - Length of Encoded Data (LITTLE)
          int lengthRLE = fm.readInt();
          FieldValidator.checkLength(lengthRLE, arcSize);
          
          int numPixels = width * height;
          int[] pixels = new int[numPixels + 100]; // TODO +100 for testing only
          int arrayPos = 0;
          
          int repeatByte = ByteConverter.unsign(fm.readByte());
          while (arrayPos < numPixels) {
            if ((repeatByte & 128) == 128) {
              int nextByte = ByteConverter.unsign(fm.readByte());
              if (nextByte == 0) {
                // found an RLE block
                repeatByte = repeatByte & 127;
          
                System.out.println(fm.getOffset() + " > Found a repeat byte of size " + repeatByte);
          
                int pixelToRepeat = ByteConverter.unsign(fm.readByte());
                for (int p = 0; p < repeatByte; p++) {
                  pixels[arrayPos + p] = pixelToRepeat;
                }
                arrayPos += repeatByte;
          
                System.out.println(fm.getOffset() + "   > now this full " + arrayPos);
          
                // now continue reading until we find a null
                nextByte = ByteConverter.unsign(fm.readByte());
                while (nextByte != 0) {
                  pixels[arrayPos] = nextByte;
                  arrayPos++;
                  nextByte = ByteConverter.unsign(fm.readByte());
                }
                // now we're at the end of the single bytes, go back to check for an RLE block
          
                System.out.println(fm.getOffset() + "   > now we've read single pixels to " + arrayPos);
          
                // Read a new repeatByte for checking
                repeatByte = ByteConverter.unsign(fm.readByte());
              }
              else {
                // not an RLE block - real pixels
                System.out.println(fm.getOffset() + "     > found a normal pixel (level 2) for pos " + arrayPos);
                pixels[arrayPos] = repeatByte;
                arrayPos++;
                repeatByte = nextByte;
                // The nextByte might be the repeatByte - go back and check whether the *new* nextByte is a null
              }
            }
            else {
              // not an RLE block - real pixels
              System.out.println(fm.getOffset() + "     > found a normal pixel (level 1) for pos " + arrayPos);
              pixels[arrayPos] = repeatByte;
              arrayPos++;
          
              // now continue reading until we find a null
              int nextByte = ByteConverter.unsign(fm.readByte());
              while (nextByte != 0) {
                pixels[arrayPos] = nextByte;
                arrayPos++;
                nextByte = ByteConverter.unsign(fm.readByte());
              }
              // now we're at the end of the single bytes, go back to check for an RLE block
          
              System.out.println(fm.getOffset() + "   > now we've read single pixels (after Level 1) to " + arrayPos);
          
              // Read a new repeatByte for checking
              repeatByte = ByteConverter.unsign(fm.readByte());
            }
          
          }
          
          System.out.println("Viewer_GXA_BMHD: Woohoo! We got it!");
          
          // Now convert all the pixel values to real colors from the palette
          for (int p = 0; p < numPixels; p++) {
            pixels[p] = palette[pixels[p]];
          }
          
          ImageResource imageResource = new ImageResource(pixels, width, height);
          frames[i] = imageResource;
          
          fm.close(); // TODO TEMP FOR TESTING ONLY
          return imageResource; // TODO TEMP FOR TESTING ONLY
          */

          fm.close();
          return null;
        }
        else {
          // Unknown
          fm.close();
          return null;
        }
      }

      if (numFrames > 1) {
        for (int i = 1; i < numFrames; i++) {
          frames[i - 1].setNextFrame(frames[i]);
        }
        frames[numFrames - 1].setNextFrame(frames[0]);
      }

      fm.close();

      return frames[0];

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