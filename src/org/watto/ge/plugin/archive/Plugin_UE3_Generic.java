/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2025 wattostudios
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
import org.watto.ge.plugin.exporter.Exporter_Custom_UE3_SoundNodeWave_Generic;
import org.watto.ge.plugin.exporter.Exporter_Custom_UE3_SoundRiotRawAsset_Generic;
import org.watto.ge.plugin.resource.Resource_Unreal;
import org.watto.io.FileManipulator;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_UE3_Generic extends PluginGroup_UE3 {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_UE3_Generic() {
    super("UE3_Generic", "Unreal Engine 3/4 (Generic)");

    setExtensions("u", "upk", "udk");
    setGames("Frontlines: Fuel Of War",
        "Legendary",
        "RoboBlitz",
        "Stargate Worlds");
    setPlatforms("PC");

    setFileTypes(
        new FileType("soundnodewave", "Sound File", FileType.TYPE_AUDIO),
        new FileType("texture2d", "Texture Image", FileType.TYPE_IMAGE));

  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public int getMatchRating(FileManipulator fm) {

    int rating = super.getMatchRating(fm, -1);
    try {
      if (FieldValidator.checkExtension(fm, extensions)) {
        rating += 25;
      }
    }
    catch (Throwable t) {
    }

    return rating;
  }

  /**
   **********************************************************************************************
   * Basic Format
   **********************************************************************************************
   **/
  @Override
  public Resource[] readGenericUE3(File path, FileManipulator fm) {
    try {

      long arcSize = fm.getLength();

      // 4 - Header
      // 2 - Version
      // ALREADY KNOW THESE 2 FIELDS FROM read()

      // 2 - License Mode
      // 4 - First File Offset
      fm.skip(6);

      // 4 - Base Name Length (including null) (eg "bg", "none", etc)
      int baseNameLength = fm.readInt();
      FieldValidator.checkFilenameLength(baseNameLength);

      // X - Base Name
      // 1 - null Base Name Terminator
      fm.skip(baseNameLength);

      // 4 - Package Flags
      fm.skip(4);

      // 4 - Number Of Names
      int numNames = fm.readInt();
      FieldValidator.checkNumFiles(numNames);

      // 4 - Name Directory Offset
      long nameOffset = fm.readInt();
      FieldValidator.checkOffset(nameOffset, arcSize);

      // 4 - Number Of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles / 4);

      // 4 - File Directory Offset
      long dirOffset = fm.readInt();
      FieldValidator.checkOffset(dirOffset, arcSize);

      // 4 - Number Of Types
      int numTypes = fm.readInt();
      FieldValidator.checkNumFiles(numTypes);

      // 4 - Type Directory Offset
      long typesOffset = fm.readInt();
      FieldValidator.checkOffset(typesOffset, arcSize);

      // 4 - Unknown
      // 16 - GUID Hash
      fm.skip(20);

      // 4 - Generation Count
      int numGenerations = fm.readInt();
      try {
        FieldValidator.checkNumFiles(numGenerations);
      }
      catch (Throwable t) {
        // 12 - rest of the next GUID
        fm.skip(12); // already skipped 4 from above

        // 4 - Generation Count
        numGenerations = fm.readInt();
        FieldValidator.checkNumFiles(numGenerations);
      }

      // for each generation
      // 4 - Number Of Files
      // 4 - Number Of Names
      fm.skip(numGenerations * 8);

      // 4 - Unknown
      // 4 - Unknown (2859)
      // 4 - Unknown (38)
      fm.skip(12);

      // 4 - Compression Type? (0=none/2=archives)
      //int compression = fm.readInt();
      fm.skip(4);

      // 4 - Number of Compressed Archive Blocks
      int numBlocks = fm.readInt();

      if (numBlocks > 0) {
        long currentOffset = fm.getOffset();

        FileManipulator decompFM = decompressArchive(fm);
        if (decompFM != null) {
          fm.close(); // close the original archive
          fm = decompFM; // now we're going to read from the decompressed file instead
          fm.seek(currentOffset); // go to the same point in the decompressed file as in the compressed file

          numBlocks = 0;
          arcSize = fm.getLength(); // use the arcSize of the decompressed file (for checking offsets etc)
          path = fm.getFile(); // So the resources are stored against the decompressed file
        }
      }

      //if (compression == 2) {
      //  return readGenericUE3Collection(path, fm, numBlocks);
      //}

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // read the names directory
      fm.seek(nameOffset);

      names = new String[numNames];

      // Loop through directory
      for (int i = 0; i < numNames; i++) {
        // 4 - Name Length (including null)
        int nameLength = fm.readInt() - 1;
        FieldValidator.checkFilenameLength(nameLength);

        // X - Name
        names[i] = fm.readString(nameLength);

        // 1 - null Name Terminator
        // 4 - null
        // 4 - Flags
        fm.skip(9);
      }

      // read the types directory
      fm.seek(typesOffset);

      String[] types = new String[numTypes];

      // Loop through directory
      for (int i = 0; i < numTypes; i++) {
        // 8 - Package Name ID
        // 8 - Format Name ID
        // 4 - Package Object ID
        fm.skip(20);

        // 8 - Object Name ID
        int objectID = (int) fm.readLong();
        types[i] = names[objectID];
      }

      // read the files directory
      fm.seek(dirOffset);

      String[] parentNames = new String[numFiles];
      for (int i = 0; i < numFiles; i++) {
        //System.out.println(fm.getOffset());

        // 4 - Type Object ID
        int typeID = fm.readInt();
        String type = "";

        if (typeID > 0) {
          typeID--;
          try {
            FieldValidator.checkLength(typeID, numNames); // check for the name
            type = names[typeID];
          }
          catch (Throwable t) {
            type = "unknown";
          }
        }
        else if (typeID == 0) {
          type = names[0];
        }
        else {
          typeID = (0 - typeID) - 1;
          FieldValidator.checkLength(typeID, numTypes); // check for the name
          type = types[typeID];
        }

        // 4 - Parent Object ID
        fm.skip(4);

        // 4 - Package Object ID [-1]
        int parentID = fm.readInt();
        if (parentID > 0) {
          parentID--;
          FieldValidator.checkLength(parentID, numFiles);
        }
        else if (parentID == 0) {
          //parentID = -1; // don't want to look this entry up in the names table
        }

        // 8 - Object Name ID
        int nameID = (int) fm.readLong();
        FieldValidator.checkLength(nameID, numNames); // checks the ID is within the names list

        // 8 - null
        // 4 - Flags
        fm.skip(12);

        // 4 - File Length
        long length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - File Offset
        long offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - Count
        int count = fm.readInt();
        // for each count
        // 4 - Unknown
        // 4 - Unknown
        // 4 - Unknown
        fm.skip(count * 12);

        // 3.15 UPDATED THE BELOW BASED ON Medal of Honor: Airbourne
        // 24 - null
        fm.skip(4);
        if (fm.readInt() == 1) {
          fm.skip(20); // 28 total
        }
        else {
          fm.skip(16); // 24 total
        }

        // put the parent IDs before the filename, in a directory structure.
        String filename = names[nameID];
        if (parentID >= 0) {
          String parentName = parentNames[parentID];
          if (parentName != null) {
            filename = parentName + "\\" + filename;
          }
        }
        parentNames[i] = filename;

        // append the type name
        filename += "." + type;

        //path,id,name,offset,length,decompLength,exporter
        resources[i] = new Resource_Unreal(path, filename, offset, length);
        resources[i].forceNotAdded(true);

        if (type.equals("SoundNodeWave")) {
          resources[i].setExporter(Exporter_Custom_UE3_SoundNodeWave_Generic.getInstance());
        }
        else if (type.equals("SoundRiotRawAsset")) {
          resources[i].setExporter(Exporter_Custom_UE3_SoundRiotRawAsset_Generic.getInstance());
        }

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

  /**
   **********************************************************************************************
   * Basic Format
   **********************************************************************************************
   **/
  public Resource[] readGenericUE3Collection(File path, FileManipulator fm, int numFiles) {
    try {

      long arcSize = fm.getLength();

      FieldValidator.checkNumFiles(numFiles);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      long startPos = fm.getOffset();
      try {
        // 16 byte entries
        for (int i = 0; i < numFiles; i++) {

          // 4 - Unknown
          fm.skip(4);

          // 4 - Decompressed Length?
          int decompLength = fm.readInt();
          FieldValidator.checkLength(decompLength);

          // 4 - Archive Offset
          int offset = fm.readInt();
          FieldValidator.checkOffset(offset, arcSize);

          // 4 - Compressed Length
          int length = fm.readInt();
          FieldValidator.checkLength(length, arcSize);

          String filename = Resource.generateFilename(i) + ".u";

          //path,id,name,offset,length,decompLength,exporter
          resources[i] = new Resource_Unreal(path, filename, offset, length, decompLength);

          TaskProgressManager.setValue(i);
        }
      }
      catch (Throwable t) {
        // Try 20-byte entries instead
        fm.relativeSeek(startPos);

        for (int i = 0; i < numFiles; i++) {

          // 4 - Unknown
          fm.skip(4);

          // 4 - Decompressed Length?
          int decompLength = fm.readInt();
          FieldValidator.checkLength(decompLength);

          // 4 - Archive Offset
          int offset = fm.readInt();
          FieldValidator.checkOffset(offset, arcSize);

          // 4 - Compressed Length
          int length = fm.readInt();
          FieldValidator.checkLength(length, arcSize);

          // 4 - Unknown (1)
          fm.skip(4);

          String filename = Resource.generateFilename(i) + ".u";

          //path,id,name,offset,length,decompLength,exporter
          resources[i] = new Resource_Unreal(path, filename, offset, length, decompLength);

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

}