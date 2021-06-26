/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2021 wattostudios
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
import org.watto.datatype.FileType;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.io.FileManipulator;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_TOD_TOD2 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_TOD_TOD2() {

    super("TOD_TOD2", "TOD_TOD2");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Pacman: Adventures In Time");
    setExtensions("tod"); // MUST BE LOWER CASE
    setPlatforms("PC");

    // MUST BE LOWER CASE !!!
    setFileTypes(new FileType("mesh", "Mesh", FileType.TYPE_MODEL));

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

      // Header
      String header = fm.readString(4);
      if (header.equals("TOD2") || header.equals("MATL")) {
        rating += 50;
      }

      if (fm.readInt() == 0) {
        rating += 5;
      }

      if (fm.readString(4).equals("INFO")) {
        rating += 5;
      }

      if (fm.readInt() == 44) {
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

      // 4 - Header (TOD2)
      String arcHeader = fm.readString(4);
      if (arcHeader.equals("TOD2")) {
        // 4 - null
        // 4 - Header (INFO)
        fm.skip(8);

        // 4 - Block Length (not including these 2 fields) (44)
        int infoLength = fm.readInt();
        FieldValidator.checkLength(infoLength, arcSize);

        // 2 - Unknown (19/20)
        // 2 - Unknown (7)
        // 4 - Unknown
        // 16 - Author Name? (null terminated, filled with nulls)
        // 16 - Mesh Filename (null terminated, filled with nulls)
        // 2 - Unknown (160)
        // 2 - Unknown
        fm.skip(infoLength);
      }
      else if (arcHeader.equals("MATL")) {
        fm.relativeSeek(0);
      }
      else {
        ErrorLogger.log("[TOD_TOD2] Unknown header block: " + arcHeader);
      }

      int numFiles = Archive.getMaxFiles();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(arcSize);

      // Loop through directory
      int realNumFiles = 0;
      while (fm.getOffset() < arcSize - 8) { // -8 to exclude the footer
        long offset = fm.getOffset();
        int length = 0;

        // 4 - Header (MESH)
        String header = fm.readString(4);
        if (header.equals("MATL")) {
          // 4 - Block Length (not including these 2 fields)
          int matlLength = fm.readInt();
          FieldValidator.checkLength(matlLength, arcSize);

          fm.skip(matlLength);
          length += 8 + matlLength;

          header = fm.readString(4);
        }
        if (header.equals("BBOX")) {
          // 4 - Block Length (not including these 2 fields)
          int bboxLength = fm.readInt();
          FieldValidator.checkLength(bboxLength, arcSize);

          fm.skip(bboxLength);
          length += 8 + bboxLength;

          header = fm.readString(4);
        }
        if (!header.equals("MESH")) {
          ErrorLogger.log("[TOD_TOD2] Unknown mesh block: " + header);
        }

        // 4 - Block Length (not including these 2 fields)
        int meshLength = fm.readInt();
        FieldValidator.checkLength(meshLength, arcSize);

        // 16 - Mesh Name/Description (null terminated, filled with nulls)
        String filename = fm.readNullString(16);
        if (arcHeader.equals("MATL")) {
          // The meshes don't have names
          filename = Resource.generateFilename(realNumFiles);
        }
        FieldValidator.checkFilename(filename);
        filename += ".mesh";

        fm.skip(meshLength - 16); // we read 16 from the mesh already
        length += 8 + meshLength; // add 8 to cover the MESH header fields, which we want to include

        // 4 - Header (ANIM)
        fm.skip(4);

        // 4 - Block Length (not including these 2 fields) (44)
        int animLength = fm.readInt();
        FieldValidator.checkLength(animLength, arcSize);

        fm.skip(animLength);
        length += 8 + animLength; // add the ANIM to the file

        if (arcHeader.equals("TOD2")) {
          // 4 - Header (KMSH)
          fm.skip(4);

          // 4 - Block Length (not including these 2 fields) (44)
          int kmshLength = fm.readInt();
          FieldValidator.checkLength(kmshLength, arcSize);

          fm.skip(kmshLength);
          length += 8 + kmshLength; // add the KMSH to the file
        }

        //path,name,offset,length,decompLength,exporter
        resources[realNumFiles] = new Resource(path, filename, offset, length);
        realNumFiles++;

        TaskProgressManager.setValue(offset);
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
