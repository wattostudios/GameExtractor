/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2024 wattostudios
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
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.io.FileManipulator;
import org.watto.io.buffer.ByteBuffer;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_DAT_9 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_DAT_9() {

    super("DAT_9", "DAT_9");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("In The Raven Shadow",
        "ShadowCaster");
    setExtensions("dat");
    setPlatforms("PC");

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

      // Number Of Files
      if (FieldValidator.checkNumFiles(fm.readShort())) {
        rating += 5;
      }

      long arcSize = fm.getLength();

      // Directory Offset
      if (FieldValidator.checkOffset(fm.readInt(), arcSize)) {
        rating += 5;
      }

      // Directory Length
      if (FieldValidator.checkLength(fm.readInt(), arcSize)) {
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
  
  **********************************************************************************************
  **/
  @Override
  public Resource[] read(File path) {
    try {

      addFileTypes();

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 2 - Number Of Files
      int numFiles = fm.readShort();
      FieldValidator.checkNumFiles(numFiles);

      // 4 - Directory Offset
      int dirOffset = fm.readInt();
      FieldValidator.checkOffset(dirOffset, arcSize);

      // 4 - Directory Length (Length of DetailsDirectory + FilenameDirectory)
      int dirLength = fm.readInt();
      FieldValidator.checkLength(dirLength, arcSize);

      fm.seek(dirOffset);

      // skip to the names and read them in
      int relativeNameOffset = (numFiles * 12);
      fm.skip(relativeNameOffset);
      dirLength -= relativeNameOffset;

      byte[] nameBytes = fm.readBytes(dirLength);
      FileManipulator nameFM = new FileManipulator(new ByteBuffer(nameBytes));

      // back to the start of the directory
      fm.relativeSeek(dirOffset);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      String directoryName = "";
      String previousFilename = "";
      int previousIncrement = 0;
      int realNumFiles = 0;
      for (int i = 0; i < numFiles; i++) {
        // 4 - File Offset
        int offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - File Size
        int length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - Filename Offset (relative to the start of the DetailsDirectory)
        int filenameOffset = fm.readInt();

        String filename = null;
        if (filenameOffset == 0) {
          filename = previousFilename;
        }
        else {
          filenameOffset -= relativeNameOffset;

          FieldValidator.checkOffset(filenameOffset, dirLength);

          nameFM.seek(filenameOffset);
          filename = nameFM.readNullString();
        }

        if (length == 0 && offset == 0) {
          if (filename.startsWith("start")) {
            directoryName += filename.substring(5) + "\\";
          }
          else if (filename.endsWith("start")) {
            directoryName += filename.substring(0, filename.length() - 5) + "\\";
          }
          else if (filename.startsWith("end")) {
            // remove just a single directory
            String directoryNameToRemove = filename.substring(3);
            int indexPos = directoryName.indexOf(directoryNameToRemove);
            if (indexPos > 0) {
              directoryName = directoryName.substring(0, indexPos);
            }
            else {
              directoryName = "";
            }
          }
          else if (filename.endsWith("end")) {
            // remove just a single directory
            String directoryNameToRemove = filename.substring(0, filename.length() - 3);
            int indexPos = directoryName.indexOf(directoryNameToRemove);
            if (indexPos > 0) {
              directoryName = directoryName.substring(0, indexPos);
            }
            else {
              directoryName = "";
            }
          }
          else {
            directoryName += filename + "\\";
          }
        }
        else {
          if (previousFilename.equals(filename)) {
            previousIncrement++;
          }
          else {
            previousIncrement = 0;
          }

          previousFilename = filename;

          if (previousIncrement != 0) {
            filename += "_" + previousIncrement;
          }

          filename = directoryName + filename;
        }

        //path,id,name,offset,length,decompLength,exporter
        if (offset != 0 && length != 0) {
          resources[realNumFiles] = new Resource(path, filename, offset, length);
          realNumFiles++;
        }

        TaskProgressManager.setValue(i);
      }

      nameFM.close();

      fm.close();

      resources = resizeResources(resources, realNumFiles);

      return resources;

    }
    catch (Throwable t) {
      logError(t);
      return null;
    }
  }

}
