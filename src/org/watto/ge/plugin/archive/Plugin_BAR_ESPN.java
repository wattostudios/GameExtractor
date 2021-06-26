
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
public class Plugin_BAR_ESPN extends ArchivePlugin {

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Plugin_BAR_ESPN() {

    super("BAR_ESPN", "BAR_ESPN");

    //         read write replace rename
    setProperties(true, false, true, false);

    setGames("Age Of Empires 3");
    setExtensions("bar");
    setPlatforms("PC");

    setFileTypes("xmb", "Unknown",
        "cam", "Camera",
        "ddt", "DDT Image",
        "hlsl", "HLSL Programming Script",
        "inc", "INC Programming Script",
        "org", "ORG Programming Script",
        "vsh", "VSH Programming Script",
        "gr2", "GR2 Animation");

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
      if (fm.readString(4).equals("ESPN")) {
        rating += 50;
      }

      // Version (2)
      if (fm.readInt() == 2) {
        rating += 5;
      }

      fm.skip(272);

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
   * Reads an [archive] File into the Resources
   **********************************************************************************************
   **/
  @Override
  @SuppressWarnings("unused")
  public Resource[] read(File path) {
    try {

      // NOTE - Compressed files MUST know their DECOMPRESSED LENGTH
      //      - Uncompressed files MUST know their LENGTH

      addFileTypes();

      // RESETTING THE GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = (int) fm.getLength();

      // 4 - Header (ESPN)
      // 4 - Version (2)
      // 4 - Unknown (1144201745)
      // 264 - null
      // 4 - Unknown
      fm.skip(280);

      // 4 - Number Of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // 8 - Directory Offset
      long dirOffset = fm.readInt();
      FieldValidator.checkOffset(dirOffset, arcSize);

      Resource[] resources = new Resource[numFiles];

      TaskProgressManager.setMaximum(numFiles);

      fm.seek(dirOffset);

      // 4 - Directory Name Length [*2 for unicode]
      int dirNameLength = fm.readInt();
      FieldValidator.checkFilenameLength(dirNameLength);

      // X - Directory Name (unicode) (including trailing slash)
      String dirName = fm.readUnicodeString(dirNameLength);

      // 4 - Number Of Files
      fm.skip(4);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {
        // 4 - File Offset
        long offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - File Length
        long length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - File Length
        long length2 = fm.readInt();
        if (length != length2) {
          //System.out.println(length + "-" + length2);
        }

        // 4 - Unknown
        // 2 - Unknown (4)
        // 2 - Unknown (9)
        // 2 - Unknown (21)
        // 2 - Unknown
        // 2 - Unknown
        // 2 - Unknown
        fm.skip(16);

        // 4 - Filename Length [*2 for unicode]
        int filenameLength = fm.readInt();
        FieldValidator.checkFilenameLength(filenameLength);

        // X - Filename (unicode)
        String filename = fm.readUnicodeString(filenameLength);

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
      FileManipulator src = new FileManipulator(new File(Settings.getString("CurrentArchive")), false);

      int numFiles = resources.length;
      TaskProgressManager.setMaximum(numFiles);

      // Calculations
      TaskProgressManager.setMessage(Language.get("Progress_PerformingCalculations"));

      long dirOffset = 292;
      for (int i = 0; i < numFiles; i++) {
        dirOffset += resources[i].getDecompressedLength();
      }

      // Write Header Data

      // 4 - Header (ESPN)
      // 4 - Version (2)
      // 4 - Unknown (1144201745)
      // 264 - null
      // 4 - Unknown
      // 4 - Number Of Files
      fm.writeBytes(src.readBytes(284));

      // 8 - Directory Offset
      long oldDirOffset = src.readLong();
      fm.writeLong(dirOffset);

      // Write Files
      TaskProgressManager.setMessage(Language.get("Progress_WritingFiles"));
      write(resources, fm);

      src.seek(oldDirOffset);

      // 4 - Directory Name Length [*2 for unicode]
      int oldDirNameLength = src.readInt();
      fm.writeInt(oldDirNameLength);

      // X - Directory Name (unicode) (including trailing slash)
      fm.writeBytes(src.readBytes(oldDirNameLength * 2));

      // 4 - Number Of Files
      fm.writeBytes(src.readBytes(4));

      // Write Directory
      TaskProgressManager.setMessage(Language.get("Progress_WritingDirectory"));
      long offset = 292;
      for (int i = 0; i < numFiles; i++) {
        Resource fd = resources[i];
        long length = fd.getDecompressedLength();

        // 4 - File Offset
        // 4 - File Length
        // 4 - File Length
        src.skip(12);
        fm.writeInt((int) offset);
        fm.writeInt((int) length);
        fm.writeInt((int) length);

        // 4 - Unknown
        // 2 - Unknown (4)
        // 2 - Unknown (9)
        // 2 - Unknown (21)
        // 2 - Unknown
        // 2 - Unknown
        // 2 - Unknown
        fm.writeBytes(src.readBytes(16));

        // 4 - Filename Length [*2 for unicode]
        int oldFileNameLength = src.readInt();
        fm.writeInt(oldFileNameLength);

        // X - Filename (unicode)
        fm.writeBytes(src.readBytes(oldFileNameLength * 2));

        offset += length;
      }

      src.close();
      fm.close();

    }
    catch (Throwable t) {
      logError(t);
    }
  }

}
