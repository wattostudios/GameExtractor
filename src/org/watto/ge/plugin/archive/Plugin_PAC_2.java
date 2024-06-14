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
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.exporter.Exporter_Custom_VAG_Audio;
import org.watto.io.FileManipulator;
import org.watto.io.converter.ShortConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_PAC_2 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_PAC_2() {

    super("PAC_2", "PAC_2");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Summon Night 3");
    setExtensions("pac", "dat_arc", "dat_texarc"); // MUST BE LOWER CASE
    setPlatforms("PS2");

    // MUST BE LOWER CASE !!!
    setFileTypes(new FileType("pac", "PAC Archive", FileType.TYPE_ARCHIVE),
        new FileType("pac_tex", "Texture Images", FileType.TYPE_IMAGE),
        new FileType("rwi", "RWI Image", FileType.TYPE_IMAGE),
        new FileType("vag", "VAG Audio", FileType.TYPE_AUDIO),
        new FileType("tex", "TEX Image", FileType.TYPE_IMAGE));

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

      // 2 - Archive Type (0/1)
      short arcType = fm.readShort();

      // 4 - Unknown (11/4)
      int secondHeader = fm.readInt();

      if (arcType == 0 && secondHeader == 11) {
        rating += 35; // not 25, as we need this to rate higher than the RMX plugin, for some archives
      }
      else if (arcType == 1 && (secondHeader == 4 || secondHeader == 5 || secondHeader == 11)) {
        rating += 35; // not 25, as we need this to rate higher than the RMX plugin, for some archives
      }
      else {
        rating = 0;
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

      // 2 - Number of Entries
      short numFiles = fm.readShort();
      FieldValidator.checkNumFiles(numFiles);

      // 2 - Archive Type (0/1)
      int arcType = fm.readShort();

      // 4 - Unknown (11/4)
      int secondHeader = fm.readInt();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      if (arcType == 0 && secondHeader == 11) {
        // type 0

        for (int i = 0; i < numFiles; i++) {
          // 2 - Offset [*2048]
          int offset = ShortConverter.unsign(fm.readShort()) * 2048;
          FieldValidator.checkOffset(offset, arcSize);

          // 2 - Length [*2048]
          int length = ShortConverter.unsign(fm.readShort()) * 2048;
          FieldValidator.checkLength(length, arcSize);

          String filename = Resource.generateFilename(i);

          //path,name,offset,length,decompLength,exporter
          resources[i] = new Resource(path, filename, offset, length);

          TaskProgressManager.setValue(i);
        }
      }
      else if (arcType == 1 && secondHeader == 4) {
        // type 1 (16 size)

        for (int i = 0; i < numFiles; i++) {
          // 4 - Offset [*16]
          int offset = fm.readInt() * 16;
          FieldValidator.checkOffset(offset, arcSize);

          // 4 - Length [*16]
          int length = fm.readInt() * 16;
          FieldValidator.checkLength(length, arcSize);

          String filename = Resource.generateFilename(i);

          //path,name,offset,length,decompLength,exporter
          resources[i] = new Resource(path, filename, offset, length);

          TaskProgressManager.setValue(i);
        }
      }
      else if (arcType == 1 && secondHeader == 5) {
        // type 1 (32 size)

        for (int i = 0; i < numFiles; i++) {
          // 4 - Offset [*32]
          int offset = fm.readInt() * 32;
          FieldValidator.checkOffset(offset, arcSize);

          // 4 - Length [*32]
          int length = fm.readInt() * 32;
          FieldValidator.checkLength(length, arcSize);

          String filename = Resource.generateFilename(i);

          //path,name,offset,length,decompLength,exporter
          resources[i] = new Resource(path, filename, offset, length);

          TaskProgressManager.setValue(i);
        }
      }
      else if (arcType == 1 && secondHeader == 11) {
        // type 1 (2048 size)

        for (int i = 0; i < numFiles; i++) {
          // 4 - Offset [*2048]
          int offset = fm.readInt() * 2048;
          FieldValidator.checkOffset(offset, arcSize + 1);

          // 4 - Length [*2048]
          int length = fm.readInt() * 2048;
          FieldValidator.checkLength(length, arcSize);

          String filename = Resource.generateFilename(i);

          //path,name,offset,length,decompLength,exporter
          resources[i] = new Resource(path, filename, offset, length);

          TaskProgressManager.setValue(i);
        }
      }
      else {
        // something we don't handle
        return null;
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

    if (headerInt1 == 0 && headerInt2 == 0 && headerInt3 == 0 && resource.getLength() > 1000) {

      ExporterPlugin exporter = Exporter_Custom_VAG_Audio.getInstance();
      resource.setExporter(exporter);

      return "vag";
    }
    else if (headerShort2 == 0 && headerInt2 == 11 && headerBytes[8] == 1 && headerBytes[9] == 0) {
      return "pac"; // TYPE 0
    }
    else if (headerShort2 == 1 && (headerInt2 == 4 || headerInt2 == 11)) {
      return "pac"; // TYPE 1
    }
    else if (headerShort1 == 0 && (headerInt2 != 1 && headerInt2 != 0 && headerShort2 != 0) && resource.getLength() > 1000) {
      return "pac_tex";
    }
    else if (headerShort1 == 1 && headerShort2 == 1 && resource.getLength() > 1000) {
      return "pac_tex";
    }
    else if ((headerInt1 == 808464432 || headerInt1 == 0) && headerInt2 == 1026) {
      return "pac_tex";
    }
    else if (headerInt1 == 4998221) {
      return "mdl";
    }
    else if (headerInt1 == 541677394) {
      return "rwi";
    }
    else if (headerInt1 == 542133825) {
      return "anp";
    }
    else if (headerInt1 == 1953719668) {
      return "test";
    }
    else if (headerInt1 == 1396917577) {
      return "iecs";
    }
    else if (headerInt1 == 541345869) {
      return "mhd";
    }
    else if (headerInt1 == 7890292) {
      return "tex";
    }
    else if (headerInt1 == 5523778) {
      return "bit";
    }

    return null;
  }

}
