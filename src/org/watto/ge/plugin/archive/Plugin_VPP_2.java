
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
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.exporter.Exporter_ZLib;
import org.watto.io.FileManipulator;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_VPP_2 extends ArchivePlugin {

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Plugin_VPP_2() {

    super("VPP_2", "VPP_2");

    //         read write replace rename
    setProperties(true, true, true, true);

    setExtensions("vpp");
    setGames("The Punisher");
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
      if (fm.readString(4).equals("" + (char) 206 + (char) 10 + (char) 137 + (char) 81)) {
        rating += 50;
      }

      // Version (3)
      if (fm.readInt() == 3) {
        rating += 5;
      }

      // Number Of Files
      if (FieldValidator.checkNumFiles(fm.readInt())) {
        rating += 5;
      }

      long arcSize = fm.getLength();

      // Archive size
      if (FieldValidator.checkEquals(fm.readInt(), arcSize)) {
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
      // 4 - Header (206 10 137 81)
      // 4 - Version (3)
      fm.skip(8);

      // 4 - numFiles
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      long arcSize = fm.getLength();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // 4 - Archive Length
      // 4 - Unknown (64)
      // 2028 - Filler (ie filled to position 2048 using nulls)
      fm.seek(2048);

      int dirPaddingSize = (2048 - ((numFiles * 32) % 2048));
      if (dirPaddingSize == 2048) {
        dirPaddingSize = 0;
      }
      long offset = (numFiles * 32) + 2048 + dirPaddingSize;

      for (int i = 0; i < numFiles; i++) {
        // 24 - Filename
        String filename = fm.readNullString(24);
        FieldValidator.checkFilename(filename);

        // 4 - Decompressed File Length
        int decompLength = fm.readInt();
        FieldValidator.checkLength(decompLength);

        // 4 - Compressed File Length
        int length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        //path,id,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, filename, offset, length, decompLength);
        if (decompLength != length) {
          resources[i].setExporter(exporter);
        }

        offset += length + (2048 - (length % 2048));// add padding between each file???

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

      int dirPaddingSize = (2048 - ((numFiles * 32) % 2048));
      if (dirPaddingSize == 2048) {
        dirPaddingSize = 0;
      }

      long arcSize = 2048 + (numFiles * 32) + dirPaddingSize;

      for (int i = 0; i < numFiles; i++) {
        long filePaddingSize = (2048 - ((resources[i].getDecompressedLength()) % 2048));
        if (filePaddingSize == 2048) {
          filePaddingSize = 0;
        }

        arcSize += resources[i].getDecompressedLength() + filePaddingSize;
      }

      // Write Header Data

      // 4 - Header (206 10 137 81)
      fm.writeByte(206);
      fm.writeByte(10);
      fm.writeByte(137);
      fm.writeByte(81);

      // 4 - Version (3)
      fm.writeInt(3);

      // 4 - Number Of Files
      fm.writeInt(numFiles);

      // 4 - Archive Length
      fm.writeInt((int) arcSize);

      // 4 - Unknown (64)
      fm.writeInt(64);

      // 2028 - Filler (ie filled to position 2048 using nulls)
      for (int i = 0; i < 2028; i++) {
        fm.writeByte(0);
      }

      // Write Directory
      TaskProgressManager.setMessage(Language.get("Progress_WritingDirectory"));
      for (int i = 0; i < numFiles; i++) {
        long length = resources[i].getDecompressedLength();

        // 24 - Filename (null)
        fm.writeNullString(resources[i].getName(), 24);

        // 4 - File Length
        fm.writeInt((int) length);

        // 4 - File Length
        fm.writeInt((int) length);
      }

      // PADDING AFTER DIRECTORY
      for (int i = 0; i < dirPaddingSize; i++) {
        fm.writeByte(0);
      }

      // Write Files
      TaskProgressManager.setMessage(Language.get("Progress_WritingFiles"));
      //write(resources, fm);

      for (int i = 0; i < numFiles; i++) {
        // X - File Data
        write(resources[i], fm);

        long length = resources[i].getDecompressedLength();

        long filePaddingSize = (2048 - ((length) % 2048));
        if (filePaddingSize == 2048) {
          filePaddingSize = 0;
        }

        // X - Padding to a multiple of 2048 bytes
        for (int j = 0; j < filePaddingSize; j++) {
          fm.writeByte(0);
        }

      }

      fm.close();

    }
    catch (Throwable t) {
      logError(t);
    }
  }

}