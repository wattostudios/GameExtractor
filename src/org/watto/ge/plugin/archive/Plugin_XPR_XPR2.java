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
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.io.FileManipulator;
import org.watto.io.converter.ByteConverter;
import org.watto.io.converter.IntConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_XPR_XPR2 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_XPR_XPR2() {

    super("XPR_XPR2", "XPR_XPR2");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Ninja Gaiden 2");
    setExtensions("xpr"); // MUST BE LOWER CASE
    setPlatforms("XBox 360");

    // MUST BE LOWER CASE !!!
    //setFileTypes(new FileType("txt", "Text Document", FileType.TYPE_DOCUMENT),
    //             new FileType("bmp", "Bitmap Image", FileType.TYPE_IMAGE)
    //             );

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
      if (fm.readString(4).equals("XPR2")) {
        rating += 50;
      }

      long arcSize = fm.getLength();

      // Directory Length [+12] (including padding)
      if (FieldValidator.checkLength(IntConverter.changeFormat(fm.readInt()), arcSize)) {
        rating += 5;
      }

      // XPR2 Block Length [+12]
      if (FieldValidator.checkLength(IntConverter.changeFormat(fm.readInt()), arcSize)) {
        rating += 5;
      }

      // Number Of Files
      if (FieldValidator.checkNumFiles(IntConverter.changeFormat(fm.readInt()))) {
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

      // 4 - Header (XPR2)
      fm.skip(4);

      // 4 - Directory Length (including padding) [+12]
      int dataOffset = IntConverter.changeFormat(fm.readInt()) + 12;
      FieldValidator.checkOffset(dataOffset, arcSize);

      // 4 - XPR2 Block Length [+12]
      fm.skip(4);

      // 4 - Number of Files
      int numFiles = IntConverter.changeFormat(fm.readInt());
      FieldValidator.checkNumFiles(numFiles);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      String[] fileTypes = new String[numFiles];
      int[] detailsOffsets = new int[numFiles];
      int[] nameOffsets = new int[numFiles];
      for (int i = 0; i < numFiles; i++) {
        // 4 - File Type (TX2D)
        String fileType = fm.readString(4);

        // 4 - Details Offset for this File [+12] (relative to the start of the Details Directory)
        int detailsOffset = IntConverter.changeFormat(fm.readInt()) + 12;
        FieldValidator.checkOffset(detailsOffset, arcSize);

        // 4 - Details Length for this File
        fm.skip(4);

        // 4 - Filename Offset [+12] (relative to the start of the Details Directory)
        int nameOffset = IntConverter.changeFormat(fm.readInt()) + 12;
        FieldValidator.checkOffset(nameOffset, arcSize);

        fileTypes[i] = fileType;
        detailsOffsets[i] = detailsOffset;
        nameOffsets[i] = nameOffset;
      }

      String[] names = new String[numFiles];
      for (int i = 0; i < numFiles; i++) {
        fm.relativeSeek(nameOffsets[i]);

        // X - Filename
        // 1 - null Filename Terminator
        String filename = fm.readNullString();
        FieldValidator.checkFilename(filename);
        names[i] = filename;
      }

      for (int i = 0; i < numFiles; i++) {
        fm.relativeSeek(detailsOffsets[i]);

        // 4 - Unknown (3)
        // 4 - Unknown (1)
        // 12 - null
        // 4 - Unknown
        // 4 - Unknown
        // 4 - Unknown
        fm.skip(32);

        // 3 - File Offset (relative to the end start of the File Data)
        // 1 - Image Format? (2=L8 greyscale, 134=ARGB)
        byte[] offsetBytes = fm.readBytes(4);
        int imageFormat = ByteConverter.unsign(offsetBytes[3]);
        offsetBytes[3] = 0;

        int offset = IntConverter.convertBig(offsetBytes) + dataOffset;
        FieldValidator.checkOffset(offset, arcSize);

        // 2 - Image Height [+1]
        // 2 - Image Width [+1]
        int dimension = IntConverter.changeFormat(fm.readInt());

        int height = (dimension >> 13) + 1;
        FieldValidator.checkHeight(height);

        int width = (dimension & 2047) + 1;
        FieldValidator.checkWidth(width);

        // 2 - null
        // 2 - Image Format? (20=L8 greyscale, 3092=ARGB)
        // 4 - null
        // 4 - Unknown (512)
        fm.skip(12);

        String filename = names[i] + "." + fileTypes[i];

        //path,name,offset,length,decompLength,exporter
        Resource resource = new Resource(path, filename, offset);
        resource.addProperty("Height", height);
        resource.addProperty("Width", width);
        resource.addProperty("ImageFormat", imageFormat);
        resources[i] = resource;

        TaskProgressManager.setValue(i);
      }

      calculateFileSizes(resources, arcSize);

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
