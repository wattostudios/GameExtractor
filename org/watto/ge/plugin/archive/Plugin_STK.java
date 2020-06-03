
package org.watto.ge.plugin.archive;

import java.io.File;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.exporter.Exporter_LZSS;
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
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_STK extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_STK() {

    super("STK", "STK");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Adi's Comprehensive Learning System",
        "Goblins",
        "Goblins 2",
        "Goblins 3");
    setExtensions("stk", "itk", "ltk");
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

      // Number Of Files
      if (FieldValidator.checkNumFiles(fm.readShort())) {
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

      addFileTypes();

      FileManipulator fm = new FileManipulator(path, false);

      ExporterPlugin exporter = Exporter_LZSS.getInstance();

      long arcSize = fm.getLength();

      // 2 - Number Of Files
      int numFiles = fm.readShort();
      FieldValidator.checkNumFiles(numFiles);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Files Directory
      int realNumFiles = 0;
      boolean[] fileCompressed = new boolean[numFiles];
      for (int i = 0; i < numFiles; i++) {

        // 13 - Filename (null)
        String filename = fm.readNullString(13);

        // 4 - File Length
        long length = fm.readInt();

        // 4 - File Offset
        long offset = fm.readInt();

        // 1 - Compression (0/1)
        int compression = fm.readByte();

        if (length == 0) {
          continue;
        }
        FieldValidator.checkFilename(filename);
        FieldValidator.checkOffset(offset, arcSize);

        FieldValidator.checkLength(length, arcSize);

        //path,id,name,offset,length,decompLength,exporter
        Resource resource = new Resource(path, filename, offset, length);

        if (compression == 1) {
          resource.setExporter(exporter);
          fileCompressed[realNumFiles] = true;
        }
        else {
          fileCompressed[realNumFiles] = false;
        }

        resources[realNumFiles] = resource;
        realNumFiles++;

        TaskProgressManager.setValue(i);
      }

      if (realNumFiles < numFiles) {
        resources = resizeResources(resources, realNumFiles);
      }

      //go and set the decompressed lengths
      fm.getBuffer().setBufferSize(4);
      for (int i = 0; i < realNumFiles; i++) {
        if (fileCompressed[i]) {
          Resource resource = resources[i];
          fm.seek(resource.getOffset());

          // 4 - Decompressed Length
          int decompLength = fm.readInt();
          FieldValidator.checkLength(decompLength);

          resource.setOffset(resource.getOffset() + 4);
          resource.setLength(resource.getLength() - 4);
          resource.setDecompressedLength(decompLength);
          TaskProgressManager.setValue(i);
        }
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
