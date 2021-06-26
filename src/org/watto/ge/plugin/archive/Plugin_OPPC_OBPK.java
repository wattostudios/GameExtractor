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
public class Plugin_OPPC_OBPK extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_OPPC_OBPK() {

    super("OPPC_OBPK", "OPPC_OBPK");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Warhammer 40,000 Space Marine");
    setExtensions("oppc"); // MUST BE LOWER CASE
    setPlatforms("PC");

    // MUST BE LOWER CASE !!!
    //setFileTypes(new FileType("txt", "Text Document", FileType.TYPE_DOCUMENT),
    //             new FileType("bmp", "Bitmap Image", FileType.TYPE_IMAGE)
    //             );

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

      File decompFile = new File(pathOnly + File.separatorChar + filenameOnly + "_ge_decompressed" + "." + extensionOnly);
      if (decompFile.exists()) {
        // we've already decompressed this file before - open and return it
        return new FileManipulator(decompFile, false);
      }

      FileManipulator decompFM = new FileManipulator(decompFile, true);

      long arcSize = fm.getLength();

      // 4 - Decompressed File Data Length
      int decompLength = fm.readInt();
      FieldValidator.checkLength(decompLength);

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

      // 4 - Unknown (10)
      if (fm.readInt() == 10) {
        rating += 5;
      }

      fm.skip(4);

      // Number Of Files
      if (FieldValidator.checkNumFiles(fm.readInt())) {
        rating += 5;
      }

      /*
      
      fm.skip(4);
      
      long arcSize = fm.getLength();
      
      // Directory Offset
      if (FieldValidator.checkOffset(fm.readInt(), arcSize)) {
        rating += 5;
      }
      */

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
      // 4 - Unknown (10)
      // 4 - null
      fm.skip(13);

      // 4 - Number of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // 4 - Unknown (40)
      fm.skip(4);

      // 4 - Filename Directory Offset
      int filenameDirOffset = fm.readInt();
      FieldValidator.checkOffset(filenameDirOffset, arcSize);

      // 4 - File Data Offset [+filenameDirOffset]
      int fileDataOffset = fm.readInt() + filenameDirOffset;
      FieldValidator.checkOffset(fileDataOffset, arcSize);

      // 4 - Decompressed File Data Length
      int decompDataLength = fm.readInt();
      FieldValidator.checkLength(decompDataLength);

      // 8 - null
      // 4 - Unknown (10000)
      // 4 - Unknown (50000)
      // X - Unknown
      fm.seek(fileDataOffset);

      // Decompress the data
      FileManipulator decompFM = decompressArchive(fm);

      if (decompFM != null) {
        path = decompFM.getFile();
        arcSize = path.length();

        decompFM.close();
      }

      fm.seek(filenameDirOffset);

      // 4 - number of ID Entries
      int numIDs = fm.readInt();
      for (int i = 0; i < numIDs; i++) {
        // 4 - Unknown (1)
        int count = fm.readInt();

        // 4 - Unknown
        fm.skip(count * 4);
      }

      // 4 - Number of Entries (Files + Folders)
      int numEntries = fm.readInt();
      FieldValidator.checkNumFiles(numEntries);

      String[] names = new String[numEntries];
      for (int i = 0; i < numEntries; i++) {
        // 4 - Filename Length
        int filenameLength = fm.readInt();
        FieldValidator.checkFilenameLength(filenameLength);

        // X - Filename
        names[i] = fm.readString(filenameLength);
        //System.out.println(names[i]);
      }

      // 4 - Number of Folders?
      int numFolders = fm.readInt();

      int[] numFilesInFolders = new int[numFolders];
      String[] dirNames = new String[numFolders];
      int currentEntry = 0;
      for (int i = 0; i < numFolders; i++) {
        // 8 - Hash?
        fm.skip(8);

        if (currentEntry >= numEntries) {
          dirNames[i] = "unknown";
        }
        else {
          dirNames[i] = names[currentEntry];
        }
        currentEntry++;

        // 4 - Number of Files in this Folder?
        int numFilesInFolder = fm.readInt();
        FieldValidator.checkRange(numFilesInFolder, 0, numEntries);
        numFilesInFolders[i] = numFilesInFolder;

        currentEntry += numFilesInFolder;
      }

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      long offset = 0;

      int currentDir = 0;
      String dirName = dirNames[currentDir];
      int numFilesRemainingInDir = numFilesInFolders[currentDir];

      for (int i = 0; i < numFiles; i++) {
        // move the directory to the next one, if needed
        if (numFilesRemainingInDir <= 0) {
          currentDir++;
          dirName = dirNames[currentDir];
          numFilesRemainingInDir = numFilesInFolders[currentDir];
        }

        // 4 - Filename Length
        int filenameLength = fm.readInt();
        FieldValidator.checkFilenameLength(filenameLength);

        // X - Filename
        String filename = fm.readString(filenameLength) + "." + dirName;
        numFilesRemainingInDir--;

        //System.out.println(fm.getOffset() + "\t" + filename);

        // 4 - Folder Name Length
        int folderNameLength = fm.readInt();
        FieldValidator.checkFilenameLength(folderNameLength);

        // X - Folder Name
        String folderName = "";
        if (folderNameLength == 1) {
          fm.skip(1); // root, just a / name
        }
        else {
          folderName = fm.readString(folderNameLength);
        }
        filename = folderName + filename;

        //int unknown1 = fm.readInt();
        //int unknown2 = fm.readByte();

        // 4 - File Length
        int length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - Unknown
        // 4 - File Length
        // 8 - null
        fm.skip(16);

        //int unknown3 = fm.readInt();
        //fm.skip(4);
        //int unknown4 = fm.readInt();
        //int unknown5 = fm.readInt();
        //System.out.println(unknown1 + "\t" + unknown2 + "\t" + unknown3 + "\t" + unknown4 + "\t" + unknown5 + "\t" + fm.getOffset() + "\t" + filename);

        // 1 - Extra Data Length (4/7)
        int extraLength = fm.readByte();

        // X - Extra Data
        fm.skip(extraLength);

        //path,name,offset,length,decompLength,exporter
        Resource resource = new Resource(path, filename, offset, length);
        resource.forceNotAdded(true);
        resources[i] = resource;

        offset += length;

        TaskProgressManager.setValue(i);
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
