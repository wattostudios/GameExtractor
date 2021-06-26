
package org.watto.ge.plugin.archive;

import java.io.File;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.exporter.Exporter_Custom_WAV_RawAudio_Compressed;
import org.watto.ge.plugin.exporter.Exporter_LZSS;
import org.watto.ge.plugin.resource.Resource_WAV_RawAudio;
////////////////////////////////////////////////////////////////////////////////////////////////
//                                                                                            //
//                                       GAME EXTRACTOR                                       //
//                               Extensible Game Archive Editor                               //
//                                http://www.watto.org/extract                                //
//                                                                                            //
//                           Copyright (C) 2002-2009  WATTO Studios                           //
//                                                                                            //
// This program is free software; you can redistribute it and/or modify it under the terms of //
// the GNU General Public License published by the Free Software Foundation; either version 2 //
// of the License, or (at your option) any later versions. This program is distributed in the //
// hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranties //
// of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License //
// at http://www.gnu.org for more details. For updates and information about this program, go //
// to the WATTO Studios website at http://www.watto.org or email watto@watto.org . Thanks! :) //
//                                                                                            //
////////////////////////////////////////////////////////////////////////////////////////////////
import org.watto.io.FileManipulator;
import org.watto.io.converter.ByteConverter;
import org.watto.io.converter.IntConverter;
import org.watto.io.converter.ShortConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_DAT_SLHALL extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_DAT_SLHALL() {

    super("DAT_SLHALL", "DAT_SLHALL");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Temporal",
        "Wizball");
    setExtensions("dat"); // MUST BE LOWER CASE
    setPlatforms("PC");

    //setFileTypes("","",
    //             "",""
    //             );

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
      if (fm.readString(8).equals("slh.ALL.")) {
        rating += 50;
      }

      // Number Of Files
      if (FieldValidator.checkNumFiles(IntConverter.changeFormat(fm.readInt()))) {
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

      ExporterPlugin exporter = Exporter_LZSS.getInstance();
      Exporter_Custom_WAV_RawAudio_Compressed exporterWave = new Exporter_Custom_WAV_RawAudio_Compressed(exporter);
      exporterWave.setSkipAmount(6);

      // RESETTING GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 8 - Header (slh.ALL.)
      fm.skip(8);

      // 4 - Number Of Files
      int numFiles = IntConverter.changeFormat(fm.readInt()) - 1;
      FieldValidator.checkNumFiles(numFiles);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {
        //System.out.println(fm.getOffset());
        String filename = Resource.generateFilename(i);

        // 4 - Field Type
        String fieldType = fm.readString(4);
        while (fieldType.equals("prop")) {
          // process a property

          // 4 - Property Type
          String propertyType = fm.readString(4);

          // 4 - Property Length
          int propertyLength = IntConverter.changeFormat(fm.readInt());
          FieldValidator.checkLength(propertyLength, arcSize);

          if (propertyType.equals("NAME")) {
            // X - Filename (no extension)
            filename = fm.readString(propertyLength);
          }
          else {
            // something else - skip the property data
            fm.skip(propertyLength);
          }

          // read the next property
          fieldType = fm.readString(4);
        }

        // if we've read something other than "prop", then it's the actual file data
        filename = filename.replaceAll("_", ".");

        // 4 - Compressed File Length
        int length = IntConverter.changeFormat(fm.readInt());
        FieldValidator.checkLength(length, arcSize);

        // 4 - Decompressed File Length
        byte[] decompBytes = fm.readBytes(4);
        if (ByteConverter.unsign(decompBytes[0]) == 255) {
          decompBytes[0] ^= 255;
          decompBytes[1] ^= 255;
          decompBytes[2] ^= 255;
          decompBytes[3] ^= 255;
        }
        int decompLength = IntConverter.convertBig(decompBytes);
        FieldValidator.checkLength(decompLength);

        if (fieldType.equals("SAMP")) {
          // Audio File (WAV)
          long offset = fm.getOffset();

          // 1 - Compression Flag (255)
          fm.skip(1);

          // 2 - Bitrate
          short bitrate = ShortConverter.changeFormat(fm.readShort());

          // 2 - Frequency
          short frequency = ShortConverter.changeFormat(fm.readShort());

          fm.skip(length - 5);

          if (length == decompLength) {
            //path,name,offset,length,decompLength,exporter
            Resource_WAV_RawAudio resource = new Resource_WAV_RawAudio(path, filename, offset, length);
            resource.setAudioProperties(frequency, bitrate, (short) 1, false);
            resources[i] = resource;
          }
          else {
            //path,name,offset,length,decompLength,exporter
            Resource_WAV_RawAudio resource = new Resource_WAV_RawAudio(path, filename, offset, length, decompLength);
            resource.setAudioProperties(frequency, bitrate, (short) 1, false);
            resource.setExporter(exporterWave);
            resources[i] = resource;
          }

        }
        else {

          // X - File Data
          long offset = fm.getOffset();
          fm.skip(length);

          if (length == decompLength) {
            //path,name,offset,length,decompLength,exporter
            resources[i] = new Resource(path, filename, offset, length);
          }
          else {
            //path,name,offset,length,decompLength,exporter
            resources[i] = new Resource(path, filename, offset, length, decompLength, exporter);
          }
        }

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

}
