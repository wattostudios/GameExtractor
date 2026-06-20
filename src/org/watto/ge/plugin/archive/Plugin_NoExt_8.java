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

import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.exporter.Exporter_Custom_WAV_RawAudio;
import org.watto.ge.plugin.exporter.Exporter_LZSS;
import org.watto.io.FileManipulator;
import org.watto.io.converter.IntConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_NoExt_8 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_NoExt_8() {

    super("NoExt_8", "NoExt_8");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Ravenloft: Stone Prophet",
        "Ravenloft: Strahd's Possession");
    setExtensions(""); // MUST BE LOWER CASE
    setPlatforms("PC");

    // MUST BE LOWER CASE !!!
    //setFileTypes(new FileType("txt", "Text Document", FileType.TYPE_DOCUMENT),
    //             new FileType("bmp", "Bitmap Image", FileType.TYPE_IMAGE)
    //             );

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
      else {
        if (fm.getFile().getName().indexOf('.') < 0) { // no extension
          rating += 25;
        }
      }

      if (FieldValidator.checkNumFiles(fm.readInt())) {
        rating += 5;
      }

      // Number Of Files
      if (FieldValidator.checkNumFiles((fm.readInt() - 4) / 4)) {
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

      // 4 - Number of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {

        // 4 - Next File Offset
        int offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        String filename = Resource.generateFilename(i);

        //path,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, filename, offset);

        TaskProgressManager.setValue(i);

      }

      calculateFileSizes(resources, arcSize);

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
    if (headerInt1 == 1634038339  ) {
      return "voc";
    }
    */

    if (headerShort1 == 18501) {
      if ((headerBytes[2] == 5 || headerBytes[2] == 6) && headerBytes[3] == 5 && headerBytes[4] == 0) {
        int decompLength = IntConverter.convertLittle(new byte[] { headerBytes[6], headerBytes[7], headerBytes[8], headerBytes[9] });
        resource.setOffset(resource.getOffset() + 10);
        resource.setLength(resource.getLength() - 10);
        resource.setDecompressedLength(decompLength);
        resource.setExporter(Exporter_LZSS.getInstance());
        return "res_tex";
      }
      else if ((headerBytes[2] == 1) && headerBytes[3] == 8 && headerBytes[4] == 0) {
        int decompLength = IntConverter.convertLittle(new byte[] { headerBytes[5], headerBytes[6], headerBytes[7], headerBytes[8] });
        resource.setOffset(resource.getOffset() + 9);
        resource.setLength(resource.getLength() - 9);
        resource.setDecompressedLength(decompLength);
        resource.setExporter(Exporter_LZSS.getInstance());
        //return "res_tex";
      }
      else if ((headerBytes[2] == 3) && headerBytes[3] == 0 && headerBytes[4] == 0) {
        int decompLength = IntConverter.convertLittle(new byte[] { headerBytes[5], headerBytes[6], headerBytes[7], headerBytes[8] });
        resource.setOffset(resource.getOffset() + 9);
        resource.setLength(resource.getLength() - 9);
        resource.setDecompressedLength(decompLength);
        return "xmi";
      }
      else if ((headerBytes[2] == 4) && headerBytes[3] == 0 && headerBytes[4] == 0) {
        int decompLength = IntConverter.convertLittle(new byte[] { headerBytes[5], headerBytes[6], headerBytes[7], headerBytes[8] });
        resource.setOffset(resource.getOffset() + 9);
        resource.setLength(resource.getLength() - 9);
        resource.setDecompressedLength(decompLength);

        if (headerBytes[9] == 67 && headerBytes[10] == 114 && headerBytes[11] == 101) {
          return "voc";
        }
        else {

          // Game = Menzoberranzan
          resource.setExporter(Exporter_Custom_WAV_RawAudio.getInstance());
          resource.addProperty("AudioFrequency", 11025);
          resource.addProperty("AudioBitRate", 8);
          resource.addProperty("AudioChannels", 1);
          resource.addProperty("AudioSigned", "true");
          return "wav";

        }
      }
      else if ((headerBytes[2] == 6) && headerBytes[3] == 0 && headerBytes[4] == 0) {
        resource.setOffset(resource.getOffset() + 7);
        resource.setLength(resource.getLength() - 7);
        //return "xmi";
      }
      else if ((headerBytes[2] == 7) && headerBytes[3] == 0 && headerBytes[4] == 0) {
        resource.setOffset(resource.getOffset() + 7);
        resource.setLength(resource.getLength() - 7);
        //return "xmi";
      }
      else if (headerShort2 == -30718 && headerBytes[4] == 0) {
        resource.setOffset(resource.getOffset() + 7);
        resource.setLength(resource.getLength() - 7);
        //return "xmi";
      }

    }

    return null;
  }

}
