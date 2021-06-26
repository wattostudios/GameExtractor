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
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.io.FileManipulator;
import org.watto.io.FilenameSplitter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_000_PCW extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_000_PCW() {

    super("000_PCW", "000_PCW");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Deus Ex: Human Revolution");
    setExtensions("000"); // MUST BE LOWER CASE
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

        // 4 - Unknown
        fm.skip(4);

        // 64 - Name ("pc-w" + nulls to fill)
        if (fm.readString(4).equals("pc-w")) {
          rating += 50;
        }
      }
      else {
        String extension = FilenameSplitter.getExtension(fm.getFile());
        Integer.parseInt(extension);

        getDirectoryFile(fm.getFile(), "000");
        rating += 50;
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

      long arcSize = (int) path.length();

      File sourcePath = getDirectoryFile(path, "000");
      FileManipulator fm = new FileManipulator(sourcePath, false);

      // 4 - Maximum Archive File Size
      long maxArchiveFileSize = fm.readInt();
      FieldValidator.checkLength(maxArchiveFileSize);

      // 64 - Name ("pc-w" + nulls to fill)
      fm.skip(64);

      // 4 - Number of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles / 8);

      // skip unknown dir 1      
      fm.skip(numFiles * 4);

      // Work out all the *.0## files and their sizes
      int maxArchives = 100;

      File[] archiveFiles = new File[maxArchives];
      long[] archiveSizes = new long[maxArchives];
      long[] relativeOffsets = new long[maxArchives];

      String archiveBase = sourcePath.getAbsolutePath();
      archiveBase = archiveBase.substring(0, archiveBase.length() - 3);

      arcSize = 0;
      int numArchives = 0;
      for (int i = 0; i < maxArchives; i++) {
        String extension = "0" + i;
        if (i < 10) {
          extension = "00" + i;
        }

        File archiveFile = new File(archiveBase + extension);
        if (!archiveFile.exists()) {
          // no more archives
          numArchives = i;
          break;
        }

        archiveFiles[i] = archiveFile;
        archiveSizes[i] = archiveFile.length();
        //relativeOffsets[i] = arcSize;
        //arcSize += archiveSizes[i];
        relativeOffsets[i] = maxArchiveFileSize * i;
      }
      arcSize = (maxArchiveFileSize * (numArchives - 1)) + archiveSizes[numArchives - 1];

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      int currentArchive = 0;
      File currentArchiveFile = archiveFiles[currentArchive];
      long relativeOffset = relativeOffsets[currentArchive];

      for (int i = 0; i < numFiles; i++) {

        // 4 - File Length
        int length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - File Offset [*2048]
        long offset = (((long) fm.readInt()) * 2048);
        //FieldValidator.checkOffset(offset, arcSize);

        // find which archive file it's in
        for (int a = 0; a < numArchives; a++) {
          long relOffset = relativeOffsets[a];
          if (offset < relOffset) {
            break;
          }
          currentArchiveFile = archiveFiles[a];
          relativeOffset = relOffset;
        }
        offset -= relativeOffset;

        /*if (offset > currentArchiveSize) {
          // move to the next archive
          currentArchive++;
          currentArchiveFile = archiveFiles[currentArchive];
          currentArchiveSize = archiveSizes[currentArchive];
          relativeOffset = relativeOffsets[currentArchive];
        }*/

        // 4 - Unknown
        // 4 - null
        fm.skip(8);

        String filename = Resource.generateFilename(i);

        //path,name,offset,length,decompLength,exporter
        Resource resource = new Resource(currentArchiveFile, filename, offset, length);
        resource.forceNotAdded(true);
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

    if (headerInt1 == 1145655875) {
      return "crid";
    }
    else if (headerInt1 == 1297237059) {
      return "cdrm";
    }
    else if (headerInt1 == 22050 || headerInt1 == 32000 || headerInt1 == 44100 || headerInt1 == 48000) {
      return "sound";
    }
    else if (headerInt1 == 561214797) {
      return "mus";
    }
    else if (headerInt1 == 876761926) {
      return "fsb";
    }
    else if (headerInt1 == 960774992) {
      return "pcd9";
    }
    else if (headerInt2 == 19490) {
      return "lang";
    }

    return null;
  }

}
