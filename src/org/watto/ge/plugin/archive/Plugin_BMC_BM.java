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
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.exporter.BlockVariableExporterWrapper;
import org.watto.ge.plugin.exporter.Exporter_Default;
import org.watto.ge.plugin.exporter.Exporter_Explode;
import org.watto.io.FileManipulator;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_BMC_BM extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_BMC_BM() {

    super("BMC_BM", "BMC_BM");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Thomas and Friends: Thomas Saves the Day");
    setExtensions("bmc"); // MUST BE LOWER CASE
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

      // Header
      if (fm.readString(2).equals("BM")) {
        rating += 50;
      }

      fm.skip(52);

      long arcSize = fm.getLength();

      if (fm.readInt() + 58 == arcSize) {
        rating += 25;
      }
      else {
        rating = 0; // don't want to pick up normal BMP images
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

      // 2 - Header (BM)
      if (!fm.readString(2).equals("BM")) {
        return null;
      }

      // 4 - File Length
      int decompLength = fm.readInt();
      FieldValidator.checkLength(decompLength);

      // 2 - Reserved (null)
      // 2 - Reserved (null)
      fm.skip(4);

      // 4 - Pixel Data Offset (54)
      int pixelOffset = fm.readInt();
      FieldValidator.checkOffset(pixelOffset);

      // 4 - Header Size (40)
      fm.skip(4);

      // 4 - Image Width
      int width = fm.readInt();
      FieldValidator.checkWidth(width);

      // 4 - Image Height
      int height = fm.readInt();
      FieldValidator.checkHeight(height);

      // 2 - Number of Color Planes (1)
      FieldValidator.checkEquals(fm.readShort(), 1);

      // 2 - Bits per Pixel
      fm.skip(2);

      // 4 - Compression
      FieldValidator.checkEquals(fm.readInt(), 0);

      // 4 - Pixel Data Length
      FieldValidator.checkLength(fm.readInt());

      // 4 - Horizontal Resolution
      // 4 - Vertical Resolution
      // 4 - Number of Colors in the Palette
      // 4 - Number of Important Colors
      // X - Pixel Data (RGBA)
      fm.relativeSeek(pixelOffset);

      // 4 - Compressed Data Length
      int length = fm.readInt();
      FieldValidator.checkEquals(length + pixelOffset + 4, arcSize);

      Resource[] resources = new Resource[1];
      TaskProgressManager.setMaximum(1);

      String filename = path.getName() + ".bmp";

      // build an exporter that will join the first 54 bytes of the BMC file, and then the decompressed pixel data
      ExporterPlugin[] blockExporters = new ExporterPlugin[] { Exporter_Default.getInstance(), Exporter_Explode.getInstance() };
      long[] blockOffsets = new long[] { 0, 58 };
      long[] blockLengths = new long[] { 54, length };
      long[] blockDecompLengths = new long[] { 54, decompLength - 54 };
      BlockVariableExporterWrapper blockExporter = new BlockVariableExporterWrapper(blockExporters, blockOffsets, blockLengths, blockDecompLengths);

      //path,name,offset,length,decompLength,exporter
      resources[0] = new Resource(path, filename, 0, length, decompLength, blockExporter);

      TaskProgressManager.setValue(1);

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
