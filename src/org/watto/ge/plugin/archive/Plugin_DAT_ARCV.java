/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2024 wattostudios
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
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

import org.watto.datatype.Archive;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.exporter.BlockVariableExporterWrapper;
import org.watto.ge.plugin.exporter.Exporter_Default;
import org.watto.ge.plugin.exporter.Exporter_Deflate;
import org.watto.io.FileManipulator;
import org.watto.io.buffer.ByteBuffer;
import org.watto.io.converter.ShortConverter;
import org.watto.io.stream.ManipulatorUnclosableInputStream;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_DAT_ARCV extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_DAT_ARCV() {

    super("DAT_ARCV", "DAT_ARCV");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Guacamelee!",
        "Guacamelee! 2");
    setExtensions("dat"); // MUST BE LOWER CASE
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
      if (fm.readString(4).equals("ARCV")) {
        rating += 50;
      }

      if (fm.readShort() == 256) {
        rating += 5;
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

      ExporterPlugin exporter = Exporter_Deflate.getInstance();
      ExporterPlugin exporterDefault = Exporter_Default.getInstance();

      // RESETTING GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 4 - Header (ARCV)
      // 2 - Unknown (256)

      fm.skip(6);

      // 4 - Directory Offset [+14]
      int compDirLength = fm.readInt();
      int dirOffset = compDirLength + 14;
      FieldValidator.checkOffset(dirOffset, arcSize);

      fm.skip(9);

      // read the compressed directory chunks
      int processedLength = 9;
      int maxDirBlocks = 100; // guess
      int numDirBlocks = 0;
      long[] dirBlockLengths = new long[maxDirBlocks];
      while (processedLength < compDirLength) {
        // 2 - Comp Block Length
        int compBlockLength = ShortConverter.unsign(fm.readShort());
        dirBlockLengths[numDirBlocks] = compBlockLength;
        numDirBlocks++;

        processedLength += compBlockLength + 2;

      }

      // decompress the directory
      int decompDirLength = numDirBlocks * 65536;
      byte[] dirBytes = new byte[decompDirLength];
      int outPos = 0;
      int currentOffset = (int) fm.getOffset();
      for (int b = 0; b < numDirBlocks; b++) {
        fm.relativeSeek(currentOffset);// just to be sure

        int compLength = (int) dirBlockLengths[b];
        currentOffset += compLength;

        int decompLength = 65536;

        InflaterInputStream readSource = new InflaterInputStream(new ManipulatorUnclosableInputStream(fm), new Inflater(true));
        for (int p = 0; p < decompLength; p++) {
          readSource.available();
          dirBytes[outPos] = (byte) readSource.read();
          outPos++;
        }
        readSource.close();
      }

      //FileManipulator tempFM = new FileManipulator(new File("C:\\temp.txt"), true);
      //tempFM.writeBytes(dirBytes);
      //tempFM.close();

      // read the filenames
      FileManipulator nameFM = new FileManipulator(new ByteBuffer(dirBytes));

      // 4 - Number of Files
      int numNames = nameFM.readInt();
      FieldValidator.checkNumFiles(numNames);

      // 4 - Actual Length of Decompressed Data (not including padding to a 65536 multiple)

      // for each file
      //   1 - Unknown
      nameFM.skip(4 + numNames);

      String[] names = new String[numNames];
      for (int n = 0; n < numNames; n++) {
        // X - Filename
        // 1 - null Filename Terminator
        String name = nameFM.readNullString();
        FieldValidator.checkFilename(name);

        if (name.startsWith(".\\")) {
          name = name.substring(2);
        }
        names[n] = name;
      }

      nameFM.close();

      fm.seek(dirOffset); // make sure we're at the right spot

      int numFiles = Archive.getMaxFiles();
      int realNumFiles = 0;

      TaskProgressManager.setMaximum(arcSize);

      // Loop through directory
      long[] offsets = new long[numFiles];
      while (fm.getOffset() < arcSize) {

        // 4 - File Offset
        int offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        if (offset + fm.getOffset() >= arcSize) {
          break;
        }

        offsets[realNumFiles] = offset;
        realNumFiles++;

        TaskProgressManager.setValue(offset);
      }

      numFiles = realNumFiles;
      Resource[] resources = new Resource[numFiles];

      long dataOffset = fm.getOffset();

      for (int i = 0; i < numFiles; i++) {
        long offset = offsets[i] + dataOffset;
        //String filename = Resource.generateFilename(i);
        String filename = names[i];

        //path,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, filename, offset);
      }

      calculateFileSizes(resources, arcSize);

      fm.getBuffer().setBufferSize(realNumFiles);

      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];

        long offset = resource.getOffset();
        long nextOffset = offset + resource.getLength();

        fm.seek(offset);
        TaskProgressManager.setValue(offset);

        // 9 - Unknown
        fm.skip(9);

        int maxChunks = 900; // guess

        long[] blockOffsets = new long[maxChunks];
        long[] blockLengths = new long[maxChunks];
        long[] blockDecompLengths = new long[maxChunks];

        int numBlocks = 0;

        long thisOffset = fm.getOffset();

        while (thisOffset < nextOffset) {
          // 2 - Compressed Chunk Length
          int compBlockLength = ShortConverter.unsign(fm.readShort());

          blockLengths[numBlocks] = compBlockLength;
          numBlocks++;

          if (compBlockLength == 0) {
            compBlockLength = 65536;
          }

          thisOffset += compBlockLength + 2;
        }

        ExporterPlugin[] blockExporters = new ExporterPlugin[numBlocks];

        long relOffset = fm.getOffset();
        for (int b = 0; b < numBlocks; b++) {
          long blockOffset = relOffset;
          long compBlockLength = blockLengths[b];

          if (compBlockLength == 0) {
            blockExporters[b] = exporterDefault;

            blockOffsets[b] = blockOffset;
            blockDecompLengths[b] = 65536;
            compBlockLength = 65536;
            blockLengths[b] = compBlockLength;
            relOffset += compBlockLength;
          }
          else {
            blockExporters[b] = exporter;

            blockOffsets[b] = blockOffset;
            blockDecompLengths[b] = 65536;
            relOffset += compBlockLength;
          }

        }

        if (numBlocks < maxChunks) {
          long[] oldBlockOffsets = blockOffsets;
          blockOffsets = new long[numBlocks];
          System.arraycopy(oldBlockOffsets, 0, blockOffsets, 0, numBlocks);

          long[] oldBlockLengths = blockLengths;
          blockLengths = new long[numBlocks];
          System.arraycopy(oldBlockLengths, 0, blockLengths, 0, numBlocks);

          long[] oldDecompLengths = blockDecompLengths;
          blockDecompLengths = new long[numBlocks];
          System.arraycopy(oldDecompLengths, 0, blockDecompLengths, 0, numBlocks);
        }

        int totalDecompLength = numBlocks * 65536;
        resource.setDecompressedLength(totalDecompLength);

        ExporterPlugin blockExporter = new BlockVariableExporterWrapper(blockExporters, blockOffsets, blockLengths, blockDecompLengths);
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
