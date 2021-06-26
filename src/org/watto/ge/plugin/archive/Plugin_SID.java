
package org.watto.ge.plugin.archive;

import java.io.File;
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
import org.watto.ge.plugin.exporter.Exporter_ZIP_Single;
import org.watto.io.FileManipulator;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_SID extends ArchivePlugin {

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Plugin_SID() {

    super("SID", "SID");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Steam Install System");
    setExtensions("sid"); // MUST BE LOWER CASE
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

      String dirName = fm.getFilePath();
      if (dirName.length() < 6) {
        return 0;
      }
      dirName = dirName.substring(0, dirName.length() - 6) + ".sim";
      if (new File(dirName).exists()) {
        rating += 25;
      }
      else {
        return 0;
      }

      long arcSize = fm.getLength();

      // First File Length
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

      ExporterPlugin exporter = Exporter_ZIP_Single.getInstance();

      // RESETTING GLOBAL VARIABLES

      long arcSize = (int) path.length();

      String dirName = path.getAbsolutePath();
      if (dirName.length() < 6) {
        return null;
      }
      File sourcePath = new File(dirName.substring(0, dirName.length() - 6) + ".sim");
      if (!sourcePath.exists()) {
        return null;
      }

      FileManipulator fm = new FileManipulator(sourcePath, false);

      long simSize = (int) sourcePath.length();

      // 4 - Hash?
      // 4 - Unknown (1)
      // 4 - Unknown (1)
      fm.skip(12);

      // 4 - Details Directory Offset
      long dirOffset = fm.readInt() + 16;
      FieldValidator.checkOffset(dirOffset, arcSize);

      fm.seek(dirOffset);

      // 4 - Directory Length (not including these 2 fields)
      fm.skip(4);

      // 4 - Number Of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      int[] nameOffsets = new int[numFiles];
      int[] parentOffsets = new int[numFiles];

      for (int i = 0; i < numFiles; i++) {
        // 4 - Directory Name Offset
        int nameOffset = fm.readInt() + 16;
        FieldValidator.checkOffset(nameOffset, simSize);
        nameOffsets[i] = nameOffset;

        // 4 - Parent Directory Name Offset
        int parentOffset = fm.readInt() + 16;
        FieldValidator.checkOffset(parentOffset, simSize);
        parentOffsets[i] = parentOffset;

        // 4 - Unknown (4261)
        fm.skip(4);

        // 8 - File Offset
        long offset = fm.readLong();
        FieldValidator.checkOffset(offset, arcSize);

        // 8 - Decompressed File Length
        long decompLength = fm.readLong();
        FieldValidator.checkLength(decompLength);

        // 4 - Unknown (1)
        fm.skip(4);

        //path,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, "", offset, decompLength, decompLength);

        TaskProgressManager.setValue(i);
      }

      //calculateFileSizes(resources,arcSize);
      for (int i = 0; i < numFiles - 1; i++) {
        long compLength = resources[i + 1].getOffset() - resources[i].getOffset();
        if (compLength == resources[i].getDecompressedLength() + 8) {
          // not compressed
          //resources[i].setLength(compLength-16);
          //resources[i].setOffset(resources[i].getOffset()+8);
        }
        else {
          // compressed
          resources[i].setLength(compLength);
          resources[i].setOffset(resources[i].getOffset() + 8);
          resources[i].setExporter(exporter);
        }
      }
      long compLength = arcSize - resources[numFiles - 1].getOffset();
      if (compLength == resources[numFiles - 1].getDecompressedLength() + 8) {
        // not compressed
        //resources[numFiles-1].setLength(compLength-16);
        //resources[numFiles-1].setOffset(resources[numFiles-1].getOffset()+8);
      }
      else {
        // compressed
        resources[numFiles - 1].setLength(compLength);
        resources[numFiles - 1].setOffset(resources[numFiles - 1].getOffset() + 8);
        resources[numFiles - 1].setExporter(exporter);
      }

      // determine names
      String[] names = new String[numFiles];
      for (int i = 0; i < numFiles; i++) {
        fm.seek(parentOffsets[i]);

        // X - Parent Filename (null terminated)
        names[i] = fm.readNullString();
      }

      for (int i = 0; i < numFiles; i++) {
        fm.seek(nameOffsets[i]);

        // X - Filename (null terminated)
        names[i] += "\\" + fm.readNullString();
        resources[i].setName(names[i]);
      }

      fm.close();

      return resources;

    }
    catch (Throwable t) {
      logError(t);
      return null;
    }
  }

}
