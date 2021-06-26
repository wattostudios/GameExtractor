
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
public class Plugin_IXF extends ArchivePlugin {

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Plugin_IXF() {

    super("IXF", "IXF");

    //         read write replace rename
    setProperties(true, false, true, false);

    setExtensions("ixf", "dat");
    setGames("Simcity 3000");
    setPlatforms("PC");

    setFileTypes("fzi", "Far Zoom Image",
        "czi", "Close Zoom Image");

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
      if (fm.readString(4).equals(new String(new byte[] { (byte) 215, (byte) 129, (byte) 195, (byte) 128 }))) {
        rating += 50;
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

      long arcSize = (int) path.length();

      FileManipulator fm = new FileManipulator(path, false);

      // Number Of Files (guessed)
      fm.skip(16);
      int numFiles = ((fm.readInt() - 4) / 20);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      fm.seek(0);

      int realNumFiles = 0;

      // 4 - Header
      fm.seek(4);

      // Blocks of 20 - go until all 20 bytes in a row are null

      int readLength = 0;
      for (int i = 0; i < numFiles; i++) {
        // 4 - Type ID?
        int typeID = fm.readInt();

        // 4 - File ID?
        int fileID = fm.readInt();

        // 4 - Instance ID?
        int instanceID = fm.readInt();

        // 4 - Data Offset
        long offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - File Length
        long length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        String filename = Resource.generateFilename(i);

        if (instanceID == 0) {
          filename += ".fzi";
        }
        else if (instanceID == 1) {
          filename += ".czi";
        }

        //path,id,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, filename, offset, length);

        TaskProgressManager.setValue(readLength);
        readLength += length;
        realNumFiles++;

        if (typeID == 0 && fileID == 0 && instanceID == 0 && offset == 0 && length == 0) {
          i = numFiles;
        }
      }

      realNumFiles--;
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

      // 4 - Header
      fm.writeBytes(src.readBytes(4));

      TaskProgressManager.setMessage(Language.get("Progress_WritingDirectory"));
      long offset = 4 + (numFiles * 20);
      for (int i = 0; i < numFiles; i++) {
        long length = resources[i].getDecompressedLength();

        // 4 - Type ID?
        // 4 - File ID?
        // 4 - Instance ID?
        fm.writeBytes(src.readBytes(12));

        // 4 - Data Offset
        // 4 - File Length
        src.skip(8);
        fm.writeInt((int) offset);
        fm.writeInt((int) length);

        offset += length;
      }

      TaskProgressManager.setMessage(Language.get("Progress_WritingFiles"));
      write(resources, fm);

      src.close();

      fm.close();

    }
    catch (Throwable t) {
      logError(t);
    }
  }

}