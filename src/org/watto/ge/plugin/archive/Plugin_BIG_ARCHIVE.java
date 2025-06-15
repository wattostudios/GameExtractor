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

import org.watto.component.WSPluginManager;
import org.watto.datatype.FileType;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.ge.plugin.exporter.Exporter_ZLib;
import org.watto.io.FileManipulator;
import org.watto.io.buffer.ByteBuffer;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_BIG_ARCHIVE extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_BIG_ARCHIVE() {

    super("BIG_ARCHIVE", "BIG_ARCHIVE");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Homeworld 2", "Homeworld Remastered");
    setExtensions("big"); // MUST BE LOWER CASE
    setPlatforms("PC");

    // MUST BE LOWER CASE !!!
    setFileTypes(new FileType("fda", "FDA Audio", FileType.TYPE_AUDIO));

    setTextPreviewExtensions("cloud", "dat", "dustcloud", "fp", "help", "hotspot", "lod", "ma", "madstate", "miss", "script", "shader", "st", "vp"); // LOWER CASE

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
      if (fm.readString(8).equals("_ARCHIVE")) {
        rating += 50;
      }

      fm.skip(164);

      long arcSize = fm.getLength();

      // File Data Offset
      if (FieldValidator.checkOffset(fm.readInt(), arcSize)) {
        rating += 5;
      }

      // File Data Offset
      if (FieldValidator.checkOffset(fm.readInt(), arcSize)) {
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

      ExporterPlugin exporter = Exporter_ZLib.getInstance();

      // RESETTING GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 8 - Header (_ARCHIVE)
      // 4 - Version (2)
      // 16 - Hash?
      // 128 - Description (unicode) (filled with nulls)
      // 12 - Hash?
      // 4 - File Data Offset (relative to the start of the Main Directory)
      fm.skip(176);

      // 4 - File Data Offset
      int fileDataOffset = fm.readInt();
      FieldValidator.checkOffset(fileDataOffset, arcSize);

      // 4 - Header Length (24)
      // 2 - Unknown (1)
      fm.skip(6);

      // 4 - Offset to the First Sub-Directory Entry
      int subDirOffset = fm.readInt() + 180;
      FieldValidator.checkOffset(subDirOffset, arcSize);

      // 2 - Number of Sub-Directories
      short numSubDirs = fm.readShort();
      FieldValidator.checkNumFiles(numSubDirs);

      // 4 - Offset to the First File Entry
      int fileDirOffset = fm.readInt() + 180;
      FieldValidator.checkOffset(fileDirOffset, arcSize);

      // 2 - Number of Files
      short numFiles = fm.readShort();
      FieldValidator.checkNumFiles(numFiles);

      // 4 - Offset to the Filename Directory
      int nameDirOffset = fm.readInt() + 180;
      FieldValidator.checkOffset(nameDirOffset, arcSize);

      // 2 - Number Of Filenames
      short numNames = fm.readShort();
      FieldValidator.checkNumFiles(numNames);

      // 64 - Short Directory Name ("locale" + nulls to fill)
      // 64 - Full Directory Name ("hw2locale" + nulls to fill)

      // read in the names data, for fast access later on
      int filenameDirLength = fileDataOffset - nameDirOffset;
      fm.seek(nameDirOffset);
      byte[] nameDirBytes = fm.readBytes(filenameDirLength);

      FileManipulator nameFM = new FileManipulator(new ByteBuffer(nameDirBytes));

      // Read the files
      fm.seek(fileDirOffset);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {
        // 4 - Filename Offset (relative to the start of the filename directory)
        int filenameOffset = fm.readInt();
        FieldValidator.checkOffset(filenameOffset, filenameDirLength);

        nameFM.seek(filenameOffset);
        // X - Filename
        // 1 - null Filename Terminator
        String filename = nameFM.readNullString();
        FieldValidator.checkFilename(filename);

        // 1 - Compression Flag? (32=Compressed)
        int compressionFlag = fm.readByte();

        // 4 - File Data Offset (relative to the start of the File Data)
        int offset = fm.readInt() + fileDataOffset; // don't need to skip the file header
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - Compressed Length
        int length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - Decompressed Length
        int decompLength = fm.readInt();
        FieldValidator.checkLength(decompLength);

        if (compressionFlag != 0) {
          // compressed

          //path,name,offset,length,decompLength,exporter
          resources[i] = new Resource(path, filename, offset, length, decompLength, exporter);
        }
        else {
          // not compressed

          //path,name,offset,length,decompLength,exporter
          resources[i] = new Resource(path, filename, offset, length, decompLength);
        }

        TaskProgressManager.setValue(i);
      }

      // read the sub-directories
      fm.seek(subDirOffset);

      for (int i = 0; i < numSubDirs; i++) {
        // 4 - Filename Offset (relative to the start of the filename directory)
        int filenameOffset = fm.readInt();
        FieldValidator.checkOffset(filenameOffset, filenameDirLength);

        nameFM.seek(filenameOffset);
        // X - Filename
        // 1 - null Filename Terminator
        String filename = nameFM.readNullString();
        if (filename.length() > 0) {
          filename += "\\"; // don't apply to the root
        }

        // 2 - Unknown
        // 2 - Unknown
        fm.skip(4);

        // 2 - ID of the first file in this sub-directory (fileID = 0 --> file #1)
        short firstID = fm.readShort();
        FieldValidator.checkRange(firstID, 0, numFiles);

        // 2 - ID of the last file in this sub-directory (fileID = 0 --> file #1)
        short lastID = fm.readShort();
        FieldValidator.checkRange(lastID, 0, numFiles);

        for (int f = firstID; f < lastID; f++) {
          Resource resource = resources[f];
          String name = filename + resource.getName();

          resource.setName(name);
          resource.setOriginalName(name);
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
   Provide hints to the previewer so that certain document types are displayed appropriately
   **********************************************************************************************
   **/
  @Override
  public ViewerPlugin previewHint(Resource resource) {
    String extension = resource.getExtension();
    if (extension.equalsIgnoreCase("fda")) {
      return (ViewerPlugin) WSPluginManager.getPlugin("Viewer", "VGMSTREAM_Audio_WAV_RIFF");
    }
    return null;
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
