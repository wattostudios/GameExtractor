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
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.ge.plugin.exporter.Exporter_ZLib;
import org.watto.ge.plugin.exporter.Exporter_ZLib_CompressedSizeOnly;
import org.watto.io.FileManipulator;
import org.watto.io.buffer.ByteBuffer;
import org.watto.io.converter.ByteConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_WD_WD extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_WD_WD() {

    super("WD_WD", "WD_WD");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Two Worlds 2");
    setExtensions("wd"); // MUST BE LOWER CASE
    setPlatforms("PC");

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

      // Header
      if (fm.readString(2).equals("WD")) {
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
   * Reads an [archive] File into the Resources
   **********************************************************************************************
   **/
  @Override
  public Resource[] read(File path) {
    try {

      // NOTE - Compressed files MUST know their DECOMPRESSED LENGTH
      //      - Uncompressed files MUST know their LENGTH

      addFileTypes();

      Exporter_ZLib exporter = Exporter_ZLib.getInstance();

      // RESETTING GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 2 - Header (WD)
      // 22 - Unknown
      // 8 - Unknown
      // 8 - Unknown (3/4)
      // 4 - null  
      fm.skip(44);

      // 4 - Compresed Directory Length
      int compDirLength = fm.readInt();
      FieldValidator.checkLength(compDirLength, arcSize);

      // X - Compresed Directory (ZLib Compression)
      Exporter_ZLib_CompressedSizeOnly dirExporter = Exporter_ZLib_CompressedSizeOnly.getInstance();
      int maxDirLength = compDirLength * 10;

      byte[] dirBytes = new byte[maxDirLength];
      int decompWritePos = 0;
      dirExporter.open(fm, compDirLength, compDirLength);

      for (int b = 0; b < maxDirLength; b++) {
        if (dirExporter.available()) { // make sure we read the next bit of data, if required
          dirBytes[decompWritePos++] = (byte) dirExporter.read();
        }
        else {
          break;
        }
      }

      // open the decompressed data for processing
      fm.close();
      fm = new FileManipulator(new ByteBuffer(dirBytes));

      // 4 - Number Of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {
        // 1 - Filename Length
        int filenameLength = ByteConverter.unsign(fm.readByte());

        // X - Filename
        String filename = fm.readString(filenameLength);

        //System.out.println(fm.getOffset() + "\t" + filename);

        // 1 - Compression Flag? (1=Zlib)
        int compressionType = fm.readByte();

        // 8 - File Offset
        long offset = fm.readLong();
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - Compressed File Length
        int length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 8 - Decompressed File Length
        long decompLength = fm.readInt();
        fm.skip(4);
        FieldValidator.checkLength(decompLength);

        //path,name,offset,length,decompLength,exporter
        if (compressionType == 1) {
          resources[i] = new Resource(path, filename, offset, length, decompLength, exporter);
        }
        else {
          resources[i] = new Resource(path, filename, offset, length, decompLength);
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
   Provide hints to the previewer so that certain document types are displayed appropriately
   **********************************************************************************************
   **/
  @Override
  public ViewerPlugin previewHint(Resource resource) {
    String extension = resource.getExtension();
    if (extension.equalsIgnoreCase("act") || extension.equalsIgnoreCase("con") || extension.equalsIgnoreCase("qtx") || extension.equalsIgnoreCase("smt") || extension.equalsIgnoreCase("lock") || extension.equalsIgnoreCase("trk") || extension.equalsIgnoreCase("clt") || extension.equalsIgnoreCase("h") || extension.equalsIgnoreCase("rc")) {
      return (ViewerPlugin) WSPluginManager.getPlugin("Viewer", "TXT");
    }
    return null;
  }

}
