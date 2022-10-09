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
import java.util.Arrays;
import org.watto.Language;
import org.watto.Settings;
import org.watto.component.PreviewPanel;
import org.watto.datatype.FileType;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.PluginFinder;
import org.watto.ge.plugin.RatedPlugin;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.ge.plugin.exporter.Exporter_ZLib;
import org.watto.ge.plugin.viewer.Viewer_CEG_GEKV_CEGTEX;
import org.watto.io.FileManipulator;
import org.watto.io.FilenameSplitter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_CEG_GEKV extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_CEG_GEKV() {

    super("CEG_GEKV", "CEG_GEKV");

    //         read write replace rename
    setProperties(true, false, true, false);

    setExtensions("ceg");
    setGames("The Punisher");
    setPlatforms("PC");

    // MUST BE LOWER CASE !!!
    setFileTypes(new FileType("ceg_tex", "Punisher Image", FileType.TYPE_IMAGE));

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
      if (fm.readString(4).equals("GEKV")) {
        rating += 50;
      }
      else {
        fm.seek(0);
        if (fm.readString(1).equals("x")) {
          rating += 25;
          return rating;
        }
        else {
          return rating;
        }
      }

      // Version (1)
      if (fm.readInt() == 1) {
        rating += 5;
      }

      long arcSize = fm.getLength();

      // Directory Length
      if (FieldValidator.checkLength(fm.readInt(), arcSize)) {
        rating += 5;
      }

      // File Data Length
      if (FieldValidator.checkLength(fm.readInt(), arcSize)) {
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
  
  **********************************************************************************************
  **/
  @Override
  public Resource[] read(File path) {
    try {

      //ExporterPlugin exporter = Exporter_ZLib.getInstance();
      addFileTypes();

      FileManipulator fm = new FileManipulator(path, false);

      // 4 - Header (GEKF)
      byte checkByte = fm.readByte();
      while (checkByte == 0 && fm.getOffset() < fm.getLength()) {
        checkByte = fm.readByte();
      }

      setCanReplace(true);

      if (new String(new byte[] { checkByte }).equals("x")) {

        long offset = fm.getOffset() - 1;
        //System.out.println(offset);

        // close AFTER we use FM in the line above
        fm.close();

        //extract the cab file first
        FileManipulator extDir = new FileManipulator(new File("temp" + File.separator + "ceg_decompressed.ceg"), true);
        String dirName = extDir.getFilePath();
        Resource directory = new Resource(path, dirName, offset, (int) path.length() - offset, (int) path.length() * 20);

        Exporter_ZLib.getInstance().extract(directory, extDir);

        extDir.close();

        path = new File(dirName);

        fm = new FileManipulator(path, false);
        fm.skip(1);

        setCanReplace(false);
      }

      long arcSize = fm.getLength();

      // 4 - Version (1)
      fm.skip(7);

      // 4 - Directory Length (first file offset = dirLength + 32)
      int firstDataOffset = fm.readInt() + 32;
      FieldValidator.checkOffset(firstDataOffset, arcSize);

      // 4 - File Data Length
      fm.skip(4);

      // 4 - numFiles
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // 4 - null
      // 4 - numFiles
      // 4 - Unknown (128)
      fm.skip(12);

      for (int i = 0; i < numFiles; i++) {
        // 4 - File Offset
        long offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 2 - Image Width (not including padding)
        short width = fm.readShort();
        FieldValidator.checkWidth(width);

        // 2 - Image Height (not including padding)
        short height = fm.readShort();
        FieldValidator.checkHeight(height);

        // 1 - Image Format? (15=DXT5, 7=RGBA?)
        int imageFormat = fm.readByte();

        // 1 - Unknown
        fm.skip(1);

        // 1 - Unknown
        int paddingFlag = fm.readByte();
        if (paddingFlag == 5 || paddingFlag == 4) {
          if (width < 16) {
            width = 16;
          }
          else if (width < 32) {
            width = 32;
          }
          else if (width < 64) {
            width = 64;
          }
          else if (width < 128) {
            width = 128;
          }
          else if (width < 256) {
            width = 256;
          }
          else if (width < 512) {
            width = 512;
          }
          else if (width < 1024) {
            width = 1024;
          }
        }

        // 1 - Unknown
        // 4 - Unknown
        fm.skip(5);

        // 24 - Filename (null)
        String filename = fm.readNullString(24) + ".ceg_tex";
        FieldValidator.checkFilename(filename);

        // 4 - File Length
        /*
        long length = fm.readInt();
        if (imageFormat == 7) {
          length *= 4;
        }
        FieldValidator.checkLength(length, arcSize);
        */
        fm.skip(4);

        // 4 - null
        fm.skip(4);

        //path,id,name,offset,length,decompLength,exporter
        Resource resource = new Resource(path, filename, offset);
        resource.addProperty("Width", width);
        resource.addProperty("Height", height);
        resource.addProperty("ImageFormat", imageFormat);
        resources[i] = resource;

        TaskProgressManager.setValue(i);
      }

      calculateFileSizes(resources, arcSize);

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

      long directorySize = numFiles * 48;
      long dataSize = 0;
      for (int i = 0; i < numFiles; i++) {
        dataSize += resources[i].getDecompressedLength();
      }

      // Write Header Data

      // 4 - Header (GEKV)
      // 4 - Version (1)
      fm.writeBytes(src.readBytes(8));

      // 4 - Directory Size
      fm.writeInt(directorySize);
      src.skip(4);

      // 4 - File Data Size
      fm.writeInt(dataSize);
      src.skip(4);

      // 4 - Number of Files
      // 4 - null
      // 4 - Unknown
      // 4 - Unknown (128)
      fm.writeBytes(src.readBytes(16));

      // Write Directory
      TaskProgressManager.setMessage(Language.get("Progress_WritingDirectory"));
      long offset = 32 + (directorySize);
      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];
        int length = (int) resource.getDecompressedLength();

        // 4 - File Offset
        fm.writeInt(offset);
        src.skip(4);

        // 2 - Image Width (not including padding)
        // 2 - Image Height (not including padding)
        fm.writeBytes(src.readBytes(4));

        // 1 - Image Format (15=DXT5, 7=?)
        int imageFormat = src.readByte();
        fm.writeByte(imageFormat);

        // 1 - Unknown
        // 1 - Image Padding Flag
        // 1 - Unknown
        // 4 - Unknown
        // 24 - Filename (null)
        fm.writeBytes(src.readBytes(31));

        // 4 - File Length
        /*
        int pixelLength = length;
        if (imageFormat == 7) {
          pixelLength /= 4;
        }
        fm.writeInt(pixelLength);
        src.skip(4);
        */
        fm.writeBytes(src.readBytes(4));

        // 4 - null
        fm.writeBytes(src.readBytes(4));

        offset += length;
      }

      // Write Files
      TaskProgressManager.setMessage(Language.get("Progress_WritingFiles"));
      write(resources, fm);

      src.close();
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
    if (beingReplacedExtension.equalsIgnoreCase("ceg_tex")) {
      // try to convert

      String toReplaceWithExtension = FilenameSplitter.getExtension(fileToReplaceWith);
      if (toReplaceWithExtension.equalsIgnoreCase("ceg_tex")) {
        // if the fileToReplace already has a STX extension, assume it's already a compatible STX file and doesn't need to be converted
        return fileToReplaceWith;
      }

      //
      //
      // if we're here, we want to scan to see if we can find an Image ViewerPlugin that can read the file into an ImageResource,
      // which we can then convert into a STX using plugin Viewer_CEG_GEKV_CEGTEX
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
      // If we're here, we have a rendered image, so we want to convert it into STX using Viewer_CEG_GEKV_CEGTEX
      //
      //
      Viewer_CEG_GEKV_CEGTEX converterPlugin = new Viewer_CEG_GEKV_CEGTEX();
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