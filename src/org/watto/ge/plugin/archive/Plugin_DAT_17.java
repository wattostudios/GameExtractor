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
import org.watto.component.WSPluginManager;
import org.watto.datatype.Archive;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.ge.plugin.exporter.BlockVariableExporterWrapper;
import org.watto.ge.plugin.exporter.Exporter_Default;
import org.watto.ge.plugin.exporter.Exporter_LZSS;
import org.watto.io.FileManipulator;
import org.watto.io.converter.ByteConverter;
import org.watto.io.converter.IntConverter;
import org.watto.io.converter.ShortConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_DAT_17 extends ArchivePlugin {

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Plugin_DAT_17() {

    super("DAT_17", "DAT_17");

    //         read write replace rename
    setProperties(true, false, false, false);

    setExtensions("dat");
    setGames("Fallout");
    setPlatforms("PC");

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

      // Number Of Directories
      int numFiles = IntConverter.changeFormat(fm.readInt());
      if (FieldValidator.checkNumFiles(numFiles)) {
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
    if (extension.equalsIgnoreCase("bio") || extension.equalsIgnoreCase("gam") || extension.equalsIgnoreCase("lst") || extension.equalsIgnoreCase("msg") || extension.equalsIgnoreCase("sve")) {
      return (ViewerPlugin) WSPluginManager.getPlugin("Viewer", "TXT");
    }
    return null;
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  @Override
  public Resource[] read(File path) {
    try {

      addFileTypes();

      FileManipulator fm = new FileManipulator(path, false);
      long arcSize = fm.getLength();

      ExporterPlugin exporter = Exporter_LZSS.getInstance();
      ExporterPlugin exporterDefault = Exporter_Default.getInstance();

      // 4 - Number of Directories
      int numDirectories = IntConverter.changeFormat(fm.readInt());
      FieldValidator.checkNumFiles(numDirectories);

      // 4 - Unknown
      // 4 - Unknown
      // 4 - Unknown
      fm.skip(12);

      int numFiles = Archive.getMaxFiles(4);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(arcSize);

      String[] dirNames = new String[numDirectories];

      for (int i = 0; i < numDirectories; i++) {
        // 1 - Directory Name Length
        int dirNameLength = ByteConverter.unsign(fm.readByte());

        // X - Directory Name
        dirNames[i] = fm.readString(dirNameLength);
      }

      int realNumFiles = 0;
      for (int i = 0; i < numDirectories; i++) {
        // 4 - Number of Files in the Directory
        int numFilesInDir = IntConverter.changeFormat(fm.readInt());
        FieldValidator.checkNumFiles(numFilesInDir);

        // 4 - Unknown
        // 4 - Unknown (16)
        // 4 - Unknown
        fm.skip(12);

        String dirName = dirNames[i] + "\\";
        if (dirName.equals(".\\")) {
          dirName = "";
        }

        for (int f = 0; f < numFilesInDir; f++) {
          // 1 - Filename Length
          int filenameLength = fm.readByte();
          FieldValidator.checkFilenameLength(filenameLength);

          // X - Filename
          String filename = fm.readString(filenameLength);
          FieldValidator.checkFilename(filename);
          filename = dirName + filename;

          // 4 - Flags (32/64)
          fm.skip(4);

          // 4 - File Offset
          long offset = IntConverter.changeFormat(fm.readInt());
          FieldValidator.checkOffset(offset, arcSize);

          // 4 - Decompressed File Length
          long decompLength = IntConverter.changeFormat(fm.readInt());
          FieldValidator.checkLength(decompLength);

          // 4 - Compressed File Length
          int length = IntConverter.changeFormat(fm.readInt());
          FieldValidator.checkLength(length, arcSize);

          if (length == 0) {
            // not compressed

            //path,id,name,offset,length,decompLength,exporter
            resources[realNumFiles] = new Resource(path, filename, offset, decompLength);
          }
          else {
            // compressed

            //path,id,name,offset,length,decompLength,exporter
            resources[realNumFiles] = new Resource(path, filename, offset, length, decompLength);
          }

          TaskProgressManager.setValue(offset);
          realNumFiles++;
        }
      }

      resources = resizeResources(resources, realNumFiles);

      // now go through each compressed file and work out the compressed blocks
      fm.getBuffer().setBufferSize(2); // teeny tiny for fast reads

      for (int i = 0; i < realNumFiles; i++) {
        Resource resource = resources[i];
        if (!resource.isCompressed()) {
          // not compressed
          continue;
        }

        fm.seek(resource.getOffset());

        int maxBlocks = (int) (resource.getDecompressedLength() / 4000); // 4096 actually, this gives a little extra buffer
        if (maxBlocks <= 0) {
          maxBlocks++;
        }

        ExporterPlugin[] exporters = new ExporterPlugin[maxBlocks];
        long[] blockOffsets = new long[maxBlocks];
        long[] blockLengths = new long[maxBlocks];
        long[] blockDecompLengths = new long[maxBlocks];

        long remainingLength = resource.getLength();
        long remainingDecompLength = resource.getDecompressedLength();

        int blockCount = 0;
        while (remainingLength > 0) {
          int decompBlockSize = 4096;
          // 2 - Compressed Block Length
          int blockLength = ShortConverter.unsign(ShortConverter.changeFormat(fm.readShort()));
          if ((blockLength & 0x8000) == 0x8000) {
            // uncompressed block
            blockLength &= 0x7fff;

            //System.out.println(resource.getName());

            decompBlockSize = blockLength;

            exporters[blockCount] = exporterDefault;
          }
          else {
            // LZSS compressed block
            exporters[blockCount] = exporter;
          }

          blockOffsets[blockCount] = fm.getOffset();
          blockLengths[blockCount] = blockLength;

          if (decompBlockSize != 4096) {
            blockDecompLengths[blockCount] = decompBlockSize;
          }
          else if (remainingDecompLength >= 4096) {
            blockDecompLengths[blockCount] = 4096;
          }
          else {
            blockDecompLengths[blockCount] = remainingDecompLength;
          }

          remainingLength -= (blockLength + 2); // +2 for the 2-byte header above
          remainingDecompLength -= decompBlockSize;
          blockCount++;

          fm.skip(blockLength);
        }

        if (blockCount < maxBlocks) {
          long[] oldOffsets = blockOffsets;
          blockOffsets = new long[blockCount];
          System.arraycopy(oldOffsets, 0, blockOffsets, 0, blockCount);

          long[] oldLengths = blockLengths;
          blockLengths = new long[blockCount];
          System.arraycopy(oldLengths, 0, blockLengths, 0, blockCount);

          long[] oldDecompLengths = blockDecompLengths;
          blockDecompLengths = new long[blockCount];
          System.arraycopy(oldDecompLengths, 0, blockDecompLengths, 0, blockCount);
        }

        BlockVariableExporterWrapper blockExporter = new BlockVariableExporterWrapper(exporters, blockOffsets, blockLengths, blockDecompLengths);
        resource.setExporter(blockExporter);
      }

      fm.close();

      return resources;

    }
    catch (Throwable t) {
      logError(t);
      return null;
    }
  }

}