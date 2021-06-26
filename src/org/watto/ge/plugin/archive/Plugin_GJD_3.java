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
import org.watto.datatype.Archive;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.exporter.BlockExporterWrapper;
import org.watto.ge.plugin.exporter.Exporter_Default;
import org.watto.io.FileManipulator;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_GJD_3 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_GJD_3() {

    super("GJD_3", "GJD_3");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("The 11th Hour");
    setExtensions("gjd"); // MUST BE LOWER CASE
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

      fm.skip(4);

      if (fm.readInt() == 0) {
        rating += 5;
      }

      fm.skip(4);

      if (fm.readInt() == 0) {
        rating += 5;
      }

      fm.skip(10);

      long arcSize = fm.getLength();

      // First File Length
      try {
        if (FieldValidator.checkLength(fm.readInt(), arcSize)) {
          rating += 5;
        }
      }
      catch (Throwable t) {
        // alternative type of file
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

      ExporterPlugin exporter = Exporter_Default.getInstance();

      // RESETTING GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false, 16); // 16, for quick block reading

      long arcSize = fm.getLength();

      // 4 - Unknown
      // 4 - null
      // 4 - Unknown
      fm.skip(12);

      // 4 - null
      if (fm.readInt() == 0) {
        // carry on

        // 4 - Unknown
        // 2 - Unknown
        fm.seek(22);
      }
      else {
        // alternative type, which only has a 6-gyte header
        fm.seek(6);
      }

      int numFiles = Archive.getMaxFiles();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(arcSize);

      // Loop through directory
      int realNumFiles = 0;
      while (fm.getOffset() < arcSize) {

        // 2 - Unknown
        // 2 - Unknown
        //String headerCheck = fm.readByte() + " " + fm.readByte() + " " + fm.readByte() + " " + fm.readByte();
        fm.skip(2);
        short headerCheck = fm.readShort();

        // 4 - File Length (including the block headers)
        int length = fm.readInt();
        if (length == -1) {
          //System.out.println("Padding" + "\t" + headerCheck + "\t" + (fm.getOffset() - 8));
          continue;
        }

        FieldValidator.checkLength(length, arcSize);

        long offset = fm.getOffset();

        if (headerCheck == 4144) {
          // multiple blocks

          long[] blockLengths = new long[0];
          long[] blockOffsets = new long[0];
          int numBlocks = 0;

          // read each block
          int remainingLength = length;
          while (remainingLength > 0) {
            // 2 - Number of Data Blocks (for the first block) otherwise Unknown
            if (numBlocks == 0) {
              short realNumBlocks = fm.readShort();

              blockLengths = new long[realNumBlocks];
              blockOffsets = new long[realNumBlocks];
            }
            else {
              fm.skip(2);
            }

            // 2 - Unknown
            fm.skip(2);

            // 4 - Block Length
            int blockLength = fm.readInt();
            FieldValidator.checkLength(blockLength, arcSize);
            blockLengths[numBlocks] = blockLength;

            // X - File Data Block
            long blockOffset = fm.getOffset();
            fm.skip(blockLength);
            blockOffsets[numBlocks] = blockOffset;

            numBlocks++;
            remainingLength -= (8 + blockLength);
          }

          //System.out.println("Multiple" + "\t" + headerCheck + "\t" + (offset - 8));

          // resize the arrays
          /*
          long[] oldBlockLengths = blockLengths;
          long[] oldBlockOffsets = blockOffsets;
          blockLengths = new long[numBlocks];
          blockOffsets = new long[numBlocks];
          System.arraycopy(oldBlockLengths, 0, blockLengths, 0, numBlocks);
          System.arraycopy(oldBlockOffsets, 0, blockOffsets, 0, numBlocks);
          */

          String filename = Resource.generateFilename(realNumFiles);

          BlockExporterWrapper blockExporter = new BlockExporterWrapper(exporter, blockOffsets, blockLengths, blockLengths);

          //path,name,offset,length,decompLength,exporter
          resources[realNumFiles] = new Resource(path, filename, offset, length, length, blockExporter);

          TaskProgressManager.setValue(offset);
          realNumFiles++;
        }
        else {
          // a single block
          //System.out.println("Single" + "\t" + headerCheck + "\t" + (offset - 8));

          String filename = Resource.generateFilename(realNumFiles);

          // X - File Data
          // check for a JPEG image
          if (length < 12) {
            fm.skip(length);
          }
          else {
            fm.skip(8);
            if (fm.readString(4).equals("JFIF")) {
              filename += ".jpg";
              fm.skip(length - 12);

              offset += 2;
              length += 6; // actually is followed by a 8-byte padder, which forms part of the JPEG image
            }
            else {
              fm.skip(length - 12);
            }
          }

          //path,name,offset,length,decompLength,exporter
          resources[realNumFiles] = new Resource(path, filename, offset, length);

          TaskProgressManager.setValue(offset);
          realNumFiles++;
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

}
