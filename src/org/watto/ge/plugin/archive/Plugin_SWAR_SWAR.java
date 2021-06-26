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
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.resource.Resource_WAV_RawAudio;
import org.watto.io.FileManipulator;
import org.watto.io.converter.ShortConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_SWAR_SWAR extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_SWAR_SWAR() {

    super("SWAR_SWAR", "Nintendo DS SWAR Sound Archive");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Nintendo DS",
        "Custom Robo Arena");
    setExtensions("swar"); // MUST BE LOWER CASE
    setPlatforms("NDS");

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
      if (fm.readString(4).equals("SWAR")) {
        rating += 50;
      }

      fm.skip(4);

      long arcSize = fm.getLength();

      // Archive Size
      if (FieldValidator.checkEquals(fm.readInt(), arcSize)) {
        rating += 5;
      }

      if (fm.readShort() == 16) {
        rating += 5;
      }

      fm.skip(42);

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

      // 4 - Header (SWAR)
      // 2 - Byte Order
      // 2 - Version
      // 4 - Archive Length
      // 2 - Header Size (16)
      // 2 - Number of Blocks (1)
      // 4 - Header (DATA)
      // 4 - Block Length
      // 32 - null
      fm.skip(56);

      // 4 - Number Of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {

        // 4 - File Offset
        int offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        String filename = Resource.generateFilename(i) + ".wav";

        //path,name,offset,length,decompLength,exporter
        Resource_WAV_RawAudio resource = new Resource_WAV_RawAudio(path, filename, offset);
        resources[i] = resource;

        TaskProgressManager.setValue(i);
      }

      calculateFileSizes(resources, arcSize);

      fm.getBuffer().setBufferSize(2);
      fm.seek(1);

      // set the codec
      for (int i = 0; i < numFiles; i++) {
        Resource_WAV_RawAudio resource = (Resource_WAV_RawAudio) resources[i];

        long offset = resource.getOffset();
        long length = resource.getLength();

        fm.seek(offset);

        // 1 - Codec (0=PCM8, 1=PCM16, 2=IMA-ADPCM)
        short codec = fm.readShort();

        // 1 - Loop Flag
        fm.skip(1);

        // 2 - Sample Rate
        int samples = ShortConverter.unsign(fm.readShort());

        // 2 - Time
        // 2 - Loop Offset [*4]
        fm.skip(4);

        // 4 - File Data Length [*4]
        //length = fm.readInt() * 4;
        fm.skip(4);

        offset += 12;
        length -= 12;

        int frequency = 32768;

        if (codec == 0) {
          resource.setAudioProperties(frequency, (short) 8, (short) 1);
        }
        else if (codec == 1) {
          resource.setAudioProperties(frequency, (short) 16, (short) 1);
        }
        else if (codec == 2) {
          resource.setAudioProperties(frequency, (short) 4, (short) 1);
          resource.setSamples(samples);
          resource.setCodec((short) 0x0011);
          offset += 4;
          length -= 4;
        }

        resource.setOffset(offset);
        resource.setLength(length);
        resource.setDecompressedLength(length);

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
