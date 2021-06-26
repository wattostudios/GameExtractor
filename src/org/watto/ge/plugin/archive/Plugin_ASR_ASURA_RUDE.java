
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
public class Plugin_ASR_ASURA_RUDE extends ArchivePlugin {

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Plugin_ASR_ASURA_RUDE() {

    super("ASR_ASURA_RUDE", "ASR_ASURA_RUDE");

    //         read write replace rename
    setProperties(true, true, true, false);

    setGames("Sniper Elite");
    setExtensions("asr");
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
      if (fm.readString(12).equals("Asura   RUDE")) {
        rating += 50;
      }

      long arcSize = fm.getLength();

      // Archive Size
      if (fm.readInt() + 24 == arcSize) {
        rating += 5;
      }

      // Version
      if (fm.readInt() == 1) {
        rating += 5;
      }

      fm.skip(4);

      // Number Of Text Strings
      if (FieldValidator.checkNumFiles(fm.readInt())) {
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

      //long arcSize = (int) fm.getLength();

      // 8 - Header 1 (Asura   )
      // 4 - Header 2 (RUDE)
      // 4 - Archive Length [+24]
      // 8 - Version (1)
      fm.skip(24);

      // 4 - Number Of Text Strings
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      Resource[] resources = new Resource[numFiles];

      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {
        // X - Text String
        long offset = fm.getOffset();

        String filename = fm.readNullString();
        if (filename.length() > 100) {
          filename = filename.substring(0, 100);
        }

        long length = filename.length();

        // 0-3 - null Padding to a multiple of 4 bytes
        long padding = 4 - ((length + 1) % 4);
        if (padding < 4) {
          fm.skip(padding);
        }

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

      int archiveSize = 20;
      for (int i = 0; i < numFiles; i++) {
        long length = resources[i].getDecompressedLength() + 1;
        long paddingSize = 4 - (length % 4);
        if (paddingSize < 4) {
          length += paddingSize;
        }
        archiveSize += length;
      }

      // Write Header Data

      // 8 - Header 1 (Asura   )
      // 4 - Header 2 (RUDE)
      fm.writeString("Asura   RUDE");

      // 4 - Archive Length [+24]
      fm.writeInt(archiveSize);

      // 8 - Version (0)
      fm.writeLong(0);

      // 4 - Number Of Text Strings
      fm.writeInt(numFiles);

      // Write Directory
      TaskProgressManager.setMessage(Language.get("Progress_WritingFiles"));
      for (int i = 0; i < numFiles; i++) {
        Resource fd = resources[i];
        long length = fd.getDecompressedLength() + 1;

        // X - Text String
        write(resources[i], fm);

        // 1 - null Text String Terminator
        fm.writeByte(0);

        // 0-3 - null Padding to a multiple of 4 bytes
        long padding = 4 - (length % 4);
        if (padding < 4) {
          for (int p = 0; p < padding; p++) {
            fm.writeByte(0);
          }
        }

      }

      // 16 - null
      for (int i = 0; i < 16; i++) {
        fm.writeByte(0);
      }

      fm.close();

    }
    catch (Throwable t) {
      logError(t);
    }
  }

}
