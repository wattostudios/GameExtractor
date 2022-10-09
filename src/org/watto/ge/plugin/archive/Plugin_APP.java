/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2022 wattostudios
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
import org.watto.datatype.FileType;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.exporter.Exporter_LZ77WII;
import org.watto.io.FileManipulator;
import org.watto.io.buffer.ByteBuffer;
import org.watto.io.converter.IntConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_APP extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_APP() {

    super("APP", "APP");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Wii");
    setExtensions("app"); // MUST BE LOWER CASE
    setPlatforms("Wii");

    // MUST BE LOWER CASE !!!
    setFileTypes(new FileType("tpl", "TPL Image", FileType.TYPE_IMAGE),
        new FileType("ogl", "OGL Audio", FileType.TYPE_AUDIO));

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
      if (fm.readInt() == 758688341) {
        rating += 50;
      }

      // 4 - Directory Offset (32)
      if (IntConverter.changeFormat(fm.readInt()) == 32) {
        rating += 5;
      }

      long arcSize = fm.getLength();

      // 4 - Length of Details Directory + Names Directory
      if (FieldValidator.checkOffset(IntConverter.changeFormat(fm.readInt()), arcSize)) {
        rating += 5;
      }

      // 4 - File Data Offset
      if (FieldValidator.checkOffset(IntConverter.changeFormat(fm.readInt()), arcSize)) {
        rating += 5;
      }

      return rating;

    }
    catch (Throwable t) {
      return 0;
    }
  }

  int realNumFiles = 0;

  int entriesRead = 0;

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

      ExporterPlugin exporter = Exporter_LZ77WII.getInstance();

      // RESETTING GLOBAL VARIABLES
      realNumFiles = 0;
      entriesRead = 0;

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 4 - Header ((bytes)85,170,56,45)
      // 4 - Directory Offset (32)
      fm.skip(8);

      // 4 - Length of Details Directory + Names Directory
      int nameDirLength = IntConverter.changeFormat(fm.readInt());

      // 4 - File Data Offset
      // 16 - null Padding to a multiple of 32 bytes
      fm.skip(20);

      // READ THE ROOT

      // 1 - Entry Type (0=File, 1=Directory)
      // 3 - Filename Offset (relative to the start of the Name Directory)
      // 4 - Index of the Parent Directory
      fm.skip(8);

      // 4 - Index of the End of this Directory
      int numFiles = IntConverter.changeFormat(fm.readInt());
      FieldValidator.checkNumFiles(numFiles);

      nameDirLength -= (numFiles * 12);
      FieldValidator.checkLength(nameDirLength, arcSize);

      fm.relativeSeek(32 + (numFiles * 12));

      byte[] nameBytes = fm.readBytes(nameDirLength);
      FileManipulator nameFM = new FileManipulator(new ByteBuffer(nameBytes));

      fm.relativeSeek(32);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory

      while (entriesRead < numFiles) {
        readDirectory(fm, nameFM, path, resources, arcSize, "");
      }

      resources = resizeResources(resources, realNumFiles);

      // now go through and find if any of the files are compressed...

      numFiles = realNumFiles;
      fm.getBuffer().setBufferSize(9);

      for (int i = 0; i < numFiles; i++) {
        TaskProgressManager.setValue(i);

        Resource resource = resources[i];
        long offset = resource.getOffset();

        fm.seek(offset);

        if (fm.readString(4).equals("CMPR")) {
          int compressionByte = fm.readByte();
          if (compressionByte == 16 || compressionByte == 17 || compressionByte == 40) {
            // 4 - Decompressed Length
            try {
              int decompLength = fm.readInt();
              FieldValidator.checkLength(decompLength);

              long length = resource.getLength();

              if (decompLength >= (length - 9)) { // length-9 is the data without the compression header

                resource.setOffset(offset + 4); // skip the CMPR header only
                resource.setLength(length - 4); // skip the CMPR header only
                resource.setDecompressedLength(decompLength);
                resource.setExporter(exporter);
              }
            }
            catch (Throwable t) {
            }
          }
        }
      }

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
  
   **********************************************************************************************
   **/

  public void readDirectory(FileManipulator fm, FileManipulator nameFM, File path, Resource[] resources, long arcSize, String dirName) {
    try {

      // 1 - Entry Type (0=File, 1=Directory)
      // 3 - Filename Offset (relative to the start of the Name Directory)
      byte[] filenameBytes = fm.readBytes(4);

      int entryType = filenameBytes[0];

      filenameBytes[0] = 0;
      int filenameOffset = IntConverter.convertBig(filenameBytes);

      nameFM.seek(filenameOffset);
      String filename = nameFM.readNullString();

      entriesRead++;

      if (entryType == 0) {
        // FILE

        // 4 - File Offset
        int offset = IntConverter.changeFormat(fm.readInt());
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - File Length
        int length = IntConverter.changeFormat(fm.readInt());
        FieldValidator.checkLength(length, arcSize);

        filename = dirName + filename;

        //path,name,offset,length,decompLength,exporter
        resources[realNumFiles] = new Resource(path, filename, offset, length);
        realNumFiles++;

        TaskProgressManager.setValue(realNumFiles);
      }
      else if (entryType == 1) {
        // DIRECTORY

        // 4 - Index of the Parent Directory
        fm.skip(4);

        // 4 - Index of the End of this Directory
        int endIndex = IntConverter.changeFormat(fm.readInt());

        filename = dirName + filename;
        if (filename.length() > 0) {
          filename += "\\";
        }

        while (entriesRead < endIndex) {
          readDirectory(fm, nameFM, path, resources, arcSize, filename);
        }
      }
      else {
        // UNKNOWN
        fm.skip(8);
      }

    }
    catch (Throwable t) {
      logError(t);
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
