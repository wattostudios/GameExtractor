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
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.exporter.Exporter_ZLib;
import org.watto.io.FileManipulator;
import org.watto.io.FilenameSplitter;
import org.watto.io.buffer.ByteBuffer;
import org.watto.io.converter.IntConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_G00S000 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_G00S000() {

    super("G00S000", "G00S000");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Marvel's Spider-Man");
    setExtensions("g00s000"); // MUST BE LOWER CASE
    setPlatforms("PS4");

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

      String filenameOnly = FilenameSplitter.getFilename(fm.getFilePath());
      if (filenameOnly.indexOf("00s0") > 0) {
        // a matching type of file
        rating += 15;
      }

      File tocFile = new File(FilenameSplitter.getDirectory(fm.getFilePath()) + File.separatorChar + "toc");
      if (tocFile.exists()) {
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

      //
      //
      // find and decompress the TOC file
      //
      //
      File tocFile = new File(path.getParent() + File.separatorChar + "toc");
      if (!tocFile.exists()) {
        return null;
      }

      FileManipulator fm = new FileManipulator(tocFile, false);

      // 4 - Unknown
      fm.skip(4);

      // 4 - Decompressed Directory
      int decompLength = fm.readInt();
      FieldValidator.checkLength(decompLength, 50000000); // actually only 18MB, but this allows for some other games potentially.

      // X - Compressed Directory (ZLib Compression)
      byte[] decompBytes = new byte[decompLength];

      Exporter_ZLib exporterZLib = Exporter_ZLib.getInstance();
      exporterZLib.open(fm, (int) fm.getLength() - 8, decompLength);
      for (int b = 0; b < decompLength; b++) {
        if (exporterZLib.available()) {
          decompBytes[b] = (byte) exporterZLib.read();
        }
      }
      exporterZLib.close();

      fm.close();

      fm = new FileManipulator(new ByteBuffer(decompBytes));

      //
      //
      // find all the individual archive files
      //
      //
      File[] arcFiles = new File[100]; // 100 max files
      long[] arcSizes = new long[100];

      int numArcFiles = 0;

      File[] filesInDir = path.getParentFile().listFiles();
      int numFilesInDir = filesInDir.length;
      for (int f = 0; f < numFilesInDir; f++) {
        File file = filesInDir[f];
        String filename = FilenameSplitter.getFilename(file);
        int splitIndex = filename.indexOf("00s0");
        if (splitIndex > 0) {
          try {
            filename = filename.substring(splitIndex + 4, splitIndex + 6);
            int fileID = Integer.parseInt(filename);
            arcFiles[fileID] = file;
            arcSizes[fileID] = file.length();

            if (fileID > numArcFiles) {
              numArcFiles = fileID;
            }
          }
          catch (Throwable t) {
          }
        }
      }

      numArcFiles++; // because the archive numbers start at 0

      //
      //
      // read the toc
      //
      //

      // 4 - Header (1TAD)
      // 4 - Unknown
      // 4 - Unknown
      // 4 - Unknown (6)
      // 4 - Unknown
      // 4 - Unknown
      // 4 - Unknown
      // 4 - Unknown
      // 4 - Unknown
      // 4 - Unknown
      // 4 - Unknown
      // 4 - Directory 2 Offset
      // 4 - Directory 2 Length
      // 4 - Unknown
      // 4 - Unknown
      // 4 - Unknown
      // 4 - Unknown
      fm.skip(68);

      // 4 - Details Directory Offset
      int dirOffset = fm.readInt();
      FieldValidator.checkOffset(dirOffset, fm.getLength());

      // X - Unknown
      fm.seek(dirOffset);

      int numFiles = (int) ((fm.getLength() - dirOffset) / 8);
      FieldValidator.checkNumFiles(numFiles / 25);

      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      int[] numFilesInArc = new int[numArcFiles]; // max 100 arcFiles
      Arrays.fill(numFilesInArc, 0);

      long[] rawOffsets = new long[numFiles];
      int[] rawArcNumbers = new int[numFiles];
      int currentNum = 0;
      for (int i = 0; i < numFiles; i++) {
        try {
          // 4 - Archive Number
          int arcNumber = fm.readInt();

          // 4 - File Offset
          long offset = IntConverter.unsign(fm.readInt());

          if (arcNumber >= numArcFiles || arcNumber < 0) {
            continue;
          }

          rawOffsets[currentNum] = offset;
          rawArcNumbers[currentNum] = arcNumber;
          numFilesInArc[arcNumber]++;

          currentNum++;

          TaskProgressManager.setValue(i);
        }
        catch (Throwable t) {
          break;
        }
      }

      // Now that we've read everything, discard the FM to reduce memory use
      fm.close();
      fm = null;

      // work out the actual number of files
      numFiles = 0;
      for (int i = 0; i < numArcFiles; i++) {
        numFiles += numFilesInArc[i];
      }

      // Now split the offsets into sorted arrays
      long[][] offsets = new long[numArcFiles][0];
      for (int i = 0; i < numArcFiles; i++) {
        offsets[i] = new long[numFilesInArc[i]];
      }
      Arrays.fill(numFilesInArc, 0);

      for (int i = 0; i < numFiles; i++) {
        try {
          int arcNumber = rawArcNumbers[i];
          long offset = rawOffsets[i];

          offsets[arcNumber][numFilesInArc[arcNumber]] = offset;
          numFilesInArc[arcNumber]++;

          TaskProgressManager.setValue(i);
        }
        catch (Throwable t) {
          numFiles = i;
          break;
        }
      }

      Resource[] resources = new Resource[numFiles];

      // now that we have all the offsets, need to calculate all the file sizes, and create the Resources
      int currentFile = 0;
      for (int a = 0; a < numArcFiles; a++) {
        int thisNumFiles = numFilesInArc[a];
        if (thisNumFiles == 0) {
          continue;
        }

        long[] thisOffsets = new long[thisNumFiles];
        System.arraycopy(offsets[a], 0, thisOffsets, 0, thisNumFiles);
        Arrays.sort(thisOffsets);
        offsets[a] = thisOffsets; // put the Shrunk array back into the Source array, so we reduce the memory footprint

        File arcFile = arcFiles[a];

        for (int i = 0; i < thisNumFiles - 1; i++) {
          long offset = thisOffsets[i];
          int length = (int) (thisOffsets[i + 1] - offset);
          String filename = Resource.generateFilename(currentFile);

          //path,name,offset,length,decompLength,exporter
          Resource resource = new Resource(arcFile, filename, offset, length);
          resource.forceNotAdded(true);
          resources[currentFile] = resource;

          TaskProgressManager.setValue(currentFile);
          currentFile++;
        }

        // add the last file for this archive
        long offset = thisOffsets[thisNumFiles - 1];
        int length = (int) (arcSizes[a] - offset);
        String filename = Resource.generateFilename(currentFile);

        //path,name,offset,length,decompLength,exporter
        Resource resource = new Resource(arcFile, filename, offset, length);
        resource.forceNotAdded(true);
        resources[currentFile] = resource;

        TaskProgressManager.setValue(currentFile);
        currentFile++;
      }

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
