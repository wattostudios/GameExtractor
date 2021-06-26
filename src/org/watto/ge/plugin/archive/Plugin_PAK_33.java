
package org.watto.ge.plugin.archive;

import java.io.File;
import org.watto.Language;
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

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_PAK_33 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_PAK_33() {

    super("PAK_33", "PAK_33");

    //         read write replace rename
    setProperties(true, true, false, false);

    setGames("Dracula Origin");
    setExtensions("pak"); // MUST BE LOWER CASE
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

      // 1 - Compression Flag (1=compressed)
      if (fm.readByte() == 1) {
        rating += 5;
      }

      long arcSize = fm.getLength();

      // 4 - Compressed File Length
      if (FieldValidator.checkLength(fm.readInt(), arcSize)) {
        rating += 5;
      }

      // 4 - Decompressed File Length
      if (FieldValidator.checkLength(fm.readInt())) {
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

      // 4 - Number Of Files
      int numFiles = Archive.getMaxFiles();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(arcSize);

      // Loop through directory
      int realNumFiles = 0;
      while (fm.getOffset() + 9 < arcSize) {
        // 1 - Compression Flag (1=compressed)
        byte compressed = fm.readByte();

        // 4 - Compressed File Length
        long length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - Decompressed File Length
        long decompressedLength = fm.readInt();
        FieldValidator.checkLength(decompressedLength);

        // X - File Data
        long offset = fm.getOffset();
        FieldValidator.checkOffset(offset, arcSize);

        fm.skip(length);

        String filename = Resource.generateFilename(realNumFiles);

        //path,name,offset,length,decompLength,exporter
        if (compressed == 1) {
          resources[realNumFiles] = new Resource(path, filename, offset, length, decompressedLength, exporter);
        }
        else {
          resources[realNumFiles] = new Resource(path, filename, offset, length);
        }

        TaskProgressManager.setValue(offset);
        realNumFiles++;
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
   * Writes an [archive] File with the contents of the Resources
   **********************************************************************************************
   **/
  @Override
  public void write(Resource[] resources, File path) {
    try {

      FileManipulator fm = new FileManipulator(path, true);
      int numFiles = resources.length;
      TaskProgressManager.setMaximum(numFiles);

      ExporterPlugin exporter = Exporter_ZLib.getInstance();

      // Write Directory
      TaskProgressManager.setMessage(Language.get("Progress_WritingFiles"));

      long[] offsets = new long[numFiles];
      long[] lengths = new long[numFiles];

      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];
        long decompLength = resource.getDecompressedLength();

        // 1 - Compression Flag (1=compressed)
        fm.writeByte(1);

        // 4 - Compressed File Length
        offsets[i] = fm.getOffset();
        fm.writeInt((int) decompLength);

        // 4 - Decompressed File Length
        fm.writeInt((int) decompLength);

        // X - File Data
        long compressedLength = write(exporter, resources[i], fm);
        lengths[i] = compressedLength;
      }

      // go back and write the compressed lengths
      for (int i = 0; i < numFiles; i++) {
        fm.seek(offsets[i]);
        // 4 - Compressed File Length
        fm.writeInt((int) lengths[i]);
      }

      fm.close();

    }
    catch (Throwable t) {
      logError(t);
    }
  }

}
