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
import org.watto.component.WSPluginManager;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.helper.FileTypeDetector;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.ge.plugin.exporter.Exporter_ZLib_CompressedSizeOnly;
import org.watto.io.FileManipulator;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_XPAC extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_XPAC() {

    super("XPAC", "XPAC");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Sonic and SEGA All Stars Racing");
    setExtensions("xpac"); // MUST BE LOWER CASE
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

      // null
      if (fm.readLong() == 8) {
        rating += 5;
      }

      long arcSize = fm.getLength();

      // First File Offset
      if (FieldValidator.checkOffset(fm.readInt(), arcSize)) {
        rating += 5;
      }

      // Number Of Files
      if (FieldValidator.checkNumFiles(fm.readInt())) {
        rating += 5;
      }

      // null
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

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 8 - null
      // 4 - First File Offset
      fm.skip(12);

      // 4 - Number of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // 4 - null
      fm.skip(4);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {
        // 4 - null
        // 4 - Hash?
        fm.skip(8);

        // 4 - File Offset
        int offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - File Length (not including padding)
        int length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - File Length (not including padding)
        int otherLength = fm.readInt();

        if (length != otherLength) {
          ErrorLogger.log("[XPAC] Lengths are different: " + length + " vs " + otherLength);
        }

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

    if (headerInt1 == 1887002999 || headerInt1 == 1920235589) {
      return "waypoint";
    }
    else if (headerInt1 == 1414681427) {
      return "ssrt";
    }
    else if (headerInt1 == 1480938576) {
      return "ptex";
    }

    else if (headerShort1 == 2573 || headerShort1 == 12079) {
      return "script";
    }
    //else if (headerShort1 == 203) {
    //  return "unknown203";
    //}

    else if (headerInt2 == 1163022150) {
      return "fore";
    }
    else if (headerInt2 == 825247827) {
      return "sh01";
    }
    else if (headerInt2 == 1262572116) {
      return "trak";
    }

    else if (headerBytes.length > 1 && headerBytes[0] == 120) {
      // ZLib compression
      resource.setExporter(Exporter_ZLib_CompressedSizeOnly.getInstance());

      // re-run the file type determination for this file
      FileTypeDetector.determineExtensions(new Resource[] { resource }, this);
      return resource.getExtension();
    }
    else if (headerInt1 == 72) {
      // ZLib compression
      resource.setOffset(resource.getOffset() + 72);
      int length = (int) (resource.getLength() - 72);
      resource.setLength(length);
      resource.setDecompressedLength(length);
      resource.setExporter(Exporter_ZLib_CompressedSizeOnly.getInstance());

      // re-run the file type determination for this file
      FileTypeDetector.determineExtensions(new Resource[] { resource }, this);
      return resource.getExtension();
    }

    return null;
  }

  /**
   **********************************************************************************************
   Provide hints to the previewer so that certain document types are displayed appropriately
   **********************************************************************************************
   **/
  @Override
  public ViewerPlugin previewHint(Resource resource) {
    String extension = resource.getExtension();
    if (extension.equalsIgnoreCase("waypoint") || extension.equalsIgnoreCase("script")) {
      return (ViewerPlugin) WSPluginManager.getPlugin("Viewer", "TXT");
    }
    return null;
  }

}
