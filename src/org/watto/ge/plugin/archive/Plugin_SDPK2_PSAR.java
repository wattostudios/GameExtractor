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

import org.watto.datatype.FileType;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.exporter.BlockExporterWrapper;
import org.watto.ge.plugin.exporter.Exporter_ZLib;
import org.watto.io.FileManipulator;
import org.watto.io.buffer.ByteBuffer;
import org.watto.io.converter.IntConverter;
import org.watto.io.converter.LongConverter;
import org.watto.io.converter.ShortConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_SDPK2_PSAR extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_SDPK2_PSAR() {

    super("SDPK2_PSAR", "SDPK2_PSAR");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Brink",
        "God of War: Ascension");
    setExtensions("sdpk2"); // MUST BE LOWER CASE
    setPlatforms("PC", "PS3");

    // MUST BE LOWER CASE !!!
    setFileTypes(new FileType("xvag", "PlayStation XVAG Audio", FileType.TYPE_AUDIO));

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
      if (fm.readString(4).equals("PSAR")) {
        rating += 50;
      }

      // 2 - Version Major? (1)
      if (ShortConverter.changeFormat(fm.readShort()) == 1) {
        rating += 5;
      }

      // 2 - Version Minor? (4)
      if (ShortConverter.changeFormat(fm.readShort()) == 4) {
        rating += 5;
      }

      // 4 - Compression Algorithm (zlib)
      if (fm.readString(4).equals("zlib")) {
        rating += 5;
      }

      fm.skip(8);

      // Number Of Files
      if (FieldValidator.checkNumFiles(IntConverter.changeFormat(fm.readInt()))) {
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

      //ExporterPlugin exporter = Exporter_ZLib_DecompressedSizeOnlyInBlocks.getInstance();
      ExporterPlugin exporter = Exporter_ZLib.getInstance();

      // RESETTING GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 4 - Header (PSAR)
      // 2 - Version Major? (1)
      // 2 - Version Minor? (4)
      // 4 - Compression Algorithm (zlib)
      // 4 - File Data Offset
      // 4 - Directory Entry Size (30)
      fm.skip(20);

      // 4 - Number of Files
      int numFiles = IntConverter.changeFormat(fm.readInt());
      FieldValidator.checkNumFiles(numFiles);

      // 4 - Decompressed Block Size (65536)
      int decompBlockSize = IntConverter.changeFormat(fm.readInt());

      // 4 - Unknown (1/2)
      fm.skip(4);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      //long[] offsets = new long[numFiles];
      int[] compBlockStarts = new int[numFiles];
      for (int i = 0; i < numFiles; i++) {
        // 16 - Hash?
        fm.skip(16);

        // 4 - Index to the First Block in the Compression Directory
        int compBlockStart = IntConverter.changeFormat(fm.readInt());
        compBlockStarts[i] = compBlockStart;

        // 5 - Decompressed Length
        //fm.skip(1);
        //int decompLength = IntConverter.changeFormat(fm.readInt());
        byte[] decompLengthBytes = new byte[] { 0, 0, 0, fm.readByte(), fm.readByte(), fm.readByte(), fm.readByte(), fm.readByte() };
        long decompLength = LongConverter.convertBig(decompLengthBytes);
        FieldValidator.checkLength(decompLength);

        // 5 - File Offset
        //fm.skip(1);
        //long offset = IntConverter.unsign(IntConverter.changeFormat(fm.readInt()));
        byte[] offsetBytes = new byte[] { 0, 0, 0, fm.readByte(), fm.readByte(), fm.readByte(), fm.readByte(), fm.readByte() };
        long offset = LongConverter.convertBig(offsetBytes);
        FieldValidator.checkOffset(offset, arcSize);
        //offsets[i] = offset;

        String filename = Resource.generateFilename(i);

        //path,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, filename, offset, decompLength, decompLength, exporter);

        TaskProgressManager.setValue(i);
      }

      // now go through and read the compressed block sizes
      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];

        int decompLength = (int) resource.getDecompressedLength();

        int numBlocks = decompLength / decompBlockSize;
        int lastBlock = decompLength % decompBlockSize;
        if (lastBlock != 0) {
          numBlocks++;
        }

        long offset = resource.getOffset();
        long totalCompLength = 0;

        long[] blockOffsets = new long[numBlocks];
        long[] blockLengths = new long[numBlocks];
        long[] blockDecompLengths = new long[numBlocks];

        for (int b = 0; b < numBlocks; b++) {
          // 2 - Compressed Block Length
          int compLength = ShortConverter.unsign(ShortConverter.changeFormat(fm.readShort()));

          blockOffsets[b] = offset;
          blockLengths[b] = compLength;
          blockDecompLengths[b] = decompBlockSize;

          if (b == numBlocks - 1) {
            if (lastBlock != 0) {
              blockDecompLengths[b] = lastBlock;
            }
          }

          offset += compLength;

          totalCompLength += compLength;
        }

        BlockExporterWrapper blockExporter = new BlockExporterWrapper(exporter, blockOffsets, blockLengths, blockDecompLengths);
        resource.setLength(totalCompLength);
        resource.setExporter(blockExporter);
      }

      fm.close();

      // Now lets read the first file, which is the file list, and set the proper filenames
      Resource firstFile = resources[0];
      byte[] decompBytes = new byte[(int) firstFile.getDecompressedLength()];
      fm = new FileManipulator(new ByteBuffer(decompBytes));
      firstFile.extract(fm);
      fm.close(); // force it to write out all the buffered content

      fm = new FileManipulator(new ByteBuffer(decompBytes));
      for (int i = 1; i < numFiles; i++) { // start at file #1
        Resource resource = resources[i];

        // X - Filename
        // 1 - End of Line (byte (10));
        String filename = fm.readLine();
        FieldValidator.checkFilename(filename);

        resource.setName(filename);
        resource.setOriginalName(filename);
      }

      String firstName = "filelist.txt";
      firstFile.setName(firstName);
      firstFile.setOriginalName(firstName);

      /*
      Arrays.sort(offsets);
      
      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];
      
        long thisOffset = resource.getOffset();
        int arrayPos = Arrays.binarySearch(offsets, thisOffset);
      
        if (arrayPos == numFiles - 1) {
          long length = arcSize - thisOffset;
          resource.setLength(length);
          //resource.setDecompressedLength(length);
        }
        else {
          long length = offsets[arrayPos + 1] - offsets[arrayPos];
          resource.setLength(length);
          //resource.setDecompressedLength(length);
        }
      
      }
      
      //calculateFileSizes(resources, arcSize);
       */

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

    if (headerInt1 == 1195464280) {
      return "xvag";
    }
    else if (headerInt1 == 1684558925) {
      return "mid";
    }

    return null;
  }

}
