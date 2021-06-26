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
import org.watto.component.WSPluginManager;
import org.watto.datatype.FileType;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.ge.plugin.exporter.BlockExporterWrapper;
import org.watto.ge.plugin.exporter.Exporter_LZO_MiniLZO;
import org.watto.io.FileManipulator;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_RFA_REFRACTOR2 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_RFA_REFRACTOR2() {

    super("RFA_REFRACTOR2", "RFA_REFRACTOR2");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("RalliSport Challenge");
    setExtensions("rfa"); // MUST BE LOWER CASE
    setPlatforms("XBox");

    // MUST BE LOWER CASE !!!
    setFileTypes(new FileType("ike", "Wave Audio", FileType.TYPE_AUDIO),
        new FileType("rct", "RCT Texture", FileType.TYPE_IMAGE));

    //setCanScanForFileTypes(true);

  }

  /**
   **********************************************************************************************
   Provide hints to the previewer so that certain document types are displayed appropriately
   **********************************************************************************************
   **/
  @Override
  public ViewerPlugin previewHint(Resource resource) {
    String extension = resource.getExtension();
    if (extension.equalsIgnoreCase("ike")) {
      return (ViewerPlugin) WSPluginManager.getPlugin("Viewer", "FFMPEG_Audio_WAV");
    }
    else if (extension.equalsIgnoreCase("rcm") || extension.equalsIgnoreCase("col") || extension.equalsIgnoreCase("con") || extension.equalsIgnoreCase("dif") || extension.equalsIgnoreCase("font") || extension.equalsIgnoreCase("lst") || extension.equalsIgnoreCase("ps") || extension.equalsIgnoreCase("rs") || extension.equalsIgnoreCase("spl") || extension.equalsIgnoreCase("vs") || extension.equalsIgnoreCase("xvs") || extension.equalsIgnoreCase("xps") || extension.equalsIgnoreCase("")) {
      return (ViewerPlugin) WSPluginManager.getPlugin("Viewer", "TXT");
    }
    return null;
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
      if (fm.readString(28).equals("Refractor2 FlatArchive 1.1  ")) {
        rating += 50;
      }

      long arcSize = fm.getLength();

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
   * Reads an [archive] File into the Resources
   **********************************************************************************************
   **/
  @Override
  public Resource[] read(File path) {
    try {

      // NOTE - Compressed files MUST know their DECOMPRESSED LENGTH
      //      - Uncompressed files MUST know their LENGTH

      addFileTypes();

      //ExporterPlugin exporter = new Exporter_QuickBMS_Decompression("LZO1X");
      ExporterPlugin exporter = Exporter_LZO_MiniLZO.getInstance();

      // RESETTING GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 28 - Header ("Refractor2 FlatArchive 1.1  ")
      fm.skip(28);

      // 4 - Directory Offset
      int dirOffset = fm.readInt();
      FieldValidator.checkOffset(dirOffset, arcSize);

      fm.seek(dirOffset);

      // 4 - Number Of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {

        // 4 - Filename Length
        int filenameLength = fm.readInt();
        FieldValidator.checkFilenameLength(filenameLength);

        // X - Filename
        String filename = fm.readString(filenameLength);

        // 4 - Compressed Length
        int length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - Decompressed Length
        int decompLength = fm.readInt();
        FieldValidator.checkLength(decompLength);

        // 4 - File Offset
        int offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - Unknown (4920640)
        // 4 - Unknown
        // 4 - Unknown
        fm.skip(12);

        //path,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, filename, offset, length, decompLength);

        TaskProgressManager.setValue(i);
      }

      fm.getBuffer().setBufferSize(64);

      // go through and work out the compressed blocks
      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];
        if (!resource.isCompressed()) {
          continue;
        }

        long offset = resource.getOffset();

        fm.seek(offset);

        // 4 - Number of Blocks
        int numBlocks = fm.readInt();
        FieldValidator.checkNumFiles(numBlocks);

        if (numBlocks == 1) {

          offset += 16;

          // 4 - Compressed Block Length
          int blockLength = fm.readInt();
          FieldValidator.checkLength(blockLength, arcSize);

          // 4 - Decompressed Block Length
          int blockDecompLength = fm.readInt();
          FieldValidator.checkLength(blockDecompLength);

          // 4 - Compressed Block Offset (first block is 0)
          fm.skip(4);

          resource.setOffset(offset);
          resource.setLength(blockLength);
          resource.setDecompressedLength(blockDecompLength);
          resource.setExporter(exporter);

        }
        else {

          offset += 4 + (numBlocks * 12);

          long[] blockOffsets = new long[numBlocks];
          long[] blockLengths = new long[numBlocks];
          long[] blockDecompLengths = new long[numBlocks];

          for (int b = 0; b < numBlocks; b++) {
            // 4 - Compressed Block Length
            int blockLength = fm.readInt();
            FieldValidator.checkLength(blockLength, arcSize);
            blockLengths[b] = blockLength;

            // 4 - Decompressed Block Length
            int blockDecompLength = fm.readInt();
            FieldValidator.checkLength(blockDecompLength);
            blockDecompLengths[b] = blockDecompLength;

            // 4 - Compressed Block Offset (first block is 0)
            long blockOffset = fm.readInt() + offset;
            FieldValidator.checkOffset(blockOffset, arcSize);
            blockOffsets[b] = blockOffset;
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
