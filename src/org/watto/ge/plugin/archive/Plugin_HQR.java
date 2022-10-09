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
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.exporter.Exporter_Custom_HQR_VOX;
import org.watto.ge.plugin.exporter.Exporter_Default;
import org.watto.ge.plugin.exporter.Exporter_LBALZSS1;
import org.watto.ge.plugin.exporter.Exporter_LBALZSS2;
import org.watto.io.FileManipulator;
import org.watto.io.FilenameSplitter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_HQR extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_HQR() {

    super("HQR", "HQR");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Little Big Adventure 2");
    setExtensions("vox", "hqr", "ile", "obl"); // MUST BE LOWER CASE
    setPlatforms("PC");

    // MUST BE LOWER CASE !!!
    setFileTypes(new FileType("hqr_tex", "Texture Image", FileType.TYPE_IMAGE),
        new FileType("hqr_texpal", "Texture Image with Palette", FileType.TYPE_IMAGE),
        new FileType("hqr_pal", "Color Palette", FileType.TYPE_PALETTE));

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
      }

      long arcSize = fm.getLength();

      // First File Offset
      if (FieldValidator.checkOffset(fm.readInt(), arcSize)) {
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

      ExporterPlugin exporterDefault = Exporter_Default.getInstance();
      ExporterPlugin exporterVOX = Exporter_Custom_HQR_VOX.getInstance();
      ExporterPlugin exporterLBALZSS1 = Exporter_LBALZSS1.getInstance();
      ExporterPlugin exporterLBALZSS2 = Exporter_LBALZSS2.getInstance();

      // RESETTING GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      int numFiles = Archive.getMaxFiles();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(arcSize);

      // Loop through directory
      int realNumFiles = 0;
      long[] offsets = new long[numFiles];
      while (fm.getOffset() < arcSize) {
        // 4 - File Offset
        int offset = fm.readInt();
        if (offset == 0) {
          continue;
        }
        else if (offset == arcSize) {
          // end of directory
          break;
        }
        FieldValidator.checkOffset(offset, arcSize);
        offsets[realNumFiles] = offset;
        realNumFiles++;
      }

      fm.getBuffer().setBufferSize(8);

      fm.seek(1);

      boolean isVOX = FilenameSplitter.getExtension(path).equalsIgnoreCase("vox");

      for (int i = 0; i < realNumFiles; i++) {
        fm.seek(offsets[i]);

        // 4 - Decompressed File Length
        int decompLength = fm.readInt();

        // 4 - Compressed File Length
        int length = fm.readInt();

        try {
          // 1 file in EN_004.VOX
          FieldValidator.checkLength(decompLength);
          FieldValidator.checkLength(length, arcSize);
        }
        catch (Throwable t) {
          length = 0;
          decompLength = 0;
        }

        // 2 - null
        int compressionType = fm.readShort();

        // X - File Data
        long offset = fm.getOffset();

        String filename = Resource.generateFilename(i);

        ExporterPlugin exporter = exporterDefault;
        if (isVOX && length != 0) {
          // X - WAVE audio file (except the first byte is 0 instead of "R")
          exporter = exporterVOX;
          filename += ".wav";
        }
        else if (compressionType == 1) {
          exporter = exporterLBALZSS1;
        }
        else if (compressionType == 2) {
          exporter = exporterLBALZSS2;
        }

        if (decompLength == 768) {
          filename += ".hqr_pal";
        }
        else if (decompLength == 307200 || decompLength == 65536) {
          filename += ".hqr_tex";
        }
        else if (decompLength == 131884) {
          filename += ".hqr_texpal";
        }

        //path,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, filename, offset, length, decompLength, exporter);

        if (decompLength == 768 && i != 0) {
          resources[i].addProperty("PaletteID", i - 1);
        }

        TaskProgressManager.setValue(i);
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

    String extension = resource.getExtension();
    if (extension != null && !extension.equals("")) {
      return extension;
    }

    if (headerInt1 == 1179011410) {
      return "wav";
    }

    return null;
  }

}
