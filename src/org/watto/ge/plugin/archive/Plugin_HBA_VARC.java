
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
public class Plugin_HBA_VARC extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_HBA_VARC() {

    super("HBA_VARC", "HBA_VARC");

    //         read write replace rename
    setProperties(true, true, true, true);

    setGames("Thread Space Hyperbol");
    setExtensions("hba");
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
      if (fm.readString(4).equals("VARC")) {
        rating += 50;
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
   * Reads an [archive] File into the Resources
   **********************************************************************************************
   **/
  @Override
  public Resource[] read(File path) {
    try {

      // NOTE - Compressed files MUST know their DECOMPRESSED LENGTH
      //      - Uncompressed files MUST know their LENGTH

      addFileTypes();

      ExporterPlugin exporter = Exporter_ZLib.getInstance();

      // RESETTING GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 4 - Header (VARC)
      fm.skip(4);

      // 4 - Number Of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {
        // 4 - Header (VARF)
        fm.skip(4);

        // 128 - Filename (null terminated)
        String filename = fm.readNullString(128);
        FieldValidator.checkFilename(filename);

        // 4 - Compression Flag? (1)
        fm.skip(4);

        // 4 - Compressed Length
        long length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - Decompressed Length
        long decompLength = fm.readInt();
        FieldValidator.checkLength(decompLength);

        // X - File Data (ZLib Compression)
        long offset = fm.getOffset();
        fm.skip(length);

        //path,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, filename, offset, length, decompLength);

        if (length != decompLength) {
          resources[i].setExporter(exporter);
        }

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

      ExporterPlugin exporter = Exporter_ZLib.getInstance();

      FileManipulator fm = new FileManipulator(path, true);
      int numFiles = resources.length;
      TaskProgressManager.setMaximum(numFiles);

      // Write Header Data

      // 4 - Header (VARC)
      fm.writeString("VARC");

      // 4 - Number Of Files
      fm.writeInt((int) numFiles);

      // Write Directory
      TaskProgressManager.setMessage(Language.get("Progress_WritingFiles"));

      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];
        long decompLength = resource.getDecompressedLength();

        // 4 - Header (VARF)
        fm.writeString("VARF");

        // 128 - Filename (null terminated)
        fm.writeNullString(resource.getName(), 128);

        // 4 - Compression Flag? (1)
        fm.writeInt(1);

        // 4 - Compressed Length
        long compOffset = fm.getOffset();
        fm.writeInt((int) decompLength);

        // 4 - Decompressed Length
        fm.writeInt((int) decompLength);

        // X - File Data (ZLib Compression)
        long compLength = write(exporter, resources[i], fm);

        long nextOffset = fm.getOffset();

        // go back and write the compressed length
        fm.seek(compOffset);
        fm.writeInt((int) compLength);
        fm.seek(nextOffset);
      }

      fm.close();

    }
    catch (Throwable t) {
      logError(t);
    }
  }

}
