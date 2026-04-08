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

import org.watto.ErrorLogger;
import org.watto.component.WSPluginManager;
import org.watto.datatype.Archive;
import org.watto.datatype.FileType;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.ge.plugin.exporter.BlockVariableExporterWrapper;
import org.watto.ge.plugin.exporter.Exporter_Default;
import org.watto.ge.plugin.exporter.Exporter_NullWriter;
import org.watto.io.FileManipulator;
import org.watto.io.converter.IntConverter;
import org.watto.io.converter.StringConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_PRE_OPEN_2 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_PRE_OPEN_2() {

    super("PRE_OPEN_2", "PRE_OPEN_2");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Saturday Night Speedway");
    setExtensions("pre"); // MUST BE LOWER CASE
    setPlatforms("PS2");

    setFileTypes(new FileType("pst", "PS2Tex Image", FileType.TYPE_IMAGE),
        new FileType("fsb", "FSB Audio", FileType.TYPE_AUDIO),
        new FileType("sc", "Script", FileType.TYPE_DOCUMENT));

    setTextPreviewExtensions("str", "props", "sc"); // LOWER CASE

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

      // 256 - Filename (null terminated, filled with nulls)
      fm.skip(256);

      // 4 - Open Header (OPEN)
      if (fm.readString(4).equals("OPEN")) {
        rating += 50;
      }

      // 1 - null Open Header Terminator
      fm.skip(1);

      // 5 - YSize Header (YSIZE)
      if (fm.readString(5).equals("YSIZE")) {
        rating += 5;
      }

      // 4 - File Length
      // 4 - File Block Length
      int length = fm.readInt();
      int blockLength = fm.readInt();
      if (length == blockLength && FieldValidator.checkLength(length, fm.getLength())) {
        rating += 10;
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
  @SuppressWarnings("unused")
  @Override
  public Resource[] read(File path) {
    try {

      // NOTE - Compressed files MUST know their DECOMPRESSED LENGTH
      //      - Uncompressed files MUST know their LENGTH

      addFileTypes();

      ExporterPlugin exporterDefault = Exporter_Default.getInstance();
      ExporterPlugin exporterNull = Exporter_NullWriter.getInstance();

      // RESETTING GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      int numFiles = Archive.getMaxFiles();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(arcSize);

      // Loop through directory
      int realNumFiles = 0;

      String filename = null;
      int length = 0;

      long[] blockOffsets = null;
      long[] blockLengths = null;
      ExporterPlugin[] blockExporters = null;

      int maxBlockSize = 32768;

      while (fm.getOffset() < arcSize) {

        byte[] commandBytes = fm.readBytes(4);
        String command = StringConverter.convertLittle(commandBytes);

        //System.out.println(command + "\t at " + (fm.getOffset() - 4));

        if (command.equals("OPEN")) {
          // Open a file

          // 2 - Open Type
          int openType = fm.readShort();
          long offset = fm.getOffset();

          if (openType == 19968) { // "N"
            // an empty file

            length = 0;

            // Create the Resource
            //path,name,offset,length,decompLength,exporter
            resources[realNumFiles] = new Resource(path, filename, offset, length);

            TaskProgressManager.setValue(offset);
            realNumFiles++;
          }
          else if (openType == 22784) { // "Y"
            // a file, where the next field is the Size of the file

            command = fm.readString(4);

            if (command.equals("SIZE")) {
              // 4 - File Length
              length = fm.readInt();
              FieldValidator.checkLength(length); // note, the length of the file can be larger than the archive, if it has lots of seeks in it

              // prepare for blocks of data
              //int numBlocks = (length / maxBlockSize) * 2 + 2; // *2 +2 to allow for lots of seek blocks as well as a few extras
              int numBlocks = (length / 256) * 2 + 2; // *2 +2 to allow for lots of seek blocks as well as a few extras

              blockOffsets = new long[numBlocks];
              blockLengths = new long[numBlocks];
              blockExporters = new ExporterPlugin[numBlocks];

              int b = 0; // the number of blocks that we've created

              long endPointer = 0;
              boolean foundNegativeBlock = false;
              boolean justReadSizeField = false;
              while (endPointer < length) {
                //System.out.println(">>" + fm.getOffset());
                // 4 - Block Length
                long blockLength = fm.readInt();

                if (blockLength == 1262830931) { // the word SEEK
                  justReadSizeField = false;
                  foundNegativeBlock = false;
                  // 4 - Seek Offset
                  int seekOffset = fm.readInt();
                  FieldValidator.checkOffset(seekOffset, length); // need to be seeking within the size of the existing file that we're processing

                  blockLength = seekOffset - endPointer; // the number of nulls

                  // if the block length is negative, lets just skip it for now, some files seem to have this repeated over and over again
                  if (blockLength < 0) {
                    //System.out.println(">> Skipping negative block");
                    foundNegativeBlock = true;
                    continue;
                  }

                  FieldValidator.checkLength(blockLength);

                  // add the empty block
                  blockOffsets[b] = 0;
                  blockLengths[b] = blockLength;
                  blockExporters[b] = exporterNull;

                  b++;

                  endPointer = seekOffset;
                }
                else if (blockLength == 1163544915) { // the word SIZE
                  // hopefully the size is the same - if not, take this new size as gospel?
                  justReadSizeField = true;

                  int newLength = fm.readInt();
                  if (length != newLength) {
                    //System.out.println("was: " + length + "\t new: " + newLength);
                  }
                  FieldValidator.checkLength(length); // note, the length of the file can be larger than the archive, if it has lots of seeks in it
                  length = newLength;
                }
                else {
                  // X - Data Block
                  try {
                    FieldValidator.checkLength(blockLength, arcSize); // this block is in the archive, so it has to be smaller than it.
                  }
                  catch (Throwable t) {
                    //if (justReadSizeField) {
                    // we just read a SIZE field, so now we're probably just reading the next filename, so
                    // lets add an empty block here, and ready ourselves for the next filename

                    // go back 4 bytes so that we're ready to re-read the filename
                    fm.relativeSeek(fm.getOffset() - 4);

                    // work out how many empty bytes at the end of the file
                    blockLength = length - endPointer;

                    // add the empty block
                    FieldValidator.checkLength(blockLength);

                    // add the empty block
                    blockOffsets[b] = 0;
                    blockLengths[b] = blockLength;
                    blockExporters[b] = exporterNull;

                    b++;

                    endPointer += blockLength;

                    continue; // go back to the while loop
                    //}
                  }

                  justReadSizeField = false;

                  long blockOffset = fm.getOffset();

                  fm.skip(blockLength);

                  if (foundNegativeBlock) {
                    continue; // don't want this data, it's a repeat
                  }

                  foundNegativeBlock = false;

                  // add the data block
                  blockOffsets[b] = blockOffset;
                  blockLengths[b] = blockLength;
                  blockExporters[b] = exporterDefault;

                  b++;

                  endPointer += blockLength;
                }

              }

              // Now we've read the whole file, so add a Resource and move on

              // Shrink the block arrays
              long[] smallBlockOffsets = new long[b];
              long[] smallBlockLengths = new long[b];
              ExporterPlugin[] smallBlockExporters = new ExporterPlugin[b];

              System.arraycopy(blockOffsets, 0, smallBlockOffsets, 0, b);
              System.arraycopy(blockLengths, 0, smallBlockLengths, 0, b);
              System.arraycopy(blockExporters, 0, smallBlockExporters, 0, b);

              // Create the BlockExporter
              BlockVariableExporterWrapper blockExporter = new BlockVariableExporterWrapper(smallBlockExporters, smallBlockOffsets, smallBlockLengths, smallBlockLengths);

              // Create the Resource
              //path,name,offset,length,decompLength,exporter
              resources[realNumFiles] = new Resource(path, filename, offset, length, length, blockExporter);

              TaskProgressManager.setValue(offset);
              realNumFiles++;

            }
            else {
              ErrorLogger.log("[PRE_OPEN_2] Unknown Command following OPEN: " + command);
              fm.relativeSeek(fm.getOffset() - 4); // treat like a new file
              //return null;
            }

          }
          else {
            ErrorLogger.log("[PRE_OPEN_2] Unknown Open Type: " + openType);
            return null;
          }

          // Clear the variables, ready for the next file

          filename = null;
          length = 0;

          blockOffsets = null;
          blockLengths = null;
          blockExporters = null;

        }
        else if (command.equals("SIZE")) {
          // Ignore it, it's just a SIZE after we've already finished reading a file to the full size that it's supposed to be

          // 4 - File Length
          fm.skip(4);
        }
        else {
          // Assume it's a filename

          // Lets check that it's not a data block from a previous file

          if (commandBytes[3] == 0 && commandBytes[2] == 0) {
            // the command is probably a Length field, not sure what the data belongs to, so lets just skip over it for now
            int blockLength = IntConverter.convertLittle(commandBytes);
            FieldValidator.checkLength(blockLength, arcSize);

            //System.out.println(">> Skipping orphan data block of size " + blockLength);

            // X - Data Block
            fm.skip(blockLength);

            continue;
          }

          // 256 - Filename (null terminated, filled with nulls)
          filename = command + fm.readNullString(252); // we've already read the first 4 characters 
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

  /**
   **********************************************************************************************
   Provide hints to the previewer so that certain document types are displayed appropriately
   **********************************************************************************************
   **/
  @Override
  public ViewerPlugin previewHint(Resource resource) {
    String extension = resource.getExtension();
    if (extension.equalsIgnoreCase("sc")) {
      return (ViewerPlugin) WSPluginManager.getPlugin("Viewer", "TXT");
    }
    return null;
  }

}
