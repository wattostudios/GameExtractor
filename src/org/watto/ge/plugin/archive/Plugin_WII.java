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

import org.watto.datatype.Archive;
import org.watto.datatype.FileType;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.io.FileManipulator;
import org.watto.io.converter.ByteConverter;
import org.watto.io.converter.IntConverter;
import org.watto.io.converter.ShortConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_WII extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_WII() {

    super("WII", "WII");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Phineas and Ferb: Quest for Cool Stuff");
    setExtensions("wii"); // MUST BE LOWER CASE
    setPlatforms("Wii");

    // MUST BE LOWER CASE !!!
    setFileTypes(new FileType("wii_tex", "Texture Image", FileType.TYPE_IMAGE));

    setTextPreviewExtensions("led"); // LOWER CASE

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

      if (fm.readInt() == 1) {
        rating += 5;
      }

      if (IntConverter.changeFormat(fm.readInt()) == 2) {
        rating += 5;
      }

      fm.skip(8);

      if (fm.getFile().getName().startsWith(fm.readNullString(12))) {
        rating += 25;
      }

      fm.skip(32);

      if (fm.readString(4).equals("Thor")) {
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

      // 4 - Unknown (1) (LITTLE ENDIAN)
      // 4 - Unknown (2)
      // 4 - null
      // 2 - Unknown
      // 2 - Unknown
      // 12 - Archive Filename without Extension (null terminated, filled with nulls)
      // 32 - Game Short Name (null terminated, filled with nulls)
      // 4 - Unknown ("Thor")
      // 4 - null
      // 4 - Unknown (-1)
      // 4 - null
      fm.skip(76);

      int maxNumDirectories = 1000; // guess
      int numDirectories = 0;

      int[] dirOffsets = new int[maxNumDirectories];
      int[] dirNumFiles = new int[maxNumDirectories];

      int earliestDirectory = (int) arcSize;
      while (fm.getOffset() < earliestDirectory) {
        // 4 - Number of Files in this Directory (can be null)
        int numFilesInDir = IntConverter.changeFormat(fm.readInt());

        // 4 - Directory Offset (-1 if there are no files in this directory)
        int dirOffset = IntConverter.changeFormat(fm.readInt());

        if (numFilesInDir != 0) {
          FieldValidator.checkNumFiles(numFilesInDir);
          FieldValidator.checkOffset(dirOffset, arcSize);

          dirNumFiles[numDirectories] = numFilesInDir;
          dirOffsets[numDirectories] = dirOffset;
          numDirectories++;

          if (dirOffset < earliestDirectory) {
            earliestDirectory = dirOffset;
          }
        }
      }

      int numFiles = Archive.getMaxFiles();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(arcSize);

      // Loop through directory
      int realNumFiles = 0;
      for (int d = 0; d < numDirectories; d++) {
        fm.relativeSeek(dirOffsets[d]);

        int numFilesInDir = dirNumFiles[d];

        for (int i = 0; i < numFilesInDir; i++) {
          // 2 - File Type
          // 2 - File ID?
          fm.skip(4);

          // 4 - File Data Offset
          int offset = IntConverter.changeFormat(fm.readInt());
          FieldValidator.checkOffset(offset, arcSize);

          // 4 - File Length
          int length = IntConverter.changeFormat(fm.readInt());
          FieldValidator.checkLength(length, arcSize);

          if (length == 0 && offset == 0) {
            continue; // skip empty files
          }

          String filename = Resource.generateFilename(realNumFiles);

          //path,name,offset,length,decompLength,exporter
          resources[realNumFiles] = new Resource(path, filename, offset, length);

          TaskProgressManager.setValue(offset);
          realNumFiles++;
        }
      }

      resources = resizeResources(resources, realNumFiles);

      numFiles = realNumFiles;

      fm.getBuffer().setBufferSize(256); // small quick reads
      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];

        fm.relativeSeek(resource.getOffset());

        // 4 - Unknown (all byte 171)
        // 2 - Unknown (773)
        // 2 - Unknown (2)
        // 4 - null
        fm.skip(12);

        // 1 - File Type?
        int type = ByteConverter.unsign(fm.readByte());
        // 3 - Unknown
        fm.skip(3);

        String typeString = "." + type;
        if (type == 60) {
          typeString = ".txt"; // text string
        }
        else if (type == 99) {
          typeString = ""; // a real file, with a file extension attached to the filename
        }
        else if (type == 34) {
          typeString = ".wii_tex"; // image
        }

        // 32 - Filename (null terminated, filled with nulls)
        String filename = fm.readNullString(32);
        if (filename != null && filename.length() > 0) {
          // ok
          filename += typeString;

          resource.setName(filename);
          resource.setOriginalName(filename);
        }

        try {
          if (type == 60) {
            // For TEXT files, just skip to the actual text data

            fm.skip(22);
            // 2 - Text String Length
            int length = ShortConverter.changeFormat(fm.readShort());

            long offset = fm.getOffset();

            FieldValidator.checkLength(length, offset + resource.getLength());

            resource.setOffset(offset);
            resource.setLength(length);
            resource.setDecompressedLength(length);
          }
          else if (type == 99) {
            // For real files, just skip to the actual file data

            fm.skip(68);

            // 4 - Source Path Length
            int sourcePathLength = IntConverter.changeFormat(fm.readInt());

            if (sourcePathLength == 1) {
              sourcePathLength = IntConverter.changeFormat(fm.readInt());
              FieldValidator.checkFilenameLength(sourcePathLength);

              // 4 - File Data Length
              int length = IntConverter.changeFormat(fm.readInt());
              FieldValidator.checkLength(length, resource.getLength());

              // X - Source File Path
              filename = fm.readNullString(sourcePathLength);
              int colonPos = filename.indexOf(':');
              if (colonPos > 0) {
                filename = filename.substring(colonPos + 2);
              }

              // X - File Data
              long offset = fm.getOffset();

              resource.setOffset(offset);
              resource.setLength(length);
              resource.setDecompressedLength(length);
              resource.setName(filename);
              resource.setOriginalName(filename);
            }
            else {
              FieldValidator.checkFilenameLength(sourcePathLength);

              // X - Source File Path
              filename = fm.readNullString(sourcePathLength);
              int colonPos = filename.indexOf(':');
              if (colonPos > 0) {
                filename = filename.substring(colonPos + 2);
              }

              // X - File Data
              long offset = fm.getOffset();
              long length = resource.getLength() - (offset - resource.getOffset());

              resource.setOffset(offset);
              resource.setLength(length);
              resource.setDecompressedLength(length);
              resource.setName(filename);
              resource.setOriginalName(filename);
            }
          }
        }
        catch (Throwable t) {
          // don't worry about it, just keep the names and sizes as they are
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
