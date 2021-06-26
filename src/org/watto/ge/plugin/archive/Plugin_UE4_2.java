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
import org.watto.ge.helper.UE4Helper_2;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.archive.datatype.UnrealImportEntry;
import org.watto.ge.plugin.resource.Resource_Unreal;
import org.watto.io.FileManipulator;
import org.watto.io.converter.IntConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_UE4_2 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_UE4_2() {

    super("UE4_2", "Unreal Engine 4 Archive - Version 2");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("ARK: Survival Evolved");
    setExtensions("uasset", "umap"); // MUST BE LOWER CASE
    setPlatforms("PC");

    setFileTypes(
        new FileType("texture2d", "Texture Image", FileType.TYPE_IMAGE));

  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public int getMatchRating(FileManipulator fm) {
    // Only allows UE4 archives with Version = 2;
    return UE4Helper_2.getMatchRating(fm, 2);
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
      // 4 - Version (2) (XOR with 255)
      // 4 - Unknown
      // 4 - Unknown
      // 4 - Unknown
      fm.skip(20);

      // 4 - Number of Header Blocks
      int numHeaderBlocks = fm.readInt();

      if (numHeaderBlocks != 0) {
        FieldValidator.checkNumFiles(numHeaderBlocks);

        // for each Header Block
        for (int i = 0; i < numHeaderBlocks; i++) {
          //   16 - GUID
          //   4 - Unknown
          fm.skip(20);

          //   4 - Header Name Length (including null terminator)
          int headerNameLength = fm.readInt();
          FieldValidator.checkFilenameLength(headerNameLength);

          //   X - Header Name
          //   1 - null Header Name Terminator
          fm.skip(headerNameLength);
        }
      }

      // 4 - File Directory Offset?
      // 4 - Package Name Length (including null terminator) (5)
      // 4 - Package Name (None)
      // 1 - null Package Name Terminator
      // 4 - Unknown
      fm.skip(17);

      // 4 - Number of Names
      int nameCount = fm.readInt();
      if (arcSize > 500000000) {
        FieldValidator.checkNumFiles(nameCount / 8);
      }
      else {
        FieldValidator.checkNumFiles(nameCount);
      }

      // 4 - Name Directory Offset
      long nameDirOffset = IntConverter.unsign(fm.readInt());
      FieldValidator.checkOffset(nameDirOffset, arcSize);

      // 4 - Number Of Exports
      int numFiles = fm.readInt();
      if (arcSize > 500000000) {
        FieldValidator.checkNumFiles(numFiles / 8);
      }
      else {
        FieldValidator.checkNumFiles(numFiles);
      }

      // 4 - Exports Directory Offset
      long dirOffset = fm.readInt();
      FieldValidator.checkOffset(dirOffset, arcSize);

      // 4 - Number Of Imports
      int importCount = fm.readInt();
      if (arcSize > 500000000) {
        FieldValidator.checkNumFiles(importCount / 8);
      }
      else {
        FieldValidator.checkNumFiles(importCount);
      }

      // 4 - Import Directory Offset
      long importDirOffset = IntConverter.unsign(fm.readInt());
      FieldValidator.checkOffset(importDirOffset, arcSize);

      // 8 - Unknown
      fm.skip(8);

      // 8 - Files Data Offset
      long filesDirOffset = fm.readLong();
      FieldValidator.checkOffset(filesDirOffset, arcSize);

      // 16 - GUID Hash
      // 4 - Unknown (1)
      // 4 - Number of Exports (again)
      // 4 - Number of Names (again)
      // 22 - null
      // 4 - Unknown
      // 4 - Unknown
      // 12 - null
      // 4 - Files Data Offset
      // 8 - Unknown
      // 8 - Unknown

      // Read the Names Directory
      fm.seek(nameDirOffset);
      UE4Helper_2.readNamesDirectory(fm, nameCount);
      String[] names = UE4Helper_2.getNames();

      // Read the Import Directory
      fm.seek(importDirOffset);
      UnrealImportEntry[] imports = UE4Helper_2.readImportDirectory(fm, importCount);

      // read the files directory
      fm.seek(dirOffset);

      Resource_Unreal[] resources = new Resource_Unreal[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      int[] parentNameIDs = new int[numFiles];
      String[] parentNames = new String[numFiles];
      for (int i = 0; i < numFiles; i++) {
        //System.out.println(fm.getOffset());

        // 4 - Type ID (XOR with 255)
        int typeID = fm.readInt();
        if (typeID < 0) {
          typeID = (0 - typeID) - 1;
        }

        String type;
        if (typeID < importCount) {
          type = imports[typeID].getName();
        }
        else {
          type = "Unknown";
        }
        //FieldValidator.checkRange(typeID, 0, numTypes);

        // 4 - null
        fm.skip(4);

        // 4 - Parent Name ID
        int parentNameID = fm.readInt();
        FieldValidator.checkRange(parentNameID, 0, numFiles);
        parentNameIDs[i] = parentNameID;

        // 4 - File Name ID
        int fileNameID = fm.readInt();
        FieldValidator.checkRange(fileNameID, 0, nameCount);

        // 4 - Distinct ID
        int distinctID = fm.readInt();

        String distinctName = "";
        if (distinctID != 0) {
          distinctName = "(" + distinctID + ")";
        }

        String filename = names[fileNameID] + distinctName + "." + type;

        parentNames[i] = names[fileNameID]; // for calculating the parent directories in the loop later on

        // 4 - Flags
        fm.skip(4);

        // 4 - File Length
        long length = IntConverter.unsign(fm.readInt());
        FieldValidator.checkLength(length, arcSize);

        // 4 - File Offset
        long offset = IntConverter.unsign(fm.readInt());
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - Unknown (0/1)
        fm.skip(4);

        // 4 - Extra Field?
        if (fm.readInt() == 1) {
          // Yep, and extra field
          fm.skip(4);
        }

        // 4 - Unknown
        // 16 - GUID Hash or NULL
        // 4 - Unknown
        // 4 - Unknown (1)
        fm.skip(28);

        //path,id,name,offset,length,decompLength,exporter
        resources[i] = new Resource_Unreal(path, filename, offset, length);

        /*
        if (type.equals("SoundWave")) {
          resources[i].setExporter(Exporter_Custom_UE4_SoundWave_Generic.getInstance());
        }
        */

        TaskProgressManager.setValue(i);
      }

      // Now that we have all the files, go through and set the parent names

      for (int i = 0; i < numFiles; i++) {
        int parentNameID = parentNameIDs[i];
        String parentName = "";

        while (parentNameID != 0 && parentNameID < numFiles) {
          String namePartToAdd = parentNames[parentNameID] + "\\";
          if (parentName.indexOf(namePartToAdd) >= 0) {
            break; // we're looping over and over the parents
          }
          parentName = namePartToAdd + parentName;
          int nextNameID = parentNameIDs[parentNameID];
          if (nextNameID == parentNameID) {
            parentNameID = 0;
          }
          else {
            parentNameID = nextNameID;
          }
        }

        Resource resource = resources[i];

        String filename = parentName + resource.getName();
        resource.setName(filename);
        resource.setOriginalName(filename); // so the archive doesn't think the name has been modified
        resource.forceNotAdded(true); // because we're setting the resource against the decompressed file, not the compressed one.
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
