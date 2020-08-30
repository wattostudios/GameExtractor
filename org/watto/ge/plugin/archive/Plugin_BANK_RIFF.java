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
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.io.FileManipulator;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_BANK_RIFF extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_BANK_RIFF() {

    super("BANK_RIFF", "BANK_RIFF");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("20XX",
        "Apsulov: End of Gods",
        "Arrog",
        "Celeste",
        "Copoka",
        "Don't Give Up",
        "Hitch Hiker",
        "Hob",
        "Into The Breach",
        "Liberated",
        "Observer",
        "Roman Sands",
        "RUINER",
        "Saint Kotar: The Yellow Mask",
        "Space Routine",
        "Stories Untold",
        "Subnautica",
        "Sundered",
        "Wheels Of Aurelia",
        "World War Z");
    setExtensions("bank"); // MUST BE LOWER CASE
    setPlatforms("PC");

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
      if (fm.readString(4).equals("RIFF")) {
        rating += 24; // so it doesn't match other RIFF archives
      }

      long arcSize = fm.getLength();

      // Archive Size
      if (FieldValidator.checkEquals(fm.readInt() + 8, arcSize)) {
        rating += 5;
      }

      // Header
      if (fm.readString(8).equals("FEV FMT ")) {
        rating += 26;
      }
      else {
        return 0;
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

      // 4 - Header (RIFF)
      // 4 - Archive Length [+8]
      // 8 - Format Header (FEV FMT )
      fm.skip(16);

      // 4 - Format Block Length (8)
      int blockLength = fm.readInt();
      FieldValidator.checkLength(blockLength, arcSize);

      // 4 - Unknown (101)
      // 4 - Unknown (99)
      fm.skip(blockLength);

      // 4 - List Header (LIST)
      // 4 - List Data Length
      // X - List Data
      // 4 - Header (SND )
      while (fm.getOffset() < arcSize) {
        if (!fm.readString(4).equals("SND ")) {
          // 4 - Block length
          blockLength = fm.readInt();
          FieldValidator.checkLength(blockLength, arcSize);
          // X - Block Data
          fm.skip(blockLength);
        }
        else {
          // 4 - File Data Length
          int fileLength = fm.readInt();
          FieldValidator.checkLength(fileLength);

          // 0-31 - null Padding to a multiple of 32 bytes
          fm.skip(calculatePadding(fm.getOffset(), 32));

          // X - FSB5 File Data
          return new Plugin_FSB_FSB5().read(fm);
        }

      }

      fm.close();

      return null;

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
