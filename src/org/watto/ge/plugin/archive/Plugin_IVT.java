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

import org.watto.ErrorLogger;
import org.watto.Language;
import org.watto.component.PreviewPanel;
import org.watto.datatype.FileType;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.viewer.Viewer_IVT;
import org.watto.io.FileManipulator;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_IVT extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_IVT() {

    super("IVT", "IVT");

    //         read write replace rename
    setProperties(true, true, false, false);

    setGames("International Volleyball 2009");
    setExtensions("ivt"); // MUST BE LOWER CASE
    setPlatforms("PC");

    // MUST BE LOWER CASE !!!
    setFileTypes(new FileType("ivt", "IVT Image", FileType.TYPE_IMAGE));

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

      fm.skip(1);

      if (FieldValidator.checkRange(fm.readByte(), 0, 3)) {
        rating += 5;
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
  @SuppressWarnings("unused")
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

      // 1 - Flags
      /*
      int flags = ByteConverter.unsign(fm.readByte());
      int flags1 = flags & 7;
      int flags2 = ((flags >> 6) & 3) + 1;
      */
      fm.skip(1);

      // 1 - Image Type
      int imageFormat = fm.readByte();

      // 6 - Unknown
      fm.skip(6);

      int numPixels = (int) arcSize - 8;

      // guess the width/height;
      int width = 256;
      int height = 256;
      int bytes = 4;
      if (numPixels == 1048576) {
        width = 512;
        height = 512;
        bytes = 4;
      }
      else if (numPixels == 786432) {
        width = 512;
        height = 512;
        bytes = 3;
      }
      else if (numPixels == 524288) {
        width = 512;
        height = 256;
        bytes = 4;
      }
      else if (numPixels == 393216) {
        width = 512;
        height = 256;
        bytes = 3;

        String name = fm.getFile().getName().toLowerCase();
        if (name.equals("sponsors.ivt")) {
          width = 256;
          height = 512;
        }
      }
      else if (numPixels == 262144) {
        width = 256;
        height = 256;
        bytes = 4;

        String name = fm.getFile().getName().toLowerCase();
        if (name.startsWith("pub2")) {
          width = 512;
          height = 128;
        }
      }
      else if (numPixels == 196608) {
        width = 256;
        height = 256;
        bytes = 3;
      }
      else if (numPixels == 131072) {
        width = 256;
        height = 128;
        bytes = 4;
      }
      else if (numPixels == 98304) {
        width = 256;
        height = 128;
        bytes = 3;

        String name = fm.getFile().getName().toLowerCase();
        if (name.startsWith("telon")) {
          width = 128;
          height = 256;
        }
      }
      else if (numPixels == 65536) {
        width = 128;
        height = 128;
        bytes = 4;

        String name = fm.getFile().getName().toLowerCase();
        if (name.equals("interface1.ivt") || name.equals("tournament5.ivt")) {
          width = 256;
          height = 64;
        }
        else if (name.equals("gallery.ivt")) {
          width = 512;
          height = 32;
        }
      }
      else if (numPixels == 49152) {
        width = 128;
        height = 128;
        bytes = 3;
      }
      else if (numPixels == 32768) {
        width = 128;
        height = 64;
        bytes = 4;
      }
      else if (numPixels == 16384) {
        width = 64;
        height = 64;
        bytes = 4;
      }
      else if (numPixels == 12288) {
        width = 64;
        height = 64;
        bytes = 3;
      }
      else if (numPixels == 8192) {
        width = 64;
        height = 32;
        bytes = 4;
      }
      else if (numPixels == 4096) {
        width = 64;
        height = 16;
        bytes = 4;

        String name = fm.getFile().getName().toLowerCase();
        if (name.startsWith("flag")) {
          width = 32;
          height = 32;
        }
      }
      else if (numPixels == 3072) {
        width = 32;
        height = 32;
        bytes = 3;
      }
      else if (numPixels == 2048) {
        width = 32;
        height = 16;
        bytes = 4;
      }
      else if (numPixels == 1024) {
        width = 16;
        height = 16;
        bytes = 4;
      }
      else {
        return null;
      }

      int numFiles = 1;

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {

        int offset = 0;
        int length = (int) arcSize;

        // X - Filename (null)
        String filename = path.getName();

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

      PreviewPanel imagePreviewPanel = loadFileForConversion(resourceBeingReplaced, fileToReplaceWith, "ivt");
      if (imagePreviewPanel == null) {
        // no conversion needed, or wasn't able to be converted
        return fileToReplaceWith;
      }

      // The plugin that will do the conversion
      Viewer_IVT converterPlugin = new Viewer_IVT();

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

      // Write Files
      TaskProgressManager.setMessage(Language.get("Progress_WritingFiles"));
      write(resources, fm);

      fm.close();

    }
    catch (Throwable t) {
      logError(t);
    }
  }

}
