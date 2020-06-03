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
import org.watto.component.WSPluginManager;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.ge.plugin.exporter.Exporter_ZLib;
import org.watto.io.FileManipulator;
import org.watto.io.converter.ByteConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_DCP_JUNK extends ArchivePlugin {

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Plugin_DCP_JUNK() {

    super("DCP_JUNK", "DCP_JUNK");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Alpha Polaris",
        "Art of Murder: FBI Confidential",
        "Oknytt",
        "Reversion: The Escape");
    setExtensions("dcp"); // MUST BE LOWER CASE
    setPlatforms("PC");

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

      // 4 - Magic Number (-557797922)
      if (fm.readInt() == -557797922) {
        rating += 5;
      }

      // 4 - Header (JUNK)
      if (fm.readString(4).equals("JUNK")) {
        rating += 50;
      }

      return rating;

    }
    catch (Throwable t) {
      return 0;
    }
  }

  /**
   **********************************************************************************************
   Provide hints to the previewer so that certain document types are displayed appropriately
   **********************************************************************************************
   **/
  @Override
  public ViewerPlugin previewHint(Resource resource) {
    String extension = resource.getExtension();
    if (extension.equalsIgnoreCase("act3d") || extension.equalsIgnoreCase("button") || extension.equalsIgnoreCase("def") || extension.equalsIgnoreCase("entity") || extension.equalsIgnoreCase("font") || extension.equalsIgnoreCase("fx") || extension.equalsIgnoreCase("game") || extension.equalsIgnoreCase("geometry") || extension.equalsIgnoreCase("image") || extension.equalsIgnoreCase("items") || extension.equalsIgnoreCase("scene") || extension.equalsIgnoreCase("settings") || extension.equalsIgnoreCase("sprite") || extension.equalsIgnoreCase("tab") || extension.equalsIgnoreCase("window")) {
      return (ViewerPlugin) WSPluginManager.getPlugin("Viewer", "TXT");
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

      ExporterPlugin exporter = Exporter_ZLib.getInstance();

      // RESETTING GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 4 - Magic Number (-557797922)
      // 4 - Header (JUNK)
      // 4 - Unknown (512)
      // 4 - null
      // 4 - Unknown (5)
      // 4 - Hash?
      // 0-X - Optional Description
      // 4 - End of Description Padding (all (byte) 32)
      // 36 - Engine Description ("(Wintermute Engine   DEAD:CODE 2010)")
      // X - null Padding to offset 124
      // 4 - Unknown (1)
      fm.seek(128);

      // 4 - Directory Offset
      int dirOffset = fm.readInt();
      FieldValidator.checkOffset(dirOffset, arcSize);

      fm.seek(dirOffset);

      // 1 - Archive Name Length (including this field)
      int nameLength = ByteConverter.unsign(fm.readByte()) - 1;
      FieldValidator.checkFilenameLength(nameLength);

      // X - Archive Name (Filename without the extension)
      fm.skip(nameLength);

      // 2 - null
      fm.skip(2);

      // 4 - Number of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {

        // 1 - Filename Length (including the XOR'd null Terminator)
        int filenameLength = ByteConverter.unsign(fm.readByte()) - 1;

        // X - Filename (XOR with (byte)68)
        byte[] filenameBytes = fm.readBytes(filenameLength);
        for (int n = 0; n < filenameLength; n++) {
          filenameBytes[n] ^= 68;
        }
        String filename = new String(filenameBytes);

        // 1 - null Filename Terminator (XOR with (byte)68)
        fm.skip(1);

        // 4 - File Offset
        int offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - Decompressed File Length
        int length1 = fm.readInt();

        // 4 - Compressed File Length
        int length2 = fm.readInt();

        // 4 - null
        // 4 - Unknown
        // 4 - null
        fm.skip(12);

        if (length2 == 0) {
          // not compressed
          int length = length1;
          FieldValidator.checkLength(length, arcSize);

          //path,name,offset,length,decompLength,exporter
          resources[i] = new Resource(path, filename, offset, length);

        }
        else {
          // compressed
          int length = length2;
          FieldValidator.checkLength(length, arcSize);

          int decompLength = length1;
          FieldValidator.checkLength(decompLength);

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

}
