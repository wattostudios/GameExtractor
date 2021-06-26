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
import org.watto.datatype.Archive;
import org.watto.datatype.ImageResource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.helper.ImageFormatReader;
import org.watto.ge.plugin.AllFilesPlugin;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.ge.plugin.archive.Plugin_SFS_AAMVHFSS;
import org.watto.ge.plugin.exporter.Exporter_ZLib;
import org.watto.io.FileManipulator;
import org.watto.io.buffer.ByteBuffer;
import org.watto.io.converter.ByteConverter;
import org.watto.io.converter.ShortConverter;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_SFS_AAMVHFSS_PNG_SPR extends ViewerPlugin {

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Viewer_SFS_AAMVHFSS_PNG_SPR() {
    super("SFS_AAMVHFSS_PNG_SPR", "SFS Compressed Image Format");
    setExtensions("png", "jp2");

    setGames("7 Wonders: The Treasures of Seven");
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
      if (plugin instanceof Plugin_SFS_AAMVHFSS) {
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
      if (fm.readString(4).equals(".SPR")) {
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

      // 4 - Header (.SPR)
      fm.skip(4);

      // 1 - Next Field Size (2=1 Byte, 3=2 Bytes)
      int nextSize = fm.readByte();

      // X - Image Width
      int imageWidth = -1;
      if (nextSize == 2) {
        imageWidth = ByteConverter.unsign(fm.readByte());
      }
      else if (nextSize == 3) {
        imageWidth = ShortConverter.unsign(fm.readShort());
      }
      else if (nextSize == 4) {
        imageWidth = fm.readInt();
      }
      FieldValidator.checkWidth(imageWidth);

      // 1 - Next Field Size (2=1 Byte, 3=2 Bytes)
      nextSize = fm.readByte();

      // X - Image Height
      int imageHeight = -1;
      if (nextSize == 2) {
        imageHeight = ByteConverter.unsign(fm.readByte());
      }
      else if (nextSize == 3) {
        imageHeight = ShortConverter.unsign(fm.readShort());
      }
      else if (nextSize == 4) {
        imageHeight = fm.readInt();
      }
      FieldValidator.checkHeight(imageHeight);

      // 1 - Unknown (2)
      fm.skip(1);

      // 1 - Compression Flag (1=Not Compressed, 2=ZLib)
      int compression = fm.readByte();

      // 1 - Unknown (10)
      fm.skip(1);

      // 1 - Next Field Size (2=1 Byte, 3=2 Bytes, 4=4 Bytes)
      nextSize = fm.readByte();

      // X - Compressed Image Length
      int imageLength = -1;
      if (nextSize == 2) {
        imageLength = ByteConverter.unsign(fm.readByte());
      }
      else if (nextSize == 3) {
        imageLength = ShortConverter.unsign(fm.readShort());
      }
      else if (nextSize == 4) {
        imageLength = fm.readInt();
      }
      FieldValidator.checkLength(imageLength, arcSize);

      ImageResource imageResource = null;
      if (compression == 1) {
        // Not Compressed
        imageResource = ImageFormatReader.readBGRA(fm, imageWidth, imageHeight);
      }
      else if (compression == 2) {
        // ZLib Compression

        int dataLength = imageWidth * imageHeight * 4;
        byte[] fileData = new byte[dataLength];
        int decompWritePos = 0;

        Exporter_ZLib exporter = Exporter_ZLib.getInstance();
        exporter.open(fm, imageLength, dataLength);

        for (int b = 0; b < dataLength; b++) {
          if (exporter.available()) { // make sure we read the next bit of data, if required
            fileData[decompWritePos++] = (byte) exporter.read();
          }
        }

        // open the decompressed file data for processing
        fm.close();
        fm = new FileManipulator(new ByteBuffer(fileData));

        imageResource = ImageFormatReader.readBGRA(fm, imageWidth, imageHeight);
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

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public void write(PreviewPanel preview, FileManipulator fm) {

  }

}