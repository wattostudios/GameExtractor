
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
public class Plugin_BFL_CMPR extends ArchivePlugin {

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Plugin_BFL_CMPR() {

    super("BFL_CMPR", "BFL_CMPR");

    //         read write replace rename
    setProperties(true, true, true, true);

    setExtensions("bfl");
    setGames("Colin McRae Rally 2");
    setPlatforms("PC");

    setFileTypes("dds", "DDS Image",
        "pcf", "PCF Image",
        "hor", "Time Settings",
        "bsp", "Unknown File",
        "ai0", "AI Settings",
        "ai1", "AI Settings",
        "ai2", "AI Settings",
        "tm0", "Unknown File",
        "tm1", "Unknown File",
        "tm2", "Unknown File",
        "grp", "Track Groups",
        "xhi", "Unknown File",
        "tre", "Landscape Map",
        "cfl", "Unknown File",
        "tsc", "Unknown File",
        "cod", "Unknown File",
        "cat", "Unknown File",
        "csp", "Unknown File",
        "gro", "Landscape File",
        "sky", "Sky Data",
        "sht", "Landscape File",
        "obj", "3D Landscape Model",
        "dat", "Track Data",
        "srf", "Unknown File",
        "hpc", "Unknown File",
        "c3d", "3D Landscape Model",
        "dmd", "Unknown File");

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
      if (fm.readString(4).equals("CMPR")) {
        rating += 50;
      }

      // Archive Size
      if (fm.readInt() + 8 == fm.getLength()) {
        rating += 5;
      }

      long arcSize = fm.getLength();
      if (arcSize - 4 < 0) {
        return 0;
      }
      fm.seek(arcSize - 4);

      // Directory Offset
      if (FieldValidator.checkOffset(fm.readInt(), arcSize)) {
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
      // 4 - Archive Length (+8 header)
      // X - Data

      fm.seek((int) path.length() - 4);

      // dirOffset
      long dirOffset = fm.readInt();
      fm.seek(dirOffset + 8);

      int numFiles = (int) ((path.length() - dirOffset) / 16);

      long arcSize = fm.getLength();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      int i = 0;
      int readLength = 0;
      while (fm.getOffset() < path.length() - 4) {
        // 4 - File Length
        long length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - Data Offset
        long offset = fm.readInt() + 8;
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - Filename Length
        int filenameLength = fm.readInt();
        FieldValidator.checkFilenameLength(filenameLength);

        // X - Filename
        String filename = fm.readString(filenameLength);
        FieldValidator.checkFilename(filename);

        // 0-3 - Filename Length Filler (fill to a multiple of 4)
        //double temp = ((double)filenameLength) / 4;
        int fillerLength = ((filenameLength / 4 + 1) * 4) - filenameLength;
        if (fillerLength == 4) {
          fillerLength = 0;
        }
        fm.skip(fillerLength);

        //path,id,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, filename, offset, length);

        TaskProgressManager.setValue(readLength);
        readLength += length;
        i++;
      }

      resources = resizeResources(resources, i);

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

      int numFiles = resources.length;
      TaskProgressManager.setMaximum(numFiles);

      FileManipulator fm = new FileManipulator(path, true);

      // 4 - Header
      fm.writeString("CMPR");

      //Loop 1 to calculate sizes
      TaskProgressManager.setMessage(Language.get("Progress_PerformingCalculations"));
      int totalLengthOfData = 0;
      int totalLengthOfHeader = 8;
      int totalLengthOfDirectory = 0;
      for (int i = 0; i < numFiles; i++) {
        totalLengthOfData += resources[i].getDecompressedLength();
        int nameLength = resources[i].getName().length();
        int fillerLength = ((nameLength / 4 + 1) * 4) - nameLength;
        if (fillerLength == 4) {
          fillerLength = 0;
        }
        totalLengthOfDirectory += 12 + nameLength + fillerLength;
      }

      // 4 - Archive Length (-8 header)
      fm.writeInt(totalLengthOfData + totalLengthOfDirectory);

      // Loop 2 - Build archive
      TaskProgressManager.setMessage(Language.get("Progress_WritingFiles"));
      write(resources, fm);

      // Loop 3 - Build directory
      TaskProgressManager.setMessage(Language.get("Progress_WritingDirectory"));
      int currentPos = 0;
      for (int i = 0; i < numFiles; i++) {
        String name = resources[i].getName();
        long length = resources[i].getDecompressedLength();

        // 4 - File Length
        fm.writeInt((int) length);

        // 4 - Data Offset
        fm.writeInt(currentPos);

        // 4 - Filename Length
        fm.writeInt(name.length());

        // X - Filename
        fm.writeString(name);

        // 1-3 - Filename Length Filler (fill to a multiple of 4)
        int fillerLength = ((name.length() / 4 + 1) * 4) - name.length();
        if (fillerLength == 4) {
          fillerLength = 0;
        }
        for (int j = 0; j < fillerLength; j++) {
          fm.writeByte(0);
        }

        currentPos += length;
      }

      // 4 - Directory Offset
      fm.writeInt(totalLengthOfData);

      fm.close();

    }
    catch (Throwable t) {
      logError(t);
    }
  }

}