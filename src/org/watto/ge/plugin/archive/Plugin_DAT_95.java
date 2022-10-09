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
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.exporter.Exporter_Custom_DAT_95;
import org.watto.io.FileManipulator;
import org.watto.io.converter.ByteConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_DAT_95 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_DAT_95() {

    super("DAT_95", "DAT_95");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("NHL Championship 2000");
    setExtensions("dat"); // MUST BE LOWER CASE
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

      String filename = fm.getFile().getName();
      if (filename.contains(".unencrypted.dat") || filename.contains(".encrypted.dat")) {
        rating += 25;
      }

      byte[] headerBytes = fm.readBytes(4);
      if (ByteConverter.unsign(headerBytes[1]) == 246 || ByteConverter.unsign(headerBytes[1]) == 9) {// detecting the tab
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

      //ExporterPlugin exporter = new Exporter_XOR(255);
      ExporterPlugin exporter = new Exporter_Custom_DAT_95(255);

      // RESETTING GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      int numFiles = 1;
      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      String arcName = path.getName();

      // THIS JUST CONVERTS THE ENTIRE FILE BETWEEN XOR(255) OR PLAIN
      byte[] headerBytes = fm.readBytes(4);
      if (arcName.contains(".encrypted.dat") || (ByteConverter.unsign(headerBytes[1]) == 246)) {
        // XOR encrypted - needs to be decrypted

        int dotPos = arcName.indexOf(".encrypted.dat");
        if (dotPos > 0) {
          arcName = arcName.substring(0, dotPos);
        }

        long offset = 0;
        long length = arcSize;
        String filename = arcName + ".unencrypted.dat";

        //path,name,offset,length,decompLength,exporter
        resources[0] = new Resource(path, filename, offset, length, length, exporter);

        TaskProgressManager.setValue(0);
      }
      else if (arcName.contains(".unencrypted.dat") || (headerBytes[1] == 9)) {
        // unencrypted - needs to be XOR encrypted

        int dotPos = arcName.indexOf(".unencrypted.dat");
        if (dotPos > 0) {
          arcName = arcName.substring(0, dotPos);
        }

        long offset = 0;
        long length = arcSize;
        String filename = arcName + ".encrypted.dat";

        //path,name,offset,length,decompLength,exporter
        resources[0] = new Resource(path, filename, offset, length, length, exporter);

        TaskProgressManager.setValue(0);
      }
      else {
        // something else (unsupported)
        ErrorLogger.log("[DAT_95] Unrecognized file type.");
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
