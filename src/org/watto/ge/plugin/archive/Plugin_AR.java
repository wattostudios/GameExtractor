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
public class Plugin_AR extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_AR() {

    super("AR", "AR");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Street Racing Syndicate");
    setExtensions("ar"); // MUST BE LOWER CASE
    setPlatforms("PC");

    setFileTypes(new FileType("arc", "ARC Archive", FileType.TYPE_ARCHIVE));

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

      File thisFile = fm.getFile();
      if (thisFile.getName().equalsIgnoreCase("archive.ar")) {
        rating += 25;
      }

      String basePath = thisFile.getParentFile().getAbsolutePath() + File.separatorChar;
      if (new File(basePath + "CDFILES.DAT").exists() || new File(basePath + "cdfiles.dat").exists()) {
        rating += 24; // 24 so that it doesn't match unless something else also matches
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

      long arcSize = (int) path.length();

      String basePath = path.getParentFile().getAbsolutePath() + File.separatorChar;
      File dirFile = new File(basePath + "CDFILES.DAT");
      if (!dirFile.exists()) {
        dirFile = new File(basePath + "cdfiles.dat");
      }
      if (!dirFile.exists()) {
        return null; // can't find the cdfiles.dat file
      }

      FileManipulator fm = new FileManipulator(dirFile, false);

      // 4 - Header (file)
      // 4 - Unknown (3)
      // 4 - Unknown
      // 4 - Unknown (4)
      // 4 - Unknown (4)
      // 4 - null
      // 4 - null
      // 4 - Number of Files
      // 4 - Unknown (12)
      fm.skip(36);

      // 4 - Padding Multiple (2048)
      int paddingMultiple = fm.readInt();
      FieldValidator.checkLength(paddingMultiple, arcSize);

      // 4 - Number of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // 4 - Unknown
      // 8 - null
      fm.skip(12);

      // 12 - Archive Filename ("\ARCHIVE.AR" + null)
      fm.readNullString();

      fm.skip(calculatePadding(fm.getOffset(), 4));

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      long[] offsets = new long[numFiles];
      for (int i = 0; i < numFiles; i++) {

        // 4 - File Offset
        long offset = fm.readInt() * paddingMultiple;
        FieldValidator.checkOffset(offset, arcSize);
        offsets[i] = offset;

        TaskProgressManager.setValue(i);
      }

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {
        // 4 - File Length
        long length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        long offset = offsets[i];

        String filename = Resource.generateFilename(i);

        //path,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, filename, offset, length);

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

    if (headerInt1 == 1128485441) {
      return "arc"; // ARCC
    }
    else if (headerInt1 == 811689068) {
      return "lda"; // lda0
    }
    else if (headerInt1 == 1634037875) {
      return "spe"; // spea
    }
    else if (headerInt1 == 1633971827) {
      return "snd"; // snda
    }

    return null;
  }

}
