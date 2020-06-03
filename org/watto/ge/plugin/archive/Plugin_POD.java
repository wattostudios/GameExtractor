
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
import org.watto.io.FileManipulator;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_POD extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_POD() {

    super("POD", "POD");

    //         read write replace rename
    setProperties(true, true, true, true);

    setExtensions("pod");
    setGames("4x4 Evolution",
        "4x4 Evolution 2",
        "Terminal Velocity",
        "CART Precision Racing");
    setPlatforms("PC");

    setFileTypes("lst", "File List",
        "opa", "Encrypted Image?",
        "act", "Image Mapping?",
        "smf", "3D Model",
        "rpl", "Replay File",
        "tex", "Texture Listing",
        "clr", "Clear Image?",
        "rtd", "Unknown File",
        "sdw", "Unknown File",
        "lvl", "Level Descriptor",
        "sit", "Race Settings",
        "trk", "Truck Information",
        "bin", "Binary Data",
        "lte", "Unknown File",
        "map", "Map File",
        "pbm", "PBM Image",
        "sfx", "Sound Settings",
        "loc", "Text Description",
        "vox", "Configuration List",
        "rsp", "File List");

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

      addFileTypes();

      FileManipulator fm = new FileManipulator(path, false);

      // 4 - Number Of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      long arcSize = fm.getLength();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // 80 - Archive Name
      fm.skip(80);

      for (int i = 0; i < numFiles; i++) {
        // 32 - Filename
        String filename = fm.readNullString(32);
        FieldValidator.checkFilename(filename);

        // 4 - File Length
        long length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - Data Offset
        long offset = fm.readInt();
        if (offset == arcSize) {
          resources = resizeResources(resources, i);
          i = numFiles;
        }
        else {
          FieldValidator.checkOffset(offset, arcSize);

          //path,id,name,offset,length,decompLength,exporter
          resources[i] = new Resource(path, filename, offset, length);

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

      // 4 - Number Of Files
      fm.writeInt((int) numFiles);

      // 80 - Comment String
      fm.writeString("Archive made by Game Extractor - http://www.watto.org/extract (c) WATTO Studios");
      fm.writeByte(0);

      TaskProgressManager.setMessage(Language.get("Progress_WritingDirectory"));
      int currentPos = 84 + (numFiles * 40);
      for (int i = 0; i < numFiles; i++) {
        String name = resources[i].getName();
        long length = resources[i].getDecompressedLength();

        //FileName-32
        fm.writeNullString(name, 32);

        //Length-4
        fm.writeInt((int) length);

        //Data Offset-4
        fm.writeInt((int) currentPos);

        currentPos += length;
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