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
import org.watto.datatype.FileType;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.exporter.Exporter_LZSS;
import org.watto.io.FileManipulator;
import org.watto.io.FilenameSplitter;
import org.watto.io.stream.LZSSOutputStream;
import org.watto.io.stream.ManipulatorOutputStream;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_LZS_LZSS extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_LZS_LZSS() {

    super("LZS_LZSS", "LZS_LZSS");

    //         read write replace rename
    setProperties(true, true, false, false);

    setGames("TOCA Race Driver 3");
    setExtensions("lzs"); // MUST BE LOWER CASE
    setPlatforms("PC");

    // MUST BE LOWER CASE !!!
    setFileTypes(new FileType("tex", "Texture Image", FileType.TYPE_IMAGE));

    setTextPreviewExtensions("idx"); // LOWER CASE

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
      if (fm.readString(4).equals("LZSS")) {
        rating += 25;
      }

      long arcSize = fm.getLength();

      // Archive Size
      if (FieldValidator.checkEquals(fm.readInt(), arcSize)) {
        rating += 5;
      }

      // Decompressed Length
      if (FieldValidator.checkLength(fm.readInt())) {
        rating += 5;
      }

      fm.skip(1);

      // Header2
      if (fm.readString(4).equals("JPAK")) {
        rating += 25;
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
  public FileManipulator decompressArchive(FileManipulator fm, long compLength, long decompLength) {
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

      // Now decompress the block into the decompressed file
      TaskProgressManager.setMessage(Language.get("Progress_DecompressingArchive")); // progress bar
      TaskProgressManager.setMaximum(arcSize); // progress bar
      TaskProgressManager.setIndeterminate(true);

      Exporter_LZSS exporter = Exporter_LZSS.getInstance();
      //exporter.open(fm, (int)compLength, (int)decompLength);
      exporter.open(fm, (int) decompLength);

      while (exporter.available()) {
        decompFM.writeByte(exporter.read());
      }

      // Force-write out the decompressed file to write it to disk, then change the buffer to read-only.
      decompFM.close();
      decompFM = new FileManipulator(decompFile, false);

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

      // 4 - Header (LZSS)
      // 4 - Archive Size
      fm.skip(8);

      // 4 - Decompressed Archive Length
      int decompLength = fm.readInt();
      FieldValidator.checkLength(decompLength);

      // X - Compressed Archive
      FileManipulator decompFM = decompressArchive(fm, arcSize - 12, decompLength);
      if (decompFM != null) {
        fm.close(); // close the original archive
        fm = decompFM; // now we're going to read from the decompressed file instead
        fm.seek(0); // go to the same point in the decompressed file as in the compressed file

        path = fm.getFile(); // So the resources are stored against the decompressed file
        arcSize = fm.getLength(); // decompressed archive length
      }

      // 4 - Header (JPAK)
      // 4 - null
      fm.skip(8);

      // 4 - Number of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // 4 - Unknown
      // 4 - null
      // 12 - Unknown
      fm.skip(20);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      int[] nameOffsets = new int[numFiles];
      for (int i = 0; i < numFiles; i++) {
        // 4 - Filename Offset
        int filenameOffset = fm.readInt();
        FieldValidator.checkOffset(filenameOffset, arcSize);
        nameOffsets[i] = filenameOffset;

        // 4 - File Length
        int length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - File Offset
        int offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize + 1); // +1 to allow empty files at the end of the archive

        // 4 - File Length
        // 8 - Unknown
        // 8 - null
        fm.skip(20);

        //path,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, "", offset, length);

        TaskProgressManager.setValue(i);
      }

      // get the filenames
      for (int i = 0; i < numFiles; i++) {
        fm.relativeSeek(nameOffsets[i]);

        // X - Filename (null)
        String filename = fm.readNullString();
        FieldValidator.checkFilename(filename);

        Resource resource = resources[i];
        resource.setName(filename);
        resource.setOriginalName(filename);
        resource.forceNotAdded(true);

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

  /**
   **********************************************************************************************
   * Writes an [archive] File with the contents of the Resources
   **********************************************************************************************
   **/
  @Override
  public void write(Resource[] resources, File path) {
    try {

      //
      //
      // Build the uncompressed file first
      //
      //
      FileManipulator fm = new FileManipulator(new File(path.getAbsolutePath() + "_ge_temp"), true);
      int numFiles = resources.length;
      TaskProgressManager.setMaximum(numFiles);

      // Calculations
      TaskProgressManager.setMessage(Language.get("Progress_PerformingCalculations"));

      long filenameOffset = 32 + numFiles * 32;

      long offset = filenameOffset;
      for (int i = 0; i < numFiles; i++) {
        offset += resources[i].getNameLength() + 1;
      }
      offset += calculatePadding(offset, 64);

      // Write Header Data

      // 4 - Header (JPAK)
      fm.writeString("JPAK");

      // 4 - null
      fm.writeInt(0);

      // 4 - Number of Files
      fm.writeInt(numFiles);

      // 4 - Padding Multiple? (64)
      fm.writeInt(64);

      // 4 - null
      fm.writeInt(0);

      // 4 - Filename Directory Offset
      fm.writeInt(filenameOffset);

      // 8 - Checksum?
      fm.writeLong(0);

      // Write Directory
      TaskProgressManager.setMessage(Language.get("Progress_WritingDirectory"));
      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];
        long decompLength = resource.getDecompressedLength();

        // 4 - Filename Offset
        fm.writeInt((int) filenameOffset);

        // 4 - File Length
        fm.writeInt((int) decompLength);

        // 4 - File Offset
        fm.writeInt((int) offset);

        // 4 - File Length
        fm.writeInt((int) decompLength);

        // 8 - Checksum?
        fm.writeLong(0);

        // 8 - null
        fm.writeLong(0);

        offset += decompLength;
        offset += calculatePadding(offset, 64);

        filenameOffset += resource.getName().length() + 1;
      }

      for (int i = 0; i < numFiles; i++) {
        // X - Filename (null)
        fm.writeString(resources[i].getName());
        fm.writeByte(0);
      }

      int paddingSize = calculatePadding(fm.getOffset(), 64);
      for (int p = 0; p < paddingSize; p++) {
        fm.writeByte(0);
      }

      // Write Files
      TaskProgressManager.setMessage(Language.get("Progress_WritingFiles"));
      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];
        write(resource, fm);

        paddingSize = calculatePadding(resource.getDecompressedLength(), 64);
        for (int p = 0; p < paddingSize; p++) {
          fm.writeByte(0);
        }

        TaskProgressManager.setValue(i);
      }

      // to force writing out the file content
      fm.close();

      // Re-open to use down below
      fm = new FileManipulator(new File(path.getAbsolutePath() + "_ge_temp"), false);

      //
      //
      // Now compress the file
      //
      //
      FileManipulator compFM = new FileManipulator(path, true);
      TaskProgressManager.setMaximum(numFiles);

      // Calculations
      TaskProgressManager.setMessage(Language.get("Progress_WritingFiles"));
      TaskProgressManager.setIndeterminate(true);

      // Write Header Data

      // 4 - Header (LZSS)
      compFM.writeString("LZSS");

      // 4 - Archive Size
      compFM.writeInt(0);

      // 4 - Decompressed Length
      compFM.writeInt((int) fm.getLength());

      // X - Compressed Archive
      ManipulatorOutputStream os = new ManipulatorOutputStream(compFM);
      LZSSOutputStream outputStream = new LZSSOutputStream(os);

      int decompLength = (int) fm.getLength();
      outputStream.write(fm.readBytes(decompLength), 0, decompLength);
      // THIS DOESN'T WORK FOR THE LZSSOutputStream, PROBABLY A BUG IN THAT CODE SOMEWHERE
      //for (int i = 0; i < decompLength; i++) {
      //  outputStream.write(new byte[] { fm.readByte() }, 0, 1);
      //}
      outputStream.close();
      os.close();

      fm.close();

      // Re-open to write the compression header (was closed by the stream closing above)
      compFM = new FileManipulator(path, true);

      compFM.seek(4);
      compFM.writeInt(compFM.getLength());
      compFM.close();

      //
      //
      //  Now delete the temporary "uncompressed" file
      //
      //
      new File(path.getAbsolutePath() + "_ge_temp").delete();

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
