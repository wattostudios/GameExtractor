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

import java.io.File;

import org.watto.component.PreviewPanel;
import org.watto.component.PreviewPanel_Image;
import org.watto.datatype.Archive;
import org.watto.datatype.ImageResource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.helper.ImageFormatReader;
import org.watto.ge.plugin.AllFilesPlugin;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.io.FileManipulator;
import org.watto.io.converter.ByteConverter;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_BOP extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_BOP() {
    super("BOP", "BOP Image");
    setExtensions("bop");

    setGames("Warhammer: Shadow of the Horned Rat");
    setPlatforms("PC");
    setStandardFileFormat(false);

    setEnabled(false); // because we don't know what the Palette format is
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
      if (!(plugin instanceof AllFilesPlugin)) {
        return 0;
      }

      if (FieldValidator.checkExtension(fm, extensions)) {
        rating += 25;
      }
      else {
        return 0;
      }

      // see if there's a FOL file with the same name
      ArchivePlugin.getDirectoryFile(fm.getFile(), "FOL");
      rating += 25;

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

      // Get the FOL file
      File bopFile = fm.getFile();
      File folFile = null;
      File palFile = null;
      boolean sysPal = false;

      folFile = ArchivePlugin.getDirectoryFile(bopFile, "FOL"); // mandatory
      try {
        palFile = ArchivePlugin.getDirectoryFile(bopFile, "PAL"); // optional
      }
      catch (Throwable t) {
        // Try to get the SYS.PAL file instead
        String path = bopFile.getParentFile().getAbsolutePath();
        palFile = new File(path + File.separatorChar + "SYS.PAL");
        sysPal = true;
      }

      if (bopFile == null || folFile == null || palFile == null) {
        return null;
      }

      // Read the PAL File
      int numPalettes = (int) palFile.length() / 512;
      if (sysPal) {
        numPalettes = 1;
      }
      FieldValidator.checkNumFiles(numPalettes);

      FileManipulator palFM = new FileManipulator(palFile, false);

      int[][] palettes = new int[numPalettes][0];
      for (int i = 0; i < numPalettes; i++) {
        // for each color (256)
        if (sysPal) {
          // 20 colors only? but read to 256 colors anyway
          // 4 - Color Value (RGBA)
          palettes[i] = ImageFormatReader.readRGBA(palFM, 1, 256).getImagePixels();
        }
        else {
          // 2 - Color Value
          palettes[i] = ImageFormatReader.readRGB555(palFM, 1, 256).getImagePixels();
        }
      }

      palFM.close();

      // Read the FOL File
      int numImages = (int) folFile.length() / 16;
      FieldValidator.checkNumFiles(numImages);

      FileManipulator folFM = new FileManipulator(folFile, false);

      ImageResource[] images = new ImageResource[numImages];
      for (int i = 0; i < numImages; i++) {
        // 2 - null
        // 1 - Unknown (0/8/9)
        // 1 - Unknown (0/31)
        folFM.skip(4);

        // 2 - Image Width
        short width = folFM.readShort();
        FieldValidator.checkWidth(width);

        // 2 - Image Height
        short height = folFM.readShort();
        FieldValidator.checkHeight(height);

        // 4 - Image Data Offset
        int offset = folFM.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 4-bits - Palette Number
        // 4-bits - Palette Type (1=System Palette, 2=PAL file, 4=PAL file with RLE)
        int palDetails = ByteConverter.unsign(folFM.readByte());
        int palType = (palDetails & 15);
        int palNumber = (palDetails >> 4);

        // 1 - Unknown (2)
        // 1 - Unknown (0=Uncompressed?/4=compressed?)
        // 1 - Unknown (64)
        folFM.skip(3);

        // Read the BOP for this image
        fm.relativeSeek(offset);
        //System.out.println(offset);

        int[] palette = palettes[palNumber];
        ImageResource imageResource = null;
        if (palType == 1 || palType == 2) {
          // uncompressed
          imageResource = ImageFormatReader.read8BitPaletted(folFM, width, height, palette);
        }
        else if (palType == 4) {
          // RLE
          int outPos = 0;
          int numPixels = width * height;
          int[] pixels = new int[numPixels];
          while (outPos < numPixels) {
            // 1 - Control Character
            int control = ByteConverter.unsign(fm.readByte());
            if (control == 0) {
              // 1 - Number of Transparent Pixels
              int count = ByteConverter.unsign(fm.readByte());
              if (count == 0) {
                break;
              }
              outPos += count;
            }
            else {
              // control character is a palette index
              pixels[outPos] = palette[control];
              outPos++;
            }
          }

          imageResource = new ImageResource(pixels, width, height);
        }

        images[i] = imageResource;
      }

      folFM.close();

      fm.close();

      // Set frame navigation
      if (numImages > 1) {
        for (int i = 0; i < numImages; i++) {
          ImageResource image = images[i];
          if (i == 0) {
            image.setNextFrame(images[i + 1]);
            image.setPreviousFrame(images[numImages - 1]);
          }
          else if (i == numImages - 1) {
            image.setNextFrame(images[0]);
            image.setPreviousFrame(images[i - 1]);
          }
          else {
            image.setNextFrame(images[i + 1]);
            image.setPreviousFrame(images[i - 1]);
          }
        }
      }

      return images[0];

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