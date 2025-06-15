/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2024 wattostudios
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

import org.watto.datatype.Archive;
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
public class Plugin_PAGES extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_PAGES() {

    super("PAGES", "PAGES");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("The Evil Within");
    setExtensions("pages"); // MUST BE LOWER CASE
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
      if (fm.readInt() == 1999870212) {
        rating += 50;
      }

      if (fm.readInt() == 2048) {
        rating += 5;
      }

      fm.skip(12);

      if (fm.readInt() == 64) {
        rating += 5;
      }

      long arcSize = fm.getLength();

      // Archive Size
      if (FieldValidator.checkEquals(fm.readInt(), arcSize)) {
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

      //ExporterPlugin exporter = Exporter_Deflate.getInstance();

      // RESETTING GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false, 128); // small quick reads

      long arcSize = fm.getLength();

      fm.skip(128);

      int numFiles = Archive.getMaxFiles();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(arcSize);

      long[] offsets = new long[numFiles];
      int currentOffset = -1;
      int writingOffset = 0;

      // Loop through directory
      int realNumFiles = 0;
      ResourceSorter_Offset[] sorter = new ResourceSorter_Offset[numFiles];
      while (fm.getOffset() < arcSize) {
        // 4 - Entry Header? (4,237,191,202)
        // 4 - Unknown (4)
        fm.skip(8);

        // 4 - Next File Offset 1 [*64]
        long nextOffset = fm.readInt() * 64;
        if (nextOffset != 0) {
          offsets[writingOffset] = nextOffset;
          writingOffset++;
        }
        long firstOffset = nextOffset;

        // 4 - Next File Offset 2 [*64] (or null)
        nextOffset = fm.readInt() * 64;
        if (nextOffset != 0) {
          offsets[writingOffset] = nextOffset;
          writingOffset++;
        }

        // 4 - Next File Offset 3 [*64] (or null)
        nextOffset = fm.readInt() * 64;
        if (nextOffset != 0) {
          offsets[writingOffset] = nextOffset;
          writingOffset++;
        }

        // 4 - Next File Offset 4 [*64] (or null)
        nextOffset = fm.readInt() * 64;
        if (nextOffset != 0) {
          offsets[writingOffset] = nextOffset;
          writingOffset++;
        }

        // 4 - Unknown
        // 4 - Unknown
        // 4 - null
        // 4 - Unknown ID
        // 4 - Unknown Header (all (byte)90)
        // 4 - Unknown
        // 4 - Unknown
        // 4 - Unknown
        // 4 - Unknown
        // 4 - Unknown
        // 4 - Unknown
        // 4 - Unknown
        // 32 - null
        fm.skip(80);

        // X - File Data
        long offset = fm.getOffset();
        int length = (int) (firstOffset - offset);

        // X - null Padding to a multiple of 64 bytes

        String filename = Resource.generateFilename(realNumFiles);

        //path,name,offset,length,decompLength,exporter
        Resource resource = new Resource(path, filename, offset, length);
        resources[realNumFiles] = resource;

        sorter[realNumFiles] = new ResourceSorter_Offset(resource);

        TaskProgressManager.setValue(offset);

        realNumFiles++;

        // now seek to the next offset
        currentOffset++;
        nextOffset = offsets[currentOffset];
        if (nextOffset == 0) {
          break;
        }
        else {
          fm.seek(nextOffset);
        }
      }

      ResourceSorter_Offset[] sorterOld = sorter;
      sorter = new ResourceSorter_Offset[realNumFiles];
      System.arraycopy(sorterOld, 0, sorter, 0, realNumFiles);

      Arrays.sort(sorter);

      resources = new Resource[realNumFiles];

      for (int i = 0; i < realNumFiles; i++) {
        Resource resource = sorter[i].getResource();
        resources[i] = resource;
        if (i != 0) {
          Resource previousResource = resources[i - 1];
          int length = (int) (resource.getOffset() - previousResource.getOffset() - 104); // 104 is the header size
          previousResource.setDecompressedLength(length);
          previousResource.setLength(length);
        }
      }
      Resource lastResource = resources[realNumFiles - 1];
      int length = (int) (arcSize - lastResource.getOffset());
      lastResource.setDecompressedLength(length);
      lastResource.setLength(length);

      //resources = resizeResources(resources, realNumFiles);

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
