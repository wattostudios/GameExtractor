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
import java.util.Arrays;
import org.watto.Language;
import org.watto.Settings;
import org.watto.datatype.FileType;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.helper.ResourceSorter_Offset;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.io.FileManipulator;
import org.watto.io.converter.IntConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_000_9 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_000_9() {

    super("000_9", "000_9");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Tomb Raider Underworld");
    setExtensions("000"); // MUST BE LOWER CASE
    setPlatforms("PC");

    // MUST BE LOWER CASE !!!
    setFileTypes(new FileType("drm", "3D Models", FileType.TYPE_IMAGE),
        new FileType("mul", "MUL Audio", FileType.TYPE_AUDIO),
        new FileType("fsb", "FSB Audio", FileType.TYPE_AUDIO));

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

      // Number Of Files
      if (FieldValidator.checkNumFiles(fm.readInt())) {
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

      //ExporterPlugin exporter = Exporter_ZLib.getInstance();

      // RESETTING GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      // 4 - Number Of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // UNKNOWN DIRECTORY
      fm.skip(numFiles * 4);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // work out how many archives there are, their sizes, and their offsets
      long arcSize = 0;
      File[] archiveFiles = new File[10];
      long[] archiveLengths = new long[10];
      long[] archiveOffsets = new long[10];
      long relOffset = 0;
      for (int i = 0; i < 10; i++) { // max 10 archives
        try {
          File archiveFile = getDirectoryFile(path, "00" + i);
          long arcLength = archiveFile.length();

          archiveFiles[i] = archiveFile;
          archiveLengths[i] = arcLength;
          archiveOffsets[i] = relOffset;

          relOffset += arcLength;
          arcSize += arcLength;
        }
        catch (Throwable t) {
          // dirFile doesn't exist - that's ok, we found the last one
        }
      }

      // 1. Get all the offsets and sort them
      // 2. Find the first offset larger than the current archive (ie the one that triggers the move to the next split file)
      // 3. Note the difference between the offset of that file and the offset of the split file
      // 4. Subtract that difference from all files from this point onwards, resetting them to the real offset

      ResourceSorter_Offset[] sorter = new ResourceSorter_Offset[numFiles];

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {
        // 4 - File Length (not including padding)
        int length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - File Offset [*2048]
        long offset = (IntConverter.unsign(fm.readInt()) * 2048);
        //FieldValidator.checkOffset(offset, arcSize);

        //System.out.println(offset + "\t" + length);

        // 4 - Unknown
        // 4 - null
        fm.skip(8);

        String filename = Resource.generateFilename(i);

        // work out which archive file contains this resource file
        File actualPath = archiveFiles[0];
        /*
        int archiveNum = 0;
        for (int a = numArchives - 1; a >= 0; a--) {
          if (offset >= archiveOffsets[a]) {
            // found the right file
            actualPath = archiveFiles[a];
            offset -= archiveOffsets[a];
            archiveNum = a;
            break;
          }
        }
        */

        //path,name,offset,length,decompLength,exporter
        Resource resource = new Resource(actualPath, filename, offset, length);
        resources[i] = resource;

        sorter[i] = new ResourceSorter_Offset(resource);

        TaskProgressManager.setValue(i);

      }

      // Sort the Resources by their offsets
      Arrays.sort(sorter);

      // Now go through, calculate the real correct offsets, and set the file appropriately if it has changed
      long offset = sorter[0].getOffset();
      long previousOffset = 0;
      int currentArcFile = 0;
      for (int i = 0; i < numFiles; i++) {
        Resource currentResource = sorter[i].getResource();

        if (currentResource.getOffset() == previousOffset) {
          // a duplicate resource - there are a few of these, need to roll back the offset and repeat it again
          offset = previousOffset;
        }
        previousOffset = currentResource.getOffset();

        //if (currentResource.getOffset() != offset) {
        //System.out.println("Old offset " + currentResource.getOffset() + "\tNew offset " + offset + "\tDifference " + (currentResource.getOffset() - offset));
        currentResource.setOffset(offset);
        currentResource.setSource(archiveFiles[currentArcFile]);
        currentResource.forceNotAdded(true);
        //}
        long length = currentResource.getLength();
        offset += (length + calculatePadding(length, 2048));

        if (offset >= archiveLengths[currentArcFile]) {
          // move to the next archive
          offset = 0;
          currentArcFile++;
        }
      }

      fm.close();

      boolean debugMode = Settings.getBoolean("DebugMode");

      if (Settings.getBoolean("IdentifyUnknownFileTypes")) {
        TaskProgressManager.setMessage(Language.get("IdentifyUnknownFileTypes"));

        // Go through, read the first little it of each file, and use it to determine what kind of file it is
        File currentFile = archiveFiles[0];
        fm = new FileManipulator(currentFile, false, 8);

        for (int i = 0; i < numFiles; i++) {
          Resource resource = sorter[i].getResource(); // use the sorted array so we're not jumping around between different files
          if (resource.getSource() != currentFile) {
            // open the next file
            fm.close();
            currentFile = resource.getSource();
            fm = new FileManipulator(currentFile, false, 8);
          }

          fm.seek(resource.getOffset());

          int fileHeader = fm.readInt();

          String extension = null;
          if (fileHeader == 1297237059) {
            extension = "drm";
          }
          else if (fileHeader == 1196314761) {
            extension = "png";
          }
          else if (fileHeader == 1380013857) {
            extension = "war";
          }
          else if (fileHeader == 561214797) {
            extension = "mus";
          }
          else if (fileHeader == 876761926) {
            extension = "fsb";
          }
          else {
            int secondHeader = fm.readInt();

            if (secondHeader == 0) {
              extension = "mul";
            }
            else if (secondHeader == -1) {
              extension = "mul";
            }
            else {
              if (debugMode) {
                if (secondHeader == 5136) {
                  extension = "type5136";
                }
                else {
                  extension = "unknown" + fileHeader;
                }
              }
              else {
                extension = "unknown";
              }
            }
          }
          resource.setExtension(extension);
          resource.setOriginalName(resource.getName()); // so it doesn't think it's been renamed

          TaskProgressManager.setValue(i);
        }

        fm.close(); // close the last file that was opened
      }

      return resources;

    }
    catch (

    Throwable t) {
      logError(t);
      return null;
    }
  }

}
