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
import org.watto.ErrorLogger;
import org.watto.Language;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.exporter.Exporter_ZLib;
import org.watto.io.FileManipulator;
import org.watto.io.FilenameSplitter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_OPPC_OBPK_2 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_OPPC_OBPK_2() {

    super("OPPC_OBPK_2", "OPPC_OBPK_2");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Darksiders");
    setExtensions("oppc"); // MUST BE LOWER CASE
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
   Decompressed an archive, where the whole archive is compressed.
   Reads the compressed block information first, then processes the compressed blocks themselves.
   Writes the output to a file with the same name, but with "_ge_decompressed" at the end of it.
   The decompressed file contains the same Unreal header as the compressed file, so you can open
   the decompressed file in GE directly, without needing to re-decompress anything.
   If the decompressed file already exists, we use that, we don't re-decompress.
   **********************************************************************************************
   **/
  public FileManipulator decompressArchive(FileManipulator fm) {
    try {
      // Build a new "_ge_decompressed" archive file in the current directory
      File origFile = fm.getFile();

      String pathOnly = FilenameSplitter.getDirectory(origFile);
      String filenameOnly = FilenameSplitter.getFilename(origFile);
      String extensionOnly = FilenameSplitter.getExtension(origFile);

      // 4 - Decompressed File Data Length
      int decompLength = fm.readInt();
      FieldValidator.checkLength(decompLength);

      File decompFile = new File(pathOnly + File.separatorChar + filenameOnly + "_ge_decompressed" + "." + extensionOnly);
      if (decompFile.exists() && decompFile.length() == decompLength) { // IMPORTANT - CHECKS THE DECOMP LENGTH AS WELL
        // we've already decompressed this file before - open and return it
        return new FileManipulator(decompFile, false);
      }

      FileManipulator decompFM = new FileManipulator(decompFile, true);

      long arcSize = fm.getLength();

      long currentOffset = fm.getOffset();

      int compLength = (int) (arcSize - currentOffset);

      // Now decompress the block into the decompressed file
      TaskProgressManager.setMessage(Language.get("Progress_DecompressingArchive")); // progress bar
      TaskProgressManager.setMaximum(arcSize); // progress bar
      TaskProgressManager.setIndeterminate(true);

      Exporter_ZLib exporter = Exporter_ZLib.getInstance();
      exporter.open(fm, compLength, decompLength);

      while (exporter.available()) {
        decompFM.writeByte(exporter.read());
      }

      // Force-write out the decompressed file to write it to disk, then change the buffer to read-only.
      decompFM.close();
      decompFM = new FileManipulator(decompFile, false);

      TaskProgressManager.setMessage(Language.get("Progress_ReadingArchive")); // progress bar
      TaskProgressManager.setIndeterminate(false);

      // Return the file pointer to the beginning, and return the decompressed file
      decompFM.seek(currentOffset);
      return decompFM;
    }
    catch (Throwable t) {
      ErrorLogger.log(t);
      return null;
    }
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
      if (fm.readString(4).equals("OBPK")) {
        rating += 50;
      }

      fm.skip(1);

      if (fm.readInt() == 6) {
        rating += 5;
      }

      // Number Of Files
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

      // NOTE - Compressed files MUST know their DECOMPRESSED LENGTH
      //      - Uncompressed files MUST know their LENGTH

      addFileTypes();

      //ExporterPlugin exporter = Exporter_ZLib.getInstance();

      // RESETTING GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 4 - Header (OBPK)
      // 1 - null
      // 4 - Unknown (6)
      // 4 - Unknown
      // 4 - Unknown
      // 1 - null
      // 4 - Unknown
      // 4 - Unknown
      fm.skip(26);

      // 4 - Number of Pre-Folders (can be null)
      int numPreFolders = fm.readInt();
      FieldValidator.checkNumFiles(numPreFolders + 1); // +1 to allow nulls

      for (int i = 0; i < numPreFolders; i++) {
        // 8 - Name Hash
        fm.skip(8);

        // 4 - Pre-Folder Name Length
        int nameLength = fm.readInt();
        FieldValidator.checkFilenameLength(nameLength);

        // X - Pre-Folder Name
        fm.skip(nameLength);
      }

      // 4 - Number of Names
      int numNames = fm.readInt();
      FieldValidator.checkNumFiles(numNames);

      long[] nameHashes = new long[numNames];
      String[] names = new String[numNames];
      for (int i = 0; i < numNames; i++) {
        // 8 - Name Hash
        long nameHash = fm.readLong();

        // 4 - Filename Length
        int nameLength = fm.readInt();
        FieldValidator.checkFilenameLength(nameLength);

        // X - Filename
        String name = fm.readString(nameLength);

        nameHashes[i] = nameHash;
        names[i] = name;
      }

      // 4 - Directory Tree Length
      int dirTreeLength = fm.readInt();
      FieldValidator.checkLength(dirTreeLength, arcSize);

      // X - Unknown
      fm.skip(dirTreeLength);

      // 4 - Number of Folders
      int numFolders = fm.readInt();
      FieldValidator.checkNumFiles(numFolders);

      int[] numFilesInFolders = new int[numFolders];
      String[] folderNames = new String[numFolders];

      int numFiles = 0;
      for (int i = 0; i < numFolders; i++) {
        // 8 - Name Hash
        long nameHash = fm.readLong();

        String name = "";
        for (int h = 0; h < numNames; h++) {
          if (nameHash == nameHashes[h]) {
            name = names[h];
            break;
          }
        }

        // 4 - Number of Files in this Folder
        int numFilesInFolder = fm.readInt();
        FieldValidator.checkNumFiles(numFilesInFolder);

        numFilesInFolders[i] = numFilesInFolder;
        folderNames[i] = name;

        numFiles += numFilesInFolder;
      }

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      int currentFile = 0;
      long offset = 0;
      for (int i = 0; i < numFolders; i++) {
        int numFilesInFolder = numFilesInFolders[i];
        String extension = "." + folderNames[i];

        for (int j = 0; j < numFilesInFolder; j++) {
          // 8 - Name Hash
          long nameHash = fm.readLong();

          String name = "";
          for (int h = 0; h < numNames; h++) {
            if (nameHash == nameHashes[h]) {
              name = names[h];
              break;
            }
          }

          // 4 - File Length
          int length = fm.readInt();
          FieldValidator.checkLength(length);

          // 1 - Flags
          int flags = fm.readByte();

          // 4 - Unknown
          // 4 - null
          fm.skip(8);

          if ((flags & 2) == 2) {
            // 3 - Unknown
            fm.skip(3);
          }

          if ((flags & 8) == 8) {
            // 8 - Unknown
            fm.skip(8);
          }

          String filename = name + extension;

          //path,name,offset,length,decompLength,exporter
          resources[currentFile] = new Resource(path, filename, offset, length);

          TaskProgressManager.setValue(currentFile);
          currentFile++;

          offset += length;
        }
      }

      // X - null Padding to a multiple of 4096 bytes
      fm.skip(calculatePadding(fm.getOffset(), 4096));

      // Decompress the data
      FileManipulator decompFM = decompressArchive(fm);

      if (decompFM != null) {
        path = decompFM.getFile();
        arcSize = path.length();

        decompFM.close();
      }

      // go through and set the path to the decompressed file
      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];
        resource.setSource(path);
        resource.forceNotAdded(true);
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

    /*
    if (headerInt1 == 2037149520) {
      return "js";
    }
    */

    return null;
  }

}
