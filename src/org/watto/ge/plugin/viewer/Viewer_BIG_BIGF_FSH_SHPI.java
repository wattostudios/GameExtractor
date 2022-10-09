/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2021 wattostudios
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
import org.watto.ErrorLogger;
import org.watto.component.PreviewPanel;
import org.watto.component.PreviewPanel_Image;
import org.watto.datatype.Archive;
import org.watto.datatype.ImageResource;
import org.watto.datatype.Resource;
import org.watto.ge.helper.ColorSplitAlpha;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.helper.ImageFormatReader;
import org.watto.ge.helper.ImageFormatWriter;
import org.watto.ge.helper.ImageManipulator;
import org.watto.ge.plugin.AllFilesPlugin;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.ge.plugin.archive.Plugin_BIG_BIGF;
import org.watto.ge.plugin.archive.Plugin_BIG_BIGF_3;
import org.watto.ge.plugin.archive.Plugin_CCD_FKNL;
import org.watto.ge.plugin.archive.Plugin_DAT_DBPF;
import org.watto.ge.plugin.archive.Plugin_SHD_MRTS;
import org.watto.ge.plugin.archive.Plugin_VIV;
import org.watto.ge.plugin.exporter.Exporter_REFPACK;
import org.watto.io.FileManipulator;
import org.watto.io.buffer.ByteBuffer;
import org.watto.io.converter.ByteConverter;
import org.watto.io.converter.IntConverter;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_BIG_BIGF_FSH_SHPI extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_BIG_BIGF_FSH_SHPI() {
    super("BIG_BIGF_FSH_SHPI", "Electronic Arts FSH Image [BIG_BIGF_FSH_SHPI]");
    setExtensions("fsh", "shpi", "ssh", "psh");

    setGames("FIFA 06",
        "FIFA 08",
        "FIFA 07",
        "FIFA 09",
        "FIFA 10",
        "FIFA 99",
        "FIFA 2000",
        "FIFA Manager 06",
        "FIFA Manager 08",
        "FIFA Manager 09",
        "FIFA Manager 10",
        "FIFA Manager 11",
        "FIFA World Cup 2006",
        "Harry Potter and the Order of the Phoenix",
        "Harry Potter: Quidditch World Cup",
        "Need For Speed 2",
        "Need For Speed 3: Hot Pursuit",
        "Need For Speed: Porsche Unleashed",
        "NHL 2000",
        "NHL 2001",
        "NHL 2002",
        "NHL 2003",
        "NHL 06",
        "SimCity 4");
    setPlatforms("PC", "PS2");
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
      if (plugin instanceof Plugin_BIG_BIGF || plugin instanceof Plugin_SHD_MRTS || plugin instanceof Plugin_CCD_FKNL || plugin instanceof Plugin_DAT_DBPF || plugin instanceof Plugin_VIV || plugin instanceof Plugin_BIG_BIGF_3) {
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

      // 4 - Header (SHPI)
      String header = fm.readString(4);
      if (header.equals("SHPI") || header.equals("ShpF") || header.equals("SHPS") || header.equals("ShpS") || header.equals("SHPP")) {
        rating += 5;
      }
      else {
        // maybe compressed - see if it is
        fm.skip(2);
        if (fm.readString(4).equals("SHPI")) {
          rating += 5;
          return rating; // exit early, so we don't check the file length below 
        }
      }

      // 4 - File Length
      if (FieldValidator.checkEquals(fm.readInt(), fm.getLength())) {
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

      readThumbnailOnly = false;
      ImageResource imageResource = readThumbnail(fm);
      readThumbnailOnly = true;

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

  /** true for thumbnails, false for Previews (set in read() to false) **/
  boolean readThumbnailOnly = true;

  /** change endian order for some formats **/
  boolean changeEndian = false;

  int[] globalPalette = null;

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

      // reset global value to the default
      changeEndian = false;
      globalPalette = null;

      // 4 - Header (SHPI)
      String header = fm.readString(4);
      if (header.equals("SHPI") || header.equals("ShpF") || header.equals("SHPS") || header.equals("ShpS") || header.equals("SHPP")) {
        // not compressed

        if (header.equals("ShpS")) {
          // SHPS, but BIG ENDIAN on some fields
          changeEndian = true;
        }
      }
      else {
        // might be compressed - check it out
        fm.skip(2);
        if (fm.readString(4).equals("SHPI")) {
          // yep, probably compressed, so decompress it before reading it

          fm.seek(0);

          int compLength = (int) fm.getLength();

          // work out the decomp length
          // 2 bytes - Signature
          short signature = fm.readShort();
          if (signature > 0) { // top bit is 0
            // 3 bytes - Compressed Size
            fm.skip(3);
          }

          // 3 bytes - Decompressed Size
          byte[] decompBytes = new byte[] { 0, fm.readByte(), fm.readByte(), fm.readByte() };
          int decompLength = IntConverter.convertBig(decompBytes);

          // go back to the start, ready for decompression
          fm.seek(0);

          Exporter_REFPACK exporter = Exporter_REFPACK.getInstance();
          exporter.open(fm, compLength, decompLength);

          byte[] fileData = new byte[decompLength];

          int decompWritePos = 0;
          while (exporter.available()) { // make sure we read the next bit of data, if required
            fileData[decompWritePos++] = (byte) exporter.read();
          }

          fm.close();
          fm = new FileManipulator(new ByteBuffer(fileData));

          // Skip the SHPI header that we would have read at the beginning, if it were a raw file
          header = fm.readString(4);

          //exporter.close(); // THIS MIGHT CAUSE PROBLEMS WITH THE BYTE[]?

        }
        else {
          return null;
        }
      }

      // 4 - File Length
      fm.skip(4);

      // 4 - Number of Images
      int numImages = fm.readInt();
      if (changeEndian) {
        numImages = IntConverter.changeFormat(numImages);
      }
      FieldValidator.checkNumFiles(numImages);

      /*
      // 3.14 moved down further, so that it works for NHL2001 PC with a global palette that isn't in the directory
      if (readThumbnailOnly) {
        numImages = 1; // only want to generate a single thumbnail image for "thumbnails", but generate all for "previews"
      }
      */

      // 4 - Image Group ID
      fm.skip(4);

      if (numImages == 1 || /*header.equals("SHPI") ||*/ header.equals("ShpF")) {
        // ie SHPI and ShpF are treated as 1 single file for now. Also used for SHPS with a single image in it

        int offset = 0;
        if (header.equals("SHPI") || header.equals("SHPS") || header.equals("SHPP")) {
          // 4 - (FIRST IMAGE) Image Code Name
          fm.skip(4);

          // 4 - (FIRST IMAGE) Offset to Image Data
          offset = fm.readInt();
          FieldValidator.checkOffset(offset, fm.getLength());
        }
        else if (header.equals("ShpS") || header.equals("ShpF")) {
          // 4 - (FIRST IMAGE) Offset to Image Data (BIG)
          offset = IntConverter.changeFormat(fm.readInt());
          FieldValidator.checkOffset(offset, fm.getLength());
        }

        ImageResource image = readSingleImage(fm, offset, header);

        fm.close();

        return image;
      }
      else {
        // multiple images in an SHPS/ShpS only
        long arcSize = fm.getLength();

        int[] offsets = new int[numImages];
        if (header.equals("ShpS")) {
          // BIG ENDIAN (eg NHL 2008 PS2)
          for (int i = 0; i < numImages; i++) {
            // 4 - Offset to Image Data (BIG)
            int offset = IntConverter.changeFormat(fm.readInt());
            FieldValidator.checkOffset(offset, arcSize);
            offsets[i] = offset;

            // 4 - Image Data Length (BIG)
            // 4 - Image Code Name
            // 1 - null
            fm.skip(9);
          }
        }
        else {
          for (int i = 0; i < numImages; i++) {
            // 4 - Image Code Name
            fm.skip(4);

            // 4 - Offset to Image Data
            int offset = fm.readInt();
            FieldValidator.checkOffset(offset, arcSize);
            offsets[i] = offset;
          }
        }

        // See if there is a global palette stored here, instead of as a PAL entry (NHL 2001 PC)
        if (fm.getOffset() + 768 < offsets[0]) {
          // maybe a global palette

          if (fm.readByte() == 36) {
            // a global palette

            fm.skip(3);

            int numColors = fm.readShort();
            if (numColors > 0 && numColors <= 256) {
              fm.skip(10);

              int[] palette = new int[numColors];
              for (int i = 0; i < numColors; i++) {
                int r = ByteConverter.unsign(fm.readByte());
                int g = ByteConverter.unsign(fm.readByte());
                int b = ByteConverter.unsign(fm.readByte());

                palette[i] = ((255 << 24) | (r << 16) | (g << 8) | b);
              }

              // store the global palette for use with other images (this is NOT stored in an image, unlike a PAL entry in the directory)
              globalPalette = palette;
            }
          }
        }

        if (readThumbnailOnly) {
          numImages = 1; // only want to generate a single thumbnail image for "thumbnails", but generate all for "previews"
        }

        ImageResource[] images = new ImageResource[numImages];
        for (int i = 0; i < numImages; i++) {
          ImageResource imageResource = readSingleImage(fm, offsets[i], header);
          if (imageResource == null) {
            // make it a blank dummy instead, as we probably just couldn't render this image or something.
            imageResource = new ImageResource(new int[0], 0, 0);
          }
          images[i] = imageResource;
        }

        // set the next/previous images
        ImageResource firstImage = null;
        if (numImages > 1) {
          for (int i = 0; i < numImages; i++) {
            ImageResource imageResource = images[i];

            if (i == numImages - 1) {
              imageResource.setNextFrame(images[0]);
            }
            else {
              imageResource.setNextFrame(images[i + 1]);
            }

            if (i == 0) {
              imageResource.setPreviousFrame(images[numImages - 1]);
            }
            else {
              imageResource.setPreviousFrame(images[i - 1]);
            }
          }

          firstImage = images[0];
          firstImage.setManualFrameTransition(true);
        }
        else {
          firstImage = images[0];
        }

        fm.close();

        return firstImage;

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
  public ImageResource readSingleImage(FileManipulator fm, long offset, String header) {
    try {
      fm.seek(offset);

      // 1 - Image Format
      int imageType = ByteConverter.unsign(fm.readByte());

      boolean ps2Striped = false;

      String imageFormat = "DXT3";
      if (imageType == 96) {
        imageFormat = "DXT1";
      }
      else if (imageType == 97) {
        imageFormat = "DXT3";
      }
      else if (imageType == 98) {
        imageFormat = "DXT5";
      }
      else if (imageType == 109) {
        imageFormat = "ARGB4444";
      }
      else if (imageType == 120) {
        imageFormat = "RGB565";
      }
      else if (imageType == 123) {
        imageFormat = "8BitPaletted";
      }
      else if (imageType == 125) {
        imageFormat = "BGRA";
      }
      else if (imageType == 126) {
        imageFormat = "ARGB1555";
      }
      else if (imageType == 127) {
        imageFormat = "BGR";
      }
      else if (imageType == 2) {
        imageFormat = "8BitPaletted";
        ps2Striped = true;
      }
      else if (imageType == 64) {
        imageFormat = "4BitPaletted"; // PS1
      }
      else if (imageType == 65) {
        imageFormat = "8BitPaletted"; // PS1
      }
      else if (imageType == 14) {
        // Paletted with 256 colors in the palette, however the pixel data is only 3bpp in size
        ErrorLogger.log("Viewer_BIG_BIGF_FSH_SHPI: Unknown Image Format: " + imageType + " at offset " + offset);

        //imageFormat = "3BitPaletted";
        //ps2Striped = true;
      }
      else if (imageType == 1) {
        imageFormat = "4BitPaletted";
        ps2Striped = true;
      }
      else if (imageType == 35) {
        // Color palette (BGRA5551)
        imageFormat = "PAL35";
      }
      else if (imageType == 36) {
        // Color palette (BGR)
        imageFormat = "PAL36";
      }
      else {
        ErrorLogger.log("Viewer_BIG_BIGF_FSH_SHPI: Unknown Image Format: " + imageType + " at offset " + offset);
        return null;
      }

      // 3 - Image Data Length (including these 16-bytes of header, and possibly multiple mipmaps)
      //fm.skip(3);
      byte[] imageDataLengthBytes = new byte[] { fm.readByte(), fm.readByte(), fm.readByte(), 0 };
      int imageDataLength = IntConverter.convertLittle(imageDataLengthBytes);
      if (changeEndian) {
        imageDataLengthBytes = new byte[] { imageDataLengthBytes[1], imageDataLengthBytes[0], imageDataLengthBytes[2], 0 };
        imageDataLength = IntConverter.convertLittle(imageDataLengthBytes);
      }

      int height = 0;
      int width = 0;
      if (header.equals("SHPI") || header.equals("SHPS") || header.equals("SHPP")) {
        // 2 - Image Width
        width = fm.readShort();
        FieldValidator.checkWidth(width);

        // 2 - Image Height
        height = fm.readShort();
        FieldValidator.checkHeight(height);

        // 2 - X Position for Image Center
        // 2 - Y Position for Image Center
        // 2 - X Position from the Left
        // 2 - Y Position from the Top
        fm.skip(8);
      }
      else if (header.equals("ShpS") || header.equals("ShpF")) {
        // 4 - null
        // 4 - Offset to Pixel Data (relative to the start of this file)(32)
        // 4 - Image Data Length (including this 32-byte header)
        // 8 - null
        fm.skip(20);

        // 4 - Image Width
        width = fm.readInt();
        FieldValidator.checkWidth(width);

        // 4 - Image Height
        height = fm.readInt();
        FieldValidator.checkHeight(height);
      }

      // X - Image Data
      ImageResource imageResource = null;

      // X - Pixels
      if (imageFormat.equals("DXT5")) {
        imageResource = ImageFormatReader.readDXT5(fm, width, height);
      }
      else if (imageFormat.equals("DXT3")) {
        imageResource = ImageFormatReader.readDXT3(fm, width, height);
      }
      else if (imageFormat.equals("DXT1")) {
        imageResource = ImageFormatReader.readDXT1(fm, width, height);
      }
      else if (imageFormat.equals("RGB565")) {
        imageResource = ImageFormatReader.readRGB565(fm, width, height);
      }
      else if (imageFormat.equals("BGRA")) {
        imageResource = ImageFormatReader.readBGRA(fm, width, height);
      }
      else if (imageFormat.equals("ARGB1555")) {
        imageResource = ImageFormatReader.readARGB1555(fm, width, height);
      }
      else if (imageFormat.equals("BGR")) {
        imageResource = ImageFormatReader.readBGR(fm, width, height);
      }
      else if (imageFormat.equals("PAL36")) {
        // width = numColors
        // height = bytes per color

        if (height == 3 || height == 1) {
          // 3 bytes per color (RGB)
          //imageResource = ImageFormatReader.readRGB(fm, width, height);

          // 24bit RGB (confirmed RGB for NHL 2000 PC)
          int numColors = width;
          int[] palette = new int[numColors];
          for (int i = 0; i < numColors; i++) {
            int r = ByteConverter.unsign(fm.readByte());
            int g = ByteConverter.unsign(fm.readByte());
            int b = ByteConverter.unsign(fm.readByte());

            palette[i] = ((255 << 24) | (r << 16) | (g << 8) | b);
          }

          // store the global palette for use with other images
          globalPalette = palette;

          // also store it as an image
          imageResource = new ImageResource(palette, width, 1);
        }
        else {
          ErrorLogger.log("Viewer_BIG_BIGF_FSH_SHPI: Unknown PAL36 Byte Size: " + height + " at offset " + offset);
          return null;
        }
      }
      else if (imageFormat.equals("PAL35")) {
        // width = numColors
        // height = bytes per color

        if (height == 2 || height == 1) {
          // 2 bytes per color (RGBA5551)

          // 16bit BGR5551 (NHL 2001 PS1) (where 0,0,0 has a real alpha value, everything else should be treated as alpha = 255)
          int numColors = width;
          int[] palette = new int[numColors];
          for (int i = 0; i < numColors; i++) {

            int byte1 = ByteConverter.unsign(fm.readByte());
            int byte2 = ByteConverter.unsign(fm.readByte());

            int b = ((byte2 >> 2) & 31) * 8;
            int g = (((byte2 & 3) << 3) | ((byte1 >> 5) & 7)) * 8;
            int r = (byte1 & 31) * 8;
            int a = (byte2 >> 7) * 255;

            if (b == 0 && g == 0 && r == 0) {
              // a is the real value
            }
            else {
              a = 255;
            }

            // OUTPUT = ARGB
            palette[i] = ((r << 16) | (g << 8) | b | (a << 24));
          }

          // store the global palette for use with other images
          globalPalette = palette;

          // also store it as an image
          imageResource = new ImageResource(palette, width, 1);
        }
        else {
          ErrorLogger.log("Viewer_BIG_BIGF_FSH_SHPI: Unknown PAL35 Byte Size: " + height + " at offset " + offset);
          return null;
        }
      }
      else if (imageFormat.equals("ARGB4444")) {
        imageResource = ImageFormatReader.readARGB4444(fm, width, height);
      }
      else if (imageFormat.equals("8BitPaletted") || imageFormat.equals("4BitPaletted")) {
        // X - Pixels
        int numPixels = width * height;
        int[] pixels = new int[numPixels];

        boolean oddWidth = (width % 2 == 1);

        if (oddWidth && (imageDataLength < ((width + 1) * height) + 16)) {
          // not actually padded for odd widths
          oddWidth = false;
        }

        if (imageFormat.equals("4BitPaletted")) {

          // if the image width is 50, for example, then each line is stored in 25 bytes, which is odd, so add another byte at the end of each row to pad to a 2-byte boundary
          int widthForCalc = width;
          if (widthForCalc % 2 == 1) {
            widthForCalc++;
          }
          boolean widthPadded = ((widthForCalc / 2) % 2) == 1;

          // 4-bits per color (The order of the pixels is verified by NHL 2001 PS1)
          int p = 0;
          for (int h = 0; h < height; h++) {
            for (int w = 0; w < width; w += 2) {
              int pixel = ByteConverter.unsign(fm.readByte());
              int pixel2 = pixel >> 4;
              int pixel1 = pixel & 15;

              pixels[p] = pixel1;
              p++;

              if (w + 2 <= width) { // if even width, we do add the second pixel, otherwise odd width just ignores it
                pixels[p] = pixel2;
                p++;
              }
            }

            if (widthPadded) {
              fm.skip(1);
            }
          }
        }
        else {
          // normal 8-bits per color
          int p = 0;
          for (int h = 0; h < height; h++) {
            for (int w = 0; w < width; w++) {
              pixels[p] = ByteConverter.unsign(fm.readByte());
              p++;
            }
            if (oddWidth) {
              // padding byte for images that have an odd width
              fm.skip(1);
            }
          }
        }

        //for (int i = 0; i < numPixels; i++) {
        //  pixels[i] = ByteConverter.unsign(fm.readByte());
        //}

        // Now seek past all the image data (in case there's multiple mipmaps) to find the palette
        if (header.equals("ShpS")) {
          // the imageDataLength isn't right in this format - it's only correct as if it's 8-bit, not 4-bit or 3-bit.

          /*
          // so calculate it here.
          if (imageFormat.equals("4BitPaletted")) {
            imageDataLength = width * height / 2;
          }
          else if (imageFormat.equals("3BitPaletted")) {
            imageDataLength = width * height / 8 * 3;
          }
          */
          //actually, just assume we're in the right spot already
          imageDataLength = (int) (fm.getOffset() - offset); // so that when we add it to the offset below, it doesn't move anywhere
        }

        offset += imageDataLength;

        int[] palette = new int[256]; // force to 256 colors, so striping works later on

        if (globalPalette != null && (imageDataLengthBytes[2] == -1 || imageDataLength == 0)) {
          // no palette or anything in this image, just had the pixel data and that's it
          palette = globalPalette;
        }
        else {

          FieldValidator.checkOffset(offset, fm.getLength());
          fm.seek(offset);

          // 4 - Palette Format
          int paletteFormat = fm.readByte();
          while (paletteFormat == 0) { // skip over null padding at the end of the image
            paletteFormat = fm.readByte();
          }
          fm.skip(3);

          if (paletteFormat == 111 && globalPalette != null) {
            // uses a global color palette
            palette = globalPalette;
          }
          else if (paletteFormat == 124 && globalPalette != null) {
            // uses a global color palette
            palette = globalPalette;
          }
          else {

            // 2 - Number of Colors?
            // 2 - Unknown (1)
            fm.skip(4);

            if (header.equals("ShpS")) {
              // 4 - Unknown (32)
              // 4 - Unknown (64)
              // 8 - null
              fm.skip(16);
            }

            // 4 - Number of Colors
            int numColors = fm.readInt();

            // 4 - null
            //fm.skip(4);
            int paletteStripeCheck = fm.readInt();
            if (paletteStripeCheck == 0) { // not striped
              ps2Striped = false;
            }

            // X - Color Palette
            if (paletteFormat == 42) {
              // 32bit BGRA
              for (int i = 0; i < numColors; i++) {
                int b = ByteConverter.unsign(fm.readByte());
                int g = ByteConverter.unsign(fm.readByte());
                int r = ByteConverter.unsign(fm.readByte());
                int a = ByteConverter.unsign(fm.readByte());

                palette[i] = ((a << 24) | (r << 16) | (g << 8) | b);
              }
            }
            else if (paletteFormat == 33) {
              // 32bit RGBA (PS2 Striped)

              for (int i = 0; i < numColors; i++) {
                int r = ByteConverter.unsign(fm.readByte());
                int g = ByteConverter.unsign(fm.readByte());
                int b = ByteConverter.unsign(fm.readByte());
                int a = ByteConverter.unsign(fm.readByte());

                /*
                //if (r == 0 && g == 0 && b == 0 && a == 0) {
                if (a == 0) {
                }
                else {
                a = 255;
                }
                */
                a *= 2;
                if (a > 255) {
                  a = 255;
                }

                palette[i] = ((a << 24) | (r << 16) | (g << 8) | b);
              }

              if (ps2Striped) {
                palette = ImageFormatReader.stripePalettePS2(palette);
              }

            }
            else if (paletteFormat == 36) {
              // 24bit RGB (confirmed RGB for Triple Play 2000)
              for (int i = 0; i < numColors; i++) {
                int r = ByteConverter.unsign(fm.readByte());
                int g = ByteConverter.unsign(fm.readByte());
                int b = ByteConverter.unsign(fm.readByte());

                palette[i] = ((255 << 24) | (r << 16) | (g << 8) | b);
              }
            }
            else if (paletteFormat == 35) {
              // 16bit BGR5551 (NHL 2001 PS1) (where 0,0,0 has a real alpha value, everything else should be treated as alpha = 255)
              for (int i = 0; i < numColors; i++) {

                int byte1 = ByteConverter.unsign(fm.readByte());
                int byte2 = ByteConverter.unsign(fm.readByte());

                int b = ((byte2 >> 2) & 31) * 8;
                int g = (((byte2 & 3) << 3) | ((byte1 >> 5) & 7)) * 8;
                int r = (byte1 & 31) * 8;
                int a = (byte2 >> 7) * 255;

                if (b == 0 && g == 0 && r == 0) {
                  // a is the real value
                }
                else {
                  a = 255;
                }

                // OUTPUT = ARGB
                palette[i] = ((r << 16) | (g << 8) | b | (a << 24));
              }
            }
            else {
              ErrorLogger.log("Viewer_BIG_BIGF_FSH_SHPI: Unknown Palette Format: " + paletteFormat + " for image at offset " + offset);
              return null;
            }
          }
        }

        for (int i = 0; i < numPixels; i++) {
          pixels[i] = palette[pixels[i]];
        }

        imageResource = new ImageResource(pixels, width, height);
      }

      if (imageResource == null) {
        return null;
      }

      imageResource.addProperty("ImageFormat", "" + imageFormat);
      if (ps2Striped) {
        imageResource.addProperty("PaletteStripedPS2", "true");
      }

      return imageResource;

    }
    catch (

    Throwable t) {
      logError(t);
      return null;
    }
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

      // reset globals
      globalPalette = null;

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

      // Extract the original resource into a byte[] array, so we can reference it
      byte[] srcBytes = new byte[(int) resourceBeingReplaced.getDecompressedLength()];
      FileManipulator src = new FileManipulator(new ByteBuffer(srcBytes));
      resourceBeingReplaced.extract(src);
      src.seek(0);

      // Build the new file using the src[] and adding in the new image content

      // 4 - Header
      String header = src.readString(4);
      fm.writeString(header);

      // 4 - File Length
      fm.writeBytes(src.readBytes(4));

      // 4 - Number of Images (1)
      int numImages = src.readInt();
      fm.writeInt(numImages);

      // 4 - Directory ID String
      fm.writeBytes(src.readBytes(4));

      int[] offsets = new int[numImages];
      for (int i = 0; i < numImages; i++) {
        // 4 - Filename String
        fm.writeBytes(src.readBytes(4));

        // 4 - File Offset
        int offset = src.readInt();
        fm.writeInt(offset);

        offsets[i] = offset;
      }

      // X - Data
      int preDataLength = (int) (offsets[0] - src.getOffset()); // extra data between current position and first offset

      long globalPaletteOffset = -1;

      // See if there is a global palette stored here, instead of as a PAL entry (NHL 2001 PC)
      if (preDataLength > 768) {
        // maybe a global palette

        if (src.readByte() == 36) {
          // a global palette
          fm.writeByte(36);
          fm.writeBytes(src.readBytes(3));

          int numColors = src.readShort();
          fm.writeShort(numColors);
          if (numColors > 0 && numColors <= 256) {
            fm.writeBytes(src.readBytes(10));

            globalPaletteOffset = fm.getOffset();
            fm.writeBytes(src.readBytes(numColors * 3));
          }
        }

        // re-calculate the preDataLength
        preDataLength = (int) (offsets[0] - src.getOffset()); // extra data between current position and first offset
      }

      fm.writeBytes(src.readBytes(preDataLength));

      for (int i = 0; i < numImages; i++) {

        // move to the next frame, if this isn't the first image
        if (i > 0) {
          imageResource = imageResource.getNextFrame();
          if (imageResource == null) {
            ErrorLogger.log("[Viewer_BIG_BIGF_FSH_SHPI] Expected " + numImages + " images but only imported " + i + " from the filesystem");
            return;
          }
          width = imageResource.getWidth();
          height = imageResource.getHeight();
          image = imageResource.getImage();
        }

        // 1 - Image Format
        int imageType = ByteConverter.unsign(src.readByte());
        fm.writeByte(imageType);

        String imageFormat = "DXT3";
        if (imageType == 96) {
          imageFormat = "DXT1";
        }
        else if (imageType == 97) {
          imageFormat = "DXT3";
        }
        else if (imageType == 98) {
          imageFormat = "DXT5";
        }
        else if (imageType == 109) {
          imageFormat = "ARGB4444";
        }
        else if (imageType == 120) {
          imageFormat = "RGB565";
        }
        else if (imageType == 123) {
          imageFormat = "8BitPaletted";
        }
        else if (imageType == 125) {
          imageFormat = "BGRA";
        }
        else if (imageType == 126) {
          imageFormat = "ARGB1555";
        }
        else if (imageType == 127) {
          imageFormat = "BGR";
        }
        else if (imageType == 36) {
          imageFormat = "PAL36";
        }
        else if (imageType == 35) {
          imageFormat = "PAL35";
        }
        else if (imageType == 2) {
          imageFormat = "8BitPaletted";
        }
        else if (imageType == 1) {
          imageFormat = "4BitPaletted";
        }
        else if (imageType == 64) {
          imageFormat = "4BitPaletted"; // PS1
        }
        else if (imageType == 65) {
          imageFormat = "8BitPaletted"; // PS1
        }
        else {
          ErrorLogger.log("[Viewer_BIG_BIGF_FSH_SHPI] Writing an Unknown Image Format: " + imageType);
          return;
        }

        // 3 - Image Data Length
        byte[] imageDataLengthBytes = new byte[] { src.readByte(), src.readByte(), src.readByte(), 0 };
        int imageDataLength = IntConverter.convertLittle(imageDataLengthBytes);

        if (imageDataLengthBytes[2] == -1) {
          // calculate the proper image data length, for when there's a global palette
          imageDataLength = IntConverter.convertLittle(new byte[] { imageDataLengthBytes[0], imageDataLengthBytes[1], 0, 0 });
        }

        if (imageFormat.equals("PAL36") || imageFormat.equals("PAL35")) {
          imageDataLength = 0;
        }

        fm.writeByte(imageDataLengthBytes[0]);
        fm.writeByte(imageDataLengthBytes[1]);
        fm.writeByte(imageDataLengthBytes[2]);

        imageDataLength -= 16; // 16 for the header

        // 2 - Width
        // 2 - Height
        // 2 - X Axis Coordinate
        // 2 - Y Axis Coordinate
        // 2 - X Axis Position
        // 2 - Y Axis Position
        fm.writeBytes(src.readBytes(12));

        // X - Pixels
        // X - Palette
        if (imageFormat.equals("DXT5")) {
          ImageFormatWriter.writeDXT5(fm, imageResource);
          src.skip(width * height);
        }
        else if (imageFormat.equals("DXT3")) {
          ImageFormatWriter.writeDXT3(fm, imageResource);
          src.skip(width * height);
        }
        else if (imageFormat.equals("DXT1")) {
          ImageFormatWriter.writeDXT1(fm, imageResource);
          src.skip(width * height / 2);
        }
        else if (imageFormat.equals("RGB565")) {
          ImageFormatWriter.writeRGB565(fm, imageResource);
          src.skip(width * height * 2);
        }
        else if (imageFormat.equals("BGRA")) {
          ImageFormatWriter.writeBGRA(fm, imageResource);
          src.skip(width * height * 4);
        }
        else if (imageFormat.equals("ARGB1555")) {
          ImageFormatWriter.writeARGB1555(fm, imageResource);
          src.skip(width * height * 2);
        }
        else if (imageFormat.equals("BGR")) {
          ImageFormatWriter.writeBGR(fm, imageResource);
          src.skip(width * height * 3);
        }
        else if (imageFormat.equals("PAL36")) {
          // Palette
          ImageFormatWriter.writeRGB(fm, imageResource);

          // 3 bytes per color (RGB)
          //imageResource = ImageFormatReader.readRGB(fm, width, height);

          // 24bit RGB (confirmed RGB for NHL 2000 PC)
          /*
          int numColors = width;
          int[] palette = new int[numColors];
          for (int p = 0; p < numColors; p++) {
            int r = ByteConverter.unsign(src.readByte());
            int g = ByteConverter.unsign(src.readByte());
            int b = ByteConverter.unsign(src.readByte());
          
            palette[p] = ((255 << 24) | (r << 16) | (g << 8) | b);
          }
          */

          ImageManipulator im = new ImageManipulator(imageResource);
          im.convertToPaletted();

          int[] palette = im.getPalette();
          int numPaletteColors = palette.length;

          src.skip(numPaletteColors * 3);

          // store the global palette for use with other images
          globalPalette = palette;
        }
        else if (imageFormat.equals("PAL35")) {
          // Palette

          // get and write the new palette
          // 16bit BGR5551 (NHL 2001 PS1) (where 0,0,0 has a real alpha value, everything else should be treated as alpha = 255)
          ImageManipulator im = new ImageManipulator(imageResource);
          im.convertToPaletted();

          int[] palette = im.getPalette();
          int numPaletteColors = palette.length;

          for (int p = 0; p < numPaletteColors; p++) {
            // INPUT = ARGB
            int pixel = palette[p];

            ColorSplitAlpha color = new ColorSplitAlpha(pixel);

            // 5bits - Blue
            // 5bits - Green
            // 5bits - Red
            // 1bit  - Alpha
            int b = color.getBlue() / 8;
            int g = color.getGreen() / 8;
            int r = color.getRed() / 8;

            int a = 1;
            if (pixel == 0) { // 0,0,0,0 == transparent, everything else is full alpha
              a = 0;
            }

            // OUTPUT = BGRA5551
            //int shortValue = (b << 11) | (g << 6) | (r << 1) | a;
            //fm.writeShort(shortValue);

            int byte1 = r | ((g & 7) << 5);
            int byte2 = (a << 7) | (b << 2) | ((g >> 3) & 3);
            fm.writeByte(byte1);
            fm.writeByte(byte2);

            /*
            int b2 = ((byte2 >> 2) & 31);
            int g2 = (((byte2 & 3) << 3) | ((byte1 >> 5) & 7));
            int r2 = (byte1 & 31);
            int a2 = (byte2 >> 7);
            */
          }

          src.skip(numPaletteColors * 2);

          // store the global palette for use with other images
          globalPalette = palette;

        }
        else if (imageFormat.equals("ARGB4444")) {
          ImageFormatWriter.writeARGB4444(fm, imageResource);
          src.skip(width * height * 2);
        }
        else if (imageFormat.equals("8BitPaletted")) {
          // X - Pixels
          int numPixels = width * height;
          src.skip(numPixels);

          if (globalPaletteOffset != -1 && globalPalette == null) {
            // we need a global palette, but we don't have one, so need to generate one from this image
            ImageManipulator im = new ImageManipulator(imageResource);
            im.convertToPaletted();
            im.changeColorCount(256);

            int[] pixels = im.getPixels();
            for (int p = 0; p < numPixels; p++) {
              fm.writeByte(pixels[p]);
            }

            globalPalette = im.getPalette();
          }
          else if (globalPalette != null && (imageDataLengthBytes[2] == -1 || (imageDataLength == -16))) {
            // no palette or anything in this image, just had the pixel data and that's it

            // Apply the global color palette to the image, then get the pixels
            ImageManipulator im = new ImageManipulator(imageResource);
            im.convertToPaletted();
            im.swapAndConvertPalette(globalPalette);

            int[] pixels = im.getPixels();
            for (int p = 0; p < numPixels; p++) {
              fm.writeByte(pixels[p]);
            }

            if (imageDataLength == -16) {
              imageDataLength = 0;
            }
          }
          else {

            // NEED TO READ IN AND FIND THE NUMCOLORS BEFORE WE CAN WRITE INTO THE NEW FILE...

            // Now seek past all the image data (in case there's multiple mipmaps) to find the palette
            imageDataLength -= numPixels;

            boolean oddWidth = (width % 2 == 1);
            if (oddWidth && imageDataLength <= 0) {
              // not actually odd width for this image
              oddWidth = false;
            }

            if (oddWidth) {
              imageDataLength -= height; // an additional 1 byte for each row
              src.skip(height);
            }

            if (imageDataLength < 0) {
              ErrorLogger.log("[Viewer_BIG_BIGF_FSH_SHPI] Negative image data length for some reason: " + imageDataLength);
              imageDataLength = 0;
            }

            byte[] additionalMipmaps = src.readBytes(imageDataLength);

            boolean palettePadded = false;

            // 4 - Palette Format
            // 2 - Number of Colors?
            // 2 - Unknown (1)
            int paletteFormat = src.readByte();

            if ((paletteFormat == 111 || paletteFormat == 124) && globalPalette != null) {
              // global palette - not sure what this stuff is here, so just write the image, then copy this extra stuff

              // NOW CAN WRITE THINGS TO CATCH UP...

              // Apply the global color palette to the image, then get the pixels
              ImageManipulator im = new ImageManipulator(imageResource);
              im.convertToPaletted();
              im.swapAndConvertPalette(globalPalette);

              int[] pixels = im.getPixels();
              for (int p = 0; p < numPixels; p++) {
                fm.writeByte(pixels[p]);
              }

              int checkByte = paletteFormat;

              while (checkByte == paletteFormat) {
                // now write the other data, whatever it is
                fm.writeByte(paletteFormat);

                // we've now caught up to the src, fyi

                int length = 0;
                if (checkByte == 111) {
                  // 3 - Length Bytes
                  fm.writeBytes(src.readBytes(3));

                  // 4 - Length of Other data
                  length = src.readInt();
                  fm.writeInt(length);
                }
                else if (checkByte == 124) {
                  // 3 - Length Bytes
                  length = src.readShort();
                  fm.writeShort(length);

                  fm.writeBytes(src.readBytes(1));

                  // 4 - Length of Other data
                  fm.writeBytes(src.readBytes(4));

                  length -= 8; // this length includes these 2 fields

                  if (length < 8) {
                    length = 0;
                  }
                }

                // X - Other Data
                fm.writeBytes(src.readBytes(length));

                // read the next checkbyte to see if there's more data to copy
                if (length == 0) {
                  checkByte = 9999; // something impossibly big
                }
                else if (src.getRemainingLength() > 0) {
                  checkByte = src.readByte();
                }
                else {
                  checkByte = 9999; // something impossibly big
                }
              }

              if (checkByte != 9999) { // only go back 1 if we actually read 1
                src.relativeSeek(src.getOffset() - 1); // go back a byte
              }

            }
            else {
              // a proper palette (or something else we don't know about)

              byte[] paletteHeaderBytes = src.readBytes(7);

              while (paletteFormat == 0) { // skip over null padding at the end of the image
                paletteFormat = src.readByte();
                palettePadded = true;
              }

              // 4 - Number of Colors
              int numColors = src.readInt();

              // NOW CAN WRITE THINGS TO CATCH UP...

              // Get the pixels and palette for the new image
              ImageManipulator im = new ImageManipulator(imageResource);
              im.convertToPaletted();
              im.changeColorCount(numColors);

              int[] pixels = im.getPixels();
              int[] palette = im.getPalette();

              if (paletteFormat == 33 || paletteFormat == 35) {
                // palette[0] needs to be 0,0,0,0 and used for all pixels with no alpha.
                // Force this by changing the colors to 255 (giving us an empty one at the end), then re-arrange the palette 

                boolean swapped = false;

                // See if we already have a pixel with 0 alpha that we can use
                int numPaletteColors = palette.length;

                for (int p = 0; p < numPaletteColors; p++) {
                  if (new ColorSplitAlpha(palette[p]).getAlpha() == 0) {
                    // found one

                    // Force it to have RGB000
                    palette[p] = 0;

                    // if this is the first color in the palette, don't need to do anything else.
                    if (p == 0) {
                    }
                    else {
                      // need to make it the first palette color, then change the palette order around
                      int[] newPalette = new int[numColors];
                      System.arraycopy(palette, 0, newPalette, 0, numColors);

                      int oldColor0 = newPalette[0];
                      newPalette[0] = newPalette[p];
                      newPalette[p] = oldColor0;
                      im.swapPaletteOrder(newPalette);
                    }

                    swapped = true;
                    break;
                  }
                }

                if (!swapped) {
                  // didn't find any colors with alpha=0, so need to make one

                  // reduce the colors to 255 so we have an empty spot for 0,0,0,0
                  if (numColors >= 256) {
                    im.changeColorCount(255);
                    im.changeColorCount(256); // back to 256 colors again, to add the 0,0,0,0,
                  }

                  // swap the first and last colors (makes 0,0,0,0 the first color)
                  int[] newPalette = new int[numColors];
                  System.arraycopy(palette, 0, newPalette, 0, numColors);

                  int oldColor0 = newPalette[0];
                  newPalette[0] = newPalette[255];
                  newPalette[255] = oldColor0;

                  // swap the palette into the image
                  im.swapPaletteOrder(newPalette);

                }

                // get the new palette and pixels, after any modifications
                palette = im.getPalette();
                pixels = im.getPixels();
              }

              /*
              // Should be done in an earlier "if" statement, instead of coming here 
              if (globalPaletteOffset != -1 && globalPalette == null) {
                // we need a global palette (and it's not stored as a PAL entry in the directory), so grab this one
                globalPalette = palette;
              }
              */

              /*
              for (int p = 0; p < numPixels; p++) {
                fm.writeByte(pixels[p]);
              }
              */

              int pix = 0;
              for (int h = 0; h < height; h++) {
                for (int w = 0; w < width; w++) {
                  fm.writeByte(pixels[pix]);
                  pix++;
                }

                if (oddWidth) {
                  fm.writeByte(0);
                }
              }

              fm.writeBytes(additionalMipmaps);

              if (palettePadded) {
                fm.writeByte(0);
              }

              fm.writeByte(paletteFormat);
              fm.writeBytes(paletteHeaderBytes);

              fm.writeInt(numColors);

              // NOW CONTINUE SIDE-BY-SIDE...

              // 4 - flags (Striping = 8192)
              int flags = src.readInt();
              fm.writeInt(flags);

              // X - Color Palette
              if (paletteFormat == 42) {
                // 32bit BGRA
                ImageFormatWriter.writePaletteBGRA(fm, palette);
                src.skip(numColors * 4);
              }
              else if (paletteFormat == 33) {
                // 32bit RGBA (PS2 Striped)
                if (flags != 0) {
                  palette = ImageFormatReader.stripePalettePS2(palette);
                }
                //ImageFormatWriter.writePaletteRGBA(fm, palette);

                int numPaletteColors = palette.length;

                for (int p = 0; p < numPaletteColors; p++) {
                  // INPUT = ARGB
                  int pixel = palette[p];

                  // 1 - Red
                  int rPixel = (pixel >> 16) & 255;

                  // 1 - Green
                  int gPixel = (pixel >> 8) & 255;

                  // 1 - Blue
                  int bPixel = pixel & 255;

                  // 1 - Alpha
                  int aPixel = (pixel >> 24) & 255;

                  // reduce alpha by half for PS2
                  aPixel /= 2;
                  if (aPixel == 127) {
                    aPixel = 128;
                  }

                  // OUTPUT = RGBA
                  fm.writeByte(rPixel);
                  fm.writeByte(gPixel);
                  fm.writeByte(bPixel);
                  fm.writeByte(aPixel);
                }

                src.skip(numColors * 4);
              }
              else if (paletteFormat == 36) {
                // 24bit RGB (confirmed RGB for Triple Play 2000)
                ImageFormatWriter.writePaletteRGB(fm, palette);
                src.skip(numColors * 3);
              }
              else if (paletteFormat == 35) {
                // 16bit BGR5551 (NHL 2001 PS1) (where 0,0,0 has a real alpha value, everything else should be treated as alpha = 255)

                int numPaletteColors = palette.length;

                for (int p = 0; p < numPaletteColors; p++) {
                  // INPUT = ARGB
                  int pixel = palette[p];

                  ColorSplitAlpha color = new ColorSplitAlpha(pixel);

                  // 5bits - Blue
                  // 5bits - Green
                  // 5bits - Red
                  // 1bit  - Alpha
                  int b = color.getBlue() / 8;
                  int g = color.getGreen() / 8;
                  int r = color.getRed() / 8;

                  int a = 1;
                  if (pixel == 0) { // 0,0,0,0 == transparent, everything else is full alpha
                    a = 0;
                  }

                  // OUTPUT = BGRA5551
                  //int shortValue = (b << 11) | (g << 6) | (r << 1) | a;
                  //fm.writeShort(shortValue);

                  int byte1 = r | ((g & 7) << 5);
                  int byte2 = (a << 7) | (b << 2) | ((g >> 3) & 3);
                  fm.writeByte(byte1);
                  fm.writeByte(byte2);

                  /*
                  int b2 = ((byte2 >> 2) & 31);
                  int g2 = (((byte2 & 3) << 3) | ((byte1 >> 5) & 7));
                  int r2 = (byte1 & 31);
                  int a2 = (byte2 >> 7);
                  */
                }

                src.skip(numColors * 2);
              }
              else {
                ErrorLogger.log("[Viewer_BIG_BIGF_FSH_SHPI] Writing an Unknown Palette Format: " + paletteFormat);
                return;
              }

            }
          }

        }
        else if (imageFormat.equals("4BitPaletted")) {
          // X - Pixels
          int widthForCalc = width;
          if (widthForCalc % 2 == 1) {
            widthForCalc++;
          }
          boolean widthPadded = ((widthForCalc / 2) % 2) == 1;

          int numPixels = widthForCalc * height / 2;
          if (widthPadded) {
            numPixels += height; // add another 1 byte at the end of each row.
          }

          src.skip(numPixels);

          // NEED TO READ IN AND FIND THE NUMCOLORS BEFORE WE CAN WRITE INTO THE NEW FILE...

          // Now seek past all the image data (in case there's multiple mipmaps) to find the palette
          imageDataLength -= numPixels;
          byte[] additionalMipmaps = src.readBytes(imageDataLength);

          boolean palettePadded = false;

          // 4 - Palette Format
          // 2 - Number of Colors?
          // 2 - Unknown (1)
          int paletteFormat = src.readByte();

          byte[] paletteHeaderBytes = src.readBytes(7);

          while (paletteFormat == 0) { // skip over null padding at the end of the image
            paletteFormat = src.readByte();
            palettePadded = true;
          }

          // 4 - Number of Colors
          int numColors = src.readInt();

          // NOW CAN WRITE THINGS TO CATCH UP...

          // Get the pixels and palette for the new image
          ImageManipulator im = new ImageManipulator(imageResource);
          im.convertToPaletted();
          im.changeColorCount(numColors);

          int[] pixels = im.getPixels();
          int[] palette = im.getPalette();

          if (paletteFormat == 33 || paletteFormat == 35) {
            // palette[0] needs to be 0,0,0,0 and used for all pixels with no alpha.
            // Force this by changing the colors to 255 (giving us an empty one at the end), then re-arrange the palette 

            boolean swapped = false;

            // See if we already have a pixel with 0 alpha that we can use
            int numPaletteColors = palette.length;

            for (int p = 0; p < numPaletteColors; p++) {
              if (new ColorSplitAlpha(palette[p]).getAlpha() == 0) {
                // found one

                // Force it to have RGB000
                palette[p] = 0;

                // if this is the first color in the palette, don't need to do anything else.
                if (p == 0) {
                }
                else {
                  // need to make it the first palette color, then change the palette order around
                  int[] newPalette = new int[numColors];
                  System.arraycopy(palette, 0, newPalette, 0, numColors);

                  int oldColor0 = newPalette[0];
                  newPalette[0] = newPalette[p];
                  newPalette[p] = oldColor0;
                  im.swapPaletteOrder(newPalette);
                }

                swapped = true;
                break;
              }
            }

            if (!swapped) {
              // didn't find any colors with alpha=0, so need to make one

              // reduce the colors to 15 so we have an empty spot for 0,0,0,0
              if (numColors >= 16) {
                im.changeColorCount(15);
                im.changeColorCount(16); // back to 256 colors again, to add the 0,0,0,0,
              }

              // swap the first and last colors (makes 0,0,0,0 the first color)
              int[] newPalette = new int[numColors];
              System.arraycopy(palette, 0, newPalette, 0, numColors);

              int oldColor0 = newPalette[0];
              newPalette[0] = newPalette[15];
              newPalette[15] = oldColor0;

              // swap the palette into the image
              im.swapPaletteOrder(newPalette);

            }

            // get the new palette and pixels, after any modifications
            palette = im.getPalette();
            pixels = im.getPixels();
          }

          /*
          // Should be done in an earlier "if" statement, instead of coming here 
          if (globalPaletteOffset != -1 && globalPalette == null) {
            // we need a global palette (and it's not stored as a PAL entry in the directory), so grab this one
            globalPalette = palette;
          }
          */

          // write the pixels (2 pixels per byte)
          // 4-bits per color (The order of the pixels is verified by NHL 2001 PS1)
          int pix = 0;
          for (int h = 0; h < height; h++) {
            for (int w = 0; w < width; w += 2) {
              int pixel = pixels[pix]; // pixel & 15
              pix++;

              //int pixel2 = pixel >> 4;
              //int pixel1 = pixel & 15;

              if (w + 2 <= width) { // if even width, we do add the second pixel, otherwise odd width just ignores it
                pixel |= (pixels[pix] << 4);
                pix++;
              }

              fm.writeByte(pixel);
            }

            if (widthPadded) {
              fm.writeByte(0);
            }
          }

          fm.writeBytes(additionalMipmaps);

          if (palettePadded) {
            fm.writeByte(0);
          }

          fm.writeByte(paletteFormat);
          fm.writeBytes(paletteHeaderBytes);

          fm.writeInt(numColors);

          // NOW CONTINUE SIDE-BY-SIDE...

          // 4 - flags (Striping = 8192)
          int flags = src.readInt();
          fm.writeInt(flags);

          // X - Color Palette
          if (paletteFormat == 42) {
            // 32bit BGRA
            ImageFormatWriter.writePaletteBGRA(fm, palette);
            src.skip(numColors * 4);
          }
          else if (paletteFormat == 33) {
            // 32bit RGBA (PS2 Striped)
            if (flags != 0) {
              palette = ImageFormatReader.stripePalettePS2(palette);
            }
            //ImageFormatWriter.writePaletteRGBA(fm, palette);

            int numPaletteColors = palette.length;

            for (int p = 0; p < numPaletteColors; p++) {
              // INPUT = ARGB
              int pixel = palette[p];

              // 1 - Red
              int rPixel = (pixel >> 16) & 255;

              // 1 - Green
              int gPixel = (pixel >> 8) & 255;

              // 1 - Blue
              int bPixel = pixel & 255;

              // 1 - Alpha
              int aPixel = (pixel >> 24) & 255;

              // reduce alpha by half for PS2
              aPixel /= 2;
              if (aPixel == 127) {
                aPixel = 128;
              }

              // OUTPUT = RGBA
              fm.writeByte(rPixel);
              fm.writeByte(gPixel);
              fm.writeByte(bPixel);
              fm.writeByte(aPixel);
            }

            src.skip(numColors * 4);
          }
          else if (paletteFormat == 36) {
            // 24bit RGB (confirmed RGB for Triple Play 2000)
            ImageFormatWriter.writePaletteRGB(fm, palette);
            src.skip(numColors * 3);
          }
          else if (paletteFormat == 35) {
            // 16bit BGR5551 (NHL 2001 PS1) (where 0,0,0 has a real alpha value, everything else should be treated as alpha = 255)

            int numPaletteColors = palette.length;

            for (int p = 0; p < numPaletteColors; p++) {
              // INPUT = ARGB
              int pixel = palette[p];

              ColorSplitAlpha color = new ColorSplitAlpha(pixel);

              // 5bits - Blue
              // 5bits - Green
              // 5bits - Red
              // 1bit  - Alpha
              int b = color.getBlue() / 8;
              int g = color.getGreen() / 8;
              int r = color.getRed() / 8;

              int a = 1;
              if (pixel == 0) { // 0,0,0,0 == transparent, everything else is full alpha
                a = 0;
              }

              // OUTPUT = BGRA5551
              //int shortValue = (b << 11) | (g << 6) | (r << 1) | a;
              //fm.writeShort(shortValue);

              int byte1 = r | ((g & 7) << 5);
              int byte2 = (a << 7) | (b << 2) | ((g >> 3) & 3);
              fm.writeByte(byte1);
              fm.writeByte(byte2);

              /*
              int b2 = ((byte2 >> 2) & 31);
              int g2 = (((byte2 & 3) << 3) | ((byte1 >> 5) & 7));
              int r2 = (byte1 & 31);
              int a2 = (byte2 >> 7);
              */

            }

            src.skip(numColors * 2);
          }
          else {
            ErrorLogger.log("[Viewer_BIG_BIGF_FSH_SHPI] Writing an Unknown Palette Format: " + paletteFormat);
            return;
          }

        }

        // X - Data (up to the start of the next image, or to end of archive)
        int postDataLength = 0;
        if (i == numImages - 1) {
          postDataLength = (int) src.getRemainingLength(); // remaining archive length
        }
        else {
          // length up to the start of the next file
          postDataLength = (int) (offsets[i + 1] - src.getOffset());
        }

        if (postDataLength < 0) {
          ErrorLogger.log("[Viewer_BIG_BIGF_FSH_SHPI] Post Data length is negative for some reason: " + postDataLength);
          postDataLength = 0;
        }

        fm.writeBytes(src.readBytes(postDataLength));

      }

      if (globalPaletteOffset != -1 && globalPalette != null) {
        // we need a global palette (and it's not stored as a PAL entry in the directory). We grabbed one earlier, so
        // now we need to go back and write it out
        fm.seek(globalPaletteOffset);

        // 24bit RGB (confirmed RGB for NHL 2001 PC)
        ImageFormatWriter.writePaletteRGB(fm, globalPalette);
      }

      src.close();
      fm.close();

    }
    catch (

    Throwable t) {
      logError(t);
    }
  }

  /**
  **********************************************************************************************
    
  **********************************************************************************************
  **/
  @Override
  public void write(PreviewPanel panel, FileManipulator destination) {
  }

}