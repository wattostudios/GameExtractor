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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.util.zip.InflaterInputStream;
import org.watto.Language;
import org.watto.Settings;
import org.watto.component.WSPluginException;
import org.watto.component.WSPopup;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.archive.datatype.UnrealProperty;
import org.watto.io.FileManipulator;
import org.watto.io.converter.ByteConverter;
import org.watto.io.converter.IntConverter;
import org.watto.io.converter.ShortConverter;
import org.watto.io.stream.ManipulatorInputStream;
import org.watto.io.stream.ManipulatorOutputStream;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public abstract class PluginGroup_U extends ArchivePlugin {

  protected static long version = -1;
  protected static String[] names;

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public static long getVersion() {
    return version;
  }

  /**
   **********************************************************************************************
   * Reads a single CompactIndex value from the given file
   **********************************************************************************************
   **/
  public static long readIndex(FileManipulator fm) throws Exception {

    boolean[] bitData = new boolean[35];
    java.util.Arrays.fill(bitData, false);
    boolean[] byte1 = fm.readBits();

    int bytes = 1;

    boolean negative = false;
    if (byte1[0]) { // positive or negative
      negative = true;
    }

    System.arraycopy(byte1, 2, bitData, 29, 6);
    if (byte1[1]) { // next byte?
      // Read byte 2
      bytes = 2;

      boolean[] byte2 = fm.readBits();
      System.arraycopy(byte2, 1, bitData, 22, 7);
      if (byte2[0]) { // next byte?
        // Read byte 3
        bytes = 3;

        boolean[] byte3 = fm.readBits();
        System.arraycopy(byte3, 1, bitData, 15, 7);
        if (byte3[0]) { // next byte?
          // Read byte 4
          bytes = 4;

          boolean[] byte4 = fm.readBits();
          System.arraycopy(byte4, 1, bitData, 8, 7);
          if (byte4[0]) { // next byte?
            // Read byte 5 (last byte)
            bytes = 5;

            boolean[] byte5 = fm.readBits();
            System.arraycopy(byte5, 0, bitData, 0, 8);

          }

        }

      }

    }

    long number = 0;

    //calculate number
    if (bytes >= 5) {
      if (bitData[7]) {
        number += 134217728;
      }
      if (bitData[6]) {
        number += 268435456;
      }
      if (bitData[5]) {
        number += 536870912;
      }
      if (bitData[4]) {
        number += 1073741824;
      }
      if (bitData[3]) {
        number += 2147483648L;
      }
      if (bitData[2]) {
        number += 4294967296L;
      }
      if (bitData[1]) {
        number += 8589934592L;
      }
      if (bitData[0]) {
        number += 17179869184L;
      }
    }
    if (bytes >= 4) {
      if (bitData[14]) {
        number += 1048576;
      }
      if (bitData[13]) {
        number += 2097152;
      }
      if (bitData[12]) {
        number += 4194304;
      }
      if (bitData[11]) {
        number += 8388608;
      }
      if (bitData[10]) {
        number += 16777216;
      }
      if (bitData[9]) {
        number += 33554432;
      }
      if (bitData[8]) {
        number += 67108864;
      }
    }
    if (bytes >= 3) {
      if (bitData[21]) {
        number += 8192;
      }
      if (bitData[20]) {
        number += 16384;
      }
      if (bitData[19]) {
        number += 32768;
      }
      if (bitData[18]) {
        number += 65536;
      }
      if (bitData[17]) {
        number += 131072;
      }
      if (bitData[16]) {
        number += 262144;
      }
      if (bitData[15]) {
        number += 524288;
      }
    }
    if (bytes >= 2) {
      if (bitData[28]) {
        number += 64;
      }
      if (bitData[27]) {
        number += 128;
      }
      if (bitData[26]) {
        number += 256;
      }
      if (bitData[25]) {
        number += 512;
      }
      if (bitData[24]) {
        number += 1024;
      }
      if (bitData[23]) {
        number += 2048;
      }
      if (bitData[22]) {
        number += 4096;
      }
    }
    if (bytes >= 1) {
      if (bitData[34]) {
        number += 1;
      }
      if (bitData[33]) {
        number += 2;
      }
      if (bitData[32]) {
        number += 4;
      }
      if (bitData[31]) {
        number += 8;
      }
      if (bitData[30]) {
        number += 16;
      }
      if (bitData[29]) {
        number += 32;
      }
    }

    if (negative) {
      number = 0 - number;
    }

    return number;

  }

  /**
   **********************************************************************************************
   * Writes a single CompactIndex value to the given file
   **********************************************************************************************
   **/
  public static void writeIndex(FileManipulator fm, long value) throws Exception {

    int numBytes = 1;
    if (value > 134217727) {
      numBytes = 5;
    }
    else if (value > 1048575) {
      numBytes = 4;
    }
    else if (value > 8191) {
      numBytes = 3;
    }
    else if (value > 63) {
      numBytes = 2;
    }

    int[] bytes = new int[numBytes];

    int byteVal = 0;

    if (numBytes >= 5) {
      if (value >= 17179869184L) {
        byteVal |= 128;
        value -= 17179869184L;
      }
      if (value >= 8589934592L) {
        byteVal |= 64;
        value -= 8589934592L;
      }
      if (value >= 4294967296L) {
        byteVal |= 32;
        value -= 4294967296L;
      }
      if (value >= 2147483648L) {
        byteVal |= 16;
        value -= 2147483648L;
      }
      if (value >= 1073741824) {
        byteVal |= 8;
        value -= 1073741824;
      }
      if (value >= 536870912) {
        byteVal |= 4;
        value -= 536870912;
      }
      if (value >= 268435456) {
        byteVal |= 2;
        value -= 268435456;
      }
      if (value >= 134217728) {
        byteVal |= 1;
        value -= 134217728;
      }
      bytes[4] = byteVal;
    }

    byteVal = 0;

    if (numBytes >= 4) {

      if (numBytes > 4) {
        // has next byte
        byteVal |= 128;
      }
      if (value >= 67108864) {
        byteVal |= 64;
        value -= 67108864;
      }
      if (value >= 33554432) {
        byteVal |= 32;
        value -= 33554432;
      }
      if (value >= 16777216) {
        byteVal |= 16;
        value -= 16777216;
      }
      if (value >= 8388608) {
        byteVal |= 8;
        value -= 8388608;
      }
      if (value >= 4194304) {
        byteVal |= 4;
        value -= 4194304;
      }
      if (value >= 2097152) {
        byteVal |= 2;
        value -= 2097152;
      }
      if (value >= 1048576) {
        byteVal |= 1;
        value -= 1048576;
      }
      bytes[3] = byteVal;
    }

    byteVal = 0;

    if (numBytes >= 3) {

      if (numBytes > 3) {
        // has next byte
        byteVal |= 128;
      }
      if (value >= 524288) {
        byteVal |= 64;
        value -= 524288;
      }
      if (value >= 262144) {
        byteVal |= 32;
        value -= 262144;
      }
      if (value >= 131072) {
        byteVal |= 16;
        value -= 131072;
      }
      if (value >= 65536) {
        byteVal |= 8;
        value -= 65536;
      }
      if (value >= 32768) {
        byteVal |= 4;
        value -= 32768;
      }
      if (value >= 16384) {
        byteVal |= 2;
        value -= 16384;
      }
      if (value >= 8192) {
        byteVal |= 1;
        value -= 8192;
      }
      bytes[2] = byteVal;
    }

    byteVal = 0;

    if (numBytes >= 2) {

      if (numBytes > 2) {
        // has next byte
        byteVal |= 128;
      }
      if (value >= 4096) {
        byteVal |= 64;
        value -= 4096;
      }
      if (value >= 2048) {
        byteVal |= 32;
        value -= 2048;
      }
      if (value >= 1024) {
        byteVal |= 16;
        value -= 1024;
      }
      if (value >= 512) {
        byteVal |= 8;
        value -= 512;
      }
      if (value >= 256) {
        byteVal |= 4;
        value -= 256;
      }
      if (value >= 128) {
        byteVal |= 2;
        value -= 128;
      }
      if (value >= 64) {
        byteVal |= 1;
        value -= 64;
      }
      bytes[1] = byteVal;
    }

    byteVal = 0;

    if (numBytes >= 1) {

      if (value < 0) {
        // negative
        byteVal |= 128;
      }
      if (numBytes > 1) {
        // has next byte
        byteVal |= 64;
      }
      if (value >= 32) {
        byteVal |= 32;
        value -= 32;
      }
      if (value >= 16) {
        byteVal |= 16;
        value -= 16;
      }
      if (value >= 8) {
        byteVal |= 8;
        value -= 8;
      }
      if (value >= 4) {
        byteVal |= 4;
        value -= 4;
      }
      if (value >= 2) {
        byteVal |= 2;
        value -= 2;
      }
      if (value >= 1) {
        byteVal |= 1;
        value -= 1;
      }
      bytes[0] = byteVal;
    }

    for (int i = 0; i < numBytes; i++) {
      fm.writeByte(bytes[i]);
    }

  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public PluginGroup_U(String code, String name) {

    //super("U","Unreal Engine");
    super(code, name);

    //         read write replace rename
    setProperties(true, false, false, false);

    setExtensions("u");
    setGames("");
    setPlatforms("PC", "XBox");

  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public FileManipulator checkCompression(FileManipulator fm) throws WSPluginException {
    try {

      long arcSize = fm.getFile().length();

      long origPosition = fm.getOffset();

      // 4 - Header
      // 2 - Version
      // 2 - License Mode
      // 2 - Package Flags
      fm.skip(10);

      // 2 - Compression Mode
      short compression = fm.readShort();
      if (compression == 0) {
        // no compression
        fm.seek(origPosition);
        return fm;
      }

      // 4 - Number Of Names
      fm.skip(4);

      // 4 - Name Table Offset
      long nameOffset = fm.readInt();
      FieldValidator.checkOffset(nameOffset);

      // 4 - Number Of Exports
      fm.skip(4);

      // 4 - Export Table Offset
      long exportOffset = fm.readInt();
      FieldValidator.checkOffset(exportOffset);

      // 4 - Number Of Imports
      fm.skip(4);

      // 4 - Import Table Offset
      long importOffset = fm.readInt();
      FieldValidator.checkOffset(importOffset);

      if (nameOffset > arcSize || exportOffset > arcSize || importOffset > arcSize) {
        // compressed

        // ask - do you want to decompress?
        if (Settings.getBoolean("Popup_DecompressArchive_Show")) {
          String ok = WSPopup.showConfirm("DecompressArchive", true);
          if (!ok.equals(WSPopup.BUTTON_YES)) {
            throw new WSPluginException("User chose not to decompress archive.");
          }
        }

        // create a temporary file
        File tempFile = new File(Settings.get("TempDirectory") + File.separator + "UnrealArchiveDecompressed");
        FileManipulator outfm = new FileManipulator(tempFile, true, 100000);

        // decompress into this file
        fm.seek(origPosition);
        decompressFile(fm, outfm);

        return outfm;
      }
      else {
        fm.seek(origPosition);
        return fm;
      }

    }
    catch (Throwable t) {
      logError(t);
      throw new WSPluginException("Decompression did not succeed!");
    }
  }

  /**
   **********************************************************************************************
   * Faster, because it reads the compressed data into an array in a single chunk.
   **********************************************************************************************
   **/
  public void decompressBlock(InputStream inStream, ManipulatorOutputStream outStream, int length) {
    try {

      byte[] compData = new byte[length];
      inStream.read(compData);
      inStream = new ByteArrayInputStream(compData);

      InflaterInputStream inflater = new InflaterInputStream(inStream);

      int byteValue = inflater.read();
      while (inflater.available() >= 1) {
        outStream.write(byteValue);
        byteValue = inflater.read();
      }
    }
    catch (Throwable t) {
    }
  }

  /**
   **********************************************************************************************
   * not used - slow
   **********************************************************************************************
   **/
  public void decompressBlock(ManipulatorInputStream inStream, ManipulatorOutputStream outStream) {
    InflaterInputStream inflater = new InflaterInputStream(inStream);
    try {
      int byteValue = inflater.read();
      while (inflater.available() >= 1) {
        outStream.write(byteValue);
        byteValue = inflater.read();
      }
    }
    catch (Throwable t) {
    }
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public void decompressFile(FileManipulator fm, FileManipulator out) {
    try {

      ManipulatorInputStream inStream = new ManipulatorInputStream(fm);
      ManipulatorOutputStream outStream = new ManipulatorOutputStream(out);

      TaskProgressManager.show(1, 0, Language.get("Progress_DecompressingArchive"));

      // 64 - Header
      out.writeBytes(fm.readBytes(64));

      // 4 - Number Of Blocks
      int numFiles = fm.readInt();

      int[] offsets = new int[numFiles];
      for (int i = 0; i < numFiles; i++) {
        // 4 - File Offset
        offsets[i] = fm.readInt();
      }

      TaskProgressManager.setMaximum(numFiles);

      for (int i = 0; i < numFiles; i++) {
        fm.seek(offsets[i]);

        // 4 - File Length
        int length = fm.readInt();

        // X - File Data (Zlib Compression)
        decompressBlock(inStream, outStream, length);
        TaskProgressManager.setValue(i);
      }

      TaskProgressManager.setMessage(Language.get("Progress_ReadingArchive"));

      //progress.setVisible(false);

    }
    catch (Throwable t) {
      logError(t);
    }
  }

  /**
   **********************************************************************************************
   * Determines the file type based on the object class ID
   **********************************************************************************************
   **/
  public String getExtension(String[] names, long objClass) {
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
  public int getMatchRating(FileManipulator fm, int version) {
    try {

      int rating = 0;

      if (FieldValidator.checkExtension(fm, extensions)) {
        rating += 25;
      }

      // 4 - Header
      if (fm.readByte() == -63 && fm.readByte() == -125 && fm.readByte() == 42 && fm.readByte() == -98) {
        rating += 50;
      }
      else {
        rating = 0;
      }

      // 2 - Version
      if (fm.readShort() == version) {
        rating += 5;
      }
      else if (version == -1) {
        // just leave the rating at 50
      }
      else {
        rating = 0;
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
  public int getMatchRating(FileManipulator fm, int[] versions) {
    try {

      int rating = 0;

      if (FieldValidator.checkExtension(fm, extensions)) {
        rating += 25;
      }

      // 4 - Header
      if (fm.readByte() == -63 && fm.readByte() == -125 && fm.readByte() == 42 && fm.readByte() == -98) {
        rating += 50;
      }
      else {
        rating = 0;
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

  **********************************************************************************************
  **/
  @Override
  public Resource[] read(File path) {
    try {

      FileManipulator fm = new FileManipulator(path, false);

      //long arcSize = fm.getLength();

      // 4 - Header
      fm.skip(4);

      // 2 - Version
      version = fm.readShort();
      System.out.println("This archive uses the Unreal Engine: Version " + version);

      if (version >= 300) {
        // Try reading as an Unreal Engine 3 archive.
        // If failed, will open as normal Unreal Engine 1/2 archive.
        Resource[] resources = readGenericUE3(path, fm);
        if (resources != null && resources.length > 0) {
          return resources;
        }
        else {
          fm.close();
          fm = new FileManipulator(path, false);
          fm.skip(6);
        }
      }
      return readGeneric(path, fm);

    }
    catch (Throwable t) {
      logError(t);
      return null;
    }
  }

  /**
   **********************************************************************************************
   * Reads an array index, used in properties
   **********************************************************************************************
   **/
  public int readArrayIndex(FileManipulator fm) throws Exception {

    byte byte1 = fm.readByte();
    if ((byte1 & 192) == 192) {
      // 4 bytes
      byte[] bytes = new byte[] { (byte) (byte1 & 192), fm.readByte(), fm.readByte(), fm.readByte() };
      return IntConverter.convertLittle(bytes);
    }
    else if ((byte1 & 128) == 128) {
      // 2 bytes
      byte[] bytes = new byte[] { (byte) (byte1 & 128), fm.readByte() };
      return ShortConverter.convertLittle(bytes);
    }
    else {
      // 1 byte
      return byte1;
    }

  }

  /**
   **********************************************************************************************
   * Basic Format
   **********************************************************************************************
   **/
  public Resource[] readGeneric(File path, FileManipulator fm) {
    return null;
  }

  /**
   **********************************************************************************************
   * Basic Format
   **********************************************************************************************
   **/
  public Resource[] readGenericUE3(File path, FileManipulator fm) {
    return null;
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
   * Reads a property object of the given type
   **********************************************************************************************
   **/
  public UnrealProperty readProperty0(UnrealProperty property, FileManipulator fm) {
    // Nothing
    return null;
  }

  /**
   **********************************************************************************************
   * Reads a property object of the given type
   **********************************************************************************************
   **/
  public UnrealProperty readProperty1(UnrealProperty property, FileManipulator fm) {
    // Byte

    property.setValue(fm.readByte());

    int extraSize = (int) property.getLength() - 1; // -1 for read above
    if (extraSize > 0) {
      fm.readBytes(extraSize);
    }

    return property;
  }

  /**
   **********************************************************************************************
   * Reads a property object of the given type
   **********************************************************************************************
   **/
  public UnrealProperty readProperty10(UnrealProperty property, FileManipulator fm) {
    // Structure

    property.setValue(fm.readBytes((int) property.getLength()));
    return property;
  }

  /**
   **********************************************************************************************
   * Reads a property object of the given type
   **********************************************************************************************
   **/
  public UnrealProperty readProperty11(UnrealProperty property, FileManipulator fm) {
    // Vector

    property.setValue(fm.readBytes((int) property.getLength()));
    return property;
  }

  /**
   **********************************************************************************************
   * Reads a property object of the given type
   **********************************************************************************************
   **/
  public UnrealProperty readProperty12(UnrealProperty property, FileManipulator fm) {
    // Rotator

    property.setValue(fm.readBytes((int) property.getLength()));
    return property;
  }

  /**
   **********************************************************************************************
   * Reads a property object of the given type
   **********************************************************************************************
   **/
  public UnrealProperty readProperty13(UnrealProperty property, FileManipulator fm) {
    // String (StrProperty)

    long readLength = fm.getOffset();

    try {
      // 1-5 - File Path Length [*2 unicode] (includes null)
      int stringLength = (int) readIndex(fm) - 1;

      // X - File Path (unicode)
      // 2 - null File Path terminator
      property.setValue(fm.readUnicodeString(stringLength));
      fm.skip(2);
    }
    catch (Throwable t) {
      logError(t);
    }

    readLength = fm.getOffset() - readLength;

    int extraSize = (int) property.getLength() - (int) readLength; // -readLength for read above
    if (extraSize > 0) {
      fm.readBytes(extraSize);
    }

    return property;
  }

  /**
   **********************************************************************************************
   * Reads a property object of the given type
   **********************************************************************************************
   **/
  public UnrealProperty readProperty14(UnrealProperty property, FileManipulator fm) {
    // Map

    property.setValue(fm.readBytes((int) property.getLength()));
    return property;
  }

  /**
   **********************************************************************************************
   * Reads a property object of the given type
   **********************************************************************************************
   **/
  public UnrealProperty readProperty15(UnrealProperty property, FileManipulator fm) {
    // FixedArray

    property.setValue(fm.readBytes((int) property.getLength()));
    return property;
  }

  /**
   **********************************************************************************************
   * Reads a property object of the given type
   **********************************************************************************************
   **/
  public UnrealProperty readProperty2(UnrealProperty property, FileManipulator fm) {
    // Integer

    property.setValue(fm.readInt());

    int extraSize = (int) property.getLength() - 4; // -4 for read above
    if (extraSize > 0) {
      fm.readBytes(extraSize);
    }

    return property;
  }

  /**
   **********************************************************************************************
   * Reads a property object of the given type
   **********************************************************************************************
   **/
  public UnrealProperty readProperty3(UnrealProperty property, FileManipulator fm) {
    // Boolean

    property.setValue(property.getArrayIndex());

    fm.readBytes((int) property.getLength());

    return property;
  }

  /**
   **********************************************************************************************
   * Reads a property object of the given type
   **********************************************************************************************
   **/
  public UnrealProperty readProperty4(UnrealProperty property, FileManipulator fm) {
    // Float

    property.setValue(fm.readFloat());

    int extraSize = (int) property.getLength() - 4; // -4 for read above
    if (extraSize > 0) {
      fm.readBytes(extraSize);
    }

    return property;
  }

  /**
   **********************************************************************************************
   * Reads a property object of the given type
   **********************************************************************************************
   **/
  public UnrealProperty readProperty5(UnrealProperty property, FileManipulator fm) {
    // Object Index

    long readLength = fm.getOffset();
    try {
      property.setValue(readIndex(fm));
    }
    catch (Throwable t) {
      logError(t);
    }
    readLength = fm.getOffset() - readLength;

    int extraSize = (int) property.getLength() - (int) readLength; // -readLength for read above
    if (extraSize > 0) {
      fm.readBytes(extraSize);
    }

    return property;
  }

  /**
   **********************************************************************************************
   * Reads a property object of the given type
   **********************************************************************************************
   **/
  public UnrealProperty readProperty6(UnrealProperty property, FileManipulator fm) {
    // Name Index

    long readLength = fm.getOffset();
    try {
      property.setValue(readIndex(fm));
    }
    catch (Throwable t) {
      logError(t);
    }
    readLength = fm.getOffset() - readLength;

    int extraSize = (int) property.getLength() - (int) readLength; // -readLength for read above
    if (extraSize > 0) {
      fm.readBytes(extraSize);
    }

    return property;
  }

  /**
   **********************************************************************************************
   * Reads a property object of the given type
   **********************************************************************************************
   **/
  public UnrealProperty readProperty7(UnrealProperty property, FileManipulator fm) {
    // String (StringProperty)

    property.setValue(fm.readBytes((int) property.getLength()));
    return property;
  }

  /**
   **********************************************************************************************
   * Reads a property object of the given type
   **********************************************************************************************
   **/
  public UnrealProperty readProperty8(UnrealProperty property, FileManipulator fm) {
    // Class

    property.setValue(fm.readBytes((int) property.getLength()));
    return property;
  }

  /**
   **********************************************************************************************
   * Reads a property object of the given type
   **********************************************************************************************
   **/
  public UnrealProperty readProperty9(UnrealProperty property, FileManipulator fm) {
    // Array

    property.setValue(fm.readBytes((int) property.getLength()));
    return property;
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