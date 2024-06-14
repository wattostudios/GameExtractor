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
import org.watto.component.PreviewPanel;
import org.watto.component.PreviewPanel_Image;
import org.watto.datatype.Archive;
import org.watto.datatype.ImageResource;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.helper.ImageFormatReader;
import org.watto.ge.helper.ImageManipulator;
import org.watto.ge.plugin.AllFilesPlugin;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.ge.plugin.archive.Plugin_CDF_TXTR;
import org.watto.ge.plugin.archive.Plugin_DAT_DAT;
import org.watto.io.FileManipulator;
import org.watto.io.buffer.ByteBuffer;
import org.watto.io.converter.ByteConverter;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_DAT_DAT_IFF_TXTR extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_DAT_DAT_IFF_TXTR() {
    super("DAT_DAT_IFF_TXTR", "ESPN NHL Hockey IFF Image");
    setExtensions("iff");

    setGames("ESPN NHL Hockey");
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
      if (plugin instanceof Plugin_DAT_DAT || plugin instanceof Plugin_CDF_TXTR) {
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
      if (fm.readString(4).equals("TXTR")) {
        rating += 50;
      }
      else {
        rating = 0;
      }

      long arcSize = fm.getLength();

      // 4 - File Length
      if (FieldValidator.checkLength(fm.readInt(), arcSize)) {
        rating += 5;
      }

      // 4 - File Length
      if (FieldValidator.checkLength(fm.readInt(), arcSize)) {
        rating += 5;
      }

      fm.skip(16);

      // 4 - Header
      if (fm.readString(4).equals("TXTR")) {
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

      int numImages = 0;
      ImageResource[] imageResources = new ImageResource[100]; // guessing a maximum

      while (fm.getOffset() < arcSize) {
        long startOffset = fm.getOffset();

        // 4 - Header (TXTR)
        String header = fm.readString(4);

        // 4 - File Length [+16]
        long endOffset = startOffset + fm.readInt() + 16;
        FieldValidator.checkOffset(endOffset, arcSize + 1);

        if (!header.equals("TXTR")) {
          fm.relativeSeek(endOffset); // skip blocks that aren't textures
          continue;
        }

        // 4 - File Length [+16]
        // 4 - null
        // 12 - null
        // 4 - Header (TXTR)
        // 4 - Unknown (17)
        // 4 - Unknown (21)
        // 8 - null
        fm.skip(40);

        // X - Description
        // 1 - null Description Terminator
        String description = fm.readNullString();

        // 0-3 - null Padding to a multiple of 4 bytes
        int descriptionLength = description.length();
        int paddingSize = ArchivePlugin.calculatePadding(descriptionLength + 1, 4);
        fm.skip(paddingSize);

        // 4 - Unknown (-7)
        while (fm.readInt() == 0) {
          // read the next int
          if (fm.getOffset() >= arcSize) {
            break; // just in case
          }
        }
        // 4 - null
        // 2 - Unknown
        // 2 - Unknown
        fm.skip(8);

        // 2 - Image Width?
        short width = fm.readShort();
        FieldValidator.checkWidth(width);

        // 2 - Image Height?
        short height = fm.readShort();
        FieldValidator.checkHeight(height);

        // 4 - Unknown
        // 4 - Unknown (8)
        // 8 - null
        // 4 - Unknown (185)
        // 4 - Image Data Length? (for all mipmaps + palettes etc)
        // 4 - null
        // 4 - Unknown (45)
        fm.skip(32);

        // for each mipmap (7 times - values are null if there are no more mipmaps)
        //   4 - Image Data Offset (relative to the start of the Image Data)
        fm.skip(4 * 7);

        // 4 - Palette Data Offset (relative to ???)
        // 16 - null
        // 4 - Unknown (-103)
        // 2 - Unknown
        // 2 - Unknown
        fm.skip(28);

        // 4 - Palette Offset [+151 + descriptionLength + nullTerminator + nullPadding]
        //long paletteOffset = startOffset + fm.readInt() + 151 + descriptionLength + 1 + paddingSize;
        fm.skip(4);
        long paletteOffset = endOffset - 1024;
        FieldValidator.checkOffset(paletteOffset, arcSize);

        // 4 - null
        // 4 - Palette Length (1024)
        // 100 - null

        long dataOffset = startOffset + 272;
        fm.relativeSeek(dataOffset);

        // X - Image Data
        int numPixels = width * height;
        byte[] pixelData = fm.readBytes(numPixels);

        fm.relativeSeek(paletteOffset);

        // 1024 - Color Palette (RGBA)
        int[] palette = ImageFormatReader.readPaletteRGBA(fm, 256);
        palette = ImageFormatReader.stripePalettePS2(palette);

        int[] pixels = new int[numPixels];
        for (int i = 0; i < numPixels; i++) {
          pixels[i] = palette[ByteConverter.unsign(pixelData[i])];
        }

        ImageResource imageResource = new ImageResource(pixels, width, height);
        imageResource = ImageFormatReader.doubleAlpha(imageResource);

        imageResources[numImages] = imageResource;

        numImages++;
      }

      ImageResource imageResource = imageResources[0];

      if (numImages > 1) {
        // set the previous and next images

        for (int i = 0; i < numImages - 1; i++) {
          imageResources[i].setNextFrame(imageResources[i + 1]);
        }

        for (int i = 1; i < numImages; i++) {
          imageResources[i].setPreviousFrame(imageResources[i - 1]);
        }

        imageResources[0].setPreviousFrame(imageResources[numImages - 1]);
        imageResources[numImages - 1].setNextFrame(imageResources[0]);

        imageResource.setManualFrameTransition(true);
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

      ImageManipulator im = new ImageManipulator((PreviewPanel_Image) preview);

      // Build the new file using the src[] and adding in the new image content

      // 4 - Header (TXTR)
      // 4 - File Length [+16]
      // 4 - File Length [+16]
      // 4 - null
      // 12 - null
      // 4 - Header (TXTR)
      // 4 - Unknown (17)
      // 4 - Unknown (21)
      // 8 - null
      fm.writeBytes(src.readBytes(48));

      // X - Description
      // 1 - null Description Terminator
      String description = src.readNullString();

      // 0-3 - null Padding to a multiple of 4 bytes
      int paddingSize = ArchivePlugin.calculatePadding(description.length() + 1, 4);
      src.skip(paddingSize);

      fm.writeString(description);
      fm.writeByte(0);
      for (int p = 0; p < paddingSize; p++) {
        fm.writeByte(0);
      }

      // 4 - Unknown (-7)
      // 4 - null
      // 2 - Unknown
      // 2 - Unknown
      // 2 - Image Width?
      // 2 - Image Height?
      // 4 - Unknown
      // 4 - Unknown (8)
      // 8 - null
      // 4 - Unknown (185)
      // 4 - Image Data Length? (for all mipmaps + palettes etc)
      // 4 - null
      // 4 - Unknown (45)
      fm.writeBytes(src.readBytes(48));

      // for each mipmap (7 times - values are null if there are no more mipmaps)
      //   4 - Image Data Offset (relative to the start of the Image Data)
      fm.writeBytes(src.readBytes(4 * 7));

      // 4 - Palette Data Offset (relative to ???)
      // 16 - null
      // 4 - Unknown (-103)
      // 2 - Unknown
      // 2 - Unknown
      // 4 - Palette Offset [+151 + descriptionLength + nullTerminator + nullPadding]
      // 4 - null
      // 4 - Palette Length (1024)
      // 100 - null
      fm.writeBytes(src.readBytes(140));

      // convert to paletted, and then stripe the palette
      im.convertToPaletted();
      im.changeColorCount(256);
      int[] palette = im.getPalette();
      palette = ImageFormatReader.stripePalettePS2(palette);
      im.setPalette(palette);

      // Generate all the mipmaps of the image
      /*
      ImageManipulator[] mipmaps = im.generatePalettedMipmaps();
      int mipmapCount = mipmaps.length;
      
      if (mipmapCount > 5) {
        mipmapCount = 5;
      }
      */

      // X - Mipmaps (only doing 1)
      //for (int i = 0; i < mipmapCount; i++) {
      //ImageManipulator mipmap = mipmaps[i];
      ImageManipulator mipmap = im;

      int pixelCount = mipmap.getNumPixels();
      int[] pixels = mipmap.getPixels();

      for (int p = 0; p < pixelCount; p++) {
        fm.writeByte(pixels[p]);
      }

      //}

      // X - Palette 
      int numColors = palette.length;

      for (int i = 0; i < numColors; i++) {
        // INPUT = ARGB
        int pixel = palette[i];

        // 1 - Red
        int rPixel = (pixel >> 16) & 255;

        // 1 - Green
        int gPixel = (pixel >> 8) & 255;

        // 1 - Blue
        int bPixel = pixel & 255;

        // 1 - Alpha
        int aPixel = (pixel >> 24) & 255;

        // REDUCE THE ALPHA BY 1/2
        if (aPixel == 255) {
          aPixel = 128;
        }
        else {
          aPixel /= 2;
        }

        // OUTPUT = RGBA
        fm.writeByte(rPixel);
        fm.writeByte(gPixel);
        fm.writeByte(bPixel);
        fm.writeByte(aPixel);
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
  public void write(PreviewPanel preview, FileManipulator fm) {
  }

}