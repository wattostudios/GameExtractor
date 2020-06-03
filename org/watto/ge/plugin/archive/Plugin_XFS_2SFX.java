
package org.watto.ge.plugin.archive;

import java.io.File;
import org.watto.task.TaskProgressManager;
import org.watto.datatype.Archive;
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
import org.watto.io.converter.IntConverter;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_XFS_2SFX extends ArchivePlugin {

  int firstFileOffset = 0;
  int realNumFiles = 0;
  ExporterPlugin exporter;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_XFS_2SFX() {

    super("XFS_2SFX", "XFS_2SFX");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Apocalyptica");
    setExtensions("xfs");
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
      if (fm.readString(4).equals("2SFX")) {
        rating += 50;
      }

      //long arcSize = fm.getLength();

      // Number Of Files (approx)
      if (FieldValidator.checkNumFiles(fm.readInt() / 12)) {
        rating += 5;
      }

      // Number Of Files in this folder
      if (FieldValidator.checkNumFiles(fm.readInt())) {
        rating += 5;
      }

      // Folder Length
      if (FieldValidator.checkLength(fm.readInt())) {
        rating += 5;
      }

      fm.skip(4);

      // null
      if (fm.readInt() == 0) {
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
      exporter = Exporter_ZLib.getInstance();

      // RESETTING THE GLOBAL VARIABLES
      realNumFiles = 0;

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = (int) fm.getLength();

      // 4 - Header (2SFX)
      fm.skip(4);

      // 4 - First File Offset [+8]
      firstFileOffset = fm.readInt() + 8;
      FieldValidator.checkOffset(firstFileOffset);

      int numFiles = Archive.getMaxFiles();

      Resource[] resources = new Resource[numFiles];

      TaskProgressManager.setMaximum(arcSize);

      while (fm.getOffset() < firstFileOffset) {
        readDirectory(path, fm, resources);
      }

      resources = resizeResources(resources, realNumFiles);

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
  public void readDirectory(File path, FileManipulator fm, Resource[] resources) throws Exception {
    long arcSize = fm.getLength();

    long relOffset = fm.getOffset();

    // 4 - Number Of Files in this folder
    int numFiles = fm.readInt();
    //System.out.println("numFiles: " + numFiles + " at " + (fm.getOffset()-4));

    // 4 - Folder Length (including sub-folders)
    // 4 - Folder Length
    // 4 - null
    fm.skip(12);

    // Loop through directory
    for (int i = 0; i < numFiles; i++) {
      boolean subfolder = false;

      // 4 - File Offset
      byte[] offset_b = fm.readBytes(4);
      long offset = 0;
      if ((offset_b[3] & 128) == 128) {
        // sub-folder (relative to the start of this folder entry)
        offset_b[3] &= 127;
        offset = IntConverter.convertLittle(offset_b);
        offset += relOffset;
        subfolder = true;
      }
      else {
        // file (relative to the first file offset)
        offset = IntConverter.convertLittle(offset_b);
        offset += firstFileOffset;
      }
      FieldValidator.checkOffset(offset, arcSize);

      // 4 - File Length
      long length = fm.readInt();
      FieldValidator.checkLength(length, arcSize);

      // 4 - Hash?
      fm.skip(4);

      if (subfolder) {
        //System.out.println("FOLDER");
        // sub-folder
        long curPos = fm.getOffset();

        fm.seek(offset);
        readDirectory(path, fm, resources);

        fm.seek(curPos);
      }
      else {

        long curPos = fm.getOffset();
        fm.seek(offset);

        // 4 - Compressed Header
        String compHeader = fm.readString(4);
        long decompLength = length;
        boolean compressed = false;
        if (compHeader.equals("ZL00")) {
          // compressed
          compressed = true;

          // 4 - Decompressed Length
          decompLength = fm.readInt();

          // 4 - Compressed Length
          length = fm.readInt();

          // X - File Data
          offset = fm.getOffset();
        }

        fm.seek(curPos);

        //System.out.println("FILE");
        // file
        String filename = Resource.generateFilename(realNumFiles);

        //path,id,name,offset,length,decompLength,exporter
        resources[realNumFiles] = new Resource(path, filename, offset, length, decompLength);
        if (compressed) {
          resources[realNumFiles].setExporter(exporter);
        }
        realNumFiles++;

        TaskProgressManager.setValue(offset);
      }
    }

  }

}
