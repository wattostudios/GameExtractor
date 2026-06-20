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
import org.watto.datatype.Archive;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.exporter.Exporter_ZLib_CompressedSizeOnly;
import org.watto.io.FileManipulator;
import org.watto.io.buffer.ByteBuffer;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_DAT_ARTEMSKAKUNOV extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_DAT_ARTEMSKAKUNOV() {

    super("DAT_ARTEMSKAKUNOV", "DAT_ARTEMSKAKUNOV");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Little Bombers Returns");
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

      // Header
      if (fm.readString(30).equals("Artem Skakunov's resource file")) {
        rating += 50;
      }

      fm.skip(2);

      long arcSize = fm.getLength();

      // Directory Length
      if (FieldValidator.checkLength(fm.readShort(), arcSize)) {
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

      ExporterPlugin exporter = Exporter_ZLib_CompressedSizeOnly.getInstance();

      // RESETTING GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 32 - Header (Artem Skakunov's resource file + (char)13,10)
      fm.skip(32);

      // 2 - Compressed Directory Length
      int dirLength = fm.readShort();
      FieldValidator.checkLength(dirLength, arcSize);

      int relativeOffset = 32 + 2 + dirLength;

      // X - Compressed Directory (ZLib)
      //byte[] compDir = fm.readBytes(dirLength);
      byte[] decompDir = new byte[dirLength * 10]; // guess max
      Exporter_ZLib_CompressedSizeOnly exporterDir = Exporter_ZLib_CompressedSizeOnly.getInstance();
      exporterDir.open(fm, dirLength, dirLength);

      int outPos = 0;
      while (exporterDir.available()) {
        decompDir[outPos] = (byte) exporter.read();
        outPos++;
      }

      // shrink the array
      byte[] oldDecompDir = decompDir;
      decompDir = new byte[outPos];
      System.arraycopy(oldDecompDir, 0, decompDir, 0, outPos);

      // Force-write out the decompressed file to write it to disk, then change the buffer to read-only.
      exporterDir.close();

      fm.close();
      fm = new FileManipulator(new ByteBuffer(decompDir));

      int numFiles = Archive.getMaxFiles();
      int realNumFiles = 0;

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(outPos);

      // Loop through directory
      while (fm.getOffset() < outPos) {
        // X - File Data line
        String entry = fm.readLine();

        //System.out.println(entry);

        String[] split = entry.split(",");
        int splitLength = split.length;

        if (splitLength == 4) {
          // name,filename,offset,compressed_size
          try {
            String filename = split[1];

            int offset = Integer.parseInt(split[2]) + relativeOffset;
            FieldValidator.checkOffset(offset, arcSize);

            int length = Integer.parseInt(split[3]);
            FieldValidator.checkLength(length, arcSize);

            //path,name,offset,length,decompLength,exporter
            resources[realNumFiles] = new Resource(path, filename, offset, length, length, exporter);
            realNumFiles++;

            TaskProgressManager.setValue(fm.getOffset());
          }
          catch (Throwable t) {
          }
        }
        else if (splitLength == 9) {
          // name,filename,sprite_type,has_transparency,tile_height,frame_width,frame_height,offset,compressed_size
          try {
            String filename = split[1];

            int offset = Integer.parseInt(split[7]) + relativeOffset;
            FieldValidator.checkOffset(offset, arcSize);

            int length = Integer.parseInt(split[8]);
            FieldValidator.checkLength(length, arcSize);

            //path,name,offset,length,decompLength,exporter
            resources[realNumFiles] = new Resource(path, filename, offset, length, length, exporter);
            realNumFiles++;

            TaskProgressManager.setValue(fm.getOffset());
          }
          catch (Throwable t) {
          }
        }
        else {
          // unknown
          ErrorLogger.log("[DAT_ARTEMSKAKUNOV] Unknown line format: " + splitLength);
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
