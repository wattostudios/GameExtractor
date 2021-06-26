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
import org.watto.ge.helper.UE4Helper;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.archive.datatype.UnrealImportEntry;
import org.watto.ge.plugin.resource.Resource_PAK_38;
import org.watto.io.FileManipulator;
import org.watto.io.converter.IntConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_UE4_6 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_UE4_6() {

    super("UE4_6", "Unreal Engine 4 Archive - Version 6");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("BARBAR_BAR",
        "Conarium",
        "Hello Neighbor");
    setExtensions("uasset"); // MUST BE LOWER CASE
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
    // Only allows UE4 archives with Version = 6;
    return UE4Helper.getMatchRating(fm, 6);
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

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 4 - Unreal Header (193,131,42,158)
      // 4 - Version (6) (XOR with 255)
      // 16 - null
      // 4 - File Directory Offset?
      // 4 - Unknown (5)
      // 4 - Package Name (None)
      // 4 - null
      // 1 - Unknown (128)
      fm.skip(41);

      // 4 - Number of Names
      int nameCount = fm.readInt();
      FieldValidator.checkNumFiles(nameCount);

      // 4 - Name Directory Offset
      long nameDirOffset = IntConverter.unsign(fm.readInt());
      FieldValidator.checkOffset(nameDirOffset, arcSize);

      // 8 - null
      // 4 - Number Of Exports
      // 4 - Exports Directory Offset
      fm.skip(16);

      // 4 - Number Of Imports
      int importCount = fm.readInt();
      FieldValidator.checkNumFiles(importCount);

      // 4 - Import Directory Offset
      long importDirOffset = IntConverter.unsign(fm.readInt());
      FieldValidator.checkOffset(importDirOffset, arcSize);

      // 20 - null
      // 16 - GUID Hash
      // 4 - Unknown (1)
      // 4 - Unknown (1/2)
      // 4 - Unknown (Number of Names - again?)
      // 36 - null
      // 4 - Unknown
      // 4 - null
      // 4 - Padding Offset
      // 4 - File Length [+4] (not always - sometimes an unknown length/offset)
      // 12 - null
      // 4 - Unknown (-1)
      fm.skip(116);

      // 4 - Files Data Offset
      long filesDirOffset = IntConverter.unsign(fm.readInt());
      try {
        FieldValidator.checkOffset(filesDirOffset, arcSize);
      }
      catch (Throwable t) {
        // Game: The Turing Test
        fm.seek(fm.getOffset() - 28);

        filesDirOffset = IntConverter.unsign(fm.readInt());
        FieldValidator.checkOffset(filesDirOffset, arcSize);
      }

      // Read the Names Directory
      fm.seek(nameDirOffset);
      UE4Helper.readNamesDirectory(fm, nameCount);

      // Read the Import Directory
      fm.seek(importDirOffset);
      UnrealImportEntry[] imports = UE4Helper.readImportDirectory(fm, importCount);

      int numFiles = importCount;
      int realNumFiles = 0;

      Resource_PAK_38[] resources = new Resource_PAK_38[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {
        UnrealImportEntry entry = imports[i];

        // Find all the Class entries, and generate filenames for them
        String packageName = "";
        if (entry.getType().equals("Class")) {
          // found a Class

          // navigate back through the parent tree to build up the package name
          int parentID = entry.getParentID();
          while (parentID != -1) {
            UnrealImportEntry parent = imports[parentID];
            packageName += parent.getName();
            parentID = parent.getParentID();
          }

        }
        else {
          // not a Class - skip it
          continue;
        }

        // Set the name of the filename, including the "type" as the file extension, and the package name if it exists.
        // Generate the filename using "i" so that it contains the actual import ID number
        String filename = "File_" + i + "." + entry.getName();
        if (!packageName.equals("")) {
          filename = packageName + "/" + filename;
        }

        //path,name,offset,length,decompLength,exporter
        //resources[realNumFiles] = new Resource(path, filename, filesDirOffset, 0);
        resources[realNumFiles] = new Resource_PAK_38(path, filename, 0, arcSize); // need to trick the offset/length here so Viewer_UE4_Texture2D_6 will read it 
        realNumFiles++;

        TaskProgressManager.setValue(i);
      }

      Resource_PAK_38[] oldResources = resources;
      resources = new Resource_PAK_38[realNumFiles];
      System.arraycopy(oldResources, 0, resources, 0, realNumFiles);

      //resources = resizeResources(resources, realNumFiles);

      /*
      // FOR TESTING ONLY --> NOW GO THROUGH AND READ ALL THE PROPERTIES FOR THE FILE
      fm.seek(filesDirOffset);
      System.out.println("Reading properties - starting at offset " + fm.getOffset() + "...");
      UnrealProperty[] properties = UE4Helper.readProperties(fm);
      for (int i = 0; i < properties.length; i++) {
        UnrealProperty property = properties[i];
        System.out.println(property.toString());
      }
      System.out.println("Done - stopped at offset " + fm.getOffset());
      */

      fm.close();

      return resources;

    }
    catch (Throwable t) {
      logError(t);
      return null;
    }
  }

}
