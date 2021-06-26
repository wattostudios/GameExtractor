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
import org.watto.ge.plugin.exporter.BlockExporterWrapper;
import org.watto.ge.plugin.exporter.Exporter_ZLib;
import org.watto.io.FileManipulator;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_CAGE extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_CAGE() {

    super("CAGE", "CAGE");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Darkest of Days");
    setExtensions("cage"); // MUST BE LOWER CASE
    setPlatforms("PC");

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

      // X - filename
      // 1 - null Filename Terminator
      String filename = fm.readNullString(256); // need to force terminate at a maximum seek of 2048 bytes, otherwise some files will read forever if no null if found in the file!
      if (filename.length() >= 256) {
        return 0;
      }
      else {
        // now that we've artificially moved the file pointer to offset, 256, we need to move it back to the right place
        fm.seek(0); // quick go back to the beginning, so the buffer doesn't reload
        fm.skip(filename.length() + 1);
      }
      FieldValidator.checkFilename(filename);

      // 4 - Decompressed Length
      if (FieldValidator.checkLength(fm.readInt())) {
        rating += 5;
      }

      long arcSize = fm.getLength();

      // 4 - Compressed Length
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
   Provide hints to the previewer so that certain document types are displayed appropriately
   **********************************************************************************************
   **/
  @Override
  public ViewerPlugin previewHint(Resource resource) {
    String extension = resource.getExtension();
    if (extension.equalsIgnoreCase("aiconf") || extension.equalsIgnoreCase("alogic") || extension.equalsIgnoreCase("bush") || extension.equalsIgnoreCase("dcl") || extension.equalsIgnoreCase("eff") || extension.equalsIgnoreCase("efx") || extension.equalsIgnoreCase("env") || extension.equalsIgnoreCase("fil") || extension.equalsIgnoreCase("frag") || extension.equalsIgnoreCase("geo") || extension.equalsIgnoreCase("gren") || extension.equalsIgnoreCase("gun") || extension.equalsIgnoreCase("guy") || extension.equalsIgnoreCase("h2o") || extension.equalsIgnoreCase("joint") || extension.equalsIgnoreCase("mat") || extension.equalsIgnoreCase("part") || extension.equalsIgnoreCase("pff") || extension.equalsIgnoreCase("pfx") || extension.equalsIgnoreCase("post") || extension.equalsIgnoreCase("rig") || extension.equalsIgnoreCase("sh") || extension.equalsIgnoreCase("sho") || extension.equalsIgnoreCase("vert") || extension.equalsIgnoreCase("voice") || extension.equalsIgnoreCase("weed")) {
      return (ViewerPlugin) WSPluginManager.getPlugin("Viewer", "TXT");
    }
    return null;
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

      ExporterPlugin exporter = Exporter_ZLib.getInstance();

      // RESETTING GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      int numFiles = Archive.getMaxFiles();
      int realNumFiles = 0;

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(arcSize);

      // Loop through the file
      while (fm.getOffset() < arcSize) {
        // X - filename
        // 1 - null Filename Terminator
        String filename = fm.readNullString();
        FieldValidator.checkFilename(filename);

        // 4 - Decompressed Length
        int decompLength = fm.readInt();
        FieldValidator.checkLength(decompLength);

        // 4 - Compressed Length
        int length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        long[] blockOffsets = new long[10];
        long[] blockLengths = new long[10];
        long[] decompLengths = new long[10];
        long processedDecompLength = 0;
        int numBlocks = 0;
        // Loop through and read mall the compressed blocks
        while (processedDecompLength < decompLength) {
          // 4 - Decompressed Block Length
          int blockDecompLength = fm.readInt();
          FieldValidator.checkLength(blockDecompLength);

          // 4 - Compressed Block Length
          int blockCompLength = fm.readInt();
          FieldValidator.checkLength(blockCompLength);

          // X - Compressed Block Data (ZLib)
          long blockOffset = fm.getOffset();
          fm.skip(blockCompLength);
          processedDecompLength += blockDecompLength;

          blockOffsets[numBlocks] = blockOffset;
          blockLengths[numBlocks] = blockCompLength;
          decompLengths[numBlocks] = blockDecompLength;
          numBlocks++;

          if (numBlocks >= blockOffsets.length) {
            // increase the size of the blocks - there are more of them
            int oldLength = blockOffsets.length;
            int newLength = oldLength + 10;

            long[] oldArray = blockOffsets;
            blockOffsets = new long[newLength];
            System.arraycopy(oldArray, 0, blockOffsets, 0, oldLength);

            oldArray = blockLengths;
            blockLengths = new long[newLength];
            System.arraycopy(oldArray, 0, blockLengths, 0, oldLength);

            oldArray = decompLengths;
            decompLengths = new long[newLength];
            System.arraycopy(oldArray, 0, decompLengths, 0, oldLength);
          }
        }

        // now that we're finished, shrink the block arrays if they're too large
        if (numBlocks < blockOffsets.length) {
          long[] oldArray = blockOffsets;
          blockOffsets = new long[numBlocks];
          System.arraycopy(oldArray, 0, blockOffsets, 0, numBlocks);

          oldArray = blockLengths;
          blockLengths = new long[numBlocks];
          System.arraycopy(oldArray, 0, blockLengths, 0, numBlocks);

          oldArray = decompLengths;
          decompLengths = new long[numBlocks];
          System.arraycopy(oldArray, 0, decompLengths, 0, numBlocks);
        }

        // Create an exporter for all the blocks
        BlockExporterWrapper blockExporter = new BlockExporterWrapper(exporter, blockOffsets, blockLengths, decompLengths);

        long offset = fm.getOffset();

        //path,name,offset,length,decompLength,exporter
        resources[realNumFiles] = new Resource(path, filename, offset, length, decompLength, blockExporter); // NOTE THE BLOCK EXPORTER!!!
        realNumFiles++;

        TaskProgressManager.setValue(offset);
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

}
