/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2026 wattostudios
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
import org.watto.ge.plugin.exporter.Exporter_LZO_SingleBlock;
import org.watto.io.FileManipulator;
import org.watto.io.converter.IntConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_PAK_MASH4VX extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_PAK_MASH4VX() {

    super("PAK_MASH4VX", "PAK_MASH4VX");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Spider-Man 3");
    setExtensions("pak"); // MUST BE LOWER CASE
    setPlatforms("PS3");

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

      fm.skip(4);

      // Header
      if (fm.readString(4).equals("mash")) {
        rating += 50;
      }

      fm.skip(1);

      if (fm.readString(3).equals("4Vx")) {
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
      ExporterPlugin exporterLZO1X = Exporter_LZO_SingleBlock.getInstance();

      // RESETTING GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 4 - Unknown
      // 4 - Unknown
      // 4 - Unknown
      // 4 - Unknown
      // 4 - Unknown
      // 4 - File Data Offset [*4096]
      // 4 - Unknown
      // 4 - Unknown
      // 4 - Unknown
      // 4 - Unknown
      // 4 - Unknown
      fm.skip(40);

      // 4 - File Data Offset
      int fileDataOffset = IntConverter.changeFormat(fm.readInt());
      FieldValidator.checkOffset(fileDataOffset, arcSize);

      // 4 - null
      fm.skip(4);

      // 4 - Number of Files
      int numFiles = IntConverter.changeFormat(fm.readInt());
      FieldValidator.checkNumFiles(numFiles);
      //int numFiles = Archive.getMaxFiles();

      // X - Unknown
      fm.seek(4516);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // read the filenames
      String[] names = new String[numFiles];
      long[] offsets = new long[numFiles + 1]; // +1 to store the arcSize in the last entry, for calculating lengths
      offsets[numFiles] = arcSize;
      for (int i = 0; i < numFiles; i++) {
        // 52 - Unknown
        fm.skip(52);

        // 4 - File Offset (Relative to the start of the file data)
        long offset = IntConverter.unsign(IntConverter.changeFormat(fm.readInt())) + fileDataOffset;
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - null
        // 4 - Unknown
        fm.skip(8);

        // X - Filename
        // 1 - null Filename Terminator
        String filename = fm.readNullString();
        FieldValidator.checkFilename(filename);

        // 0-3 - null Padding to a multiple of 4 bytes
        fm.skip(calculatePadding(filename.length() + 1, 4));

        // 4 - Unknown
        fm.skip(4);

        offsets[i] = offset;
        names[i] = filename;

      }

      /*
      int numNames = 0;
      
      // X - Filename
      // 1 - null Filename Terminator
      String name = fm.readNullString();
      while (name != null && name.length() != 0) {
        names[numNames] = name;
        numNames++;
        // 0-3 - null Padding to a multiple of 4 bytes
        // 68 - Other Data
        fm.skip(68 + calculatePadding(name.length() + 1, 4));
      
        // X - Filename
        // 1 - null Filename Terminator
        name = fm.readNullString();
      }
      */

      fm.getBuffer().setBufferSize(32); // small quick reads

      for (int i = 0; i < numFiles; i++) {
        long startOffset = offsets[i];
        long endOffset = offsets[i + 1];

        fm.seek(startOffset);

        int maxBlocks = 1000; // guess

        long[] blockOffsets = new long[maxBlocks];
        long[] blockLengths = new long[maxBlocks];
        long[] blockDecompLengths = new long[maxBlocks];
        ExporterPlugin[] blockCompressions = new ExporterPlugin[maxBlocks];
        int totalLength = 0;
        int totalDecompLength = 0;

        int numBlocks = 0;

        long offset = startOffset;

        while (offset < endOffset) {

          // 4 - Header ("NCH" + null)
          if (fm.readInt() == 4735822) {
            // 4 - Compressed Block Length
            int blockLength = IntConverter.changeFormat(fm.readInt());
            FieldValidator.checkLength(blockLength, arcSize);

            // 4 - CRC
            fm.skip(4);

            // 4 - Decompressed Block Length
            int blockDecompLength = IntConverter.changeFormat(fm.readInt());
            FieldValidator.checkLength(blockDecompLength);

            // 4 - Offset in the Decompressed File to store this Block
            // 4 - CRC
            fm.skip(8);

            // 4 - Compressed Block Length (including header)
            int blockLengthWithHeaders = IntConverter.changeFormat(fm.readInt());
            FieldValidator.checkLength(blockLengthWithHeaders, arcSize);

            // 4 - Compression Flag (0=uncompressed, 1=lzo1x)
            int compressionFlag = IntConverter.changeFormat(fm.readInt());

            // X - Block Data
            // store the block of data
            //blockOffsets[numBlocks] = offset + 32; // doesn't work, as sometimes the data has some random padding at the start of the compressed data
            blockOffsets[numBlocks] = offset + blockLengthWithHeaders - blockLength;
            blockLengths[numBlocks] = blockLength;
            blockDecompLengths[numBlocks] = blockDecompLength;

            if (compressionFlag == 0) {
              blockCompressions[numBlocks] = exporterDefault;
            }
            else if (compressionFlag == 1) {
              blockCompressions[numBlocks] = exporterLZO1X;
            }
            else {
              //ErrorLogger.log("[PAK_MASH4VX] Unknown block compression: " + compressionFlag + " at offset " + offset);
              blockCompressions[numBlocks] = exporterLZO1X;
            }
            totalLength += blockLength;
            totalDecompLength += blockDecompLength;
            numBlocks++;

            offset += blockLengthWithHeaders;
          }
          else {
            int padding = calculatePadding(offset, 4096); // just in case we're mis-aligned.
            if (padding != 0) {
              offset += padding;
            }
            else {
              offset += 4096;
            }
          }

          TaskProgressManager.setValue(offset);
        }

        // now we've read in the full file, so create it
        // we have at least 1 block of data, create the previous file

        long[] fileBlockOffsets = new long[numBlocks];
        long[] fileBlockLengths = new long[numBlocks];
        long[] fileBlockDecompLengths = new long[numBlocks];
        ExporterPlugin[] fileBlockCompressions = new ExporterPlugin[numBlocks];

        System.arraycopy(blockOffsets, 0, fileBlockOffsets, 0, numBlocks);
        System.arraycopy(blockLengths, 0, fileBlockLengths, 0, numBlocks);
        System.arraycopy(blockDecompLengths, 0, fileBlockDecompLengths, 0, numBlocks);
        System.arraycopy(blockCompressions, 0, fileBlockCompressions, 0, numBlocks);

        BlockVariableExporterWrapper blockExporter = new BlockVariableExporterWrapper(fileBlockCompressions, fileBlockOffsets, fileBlockLengths, fileBlockDecompLengths);

        int length = totalLength;
        int decompLength = totalDecompLength;

        String filename = names[i];

        //path,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, filename, offset, length, decompLength, blockExporter);

      }

      /*
      // Loop through directory
      int realNumFiles = 0;
      long offset = 0;
      
      int maxBlocks = 1000; // guess
      
      long[] blockOffsets = new long[maxBlocks];
      long[] blockLengths = new long[maxBlocks];
      long[] blockDecompLengths = new long[maxBlocks];
      ExporterPlugin[] blockCompressions = new ExporterPlugin[maxBlocks];
      int totalLength = 0;
      int totalDecompLength = 0;
      long startOffset = 0;
      int numBlocks = 0;
      
      while (offset < arcSize) {
        fm.seek(offset);
      
        // 4 - Header ("NCH" + null)
        if (fm.readInt() == 4735822) {
          // 4 - Compressed Block Length
          int blockLength = IntConverter.changeFormat(fm.readInt());
          FieldValidator.checkLength(blockLength, arcSize);
      
          // 4 - CRC
          fm.skip(4);
      
          // 4 - Decompressed Block Length
          int blockDecompLength = IntConverter.changeFormat(fm.readInt());
          FieldValidator.checkLength(blockDecompLength);
      
          // 4 - Offset in the Decompressed File to store this Block
          int blockOutputOffset = IntConverter.changeFormat(fm.readInt());
      
          // 4 - CRC
          fm.skip(4);
      
          // 4 - Compressed Block Length (including header)
          int blockLengthWithHeaders = IntConverter.changeFormat(fm.readInt());
          FieldValidator.checkLength(blockLengthWithHeaders, arcSize);
      
          // 4 - Compression Flag (0=uncompressed, 1=lzo1x)
          int compressionFlag = IntConverter.changeFormat(fm.readInt());
      
          // X - Block Data
          if (blockOutputOffset == 0) {
            // the start of a new file
            if (numBlocks != 0) {
              // we have at least 1 block of data, create the previous file
      
              long[] fileBlockOffsets = new long[numBlocks];
              long[] fileBlockLengths = new long[numBlocks];
              long[] fileBlockDecompLengths = new long[numBlocks];
              ExporterPlugin[] fileBlockCompressions = new ExporterPlugin[numBlocks];
      
              System.arraycopy(blockOffsets, 0, fileBlockOffsets, 0, numBlocks);
              System.arraycopy(blockLengths, 0, fileBlockLengths, 0, numBlocks);
              System.arraycopy(blockDecompLengths, 0, fileBlockDecompLengths, 0, numBlocks);
              System.arraycopy(blockCompressions, 0, fileBlockCompressions, 0, numBlocks);
      
              BlockVariableExporterWrapper blockExporter = new BlockVariableExporterWrapper(fileBlockCompressions, fileBlockOffsets, fileBlockLengths, fileBlockDecompLengths);
      
              int length = totalLength;
              int decompLength = totalDecompLength;
      
              String filename = Resource.generateFilename(realNumFiles);
      
              //path,name,offset,length,decompLength,exporter
              resources[realNumFiles] = new Resource(path, filename, startOffset, length, decompLength, blockExporter);
              realNumFiles++;
            }
      
            // reset the arrays
            blockOffsets = new long[maxBlocks];
            blockLengths = new long[maxBlocks];
            blockDecompLengths = new long[maxBlocks];
            blockCompressions = new ExporterPlugin[maxBlocks];
            totalLength = 0;
            totalDecompLength = 0;
            startOffset = 0;
            numBlocks = 0;
          }
      
          if (startOffset == 0) {
            startOffset = offset;
          }
      
          // store the block of data
          //blockOffsets[numBlocks] = offset + 32; // doesn't work, as sometimes the data has some random padding at the start of the compressed data
          blockOffsets[numBlocks] = offset + blockLengthWithHeaders - blockLength;
          blockLengths[numBlocks] = blockLength;
          blockDecompLengths[numBlocks] = blockDecompLength;
      
          if (compressionFlag == 0) {
            blockCompressions[numBlocks] = exporterDefault;
          }
          else if (compressionFlag == 1) {
            blockCompressions[numBlocks] = exporterLZO1X;
          }
          else {
            //ErrorLogger.log("[PAK_MASH4VX] Unknown block compression: " + compressionFlag + " at offset " + offset);
            blockCompressions[numBlocks] = exporterLZO1X;
          }
          totalLength += blockLength;
          totalDecompLength += blockDecompLength;
          numBlocks++;
      
          offset += blockLengthWithHeaders;
        }
        else {
          int padding = calculatePadding(offset, 4096); // just in case we're mis-aligned.
          if (padding != 0) {
            offset += padding;
          }
          else {
            offset += 4096;
          }
        }
      
        TaskProgressManager.setValue(offset);
      
      }
      
      // Store the last file
      if (numBlocks != 0) {
        // we have at least 1 block of data, create the previous file
      
        long[] fileBlockOffsets = new long[numBlocks];
        long[] fileBlockLengths = new long[numBlocks];
        long[] fileBlockDecompLengths = new long[numBlocks];
        ExporterPlugin[] fileBlockCompressions = new ExporterPlugin[numBlocks];
      
        System.arraycopy(blockOffsets, 0, fileBlockOffsets, 0, numBlocks);
        System.arraycopy(blockLengths, 0, fileBlockLengths, 0, numBlocks);
        System.arraycopy(blockDecompLengths, 0, fileBlockDecompLengths, 0, numBlocks);
        System.arraycopy(blockCompressions, 0, fileBlockCompressions, 0, numBlocks);
      
        BlockVariableExporterWrapper blockExporter = new BlockVariableExporterWrapper(fileBlockCompressions, fileBlockOffsets, fileBlockLengths, fileBlockDecompLengths);
      
        int length = totalLength;
        int decompLength = totalDecompLength;
      
        String filename = Resource.generateFilename(realNumFiles);
      
        //path,name,offset,length,decompLength,exporter
        resources[realNumFiles] = new Resource(path, filename, offset, length, decompLength, blockExporter);
        realNumFiles++;
      }
      
      resources = resizeResources(resources, realNumFiles);
      
      */

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
