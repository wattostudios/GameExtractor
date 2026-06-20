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
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.exporter.Exporter_ZLib;
import org.watto.io.FileManipulator;
import org.watto.io.converter.ShortConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_IMG_9 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_IMG_9() {

    super("IMG_9", "IMG_9");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Europe Racer");
    setExtensions("img"); // MUST BE LOWER CASE
    setPlatforms("PC");

    setFileTypes(new FileType("d3d", "D3D Mesh", FileType.TYPE_MODEL));

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

      getDirectoryFile(fm.getFile(), "IND");
      rating += 25;

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

      ExporterPlugin exporter = Exporter_ZLib.getInstance();

      // RESETTING GLOBAL VARIABLES

      long arcSize = path.length();

      File sourcePath = getDirectoryFile(path, "IND");
      FileManipulator fm = new FileManipulator(sourcePath, false);

      // 2 - Number of Files
      int numFiles = ShortConverter.unsign(fm.readShort());

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      boolean compressed = false;
      if (sourcePath.length() == (2 + (numFiles * 4))) {
        // no filenames
        setCanScanForFileTypes(true);

        compressed = false;

        for (int i = 0; i < numFiles; i++) {

          // 4 - File Offset
          long offset = fm.readInt();
          FieldValidator.checkOffset(offset, arcSize);

          String filename = Resource.generateFilename(i);

          //path,name,offset,length,decompLength,exporter
          resources[i] = new Resource(path, filename, offset);

          TaskProgressManager.setValue(i);
        }
      }
      else {
        // filenames
        setCanScanForFileTypes(false);

        compressed = true;

        for (int i = 0; i < numFiles; i++) {

          // 20 - Filename (null terminated, filled with nulls)
          String filename = fm.readNullString(20);
          FieldValidator.checkFilename(filename);

          // 4 - File Offset
          long offset = fm.readInt();
          FieldValidator.checkOffset(offset, arcSize);

          //path,name,offset,length,decompLength,exporter
          resources[i] = new Resource(path, filename, offset);

          TaskProgressManager.setValue(i);
        }
      }

      calculateFileSizes(resources, arcSize);

      fm.close();

      if (compressed) {
        // go to each file and find the decompressed lengths
        fm = new FileManipulator(path, false, 4); // small quick reads

        for (int i = 0; i < numFiles; i++) {
          Resource resource = resources[i];

          long offset = resource.getOffset();
          fm.seek(offset);

          // 4 - Decompressed File Length
          int decompLength = fm.readInt();
          FieldValidator.checkLength(decompLength);

          long length = resource.getLength() - 4;

          // X - File Data (ZLib)
          offset += 4;

          resource.setOffset(offset);
          resource.setLength(length);
          resource.setDecompressedLength(decompLength);

          if (length != decompLength) {
            resource.setExporter(exporter);
          }

        }

        fm.close();
      }

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

    if (((headerShort1 * headerShort2) + 8) == resource.getDecompressedLength()) {
      return "img_tex";
    }

    return null;
  }

}
