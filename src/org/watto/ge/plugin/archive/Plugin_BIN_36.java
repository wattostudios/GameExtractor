/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2024 wattostudios
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
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.io.FileManipulator;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_BIN_36 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_BIN_36() {

    super("BIN_36", "BIN_36");

    // read write replace rename
    setProperties(true, false, false, false);

    setGames("Transformers Tataki");
    setExtensions("bin"); // MUST BE LOWER CASE
    setPlatforms("PS2");

    setEnabled(false);

    // MUST BE LOWER CASE !!!
    // setFileTypes(new FileType("txt", "Text Document", FileType.TYPE_DOCUMENT),
    // new FileType("bmp", "Bitmap Image", FileType.TYPE_IMAGE)
    // );

    // setTextPreviewExtensions("colours", "rat", "screen", "styles"); // LOWER CASE

    // setCanScanForFileTypes(true);

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

      if (fm.getFile().getName().toLowerCase().contains("model.bin")) {
        rating += 25;
      }

      fm.skip(20);

      long arcSize = fm.getLength();

      // File Length
      if (FieldValidator.checkLength(fm.readInt(), arcSize)) {
        rating += 5;
      }

      return rating;

    } catch (Throwable t) {
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
      // - Uncompressed files MUST know their LENGTH

      addFileTypes();

      // ExporterPlugin exporter = Exporter_ZLib.getInstance();

      // RESETTING GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false, 24); // small quick reads

      long arcSize = fm.getLength();

      int numFiles = Archive.getMaxFiles();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(arcSize);

      // Loop through directory
      int realNumFiles = 0;
      long[] offsets = new long[numFiles];
      while (fm.getOffset() < arcSize) {
        long offset = fm.getOffset();

        short header1 = fm.readShort();
        short header2 = fm.readShort();

        if ((header1 == 6 && header2 == 6) || (header1 == 4 && (header2 == 1 || header2 == 2)) || (header1 == 2 && header2 == 1) || (header1 == 7 && header2 == 5) || (header1 == 1 && header2 == 1)) {
          offsets[realNumFiles] = offset;
          realNumFiles++;
          TaskProgressManager.setValue(offset);
        } else {
          System.out.println(header1 + "\t" + header2);
        }

        fm.seek(offset + 2048);
      }

      for (int i = 0; i < realNumFiles; i++) {
        String filename = Resource.generateFilename(i);

        // path,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, filename, offsets[i]);
      }

      resources = resizeResources(resources, realNumFiles);
      calculateFileSizes(resources, arcSize);

      fm.close();

      return resources;

    } catch (Throwable t) {
      logError(t);
      return null;
    }
  }

  /**
   **********************************************************************************************
   * If an archive doesn't have filenames stored in it, the scanner can come here
   * to try to work out what kind of file a Resource is. This method allows the
   * plugin to provide additional plugin-specific extensions, which will be tried
   * before any standard extensions.
   * 
   * @return null if no extension can be determined, or the extension if one can
   *         be found
   **********************************************************************************************
   **/
  @Override
  public String guessFileExtension(Resource resource, byte[] headerBytes, int headerInt1, int headerInt2, int headerInt3, short headerShort1, short headerShort2, short headerShort3, short headerShort4, short headerShort5, short headerShort6) {

    /*
     * if (headerInt1 == 2037149520) { return "js"; }
     */

    return null;
  }

}
