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
import org.watto.datatype.Archive;
import org.watto.datatype.FileType;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.io.FileManipulator;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_FAB_FARC extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_FAB_FARC() {

    super("FAB_FARC", "FAB_FARC");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Sunday vs Magazine: Shūketsu! Chōjō Daikessen");
    setExtensions("fab"); // MUST BE LOWER CASE
    setPlatforms("PSP");

    // MUST BE LOWER CASE !!!
    setFileTypes(new FileType("gim", "GIM Image", FileType.TYPE_IMAGE));

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
      if (fm.readString(4).equals("FARC")) {
        rating += 50;
      }

      if (fm.readInt() == 256) {
        rating += 5;
      }

      fm.skip(4);

      int length1 = fm.readInt();
      int length2 = fm.readInt();

      if (length1 == length2) {
        rating += 5;
      }

      long arcSize = fm.getLength();

      // Next Block Offset
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

      //ExporterPlugin exporter = Exporter_ZLib.getInstance();

      // RESETTING GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      int numFiles = Archive.getMaxFiles();
      int realNumFiles = 0;

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(arcSize);

      // Loop through directory
      String previousHeader = null;
      while (fm.getOffset() < arcSize) {

        // 4 - Header
        String header = fm.readString(4);

        System.out.println((fm.getOffset() - 4) + "\t" + header);

        // 4 - Unknown
        fm.skip(4);

        // 4 - Number of Entries
        int numEntries = fm.readInt();

        // 4 - Block Length
        int blockLength = fm.readInt();

        if (header.equals("FARC")) {
          blockLength -= 16;
          FieldValidator.checkLength(blockLength, arcSize);
          fm.skip(blockLength);
        }
        else if (header.equals("CHNK")) {
          // empty, but just in case
          FieldValidator.checkLength(blockLength, arcSize);
          fm.skip(blockLength);
        }
        else if (header.equals("CHAR") || header.equals("BACK")) {
          FieldValidator.checkLength(blockLength, arcSize);
          fm.skip(blockLength);
        }
        else if (header.equals("TEXR")) {
          // do nothing, want to read the inner block
        }
        else if (header.equals("TARC")) {
          FieldValidator.checkNumFiles(numEntries);

          long baseOffset = fm.getOffset() - 16;

          long largestOffset = 0;
          int largestLength = 0;

          for (int i = 0; i < numEntries; i++) {
            // 56 - Filename (null terminated, filled with nulls)
            String filename = fm.readNullString(56);
            FieldValidator.checkFilename(filename);
            filename = previousHeader + "\\" + filename;

            // 4 - File Offset (relative to the start of the TARC Block)
            long offset = fm.readInt() + baseOffset;
            FieldValidator.checkOffset(offset, arcSize);

            // 4 - File Length
            int length = fm.readInt();
            FieldValidator.checkLength(length, arcSize);

            //path,name,offset,length,decompLength,exporter
            resources[realNumFiles] = new Resource(path, filename, offset, length);
            realNumFiles++;

            TaskProgressManager.setValue(offset);

            if (offset > largestOffset) {
              largestOffset = offset;
              largestLength = length;
            }
          }

          long nextBlockOffset = largestOffset + largestLength;

          fm.seek(nextBlockOffset);
        }
        else if (header.equals("MODL")) {
          long baseOffset = fm.getOffset();

          long nextBlockOffset = baseOffset + blockLength;
          FieldValidator.checkOffset(nextBlockOffset, arcSize);

          long endOffset = nextBlockOffset;
          while (fm.getOffset() < endOffset) {
            // 36 - Filename (null terminated, filled with nulls)
            String filename = fm.readNullString(36);
            FieldValidator.checkFilename(filename);
            filename = header + "\\" + filename;

            // 4 - Texture Number? (starts at 1)
            fm.skip(4);

            // 4 - File Length
            int length = fm.readInt();
            FieldValidator.checkLength(length, arcSize);

            // 4 - File Offset (relative to the start of the MODL Block) [+16 for the header]
            long offset = fm.readInt() + baseOffset;
            FieldValidator.checkOffset(offset, arcSize);

            //path,name,offset,length,decompLength,exporter
            resources[realNumFiles] = new Resource(path, filename, offset, length);
            realNumFiles++;

            TaskProgressManager.setValue(offset);

            if (offset < endOffset) {
              endOffset = offset;
            }
          }

          fm.seek(nextBlockOffset);
        }
        else if (header.equals("MOTN")) {
          long baseOffset = fm.getOffset();

          long nextBlockOffset = baseOffset + blockLength;
          FieldValidator.checkOffset(nextBlockOffset, arcSize);

          long endOffset = nextBlockOffset;
          while (fm.getOffset() < endOffset) {
            // 40 - Filename (null terminated, filled with nulls)
            String filename = fm.readNullString(40);
            FieldValidator.checkFilename(filename);
            filename = header + "\\" + filename;

            // 4 - File Length
            int length = fm.readInt();
            FieldValidator.checkLength(length, arcSize);

            // 4 - File Offset (relative to the start of the MOTN Block) [+16 for the header]
            long offset = fm.readInt() + baseOffset;
            FieldValidator.checkOffset(offset, arcSize);

            //path,name,offset,length,decompLength,exporter
            resources[realNumFiles] = new Resource(path, filename, offset, length);
            realNumFiles++;

            TaskProgressManager.setValue(offset);

            if (offset < endOffset) {
              endOffset = offset;
            }
          }

          fm.seek(nextBlockOffset);
        }
        else if (header.equals("UVAM")) {
          FieldValidator.checkLength(blockLength, arcSize);
          fm.skip(blockLength);
        }
        else if (header.equals("SEQH")) {
          FieldValidator.checkNumFiles(numEntries);

          long baseOffset = fm.getOffset();

          long largestOffset = 0;
          int largestLength = 0;

          for (int i = 0; i < numEntries; i++) {
            // 32 - Unknown
            // 4 - Unknown
            // 4 - Unknown
            fm.skip(40);
            String filename = Resource.generateFilename(realNumFiles) + ".seq";
            filename = header + "\\" + filename;

            // 4 - File Offset (relative to the start of the SEQH Block) [+16 for the header]
            long offset = fm.readInt() + baseOffset;
            FieldValidator.checkOffset(offset, arcSize);

            // 4 - File Length
            int length = fm.readInt();
            FieldValidator.checkLength(length, arcSize);

            //path,name,offset,length,decompLength,exporter
            resources[realNumFiles] = new Resource(path, filename, offset, length);
            realNumFiles++;

            TaskProgressManager.setValue(offset);

            if (offset > largestOffset || (offset >= largestOffset && largestLength == 0)) {
              largestOffset = offset;
              largestLength = length;
            }
          }

          long nextBlockOffset = largestOffset + largestLength;
          fm.seek(nextBlockOffset);
        }
        else if (header.equals("EMIT")) {
          FieldValidator.checkNumFiles(numEntries);

          fm.skip(numEntries * 288);
        }
        else {
          ErrorLogger.log("[FAB_FARC] Unknown Entry Header: " + header);
        }

        previousHeader = header;
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
