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
import org.watto.component.PreviewPanel;
import org.watto.datatype.FileType;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.PluginFinder;
import org.watto.ge.plugin.RatedPlugin;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.ge.plugin.viewer.Viewer_BIG_4_TEX256;
import org.watto.io.FileManipulator;
import org.watto.io.FilenameSplitter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_BIG_4 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_BIG_4() {

    super("BIG_4", "BIG_4");

    //         read write replace rename
    setProperties(true, true, false, false);

    setGames("Legacy of Kain: Soul Reaver");
    setExtensions("big"); // MUST BE LOWER CASE
    setPlatforms("PC");

    // MUST BE LOWER CASE !!!
    setFileTypes(new FileType("tex256", "Texture 256 Image", FileType.TYPE_IMAGE));

    //setTextPreviewExtensions("colours", "rat", "screen", "styles"); // LOWER CASE

    setCanConvertOnReplace(true);

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

      if (fm.getFile().getName().toLowerCase().equals("textures.big")) {
        rating += 25;
      }
      else {
        rating -= 25;
      }

      fm.skip(2);

      // Number Of Files
      if (FieldValidator.checkNumFiles(fm.readShort())) {
        rating += 5;
      }

      if (fm.readLong() == 0) {
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

      // 2 - Unknown
      fm.skip(2);

      // 2 - Number of Images
      short numFiles = fm.readShort();
      FieldValidator.checkNumFiles(numFiles);

      // 4092 - null Padding to offset 4096
      fm.skip(4092);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      long offset = 4096;
      int length = 131072;
      for (int i = 0; i < numFiles; i++) {
        // 131072 - Image Data (256*256*2)
        FieldValidator.checkOffset(offset, arcSize);

        String filename = Resource.generateFilename(i) + ".tex256";

        //path,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, filename, offset, length);

        TaskProgressManager.setValue(i);

        offset += length;
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
   * Writes an [archive] File with the contents of the Resources
   **********************************************************************************************
   **/
  @Override
  public void write(Resource[] resources, File path) {
    try {

      FileManipulator fm = new FileManipulator(path, true);
      int numFiles = resources.length;
      TaskProgressManager.setMaximum(numFiles);

      // Write Header Data

      // 2 - Unknown (512)
      fm.writeShort(512);

      // 2 - Number of Images
      fm.writeShort(numFiles);

      // 4092 - null Padding to offset 4096
      for (int p = 0; p < 4092; p++) {
        fm.writeByte(0);
      }

      // Write Files
      TaskProgressManager.setMessage(Language.get("Progress_WritingFiles"));
      write(resources, fm);

      //ExporterPlugin exporter = new Exporter_ZLib();
      //long[] compressedLengths = write(exporter,resources,fm);

      fm.close();

    }
    catch (Throwable t) {
      logError(t);
    }
  }

  /**
   **********************************************************************************************
   When replacing TEX images, if the fileToReplaceWith is a different format image (eg DDS, PNG, ...)
   it can be converted into a TEX image. All other files are replaced without conversion
   @param resourceBeingReplaced the Resource in the archive that is being replaced
   @param fileToReplaceWith the file on your PC that will replace the Resource. This file is the
          one that will be converted into a different format, if applicable.
   @return the converted file, if conversion was applicable/successful, else the original fileToReplaceWith
   **********************************************************************************************
   **/
  @Override
  public File convertOnReplace(Resource resourceBeingReplaced, File fileToReplaceWith) {
    String beingReplacedExtension = resourceBeingReplaced.getExtension();
    if (beingReplacedExtension.equalsIgnoreCase("tex256")) {
      // try to convert

      String toReplaceWithExtension = FilenameSplitter.getExtension(fileToReplaceWith);
      if (toReplaceWithExtension.equalsIgnoreCase("tex256")) {
        // if the fileToReplace already has a tex256 extension, assume it's already a compatible tex256 file and doesn't need to be converted
        return fileToReplaceWith;
      }

      //
      //
      // if we're here, we want to scan to see if we can find an Image ViewerPlugin that can read the file into an ImageResource,
      // which we can then convert into a tex256 using plugin Viewer_BIG_4_TEX256
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
      // If we're here, we have a rendered image, so we want to convert it into tex256 using Viewer_BIG_4_TEX256
      //
      //
      Viewer_BIG_4_TEX256 converterPlugin = new Viewer_BIG_4_TEX256();
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
