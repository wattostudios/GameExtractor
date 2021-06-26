
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
public class Plugin_WAD_WAD extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_WAD_WAD() {

    super("WAD_WAD", "WAD_WAD");

    //         read write replace rename
    setProperties(true, false, true, true);

    setGames("WWE WMXXI");
    setExtensions("wad");
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
      if (fm.readString(3).equals("WAD")) {
        rating += 50;
      }

      // Version (1)
      if (fm.readInt() == 1) {
        rating += 5;
      }

      // null
      if (fm.readInt() == 0) {
        rating += 5;
      }

      // Number Of Files
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
  
  **********************************************************************************************
  **/
  @Override
  public Resource[] read(File path) {
    try {

      ExporterPlugin exporter = Exporter_ZLib.getInstance();
      addFileTypes();

      // RESETTING THE GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      // 3 - Header (WAD)
      // 4 - Version (1)
      // 4 - null
      fm.skip(11);

      // 4 - Number Of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      long arcSize = fm.getLength();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      int[] offsets = new int[numFiles];
      for (int i = 0; i < numFiles; i++) {
        // 4 - Unknown (Hash?)
        fm.skip(4);

        // 4 - File Offset
        offsets[i] = fm.readInt();
        FieldValidator.checkOffset(offsets[i], arcSize);

        // 4 - null
        fm.skip(4);
      }

      // Sort the file offsets
      java.util.Arrays.sort(offsets);

      // Read each file header
      for (int i = 0; i < numFiles; i++) {
        fm.seek(offsets[i]);

        // 2 - Length of file header [+2 for this field]
        int filenameLength = fm.readShort() - 9;
        FieldValidator.checkFilenameLength(filenameLength);

        // 4 - Decompressed Length (or null)
        int decompLength = fm.readInt();

        // 4 - null
        // 1 - Unknown (1)
        fm.skip(5);

        // X - Filename (determine the filename length from the LengthOfFileHeader field)
        String filename = fm.readString(filenameLength);

        // X - File Data
        long offset = (int) fm.getOffset();

        //path,id,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, filename, offset, 0, decompLength, exporter);

        TaskProgressManager.setValue(i);
      }

      // Calculate the file sizes
      for (int i = 0; i < numFiles - 1; i++) {
        resources[i].setLength((int) (resources[i + 1].getOffset() - resources[i].getOffset() - resources[i + 1].getNameLength() - 11));
        FieldValidator.checkLength(resources[i].getLength(), arcSize);
      }
      resources[numFiles - 1].setLength((int) (arcSize - resources[numFiles - 1].getOffset()));

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
  @SuppressWarnings("unused")
  public void write(Resource[] resources, File path) {
    try {

      ExporterPlugin exporter = Exporter_ZLib.getInstance();

      FileManipulator fm = new FileManipulator(path, true);
      FileManipulator src = new FileManipulator(new File(Settings.getString("CurrentArchive")), false);

      int numFiles = resources.length;
      TaskProgressManager.setMaximum(numFiles);

      // Calculations
      TaskProgressManager.setMessage(Language.get("Progress_PerformingCalculations"));
      int archiveSize = 16;
      int directorySize = 0;
      int filesSize = 0;
      for (int i = 0; i < numFiles; i++) {
        filesSize += resources[i].getDecompressedLength();
        directorySize += 8 + resources[i].getNameLength() + 1;
      }
      archiveSize += filesSize + directorySize;

      // Write Header Data

      // 3 - Header (WAD)
      fm.writeString("WAD");

      // 4 - Version (1)
      fm.writeInt(1);

      // 4 - null
      fm.writeInt(0);

      // 4 - Number Of Files
      fm.writeInt(numFiles);

      // Write Directory
      TaskProgressManager.setMessage(Language.get("Progress_WritingDirectory"));

      // Just fill it with the default data, then come back and change it later
      // when we know what the offsets are
      /*
       * for(int i=0;i<numFiles;i++){ // 4 - Unknown (Hash?) // 4 - File Offset // 4 - null
       * fm.writeBytes(src.readBytes(12)); }
       */

      fm.setLength(numFiles * 12 + 16);
      fm.seek(numFiles * 12 + 15);

      // Write Files
      TaskProgressManager.setMessage(Language.get("Progress_WritingFiles"));
      //write(resources, fm);

      int[] offsets = new int[numFiles];
      for (int i = 0; i < numFiles; i++) {
        offsets[i] = (int) fm.getOffset();

        Resource resource = resources[i];

        long decompLength = resource.getDecompressedLength();
        String filename = resource.getName();

        // 2 - Length of file header [+2 for this field]
        fm.writeShort((short) (9 + filename.length()));

        // 4 - Decompressed Length (or null)
        fm.writeInt((int) decompLength);

        // 4 - null
        fm.writeInt(0);

        // 1 - Unknown (1)
        fm.writeByte(1);

        // X - Filename (determine the filename length from the LengthOfFileHeader field)
        fm.writeString(filename);

        // X - File Data
        write(exporter, resource, fm);

        TaskProgressManager.setValue(i);

      }

      // Now we know the offsets, so go back and set them
      fm.seek(15);
      src.seek(15);

      // Write Directory
      TaskProgressManager.setMessage(Language.get("Progress_WritingDirectory"));

      for (int i = 0; i < numFiles; i++) {
        // 4 - Unknown (Hash?)
        //fm.skip(4);
        fm.writeBytes(src.readBytes(4));

        // 4 - File Offset
        fm.writeInt(offsets[i]);

        // 4 - null
        //fm.skip(4);
        fm.writeBytes(src.readBytes(4));
      }

      src.close();

      fm.close();

    }
    catch (Throwable t) {
      logError(t);
    }
  }

}
