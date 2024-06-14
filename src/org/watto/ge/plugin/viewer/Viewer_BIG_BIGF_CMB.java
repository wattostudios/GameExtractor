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
import org.watto.ErrorLogger;
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
import org.watto.ge.plugin.archive.Plugin_BIG_BIGF;
import org.watto.io.FileManipulator;
import org.watto.io.buffer.ByteBuffer;
import org.watto.io.converter.ByteConverter;
import org.watto.io.converter.IntConverter;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_BIG_BIGF_CMB extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_BIG_BIGF_CMB() {
    super("BIG_BIGF_CMB", "BIG_BIGF_CMB");
    setExtensions("cmb");

    setGames("NHL 2002");
    setPlatforms("PS2");
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
      if (plugin instanceof Plugin_BIG_BIGF) {
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

      // 4 - Offset to SSH Image
      long offset = fm.readInt();
      if (FieldValidator.checkOffset(offset, fm.getLength())) {
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

      // 4 - Offset to SSH Image
      int sshOffset = fm.readInt();
      FieldValidator.checkOffset(sshOffset, arcSize);

      int sshSkip = sshOffset - 4;
      FieldValidator.checkPositive(sshSkip);
      fm.skip(sshSkip);

      // 4 - Header (SHPS)
      String header = fm.readString(4);
      if (header.equals("SHPS")) {
        // Found the image
      }
      else {
        return null;
      }

      // 4 - File Length
      fm.skip(4);

      // 4 - Number of Images
      int numImages = fm.readInt();
      FieldValidator.checkNumFiles(numImages);

      if (readThumbnailOnly) {
        numImages = 1; // only want to generate a single thumbnail image for "thumbnails", but generate all for "previews"
      }

      // 4 - Image Group ID
      fm.skip(4);

      if (numImages == 1) {
        // A SHPS with a single image in it

        int offset = 0;
        if (header.equals("SHPS")) {
          // 4 - (FIRST IMAGE) Image Code Name
          fm.skip(4);

          // 4 - (FIRST IMAGE) Offset to Image Data
          offset = fm.readInt() + sshOffset; // NOTE: +sshOffset
          FieldValidator.checkOffset(offset, fm.getLength());
        }

        ImageResource image = readSingleImage(fm, offset, header);

        fm.close();

        return image;
      }
      else {
        // multiple images in an SHPS only

        int[] offsets = new int[numImages];
        for (int i = 0; i < numImages; i++) {
          // 4 - Image Code Name
          fm.skip(4);

          // 4 - Offset to Image Data
          int offset = fm.readInt() + sshOffset; // NOTE: +sshOffset
          FieldValidator.checkOffset(offset, arcSize);
          offsets[i] = offset;
        }

        ImageResource[] images = new ImageResource[numImages];
        for (int i = 0; i < numImages; i++) {
          ImageResource imageResource = readSingleImage(fm, offsets[i], header);
          images[i] = imageResource;
        }

        // set the next/previous images
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

        ImageResource firstImage = images[0];
        firstImage.setManualFrameTransition(true);

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

      //fm.seek(offset);
      int skipSize = (int) (offset - fm.getOffset());
      FieldValidator.checkPositive(skipSize);
      fm.skip(skipSize);

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
      else {
        ErrorLogger.log("[Viewer_BIG_BIGF_CMB] Unknown Image Format: " + imageType + " at offset " + offset);
        return null;
      }

      // 3 - Image Data Length (including these 16-bytes of header, and possibly multiple mipmaps)
      //fm.skip(3);
      byte[] imageDataLengthBytes = new byte[] { fm.readByte(), fm.readByte(), fm.readByte(), 0 };
      int imageDataLength = IntConverter.convertLittle(imageDataLengthBytes);

      int height = 0;
      int width = 0;
      if (header.equals("SHPS")) {
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

        boolean oddWidth = (width % 2 == 1);

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

        //for (int i = 0; i < numPixels; i++) {
        //  pixels[i] = ByteConverter.unsign(fm.readByte());
        //}

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
        //fm.skip(4);
        int paletteStripeCheck = fm.readInt();
        if (paletteStripeCheck == 0) { // not striped
          ps2Striped = false;
        }

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
        else {
          ErrorLogger.log("[Viewer_BIG_BIGF_CMB] Unknown Palette Format: " + paletteFormat);
          return null;
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

      // 4 - Offset to SSH Image
      int sshOffset = src.readInt();
      fm.writeInt(sshOffset);

      // X - Unknown pre-data
      int preSize = sshOffset - 4;
      FieldValidator.checkPositive(preSize);
      fm.writeBytes(src.readBytes(preSize));

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
      int preDataLength = (int) (offsets[0] + sshOffset - src.getOffset()); // extra data between current position and first offset // NOTE +sshOffset
      fm.writeBytes(src.readBytes(preDataLength));

      for (int i = 0; i < numImages; i++) {

        // move to the next frame, if this isn't the first image
        if (i > 0) {
          imageResource = imageResource.getNextFrame();
          if (imageResource == null) {
            ErrorLogger.log("[Viewer_BIG_BIGF_CMB] Expected " + numImages + " images but only imported " + i + " from the filesystem");
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
        else if (imageType == 2) {
          imageFormat = "8BitPaletted";
        }
        else {
          ErrorLogger.log("[Viewer_BIG_BIGF_CMB] Writing an Unknown Image Format: " + imageType);
          return;
        }

        // 3 - Image Data Length
        byte[] imageDataLengthBytes = new byte[] { src.readByte(), src.readByte(), src.readByte(), 0 };
        int imageDataLength = IntConverter.convertLittle(imageDataLengthBytes);
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
        else if (imageFormat.equals("ARGB4444")) {
          ImageFormatWriter.writeARGB4444(fm, imageResource);
          src.skip(width * height * 2);
        }
        else if (imageFormat.equals("8BitPaletted")) {
          // X - Pixels
          int numPixels = width * height;
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

          for (int p = 0; p < numPixels; p++) {
            fm.writeByte(pixels[p]);
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
          else {
            ErrorLogger.log("[Viewer_BIG_BIGF_CMB] Writing an Unknown Palette Format: " + paletteFormat);
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
          postDataLength = (int) (offsets[i + 1] + sshOffset - src.getOffset()); // NOTE: +sshOffset
        }

        if (postDataLength < 0) {
          ErrorLogger.log("[Viewer_BIG_BIGF_CMB] Post Data length is negative for some reason: " + postDataLength);
          postDataLength = 0;
        }

        fm.writeBytes(src.readBytes(postDataLength));

      }

      src.close();
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
  @Override
  public void write(PreviewPanel panel, FileManipulator destination) {
  }

}