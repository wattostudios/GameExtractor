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
import org.watto.ErrorLogger;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.archive.datatype.UnrealImportEntry;
import org.watto.ge.plugin.archive.datatype.UnrealProperty;
import org.watto.ge.plugin.exporter.Exporter_Custom_UE3_SoundNodeWave_451;
import org.watto.ge.plugin.resource.Resource_Unreal;
import org.watto.io.FileManipulator;
import org.watto.io.converter.IntConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_UE3_451 extends PluginGroup_UE3 {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_UE3_451() {
    super("UE3_451", "Unreal Engine 3 [451]");

    setExtensions("upk", "ut3");
    setGames("Alliance of Valiant Arms (AVA)");
    setPlatforms("PC");

  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public int getMatchRating(FileManipulator fm) {
    return super.getMatchRating(fm, 451);
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public Resource[] read(File path) {
    try {

      Exporter_Custom_UE3_SoundNodeWave_451 audioExporter = Exporter_Custom_UE3_SoundNodeWave_451.getInstance();

      addFileTypes();

      FileManipulator fm = new FileManipulator(path, false);
      long arcSize = fm.getLength();

      // 4 - Unreal Header (193,131,42,158)
      fm.skip(4);

      // 2 - Version (507)
      version = fm.readShort();

      // 2 - License Mode (11)
      // 4 - First File Offset
      fm.skip(6);

      // 4 - Base Name Length (including null) (eg "bg", "none", etc)
      int baseNameLength = fm.readInt();
      FieldValidator.checkFilenameLength(baseNameLength);

      // X - Base Name
      // 1 - null Base Name Terminator
      fm.skip(baseNameLength);

      // 2 - Package Flags (9)
      // 2 - Package Flags (8)
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

      // 4 - Padding Offset (offset to the end of the File Details directory)
      // 16 - GUID Hash
      fm.skip(20);

      // 4 - Number Of Generations
      int numGenerations = fm.readInt();
      FieldValidator.checkNumFiles(numGenerations);

      // for each generation
      // 4 - Number Of Files
      // 4 - Number Of Names
      fm.skip(numGenerations * 8);

      // 4 - Unknown
      // 4 - Unknown
      // 4 - Unknown
      // 8 - null
      fm.skip(20);

      Resource_Unreal[] resources = new Resource_Unreal[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // read the names directory
      fm.seek(nameOffset);
      readNamesDirectory(fm, numNames);

      // read the types directory
      fm.seek(typesOffset);
      UnrealImportEntry[] imports = readImportDirectory(fm, numTypes);

      // read the files directory
      fm.seek(dirOffset);

      int[] parentNameIDs = new int[numFiles];
      String[] parentNames = new String[numFiles];
      for (int i = 0; i < numFiles; i++) {
        /*
        // 4 - Unknown ID (XOR with 255)
        // 4 - null
        fm.skip(8);
        
        // 4 - Type ID
        int typeID = fm.readInt();
        FieldValidator.checkRange(typeID, 0, numTypes);
        String type = imports[typeID].getName();
        
        // 4 - File Name ID
        int fileNameID = fm.readInt();
        FieldValidator.checkRange(fileNameID, 0, numNames);
        String filename = names[fileNameID] + "." + type;
        
        // 4 - Unknown ID
        // 8 - null
        // 4 - Flags
        fm.skip(16);
        */

        //System.out.println(fm.getOffset());

        // 4 - Type ID (XOR with 255)
        int typeID = fm.readInt();
        if (typeID < 0) {
          typeID = (0 - typeID) - 1;
        }
        FieldValidator.checkRange(typeID, 0, numTypes);
        String type = imports[typeID].getName();

        // 4 - null
        fm.skip(4);

        // 4 - Parent Name ID
        int parentNameID = fm.readInt();
        FieldValidator.checkRange(parentNameID, 0, numFiles);
        parentNameIDs[i] = parentNameID;

        // 4 - File Name ID
        int fileNameID = fm.readInt();
        FieldValidator.checkRange(fileNameID, 0, numNames);

        // 4 - Distinct ID
        int distinctID = fm.readInt();

        String distinctName = "";
        if (distinctID != 0) {
          distinctName = "(" + distinctID + ")";
        }

        String filename = names[fileNameID] + distinctName + "." + type;

        parentNames[i] = names[fileNameID]; // for calculating the parent directories in the loop later on

        // 8 - null
        // 4 - Flags
        fm.skip(12);

        // 4 - File Length
        int length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - File Offset
        long offset = IntConverter.unsign(fm.readInt());
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - Extra Fields?
        int numExtra = fm.readInt() * 12;
        FieldValidator.checkOffset(fm.getOffset() + numExtra);
        fm.skip(numExtra);

        // 4 - Unknown (0/1)
        fm.skip(4);

        // 16 - GUID Hash or NULL
        // 4 - Unknown (0/1)
        fm.skip(20);

        //path,id,name,offset,length,decompLength,exporter
        resources[i] = new Resource_Unreal(path, filename, offset, length);

        if (type.equals("SoundNodeWave")) {
          resources[i].setExporter(audioExporter);
        }

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

      /*
      // FOR TESTING ONLY --> NOW GO THROUGH AND READ ALL THE PROPERTIES FOR THE (first) FILE
      fm.seek(resources[0].getOffset() + 4);
      System.out.println("Reading properties - starting at offset " + fm.getOffset() + "...");
      UnrealProperty[] properties = readProperties(fm);
      for (int i = 0; i < properties.length; i++) {
        UnrealProperty property = properties[i];
        System.out.println(property.toString());
      }
      System.out.println("Done - stopped at offset " + fm.getOffset());
      */

      /*
      // FOR TESTING ONLY --> See if the first file will decompress OK
      resources[0].setDecompressedLength(32768);
      resources[0].setLength(11278);
      resources[0].setOffset(2219);
      resources[0].setExporter(Exporter_LZO_SingleBlock.getInstance());
      */

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
   Reads the Names Directory into the <i>names</i> global variable
   **********************************************************************************************
   **/
  @Override
  public void readNamesDirectory(FileManipulator fm, int nameCount) {
    try {
      names = new String[nameCount];

      for (int i = 0; i < nameCount; i++) {
        // 4 - Name Length (including null)
        int nameLength = fm.readInt() - 1;

        if (nameLength < 0 && nameLength >= -255) {
          // the name exists, but it's encoded or something
          nameLength = 0 - nameLength;

          // X - Name
          fm.skip(nameLength);
          names[i] = "Unknown";
        }
        else {
          // read the name as normal
          FieldValidator.checkFilenameLength(nameLength);

          // X - Name
          names[i] = fm.readString(nameLength);
        }

        // 1 - null Name Terminator
        fm.skip(1);

        // 1 - Unknown
        // X - Extra Data
        // 4 - null
        for (int p = 0; p < 100; p++) {
          // continue reading until we find the 4 nulls.
          // limit of 100 characters, just in case it runs away.
          if (fm.readByte() == 0 && fm.readByte() == 0 && fm.readByte() == 0 && fm.readByte() == 0) {
            // found the 4 nulls
            break;
          }
        }

        // 4 - Flags
        //fm.skip(4);
        while (fm.readByte() == 0) {
          // read another byte until we don't get a null any more
        }
        fm.skip(3);

      }
    }
    catch (Throwable t) {
      names = new String[0];
      ErrorLogger.log(t);
    }
  }

  /**
   **********************************************************************************************
   StrProperty
   **********************************************************************************************
   **/
  @Override
  public UnrealProperty readStrProperty(FileManipulator fm, UnrealProperty property) {
    try {

      String text = "";

      // 4 - String Length (including null terminator)
      int stringLength = fm.readInt();
      if (stringLength > 0) {
        stringLength -= 1;// -1 for the null terminator

        FieldValidator.checkLength(stringLength);

        // X - String (ASCII)
        text = fm.readString(stringLength);

        // 1 - null String Terminator
        fm.skip(1);
      }
      else {
        // negative means a unicode string, not an ascii string
        stringLength = 0 - stringLength;

        stringLength -= 1;// -1 for the null terminator
        FieldValidator.checkLength(stringLength);

        // X - String (Unicode)
        text = fm.readUnicodeString(stringLength);

        // 2 - null Unicode String Terminator
        fm.skip(2);
      }

      property.setValue(text);

      return property;
    }
    catch (Throwable t) {
      ErrorLogger.log(t);
      return null;
    }
  }

}