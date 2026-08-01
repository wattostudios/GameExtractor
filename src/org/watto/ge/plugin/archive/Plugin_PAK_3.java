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
package org.watto.ge.plugin.archive;

import java.io.File;

import org.watto.ErrorLogger;
import org.watto.datatype.Archive;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.exporter.Exporter_Explode;
import org.watto.io.FileManipulator;
import org.watto.io.buffer.ByteBuffer;
import org.watto.io.buffer.XORRepeatingKeyBufferWrapper;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_PAK_3 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_PAK_3() {

    super("PAK_3", "PAK_3");

    //         read write replace rename
    setProperties(true, false, false, false);

    setExtensions("pak");
    setGames("Arx Fatalis");
    setPlatforms("PC");

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

      long arcSize = fm.getLength();

      // Directory Offset
      if (FieldValidator.checkOffset(fm.readInt(), arcSize)) {
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
  
  **********************************************************************************************
  **/
  @Override
  public Resource[] read(File path) {
    try {

      addFileTypes();

      ExporterPlugin exporter = Exporter_Explode.getInstance();

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 4 - Directory Offset
      int dirOffset = fm.readInt();
      FieldValidator.checkOffset(dirOffset, arcSize);

      fm.seek(dirOffset);

      // 4 - Directory Length (not including this field)
      int dirLength = fm.readInt();
      FieldValidator.checkLength(dirLength, arcSize);

      int[] keyDemo = new int[] { 78, 83, 73, 65, 82, 75, 80, 82, 81, 80, 72, 66, 84, 69, 53, 48, 71, 82, 73, 72, 51, 65, 89, 88, 74, 80, 50, 65, 77, 70, 51, 70, 67, 69, 89, 65, 86, 81, 79, 53, 81, 71, 65, 48, 74, 71, 73, 73, 72, 50, 65, 89, 88, 75, 86, 79, 65, 49, 86, 79, 71, 71, 85, 53, 71, 83, 81, 75, 75, 89, 69, 79, 73, 65, 81, 71, 49, 88, 82, 88, 48, 74, 52, 70, 53, 79, 69, 65, 69, 70, 73, 52, 68, 68, 51, 76, 76, 52, 53, 86, 74, 84, 86, 79, 65, 49, 86, 79, 71, 71, 85, 75, 69, 53, 48, 71, 82, 73 };
      int[] keyFull = new int[] { 65, 86, 81, 70, 51, 70, 67, 75, 69, 53, 48, 71, 82, 73, 65, 89, 88, 74, 80, 50, 65, 77, 69, 89, 79, 53, 81, 71, 65, 48, 74, 71, 73, 73, 72, 50, 78, 72, 66, 84, 86, 79, 65, 49, 86, 79, 71, 71, 85, 53, 72, 51, 71, 83, 83, 73, 65, 82, 75, 80, 82, 81, 80, 81, 75, 75, 89, 69, 79, 73, 65, 81, 71, 49, 88, 82, 88, 48, 74, 52, 70, 53, 79, 69, 65, 69, 70, 73, 52, 68, 68, 51, 76, 76, 52, 53, 86, 74, 84, 86, 79, 65, 49, 86, 79, 71, 71, 85, 75, 69, 53, 48, 71, 82, 73, 65, 89, 88 };

      int[] key = keyFull;

      if (fm.readByte() == 78 && fm.readByte() == 83 && fm.readByte() == 73 && fm.readByte() == 65) {
        key = keyDemo;
      }
      fm.relativeSeek(dirOffset + 4);

      byte[] dirBytes = fm.readBytes(dirLength);

      fm.close();
      fm = new FileManipulator(new XORRepeatingKeyBufferWrapper(new ByteBuffer(dirBytes), key));

      int numFiles = Archive.getMaxFiles();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(arcSize);

      int realNumFiles = 0;
      while (fm.getOffset() < dirLength) {
        // X - Folder Name (can be null)
        // 1 - null Folder Name Terminator
        String dirName = fm.readNullString();

        // 4 - Number of Files in this Folder (can be null)
        int numFilesInFolder = fm.readInt();
        FieldValidator.checkNumFiles(numFilesInFolder + 1); // +1 to allow nulls

        for (int i = 0; i < numFilesInFolder; i++) {
          // X - Filename
          // 1 - null Filename Terminator
          String filename = fm.readNullString();
          FieldValidator.checkFilename(filename);
          filename = dirName + filename;

          // 4 - File Offset
          int offset = fm.readInt();
          FieldValidator.checkOffset(offset, arcSize);

          // 4 - Compression Flags (0=uncompressed, 1=explode)
          int compressionFlag = fm.readInt();

          // 4 - Decompressed Length
          int decompLength = fm.readInt();

          // 4 - File Length
          int length = fm.readInt();

          if (compressionFlag == 0) {
            // not compressed
            //path,id,name,offset,length,decompLength,exporter
            resources[realNumFiles] = new Resource(path, filename, offset, length);
          }
          else if (compressionFlag == 1) {
            // compressed (explode)
            //path,id,name,offset,length,decompLength,exporter
            resources[realNumFiles] = new Resource(path, filename, offset, length, decompLength, exporter);
          }
          else {
            ErrorLogger.log("[PAK_3] Unknown compression type: " + compressionFlag);
            //path,id,name,offset,length,decompLength,exporter
            resources[realNumFiles] = new Resource(path, filename, offset, length, decompLength);

          }
          realNumFiles++;

          TaskProgressManager.setValue(offset);
        }
      }

      resources = resizeResources(resources, realNumFiles);

      fm.close();

      return resources;

    }
    catch (Throwable t) {
      logError(t);
      return null;
    }
  }

}