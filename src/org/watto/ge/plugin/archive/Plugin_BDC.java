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
import org.watto.ErrorLogger;
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
public class Plugin_BDC extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_BDC() {

    super("BDC", "BDC");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Bioshock 2");
    setExtensions("blk", "bdc"); // MUST BE LOWER CASE
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

      // need to find the Catalog.bdc file
      File sourceFile = fm.getFile();
      if (sourceFile.getName().equalsIgnoreCase("catalog.bdc")) {
        rating += 25;
      }
      else {
        File catalogFile = new File(sourceFile.getParentFile().getAbsolutePath() + File.separatorChar + "Catalog.bdc");
        if (catalogFile.exists()) {
          rating += 25;
        }
        else {
          rating = 0;
        }
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

      // need to find the Catalog.bdc file
      if (path.getName().equalsIgnoreCase("catalog.bdc")) {
        // opened it already
      }
      else {
        File catalogFile = new File(path.getParentFile().getAbsolutePath() + File.separatorChar + "Catalog.bdc");
        if (catalogFile.exists()) {
          // found the catalog file
          path = catalogFile;
        }
        else {
          return null;
        }
      }

      String dirPath = path.getParentFile().getAbsolutePath() + File.separatorChar;

      // Now we're reading the catalog.bdc file

      //ExporterPlugin exporter = Exporter_ZLib.getInstance();

      // RESETTING GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      int numFiles = Archive.getMaxFiles();
      int realNumFiles = 0;

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(arcSize);

      // 4 - Unknown
      // 4 - Unknown
      // 1 - Unknown (64)
      // 4 - null
      // 2 - Unknown
      fm.skip(15);

      // Loop through directory
      while (fm.getOffset() < arcSize) {

        // 2 - Unknown
        // 2 - Unknown
        // 2 - Unknown
        // 1 - Unknown
        // 1 - Unknown (64)
        fm.skip(8);

        // 1 - Archive Name Length [*2 for unicode] (includes 2-byte null unicode terminator)
        //int arcNameLength = ByteConverter.unsign(fm.readByte()) - 1;
        int arcNameLength = (int) (PluginGroup_U.readIndex(fm) - 1);
        if (arcNameLength < 0) {
          return null;
        }

        // X - Archive Name (unicode)
        String arcFilename = fm.readNullUnicodeString(arcNameLength);

        // 2 - null Unicode Archive Name Terminator
        fm.skip(2);

        // 1 - Number of Files in this BLK
        //int numBlkFiles = ByteConverter.unsign(fm.readByte());
        int numBlkFiles = (int) (PluginGroup_U.readIndex(fm));

        // check that the file exists, etc.
        File arcFile = new File(dirPath + arcFilename);
        if (!arcFile.exists()) {
          ErrorLogger.log("[BDC] Missing Archive File " + arcFilename);
        }
        long arcFileSize = arcFile.length();

        for (int i = 0; i < numBlkFiles; i++) {
          // 1 - Type Name Length [*2 for unicode] (includes 2-byte null unicode terminator)
          //int typeNameLength = ByteConverter.unsign(fm.readByte()) - 1;
          int typeNameLength = (int) (PluginGroup_U.readIndex(fm) - 1);
          if (typeNameLength < 0) {
            return null;
          }

          // X - Type Name (unicode)
          String typeName = fm.readNullUnicodeString(typeNameLength);

          // 2 - null Unicode Type Name Terminator
          fm.skip(2);

          // 1 - Object Name Length [*2 for unicode] (includes 2-byte null unicode terminator)
          //int objectNameLength = ByteConverter.unsign(fm.readByte()) - 1;
          int objectNameLength = (int) (PluginGroup_U.readIndex(fm) - 1);
          if (objectNameLength < 0) {
            return null;
          }

          // X - Object Name (unicode)
          String objectName = fm.readNullUnicodeString(objectNameLength);

          // 2 - null Unicode Object Name Terminator
          fm.skip(2);

          // 4 - null
          fm.skip(4);

          // 4 - File Offset
          int offset = fm.readInt();
          FieldValidator.checkOffset(offset, arcFileSize);

          // 4 - File Length
          int length = fm.readInt();
          FieldValidator.checkLength(length, arcFileSize);

          // 4 - File Length
          int length2 = fm.readInt();
          FieldValidator.checkLength(length2, arcFileSize);

          // 4 - Unknown ID?
          int id = fm.readInt();

          String filename = typeName + "\\" + objectName + "(" + id + ").dxtraw";

          //path,name,offset,length,decompLength,exporter
          Resource resource = new Resource(arcFile, filename, offset, length);
          resource.forceNotAdded(true);
          resources[realNumFiles] = resource;
          realNumFiles++;

          TaskProgressManager.setValue(fm.getOffset());
        }

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
