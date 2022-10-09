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
import org.watto.ErrorLogger;
import org.watto.datatype.Archive;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.io.FileManipulator;
import org.watto.io.FilenameSplitter;
import org.watto.io.converter.ShortConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_DGP extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_DGP() {

    super("DGP", "DGP");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Defense Grid: The Awakening");
    setExtensions("dgp"); // MUST BE LOWER CASE
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

      // most archives start with a 4-byte null
      if (fm.readInt() == 0) {
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

      // Find the 0000 file
      String baseDirectory = FilenameSplitter.getDirectory(path);
      String baseName = FilenameSplitter.getFilename(path);
      if (baseName.length() < 5) {
        return null;
      }
      baseName = baseName.substring(0, baseName.length() - 4);
      File dirArchive = new File(baseDirectory + File.separatorChar + baseName + "0000.dgp");
      if (!dirArchive.exists()) {
        ErrorLogger.log("[DGP] Missing archive file number 0000");
        return null;
      }

      FileManipulator fm = new FileManipulator(dirArchive, false);

      long arcSize = fm.getLength();

      // 2 - Number of Spanned Archives (not including the 0000 file)
      int numArchives = fm.readShort();
      FieldValidator.checkPositive(numArchives);

      // 2 - Header (DH/KH)
      fm.skip(2);

      int numFiles = Archive.getMaxFiles();
      int realNumFiles = 0;

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numArchives);

      // Loop through each archive 
      for (int a = 0; a < numArchives; a++) {

        int arcNumberRaw = a + 1;
        String arcNumber = null;
        if (arcNumberRaw < 10) {
          arcNumber = "000" + arcNumberRaw;
        }
        else if (arcNumberRaw < 100) {
          arcNumber = "00" + arcNumberRaw;
        }
        else if (arcNumberRaw < 1000) {
          arcNumber = "0" + arcNumberRaw;
        }
        else {
          arcNumber = "" + arcNumberRaw;
        }

        File arcFile = new File(baseDirectory + File.separatorChar + baseName + arcNumber + ".dgp");
        if (!arcFile.exists()) {
          ErrorLogger.log("[DGP] Missing archive file number " + arcNumber);
          return null;
        }
        arcSize = arcFile.length();

        // 2 - Number of Files in this Folder
        int numFilesInArchive = ShortConverter.unsign(fm.readShort());

        // 4 - Folder Name Length [*2 for unicode] (can be null)
        int folderNameLength = fm.readInt();
        FieldValidator.checkFilenameLength(folderNameLength + 1);//+1 to allow nulls

        // X - Folder Name (unicode)
        String folderName = "";
        if (folderNameLength > 0) {
          folderName = fm.readUnicodeString(folderNameLength) + File.separatorChar;
        }

        // 1 - Unknown (0/1)
        fm.skip(1);

        // 4 - First File Offset? (usually 4)
        int offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        for (int i = 0; i < numFilesInArchive; i++) {
          // 4 - Filename Length [*2 for unicode]
          int filenameLength = fm.readInt();
          FieldValidator.checkFilenameLength(filenameLength);

          // X - Filename (unicode)
          String filename = folderName + fm.readUnicodeString(filenameLength);

          // 4 - File Length
          int length = fm.readInt();
          FieldValidator.checkLength(length, arcSize);

          // 8 - Hash?
          fm.skip(8);

          //path,name,offset,length,decompLength,exporter
          Resource resource = new Resource(arcFile, filename, offset, length);
          resource.forceNotAdded(true);
          resources[realNumFiles] = resource;

          realNumFiles++;

          offset += length;
        }

        TaskProgressManager.setValue(a);
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
