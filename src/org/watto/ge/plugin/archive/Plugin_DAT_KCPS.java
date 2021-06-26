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
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.io.FileManipulator;
import org.watto.io.FilenameSplitter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_DAT_KCPS extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_DAT_KCPS() {

    super("DAT_KCPS", "DAT_KCPS");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Duke Nukem Forever");
    setExtensions("dat"); // MUST BE LOWER CASE
    setPlatforms("PC");

    //setFileTypes(new FileType("txt", "Text Document", FileType.TYPE_DOCUMENT),
    //             new FileType("bmp", "Bitmap Image", FileType.TYPE_IMAGE)
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

      if (FilenameSplitter.getFilenameAndExtension(fm.getFile()).equals("Sounds.dat")) {
        rating += 25;
      }
      else {
        rating = 0;
      }

      File dirFile = new File(fm.getFile().getAbsoluteFile().getParent() + File.separatorChar + "SoundDir.dat");
      if (dirFile.exists()) {
        rating += 24;
      }
      else {
        rating = 0;
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

      //ExporterPlugin exporter = Exporter_ZLib.getInstance();

      // RESETTING GLOBAL VARIABLES

      long arcSize = (int) path.length();

      File sourcePath = new File(path.getAbsoluteFile().getParent() + File.separatorChar + "SoundDir.dat");
      FileManipulator fm = new FileManipulator(sourcePath, false);

      // 4 - Header (KCPS)
      // 4 - Version (0)
      fm.skip(8);

      // 1-5 - Number of Files
      int numFiles = (int) PluginGroup_U.readIndex(fm);
      FieldValidator.checkNumFiles(numFiles);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      int[][] nameIDs = new int[numFiles][5];
      for (int i = 0; i < numFiles; i++) {

        // 1-5 - Unknown
        PluginGroup_U.readIndex(fm);
        // 1-5 - Unknown
        PluginGroup_U.readIndex(fm);
        // 1-5 - Unknown
        PluginGroup_U.readIndex(fm);
        // 1-5 - Unknown
        PluginGroup_U.readIndex(fm);

        // 1-5 - Filename Index 1
        int nameIndex1 = (int) PluginGroup_U.readIndex(fm);

        // 1-5 - Filename Index 2
        int nameIndex2 = (int) PluginGroup_U.readIndex(fm);

        // 1-5 - Filename Index 3
        int nameIndex3 = (int) PluginGroup_U.readIndex(fm);

        // 1-5 - Filename Index 4
        int nameIndex4 = (int) PluginGroup_U.readIndex(fm);

        // 1-5 - Filename Index 5
        int nameIndex5 = (int) PluginGroup_U.readIndex(fm);

        nameIDs[i] = new int[] { nameIndex1, nameIndex2, nameIndex3, nameIndex4, nameIndex5 };

        // 4 - Unknown
        // 4 - Unknown
        fm.skip(8);

        // 1-5 - Unknown
        PluginGroup_U.readIndex(fm);

        // 4 - File Offset
        long offset = fm.readInt();
        //System.out.println(fm.getOffset() + "\t" + offset);
        FieldValidator.checkOffset(offset, arcSize);

        // 1-5 - Unknown
        PluginGroup_U.readIndex(fm);
        // 1-5 - Unknown
        PluginGroup_U.readIndex(fm);
        // 1-5 - Unknown
        PluginGroup_U.readIndex(fm);
        // 1-5 - Unknown
        PluginGroup_U.readIndex(fm);
        // 1-5 - Unknown
        PluginGroup_U.readIndex(fm);
        // 1-5 - Unknown
        PluginGroup_U.readIndex(fm);
        // 1-5 - Unknown
        PluginGroup_U.readIndex(fm);
        // 1-5 - Unknown
        PluginGroup_U.readIndex(fm);
        // 1-5 - Unknown
        PluginGroup_U.readIndex(fm);
        // 1-5 - Unknown
        PluginGroup_U.readIndex(fm);
        // 1-5 - Unknown
        PluginGroup_U.readIndex(fm);
        // 1-5 - Unknown
        PluginGroup_U.readIndex(fm);
        // 1-5 - Unknown
        PluginGroup_U.readIndex(fm);

        //path,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, "", offset);

        TaskProgressManager.setValue(i);
      }

      System.out.println(fm.getOffset());

      // 1-5 - Number of Names
      //int numNames = (int) PluginGroup_U.readIndex(fm);
      //FieldValidator.checkNumFiles(numNames);
      //int numNames = ShortConverter.unsign(fm.readShort());
      int numNames = fm.readInt();
      FieldValidator.checkNumFiles(numNames);

      String[] names = new String[numNames];
      for (int i = 0; i < numNames; i++) {
        // 1-5 - Filename Length (including null terminator)
        int nameLength = (int) PluginGroup_U.readIndex(fm);

        // X - Filename
        // 1 - null Filename Terminator
        String name = fm.readNullString(nameLength);
        names[i] = name;
      }

      // Set the filenames
      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];

        String filename = null;
        for (int j = 0; j < 5; j++) {
          int nameID = nameIDs[i][j];
          if (nameID != 0) {
            FieldValidator.checkRange(nameID, 0, numNames);
            String namePiece = names[nameID];
            if (namePiece.equals("")) {
              continue;
            }
            if (filename == null) {
              filename = namePiece;
            }
            else {
              filename += "\\" + namePiece;
            }
          }
        }

        if (filename != null) {
          filename += ".mp3";
          resource.setName(filename);
          resource.setOriginalName(filename);
          resource.forceNotAdded(true);
        }
      }

      calculateFileSizes(resources, arcSize);

      fm.close();

      return resources;

    }
    catch (Throwable t) {
      logError(t);
      return null;
    }
  }

}
