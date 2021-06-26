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
import org.watto.datatype.Archive;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.exporter.BlockVariableExporterWrapper;
import org.watto.ge.plugin.exporter.Exporter_Default;
import org.watto.io.FileManipulator;
import org.watto.io.converter.ByteConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_VFS0 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_VFS0() {

    super("VFS0", "VFS0");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Metro 2033");
    setExtensions("vfs0"); // MUST BE LOWER CASE
    setPlatforms("PC");

    // MUST BE LOWER CASE !!!
    //setFileTypes(new FileType("txt", "Text Document", FileType.TYPE_DOCUMENT),
    //             new FileType("bmp", "Bitmap Image", FileType.TYPE_IMAGE)
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

      File dirFile = new File(fm.getFile().getParent() + File.separator + "content.vfi");
      if (dirFile.exists()) {
        rating += 25;
      }
      else {
        return 0;
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

      //ExporterPlugin exporterLZ4 = Exporter_LZ4.getInstance();
      ExporterPlugin exporterDefault = Exporter_Default.getInstance();

      // RESETTING GLOBAL VARIABLES

      long arcSize = (int) path.length();

      String parentPath = path.getParent() + File.separator;

      File sourcePath = new File(parentPath + "content.vfi");
      if (!sourcePath.exists()) {
        return null;
      }

      FileManipulator fm = new FileManipulator(sourcePath, false);

      long dirSize = (int) sourcePath.length();

      int numFiles = Archive.getMaxFiles() * 10;
      int realNumFiles = 0;

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(dirSize);

      // 4 - Unknown (-1)
      // 4 - Number of Archives?
      // 4 - null
      // 16 - CRC?
      fm.skip(28);

      // Loop through directory
      while (fm.getOffset() < dirSize) {

        // 4 - Archive Number (incremental from 0)
        // 4 - Length of Directory for this Archive File (from this point)
        // 4 - null
        // 4 - Archive Filename Length [-5]
        fm.skip(16);

        // X - Archive Filename
        // 1 - null Archive Filename Terminator
        String archiveFilename = fm.readNullString();
        FieldValidator.checkFilename(archiveFilename);

        File archiveFile = new File(parentPath + archiveFilename);
        if (!archiveFile.exists()) {
          ErrorLogger.log("[VFS0] Missing archive file: " + archiveFile);
          return null;
        }

        long thisArcSize = archiveFile.length();

        //System.out.println("Archive \t" + archiveFilename);

        // 4 - Archive Length
        // 4 - Unknown (1)
        fm.skip(8);

        // 4 - Length of Directory for this Archive File (from this point)
        int dirLength = fm.readInt();
        FieldValidator.checkLength(dirLength, arcSize);

        while (dirLength > 12) {

          // 1 - XOR Value
          int xorValue = ByteConverter.unsign(fm.readByte());

          // 3 - Unknown
          fm.skip(3);

          // 4 - File Offset
          int offset = fm.readInt();

          // 4 - Decompressed File Length
          int decompLength = fm.readInt();

          // 4 - Compressed File Length
          int length = fm.readInt();

          // 4 - Filename Length (including null terminator)
          int filenameLength = fm.readInt();
          FieldValidator.checkFilenameLength(filenameLength);

          // X - Filename
          // 1 - null Filename Terminator
          //String filename = fm.readNullString();
          //FieldValidator.checkFilename(filename);
          filenameLength -= 1;
          byte[] filenameBytes = fm.readBytes(filenameLength);
          fm.skip(1);
          for (int b = 0; b < filenameLength; b++) {
            filenameBytes[b] ^= xorValue;
          }
          String filename = new String(filenameBytes);

          dirLength -= (20 + (filenameLength + 1));

          try {
            FieldValidator.checkOffset(offset, thisArcSize);
            FieldValidator.checkLength(length, thisArcSize);
            FieldValidator.checkLength(decompLength);

            //path,name,offset,length,decompLength,exporter
            Resource resource = new Resource(archiveFile, filename, offset, length, decompLength);
            resource.forceNotAdded(true);
            resources[realNumFiles] = resource;
            realNumFiles++;
          }
          catch (Throwable t) {
            // skip this - must be a CRC entry or something
          }

          TaskProgressManager.setValue(fm.getOffset());
        }

        // 4 - Unknown (2)
        // 4 - Unknown (4)
        // 4 - null
        fm.skip(12);

      }

      numFiles = realNumFiles;
      resources = resizeResources(resources, numFiles);

      fm.close();

      // Now we need to go through each compressed file to read the compressed blocks
      File archiveFile = null;
      fm = null;

      TaskProgressManager.setMaximum(numFiles);
      //TaskProgressManager.setMessage(Language.get("Progress_DecompressingArchive")); // progress bar

      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];

        long compLength = resource.getLength();
        long decompLength = resource.getDecompressedLength();

        if (compLength == decompLength) {
          // not a compressed file
          continue;
        }

        File source = resource.getSource();
        if (archiveFile != null && source.equals(archiveFile)) {
          // the archive is already opened
        }
        else {
          // need to open the archive to read it
          if (fm != null) {
            fm.close();
          }
          archiveFile = source;
          fm = new FileManipulator(archiveFile, false, 9); // buffer size = 9
        }

        fm.seek(resource.getOffset());

        int blockCount = (int) (decompLength / 131072);
        if (decompLength % 131072 != 0) {
          blockCount++;
        }

        long[] blockOffsets = new long[blockCount];
        long[] blockCompLengths = new long[blockCount];
        long[] blockDecompLengths = new long[blockCount];
        ExporterPlugin[] exporters = new ExporterPlugin[blockCount];
        for (int b = 0; b < blockCount; b++) {
          // read each compressed block

          // 1 - Compression Header
          int compressionHeader = ByteConverter.unsign(fm.readByte());
          if (compressionHeader == 127) {
            // 4 - Compressed Block Length (including this 9-byte header)
            int compBlock = fm.readInt();
            FieldValidator.checkLength(compBlock, arcSize);
            blockCompLengths[b] = compBlock;

            // 4 - Decompressed Block Length
            int decompBlock = fm.readInt();
            FieldValidator.checkLength(decompBlock);
            blockDecompLengths[b] = decompBlock;

            // X - Block Data (LZ4 Compression)
            long blockOffset = fm.getOffset() - 9;
            blockOffsets[b] = blockOffset;
            fm.skip(compBlock - 9);

            //exporters[b] = exporterLZ4;
            exporters[b] = exporterDefault;
          }
          else if (compressionHeader == 126) {
            // 4 - Block Length (including this 9-byte header)
            int compBlock = fm.readInt() - 9;
            FieldValidator.checkLength(compBlock, arcSize);
            blockCompLengths[b] = compBlock;

            // 4 - Block Length
            int decompBlock = fm.readInt();
            FieldValidator.checkLength(decompBlock);
            blockDecompLengths[b] = decompBlock;

            // X - Block Data (Raw)
            long blockOffset = fm.getOffset();
            blockOffsets[b] = blockOffset;
            fm.skip(compBlock);

            exporters[b] = exporterDefault;
          }
          else if (compressionHeader == 125) {
            // 1 - Compressed Block Length (including this 3-byte header)
            int compBlock = ByteConverter.unsign(fm.readByte());
            blockCompLengths[b] = compBlock;

            // 1 - Decompressed Block Length
            int decompBlock = ByteConverter.unsign(fm.readByte());
            blockDecompLengths[b] = decompBlock;

            // X - Block Data (LZ4 Compression)
            long blockOffset = fm.getOffset() - 3;
            blockOffsets[b] = blockOffset;
            fm.skip(compBlock - 3);

            //exporters[b] = exporterLZ4;
            exporters[b] = exporterDefault;
          }
          else if (compressionHeader == 124) {
            // 1 - Block Length (including this 3-byte header)
            int compBlock = ByteConverter.unsign(fm.readByte()) - 3;
            blockCompLengths[b] = compBlock;

            // 1 - Block Length
            int decompBlock = ByteConverter.unsign(fm.readByte());
            blockDecompLengths[b] = decompBlock;

            // X - Block Data (Raw)
            long blockOffset = fm.getOffset();
            blockOffsets[b] = blockOffset;
            fm.skip(compBlock);

            exporters[b] = exporterDefault;
          }
          else {
            ErrorLogger.log("[VFS0]: Unknown compression header: " + compressionHeader + "\t" + fm.getOffset());
            b = blockCount;
            blockCompLengths = null;
            blockDecompLengths = null;
            blockOffsets = null;
            continue;
          }
        }

        if (blockCompLengths != null && blockDecompLengths != null && blockOffsets != null) {
          TaskProgressManager.setValue(i);
          BlockVariableExporterWrapper blockExporter = new BlockVariableExporterWrapper(exporters, blockOffsets, blockCompLengths, blockDecompLengths);
          resource.setExporter(blockExporter);
        }

      }

      if (fm != null) {
        fm.close();
      }

      return resources;

    }
    catch (Throwable t) {
      logError(t);
      return null;
    }
  }

}
