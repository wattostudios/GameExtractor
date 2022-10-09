/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2022 wattostudios
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
import org.watto.datatype.FileType;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.helper.ResourceSorter_Offset;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.io.FileManipulator;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_ARC_ARC0 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_ARC_ARC0() {

    super("ARC_ARC0", "ARC_ARC0");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Big Mutha Truckers");
    setExtensions("arc"); // MUST BE LOWER CASE
    setPlatforms("PS2");

    // MUST BE LOWER CASE !!!
    setFileTypes(new FileType("arc_tex", "Texture Image", FileType.TYPE_IMAGE));

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
      if (fm.readString(4).equals("ARC0")) {
        rating += 50;
      }

      // Number Of Files
      if (FieldValidator.checkNumFiles(fm.readInt())) {
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

      // 4 - Header (ARC0)
      fm.skip(4);

      // 4 - Number of Files (including the start and end markers)
      int numFiles = fm.readInt() - 2; // -2 to exclude the markers
      FieldValidator.checkNumFiles(numFiles);

      //for (28){
      //  2 - Number of Files of type #
      //  }
      fm.skip(56);

      // 4 - null
      // 4 - null
      // 4 - null
      // 2 - Start of Directory Marker (255)
      // 2 - null
      fm.skip(16);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      int baseOffset = (int) fm.getOffset() + numFiles * 16 + 16;

      // Loop through directory
      int[] nameOffsets = new int[numFiles];
      int[] types = new int[numFiles];
      ResourceSorter_Offset[] sorter = new ResourceSorter_Offset[numFiles];
      for (int i = 0; i < numFiles; i++) {
        // 4 - Unknown
        fm.skip(4);

        // 4 - File Offset (relative to the start of the file data)
        int offset = fm.readInt() + baseOffset;
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - Filename Offset (relative to the start of the filename directory)
        int filenameOffset = fm.readInt();
        nameOffsets[i] = filenameOffset;

        // 2 - File Type ID (1=Image (bmp/tga))
        short fileType = fm.readShort();
        types[i] = fileType;

        // 2 - Unknown
        fm.skip(2);

        //path,name,offset,length,decompLength,exporter
        Resource resource = new Resource(path, "", offset);
        resources[i] = resource;

        sorter[i] = new ResourceSorter_Offset(resource);

        TaskProgressManager.setValue(i);
      }

      // 4 - null
      fm.skip(4);

      // 4 - File Data Length
      int fileDataLength = fm.readInt();
      FieldValidator.checkLength(fileDataLength, arcSize);

      // 4 - Unknown (-1)
      // 2 - End of Directory Marker (253)
      // 2 - Unknown
      fm.skip(8);

      int filenameDirOffset = baseOffset + fileDataLength;

      // read the filenames
      for (int i = 0; i < numFiles; i++) {
        int nameOffset = nameOffsets[i];
        String filename = null;
        if (nameOffset == -1) {
          filename = Resource.generateFilename(i);
        }
        else {
          fm.relativeSeek(filenameDirOffset + nameOffset);

          // X - Filename
          filename = fm.readNullString();
        }

        int typeNumber = types[i];

        String fileType = "" + typeNumber;
        if (typeNumber == 1) {
          fileType = "arc_tex";
        }

        filename += "." + fileType;

        Resource resource = resources[i];
        resource.setName(filename);
        resource.setOriginalName(filename);
      }

      Arrays.sort(sorter);

      // put the resources back in order of the sorter
      for (int i = 0; i < numFiles; i++) {
        resources[i] = sorter[i].getResource();
      }

      calculateFileSizes(resources, filenameDirOffset);

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
