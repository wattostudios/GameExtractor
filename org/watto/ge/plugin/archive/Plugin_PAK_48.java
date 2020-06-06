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
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_PAK_48 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_PAK_48() {

    super("PAK_48", "PAK_48");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Wild Terra Online",
        "Future Proof",
        "Survive in Angaria TWO",
        "Achievement Collector: Cat",
        "Baikonur Space",
        "Hello Pollution",
        "Indecision",
        "Kitty Run");
    setExtensions("pak"); // MUST BE LOWER CASE
    setPlatforms("PC");

    setCanScanForFileTypes(true);

    // MUST BE LOWER CASE !!!
    //setFileTypes(new FileType("txt", "Text Document", FileType.TYPE_DOCUMENT),
    //             new FileType("bmp", "Bitmap Image", FileType.TYPE_IMAGE)
    //             );

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

      // 4 - Unknown (5)
      if (fm.readInt() == 5) {
        rating += 5;
      }

      // 4 - Unknown (1)
      if (fm.readInt() == 1) {
        rating += 5;
      }

      // 2 - Number of Files
      if (FieldValidator.checkNumFiles(fm.readShort())) {
        rating += 5;
      }

      // 2 - Unknown (4)
      if (fm.readShort() == 4) {
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
  If an archive doesn't have filenames stored in it, the scanner can come here to try to work out
  what kind of file a Resource is. This method allows the plugin to provide additional plugin-specific
  extensions, which will be tried before any standard extensions.
  @return null if no extension can be determined, or the extension if one can be found
  **********************************************************************************************
  **/
  @Override
  public String guessFileExtension(Resource resource, byte[] headerBytes, int headerInt1, int headerInt2, int headerInt3, short headerShort1, short headerShort2, short headerShort3, short headerShort4, short headerShort5, short headerShort6) {

    if (headerShort1 == 10799 || headerShort1 == 12079 || headerShort1 == -17425 || headerShort1 == 8508 || headerShort1 == 29500 || headerShort1 == 26684 || headerShort1 == 27708 || headerShort1 == 26152 || headerShort1 == 25660 || headerShort1 == 2683 || headerShort1 == 24950 || headerShort1 == 3451 || headerInt1 == 2037149520 || headerInt1 == 1920298835 || headerInt1 == 1936617283 || headerInt1 == 1936617315 || headerInt1 == 1953460816 || headerInt1 == 1953785171 || headerInt1 == 1969448275 || headerInt1 == 2003792450 || headerInt1 == 2004116814 || headerInt1 == 1147956321 || headerInt1 == 1635017028 || headerInt1 == 1635148106 || headerInt1 == 1667855697 || headerInt1 == 1685025360 || headerInt1 == 1701470799 || headerInt1 == 1701669204 || headerInt1 == 1702065447 || headerInt1 == 1702260547 || headerInt1 == 1702453580 || headerInt1 == 1717987652 || headerInt1 == 1718579792 || headerInt1 == 1718773072 || headerInt1 == 1768713801 || headerInt1 == 1769366852 || headerInt1 == 1802465091 || headerInt1 == 1802661719 || headerInt1 == 1835363397 || headerInt1 == 1835626049 || headerInt1 == 1836216134 || headerInt1 == 1851877443 || headerInt1 == 1852143173 || headerInt1 == 1869377347 || headerInt1 == 1869639017 || headerInt1 == 1869641829 || headerInt1 == 1869833554 || headerInt1 == 1885431112 || headerInt1 == 1885957715 || headerInt1 == 1886152008 || headerInt1 == 1918985555) {
      return "txt";
    }
    else if (headerInt1 == 843468663) {
      return "woff2";
    }

    return null;
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

      // 4 - Unknown (5)
      // 4 - Unknown (1)
      fm.skip(8);

      // 2 - Number of Files
      short numFiles = fm.readShort();
      FieldValidator.checkNumFiles(numFiles);

      // 2 - Unknown (4)
      fm.skip(2);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {
        // 2 - File ID
        fm.skip(2);

        // 4 - File Offset
        int offset = fm.readInt();
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

}
