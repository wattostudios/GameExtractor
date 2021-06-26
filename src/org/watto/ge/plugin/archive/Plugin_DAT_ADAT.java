
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
public class Plugin_DAT_ADAT extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_DAT_ADAT() {

    super("DAT_ADAT", "DAT_ADAT");

    //         read write replace rename
    setProperties(true, false, false, false);

    setExtensions("dat");
    setGames("Anachronox");
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
      if (fm.readString(4).equals("ADAT")) {
        rating += 50;
      }

      long arcSize = fm.getLength();

      // Directory Offset
      if (FieldValidator.checkOffset(fm.readInt(), arcSize)) {
        rating += 5;
      }

      // Number Of Files
      int numFiles = fm.readInt() / 144;
      if (FieldValidator.checkNumFiles(numFiles)) {
        rating += 5;
      }

      // Unknown (9)
      if (fm.readInt() == 9) {
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
      long arcSize = fm.getLength();

      // 4 - Header (ADAT)
      fm.skip(4);

      // 4 - dirOffset
      long dirOffset = fm.readInt();
      FieldValidator.checkOffset(dirOffset, arcSize);

      // 4 - numFiles (dirLength / 144)
      int numFiles = fm.readInt() / 144;
      FieldValidator.checkNumFiles(numFiles);

      // 4 - Unknown (9)

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      fm.seek(dirOffset);

      for (int i = 0; i < numFiles; i++) {
        // 128 - Filename (null)
        String filename = fm.readNullString(128);
        FieldValidator.checkFilename(filename);

        // 4 - fileOffset
        long offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - Decompressed File Length
        int decompLength = fm.readInt();

        // 4 - Compressed File Length
        long length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - Unknown (Hash?)
        fm.skip(4);

        //path,id,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, filename, offset, length, decompLength, exporter);

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

      // TEST - check that the writing works, then enable canWrite=true!
      // NOTE - NOT ACTUALLY USED!!!
      // Needs to implement the compression of data before this can be used
      ExporterPlugin exporter = Exporter_ZLib.getInstance();

      int numFiles = resources.length;
      TaskProgressManager.setMaximum(numFiles);

      FileManipulator fm = new FileManipulator(path, true);
      FileManipulator src = new FileManipulator(new File(Settings.getString("CurrentArchive")), false);

      // 4 - Header (ADAT)
      // 4 - dirOffset
      // 4 - dirLength
      // 4 - Unknown (9)
      fm.writeBytes(src.readBytes(16));

      TaskProgressManager.setMessage(Language.get("Progress_WritingFiles"));
      long[] compressedLengths = write(exporter, resources, fm);

      TaskProgressManager.setMessage(Language.get("Progress_WritingDirectory"));
      long offset = 12;
      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];
        long decompLength = resource.getDecompressedLength();

        // 128 - Filename (null)
        // 4 - FileOffset
        // 4 - Decompressed Length
        // 4 - Compressed Length
        src.skip(140);
        fm.writeNullString(resources[i].getName(), 128);
        fm.writeInt((int) offset);
        fm.writeInt((int) decompLength);
        fm.writeInt((int) compressedLengths[i]);

        // 4 - Unknown
        fm.writeBytes(src.readBytes(4));

        offset += compressedLengths[i];
      }

      src.close();

      fm.close();

    }
    catch (Throwable t) {
      logError(t);
    }
  }

}