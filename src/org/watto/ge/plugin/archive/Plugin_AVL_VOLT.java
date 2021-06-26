
package org.watto.ge.plugin.archive;

import java.io.File;
import org.watto.Language;
import org.watto.Settings;
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
public class Plugin_AVL_VOLT extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_AVL_VOLT() {

    super("AVL_VOLT", "AVL_VOLT");

    //         read write replace rename
    setProperties(true, false, true, true);

    setGames("Catwoman",
        "Bionicle");
    setExtensions("avl", "vol");
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
      if (fm.readString(4).equals("VOLT")) {
        rating += 50;
      }

      // Version (2)
      if (fm.readInt() == 2) {
        rating += 5;
      }

      // Number Of Files
      if (FieldValidator.checkNumFiles(fm.readInt())) {
        rating += 5;
      }

      // Directory Length
      if (FieldValidator.checkLength(fm.readInt(), (int) fm.getLength())) {
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

      // 4 - Header (VOLT)
      // 4 - Version? (2)
      fm.skip(8);

      // 4 - numFiles
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // 4 - Directory Length
      fm.skip(4);

      // for each file
      // 4 - Unknown
      // 4 - File Type ID? (1)
      // 4 - File ID Number? (goes up by 25 for each file)
      fm.skip(numFiles * 12);

      long arcSize = fm.getLength();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {
        // 4 - Data Offset
        long offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - null
        fm.skip(4);

        // 4 - File Length
        long length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - null
        fm.skip(4);

        // X - Filename (null)
        String filename = fm.readNullString();
        FieldValidator.checkFilename(filename);

        //path,id,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, filename, offset, length);

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

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public void write(Resource[] resources, File path) {
    try {

      FileManipulator src = new FileManipulator(new File(Settings.getString("CurrentArchive")), false);
      FileManipulator fm = new FileManipulator(path, true);

      TaskProgressManager.setMessage(Language.get("Progress_WritingDirectory"));

      int numFiles = resources.length;
      TaskProgressManager.setMaximum(numFiles);

      // Write Header Data

      // 4 - Header (VOLT)
      // 4 - Version? (2)
      // 4 - numFiles
      // 4 - Directory Length
      byte[] bytes = src.readBytes(16);
      fm.writeBytes(bytes);

      // for each file
      // 4 - Unknown
      // 4 - File Type ID? (1)
      // 4 - File ID Number? (goes up by 25 for each file)
      fm.writeBytes(src.readBytes(12 * numFiles));

      // Write Directory
      int currentPos = (int) src.getOffset();
      long offset = src.readInt();
      src.seek(currentPos);

      for (int i = 0; i < numFiles; i++) {
        Resource fd = resources[i];
        long length = fd.getDecompressedLength();

        // 4 - Data Offset
        src.skip(4);
        fm.writeInt((int) offset);

        // 4 - null
        fm.writeBytes(src.readBytes(4));

        // 4 - File Length
        src.skip(4);
        fm.writeInt((int) length);

        // 4 - null
        fm.writeBytes(src.readBytes(4));

        // X - Filename (null)
        fm.writeBytes(src.readBytes(fd.getName().length() + 1));

        offset += length;
      }

      // Padding to 2048 with (byte)105
      int numPadding = 2048 - (int) fm.getOffset();
      for (int i = 0; i < numPadding; i++) {
        fm.writeByte(105);
      }

      // Write Files
      TaskProgressManager.setMessage(Language.get("Progress_WritingFiles"));
      write(resources, fm);

      src.close();
      fm.close();

    }
    catch (Throwable t) {
      logError(t);
    }
  }

}
