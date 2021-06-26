/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2021 wattostudios
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
import org.watto.io.FileManipulator;
import org.watto.io.converter.ShortConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_SDAT_SDAT extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_SDAT_SDAT() {

    super("SDAT_SDAT", "Nintendo DS SDAT Sound Archive");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Nintendo DS",
        "Custom Robo Arena");
    setExtensions("sdat"); // MUST BE LOWER CASE
    setPlatforms("NDS");

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
      if (fm.readString(4).equals("SDAT")) {
        rating += 50;
      }

      fm.skip(4);

      long arcSize = fm.getLength();

      // Archive Size
      if (FieldValidator.checkEquals(fm.readInt(), arcSize)) {
        rating += 5;
      }

      if (fm.readShort() == 64) {
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

      long arcSize = fm.getLength();

      // 4 - Header (SDAT)
      // 2 - Byte Order
      // 2 - Version
      // 4 - File Length
      // 2 - Header Size (64)
      // 2 - Number of Blocks
      fm.skip(16);

      // 4 - SYMB Block Offset
      int symbOffset = fm.readInt();
      FieldValidator.checkOffset(symbOffset, arcSize);

      // 4 - SYMB Block Length
      int symbLength = fm.readInt();

      // 4 - INFO Block Offset
      int infoOffset = fm.readInt();
      FieldValidator.checkOffset(infoOffset, arcSize);

      // 4 - INFO Block Length
      fm.skip(4);

      // 4 - FAT Block Offset
      int fatOffset = fm.readInt();
      FieldValidator.checkOffset(fatOffset, arcSize);

      // 4 - FAT Block Length
      // 4 - File Block Offset
      // 4 - File Block Length
      // X - null Padding to a multiple of 32 bytes

      fm.seek(fatOffset);

      // 4 - Header (FAT )
      // 4 - Block Length
      fm.skip(8);

      // 4 - Number of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      long[] offsets = new long[numFiles];
      long[] lengths = new long[numFiles];
      for (int i = 0; i < numFiles; i++) {

        // 4 - File Offset
        int offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);
        offsets[i] = offset;

        // 4 - File Length
        int length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);
        lengths[i] = length;

        // 8 - null
        fm.skip(8);

        TaskProgressManager.setValue(i);
      }

      // Read the names, if they exist
      String[] filenames = new String[numFiles];
      long[] filenameOffsets = new long[numFiles];
      if (symbLength != 0) {
        fm.seek(symbOffset);

        // 4 - Header (SYMB)
        // 4 - Block Length
        fm.skip(8);

        // 4 - SSEQ File List Offset (relative to the start of the SYMB block)
        // 4 - SSAR Folder List Offset (relative to the start of the SYMB block)
        // 4 - BANK File List Offset (relative to the start of the SYMB block)
        // 4 - SWAR File List Offset (relative to the start of the SYMB block)
        // 4 - Player File List Offset (relative to the start of the SYMB block)
        // 4 - Group File List Offset (relative to the start of the SYMB block)
        // 4 - Player2 File List Offset (relative to the start of the SYMB block)
        // 4 - STRM File List Offset (relative to the start of the SYMB block)
        // 24 - null
        long[] nameOffsets = new long[8];
        int[] nameCounts = new int[8];
        for (int n = 0; n < 8; n++) {
          long offset = fm.readInt();
          if (offset == 0) {
            nameOffsets[n] = 0;
          }
          else {
            nameOffsets[n] = symbOffset + offset;
          }
        }

        int currentName = 0;

        long offset = nameOffsets[0];
        if (offset != 0) {
          fm.seek(offset);

          // 4 - Number of SSEQ Entries
          int numEntries = fm.readInt();
          nameCounts[0] = numEntries;
          if (numEntries != 0) {
            FieldValidator.checkNumFiles(numEntries);
            for (int e = 0; e < numEntries; e++) {
              // 4 - Filename Offset (relative to the start of the SYMB block)
              int filenameOffset = fm.readInt() + symbOffset;
              FieldValidator.checkOffset(filenameOffset, arcSize);
              filenameOffsets[currentName] = filenameOffset;
              currentName++;
            }
          }
        }
        else {
          nameCounts[0] = 0;
        }

        offset = nameOffsets[1];
        if (offset != 0) {
          fm.seek(offset);

          // 4 - Number of SSAR Entries
          int numEntries = fm.readInt();
          nameCounts[1] = numEntries;
          if (numEntries != 0) {
            FieldValidator.checkNumFiles(numEntries);
            for (int e = 0; e < numEntries; e++) {
              // 4 - Filename Offset (relative to the start of the SYMB block)
              int filenameOffset = fm.readInt() + symbOffset;
              FieldValidator.checkOffset(filenameOffset, arcSize);
              filenameOffsets[currentName] = filenameOffset;
              currentName++;

              // 4 - SSEQ File List Offset (relative to the start of the SYMB block)
              fm.skip(4);
            }
          }
        }
        else {
          nameCounts[1] = 0;
        }

        for (int n = 2; n < 8; n++) {
          offset = nameOffsets[n];

          if (n == 4 || n == 5 || n == 6) {
            // don't want the names for Player, Group, or Player2 as they aren't real files
            nameCounts[n] = 0;
            continue;
          }

          if (offset != 0) {
            fm.seek(offset);

            // 4 - Number of Entries
            int numEntries = fm.readInt();
            nameCounts[n] = numEntries;
            if (numEntries != 0) {
              FieldValidator.checkNumFiles(numEntries);
              for (int e = 0; e < numEntries; e++) {
                // 4 - Filename Offset (relative to the start of the SYMB block)
                int filenameOffset = fm.readInt() + symbOffset;
                FieldValidator.checkOffset(filenameOffset, arcSize);
                filenameOffsets[currentName] = filenameOffset;
                currentName++;
              }
            }
          }
          else {
            nameCounts[n] = 0;
          }
        }

        // now we have all the offsets, so get the names, and append the type to it

        String[] types = new String[] { "SSEQ", "SSAR", "BANK", "SWAR", "Player", "Group", "Player2", "STRM" };

        currentName = -1;
        int currentTypeIndex = -1;
        String currentType = "";
        for (int i = 0; i < numFiles; i++) {
          if (currentName <= 0) {
            currentTypeIndex++;
            currentType = types[currentTypeIndex];
            currentName = nameCounts[currentTypeIndex];
          }

          offset = filenameOffsets[i];
          fm.relativeSeek(offset);

          // X - Filename (null)
          String filename = fm.readNullString();
          FieldValidator.checkFilename(filename);

          filename += "." + currentType;
          filenames[i] = filename;
          TaskProgressManager.setValue(i);

          currentName--;
        }
      }

      // now read the INFO
      fm.seek(infoOffset);

      // 4 - Header (INFO)
      // 4 - Block Length
      fm.skip(8);

      // 4 - SSEQ Info List Offset (relative to the start of the INFO block)
      // 4 - SSAR Info List Offset (relative to the start of the INFO block)
      // 4 - BANK Info List Offset (relative to the start of the INFO block)
      // 4 - SWAR Info List Offset (relative to the start of the INFO block)
      // 4 - Player Info List Offset (relative to the start of the INFO block)
      // 4 - Group Info List Offset (relative to the start of the INFO block)
      // 4 - Player2 Info List Offset (relative to the start of the INFO block)
      // 4 - STRM Info List Offset (relative to the start of the INFO block)
      // 24 - null

      long[] infoOffsets = new long[8];
      for (int n = 0; n < 8; n++) {
        long offset = fm.readInt();
        if (offset == 0) {
          infoOffsets[n] = 0;
        }
        else {
          infoOffsets[n] = infoOffset + offset;
        }
      }

      // 4 - Number of SSEQ Entries
      int currentFile = 0;
      long offset = infoOffsets[0];
      if (offset != 0) {
        fm.relativeSeek(offset);
        int numEntries = fm.readInt();
        if (numEntries != 0) {
          FieldValidator.checkNumFiles(numEntries);

          for (int i = 0; i < numEntries; i++) {
            // 2 - FAT File ID
            int fileID = ShortConverter.unsign(fm.readShort());

            // 2 - Unknown
            // 2 - Associated BANK
            // 1 - Volume
            // 1 - CPR
            // 1 - PPR
            // 1 - PLY
            // 2 - Unknown
            fm.skip(10);

            //System.out.println(fileID);
            fileID = currentFile;
            currentFile++;

            offset = offsets[fileID];
            long length = lengths[fileID];
            String filename = filenames[fileID];

            //path,name,offset,length,decompLength,exporter
            resources[fileID] = new Resource(path, filename, offset, length);

            TaskProgressManager.setValue(fileID);
          }
        }
      }

      // 4 - Number of SSAR Entries
      offset = infoOffsets[1];
      if (offset != 0) {
        fm.relativeSeek(offset);
        int numEntries = fm.readInt();
        if (numEntries != 0) {
          FieldValidator.checkNumFiles(numEntries);

          for (int i = 0; i < numEntries; i++) {
            // 2 - FAT File ID
            int fileID = ShortConverter.unsign(fm.readShort());

            // 2 - Unknown
            fm.skip(2);

            //System.out.println(fileID);
            fileID = currentFile;
            currentFile++;

            offset = offsets[fileID];
            long length = lengths[fileID];
            String filename = filenames[fileID];

            //path,name,offset,length,decompLength,exporter
            resources[fileID] = new Resource(path, filename, offset, length);

            TaskProgressManager.setValue(fileID);
          }
        }
      }

      // 4 - Number of BANK Entries
      offset = infoOffsets[2];
      if (offset != 0) {
        fm.relativeSeek(offset);
        int numEntries = fm.readInt();
        if (numEntries != 0) {
          FieldValidator.checkNumFiles(numEntries);

          for (int i = 0; i < numEntries; i++) {
            // 2 - FAT File ID
            int fileID = ShortConverter.unsign(fm.readShort());

            // 2 - Unknown
            // 2 - SWAR #1
            // 2 - SWAR #2
            // 2 - SWAR #3
            // 2 - SWAR #4
            fm.skip(10);

            //System.out.println(fileID);
            fileID = currentFile;
            currentFile++;

            offset = offsets[fileID];
            long length = lengths[fileID];
            String filename = filenames[fileID];

            //path,name,offset,length,decompLength,exporter
            resources[fileID] = new Resource(path, filename, offset, length);

            TaskProgressManager.setValue(fileID);
          }
        }
      }

      // 4 - Number of SWAR Entries
      offset = infoOffsets[3];
      if (offset != 0) {
        fm.relativeSeek(offset);
        int numEntries = fm.readInt();
        if (numEntries != 0) {
          FieldValidator.checkNumFiles(numEntries);

          for (int i = 0; i < numEntries; i++) {
            // 2 - FAT File ID
            int fileID = ShortConverter.unsign(fm.readShort());

            // 2 - Unknown
            fm.skip(2);

            //System.out.println(fileID);
            fileID = currentFile;
            currentFile++;

            offset = offsets[fileID];
            long length = lengths[fileID];
            String filename = filenames[fileID];

            //path,name,offset,length,decompLength,exporter
            resources[fileID] = new Resource(path, filename, offset, length);

            TaskProgressManager.setValue(fileID);
          }
        }
      }

      /*
      // 4 - Number of Player Entries
      numEntries = fm.readInt();
      if (numEntries != 0) {
        FieldValidator.checkNumFiles(numEntries);
      
        // 1 - Unknown
        // 3 - Padding
        // 4 - Unknown
        fm.skip(numEntries * 8);
      }
      
      // 4 - Number of Group Entries
      numEntries = fm.readInt();
      if (numEntries != 0) {
        FieldValidator.checkNumFiles(numEntries);
      
        for (int i = 0; i < numEntries; i++) {
          // 4 - Number of Entries in the Group
          int numInGroup = fm.readInt();
          FieldValidator.checkNumFiles(numInGroup + 1); // +1 to allow 0 entries in a group
      
          // 4 - ID
          // 4 - Index
          fm.skip(numInGroup * 8);
        }
      
      }
      
      // 4 - Number of Player2 Entries
      numEntries = fm.readInt();
      if (numEntries != 0) {
        FieldValidator.checkNumFiles(numEntries);
      
        // 1 - Number of Used Entries in the below array
        // 16 - Unknown Array
        // 7 - Unknown
        fm.skip(numEntries * 24);
      }
      */

      // 4 - Number of STRM Entries
      offset = infoOffsets[7];
      if (offset != 0) {
        fm.relativeSeek(offset);
        int numEntries = fm.readInt();
        if (numEntries != 0) {
          FieldValidator.checkNumFiles(numEntries);

          for (int i = 0; i < numEntries; i++) {
            // 2 - FAT File ID
            int fileID = ShortConverter.unsign(fm.readShort());

            // 2 - Unknown
            // 1 - Volumn
            // 1 - Priority
            // 1 - Play
            // 5 - Unknown
            fm.skip(10);

            //System.out.println(fileID);
            fileID = currentFile;
            currentFile++;

            offset = offsets[fileID];
            long length = lengths[fileID];
            String filename = filenames[fileID];

            //path,name,offset,length,decompLength,exporter
            resources[fileID] = new Resource(path, filename, offset, length);

            TaskProgressManager.setValue(fileID);
          }
        }
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
