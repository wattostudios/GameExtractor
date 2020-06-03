
package org.watto.ge.plugin.archive;

import java.io.File;
import org.watto.task.TaskProgressManager;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.helper.ResourceSorter_OffsetName;
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
import org.watto.ge.plugin.exporter.Exporter_ZLib_CompressedSizeOnly;
import org.watto.io.FileManipulator;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_GTX_TEXT extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_GTX_TEXT() {

    super("GTX_TEXT", "GTX_TEXT");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Armobiles");
    setExtensions("gtx");
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

      // Header
      if (fm.readString(4).equals("TEXT")) {
        rating += 50;
      }

      fm.skip(8);

      long arcSize = fm.getLength();

      // Directory Offset
      if (FieldValidator.checkOffset(fm.readInt(), arcSize)) {
        rating += 5;
      }

      // Number Of Files
      if (FieldValidator.checkNumFiles(fm.readInt())) {
        rating += 5;
      }

      // Number Of Files
      if (FieldValidator.checkNumFiles(fm.readInt())) {
        rating += 5;
      }

      fm.skip(4);

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
   * Reads an [archive] File into the Resources
   **********************************************************************************************
   **/
  @Override
  public Resource[] read(File path) {
    try {

      // NOTE - Compressed files MUST know their DECOMPRESSED LENGTH
      //      - Uncompressed files MUST know their LENGTH

      addFileTypes();
      ExporterPlugin exporter = Exporter_ZLib_CompressedSizeOnly.getInstance();

      // RESETTING THE GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = (int) fm.getLength();

      // 4 - Header (TEXT)
      // 4 - Unknown (401)
      // 4 - Hash?
      // 4 - Files Directory Offset
      fm.skip(16);

      // 4 - Number Of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // 4 - Number Of Folders
      int numFolders = fm.readInt();
      FieldValidator.checkNumFiles(numFolders);

      // 4 - Folder Names Directory Length
      fm.skip(4);

      // 4 - Files Directory Offset
      long dirOffset = fm.readInt();
      FieldValidator.checkOffset(dirOffset, arcSize);

      // 4 - Folders Directory Offset
      int folderDirOffset = fm.readInt();
      FieldValidator.checkOffset(folderDirOffset, arcSize);

      // 4 - Filename Directory Offset
      int filenameDirOffset = fm.readInt();
      FieldValidator.checkOffset(filenameDirOffset, arcSize);

      Resource[] resources = new Resource[numFiles];

      TaskProgressManager.setMaximum(numFiles);

      fm.seek(folderDirOffset);

      // Loop through directory
      ResourceSorter_OffsetName[] folders = new ResourceSorter_OffsetName[numFolders];
      for (int i = 0; i < numFolders; i++) {
        // 4 - Unknown
        // 4 - Unknown
        // 4 - Hash?
        // 4 - Unknown
        fm.skip(16);

        // 4 - Filename Offset (relative to the start of the Folder Name Directory)
        int filenameOffset = fm.readInt() + filenameDirOffset;
        FieldValidator.checkOffset(filenameOffset, arcSize);

        // 4 - First File That Belongs To This Folder
        int firstFile = fm.readInt();

        long curPos = fm.getOffset();
        fm.seek(filenameOffset);

        // X - Filename
        String name = fm.readNullString();
        FieldValidator.checkFilename(name);

        fm.seek(curPos);

        folders[i] = new ResourceSorter_OffsetName(firstFile, name);
      }

      java.util.Arrays.sort(folders);

      fm.seek(dirOffset);

      // Loop through directory
      String dirName = folders[0].getName();
      int curDir = 0;
      int numLeft = numFiles;
      if (numFolders > 1) {
        numLeft = (int) folders[1].getOffset();
      }

      for (int i = 0; i < numFiles; i++) {
        // 4 - Compression Tag (0=Not Compressed, 1=Compressed)
        int comp = fm.readInt();

        // 4 - File Offset
        long offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - File Length
        long length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        String filename = dirName + "\\" + Resource.generateFilename(i);

        //path,id,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, filename, offset, length);

        if (comp == 1) {
          resources[i].setExporter(exporter);
        }

        TaskProgressManager.setValue(i);

        numLeft--;
        if (numLeft <= 0) {
          curDir++;
          if (curDir < numFolders) {
            dirName = folders[curDir].getName();
          }
          else {
            dirName = "";
          }

          if (curDir + 1 < numFolders) {
            numLeft = (int) folders[curDir + 1].getOffset();
          }
          else {
            numLeft = numFiles;
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
