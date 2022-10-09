/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2022 wattostudios
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
import org.watto.datatype.Archive;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.exporter.Exporter_LZSS;
import org.watto.io.FileManipulator;
import org.watto.io.converter.ByteConverter;
import org.watto.io.converter.IntConverter;
import org.watto.io.converter.ShortConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_001_2 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_001_2() {

    super("001_2", "001_2");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Star Trek: 25th Anniversary",
        "Star Trek: Judgement Rights");
    setExtensions("001");
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

      getDirectoryFile(fm.getFile(), "dir");
      rating += 25;

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

      // NOTE - Compressed file MUST know their DECOMPRESSED LENGTH
      //      - Uncompressed files MUST know their LENGTH

      addFileTypes();
      ExporterPlugin exporter = Exporter_LZSS.getInstance();

      // RESETTING THE GLOBAL VARIABLES

      long arcSize = (int) path.length();

      File sourcePath = getDirectoryFile(path, "dir");

      FileManipulator fm = new FileManipulator(sourcePath, false);

      //int numFiles = (int) (fm.getLength() / 14);
      //FieldValidator.checkNumFiles(numFiles);

      int numFiles = Archive.getMaxFiles();

      int numEntries = (int) (fm.getLength() / 14);
      FieldValidator.checkNumFiles(numEntries);

      Resource[] resources = new Resource[numFiles];

      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      int realNumFiles = 0;
      String[] names = new String[numFiles];
      int[] offsets = new int[numFiles];
      for (int i = 0; i < numEntries; i++) {
        // 8 - Filename (null)
        String filename = fm.readNullString(8);
        FieldValidator.checkFilename(filename);

        // 3 - Filename Extension (null)
        String ext = fm.readNullString(3);
        //FieldValidator.checkFilename(ext);

        // 3 - File Offset
        byte byte1 = fm.readByte();
        byte byte2 = fm.readByte();
        byte byte3 = fm.readByte();

        int numFilesForEntry = 1;
        if ((ByteConverter.unsign(byte3) & 128) == 128) {
          // multiple files at this offset
          numFilesForEntry = ByteConverter.unsign(byte3) & 127;
          byte3 = 0;

          for (int j = 0; j < numFilesForEntry; j++) {
            offsets[realNumFiles] = -1; // will calculate this later on

            if (ext.length() == 0) {
              names[realNumFiles] = filename + j;
            }
            else {
              names[realNumFiles] = filename + j + "." + ext;
            }
            //System.out.println(names[i]);

            realNumFiles++;
          }
        }
        else {
          byte[] offsetBytes = new byte[] { byte1, byte2, byte3, 0 };

          offsets[realNumFiles] = IntConverter.convertLittle(offsetBytes);
          //offsets[i] = ByteConverter.unsign(fm.readByte())<<24 + ByteConverter.unsign(fm.readByte())<<16 + ByteConverter.unsign(fm.readByte())<<8;
          //System.out.println(offsets[i]);
          //FieldValidator.checkOffset(offsets[i],arcSize);

          if (ext.length() == 0) {
            names[realNumFiles] = filename;
          }
          else {
            names[realNumFiles] = filename + "." + ext;
          }
          //System.out.println(names[i]);

          realNumFiles++;
        }
      }

      fm.close();

      resources = resizeResources(resources, realNumFiles);
      numFiles = realNumFiles;

      fm = new FileManipulator(path, false);

      // Loop through directory
      long previousOffset = 0;
      int previousLength = 0;
      for (int i = 0; i < numFiles; i++) {
        long offset = offsets[i];

        if (offset == -1) {
          offset = previousOffset + previousLength;
          offset += calculatePadding(offset, 2);
        }

        fm.seek(offset);

        // 2 - Decompressed File Size
        int decompLength = ShortConverter.unsign(fm.readShort());

        // 2 - File Size
        int length = ShortConverter.unsign(fm.readShort());
        FieldValidator.checkLength(length, arcSize);

        // X - File Data
        offset = fm.getOffset();

        previousOffset = offset;
        previousLength = length;

        String filename = names[i];

        if (decompLength == length) {
          //path,id,name,offset,length,decompLength,exporter
          resources[i] = new Resource(path, filename, offset, length);
        }
        else {
          //path,id,name,offset,length,decompLength,exporter
          resources[i] = new Resource(path, filename, offset, length, decompLength, exporter);
        }

        TaskProgressManager.setValue(i);
      }

      fm.close();

      return resources;

    }
    catch (Throwable t) {
      logError(t);
      return null;
    }
  }

}
