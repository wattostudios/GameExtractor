
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
import org.watto.io.converter.IntConverter;
import org.watto.io.converter.ShortConverter;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_AE extends ArchivePlugin {

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Plugin_AE() {

    super("AE", "AE");

    //         read write replace rename
    setProperties(true, true, true, true);

    setGames("Curse The Eye Of Isis");
    setExtensions("ae");
    setPlatforms("XBox");

    //setFileTypes("","",
    //             "",""
    //             );

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

      // Directory Length
      if (FieldValidator.checkLength(IntConverter.changeFormat(fm.readInt()), arcSize)) {
        rating += 5;
      }

      // Number Of Files
      if (FieldValidator.checkNumFiles(IntConverter.changeFormat(fm.readInt()))) {
        rating += 5;
      }

      // null
      if (fm.readLong() == 0) {
        rating += 5;
      }

      // null
      if (fm.readLong() == 0) {
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

      //ExporterPlugin exporter = Exporter_ZLib.getInstance();

      // RESETTING GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 4 - Directory Length (including null padding at the end of the directory)
      fm.skip(4);

      // 4 - Number Of Files?
      int numFiles = IntConverter.changeFormat(fm.readInt());
      FieldValidator.checkNumFiles(numFiles);

      // 2040 - null Padding to a multiple of 2048 bytes
      fm.seek(2048);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {

        // 4 - File Offset
        long offset = IntConverter.changeFormat(fm.readInt());
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - File Length (including null padding at the end of the file data)
        long length = IntConverter.changeFormat(fm.readInt());
        FieldValidator.checkLength(length, arcSize);

        // 2 - null
        fm.skip(2);

        // 2 - Filename Length
        short filenameLength = ShortConverter.changeFormat(fm.readShort());
        FieldValidator.checkFilenameLength(filenameLength);

        // X - Filename
        String filename = fm.readString(filenameLength);

        // 0-3 - null Padding to a multiple of 4 bytes
        int paddingSize = 4 - (filenameLength % 4);
        if (paddingSize != 4) {
          fm.skip(paddingSize);
        }

        //path,name,offset,length,decompLength,exporter
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

      long dirLength = 2048 + (numFiles * 12);
      for (int i = 0; i < numFiles; i++) {
        int filenameLength = resources[i].getNameLength();
        int paddingSize = 4 - (filenameLength % 4);
        if (paddingSize != 4) {
          filenameLength += paddingSize;
        }
        dirLength += filenameLength;
      }

      int paddingSize = (int) (2048 - (dirLength % 2048));
      if (paddingSize != 2048) {
        dirLength += paddingSize;
      }

      // Write Header Data

      // 4 - Directory Length (including null padding at the end of the directory)
      fm.writeInt(IntConverter.changeFormat((int) dirLength));

      // 4 - Number Of Files?
      fm.writeInt(IntConverter.changeFormat(numFiles));

      // 2040 - null Padding to a multiple of 2048 bytes
      for (int p = 0; p < 2040; p++) {
        fm.writeByte(0);
      }

      // Write Directory
      TaskProgressManager.setMessage(Language.get("Progress_WritingDirectory"));
      long offset = dirLength;
      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];
        long decompLength = resource.getDecompressedLength();
        String name = resource.getName();

        paddingSize = (int) (2048 - (decompLength % 2048));
        if (paddingSize != 2048) {
          decompLength += paddingSize;
        }

        // 4 - File Offset
        fm.writeInt(IntConverter.changeFormat((int) offset));

        // 4 - File Length (including null padding at the end of the file data)
        fm.writeInt(IntConverter.changeFormat((int) decompLength));

        // 2 - null
        fm.writeInt(ShortConverter.changeFormat((short) 0));

        // 2 - Filename Length
        fm.writeInt(ShortConverter.changeFormat((short) name.length()));

        // X - Filename
        fm.writeString(name);

        // 0-3 - null Padding to a multiple of 4 bytes
        paddingSize = 4 - (name.length() % 4);
        if (paddingSize != 4) {
          for (int p = 0; p < paddingSize; p++) {
            fm.writeByte(0);
          }
        }

        offset += decompLength;
      }

      // 0-2047 - null Padding to a multiple of 2408 bytes
      paddingSize = (int) (2048 - (dirLength % 2048));
      if (paddingSize != 2048) {
        for (int p = 0; p < paddingSize; p++) {
          fm.writeByte(0);
        }
      }

      // Write Files
      TaskProgressManager.setMessage(Language.get("Progress_WritingFiles"));

      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];
        long decompLength = resource.getDecompressedLength();

        // X - File Data
        write(resource, fm);

        // 0-2047 - null Padding to a multiple of 2048 bytes
        paddingSize = (int) (2048 - (decompLength % 2048));
        if (paddingSize != 2048) {
          for (int p = 0; p < paddingSize; p++) {
            fm.writeByte(0);
          }
        }
      }

      fm.close();

    }
    catch (Throwable t) {
      logError(t);
    }
  }

}
