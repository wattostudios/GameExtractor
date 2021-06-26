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
import org.watto.datatype.Archive;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.exporter.Exporter_ZLib_CompressedSizeOnly;
import org.watto.io.FileManipulator;
import org.watto.io.FilenameSplitter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_S3DPAK extends ArchivePlugin {

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Plugin_S3DPAK() {

    super("S3DPAK", "S3DPAK");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("TimeShift");
    setExtensions("s3dpak");
    setPlatforms("PC");

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

      fm.seek(0); // return to the start, ready for decompression

      int compLength = (int) fm.getLength();

      // Now decompress the block into the decompressed file
      TaskProgressManager.setMessage(Language.get("Progress_DecompressingArchive")); // progress bar
      TaskProgressManager.setMaximum(compLength); // progress bar
      TaskProgressManager.setIndeterminate(true);

      Exporter_ZLib_CompressedSizeOnly exporter = Exporter_ZLib_CompressedSizeOnly.getInstance();
      exporter.open(fm, compLength, compLength); // decompLength isn't used by this exporter, but is included for consistency

      while (exporter.available()) {
        decompFM.writeByte(exporter.read());
      }

      // Force-write out the decompressed file to write it to disk, then change the buffer to read-only.
      decompFM.close();
      decompFM = new FileManipulator(decompFile, false);

      TaskProgressManager.setMessage(Language.get("Progress_ReadingArchive")); // progress bar
      TaskProgressManager.setIndeterminate(false);

      // Return the file pointer to the beginning, and return the decompressed file
      decompFM.seek(0);
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
      if (fm.readString(1).equals("x")) {
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

      // RESETTING THE GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      if (fm.readString(1).equals("x")) {
        /*
        fm.close();

        ExporterPlugin exporter = Exporter_ZLib_CompressedSizeOnly.getInstance();
        //decompress the file first
        FileManipulator extDir = new FileManipulator(new File("temp" + File.separator + "s3dpak_decompressed.dat"), true);
        String dirName = extDir.getFilePath();
        Resource directory = new Resource(path, dirName, 0, (int) path.length(), (int) path.length() * 20);

        exporter.extract(directory, extDir);

        extDir.close();

        path = new File(dirName);

        // important for repacking!
        Settings.set("CurrentArchive", path.getAbsolutePath());
        fm = new FileManipulator(path, false);
        */
        FileManipulator decompFM = decompressArchive(fm);
        if (decompFM != null) {
          fm.close(); // close the original archive
          fm = decompFM; // now we're going to read from the decompressed file instead
          fm.seek(0); // go to the same point in the decompressed file as in the compressed file

          path = fm.getFile(); // So the resources are stored against the decompressed file
        }
      }
      else {
        fm.seek(0);
      }

      long arcSize = (int) fm.getLength();

      int numFiles = Archive.getMaxFiles();

      Resource[] resources = new Resource[numFiles];

      TaskProgressManager.setMaximum(arcSize);

      // Loop through directory
      int realNumFiles = 0;
      for (int i = 0; i < numFiles; i++) {
        // 2 - Unknown (240)
        fm.skip(2);

        // 4 - Next File Offset
        int nextOffset = fm.readInt();
        if (nextOffset + 2 >= arcSize) {
          break;
        }
        FieldValidator.checkOffset(nextOffset, arcSize);

        // 4 - Filename Length
        int filenameLength = fm.readInt();
        FieldValidator.checkFilenameLength(filenameLength);

        // X - Filename
        String filename = fm.readString(filenameLength);
        FieldValidator.checkFilename(filename);
        filename = filename.replaceAll("#", "\\\\");
        //System.out.println(filename);

        // 4 - Unknown (4)
        fm.skip(4);
        //int unknown1 = fm.readInt();
        //System.out.println(unknown1 + "\t\t" + filename);

        // X - File Data
        long offset = fm.getOffset();

        long length = nextOffset - offset;
        FieldValidator.checkLength(length);

        fm.seek(nextOffset);

        //path,id,name,offset,length,decompLength,exporter
        resources[realNumFiles] = new Resource(path, filename, offset, length);
        resources[realNumFiles].forceNotAdded(true);

        TaskProgressManager.setValue(offset);
        realNumFiles++;
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
