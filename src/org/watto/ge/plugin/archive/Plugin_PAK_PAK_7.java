/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2025 wattostudios
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

import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.exporter.BlockVariableExporterWrapper;
import org.watto.ge.plugin.exporter.Exporter_Default;
import org.watto.ge.plugin.exporter.Exporter_LZO_MiniLZO;
import org.watto.ge.plugin.exporter.Exporter_ZLib_CompressedSizeOnly;
import org.watto.io.FileManipulator;
import org.watto.io.buffer.ByteBuffer;
import org.watto.io.converter.ByteConverter;
import org.watto.io.converter.IntConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_PAK_PAK_7 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_PAK_PAK_7() {

    super("PAK_PAK_7", "PAK_PAK_7");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("James Cameron's Avatar - The Game");
    setExtensions("pak"); // MUST BE LOWER CASE
    setPlatforms("PC");

    // MUST BE LOWER CASE !!!
    //setFileTypes(new FileType("txt", "Text Document", FileType.TYPE_DOCUMENT),
    //             new FileType("bmp", "Bitmap Image", FileType.TYPE_IMAGE)
    //             );

    //setTextPreviewExtensions("colours", "rat", "screen", "styles"); // LOWER CASE

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
      if (fm.readString(4).equals("PAK!")) {
        rating += 50;
      }

      if (fm.readInt() == 4) {
        rating += 5;
      }

      long arcSize = fm.getLength();

      // Directory Offset
      if (FieldValidator.checkOffset(fm.readInt(), arcSize)) {
        rating += 5;
      }

      if (fm.readInt() == 0) {
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

      ExporterPlugin exporterDefault = Exporter_Default.getInstance();
      Exporter_LZO_MiniLZO exporterLZO = Exporter_LZO_MiniLZO.getInstance();
      exporterLZO.setCheckDecompressedLength(false); // 3.16 this is needed otherwise files with multiple compressed chunks will fail (incorrectly) to decompress

      // RESETTING GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 4 - Header (PAK!)
      // 4 - Unknown (4)
      fm.skip(8);

      // 4 - Details Directory Offset
      int dirOffset = fm.readInt();
      FieldValidator.checkOffset(dirOffset, arcSize);

      // 4 - null
      fm.seek(dirOffset);

      // 4 - Total Compressed Directory Length (including this field)
      int dirLength = fm.readInt();
      FieldValidator.checkOffset(dirOffset + dirLength);

      fm.seek(dirOffset + dirLength);

      // 4 - Number of Compressed Directory Chunks
      int numChunks = fm.readInt();
      FieldValidator.checkNumFiles(numChunks);

      int[] chunkOffsets = new int[numChunks + 1];

      for (int c = 0; c < numChunks; c++) {
        // 2 - null (except the last entry)
        // 2 - Compressed Directory ID (incremental from 0, the last entry has the same ID as the second-last entry)
        fm.skip(4);

        // 4 - Compressed Directory Data Offset (relative to the start of the Details Directory) (top bit is set, to indicate compression)
        byte[] offsetBytes = fm.readBytes(4);
        offsetBytes[3] &= 127;

        int chunkOffset = dirOffset + IntConverter.convertLittle(offsetBytes);
        FieldValidator.checkOffset(chunkOffset, arcSize);
        chunkOffsets[c] = chunkOffset;
      }

      chunkOffsets[numChunks] = dirOffset + dirLength;

      // decompress the directory chunks
      int maxDirLength = numChunks * 65536;
      byte[] dirBytes = new byte[maxDirLength];
      int decompWritePos = 0;

      for (int c = 0; c < numChunks; c++) {
        fm.relativeSeek(chunkOffsets[c]);

        int chunkLength = chunkOffsets[c + 1] - chunkOffsets[c];
        //byte[] compBytes = fm.readBytes(chunkLength);
        //FileManipulator compFM = new FileManipulator(new ByteBuffer(compBytes));

        Exporter_ZLib_CompressedSizeOnly exporter = Exporter_ZLib_CompressedSizeOnly.getInstance();
        exporter.open(fm, chunkLength, chunkLength);

        for (int b = 0; b < 65537; b++) {
          if (exporter.available()) { // make sure we read the next bit of data, if required
            dirBytes[decompWritePos++] = (byte) exporter.read();
          }
          else {
            break;
          }
        }

      }

      // open the decompressed data for processing
      fm.close();
      fm = new FileManipulator(new ByteBuffer(dirBytes));

      //FileManipulator tempFM = new FileManipulator(new File("c:\\out.tmp"), true);
      //tempFM.writeBytes(dirBytes);
      //tempFM.close();

      // 1 - Unknown (1)
      fm.skip(1);

      // 4 - Number of Files ?
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles / 5);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {
        //System.out.println(fm.getOffset());

        // 4 - File Data Offset
        int offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - Decompressed Data Length
        int decompLength = fm.readInt();
        FieldValidator.checkLength(decompLength);

        // 4 - Hash?
        fm.skip(4);

        numChunks = decompLength / 65535;
        int lastChunkLength = decompLength % 65535;
        if (lastChunkLength != 0) {
          numChunks++;
        }

        if (numChunks == 1) {
          ExporterPlugin chunkExporter = exporterLZO; // most chunks are compressed with LZO1X

          // 4 - Compressed Data Length (if this is negative, this chunk isn't compressed)
          int length = fm.readInt();

          if (length < 0) {
            //length = (int) IntConverter.unsign(length);
            length = 0 - length;
            chunkExporter = exporterDefault; // a raw chunk
          }

          FieldValidator.checkLength(length, arcSize);

          String filename = Resource.generateFilename(i);

          //path,name,offset,length,decompLength,exporter
          resources[i] = new Resource(path, filename, offset, length, decompLength, chunkExporter);

        }
        else {
          long[] blockOffsets = new long[numChunks];
          long[] blockLengths = new long[numChunks];
          long[] blockDecompLengths = new long[numChunks];
          ExporterPlugin[] blockExporters = new ExporterPlugin[numChunks];

          long totalLength = 0;

          for (int c = 0; c < numChunks; c++) {
            blockExporters[c] = exporterLZO; // most chunks are compressed with LZO1X
            //blockExporters[c] = exporterDefault; // a raw chunk

            // 4 - Compressed Data Length (if this is negative, this chunk isn't compressed)
            int length = fm.readInt();

            if (length < 0) {
              //length = (int) IntConverter.unsign(length);
              length = 0 - length;
              blockExporters[c] = exporterDefault; // a raw chunk
            }

            FieldValidator.checkLength(length, arcSize);

            blockOffsets[c] = offset + totalLength;
            blockLengths[c] = length;
            blockDecompLengths[c] = 65535;

            totalLength += length;
          }

          blockDecompLengths[numChunks - 1] = lastChunkLength;

          BlockVariableExporterWrapper blockExporter = new BlockVariableExporterWrapper(blockExporters, blockOffsets, blockLengths, blockDecompLengths);

          String filename = Resource.generateFilename(i);

          //path,name,offset,length,decompLength,exporter
          resources[i] = new Resource(path, filename, offset, totalLength, decompLength, blockExporter);
        }

        TaskProgressManager.setValue(i);
      }

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];

        // 8 - Hash or Timestamp?
        fm.skip(8);

        // 1 - Filename Length
        int filenameLength = ByteConverter.unsign(fm.readByte());

        // X - Filename
        String filename = fm.readString(filenameLength);

        resource.setName(filename);
        resource.setOriginalName(filename);

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
