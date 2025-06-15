/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2024 wattostudios
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
import java.io.IOException;
import java.util.zip.DeflaterOutputStream;

import org.watto.ErrorLogger;
import org.watto.Language;
import org.watto.datatype.Archive;
import org.watto.datatype.FileType;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.exporter.Exporter_ZLib;
import org.watto.io.FileManipulator;
import org.watto.io.FilenameSplitter;
import org.watto.io.converter.ByteArrayConverter;
import org.watto.io.converter.StringConverter;
import org.watto.io.stream.ManipulatorOutputStream;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_SPK_3 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_SPK_3() {

    super("SPK_3", "SPK_3");

    //         read write replace rename
    setProperties(true, true, false, true);

    setGames("Wolfenstein");
    setExtensions("spk", "mpk"); // MUST BE LOWER CASE
    setPlatforms("PC");

    // MUST BE LOWER CASE !!!
    setFileTypes(new FileType("txtr", "Texture Image", FileType.TYPE_IMAGE),
        new FileType("snds", "Audio Sound File", FileType.TYPE_AUDIO),
        new FileType("modl", "Model File", FileType.TYPE_OTHER),
        new FileType("ents", "Entity File", FileType.TYPE_OTHER),
        new FileType("vido", "Video File", FileType.TYPE_VIDEO));

    setTextPreviewExtensions("proc"); // LOWER CASE

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
  public FileManipulator decompressResource(FileManipulator fm, int length, int decompLength, int fileNum, String type) {
    try {
      // Build a new "_ge_decompressed" archive file in the current directory
      File origFile = fm.getFile();

      String pathOnly = FilenameSplitter.getDirectory(origFile);
      String filenameOnly = FilenameSplitter.getFilename(origFile);
      String extensionOnly = FilenameSplitter.getExtension(origFile);

      File decompFile = new File(pathOnly + File.separatorChar + filenameOnly + "_" + type + "_" + fileNum + "_ge_decompressed" + "." + extensionOnly);
      if (decompFile.exists()) {
        // we've already decompressed this file before - open and return it
        return new FileManipulator(decompFile, false);
      }

      FileManipulator decompFM = new FileManipulator(decompFile, true);

      long currentOffset = fm.getOffset();
      long arcSize = fm.getLength();

      int compLength = (int) fm.getLength();

      // Now decompress the block into the decompressed file
      TaskProgressManager.setMessage(Language.get("Progress_DecompressingArchive")); // progress bar
      TaskProgressManager.setMaximum(arcSize); // progress bar
      TaskProgressManager.setIndeterminate(true);

      Exporter_ZLib exporter = Exporter_ZLib.getInstance();
      exporter.open(fm, compLength, decompLength);

      for (int i = 0; i < decompLength; i++) {
        exporter.available();
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
      if (fm.readInt() == 300) {
        rating += 50;
      }

      fm.skip(8);

      long arcSize = fm.getLength();

      // First File Compressed Length
      if (FieldValidator.checkLength(fm.readInt(), arcSize)) {
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

      FileManipulator fm = new FileManipulator(path, false, 16);

      long arcSize = fm.getLength();

      // 4 - Unknown (300)
      fm.skip(4);

      int numFiles = Archive.getMaxFiles();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(arcSize);

      // Loop through directory
      int realNumFiles = 0;
      long[] offsets = new long[numFiles];
      long[] lengths = new long[numFiles];
      long[] decompLengths = new long[numFiles];
      String[] types = new String[numFiles];
      while (fm.getOffset() < arcSize) {

        // 4 - Header (LCED/RTXT/SDNS/etc...)
        String type = fm.readString(4);
        types[realNumFiles] = type;

        // 4 - Decompressed File Length
        int decompLength = fm.readInt();
        FieldValidator.checkLength(decompLength);
        decompLengths[realNumFiles] = decompLength;

        // 4 - Compressed File Length
        int length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);
        lengths[realNumFiles] = length;

        // X - File Data (ZLib Compression)
        long offset = fm.getOffset();
        fm.skip(length);
        offsets[realNumFiles] = offset;

        TaskProgressManager.setValue(offset);
        realNumFiles++;
      }

      // now go through each resource, decompress them...
      fm.getBuffer().setBufferSize(2048); // bigger again

      FileManipulator[] sources = new FileManipulator[realNumFiles];
      for (int i = 0; i < realNumFiles; i++) {
        fm.seek(offsets[i]);

        FileManipulator decompFM = decompressResource(fm, (int) lengths[i], (int) decompLengths[i], i, types[i]);
        if (decompFM != null) {
          sources[i] = decompFM;

        }
      }

      fm.close();

      // now process each file...
      int numSources = realNumFiles;
      realNumFiles = 0;

      for (int f = 0; f < numSources; f++) {
        fm = sources[f];
        fm.seek(0);

        path = fm.getFile(); // So the resources are stored against the decompressed file
        arcSize = fm.getLength();

        // 4 - Number of Files
        numFiles = fm.readInt();
        FieldValidator.checkNumFiles(numFiles);

        TaskProgressManager.setMaximum(numFiles);

        long offset = 0;
        int startFileNum = realNumFiles;
        String type = StringConverter.reverse(types[f]);
        for (int i = 0; i < numFiles; i++) {
          // X - Filename
          // 1 - null Filename Terminator
          String filename = fm.readNullString();
          FieldValidator.checkFilename(filename);
          filename += "." + type;

          // 4 - File Length
          int length = fm.readInt();
          FieldValidator.checkLength(length, arcSize);

          // 4 - File Length
          fm.skip(4);

          //path,name,offset,length,decompLength,exporter
          Resource resource = new Resource(path, filename, offset, length);
          resource.forceNotAdded(true);
          resources[realNumFiles] = resource;
          realNumFiles++;

          offset += length;
          offset += calculatePadding(length, 4);

          TaskProgressManager.setValue(i);
        }

        long relativeOffset = fm.getOffset();
        relativeOffset += calculatePadding(relativeOffset, 4);

        for (int i = 0; i < numFiles; i++) {
          Resource resource = resources[startFileNum + i];
          resource.setOffset(resource.getOffset() + relativeOffset);
        }

        fm.close();
      }

      resources = resizeResources(resources, realNumFiles);

      return resources;

    }
    catch (Throwable t) {
      logError(t);
      return null;
    }
  }

  /**
   **********************************************************************************************
   * Writes an [archive] File with the contents of the Resources
   **********************************************************************************************
   **/
  @Override
  public void write(Resource[] resources, File path) {
    try {

      FileManipulator fm = new FileManipulator(path, true);
      int numFiles = resources.length;
      TaskProgressManager.setMaximum(numFiles);

      TaskProgressManager.setMessage(Language.get("Progress_WritingFiles"));

      // first find all the extensions that we have, and the number of files of each extension
      String[] extensions = new String[numFiles];
      int[] extensionCount = new int[numFiles];
      boolean[] extensionFull = new boolean[numFiles];
      long[] decompLengths = new long[numFiles];
      long[] dirLengths = new long[numFiles];
      int numExtensions = 0;

      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];
        String extension = resource.getExtension();

        int decompLength = (int) resource.getDecompressedLength();
        decompLength += calculatePadding(decompLength, 4);

        int entryLength = resource.getNameLength() - 5 + 1 + 8;//-5 to remove the extension

        boolean found = false;
        for (int e = 0; e < numExtensions; e++) {
          if (extensions[e].equals(extension) && !extensionFull[e]) {
            // found it

            // see if the decompLength is small enough
            if (decompLengths[e] + decompLength >= 10500000) { // guess
              // too large, need a new group for this extension (which will be added down below)
              extensionFull[e] = true;
              break;
            }
            // otherwise, we're OK, the file length is OK so just add it to the existing one
            extensionCount[e]++;
            decompLengths[e] += decompLength;
            dirLengths[e] += entryLength;
            found = true;
            break;
          }
        }

        if (!found) {
          // add a new extension
          extensions[numExtensions] = extension;
          extensionCount[numExtensions] = 1;
          decompLengths[numExtensions] = decompLength + 4; // add the 4 null bytes at the end of the whole type directory
          dirLengths[numExtensions] = entryLength;
          numExtensions++;
        }
      }

      // add padding to the dirLengths, and append to the decompLengths
      for (int e = 0; e < numExtensions; e++) {
        dirLengths[e] += calculatePadding(dirLengths[e], 4);
        decompLengths[e] += dirLengths[e];
      }

      // Write Header Data

      // 4 - Unknown (300)
      fm.writeInt(300);

      // Write the extensions
      long[] compLengths = new long[numExtensions];
      long[] compLengthOffsets = new long[numExtensions];
      boolean[] processed = new boolean[numFiles];
      int numProcessed = 0; // for setting the progress bar only
      for (int e = 0; e < numExtensions; e++) {
        String extension = extensions[e];
        int numFilesOfType = extensionCount[e];
        long decompLength = decompLengths[e];

        String extensionResized = StringConverter.reverse(extension);
        int extensionLength = extensionResized.length();
        if (extensionLength > 4) {
          extensionResized = extensionResized.substring(0, 4);
        }
        else if (extensionLength < 4) {
          while (extensionLength < 4) {
            extensionResized += " ";
            extensionLength++;
          }
        }

        // 4 - File Type (LCED/RTXT/SDNS/etc...)
        fm.writeString(extensionResized);

        // 4 - Decompressed File Length
        fm.writeInt(decompLength);

        // 4 - Compressed File Length
        compLengthOffsets[e] = fm.getOffset();
        fm.writeInt(0);

        // X - File Data (ZLib Compression)
        long startOffset = fm.getOffset();

        // start the compression
        DeflaterOutputStream outputStream = null;
        try {
          outputStream = new DeflaterOutputStream(new ManipulatorOutputStream(fm));

          // write the directory
          // 4 - Number of Files of this Type
          outputStream.write(ByteArrayConverter.convertLittle(numFilesOfType));

          // for each file of this Type
          int bytesWritten = 0;
          for (int i = 0, j = 0; i < numFiles && j < numFilesOfType; i++) {
            if (processed[i]) {
              continue;
            }

            Resource resource = resources[i];

            if (resource.getExtension().equals(extension)) {
              // found a file of this type

              // X - Filename
              String name = resource.getName();
              name = name.substring(0, name.length() - 5); // remove the extension
              outputStream.write(ByteArrayConverter.convertLittle(name));

              // 1 - null Filename Terminator
              outputStream.write(0);

              int length = (int) resource.getDecompressedLength();

              // 4 - File Length
              outputStream.write(ByteArrayConverter.convertLittle(length));

              // 4 - File Length
              outputStream.write(ByteArrayConverter.convertLittle(length));

              bytesWritten += name.length() + 1 + 4 + 4;

              j++;
            }

          }

          // 0-3 - null Padding to a multiple of 4 bytes
          int padding = calculatePadding(bytesWritten, 4);
          for (int p = 0; p < padding; p++) {
            outputStream.write(0);
          }

          // write the files
          for (int i = 0, j = 0; i < numFiles && j < numFilesOfType; i++) {
            if (processed[i]) {
              continue;
            }

            Resource resource = resources[i];
            if (resource.getExtension().equals(extension)) {
              // found a file of this type

              // X - File Data
              ExporterPlugin fileExporter = resource.getExporter();
              fileExporter.open(resource);

              while (fileExporter.available()) {
                outputStream.write(fileExporter.read());
              }

              fileExporter.close();

              // 0-3 - null Padding to a multiple of 4 bytes
              padding = calculatePadding(resource.getDecompressedLength(), 4);
              for (int p = 0; p < padding; p++) {
                outputStream.write(0);
              }

              processed[i] = true;

              j++;

              numProcessed++;
              TaskProgressManager.setValue(numProcessed);
            }
          }

          // finished writing all the files for this type

          // 4 - null
          outputStream.write(ByteArrayConverter.convertLittle(0));

          outputStream.finish();

        }
        catch (Throwable t) {
          logError(t);
          if (outputStream != null) {
            try {
              outputStream.finish();
            }
            catch (IOException ioe) {
            }
          }
        }

        // finish up, then get ready for the next extension
        long endOffset = fm.getOffset();
        compLengths[e] = endOffset - startOffset;

      }

      // Go back and write the compLengths into the archive
      for (int e = 0; e < numExtensions; e++) {
        fm.seek(compLengthOffsets[e]);
        // 4 - Compressed File Length
        fm.writeInt(compLengths[e]);
      }

      fm.close();

    }
    catch (Throwable t) {
      logError(t);
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
