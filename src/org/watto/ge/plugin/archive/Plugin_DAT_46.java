
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
import org.watto.io.converter.ByteConverter;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_DAT_46 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_DAT_46() {

    super("DAT_46", "DAT_46");

    //         read write replace rename
    setProperties(true, true, true, true);

    setGames("Project Aftermath");
    setExtensions("dat"); // MUST BE LOWER CASE
    setPlatforms("PC");

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

      // Header
      int header1 = ByteConverter.unsign(fm.readByte());
      int header2 = ByteConverter.unsign(fm.readByte());
      int header3 = ByteConverter.unsign(fm.readByte());
      int header4 = ByteConverter.unsign(fm.readByte());
      if (header1 == 106 && header2 == 246 && header3 == 253 && header4 == 52) {
        rating += 50;
      }

      fm.skip(4);

      // Number Of Files
      if (FieldValidator.checkNumFiles(fm.readInt())) {
        rating += 5;
      }

      // Archive Header Length
      if (fm.readInt() == 28) {
        rating += 5;
      }

      fm.skip(4);

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

      // 4 - Header (106,246,253,52)
      // 4 - Version (1)
      fm.skip(8);

      // 4 - Number Of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // 4 - Archive Header Length (28)
      // 4 - File Data Length
      fm.skip(8);

      // 4 - Directory Offset
      long dirOffset = fm.readInt();
      FieldValidator.checkOffset(dirOffset, arcSize);

      // 4 - Directory Length
      fm.seek(dirOffset);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {

        // 1 - Filename Length
        int filenameLength = ByteConverter.unsign(fm.readByte());

        // X - Filename
        String filename = fm.readString(filenameLength);

        // 4 - File Offset [+28 (because it is relative to the start of the file data)]
        long offset = fm.readInt() + 28;
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - File Length
        long length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

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

      long fileDataLength = 0;
      long directoryLength = 0;
      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];
        fileDataLength += resource.getDecompressedLength();
        directoryLength += 8 + resource.getNameLength() + 1;
      }

      // Write Header Data

      // 4 - Header (106,246,253,52)
      fm.writeByte((byte) 106);
      fm.writeByte((byte) 246);
      fm.writeByte((byte) 253);
      fm.writeByte((byte) 52);

      // 4 - Version (1)
      fm.writeInt(1);

      // 4 - Number Of Files
      fm.writeInt((int) numFiles);

      // 4 - Archive Header Length (28)
      fm.writeInt(28);

      // 4 - File Data Length
      fm.writeInt((int) fileDataLength);

      // 4 - Directory Offset
      fm.writeInt((int) (fileDataLength + 28));

      // 4 - Directory Length
      fm.writeInt((int) directoryLength);

      // Write Files
      TaskProgressManager.setMessage(Language.get("Progress_WritingFiles"));
      write(resources, fm);

      // Write Directory
      TaskProgressManager.setMessage(Language.get("Progress_WritingDirectory"));
      long offset = 0;
      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];
        long decompLength = resource.getDecompressedLength();
        String filename = resource.getName();

        if (filename.length() > 255) {
          filename = filename.substring(0, 255);
        }

        // 1 - Filename Length
        fm.writeByte(filename.length());

        // X - Filename
        fm.writeString(resource.getName());

        // 4 - File Offset [+28 (because it is relative to the start of the file data)]
        fm.writeInt((int) offset);

        // 4 - File Length
        fm.writeInt((int) decompLength);

        offset += decompLength;
      }

      fm.close();

    }
    catch (Throwable t) {
      logError(t);
    }
  }

}
