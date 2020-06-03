
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
import org.watto.io.FileManipulator;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_AR_ARES extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_AR_ARES() {

    super("AR_ARES", "AR_ARES");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Midtown Madness");
    setExtensions("ar");
    setPlatforms("PC");

    setFileTypes("msh", "Object Mesh",
        "bnd", "Object Bindings",
        "dds", "DirectX Image",
        "pld", "PLD File",
        "set", "Settings",
        "vtx", "Person Vertex",
        "bne", "Person Bones",
        "mms", "Midtown Madness Settings",
        "car", "Car Settings",
        "vid", "Video Position",
        "mnu", "Menu Settings");

  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public String detectFileType(String fileStart) {
    try {

      if (fileStart.indexOf("RIFF") >= 0) {
        return ".wav";
      }
      else if (fileStart.indexOf("JFIF") >= 0) {
        return ".jpg";
      }
      else if (fileStart.indexOf("3HSM") >= 0) {
        return ".msh";
      }
      else if (fileStart.indexOf("2DNB") >= 0) {
        return ".bnd";
      }
      else if (fileStart.indexOf("DDS") >= 0) {
        return ".dds";
      }
      else if (fileStart.indexOf("DLP7") >= 0) {
        return ".pld";
      }
      else if (fileStart.indexOf("x,y,z") >= 0) {
        return ".set";
      }
      else if (fileStart.indexOf("# ") >= 0) {
        return ".set";
      }
      else if (fileStart.indexOf("version") >= 0) {
        return ".vtx";
      }
      else if (fileStart.indexOf("NumBones") >= 0) {
        return ".bne";
      }
      else if (fileStart.indexOf("mm") >= 0) {
        return ".mms";
      }
      else if (fileStart.indexOf("name") >= 0) {
        return ".car";
      }
      else if (fileStart.indexOf("TrackCam") >= 0) {
        return ".vid";
      }
      else if (fileStart.indexOf("PovCam") >= 0) {
        return ".vid";
      }
      else if (fileStart.indexOf("MENU") >= 0) {
        return ".mnu";
      }
      else if (fileStart.indexOf("BaseName") >= 0) {
        return ".set";
      }

      return ".unk";

    }
    catch (Throwable t) {
      return ".unk";
    }
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
      if (fm.readString(4).equals("ARES")) {
        rating += 50;
      }

      // Number Of Files
      int numFiles = fm.readInt();
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

      // 4 - Header (ARES)
      fm.skip(4);

      // 4 - Number Of Files
      int numFiles = fm.readInt() - 3;
      FieldValidator.checkNumFiles(numFiles);

      long arcSize = fm.getLength();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      fm.seek(52);

      long[] offsets = new long[numFiles];

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {
        // 4 Bytes - Data Offset
        long offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        offsets[i] = offset;

        // 4 Bytes - Unknown
        // 4 Bytes - Unknown
        fm.skip(8);
      }

      for (int i = 0; i < numFiles; i++) {
        long length;
        if (i < numFiles - 1) {
          length = (offsets[i + 1] - offsets[i]);
        }
        else {
          length = arcSize - offsets[i];
        }

        String filename = Resource.generateFilename(i);

        fm.seek(offsets[i]);
        String fileStart = fm.readString(16);
        filename += detectFileType(fileStart);

        //path,id,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, filename, offsets[i], length);

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

}
