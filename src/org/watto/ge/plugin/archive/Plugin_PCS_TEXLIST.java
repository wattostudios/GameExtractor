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
import org.watto.ge.plugin.exporter.Exporter_ZLib_CompressedSizeOnly;
import org.watto.io.FileManipulator;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_PCS_TEXLIST extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_PCS_TEXLIST() {

    super("PCS_TEXLIST", "PCS_TEXLIST");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("King Of Fighters XIII");
    setExtensions("pcs"); // MUST BE LOWER CASE
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

      // 8 - Header ("TEXLIST" + null)
      String headerText = fm.readString(7);
      int headerByte = fm.readByte();
      if (headerText.equals("TEXLIST") && headerByte == 0) {
        rating += 50;
      }

      long arcSize = fm.getLength();

      // 8 - Length of "DETAILS AND FILE DATA" Section
      if (FieldValidator.checkLength(fm.readLong(), arcSize)) {
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

      ExporterPlugin exporter = Exporter_ZLib_CompressedSizeOnly.getInstance();

      // RESETTING GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 8 - Header ("TEXLIST" + null)
      fm.skip(8);

      // 8 - Length of "DETAILS AND FILE DATA" Section
      long dirLength = fm.readLong();
      FieldValidator.checkLength(dirLength, arcSize);

      int numFiles = Archive.getMaxFiles();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(dirLength);

      // Loop through directory
      int realNumFiles = 0;
      while (fm.getOffset() < dirLength) {

        // 8 - Header ("TEXTURE" + null)
        fm.skip(8);

        // 8 - File Length (from the end of this field)
        long length = fm.readLong();
        FieldValidator.checkLength(length, arcSize);

        // 4 - Image Format (L8/DXT1/DXT3)
        String type = fm.readNullString(4);

        // 4 - ZIP header ("ZIP" + null)
        fm.skip(4);

        // 4 - Compressed Data Length (total size of all compressed chunks + their 4-byte headers)
        length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - Unknown (8)
        fm.skip(4);

        // 4 - Number of Compressed Chunks
        int numChunks = fm.readInt();
        FieldValidator.checkNumFiles(numChunks);

        // 4 - Unknown (4)
        fm.skip(4);

        String filename = Resource.generateFilename(realNumFiles) + "." + type;

        // Compressed Chunks
        long offset = fm.getOffset();
        long[] blockOffsets = new long[numChunks];
        long[] blockLengths = new long[numChunks];
        for (int b = 0; b < numChunks; b++) {
          // 4 - Compressed File Length
          long blockLength = fm.readInt();
          FieldValidator.checkLength(blockLength, arcSize);
          blockLengths[b] = blockLength;

          // X - Compressed File Data
          blockOffsets[b] = fm.getOffset();
          fm.skip(blockLength);
        }

        // We don't know the decompressed lengths, but the exporter is ZLib_CompressedSizeOnly, so we don't need to know it, so we fake it.
        BlockExporterWrapper blockExporter = new BlockExporterWrapper(exporter, blockOffsets, blockLengths, blockLengths);

        //path,name,offset,length,decompLength,exporter
        resources[realNumFiles] = new Resource(path, filename, offset, length, length, blockExporter);
        realNumFiles++;

        TaskProgressManager.setValue(fm.getOffset());
      }

      /*
      // So now we should be at the IMGLIST section (force it anyway)
      fm.seek(dirLength + 16);
      
      // 8 - Header ("IMGLIST" + null)
      // 4 - Length of this section (excluding these 2 8-byte fields)
      fm.skip(16);
      
      while (fm.getOffset() < arcSize) {
        // 8 - Image Header ("IMAGE" + 3 nulls)
        //System.out.println(fm.readNullString(8));
        fm.skip(8);
      
        // 8 - Length of Image Descriptor
        int imgLength = (int) fm.readLong() - 8;
      
        // 8 - Data Type Header? ("MAPPLT" + 2 nulls, or "DBLPLT" + 2 nulls)
        //System.out.println("--" + fm.readNullString(8));
        fm.skip(8);
      
        while (imgLength > 0) {
          // 8 - Primitive Header ("PRIMTIVE")
          //System.out.println("----" + fm.readNullString(8));
          fm.skip(8);
      
          // 8 - Length of Primitive Block (32)
          fm.skip(8);
          // 4 - Unknown
          int value1 = fm.readInt();
          // 4 - Unknown
          int value2 = fm.readInt();
      
          // 4 - Image Width/Height?
          int value3 = fm.readInt();
          // 4 - Image Width/Height?
          int value4 = fm.readInt();
      
          // 4 - null
          int value5 = fm.readInt();
          // 4 - Image Number (incremental from 0)
          fm.skip(4);
      
          // 4 - Texture Number (referring to a Texture in the DETAILS AND FILE DATA section)
          int textureNumber = fm.readInt();
      
          // 4 - Relative Offset to something?
          int value6 = fm.readInt();
      
          System.out.println("Texture #" + textureNumber + "\t" + value1 + "\t" + value2 + "\t" + value3 + "\t" + value4 + "\t" + value5 + "\t" + value6);
          imgLength -= 48;
        }
      }
      */

      resources = resizeResources(resources, realNumFiles);

      fm.close();

      return resources;

    }
    catch (

    Throwable t) {
      logError(t);
      return null;
    }
  }

}
