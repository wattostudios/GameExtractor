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

import org.watto.Language;
import org.watto.datatype.Archive;
import org.watto.datatype.FileType;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.exporter.Exporter_XOR;
import org.watto.io.FileManipulator;
import org.watto.io.buffer.XORBufferWrapper;
import org.watto.io.converter.IntConverter;
import org.watto.io.converter.StringConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_A_LECF extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_A_LECF() {

    super("A_LECF", "A_LECF");

    //         read write replace rename
    setProperties(true, true, false, false);

    setGames("Fatty Bear's Birthday Surprise",
        "Moonbase Commander");
    setExtensions("(a)"); // MUST BE LOWER CASE
    setPlatforms("PC");

    // MUST BE LOWER CASE !!!
    setFileTypes(new FileType("flfl", "FLFL Archive", FileType.TYPE_ARCHIVE));

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

      // Header (LECF XOR'd)
      if (fm.readInt() == 791292965) {
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

      // WHOLE ARCHIVE IS XOR WITH (byte) 105
      fm.setBuffer(new XORBufferWrapper(fm.getBuffer(), 105));

      long arcSize = fm.getLength();

      // 4 - Header (LECF)
      // 4 - Archive Length
      fm.skip(8);

      int numFiles = Archive.getMaxFiles();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(arcSize);

      // Loop through directory
      int realNumFiles = 0;
      while (fm.getOffset() < arcSize) {

        // 4 - File Extension (Reversed String)
        String type = StringConverter.reverse(fm.readString(4));

        // 4 - File Length (including these 2 fields)
        int length = IntConverter.changeFormat(fm.readInt()) - 8;
        FieldValidator.checkLength(length, arcSize);

        // X - File Data
        long offset = fm.getOffset();
        fm.skip(length);

        String filename = Resource.generateFilename(realNumFiles) + "." + type;

        //path,name,offset,length,decompLength,exporter
        resources[realNumFiles] = new Resource(path, filename, offset, length, length, exporter);
        realNumFiles++;

        TaskProgressManager.setValue(offset);
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
   * Writes an [archive] File with the contents of the Resources
   **********************************************************************************************
   **/
  @Override
  public void write(Resource[] resources, File path) {
    try {

      FileManipulator fm = new FileManipulator(path, true);

      // WHOLE ARCHIVE IS XOR WITH (byte) 105
      fm.setBuffer(new XORBufferWrapper(fm.getBuffer(), 105));

      int numFiles = resources.length;
      TaskProgressManager.setMaximum(numFiles);

      // Calculations
      TaskProgressManager.setMessage(Language.get("Progress_PerformingCalculations"));

      long archiveSize = 8;
      for (int i = 0; i < numFiles; i++) {
        archiveSize += resources[i].getDecompressedLength() + 8;
      }

      // Write Header Data

      // 4 - Header (LECF)
      fm.writeString("LECF");

      // 4 - Archive Size
      fm.writeInt(IntConverter.changeFormat((int) archiveSize));

      // Write Files
      TaskProgressManager.setMessage(Language.get("Progress_WritingFiles"));
      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];

        // 4 - File Extension (Reversed String)
        String extension = resource.getExtension();
        if (extension == null) {
          extension = "    ";
        }
        int extensionLength = extension.length();
        if (extensionLength > 4) {
          extension = extension.substring(0, 4);
        }
        extension = StringConverter.reverse(extension);

        int extensionPadding = 4 - extensionLength;
        for (int p = 0; p < extensionPadding; p++) {
          extension += " ";
        }
        fm.writeString(extension);

        // 4 - File Length (including these 2 fields)
        long decompLength = resource.getDecompressedLength() + 8;
        fm.writeInt(IntConverter.changeFormat((int) decompLength));

        // X - File Data
        write(resource, fm);
        TaskProgressManager.setValue(i);
      }

      fm.close();

    }
    catch (Throwable t) {
      logError(t);
    }
  }

}
