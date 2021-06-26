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
import org.watto.ge.plugin.exporter.Exporter_Custom_RPKG_GKPR_Multi;
import org.watto.ge.plugin.exporter.Exporter_LZ4;
import org.watto.ge.plugin.exporter.Exporter_XOR_RepeatingKey;
import org.watto.io.FileManipulator;
import org.watto.io.converter.IntConverter;
import org.watto.io.converter.StringConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_RPKG_GKPR extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_RPKG_GKPR() {

    super("RPKG_GKPR", "RPKG_GKPR");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Hitman (2016)",
        "Hitman 2");
    setExtensions("rpkg"); // MUST BE LOWER CASE
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
      if (fm.readString(4).equals("GKPR")) {
        rating += 50;
      }

      // Number Of Files
      long arcSize = fm.getLength();
      if (arcSize > 4000000000l) {
        // the largest archive has an excessive number of files in it!
        if (FieldValidator.checkNumFiles(fm.readInt() / 3)) {
          rating += 5;
        }
      }
      else {
        if (FieldValidator.checkNumFiles(fm.readInt())) {
          rating += 5;
        }
      }

      // Details Directory Length
      if (FieldValidator.checkLength(fm.readInt(), arcSize)) {
        rating += 5;
      }

      // Types Directory Length
      if (FieldValidator.checkLength(fm.readInt(), arcSize)) {
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

      ExporterPlugin lz4Exporter = Exporter_LZ4.getInstance();
      ExporterPlugin encryptedExporter = new Exporter_XOR_RepeatingKey(new int[] { 220, 69, 166, 156, 211, 114, 76, 171 });
      ExporterPlugin multiExporter = new Exporter_Custom_RPKG_GKPR_Multi();
      //ExporterPlugin multiExporter = new ChainedExporterWrapper(encryptedExporter, lz4Exporter);

      // RESETTING GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 4 - Header (GKPR)
      fm.skip(4);

      // 4 - Number of Files
      int numFiles = fm.readInt();

      if (arcSize > 4000000000l) {
        // the largest archive has an excessive number of files in it!
        FieldValidator.checkNumFiles(numFiles / 3);
      }
      else {
        FieldValidator.checkNumFiles(numFiles);
      }

      // 4 - Details Directory Length
      // 4 - Types Directory Length
      fm.skip(8);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // See if there's an "unknown" directory in this archive
      int unknownCount = fm.readInt();
      if (unknownCount >= 0 && unknownCount <= numFiles) {
        // yes, so skip it
        fm.skip(unknownCount * 8);
      }
      else {
        // No, so go back to offset 16
        fm.seek(16);
      }

      // Loop through directory
      boolean[] encrypted = new boolean[numFiles];
      for (int i = 0; i < numFiles; i++) {
        // 8 - Hash?
        fm.skip(8);

        // 8 - File Offset
        long offset = fm.readLong();
        FieldValidator.checkOffset(offset, arcSize);

        // 3 - Compressed File Length (or null)
        // 1 - Encryption Flag? (128 = encryption)
        byte[] lengthBytes = fm.readBytes(4);
        encrypted[i] = false;
        if ((lengthBytes[3] & 128) == 128) {
          encrypted[i] = true;
          lengthBytes[3] &= 127;
        }
        int length = IntConverter.convertLittle(lengthBytes);
        FieldValidator.checkLength(length, arcSize);

        String filename = Resource.generateFilename(i);

        //path,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, filename, offset, length);

        TaskProgressManager.setValue(i);
      }

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {
        // 4 - File Type Header (eg "ULBC", "TPPC", "TXET", "ETAM", ...)
        String fileType = StringConverter.reverse(fm.readString(4));

        // 4 - Extra Data Length (can be null)
        int extraLength = fm.readInt();
        FieldValidator.checkLength(extraLength);

        // 4 - null
        fm.skip(4);

        // 4 - Decompressed File Length
        int decompLength = fm.readInt();
        FieldValidator.checkLength(decompLength);

        // 4 - Unknown (32)
        // 4 - Unknown (-1)
        // X - Extra Data
        fm.skip(8 + extraLength);

        Resource resource = resources[i];
        resource.setExtension(fileType);
        resource.setOriginalName(resource.getName()); // so it doesn't think that it's been added

        if (decompLength != resource.getLength()) {
          resource.setDecompressedLength(decompLength);
          if (encrypted[i]) {
            resource.setExporter(multiExporter);
          }
          else {
            resource.setExporter(lz4Exporter);
          }
        }
        else if (encrypted[i]) {
          resource.setExporter(encryptedExporter);
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
    if (extension.equalsIgnoreCase("json") || extension.equalsIgnoreCase("repo")) {
      return (ViewerPlugin) WSPluginManager.getPlugin("Viewer", "TXT");
    }
    return null;
  }

}
