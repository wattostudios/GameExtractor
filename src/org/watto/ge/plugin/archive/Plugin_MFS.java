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
import org.watto.Settings;
import org.watto.component.PreviewPanel;
import org.watto.component.WSPluginManager;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.PluginFinder;
import org.watto.ge.plugin.RatedPlugin;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.ge.plugin.viewer.Viewer_MFS_WIM_WIMG;
import org.watto.io.FileManipulator;
import org.watto.io.FilenameSplitter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_MFS extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_MFS() {

    super("MFS", "MFS");

    //         read write replace rename
    setProperties(true, false, true, false);

    setGames("Gladiator: Sword Of Vengeance",
        "Made Man");
    setExtensions("mfs");
    setPlatforms("PC", "XBox");

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

      // Number Of Files
      if (FieldValidator.checkNumFiles(fm.readInt())) {
        rating += 5;
      }

      // Version (1)
      if (fm.readInt() == 1) {
        rating += 5;
      }

      long arcSize = fm.getLength();

      // First File Offset
      if (FieldValidator.checkOffset(fm.readInt(), arcSize)) {
        rating += 5;
      }

      // Header
      if (fm.readString(4).equals("MFS4")) {
        rating += 50;
      }

      return rating;

    }
    catch (Throwable t) {
      return 0;
    }
  }

  /**
   **********************************************************************************************
   Provide hints to the previewer so that certain document types are displayed appropriately
   **********************************************************************************************
   **/
  @Override
  public ViewerPlugin previewHint(Resource resource) {
    String extension = resource.getExtension();
    if (extension.equalsIgnoreCase("pea") || extension.equalsIgnoreCase("se") || extension.equalsIgnoreCase("slf")) {
      return (ViewerPlugin) WSPluginManager.getPlugin("Viewer", "TXT");
    }
    return null;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public Resource[] read(File path) {
    try {

      addFileTypes();

      // RESETTING THE GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 4 - Number Of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // 4 - Version (1)
      fm.skip(4);

      // 4 - First File Offset
      int firstFileOffset = fm.readInt();
      FieldValidator.checkOffset(firstFileOffset, arcSize);

      // 4 - Header (MFS4)
      fm.skip(4);

      // 4 - Padding Size (2048)
      int paddingSize = fm.readInt();
      FieldValidator.checkLength(paddingSize, arcSize);

      // 32 - Filename Of Archive, in CAPS (eg CD-2.EN)
      fm.skip(32);

      // 4 - Filename Directory Offset
      long dirOffset = fm.readInt();
      FieldValidator.checkOffset(dirOffset, arcSize);

      // 4 - Filename Directory Length
      // 4 - null

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Read the Filenames
      fm.seek(dirOffset);

      // 4 - Extensions Header (EXT )
      // 4 - Length Of Data (excluding padding at the end) [+12]
      fm.skip(8);

      // 4 - Offset to the start of filenames
      int filenameDirOffset = fm.readInt();
      FieldValidator.checkOffset(filenameDirOffset, arcSize);

      // Read the offsets directory
      long[] filenameOffsets = new long[numFiles];
      for (int i = 0; i < numFiles; i++) {
        // 4 - Filename Offset
        long filenameOffset = fm.readInt();
        FieldValidator.checkOffset(filenameOffset, arcSize);
        filenameOffsets[i] = filenameOffset;

        // 4 - Unknown
        fm.skip(4);
      }

      fm.seek(filenameDirOffset);

      // Loop through directory
      String[] filenames = new String[numFiles];
      for (int i = 0; i < numFiles; i++) {
        // just to be sure - go to the correct filename offset
        fm.seek(filenameOffsets[i]);

        // X - Filename (null) (including "c:\" etc.)
        String filename = fm.readNullString();
        if (filename.length() > 3 && filename.charAt(1) == ':') {
          filename = filename.substring(3);
        }
        filenames[i] = filename;
      }

      // now loop through the files directory
      fm.seek(64);

      for (int i = 0; i < numFiles; i++) {
        // 4 - Unknown
        fm.skip(4);

        // 4 - File Offset [* PaddingSize]
        long offset = fm.readInt() * paddingSize;
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - File Size
        int length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - File Size
        int otherLength = fm.readInt();
        FieldValidator.checkLength(otherLength, arcSize);

        // 2 - null
        // 2 - Unknown
        fm.skip(4);

        //path,id,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, filenames[i], offset, length);

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

      // 4 - Number Of Files
      // 4 - Version (1)
      fm.writeBytes(src.readBytes(8));

      // 4 - First File Offset
      long offset = src.readInt();
      fm.writeInt((int) offset);

      // 4 - Header (MFS4)
      fm.writeBytes(src.readBytes(4));

      // 4 - Padding Size (2048)
      int paddingSize = src.readInt();
      fm.writeInt(paddingSize);

      offset /= paddingSize;

      // 32 - Filename Of Archive, in CAPS (eg CD-2.EN)
      // 4 - Filename Directory Offset
      fm.writeBytes(src.readBytes(36));

      // 4 - Filename Directory Length (including padding)
      int filenameDirLength = src.readInt();
      fm.writeInt(filenameDirLength);

      // 4 - null
      fm.writeBytes(src.readBytes(4));

      // Write Directory
      TaskProgressManager.setMessage(Language.get("Progress_WritingDirectory"));

      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];
        long length = resource.getDecompressedLength();

        // 4 - Hash?
        fm.writeBytes(src.readBytes(4));

        // 4 - File Offset [* PaddingSize]
        fm.writeInt(offset);

        // 4 - File Size
        fm.writeInt(length);

        // 4 - File Size
        fm.writeInt(length);

        src.skip(12);

        // 2 - null
        // 2 - Unknown
        fm.writeBytes(src.readBytes(4));

        int lengthPadding = calculatePadding(length, paddingSize);
        length += lengthPadding;
        length /= paddingSize;

        offset += length;
      }

      // write the padding after the directory
      int padding = calculatePadding((numFiles * 20 + 64), paddingSize);
      for (int p = 0; p < padding; p++) {
        fm.writeByte(0);
      }
      src.skip(padding);

      // write the filename directory (including padding)
      fm.writeBytes(src.readBytes(filenameDirLength));

      // Write Files
      TaskProgressManager.setMessage(Language.get("Progress_WritingFiles"));
      for (int i = 0; i < resources.length; i++) {
        Resource resource = resources[i];

        // X - File Data
        write(resource, fm);

        // 0-2047 - null Padding to a multiple of 2048 bytes
        padding = calculatePadding((resource.getDecompressedLength()), paddingSize);
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
   When replacing dtx images, if the fileToReplaceWith is a different format image (eg DDS, PNG, ...)
   it can be converted into a dtx image. All other files are replaced without conversion
   @param resourceBeingReplaced the Resource in the archive that is being replaced
   @param fileToReplaceWith the file on your PC that will replace the Resource. This file is the
          one that will be converted into a different format, if applicable.
   @return the converted file, if conversion was applicable/successful, else the original fileToReplaceWith
   **********************************************************************************************
   **/
  @Override
  public File convertOnReplace(Resource resourceBeingReplaced, File fileToReplaceWith) {

    String beingReplacedExtension = resourceBeingReplaced.getExtension();
    if (beingReplacedExtension.equalsIgnoreCase("wim")) {
      // try to convert

      String toReplaceWithExtension = FilenameSplitter.getExtension(fileToReplaceWith);
      if (toReplaceWithExtension.equalsIgnoreCase("wim")) {
        // if the fileToReplace already has a dtx extension, assume it's already a compatible dtx file and doesn't need to be converted
        return fileToReplaceWith;
      }

      //
      //
      // if we're here, we want to scan to see if we can find an Image ViewerPlugin that can read the file into an ImageResource,
      // which we can then convert into a dtx using plugin Viewer_REZ_REZMGR_DTX
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
          // found a previewer
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
      // If we're here, we have a rendered image, so we want to convert it into WIM
      //
      //
      Viewer_MFS_WIM_WIMG converterPlugin = new Viewer_MFS_WIM_WIMG();

      File destination = new File(fileToReplaceWith.getAbsolutePath() + "." + beingReplacedExtension);
      if (destination.exists()) {
        destination.delete();
      }

      FileManipulator fmOut = new FileManipulator(destination, true);
      converterPlugin.replace(resourceBeingReplaced, imagePreviewPanel, fmOut);
      fmOut.close();

      return destination;

    }
    else {
      return fileToReplaceWith;
    }
  }

}
