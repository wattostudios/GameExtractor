
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
public class Plugin_HD extends ArchivePlugin {

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Plugin_HD() {

    super("HD", "HD");

    //         read write replace rename
    setProperties(true, true, true, true);

    setExtensions("hd", "cd");
    setGames("Cyclones");
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

      // Number Of Files
      if (FieldValidator.checkNumFiles(fm.readShort())) {
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

      FileManipulator fm = new FileManipulator(path, false);

      // 2 - numFiles
      int numFiles = fm.readShort();
      FieldValidator.checkNumFiles(numFiles);

      long arcSize = fm.getLength();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // 4 - dirOffset
      long dirOffset = fm.readInt();
      FieldValidator.checkOffset(dirOffset, arcSize);

      // 4 - dirLength (includes all directory entries and filenames)
      // X - fileData

      fm.seek(dirOffset);
      int realNumFiles = 0;
      for (int i = 0; i < numFiles; i++) {
        // 4 - Data Offset (relative to the first file offset. ie 10)
        long offset = fm.readInt() + 10;
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - File Length
        long length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - Filename Offset (relative to dirOffset)
        long filenameOffset = fm.readInt() + dirOffset;
        FieldValidator.checkOffset(filenameOffset, arcSize);

        if (offset != 0 && length != 0) {
          //path,id,name,offset,length,decompLength,exporter
          resources[realNumFiles] = new Resource(path, "", offset, length);

          TaskProgressManager.setValue(i);
          realNumFiles++;
        }
      }

      for (int i = 0; i < realNumFiles; i++) {
        // X - Filename (null)
        String filename = fm.readNullString();
        FieldValidator.checkFilename(filename);
        resources[i].setName(filename);
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

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  @Override
  public void write(Resource[] resources, File path) {
    try {

      FileManipulator fm = new FileManipulator(path, true);

      int numFiles = resources.length;
      TaskProgressManager.setMaximum(numFiles);

      TaskProgressManager.setMessage(Language.get("Progress_PerformingCalculations"));
      int dirLength = 0;
      long dirOffset = 10;
      for (int i = 0; i < numFiles; i++) {
        Resource fd = resources[i];
        dirLength += 12 + fd.getName().length();
        dirOffset += fd.getDecompressedLength();
      }

      // 2 - numFiles
      fm.writeShort((short) numFiles);

      // 4 - dirOffset
      fm.writeInt((int) dirOffset);

      // 4 - dirLength (includes all directory entries and filenames)
      fm.writeInt(dirLength);

      TaskProgressManager.setMessage(Language.get("Progress_WritingFiles"));
      write(resources, fm);

      TaskProgressManager.setMessage(Language.get("Progress_WritingDirectory"));
      long offset = 0;
      int filenameOffset = 0;
      for (int i = 0; i < numFiles; i++) {
        Resource fd = resources[i];
        long length = fd.getDecompressedLength();
        int filenameLength = fd.getName().length() + 1;

        // 4 - Data Offset (relative to the first file offset. ie 10)
        fm.writeInt((int) offset);

        // 4 - File Length
        fm.writeInt((int) length);

        // 4 - Filename Offset (relative to dirOffset)
        fm.writeInt(filenameOffset);

        offset += length;
        filenameOffset += filenameLength;
      }

      for (int i = 0; i < numFiles; i++) {
        // X - Filename (null)
        fm.writeNullString(resources[i].getName());
      }

      fm.close();

    }
    catch (Throwable t) {
      logError(t);
    }
  }

}