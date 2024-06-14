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

import org.watto.Language;
import org.watto.datatype.FileType;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.exporter.Exporter_Custom_VAG_Audio;
import org.watto.io.FileManipulator;
import org.watto.io.converter.ShortConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_DAT_104 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_DAT_104() {

    super("DAT_104", "DAT_104");

    //         read write replace rename
    setProperties(true, true, false, false);

    setGames("Summon Night 2", "Summon Night 3", "Black Matrix Cross");
    setExtensions("dat"); // MUST BE LOWER CASE
    setPlatforms("PS2", "PS1");

    // MUST BE LOWER CASE !!!
    setFileTypes(new FileType("pac", "PAC Archive", FileType.TYPE_ARCHIVE),
        new FileType("pac_tex", "Texture Images", FileType.TYPE_IMAGE),
        new FileType("dat_tex", "Texture Images", FileType.TYPE_IMAGE),
        new FileType("vag", "VAG Audio", FileType.TYPE_AUDIO));

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

      // Number Of Files
      if (FieldValidator.checkNumFiles(fm.readShort())) {
        rating += 5;
      }

      if (fm.readShort() == 0) {
        rating += 5;
      }

      if (fm.readInt() == 11) {
        rating += 5;
      }

      long arcSize = fm.getLength();

      // File Offset
      if (FieldValidator.checkOffset(fm.readShort() * 2048, arcSize)) {
        rating += 5;
      }

      // File Length
      if (FieldValidator.checkLength(fm.readShort() * 2048, arcSize)) {
        rating += 4;
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

      // 2 - Number of Files
      int numFiles = fm.readShort();
      FieldValidator.checkNumFiles(numFiles);

      // 2 - Unknown (0)
      // 4 - Unknown (11)
      fm.skip(6);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {

        // 2 - File Offset [*2048]
        int offset = ShortConverter.unsign(fm.readShort()) * 2048;
        FieldValidator.checkOffset(offset, arcSize);

        // 2 - File Length [*2048] (including file padding)
        int length = fm.readShort() * 2048;
        FieldValidator.checkLength(length, arcSize);

        String filename = Resource.generateFilename(i);

        //path,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, filename, offset, length);

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
   * Writes an [archive] File with the contents of the Resources
   **********************************************************************************************
   **/
  @Override
  public void write(Resource[] resources, File path) {
    try {

      FileManipulator fm = new FileManipulator(path, true);
      int numFiles = resources.length;
      TaskProgressManager.setMaximum(numFiles);

      // Calculations
      TaskProgressManager.setMessage(Language.get("Progress_PerformingCalculations"));

      // Write Header Data

      // 2 - Number of Files
      fm.writeShort(numFiles);

      // 2 - Unknown (0)
      fm.writeShort(0);

      // 4 - Unknown (11)
      fm.writeInt(11);

      // Write Directory
      TaskProgressManager.setMessage(Language.get("Progress_WritingDirectory"));
      long offset = 8 + (numFiles * 4);
      offset += calculatePadding(offset, 2048);
      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];
        long decompLength = resource.getDecompressedLength();
        decompLength += calculatePadding(decompLength, 2048);

        // 2 - File Offset [*2048]
        fm.writeShort((short) (offset / 2048));

        // 2 - File Length
        fm.writeShort((short) (decompLength / 2048));

        offset += decompLength;
      }

      // X - null Padding to a multiple of 2048 bytes
      int padding = calculatePadding(fm.getOffset(), 2048);
      for (int p = 0; p < padding; p++) {
        fm.writeByte(0);
      }

      // Write Files
      TaskProgressManager.setMessage(Language.get("Progress_WritingFiles"));
      for (int i = 0; i < resources.length; i++) {
        Resource resource = resources[i];

        // X - File Data
        write(resource, fm);

        // X - null Padding to a multiple of 2048 bytes
        padding = calculatePadding(resource.getDecompressedLength(), 2048);
        for (int p = 0; p < padding; p++) {
          fm.writeByte(0);
        }

        TaskProgressManager.setValue(i);
      }

      fm.close();

    }
    catch (Throwable t) {
      logError(t);
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

    if (headerShort2 == 0 && headerInt2 == 11 && headerBytes[8] == 1 && headerBytes[9] == 0) {
      return "pac"; // TYPE 0
    }
    else if (headerShort2 == 1 && (headerInt2 == 4 || headerInt2 == 5 || headerInt2 == 11)) {
      return "pac"; // TYPE 1
    }
    else if ((headerInt1 == 808464432 || headerInt1 == 0) && headerInt2 == 1026) {
      return "pac_tex";
    }
    else if (headerInt1 == 12 && headerInt2 == 0 && headerInt3 == 0 && resource.getLength() > 2048) {

      resource.addProperty("Frequency", "44100");

      ExporterPlugin exporter = Exporter_Custom_VAG_Audio.getInstance();
      resource.setExporter(exporter);

      return "vag";
    }
    else if (headerInt1 == 0 && headerInt2 == 0 && headerInt3 == 0 && resource.getLength() > 2048) {

      resource.addProperty("Frequency", "44100");

      ExporterPlugin exporter = Exporter_Custom_VAG_Audio.getInstance();
      resource.setExporter(exporter);

      return "vag";
    }
    else if (headerInt1 == 843666256) {
      return "psi2";
    }
    else if ((headerShort1 == 0 || headerShort1 == 1) && headerInt2 == 128) {
      return "dat_tex";
    }
    else if (headerInt1 == 3 && headerInt2 == 4) {
      return "dat_tex";
    }

    return null;
  }

}
