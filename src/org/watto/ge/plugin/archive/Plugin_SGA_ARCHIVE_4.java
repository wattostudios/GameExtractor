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
import org.watto.io.converter.IntConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_SGA_ARCHIVE_4 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_SGA_ARCHIVE_4() {

    super("SGA_ARCHIVE_4", "SGA_ARCHIVE_4");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Age Of Empires 4");
    setExtensions("sga");
    setPlatforms("PC");

    setFileTypes(new FileType("rrtex", "Relic Image", FileType.TYPE_IMAGE));

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

      // Version
      if (FieldValidator.checkEquals(fm.readInt(), 10)) {
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
   Provide hints to the previewer so that certain document types are displayed appropriately
   **********************************************************************************************
   **/
  @Override
  public ViewerPlugin previewHint(Resource resource) {
    String extension = resource.getExtension();
    if (extension.equalsIgnoreCase("lua") || extension.equalsIgnoreCase("sua") || extension.equalsIgnoreCase("mua") || extension.equalsIgnoreCase("abp") || extension.equalsIgnoreCase("scar") || extension.equalsIgnoreCase("ai") || extension.equalsIgnoreCase("bdf") || extension.equalsIgnoreCase("fnt") || extension.equalsIgnoreCase("gdf") || extension.equalsIgnoreCase("shader") || extension.equalsIgnoreCase("squadai") || extension.equalsIgnoreCase("terrainmaterial") || extension.equalsIgnoreCase("win") || extension.equalsIgnoreCase("events") || extension.equalsIgnoreCase("info") || extension.equalsIgnoreCase("options") || extension.equalsIgnoreCase("scenref") || extension.equalsIgnoreCase("prefabdata") || extension.equalsIgnoreCase("api") || extension.equalsIgnoreCase("ems") || extension.equalsIgnoreCase("str") || extension.equalsIgnoreCase("vmg")) {
      return (ViewerPlugin) WSPluginManager.getPlugin("Viewer", "TXT");
    }
    return null;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  @SuppressWarnings("unused")
  public Resource[] read(File path) {
    try {

      ExporterPlugin exporter = Exporter_ZLib.getInstance();
      addFileTypes();

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 8 - Header (_ARCHIVE)
      // 4 - Version (10)
      // 128 - Archive Name (unicode)
      fm.seek(140);

      // 8 - Directory Header Offset
      int directoryOffset = fm.readInt();
      FieldValidator.checkOffset(directoryOffset, arcSize);

      fm.seek(directoryOffset);

      int fileDataOffset = 428;

      // 4 - Descriptions Directory Offset (relative to the start of this Directory) (44)
      // 4 - Number of Descriptions (1)
      fm.skip(8);

      // 4 - Folders Directory Offset (relative to the start of this Directory)
      int folderDirOffset = fm.readInt() + directoryOffset;
      FieldValidator.checkOffset(folderDirOffset, arcSize);

      // 4 - Number of Folders
      int numFolders = fm.readInt();
      FieldValidator.checkNumFiles(numFolders);

      // 4 - Files Directory Offset (relative to the start of this Directory)
      int filesDirOffset = fm.readInt() + directoryOffset;
      FieldValidator.checkOffset(filesDirOffset, arcSize);

      // 4 - Number of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // 4 - Names Directory Offset (relative to the start of this Directory)
      int filenameDirOffset = fm.readInt() + directoryOffset;
      FieldValidator.checkOffset(filenameDirOffset, arcSize);

      // 4 - Names Directory Length
      int filenameDirLength = fm.readInt();
      FieldValidator.checkLength(filenameDirLength, arcSize);

      // 4 - End of Archive Offset (relative to the start of this Directory)
      // 4 - null
      // 4 - Unknown

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Read the filenames directory
      fm.seek(filenameDirOffset);
      byte[] nameBytes = fm.readBytes(filenameDirLength);
      FileManipulator nameFM = new FileManipulator(new ByteBuffer(nameBytes));

      // Files Directory
      fm.seek(filesDirOffset);
      for (int i = 0; i < numFiles; i++) {
        // 8 - Filename Offset (relative to the start of the FILENAME DIRECTORY)
        int filenameOffset = fm.readInt();
        fm.skip(4);
        FieldValidator.checkOffset(filenameOffset, arcSize);

        nameFM.seek(filenameOffset);
        // X - Filename (can be empty)
        // 1 - null Filename Terminator
        String filename = nameFM.readNullString();

        // 8 - File Offset (relative to start of the FILE DATA)
        long offset = IntConverter.unsign(fm.readInt()) + fileDataOffset;
        FieldValidator.checkOffset(offset, arcSize + 1);
        fm.skip(4);

        // 4 - Compressed File Size
        long length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - Decompressed File Size
        int decompLength = fm.readInt();
        FieldValidator.checkLength(decompLength);

        // 2 - Unknown
        // 4 - Hash
        fm.skip(6);

        //path,id,name,offset,length,decompLength,exporter
        if (length != decompLength) {
          resources[i] = new Resource(path, filename, offset, length, decompLength, exporter);
        }
        else {
          resources[i] = new Resource(path, filename, offset, length, decompLength);
        }

        TaskProgressManager.setValue(i);
      }

      // Read the Folders Directory
      fm.seek(folderDirOffset);
      int[] firstFilenames = new int[numFolders];
      int[] lastFilenames = new int[numFolders];
      for (int i = 0; i < numFolders; i++) {
        // 4 - Folder Name Offset (relative to the start of the FILENAME DIRECTORY)
        int folderNameOffset = fm.readInt();
        FieldValidator.checkOffset(folderNameOffset, arcSize);

        nameFM.seek(folderNameOffset);
        // X - Filename (can be empty)
        // 1 - null Filename Terminator
        String folderName = nameFM.readNullString();

        // 4 - First Sub-Folder Number
        // 4 - Last Sub-Folder Number
        // NOTE - IF THE 2 FIELDS ABOVE =36, THEY CONTAIN NO SUB-FOLDERS
        fm.skip(8);

        // 4 - First File Number
        int firstFilename = fm.readInt();
        FieldValidator.checkRange(firstFilename, 0, numFiles);

        // 4 - Last File Number
        int lastFilename = fm.readInt();
        FieldValidator.checkRange(lastFilename, 0, numFiles);

        if (folderName.length() > 0) {
          // assign names to the files
          folderName += "\\";

          for (int j = firstFilename; j < lastFilename; j++) {
            Resource resource = resources[j];
            String name = folderName + resource.getName();
            resource.setName(name);
            resource.setOriginalName(name);
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

}
