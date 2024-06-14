/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2023 wattostudios
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
import org.watto.ge.plugin.exporter.Exporter_ZLib;
import org.watto.io.FileManipulator;
import org.watto.io.buffer.ByteBuffer;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_RGF_RATALAIKA extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_RGF_RATALAIKA() {

    super("RGF_RATALAIKA", "RGF_RATALAIKA");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("League of Evil");
    setExtensions("rgf"); // MUST BE LOWER CASE
    setPlatforms("PC");

    // MUST BE LOWER CASE !!!
    //setFileTypes(new FileType("txt", "Text Document", FileType.TYPE_DOCUMENT),
    //             new FileType("bmp", "Bitmap Image", FileType.TYPE_IMAGE)
    //             );

    setTextPreviewExtensions("py", "fnt"); // LOWER CASE

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
      if (fm.readString(15).equals("Ratalaika Games")) {
        rating += 50;
      }

      fm.skip(17);

      long arcSize = fm.getLength();

      // Directory Length
      if (FieldValidator.checkLength(fm.readInt(), arcSize)) {
        rating += 5;
      }

      fm.skip(4);

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

      // 32 - Header ("Ratalaika Games" + (bytes)0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,0,0))
      fm.skip(32);

      // 8 - File Data Length
      int dirOffset = fm.readInt() + 48;
      FieldValidator.checkOffset(dirOffset, arcSize);

      fm.skip(4);

      // 4 - Number of Files?
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // 4 - Unknown (512)
      fm.seek(dirOffset);

      // 4 - Compressed Directory Length
      int dirLength = fm.readInt();
      FieldValidator.checkLength(dirLength, arcSize);

      // 4 - Decompressed Directory Length
      int dirDecompLength = fm.readInt();
      FieldValidator.checkLength(dirDecompLength);

      // X - Compressed Directory Data (ZLib)
      byte[] dirBytes = new byte[dirDecompLength];
      int decompWritePos = 0;
      Exporter_ZLib exporter = Exporter_ZLib.getInstance();
      exporter.open(fm, dirLength, dirDecompLength);

      for (int b = 0; b < dirDecompLength; b++) {
        if (exporter.available()) { // make sure we read the next bit of data, if required
          dirBytes[decompWritePos++] = (byte) exporter.read();
        }
      }

      // open the decompressed data for processing
      fm.close();
      fm = new FileManipulator(new ByteBuffer(dirBytes));

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {
        // X - Filename
        // 1 - null Filename Terminator
        String filename = fm.readNullString();
        FieldValidator.checkFilename(filename);

        // 4 - Compressed Length (not including Padding)
        int length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - Compressed Length (including Padding)
        fm.skip(4);

        // 4 - Decompressed Length
        int decompLength = fm.readInt();
        FieldValidator.checkLength(decompLength);

        // 4 - Compression Flag? (1=Compressed)
        fm.skip(4);

        // 4 - File Data Offset (relative to the start of the file data)
        int offset = fm.readInt() + 48;
        FieldValidator.checkOffset(offset, arcSize);

        //path,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, filename, offset, length, decompLength, exporter);

        TaskProgressManager.setValue(i);

        if (fm.getOffset() >= fm.getLength()) {
          // early exit
          resources = resizeResources(resources, i + 1);
          break;
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

    /*
    if (headerInt1 == 2037149520) {
      return "js";
    }
    */

    return null;
  }

}
