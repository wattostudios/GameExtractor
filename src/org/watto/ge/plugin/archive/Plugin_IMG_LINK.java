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
import org.watto.io.FileManipulator;
import org.watto.io.FilenameSplitter;
import org.watto.io.converter.ByteConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_IMG_LINK extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_IMG_LINK() {

    super("IMG_LINK", "IMG_LINK");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Shadow Of Rome");
    setExtensions("img");
    setPlatforms("PS2");

    // MUST BE LOWER CASE !!!
    setFileTypes(new FileType("link", "LINK Archive", FileType.TYPE_ARCHIVE),
        new FileType("cs", "CS File", FileType.TYPE_OTHER));

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
      if (fm.readString(4).equals("LINK")) {
        rating += 50;
      }

      fm.skip(8);

      // Length Of Link Header (48)
      if (fm.readInt() == 48) {
        rating += 5;
      }

      long arcSize = fm.getLength();

      // File Length
      if (FieldValidator.checkLength(fm.readInt(), arcSize)) {
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

      // NOTE - Compressed file MUST know their DECOMPRESSED LENGTH
      //      - Uncompressed files MUST know their LENGTH

      addFileTypes();

      // RESETTING THE GLOBAL VARIABLES

      // See if we have the file "SLUS_209.02" in the filesystem. If we do, read the directory from that instead.
      File dirFile = new File(FilenameSplitter.getDirectory(path) + File.separator + "SLUS_209.02");
      if (dirFile.exists()) {
        Resource[] resources = readDirectoryFile(path, dirFile);
        if (resources != null && resources.length > 0) {
          return resources;
        }
      }

      FileManipulator fm = new FileManipulator(path, false);

      int numFiles = Archive.getMaxFiles();

      long arcSize = (int) fm.getLength();

      Resource[] resources = new Resource[numFiles];

      TaskProgressManager.setMaximum(arcSize);

      // Loop through directory
      int realNumFiles = 0;
      int numLinks = 0;
      while (fm.getOffset() < arcSize) {
        long startOffset = fm.getOffset();

        /*
        System.out.println(startOffset);
        if (startOffset == 1201090560) {
          System.out.println("BREAK");
        }
        */

        // 4 - Link Header (LINK)
        String header = fm.readString(4);

        if (header.equals("LINK")) {

          // 1 - Number of Sub-files
          int numSubFiles = ByteConverter.unsign(fm.readByte());

          // 1 - Unknown
          // 2 - null
          fm.skip(3);

          String baseFilename = Resource.generateFilename(numLinks);

          long lastOffset = 0;
          long lastLength = 0;
          int blockSize = 16;
          for (int i = 0; i < numSubFiles; i++) {
            // 4 - Unknown
            fm.skip(4);

            // 4 - Sub-File Offset (relative to the start of the LINK)
            int relOffset = fm.readInt();
            long offset = relOffset + startOffset;
            FieldValidator.checkOffset(offset, arcSize);

            if (lastOffset == 0) {
              // use the first offset to calculate the block size (either 12 or 16)
              blockSize = (int) ((relOffset - 8) / numSubFiles);
              if (blockSize == 17) {
                blockSize = 16;
              }
              else if (blockSize == 13) {
                blockSize = 12;
              }
            }

            lastOffset = offset;

            // 4 - File Length
            int length = fm.readInt();
            FieldValidator.checkLength(length, arcSize);
            lastLength = length;

            if (blockSize == 16) {
              // 4 - null
              fm.skip(4);
            }

            String filename = baseFilename + "-" + (i + 1) + ".link";

            //path,id,name,offset,length,decompLength,exporter
            resources[realNumFiles] = new Resource(path, filename, offset, length);
            realNumFiles++;
          }

          // 8 - null
          // X - File Data (for each sub-file)
          // X - null Padding to a multiple of 2048 bytes

          long nextOffset = lastOffset + lastLength;
          nextOffset += calculatePadding(nextOffset, 2048);
          fm.seek(nextOffset);

          TaskProgressManager.setValue(nextOffset);

          numLinks++;
        }
        else if (header.equals("CShd")) {
          long offset = startOffset;

          // 2 - Unknown (2)
          // 2 - Unknown (1)
          // 8 - null
          // 8 - Unknown (-1)
          // 4 - null
          fm.skip(24);

          // 4 - File Length (including all these fields)
          int length = fm.readInt();
          FieldValidator.checkLength(length, arcSize);

          // X - Rest of the File Data
          // X - null Padding to a multiple of 2048 bytes
          long nextOffset = offset + length;
          nextOffset += calculatePadding(nextOffset, 2048);
          fm.seek(nextOffset);

          String filename = Resource.generateFilename(numLinks) + ".cs";

          //path,id,name,offset,length,decompLength,exporter
          resources[realNumFiles] = new Resource(path, filename, offset, length);
          realNumFiles++;

          numLinks++;
        }
        else {
          //ErrorLogger.log("[IMG_LINK] Unknown file type header: " + header);
          fm.skip(2044); // skipping 2048 bytes, we've already read 4 bytes for the header
        }

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
   
   **********************************************************************************************
   **/
  public Resource[] readDirectoryFile(File path, File dirPath) {
    try {

      int imgNumber = -1;

      String basePath = FilenameSplitter.getDirectory(path) + File.separator;

      File imgFile = null;
      long arcSize = 0;

      FileManipulator fm = new FileManipulator(dirPath, false);

      // HARD CODED DIRECTORY OFFSET AND FILE COUNT
      fm.seek(3516448);
      int numFiles = 694;

      Resource[] resources = new Resource[numFiles];

      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      int realNumFiles = 0;
      for (int i = 0; i < numFiles; i++) {
        // 4 - File Offset [*2048]
        long offset = fm.readInt() * 2048;

        // 4 - File Length (including padding) [*2048]
        long length = fm.readInt() * 2048;

        if (length == 0) {
          continue;
        }

        if (offset == 0) {
          // open the next IMG archive
          imgNumber++;

          imgFile = new File(basePath + "IMAGE" + imgNumber + ".IMG");
          if (!imgFile.exists()) {
            break;
          }

          arcSize = imgFile.length();
        }

        FieldValidator.checkOffset(offset, arcSize);
        FieldValidator.checkLength(length, arcSize);

        String filename = Resource.generateFilename(realNumFiles);

        //path,id,name,offset,length,decompLength,exporter
        Resource resource = new Resource(imgFile, filename, offset, length);
        resource.forceNotAdded(true);

        resources[realNumFiles] = resource;
        realNumFiles++;
      }

      resources = resizeResources(resources, realNumFiles);

      setCanScanForFileTypes(true);

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

    if (headerInt1 == 1263421772) {
      return "link";
    }
    else if (headerInt1 == 1684558659) {
      return "cs";
    }
    else if (headerInt1 == -1174339584) {
      return "mpeg";
    }
    else if (headerInt1 == 0) {
      return "empty";
    }

    return null;
  }

}
