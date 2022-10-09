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
import org.watto.ErrorLogger;
import org.watto.Language;
import org.watto.component.WSPluginManager;
import org.watto.datatype.Archive;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.ge.plugin.exporter.Exporter_Custom_EBO;
import org.watto.io.FileManipulator;
import org.watto.io.FilenameSplitter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_EBO_SREV extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_EBO_SREV() {

    super("EBO_SREV", "EBO_SREV");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("ArmA 3");
    setExtensions("ebo");
    setPlatforms("PC");

    setEnabled(false);

    //setFileTypes("","",
    //             "",""
    //             );

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

      // 1 - null
      if (fm.readByte() == 0) {
        rating += 5;
      }

      // 4 - Header (sreV)
      if (fm.readString(4).equals("sreV")) {
        rating += 50;
      }

      // 16 - null
      if (fm.readLong() == 0) {
        rating += 5;
      }
      if (fm.readLong() == 0) {
        rating += 5;
      }

      // 6 - Prefix Descriptor (Prefix)
      if (fm.readString(6).equalsIgnoreCase("prefix")) {
        rating += 5;
      }

      // 1 - null
      if (fm.readByte() == 0) {
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
    if (extension.equalsIgnoreCase("inc") || extension.equalsIgnoreCase("sqf") || extension.equalsIgnoreCase("sqs") || extension.equalsIgnoreCase("fsm") || extension.equalsIgnoreCase("prj") || extension.equalsIgnoreCase("hpp") || extension.equalsIgnoreCase("bikb") || extension.equalsIgnoreCase("bisurf")) {
      return (ViewerPlugin) WSPluginManager.getPlugin("Viewer", "TXT");
    }
    return null;
  }

  /**
   **********************************************************************************************
   Decompressed an archive, where the whole archive is compressed.
   Reads the compressed block information first, then processes the compressed blocks themselves.
   Writes the output to a file with the same name, but with "_ge_decompressed" at the end of it.
   The decompressed file contains the same header as the compressed file, so you can open
   the decompressed file in GE directly, without needing to re-decompress anything.
   If the decompressed file already exists, we use that, we don't re-decompress.
   **********************************************************************************************
   **/
  public FileManipulator decryptArchive(FileManipulator fm, int headerSize) {
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

      //
      // write the header out
      //
      fm.seek(0);
      decompFM.writeBytes(fm.readBytes(headerSize));

      int archiveLength = (int) fm.getLength();
      int readLength = archiveLength - headerSize;

      // Now decompress the block into the decompressed file
      TaskProgressManager.setMessage(Language.get("Progress_DecompressingArchive")); // progress bar
      TaskProgressManager.setMaximum(arcSize); // progress bar
      TaskProgressManager.setIndeterminate(true);

      Exporter_Custom_EBO exporter = Exporter_Custom_EBO.getInstance();
      exporter.open(fm, archiveLength, readLength);

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

      // 1 - null
      // 4 - Header (sreV)
      // 16 - null
      fm.skip(21);

      // X - Header (prefix/product/version)
      String header = fm.readNullString();
      while (header.length() > 0) {
        // X - Value
        fm.readNullString();

        // X - read the next Header
        header = fm.readNullString();
      }

      int headerSize = (int) fm.getOffset();

      //
      //
      // We've read all the header fields, now decrypt the rest of the archive
      //
      //
      FileManipulator decompFM = decryptArchive(fm, headerSize);
      if (decompFM != null && decompFM != fm) {
        fm.close(); // close the original archive
        fm = decompFM; // now we're going to read from the decompressed file instead
        fm.seek(headerSize); // go to the same point in the decompressed file as in the compressed file
        path = fm.getFile(); // So the resources are stored against the decompressed file
      }

      //
      //
      // Now read the archive, same as PBO_SREV
      //
      //

      int numFiles = Archive.getMaxFiles();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      int realNumFiles = 0;
      while (fm.getOffset() < arcSize) {
        // X - Filename
        // 1 - null Filename Terminator
        String filename = fm.readNullString();
        if (filename.length() == 0) {
          fm.skip(20);
          break; // end of directory
        }
        FieldValidator.checkFilename(filename);

        // 12 - null
        // 4 - Unknown
        fm.skip(16);

        // 4 - File Length
        long length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        //path,name,offset,length,decompLength,exporter
        resources[realNumFiles] = new Resource(path, filename, 0, length);

        TaskProgressManager.setValue(realNumFiles);
        realNumFiles++;
      }

      if (realNumFiles < 3) {
        return null; // not a real archive - exit so that it tries Plugin_PBO
      }

      // go back and set the offsets
      long offset = fm.getOffset();
      for (int i = 0; i < realNumFiles; i++) {
        Resource resource = resources[i];
        resource.setOffset(offset);
        offset += resource.getDecompressedLength();
      }

      resources = resizeResources(resources, realNumFiles);

      fm.close();

      return resources;

    }
    catch (Throwable t) {
      logError(t);
      return null;
    }
  }

}
