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
import org.watto.component.PreviewPanel_Image;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.PluginFinder;
import org.watto.ge.plugin.RatedPlugin;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.ge.plugin.viewer.Viewer_XAF_XAF_STX;
import org.watto.io.FileManipulator;
import org.watto.io.FilenameSplitter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_XAF_XAF extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_XAF_XAF() {

    super("XAF_XAF", "XAF_XAF");

    //         read write replace rename
    setProperties(true, true, false, true);

    setGames("Beat Down: Fists Of Vengeance");
    setExtensions("xaf");
    setPlatforms("XBox", "PS2");

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
      if (fm.readString(4).equals("XAF" + (char) 0)) {
        rating += 50;
      }

      // Unknown (256)
      if (fm.readInt() == 256) {
        rating += 5;
      }

      // Description
      if (fm.readString(28).equals("XPEC Entertainment Inc. 2003")) {
        rating += 5;
      }

      // null
      if (fm.readInt() == 0) {
        rating += 5;
      }

      // Number Of Files
      if (FieldValidator.checkNumFiles(fm.readInt())) {
        rating += 5;
      }

      // Number Of Files
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

      // RESETTING THE GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = (int) fm.getLength();

      // 4 - Header ("XAF" + null)
      // 4 - Max Filename Length (256)
      // 28 - Description (XPEC Entertainment Inc. 2003)
      // 4 - null
      fm.skip(40);

      // 4 - Number Of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // 4 - Number Of Files [+1]
      // 4 - Archive Size
      // 12 - null
      fm.skip(20 + numFiles * 8);

      // Loop through directory
      String[] names = new String[numFiles];
      for (int i = 0; i < numFiles; i++) {
        // 260 - Filename (null)
        String filename = fm.readNullString(260);

        if (filename.startsWith("..\\..\\")) {
          filename = "______" + filename.substring(6);
        }

        names[i] = filename;
      }

      fm.seek(64);

      Resource[] resources = new Resource[numFiles];

      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {
        // 4 - File Offset
        long offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - File Length
        long length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        String filename = names[i];

        // just a WAVE audio file - no need for a special exporter
        if (filename.indexOf(".xau") > 0) {
          offset += 64;
          length -= 64;
          filename += ".wav";
        }

        //path,id,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, filename, offset, length);

        TaskProgressManager.setValue(i);
      }

      // If the file is an STX or BIN, we want to read the unknown value and store it on the Resource (for Replacing purposes)
      fm.getBuffer().setBufferSize(4);
      fm.seek(1);

      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];
        String extension = resource.getExtension();
        if (extension.equalsIgnoreCase("stx") || extension.equalsIgnoreCase("bin")) {
          fm.seek(resource.getOffset() + 8);
          // 4 - Unknown Value
          int unknownValue = fm.readInt();
          resource.addProperty("ExtraBytes", unknownValue);
        }
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

      // Calculations
      TaskProgressManager.setMessage(Language.get("Progress_PerformingCalculations"));

      long directorySize = 64 + (numFiles * 268);

      int dirPaddingSize = calculatePadding(directorySize, 2048);
      directorySize += dirPaddingSize;

      int fileIDDirLength = (numFiles - 1) * 2;
      fileIDDirLength += calculatePadding(fileIDDirLength, 2048);

      long filesSize = 0;
      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];
        long decompLength = resource.getDecompressedLength();

        if (resource.getExtension().equals("wav")) {
          // put it back into an XAU file
          decompLength += 64;
        }

        filesSize += decompLength + calculatePadding(decompLength, 2048);
      }

      long archiveSize = directorySize + fileIDDirLength + filesSize;

      // Write Header Data

      // 4 - Header ("XAF" + null)
      fm.writeString("XAF");
      fm.writeByte(0);

      // 4 - Max Filename Length (256)
      fm.writeInt(256);
      ;

      // 28 - Description (XPEC Entertainment Inc. 2003)
      fm.writeString("XPEC Entertainment Inc. 2003");

      // 4 - null
      fm.writeInt(0);

      // 4 - Number Of Files
      fm.writeInt(numFiles);

      // 4 - Number Of Files [+1]
      fm.writeInt(numFiles - 1);

      // 4 - Archive Size
      fm.writeInt(archiveSize);

      // 12 - null
      fm.writeInt(0);
      fm.writeInt(0);
      fm.writeInt(0);

      // Write Details Directory
      TaskProgressManager.setMessage(Language.get("Progress_WritingDirectory"));
      long offset = directorySize + fileIDDirLength;
      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];
        long decompLength = resource.getDecompressedLength();

        if (resource.getExtension().equals("wav")) {
          // put it back into an XAU file
          decompLength += 64;
        }

        // 4 - File Offset
        fm.writeInt((int) offset);

        // 4 - File Length
        fm.writeInt((int) decompLength);

        offset += decompLength + calculatePadding(decompLength, 2048);
      }

      // Write Filename Directory
      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];

        String name = resource.getName();

        if (name.endsWith(".wav")) {
          // put it back into an XAU file
          name = name.substring(0, name.length() - 4);
        }

        if (name.startsWith("______")) {
          name = "..\\..\\" + name.substring(6);
        }

        // 256 - Filename (null)
        fm.writeNullString(name, 256);

        // 4 - File Type? (0/4)
        fm.writeInt(4);
      }

      // X - null Padding to a multiple of 2048 bytes
      for (int i = 0; i < dirPaddingSize; i++) {
        fm.writeByte(0);
      }

      // Write File ID Directory
      for (int i = 1; i < numFiles; i++) {
        // 2 - File ID (incremental from 1, doesn't include the last file)
        fm.writeShort(i);
      }

      // X - null Padding to a multiple of 2048 bytes
      int fileIDDirPadding = calculatePadding(((numFiles - 1) * 2), 2048);
      for (int i = 0; i < fileIDDirPadding; i++) {
        fm.writeByte(0);
      }

      // Write Files
      TaskProgressManager.setMessage(Language.get("Progress_WritingFiles"));
      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];

        long decompLength = resource.getDecompressedLength();

        if (resource.getExtension().equals("wav")) {
          // put it back into an XAU file
          decompLength += 64;

          // write the 64-byte header (just nulls, not sure what it should be)
          for (int p = 0; p < 64; p++) {
            fm.writeByte(0);
          }
        }

        // X - File Data
        write(resource, fm);
        TaskProgressManager.setValue(i);

        // 0-2047 - null Padding to a multiple of 2048 bytes
        int paddingSize = calculatePadding(decompLength, 2048);
        for (int p = 0; p < paddingSize; p++) {
          fm.writeByte(0);
        }

      }

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
    if (beingReplacedExtension.equalsIgnoreCase("stx") || beingReplacedExtension.equalsIgnoreCase("bin")) {
      // try to convert

      String toReplaceWithExtension = FilenameSplitter.getExtension(fileToReplaceWith);
      if (toReplaceWithExtension.equalsIgnoreCase("stx") || toReplaceWithExtension.equalsIgnoreCase("bin")) {
        // if the fileToReplace already has a STX extension, assume it's already a compatible STX file and doesn't need to be converted
        return fileToReplaceWith;
      }

      //
      //
      // if we're here, we want to scan to see if we can find an Image ViewerPlugin that can read the file into an ImageResource,
      // which we can then convert into a STX using plugin Viewer_XAF_XAF_STX
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
      // If we're here, we have a rendered image, so we want to convert it into STX using Viewer_XAF_XAF_STX
      //
      //
      Viewer_XAF_XAF_STX converterPlugin = new Viewer_XAF_XAF_STX();
      //File destination = new File(fileToReplaceWith.getAbsolutePath() + "." + converterPlugin.getExtension(0));
      File destination = new File(fileToReplaceWith.getAbsolutePath() + "." + beingReplacedExtension);
      if (destination.exists()) {
        destination.delete();
      }

      // We need to store the extra bytes on the image, so we can use them in the Writer
      try {
        String extraBytesString = resourceBeingReplaced.getProperty("ExtraBytes");
        int extraBytes = -1;
        if (extraBytesString != null && extraBytesString.length() > 0) {
          extraBytes = Integer.parseInt(extraBytesString);
        }
        if (extraBytes != -1) {
          ((PreviewPanel_Image) imagePreviewPanel).getImageResource().addProperty("ExtraBytes", "" + extraBytes);
        }
      }
      catch (Throwable t) {
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
