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

import java.awt.Point;
import java.io.File;
import org.watto.ErrorLogger;
import org.watto.Language;
import org.watto.datatype.FileType;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.archive.datatype.UnrealImportEntry;
import org.watto.ge.plugin.archive.datatype.UnrealProperty;
import org.watto.ge.plugin.exporter.BlockExporterWrapper;
import org.watto.ge.plugin.exporter.Exporter_LZO_SingleBlock;
import org.watto.ge.plugin.exporter.Exporter_ZLib;
import org.watto.io.FileManipulator;
import org.watto.io.FilenameSplitter;
import org.watto.io.converter.IntConverter;
import org.watto.task.TaskProgressManager;
import com.sun.scenario.Settings;

/**
**********************************************************************************************

**********************************************************************************************
**/
public abstract class PluginGroup_UE3 extends ArchivePlugin {

  protected static long version = -1;

  protected static String[] names;

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
  
  **********************************************************************************************
  **/
  public static long getVersion() {
    return version;
  }

  /**
   **********************************************************************************************
  
   **********************************************************************************************
   **/
  public static void setNames(String[] namesIn) {
    names = namesIn;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public PluginGroup_UE3(String code, String name) {

    //super("UE3","Unreal Engine 3");
    super(code, name);

    //         read write replace rename
    setProperties(true, false, false, false);

    setExtensions("upk");
    setGames("");
    setPlatforms("PC", "XBox");

    // MUST BE LOWER CASE!!!
    setFileTypes(new FileType("texture2d", "Texture2D Image", FileType.TYPE_IMAGE),
        new FileType("soundnodewave", "SoundNodeWave Audio", FileType.TYPE_AUDIO));

  }

  /**
   **********************************************************************************************
   Decompressed an archive, where the whole archive is compressed.
   Reads the compressed block information first, then processes the compressed blocks themselves.
   Writes the output to a file with the same name, but with "_ge_decompressed" at the end of it.
   The decompressed file contains the same Unreal header as the compressed file, so you can open
   the decompressed file in GE directly, without needing to re-decompress anything.
   If the decompressed file already exists, we use that, we don't re-decompress.
   **********************************************************************************************
   **/
  public FileManipulator decompressArchive(FileManipulator fm) {
    try {
      // Build a new "_ge_decompressed" archive file in the current directory
      File origFile = fm.getFile();

      String pathOnly = FilenameSplitter.getDirectory(origFile);
      String filenameOnly = FilenameSplitter.getFilename(origFile);
      String extensionOnly = FilenameSplitter.getExtension(origFile);

      File decompFile = new File(pathOnly + File.separatorChar + filenameOnly + "_ge_decompressed" + "." + extensionOnly);
      if (decompFile.exists()) {
        // we've already decompressed this file before - open and return it
        return new FileManipulator(decompFile, false);
      }

      FileManipulator decompFM = new FileManipulator(decompFile, true);

      // First, copy all the header data from the original file to the decompressed one
      long currentOffset = fm.getOffset();
      long arcSize = fm.getLength();

      fm.seek(0);

      // write all bytes up to the current offset - 4
      decompFM.writeBytes(fm.readBytes((int) currentOffset - 4));

      // force-write that there are 0 compressed blocks
      int numBigBlocks = fm.readInt();
      decompFM.writeInt(0);

      // Now decompress each block into the decompressed file

      // --> get all the Big Blocks
      BlockExporterWrapper bigBlocks = readCompressedArchiveBlocks(fm, numBigBlocks);

      long[] bigOffsets = bigBlocks.getBlockOffsets();
      //long[] bigLengths = bigBlocks.getBlockLengths(); // don't actually need this

      // write the remaining 8 bytes (or sometimes more) at the end of the big blocks, before the compressed data
      int remainingBytes = (int) (bigOffsets[0] - fm.getOffset());
      decompFM.writeBytes(fm.readBytes(remainingBytes));

      // --> Go in to each big block
      TaskProgressManager.setMessage(Language.get("Progress_DecompressingArchive")); // progress bar
      TaskProgressManager.setMaximum(arcSize); // progress bar

      for (int b = 0; b < numBigBlocks; b++) {
        fm.seek(bigOffsets[b]);

        // --> read the little blocks
        // 4 - Unreal Header (193,131,42,158)
        // 2 - null
        fm.skip(6);

        // 2 - Compression Type? (1=ZLib, 2 = LZO, 4=LZX) (https://github.com/gildor2/UModel/blob/master/Unreal/UnCore.h)
        int compType = fm.readShort();
        FieldValidator.checkRange(compType, 0, 4);

        Exporter_ZLib exporterZLib = Exporter_ZLib.getInstance();
        Exporter_LZO_SingleBlock exporterLZO = Exporter_LZO_SingleBlock.getInstance();

        if (compType == 1) {
          //exporterZLib;
        }
        else if (compType == 2) {
          //exporterLZO
        }
        else {
          ErrorLogger.log("[PluginGroup_UE3] Unsupported Compression: " + compType);
          return null;
        }

        // 4 - Compressed Length
        int bigBlockCompLength = fm.readInt();
        FieldValidator.checkLength(bigBlockCompLength, arcSize);

        // 4 - Decompressed Length
        int bigBlockDecompLength = fm.readInt();
        FieldValidator.checkLength(bigBlockDecompLength);

        int numBlocks = 1000; // max number of blocks - guess
        int realNumBlocks = 0;

        int[] blockCompLengths = new int[numBlocks];
        int[] blockDecompLengths = new int[numBlocks];

        while (bigBlockDecompLength > 0) {
          // 4 - Compressed Length
          int blockCompLength = fm.readInt();
          FieldValidator.checkLength(blockCompLength, arcSize);
          blockCompLengths[realNumBlocks] = blockCompLength;

          // 4 - Decompressed Length
          int blockDecompLength = fm.readInt();
          FieldValidator.checkLength(blockDecompLength);
          blockDecompLengths[realNumBlocks] = blockDecompLength;

          realNumBlocks++;
          bigBlockDecompLength -= blockDecompLength;
        }

        numBlocks = realNumBlocks;

        // --> Decompress each little block
        ExporterPlugin exporter = null;
        for (int i = 0; i < realNumBlocks; i++) {
          TaskProgressManager.setValue(fm.getOffset()); // progress bar

          if (compType == 1) {
            exporterZLib.open(fm, blockCompLengths[i], blockDecompLengths[i]);
            exporter = exporterZLib;
          }
          else if (compType == 2) {
            exporterLZO.open(fm, blockCompLengths[i], blockDecompLengths[i]);
            exporter = exporterLZO;
          }

          while (exporter.available()) {
            decompFM.writeByte(exporter.read());
          }
        }

      }

      // Force-write out the decompressed file to write it to disk, then change the buffer to read-only.
      decompFM.close();
      decompFM = new FileManipulator(decompFile, false);

      TaskProgressManager.setMessage(Language.get("Progress_ReadingArchive")); // progress bar

      // Return the file pointer to the beginning, and return the decompressed file
      decompFM.seek(currentOffset);
      return decompFM;
    }
    catch (Throwable t) {
      ErrorLogger.log(t);
      return null;
    }
  }

  /**
   **********************************************************************************************
   Decompressed a TFC archive, where the whole archive is compressed.
   If the decompressed file already exists, we use that, we don't re-decompress.
   **********************************************************************************************
   **/
  public FileManipulator decompressTFCArchive(FileManipulator fm) {
    try {
      // Build a new "_ge_decompressed" archive file in the current directory
      File origFile = fm.getFile();

      String pathOnly = FilenameSplitter.getDirectory(origFile);
      String filenameOnly = FilenameSplitter.getFilename(origFile);
      String extensionOnly = FilenameSplitter.getExtension(origFile);

      File decompFile = new File(pathOnly + File.separatorChar + filenameOnly + "_ge_decompressed" + "." + extensionOnly);
      if (decompFile.exists()) {
        // we've already decompressed this file before - open and return it
        return new FileManipulator(decompFile, false);
      }

      FileManipulator decompFM = new FileManipulator(decompFile, true);

      // First, copy all the header data from the original file to the decompressed one
      long currentOffset = fm.getOffset();
      long arcSize = fm.getLength();

      fm.seek(0);

      TaskProgressManager.setMessage(Language.get("Progress_DecompressingArchive")); // progress bar
      TaskProgressManager.setMaximum(arcSize); // progress bar

      while (fm.getOffset() < arcSize) {

        // --> read the little blocks
        // 4 - Unreal Header (193,131,42,158)
        // 2 - null
        fm.skip(6);

        // 2 - Compression Type? (1=ZLib, 2 = LZO, 4=LZX) (https://github.com/gildor2/UModel/blob/master/Unreal/UnCore.h)
        int compType = fm.readShort();
        FieldValidator.checkRange(compType, 0, 4);

        Exporter_ZLib exporterZLib = Exporter_ZLib.getInstance();
        Exporter_LZO_SingleBlock exporterLZO = Exporter_LZO_SingleBlock.getInstance();

        if (compType == 1) {
          //exporterZLib;
        }
        else if (compType == 2) {
          //exporterLZO
        }
        else {
          ErrorLogger.log("[PluginGroup_UE3] Unsupported Compression: " + compType);
          return null;
        }

        // 4 - Compressed Length
        int bigBlockCompLength = fm.readInt();
        FieldValidator.checkLength(bigBlockCompLength, arcSize);

        // 4 - Decompressed Length
        int bigBlockDecompLength = fm.readInt();
        FieldValidator.checkLength(bigBlockDecompLength);

        int numBlocks = 1000; // max number of blocks - guess
        int realNumBlocks = 0;

        int[] blockCompLengths = new int[numBlocks];
        int[] blockDecompLengths = new int[numBlocks];

        while (bigBlockDecompLength > 0) {
          // 4 - Compressed Length
          int blockCompLength = fm.readInt();
          FieldValidator.checkLength(blockCompLength, arcSize);
          blockCompLengths[realNumBlocks] = blockCompLength;

          // 4 - Decompressed Length
          int blockDecompLength = fm.readInt();
          FieldValidator.checkLength(blockDecompLength);
          blockDecompLengths[realNumBlocks] = blockDecompLength;

          realNumBlocks++;
          bigBlockDecompLength -= blockDecompLength;
        }

        numBlocks = realNumBlocks;

        // --> Decompress each little block
        ExporterPlugin exporter = null;
        for (int i = 0; i < realNumBlocks; i++) {
          TaskProgressManager.setValue(fm.getOffset()); // progress bar

          if (compType == 1) {
            exporterZLib.open(fm, blockCompLengths[i], blockDecompLengths[i]);
            exporter = exporterZLib;
          }
          else if (compType == 2) {
            exporterLZO.open(fm, blockCompLengths[i], blockDecompLengths[i]);
            exporter = exporterLZO;
          }

          while (exporter.available()) {
            decompFM.writeByte(exporter.read());
          }
        }

      }

      // Force-write out the decompressed file to write it to disk, then change the buffer to read-only.
      decompFM.close();
      decompFM = new FileManipulator(decompFile, false);

      TaskProgressManager.setMessage(Language.get("Progress_ReadingArchive")); // progress bar

      // Return the file pointer to the beginning, and return the decompressed file
      decompFM.seek(currentOffset);
      return decompFM;
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
  public int getMatchRating(FileManipulator fm, int... versions) {
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

      // 2 - Version
      int version = fm.readShort();
      for (int i = 0; i < versions.length; i++) {
        if (version == versions[i]) {
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
   Given the main definition of a Property, call the appropriate method for reading the data for
   the <i>type</i> of property. eg if its a BoolProperty, call readBoolProperty();
   **********************************************************************************************
   **/
  public UnrealProperty handlePropertyType(FileManipulator fm, UnrealProperty property) {
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
      else {
        if (Settings.getBoolean("DebugMode")) {
          System.out.println("[PluginGroup_UE3] Unknown property: " + type + " starting at offset " + fm.getOffset() + " - trying to parse as readUnrecognisedType()");
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
  public UnrealProperty readArrayProperty(FileManipulator fm, UnrealProperty property) {
    try {
      // 8 - Class ID
      long typeID = fm.readLong();
      String type = names[(int) typeID];

      // NOTE: LengthProperty from above gives the length of the next 4-byte field and the full contents of the array "for" loop

      // 4 - Number of Values in the Array
      int entryCount = fm.readInt();
      FieldValidator.checkNumFiles(entryCount); // sanity check, in case the number is HUGE

      boolean isStruct = false;
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
        UnrealProperty innerProperty = new UnrealProperty("", 0, type, typeID, 0);

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
  public UnrealProperty readBoolProperty(FileManipulator fm, UnrealProperty property) {
    try {
      // 4 - Boolean Value (0/1)
      int boolValue = fm.readInt();

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
  public UnrealProperty readByteProperty(FileManipulator fm, UnrealProperty property) {
    try {
      long length = property.getLength();
      if (length < 0) {
        return null; // error case
      }

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
   ColorMaterialInput
   **********************************************************************************************
   **/
  public UnrealProperty readColorMaterialInput(FileManipulator fm, UnrealProperty property) {
    try {
      long length = property.getLength();
      if (length < 0) {
        return null; // error case
      }

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
   When a whole archive is compressed, this will read the compressed block information for the
   archive, and return it
   **********************************************************************************************
   **/
  public BlockExporterWrapper readCompressedArchiveBlocks(FileManipulator fm, int numBlocks) throws Exception {
    long arcSize = fm.getLength();

    // Now read each block
    long[] offsets = new long[numBlocks];
    long[] compLengths = new long[numBlocks];
    for (int i = 0; i < numBlocks; i++) {
      // 4 - Offset in the Decompressed File for where this data belongs
      // 4 - Decompressed Data Length
      fm.skip(8);

      // 4 - Compressed Data Offset (Offset to Unreal Header for the Compressed Block)
      long offset = IntConverter.unsign(fm.readInt());
      FieldValidator.checkOffset(offset, arcSize);
      offsets[i] = offset;

      // 4 - Compressed Data Length
      long compLength = IntConverter.unsign(fm.readInt());
      FieldValidator.checkLength(compLength, arcSize);
      compLengths[i] = compLength;
    }

    return new BlockExporterWrapper(null, offsets, compLengths, compLengths);
  }

  /**
   **********************************************************************************************
   FloatProperty
   **********************************************************************************************
   **/
  public UnrealProperty readFloatProperty(FileManipulator fm, UnrealProperty property) {
    try {

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
  public UnrealProperty readFontCharacter(FileManipulator fm, UnrealProperty property) {
    try {

      // 4 - Unknown
      // 4 - Unknown
      // 4 - Unknown
      // 8 - Unknown
      fm.skip(20);

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
  public UnrealProperty readFontImportOptionsData(FileManipulator fm, UnrealProperty property) {
    try {

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
  public UnrealProperty readGuid(FileManipulator fm, UnrealProperty property) {
    try {

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
  public UnrealImportEntry[] readImportDirectory(FileManipulator fm, int importCount) {
    try {
      UnrealImportEntry[] imports = new UnrealImportEntry[importCount];

      for (int i = 0; i < importCount; i++) {
        // 8 - Parent Directory Name ID
        long parentNameID = fm.readLong();

        // 8 - Class ID (ID to "Package", "Class", etc)
        long typeID = fm.readLong();

        // 4 - Parent Import Object ID (-1 for no parent) (XOR with 255)
        /*
        byte[] parentIDBytes = new byte[4];
        parentIDBytes[0] = (byte) ((fm.readByte()) ^ 255);
        parentIDBytes[1] = (byte) ((fm.readByte()) ^ 255);
        parentIDBytes[2] = (byte) ((fm.readByte()) ^ 255);
        parentIDBytes[3] = (byte) ((fm.readByte()) ^ 255);
        int parentID = IntConverter.convertLittle(parentIDBytes);
        */
        int parentID = fm.readInt();

        // 4 - Name ID
        int nameID = fm.readInt();

        // 4 - Unknown ID
        int unknownID = fm.readInt();

        imports[i] = new UnrealImportEntry(parentNameID, names[(int) typeID], typeID, parentID, names[nameID], nameID, unknownID);
      }

      return imports;
    }
    catch (Throwable t) {

      ErrorLogger.log(t);
    }
    return new UnrealImportEntry[0];
  }

  /**
   **********************************************************************************************
   IntPoint
   **********************************************************************************************
   **/
  public UnrealProperty readIntPoint(FileManipulator fm, UnrealProperty property) {
    try {

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
  public UnrealProperty readIntProperty(FileManipulator fm, UnrealProperty property) {
    try {

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
  public UnrealProperty readMaterialTextureInfo(FileManipulator fm, UnrealProperty property) {
    try {

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
  public UnrealProperty readNameProperty(FileManipulator fm, UnrealProperty property) {
    try {

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
  public void readNamesDirectory(FileManipulator fm, int nameCount) {
    try {
      names = new String[nameCount];

      for (int i = 0; i < nameCount; i++) {
        // 4 - Name Length (including null)
        int nameLength = fm.readInt() - 1;
        FieldValidator.checkFilenameLength(nameLength);

        // X - Name
        names[i] = fm.readString(nameLength);

        // 1 - null Name Terminator
        // 4 - Flags
        fm.skip(5);
      }
    }
    catch (Throwable t) {
      names = new String[0];
      ErrorLogger.log(t);
    }
  }

  /**
   **********************************************************************************************
   ObjectProperty
   **********************************************************************************************
   **/
  public UnrealProperty readObjectProperty(FileManipulator fm, UnrealProperty property) {
    try {
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
  public UnrealProperty[] readProperties(FileManipulator fm) throws Exception {
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
  public UnrealProperty readProperty(FileManipulator fm) {
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
  public UnrealProperty readScalarMaterialInput(FileManipulator fm, UnrealProperty property) {
    try {
      long length = property.getLength();
      if (length < 0) {
        return null; // error case
      }

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
   When there is a property of unknown type, this is what it runs, rather than erroring out
   **********************************************************************************************
   **/
  public static UnrealProperty readUnrecognisedType(FileManipulator fm, UnrealProperty property) {
    try {
      long length = property.getLength();
      if (length < 0) {
        return null; // error case
      }

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
  public UnrealProperty readStrProperty(FileManipulator fm, UnrealProperty property) {
    try {

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
  public UnrealProperty readStructProperty(FileManipulator fm, UnrealProperty property) {
    try {
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

  /**
   **********************************************************************************************
   TextureStreamingData
   **********************************************************************************************
   **/
  public UnrealProperty readTextureStreamingData(FileManipulator fm, UnrealProperty property) {
    try {

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
   * Skips over all the Properties until a 'None' one is reached
   **********************************************************************************************
   **/
  public void skipProperties(FileManipulator fm) throws Exception {
    while (readProperty(fm) != null) {
      // continue reading
    }
  }

}