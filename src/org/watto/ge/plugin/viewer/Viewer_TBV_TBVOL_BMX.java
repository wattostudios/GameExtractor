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
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.AllFilesPlugin;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.ge.plugin.archive.Plugin_TBV_TBVOL;
import org.watto.ge.plugin.exporter.Exporter_LZO_MiniLZO;
import org.watto.io.FileManipulator;
import org.watto.io.buffer.ByteBuffer;
import org.watto.io.converter.ByteConverter;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_TBV_TBVOL_BMX extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_TBV_TBVOL_BMX() {
    super("TBV_TBVOL_BMX", "TBV_TBVOL BMX Image");
    setExtensions("bmx");

    setGames("3D Ultra Lionel Traintown");
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
      if (plugin instanceof Plugin_TBV_TBVOL) {
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

      fm.skip(12);

      // 4 - File Length
      if (FieldValidator.checkLength(fm.readInt(), fm.getLength())) {
        rating += 5;
      }

      fm.skip(48);

      // 4 - Number of Sprites
      if (FieldValidator.checkNumFiles(fm.readInt())) {
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

      // 4 - Header?
      // 4 - Unknown (4)
      fm.skip(8);

      // 4 - Unknown (1/2)
      int imageType = fm.readInt();

      // 4 - File Data Length
      // 48 - null
      fm.skip(52);

      // 4 - Number of Sprites
      int numSprites = fm.readInt();
      FieldValidator.checkNumFiles(numSprites);

      int[] offsets = new int[numSprites];
      int[] lengths = new int[numSprites];
      short[] widths = new short[numSprites];
      short[] heights = new short[numSprites];
      short[] compTypes = new short[numSprites];
      for (int s = 0; s < numSprites; s++) {
        // 4 - Offset [+68]
        int offset = fm.readInt() + 68 + (s * 16);
        FieldValidator.checkOffset(offset, arcSize);

        // 2 - X Position?
        // 2 - Y Position?
        fm.skip(4);

        // 2 - Sprite Width
        short width = fm.readShort();
        FieldValidator.checkWidth(width + 1); // to allow for 0 width (mainly with thumbnails)

        // 2 - Sprite Height
        short height = fm.readShort();
        FieldValidator.checkHeight(height + 1); // to allow for 0 width (mainly with thumbnails)

        // 2 - Unknown (32)
        fm.skip(2);

        // 2 - Compression Type (0=Raw Pixels, 1=RLE, 4=LZO1X+RLE)
        short compType = fm.readShort();

        offsets[s] = offset;
        widths[s] = width;
        heights[s] = height;
        compTypes[s] = compType;
      }

      // Calculate the sprite lengths. We need this because we need to detect Grayscale images vs RGB555 images
      for (int s = 0; s < numSprites - 1; s++) {
        lengths[s] = offsets[s + 1] - offsets[s];
      }
      lengths[numSprites - 1] = (int) arcSize - offsets[numSprites - 1];

      // now read the sprites
      ImageResource[] imageResources = new ImageResource[numSprites];

      for (int s = 0; s < numSprites; s++) {
        fm.relativeSeek(offsets[s]);

        int width = widths[s];
        int height = heights[s];

        int numPixels = width * height;
        int[] pixels = new int[numPixels];

        short compType = compTypes[s];

        if (compType == 5) {
          // empty file? Make it an empty image
        }
        else if (compType == 0) {
          //
          // Raw Pixels
          //
          if (imageType == 1 || (width * height == lengths[s])) {
            // Grayscale
            int writePos = 0;
            for (int i = 0; i < numPixels; i++) {
              int byte1 = ByteConverter.unsign(fm.readByte());

              int r = byte1;
              int g = byte1;
              int b = byte1;

              /*
              int a = 255;
              
              if (byte1 == 0) {
                a = 0;
              }
              */
              int a = byte1;

              // OUTPUT = ARGB
              pixels[writePos] = ((r << 16) | (g << 8) | b | (a << 24));

              writePos++;
            }
          }
          else {
            // RGB555
            int writePos = 0;
            for (int i = 0; i < numPixels; i++) {
              int byte1 = ByteConverter.unsign(fm.readByte());
              int byte2 = ByteConverter.unsign(fm.readByte());

              int r = ((byte2 >> 2) & 31) * 8;
              int g = (((byte2 & 3) << 3) | ((byte1 >> 5) & 7)) * 8;
              int b = (byte1 & 31) * 8;
              int a = (byte2 >> 7) * 255;

              if (r == 0 && g == 0 & b == 0) {
                // leave alpha as it is ("a" is accurate for either being transparent or being Black)
              }
              else {
                a = 255; // otherwise we want the pixel showing
              }

              // OUTPUT = ARGB
              pixels[writePos] = ((r << 16) | (g << 8) | b | (a << 24));

              writePos++;
            }

          }
        }
        else if (compType == 3) {
          // LZO1X
          FileManipulator originalFM = fm; // so we can flick back here when we've finished reading LZO1X-compressed images, in case the next image is a normal one

          // 4 - Compressed Data Length
          int compLength = fm.readInt();
          FieldValidator.checkLength(compLength, arcSize);

          // 4 - Decompressed Data Length
          int decompLength = fm.readInt();
          FieldValidator.checkLength(decompLength);

          // X - File Data (LZO1X)
          byte[] compBytes = fm.readBytes(compLength); // need to read this all in first, so it behaves nicely for thumbnails
          FileManipulator compFM = new FileManipulator(new ByteBuffer(compBytes));

          byte[] decompBytes = new byte[decompLength];

          Exporter_LZO_MiniLZO exporter = new Exporter_LZO_MiniLZO(); // need a NEW exporter (not .getInstance()) because otherwise it conflicts with reading the compBytes above
          exporter.open(compFM, compLength, decompLength);

          for (int b = 0; b < decompLength; b++) {
            if (exporter.available()) { // make sure we read the next bit of data, if required
              decompBytes[b] = (byte) exporter.read();
            }
          }

          compFM.close();

          // open the decompressed file data for processing (NOTE: We don't close the original FM, as we flick back to it later after we've finished with this small file)
          fm = new FileManipulator(new ByteBuffer(decompBytes));

          //
          // Read the plain pixels...
          //
          // RGB555
          int writePos = 0;
          for (int i = 0; i < numPixels; i++) {
            int byte1 = ByteConverter.unsign(fm.readByte());
            int byte2 = ByteConverter.unsign(fm.readByte());

            int r = ((byte2 >> 2) & 31) * 8;
            int g = (((byte2 & 3) << 3) | ((byte1 >> 5) & 7)) * 8;
            int b = (byte1 & 31) * 8;
            int a = (byte2 >> 7) * 255;

            if (r == 0 && g == 0 & b == 0) {
              // leave alpha as it is ("a" is accurate for either being transparent or being Black)
            }
            else {
              a = 255; // otherwise we want the pixel showing
            }

            // OUTPUT = ARGB
            pixels[writePos] = ((r << 16) | (g << 8) | b | (a << 24));

            writePos++;
          }

          fm.close(); // close the small decompressed file
          fm = originalFM; // flick back to the original FM, ready to read the next image

        }
        else {

          //
          // Check is it's LZO1X-compressed first
          //
          FileManipulator originalFM = fm; // so we can flick back here when we've finished reading LZO1X-compressed images, in case the next image is a normal one 
          if (compType == 4) {
            // LZO1X

            // 4 - Compressed Data Length
            int compLength = fm.readInt();
            FieldValidator.checkLength(compLength, arcSize);

            // 4 - Decompressed Data Length
            int decompLength = fm.readInt();
            FieldValidator.checkLength(decompLength);

            // X - File Data (LZO1X)
            byte[] compBytes = fm.readBytes(compLength); // need to read this all in first, so it behaves nicely for thumbnails
            FileManipulator compFM = new FileManipulator(new ByteBuffer(compBytes));

            byte[] decompBytes = new byte[decompLength];

            Exporter_LZO_MiniLZO exporter = new Exporter_LZO_MiniLZO(); // need a NEW exporter (not .getInstance()) because otherwise it conflicts with reading the compBytes above
            exporter.open(compFM, compLength, decompLength);

            for (int b = 0; b < decompLength; b++) {
              if (exporter.available()) { // make sure we read the next bit of data, if required
                decompBytes[b] = (byte) exporter.read();
              }
            }

            compFM.close();

            // open the decompressed file data for processing (NOTE: We don't close the original FM, as we flick back to it later after we've finished with this small file)
            fm = new FileManipulator(new ByteBuffer(decompBytes));

          }

          else if (compType == 1) {
            // Plain RLE
          }
          else {
            // not sure
            ErrorLogger.log("[Viewer_TBV_TBVOL_BMX] Unknown sprite compression: " + compType);
          }

          // X - RLE-encoded sprite data
          for (int h = 0; h < height; h++) {
            // 2 - Block Length
            short blockLength = fm.readShort();
            FieldValidator.checkPositive(blockLength);

            int readPos = 0;
            int writePos = h * width;
            // X - Block
            while (readPos < blockLength) {
              // 1 - Control Character (<128 = copy pixels raw, >128 = Repeat the next pixel X-128 times)
              int control = ByteConverter.unsign(fm.readByte());
              readPos++;

              // X - Pixels (2-byte value)
              if (control == 0) {
                // end of line
                break;
              }
              else if (control < 128) {
                // copy pixels raw
                //int[] rawPixels = ImageFormatReader.readRGB555(fm, control, 1).getPixels();
                //System.arraycopy(rawPixels, 0, pixels, writePos, control);
                //readPos += (control * 2); // read 2 bytes for each pixel
                //writePos += control;

                for (int i = 0; i < control; i++) {
                  int byte1 = ByteConverter.unsign(fm.readByte());
                  int byte2 = ByteConverter.unsign(fm.readByte());

                  int r = ((byte2 >> 2) & 31) * 8;
                  int g = (((byte2 & 3) << 3) | ((byte1 >> 5) & 7)) * 8;
                  int b = (byte1 & 31) * 8;
                  int a = (byte2 >> 7) * 255;

                  if (r == 0 && g == 0 & b == 0) {
                    // leave alpha as it is ("a" is accurate for either being transparent or being Black)
                  }
                  else {
                    a = 255; // otherwise we want the pixel showing
                  }

                  // OUTPUT = ARGB
                  pixels[writePos] = ((r << 16) | (g << 8) | b | (a << 24));

                  readPos += 2;
                  writePos++;
                }

              }
              else {
                // Repeat the next pixel X-128 times
                //int rawPixel = ImageFormatReader.readRGB555(fm, 1, 1).getPixels()[0];
                //readPos += 2; // read a single pixel (2 bytes);

                int byte1 = ByteConverter.unsign(fm.readByte());
                int byte2 = ByteConverter.unsign(fm.readByte());

                int r = ((byte2 >> 2) & 31) * 8;
                int g = (((byte2 & 3) << 3) | ((byte1 >> 5) & 7)) * 8;
                int b = (byte1 & 31) * 8;
                int a = (byte2 >> 7) * 255;

                if (r == 0 && g == 0 & b == 0) {
                  // leave alpha as it is ("a" is accurate for either being transparent or being Black)
                }
                else {
                  a = 255; // otherwise we want the pixel showing
                }

                // OUTPUT = ARGB
                int rawPixel = ((r << 16) | (g << 8) | b | (a << 24));

                readPos += 2;

                int repeat = control - 128;
                for (int rep = 0; rep < repeat; rep++) {
                  pixels[writePos] = rawPixel;
                  writePos++;
                }
              }
            }

          }

          if (compType == 4 || compType == 3) {
            // LZO1X
            fm.close(); // close the small decompressed file
            fm = originalFM; // flick back to the original FM, ready to read the next image
          }
        }

        ImageResource image = new ImageResource(pixels, width, height);
        //image = ImageFormatReader.removeAlpha(image);
        imageResources[s] = image;
      }

      fm.close();

      // link the images together
      ImageResource firstImage = imageResources[0];
      if (numSprites > 1) {
        for (int s = 0; s < numSprites; s++) {
          ImageResource image = imageResources[s];
          if (s == 0) {
            image.setNextFrame(imageResources[s + 1]);
            image.setPreviousFrame(imageResources[numSprites - 1]);
          }
          else if (s == numSprites - 1) {
            image.setNextFrame(imageResources[0]);
            image.setPreviousFrame(imageResources[s - 1]);
          }
          else {
            image.setNextFrame(imageResources[s + 1]);
            image.setPreviousFrame(imageResources[s - 1]);
          }
        }

        // lots of images have a "blank" frame 1 (type = 5), which is bad for thumbnails. For those, lets move that frame to the end, and make frame 2 the first one.
        if (compTypes[0] == 5) {
          firstImage = firstImage.getNextFrame();
        }

        firstImage.setManualFrameTransition(true);
      }

      return firstImage;

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