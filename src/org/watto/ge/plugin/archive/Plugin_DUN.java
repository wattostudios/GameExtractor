
package org.watto.ge.plugin.archive;

import java.io.File;
import org.watto.Language;
import org.watto.component.WSPluginException;
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
import org.watto.io.converter.ByteConverter;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_DUN extends ArchivePlugin {

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Plugin_DUN() {

    super("DUN", "DUN");

    //         read write replace rename
    setProperties(true, true, true, true);

    setExtensions("dun");
    setGames("Frank Herbert's Dune");
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
      if (ByteConverter.unsign(fm.readByte()) == 114 && ByteConverter.unsign(fm.readByte()) == 16 && ByteConverter.unsign(fm.readByte()) == 234 && ByteConverter.unsign(fm.readByte()) == 244) {
        rating += 50;
      }

      // null
      if (fm.readInt() == 0) {
        rating += 5;
      }

      long arcSize = fm.getLength();

      // Directory Offset
      if (FieldValidator.checkOffset(fm.readInt(), arcSize)) {
        rating += 5;
      }

      // Number Of Files
      int numFiles = fm.readInt();
      if (FieldValidator.checkNumFiles(numFiles)) {
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
      long arcSize = fm.getLength();

      // 4 - Header? ((bytes)114 16 234 244)
      // 4 - null
      fm.seek(8);

      // 4 - Directory Offset
      long dirOffset = fm.readInt();
      FieldValidator.checkOffset(dirOffset, arcSize);

      // 4 - Number Of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      fm.seek(dirOffset);

      for (int i = 0; i < numFiles; i++) {
        // X - Filename (terminated by (byte)10) (starting with "..\" or "R:\")
        String filename = "";
        int filenameByte = fm.readByte();
        while (filenameByte != 10) {
          filename += (char) filenameByte;
          filenameByte = fm.readByte();
        }

        FieldValidator.checkFilename(filename);

        if (filename.length() > 2 && filename.charAt(2) == '\\') {
          filename = filename.substring(3);
        }
        else if (filename.charAt(3) == (char) 0) {
          throw new WSPluginException("Wrong Version");
        }

        // 4 - File Offset
        long offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - File Length
        long length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

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

      FileManipulator fm = new FileManipulator(path, true);
      int numFiles = resources.length;
      TaskProgressManager.setMaximum(numFiles);

      // Calculations
      TaskProgressManager.setMessage(Language.get("Progress_PerformingCalculations"));
      int filesSize = 0;
      for (int i = 0; i < numFiles; i++) {
        filesSize += resources[i].getDecompressedLength();
      }

      // Write Header Data

      // 4 - Header? ((bytes)114 16 234 244)
      fm.writeByte(114);
      fm.writeByte(16);
      fm.writeByte(234);
      fm.writeByte(244);

      // 4 - null
      fm.writeInt(0);

      // 4 - Directory Offset
      fm.writeInt(16 + filesSize);

      // 4 - Number Of Files
      fm.writeInt(numFiles);

      // Write Files
      TaskProgressManager.setMessage(Language.get("Progress_WritingFiles"));
      write(resources, fm);

      // Write Directory
      TaskProgressManager.setMessage(Language.get("Progress_WritingDirectory"));
      long offset = 16;
      for (int i = 0; i < numFiles; i++) {
        long length = resources[i].getDecompressedLength();

        // X - Filename (terminated by (byte)10) (some starting with "..\" or "R:\")
        fm.writeString(resources[i].getName());
        fm.writeByte(10);

        // 4 - File Offset
        fm.writeInt((int) offset);

        // 4 - File Length
        fm.writeInt((int) length);

        offset += length;
      }

      fm.close();

    }
    catch (Throwable t) {
      logError(t);
    }
  }

}