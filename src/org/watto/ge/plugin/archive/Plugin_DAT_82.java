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
import org.watto.io.FileManipulator;
import org.watto.io.converter.IntConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_DAT_82 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_DAT_82() {

    super("DAT_82", "DAT_82");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Who Wants To Be A Millionaire");
    setExtensions("dat"); // MUST BE LOWER CASE
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

      fm.skip(4);

      long arcSize = fm.getLength();

      // 4 - File Data Length
      if (FieldValidator.checkLength(IntConverter.changeFormat(fm.readInt()), arcSize)) {
        rating += 5;
      }

      // 4 - Filename Directory Length
      // 4 - Unknown Directory Length
      fm.skip(8);

      // 8 - null
      if (fm.readLong() == 0) {
        rating += 5;
      }

      // 4 - Details Directory Length
      if (FieldValidator.checkLength(IntConverter.changeFormat(fm.readInt()), arcSize)) {
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

      // 4 - Unknown (131072)
      fm.skip(4);

      // 4 - File Data Length
      int filenameDirOffset = IntConverter.changeFormat(fm.readInt()) + 32;
      FieldValidator.checkOffset(filenameDirOffset, arcSize);

      // 4 - Filename Directory Length
      int filenameDirLength = IntConverter.changeFormat(fm.readInt());
      FieldValidator.checkLength(filenameDirLength, arcSize);

      // 4 - Unknown Directory 1 Length
      int unknownDir1Length = IntConverter.changeFormat(fm.readInt());
      FieldValidator.checkLength(unknownDir1Length, arcSize);

      int unknownDirOffset = filenameDirOffset + filenameDirLength;
      FieldValidator.checkOffset(unknownDirOffset, arcSize);

      // 4 - Unknown Directory 2 Length
      int unknownDir2Length = IntConverter.changeFormat(fm.readInt());
      FieldValidator.checkLength(unknownDir2Length + 1, arcSize); // +1 to allow for nulls

      // 4 - Unknown Directory 3 Length
      int unknownDir3Length = IntConverter.changeFormat(fm.readInt());
      FieldValidator.checkLength(unknownDir3Length + 1, arcSize); // +1 to allow for nulls

      // 4 - Details Directory Length
      int dirLength = IntConverter.changeFormat(fm.readInt());
      FieldValidator.checkLength(dirLength, arcSize);

      int dirOffset = filenameDirOffset + filenameDirLength + unknownDir1Length + unknownDir2Length + unknownDir3Length;
      FieldValidator.checkOffset(dirOffset, arcSize);

      // 4 - null
      fm.skip(4);

      int numFiles = dirLength / 28;
      FieldValidator.checkNumFiles(numFiles);

      int numNames = unknownDir1Length / 8;
      FieldValidator.checkNumFiles(numNames);

      fm.seek(filenameDirOffset);

      String[] names = new String[numNames];
      for (int i = 0; i < numNames; i++) {
        // 1 - Filename Length
        int filenameLength = fm.readByte();

        // X - Filename
        String filename = fm.readString(filenameLength);
        names[i] = filename;

        // 2 - File ID (incremental from 0)
        fm.skip(2);
      }

      fm.seek(unknownDirOffset);

      String[] mappedNames = new String[numFiles];
      for (int i = 0; i < numNames; i++) {
        // 4 - Details Entry ID
        int fileID = IntConverter.changeFormat(fm.readInt());
        mappedNames[fileID] = names[i];

        // 4 - Unknown (65536)
        fm.skip(4);
      }

      fm.seek(dirOffset);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {

        // 4 - File Offset
        int offset = IntConverter.changeFormat(fm.readInt());
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - Compressed File Length
        int length = IntConverter.changeFormat(fm.readInt());
        FieldValidator.checkLength(length, arcSize);

        // 4 - Unknown (null if not compressed)
        fm.skip(4);

        // 4 - Decompressed File Length? (null if not compressed)
        int decompLength = IntConverter.changeFormat(fm.readInt());
        FieldValidator.checkLength(decompLength + 1); // +1 to allow empty files for uncompressed

        // 8 - null
        // 4 - Unknown (262144)
        fm.skip(12);

        String filename = mappedNames[i];
        if (filename == null) {
          filename = Resource.generateFilename(i);
        }

        if (decompLength == 0) {
          // uncompressed

          //path,name,offset,length,decompLength,exporter
          resources[i] = new Resource(path, filename, offset, length);
        }
        else {
          // compressed

          //path,name,offset,length,decompLength,exporter
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
