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
import org.watto.ge.plugin.exporter.Exporter_Custom_VAG_Audio;
import org.watto.io.FileManipulator;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_DAT_120 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_DAT_120() {

    super("DAT_120", "DAT_120");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Bleach: Hanatareshi Yabou");
    setExtensions("dat"); // MUST BE LOWER CASE
    setPlatforms("PS2");

    setFileTypes(new FileType("vag", "VAG Audio", FileType.TYPE_AUDIO));

    //setTextPreviewExtensions("script"); // LOWER CASE

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

      File originalFile = fm.getFile();
      if (originalFile.getName().equalsIgnoreCase("bleach.dat")) {
        rating += 25;
      }

      originalFile = new File(originalFile.getParent() + File.separatorChar + "cdimage.tbl");

      if (originalFile.exists()) {
        rating += 25;
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

      File sourcePath = new File(path.getParent() + File.separatorChar + "cdimage.tbl");
      if (sourcePath.exists() && sourcePath.isFile()) {
        // ok
      }
      else {
        return null;
      }

      FileManipulator fm = new FileManipulator(sourcePath, false);

      // 4 - Number Of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {

        // 4 - File Offset
        long offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - File Length
        long length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

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

    if (headerInt1 == 4606797) {
      return "mkf";
    }
    else if (headerInt1 == 4869707) {
      return "knj";
    }
    else if (headerInt1 == 4412225) {
      return "asc";
    }
    else if (headerInt1 == 39014484) {
      return "tps";
    }
    else if (headerInt1 == 6517364) {
      return "trc";
    }
    else if (headerInt1 == 860897613) {
      return "m3d";
    }
    else if (headerInt1 == 131073) {
      return "mpic";
    }
    else if (headerInt1 == 1396917577) {
      return "scei_aud";
    }
    else if (headerInt1 == 1296974163) {
      return "sanm";
    }
    else if (headerInt1 == 1230127955 || headerShort1 == 17993 || headerInt1 == 1598833741 || headerInt1 == 1598837826) {
      return "script";
    }
    else if (headerInt1 == 0 && headerInt2 == 0) {
      resource.setExporter(Exporter_Custom_VAG_Audio.getInstance());
      return "vag";
    }
    else if (headerShort2 == 16) {
      int fileLength = (int) resource.getLength() - ((int) headerShort1 * 4 + headerInt2);

      if (fileLength >= 0 && fileLength <= 20) {
        return "mkf_arc";
      }
    }
    else if (headerShort2 == 64 && headerShort1 < 64) {
      return "arc";
    }
    else if (headerInt1 == 1145980218) {
      return "end";
    }
    else if (headerInt1 == 1163477324) {
      return "layer";
    }

    return null;
  }

}
