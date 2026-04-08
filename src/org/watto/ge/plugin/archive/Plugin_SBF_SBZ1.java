/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2026 wattostudios
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
import java.util.Arrays;

import org.watto.ErrorLogger;
import org.watto.Language;
import org.watto.datatype.FileType;
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
public class Plugin_SBF_SBZ1 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_SBF_SBZ1() {

    super("SBF_SBZ1", "SBF_SBZ1");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("SEGA Rally Revo");
    setExtensions("sbf"); // MUST BE LOWER CASE
    setPlatforms("PC");

    // MUST BE LOWER CASE !!!
    setFileTypes(new FileType("mesh", "3D Model", FileType.TYPE_MODEL));

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
      int header = fm.readInt();
      if (header == 827998803) { // "SBZ1"
        rating += 50;

        // Decomp Length
        if (FieldValidator.checkLength(fm.readInt())) {
          rating += 5;
        }

        // ZLib Header
        if (fm.readByte() == 120) {
          rating += 5;
        }
      }
      else if (header > 0 || header <= 20) { // already decompressed
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
   Decompressed an archive, where the whole archive is compressed.
   Reads the compressed block information first, then processes the compressed blocks themselves.
   Writes the output to a file with the same name, but with "_ge_decompressed" at the end of it.
   The decompressed file contains the same header as the compressed file, so you can open
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

      // 4 bytes - Decompressed Size
      int decompLength = fm.readInt();
      int compLength = (int) fm.getLength() - 8;

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

      // 4 - Header (SBZ1)
      int header = fm.readInt();
      if (header == 827998803) { // "SBZ1"

        // decompress the archive first

        FileManipulator decompFM = decompressArchive(fm);
        if (decompFM != null) {
          fm.close(); // close the original archive
          fm = decompFM; // now we're going to read from the decompressed file instead
          fm.seek(0); // go to the same point in the decompressed file as in the compressed file

          path = fm.getFile(); // So the resources are stored against the decompressed file
        }
      }
      else {
        // uncompressed file
        fm.relativeSeek(0);
      }

      long arcSize = fm.getLength();

      // 4 - Unknown (4)
      // 16 - null
      fm.skip(20);

      // 4 - Number of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      int[] offsets = new int[numFiles + 1]; // +1 to allow us to add the arcSize for calculating file lengths later on

      for (int i = 0; i < numFiles; i++) {
        // 4 - Hash?
        fm.skip(4);

        // 4 - File Offset
        int offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);
        offsets[i] = offset;

        TaskProgressManager.setValue(i);
      }

      // add the arcSize so we can calculate file lengths
      offsets[numFiles] = (int) arcSize;

      Arrays.sort(offsets);

      for (int i = 0; i < numFiles; i++) {
        long offset = offsets[i];
        long length = offsets[i + 1] - offset;
        String filename = Resource.generateFilename(i);

        //path,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, filename, offset, length);

        TaskProgressManager.setValue(i);
      }

      // small quick reads to determine each file type
      fm.getBuffer().setBufferSize(32);
      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];

        long offset = resource.getOffset();
        long length = resource.getDecompressedLength();

        fm.seek(offset);

        // 4 - File Type? (4/5)
        int fileType = fm.readInt();

        // 4 - Hash? (same as in the Directory)
        // 4 - File Length
        // 4 - null
        // 4 - Unknown (0/2)
        // 4 - Unknown (0/8)
        // 4 - Unknown (1/12)
        // 4 - null
        // X - File Data

        offset += 32;
        length -= 32;

        String fileTypeString = null;
        if (fileType == 4) {
          offset += 36;
          length -= 36;

          fileTypeString = ".dds";
        }
        else if (fileType == 1) {
          fileTypeString = ".mesh";
        }
        else {
          fileTypeString = "." + fileType;
        }

        resource.setOffset(offset);
        resource.setLength(length);
        resource.setDecompressedLength(length);

        String name = resource.getName() + fileTypeString;
        resource.setName(name);
        resource.setOriginalName(name);

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
