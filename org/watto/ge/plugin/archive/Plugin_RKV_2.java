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
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.io.FileManipulator;
import org.watto.io.FilenameSplitter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_RKV_2 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_RKV_2() {

    super("RKV_2", "RKV_2");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Project Snowblind");
    setExtensions("rkv", "r01", "r02", "r03", "r04", "r05", "r06", "r07", "r08", "r09"); // MUST BE LOWER CASE
    setPlatforms("PC");

    // MUST BE LOWER CASE !!!
    //setFileTypes(new FileType("txt", "Text Document", FileType.TYPE_DOCUMENT),
    //             new FileType("bmp", "Bitmap Image", FileType.TYPE_IMAGE)
    //             );

    //setTextPreviewExtensions("colours", "rat", "screen", "styles"); // LOWER CASE

    setCanScanForFileTypes(true);

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

      if (!FilenameSplitter.getExtension(fm.getFile()).equalsIgnoreCase("rkv")) {
        getDirectoryFile(fm.getFile(), "rkv");
        rating += 25;
      }
      else {
        // 4 - Version (1)
        if (fm.readInt() == 1) {
          rating += 5;
        }

        long arcSize = fm.getLength();

        // 4 - First File Offset [+8]
        if (FieldValidator.checkLength(fm.readInt(), arcSize)) {
          rating += 5;
        }

        // 4 - Number of Languages
        if (FieldValidator.checkNumFiles(fm.readInt())) {
          rating += 5;
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

      // first get the RKV file
      File sourcePath = getDirectoryFile(path, "rkv");
      FileManipulator fm = new FileManipulator(sourcePath, false);

      // now work out if there are any r## files
      String filePath = sourcePath.getAbsolutePath();
      int filePathLength = filePath.length();
      String basePath = filePath.substring(0, filePathLength - 2);

      // now work out how many archives there are, and where the file offset boundaries lie
      int numArchives = 0;
      File[] archiveFiles = new File[10];
      long[] offsets = new long[10];
      long[] lengths = new long[10];
      long currentOffset = 0;

      // add the RKV file as the first one
      archiveFiles[0] = sourcePath;
      offsets[0] = 0;
      lengths[0] = sourcePath.length();
      //currentOffset += lengths[0];
      currentOffset += 1073741824; // maxSize of an archive file
      numArchives++;

      // now add the remaining r## files
      for (int i = 1; i < 10; i++) {
        File archiveFile = new File(basePath + "0" + i);
        if (!archiveFile.exists()) {
          break; // found the last file
        }
        archiveFiles[i] = archiveFile;
        offsets[i] = currentOffset;
        lengths[i] = archiveFile.length();
        //currentOffset += lengths[i];
        currentOffset += 1073741824; // maxSize of an archive file
        numArchives++;
      }

      // Now read the RKV file

      // 4 - Version (1)
      fm.skip(4);

      // 4 - First File Offset [+8]
      int dataOffset = fm.readInt() + 8;

      // 4 - Number of Languages
      int numLanguages = fm.readInt();
      FieldValidator.checkNumFiles(numLanguages);

      for (int l = 0; l < numLanguages; l++) {
        // 4 - Language Name Length
        int langLength = fm.readInt();
        FieldValidator.checkFilenameLength(langLength);

        // X - Language Name
        fm.skip(langLength);
      }

      // 4 - Number of Types
      int numTypes = fm.readInt();
      FieldValidator.checkNumFiles(numTypes);

      for (int t = 0; t < numTypes; t++) {
        // 4 - Type Name Length
        int typeLength = fm.readInt();
        FieldValidator.checkFilenameLength(typeLength);

        // X - Type Name
        fm.skip(typeLength);
      }

      // 4 - Number Of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      int realNumFiles = 0;
      for (int i = 0; i < numFiles; i++) {
        //System.out.println(fm.getOffset());

        // 4 - Entry Type ID (0/1)
        fm.skip(4);

        // 4 - Filename Length
        int filenameLength = fm.readInt();
        FieldValidator.checkFilenameLength(filenameLength);

        // X - Filename
        String filename = fm.readString(filenameLength);

        // 4 - Flags? (3)
        fm.skip(4);

        // 4 - File Offset [*2048]
        long offset = (long) fm.readInt();

        // 4 - File Length
        int length = fm.readInt();

        if (length == -1 || offset == -1) {
          continue;
        }

        offset *= 2048;
        offset += dataOffset;

        // work out which archive it's in
        int archiveNumber = -1;
        for (int a = 0; a < numArchives; a++) {
          if (offset >= offsets[a]) {
            // keep going
            archiveNumber++;
          }
          else {
            // gone too far
            break;
          }
        }

        if (archiveNumber > 0) {
          offset -= dataOffset;
        }

        File archiveFile = archiveFiles[archiveNumber];
        offset -= offsets[archiveNumber];
        long thisArcSize = lengths[archiveNumber];

        FieldValidator.checkOffset(offset, thisArcSize);
        FieldValidator.checkLength(length, thisArcSize);

        //path,name,offset,length,decompLength,exporter
        Resource resource = new Resource(archiveFile, filename, offset, length);
        resource.forceNotAdded(true);
        resources[realNumFiles] = resource;

        realNumFiles++;

        TaskProgressManager.setValue(i);
      }

      if (realNumFiles != numFiles) {
        resources = resizeResources(resources, realNumFiles);
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
  If an archive doesn't have filenames stored in it, the scanner can come here to try to work out
  what kind of file a Resource is. This method allows the plugin to provide additional plugin-specific
  extensions, which will be tried before any standard extensions.
  @return null if no extension can be determined, or the extension if one can be found
  **********************************************************************************************
  **/
  @Override
  public String guessFileExtension(Resource resource, byte[] headerBytes, int headerInt1, int headerInt2, int headerInt3, short headerShort1, short headerShort2, short headerShort3, short headerShort4, short headerShort5, short headerShort6) {

    if (headerInt2 == 44100) {
      resource.setOffset(resource.getOffset() + 2048);
      resource.setLength(resource.getLength() - 2048);
      return "ogg";
    }

    return null;
  }

}
