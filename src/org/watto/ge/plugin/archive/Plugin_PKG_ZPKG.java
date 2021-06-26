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
import org.watto.Language;
import org.watto.Settings;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.io.FileManipulator;
import org.watto.io.buffer.ByteBuffer;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_PKG_ZPKG extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_PKG_ZPKG() {

    super("PKG_ZPKG", "PKG_ZPKG");

    //         read write replace rename
    setProperties(true, false, true, false);

    setGames("Psychonauts");
    setExtensions("pkg");
    setPlatforms("PC");

    setTextPreviewExtensions("asd", "atx", "dfs", "h", "hlps", "psh", "vsh"); // LOWER CASE

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
      if (fm.readString(4).equals("ZPKG")) {
        rating += 50;
      }

      // Version (1)
      if (fm.readInt() == 1) {
        rating += 5;
      }

      long arcSize = fm.getLength();

      // First File Offset
      if (FieldValidator.checkOffset(fm.readInt(), arcSize)) {
        rating += 5;
      }

      // Number Of Files in Dir 1
      if (FieldValidator.checkNumFiles(fm.readInt())) {
        rating += 5;
      }

      // Directory 2 Offset
      if (FieldValidator.checkOffset(fm.readInt(), arcSize)) {
        rating += 5;
      }

      // Number Of Files in Dir 2
      if (FieldValidator.checkNumFiles(fm.readInt())) {
        rating += 5;
      }

      return rating;

    }
    catch (Throwable t) {
      return 0;
    }
  }

  /**
   **********************************************************************************************
   * Reads an [archive] File into the Resources
   **********************************************************************************************
   **/
  @Override
  public Resource[] read(File path) {
    try {

      // NOTE - Compressed file MUST know their DECOMPRESSED LENGTH
      //      - Uncompressed files MUST know their LENGTH

      addFileTypes();

      // RESETTING THE GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = (int) fm.getLength();

      // 4 - Header (ZPKG)
      // 4 - Version (1)
      fm.skip(8);

      // 4 - Padding Offset
      int paddingOffset = fm.readInt();
      FieldValidator.checkOffset(paddingOffset, arcSize);

      // 4 - Number Of Files In Directory 1
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);
      if (numFiles < 10) {
        return null; // so it errors-out some other files that pick this up
      }

      // 4 - Directory 2 Offset
      int foldersOffset = fm.readInt();
      FieldValidator.checkOffset(foldersOffset, arcSize);

      // 4 - Number Of Files In Directory 2
      int numFolders = fm.readInt();
      FieldValidator.checkNumFiles(numFolders);

      // 4 - Filename Directory Offset
      int filenameDirOffset = fm.readInt();
      FieldValidator.checkOffset(filenameDirOffset, arcSize);

      // 4 - Extension Directory Offset
      int extDirOffset = fm.readInt();
      FieldValidator.checkOffset(extDirOffset, arcSize);

      // Read in the Filename Directory
      int filenameDirLength = extDirOffset - filenameDirOffset;
      FieldValidator.checkLength(filenameDirLength, arcSize);

      fm.seek(filenameDirOffset);
      byte[] filenameDirBytes = fm.readBytes(filenameDirLength);

      FileManipulator nameFM = new FileManipulator(new ByteBuffer(filenameDirBytes));

      // Read in the Extension Directory
      int extDirLength = paddingOffset - extDirOffset;
      FieldValidator.checkLength(extDirLength, arcSize);

      fm.seek(extDirOffset);
      byte[] extDirBytes = fm.readBytes(extDirLength);

      FileManipulator extFM = new FileManipulator(new ByteBuffer(extDirBytes));

      // Process the Folders Directory
      fm.seek(foldersOffset);

      String[] pathNames = new String[1000];
      int[][] pathIDs = new int[1000][2];
      int numPaths = 0;

      String[] returnNames = new String[100];
      int[] returnIDs = new int[100];
      int numReturns = 0;

      String currentName = "";

      for (int i = 0; i < numFolders; i++) {
        // 1 - Letter
        char nextLetter = (char) fm.readByte();

        // 1 - null
        fm.skip(1);

        // 2 - Return Pos 1
        int returnPos1 = fm.readShort();
        if (returnPos1 != 0) {
          returnNames[numReturns] = currentName;
          returnIDs[numReturns] = returnPos1;
          numReturns++;
        }

        // 2 - Return Pos 2
        int returnPos2 = fm.readShort();
        if (returnPos2 != 0) {
          returnNames[numReturns] = currentName;
          returnIDs[numReturns] = returnPos2;
          numReturns++;
        }

        currentName += nextLetter; // needs to be done AFTER the return pos

        // 2 - Character ID (incremental from 1)
        fm.skip(2);

        // 2 - First File ID in this Folder
        int startIndex = fm.readShort();

        // 2 - Last File ID in this Folder
        int endIndex = fm.readShort();

        if (startIndex != 0 && endIndex != 0) {
          pathNames[numPaths] = currentName;
          pathIDs[numPaths] = new int[] { startIndex, endIndex };
          numPaths++;
          //System.out.println("Directory " + currentName + " for files " + startIndex + "-" + endIndex);
        }

        // process the returns now
        int thisID = i + 1;
        for (int r = 0; r < numReturns; r++) {
          if (returnIDs[r] == thisID) {
            // found one
            currentName = returnNames[r];

            // shuffle the names array to remove this entry
            if (r != numReturns - 1) {
              returnIDs[r] = returnIDs[numReturns - 1];
              returnNames[r] = returnNames[numReturns - 1];
            }

            numReturns--;
          }
        }
      }

      // Go back and process the directory
      fm.seek(512);

      Resource[] resources = new Resource[numFiles];

      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {

        // 1 - null
        fm.skip(1);

        // 2 - File Extension Offset (relative to the start of the Extension Directory)
        int extOffset = fm.readShort();
        FieldValidator.checkOffset(extOffset, extDirLength);

        extFM.seek(extOffset);
        String extension = extFM.readNullString();

        // 1 - null
        fm.skip(1);

        // 4 - Filename Offset (relative to the start of the Filename Directory)
        int nameOffset = fm.readInt();
        FieldValidator.checkOffset(nameOffset, filenameDirLength);

        nameFM.seek(nameOffset);
        String filename = nameFM.readNullString() + "." + extension;

        // 4 - File Offset
        long offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - File Length
        long length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        //path,id,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, filename, offset, length);

        TaskProgressManager.setValue(i);
      }

      // Now apply the folder paths to each file
      for (int p = 0; p < numPaths; p++) {
        String pathName = pathNames[p] + "/";

        int startIndex = pathIDs[p][0]--; // convert IDs to start at 0 instead of 1
        int endIndex = pathIDs[p][1]--; // convert IDs to start at 0 instead of 1

        for (int i = startIndex; i < endIndex; i++) {
          Resource resource = resources[i];
          String name = pathName + resource.getName();
          resource.setName(name);
          resource.setOriginalName(name);
        }
      }

      extFM.close();
      nameFM.close();

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
   * Writes an [archive] File with the contents of the Resources. The archive is written using
   * data from the initial archive - it isn't written from scratch.
   **********************************************************************************************
   **/
  @Override
  public void replace(Resource[] resources, File path) {
    try {

      FileManipulator fm = new FileManipulator(path, true);
      FileManipulator src = new FileManipulator(new File(Settings.getString("CurrentArchive")), false);

      int numFiles = resources.length;
      TaskProgressManager.setMaximum(numFiles);

      // Write Header Data

      // 4 - Header (ZPKG)
      // 4 - Version (1)
      // 4 - Padding Offset
      // 4 - Number Of Files
      // 4 - Folder Directory Offset
      // 4 - Number Of Letters
      // 4 - Filename Directory Offset
      // 4 - Extension Directory Offset
      // 480 - null Padding to offset 512
      fm.writeBytes(src.readBytes(512));

      // Write Directory
      TaskProgressManager.setMessage(Language.get("Progress_WritingDirectory"));
      long offset = 524288;
      for (int i = 0; i < numFiles; i++) {
        Resource fd = resources[i];
        long length = fd.getDecompressedLength();

        // 1 - null
        // 2 - File Extension Offset (relative to the start of the Extension Directory)
        // 1 - null
        // 4 - Filename Offset (relative to the start of the Filename Directory)
        fm.writeBytes(src.readBytes(8));

        // 4 - File Offset
        fm.writeInt(offset);

        // 4 - File Length
        fm.writeInt(length);

        src.skip(8);

        offset += length;
        offset += calculatePadding(length, 512); // add the padding (to a multiple of 512 bytes)
      }

      // FOLDER DIRECTORY
      // FILENAME DIRECTORY
      // EXTENSION DIRECTORY  
      // PADDING
      int remainingBytes = (int) (524288 - fm.getOffset());
      fm.writeBytes(src.readBytes(remainingBytes));

      // Write Files
      TaskProgressManager.setMessage(Language.get("Progress_WritingFiles"));
      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];

        // X - File Data
        write(resource, fm);

        // 0-511 - null Padding to a multiple of 512 bytes
        int paddingSize = calculatePadding(resource.getDecompressedLength(), 512);
        for (int p = 0; p < paddingSize; p++) {
          fm.writeByte(0);
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
