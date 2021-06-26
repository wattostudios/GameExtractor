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
import org.watto.component.WSPluginManager;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.ge.plugin.exporter.Exporter_REFPACK;
import org.watto.io.FileManipulator;
import org.watto.io.FilenameSplitter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_CCD_FKNL extends ArchivePlugin {

  long fileDataOffset = 0;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_CCD_FKNL() {

    super("CCD_FKNL", "CCD_FKNL");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Harry Potter: Quidditch World Cup");
    setExtensions("ccd"); // MUST BE LOWER CASE
    setPlatforms("PC");

    //setFileTypes("","",
    //             "",""
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
  @SuppressWarnings("static-access")
  public FileManipulator decompressArchive(FileManipulator fm, int compLength, int decompLength) {
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

      long currentOffset = fm.getOffset();
      long arcSize = fm.getLength();

      fm.seek(fileDataOffset); // to fill the buffer from the start of the file, for efficient reading

      // Now decompress the block into the decompressed file
      TaskProgressManager.setMessage(Language.get("Progress_DecompressingArchive")); // progress bar
      TaskProgressManager.setMaximum(arcSize); // progress bar
      TaskProgressManager.setIndeterminate(true);

      Exporter_REFPACK exporter = Exporter_REFPACK.getInstance();

      // Skip the 6-byte header
      fm.skip(6);
      // Tell the decompressor that the header has already been skipped
      exporter.setReadDecompHeader(false);
      exporter.setSkipHeaders(true);

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

      // 4 - Header (FKNL)
      if (fm.readString(4).equals("FKNL")) {
        rating += 50;
      }

      fm.skip(4);

      long arcSize = fm.getLength();

      // 4 - First File Offset
      if (FieldValidator.checkOffset(fm.readInt(), arcSize)) {
        rating += 5;
      }

      fm.skip(4);

      // 4 - File Data Length
      if (FieldValidator.checkLength(fm.readInt(), arcSize)) {
        rating += 5;
      }

      // 4 - Number Of Files
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
   Provide hints to the previewer so that certain document types are displayed appropriately
   **********************************************************************************************
   **/
  @Override
  public ViewerPlugin previewHint(Resource resource) {
    String extension = resource.getExtension();
    if (extension.equalsIgnoreCase("cli") || extension.equalsIgnoreCase("gro") || extension.equalsIgnoreCase("csv")) {
      return (ViewerPlugin) WSPluginManager.getPlugin("Viewer", "TXT");
    }
    return null;
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

      // 4 - Header (FKNL)
      // 4 - Version (2)
      fm.skip(8);

      // 4 - First File Offset
      fileDataOffset = fm.readInt();
      FieldValidator.checkOffset(fileDataOffset, arcSize);

      // 4 - Decompressed File Data Length
      int decompArcLength = fm.readInt();
      FieldValidator.checkLength(decompArcLength);

      // 4 - Compressed File Data Length
      int compArcLength = fm.readInt();
      FieldValidator.checkLength(compArcLength, arcSize);

      // see if we can decompress the file data first
      FileManipulator decompFM = decompressArchive(fm, compArcLength, decompArcLength);
      if (decompFM != null) {
        arcSize = decompFM.getLength();
        path = decompFM.getFile();  // So the resources are stored against the decompressed file
        decompFM.close();
      }
      fm.seek(20);

      // 4 - Number Of Files
      // 4 - Unknown (1)
      // 4 - Unknown
      // 4 - Filename Directory Offset
      // 4 - Unknown (40)
      // 4 - Directory Offset (56)
      // 4 - Directory Length
      fm.skip(28);

      // 4 - Number Of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // 4 - null
      fm.skip(4);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      int[] filenameOffsets = new int[numFiles];
      for (int i = 0; i < numFiles; i++) {
        // 4 - Filename Offset
        int filenameOffset = fm.readInt();
        FieldValidator.checkOffset(filenameOffset, arcSize);
        filenameOffsets[i] = filenameOffset;

        // 4 - File Offset (relative to the start of the File Data)
        int offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - File Length
        int length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - File Length
        fm.skip(4);

        //path,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, offset, length);

        // force the "not added" icon, in case the original archive was decompressed
        resources[i].forceNotAdded(true);

        TaskProgressManager.setValue(i);
      }

      // get the filenames
      for (int i = 0; i < numFiles; i++) {
        fm.seek(filenameOffsets[i]);

        // X - Filename
        // 1 - null Filename Terminator
        String filename = fm.readNullString();
        FieldValidator.checkFilename(filename);
        resources[i].setName(filename);
        resources[i].setOriginalName(filename); // so it doesn't think that it's been renamed, and thus shows a normal icon
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
