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
public class Plugin_DAT_35 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_DAT_35() {

    super("DAT_35", "DAT_35");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("LEGO Star Wars 2: The Original Trilogy",
        "The Chronicles of Narnia: The Lion, The Witch and The Wardrobe");
    setExtensions("dat");
    setPlatforms("PC");

    // MUST BE LOWER CASE !!!
    setFileTypes(new FileType("ai2", "AI Level Path", FileType.TYPE_OTHER),
        new FileType("an3", "Animation", FileType.TYPE_OTHER),
        new FileType("ani", "Animation", FileType.TYPE_OTHER),
        new FileType("anm", "Animation", FileType.TYPE_OTHER),
        new FileType("cd", "Character Definition", FileType.TYPE_OTHER),
        new FileType("cft", "Font", FileType.TYPE_OTHER),
        new FileType("cu2", "Cutscene", FileType.TYPE_VIDEO),
        new FileType("fnt", "Font", FileType.TYPE_OTHER),
        new FileType("ft2", "Font", FileType.TYPE_OTHER),
        new FileType("git", "Git Options", FileType.TYPE_DOCUMENT),
        new FileType("giz", "Giz Obstacle", FileType.TYPE_OTHER),
        new FileType("gsc", "GSC Image Archive", FileType.TYPE_ARCHIVE),
        new FileType("par", "Part", FileType.TYPE_OTHER),
        new FileType("scp", "Script File", FileType.TYPE_DOCUMENT),
        new FileType("sf", "Script File", FileType.TYPE_DOCUMENT),
        new FileType("spl", "Spline", FileType.TYPE_OTHER),
        new FileType("sub", "Subtitles", FileType.TYPE_DOCUMENT),
        new FileType("subopt", "Subtitles", FileType.TYPE_DOCUMENT),
        new FileType("txm", "Minikit", FileType.TYPE_DOCUMENT),
        new FileType("txc", "Collectable", FileType.TYPE_DOCUMENT),
        new FileType("tex", "Texture Image", FileType.TYPE_IMAGE),
        new FileType("fmv", "FMV Video", FileType.TYPE_VIDEO));

    setTextPreviewExtensions("ats", "git", "h", "scp"); // LOWER CASE

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

      long arcSize = fm.getLength();

      // Directory Offset
      if (FieldValidator.checkOffset(fm.readInt(), arcSize)) {
        rating += 5;
      }

      // Directory Length
      if (FieldValidator.checkLength(fm.readInt(), arcSize)) {
        rating += 5;
      }

      // null
      if (fm.readLong() == 0) {
        rating += 5;
      }

      // null
      if (fm.readLong() == 0) {
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

      // RESETTING THE GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = (int) fm.getLength();

      // 4 - Directory Offset
      long dirOffset = fm.readInt();
      FieldValidator.checkOffset(dirOffset, arcSize);

      // 4 - Directory Length
      // 2040 - null Padding to a multiple of 2048 bytes

      fm.seek(dirOffset + 4);

      // 4 - Number Of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      Resource[] resources = new Resource[numFiles];

      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {
        // 4 - File Offset
        long offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - File Length
        long length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - File Length
        long length2 = fm.readInt();
        if (length != length2) {
          //System.out.println(length + " - " + length2);
        }

        // 4 - null
        fm.skip(4);

        String filename = Resource.generateFilename(i);

        //path,id,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, filename, offset, length);

        TaskProgressManager.setValue(i);
      }

      // 4 - Number Of Files In The Filename Directories
      int numNames = fm.readInt();
      FieldValidator.checkNumFiles(numNames);

      // 4 - Number Of Files In The Filename Directories [+2]
      fm.skip(4);

      //fm.skip(numNames*8);

      boolean[] dirEntry = new boolean[numNames];
      for (int i = 0; i < numNames; i++) {
        // 4 - Filename Offset (relative to the start of the filename directory)
        // 2 - Unknown (if directory, this is the number of files in this directory?)
        fm.skip(6);

        // 2 - Directory/File Indicator (0=directory, #=file)
        int entry = fm.readShort();
        if (entry == 0) {
          dirEntry[i] = true;
        }
        else {
          dirEntry[i] = false;
        }
      }

      dirEntry[numNames - 1] = false;

      String dirName = "";
      int fileNum = 0;
      for (int i = 0; i < numNames; i++) {
        // X - Filename (null)
        String filename = fm.readNullString();

        if (dirEntry[i]) {
          dirName = filename + "\\";
        }
        else {
          String name = dirName + filename;
          Resource resource = resources[fileNum];
          resource.setName(name);
          resource.setOriginalName(name);
          fileNum++;
        }

        //if (filename != null && filename.indexOf(".") == -1){
        //  System.out.println("----------" + filename);
        //  }
        //else {
        //  System.out.println(filename);
        //  }
      }

      //resources[fileNum].setName(dirName + fm.readNullString());

      fm.close();

      return resources;

    }
    catch (Throwable t) {
      logError(t);
      return null;
    }
  }

}
