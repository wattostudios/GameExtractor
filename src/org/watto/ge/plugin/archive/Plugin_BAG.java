/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2020 wattostudios
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
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.PluginFinder;
import org.watto.ge.plugin.RatedPlugin;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.ge.plugin.viewer.Viewer_BAG_TEX;
import org.watto.io.FileManipulator;
import org.watto.io.FilenameSplitter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_BAG extends ArchivePlugin {

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Plugin_BAG() {

    super("BAG", "BAG");

    //         read write replace rename
    setProperties(true, true, true, true);

    setGames("Brian Lara International Cricket 2005",
        "Ricky Ponting International Cricket 2005");
    setExtensions("bag");
    setPlatforms("PC");

    // We can convert some images into TEX format when replacing
    setCanConvertOnReplace(true);

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
    if (resourceBeingReplaced.getExtension().equalsIgnoreCase("tex")) {
      // try to convert

      if (FilenameSplitter.getExtension(fileToReplaceWith).equalsIgnoreCase("tex")) {
        // if the fileToReplace already has a TEX extension, assume it's already a compatible TEX file and doesn't need to be converted
        return fileToReplaceWith;
      }

      //
      //
      // if we're here, we want to scan to see if we can find an Image ViewerPlugin that can read the file into an ImageResource,
      // which we can then convert into a TEX using plugin Viewer_BAG_TEX
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
      // If we're here, we have a rendered image, so we want to convert it into TEX using Viewer_BAG_TEX
      //
      //
      ViewerPlugin converterPlugin = new Viewer_BAG_TEX();
      File destination = new File(fileToReplaceWith.getAbsolutePath() + "." + converterPlugin.getExtension(0));
      if (destination.exists()) {
        destination.delete();
      }
      converterPlugin.write(imagePreviewPanel, destination);
      return destination;
    }
    else {
      return fileToReplaceWith;
    }
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

      // Version
      if (fm.readInt() == 0) {
        rating += 5;
      }

      // Number Of Files
      if (FieldValidator.checkNumFiles(fm.readInt())) {
        rating += 5;
      }

      // Needed to add in these additional fields to help GE detect the plugins better

      // Unknown
      int value = fm.readInt();
      if (value > 0 && value < 50) {
        rating += 5;
      }

      // Unknown
      value = fm.readInt();
      if (value > 0 && value < 50) {
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

      // NOTE - Compressed file MUST know their DECOMPRESSED LENGTH
      //      - Uncompressed files MUST know their LENGTH

      addFileTypes();
      //ExporterPlugin exporter = Exporter_ZLib.getInstance();

      // RESETTING THE GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      // 4 - Version (0)
      fm.skip(4);

      // 4 - Number Of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      long arcSize = (int) fm.getLength();

      Resource[] resources = new Resource[numFiles];

      TaskProgressManager.setMaximum(numFiles);

      // skip filename length directory
      fm.skip(numFiles * 4);

      // Loop through lengths directory
      long[] lengths = new long[numFiles];
      for (int i = 0; i < numFiles; i++) {
        // 4 - File Size
        long length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);
        lengths[i] = length;
      }

      // Loop through filename directory
      String[] names = new String[numFiles];
      for (int i = 0; i < numFiles; i++) {
        // 64 - Filename (null)
        String filename = fm.readNullString(64);
        FieldValidator.checkFilename(filename);
        names[i] = filename;
      }

      int relOffset = (numFiles * 76) + 8;
      relOffset += calculatePadding(relOffset, 128);

      // Loop through offsets directory
      for (int i = 0; i < numFiles; i++) {
        // 4 - File Offset (relative to the first file offset)
        long offset = fm.readInt() + relOffset;
        FieldValidator.checkOffset(offset, arcSize);

        long length = lengths[i];
        String filename = names[i];

        //path,id,name,offset,length,decompLength,exporter
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
   * Writes an [archive] File with the contents of the Resources
   **********************************************************************************************
   **/
  @Override
  public void write(Resource[] resources, File path) {
    try {

      FileManipulator fm = new FileManipulator(path, true);
      int numFiles = resources.length;
      TaskProgressManager.setMaximum(numFiles);

      // GLOBALS
      int paddingMultiple = 2048;

      // Write Header Data

      // 4 - Version (0)
      fm.writeInt(0);

      // 4 - Number Of Files
      fm.writeInt(numFiles);

      TaskProgressManager.setMessage(Language.get("Progress_WritingDirectory"));

      // Write Filename Length Directory
      for (int i = 0; i < numFiles; i++) {
        int filenameLength = resources[i].getNameLength();
        if (filenameLength > 64) {
          filenameLength = 64;
        }

        // 4 - Filename Length
        fm.writeInt(filenameLength);
      }

      // Write File Length Directory
      for (int i = 0; i < numFiles; i++) {
        // 4 - File Length
        fm.writeInt((int) resources[i].getDecompressedLength());
      }

      // Write Filename Directory
      for (int i = 0; i < numFiles; i++) {
        // 64 - Filename (null)
        fm.writeNullString(resources[i].getName(), 64);
      }

      // Write File Offset Directory
      long offset = 0;
      for (int i = 0; i < numFiles; i++) {
        long decompLength = resources[i].getDecompressedLength();

        // 4 - File Offset (relative to the start of the file data)
        fm.writeInt((int) offset);

        offset += decompLength;

        int paddingSize = (int) (paddingMultiple - (decompLength % paddingMultiple));
        if (paddingSize != paddingMultiple) {
          offset += paddingSize;
        }
      }

      // X - null Padding to a multiple of 128 bytes <-- SPECIAL CASE - 128 byte padding here
      int paddingSize = (int) (128 - (fm.getOffset() % 128));
      if (paddingSize != 128) {
        for (int p = 0; p < paddingSize; p++) {
          fm.writeByte(0);
        }
      }

      TaskProgressManager.setMessage(Language.get("Progress_WritingFiles"));

      // Write Files
      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];
        long decompLength = resource.getDecompressedLength();

        // X - File Data
        write(resource, fm);

        // X -null Padding to a multiple of paddingMultiple bytes
        paddingSize = (int) (paddingMultiple - (decompLength % paddingMultiple));
        if (paddingSize != paddingMultiple) {
          for (int p = 0; p < paddingSize; p++) {
            fm.writeByte(0);
          }
        }
      }

      fm.close();

    }
    catch (Throwable t) {
      logError(t);
    }
  }

}
