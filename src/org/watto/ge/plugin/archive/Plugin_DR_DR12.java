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
import org.watto.datatype.Archive;
import org.watto.datatype.FileType;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.exporter.Exporter_ZLib;
import org.watto.io.FileManipulator;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_DR_DR12 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_DR_DR12() {

    super("DR_DR12", "DR_DR12");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Showdown: Legends of Wrestling");
    setExtensions("dr"); // MUST BE LOWER CASE
    setPlatforms("PS2");

    // MUST BE LOWER CASE !!!
    setFileTypes(new FileType("8image", "Paletted Image", FileType.TYPE_IMAGE),
        new FileType("16image", "16bit Image", FileType.TYPE_IMAGE));

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
      // ZLib header
      if (fm.readString(1).equals("x")) {
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

      ExporterPlugin exporter = Exporter_ZLib.getInstance();

      // RESETTING GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false, 28); // 28 for reading the headers only

      long arcSize = fm.getLength();

      int numFiles = Archive.getMaxFiles();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      fm.seek(arcSize - 28);

      // Loop through directory
      int realNumFiles = 0;
      while (fm.getOffset() > 0) {

        // 4 - Footer (DR12)
        //String footer = fm.readString(4);
        //System.out.println((fm.getOffset() - 4) + "\t" + footer);
        // 4 - Unknown
        // 4 - Unknown
        //int field1 = fm.readShort();
        //int field2 = fm.readShort();
        //System.out.println(field1 + "\t" + field2 + "\t at " + (fm.getOffset() - 12));
        // 4 - Unknown
        fm.skip(16);

        // 4 - Decompressed Length
        int decompLength = fm.readInt();
        FieldValidator.checkLength(decompLength);

        // 4 - Compressed Length (not including the padding of these footer fields)
        int length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - Unknown
        fm.skip(4);

        int lengthWithPadding = (length + 28);
        lengthWithPadding += calculatePadding(lengthWithPadding, 2048);

        long offset = fm.getOffset() - lengthWithPadding;
        FieldValidator.checkOffset(offset, arcSize);

        String filename = Resource.generateFilename(realNumFiles);

        if (decompLength == length) {
          // uncompressed

          //path,name,offset,length,decompLength,exporter
          resources[realNumFiles] = new Resource(path, filename, offset, length, decompLength);
        }
        else {
          // compressed

          //path,name,offset,length,decompLength,exporter
          resources[realNumFiles] = new Resource(path, filename, offset, length, decompLength, exporter);
        }
        realNumFiles++;

        TaskProgressManager.setValue(arcSize - offset);

        if (offset == 0) {
          break; // found the file at the start of the archive
        }

        offset -= 28;
        FieldValidator.checkOffset(offset, arcSize);

        fm.seek(offset);
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

    if (headerInt1 == 1347629362) {
      return "21sp";
    }
    else if (headerInt2 == 1 && (headerInt1 > 0 && headerInt1 < 10000)) {
      return "mdf_mesh";
    }
    else if (headerInt3 == 256) {
      return "8image";
    }
    else if ((headerInt1 * headerInt2 * 2) + 64 == resource.getDecompressedLength()) {
      return "16image";
    }

    return null;
  }

}
