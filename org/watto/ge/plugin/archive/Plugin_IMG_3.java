
package org.watto.ge.plugin.archive;

import java.io.File;
import org.watto.task.TaskProgressManager;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
////////////////////////////////////////////////////////////////////////////////////////////////
//                                                                                            //
//                                       GAME EXTRACTOR                                       //
//                               Extensible Game Archive Editor                               //
//                                http://www.watto.org/extract                                //
//                                                                                            //
//                           Copyright (C) 2002-2009  WATTO Studios                           //
//                                                                                            //
// This program is free software; you can redistribute it and/or modify it under the terms of //
// the GNU General Public License published by the Free Software Foundation; either version 2 //
// of the License, or (at your option) any later versions. This program is distributed in the //
// hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranties //
// of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License //
// at http://www.gnu.org for more details. For updates and information about this program, go //
// to the WATTO Studios website at http://www.watto.org or email watto@watto.org . Thanks! :) //
//                                                                                            //
////////////////////////////////////////////////////////////////////////////////////////////////
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.exporter.Exporter_ZLib;
import org.watto.io.FileManipulator;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_IMG_3 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_IMG_3() {

    super("IMG_3", "IMG_3");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Splinter Cell: Pandora Tomorrow");
    setExtensions("img");
    setPlatforms("PS2");

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

      // Archive Size
      if (FieldValidator.checkEquals(fm.readInt(), arcSize)) {
        rating += 5;
      }

      // Directory Length
      if (FieldValidator.checkLength(fm.readInt(), arcSize)) {
        rating += 5;
      }

      // Padding Multiple
      if (fm.readInt() == 2048) {
        rating += 5;
      }

      // Directory Offset
      if (FieldValidator.checkOffset(fm.readInt(), arcSize)) {
        rating += 5;
      }

      // Directory Offset
      if (FieldValidator.checkOffset(fm.readInt(), arcSize)) {
        rating += 4;
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
      ExporterPlugin exporter = Exporter_ZLib.getInstance();

      // RESETTING THE GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = (int) fm.getLength();

      // 4 - Archive Size
      // 4 - Total Directory Length (not including padding at the end of the filename directory)
      // 4 - Padding Multiple (2048)
      fm.skip(12);

      // 4 - Empty Directory Offset
      int numFiles = ((fm.readInt() - 2048) / 48) - 1;
      FieldValidator.checkNumFiles(numFiles);

      // 4 - Filename Directory Offset
      int filenameDirOffset = fm.readInt();
      FieldValidator.checkOffset(filenameDirOffset, arcSize);

      // 4 - First File Offset
      // 8 - null
      // 4 - Unknown Directory Offset
      // 2012 - null Padding to offset 2048

      Resource[] resources = new Resource[numFiles];

      TaskProgressManager.setMaximum(numFiles);

      fm.seek(2048);

      // Loop through directory
      int[] nameOffsets = new int[numFiles];
      int[] parentIDs = new int[numFiles];
      boolean[] isFolder = new boolean[numFiles];

      int realNumFiles = 0;
      for (int i = 0; i < numFiles; i++) {
        // 4 - Filename Offset (relative to the start of the filename directory)
        int filenameOffset = fm.readInt() + filenameDirOffset;
        FieldValidator.checkOffset(filenameOffset, arcSize);
        nameOffsets[i] = filenameOffset;

        // 4 - File ID?
        fm.skip(4);

        // 4 - Parent Folder ID (folder that this file/folder belongs to) [+1]
        parentIDs[i] = fm.readInt();

        // 4 - Folder ID (0=file, #=folder)
        isFolder[i] = (fm.readInt() != 0);

        // 4 - Compression (0=uncompressed, 1=compressed)
        int compression = fm.readInt();

        // 4 - Entry Type (0=folder, 1=file)
        fm.skip(4);

        // 4 - Compressed File Length
        long length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - Decompressed File Length
        int decompLength = fm.readInt();
        FieldValidator.checkLength(decompLength);

        // 4 - File Offset
        long offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - Unknown
        // 2 - Unknown
        // 2 - Unknown
        // 4 - Unknown (often null)
        fm.skip(12);

        if (compression == 1) {
          offset += 8;
          length -= 8;
        }

        //path,id,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, "", offset, length, decompLength);
        if (compression == 1) {
          resources[i].setExporter(exporter);
        }

        TaskProgressManager.setValue(i);

        if (offset != 0) {
          realNumFiles++;
        }
      }

      // Loop through directory
      Resource[] temp = resources;
      resources = new Resource[realNumFiles];
      realNumFiles = 0;

      String[] names = new String[numFiles];
      for (int i = 0; i < numFiles; i++) {
        fm.seek(nameOffsets[i]);

        // X - Filename (null)
        String filename = fm.readNullString();
        FieldValidator.checkFilename(filename);

        names[i] = filename;

        String parentName = "";
        int parentID = parentIDs[i];
        if (parentID == 0) { // root directory
        }
        else {
          //parentID--;
          parentName = names[parentID];
        }

        if (isFolder[i]) {
          names[i] = parentName + filename + "\\";
        }
        else {
          temp[i].setName(parentName + filename);

          if (temp[i].getOffset() != 0) {
            resources[realNumFiles] = temp[i];
            realNumFiles++;
          }
        }

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
