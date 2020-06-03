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
import org.watto.ge.plugin.exporter.Exporter_Custom_UE3_SoundNodeWave_648;
import org.watto.ge.plugin.resource.Resource_Unreal;
import org.watto.io.FileManipulator;
import org.watto.io.converter.IntConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_UE3_507 extends PluginGroup_UE3 {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_UE3_507() {
    super("UE3_507", "Unreal Engine 3 version 507");

    setExtensions("upk");
    setGames("Grimm",
        "Styx: Master of Shadows",
        "The Last Remnant",
        "Thief");
    setPlatforms("PC");

  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public int getMatchRating(FileManipulator fm) {
    return super.getMatchRating(fm, 507);
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public Resource[] read(File path) {
    try {

      Exporter_Custom_UE3_SoundNodeWave_648 audioExporter = Exporter_Custom_UE3_SoundNodeWave_648.getInstance();

      addFileTypes();

      FileManipulator fm = new FileManipulator(path, false);
      long arcSize = fm.getLength();

      // 4 - Unreal Header (193,131,42,158)
      fm.skip(4);

      // 2 - Version (648)
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

      // 4 - Unknown
      // 4 - Unknown
      // 12 - null
      // 16 - GUID Hash
      fm.skip(36);

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
      // 4 - Unknown
      fm.skip(16);

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

      /*
      //
      // TEST FOR FULL ARCHIVE COMPRESSION
      //
      long currentOffset = fm.getOffset();
      
      // 4 - Compressed Data Offset (Offset to Unreal Header for the Compressed Block)
      int compressedBlockOffset = fm.readInt();
      if (compressedBlockOffset < currentOffset || compressedBlockOffset > arcSize) {
        // not compressed
        fm.seek(currentOffset);
      }
      else {
        fm.seek(compressedBlockOffset);
      
        // 4 - Unreal Header (193,131,42,158)
        if (fm.readByte() == -63 && fm.readByte() == -125 && fm.readByte() == 42 && fm.readByte() == -98) {
          // The whole archive is compressed
          fm.seek(currentOffset);
      
          FileManipulator decompFM = decompressArchive(fm);
          if (decompFM != null) {
            fm.close(); // close the original archive
            fm = decompFM; // now we're going to read from the decompressed file instead
            fm.seek(currentOffset); // go to the same point in the decompressed file as in the compressed file
          }
        }
        else {
          // not compressed
          fm.seek(currentOffset);
        }
      }
      */

      // 4 - Unknown
      // 4 - Unknown
      fm.skip(8);

      /*
      // TEMPORARY FOR DECOMPRESSION TESTING ONLY!!!
      
      // read the names directory
      fm.seek(nameOffset);
      readNamesDirectory(fm, numNames);
      for (int i = 0; i < numNames; i++) {
        System.out.println("Name " + i + "\t" + names[i]);
      }
      
      // read the types directory
      fm.seek(typesOffset);
      UnrealImportEntry[] imports = readImportDirectory(fm, numTypes);
      for (int i = 0; i < numTypes; i++) {
        System.out.println("Type " + i + "\t" + imports[i].getName());
      }
      
      fm.close();
      Resource[] resources = null;
      */

      //
      // Now we're reading from the decompressed file (if it was compressed), or the original file (if not compressed)
      //

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
        //System.out.println(fm.getOffset());

        // 4 - Type ID (XOR with 255)
        int typeID = fm.readInt();
        if (typeID < 0) {
          typeID = (0 - typeID) - 1;
        }

        String type;
        if (typeID < numTypes) {
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
        FieldValidator.checkRange(fileNameID, 0, numNames);

        // 4 - Distinct ID
        int distinctID = fm.readInt();

        String distinctName = "";
        if (distinctID != 0) {
          distinctName = "(" + distinctID + ")";
        }

        String filename = names[fileNameID] + distinctName + "." + type;

        parentNames[i] = names[fileNameID]; // for calculating the parent directories in the loop later on

        // 4 - Unknown
        // 4 - null
        // 4 - Flags
        fm.skip(12);

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

        // 16 - GUID Hash or NULL
        // 4 - Unknown (0/1)
        fm.skip(20);

        /*
        // 4 - Unknown (0/1)
        // 4 - Unknown (0/1)
        // 4 - Unknown
        fm.skip(12);

        // 16 - GUID Hash or NULL
        int hash1 = fm.readInt();
        int hash2 = fm.readInt();
        int hash3 = fm.readInt();
        int hash4 = fm.readInt();

        if (hash1 != 0 || hash2 != 0 || hash3 != 0 || (hash4 != 0 && hash4 != 1)) {
          // 4 - Unknown (0/1)
          fm.skip(4);
        }
        */

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
   BoolProperty
   **********************************************************************************************
   **/
  @Override
  public UnrealProperty readBoolProperty(FileManipulator fm, UnrealProperty property) {
    try {
      // 4 - Boolean Value (0/1)
      int boolValue = fm.readByte();

      if (boolValue == 0) {
        property.setValue(new Boolean(false));
      }
      else {
        property.setValue(new Boolean(true));
      }

      return property;
    }
    catch (Throwable t) {
      ErrorLogger.log(t);
      return null;
    }
  }

  /**
   **********************************************************************************************
   ByteProperty
   **********************************************************************************************
   **/
  @Override
  public UnrealProperty readByteProperty(FileManipulator fm, UnrealProperty property) {
    try {
      long length = property.getLength();
      if (length < 0) {
        return null; // error case
      }

      // X - Bytes (ignore this first entry)
      fm.skip(length);

      // X - Bytes (the length of X = LengthProperty from above)
      if (length == 1) {
        property.setValue(fm.readByte());
      }
      else if (length == 2) {
        property.setValue(fm.readShort());
      }
      else if (length == 4) {
        property.setValue(fm.readInt());
      }
      else if (length == 8) {
        property.setValue(fm.readLong());
      }
      else {
        byte[] bytes = fm.readBytes((int) length);
        property.setValue(bytes);
      }

      return property;
    }
    catch (Throwable t) {
      ErrorLogger.log(t);
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
        if (nameLength == -1) {
          // doesn't have the real filename length (it's null)

          // X - Name
          // 1 - null Name Terminator
          names[i] = fm.readNullString();

          // 4 - null
          // 4 - Flags
          fm.skip(8);
        }
        else {
          // has the real filename length

          FieldValidator.checkFilenameLength(nameLength);

          // X - Name
          names[i] = fm.readString(nameLength);

          // 1 - null Name Terminator
          // 4 - null
          // 4 - Flags
          fm.skip(9);
        }
      }
    }
    catch (Throwable t) {
      names = new String[0];
      ErrorLogger.log(t);
    }
  }

  /**
   **********************************************************************************************
   StructProperty
   **********************************************************************************************
   **/
  @Override
  public UnrealProperty readStructProperty(FileManipulator fm, UnrealProperty property) {
    try {
      // 8 - Class ID
      long typeID = fm.readLong();
      String type = names[(int) typeID];

      // innerProperty inherits the length of the StructProperty
      UnrealProperty innerProperty = new UnrealProperty("", 0, type, typeID, property.getLength());
      innerProperty = handlePropertyType(fm, innerProperty);

      if (innerProperty == null) {
        return null; // error case
      }

      property.setValue(innerProperty);

      return property;
    }
    catch (Throwable t) {
      ErrorLogger.log(t);
      return null;
    }
  }
}