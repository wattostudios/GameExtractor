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
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.exporter.BlockExporterWrapper;
import org.watto.ge.plugin.exporter.Exporter_ZLib;
import org.watto.io.FileManipulator;
import org.watto.task.TaskProgressManager;
import org.watto.xml.XMLNode;
import org.watto.xml.XMLReader;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_GPK_KPG extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_GPK_KPG() {

    super("GPK_KPG", "GPK_KPG");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Iron Sky Invasion");
    setExtensions("gpk"); // MUST BE LOWER CASE
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

      String arcFile = fm.getFile().getAbsolutePath();
      int dotPos = arcFile.lastIndexOf(".");
      if (dotPos < 0) {
        File dirFile = new File(arcFile.substring(0, dotPos) + "_gpk.mod");
        if (dirFile.exists()) {
          rating += 25;
        }
      }

      // Header
      fm.skip(1);
      if (fm.readString(3).equals("KPG")) {
        rating += 50;
      }

      long arcSize = fm.getLength();

      // Archive Size
      if (FieldValidator.checkEquals(fm.readInt() + 24, arcSize)) {
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

      ExporterPlugin exporter = Exporter_ZLib.getInstance();

      // RESETTING GLOBAL VARIABLES

      long arcSize = (int) path.length();

      File sourcePath = null;
      String arcFile = path.getAbsolutePath();
      int dotPos = arcFile.lastIndexOf(".");
      if (dotPos > 0) {
        sourcePath = new File(arcFile.substring(0, dotPos) + "_gpk.mod");
        if (!sourcePath.exists()) {
          return null;
        }
      }

      XMLNode xml = XMLReader.read(sourcePath);
      if (xml == null) {
        return null;
      }
      XMLNode filelistNode = xml.getChild("FileList");
      if (filelistNode == null) {
        return null;
      }

      int numFiles = filelistNode.getChildCount();
      FieldValidator.checkNumFiles(numFiles);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {
        // <File alias="STR: sounds/engine/engine_1.mp3" source="STR: sounds.gpk" virtual="BOL: 0" size="U64: 17725" originalSize="U64: 17725" compression="ENM: CM_STORE" offset="U64: 512" md5="U64: 0" type="ENM: PT_BINARY" seekTableSize="U32: 0" randomAccess="BOL: 0" />
        XMLNode fileNode = filelistNode.getChild(i);
        if (fileNode == null) {
          return null;
        }

        // Filename
        String filename = fileNode.getAttribute("alias");
        if (filename == null || filename.isEmpty()) {
          return null;
        }
        if (filename.startsWith("STR: ")) {
          filename = filename.substring(5);
        }

        // Compressed Length
        String lengthString = fileNode.getAttribute("size");
        if (lengthString == null || lengthString.isEmpty()) {
          return null;
        }
        if (lengthString.startsWith("U64: ")) {
          lengthString = lengthString.substring(5);
        }
        int length = Integer.parseInt(lengthString);
        FieldValidator.checkLength(length, arcSize);

        // Decompressed Length
        lengthString = fileNode.getAttribute("originalSize");
        if (lengthString == null || lengthString.isEmpty()) {
          return null;
        }
        if (lengthString.startsWith("U64: ")) {
          lengthString = lengthString.substring(5);
        }
        int decompLength = Integer.parseInt(lengthString);
        FieldValidator.checkLength(decompLength);

        // Offset
        String offsetString = fileNode.getAttribute("offset");
        if (offsetString == null || offsetString.isEmpty()) {
          return null;
        }
        if (offsetString.startsWith("U64: ")) {
          offsetString = offsetString.substring(5);
        }
        int offset = Integer.parseInt(offsetString);
        FieldValidator.checkLength(offset, arcSize);

        // Compression
        String compString = fileNode.getAttribute("compression");
        if (compString == null || compString.isEmpty()) {
          return null;
        }
        if (compString.startsWith("ENM: ")) {
          compString = compString.substring(5);
        }

        if (compString.contentEquals("CM_COMPRESS")) {
          // ZLib compression

          //path,name,offset,length,decompLength,exporter
          resources[i] = new Resource(path, filename, offset, length, decompLength, exporter);
        }
        else if (compString.equals("CM_STORE")) {
          // Uncompressed

          //path,name,offset,length,decompLength,exporter
          resources[i] = new Resource(path, filename, offset, length, decompLength);
        }
        else {
          ErrorLogger.log("[GPK_KPG] Unknown compression: " + compString);

          //path,name,offset,length,decompLength,exporter
          resources[i] = new Resource(path, filename, offset, length, decompLength);
        }

        TaskProgressManager.setValue(i);
      }

      // Now need to read the actual data archive, to get all the blocks for each compressed file
      FileManipulator fm = new FileManipulator(path, false);
      fm.getBuffer().setBufferSize(512);

      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];

        if (resource.getExporter() == exporter) {
          // ZLib compression
          long resourceOffset = resource.getOffset();

          fm.seek(resourceOffset);

          // 4 - Unknown
          fm.skip(4);

          // 4 - Maximum Decompressed Block Length (262144)
          int maxBlockSize = fm.readInt();

          // 4 - Number of Compressed Blocks
          int numBlocks = fm.readInt();
          FieldValidator.checkNumFiles(numBlocks);

          // 4 - Padding Length
          // 4 - Compressed File Length
          fm.skip(8);

          // 4 - Decompressed File Length
          int decompLength = fm.readInt();
          FieldValidator.checkLength(decompLength, arcSize);

          long[] offsets = new long[numBlocks];
          long[] compLengths = new long[numBlocks];
          long[] decompLengths = new long[numBlocks];
          for (int b = 0; b < numBlocks; b++) {
            // 4 - Compressed Block Length
            int compLength = fm.readInt();
            FieldValidator.checkLength(compLength, arcSize);

            // 4 - Compressed Block Offset
            long offset = fm.readInt() + resourceOffset;
            FieldValidator.checkOffset(offset, arcSize);

            offsets[b] = offset;
            compLengths[b] = compLength;
            decompLengths[b] = maxBlockSize;
          }

          int lastSize = decompLength - (maxBlockSize * (numBlocks - 1));
          decompLengths[numBlocks - 1] = lastSize;

          BlockExporterWrapper blockExporter = new BlockExporterWrapper(exporter, offsets, compLengths, decompLengths);
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

}
