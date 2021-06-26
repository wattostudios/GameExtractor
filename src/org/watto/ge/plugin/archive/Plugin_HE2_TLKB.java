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
import org.watto.ge.plugin.exporter.Exporter_Custom_WAV_RawAudio_XOR;
import org.watto.ge.plugin.exporter.Exporter_Default;
import org.watto.ge.plugin.exporter.Exporter_XOR;
import org.watto.ge.plugin.resource.Resource_WAV_RawAudio;
import org.watto.io.FileManipulator;
import org.watto.io.buffer.XORBufferWrapper;
import org.watto.io.converter.IntConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_HE2_TLKB extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_HE2_TLKB() {

    super("HE2_TLKB", "HE2_TLKB");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Putt-Putt and Fatty Bears Activity Pack",
        "Putt-Putt and Peps Balloon-o-Rama",
        "Putt-Putt and Peps Dog on a Stick",
        "Spy Fox In Cheese Chase");
    setExtensions("he2", "he4"); // MUST BE LOWER CASE
    setPlatforms("PC");

    // MUST BE LOWER CASE !!!
    setFileTypes(new FileType("talk", "Talk Voice Audio", FileType.TYPE_AUDIO),
        new FileType("digi", "DIGI Audio", FileType.TYPE_AUDIO));

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
      String header = fm.readString(4);
      if (header.equals("TLKB") || header.equals("SONG")) {
        rating += 50;
      }
      else if (IntConverter.convertBig(header.getBytes()) == 1025843755) {
        // TLKB, with XOR encryption
        rating += 50;
        return rating;
      }

      long arcSize = fm.getLength();

      // Archive Size
      if (FieldValidator.checkEquals(IntConverter.changeFormat(fm.readInt()), arcSize)) {
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

      ExporterPlugin exporter = Exporter_Default.getInstance();
      ExporterPlugin exporterWAV = null;

      // RESETTING GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false, 8); // small quick reads

      long arcSize = fm.getLength();

      // 4 - Header (TLKB)
      int header = IntConverter.changeFormat(fm.readInt());

      if (header == 1025843755) {
        // TLKB with XOR(105) encryption
        fm.setBuffer(new XORBufferWrapper(fm.getBuffer(), 105));
        exporter = new Exporter_XOR(105);
        exporterWAV = new Exporter_Custom_WAV_RawAudio_XOR(105);
      }

      // 4 - Archive Length
      fm.skip(4);

      int numFiles = Archive.getMaxFiles();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(arcSize);

      // Loop through directory
      int realNumFiles = 0;
      while (fm.getOffset() < arcSize) {
        // 4 - File Type
        String fileType = fm.readString(4);
        System.out.println(fm.getOffset() + "\t" + fileType);

        // 4 - File Length (including these 2 fields)
        int length = IntConverter.changeFormat(fm.readInt()) - 8;
        FieldValidator.checkLength(length, arcSize);

        // X - File Data
        long offset = fm.getOffset();
        fm.skip(length);

        String filename = Resource.generateFilename(realNumFiles) + "." + fileType;

        if (fileType.equals("TALK") || fileType.equals("DIGI")) {
          offset += 32;
          length -= 32;

          //path,name,offset,length,decompLength,exporter
          Resource_WAV_RawAudio resource = new Resource_WAV_RawAudio(path, filename, offset, length, length, exporterWAV);
          resource.setAudioProperties(11025, (short) 8, (short) 1);
          resources[realNumFiles] = resource;
        }
        else {
          //path,name,offset,length,decompLength,exporter
          resources[realNumFiles] = new Resource(path, filename, offset, length, length, exporter);
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
