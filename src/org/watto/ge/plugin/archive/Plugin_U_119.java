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
import org.watto.ge.plugin.archive.datatype.UnrealProperty;
import org.watto.ge.plugin.exporter.Exporter_Custom_U_Object_Generic;
import org.watto.ge.plugin.exporter.Exporter_Custom_U_Palette_Generic;
import org.watto.ge.plugin.exporter.Exporter_Custom_U_Sound_Generic;
import org.watto.ge.plugin.exporter.Exporter_Custom_U_Texture_Generic;
import org.watto.ge.plugin.resource.Resource_Unreal;
import org.watto.io.FileManipulator;
import org.watto.io.converter.ByteConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_U_119 extends PluginGroup_U {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_U_119() {
    super("U_119", "Unreal Engine (Version 119)");

    setExtensions("u", "uax", "ukx", "umx", "upx", "usx", "utx", "uvx");
    setGames("Pariah", "WarPath");
    setPlatforms("PC", "XBox");

  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public int getMatchRating(FileManipulator fm) {
    return super.getMatchRating(fm, 119);
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public Resource[] read(File path) {
    try {

      Exporter_Custom_U_Texture_Generic exporterTexture = Exporter_Custom_U_Texture_Generic.getInstance();
      Exporter_Custom_U_Sound_Generic exporterSound = Exporter_Custom_U_Sound_Generic.getInstance();
      Exporter_Custom_U_Palette_Generic exporterPalette = Exporter_Custom_U_Palette_Generic.getInstance();
      Exporter_Custom_U_Object_Generic exporterObject = Exporter_Custom_U_Object_Generic.getInstance();

      addFileTypes();

      FileManipulator fm = new FileManipulator(path, false);
      long arcSize = fm.getLength();

      // 4 - Header
      fm.skip(4);

      // 2 - Version
      version = fm.readShort();

      // 2 - License Mode
      // 4 - Package Flags
      fm.skip(6);

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

      // if (version < 68){
      //   4 - Number Of Generations
      //   4 - Generation Directory Offset
      //   for each generation
      //     16 - GUID Hash
      //   }
      // else {
      //   16 - GUID Hash
      //   4 - Number Of Generations
      //   for each generation
      //     4 - Number Of Names
      //     4 - Number Of Files
      //   }

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // read the names directory
      fm.seek(nameOffset);

      names = new String[numNames];

      // Loop through directory
      for (int i = 0; i < numNames; i++) {
        // 2 - Name Length (not including null)
        int nameLength = fm.readShort();

        // X - Name
        names[i] = fm.readString(nameLength);

        // 1 - null Name Terminator
        // 4 - Flags
        fm.skip(5);
      }

      // read the types directory
      fm.seek(typesOffset);

      String[] types = new String[numTypes];

      // Loop through directory
      for (int i = 0; i < numTypes; i++) {
        // 1-5 - Package Name ID
        readIndex(fm);

        // 1-5 - Format Name ID
        readIndex(fm);

        // 4 - Package Object ID
        fm.skip(4);

        // 1-5 - Object Name ID
        int objectID = (int) readIndex(fm);
        types[i] = names[objectID];
      }

      // read the files directory
      fm.seek(dirOffset);

      String[] parentNames = new String[numFiles];
      for (int i = 0; i < numFiles; i++) {
        // 1-5 - Type Object ID
        int typeID = (int) readIndex(fm);
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

        // 1-5 - Parent Object ID
        readIndex(fm);

        // 4 - Package Object ID [-1]
        int parentID = fm.readInt();
        if (parentID > 0) {
          parentID--;
          FieldValidator.checkLength(parentID, numFiles);
        }
        else if (parentID == 0) {
          //parentID = -1; // don't want to look this entry up in the names table
        }

        // 1-5 - Object Name ID
        int nameID = (int) readIndex(fm);
        FieldValidator.checkLength(nameID, numNames); // checks the ID is within the names list

        // 4 - Flags
        fm.skip(4);

        // 1-5 - File Length
        long length = readIndex(fm);
        FieldValidator.checkLength(length, arcSize);

        // 1-5 - File Offset
        long offset = 0;
        if (length > 0) {
          offset = readIndex(fm);
          FieldValidator.checkOffset(offset - 1, arcSize); // to trap offset=0
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
        Resource resource = new Resource_Unreal(path, filename, offset, length);

        if (type.equals("Texture")) {
          resource.setExporter(exporterTexture);
        }
        else if (type.equals("Sound")) {
          resource.setExporter(exporterSound);
        }
        else if (type.equals("Palette")) {
          resource.setExporter(exporterPalette);
        }
        else {
          resource.setExporter(exporterObject);
        }

        resources[i] = resource;

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
   * Reads a single Property
   **********************************************************************************************
   **/
  @Override
  public UnrealProperty readProperty(FileManipulator fm) throws Exception {

    long nameID = readIndex(fm);

    try {
      if (names[(int) nameID].equals("None")) {
        return null;
      }
    }
    catch (Throwable t) {
      return null;
    }

    int infoByte = ByteConverter.unsign(fm.readByte());

    boolean array = (infoByte >= 128);
    int size = (infoByte >> 4) & 7;
    int type = (infoByte & 15);

    // convert sizes
    if (size == 0) {
      size = 1;
    }
    else if (size == 1) {
      size = 2;
    }
    else if (size == 2) {
      size = 4;
    }
    else if (size == 3) {
      size = 12;
    }
    else if (size == 4) {
      size = 16;
    }
    else if (size == 5) {
      size = ByteConverter.unsign(fm.readByte());
    }
    else if (size == 6) {
      size = fm.readShort();
    }
    else if (size == 7) {
      size = fm.readInt();
    }

    // read array index
    int arrayIndex = 0;
    if (type == 3) {
      // boolean type - not an array
      if (array) {
        arrayIndex = 1;
      }
    }
    else if (array) {
      // array - need to read index
      arrayIndex = readArrayIndex(fm);
    }

    //System.out.println(arrayIndex + "\t" + size + "\t" + type + "\t" + nameID + "\t" + names[(int)nameID]);

    UnrealProperty property = new UnrealProperty(names[(int) nameID], nameID, arrayIndex, size, type);

    // analyse property data
    if (type == 0) {
      return readProperty0(property, fm);
    }
    else if (type == 1) {
      return readProperty1(property, fm);
    }
    else if (type == 2) {
      return readProperty2(property, fm);
    }
    else if (type == 3) {
      return readProperty3(property, fm);
    }
    else if (type == 4) {
      return readProperty4(property, fm);
    }
    else if (type == 5) {
      return readProperty5(property, fm);
    }
    else if (type == 6) {
      return readProperty6(property, fm);
    }
    else if (type == 7) {
      return readProperty7(property, fm);
    }
    else if (type == 8) {
      return readProperty8(property, fm);
    }
    else if (type == 9) {
      return readProperty9(property, fm);
    }
    else if (type == 10) {
      return readProperty10(property, fm);
    }
    else if (type == 11) {
      return readProperty11(property, fm);
    }
    else if (type == 12) {
      return readProperty12(property, fm);
    }
    else if (type == 13) {
      return readProperty13(property, fm);
    }
    else if (type == 14) {
      return readProperty14(property, fm);
    }
    else if (type == 15) {
      return readProperty15(property, fm);
    }

    return property;

  }

  /**
   **********************************************************************************************
   * Overwritten - slight change
   **********************************************************************************************
   **/
  @Override
  public UnrealProperty readProperty10(UnrealProperty property, FileManipulator fm) {
    // Structure

    String name = names[(int) property.getNameID()];

    if (name.equals("MipZero") || name.equals("MaxColor")) {
      property.setValue(fm.readBytes(5));
    }
    else {
      // normal
      property.setValue(fm.readBytes((int) property.getLength()));
    }
    return property;
  }

}