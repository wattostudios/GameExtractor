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
import org.watto.datatype.Archive;
import org.watto.datatype.FileType;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.viewer.Viewer_TEX_5_PXL;
import org.watto.io.FileManipulator;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_TEX_5 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_TEX_5() {

    super("TEX_5", "TEX_5");

    //         read write replace rename
    setProperties(true, false, true, false);

    setGames("Dino Crisis 2");
    setExtensions("tex"); // MUST BE LOWER CASE
    setPlatforms("PC");

    // MUST BE LOWER CASE !!!
    setFileTypes(new FileType("pxl", "PXL Image", FileType.TYPE_IMAGE));

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

      if (fm.readInt() == 1) {
        rating += 5;
      }

      if (fm.readInt() == 65536) {
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

      int numFiles = Archive.getMaxFiles();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(2048);

      // Loop through directory
      int realNumFiles = 0;
      int offset = 2048;
      while (fm.getOffset() < 2048) {

        // 4 - Type (1=image)
        if (fm.readInt() == 1835890020) {
          // dummy - end of directory
          break;
        }

        // 4 - Data Length
        int length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - Unknown
        fm.skip(4);

        // 2 - Image Width
        short width = (short) (fm.readShort() * 2);
        FieldValidator.checkWidth(width);

        // 2 - Image Height
        short height = fm.readShort();
        FieldValidator.checkHeight(height);

        // 16 - null
        fm.skip(16);

        // 4 - Type (2=palette)
        fm.skip(4);

        // 4 - Data Length
        length += fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - Unknown
        // 2 - Image Height
        // 2 - Image Width
        // 16 - null
        fm.skip(24);

        String filename = Resource.generateFilename(realNumFiles) + ".pxl";

        //path,name,offset,length,decompLength,exporter
        Resource resource = new Resource(path, filename, offset, length);
        resource.addProperty("Width", width);
        resource.addProperty("Height", height);
        resources[realNumFiles] = resource;

        offset += length;
        offset += calculatePadding(offset, 2048);

        TaskProgressManager.setValue(realNumFiles);
        realNumFiles++;
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

      // Write Directory
      TaskProgressManager.setMessage(Language.get("Progress_WritingDirectory"));
      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];
        long length = resource.getDecompressedLength() - 512; // -512 because the file contains the palette at the end

        // 4 - Type (1=image)
        fm.writeBytes(src.readBytes(4));

        // 4 - Data Length
        fm.writeInt(length);
        src.skip(4);

        // 4 - Unknown
        // 2 - Image Height
        // 2 - Image Width
        // 16 - null
        fm.writeBytes(src.readBytes(24));

        // 4 - Type (2=palette)
        fm.writeBytes(src.readBytes(4));

        // 4 - Data Length
        fm.writeInt(512);
        src.skip(4);

        // 4 - Unknown
        // 2 - Image Height
        // 2 - Image Width
        // 16 - null
        fm.writeBytes(src.readBytes(24));
      }

      int paddingSize = (int) (2048 - src.getOffset());
      fm.writeBytes(src.readBytes(paddingSize));

      // Write Files
      TaskProgressManager.setMessage(Language.get("Progress_WritingFiles"));
      for (int i = 0; i < resources.length; i++) {
        Resource resource = resources[i];
        // X - Image Data (including Palette)
        write(resource, fm);
        TaskProgressManager.setValue(i);

        // X - Padding
        paddingSize = ArchivePlugin.calculatePadding(resource.getDecompressedLength(), 2048);
        for (int p = 0; p < paddingSize; p++) {
          fm.writeByte(0);
        }
      }

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

      PreviewPanel imagePreviewPanel = loadFileForConversion(resourceBeingReplaced, fileToReplaceWith, "pxl");
      if (imagePreviewPanel == null) {
        // no conversion needed, or wasn't able to be converted
        return fileToReplaceWith;
      }

      // The plugin that will do the conversion
      Viewer_TEX_5_PXL converterPlugin = new Viewer_TEX_5_PXL();

      String beingReplacedExtension = resourceBeingReplaced.getExtension();
      File destination = new File(fileToReplaceWith.getAbsolutePath() + "." + beingReplacedExtension);
      if (destination.exists()) {
        destination.delete();
      }

      FileManipulator fmOut = new FileManipulator(destination, true);
      converterPlugin.write(imagePreviewPanel, fmOut);
      fmOut.close();

      return destination;

    }
    catch (Throwable t) {
      ErrorLogger.log(t);
      return fileToReplaceWith;
    }
  }

}
