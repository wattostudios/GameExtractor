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
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.exporter.BlockExporterWrapper;
import org.watto.ge.plugin.exporter.Exporter_LZSS;
import org.watto.io.FileManipulator;
import org.watto.io.converter.ShortConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_FS extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_FS() {

    super("FS", "FS");

    //         read write replace rename
    setProperties(true, true, true, true);

    setExtensions("fs");
    setGames("Chaser", "Gene Troopers");
    setPlatforms("PC");

    setTextPreviewExtensions("cew", "cpp", "sc", "sca", "scs", "def", "mtl"); // LOWER CASE

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
      if (fm.readByte() == 0) {
        rating += 5;
      }

      long arcSize = fm.getLength();

      if (arcSize - 4 < 0) {
        return 0;
      }
      fm.seek(arcSize - 4);

      // Directory Offset
      if (FieldValidator.checkOffset(fm.readInt(), arcSize)) {
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
  
  **********************************************************************************************
  **/
  @Override
  public Resource[] read(File path) {
    try {

      addFileTypes();

      ExporterPlugin exporter = new Exporter_LZSS();

      FileManipulator fm = new FileManipulator(path, false);
      long arcSize = fm.getLength();

      // 1 - Header

      // go to endOfFile-4
      fm.seek((int) fm.getLength() - 4);

      // 4 - Directory Offset
      long dirOffset = fm.readInt();
      FieldValidator.checkOffset(dirOffset, arcSize);

      fm.seek(dirOffset);

      // 4 - Directory Length
      // 4 - Unknown (8192)
      fm.skip(8);

      // 4 - Archive Name Length
      int arcNameLength = fm.readInt();
      FieldValidator.checkLength(arcNameLength, arcSize);

      // 4 - Version (1)
      // 4 - Filename Directory Length
      fm.skip(8);

      // 4 - Number of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // X - Archive Name (null)
      fm.skip(arcNameLength);

      // loop through filename directory
      String[] names = new String[numFiles];
      for (int i = 0; i < numFiles; i++) {
        // X - Filename (null)
        names[i] = fm.readNullString();
        FieldValidator.checkFilename(names[i]);
      }

      short[] compressionFlags = new short[numFiles];
      for (int i = 0; i < numFiles; i++) {
        // 4 - Data Offset
        int offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - Compressed File Length
        int length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - Decompressed File Length
        int decompLength = fm.readInt();
        FieldValidator.checkLength(decompLength);

        // 2 - Folder Number
        fm.skip(2);

        // 2 - Compression Flag
        short compression = fm.readShort();
        compressionFlags[i] = compression;

        String filename = names[i];

        //path,id,name,offset,length,decompLength,exporter
        if (compression == 0) {
          resources[i] = new Resource(path, filename, offset, length);
        }
        else if (compression == 2 || compression == 3) {
          resources[i] = new Resource(path, filename, offset, length, decompLength);
        }
        else {
          ErrorLogger.log("[FS] Unknown Compression Type: " + compression);
          return null;
        }

        TaskProgressManager.setValue(i);
      }

      fm.getBuffer().setBufferSize(128);
      fm.seek(1); // to reset the read buffer

      // read the compressions
      for (int i = 0; i < numFiles; i++) {
        short compression = compressionFlags[i];

        if (compression == 0) {
          continue;
        }
        else if (compression == 2) {
          Resource resource = resources[i];
          long offset = resource.getOffset();

          fm.seek(offset);

          // work out the number of blocks
          int numBlocks = fm.readInt() / 8;
          FieldValidator.checkNumFiles(numBlocks);

          fm.relativeSeek(offset); // back to the start of the file

          long[] blockOffsets = new long[numBlocks];
          long[] blockLengths = new long[numBlocks];
          long[] blockDecompLengths = new long[numBlocks];

          for (int b = 0; b < numBlocks; b++) {
            // 4 - Block Offset (relative to the start of this file)
            long blockOffset = offset + fm.readInt();
            FieldValidator.checkLength(blockOffset, arcSize);
            blockOffsets[b] = blockOffset;

            // 2 - Unknown
            fm.skip(2);

            // 2 - Block Compressed Length
            int blockLength = ShortConverter.unsign(fm.readShort());
            blockLengths[b] = blockLength;

            blockDecompLengths[b] = 16384;
          }

          int lastDecompLength = ((int) resource.getDecompressedLength()) % 16384;
          if (lastDecompLength != 0) {
            blockDecompLengths[numBlocks - 1] = lastDecompLength;
          }

          BlockExporterWrapper blockExporter = new BlockExporterWrapper(exporter, blockOffsets, blockLengths, blockDecompLengths);
          resource.setExporter(blockExporter);
        }
        else if (compression == 3) {
          Resource resource = resources[i];
          long offset = resource.getOffset();

          fm.seek(offset);

          // work out the number of blocks
          int numBlocks = fm.readInt() / 8;
          FieldValidator.checkNumFiles(numBlocks);

          fm.relativeSeek(offset); // back to the start of the file

          long[] blockOffsets = new long[numBlocks];
          long[] blockLengths = new long[numBlocks];
          long[] blockDecompLengths = new long[numBlocks];

          for (int b = 0; b < numBlocks; b++) {
            // 4 - Block Offset (relative to the start of this file)
            long blockOffset = offset + fm.readInt();
            FieldValidator.checkLength(blockOffset, arcSize);
            blockOffsets[b] = blockOffset;

            // 4 - Block Compressed Length
            int blockLength = fm.readInt();
            FieldValidator.checkLength(blockLength, arcSize);
            blockLengths[b] = blockLength;

            blockDecompLengths[b] = 32768;
          }

          int lastDecompLength = ((int) resource.getDecompressedLength()) % 32768;
          if (lastDecompLength != 0) {
            blockDecompLengths[numBlocks - 1] = lastDecompLength;
          }

          BlockExporterWrapper blockExporter = new BlockExporterWrapper(exporter, blockOffsets, blockLengths, blockDecompLengths);
          resource.setExporter(blockExporter);
        }
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
  
  **********************************************************************************************
  **/
  @Override
  public void write(Resource[] resources, File path) {
    try {

      int numFiles = resources.length;
      TaskProgressManager.setMaximum(numFiles);

      FileManipulator fm = new FileManipulator(path, true);

      fm.writeByte(0);

      TaskProgressManager.setMessage(Language.get("Progress_WritingFiles"));
      write(resources, fm);

      long dirOffset = (int) fm.getOffset();

      // 4 - Directory Length (not including the 4 dir offset at the end)
      fm.setLength(dirOffset + 24);
      fm.seek(dirOffset + 4);

      // 4 - Unknown (8192)
      fm.writeInt((int) 8192);

      // 4 - Archive Name Length
      fm.writeInt((int) 6);

      // 4 - Version (1)
      fm.writeInt((int) 1);

      // 4 - Filename Directory Length
      fm.skip(4);

      // 4 - Number of Files
      fm.writeInt((int) numFiles);

      // X - Archive Name (null)
      fm.writeNullString("Texts");

      TaskProgressManager.setMessage(Language.get("Progress_WritingDirectory"));
      for (int i = 0; i < numFiles; i++) {
        String name = resources[i].getName();
        fm.writeString(name);
        fm.writeByte(0);
      }

      long filenameDirLength = (int) fm.getOffset() - dirOffset - 30;

      long offset = 1;

      for (int i = 0; i < numFiles; i++) {
        Resource fd = resources[i];
        long length = fd.getDecompressedLength();

        // 4 - Data Offset
        fm.writeInt((int) offset);

        // 4 - Compressed File Length
        fm.writeInt((int) length);

        // 4 - Decompressed File Length
        fm.writeInt((int) length);

        // 4 - null
        fm.writeInt((int) 0);

        offset += length;
      }

      long dirLength = (int) fm.getOffset() - dirOffset;

      // 4 - Directory Offset
      fm.writeInt((int) dirOffset);

      // go back and write the missing stuff
      fm.seek(dirOffset);
      fm.writeInt((int) dirLength);
      fm.skip(12);
      fm.writeInt((int) filenameDirLength);

      fm.close();

    }
    catch (Throwable t) {
      logError(t);
    }
  }

}