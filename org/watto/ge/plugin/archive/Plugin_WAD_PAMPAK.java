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
import org.watto.component.WSPluginManager;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.ge.plugin.exporter.BlockExporterWrapper;
import org.watto.ge.plugin.exporter.Exporter_ZLib_CompressedSizeOnly;
import org.watto.io.FileManipulator;
import org.watto.io.converter.ShortConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_WAD_PAMPAK extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_WAD_PAMPAK() {

    super("WAD_PAMPAK", "WAD_PAMPAK");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Pro Beach Soccer");
    setExtensions("wad"); // MUST BE LOWER CASE
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

      // 8 - Header ("PAM_PAK" + (byte)0)
      if (fm.readString(7).equals("PAM_PAK")) {
        rating += 50;
      }
      fm.skip(1);

      // 2 - Number Of Files
      if (FieldValidator.checkNumFiles(fm.readShort())) {
        rating += 5;
      }

      // 2 - Unknown
      fm.skip(2);

      long arcSize = fm.getLength();

      // 4 - Filename Directory Length
      if (FieldValidator.checkLength(fm.readInt(), arcSize)) {
        rating += 5;
      }

      // 4 - File Data Offset
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
   Provide hints to the previewer so that certain document types are displayed appropriately
   **********************************************************************************************
   **/
  @Override
  public ViewerPlugin previewHint(Resource resource) {
    String extension = resource.getExtension();
    if (extension.equalsIgnoreCase("bat") || extension.equalsIgnoreCase("ms")) {
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

      ExporterPlugin exporter = Exporter_ZLib_CompressedSizeOnly.getInstance();

      // RESETTING GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 8 - Header ("PAM_PAK" + (byte)0)
      fm.skip(8);

      // 2 - Number Of Files
      int numFiles = ShortConverter.unsign(fm.readShort());
      FieldValidator.checkNumFiles(numFiles);

      // 2 - Number of Names
      int numNames = ShortConverter.unsign(fm.readShort());
      FieldValidator.checkNumFiles(numNames);

      // 4 - Filename Directory Length
      // 4 - File Data Offset
      fm.skip(8);

      // skip over to the filename directory
      fm.skip(numFiles * 16);

      // read the filenames
      //String[] filenames = new String[numNames];
      String[] filenames = new String[numFiles]; // because we're going to build the filenames up and discard the empty directory names
      int currentFilenamePos = numFiles - 1; // files (and filenames) are stored in reverse order, so start at the end
      String directoryName = "";
      String previousName = "";
      String previousDirectoryName = "";
      boolean previousWasDirectory = true;
      for (int i = 0; i < numNames; i++) {
        // X - Filename/Directory Name
        // 1 - null Name Terminator
        String filename = fm.readNullString();
        FieldValidator.checkFilename(filename);

        if (filename.indexOf(".") > 0 || filename.equals("00000000")) {
          // a filename

          if (previousWasDirectory) {
            // if the previous name was a directory, just add this file to the directory
            previousWasDirectory = false;

            previousName = filename;
            filename = directoryName + "\\" + filename;

            //System.out.println(filename);
            filenames[currentFilenamePos] = filename;
            currentFilenamePos--; // stored in reverse order
          }
          else {
            previousWasDirectory = false;

            // compare to the previous name... (but strip off the "_" characters for proper comparisons!)

            // first work out which name has the earlier underscore...
            int underscorePosFilename = filename.indexOf('_');
            int underscorePosPrevious = previousName.indexOf('_');
            if (underscorePosFilename > 0) {
              if (underscorePosPrevious > 0 && underscorePosPrevious < underscorePosFilename) {
                underscorePosFilename = underscorePosPrevious;
              }
            }
            else if (underscorePosPrevious > 0) {
              underscorePosFilename = underscorePosPrevious;
            }

            // now make sure both names are longer than the underscore pos
            if (underscorePosFilename > 0) {
              if (filename.length() < underscorePosFilename) {
                underscorePosFilename = filename.length();
              }
              if (previousName.length() < underscorePosFilename) {
                underscorePosFilename = previousName.length();
              }
            }

            // now compare either the full names, or the underscore-substring'd names
            int comparison = 0;
            if (underscorePosFilename > 0) {
              comparison = filename.substring(0, underscorePosFilename).compareToIgnoreCase(previousName.substring(0, underscorePosFilename));
            }
            else {
              comparison = filename.compareToIgnoreCase(previousName);
            }

            if (comparison <= 0) {
              // if it is earlier in the alphabet, it belongs to the current directory
              previousName = filename;
              filename = directoryName + "\\" + filename;

              //System.out.println(filename);
              filenames[currentFilenamePos] = filename;
              currentFilenamePos--; // stored in reverse order
            }
            else {
              // if it is later in the alphabet, it belongs to the previous directory
              int slashPos = directoryName.lastIndexOf("\\");
              if (slashPos >= 0) {
                directoryName = directoryName.substring(0, slashPos);
              }
              previousName = filename;
              filename = directoryName + "\\" + filename;

              //System.out.println(filename);
              filenames[currentFilenamePos] = filename;
              currentFilenamePos--; // stored in reverse order
            }
          }
        }
        else {
          // a directory name

          if (previousWasDirectory) {
            // if the previous name was a directory, just keep building the directory tree until we reach a file
            previousWasDirectory = true;

            if (filename.equals(".")) {
              // Want the root to be empty, not a "."
              filename = "";
              directoryName = "";

              previousName = filename;
              previousDirectoryName = previousName;
            }
            else {
              // something other than root

              // see if the directory name exists earlier in the parent path - if it does, go there...
              int slashPos = directoryName.indexOf("\\" + filename);
              if (slashPos >= 0) {
                directoryName = directoryName.substring(0, slashPos);
              }

              previousName = filename;
              directoryName += "\\" + filename;
              previousDirectoryName = previousName;
            }

          }
          else {
            previousWasDirectory = true;

            int smallestSize = filename.length();
            if (previousName.length() < smallestSize) {
              smallestSize = previousName.length();
            }

            int comparison = filename.substring(0, smallestSize).compareToIgnoreCase(previousName.substring(0, smallestSize));

            if (comparison < 0) {
              // if it is earlier in the alphabet, it belongs to the current directory

              // see if we can find the current name somewhere in the parent hierarchy - if so, set the parent to there
              int slashPos = directoryName.lastIndexOf("\\" + filename);
              if (slashPos >= 0) {
                directoryName = directoryName.substring(0, slashPos);

                previousName = filename;
                directoryName += "\\" + filename;
                filename = directoryName;
                previousDirectoryName = previousName;
              }
              else {
                // not found earlier...

                // now, see if it's also earlier than the previous directory name
                comparison = filename.compareToIgnoreCase(previousDirectoryName);
                // force cases - can't find a better way yet
                if (previousDirectoryName.equalsIgnoreCase("Away")) {
                  comparison = 1;

                  // also chop off the directory so we can't trip up on it down below...
                  slashPos = directoryName.lastIndexOf("\\");
                  directoryName = directoryName.substring(0, slashPos);
                }
                if (comparison < 0) {
                  // it belongs to the parent directory

                  // find where the previous directory name is in the directory name
                  slashPos = directoryName.lastIndexOf("\\" + previousDirectoryName);
                  if (slashPos >= 0) {
                    directoryName = directoryName.substring(0, slashPos);
                  }

                  previousName = filename;
                  directoryName += "\\" + filename;
                  filename = directoryName;
                  previousDirectoryName = previousName;
                }
                else {

                  // now, if the name is also earlier than the parent directory name, it needs to be listed as the parent directory then this directory.
                  // so, compare to the parent directory...
                  slashPos = directoryName.lastIndexOf("\\");
                  String parentDirectory = directoryName.substring(slashPos + 1);
                  String otherPart = directoryName.substring(0, slashPos);

                  comparison = filename.compareToIgnoreCase(parentDirectory);

                  //while (comparison <= 0) {
                  while (comparison > 0) {
                    slashPos = otherPart.lastIndexOf("\\");
                    if (slashPos < 0) {
                      // hit the root but didn't find a match, so just put it in the parent directory
                      otherPart = directoryName;
                      break;
                    }
                    parentDirectory = otherPart.substring(slashPos + 1);
                    otherPart = otherPart.substring(0, slashPos);

                    comparison = filename.compareToIgnoreCase(parentDirectory);

                    // force cases - can't find a better way yet
                    if (parentDirectory.equalsIgnoreCase("Goal") || parentDirectory.equalsIgnoreCase("Away")) {
                      comparison = 1;
                    }
                  }

                  previousName = filename;
                  directoryName = otherPart + "\\" + filename;
                  filename = directoryName;

                }
              }

            }
            else if (comparison == 0) {
              comparison = filename.compareToIgnoreCase(previousDirectoryName);

              if (comparison == 0) {
                // exact duplicate - ignore
              }
              else {
                directoryName += "\\" + filename;
              }

              previousName = filename;
              filename = directoryName;
              previousDirectoryName = previousName;
            }
            else {
              // if it is later in the alphabet, it belongs to the previous directory
              String parentDirectory = "";
              int slashPos = directoryName.lastIndexOf("\\");
              if (slashPos >= 0) {
                parentDirectory = directoryName.substring(slashPos + 1);
                directoryName = directoryName.substring(0, slashPos);
              }

              // see if we can find the current name somewhere in the parent hierarchy - if so, set the parent to there
              slashPos = directoryName.lastIndexOf("\\" + filename);
              if (slashPos >= 0) {
                directoryName = directoryName.substring(0, slashPos);
              }
              else {
                // check backwards through the parent tree until we find a "earlier" place to put it

                String otherPart = directoryName;

                comparison = filename.compareToIgnoreCase(parentDirectory);

                while (comparison > 0) {
                  slashPos = otherPart.lastIndexOf("\\");
                  if (slashPos < 0) {
                    // hit the root but didn't find a match, so just put it in the parent directory
                    otherPart = directoryName;

                    // force cases - can't find a better way yet
                    slashPos = otherPart.indexOf("\\Menu");
                    if (slashPos > 0) {
                      otherPart = otherPart.substring(0, slashPos);
                    }

                    break;
                  }
                  parentDirectory = otherPart.substring(slashPos + 1);
                  otherPart = otherPart.substring(0, slashPos);

                  comparison = filename.compareToIgnoreCase(parentDirectory);
                }

                directoryName = otherPart;

              }

              previousName = filename;
              directoryName += "\\" + filename;
              filename = directoryName;
              previousDirectoryName = previousName;

            }

          }

          //previousDirectoryName = previousName;

          filename = directoryName;

        }

        //filenames[i] = filename;
        //System.out.println(filename);
      }

      // go back and read the files directory
      fm.seek(20);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {
        // 4 - File Length (Compressed)
        int length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - File Offset
        int offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        String filename = filenames[i];

        //path,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, filename, offset, length);

        TaskProgressManager.setValue(i);
      }

      // Now go through each file and work out if they're compressed or not
      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];

        long offset = resource.getOffset();

        fm.seek(offset);

        // 8 - File Header ("PAM-ZLB" + (byte)0)
        String header = fm.readString(7);

        if (header.equals("PAM-ZLB")) {
          // ZLib compression - in blocks/chunks
        }
        else {
          // not compressed - go to the next file
          continue;
        }

        // skip the null byte (byte 8) at the end of the header
        fm.skip(1);

        // 4 - Number of Blocks
        int numBlocks = fm.readInt();
        FieldValidator.checkNumFiles(numBlocks);

        // 4 - Decompressed Block Length
        int decompBlockLength = fm.readInt();
        FieldValidator.checkLength(decompBlockLength);

        // 4 - Decompressed Last Block Length (0 if there is only 1 block)
        int lastDecompBlockLength = fm.readInt();
        FieldValidator.checkLength(lastDecompBlockLength);

        // 4 - Total Decompressed Length
        int decompLength = fm.readInt();
        FieldValidator.checkLength(decompLength);

        long[] blockDecompLengths = new long[numBlocks];
        long[] blockCompLengths = new long[numBlocks];
        long[] blockOffsets = new long[numBlocks];
        for (int b = 0; b < numBlocks; b++) {
          // 4 - Block Offset (Relative to the start of this file data)
          long blockOffset = fm.readInt() + offset;
          FieldValidator.checkOffset(blockOffset, arcSize);

          blockOffsets[b] = blockOffset;
        }

        // work out the block compressed lengths
        for (int b = 0; b < numBlocks - 1; b++) { // note the -1
          blockCompLengths[b] = blockOffsets[b + 1] - blockOffsets[b];
          blockDecompLengths[b] = decompBlockLength;
        }
        // and work out the details for the last block
        blockCompLengths[numBlocks - 1] = resource.getLength() - (blockOffsets[numBlocks - 1] - offset);
        if (numBlocks == 1) {
          blockDecompLengths[0] = decompBlockLength;
        }
        else {
          blockDecompLengths[numBlocks - 1] = lastDecompBlockLength;
        }

        if (numBlocks == 1) {
          // set a simple exporter

          resource.setLength(blockCompLengths[0]);
          resource.setDecompressedLength(blockDecompLengths[0]);
          resource.setOffset(blockOffsets[0]);
          resource.setExporter(exporter);
        }
        else {
          // set a multi-block exporter

          resource.setDecompressedLength(decompLength);
          resource.setOffset(blockOffsets[0]);

          BlockExporterWrapper blockExporter = new BlockExporterWrapper(exporter, blockOffsets, blockCompLengths, blockDecompLengths);
          resource.setExporter(blockExporter);
        }

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

}
