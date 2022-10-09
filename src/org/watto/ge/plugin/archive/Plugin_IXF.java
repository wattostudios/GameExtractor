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
import org.watto.Language;
import org.watto.Settings;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.io.FileManipulator;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_IXF extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_IXF() {

    super("IXF", "IXF");

    //         read write replace rename
    setProperties(true, false, true, false);

    setExtensions("ixf", "dat");
    setGames("SimCity 3000");
    setPlatforms("PC");

    setFileTypes("spr", "Image Sprite",
        "img", "Image Sprite Information",
        "kbd", "Keyboard Accelerators");

    setTextPreviewExtensions("kbd", "txt!", "script"); // LOWER CASE

  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public int getMatchRating(FileManipulator fm) {
    try {

      int rating = 0;

      if (FieldValidator.checkExtension(fm, extensions)) {
        rating += 25;
      }

      // Header
      if (fm.readString(4).equals(new String(new byte[] { (byte) 215, (byte) 129, (byte) 195, (byte) 128 }))) {
        rating += 50;
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

      addFileTypes();

      long arcSize = (int) path.length();

      FileManipulator fm = new FileManipulator(path, false);

      // Number Of Files (guessed)
      fm.skip(16);
      int numFiles = ((fm.readInt() - 4) / 20);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      fm.seek(0);

      int realNumFiles = 0;

      // 4 - Header
      fm.seek(4);

      // Blocks of 20 - go until all 20 bytes in a row are null

      int readLength = 0;
      for (int i = 0; i < numFiles; i++) {
        // 4 - Instance ID?
        int instanceID = fm.readInt();

        // 4 - Group ID?
        int groupID = fm.readInt();

        // 4 - Type ID?
        int typeID = fm.readInt();

        // 4 - Data Offset
        int offset = fm.readInt();

        // 4 - File Length
        int length = fm.readInt();

        if (typeID == -1 && groupID == -1 && instanceID == -1 && offset == -1 && length == -1) {
          continue; // empty entry, but not end of directory
        }

        FieldValidator.checkOffset(offset, arcSize);
        FieldValidator.checkLength(length, arcSize);

        String filename = Resource.generateFilename(i);

        if (typeID == 1656347172) {
          filename += ".bmp";
        }
        else if (typeID == 539399691) {
          filename += ".txt";
        }
        else if (typeID == -1562127053) {
          filename += ".kbd";
        }
        else if (typeID == -2946045 || typeID == -500992993 || typeID == 545184782 || typeID == 570447329) {
          filename += ".tkb";
        }
        else if (typeID == 0) {
          filename += ".spr";
        }
        else if (typeID == 1) {
          filename += ".img";
        }
        else if (typeID == 573275876 || typeID == -1037347590 || typeID == -1576274320 || typeID == 570257135 || typeID == 1650221606 || typeID == -1034128142 || typeID == -1034146825 || typeID == 1650224327) {
          filename += ".bin";
        }
        else if (typeID == -2114634487) {
          filename += ".txt!"; // TXT! files
        }
        else if (typeID == -2111090262 || typeID == 1658834156 || typeID == 583990681) {
          filename += ".script";
        }
        else {
          filename += "." + typeID;
        }

        //path,id,name,offset,length,decompLength,exporter
        resources[realNumFiles] = new Resource(path, filename, offset, length);

        TaskProgressManager.setValue(readLength);
        readLength += length;
        realNumFiles++;

        if (typeID == 0 && groupID == 0 && instanceID == 0 && offset == 0 && length == 0) {
          i = numFiles;
        }
      }

      realNumFiles--;
      resources = resizeResources(resources, realNumFiles);

      numFiles = realNumFiles;

      // fix the offsets/lengths for TXT files
      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];

        if (resource.getExtension().equals("txt")) {
          long offset = resource.getOffset();
          fm.relativeSeek(offset);

          // 4 - File Length
          int length = fm.readInt();

          resource.setOffset(offset + 4);
          resource.setDecompressedLength(length);
          resource.setLength(length);

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

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @SuppressWarnings("unused")
  @Override
  public void write(Resource[] resources, File path) {
    try {

      int numFiles = resources.length;
      TaskProgressManager.setMaximum(numFiles);

      FileManipulator fm = new FileManipulator(path, true);
      FileManipulator src = new FileManipulator(new File(Settings.getString("CurrentArchive")), false);

      // 4 - Header
      fm.writeBytes(src.readBytes(4));

      TaskProgressManager.setMessage(Language.get("Progress_WritingDirectory"));
      long offset = (numFiles * 20) + 20; // +20 for the blank entry

      int paddingSize = calculatePadding(offset, 20480);

      offset += paddingSize;
      offset += 4; // +4 for the header, added after the padding calculation

      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];

        long length = resource.getDecompressedLength();

        // 4 - Instance ID?
        int instanceID = src.readInt();
        fm.writeInt(instanceID);

        // 4 - Group ID?
        int groupID = src.readInt();
        fm.writeInt(groupID);

        // 4 - Type ID?
        int typeID = src.readInt();
        fm.writeInt(typeID);

        if (instanceID == -1 && groupID == -1 && typeID == -1) {
          src.skip(8);
          fm.writeInt(-1);
          fm.writeInt(-1);

          i--; // we didn't actually process this file, so go back and process it in the next entry
          continue;
        }
        else {
          // 4 - Data Offset
          int oldOffset = src.readInt();
          fm.writeInt((int) offset);

          // 4 - File Length
          int oldLength = src.readInt();
          fm.writeInt((int) length);
        }

        offset += length;

        if (resource.getExtension().equals("txt")) {
          offset += 4; // 4-byte header for these 
        }
      }

      // Write the blank entry
      fm.writeInt(0);
      fm.writeInt(0);
      fm.writeInt(0);
      fm.writeInt(0);
      fm.writeInt(0);

      // write the padding
      for (int p = 0; p < paddingSize; p++) {
        fm.writeByte(0);
      }

      TaskProgressManager.setMessage(Language.get("Progress_WritingFiles"));

      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];

        if (resource.getExtension().equals("txt")) {
          // 4 - File Length
          fm.writeInt(resource.getDecompressedLength());

          // X - File Data
          write(resource, fm);
        }
        else {
          // X - File Data
          write(resource, fm);
        }

        TaskProgressManager.setValue(i);
      }

      src.close();

      fm.close();

    }
    catch (Throwable t) {
      logError(t);
    }
  }

}