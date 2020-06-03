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
import org.watto.component.WSPluginManager;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.ge.plugin.exporter.Exporter_ZLib;
import org.watto.io.FileManipulator;
import org.watto.io.FilenameSplitter;
import org.watto.io.converter.IntConverter;
import org.watto.io.converter.ShortConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_SGA_ARCHIVE_3 extends ArchivePlugin {

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Plugin_SGA_ARCHIVE_3() {

    super("SGA_ARCHIVE_3", "SGA_ARCHIVE_3");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Company of Heroes",
        "Company of Heroes: Legacy Edition",
        "Company of Heroes: Relaunch");
    setExtensions("sga");
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

      // Header
      if (fm.readString(8).equals("_ARCHIVE")) {
        rating += 50;
      }

      // Version
      if (FieldValidator.checkEquals(fm.readInt(), 4)) {
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

      // 8 - Header (_ARCHIVE)
      // 4 - Version (4)
      // 16 - Unknown
      // 128 - Archive Name (unicode)
      // 16 - Unknown
      // 4 - File Data Offset [+184]
      fm.seek(176);

      // 4 - File Data Offset
      int fileDataOffset = fm.readInt();

      // 4 - Unknown (1)
      // 4 - Descriptions Directory Offset [+184]
      // 2 - Number Of Descriptions (1)
      fm.skip(10);

      // 4 - Folders Directory Offset [+184]
      int folderDirOffset = fm.readInt() + 184;

      // 2 - Number Of Folders
      int numFolders = ShortConverter.unsign(fm.readShort());

      // 4 - Files Directory Offset [+184]
      long dirOffset = fm.readInt() + 184;

      // 2 - Number Of Files
      int numFiles = ShortConverter.unsign(fm.readShort());

      // 4 - Filename Directory Offset [+184]
      int filenameDirOffset = fm.readInt() + 184;

      // 2 - Number Of Filenames
      int numNames = ShortConverter.unsign(fm.readShort());

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      long arcSize = fm.getLength();

      // Files Directory
      fm.seek(dirOffset);

      long[] filenameOffsets = new long[numFiles];
      for (int i = 0; i < numFiles; i++) {
        // 4 - Filename Offset (relative to filename directory offset)
        int filenameOffset = fm.readInt() + filenameDirOffset;
        FieldValidator.checkOffset(filenameOffset, arcSize);
        filenameOffsets[i] = filenameOffset;

        // 4 - File Offset (relative to start of the file data)
        long offset = IntConverter.unsign(fm.readInt()) + fileDataOffset;
        FieldValidator.checkOffset(offset, arcSize + 1);

        // 4 - Compressed File Size
        long length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - Decompressed File Size
        int decompLength = fm.readInt();
        FieldValidator.checkLength(decompLength);

        // 4 - Hash?
        // 2 - Unknown
        fm.skip(6);

        String filename = ""; // filenames set down further

        //path,id,name,offset,length,decompLength,exporter
        if (length != decompLength) {
          resources[i] = new Resource(path, filename, offset, length, decompLength, exporter);
        }
        else {
          resources[i] = new Resource(path, filename, offset, length, decompLength);
        }

        TaskProgressManager.setValue(i);
      }

      // Go through and set all the filenames
      for (int i = 0; i < numFiles; i++) {
        fm.seek(filenameOffsets[i]);
        // X - filename
        String filename = fm.readNullString();

        Resource resource = resources[i];
        resource.setName(filename);
        resource.setOriginalName(filename);

        // SMF files are just WAV files with a 12-byte header
        if (FilenameSplitter.getExtension(filename).equalsIgnoreCase("smf")) {
          resource.setOffset(resource.getOffset() + 12);

          long length = resource.getLength() - 12;
          resource.setLength(length);
          resource.setDecompressedLength(length);
        }
      }

      // Read the Folders Directory
      fm.seek(folderDirOffset);
      long[] folderNameOffsets = new long[numFolders];
      int[] firstFilenames = new int[numFolders];
      int[] lastFilenames = new int[numFolders];
      for (int i = 0; i < numFolders; i++) {
        // 4 - Folder Name Offset (relative to filename directory offset)
        int folderNameOffset = fm.readInt() + filenameDirOffset;
        FieldValidator.checkOffset(folderNameOffset, arcSize);
        folderNameOffsets[i] = folderNameOffset;

        // 2 - First Sub-Folder Number
        // 2 - Last Sub-Folder Number
        // NOTE - IF THE 2 FIELDS ABOVE =36, THEY CONTAIN NO SUB-FOLDERS
        fm.skip(4);

        // 2 - First Filename Number
        int firstFilename = ShortConverter.unsign(fm.readShort());
        FieldValidator.checkRange(firstFilename, 0, numFiles);
        firstFilenames[i] = firstFilename;

        // 2 - Last Filename Number
        int lastFilename = ShortConverter.unsign(fm.readShort());
        FieldValidator.checkRange(lastFilename, 0, numFiles);
        lastFilenames[i] = lastFilename;
      }

      // Read the folder names and set the folders on the Resources
      for (int i = 0; i < numFolders; i++) {
        fm.seek(folderNameOffsets[i]);

        // X - filename
        String filename = fm.readNullString();

        int firstFilename = firstFilenames[i];
        int lastFilename = lastFilenames[i];

        if (filename.length() > 0) {
          // assign names to the files
          for (int j = firstFilename; j < lastFilename; j++) {
            //String filename = names[i] + names[numFolders+j];
            String name = filename + "\\" + resources[j].getName();
            Resource resource = resources[j];

            resource.setName(name);
            resource.setOriginalName(name);
          }
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

}
