/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2023 wattostudios
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
import org.watto.ge.plugin.viewer.Viewer_SPR_PA_SPRTEX;
import org.watto.io.FileManipulator;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_SPR_PA extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_SPR_PA() {

    super("SPR_PA", "SPR_PA");

    //         read write replace rename
    setProperties(true, false, true, false);

    setGames("Pax Corpus");
    setExtensions("spr"); // MUST BE LOWER CASE
    setPlatforms("PC");

    setFileTypes(new FileType("spr_tex", "Texture Image", FileType.TYPE_IMAGE));

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
      if (fm.readString(4).equals(":PA:")) {
        rating += 50;
      }

      fm.skip(4);

      if (fm.readInt() == 0) {
        rating += 5;
      }

      if (fm.readInt() == 128) {
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

      // 4 - Header (:PA:)
      // 2 - Unknown (768)
      // 2 - Unknown (256)
      // 4 - null
      // 4 - Unknown (128)

      // for each palette (2)
      //   256*4 - Colors
      fm.seek(2064);

      int numFiles = Archive.getMaxFiles();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(arcSize);

      // Loop through directory
      int realNumFiles = 0;
      while (fm.getOffset() < arcSize) {

        // 4 - Image Width/Height
        int width = fm.readInt();
        FieldValidator.checkWidth(width);

        // 4 - Image Height (real?)
        int height = fm.readInt();
        FieldValidator.checkHeight(height);

        // 4 - Unknown (255)
        // 4 - Unknown (255)
        // 4 - null
        fm.skip(12);

        // X - Pixel Data (RGB)
        long offset = fm.getOffset();

        long length = width * height * 3;
        FieldValidator.checkLength(length);
        fm.skip(length);

        // X - Paletted Image
        fm.skip(width * height);

        // 4 - null    
        fm.skip(4);

        String filename = Resource.generateFilename(realNumFiles) + ".spr_tex";

        //path,name,offset,length,decompLength,exporter
        Resource resource = new Resource(path, filename, offset, length);
        resource.addProperty("Width", width);
        resource.addProperty("Height", height);
        resources[realNumFiles] = resource;

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

      // 4 - Header (:PA:)
      // 2 - Unknown (768)
      // 2 - Unknown (256)
      // 4 - null
      // 4 - Unknown (128)

      // for each palette (2)
      //   256*4 - Colors
      fm.writeBytes(src.readBytes(2064));

      // Write Files
      TaskProgressManager.setMessage(Language.get("Progress_WritingFiles"));
      for (int i = 0; i < resources.length; i++) {
        Resource resource = resources[i];

        int height = 32;
        int width = 32;

        // get the width/height from the properties of the image resource, which were read by the ArchivePlugin
        try {
          height = Integer.parseInt(resource.getProperty("Height"));
          width = Integer.parseInt(resource.getProperty("Width"));
        }
        catch (Throwable t) {
        }

        // 4 - Image Width/Height
        fm.writeInt(width);

        // 4 - Image Height (real?)
        fm.writeInt(height);

        // 4 - Unknown (255)
        fm.writeInt(255);

        // 4 - Unknown (255)
        fm.writeInt(255);

        // 4 - null
        fm.writeInt(0);

        // X - Pixel Data (RGB)
        write(resource, fm);

        // X - Paletted Data?
        int paddingSize = (width * height);
        for (int p = 0; p < paddingSize; p++) {
          fm.writeByte(255);
        }

        // 4 - null    
        fm.writeInt(0);

        TaskProgressManager.setValue(i);
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

      PreviewPanel imagePreviewPanel = loadFileForConversion(resourceBeingReplaced, fileToReplaceWith, "spr_tex");
      if (imagePreviewPanel == null) {
        // no conversion needed, or wasn't able to be converted
        return fileToReplaceWith;
      }

      // The plugin that will do the conversion
      Viewer_SPR_PA_SPRTEX converterPlugin = new Viewer_SPR_PA_SPRTEX();

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
