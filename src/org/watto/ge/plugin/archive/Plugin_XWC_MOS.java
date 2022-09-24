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
public class Plugin_XWC_MOS extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_XWC_MOS() {

    super("XWC_MOS", "XWC_MOS");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("The Chronicles of Riddick: Escape From Butcher Bay");
    setExtensions("xwc", "xtc");
    setPlatforms("PC");

    // MUST BE LOWER CASE !!!
    setFileTypes(new FileType("xtc_tex", "XTC Texture Image", FileType.TYPE_IMAGE));

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
      if (fm.readString(15).equals("MOS DATAFILE2.0")) {
        rating += 50;
      }

      // null
      if (fm.readByte() == 0) {
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

      // 16 - Header ("MOS DATAFILE2.0" + null)
      // 8 - null
      fm.skip(24);

      // 4 - Directory Offset
      int dirOffset = fm.readInt();
      FieldValidator.checkOffset(dirOffset, arcSize);

      // 20 - null
      fm.seek(dirOffset);

      // 24 - Directory Type String (VERSION,WAVEDATA,WAVEINFO,WAVEDESC,SFXDESC,LIPSYNC, etc...) (padded with null bytes)
      String dirType = fm.readNullString(24);

      // 4 - Next Directory Offset
      dirOffset = fm.readInt();
      FieldValidator.checkOffset(dirOffset, arcSize);

      // 4 - null
      // 4 - Offset to Entries for this Directory
      // 4 - Unknown (maybe table data pointer location)
      // 4 - Number of entries at that location
      // 4 - null
      fm.seek(dirOffset);

      int numFiles = 0;

      long earliestDirOffset = 0;

      if (dirType.equals("TEXTURES")) {
        // OK

        // 24 - Directory Type String (VERSION,WAVEDATA,WAVEINFO,WAVEDESC,SFXDESC,LIPSYNC, etc...) (padded with null bytes)
        dirType = fm.readNullString(24);

        // 4 - Next Directory Offset
        // 4 - null
        fm.skip(8);

        // 4 - Offset to Entries for this Directory
        dirOffset = fm.readInt();
        FieldValidator.checkOffset(dirOffset, arcSize);

        // 4 - Unknown (maybe table data pointer location)
        fm.skip(4);

        // 4 - Number of entries at that location
        numFiles = fm.readInt();
        FieldValidator.checkNumFiles(numFiles);

        // 4 - null
        fm.seek(dirOffset);

        earliestDirOffset = dirOffset;

      }
      else if (dirType.equals("SOURCE")) {
        // 24 - Directory Type String (IMAGELIST)
        dirType = fm.readNullString(24);

        earliestDirOffset = dirOffset;

        if (dirType.equals("IMAGELIST")) {
          // 4 - null
          fm.skip(4);

          // 4 - next Directory Offset
          dirOffset = fm.readInt();
          FieldValidator.checkOffset(dirOffset, arcSize);
          fm.seek(dirOffset);

          // 24 - Directory Type String (DIRECTORYHEADER)
          dirType = fm.readNullString(24);
        }

        if (dirType.equals("DIRECTORYHEADER")) {
          // 4 - next Directory Offset
          dirOffset = fm.readInt();
          FieldValidator.checkOffset(dirOffset, arcSize);
          fm.seek(dirOffset);

          // 24 - Directory Type String (IMAGEDIRECTORY4)
          dirType = fm.readNullString(24);
        }

        if (dirType.equals("IMAGEDIRECTORY4")) {
          // 4 - next Directory Offset
          // 4 - null
          fm.skip(8);

          // 4 - Dir Offset
          dirOffset = fm.readInt();
          FieldValidator.checkOffset(dirOffset, arcSize);

          // 4 - Dir End Offset
          fm.skip(4);

          // 4 - Number of Entries
          numFiles = fm.readInt();
          FieldValidator.checkNumFiles(numFiles);

          fm.seek(dirOffset);
        }

      }
      else if (dirType.equals("VERSION")) {
        // 24 - Directory Type String (WAVEDATA)
        dirType = fm.readNullString(24);

        earliestDirOffset = dirOffset;

        if (dirType.equals("WAVEDATA")) {
          // 4 - Directory Offset
          dirOffset = fm.readInt();
          FieldValidator.checkOffset(dirOffset, arcSize);
          fm.seek(dirOffset);

          // 24 - Directory Type String (WAVEINFO)
          dirType = fm.readNullString(24);
        }

        if (dirType.equals("WAVEINFO")) {
          // 4 - next Directory Offset
          // 4 - null
          fm.skip(8);

          // 4 - Dir Offset
          dirOffset = fm.readInt();
          FieldValidator.checkOffset(dirOffset, arcSize);

          // 4 - Dir End Offset
          fm.skip(4);

          // 4 - Number of Entries
          numFiles = fm.readInt();
          FieldValidator.checkNumFiles(numFiles);

          fm.seek(dirOffset);
        }

      }
      else {
        ErrorLogger.log("[XWC_MOS] Unrecognized primary directory type: " + dirType);
        return null;
      }

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      if (dirType.equals("IMAGEDIRECTORY5")) {
        // OK

        // Loop through directory
        for (int i = 0; i < numFiles; i++) {

          // 4 - Unknown (-1)
          // 4 - null
          fm.skip(8);

          // 4 - Filename Length (not including padding)
          int filenameLength = fm.readInt();
          FieldValidator.checkFilenameLength(filenameLength);

          // X - Filename
          String filename = fm.readString(filenameLength) + ".xtc_tex";

          // 0-3 - Padding to a multiple of 4 bytes (padded with (byte)32)
          fm.skip(calculatePadding(filenameLength, 4));

          // 4 - null
          fm.skip(4);

          // 4 - Number of Mipmaps
          int mipmapCount = fm.readInt();

          // 4 - Unknown (768)
          // 4 - Unknown
          // 4 - Unknown
          // 4 - Unknown
          // 4 - Unknown
          // 4 - Unknown
          // 4 - null
          fm.skip(28);

          // 4 - File Offset
          int offset = fm.readInt();
          FieldValidator.checkOffset(offset, arcSize);

          // 4 - Unknown
          // 6 - null
          // 4 - Unknown (1792)
          // 4 - null
          fm.skip(18);

          //path,name,offset,length,decompLength,exporter
          Resource resource = new Resource(path, filename, offset);
          resource.addProperty("MipmapCount", mipmapCount);
          resources[i] = resource;

          TaskProgressManager.setValue(i);
        }
      }
      else if (dirType.equals("IMAGEDIRECTORY4")) {
        // OK

        // Loop through directory
        for (int i = 0; i < numFiles; i++) {

          // 4 - Unknown (-1)
          // 4 - null
          fm.skip(8);

          // 4 - Filename Length (not including padding)
          int filenameLength = fm.readInt();
          FieldValidator.checkFilenameLength(filenameLength);

          // X - Filename
          String filename = fm.readString(filenameLength) + ".xtc_tex";

          // 0-3 - Padding to a multiple of 4 bytes (padded with (byte)32)
          fm.skip(calculatePadding(filenameLength, 4));

          // 4 - Number of Mipmaps
          int mipmapCount = fm.readInt();

          // 4 - Unknown (768)
          // 4 - Unknown
          // 4 - Unknown
          // 4 - Unknown
          // 4 - Unknown
          // 4 - Unknown
          // 4 - null
          fm.skip(28);

          // 4 - File Offset
          int offset = fm.readInt();
          FieldValidator.checkOffset(offset, arcSize);

          // for each mipmap
          //   4 - File Offset
          fm.skip((mipmapCount - 1) * 4);

          // 4 - Unknown
          // 6 - null
          // 4 - Unknown (1792)
          // 4 - null
          fm.skip(18);

          //path,name,offset,length,decompLength,exporter
          Resource resource = new Resource(path, filename, offset);
          resource.addProperty("MipmapCount", mipmapCount);
          resources[i] = resource;

          TaskProgressManager.setValue(i);
        }

        calculateFileSizes(resources, earliestDirOffset);

      }
      else if (dirType.equals("WAVEINFO")) {
        // OK

        // Loop through directory
        for (int i = 0; i < numFiles; i++) {

          // 4 - File Offset
          int offset = fm.readInt() + 40; // 40-byte header
          FieldValidator.checkOffset(offset, arcSize);

          // 4 - File Length
          int length = fm.readInt() - 40; // 40-byte header
          FieldValidator.checkLength(length, arcSize);

          // 4 - Filename Length (not including padding)
          int filenameLength = fm.readInt();
          FieldValidator.checkFilenameLength(filenameLength);

          // X - Filename
          String filename = fm.readString(filenameLength) + ".ogg";

          // 0-3 - Padding to a multiple of 4 bytes (padded with (byte)32)
          fm.skip(calculatePadding(filenameLength, 4));

          //path,name,offset,length,decompLength,exporter
          Resource resource = new Resource(path, filename, offset, length);
          resources[i] = resource;

          TaskProgressManager.setValue(i);
        }

      }
      else {
        ErrorLogger.log("[XWC_MOS] Unrecognized secondary directory type: " + dirType);
        return null;
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
