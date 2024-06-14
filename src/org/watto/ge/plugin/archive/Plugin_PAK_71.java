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
import org.watto.Language;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.exporter.BlockExporterWrapper;
import org.watto.ge.plugin.exporter.Exporter_LZO_MiniLZO;
import org.watto.io.FileManipulator;
import org.watto.io.StringHelper;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_PAK_71 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_PAK_71() {

    super("PAK_71", "PAK_71");

    //         read write replace rename
    setProperties(true, true, false, false);

    setGames("Dead to Rights 2",
        "Pirates: Legend of the Black Buccaneer");
    setExtensions("pak"); // MUST BE LOWER CASE
    setPlatforms("PC", "PS2");

    // MUST BE LOWER CASE !!!
    //setFileTypes(new FileType("txt", "Text Document", FileType.TYPE_DOCUMENT),
    //             new FileType("bmp", "Bitmap Image", FileType.TYPE_IMAGE)
    //             );

    setTextPreviewExtensions("scr", "met", "vfx"); // LOWER CASE

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
      if (fm.readInt() == -185986958) {
        rating += 50;
      }

      if (fm.readInt() == 0) {
        rating += 5;
      }

      if (fm.readInt() == 16) {
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

      Exporter_LZO_MiniLZO exporter = Exporter_LZO_MiniLZO.getInstance();
      exporter.setCheckDecompressedLength(false);
      exporter.setUseActualDecompressedLength(true);

      // RESETTING GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 4 - Header (-185986958)
      // 4 - null
      fm.skip(8);

      // 4 - Directory Offset (16)
      int dirOffset = fm.readInt();
      FieldValidator.checkOffset(dirOffset, arcSize);

      // 4 - Number of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      fm.relativeSeek(dirOffset);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      byte terminatorByte = (byte) 10;

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {
        // X - Filename
        // 1 - Filename Terminator (byte 10)
        String filename = StringHelper.readTerminatedString(fm.getBuffer(), terminatorByte);
        FieldValidator.checkFilename(filename);

        // 4 - File Offset
        int offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - Decompressed File Length
        int decompLength = fm.readInt();
        FieldValidator.checkLength(decompLength);

        // 4 - Compressed File Length (0 = no compression)
        int length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        String originalName = filename;
        String filenameCheck = filename.toLowerCase();

        if (filenameCheck.startsWith("s:\\") || filenameCheck.startsWith("s:/")) {
          filename = filename.substring(3);
        }

        Resource resource;
        if (length == 0) {
          //path,name,offset,length,decompLength,exporter
          resource = new Resource(path, filename, offset, decompLength);
        }
        else {
          //path,name,offset,length,decompLength,exporter
          /*
          if (length == 2048) {
            resource = new Resource(path, filename, offset, length, decompLength, exporter);
          }
          else {
          */
          // split into blocks of 2048
          int numBlocks = length / 2048;
          int blockOffset = offset;
          long[] blockOffsets = new long[numBlocks];
          long[] blockLengths = new long[numBlocks];
          long[] blockDecompLengths = new long[numBlocks];
          for (int b = 0; b < numBlocks; b++) {
            blockOffsets[b] = blockOffset;
            blockLengths[b] = 2048;
            blockDecompLengths[b] = 40960; // allowing max 20x compression - guess
            blockOffset += 2048;
          }
          BlockExporterWrapper blockExporter = new BlockExporterWrapper(exporter, blockOffsets, blockLengths, blockDecompLengths);
          resource = new Resource(path, filename, offset, length, decompLength, blockExporter);
        }

        /*}*/

        resource.addProperty("OriginalName", originalName);

        resources[i] = resource;

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

      FileManipulator fm = new FileManipulator(path, true);
      int numFiles = resources.length;
      TaskProgressManager.setMaximum(numFiles);

      // Calculations
      TaskProgressManager.setMessage(Language.get("Progress_PerformingCalculations"));

      long headerSize = 16;
      long directorySize = 0;
      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];

        String originalName = resource.getProperty("OriginalName");
        if (originalName == null || originalName.equals("")) {
          originalName = resource.getName();
        }

        directorySize += 12 + originalName.length() + 1;
      }

      // Write Header Data

      // 4 - Header (-185986958)
      fm.writeInt(-185986958);

      // 4 - null
      fm.writeInt(0);

      // 4 - Directory Offset (16)
      fm.writeInt(16);

      // 4 - Number of Files
      fm.writeInt(numFiles);

      // Write Directory
      TaskProgressManager.setMessage(Language.get("Progress_WritingDirectory"));
      long offset = headerSize + directorySize;
      byte terminatorByte = (byte) 10;
      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];
        long decompLength = resource.getDecompressedLength();

        String originalName = resource.getProperty("OriginalName");
        if (originalName == null || originalName.equals("")) {
          originalName = resource.getName();
        }

        // X - Filename
        // 1 - Filename Terminator (byte 10)
        fm.writeString(originalName);
        fm.writeByte(terminatorByte);

        // 4 - File Offset
        fm.writeInt((int) offset);

        // 4 - Decompressed File Length
        fm.writeInt((int) decompLength); // we just write all files as decompressed files

        // 4 - Compressed File Length (0 = no compression)
        fm.writeInt(0); // we just write all files as decompressed files

        offset += decompLength;
      }

      // Write Files
      TaskProgressManager.setMessage(Language.get("Progress_WritingFiles"));
      write(resources, fm);

      //ExporterPlugin exporter = new Exporter_ZLib();
      //long[] compressedLengths = write(exporter,resources,fm);

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
