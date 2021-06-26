/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2020 wattostudios
 *
 * License Information:
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License
 * published by the Free Software Foundation; either version 2 of the License, or (at your option) any later versions. This
 * program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranties
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License at http://www.gnu.org for more
 * details. For further information on this application, refer to the authors' website.
 */

package org.watto.ge.plugin.archive;

import java.io.File;
import org.watto.Language;
import org.watto.datatype.FileType;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.io.FileManipulator;
import org.watto.task.TaskProgressManager;

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
        "CART Precision Racing",
        "Monster Truck Madness 2");
    setPlatforms("PC");

    // MUST BE LOWER CASE !!!
    setFileTypes(new FileType("lst", "File List", FileType.TYPE_OTHER),
        new FileType("opa", "Encrypted Image?", FileType.TYPE_OTHER),
        new FileType("act", "Image Mapping?", FileType.TYPE_OTHER),
        new FileType("smf", "3D Model", FileType.TYPE_OTHER),
        new FileType("rpl", "Replay File", FileType.TYPE_OTHER),
        new FileType("tex", "Texture Listing", FileType.TYPE_OTHER),
        new FileType("clr", "Clear Image?", FileType.TYPE_OTHER),
        new FileType("rtd", "Unknown File", FileType.TYPE_OTHER),
        new FileType("sdw", "Unknown File", FileType.TYPE_OTHER),
        new FileType("lvl", "Level Descriptor", FileType.TYPE_OTHER),
        new FileType("sit", "Race Settings", FileType.TYPE_OTHER),
        new FileType("trk", "Truck Information", FileType.TYPE_OTHER),
        new FileType("bin", "3D Model", FileType.TYPE_MODEL),
        new FileType("lte", "Unknown File", FileType.TYPE_OTHER),
        new FileType("map", "Map File", FileType.TYPE_OTHER),
        new FileType("pbm", "PBM Image", FileType.TYPE_OTHER),
        new FileType("sfx", "Sound Settings", FileType.TYPE_OTHER),
        new FileType("loc", "Text Description", FileType.TYPE_OTHER),
        new FileType("vox", "Configuration List", FileType.TYPE_OTHER),
        new FileType("rsp", "File List", FileType.TYPE_OTHER));

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