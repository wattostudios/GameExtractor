
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
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.exporter.Exporter_ZLib;
import org.watto.io.FileManipulator;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_DAT_5 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_DAT_5() {

    super("DAT_5", "DAT_5");

    //         read write replace rename
    setProperties(true, false, true, true);

    setExtensions("dat");
    setGames("Imperium Galactica 2");
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

      // go to end of file - 8
      if (fm.getLength() - 8 < 0) {
        return 0;
      }
      fm.seek(fm.getLength() - 8);

      // Number Of Files
      if (FieldValidator.checkNumFiles(fm.readInt())) {
        rating += 5;
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

      ExporterPlugin exporter = Exporter_ZLib.getInstance();
      addFileTypes();

      FileManipulator fm = new FileManipulator(path, false);

      // go to end of file - 8
      fm.seek(fm.getLength() - 8);

      // 4 - numFiles
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      long arcSize = fm.getLength();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // 4 - Directory Offset (from END of file)
      long dirOffset = (int) fm.getLength() - fm.readInt();

      fm.seek(dirOffset);
      int readLength = 0;
      for (int i = 0; i < numFiles; i++) {
        // X - Filename (null)
        String filename = fm.readNullString();
        FieldValidator.checkFilename(filename);

        // 4 - Data Offset
        long offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - Unknown
        fm.skip(4);

        // 4 - Compressed File Length
        long length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - Raw File Length
        int decompLength = fm.readInt();

        // 4 - Unknown
        fm.skip(4);

        //path,id,name,offset,length,decompLength,exporter
        // TEST - Don't know if this plugin works or has been verified - make it unverified!
        resources[i] = new Resource(path, filename, offset, length, decompLength, exporter);

        TaskProgressManager.setValue(readLength);
        readLength += length;
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

      ExporterPlugin exporter = Exporter_ZLib.getInstance();

      FileManipulator fm = new FileManipulator(path, true);
      FileManipulator src = new FileManipulator(new File(Settings.getString("CurrentArchive")), false);

      int numFiles = resources.length;
      TaskProgressManager.setMaximum(numFiles);

      TaskProgressManager.setMessage(Language.get("Progress_WritingFiles"));
      long[] compressedLengths = write(exporter, resources, fm);

      int dirStart = (int) fm.getOffset();

      src.seek((int) src.getLength() - 4);
      src.seek((int) src.getLength() - src.readInt());// seek to correct point in the source file

      long offset = 0;

      TaskProgressManager.setMessage(Language.get("Progress_WritingDirectory"));
      for (int i = 0; i < numFiles; i++) {
        Resource fd = resources[i];
        long length = fd.getDecompressedLength();
        String name = fd.getName();

        // X - Filename (null)
        src.readNullString();
        fm.writeNullString(resources[i].getName());

        // 4 - Data Offset
        src.skip(4);
        fm.writeInt((int) offset);

        // 4 - Unknown
        fm.writeBytes(src.readBytes(4));

        // 4 - Compressed File Length
        src.skip(4);
        fm.writeInt((int) compressedLengths[i]);

        // 4 - Raw File Length
        src.skip(4);
        fm.writeInt((int) length);

        // 4 - Unknown
        fm.writeBytes(src.readBytes(4));

        offset += compressedLengths[i];
      }

      // Loop 3 - End Of Directory Data

      // 4 - numFiles
      fm.writeInt(numFiles);

      // 4 - directoryOffset (relative to end of file)
      fm.writeInt((int) fm.getOffset() + 4 - dirStart);

      src.close();

      fm.close();

    }
    catch (Throwable t) {
      logError(t);
    }
  }

}