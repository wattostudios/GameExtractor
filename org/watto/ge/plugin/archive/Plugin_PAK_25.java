
package org.watto.ge.plugin.archive;

import java.io.File;
import org.watto.Language;
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
import org.watto.io.FileManipulator;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_PAK_25 extends ArchivePlugin {

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Plugin_PAK_25() {

    super("PAK_25", "PAK_25");

    //         read write replace rename
    setProperties(true, true, true, true);

    setGames("Tycoon City: New York",
        "Hospital Tycoon");
    setExtensions("pak");
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

      fm.skip(4);

      // Number Of Files
      if (FieldValidator.checkNumFiles(fm.readInt())) {
        rating += 5;
      }

      long arcSize = fm.getLength();

      // Directory Offset
      if (FieldValidator.checkOffset(fm.readInt(), arcSize)) {
        rating += 5;
      }

      // Directory Length
      if (FieldValidator.checkLength(fm.readInt(), arcSize)) {
        rating += 5;
      }

      fm.skip(8);

      // Header Size
      if (fm.readInt() == 32) {
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

      // RESETTING THE GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = (int) fm.getLength();

      // 4 - Header
      fm.skip(4);

      // 4 - Number Of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // 4 - Files Directory Offset
      long dirOffset = fm.readInt();
      FieldValidator.checkOffset(dirOffset, arcSize);

      // 4 - Files Directory Length
      fm.skip(4);

      // 4 - Filename Directory Offset
      int nameDirOffset = fm.readInt();
      FieldValidator.checkOffset(nameDirOffset, arcSize);

      // 4 - Filename Directory Length
      // 4 - Archive Header Length (32)
      // 4 - File Data Length
      fm.seek(dirOffset);

      Resource[] resources = new Resource[numFiles];

      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      int[] nameOffsets = new int[numFiles];
      for (int i = 0; i < numFiles; i++) {
        // 4 - File Offset (relative to the start of the file data)
        long offset = fm.readInt() + 32;
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - Filename Offset (relative to the start of the filename directory)
        int filenameOffset = fm.readInt() + nameDirOffset;
        FieldValidator.checkOffset(filenameOffset, arcSize);
        nameOffsets[i] = filenameOffset;

        // 4 - File Length
        long length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        //path,id,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, "", offset, length);

        TaskProgressManager.setValue(i);
      }

      for (int i = 0; i < numFiles; i++) {
        fm.seek(nameOffsets[i]);
        // X - Filename (null)
        String filename = fm.readNullString();
        FieldValidator.checkFilename(filename);
        resources[i].setName(filename);
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
   * Writes an [archive] File with the contents of the Resources
   **********************************************************************************************
   **/
  @Override
  public void write(Resource[] resources, File path) {
    try {

      FileManipulator fm = new FileManipulator(path, true);
      int numFiles = resources.length;
      TaskProgressManager.setMaximum(numFiles);

      // Calculations
      TaskProgressManager.setMessage(Language.get("Progress_PerformingCalculations"));

      long filenameDirectoryLength = 0;
      long fileDataLength = 0;
      for (int i = 0; i < numFiles; i++) {
        fileDataLength += resources[i].getDecompressedLength();
        filenameDirectoryLength += resources[i].getNameLength() + 1;
      }
      long filesDirectoryOffset = fileDataLength + 32;
      long filesDirectoryLength = numFiles * 12;
      long filenameDirectoryOffset = filesDirectoryOffset + filesDirectoryLength;

      // Write Header Data

      // 4 - Header
      fm.writeBytes(new byte[] { 0, 0, 0, 0 });

      // 4 - Number Of Files
      fm.writeInt(numFiles);

      // 4 - Files Directory Offset
      fm.writeInt((int) filesDirectoryOffset);

      // 4 - Files Directory Length
      fm.writeInt((int) filesDirectoryLength);

      // 4 - Filename Directory Offset
      fm.writeInt((int) filenameDirectoryOffset);

      // 4 - Filename Directory Length
      fm.writeInt((int) filenameDirectoryLength);

      // 4 - Archive Header Length (32)
      fm.writeInt(32);

      // 4 - File Data Length
      fm.writeInt((int) fileDataLength);

      // Write Files
      TaskProgressManager.setMessage(Language.get("Progress_WritingFiles"));
      write(resources, fm);

      TaskProgressManager.setMessage(Language.get("Progress_WritingDirectory"));

      // Write Directory
      long offset = 0;
      long filenameOffset = 0;
      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];
        long decompLength = resource.getDecompressedLength();

        // 4 - File Offset (relative to the start of the file data)
        fm.writeInt((int) offset);

        // 4 - Filename Offset (relative to the start of the filename directory)
        fm.writeInt((int) filenameOffset);

        // 4 - File Length
        fm.writeInt((int) decompLength);

        offset += decompLength;
        filenameOffset += resource.getNameLength() + 1;
      }

      // Write Filename Directory
      for (int i = 0; i < numFiles; i++) {
        // X - Filename
        // 1 - null Filename Terminator
        fm.writeNullString(resources[i].getName());
      }

      fm.close();

    }
    catch (Throwable t) {
      logError(t);
    }
  }

}
