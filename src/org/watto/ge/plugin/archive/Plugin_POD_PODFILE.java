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
public class Plugin_POD_PODFILE extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_POD_PODFILE() {

    super("POD_PODFILE", "POD_PODFILE");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Necrodome");
    setExtensions("pod"); // MUST BE LOWER CASE
    setPlatforms("PC");

    // MUST BE LOWER CASE !!!
    //setFileTypes(new FileType("txt", "Text Document", FileType.TYPE_DOCUMENT),
    //             new FileType("bmp", "Bitmap Image", FileType.TYPE_IMAGE)
    //             );

    //setTextPreviewExtensions("colours", "rat", "screen", "styles"); // LOWER CASE

    //setCanScanForFileTypes(true);

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
      if (fm.readString(8).equals("PODFILE" + (char) 0)) {
        rating += 50;
      }

      fm.skip(4);

      // Number Of Files
      if (FieldValidator.checkNumFiles(fm.readInt())) {
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
   * Reads an [archive] File into the Resources
   **********************************************************************************************
   **/
  @Override
  public Resource[] read(File path) {
    try {

      // NOTE - Compressed files MUST know their DECOMPRESSED LENGTH
      //      - Uncompressed files MUST know their LENGTH

      addFileTypes();

      //ExporterPlugin exporter = Exporter_ZLib.getInstance();

      // RESETTING GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 8 - Header ("PODFILE" + null)
      // 2 - Unknown (108)
      // 2 - Unknown (3)
      fm.skip(12);

      // 4 - Number of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // 4 - Directory Offset
      int dirOffset = fm.readInt();
      FieldValidator.checkOffset(dirOffset, arcSize);

      // 4 - Length of DetailsDirectory + FilenameDirectory
      int dirLength = fm.readInt();
      FieldValidator.checkLength(dirLength, arcSize);

      // 40 - null
      fm.seek(dirOffset);

      int relativeNameOffset = (numFiles * 32);
      fm.skip(relativeNameOffset);

      // read the filenames
      dirLength -= relativeNameOffset;

      byte[] nameBytes = fm.readBytes(dirLength);
      fm.relativeSeek(dirOffset);

      FileManipulator nameFM = new FileManipulator(new ByteBuffer(nameBytes));

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      String dirName = "";
      int realNumFiles = 0;
      for (int i = 0; i < numFiles; i++) {

        // 4 - File Offset
        int offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - File Length
        int length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - Filename Offset (relative to the start of the DetailsDirectory)
        int filenameOffset = fm.readInt();

        // 4 - File Type ID? (50=Directory)
        int typeID = fm.readInt();

        // 2 - File ID?
        short fileID = fm.readShort();

        // 2 - Entry Type? (0=Directory, 1=File)
        // 12 - null
        fm.skip(14);

        String filename = "";
        if (filenameOffset != 0) {
          filenameOffset -= relativeNameOffset;
          nameFM.seek(filenameOffset);

          // X - Filename (null)
          filename = nameFM.readNullString();
          FieldValidator.checkFilename(filename);

          if (offset == 0 && length == 0) {

          }
          else {

            String type = null;
            if (typeID == 1) {
              type = ".dat";
            }
            else if (typeID == 2) {
              type = ".pal";
            }
            else if (typeID == 3) {
              type = ".int3";
            }
            else if (typeID == 4) {
              type = ".int4";
            }
            else if (typeID == 6) {
              type = ".wall";
            }
            else if (typeID == 7) {
              type = ".tex";
            }
            else if (typeID == 12) {
              type = ".poly";
            }
            else if (typeID == 13) {
              type = ".wall13";
            }
            else if (typeID == 14) {
              type = ".spr";
            }
            else if (typeID == 18) {
              type = ".fnt";
            }
            else {
              type = "." + typeID;
            }

            filename += type;
          }

        }

        if (offset == 0 && length == 0) {
          if (filename.startsWith("start")) {
            dirName += filename.substring(5) + "\\";
          }
          else if (filename.endsWith("start")) {
            dirName += filename.substring(0, filename.length() - 5) + "\\";
          }
          else if (filename.startsWith("end")) {
            int indexPos = dirName.indexOf(filename.substring(3));
            if (indexPos > 0) {
              dirName = dirName.substring(0, indexPos);
            }
            else {
              dirName = "";
            }
          }
          else if (filename.endsWith("end")) {
            int indexPos = dirName.indexOf(filename.substring(0, filename.length() - 3));
            if (indexPos > 0) {
              dirName = dirName.substring(0, indexPos);
            }
            else {
              dirName = "";
            }
          }
        }
        else {
          filename = dirName + filename;

          //path,name,offset,length,decompLength,exporter
          Resource resource = new Resource(path, filename, offset, length);
          resource.addProperty("FileID", fileID);
          resources[realNumFiles] = resource;
          realNumFiles++;
        }

        TaskProgressManager.setValue(i);
      }

      resources = resizeResources(resources, realNumFiles);

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
  If an archive doesn't have filenames stored in it, the scanner can come here to try to work out
  what kind of file a Resource is. This method allows the plugin to provide additional plugin-specific
  extensions, which will be tried before any standard extensions.
  @return null if no extension can be determined, or the extension if one can be found
  **********************************************************************************************
  **/
  @Override
  public String guessFileExtension(Resource resource, byte[] headerBytes, int headerInt1, int headerInt2, int headerInt3, short headerShort1, short headerShort2, short headerShort3, short headerShort4, short headerShort5, short headerShort6) {

    /*
    if (headerInt1 == 2037149520) {
      return "js";
    }
    */

    return null;
  }

}
