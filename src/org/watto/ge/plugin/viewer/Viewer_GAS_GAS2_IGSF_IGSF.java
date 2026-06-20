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
import org.watto.datatype.Palette;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.helper.ImageFormatReader;
import org.watto.ge.helper.PaletteGenerator;
import org.watto.ge.helper.PaletteManager;
import org.watto.ge.plugin.AllFilesPlugin;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.ge.plugin.archive.Plugin_GAS_GAS2;
import org.watto.ge.plugin.exporter.Exporter_RNC1;
import org.watto.ge.plugin.exporter.Exporter_RNC2;
import org.watto.io.FileManipulator;
import org.watto.io.buffer.ByteBuffer;
import org.watto.io.converter.ByteConverter;
import org.watto.io.converter.IntConverter;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_GAS_GAS2_IGSF_IGSF extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_GAS_GAS2_IGSF_IGSF() {
    super("GAS_GAS2_IGSF_IGSF", "GAS_GAS2_IGSF_IGSF Image");
    setExtensions("igsf");

    setGames("Thomas and Friends: Trouble on the Tracks");
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
      if (plugin instanceof Plugin_GAS_GAS2) {
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
      if (fm.readString(4).equals("IGSF")) {
        rating += 50;
      }
      else {
        rating = 0;
      }

      if (fm.readShort() == 36) {
        rating += 5;
      }

      fm.skip(8);

      // 2 - Image Width
      if (FieldValidator.checkWidth(fm.readShort())) {
        rating += 5;
      }

      // 2 - Image Height
      if (FieldValidator.checkHeight(fm.readShort())) {
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

      // 4 - Header (IGSF)
      fm.skip(4);

      // 2 - Header Length (36)
      int dataOffset = fm.readShort();
      FieldValidator.checkOffset(dataOffset, arcSize);

      // 2 - Unknown
      // 4 - Unknown
      // 2 - Unknown
      fm.skip(8);

      // 2 - Image Width
      short width = fm.readShort();
      FieldValidator.checkWidth(width);

      // 2 - Image Height
      short height = fm.readShort();
      FieldValidator.checkHeight(height);

      // 2 - X Origin
      // 2 - Y Origin
      // 2 - Unknown
      // 2 - Unknown
      // 2 - Unknown
      // 4 - Unknown (8)
      // 4 - Image Data Length
      fm.relativeSeek(dataOffset);

      ImageResource imageResource = null;

      // 3 - Header (PAK)
      String headerCheck = fm.readString(3);

      if (headerCheck.equals("PAK")) {
        // all the image data is compressed (STUX and Palette and Pixels)

        // 1 - Compression Version (1 or 2)
        int compression = fm.readByte();

        // 4 - Decompressed Length (BIG ENDIAN)
        int decompLength = IntConverter.changeFormat(fm.readInt());
        FieldValidator.checkLength(decompLength);

        // 4 - Compressed Length (BIG ENDIAN)
        int compLength = IntConverter.changeFormat(fm.readInt());
        FieldValidator.checkLength(compLength, arcSize);

        // 2 - Uncompressed Data CRC
        // 2 - Compressed Data CRC
        // 1 - Leeway
        // 1 - Number of Chunks
        fm.skip(6);

        // X - Compressed Image Data (RNC Compression)
        byte[] decompBytes = new byte[decompLength];

        if (compression == 1) {
          Exporter_RNC1 exporter = Exporter_RNC1.getInstance();
          exporter.open(fm, compLength, decompLength);

          for (int b = 0; b < decompLength; b++) {
            if (exporter.available()) { // make sure we read the next bit of data, if required
              decompBytes[b] = (byte) exporter.read();
            }
          }

          // open the decompressed file data for processing
          fm.close();
          fm = new FileManipulator(new ByteBuffer(decompBytes));

          arcSize = decompLength;
        }
        else if (compression == 2) {
          Exporter_RNC2 exporter = Exporter_RNC2.getInstance();
          exporter.open(fm, compLength, decompLength);

          for (int b = 0; b < decompLength; b++) {
            if (exporter.available()) { // make sure we read the next bit of data, if required
              decompBytes[b] = (byte) exporter.read();
            }
          }

          //FileManipulator tempFM = new FileManipulator(new File("c:\\decomp.txt"), true);
          //tempFM.writeBytes(decompBytes);
          //tempFM.close();

          // open the decompressed file data for processing
          fm.close();
          fm = new FileManipulator(new ByteBuffer(decompBytes));

          arcSize = decompLength;
        }
        else {
          ErrorLogger.log("[Viewer_GAS_GAS2_IGSF_IGSF] Unknown Compression Version: " + compression);
          return null;
        }

        // Read the uncompressed data

        boolean hasAlpha = true;
        int paletteOffset = 0;

        // 4 - Header (STUX)
        String stuxHeader = fm.readString(4);

        if (stuxHeader.equals("STUX")) {
          // has alpha
          hasAlpha = true;

          // 4 - Palette Offset
          paletteOffset = fm.readInt();
          FieldValidator.checkOffset(paletteOffset, arcSize);
        }
        else {
          // no alpha, just palette and pixels
          hasAlpha = false;
        }

        // ALPHA DATA (RLE ENCODED) - process it later
        fm.relativeSeek(paletteOffset);

        if (decompLength == width * height * 2) {
          // image data is RGB565

          imageResource = ImageFormatReader.readRGB565(fm, width, height);

          if (!hasAlpha) {
            // every completely 0'd pixel should be transparent
            int[] pixels = imageResource.getImagePixels();
            int numPixels = pixels.length;
            for (int p = 0; p < numPixels; p++) {
              if (pixels[p] << 8 == 0) {
                pixels[p] = 0;
              }
            }
            imageResource.setPixels(pixels);
          }

        }
        else {
          // palette followed by palette indexes

          // PALETTE DATA
          // 1 - Palette Type (1=Use previous palette, 0=256 colors, #=number of Colors in this Palette)
          int paletteType = ByteConverter.unsign(fm.readByte());
          int[] palette = null;
          if (paletteType == 1) {
            try {
              palette = PaletteManager.getPalette(PaletteManager.getNumPalettes() - 1).getPalette();
            }
            catch (Throwable t) {
              palette = PaletteGenerator.getGrayscale();
            }
          }
          else {
            int numColors = paletteType;
            if (paletteType == 0) {
              numColors = 256;
            }

            // X - Palette Data (RGB565)
            palette = ImageFormatReader.readRGB565(fm, 1, numColors).getImagePixels();

            // convert to a 256-color palette
            int[] oldPalette = palette;
            palette = new int[256];
            System.arraycopy(oldPalette, 0, palette, 0, numColors);

            PaletteManager.addPalette(new Palette(palette), false); // so that the next frame can use this palette
          }

          // PIXEL DATA
          imageResource = ImageFormatReader.read8BitPaletted(fm, width, height, palette);

        }

        // Now apply the Alpha to the Pixels
        if (hasAlpha) {
          int[] pixels = imageResource.getImagePixels();

          fm.relativeSeek(8);

          boolean moreAlpha = true;
          int x = 0;
          int y = 0;
          while (moreAlpha) {
            if (fm.getOffset() >= paletteOffset) {
              moreAlpha = false;
              break;
            }
            if (y >= height) {
              moreAlpha = false;
              break;
            }

            // 1 - Control Character
            int control = ByteConverter.unsign(fm.readByte());

            if (control == 0x78) {
              // end of alpha data
              moreAlpha = false;
              break;
            }
            else if (control == 0x73) {
              // the next Count bytes are transparent
              // 1 - Count
              int count = ByteConverter.unsign(fm.readByte());

              int startPos = (y * width) + x;
              for (int c = 0; c < count; c++) {
                pixels[startPos] = 0;
                startPos++;
              }

              x += count;
            }
            else if (control == 0x62) {
              // 1 - Count
              int count = ByteConverter.unsign(fm.readByte());

              int startPos = (y * width) + x;

              // for each count
              for (int c = 0; c < count; c++) {
                // 1 - Alpha Value
                int alpha = ByteConverter.unsign(fm.readByte());

                pixels[startPos] = alpha << 24 | (pixels[startPos] & 0xFFFFFF);
                startPos++;
              }

              x += count;

            }
            else if (control == 0x63) {
              // 1 - Count
              int count = ByteConverter.unsign(fm.readByte());

              int startReadPos = ((y - 1) * width) + x;
              int startWritePos = (y * width) + x;

              if (startReadPos < 0) {
                startReadPos = startWritePos - 1;
              }
              if (startReadPos == -1) {
                startReadPos = 0;
              }

              // for each count
              for (int c = 0; c < count; c++) {
                // 1 - Copy Value from Previous Row
                int alpha = ByteConverter.unsign((byte) (pixels[startReadPos] >> 24));
                alpha = ((alpha + 255) / 2); // correct the alpha for each subsequent row, otherwise it streaks

                pixels[startWritePos] = alpha << 24 | (pixels[startWritePos] & 0xFFFFFF);
                startReadPos++;
                startWritePos++;
              }

              x += count;
            }
            else if (control == 0x65) {
              // end of current row (rest of the pixels are transparent in this row)
              int count = width - x;

              if (count > 0) {
                int startPos = (y * width) + x;
                for (int c = 0; c < count; c++) {
                  pixels[startPos] = 0;
                  startPos++;
                }
              }

              y++;
              x = 0;
            }
            else {
              // unsupported
              moreAlpha = false;
              break;
            }
          }

          imageResource.setPixels(pixels);
        }

      }
      else if (headerCheck.equals("STU")) { // STUX
        // STUX data (uncompressed), then the PAK compressed image data and palette

        // 4 - Header (STUX)
        fm.skip(1); // already read 3 bytes for the header check above

        boolean hasAlpha = true;

        // 4 - Alpha Data Length (including these 2 fields)
        int alphaDataLength = fm.readInt() - 8;
        FieldValidator.checkOffset(alphaDataLength, arcSize);

        // ALPHA DATA (RLE ENCODED) - process it later
        byte[] alphaData = fm.readBytes(alphaDataLength);

        // PAK DATA (compressed)

        // 3 - Header (PAK)
        fm.skip(3);

        // 1 - Compression Version (1 or 2)
        int compression = fm.readByte();

        // 4 - Decompressed Length (BIG ENDIAN)
        int decompLength = IntConverter.changeFormat(fm.readInt());
        FieldValidator.checkLength(decompLength);

        // 4 - Compressed Length (BIG ENDIAN)
        int compLength = IntConverter.changeFormat(fm.readInt());
        FieldValidator.checkLength(compLength, arcSize);

        // 2 - Uncompressed Data CRC
        // 2 - Compressed Data CRC
        // 1 - Leeway
        // 1 - Number of Chunks
        fm.skip(6);

        // X - Compressed Image Data (RNC Compression)
        byte[] decompBytes = new byte[decompLength];

        if (compression == 1) {
          Exporter_RNC1 exporter = Exporter_RNC1.getInstance();
          exporter.open(fm, compLength, decompLength);

          for (int b = 0; b < decompLength; b++) {
            if (exporter.available()) { // make sure we read the next bit of data, if required
              decompBytes[b] = (byte) exporter.read();
            }
          }

          // open the decompressed file data for processing
          fm.close();
          fm = new FileManipulator(new ByteBuffer(decompBytes));

          arcSize = decompLength;
        }
        else if (compression == 2) {
          Exporter_RNC2 exporter = Exporter_RNC2.getInstance();
          exporter.open(fm, compLength, decompLength);

          for (int b = 0; b < decompLength; b++) {
            if (exporter.available()) { // make sure we read the next bit of data, if required
              decompBytes[b] = (byte) exporter.read();
            }
          }

          //FileManipulator tempFM = new FileManipulator(new File("c:\\decomp.txt"), true);
          //tempFM.writeBytes(decompBytes);
          //tempFM.close();

          // open the decompressed file data for processing
          fm.close();
          fm = new FileManipulator(new ByteBuffer(decompBytes));

          arcSize = decompLength;
        }
        else {
          ErrorLogger.log("[Viewer_GAS_GAS2_IGSF_IGSF] Unknown Compression Version: " + compression);
          return null;
        }

        // Read the uncompressed data

        // PIXEL DATA (RGB565)
        imageResource = ImageFormatReader.readRGB565(fm, width, height);

        // Now apply the Alpha to the Pixels
        if (hasAlpha) {
          int[] pixels = imageResource.getImagePixels();

          // open the alpha data we've already read
          fm.close();
          fm = new FileManipulator(new ByteBuffer(alphaData));

          boolean moreAlpha = true;
          int x = 0;
          int y = 0;
          while (moreAlpha) {
            if (fm.getOffset() >= alphaDataLength) {
              moreAlpha = false;
              break;
            }
            if (y >= height) {
              moreAlpha = false;
              break;
            }

            // 1 - Control Character
            int control = ByteConverter.unsign(fm.readByte());

            if (control == 0x78) {
              // end of alpha data
              moreAlpha = false;
              break;
            }
            else if (control == 0x73) {
              // the next Count bytes are transparent
              // 1 - Count
              int count = ByteConverter.unsign(fm.readByte());

              int startPos = (y * width) + x;
              for (int c = 0; c < count; c++) {
                pixels[startPos] = 0;
                startPos++;
              }

              x += count;
            }
            else if (control == 0x62) {
              // 1 - Count
              int count = ByteConverter.unsign(fm.readByte());

              int startPos = (y * width) + x;

              // for each count
              for (int c = 0; c < count; c++) {
                // 1 - Alpha Value
                int alpha = ByteConverter.unsign(fm.readByte());

                pixels[startPos] = alpha << 24 | (pixels[startPos] & 0xFFFFFF);
                startPos++;
              }

              x += count;

            }
            else if (control == 0x63) {
              // 1 - Count
              int count = ByteConverter.unsign(fm.readByte());

              int startReadPos = ((y - 1) * width) + x;
              int startWritePos = (y * width) + x;

              if (startReadPos < 0) {
                startReadPos = startWritePos - 1;
              }

              if (startReadPos == -1) {
                startReadPos = 0;
              }

              // for each count
              for (int c = 0; c < count; c++) {
                // 1 - Copy Value from Previous Row
                int alpha = ByteConverter.unsign((byte) (pixels[startReadPos] >> 24));
                alpha = ((alpha + 255) / 2); // correct the alpha for each subsequent row, otherwise it streaks

                pixels[startWritePos] = alpha << 24 | (pixels[startWritePos] & 0xFFFFFF);
                startReadPos++;
                startWritePos++;
              }

              x += count;
            }
            else if (control == 0x65) {
              // end of current row (rest of the pixels are transparent in this row)
              int count = width - x;

              if (count > 0) {
                int startPos = (y * width) + x;
                for (int c = 0; c < count; c++) {
                  pixels[startPos] = 0;
                  startPos++;
                }
              }

              y++;
              x = 0;
            }
            else {
              // unsupported
              moreAlpha = false;
              break;
            }
          }

          imageResource.setPixels(pixels);
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