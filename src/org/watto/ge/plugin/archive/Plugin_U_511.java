/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2021 wattostudios
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
import org.watto.ge.plugin.resource.Resource_Unreal;
import org.watto.io.FileManipulator;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_U_511 extends PluginGroup_U {

  String[] names;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_U_511() {
    super("U_511", "Unreal Engine 3 version 511");

    setExtensions("u", "upk", "ut3");
    setGames("Unreal Tournament 3");
    setPlatforms("PC");

  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public int getMatchRating(FileManipulator fm) {
    return super.getMatchRating(fm, 511);
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Resource[] read(File path) {
    try {

      addFileTypes();

      FileManipulator fm = new FileManipulator(path, false);
      long arcSize = fm.getLength();

      // 4 - Header
      fm.skip(4);

      // 2 - Version
      version = fm.readShort();

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
      FieldValidator.checkNumFiles(numFiles);

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
      FieldValidator.checkNumFiles(numGenerations);

      // for each generation
      // 4 - Number Of Files
      // 4 - Number Of Names
      fm.skip(numGenerations * 8);

      // 4 - Unknown
      // 4 - Unknown (2859)
      // 4 - Unknown (38)
      fm.skip(12);

      // 4 - Compression Type? (0=none/2=archives)
      int compression = fm.readInt();

      // 4 - Number Of Archives (0 if field above is 0);
      int numArchives = fm.readInt();

      if (compression == 2) {
        return readCollection(path, fm, numArchives);
      }

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
        // 4 - Type Object ID
        int typeID = fm.readInt();
        String type = "";

        if (typeID > 0) {
          typeID--;
          FieldValidator.checkLength(typeID, numNames); // check for the name
          type = names[typeID];
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

        // 4 - Object Name ID
        int nameID = fm.readInt();
        FieldValidator.checkLength(nameID, numNames); // checks the ID is within the names list

        // 4 - Property ID
        int propertyID = fm.readInt();

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

        // 28 - null
        fm.skip(28);

        // put the parent IDs before the filename, in a directory structure.
        String filename = names[nameID];
        if (parentID >= 0) {
          String parentName = parentNames[parentID];
          if (parentName != null) {
            filename = parentName + "\\" + filename;
          }
        }

        if (propertyID != 0) {
          filename += names[propertyID];
        }

        parentNames[i] = filename;

        // append the type name
        filename += "." + type;

        //System.out.println(types[nextID] + "\t\t" + filename);

        //path,id,name,offset,length,decompLength,exporter
        resources[i] = new Resource_Unreal(path, filename, offset, length);

        //if (type.equals("Texture")){
        //  resources[i].setExporter(Exporter_Custom_U_Texture_Generic.getInstance());
        //  }
        //else if (type.equals("Palette")){
        //  resources[i].setExporter(Exporter_Custom_U_Palette_Generic.getInstance());
        //  }

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
  
  **********************************************************************************************
  **/
  public Resource[] readCollection(File path, FileManipulator fm, int numFiles) {
    try {

      long arcSize = fm.getLength();

      FieldValidator.checkNumFiles(numFiles);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

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

      fm.close();

      return resources;

    }
    catch (Throwable t) {
      logError(t);
      return null;
    }
  }

}