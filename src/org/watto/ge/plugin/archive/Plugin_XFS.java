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
import java.util.Arrays;

import org.watto.ErrorLogger;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.exporter.Exporter_RAGE_XFS;
import org.watto.io.FileManipulator;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_XFS extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_XFS() {

    super("XFS", "XFS");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Wargasm");
    setExtensions("xfs");
    setPlatforms("PC");

    // MUST BE LOWER CASE !!!
    //setFileTypes(new FileType("txt", "Text Document", FileType.TYPE_DOCUMENT),
    //             new FileType("bmp", "Bitmap Image", FileType.TYPE_IMAGE)
    //             );

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

      getDirectoryFile(fm.getFile(), "ndx");
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

      ExporterPlugin exporter = Exporter_RAGE_XFS.getInstance();

      // RESETTING THE GLOBAL VARIABLES

      long arcSize = (int) path.length();

      File sourcePath = getDirectoryFile(path, "ndx");

      FileManipulator fm = new FileManipulator(sourcePath, false);

      // Number Of Files
      int numFiles = (int) (fm.getLength() / 8);
      FieldValidator.checkNumFiles(numFiles);

      Resource[] resources = new Resource[numFiles];

      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      long[] offsets = new long[numFiles];
      for (int i = 0; i < numFiles; i++) {
        // 4 - Hash?
        fm.skip(4);

        // 4 - File Offset
        long offset = fm.readInt();
        if (offset > arcSize) {
          if (offset - arcSize < 2000) {
            // there's 1 file slightly past the end of the archive, for some reason
            offset = arcSize;
          }
        }
        FieldValidator.checkOffset(offset, arcSize + 1);
        offsets[i] = offset;

        TaskProgressManager.setValue(i);
      }

      Arrays.sort(offsets);

      for (int i = 0; i < numFiles; i++) {
        String filename = Resource.generateFilename(i);

        //path,id,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, filename, offsets[i]);

        TaskProgressManager.setValue(i);
      }

      calculateFileSizes(resources, arcSize);

      fm.close();

      fm = new FileManipulator(path, false, 24); // small quick reads

      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];

        fm.seek(resource.getOffset());

        // 4 - Decompressed Length
        int decompLength = fm.readInt();
        FieldValidator.checkLength(decompLength);

        // 4 - Compression Type (1=compressed, 0=uncompressed)
        int compressionType = fm.readInt();

        if (compressionType == 0) {

          if (decompLength > resource.getLength()) {
            decompLength = (int) resource.getLength() - 8;
          }

          // X - File Data
          resource.setOffset(fm.getOffset());
          resource.setLength(decompLength);
          resource.setDecompressedLength(decompLength);
        }
        else if (compressionType == 1) {
          // 4 - Compression Header ("RA" + null + (byte)2)
          // 4 - Decompressed Length
          // 4 - Unknown
          fm.skip(12);

          // X - File Data
          resource.setOffset(fm.getOffset());
          resource.setLength(resource.getLength() - 20);
          resource.setDecompressedLength(decompLength);
          resource.setExporter(exporter);
        }
        else {
          ErrorLogger.log("[XFS] Unknown compression type: " + compressionType);
        }

        TaskProgressManager.setValue(i);
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

    if (headerInt1 == 0 && ((headerInt2 * 4) + 16) == headerInt3) {
      return "xfs_arc";
    }
    else if (headerInt1 == 1734689371 || headerInt1 == 1750343739 || headerInt1 == 1768383842 || headerShort1 == 8251 || headerShort1 == 15163 || headerShort1 == 2573) {
      return "txt";
    }

    return null;
  }

}
