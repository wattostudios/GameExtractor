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
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.exporter.Exporter_XOR;
import org.watto.io.FileManipulator;
import org.watto.io.buffer.XORBufferWrapper;
import org.watto.io.converter.IntConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_001_LECF extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_001_LECF() {

    super("001_LECF", "001_LECF");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Indiana Jones and the Fate of Atlantis");
    setExtensions("001"); // MUST BE LOWER CASE
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

      // Header (LECF when XOR'd with byte(105))
      if (fm.readInt() == 623651375) {
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

      ExporterPlugin exporter = new Exporter_XOR(105);

      // RESETTING GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      // The whole archive is XOR with byte(105)
      fm.setBuffer(new XORBufferWrapper(fm.getBuffer(), 105));

      long arcSize = fm.getLength();

      // 4 - Header (LECF)
      // 4 - Archive Length
      // 4 - Header (LOFF)
      fm.skip(12);

      // 4 - Directory Length (including these 2 header fields)
      int dirLength = IntConverter.changeFormat(fm.readInt()) - 10;
      FieldValidator.checkLength(dirLength, arcSize);

      // 2 - Unknown
      fm.skip(2);

      int numFiles = (dirLength / 5);
      FieldValidator.checkNumFiles(numFiles);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {
        // 4 - File Offset (LITTLE ENDIAN) (Points to the actual file data, NOT to the LFLF header)
        int offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 1 - Unknown (Maybe an ID number?)
        fm.skip(1);

        String filename = Resource.generateFilename(i) + ".block";

        //path,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, filename, offset, 0, 0, exporter); // set the XOR exporter as well

        TaskProgressManager.setValue(i);
      }

      // now calculate the lengths
      for (int i = 0; i < numFiles - 1; i++) {
        Resource resource = resources[i];
        long length = resources[i + 1].getOffset() - resource.getOffset() - 8; // -8 for the 8-byte header
        resource.setLength(length);
        resource.setDecompressedLength(length);
      }
      int lastLength = (int) (arcSize - resources[numFiles - 1].getOffset());
      resources[numFiles - 1].setLength(lastLength);
      resources[numFiles - 1].setDecompressedLength(lastLength);

      fm.close();

      return resources;

    }
    catch (Throwable t) {
      logError(t);
      return null;
    }
  }

}
