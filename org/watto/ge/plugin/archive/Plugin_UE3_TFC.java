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
import org.watto.ge.plugin.archive.datatype.UnrealProperty;
import org.watto.io.FileManipulator;

/**
**********************************************************************************************
THIS IS ONLY FOR TESTING. IN REALITY, THIS PLUGIN IS USELESS, AS THIS IS JUST A DATA STORE FOR
CONTENT THAT'S REFERENCED IN UPK ARCHIVES.
**********************************************************************************************
**/
public class Plugin_UE3_TFC extends PluginGroup_UE3 {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_UE3_TFC() {
    super("UE3_TFC", "Unreal Engine 3 TFC Archive");

    setExtensions("tfc");
    setGames("Unreal Engine");
    setPlatforms("PC");

    setEnabled(false); // THIS IS FOR TESTING ONLY

  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public int getMatchRating(FileManipulator fm) {
    return super.getMatchRating(fm, 0);
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

      FileManipulator decompFM = decompressTFCArchive(fm);
      if (decompFM != null) {
        fm.close(); // close the original archive
        fm = decompFM; // now we're going to read from the decompressed file instead
        fm.seek(0); // go to the same point in the decompressed file as in the compressed file

        path = fm.getFile(); // So the resources are stored against the decompressed file
      }

      fm.close();
      return null;

      //fm.close();

      //return resources;

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