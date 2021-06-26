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
import org.watto.datatype.Archive;
import org.watto.datatype.FileType;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.exporter.Exporter_RNC2;
import org.watto.io.FileManipulator;
import org.watto.io.FilenameSplitter;
import org.watto.io.converter.IntConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_GSC_NU20_2 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_GSC_NU20_2() {

    super("GSC_NU20_2", "GSC_NU20_2");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Crash Bandicoot: The Wrath of Cortex");
    setExtensions("gsc"); // MUST BE LOWER CASE
    setPlatforms("ps2");

    // MUST BE LOWER CASE !!!
    //setFileTypes(new FileType("txt", "Text Document", FileType.TYPE_DOCUMENT),
    //             new FileType("bmp", "Bitmap Image", FileType.TYPE_IMAGE)
    //             );

    setFileTypes(new FileType("txm0", "Texture Image", FileType.TYPE_IMAGE));

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
      String header = fm.readString(4);
      if (header.equals("NU20")) {
        rating += 50;

        // DECOMPRESSED ALREADY

        fm.skip(12);

        // Header
        if (fm.readString(4).equals("NTBL")) {
          rating += 5;
        }

        long arcSize = fm.getLength();

        // Directory Length
        if (FieldValidator.checkLength(fm.readInt(), arcSize)) {
          rating += 5;
        }

        // Directory Length
        if (FieldValidator.checkLength(fm.readInt(), arcSize)) {
          rating += 5;
        }
      }
      else if (header.substring(0, 3).equals("RNC")) {
        if (((byte) header.charAt(3)) == 2) {
          rating += 50;

          // COMPRESSED

          long arcSize = fm.getLength();

          // 4 - Decompressed Length
          if (FieldValidator.checkLength(IntConverter.changeFormat(fm.readInt()))) {
            rating += 5;
          }

          // 4 - Compressed Length
          if (FieldValidator.checkLength(IntConverter.changeFormat(fm.readInt()), arcSize)) {
            rating += 5;
          }
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

      long currentOffset = fm.getOffset();
      long arcSize = fm.getLength();

      fm.seek(0); // to fill the buffer from the start of the file, for efficient reading

      // 3 - Header (RNC)
      // 1 - Compression Mode (1/2)
      fm.skip(4); // skip the 4-byte compression header, so we can grab the decompressed size

      // 4 - Decompressed Length
      int decompLength = IntConverter.changeFormat(fm.readInt());
      FieldValidator.checkLength(decompLength);

      // 4 - Compressed Length
      int compLength = IntConverter.changeFormat(fm.readInt());
      FieldValidator.checkLength(compLength, arcSize);

      // 2 - Checksum of Decompressed Data
      // 2 - Checksum of Compressed Data
      // 1 - Unknown
      // 1 - Number of Chunks
      fm.skip(6);

      // X - Compressed Data (RNC Compression)

      // Now decompress the block into the decompressed file
      TaskProgressManager.setMessage(Language.get("Progress_DecompressingArchive")); // progress bar
      TaskProgressManager.setMaximum(arcSize); // progress bar
      TaskProgressManager.setIndeterminate(true);

      Exporter_RNC2 exporter = Exporter_RNC2.getInstance();
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

      // 4 - Header (NU20)
      if (fm.readString(3).equals("RNC")) {
        // the whole file is compressed using RNC2 - decompress it first
        if (fm.readByte() == 2) {

          FileManipulator decompFM = decompressArchive(fm);
          if (decompFM != null) {
            fm.close(); // close the original archive
            fm = decompFM; // now we're going to read from the decompressed file instead
            fm.seek(0); // go to the same point in the decompressed file as in the compressed file

            path = fm.getFile(); // So the resources are stored against the decompressed file
            fm.skip(4); // skip the 2-byte header we checked at the beginning
          }
        }
      }
      else {
        fm.skip(1); // skip the 4th byte
      }

      long arcSize = fm.getLength();

      // 4 - Unknown
      // 4 - Unknown (6)
      // 4 - null
      fm.skip(12);

      int numFiles = Archive.getMaxFiles();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(arcSize);

      int realNumFiles = 0;

      while (fm.getOffset() < arcSize) {
        // 4 - Block Header
        String blockHeader = fm.readString(4);

        //System.out.println(blockHeader + " at " + (fm.getOffset() - 4));

        // 4 - Block Length (including these 2 header fields)
        int blockLength = fm.readInt() - 8;
        FieldValidator.checkLength(blockLength, arcSize);

        long nextOffset = fm.getOffset() + blockLength;

        if (blockHeader.equals("TST0")) {
          // Texture Block

          // 4 - Number of Texture Images
          int numImages = fm.readInt();
          if (numImages == 0) {
            // skip
            fm.skip(blockLength - 4);
            continue;
          }
          FieldValidator.checkNumFiles(numImages);

          // 4 - null
          fm.skip(4);

          // Loop through directory
          while (fm.getOffset() < nextOffset) {
            //for (int i = 0; i < numImages; i++) {

            //System.out.println("Texture at " + fm.getOffset());

            // 4 - Image Data Length
            int length = fm.readInt();
            FieldValidator.checkLength(length, arcSize);

            // 4 - Padding Length
            int paddingLength = fm.readInt();
            FieldValidator.checkLength(paddingLength, arcSize);

            // X - Padding
            fm.skip(paddingLength);

            // X - Image Data
            long offset = fm.getOffset();
            fm.skip(length);

            String filename = Resource.generateFilename(realNumFiles) + ".txm0";

            if (length != 0) {
              //path,name,offset,length,decompLength,exporter
              Resource resource = new Resource(path, filename, offset, length);
              resource.forceNotAdded(true);
              resources[realNumFiles] = resource;
              realNumFiles++;
            }

            TaskProgressManager.setValue(offset);
          }

          //System.out.println(nextOffset + "\t" + fm.getOffset());

        }
        else {
          // Something else - skip it
          long offset = fm.getOffset() - 8;
          int length = blockLength + 8;
          String filename = Resource.generateFilename(realNumFiles) + "." + blockHeader.toLowerCase();

          //path,name,offset,length,decompLength,exporter
          Resource resource = new Resource(path, filename, offset, length);
          resource.forceNotAdded(true);
          resources[realNumFiles] = resource;
          realNumFiles++;

          TaskProgressManager.setValue(offset);

          fm.skip(blockLength);
        }
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
