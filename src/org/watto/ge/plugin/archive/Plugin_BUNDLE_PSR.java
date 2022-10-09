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
import org.watto.ge.plugin.exporter.Exporter_LZ4;
import org.watto.ge.plugin.resource.Resource_WAV_RawAudio;
import org.watto.io.FileManipulator;
import org.watto.io.FilenameSplitter;
import org.watto.io.converter.StringConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_BUNDLE_PSR extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_BUNDLE_PSR() {

    super("BUNDLE_PSR", "BUNDLE_PSR");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("The Lion's Song");
    setExtensions("bundle"); // MUST BE LOWER CASE
    setPlatforms("PC");

    // MUST BE LOWER CASE !!!
    setFileTypes(new FileType("tex", "TEX Image", FileType.TYPE_IMAGE));

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

      fm.skip(4);

      long arcSize = fm.getLength();

      // Archive Size
      if (FieldValidator.checkEquals(fm.readInt() + 8, arcSize)) {
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

      // 4 - Decompressed File Length
      int decompLength = fm.readInt();
      FieldValidator.checkLength(decompLength);

      // 4 - Compressed File Length
      int compLength = fm.readInt();
      FieldValidator.checkLength(compLength, arcSize);

      FileManipulator decompFM = decompressArchive(fm, compLength, decompLength);
      if (decompFM != null) {
        fm.close(); // close the original archive
        fm = decompFM; // now we're going to read from the decompressed file instead
        fm.seek(0);

        path = fm.getFile(); // So the resources are stored against the decompressed file
        arcSize = path.length();
      }

      int numFiles = Archive.getMaxFiles();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(arcSize);

      // 4 - Header (" PSR")
      fm.skip(4);

      // 4 - Number of Blocks
      int numBlocks = fm.readInt();
      FieldValidator.checkNumFiles(numBlocks);

      int realNumFiles = 0;
      for (int b = 0; b < numBlocks; b++) {

        // 4 - Block Header ("SPSR")
        fm.skip(4);

        // 4 - Block Type ("OIDA"/" XFG"/" SEG")
        String blockType = StringConverter.reverse(fm.readString(4)).trim() + "\\";

        // 4 - Number of Files in this Block
        int numFilesInBlock = fm.readInt();
        FieldValidator.checkNumFiles(numFilesInBlock);

        // Loop through directory
        for (int i = 0; i < numFilesInBlock; i++) {

          // 4 - Hash?
          fm.skip(4);

          // 4 - File Length
          int length = fm.readInt();
          FieldValidator.checkLength(length, arcSize);

          // X - File Data
          long offset = fm.getOffset();

          int skipLength = length;

          String fileType = null;
          if (length > 4) {
            fileType = "." + StringConverter.reverse(fm.readString(4)).trim();
            skipLength -= 4;
          }
          else {
            fileType = ".unk";
          }

          int channels = 0;
          int frequency = 0;
          int bitrate = 0;

          if (fileType.equals(".MUS") && length > 28) {
            fm.skip(24);
            skipLength -= 24;

            if (fm.readString(4).equals("OggS")) {
              skipLength -= 4;

              offset += 28;
              length -= 28;
              fileType = ".ogg";
            }
          }
          else if (fileType.equals(".SND") && length > 24) {
            // 4 - null
            fm.skip(4);

            // 4 - Channels
            channels = fm.readInt();

            // 4 - Frequency
            frequency = fm.readInt();

            // 4 - Bitrate
            bitrate = fm.readInt();

            // 4 - File Length
            // X - File Data

            skipLength -= 16;

            offset += 24;
            length -= 24;
            fileType = ".wav";
          }

          fm.skip(skipLength);

          String filename = blockType + Resource.generateFilename(i) + fileType;

          //path,name,offset,length,decompLength,exporter
          if (channels != 0 && frequency != 0 && bitrate != 0) {
            // raw audio
            Resource_WAV_RawAudio resource = new Resource_WAV_RawAudio(path, filename, offset, length);
            resource.setAudioProperties(frequency, bitrate, channels);
            resource.forceNotAdded(true);
            resources[realNumFiles] = resource;
          }
          else {
            // any other file
            Resource resource = new Resource(path, filename, offset, length);
            resource.forceNotAdded(true);
            resources[realNumFiles] = resource;
          }

          realNumFiles++;

          TaskProgressManager.setValue(offset);
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
   Decompressed an archive, where the whole archive is compressed.
   Reads the compressed block information first, then processes the compressed blocks themselves.
   Writes the output to a file with the same name, but with "_ge_decompressed" at the end of it.
   The decompressed file contains the same header as the compressed file, so you can open
   the decompressed file in GE directly, without needing to re-decompress anything.
   If the decompressed file already exists, we use that, we don't re-decompress.
   **********************************************************************************************
   **/
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

      // Now decompress the block into the decompressed file
      TaskProgressManager.setMessage(Language.get("Progress_DecompressingArchive")); // progress bar
      TaskProgressManager.setMaximum(999); // progress bar (set any value)
      TaskProgressManager.setIndeterminate(true);

      Exporter_LZ4 exporter = Exporter_LZ4.getInstance();
      exporter.open(fm, compLength, decompLength);

      while (exporter.available()) {
        decompFM.writeByte(exporter.read());
      }

      // Force-write out the decompressed file to write it to disk, then change the buffer to read-only.
      decompFM.close();
      decompFM = new FileManipulator(decompFile, false, 32); // small quick reads 

      TaskProgressManager.setMessage(Language.get("Progress_ReadingArchive")); // progress bar
      TaskProgressManager.setIndeterminate(false);

      return decompFM;
    }
    catch (Throwable t) {
      ErrorLogger.log(t);
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
