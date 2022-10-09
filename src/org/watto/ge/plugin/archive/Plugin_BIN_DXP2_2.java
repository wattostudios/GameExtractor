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
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.exporter.Exporter_ZStd;
import org.watto.io.FileManipulator;
import org.watto.io.converter.IntConverter;
import org.watto.io.converter.StringConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_BIN_DXP2_2 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_BIN_DXP2_2() {

    super("BIN_DXP2_2", "BIN_DXP2_2");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("CRSED: F.O.A.D.");
    setExtensions("bin"); // MUST BE LOWER CASE
    setPlatforms("PC");

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
      if (fm.readString(4).equals("DxP2")) {
        rating += 50;
      }

      // Version
      if (fm.readInt() == 2) {
        rating += 5;
      }

      // Number Of Files
      if (FieldValidator.checkNumFiles(fm.readInt())) {
        rating += 5;
      }

      long arcSize = fm.getLength();

      // File Data Offset
      if (FieldValidator.checkOffset(fm.readInt(), arcSize)) {
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

      ExporterPlugin exporter = Exporter_ZStd.getInstance();

      // RESETTING GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 4 - Header (DxP2)
      // 4 - Version (2)
      fm.skip(8);

      // 4 - Number Of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // 4 - Padding Offset [+16]
      fm.skip(4);

      // 4 - Filename Offset Directory Offset [+16]
      int filenameDirOffset = fm.readInt() + 16;
      FieldValidator.checkOffset(filenameDirOffset, arcSize);

      // 4 - Number of Filename Offset Entries
      int numFilenames = fm.readInt();
      FieldValidator.checkNumFiles(numFilenames);

      // 8 - null
      fm.skip(8);

      // 4 - Offset to the Image Properties Directory [+16]
      int imagePropertyDirOffset = fm.readInt() + 16;
      FieldValidator.checkOffset(imagePropertyDirOffset, arcSize);

      // 4 - Number of Image Property Entries
      int numImageProperties = fm.readInt();
      FieldValidator.checkNumFiles(numImageProperties);

      // 8 - null
      fm.skip(8);

      // 4 - Offset to the File Data Properties Directory [+16]
      int filePropertyDirOffset = fm.readInt() + 16;
      FieldValidator.checkOffset(filePropertyDirOffset, arcSize);

      // 4 - Number of File Data Property Entries
      int numFileProperties = fm.readInt();
      FieldValidator.checkNumFiles(numFileProperties);

      // 8 - null
      // 8 - null
      fm.skip(16);

      // Loop through directory
      String[] names = new String[numFilenames];
      for (int i = 0; i < numFilenames; i++) {
        // X - Filename (null)
        String filename = fm.readNullString();
        FieldValidator.checkFilename(filename);
        names[i] = filename;

        TaskProgressManager.setValue(i);
      }

      fm.seek(imagePropertyDirOffset);

      // Loop through directory
      String[] imageFormats = new String[numImageProperties];
      int[] widths = new int[numImageProperties];
      int[] heights = new int[numImageProperties];
      int[] decompLengths = new int[numImageProperties];
      for (int i = 0; i < numImageProperties; i++) {

        // 4 - Image Format (DDSx)
        fm.skip(4);

        // 4 - DDS Format (DXT1/DXT3/DXT5/(byte)21/(byte)50)
        byte[] imageFormatBytes = fm.readBytes(4);
        String imageFormat = StringConverter.convertLittle(imageFormatBytes);
        if (imageFormat.equals("DXT1") || imageFormat.equals("DXT3") || imageFormat.equals("DXT5") || imageFormat.equals("BC7 ") || imageFormat.equals("ATI2") || imageFormat.equals("ATI1")) {
          //
        }
        else {
          //System.out.println(imageFormat);
          imageFormat = "" + IntConverter.convertLittle(imageFormatBytes);
        }
        imageFormats[i] = imageFormat;

        // 4 - Unknown
        fm.skip(4);

        // 2 - Image Width
        short width = fm.readShort();
        FieldValidator.checkWidth(width);
        widths[i] = width;

        // 2 - Image Height
        short height = fm.readShort();
        FieldValidator.checkHeight(height);
        heights[i] = height;

        // 4 - Unknown
        // 4 - Unknown
        fm.skip(8);

        // 4 - Decompressed File Length
        int decompLength = fm.readInt();
        FieldValidator.checkLength(decompLength);
        decompLengths[i] = decompLength;

        // 4 - Compressed File Length
        fm.skip(4);

        TaskProgressManager.setValue(i);
      }

      fm.seek(filePropertyDirOffset);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      for (int i = 0; i < numFileProperties; i++) {
        // 8 - null
        // 4 - Unknown (-1)
        fm.skip(12);

        // 4 - File Offset
        int offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - Compressed Length
        int length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - null
        fm.skip(4);

        int decompLength = decompLengths[i];

        String filename = names[i];
        int starPos = filename.indexOf('*');
        if (starPos > 0) {
          filename = filename.substring(0, starPos);
        }
        filename += ".ddsx";

        String imageFormat = imageFormats[i];
        int width = widths[i];
        int height = heights[i];

        Resource resource = null;
        if (decompLength == 0) {
          // uncompressed

          //path,name,offset,length,decompLength,exporter
          resource = new Resource(path, filename, offset, length);
        }
        else {
          // compressed

          /*
          if (imageFormat.equals("DXT1")) {
            decompLength = width * height / 2;
          }
          else if (imageFormat.equals("DXT3") || imageFormat.equals("DXT5")) {
            decompLength = width * height;
          }
          else if (imageFormat.equals("21")) {
            decompLength = width * height * 4;
          }
          else if (imageFormat.equals("50")) {
            decompLength = width * height;
          }
          */

          //path,name,offset,length,decompLength,exporter
          resource = new Resource(path, filename, offset, length, decompLength, exporter);
        }

        resource.addProperty("Width", width);
        resource.addProperty("Height", height);
        resource.addProperty("ImageFormat", imageFormat);

        resources[i] = resource;

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

    /*
    if (headerInt1 == 2037149520) {
      return "js";
    }
    */

    return null;
  }

}
