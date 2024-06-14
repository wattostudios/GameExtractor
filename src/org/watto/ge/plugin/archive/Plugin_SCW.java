/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2022 wattostudios
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
import org.watto.datatype.Archive;
import org.watto.datatype.FileType;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.exporter.BlockExporterWrapper;
import org.watto.ge.plugin.exporter.Exporter_Default;
import org.watto.ge.plugin.resource.Resource_WAV_RawAudio_Chunks;
import org.watto.io.FileManipulator;
import org.watto.io.converter.StringConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_SCW extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_SCW() {

    super("SCW", "Lord Of The Rings SCW");

    //         read write replace rename
    setProperties(true, false, false, false);

    setExtensions("scw");
    setGames("Lord Of The Rings: The Battle For Middle Earth",
        "Lord Of The Rings: The Fellowship Of The Ring",
        "Lord Of The Rings: The Two Towers",
        "Lord Of The Rings: Return Of The King");
    setPlatforms("PC");

    // MUST BE LOWER CASE !!!
    setFileTypes(new FileType("samp", "Audio Sample", FileType.TYPE_AUDIO));

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

      return rating;

    }
    catch (Throwable t) {
      return 0;
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public Resource[] read(File path) {
    try {

      addFileTypes();

      ExporterPlugin exporterDefault = Exporter_Default.getInstance();

      FileManipulator fm = new FileManipulator(path, false);

      Resource[] resources = new Resource[Archive.getMaxFiles(4)];

      long arcSize = fm.getLength();

      TaskProgressManager.setMaximum(arcSize);

      String filename = "";
      String extension = "";
      int decompLength = 0;
      int totalLength = 0;

      long[] blockOffsets = null;
      long[] blockLengths = null;
      long[] blockDecompLengths = null;
      int currentChunk = 0;

      boolean readingFile = false;

      int realNumFiles = 0;
      while (fm.getOffset() < arcSize) {

        // 4 - Type Code
        String type = StringConverter.reverse(fm.readString(4));

        if (type.equals("FILL")) {
          /*
          long paddingSize = 65536 - (fm.getOffset() % 65536);
          if (paddingSize < 65536) {
            fm.skip(paddingSize);
          }
          //System.out.println((fm.getOffset())%65536);
          */
          fm.skip(calculatePadding(fm.getOffset(), 65536));
        }
        else {
          // 4 - Length
          int length = fm.readInt() - 8;
          if (length == -8) {
            fm.seek(arcSize);
          }
          else {
            FieldValidator.checkLength(length, arcSize);

            long offset = (int) fm.getOffset();

            if (type.equals("SWVR")) {
              // the start of a file

              // 8 - null
              fm.skip(8);

              // 4 - Header (ELIF)
              String fileHeader = StringConverter.reverse(fm.readString(4));
              if (fileHeader.equals("FILE")) {
                // X - Filename (null terminated, nulls to fill)
                filename = fm.readNullString(length - 12);
                continue; // we've finished reading this chunk
              }
              else {
                // something else
                fm.relativeSeek(offset + length);
              }
            }
            else if (type.equals("CTRL")) {
              // skip it
              fm.relativeSeek(offset + length);
              continue;
            }
            else if (type.equals("STOC")) {
              // table of contents (contains longer filenames)

              // 8 - null
              fm.skip(8);

              // 4 - Header (COTS)
              //String fileHeader = StringConverter.reverse(fm.readString(4));
              fm.skip(4);

              // 4 - Data Length

              // 4 - Number of Files

              // 4 - Unknown (1)
              // 8 - null
              // 4 - Number of Files

              // for each file
              // 4 - Filename Offset
              // 4 - File Offset
              // 4 - File Length?
              // 4 - Unknown (1/2)

              // for each file
              // X - Filename
              // 1 - null Filename Terminator

              fm.relativeSeek(offset + length);
              continue;
            }
            else if (type.equals("SHOC") || type.equals("SONO")) {
              // data chunk

              // 8 - null
              fm.skip(8);

              // 4 - Header (RDHS=header)
              String fileHeader = StringConverter.reverse(fm.readString(4));

              if (fileHeader.equals("SHDR")) {
                // start of a file

                // close off the existing file
                if (readingFile) {
                  long[] oldBlockOffsets = blockOffsets;
                  blockOffsets = new long[currentChunk];
                  System.arraycopy(oldBlockOffsets, 0, blockOffsets, 0, currentChunk);

                  long[] oldBlockLengths = blockLengths;
                  blockLengths = new long[currentChunk];
                  System.arraycopy(oldBlockLengths, 0, blockLengths, 0, currentChunk);

                  long[] oldBlockDecompLengths = blockDecompLengths;
                  blockDecompLengths = new long[currentChunk];
                  System.arraycopy(oldBlockDecompLengths, 0, blockDecompLengths, 0, currentChunk);

                  if (filename == null || filename.equals("")) {
                    filename = Resource.generateFilename(realNumFiles);
                  }

                  if (!extension.equals("")) {
                    type = extension;
                  }

                  if (decompLength == 0) {
                    decompLength = totalLength;
                  }

                  ExporterPlugin blockExporter = new BlockExporterWrapper(exporterDefault, blockOffsets, blockLengths, blockDecompLengths);

                  if (extension.equals("samp")) {
                    //path,id,name,offset,length,decompLength,exporter
                    Resource_WAV_RawAudio_Chunks resource = new Resource_WAV_RawAudio_Chunks(path, filename + "." + type, blockOffsets[0], totalLength, decompLength);
                    resource.setLengths(blockLengths);
                    resource.setOffsets(blockOffsets);
                    resource.setFrequency(22050);
                    resource.setBitrate((short) 16);
                    resource.setChannels((short) 1);
                    resources[realNumFiles] = resource;
                  }
                  else {
                    //path,id,name,offset,length,decompLength,exporter
                    resources[realNumFiles] = new Resource(path, filename + "." + type, blockOffsets[0], totalLength, decompLength, blockExporter);
                  }

                  TaskProgressManager.setValue(offset);
                  realNumFiles++;
                }

                // start reading the new file

                // 4 - Unknown
                fm.skip(4);

                // 4 - File Type
                extension = StringConverter.reverse(fm.readString(4));

                // 4 - Unknown
                fm.skip(4);

                // 4 - Decompressed Length of File
                decompLength = fm.readInt();
                FieldValidator.checkLength(decompLength);

                int maxChunks = (decompLength / 8000) + 1;
                blockOffsets = new long[maxChunks];
                blockLengths = new long[maxChunks];
                blockDecompLengths = new long[maxChunks];
                currentChunk = 0;

                totalLength = 0;

                readingFile = true;

                // X - Unknown
                fm.relativeSeek(offset + length);
                continue;
              }
              else if (fileHeader.equals("SDAT")) {
                // reading a chunk of data for the existing file (UNCOMPRESSED)
                length -= 56;
                fm.skip(56 - 12);

                blockOffsets[currentChunk] = fm.getOffset();
                blockLengths[currentChunk] = length;
                blockDecompLengths[currentChunk] = length;
                currentChunk++;

                fm.skip(length);
                decompLength -= length;

                totalLength += length;

              }
              else if (fileHeader.equals("Rdat")) {
                // reading a chunk of data for the existing file (COMPRESSED)
                length -= 56;
                fm.skip(56 - 12);

                // 4 - Decompressed Length
                int chunkDecompLength = fm.readInt();
                FieldValidator.checkLength(chunkDecompLength);

                length -= 4;

                blockOffsets[currentChunk] = fm.getOffset();
                blockLengths[currentChunk] = length;
                blockDecompLengths[currentChunk] = chunkDecompLength;
                currentChunk++;

                fm.skip(length);

                totalLength += length;
              }
              else {
                // reading a chunk of data for the existing file (something else)
                ErrorLogger.log("[SCW] Unexpected data chunk type: " + type);
                fm.skip(length - 12);
              }

              continue;
            }
            else if (type.equals("PADD")) {
              // Padding
              fm.skip(length);
            }
            else {
              // something else
              ErrorLogger.log("[SCW] Unexpected chunk type: " + type);
              fm.skip(length);
            }

          }
        }
      }

      // close off the existing file
      if (readingFile) {
        long[] oldBlockOffsets = blockOffsets;
        blockOffsets = new long[currentChunk];
        System.arraycopy(oldBlockOffsets, 0, blockOffsets, 0, currentChunk);

        long[] oldBlockLengths = blockLengths;
        blockLengths = new long[currentChunk];
        System.arraycopy(oldBlockLengths, 0, blockLengths, 0, currentChunk);

        long[] oldBlockDecompLengths = blockDecompLengths;
        blockDecompLengths = new long[currentChunk];
        System.arraycopy(oldBlockDecompLengths, 0, blockDecompLengths, 0, currentChunk);

        if (filename == null || filename.equals("")) {
          filename = Resource.generateFilename(realNumFiles);
        }

        String type = extension;

        if (decompLength == 0) {
          decompLength = totalLength;
        }

        ExporterPlugin blockExporter = new BlockExporterWrapper(exporterDefault, blockOffsets, blockLengths, blockDecompLengths);

        //path,id,name,offset,length,decompLength,exporter
        resources[realNumFiles] = new Resource(path, filename + "." + type, blockOffsets[0], totalLength, decompLength, blockExporter);
        realNumFiles++;
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