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

import org.watto.datatype.FileType;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.io.FileManipulator;
import org.watto.io.converter.ByteConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_FLD extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_FLD() {

    super("FLD", "FLD");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("ChainDive");
    setExtensions("fld"); // MUST BE LOWER CASE
    setPlatforms("PS2");

    // MUST BE LOWER CASE !!!
    setFileTypes(new FileType("tm2", "TM2 Texture Image", FileType.TYPE_IMAGE),
        new FileType("res", "RES Archive", FileType.TYPE_ARCHIVE),
        new FileType("tm2col", "TM2 Archive", FileType.TYPE_ARCHIVE));

    //setTextPreviewExtensions("colours", "rat", "screen", "styles"); // LOWER CASE

    setCanScanForFileTypes(true);

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

      // 1 - Unknown (208)
      if (ByteConverter.unsign(fm.readByte()) == 208) {
        rating += 20;
      }

      // Number Of Files
      if (FieldValidator.checkNumFiles(fm.readShort())) {
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

      // 1 - Unknown (208)
      fm.skip(1);

      // 2 - Number of Files
      short numFiles = fm.readShort();
      FieldValidator.checkNumFiles(numFiles);

      // 1 - null
      fm.skip(1);

      // X - Archive Path and Filename
      // 1 - null Filename Terminator
      fm.readNullString();

      // X - null Padding to a multiple of 2048 bytes
      fm.skip(calculatePadding(fm.getOffset(), 4));

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {
        // 4 - File Offset [>>11<<11]
        int offset = ((fm.readInt() >> 11) << 11);
        FieldValidator.checkOffset(offset, arcSize);

        String filename = Resource.generateFilename(i);

        //path,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, filename, offset);

        TaskProgressManager.setValue(i);
      }

      calculateFileSizes(resources, arcSize);

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

    if (headerInt1 == 843925844) {
      return "tm2";
    }
    else if (headerInt1 == 1396917577) {
      return "audio";
    }
    else if (headerInt1 == 1598305097) {
      return "i3d";
    }
    else if (headerInt1 == 7564905) {
      return "res"; // ins
    }
    else if (headerInt1 == 1869440356) {
      return "demo";
    }
    else if (headerInt1 == 1936028272) {
      return "preset";
    }
    else if (headerInt1 == 7040097) {
      return "alk";
    }
    else if (headerInt1 == 0 && headerInt2 == 1 && headerInt3 == 0) {
      long offset = resource.getOffset() + 32;
      long length = resource.getDecompressedLength() - 32;
      resource.setOffset(offset);
      resource.setLength(length);
      resource.setDecompressedLength(length);
      return "tm2";
    }
    else if (headerInt1 == 0 && (headerInt2 > 1 && headerInt2 < 255) && headerInt3 == 0 && resource.getDecompressedLength() > 80) {
      return "tm2col";
    }

    return null;
  }

}
