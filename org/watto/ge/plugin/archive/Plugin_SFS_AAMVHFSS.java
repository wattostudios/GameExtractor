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
import org.watto.component.WSPluginManager;
import org.watto.datatype.FileType;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.ge.plugin.exporter.BlockExporterWrapper;
import org.watto.ge.plugin.exporter.Exporter_Default;
import org.watto.io.FileManipulator;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_SFS_AAMVHFSS extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_SFS_AAMVHFSS() {

    super("SFS_AAMVHFSS", "SFS_AAMVHFSS");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("7 Wonders: The Treasures of Seven",
        "Little Farm",
        "Midnight Mysteries: The Edgar Allan Poe Conspiracy",
        "Samantha Swift and the Hidden Roses of Athena");
    setExtensions("sfs"); // MUST BE LOWER CASE
    setPlatforms("PC");

    // MUST BE LOWER CASE !!!
    setFileTypes(new FileType("ani", "Animation XML", FileType.TYPE_DOCUMENT),
        new FileType("jp2", "JPEG Image", FileType.TYPE_IMAGE));

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
      if (fm.readString(8).equals("AAMVHFSS")) {
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
   Provide hints to the previewer so that certain document types are displayed appropriately
   **********************************************************************************************
   **/
  @Override
  public ViewerPlugin previewHint(Resource resource) {
    String extension = resource.getExtension();
    if (extension.equalsIgnoreCase("xml") || extension.equalsIgnoreCase("win") || extension.equalsIgnoreCase("ani") || extension.equalsIgnoreCase("fnt") || extension.equalsIgnoreCase("x")) {
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

      //ExporterPlugin exporter = Exporter_ZLib.getInstance();

      // RESETTING GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 8 - Header (AAMVHFSS)
      // 4 - Unknown (-1)
      // 264 - null
      // 2 - Unknown
      // 2 - Useful Key
      // 8 - Header 2 (AASFSSGN)
      // 8 - Hash? CSC?
      fm.skip(296);

      // 4 - Chunk Size (4096)
      int chunkSize = fm.readInt();
      FieldValidator.checkRange(chunkSize, 0, 100000);

      // 4 - Unknown (8)
      // 4 - Unknown (1)
      // 4 - Unknown (1)
      // 4 - Unknown (2)
      // 4 - Unknown (522)
      // 4 - Directory Tree Offset? [*chunkSize +280]
      fm.skip(24);

      // 4 - Number of Entries
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // 4 - Number of Chunks in the Archive
      int numChunks = fm.readInt();
      FieldValidator.checkLength(numChunks, arcSize);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      fm.getBuffer().setBufferSize(chunkSize);
      fm.flush();

      // go to the first chunk
      fm.seek(4 * chunkSize + 280);

      int realNumFiles = 0;

      String[] directoryNames = new String[numFiles];
      int[] parentIDs = new int[numFiles];

      int currentFile = 0;

      // Loop through directory
      boolean moreChunks = true;
      while (moreChunks && currentFile < numFiles) {

        // 4 - Next Chunk Offset [*chunkSize +280] (-1 for no more chunks)
        int nextChunkOffset = fm.readInt();
        if (nextChunkOffset == 0 || nextChunkOffset == -1) {
          moreChunks = false;
        }
        else {
          FieldValidator.checkRange(nextChunkOffset, 0, numChunks);

          nextChunkOffset = (nextChunkOffset * chunkSize) + 280;
        }

        // 4 - Unknown
        // 4 - Unknown (1)
        // 4 - Unknown (4567)
        // 4 - null
        // 4 - Header Size (32)
        // 4 - null
        // 4 - Unknown
        fm.skip(28);

        int numFilesInChunk = (chunkSize - 512) / 512;
        for (int c = 0; c < numFilesInChunk; c++) {

          if (currentFile >= numFiles) {
            break; // in case the last chunk isn't full of files
          }

          // 4 - File Data Offset [*chunkSize +280] (-1 for a directory or an empty file)
          int fileDataOffset = fm.readInt();
          if (fileDataOffset != 0 && fileDataOffset != -1) {
            fileDataOffset = (fileDataOffset * chunkSize) + 280;
            FieldValidator.checkOffset(fileDataOffset, arcSize);
          }

          // 8 - File Length (0 for a directory)
          long fileLength = fm.readLong();
          if (fileLength != 0 && fileLength != -1) {
            FieldValidator.checkLength(fileLength, arcSize);
          }

          // 8 - Timestamp?
          // 8 - Timestamp?
          // 8 - Timestamp?
          // 4 - Unknown (32) (0 for a file)
          fm.skip(28);

          // 4 - Index of the Parent directory (first directory = 0)
          int parentID = fm.readInt();
          if (parentID == -1) {
            parentIDs[currentFile] = -1;
          }
          else {
            FieldValidator.checkRange(parentID, 0, numFiles);
            parentIDs[currentFile] = parentID;
          }

          // 176 - null
          fm.skip(176);

          // 4 - Entry Type (0=File, 1=Directory)
          int entryType = fm.readInt();

          // 288 - Filename (null terminated, filled with nulls)
          String filename = fm.readNullString(288);
          //System.out.println(filename);

          directoryNames[currentFile] = filename;

          if (entryType == 1) {
            // just a directory - continue...
            //System.out.println("  ^ is a directory");

            resources[currentFile] = null;
            currentFile++; // need to increment here, because of the continue;

            continue;

          }
          else if (entryType == 0) {

            //System.out.println("  ^ is a file at offset " + fileDataOffset);

            long currentOffset = fm.getOffset();
            fm.seek(fileDataOffset);

            long[] overallFileChunkOffsets = new long[0];

            boolean moreChunksForFileChunkTable = true;
            while (moreChunksForFileChunkTable) {
              // 4 - Next Chunk Offset [*chunkSize +280] (-1 for no more chunks)
              int nextChunkForFileChunkTable = fm.readInt();
              if (nextChunkForFileChunkTable == 0 || nextChunkForFileChunkTable == -1) {
                moreChunksForFileChunkTable = false;
              }
              else {
                FieldValidator.checkRange(nextChunkForFileChunkTable, 0, numChunks);

                nextChunkForFileChunkTable = (nextChunkForFileChunkTable * chunkSize) + 280;
              }

              // 4 - Unknown (1)
              // 4 - Unknown (5)
              // 4 - Unknown
              // 4 - Unknown (1)
              // 4 - Unknown
              // 4 - Unknown
              // 4 - Unknown
              fm.skip(28);

              int maxFileChunks = (chunkSize - 32) / 4;
              int realNumFileChunks = 0;
              long[] fileChunks = new long[maxFileChunks];
              for (int f = 0; f < maxFileChunks; f++) {
                // 4 - File Data Chunk Offset [*chunkSize +280]
                int fileChunkOffset = fm.readInt();
                if (fileChunkOffset == 0 || fileChunkOffset == -1) {
                  // no more
                  break;
                }
                else {
                  FieldValidator.checkRange(fileChunkOffset, 0, numChunks);

                  fileChunkOffset = (fileChunkOffset * chunkSize) + 280 + 32; // +32 to skip the chunk header implicitly
                  fileChunks[realNumFileChunks] = fileChunkOffset;
                  realNumFileChunks++;
                }
              }

              // if this is the last descriptor, shrink it
              if (realNumFileChunks < maxFileChunks) {
                long[] oldChunks = fileChunks;
                fileChunks = new long[realNumFileChunks];
                System.arraycopy(oldChunks, 0, fileChunks, 0, realNumFileChunks);
              }

              // if there was multiple chunks, increase the larger array and add these to it
              if (overallFileChunkOffsets.length == 0) {
                overallFileChunkOffsets = fileChunks;
              }
              else {
                long[] oldOverallFileChunkOffsets = overallFileChunkOffsets;
                overallFileChunkOffsets = new long[oldOverallFileChunkOffsets.length + realNumFileChunks];
                System.arraycopy(oldOverallFileChunkOffsets, 0, overallFileChunkOffsets, 0, oldOverallFileChunkOffsets.length);
                System.arraycopy(fileChunks, 0, overallFileChunkOffsets, oldOverallFileChunkOffsets.length, realNumFileChunks);
              }

              // move to the next chunk for processing
              if (moreChunksForFileChunkTable) {
                fm.seek(nextChunkForFileChunkTable);
              }

            }

            // So now we have all the chunks, so lets calc the lengths
            int numChunksInFile = overallFileChunkOffsets.length;
            long[] lengths = new long[numChunksInFile];
            int maxChunkSize = chunkSize - 32;
            for (int l = 0; l < numChunksInFile - 1; l++) {
              lengths[l] = maxChunkSize;
            }
            lengths[numChunksInFile - 1] = fileLength % maxChunkSize;

            // Now save the file

            //path,name,offset,length,decompLength,exporter
            BlockExporterWrapper exporter = new BlockExporterWrapper(Exporter_Default.getInstance(), overallFileChunkOffsets, lengths, lengths);
            resources[currentFile] = new Resource(path, filename, fileDataOffset, fileLength, fileLength, exporter);

            TaskProgressManager.setValue(currentFile);
            currentFile++;
            realNumFiles++; // to count the number of actual files

            // go back to the directory to read the next file
            fm.seek(currentOffset);

          }
          else {
            ErrorLogger.log("[SFS_AAMVHFSS] Unknown entry type: " + entryType);
          }
        }

        // move to the next chunk for processing
        if (moreChunks) {
          fm.seek(nextChunkOffset);
        }

      }

      // Now go through and set all the parent directory names (and filter out all the directories, leaving the files only)
      Resource[] realResources = new Resource[realNumFiles];
      for (int i = 0; i < numFiles; i++) {
        // if it's a directory, set the name as the parent
        if (resources[i] == null) {
          int parentID = parentIDs[i];
          if (parentID != -1 && parentID != i) {
            directoryNames[i] = directoryNames[parentID] + "\\" + directoryNames[i];
          }
        }
      }
      realNumFiles = 0;
      for (int i = 0; i < numFiles; i++) {
        // now filter out the files and set the directory names
        if (resources[i] != null) {
          int parentID = parentIDs[i];
          String name = directoryNames[i];
          if (parentID != -1 && parentID != i) {
            name = directoryNames[parentID] + "\\" + name;
          }
          Resource resource = resources[i];
          resource.setName(name);
          resource.setOriginalName(name);
          realResources[realNumFiles] = resource;
          realNumFiles++;
        }
      }
      // now set the returning array to only contain the files
      resources = realResources;

      fm.close();

      return resources;

    }
    catch (Throwable t) {
      logError(t);
      return null;
    }
  }

}
