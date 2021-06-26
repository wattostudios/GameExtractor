
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
public class Plugin_FPF extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_FPF() {

    super("FPF", "FPF");

    //         read write replace rename
    setProperties(true, false, true, false);

    setExtensions("fpf");
    setGames("Monopoly Deluxe");
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

      fm.skip(6);

      // Number Of Files
      int numFiles = fm.readInt() / 4;
      if (FieldValidator.checkNumFiles(numFiles)) {
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

      // 4 - Header
      // 2 - Unknown (null)
      fm.skip(6);

      // 4 - first file offset
      int numFiles = fm.readInt() / 4;
      FieldValidator.checkNumFiles(numFiles);

      //long arcSize = fm.getLength();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      fm.seek(6);

      int realNumFiles = 0;
      int readLength = 0;
      for (int i = 0; i < numFiles; i++) {

        // 4 - Data Offset (if this is greater than 1070000000 then discard and continue - it is not included in the numFiles count)
        long offset = fm.readInt();
        if (offset < fm.getLength() && offset >= 0) {

          long fileLength = 0;
          if (i == numFiles - 1) {
            fileLength = (int) fm.getLength() - offset;
          }
          else {
            int currentPos = (int) fm.getOffset();
            // 4 - filesize - dataoffset
            int nextDataOffset = fm.readInt();
            while (nextDataOffset == 0) {
              nextDataOffset = fm.readInt();
            }
            fileLength = nextDataOffset - offset;
            //FieldValidator.checkLength(fileLength,arcSize);
            fm.seek(currentPos);
          }

          if (fileLength > 0 && fileLength < fm.getLength()) {

            String filename = Resource.generateFilename(i + 1);

            //path,id,name,offset,length,decompLength,exporter
            resources[realNumFiles] = new Resource(path, filename, offset, fileLength);

            TaskProgressManager.setValue(readLength);
            readLength += fileLength;
            realNumFiles++;
          }
        }

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
  @Override
  public void write(Resource[] resources, File path) {
    try {

      int numFiles = resources.length;
      TaskProgressManager.setMaximum(numFiles);

      FileManipulator fm = new FileManipulator(path, true);
      FileManipulator src = new FileManipulator(new File(Settings.getString("CurrentArchive")), false);

      // 4 - numFiles
      // 2 - Unknown (null)
      fm.writeBytes(src.readBytes(6));

      src.close();

      TaskProgressManager.setMessage(Language.get("Progress_WritingDirectory"));
      long offset = 6 + (4 * numFiles);
      for (int i = 0; i < numFiles; i++) {
        Resource fd = resources[i];
        long length = fd.getDecompressedLength();

        // 4 - Data Offset
        fm.writeInt((int) offset);

        offset += length;
      }

      TaskProgressManager.setMessage(Language.get("Progress_WritingFiles"));
      write(resources, fm);

      fm.close();

    }
    catch (Throwable t) {
      logError(t);
    }
  }

}