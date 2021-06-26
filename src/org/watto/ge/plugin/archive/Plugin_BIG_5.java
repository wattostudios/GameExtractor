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
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.exporter.Exporter_QCMP1;
import org.watto.io.FileManipulator;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_BIG_5 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_BIG_5() {

    super("BIG_5", "BIG_5");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Sleeping Dogs: Definitive Edition");
    setExtensions("big"); // MUST BE LOWER CASE
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

      getDirectoryFile(fm.getFile(), "bix");
      rating += 25;

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

      ExporterPlugin exporter = Exporter_QCMP1.getInstance();

      // RESETTING GLOBAL VARIABLES

      long arcSize = path.length() + 1; // +1 to allow offsets at the length of the archive

      File sourcePath = getDirectoryFile(path, "bix");
      FileManipulator fm = new FileManipulator(sourcePath, false);

      // 4 - Header ((bytes)168,64,92,44)
      // 4 - BIX File Length [+16]
      // 4 - BIX File Length [+16]
      // 28 - null
      // 4 - Unknown
      // 20 - null
      // 4 - Unknown
      // 36 - Archive Name (null terminated, filled with nulls)
      // 8 - Unknown (-1)
      fm.skip(112);

      // 4 - Number of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // 4 - null
      // 4 - Unknown (72)
      // 12 - null
      // 4 - Unknown
      // 8 - null
      // 14 - Archive Name (null terminated, filled with nulls)
      // 4 - Unknown (86)
      // 4 - Unknown
      // 4 - Unknown (87)
      // 4 - Unknown
      // 4 - Unknown (87)
      // 10 - null
      fm.skip(76);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {
        // 4 - Hash
        fm.skip(4);

        // 4 - File Offset [*4]
        // 4 - Extra Offset
        long offset = (((long) fm.readInt()) * 4) + (fm.readInt() & 0x0FFF);
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - Compressed Length
        int length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - Unknown
        fm.skip(4);

        // 4 - Decompressed Length
        long decompLength = fm.readInt();
        FieldValidator.checkLength(decompLength, arcSize);

        String filename = Resource.generateFilename(i);

        if (length == 0) {
          // uncompressed

          //path,name,offset,length,decompLength,exporter
          resources[i] = new Resource(path, filename, offset, decompLength);
        }
        else {
          // compressed

          //path,name,offset,length,decompLength,exporter
          resources[i] = new Resource(path, filename, offset, length, decompLength, exporter);
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

    if (headerInt1 == 337173876) {
      return "lightrig";
    }
    else if (headerInt1 == -1966106268) {
      return "anim";
    }

    return null;
  }

}
