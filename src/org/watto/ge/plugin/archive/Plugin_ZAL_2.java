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

package org.watto.ge.plugin.archive;

import java.io.File;

import org.watto.ErrorLogger;
import org.watto.Language;
import org.watto.Settings;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.exporter.Exporter_LZO_SingleBlock;
import org.watto.io.FileManipulator;
import org.watto.io.converter.ByteConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_ZAL_2 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_ZAL_2() {

    super("ZAL_2", "ZAL_2");

    //         read write replace rename
    setProperties(true, false, true, false);

    setGames("Dave Mirra Freestyle BMX");
    setExtensions("zal"); // MUST BE LOWER CASE
    setPlatforms("PC");

    //setFileTypes(new FileType("txt", "Text Document", FileType.TYPE_DOCUMENT),
    //             new FileType("bmp", "Bitmap Image", FileType.TYPE_IMAGE)
    //             );

    setCanScanForFileTypes(true);

  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public int getMatchRating(FileManipulator fm) {
    try {

      int rating = 0;

      if (FieldValidator.checkExtension(fm, extensions)) {
        rating += 25;
      }

      // Header
      if (fm.readInt() == 713117980) {
        rating += 50;
      }

      if (fm.readInt() == 0) {
        rating += 2;
      }
      if (fm.readLong() == 0) {
        rating += 3;
      }
      if (fm.readByte() == 1) {
        rating += 5;
      }
      fm.skip(3);
      if (fm.readInt() == 0) {
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
   * Reads an [archive] File into the Resources
   **********************************************************************************************
   **/
  @Override
  public Resource[] read(File path) {
    try {

      // NOTE - Compressed files MUST know their DECOMPRESSED LENGTH
      //      - Uncompressed files MUST know their LENGTH

      addFileTypes();

      ExporterPlugin exporter = Exporter_LZO_SingleBlock.getInstance();

      // RESETTING GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 4 - Header? (28,81,129,42)
      // 4 - null
      // 4 - null
      // 4 - null
      // 1 - Unknown (1)
      fm.skip(17);

      // 1 - Compression Flag (128 = Compressed File Data | 0 = Uncompressed)
      int compression = ByteConverter.unsign(fm.readByte());

      // 2 - Number of Files (Compressed --> upper 4 bits == 4 | Uncompressed --> upper 4 bits == 0)
      short numFiles = (short) (fm.readShort() & 4095);
      FieldValidator.checkNumFiles(numFiles);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      if (compression == 0) {
        // Uncompressed
        for (int i = 0; i < numFiles; i++) {

          // 4 - File ID (incremental from 0)
          fm.skip(4);

          // 4 - File Offset [+16]
          long offset = fm.readInt() + 16;
          FieldValidator.checkOffset(offset, arcSize);

          // 4 - File Length
          long length = fm.readInt();
          FieldValidator.checkLength(length, arcSize);

          String filename = Resource.generateFilename(i);

          //path,name,offset,length,decompLength,exporter
          resources[i] = new Resource(path, filename, offset, length);

          TaskProgressManager.setValue(i);
        }
      }
      else if (compression == 128) {
        for (int i = 0; i < numFiles; i++) {
          // 4 - File ID (incremental from 0)
          fm.skip(4);

          // 4 - File Offset [+16]
          long offset = fm.readInt() + 16;
          FieldValidator.checkOffset(offset, arcSize);

          // 4 - Decompressed File Length
          long decompLength = fm.readInt();
          FieldValidator.checkLength(decompLength);

          // 4 - Compressed File Length
          long length = fm.readInt();
          FieldValidator.checkLength(length, arcSize);

          String filename = Resource.generateFilename(i);

          //path,name,offset,length,decompLength,exporter
          resources[i] = new Resource(path, filename, offset, length, decompLength, exporter);

          TaskProgressManager.setValue(i);
        }
      }
      else {
        ErrorLogger.log("[ZAL_2] Unknown compression flag: " + compression);
      }

      fm.close();

      return resources;

    }
    catch (Throwable t) {
      logError(t);
      return null;
    }
  }

  /**
   **********************************************************************************************
   * Writes an [archive] File with the contents of the Resources. The archive is written using
   * data from the initial archive - it isn't written from scratch.
   **********************************************************************************************
   **/
  @Override
  public void replace(Resource[] resources, File path) {
    try {

      // EVEN IF THIS IS A COMPRESSED ARCHIVE, THIS WILL ALWAYS WRITE OUT AS AN UNCOMPRESSED ARCHIVE (as we can't do LZO1X compression)

      FileManipulator fm = new FileManipulator(path, true);
      FileManipulator src = new FileManipulator(new File(Settings.getString("CurrentArchive")), false);

      int numFiles = resources.length;
      TaskProgressManager.setMaximum(numFiles);

      // Calculations
      TaskProgressManager.setMessage(Language.get("Progress_PerformingCalculations"));

      // Write Header Data

      // 4 - Header? (28,81,129,42)
      // 4 - null
      // 4 - null
      // 4 - null
      fm.writeBytes(src.readBytes(16));

      // 1 - Unknown (1)
      fm.writeByte(1);
      // 1 - Compression Flag (0 = Uncompressed File Data | 128 = Compressed File Data)
      fm.writeByte(0);

      // 2 - Number of Files
      fm.writeShort(numFiles);

      src.skip(4);

      // Write Directory
      TaskProgressManager.setMessage(Language.get("Progress_WritingDirectory"));
      long offset = 4 + (12 * numFiles);
      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];
        long length = resource.getDecompressedLength();

        // 4 - File ID
        fm.writeBytes(src.readBytes(4));

        // 4 - File Offset
        fm.writeInt(offset);

        // 4 - File Length
        fm.writeInt(length);

        src.skip(8);

        int paddingSize = calculatePadding(length, 4);

        offset += length + paddingSize;
      }

      // Write Files
      TaskProgressManager.setMessage(Language.get("Progress_WritingFiles"));
      for (int i = 0; i < resources.length; i++) {
        Resource resource = resources[i];
        write(resource, fm);
        TaskProgressManager.setValue(i);

        long length = resource.getDecompressedLength();
        int paddingSize = calculatePadding(length, 4);
        for (int p = 0; p < paddingSize; p++) {
          fm.writeByte(0);
        }
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
  If an archive doesn't have filenames stored in it, the scanner can come here to try to work out
  what kind of file a Resource is. This method allows the plugin to provide additional plugin-specific
  extensions, which will be tried before any standard extensions.
  @return null if no extension can be determined, or the extension if one can be found
  **********************************************************************************************
  **/
  @Override
  public String guessFileExtension(Resource resource, byte[] headerBytes, int headerInt1, int headerInt2, int headerInt3, short headerShort1, short headerShort2, short headerShort3, short headerShort4, short headerShort5, short headerShort6) {

    if (headerInt1 == 1397904467) {
      return "strs";
    }
    else if (headerInt1 == 1297239878) {
      return "form";
    }
    else if (headerBytes[1] == 66 && headerBytes[2] == 77) {
      return "bm";
    }
    else if (headerInt1 == 713117980) {
      return "zal";
    }
    else if (headerInt3 == ((headerInt1 * 8) + 4)) {
      return "scrn";
    }
    else if (headerInt1 == 16) {
      return "tim";
    }

    return null;
  }

}
