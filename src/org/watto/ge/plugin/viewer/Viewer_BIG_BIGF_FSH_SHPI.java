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
import org.watto.ge.plugin.archive.Plugin_BIG_BIGF;
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
    setExtensions("fsh", "shpi");

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
        "NHL 06",
        "SimCity 4");
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
      if (plugin instanceof Plugin_BIG_BIGF || plugin instanceof Plugin_SHD_MRTS || plugin instanceof Plugin_CCD_FKNL || plugin instanceof Plugin_DAT_DBPF || plugin instanceof Plugin_VIV) {
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
      if (header.equals("SHPI") || header.equals("ShpF")) {
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

      // 4 - Header (SHPI)
      String header = fm.readString(4);
      if (header.equals("SHPI") || header.equals("ShpF")) {
        // not compressed
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
      // 4 - Number of Images
      // 4 - Image Group ID
      fm.skip(12);

      int offset = 0;
      if (header.equals("SHPI")) {
        // 4 - (FIRST IMAGE) Image Code Name
        fm.skip(4);

        // 4 - (FIRST IMAGE) Offset to Image Data
        offset = fm.readInt();
        FieldValidator.checkOffset(offset, fm.getLength());
      }
      else if (header.equals("ShpF")) {
        // 4 - (FIRST IMAGE) Offset to Image Data (BIG)
        offset = IntConverter.changeFormat(fm.readInt());
        FieldValidator.checkOffset(offset, fm.getLength());
      }

      fm.seek(offset);

      // 1 - Image Format
      int imageType = ByteConverter.unsign(fm.readByte());

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
      else {
        ErrorLogger.log("Viewer_BIG_BIGF_FSH_SHPI: Unknown Image Format: " + imageType);
        return null;
      }

      // 3 - Image Data Length (including these 16-bytes of header, and possibly multiple mipmaps)
      //fm.skip(3);
      byte[] imageDataLengthBytes = new byte[] { fm.readByte(), fm.readByte(), fm.readByte(), 0 };
      int imageDataLength = IntConverter.convertLittle(imageDataLengthBytes);

      int height = 0;
      int width = 0;
      if (header.equals("SHPI")) {
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
      else if (header.equals("ShpF")) {
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
      else if (imageFormat.equals("ARGB4444")) {
        imageResource = ImageFormatReader.readARGB4444(fm, width, height);
      }
      else if (imageFormat.equals("8BitPaletted")) {
        // X - Pixels
        int numPixels = width * height;
        int[] pixels = new int[numPixels];

        for (int i = 0; i < numPixels; i++) {
          pixels[i] = ByteConverter.unsign(fm.readByte());
        }

        // Now seek past all the image data (in case there's multiple mipmaps) to find the palette
        offset += imageDataLength;
        FieldValidator.checkOffset(offset, fm.getLength());
        fm.seek(offset);

        // 4 - Palette Format
        int paletteFormat = fm.readByte();
        while (paletteFormat == 0) { // skip over null padding at the end of the image
          paletteFormat = fm.readByte();
        }
        fm.skip(3);

        // 2 - Number of Colors?
        // 2 - Unknown (1)
        fm.skip(4);

        // 4 - Number of Colors
        int numColors = fm.readInt();

        // 4 - null
        fm.skip(4);

        // X - Color Palette
        int[] palette = new int[numColors];
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
        else if (paletteFormat == 36) {
          // 24bit RGB (confirmed RGB for Triple Play 2000)
          for (int i = 0; i < numColors; i++) {
            int r = ByteConverter.unsign(fm.readByte());
            int g = ByteConverter.unsign(fm.readByte());
            int b = ByteConverter.unsign(fm.readByte());

            palette[i] = ((255 << 24) | (r << 16) | (g << 8) | b);
          }
        }
        else {
          ErrorLogger.log("Viewer_BIG_BIGF_FSH_SHPI: Unknown Palette Format: " + paletteFormat);
          return null;
        }

        for (int i = 0; i < numPixels; i++) {
          pixels[i] = palette[pixels[i]];
        }

        imageResource = new ImageResource(pixels, width, height);
      }

      fm.close();

      if (imageResource == null) {
        return null;
      }

      imageResource.addProperty("ImageFormat", "" + imageFormat);

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