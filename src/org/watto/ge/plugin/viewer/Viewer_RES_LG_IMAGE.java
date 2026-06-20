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
import org.watto.SingletonManager;
import org.watto.component.PreviewPanel;
import org.watto.component.PreviewPanel_Image;
import org.watto.datatype.Archive;
import org.watto.datatype.ImageResource;
import org.watto.datatype.Palette;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.helper.ImageFormatReader;
import org.watto.ge.helper.PaletteManager;
import org.watto.ge.plugin.AllFilesPlugin;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.ge.plugin.archive.Plugin_RES_LG;
import org.watto.io.FileManipulator;
import org.watto.io.buffer.ByteBuffer;
import org.watto.io.converter.ByteConverter;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_RES_LG_IMAGE extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_RES_LG_IMAGE() {
    super("RES_LG_IMAGE", "RES_LG_IMAGE Image");
    setExtensions("image");

    setGames("British Open Championship Golf");
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
      if (plugin instanceof Plugin_RES_LG) {
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

      fm.skip(4);

      int imageType = fm.readByte();
      if (imageType == 2 || imageType == 4) {
        rating += 5;
      }

      fm.skip(3);

      // 2 - Image Width
      if (FieldValidator.checkWidth(fm.readShort())) {
        rating += 5;
      }

      // 2 - Image Height
      if (FieldValidator.checkHeight(fm.readShort())) {
        rating += 5;
      }

      // 2 - Image Width (Stride)
      if (FieldValidator.checkWidth(fm.readShort())) {
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

      // 4 - Unknown
      fm.skip(4);

      // 1 - Type (2=Uncompressed, 4=RLE Compressed)

      int imageType = fm.readByte();

      // 1 - Palette Number?
      fm.skip(1);

      // 2 - Transparency Flag
      int transparencyFlag = fm.readByte();
      fm.skip(1);

      // 2 - Image Width
      fm.skip(2);

      // 2 - Image Height
      short height = fm.readShort();
      FieldValidator.checkHeight(height);

      // 2 - Stride
      short width = fm.readShort();
      FieldValidator.checkWidth(width);

      // 1 - Width Factor
      // 1 - Height Fator
      // 2 - Box 1
      // 2 - Box 2
      // 2 - Box 3
      // 2 - Box 4
      fm.skip(10);

      // 4 - Palette Offset [-6]
      int paletteOffset = fm.readInt();

      byte[] pixels = null;
      if (imageType == 2) {
        // uncompressed
        int numPixels = width * height;
        pixels = fm.readBytes(numPixels);
      }
      else if (imageType == 4) {
        // RLE
        int numPixels = width * height;
        pixels = new byte[numPixels];
        decompressRLE(fm, pixels);
      }
      else {
        ErrorLogger.log("[Viewer_RES_LG_IMAGE] Unsupported Image Format: " + imageType);
        return null;
      }

      int[] palette = null;
      boolean usingPaletteManager = false;

      if (paletteOffset != 0) {
        // 2 - Unknown (0)
        // 2 - Number of Colors (256)
        fm.skip(4);

        palette = ImageFormatReader.readPaletteRGB(fm, 256);
      }
      else {
        // find the previous file in the archive, which should be a Palette in RGB565 format

        Object resourceObject = SingletonManager.get("CurrentResource");
        if (resourceObject == null || !(resourceObject instanceof Resource)) {
          return null;
        }

        Resource resource = (Resource) resourceObject;
        Resource paletteResource = null;

        // Find the current resource in the archive
        Resource[] resources = Archive.getResources();
        int numResources = resources.length;
        for (int i = 0; i < numResources; i++) {
          if (resources[i] == resource) {
            // found the current image, so the file before it should be the palette
            if (i <= 0) {
              break;
            }
            paletteResource = resources[i - 1];
            break;
          }
        }

        if (paletteResource == null || paletteResource.getDecompressedLength() != 512) {
          // not a valid palette, use grayscale instead
          //palette = PaletteGenerator.getGrayscale();

          // load all the palette files that we find in the archive
          usingPaletteManager = true;
          if (PaletteManager.getNumPalettes() == 0) {
            for (int i = 0; i < numResources; i++) {
              paletteResource = resources[i];
              if (paletteResource.getExtension().equals("palette512")) {
                // read the palette file
                int palLength = (int) paletteResource.getLength();

                ByteBuffer palBuffer = new ByteBuffer(palLength);
                FileManipulator palFM = new FileManipulator(palBuffer);
                paletteResource.extract(palFM);

                palFM.seek(0);

                palette = ImageFormatReader.readRGB565(palFM, 256, 1).getImagePixels();

                // add the palette to the PaletteManager
                PaletteManager.addPalette(new Palette(palette), false);
              }
            }
          }
        }
        else {
          // read the palette file
          int palLength = (int) paletteResource.getLength();

          ByteBuffer palBuffer = new ByteBuffer(palLength);
          FileManipulator palFM = new FileManipulator(palBuffer);
          paletteResource.extract(palFM);

          palFM.seek(0);

          palette = ImageFormatReader.readRGB565(palFM, 256, 1).getImagePixels();
        }
      }

      if (transparencyFlag == 1 && !usingPaletteManager) {
        palette[0] = 0;
      }

      fm.close();
      fm = new FileManipulator(new ByteBuffer(pixels));

      // X - Pixels
      ImageResource imageResource = null;
      if (usingPaletteManager) {
        // tell it to allow the user to choose the palette
        imageResource = ImageFormatReader.read8BitPaletted(fm, width, height, true);
      }
      else {
        // read using the palette we've already found
        imageResource = ImageFormatReader.read8BitPaletted(fm, width, height, palette);
      }

      fm.close();

      //ColorConverter.convertToPaletted(resource);

      return imageResource;

    }
    catch (Throwable t) {
      logError(t);
      return null;
    }
  }

  public void decompressRLE(FileManipulator fm, byte[] buffer) {
    int outIndex = 0;
    int outLength = buffer.length;
    boolean done = false;

    while (!done && outIndex < outLength) {
      int first = ByteConverter.unsign(fm.readByte());

      if (first == 0x00) {
        int count = ByteConverter.unsign(fm.readByte());
        int value = ByteConverter.unsign(fm.readByte());

        //outIndex += writeBytesOfValue(output[outIndex:outIndex+int(nn)], func() byte { return zz });
        for (int i = 0; i < count; i++) {
          buffer[outIndex] = (byte) value;
          outIndex++;
        }
      }
      else if (first < 0x80) {
        //outIndex += writeBytesOfValue(output[outIndex:outIndex+int(first)], nextByte);
        int count = first;
        for (int i = 0; i < count; i++) {
          buffer[outIndex] = fm.readByte();
          outIndex++;
        }
      }
      else if (first == 0x80) {
        int control = ByteConverter.unsign(fm.readByte());
        control += (ByteConverter.unsign(fm.readByte()) << 8);
        if (control == 0x0000) {
          done = true;
        }
        else if (control < 0x8000) {
          outIndex += control;
        }
        else if (control < 0xC000) {
          //outIndex += writeBytesOfValue(output[outIndex:outIndex+int(control&0x3FFF)], nextByte);
          int count = (control & 0x3FFF);
          for (int i = 0; i < count; i++) {
            buffer[outIndex] = fm.readByte();
            outIndex++;
          }
        }
        else if ((control & 0xFF00) == 0xC000) {
          //err = fmt.Errorf("Undefined case 80 nn C0")
          done = true;
        }
        else {
          int value = ByteConverter.unsign(fm.readByte());
          //outIndex += writeBytesOfValue(output[outIndex:outIndex+int(control&0x3FFF)], func() byte { return zz });
          int count = (control & 0x3FFF);
          for (int i = 0; i < count; i++) {
            buffer[outIndex] = (byte) value;
            outIndex++;
          }
        }
      }
      else {
        outIndex += (first & 0x7F);
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