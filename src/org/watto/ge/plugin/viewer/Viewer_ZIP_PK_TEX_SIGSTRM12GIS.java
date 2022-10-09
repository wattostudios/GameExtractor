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
import org.watto.ge.plugin.archive.Plugin_ZIP_PK;
import org.watto.io.FileManipulator;
import org.watto.io.FilenameSplitter;
import org.watto.io.buffer.ByteBuffer;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_ZIP_PK_TEX_SIGSTRM12GIS extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_ZIP_PK_TEX_SIGSTRM12GIS() {
    super("ZIP_PK_TEX_SIGSTRM12GIS", "Serious Sam: The First Encounter TEX image");
    setExtensions("tex");

    setGames("Serious Sam: The First Encounter");
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
      if (plugin instanceof Plugin_ZIP_PK) {
        if (FilenameSplitter.getExtension(Archive.getBasePath()).equalsIgnoreCase("gro")) {
          rating += 50;
        }
        else {
          rating += 5;
        }
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

      // 12 - Header
      if (fm.readString(12).equals("SIGSTRM12GIS")) {
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

      // 12 - Header (SIGSTRM12GIS)
      // 28 - Unknown
      fm.skip(40);

      // 4 - Unknown Name String Length
      int nameLength = fm.readInt();
      FieldValidator.checkFilenameLength(nameLength + 1); // +1 to allow nulls

      // X - Unknown Name String
      fm.skip(nameLength);

      // 4 - Signature Length (256)
      fm.skip(4);

      // 4 - Signature Format Name Length
      nameLength = fm.readInt();
      FieldValidator.checkFilenameLength(nameLength);

      // X - Signature Format String
      fm.skip(nameLength);

      // 256 - Signature
      fm.skip(256);

      int blockOffset = (int) fm.getOffset();

      // scan through, find the DCON block
      boolean foundDCON = false;
      int currentByte = fm.readByte();
      for (int f = 0; f < 4096; f++) {

        if (currentByte == 68) {
          currentByte = fm.readByte();
          if (currentByte == 67) {
            currentByte = fm.readByte();
            if (currentByte == 79) {
              currentByte = fm.readByte();
              if (currentByte == 78) {
                // found it
                foundDCON = true;
                break;
              }
            }
          }
        }
        else {
          currentByte = fm.readByte();
        }
      }

      if (!foundDCON) {
        return null;
      }

      // 40 - Unknown
      fm.skip(40);

      // 4 - Image Width
      int width = fm.readInt();
      FieldValidator.checkWidth(width);

      // 4 - Image Height
      int height = fm.readInt();
      FieldValidator.checkHeight(height);

      // 4 - Unknown (1)
      fm.skip(4);

      // 4 - Image Format? (10=DXT5, 8=RGBA, 15=?)
      int imageFormat = fm.readInt();

      // 4 - Block Header (STAR)
      fm.skip(4);

      // 4 - Image Data Length
      int imageDataLength = fm.readInt();
      FieldValidator.checkLength(imageDataLength, arcSize);

      // read in the image data, noting that we have to skip 256-byte signatures every 65536 bytes
      byte[] imageBytes = new byte[imageDataLength];

      int blockSize = (int) (65536 - fm.getOffset() + blockOffset); // first block is smaller, because it contains all the header details as well
      if (imageDataLength < blockSize) {
        blockSize = imageDataLength;
      }

      int writePos = 0;
      while (imageDataLength > 0) {

        for (int i = 0; i < blockSize; i++) {
          imageBytes[writePos++] = fm.readByte();
        }

        imageDataLength -= blockSize;

        if (imageDataLength > 0) {
          // skip the 256-byte signature
          fm.skip(256);

          // set the next block size
          blockSize = 65536;

          if (imageDataLength < blockSize) {
            blockSize = imageDataLength;
          }
        }
      }

      fm.close();
      fm = new FileManipulator(new ByteBuffer(imageBytes));

      // X - Pixels
      ImageResource imageResource = null;
      if (imageFormat == 10) {
        imageResource = ImageFormatReader.readDXT5(fm, width, height);
      }
      else if (imageFormat == 8) {
        imageResource = ImageFormatReader.readRGBA(fm, width, height);
      }
      else if (imageFormat == 15) {
        imageResource = ImageFormatReader.readBC5(fm, width, height);
      }
      else if (imageFormat == 2) {
        imageResource = ImageFormatReader.readBGRA(fm, width, height);
      }
      else if (imageFormat == 3) {
        imageResource = ImageFormatReader.readBGRA(fm, width, height);
      }
      else if (imageFormat == 9) {
        imageResource = ImageFormatReader.readDXT1(fm, width, height);
      }
      else {
        ErrorLogger.log("[ZIP_PK_TEX_SIGSTRM12GIS]: Unknown Image Type: " + imageFormat);
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