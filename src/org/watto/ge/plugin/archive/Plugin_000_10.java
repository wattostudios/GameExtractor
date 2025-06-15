/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2025 wattostudios
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

import org.watto.component.WSPluginManager;
import org.watto.datatype.Archive;
import org.watto.datatype.FileType;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.ge.plugin.exporter.Exporter_Explode;
import org.watto.io.FileManipulator;
import org.watto.io.converter.ByteConverter;
import org.watto.io.converter.ShortConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_000_10 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_000_10() {

    super("000_10", "Sierra SCI 1.1 Game Engine");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("EcoQuest: The Search for Cetus",
        "EcoQuest 2: Lost Secret of the Rainforest");
    setExtensions("000", "msg"); // MUST BE LOWER CASE
    setPlatforms("PC");

    // MUST BE LOWER CASE !!!
    setFileTypes(new FileType("v56", "256 Color View", FileType.TYPE_IMAGE),
        new FileType("p56", "256 Color Background Picture", FileType.TYPE_IMAGE),
        new FileType("scr", "Script", FileType.TYPE_OTHER),
        new FileType("tex", "Text", FileType.TYPE_DOCUMENT),
        new FileType("snd", "Midi Sound", FileType.TYPE_AUDIO),
        new FileType("133", "Memory", FileType.TYPE_OTHER),
        new FileType("voc", "Vocabulary", FileType.TYPE_OTHER),
        new FileType("fon", "Font", FileType.TYPE_OTHER),
        new FileType("cur", "Cursor", FileType.TYPE_OTHER),
        new FileType("pat", "Audio Patch", FileType.TYPE_OTHER),
        new FileType("bit", "Bitmap", FileType.TYPE_OTHER),
        new FileType("pal", "Color Palette", FileType.TYPE_PALETTE),
        new FileType("cda", "CD Audio", FileType.TYPE_AUDIO),
        new FileType("aud", "Audio", FileType.TYPE_AUDIO),
        new FileType("syn", "Audio Sync", FileType.TYPE_OTHER),
        new FileType("msg", "Message Text", FileType.TYPE_DOCUMENT),
        new FileType("hep", "Heap", FileType.TYPE_OTHER));

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

      String filename = fm.getFile().getName();
      if (filename.equalsIgnoreCase("RESOURCE.000") || filename.equalsIgnoreCase("RESOURCE.MSG")) {
        rating += 25;
      }

      fm.skip(3);

      long arcSize = fm.getLength();

      // Compressed File Length
      int compLength = ShortConverter.unsign(fm.readShort());
      if (FieldValidator.checkLength(compLength, arcSize)) {
        rating += 5;
      }

      // Decompressed File Length
      int decompLength = ShortConverter.unsign(fm.readShort());
      if (FieldValidator.checkLength(decompLength) && decompLength >= compLength) {
        rating += 5;
      }

      // Compression
      short compression = fm.readShort();
      if (compression == 0 || compression == 18 || compression == 19 || compression == 20) {
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

      ExporterPlugin exporter = Exporter_Explode.getInstance();

      // RESETTING GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      int numFiles = Archive.getMaxFiles();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(arcSize);

      // Loop through directory
      int realNumFiles = 0;
      while (fm.getOffset() < arcSize) {
        // 1 - Resource Type
        int type = ByteConverter.unsign(fm.readByte());
        String extension = "" + type;

        if (type == 128) {
          extension = "V56";
        }
        else if (type == 129) {
          extension = "P56";
        }
        else if (type == 130) {
          extension = "SCR";
        }
        else if (type == 131) {
          extension = "TEX";
        }
        else if (type == 132) {
          extension = "SND";
        }
        else if (type == 133) {
          extension = "133";
        }
        else if (type == 134) {
          extension = "VOC";
        }
        else if (type == 135) {
          extension = "FON";
        }
        else if (type == 136) {
          extension = "CUR";
        }
        else if (type == 137) {
          extension = "PAT";
        }
        else if (type == 138) {
          extension = "BIT";
        }
        else if (type == 139) {
          extension = "PAL";
        }
        else if (type == 140) {
          extension = "CDA";
        }
        else if (type == 141) {
          extension = "AUD";
        }
        else if (type == 142) {
          extension = "SYN";
        }
        else if (type == 143) {
          extension = "MSG";
        }
        else if (type == 144) {
          extension = "MAP";
        }
        else if (type == 145) {
          extension = "HEP";
        }

        // 2 - Resource Number
        int resourceNumber = ShortConverter.unsign(fm.readShort());

        // 2 - Compressed File Length
        int length = ShortConverter.unsign(fm.readShort());
        FieldValidator.checkLength(length, arcSize);

        // 2 - Decompressed File Length
        int decompLength = ShortConverter.unsign(fm.readShort());
        FieldValidator.checkLength(decompLength);

        if (decompLength < length) {
          return null; // not this type of archive, maybe a different type of SCI archive?
        }

        // 2 - Compression Method
        short compression = fm.readShort();

        long offset = fm.getOffset();
        fm.skip(length);

        // 0-1 - Padding to a multiple of 2 bytes
        if ((offset + length) % 2 == 1) {
          fm.skip(1);
        }

        String filename = resourceNumber + "." + extension;

        //path,name,offset,length,decompLength,exporter
        if (compression == 18 || compression == 19 || compression == 20) {
          //DCL-EXPLODE compression
          resources[realNumFiles] = new Resource(path, filename, offset, length, decompLength, exporter);
        }
        else {
          //No compression
          resources[realNumFiles] = new Resource(path, filename, offset, length, decompLength);
        }

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
   Provide hints to the previewer so that certain document types are displayed appropriately
   **********************************************************************************************
   **/
  @Override
  public ViewerPlugin previewHint(Resource resource) {
    String extension = resource.getExtension();
    if (extension.equalsIgnoreCase("aud")) {
      return (ViewerPlugin) WSPluginManager.getPlugin("Viewer", "FFMPEG_Audio_WAV");
    }
    return null;
  }

}
