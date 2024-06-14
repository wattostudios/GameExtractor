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

import org.watto.Language;
import org.watto.datatype.FileType;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.exporter.BlockExporterWrapper;
import org.watto.ge.plugin.exporter.Exporter_Default;
import org.watto.ge.plugin.exporter.Exporter_Deflate;
import org.watto.ge.plugin.exporter.SubsetExporterWrapper;
import org.watto.io.FileManipulator;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_ARC_13 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_ARC_13() {

    super("ARC_13", "ARC_13");

    //         read write replace rename
    setProperties(true, true, false, false);

    setGames("Mad Max");
    setExtensions("arc"); // MUST BE LOWER CASE
    setPlatforms("PC");

    setFileTypes(new FileType("sarc", "SARC Archive", FileType.TYPE_ARCHIVE),
        new FileType("fsb", "FSB Audio Archive", FileType.TYPE_ARCHIVE),
        new FileType("hkx", "Havok Pack File", FileType.TYPE_ARCHIVE));

    //setTextPreviewExtensions("colours", "rat", "screen", "styles"); // LOWER CASE

    setCanScanForFileTypes(true);

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

      getDirectoryFile(fm.getFile(), "tab");
      rating += 25;

      // Header
      if (fm.readString(4).equals("")) {
        rating += 50;
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

      // RESETTING GLOBAL VARIABLES

      long arcSize = (int) path.length();

      File sourcePath = getDirectoryFile(path, "tab");
      FileManipulator fm = new FileManipulator(sourcePath, false);

      // 4 - Block Size (2048)
      fm.skip(4);

      // 4 - Number of Chunks
      int numChunks = fm.readInt();
      FieldValidator.checkNumFiles(numChunks + 1); // allow for 0 chunked files
      //unknownCount = numChunks;

      // grab the chunk filename CRCs so we know which files are chunked, and can come back and fix up later
      int[] chunkCRCs = new int[numChunks];
      Resource[] chunkedFiles = new Resource[numChunks];
      int chunksFound = 0;

      for (int c = 0; c < numChunks; c++) {
        // 4 - Filename CRC // to match with a file in the details directory
        int filenameCRC = fm.readInt();
        chunkCRCs[c] = filenameCRC;

        // 4 - Number of Pieces
        int numPieces = fm.readInt();
        FieldValidator.checkNumFiles(numPieces);

        fm.skip(numPieces * 8);
      }

      int numFiles = (int) (sourcePath.length() - fm.getOffset()) / 16;
      FieldValidator.checkNumFiles(numFiles);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {

        // 4 - Filename CRC
        int filenameCRC = fm.readInt();

        // 4 - File Offset
        long offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - Compressed File Length
        long length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - Decompressed File Length
        long decompLength = fm.readInt();
        FieldValidator.checkLength(decompLength);

        String filename = Resource.generateFilename(i);

        Resource resource = null;
        if (length == decompLength) {
          //path,name,offset,length,decompLength,exporter
          resource = new Resource(path, filename, offset, length, decompLength);
        }
        else {
          //path,name,offset,length,decompLength,exporter
          resource = new Resource(path, filename, offset, length, decompLength, exporter);
        }
        resource.addProperty("Hash", filenameCRC);
        resources[i] = resource;

        // see if this file is chunked or not
        for (int c = 0; c < numChunks; c++) {
          if (filenameCRC == chunkCRCs[c]) {
            chunkedFiles[chunksFound] = resource;
            chunksFound++;
          }
        }

        TaskProgressManager.setValue(i);
      }

      // now go back and process the chunks (now that we know the files and their sizes
      fm.seek(8);

      for (int c = 0; c < numChunks; c++) {
        Resource chunkedFile = chunkedFiles[c];

        // 4 - Filename CRC // to match with a file in the details directory
        fm.skip(4);

        // 4 - Number of Pieces
        int numPieces = fm.readInt();
        FieldValidator.checkNumFiles(numPieces);

        // for each piece

        long[] blockOffsets = new long[numPieces];
        long[] blockLengths = new long[numPieces];
        long[] blockDecompLengths = new long[numPieces];

        long offset = chunkedFile.getOffset();
        long compLength = chunkedFile.getLength();
        long decompLength = chunkedFile.getDecompressedLength();

        // for each piece
        long[] decompOffsets = new long[numPieces + 1]; // +1 to store the totalDecompOffset in the last one, for length calculations
        long[] compOffsets = new long[numPieces + 1]; // +1 to store the totalCompOffset in the last one, for length calculations
        for (int p = 0; p < numPieces; p++) {
          // 4 - Decompressed Offset for this Piece (relative to the start of this files decompressed data)
          long blockDecompOffset = fm.readInt();
          decompOffsets[p] = blockDecompOffset;

          // 4 - Compressed Offset for this Piece (relative to the start of this files compressed data)
          long blockCompOffset = fm.readInt();
          compOffsets[p] = blockCompOffset;
        }
        compOffsets[numPieces] = compLength;
        decompOffsets[numPieces] = decompLength;

        for (int p = 0; p < numPieces; p++) {
          blockOffsets[p] = offset + compOffsets[p];
          blockLengths[p] = compOffsets[p + 1] - compOffsets[p];
          blockDecompLengths[p] = decompOffsets[p + 1] - decompOffsets[p];
        }

        /*
        blockOffsets[numPieces - 1] = offset;
        blockLengths[numPieces - 1] = compLength;
        blockDecompLengths[numPieces - 1] = decompLength;
        */

        ExporterPlugin blockExporter = new BlockExporterWrapper(exporter, blockOffsets, blockLengths, blockDecompLengths);
        chunkedFile.setExporter(blockExporter);
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
   * Writes an [archive] File with the contents of the Resources
   **********************************************************************************************
   **/
  @Override
  public void write(Resource[] resources, File path) {
    try {

      ExporterPlugin exporterDefault = Exporter_Default.getInstance();

      int numFiles = resources.length;
      TaskProgressManager.setMaximum(numFiles);

      // WRITE THE FILE THAT CONTAINS THE DIRECTORY
      File dirPath = getDirectoryFile(path, "tab", false);
      FileManipulator fm = new FileManipulator(dirPath, true);

      // Calculate the number of chunked files
      int maxChunkDecompLength = 46137344;
      int numChunks = 0;

      boolean[] chunkedFile = new boolean[numFiles];
      for (int i = 0; i < numFiles; i++) {
        if (resources[i].getDecompressedLength() > maxChunkDecompLength) {
          chunkedFile[i] = true;
          numChunks++;
        }
        else {
          chunkedFile[i] = false;
        }
      }

      // Write Header Data

      TaskProgressManager.setMessage(Language.get("Progress_PerformingCalculations"));

      // 4 - Block Size (2048)
      fm.writeInt(2048);

      // 4 - Number of Chunks
      fm.writeInt(numChunks);

      // skip the chunk details for now, we'll have to come back and write them when we have run the compression
      int currentPos = 0;
      int skipLength = 0;
      for (int c = 0; c < numChunks; c++) {
        for (int i = currentPos; i < numFiles; i++) {
          if (chunkedFile[i]) {
            int decompLength = (int) resources[i].getDecompressedLength();
            int numPieces = decompLength / maxChunkDecompLength;
            if (decompLength % maxChunkDecompLength > 0) {
              numPieces++;
            }
            // 4 - Filename CRC // to match with a file in the details directory
            // 4 - Number of Pieces
            // numPieces*8 - Piece data
            skipLength += (8 + numPieces * 8);

            currentPos = i + 1;
          }
        }
      }

      fm.seek(8 + skipLength); // skip over the chunks directory

      // Write Directory
      // Note that we have to come back here to write the proper offsets and compLengths after we know what they'll be
      TaskProgressManager.setMessage(Language.get("Progress_WritingDirectory"));
      long offset = 0;
      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];
        int length = (int) resource.getLength();
        int decompLength = (int) resource.getDecompressedLength();

        int hash = 0;
        String hashString = resource.getProperty("Hash");
        if (hashString != null && !hashString.equals("")) {
          hash = Integer.parseInt(hashString);
        }

        // 4 - Hash?
        fm.writeInt(hash);

        // 4 - File Offset
        fm.writeInt((int) offset);

        // 4 - Compressed File Length
        fm.writeInt(length);

        // 4 - Decompressed File Length
        fm.writeInt(decompLength);

        offset += length;
        offset += calculatePadding(length, 2048);
      }

      FileManipulator tabFM = fm; // because we need to write the chunk information later on
      tabFM.seek(8);

      // WRITE THE FILE THAT CONTAINS THE DATA
      fm = new FileManipulator(path, true);

      ExporterPlugin exporter = Exporter_Deflate.getInstance();

      // Write Files

      //long[] totalCompLengths = new long[numChunks];
      //int currentChunk = 0;

      long[] actualOffsets = new long[numFiles];
      long[] actualCompLengths = new long[numFiles];

      TaskProgressManager.setMessage(Language.get("Progress_WritingFiles"));
      for (int i = 0; i < resources.length; i++) {
        Resource resource = resources[i];

        actualOffsets[i] = fm.getOffset();

        if (chunkedFile[i]) {
          if (!resource.isReplaced()) {
            // easier - just copy the compressed data we already have

            int length = (int) resource.getLength();

            // Need to reset the exporter so that we just extract the files using existing compression (and so we don't do the block extract of DDS files)
            ExporterPlugin originalExporter = resource.getExporter();
            resource.setExporter(exporterDefault);

            // X - File Data
            write(resource, fm);

            // put the exporter back again
            resource.setExporter(originalExporter);

            // 0-2047 - Padding to a multiple of 2048 bytes (byte 48)
            int paddingSize = calculatePadding(length, 2048);
            for (int p = 0; p < paddingSize; p++) {
              fm.writeByte(48);
            }

            // now write the chunk details to the tabFM
            BlockExporterWrapper blockExporter = (BlockExporterWrapper) resource.getExporter();
            long[] compLengths = blockExporter.getBlockLengths();
            long[] decompLengths = blockExporter.getDecompLengths();
            int numPieces = compLengths.length;

            int hash = 0;
            String hashString = resource.getProperty("Hash");
            if (hashString != null && !hashString.equals("")) {
              hash = Integer.parseInt(hashString);
            }

            // get ready to write the chunk details to the tabFM
            // 4 - Filename CRC // to match with a file in the details directory
            tabFM.writeInt(hash);

            // 4 - Number of Pieces
            tabFM.writeInt(numPieces);

            int localCompOffset = 0;
            int localDecompOffset = 0;
            for (int p = 0; p < numPieces; p++) {
              // write the chunk details to the tabFM
              // 4 - Decompressed Offset for this Piece (relative to the start of this files decompressed data)
              tabFM.writeInt(localDecompOffset);

              // 4 - Compressed Offset for this Piece (relative to the start of this files compressed data)
              tabFM.writeInt(localCompOffset);

              localDecompOffset += decompLengths[p];
              localCompOffset += compLengths[p];
            }

            actualCompLengths[i] = (int) resource.getLength(); // we already know the total length

          }
          else {
            // harder - need to compress the file in pieces, and return the compressed piece lengths
            int decompLength = (int) resource.getDecompressedLength();
            int numPieces = decompLength / maxChunkDecompLength;
            int lastPieceLength = decompLength % maxChunkDecompLength;
            if (lastPieceLength != 0) {
              numPieces++;
            }

            long totalCompLength = 0;

            int hash = 0;
            String hashString = resource.getProperty("Hash");
            if (hashString != null && !hashString.equals("")) {
              hash = Integer.parseInt(hashString);
            }

            // get ready to write the chunk details to the tabFM
            // 4 - Filename CRC // to match with a file in the details directory
            tabFM.writeInt(hash);

            // 4 - Number of Pieces
            tabFM.writeInt(numPieces);

            long currentOffset = resource.getOffset();
            File sourceFile = resource.getSource();
            int localCompOffset = 0;
            int localDecompOffset = 0;
            for (int p = 0; p < numPieces - 1; p++) {
              // write the piece
              Resource pieceResource = new Resource(sourceFile, "", currentOffset, maxChunkDecompLength, maxChunkDecompLength);
              long compLength = write(exporter, pieceResource, fm);
              currentOffset += maxChunkDecompLength;

              // write the chunk details to the tabFM
              // 4 - Decompressed Offset for this Piece (relative to the start of this files decompressed data)
              tabFM.writeInt(localDecompOffset);

              // 4 - Compressed Offset for this Piece (relative to the start of this files compressed data)
              tabFM.writeInt(localCompOffset);

              localDecompOffset += maxChunkDecompLength;
              localCompOffset += compLength;

              totalCompLength += compLength;
            }

            // the last (smaller) piece
            Resource pieceResource = new Resource(sourceFile, "", currentOffset, lastPieceLength, lastPieceLength);
            long compLength = write(exporter, pieceResource, fm);

            // write the chunk details to the tabFM
            // 4 - Decompressed Offset for this Piece (relative to the start of this files decompressed data)
            tabFM.writeInt(localDecompOffset);

            // 4 - Compressed Offset for this Piece (relative to the start of this files compressed data)
            tabFM.writeInt(localCompOffset);

            totalCompLength += compLength;

            actualCompLengths[i] = totalCompLength;

            // 0-2047 - Padding to a multiple of 2048 bytes (byte 48)
            int paddingSize = calculatePadding(totalCompLength, 2048);
            for (int p = 0; p < paddingSize; p++) {
              fm.writeByte(48);
            }

          }
        }
        else {

          int length = (int) resource.getLength();

          // Need to reset the exporter so that we just extract the files using existing compression (and so we don't do the block extract of DDS files)
          ExporterPlugin originalExporter = resource.getExporter();
          resource.setExporter(exporterDefault);

          // X - File Data
          write(resource, fm);

          // put the exporter back again
          resource.setExporter(originalExporter);

          // 0-2047 - Padding to a multiple of 2048 bytes (byte 48)
          int paddingSize = calculatePadding(length, 2048);
          for (int p = 0; p < paddingSize; p++) {
            fm.writeByte(48);
          }

          actualCompLengths[i] = length;
        }

        TaskProgressManager.setValue(i);
      }

      // now that we've written the chunks, and we know the correct offset and compLength for each file, we need to write it to tabFM
      // tabFM is already at the end of the chunkDirectory, so we can just start iterating through the normal directory now, to the right place
      for (int i = 0; i < resources.length; i++) {
        // 4 - Filename CRC
        tabFM.skip(4);

        // 4 - File Offset
        tabFM.writeInt(actualOffsets[i]);

        // 4 - Compressed File Length
        tabFM.writeInt(actualCompLengths[i]);

        // 4 - Decompressed File Length
        tabFM.skip(4);
      }

      tabFM.close();

      fm.close();

    }
    catch (Throwable t) {
      logError(t);
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

    if (headerInt1 == 1094993440) {
      return "adf";
    }
    else if (headerInt2 == 542327876) {
      // DDS with a 4-byte header and some content at the end of it
      resource.setExporter(new SubsetExporterWrapper(resource.getExporter(), resource.getOffset(), resource.getLength(), resource.getDecompressedLength(), 4, headerInt1));
      return "dds";
    }
    else if (headerInt2 == 1129464147) {
      return "sarc";
    }
    else if (headerInt1 == 5391702) {
      return "ver";
    }
    else if (headerInt1 == 893539142) {
      return "fsb";
    }
    else if (headerInt1 == 843924545) {
      return "adm2";
    }
    else if (headerInt1 == 1129337938) {
      return "rtpc";
    }
    else if (headerInt1 == 1474355287) {
      return "hkx";
    }

    return null;
  }

}
