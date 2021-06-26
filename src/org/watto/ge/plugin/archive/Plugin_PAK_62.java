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
import org.watto.datatype.Archive;
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
public class Plugin_PAK_62 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_PAK_62() {

    super("PAK_62", "PAK_62");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Torchlight 2");
    setExtensions("pak"); // MUST BE LOWER CASE
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

      getDirectoryFile(fm.getFile(), "PAK.MAN");
      rating += 25;

      fm.skip(8);

      long arcSize = fm.getLength();

      // Compressed Length
      if (FieldValidator.checkLength(fm.readInt(), arcSize)) {
        rating += 5;
      }

      // Decompressed Length
      if (FieldValidator.checkLength(fm.readInt())) {
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

      long arcSize = (int) path.length();

      File sourcePath = getDirectoryFile(path, "PAK.MAN");
      FileManipulator fm = new FileManipulator(sourcePath, false);

      // 2 - Unknown (2)
      // 4 - Unknown
      fm.skip(6);

      // 2 - Root Folder Name Length [*2 for unicode]
      short rootNameLength = fm.readShort();
      FieldValidator.checkFilenameLength(rootNameLength);

      // X - Root Folder Name (unicode)
      fm.skip(rootNameLength * 2);

      // 4 - Unknown
      // 4 - Unknown
      fm.skip(8);

      int numFiles = Archive.getMaxFiles();
      int realNumFiles = 0;

      long dirLength = fm.getLength();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(dirLength);

      // Loop through directory
      while (fm.getOffset() < dirLength) {

        // 2 - Folder Name Length [*2 for unicode]
        short folderNameLength = fm.readShort();
        FieldValidator.checkFilenameLength(folderNameLength);

        // X - Folder Name (unicode)
        String folderName = fm.readUnicodeString(folderNameLength);
        //System.out.println(fm.getOffset() + "\t" + folderName);

        // 4 - Number of Entries in this Folder
        int numFilesInFolder = fm.readInt();
        FieldValidator.checkNumFiles(numFilesInFolder);

        for (int i = 0; i < numFilesInFolder; i++) {
          // 4 - Unknown
          // 1 - Entry Type?
          fm.skip(5);

          // 2 - Filename Length [*2 for unicode]
          short filenameLength = fm.readShort();
          FieldValidator.checkFilenameLength(filenameLength);

          // X - Filename (unicode)
          String filename = folderName + fm.readUnicodeString(filenameLength);
          //System.out.println(fm.getOffset() + "\t" + entryType + "\t" + filename);

          // 4 - File Offset
          int offset = fm.readInt();

          // 4 - Decompressed File Length
          int decompLength = fm.readInt();

          // 8 - Unknown
          fm.skip(8);

          if (decompLength != 0 && offset != 0) {
            // a file
            FieldValidator.checkOffset(offset, arcSize);
            FieldValidator.checkLength(decompLength);

            resources[realNumFiles] = new Resource(path, filename, offset, 0, decompLength);
            realNumFiles++;

            TaskProgressManager.setValue(fm.getOffset());
          }

        }

      }

      resources = resizeResources(resources, realNumFiles);

      // now open the REAL archive and get the file sizes
      fm.close();
      fm = new FileManipulator(path, false, 8);

      numFiles = realNumFiles;

      TaskProgressManager.setMaximum(numFiles);
      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];
        fm.seek(resource.getOffset());

        // 4 - Decompressed Length
        int decompLength = fm.readInt();
        FieldValidator.checkLength(decompLength);

        // 4 - Compressed Length
        int length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        //System.out.println(resource.getName() + "\t" + decompLength + "\t" + length);

        // X - Compressed File Data (ZLib)
        long offset = fm.getOffset();

        if (length == 0) {
          // not compressed
          length = decompLength;

          resource.setOffset(offset);
          resource.setLength(length);
          resource.setDecompressedLength(decompLength);
        }
        else {
          // compressed
          resource.setOffset(offset);
          resource.setLength(length);
          resource.setDecompressedLength(decompLength);
          resource.setExporter(exporter);
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
