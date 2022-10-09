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
import java.util.Arrays;
import org.watto.Language;
import org.watto.Settings;
import org.watto.component.PreviewPanel;
import org.watto.datatype.Archive;
import org.watto.datatype.FileType;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.PluginFinder;
import org.watto.ge.plugin.RatedPlugin;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.ge.plugin.viewer.Viewer_BIN_MWO3_BINTEX;
import org.watto.io.FileManipulator;
import org.watto.io.FilenameSplitter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_BIN_MWO3 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_BIN_MWO3() {

    super("BIN_MWO3", "BIN_MWO3");

    //         read write replace rename
    setProperties(true, false, true, false);

    setGames("ESPN National Hockey Night");
    setExtensions("bin"); // MUST BE LOWER CASE
    setPlatforms("PC");

    setFileTypes(new FileType("bin_tex", "Texture Image", FileType.TYPE_IMAGE));

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
      if (fm.readString(4).equals("MWo3")) {
        rating += 50;
      }

      fm.skip(8);

      long arcSize = fm.getLength();

      // Archive Size
      if (FieldValidator.checkEquals(fm.readInt() + 64, arcSize)) {
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

      // 4 - Header (MWo3)
      // 4 - Unknown
      // 4 - Unknown
      // 4 - Archive Length [+64]
      // 8 - null
      // 4 - Unknown
      // 4 - Unknown
      // 32 - Archive Filename (null terminated, filled with nulls)
      // 192 - null Padding to offset 256
      fm.skip(256);

      int numFiles = Archive.getMaxFiles();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(arcSize);

      // Loop through directory
      int realNumFiles = 0;
      while (fm.getOffset() < arcSize) {
        // 2 - Image Width
        int width = fm.readShort();
        FieldValidator.checkWidth(width);

        // 2 - Image Height
        int height = fm.readShort();
        FieldValidator.checkHeight(height);

        // 2 - Unknown
        // 2 - Unknown (513)
        // 4 - Unknown (128)
        // 16 - null
        // 2 - Unknown
        // 2 - Unknown
        // 4 - Unknown
        // 4 - Unknown
        // 8 - null
        fm.skip(44);

        // X - Image Data (16bit)
        long offset = fm.getOffset();

        int length = width * height * 2;
        fm.skip(length);

        // X - null Padding to a multiple of 256? bytes
        fm.skip(calculatePadding(fm.getOffset(), 256));

        String filename = Resource.generateFilename(realNumFiles) + ".bin_tex";

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

      // Calculations
      TaskProgressManager.setMessage(Language.get("Progress_PerformingCalculations"));

      long archiveSize = 192;
      for (int i = 0; i < numFiles; i++) {
        int length = (int) resources[i].getDecompressedLength() + 44;
        int paddingLength = calculatePadding(length, 256);
        archiveSize += length + paddingLength;
      }

      // Write Header Data

      // 4 - Header (MWo3)
      // 4 - Unknown
      // 4 - Unknown
      fm.writeBytes(src.readBytes(12));

      // 4 - Archive Length [+64]
      fm.writeInt(archiveSize);
      src.skip(4);

      // 8 - null
      // 4 - Unknown
      // 4 - Unknown
      // 32 - Archive Filename (null terminated, filled with nulls)
      // 192 - null Padding to offset 256
      fm.writeBytes(src.readBytes(240));

      // Write Files
      TaskProgressManager.setMessage(Language.get("Progress_WritingFiles"));
      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];
        int length = (int) resource.getDecompressedLength();

        int width = 0;
        int height = 0;

        try {
          width = Integer.parseInt(resource.getProperty("Width"));
          height = Integer.parseInt(resource.getProperty("Height"));
        }
        catch (Throwable t) {
        }

        // 2 - Image Width
        // 2 - Image Height
        int oldWidth = src.readShort();
        int oldHeight = src.readShort();
        fm.writeShort(width);
        fm.writeShort(height);

        // 2 - Unknown
        // 2 - Unknown (513)
        // 4 - Unknown (128)
        // 16 - null
        // 2 - Unknown
        // 2 - Unknown
        // 4 - Unknown
        // 4 - Unknown
        // 8 - null
        fm.writeBytes(src.readBytes(44));

        // X - Image Data (16bit)
        // X - null Padding to a multiple of 256? bytes

        int oldLength = oldWidth * oldHeight * 2;
        int oldPadding = calculatePadding(oldLength + 48, 256);
        src.skip(oldLength + oldPadding);

        write(resource, fm);

        int padding = calculatePadding(length + 48, 256);
        for (int p = 0; p < padding; p++) {
          fm.writeByte(0);
        }

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
   When replacing BIN_TEX images, if the fileToReplaceWith is a different format image (eg DDS, PNG, ...)
   it can be converted into a BIN_TEX image. All other files are replaced without conversion
   @param resourceBeingReplaced the Resource in the archive that is being replaced
   @param fileToReplaceWith the file on your PC that will replace the Resource. This file is the
          one that will be converted into a different format, if applicable.
   @return the converted file, if conversion was applicable/successful, else the original fileToReplaceWith
   **********************************************************************************************
   **/
  @Override
  public File convertOnReplace(Resource resourceBeingReplaced, File fileToReplaceWith) {
    String beingReplacedExtension = resourceBeingReplaced.getExtension();
    if (beingReplacedExtension.equalsIgnoreCase("bin_tex")) {
      // try to convert

      String toReplaceWithExtension = FilenameSplitter.getExtension(fileToReplaceWith);
      if (toReplaceWithExtension.equalsIgnoreCase("bin_tex")) {
        // if the fileToReplace already has a BIN_TEX extension, assume it's already a compatible BIN_TEX file and doesn't need to be converted
        return fileToReplaceWith;
      }

      //
      //
      // if we're here, we want to scan to see if we can find an Image ViewerPlugin that can read the file into an ImageResource,
      // which we can then convert into a BIN_TEX using plugin Viewer_BIN_MWO3_BINTEX
      //
      //

      // 1. Open the file
      FileManipulator fm = new FileManipulator(fileToReplaceWith, false);

      // 2. Get all the ViewerPlugins that can read this file type
      RatedPlugin[] plugins = PluginFinder.findPlugins(fm, ViewerPlugin.class); // NOTE: This closes the fm pointer!!!
      if (plugins == null || plugins.length == 0) {
        // no viewer plugins found that will accept this file
        return fileToReplaceWith;
      }

      Arrays.sort(plugins);

      // re-open the file - it was closed at the end of findPlugins();
      fm = new FileManipulator(fileToReplaceWith, false);

      // 3. Try each plugin until we find one that can render the file as an ImageResource
      PreviewPanel imagePreviewPanel = null;
      for (int i = 0; i < plugins.length; i++) {
        fm.seek(0); // go back to the start of the file
        imagePreviewPanel = ((ViewerPlugin) plugins[i].getPlugin()).read(fm);

        if (imagePreviewPanel != null) {
          // 4. We have found a plugin that was able to render the image
          break;
        }
      }

      fm.close();

      if (imagePreviewPanel == null) {
        // no plugins were able to open this file successfully
        return fileToReplaceWith;
      }

      //
      //
      // If we're here, we have a rendered image, so we want to convert it into BIN_TEX using Viewer_BIN_MWO3_BINTEX
      //
      //
      Viewer_BIN_MWO3_BINTEX converterPlugin = new Viewer_BIN_MWO3_BINTEX();
      //File destination = new File(fileToReplaceWith.getAbsolutePath() + "." + converterPlugin.getExtension(0));
      File destination = new File(fileToReplaceWith.getAbsolutePath() + "." + beingReplacedExtension);
      if (destination.exists()) {
        destination.delete();
      }

      FileManipulator fmOut = new FileManipulator(destination, true);
      converterPlugin.write(imagePreviewPanel, fmOut);
      fmOut.close();

      return destination;
    }
    else {
      return fileToReplaceWith;
    }
  }

}
