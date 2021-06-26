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
import org.watto.ge.plugin.exporter.Exporter_Custom_U_Object_Generic;
import org.watto.ge.plugin.exporter.Exporter_Custom_U_Palette_Generic;
import org.watto.ge.plugin.exporter.Exporter_Custom_U_Sound_Generic;
import org.watto.ge.plugin.exporter.Exporter_Custom_U_Texture_Generic;
import org.watto.ge.plugin.resource.Resource_Unreal;
import org.watto.io.FileManipulator;
import org.watto.io.converter.ByteConverter;
import org.watto.io.stream.ManipulatorInputStream;
import org.watto.io.stream.ManipulatorOutputStream;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_U_85 extends PluginGroup_U {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_U_85() {
    super("U_85", "Unreal Engine 3 version 85");
    setExtensions("sac");
    setGames("Clive Barkers Undying");
    setPlatforms("PC");
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public FileManipulator checkCompression(FileManipulator fm) throws WSPluginException {
    try {

      //long arcSize = fm.getFile().length();

      long origPosition = fm.getOffset();

      // 4 - Unreal Header (193,131,42,158)
      // 2 - Version (83/85)
      // 2 - License Mode (0)
      fm.skip(8);

      // 2 - Package Flags (33)
      int compression = fm.readShort();
      if ((compression & 32) == 32) {
        //compressed
      }
      else {
        // no compression
        fm.seek(origPosition);
        return fm;
      }

      // 2 - Package Flags (0)
      // 4 - Number Of Names
      // 4 - Name Directory Offset
      // 4 - Number Of Files
      // 4 - File Directory Offset
      // 4 - Number Of Types
      // 4 - Type Directory Offset
      // 16 - GUID Hash
      fm.skip(42);

      // 4 - Number Of Generations
      int numGenerations = fm.readInt();

      // for each generation
      // 4 - Number Of Names
      // 4 - Number Of Files
      fm.skip(numGenerations * 8);

      int headerLength = (int) (fm.getOffset() - origPosition);

      // check for ZLib header
      fm.skip(12);
      if (!fm.readString(1).equals("x")) {
        // not compressed
        fm.seek(origPosition);
        return fm;
      }

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
      outfm.writeBytes(fm.readBytes(headerLength));
      decompressFile(fm, outfm);

      return outfm;

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
  @Override
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
  @Override
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
  @Override
  public void decompressFile(FileManipulator fm, FileManipulator out) {
    try {

      ManipulatorInputStream inStream = new ManipulatorInputStream(fm);
      ManipulatorOutputStream outStream = new ManipulatorOutputStream(out);

      TaskProgressManager.show(1, 0, Language.get("Progress_DecompressingArchive"));

      long arcSize = fm.getLength();

      TaskProgressManager.setMaximum(arcSize);

      while (fm.getOffset() < arcSize) {
        // 4 - Compression Header (19088743)
        fm.skip(4);

        // 4 - Compressed Length
        int length = fm.readInt();

        // 4 - Decompressed Length
        fm.skip(4);

        // X - File Data (Zlib Compression)
        decompressBlock(inStream, outStream, length);
        TaskProgressManager.setValue(fm.getOffset());
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
  
  **********************************************************************************************
  **/
  @Override
  public int getMatchRating(FileManipulator fm) {
    int rating = super.getMatchRating(fm, new int[] { 85, 83 });
    return rating;
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

      FileManipulator newfm = checkCompression(fm);

      if (newfm != fm) {
        // compression was performed
        path = newfm.getFile();

        // Close all the files
        fm.close();
        newfm.close();

        // open up the decompressed file
        fm = new FileManipulator(path, false);
      }

      long arcSize = fm.getLength();

      // 4 - Header
      fm.skip(4);

      // 2 - Version
      version = fm.readShort();
      System.out.println("This archive uses the Unreal Engine: Version " + version);

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
      if (version < 64) {
        for (int i = 0; i < numNames; i++) {
          // X - Name
          // 1 - null Name Terminator
          String name = fm.readNullString();
          FieldValidator.checkFilename(name);
          names[i] = name;

          // 4 - Flags
          fm.skip(4);
        }
      }
      else {
        for (int i = 0; i < numNames; i++) {
          // 1 - Name Length (including null)
          int nameLength = ByteConverter.unsign(fm.readByte()) - 1;

          // X - Name
          names[i] = fm.readString(nameLength);

          // 1 - null Name Terminator
          // 4 - Flags
          fm.skip(5);
        }
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
          FieldValidator.checkOffset(offset, arcSize);
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

}