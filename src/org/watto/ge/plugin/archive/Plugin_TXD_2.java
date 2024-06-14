/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2022 wattostudios
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
import org.watto.ErrorLogger;
import org.watto.Language;
import org.watto.Settings;
import org.watto.component.PreviewPanel;
import org.watto.datatype.FileType;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.viewer.Viewer_TXD_2_TXDTEX;
import org.watto.io.FileManipulator;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_TXD_2 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_TXD_2() {

    super("TXD_2", "TXD_2");

    //         read write replace rename
    setProperties(true, false, true, false);

    setGames("Kill Switch");
    setExtensions("txd"); // MUST BE LOWER CASE
    setPlatforms("PC");

    // MUST BE LOWER CASE !!!
    setFileTypes(new FileType("txd_tex", "Texture Image", FileType.TYPE_IMAGE));

    //setTextPreviewExtensions("colours", "rat", "screen", "styles"); // LOWER CASE

    //setCanScanForFileTypes(true);

    setCanConvertOnReplace(true);

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
      if (fm.readInt() == 22) {
        rating += 25;
      }

      fm.skip(8);

      if (fm.readInt() == 12) {
        rating += 5;
      }

      if (fm.readInt() == 4) {
        rating += 5;
      }

      fm.skip(4);

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

      // 4 - Header (22)
      // 4 - Chunk Size
      // 4 - Version
      // 4 - Chunk Type (12)
      // 4 - Chunk Size (4)
      // 4 - Version
      fm.skip(24);

      // 4 - Number of Images
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {
        long offset = fm.getOffset();

        // 4 - Chunk Type (21 = Raster)
        fm.skip(4);

        // 4 - Chunk Data Length
        int length = fm.readInt() + 12;
        FieldValidator.checkLength(length, arcSize);

        // 4 - Version
        // 4 - Chunk Type (1 = Struct)
        // 4 - Chunk Data Length
        // 4 - Version
        // 4 - Platform ID
        // 1 - Filter Mode
        // 1 - Addressing
        // 2 - Padding
        fm.skip(24);

        // 32 - Raster Name (null terminated, filled with nulls)
        String filename = fm.readNullString(32);
        FieldValidator.checkFilename(filename);
        filename += ".txd_tex";

        // 32 - Mask Name (null terminated, filled with nulls)

        // 4 - Alpha Flags
        // 4 - DirectX Image Format (or null)
        // 2 - Image Width
        // 2 - Image Height
        // 1 - Bits Per Pixel
        // 1 - Number of Mipmaps
        // 1 - Raster Type
        // 1 - Flags

        // if (bitsPerPixel <= 8){
        // 1024 - Color Palette (256*RGBA)
        // }

        // for each mipmap
        // 4 - Mipmap Pixel Data Length
        // X - Mipmap Pixel Data

        // 4 - Chunk Type (3 = Extension)
        // 4 - Chunk Size
        // 4 - Version
        // X - Chunk Extension Data
        fm.relativeSeek(offset + length);

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
   * Writes an [archive] File with the contents of the Resources. The archive is written using
   * data from the initial archive - it isn't written from scratch.
   **********************************************************************************************
   **/
  @Override
  public void replace(Resource[] resources, File path) {
    try {

      FileManipulator fm = new FileManipulator(path, true);
      FileManipulator src = new FileManipulator(new File(Settings.getString("CurrentArchive")), false);

      int numFiles = resources.length;
      TaskProgressManager.setMaximum(numFiles);

      // Write Header Data

      // 4 - Header (22)
      // 4 - Chunk Size
      // 4 - Version
      fm.writeBytes(src.readBytes(12));

      // CHUNK HEADER
      // 4 - Chunk Type (12)
      // 4 - Chunk Size (4)
      // 4 - Version
      // 4 - Number of Images
      fm.writeBytes(src.readBytes(16));

      // Write Files
      TaskProgressManager.setMessage(Language.get("Progress_WritingFiles"));
      for (int i = 0; i < resources.length; i++) {

        // 4 - Chunk Type (21 = Raster)
        src.skip(4);

        // 4 - Chunk Data Length
        int srcLength = src.readInt();

        // 4 - Version
        src.skip(4);

        // X - Data
        src.skip(srcLength);

        write(resources[i], fm);
        TaskProgressManager.setValue(i);
      }

      // see if there's any more data in the src - if so, copy it
      int srcRemaining = (int) (src.getLength() - src.getOffset());
      fm.writeBytes(src.readBytes(srcRemaining));

      // finally, go back and write the archive size
      fm.seek(4);

      // 4 - Chunk Size
      fm.writeInt((fm.getLength() - 12));

      src.close();
      fm.close();

    }
    catch (Throwable t) {
      logError(t);
    }
  }

  /**
   **********************************************************************************************
   When replacing files, if the file is of a certain type, it will be converted before replace
   @param resourceBeingReplaced the Resource in the archive that is being replaced
   @param fileToReplaceWith the file on your PC that will replace the Resource. This file is the
          one that will be converted into a different format, if applicable.
   @return the converted file, if conversion was applicable/successful, else the original fileToReplaceWith
   **********************************************************************************************
   **/
  @Override
  public File convertOnReplace(Resource resourceBeingReplaced, File fileToReplaceWith) {
    try {

      PreviewPanel imagePreviewPanel = loadFileForConversion(resourceBeingReplaced, fileToReplaceWith, "txd_tex");
      if (imagePreviewPanel == null) {
        // no conversion needed, or wasn't able to be converted
        return fileToReplaceWith;
      }

      // The plugin that will do the conversion
      Viewer_TXD_2_TXDTEX converterPlugin = new Viewer_TXD_2_TXDTEX();

      String beingReplacedExtension = resourceBeingReplaced.getExtension();
      File destination = new File(fileToReplaceWith.getAbsolutePath() + "." + beingReplacedExtension);
      if (destination.exists()) {
        destination.delete();
      }

      FileManipulator fmOut = new FileManipulator(destination, true);
      converterPlugin.replace(resourceBeingReplaced, imagePreviewPanel, fmOut);
      fmOut.close();

      return destination;

    }
    catch (Throwable t) {
      ErrorLogger.log(t);
      return fileToReplaceWith;
    }
  }

}
