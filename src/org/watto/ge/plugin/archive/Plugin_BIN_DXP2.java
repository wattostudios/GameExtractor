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
import org.watto.ErrorLogger;
import org.watto.Language;
import org.watto.Settings;
import org.watto.component.PreviewPanel;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.exporter.Exporter_Default;
import org.watto.ge.plugin.exporter.Exporter_ZLib_CompressedSizeOnly;
import org.watto.ge.plugin.viewer.Viewer_BIN_DXP2_DDSX_DDSX;
import org.watto.io.FileManipulator;
import org.watto.io.converter.IntConverter;
import org.watto.io.converter.StringConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_BIN_DXP2 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_BIN_DXP2() {

    super("BIN_DXP2", "BIN_DXP2");

    //         read write replace rename
    setProperties(true, false, true, false);

    setGames("Wings Of Prey",
        "Blades of Time");
    setExtensions("bin"); // MUST BE LOWER CASE
    setPlatforms("PC");

    // MUST BE LOWER CASE !!!
    //setFileTypes(new FileType("txt", "Text Document", FileType.TYPE_DOCUMENT),
    //             new FileType("bmp", "Bitmap Image", FileType.TYPE_IMAGE)
    //             );

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
      if (fm.readString(4).equals("DxP2")) {
        rating += 50;
      }

      // Version
      if (fm.readInt() == 1) {
        rating += 5;
      }

      // Number Of Files
      if (FieldValidator.checkNumFiles(fm.readInt())) {
        rating += 5;
      }

      long arcSize = fm.getLength();

      // File Data Offset
      if (FieldValidator.checkOffset(fm.readInt(), arcSize)) {
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

      ExporterPlugin exporter = Exporter_ZLib_CompressedSizeOnly.getInstance();

      // RESETTING GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 4 - Header (DxP2)
      // 4 - Version (1)
      fm.skip(8);

      // 4 - Number Of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // 4 - File Data Offset
      fm.skip(4);

      // 4 - Offset to the Filename Offset Directory [+16]
      int filenameDirOffset = fm.readInt() + 16;
      FieldValidator.checkOffset(filenameDirOffset, arcSize);

      // 4 - Number of Filename Offset Entries
      int numFilenames = fm.readInt();
      FieldValidator.checkNumFiles(numFilenames);

      // 4 - Offset to the Image Properties Directory [+16]
      int imagePropertyDirOffset = fm.readInt() + 16;
      FieldValidator.checkOffset(imagePropertyDirOffset, arcSize);

      // 4 - Number of Image Property Entries
      int numImageProperties = fm.readInt();
      FieldValidator.checkNumFiles(numImageProperties);

      // 4 - Offset to the File Data Properties Directory [+16]
      int filePropertyDirOffset = fm.readInt() + 16;
      FieldValidator.checkOffset(filePropertyDirOffset, arcSize);

      // 4 - Number of File Data Property Entries
      int numFileProperties = fm.readInt();
      FieldValidator.checkNumFiles(numFileProperties);

      // 4 - null
      // 4 - null
      fm.skip(8);

      // Loop through directory
      String[] names = new String[numFilenames];
      for (int i = 0; i < numFilenames; i++) {
        // X - Filename (null)
        String filename = fm.readNullString();
        FieldValidator.checkFilename(filename);
        names[i] = filename;

        TaskProgressManager.setValue(i);
      }

      fm.seek(imagePropertyDirOffset);

      // Loop through directory
      String[] imageFormats = new String[numImageProperties];
      int[] widths = new int[numImageProperties];
      int[] heights = new int[numImageProperties];
      int[] decompLengths = new int[numImageProperties];
      for (int i = 0; i < numImageProperties; i++) {

        // 4 - Image Format (DDSx)
        fm.skip(4);

        // 4 - DDS Format (DXT1/DXT3/DXT5/(byte)21/(byte)50)
        byte[] imageFormatBytes = fm.readBytes(4);
        String imageFormat = StringConverter.convertLittle(imageFormatBytes);
        if (imageFormat.equals("DXT1") || imageFormat.equals("DXT3") || imageFormat.equals("DXT5")) {
          //
        }
        else {
          imageFormat = "" + IntConverter.convertLittle(imageFormatBytes);
        }
        imageFormats[i] = imageFormat;

        // 4 - Unknown
        fm.skip(4);

        // 2 - Image Width
        short width = fm.readShort();
        FieldValidator.checkWidth(width);
        widths[i] = width;

        // 2 - Image Height
        short height = fm.readShort();
        FieldValidator.checkHeight(height);
        heights[i] = height;

        // 2 - Number Of Mipmaps?
        // 4 - null
        // 2 - Unknown
        fm.skip(8);

        // 4 - Decompressed File Length
        int decompLength = fm.readInt();
        FieldValidator.checkLength(decompLength);

        // 4 - Compressed File Length (0 = uncompressed)
        int compLength = fm.readInt();
        FieldValidator.checkLength(compLength, arcSize);

        if (compLength == 0) {
          decompLengths[i] = 0;
        }
        else {
          decompLengths[i] = decompLength;
        }

        TaskProgressManager.setValue(i);
      }

      fm.seek(filePropertyDirOffset);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      for (int i = 0; i < numFileProperties; i++) {
        // 4 - File Data Offset
        int offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - null
        // 4 - Padding (-1)
        fm.skip(8);

        // 4 - Compressed Length
        int length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        int decompLength = decompLengths[i];

        String filename = names[i];
        int starPos = filename.indexOf('*');
        if (starPos > 0) {
          filename = filename.substring(0, starPos);
        }
        filename += ".ddsx";

        String imageFormat = imageFormats[i];
        int width = widths[i];
        int height = heights[i];

        Resource resource = null;
        if (decompLength == 0) {
          // uncompressed

          //path,name,offset,length,decompLength,exporter
          resource = new Resource(path, filename, offset, length);
        }
        else {
          // compressed

          /*
          if (imageFormat.equals("DXT1")) {
            decompLength = width * height / 2;
          }
          else if (imageFormat.equals("DXT3") || imageFormat.equals("DXT5")) {
            decompLength = width * height;
          }
          else if (imageFormat.equals("21")) {
            decompLength = width * height * 4;
          }
          else if (imageFormat.equals("50")) {
            decompLength = width * height;
          }
          */

          //path,name,offset,length,decompLength,exporter
          resource = new Resource(path, filename, offset, length, decompLength, exporter);
        }

        resource.addProperty("Width", width);
        resource.addProperty("Height", height);
        resource.addProperty("ImageFormat", imageFormat);

        resources[i] = resource;

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

      // 4 - Header (DxP2)
      // 4 - Version (1)
      // 4 - Number Of Files
      fm.writeBytes(src.readBytes(12));

      // 4 - File Data Offset [+16]
      int offset = src.readInt();
      fm.writeInt(offset);
      offset += 16;

      // 4 - Offset to the Filename Offset Directory [+16]
      // 4 - Number of Filename Offset Entries
      fm.writeBytes(src.readBytes(8));

      // 4 - Offset to the Image Properties Directory [+16]
      int propertiesOffset = src.readInt();
      fm.writeInt(propertiesOffset);
      propertiesOffset += 16;

      // 4 - Number of Image Property Entries
      fm.writeBytes(src.readBytes(4));

      // 4 - Offset to the File Data Properties Directory [+16]
      int detailsOffset = src.readInt();
      fm.writeInt(detailsOffset);

      // 4 - Number of File Data Property Entries
      // 4 - null
      // 4 - null
      fm.writeBytes(src.readBytes(12));

      // FILENAME DIRECTORY
      // FILENAME OFFSET DIRECTORY
      int filenameDirLength = propertiesOffset - (int) fm.getOffset();
      fm.writeBytes(src.readBytes(filenameDirLength));

      TaskProgressManager.setMessage(Language.get("Progress_WritingDirectory"));
      // IMAGE PROPERTIES DIRECTORY
      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];
        long length = resource.getDecompressedLength();

        if (resource.isReplaced()) {
          short width = 0;
          short height = 0;
          short mipmapCount = 0;
          String imageFormat = "";
          int imageFormatNumber = 0;

          try {
            height = (short) Integer.parseInt(resource.getProperty("Height"));
            width = (short) Integer.parseInt(resource.getProperty("Width"));

            int minDimension = width;
            if (height < width) {
              minDimension = height;
            }
            while (minDimension > 0) {
              mipmapCount++;
              minDimension /= 2;
            }

          }
          catch (Throwable t) {
          }

          imageFormat = resource.getProperty("ImageFormat");
          if (imageFormat == null || imageFormat.equals("")) {
            imageFormat = "DXT5";
          }
          while (imageFormat.length() < 4) {
            imageFormat = imageFormat + " ";
          }

          try {
            imageFormatNumber = Integer.parseInt(imageFormat);
          }
          catch (Throwable t) {
            imageFormatNumber = 0;
          }

          // 4 - Image Format (DDSx)
          fm.writeBytes(src.readBytes(4));

          // 4 - DDS Format (DXT1/DXT3/DXT5/(byte)21/(byte)50)
          if (imageFormatNumber != 0) {
            fm.writeInt(imageFormatNumber);
          }
          else {
            fm.writeString(imageFormat);
          }
          src.skip(4);

          // 4 - Unknown
          fm.writeBytes(src.readBytes(4));

          // 2 - Image Width
          fm.writeShort(width);
          src.skip(2);

          // 2 - Image Height
          fm.writeShort(height);
          src.skip(2);

          // 2 - Number Of Mipmaps
          fm.writeShort(mipmapCount);
          src.skip(2);

          // 4 - null
          // 2 - Unknown
          fm.writeBytes(src.readBytes(6));

          // 4 - Decompressed File Length
          // 4 - Compressed File Length (0 = uncompressed)
          fm.writeInt(length);
          fm.writeInt(0);
          src.skip(8);
        }
        else {
          // 4 - Image Format (DDSx)
          // 4 - DDS Format (DXT1/DXT3/DXT5/(byte)21/(byte)50)
          // 4 - Unknown
          // 2 - Image Width
          // 2 - Image Height
          // 2 - Number Of Mipmaps
          // 4 - null
          // 2 - Unknown
          // 4 - Decompressed File Length
          // 4 - Compressed File Length (0 = uncompressed)
          fm.writeBytes(src.readBytes(32));
        }

      }

      // FILE DATA PROPERTIES DIRECTORY
      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];

        long length = resource.getLength();
        if (resource.isReplaced()) {
          length = resource.getDecompressedLength();
        }

        // 4 - File Data Offset
        fm.writeInt(offset);
        src.skip(4);

        // 4 - null
        // 4 - Padding (-1)
        fm.writeBytes(src.readBytes(8));

        // 4 - Compressed Length
        fm.writeInt(length);
        src.skip(4);

        offset += length;
      }

      // Write Files
      TaskProgressManager.setMessage(Language.get("Progress_WritingFiles"));
      ExporterPlugin exporterDefault = Exporter_Default.getInstance();
      for (int i = 0; i < resources.length; i++) {
        Resource resource = resources[i];

        ExporterPlugin originalExporter = resource.getExporter();
        resource.setExporter(exporterDefault);
        write(resource, fm);
        resource.setExporter(originalExporter);

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

      PreviewPanel imagePreviewPanel = loadFileForConversion(resourceBeingReplaced, fileToReplaceWith, "ddsx");
      if (imagePreviewPanel == null) {
        // no conversion needed, or wasn't able to be converted
        return fileToReplaceWith;
      }

      // The plugin that will do the conversion
      Viewer_BIN_DXP2_DDSX_DDSX converterPlugin = new Viewer_BIN_DXP2_DDSX_DDSX();

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
