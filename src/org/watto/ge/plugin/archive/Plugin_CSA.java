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

import org.watto.datatype.FileType;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.io.FileManipulator;
import org.watto.io.buffer.ByteBuffer;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_CSA extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_CSA() {

    super("CSA", "CSA");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Star Stable Online");
    setExtensions("csa"); // MUST BE LOWER CASE
    setPlatforms("PC");

    setFileTypes(new FileType("pte", "PTE Texture", FileType.TYPE_IMAGE),
        new FileType("pme", "PME Mesh", FileType.TYPE_MODEL),
        new FileType("bank", "Audio Bank", FileType.TYPE_ARCHIVE));

    setTextPreviewExtensions("glsl"); // LOWER CASE

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

      getDirectoryFile(fm.getFile(), "csaheader");
      rating += 25;

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

      File sourcePath = getDirectoryFile(path, "csaheader");
      FileManipulator fm = new FileManipulator(sourcePath, false);

      int thisSize = (int) sourcePath.length();

      // 4 - Number of Entries [/8]
      // 4 - Unknown (4)
      // 96 - Header Entry
      // 4 - Unknown
      fm.relativeSeek(108);

      // 4 - Number Of Files (including this entry)
      int numFiles = fm.readInt() - 1;
      FieldValidator.checkNumFiles(numFiles);

      // 88 - rest of directory details entry
      fm.skip(88);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      int[] nameOffsets = new int[numFiles];
      for (int i = 0; i < numFiles; i++) {
        // 4 - Unknown (0)
        // 4 - Entry Type? (7=File Entry, -1 = Header Entry, anything else is the Number of Files)
        // 4 - Unknown (256)
        // 4 - Unknown (191822180)
        fm.skip(16);

        // 4 - File Offset
        long offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - Unknown (2562)
        // 4 - Unknown (186281480)
        fm.skip(8);

        // 4 - File Length
        long length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - Unknown (2562)
        // 4 - Unknown (29776520)
        fm.skip(8);

        // 4 - Filename Offset (relative to the start of the Filename Directory) (points to the Filename Length field)
        int nameOffset = fm.readInt();
        FieldValidator.checkOffset(nameOffset, thisSize);
        nameOffsets[i] = nameOffset;

        // 4 - Unknown (516)
        // 4 - Unknown (211451933)
        // 4 - Unknown
        // 4 - Unknown (2562)
        // 4 - Unknown (152998117)
        // 4 - Unknown
        // 4 - Unknown (256)
        // 4 - Unknown (13856629)
        // 4 - Unknown
        // 4 - Unknown (2562)
        // 4 - Unknown (243916309)
        // 4 - Unknown
        // 4 - Unknown (2562)
        fm.skip(52);

        String filename = Resource.generateFilename(i);

        //path,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, filename, offset, length);

        TaskProgressManager.setValue(i);
      }

      byte[] nameBytes = fm.readBytes((int) (thisSize - fm.getOffset()));
      fm.close();
      fm = new FileManipulator(new ByteBuffer(nameBytes));

      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];

        fm.seek(nameOffsets[i]);

        // 4 - Filename Length
        int filenameLength = fm.readInt();
        FieldValidator.checkFilenameLength(filenameLength);

        // X - Filename
        // 1 - null Filename Terminator
        String filename = fm.readNullString(filenameLength);
        FieldValidator.checkFilename(filename);

        resource.setName(filename);
        resource.setOriginalName(filename);
        resource.forceNotAdded(true);
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
