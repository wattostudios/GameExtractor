
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
public class Plugin_AGG_2 extends ArchivePlugin {

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Plugin_AGG_2() {

    super("AGG_2", "AGG_2");

    //         read write replace rename
    setProperties(true, false, true, true);

    setGames("Heroes of Might and Magic");
    setExtensions("agg");
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
      int numFiles = fm.readShort();
      if (FieldValidator.checkNumFiles(numFiles)) {
        rating += 5;
      }

      long arcSize = fm.getLength();

      fm.skip(2);

      // First File Offset
      long offset = fm.readInt();
      if (FieldValidator.checkOffset(offset, arcSize)) {
        rating += 5;
      }

      // First File Size
      long length = fm.readInt();
      if (FieldValidator.checkLength(length, arcSize)) {
        rating += 5;
      }

      // First File Size (again)
      long length2 = fm.readInt();
      if (FieldValidator.checkLength(length2, arcSize)) {
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

      // 2 - Number Of Files
      int numFiles = fm.readShort();
      FieldValidator.checkNumFiles(numFiles);

      long arcSize = fm.getLength();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      fm.seek(arcSize - numFiles * 15);

      String[] names = new String[numFiles];
      for (int i = 0; i < numFiles; i++) {
        // 15 - Filename(null)
        String filename = fm.readNullString(15);
        FieldValidator.checkFilename(filename);
        names[i] = filename;
      }

      fm.seek(2);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {
        // 2 - Unknown
        fm.skip(2);

        // 4 - Data Offset
        long offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - File Length
        long length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - File Length
        long length2 = fm.readInt();
        FieldValidator.checkLength(length2, arcSize);

        if (length2 != length) {
          //System.out.println(length + " - " + length2);
        }

        //path,id,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, names[i], offset, length);

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
  @SuppressWarnings("unused")
  @Override
  public void write(Resource[] resources, File path) {
    try {

      FileManipulator fm = new FileManipulator(path, true);
      FileManipulator src = new FileManipulator(new File(Settings.getString("CurrentArchive")), false);

      int numFiles = resources.length;
      TaskProgressManager.setMaximum(numFiles);

      // Write Header Data

      // 2 - Number Of Files
      fm.writeShort((short) numFiles);

      // Write Directory
      TaskProgressManager.setMessage(Language.get("Progress_WritingDirectory"));
      long offset = 2 + (numFiles * 12);
      for (int i = 0; i < numFiles; i++) {
        long length = resources[i].getDecompressedLength();
        // 2 - Unknown
        fm.writeBytes(src.readBytes(2));

        // 4 - Data Offset
        // 4 - File Length
        // 4 - File Length
        src.skip(12);
        fm.writeInt((int) resources[i].getOffset());
        fm.writeInt((int) length);
        fm.writeInt((int) length);

        offset += length;
      }

      // Write Files
      TaskProgressManager.setMessage(Language.get("Progress_WritingFiles"));
      write(resources, fm);

      // Write Filenames
      TaskProgressManager.setMessage(Language.get("Progress_WritingDirectory"));
      for (int i = 0; i < numFiles; i++) {
        // 15 - Filename
        fm.writeNullString(resources[i].getName(), 15);
      }

      src.close();
      fm.close();

    }
    catch (Throwable t) {
      logError(t);
    }
  }

}
