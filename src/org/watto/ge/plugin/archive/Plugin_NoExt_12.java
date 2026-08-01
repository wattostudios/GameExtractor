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

import org.watto.ErrorLogger;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.io.FileManipulator;
import org.watto.io.FilenameSplitter;
import org.watto.io.converter.ByteConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_NoExt_12 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_NoExt_12() {

    super("NoExt_12", "GameLoft J2ME Archives");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("GameLoft J2ME Games",
        "Asphalt Nitro");
    setExtensions(""); // MUST BE LOWER CASE
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

      String extension = FilenameSplitter.getExtension(fm.getFile());
      String filePath = fm.getFile().getAbsolutePath();

      if (extension == null || extension.equals("")) {
        // already reading the base archive
        rating += 25;

        // Number Of Files
        if (FieldValidator.checkNumFiles(fm.readShort())) {
          rating += 5;
        }

        // Number Of Archives
        if (FieldValidator.checkRange(fm.readShort(), 1, 9)) { // guess max 10 archives
          rating += 5;
        }

        if (fm.readShort() == 0) {
          rating += 5;
        }
      }
      else if (extension.length() == 1) {
        if (new File(filePath.substring(0, filePath.length() - 2)).exists()) {
          // found the base archive
          rating += 25;

          // First File Offset
          if (FieldValidator.checkOffset(fm.readInt(), fm.getLength())) {
            rating += 5;
          }
        }
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

      // Find the base archive
      String extension = FilenameSplitter.getExtension(path);
      String filePath = path.getAbsolutePath();

      File baseArchive = path;
      if (extension == null || extension.equals("")) {
        // already have the base archive
      }
      else if (extension.length() == 1) {
        baseArchive = new File(filePath.substring(0, filePath.length() - 2));
        if (baseArchive.exists()) {
          // found the base archive
        }
        else {
          // rever to using the existing chosen file
          baseArchive = path;
        }
      }

      FileManipulator fm = new FileManipulator(baseArchive, false);

      // 2 - Number of Files
      short numFiles = fm.readShort();
      FieldValidator.checkNumFiles(numFiles);

      // 2 - Number of Archives
      int numArchives = fm.readShort();
      FieldValidator.checkNumFiles(numArchives);

      // build all the archive Files, lengths, etc.
      File[] archiveFiles = new File[numArchives];
      long[] archiveLengths = new long[numArchives];
      int[] archiveNumFiles = new int[numArchives + 1];

      String basePath = baseArchive.getAbsolutePath();
      for (int i = 0; i < numArchives; i++) {
        if (i == 0) {
          archiveFiles[i] = baseArchive;
          archiveLengths[i] = baseArchive.length();
        }
        else {
          File archiveFile = new File(basePath + "." + i);
          if (!archiveFile.exists()) {
            ErrorLogger.log("[NoExt_12] Missing archive file number " + i);
            return null;
          }
          archiveFiles[i] = archiveFile;
          archiveLengths[i] = archiveFile.length();
        }
      }

      // read the starting file numbers
      archiveNumFiles[numArchives] = numFiles;
      for (int i = 0; i < numArchives; i++) {
        archiveNumFiles[i] = fm.readShort();
      }

      // set the numFiles correctly now
      for (int i = 0; i < numArchives; i++) {
        archiveNumFiles[i] = archiveNumFiles[i + 1] - archiveNumFiles[i];
      }

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // read each archive
      int realNumFiles = 0;
      for (int a = 0; a < numArchives; a++) {

        File archiveFile = archiveFiles[a];

        fm.close();
        fm = new FileManipulator(archiveFile, false);

        if (a == 0) {
          // skip over the archive header details, to the directory
          fm.skip(4 + numArchives * 2);
        }

        int numFilesInArchive = archiveNumFiles[a];

        long arcSize = archiveLengths[a];

        // 4 - First File Offset
        int offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // Loop through directory
        for (int i = 0; i < numFilesInArchive; i++) {

          // 4 - Next File Offset
          int nextOffset = fm.readInt();
          FieldValidator.checkOffset(nextOffset, arcSize + 1); // as the last file in each archive is the archive length

          int length = nextOffset - offset;
          FieldValidator.checkLength(length, arcSize);

          offset = nextOffset;

          String filename = Resource.generateFilename(realNumFiles);

          //path,name,offset,length,decompLength,exporter
          Resource resource = new Resource(archiveFile, filename, offset, length);
          resource.forceNotAdded(true);
          resources[realNumFiles] = resource;

          TaskProgressManager.setValue(realNumFiles);
          realNumFiles++;
        }
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

    boolean shuffleOffset = false;

    String type = null;
    if (headerInt1 == 1750355200) {
      type = "mid";
      shuffleOffset = true;
    }
    else if (headerInt1 == 1179210241) {
      type = "wav";
      shuffleOffset = true;
    }
    else if (ByteConverter.unsign(headerBytes[1]) == 223 && headerBytes[2] == 5) {
      type = "sprite";
      shuffleOffset = true;
    }

    if (shuffleOffset) {
      resource.setOffset(resource.getOffset() + 1);
      long length = resource.getLength() - 1;
      resource.setLength(length);
      resource.setDecompressedLength(length);
    }

    return type;
  }

}
