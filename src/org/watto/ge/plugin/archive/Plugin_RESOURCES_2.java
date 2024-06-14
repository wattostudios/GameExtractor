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
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.exporter.Exporter_Default;
import org.watto.ge.plugin.exporter.Exporter_Deflate;
import org.watto.ge.plugin.viewer.Viewer_RESOURCES_2_BIMAGE;
import org.watto.io.FileManipulator;
import org.watto.io.converter.IntConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_RESOURCES_2 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_RESOURCES_2() {

    super("RESOURCES_2", "RESOURCES_2");

    //         read write replace rename
    setProperties(true, false, true, false);

    setGames("RAGE");
    setExtensions("resources"); // MUST BE LOWER CASE
    setPlatforms("PC");

    setFileTypes(new FileType("bimage", "Bitmap Image", FileType.TYPE_IMAGE));

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

      fm.skip(4);

      long arcSize = fm.getLength();

      // Directory Offset
      if (FieldValidator.checkOffset(IntConverter.changeFormat(fm.readInt()), arcSize)) {
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

      ExporterPlugin exporter = Exporter_Deflate.getInstance();

      // RESETTING GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 4 - Unknown
      fm.skip(4);

      // 4 - Directory Offset
      int dirOffset = IntConverter.changeFormat(fm.readInt());
      FieldValidator.checkOffset(dirOffset, arcSize);

      // 4 - Unknown
      // 4 - null
      fm.relativeSeek(dirOffset);

      // 4 - Number Of Files
      int numFiles = IntConverter.changeFormat(fm.readInt());
      FieldValidator.checkNumFiles(numFiles);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {
        // 4 - File ID Number
        fm.skip(4);

        // 4 - Data Type Name Length (LITTLE)
        int nameLength = fm.readInt();
        FieldValidator.checkFilenameLength(nameLength + 1); // +1 to allow nulls

        // X - Data Type Name
        fm.skip(nameLength);
        //String dataTypeName = fm.readString(nameLength);

        // 4 - Source Data Name Length (LITTLE)
        nameLength = fm.readInt();
        FieldValidator.checkFilenameLength(nameLength + 1); // +1 to allow nulls

        // X - Source Data Name
        fm.skip(nameLength);
        //String sourceName = fm.readString(nameLength);

        // 4 - Filename Length (LITTLE) (can be null)
        nameLength = fm.readInt();
        FieldValidator.checkFilenameLength(nameLength + 1); // +1 to allow nulls

        // X - Filename
        String filename = fm.readString(nameLength);

        // 4 - File Offset
        long offset = IntConverter.changeFormat(fm.readInt());
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - Decompressed Length
        long decompLength = IntConverter.changeFormat(fm.readInt());
        FieldValidator.checkLength(decompLength);

        // 4 - Compressed Length
        long length = IntConverter.changeFormat(fm.readInt());
        FieldValidator.checkLength(length, arcSize);

        // 4 - Extra Length [*24]
        int extraLength = IntConverter.changeFormat(fm.readInt());
        FieldValidator.checkRange(extraLength, 0, 100); // guess

        // X - Extra Data
        fm.skip(extraLength * 24);

        // 4 - Unknown
        // 4 - Unknown
        // 4 - Unknown
        // 4 - Unknown (LITTLE)
        // 4 - Unknown (LITTLE)
        fm.skip(20);

        if (length == 0 && filename.length() == 0) {
          filename = Resource.generateFilename(i);
        }

        if (length != decompLength) {
          // compressed

          //path,name,offset,length,decompLength,exporter
          resources[i] = new Resource(path, filename, offset, length, decompLength, exporter);
        }
        else {
          // uncompressed

          //path,name,offset,length,decompLength,exporter
          resources[i] = new Resource(path, filename, offset, length);
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
   * Writes an [archive] File with the contents of the Resources. The archive is written using
   * data from the initial archive - it isn't written from scratch.
   **********************************************************************************************
   **/
  @Override
  public void replace(Resource[] resources, File path) {
    try {

      ExporterPlugin exporterDefault = Exporter_Default.getInstance();

      FileManipulator fm = new FileManipulator(path, true);
      FileManipulator src = new FileManipulator(new File(Settings.getString("CurrentArchive")), false);

      int numFiles = resources.length;
      TaskProgressManager.setMaximum(numFiles);

      // Calculations
      TaskProgressManager.setMessage(Language.get("Progress_PerformingCalculations"));

      long directoryOffset = 16;
      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];
        if (resource.isReplaced()) {
          directoryOffset += resource.getDecompressedLength();
        }
        else {
          directoryOffset += resource.getLength();
        }
      }

      // Write Header Data

      // 4 - Unknown
      fm.writeBytes(src.readBytes(4));

      // 4 - Directory Offset
      fm.writeInt(IntConverter.changeFormat((int) directoryOffset));
      int srcDirOffset = IntConverter.changeFormat(src.readInt());

      // 4 - Unknown
      // 4 - null
      fm.writeBytes(src.readBytes(8));

      // Write Files
      TaskProgressManager.setMessage(Language.get("Progress_WritingFiles"));
      for (int i = 0; i < resources.length; i++) {
        Resource resource = resources[i];

        if (resource.isReplaced()) {
          write(resource, fm);
        }
        else {
          ExporterPlugin originalExporter = resource.getExporter();
          resource.setExporter(exporterDefault);
          write(resource, fm);
          resource.setExporter(originalExporter);
        }
        TaskProgressManager.setValue(i);
      }

      src.seek(srcDirOffset);

      // Write Directory
      TaskProgressManager.setMessage(Language.get("Progress_WritingDirectory"));

      // 4 - Number Of Files
      fm.writeInt(IntConverter.changeFormat(numFiles));
      src.skip(4);

      long offset = 16;
      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];
        int decompLength = (int) resource.getDecompressedLength();
        int length = (int) resource.getLength();

        // 4 - File ID Number
        fm.writeBytes(src.readBytes(4));

        // 4 - Data Type Name Length (LITTLE)
        int srcNameLength = src.readInt();
        fm.writeInt(srcNameLength);

        // X - Data Type Name
        fm.writeBytes(src.readBytes(srcNameLength));

        // 4 - Source Data Name Length (LITTLE)
        srcNameLength = src.readInt();
        fm.writeInt(srcNameLength);

        // X - Source Data Name
        fm.writeBytes(src.readBytes(srcNameLength));

        // 4 - Filename Length (LITTLE) (can be null)
        srcNameLength = src.readInt();
        fm.writeInt(srcNameLength);

        // X - Filename
        fm.writeBytes(src.readBytes(srcNameLength));

        // 4 - File Offset
        fm.writeInt(IntConverter.changeFormat((int) offset));
        src.skip(4);

        // 4 - Decompressed Length
        fm.writeInt(IntConverter.changeFormat((int) decompLength));
        src.skip(4);

        // 4 - Compressed Length
        fm.writeInt(IntConverter.changeFormat((int) length));
        src.skip(4);

        // 4 - Extra Length [*24]
        int srcExtraLength = IntConverter.changeFormat(src.readInt());
        fm.writeInt(IntConverter.changeFormat(srcExtraLength));

        // X - Extra Data
        fm.writeBytes(src.readBytes(srcExtraLength * 24));

        // 4 - Unknown
        // 4 - Unknown
        // 4 - Unknown
        // 4 - Unknown (LITTLE)
        // 4 - Unknown (LITTLE)
        fm.writeBytes(src.readBytes(20));

        if (resource.isReplaced()) {
          offset += decompLength;
        }
        else {
          offset += length;
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

      PreviewPanel imagePreviewPanel = loadFileForConversion(resourceBeingReplaced, fileToReplaceWith, "bimage");
      if (imagePreviewPanel == null) {
        // no conversion needed, or wasn't able to be converted
        return fileToReplaceWith;
      }

      // The plugin that will do the conversion
      Viewer_RESOURCES_2_BIMAGE converterPlugin = new Viewer_RESOURCES_2_BIMAGE();

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
