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

package org.watto.ge.helper;

import java.awt.Point;
import java.io.File;

import org.watto.ErrorLogger;
import org.watto.Settings;
import org.watto.ge.plugin.archive.datatype.UnrealImportEntry;
import org.watto.ge.plugin.archive.datatype.UnrealProperty;
import org.watto.io.FileManipulator;
import org.watto.io.Hex;
import org.watto.io.converter.ByteArrayConverter;
import org.watto.io.converter.IntConverter;
import org.watto.xml.XMLNode;
import org.watto.xml.XMLReader;

/**
**********************************************************************************************
Plugin for reading Unreal Engine 4 uasset files
**********************************************************************************************
**/
public class UE4Helper {

  protected static String[] names;

  protected static byte[][] aesKeys = null;

  static int version = 0;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public static byte[][] getAESKeys() {
    if (aesKeys == null || aesKeys.length <= 0) {
      loadAESKeys();
    }
    return aesKeys;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public static void loadAESKeys() {
    try {
      String keysFileString = Settings.get("UE4AESKeysFile");

      if (keysFileString == null || keysFileString.equals("")) {
        return;
      }

      File keysFile = new File(keysFileString);
      if (!keysFile.exists()) {
        return;
      }

      // Parse the XML
      XMLNode root = XMLReader.read(keysFile);

      int numKeys = root.getChildCount();
      aesKeys = new byte[numKeys][0];

      /*
      for (int i = 0; i < numKeys; i++) {
        XMLNode keyNode = root.getChild(i);
        if (keyNode != null) {
          XMLNode hexNode = keyNode.getChild("hex");
          if (hexNode != null) {
            String hexString = hexNode.getContent();
            if (hexString.startsWith("0x")) {
              hexString = hexString.substring(2);
            }
            aesKeys[i] = ByteArrayConverter.convertLittle(new Hex(hexString));
          }
        }
      }
      */
      for (int i = 0; i < numKeys; i++) {
        XMLNode keyNode = root.getChild(i);
        if (keyNode != null) {
          String hexString = keyNode.getContent();
          if (hexString.startsWith("0x")) {
            hexString = hexString.substring(2);
          }
          aesKeys[i] = ByteArrayConverter.convertLittle(new Hex(hexString));
        }
      }

      //aesKeys = new byte[3][0];
      //aesKeys[0] = ByteArrayConverter.convertLittle(new Hex("90BAAAE538F6B96FBE77F4A1EF75DDEB62AAE6A54790B37F46AE055D2E787821"));
      //aesKeys[1] = ByteArrayConverter.convertLittle(new Hex("80BAAAE538F6B96FBE77F4A1EF75DDEB62AAE6A54790B37F46AE055D2E787821"));
      //aesKeys[2] = ByteArrayConverter.convertLittle(new Hex("D0BAAAE538F6B96FBE77F4A1EF75DDEB62AAE6A54790B37F46AE055D2E787821"));
    }
    catch (Throwable t) {
      ErrorLogger.log(t);
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public static int getVersion() {
    return version;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public static void setVersion(int version) {
    UE4Helper.version = version;
  }

  /**
   **********************************************************************************************
   * Determines the file type based on the object class ID
   **********************************************************************************************
   **/
  public static String getExtension(long objClass) {
    try {

      if (objClass == 0) {
        return "";
      }

      String typeName = "";

      if (objClass > 0 && objClass < names.length) {
        typeName = names[(int) objClass];
      }
      else if (objClass < 0) {
        objClass = 0 - objClass - 1;
        if (objClass < names.length) {
          typeName = names[(int) objClass];
        }
        else {
          return "";
        }
      }
      else {
        return "";
      }

      return "." + typeName;

    }
    catch (Throwable t) {
      return "";
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public static int getMatchRating(FileManipulator fm, int... versions) {
    try {

      int rating = 0;

      //if (FieldValidator.checkExtension(fm, extensions)) {
      //  rating += 25;
      //}

      // 4 - Header
      if (fm.readByte() == -63 && fm.readByte() == -125 && fm.readByte() == 42 && fm.readByte() == -98) {
        rating += 50;
      }
      else {
        return 0; // must have the Unreal Engine header!
      }

      // 4 - Version (XOR with 255)
      byte[] versionBytes = new byte[4];
      versionBytes[0] = (byte) ((fm.readByte()) ^ 255);
      versionBytes[1] = (byte) ((fm.readByte()) ^ 255);
      versionBytes[2] = (byte) ((fm.readByte()) ^ 255);
      versionBytes[3] = (byte) ((fm.readByte()) ^ 255);

      version = IntConverter.convertLittle(versionBytes);
      for (int i = 0; i < versions.length; i++) {
        if (version == versions[i]) {
          ErrorLogger.log("Processing UE4 version " + version);
          rating += 5;
          break;
        }
      }

      return rating;

    }
    catch (Throwable t) {
      return 0;
    }
  }

  /**
   **********************************************************************************************
  
   **********************************************************************************************
   **/
  public static String getName(long nameID) {
    if (nameID >= 0 && nameID < names.length) {
      return names[(int) nameID];
    }
    return null;
  }

  /**
   **********************************************************************************************
  
   **********************************************************************************************
   **/
  public static String[] getNames() {
    return names;
  }

  /**
   **********************************************************************************************
   Given the main definition of a Property, call the appropriate method for reading the data for
   the <i>type</i> of property. eg if its a BoolProperty, call readBoolProperty();
   **********************************************************************************************
   **/
  public static UnrealProperty handlePropertyType(FileManipulator fm, UnrealProperty property) {
    try {
      String type = property.getType();

      if (type.equals("StructProperty")) {
        return readStructProperty(fm, property);
      }
      else if (type.equals("IntPoint")) {
        return readIntPoint(fm, property);
      }
      else if (type.equals("BoolProperty")) {
        return readBoolProperty(fm, property);
      }
      else if (type.equals("ByteProperty")) {
        return readByteProperty(fm, property);
      }
      else if (type.equals("FloatProperty")) {
        return readFloatProperty(fm, property);
      }
      else if (type.equals("IntProperty")) {
        return readIntProperty(fm, property);
      }
      else if (type.equals("NameProperty")) {
        return readNameProperty(fm, property);
      }
      else if (type.equals("ObjectProperty")) {
        return readObjectProperty(fm, property);
      }
      else if (type.equals("ArrayProperty")) {
        return readArrayProperty(fm, property);
      }
      else if (type.equals("StrProperty")) {
        return readStrProperty(fm, property);
      }
      else if (type.equals("Guid")) {
        return readGuid(fm, property);
      }
      else if (type.equals("ColorMaterialInput")) {
        return readColorMaterialInput(fm, property);
      }
      else if (type.equals("ScalarMaterialInput")) {
        return readScalarMaterialInput(fm, property);
      }
      else if (type.equals("SoundConcurrencySettings")) {
        return readSoundConcurrencySettings(fm, property);
      }
      else if (type.equals("TextureStreamingData")) {
        return readTextureStreamingData(fm, property);
      }
      else if (type.equals("MaterialTextureInfo")) {
        return readMaterialTextureInfo(fm, property);
      }
      else if (type.equals("FontCharacter")) {
        return readFontCharacter(fm, property);
      }
      else if (type.equals("FontImportOptionsData")) {
        return readFontImportOptionsData(fm, property);
      }
      else if (type.equals("SubtitleCue")) {
        return readSubtitleCue(fm, property);
      }
      else {
        if (Settings.getBoolean("DebugMode")) {
          System.out.println("[UE4Helper] Unknown property: " + type + " starting at offset " + fm.getOffset() + " - trying to parse as readUnrecognisedType()");
          //throw new Exception(); // to generate the current stack
        }
        return readUnrecognisedType(fm, property);
        //return null; // want to force-stop here!
      }

    }
    catch (Throwable t) {
      ErrorLogger.log(t);
      return null;
    }

  }

  /**
   **********************************************************************************************
   ArrayProperty
   **********************************************************************************************
   **/
  @SuppressWarnings("unused")
  public static UnrealProperty readArrayProperty(FileManipulator fm, UnrealProperty property) {
    try {
      // 8 - Class ID
      long typeID = fm.readLong();
      String type = names[(int) typeID];

      // 1 - null
      fm.skip(1);

      // NOTE: LengthProperty from above gives the length of the next 4-byte field and the full contents of the array "for" loop

      // 4 - Number of Values in the Array
      int entryCount = fm.readInt();
      FieldValidator.checkNumFiles(entryCount); // sanity check, in case the number is HUGE

      boolean isStruct = false;
      int length = 0;
      if (type.equals("StructProperty")) {
        isStruct = true;

        // 8 - Name ID
        long innerNameID = fm.readLong();
        String innerName = names[(int) innerNameID];

        // 8 - Class ID
        long innerTypeID = fm.readLong();
        String innerType = names[(int) innerTypeID];

        // 8 - Length Property
        long innerLength = fm.readLong();
        length = (int) innerLength;

        // 8 - Class ID
        // HERE WE OVERWRITE THE ONES DEFINED IN THE ARRAY WITH THE TYPE DEFINED IN THE STRUCTPROPERTY
        typeID = fm.readLong();
        type = names[(int) typeID];

        // 8 - null
        // 8 - null
        fm.skip(16);
      }

      UnrealProperty[] innerProperties = new UnrealProperty[entryCount];
      for (int i = 0; i < entryCount; i++) {
        //UnrealProperty innerProperty = new UnrealProperty("", 0, type, typeID, 0);
        UnrealProperty innerProperty = new UnrealProperty("", 0, type, typeID, length);

        innerProperty = handlePropertyType(fm, innerProperty);

        if (innerProperty == null) {
          return null; // error case
        }

        innerProperties[i] = innerProperty;
      }

      if (isStruct && entryCount > 1) {
        // 1 - null
        fm.skip(1);
      }

      property.setValue(innerProperties);

      return property;
    }
    catch (Throwable t) {
      ErrorLogger.log(t);
      return null;
    }
  }

  /**
   **********************************************************************************************
   BoolProperty
   **********************************************************************************************
   **/
  public static UnrealProperty readBoolProperty(FileManipulator fm, UnrealProperty property) {
    try {

      // 2 - Boolean Value (0/1)
      short boolValue = fm.readShort();

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
  public static UnrealProperty readByteProperty(FileManipulator fm, UnrealProperty property) {
    try {
      long length = property.getLength();
      if (length < 0) {
        return null; // error case
      }

      // X - Bytes (the length of X = LengthProperty from above)
      byte[] bytes = fm.readBytes((int) length);
      property.setValue(bytes);

      // 1 - null
      // X - Bytes (the length of X = LengthProperty from above)
      fm.skip(length + 1);

      return property;
    }
    catch (Throwable t) {
      ErrorLogger.log(t);
      return null;
    }
  }

  /**
   **********************************************************************************************
   ColorMaterialInput
   **********************************************************************************************
   **/
  public static UnrealProperty readColorMaterialInput(FileManipulator fm, UnrealProperty property) {
    try {
      long length = property.getLength();
      if (length < 0) {
        return null; // error case
      }

      // 1 - null
      fm.skip(1);

      // X - Color Material Input Data (the length of X = LengthProperty from above)
      byte[] bytes = fm.readBytes((int) length);
      property.setValue(bytes);

      return property;
    }
    catch (Throwable t) {
      ErrorLogger.log(t);
      return null;
    }
  }

  /**
   **********************************************************************************************
   FloatProperty
   **********************************************************************************************
   **/
  public static UnrealProperty readFloatProperty(FileManipulator fm, UnrealProperty property) {
    try {
      // 1 - null
      fm.skip(1);

      // 4 - Float Value
      float floatValue = fm.readFloat();

      property.setValue(new Float(floatValue));

      return property;
    }
    catch (Throwable t) {
      ErrorLogger.log(t);
      return null;
    }
  }

  /**
   **********************************************************************************************
   FontCharacter
   **********************************************************************************************
   **/
  public static UnrealProperty readFontCharacter(FileManipulator fm, UnrealProperty property) {
    try {

      // 1 - null
      // 4 - Unknown
      // 4 - Unknown
      // 4 - Unknown
      // 8 - Unknown
      fm.skip(21);

      return property;
    }
    catch (Throwable t) {
      ErrorLogger.log(t);
      return null;
    }
  }

  /**
   **********************************************************************************************
   FontImportOptionsData
   **********************************************************************************************
   **/
  public static UnrealProperty readFontImportOptionsData(FileManipulator fm, UnrealProperty property) {
    try {

      // 1 - null
      fm.skip(1);

      //System.out.println("FontImportOptionsData: " + fm.getOffset());

      // Read properties until a "None" is reached - these properties form part of the FontImportOptionsData class
      UnrealProperty[] innerProperties = readProperties(fm);
      property.setValue(innerProperties);

      // These fields come after the property reading above
      // 8 - Class ID (points to None)
      // 4 - null
      fm.skip(12);

      // 4 - Extra Data Length [*4]
      int extraDataLength = fm.readInt() * 4;
      FieldValidator.checkLength(extraDataLength);

      // X - Extra Data
      fm.skip(extraDataLength);

      return property;
    }
    catch (Throwable t) {
      ErrorLogger.log(t);
      return null;
    }
  }

  /**
   **********************************************************************************************
   Guid
   **********************************************************************************************
   **/
  public static UnrealProperty readGuid(FileManipulator fm, UnrealProperty property) {
    try {
      // 1 - null
      fm.skip(1);

      // 16 - GUID
      byte[] guid = fm.readBytes(16);
      property.setValue(guid);

      return property;
    }
    catch (Throwable t) {
      ErrorLogger.log(t);
      return null;
    }
  }

  /**
   **********************************************************************************************
   Reads the Import Directory
   **********************************************************************************************
   **/
  public static UnrealImportEntry[] readImportDirectory(FileManipulator fm, int importCount) {
    long startOffset = fm.getOffset();

    boolean try32 = false;
    try {
      UnrealImportEntry[] imports = new UnrealImportEntry[importCount];

      for (int i = 0; i < importCount; i++) {
        // 8 - Parent Directory Name ID
        long parentNameID = fm.readLong();

        // 8 - Class ID (ID to "Package", "Class", etc)
        long typeID = fm.readLong();

        if (parentNameID > 999999 && typeID > 999999) {
          // try 32 instead
          try32 = true;
          break;
        }

        // 4 - Parent Import Object ID (-1 for no parent) (XOR with 255)
        byte[] parentIDBytes = new byte[4];
        parentIDBytes[0] = (byte) ((fm.readByte()) ^ 255);
        parentIDBytes[1] = (byte) ((fm.readByte()) ^ 255);
        parentIDBytes[2] = (byte) ((fm.readByte()) ^ 255);
        parentIDBytes[3] = (byte) ((fm.readByte()) ^ 255);
        int parentID = IntConverter.convertLittle(parentIDBytes);

        // 4 - Name ID
        int nameID = fm.readInt();

        // 4 - Unknown ID
        int unknownID = fm.readInt();

        imports[i] = new UnrealImportEntry(parentNameID, names[(int) typeID], typeID, parentID, names[nameID], nameID, unknownID);
      }

      if (!try32) {
        return imports;
      }
    }
    catch (Throwable t) {
      try32 = true; // imports weren't 28 bytes, so try 32 bytes (in some newer games)
    }

    if (try32) {
      fm.relativeSeek(startOffset);

      try {
        UnrealImportEntry[] imports = new UnrealImportEntry[importCount];

        for (int i = 0; i < importCount; i++) {
          // 8 - Parent Directory Name ID
          long parentNameID = fm.readLong();

          // 8 - Class ID (ID to "Package", "Class", etc)
          long typeID = fm.readLong();

          // 4 - Parent Import Object ID (-1 for no parent) (XOR with 255)
          byte[] parentIDBytes = new byte[4];
          parentIDBytes[0] = (byte) ((fm.readByte()) ^ 255);
          parentIDBytes[1] = (byte) ((fm.readByte()) ^ 255);
          parentIDBytes[2] = (byte) ((fm.readByte()) ^ 255);
          parentIDBytes[3] = (byte) ((fm.readByte()) ^ 255);
          int parentID = IntConverter.convertLittle(parentIDBytes);

          // 4 - Name ID
          int nameID = fm.readInt();

          // 4 - Unknown ID
          int unknownID = fm.readInt();

          // 4 - Unknown ID 2 (null?)
          fm.skip(4);

          imports[i] = new UnrealImportEntry(parentNameID, names[(int) typeID], typeID, parentID, names[nameID], nameID, unknownID);
        }

        return imports;
      }
      catch (Throwable t) {
        ErrorLogger.log(t);
      }
    }

    return new UnrealImportEntry[0];
  }

  /**
   **********************************************************************************************
   IntPoint
   **********************************************************************************************
   **/
  public static UnrealProperty readIntPoint(FileManipulator fm, UnrealProperty property) {
    try {

      // 1 - null
      fm.skip(1);

      // 4 - Point X
      int xPoint = fm.readInt();

      // 4 - Point Y
      int yPoint = fm.readInt();

      property.setValue(new Point(xPoint, yPoint));

      return property;
    }
    catch (Throwable t) {
      ErrorLogger.log(t);
      return null;
    }
  }

  /**
   **********************************************************************************************
   IntProperty
   **********************************************************************************************
   **/
  public static UnrealProperty readIntProperty(FileManipulator fm, UnrealProperty property) {
    try {

      // 1 - null
      fm.skip(1);

      // 4 - Integer Value
      int intValue = fm.readInt();

      property.setValue(new Integer(intValue));

      return property;
    }
    catch (Throwable t) {
      ErrorLogger.log(t);
      return null;
    }
  }

  /**
   **********************************************************************************************
   MaterialTextureInfo
   **********************************************************************************************
   **/
  public static UnrealProperty readMaterialTextureInfo(FileManipulator fm, UnrealProperty property) {
    try {

      // 1 - null
      fm.skip(1);

      // Read properties until a "None" is reached - these properties form part of the MaterialTextureInfo class
      UnrealProperty[] innerProperties = readProperties(fm);
      property.setValue(innerProperties);

      return property;
    }
    catch (Throwable t) {
      ErrorLogger.log(t);
      return null;
    }
  }

  /**
   **********************************************************************************************
   NameProperty
   **********************************************************************************************
   **/
  public static UnrealProperty readNameProperty(FileManipulator fm, UnrealProperty property) {
    try {
      // 1 - null
      fm.skip(1);

      // 4 - Name ID
      int nameID = fm.readInt();
      String name = names[nameID];

      property.setValue(name);

      // 4 - Unknown
      fm.skip(4);

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
  public static void readNamesDirectory(FileManipulator fm, int nameCount) {
    long currentOffset = fm.getOffset();
    boolean tryAgain = false;

    try {
      names = new String[nameCount];

      for (int i = 0; i < nameCount; i++) {
        // 4 - Name Length (including null)
        int nameLength = fm.readInt();
        boolean unicode = false;
        if (nameLength < 0) {
          unicode = true;
          nameLength = 0 - nameLength;
        }
        nameLength -= 1; // remove the null terminator
        FieldValidator.checkFilenameLength(nameLength);

        if (unicode) {
          // X - Name (unicode)
          names[i] = fm.readUnicodeString(nameLength);

          // 2 - null Unicode Name Terminator
          fm.skip(2);
        }
        else {
          // X - Name
          names[i] = fm.readString(nameLength);

          // 1 - null Name Terminator
          fm.skip(1);
        }

        // 4 - Flags
        fm.skip(4);
      }
    }
    catch (Throwable t) {
      tryAgain = true;
    }

    if (!tryAgain) {
      return; // already successfully read the filenames
    }

    try {
      fm.seek(currentOffset); // go back to the start of the names directory again
      names = new String[nameCount];

      // try reading again, but without the flag fields
      for (int i = 0; i < nameCount; i++) {
        /*
        // 4 - Name Length (including null)
        int nameLength = fm.readInt() - 1;
        if (i == nameCount - 1 && (nameLength <= 0 || nameLength > 500)) {
          // sometimes the last name doesn't actually exist
          names[i] = "";
          continue;
        }
        FieldValidator.checkFilenameLength(nameLength);
        
        // X - Name
        names[i] = fm.readString(nameLength);
        
        // 1 - null Name Terminator
        fm.skip(1);
        */

        // 4 - Name Length (including null)
        int nameLength = fm.readInt();
        boolean unicode = false;
        if (nameLength < 0) {
          unicode = true;
          nameLength = 0 - nameLength;
        }
        nameLength -= 1; // remove the null terminator

        if (i == nameCount - 1 && (nameLength <= 0 || nameLength > 500)) {
          // sometimes the last name doesn't actually exist
          names[i] = "";
          continue;
        }

        FieldValidator.checkFilenameLength(nameLength);

        if (unicode) {
          // X - Name (unicode)
          names[i] = fm.readUnicodeString(nameLength);

          // 2 - null Unicode Name Terminator
          fm.skip(2);
        }
        else {
          // X - Name
          names[i] = fm.readString(nameLength);

          // 1 - null Name Terminator
          fm.skip(1);
        }

      }
    }
    catch (Throwable t) {
      // nope, still failed - throw an error
      names = new String[0];
      ErrorLogger.log(t);
    }
  }

  /**
   **********************************************************************************************
   ObjectProperty
   **********************************************************************************************
   **/
  public static UnrealProperty readObjectProperty(FileManipulator fm, UnrealProperty property) {
    try {

      // 1 - null
      fm.skip(1);

      // 4 - Property
      int objectProperty = fm.readInt();
      property.setValue(new Integer(objectProperty));

      return property;
    }
    catch (Throwable t) {
      ErrorLogger.log(t);
      return null;
    }
  }

  /**
   **********************************************************************************************
   * Reads all the Properties until a 'None' one is reached
   **********************************************************************************************
   **/
  public static UnrealProperty[] readProperties(FileManipulator fm) throws Exception {
    UnrealProperty[] properties = new UnrealProperty[5];
    int currentProperty = 0;

    UnrealProperty property = readProperty(fm);
    while (property != null) {
      // enlarge the array
      if (currentProperty >= properties.length) {
        UnrealProperty[] temp = properties;
        properties = new UnrealProperty[currentProperty + 5];
        System.arraycopy(temp, 0, properties, 0, currentProperty);
      }

      properties[currentProperty] = property;
      currentProperty++;

      // read the next property
      property = readProperty(fm);
    }

    // shrink the final array
    if (currentProperty < properties.length) {
      UnrealProperty[] temp = properties;
      properties = new UnrealProperty[currentProperty];
      System.arraycopy(temp, 0, properties, 0, currentProperty);
    }

    return properties;
  }

  /**
   **********************************************************************************************
   * Reads a single Property
   **********************************************************************************************
   **/
  public static UnrealProperty readProperty(FileManipulator fm) {
    try {
      // 8 - Name ID
      long nameID = fm.readLong();
      String name = names[(int) nameID];

      if (name.equals("None")) {
        return null;
      }

      // 8 - Class ID
      long typeID = fm.readLong();
      String type = names[(int) typeID];

      // 8 - Length Property
      long length = fm.readLong();

      UnrealProperty property = new UnrealProperty(name, nameID, type, typeID, length);

      // call the appropriate method for the property type, to read the rest of the property data
      return handlePropertyType(fm, property);

    }
    catch (Throwable t) {
      ErrorLogger.log(t);
      return null;
    }

  }

  /**
   **********************************************************************************************
   ScalarMaterialInput
   **********************************************************************************************
   **/
  public static UnrealProperty readScalarMaterialInput(FileManipulator fm, UnrealProperty property) {
    try {
      long length = property.getLength();
      if (length < 0) {
        return null; // error case
      }

      // 1 - null
      fm.skip(1);

      // X - Scalar Material Input Data (the length of X = LengthProperty from above)
      byte[] bytes = fm.readBytes((int) length);
      property.setValue(bytes);

      return property;
    }
    catch (Throwable t) {
      ErrorLogger.log(t);
      return null;
    }
  }

  /**
   **********************************************************************************************
   SoundConcurrencySettings
   **********************************************************************************************
   **/
  public static UnrealProperty readSoundConcurrencySettings(FileManipulator fm, UnrealProperty property) {
    try {
      long length = property.getLength();
      if (length < 0) {
        return null; // error case
      }

      // 1 - null
      fm.skip(1);

      // X - Sound Concurrency Settings Data (the length of X = LengthProperty from above)
      byte[] bytes = fm.readBytes((int) length);
      property.setValue(bytes);

      return property;
    }
    catch (Throwable t) {
      ErrorLogger.log(t);
      return null;
    }
  }

  /**
   **********************************************************************************************
   SoundConcurrencySettings
   **********************************************************************************************
   **/
  public static UnrealProperty readSubtitleCue(FileManipulator fm, UnrealProperty property) {
    try {
      long length = property.getLength();
      if (length < 0) {
        return null; // error case
      }

      // 1 - null
      fm.skip(1);

      // X - Subtitle Cue Data (the length of X = LengthProperty from above)
      byte[] bytes = fm.readBytes((int) length);
      property.setValue(bytes);

      return property;
    }
    catch (Throwable t) {
      ErrorLogger.log(t);
      return null;
    }
  }

  /**
   **********************************************************************************************
   When there is a property of unknown type, this is what it runs, rather than erroring out
   **********************************************************************************************
   **/
  public static UnrealProperty readUnrecognisedType(FileManipulator fm, UnrealProperty property) {
    try {
      long length = property.getLength();
      if (length < 0) {
        return null; // error case
      }

      // 1 - null
      fm.skip(1);

      // X - Unknown Data (the length of X = LengthProperty from above)
      byte[] bytes = fm.readBytes((int) length);
      property.setValue(bytes);

      return property;
    }
    catch (Throwable t) {
      ErrorLogger.log(t);
      return null;
    }
  }

  /**
   **********************************************************************************************
   StrProperty
   **********************************************************************************************
   **/
  public static UnrealProperty readStrProperty(FileManipulator fm, UnrealProperty property) {
    try {
      // 1 - null
      fm.skip(1);

      // 4 - String Length (including null terminator)
      int stringLength = fm.readInt() - 1; // -1 for the null terminator
      FieldValidator.checkLength(stringLength);

      // X - String
      String text = fm.readString(stringLength);

      // 1 - null String Terminator
      fm.skip(1);

      property.setValue(text);

      return property;
    }
    catch (Throwable t) {
      ErrorLogger.log(t);
      return null;
    }
  }

  /**
   **********************************************************************************************
   StructProperty
   **********************************************************************************************
   **/
  public static UnrealProperty readStructProperty(FileManipulator fm, UnrealProperty property) {
    try {

      long startOffset = fm.getOffset();

      // 8 - Class ID
      long typeID = fm.readLong();
      String type = names[(int) typeID];

      // 8 - null
      // 8 - null
      fm.skip(16);

      // innerProperty inherits the length of the StructProperty
      UnrealProperty innerProperty = new UnrealProperty("", 0, type, typeID, property.getLength());
      innerProperty = handlePropertyType(fm, innerProperty);

      if (innerProperty == null) {
        // error case, but try to be nice, so skip any remaining length and continue
        int remainingBytesToRead = (int) (property.getLength() - (fm.getOffset() - startOffset) - 24); // -24 for the 3 header fields
        if (remainingBytesToRead >= 0) {
          fm.skip(remainingBytesToRead);
        }
        else {
          // we've already gone too far, so just die
          return null;
        }
        //return null;
      }

      property.setValue(innerProperty);

      return property;
    }
    catch (Throwable t) {
      ErrorLogger.log(t);
      return null;
    }
  }

  /**
   **********************************************************************************************
   TextureStreamingData
   **********************************************************************************************
   **/
  public static UnrealProperty readTextureStreamingData(FileManipulator fm, UnrealProperty property) {
    try {

      // 1 - null
      fm.skip(1);

      // 8 - null
      // 8 - null
      byte[] bytes = fm.readBytes(16);
      property.setValue(bytes);

      return property;
    }
    catch (Throwable t) {
      ErrorLogger.log(t);
      return null;
    }
  }

  /**
   **********************************************************************************************
  
   **********************************************************************************************
   **/
  public static void setNames(String[] names) {
    UE4Helper.names = names;
  }

  /**
   **********************************************************************************************
   * Skips over all the Properties until a 'None' one is reached
   **********************************************************************************************
   **/
  public static void skipProperties(FileManipulator fm) throws Exception {
    while (readProperty(fm) != null) {
      // continue reading
    }
  }

}