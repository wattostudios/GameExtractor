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
import org.watto.ErrorLogger;
import org.watto.Language;
import org.watto.component.PreviewPanel;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.PluginFinder;
import org.watto.ge.plugin.RatedPlugin;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.ge.plugin.exporter.Exporter_Default;
import org.watto.ge.plugin.exporter.Exporter_ZLib;
import org.watto.ge.plugin.exporter.HeaderSkipExporter;
import org.watto.ge.plugin.viewer.Viewer_POD_POD6_TEX;
import org.watto.io.FileManipulator;
import org.watto.io.FilenameSplitter;
import org.watto.io.buffer.ByteBuffer;
import org.watto.io.buffer.ExporterByteBuffer;
import org.watto.io.converter.ByteArrayConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_POD_POD6 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_POD_POD6() {

    super("POD_POD6", "POD_POD6");

    //         read write replace rename
    setProperties(true, true, false, true);

    setGames("Ghostbusters: The Video Game: Remastered");
    setExtensions("pod"); // MUST BE LOWER CASE
    setPlatforms("PC");

    // MUST BE LOWER CASE !!!
    //setFileTypes(new FileType("txt", "Text Document", FileType.TYPE_DOCUMENT),
    //             new FileType("bmp", "Bitmap Image", FileType.TYPE_IMAGE)
    //             );

    setTextPreviewExtensions("dante", "jug", "lvl", "sbs", "sec", "fnt"); // LOWER CASE

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
      if (fm.readString(4).equals("POD6")) {
        rating += 50;
      }

      // Number Of Files
      if (FieldValidator.checkNumFiles(fm.readInt())) {
        rating += 5;
      }

      fm.skip(4);

      long arcSize = fm.getLength();

      // Directory Offset
      if (FieldValidator.checkOffset(fm.readInt(), arcSize)) {
        rating += 5;
      }

      // Directory Length
      if (FieldValidator.checkLength(fm.readInt(), arcSize)) {
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

      ExporterPlugin exporter = Exporter_ZLib.getInstance();
      ExporterPlugin exporterWithHeaderSkip = new HeaderSkipExporter(160);

      // RESETTING GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 4 - Header (POD6)
      fm.skip(4);

      // 4 - Number of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // 4 - Unknown (1000)
      fm.skip(4);

      // 4 - Details Directory Offset
      int dirOffset = fm.readInt();
      FieldValidator.checkOffset(dirOffset, arcSize);

      // 4 - Filename Directory Length
      int filenameDirLength = fm.readInt();
      FieldValidator.checkLength(filenameDirLength, arcSize);

      // 108 - Archive Filename (null terminated, filled with nulls)
      int filenameDirOffset = dirOffset + (numFiles * 24);
      FieldValidator.checkOffset(filenameDirOffset, arcSize);

      fm.seek(filenameDirOffset);
      byte[] nameBytes = fm.readBytes(filenameDirLength);

      FileManipulator nameFM = new FileManipulator(new ByteBuffer(nameBytes));

      fm.seek(dirOffset);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {
        // 4 - Filename Offset (relative to the start of the Filename Directory)
        int nameOffset = fm.readInt();
        FieldValidator.checkOffset(nameOffset, filenameDirLength);

        nameFM.seek(nameOffset);
        // X - Filename
        // 1 - null Filename Terminator
        // 0-15 - null Padding to a multiple of 16 bytes
        String filename = nameFM.readNullString();
        FieldValidator.checkFilename(filename);

        // 4 - Compressed Length
        int length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - File Offset
        int offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - Decompressed Length
        int decompLength = fm.readInt();
        FieldValidator.checkLength(decompLength);

        // 4 - Compression Flag? (0=uncompressed, 8=ZLib)
        int compressionFlag = fm.readInt();

        // 4 - null
        fm.skip(4);

        if (compressionFlag == 0) {
          // uncompressed

          //path,name,offset,length,decompLength,exporter
          Resource resource = new Resource(path, filename, offset, length);
          resources[i] = resource;

          if (filename.endsWith(".smp")) {
            // OGG file with 160-byte header
            resource.setExporter(exporterWithHeaderSkip);
          }

        }
        else if (compressionFlag == 8) {
          // ZLib

          //path,name,offset,length,decompLength,exporter
          resources[i] = new Resource(path, filename, offset, length, decompLength, exporter);
        }
        else {
          ErrorLogger.log("[POD_POD6] Unknown compression: " + compressionFlag);

          //path,name,offset,length,decompLength,exporter
          resources[i] = new Resource(path, filename, offset, length, decompLength);
        }

        TaskProgressManager.setValue(i);
      }

      nameFM.close();

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

      ExporterPlugin exporterDefault = Exporter_Default.getInstance();

      FileManipulator fm = new FileManipulator(path, true);
      int numFiles = resources.length;
      TaskProgressManager.setMaximum(numFiles);

      // Calculations
      TaskProgressManager.setMessage(Language.get("Progress_PerformingCalculations"));

      long dirOffset = 128;
      long filenameDirLength = 0;
      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];

        String filename = resource.getName();
        int filenameLength = filename.length() + 1;
        filenameLength += calculatePadding(filenameLength, 16);
        filenameDirLength += filenameLength;

        long fileLength = resource.getLength();
        fileLength += calculatePadding(fileLength, 16);
        dirOffset += fileLength;
      }

      // Write Header Data

      // 4 - Header (POD6)
      fm.writeString("POD6");

      // 4 - Number of Files
      fm.writeInt(numFiles);

      // 4 - Unknown (1000)
      fm.writeInt(1000);

      // 4 - Details Directory Offset
      fm.writeInt(dirOffset);

      // 4 - Filename Directory Length
      fm.writeInt(filenameDirLength);

      // 108 - Archive Filename (null terminated, filled with nulls)
      for (int f = 0; f < 108; f++) {
        fm.writeByte(0);
      }

      // Write Files
      TaskProgressManager.setMessage(Language.get("Progress_WritingFiles"));
      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];

        if (!resource.isReplaced() || resource.getExporter() instanceof HeaderSkipExporter) {
          // if the file is compressed, keep it compressed
          ExporterPlugin origExporter = resource.getExporter();
          resource.setExporter(exporterDefault);
          write(resource, fm);
          resource.setExporter(origExporter);
        }
        else {

          // write as default
          write(resource, fm);
        }

        int length = (int) resource.getLength();
        int paddingSize = calculatePadding(length, 16);
        for (int p = 0; p < paddingSize; p++) {
          fm.writeByte(0);
        }

        TaskProgressManager.setValue(i);
      }

      // Write Directory
      TaskProgressManager.setMessage(Language.get("Progress_WritingDirectory"));
      long offset = 128;
      long filenameOffset = 0;
      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];
        long length = resource.getLength();

        // 4 - Filename Offset (relative to the start of the Filename Directory)
        fm.writeInt((int) filenameOffset);

        // 4 - Compressed Length
        fm.writeInt((int) length);

        // 4 - File Offset
        fm.writeInt((int) offset);

        // 4 - Decompressed Length
        fm.writeInt((int) resource.getDecompressedLength());

        // 4 - Compression Flag? (0=uncompressed, 8=ZLib)
        if (resource.isCompressed()) {
          fm.writeInt(8);
        }
        else {
          fm.writeInt(0);
        }

        // 4 - null
        fm.writeInt(0);

        int paddingSize = calculatePadding(length, 16);
        offset += length + paddingSize;

        String filename = resource.getName();
        int filenameLength = filename.length() + 1;
        filenameLength += calculatePadding(filenameLength, 16);

        filenameOffset += filenameLength;
      }

      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];
        String filename = resource.getName();

        // X - Filename
        fm.writeString(filename);

        // 1 - null Filename Terminator
        fm.writeByte(0);

        // 0-15 - null Padding to a multiple of 16 bytes
        int paddingSize = calculatePadding(filename.length() + 1, 16);
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
   Supports...
     * replacing SMP audio files with new OGG audio
     * replacing TEX images with other images 
   @param resourceBeingReplaced the Resource in the archive that is being replaced
   @param fileToReplaceWith the file on your PC that will replace the Resource. This file is the
          one that will be converted into a different format, if applicable.
   @return the converted file, if conversion was applicable/successful, else the original fileToReplaceWith
   **********************************************************************************************
   **/
  @Override
  public File convertOnReplace(Resource resourceBeingReplaced, File fileToReplaceWith) {
    try {
      String beingReplacedExtension = resourceBeingReplaced.getExtension();

      if (beingReplacedExtension.equalsIgnoreCase("smp")) {
        //
        // SMP AUDIO
        //

        if (!FilenameSplitter.getExtension(fileToReplaceWith).equalsIgnoreCase("ogg")) {
          // If the fileToReplaceWith has a OGG extension, we can import it by prepending the 160-byte header to it.
          // Otherwise, replace as raw.
          return fileToReplaceWith;
        }

        //
        //
        // If we're here, we want to write out a temp file that contains the 160-byte header, followed by OGG data
        //
        //
        File destination = new File(fileToReplaceWith.getAbsolutePath() + "." + beingReplacedExtension);
        if (destination.exists()) {
          destination.delete();
        }

        // Read the 160-byte header from the existing (original) file
        Resource clonedOriginal = (Resource) resourceBeingReplaced.clone();
        clonedOriginal.setDecompressedLength(160);
        clonedOriginal.setLength(160);
        FileManipulator fm = new FileManipulator(new ExporterByteBuffer(clonedOriginal));
        byte[] headerBytes = fm.readBytes(160);
        fm.close();

        // replace the file length in the header, with the new file length (at offset 32)
        int newFileLength = (int) fileToReplaceWith.length();
        byte[] newLengthBytes = ByteArrayConverter.convertLittle(newFileLength);
        headerBytes[32] = newLengthBytes[0];
        headerBytes[33] = newLengthBytes[1];
        headerBytes[34] = newLengthBytes[2];
        headerBytes[35] = newLengthBytes[3];

        // Write the header to the temp file
        FileManipulator fmOut = new FileManipulator(destination, true);
        fmOut.writeBytes(headerBytes);

        // Now write the bytes of the new OGG file
        fm = new FileManipulator(fileToReplaceWith, false);
        fmOut.writeBytes(fm.readBytes(newFileLength));
        fm.close();

        fmOut.close();

        return destination;

      }
      else if (beingReplacedExtension.equalsIgnoreCase("tex")) {
        //
        // TEX IMAGE
        //
        if (FilenameSplitter.getExtension(fileToReplaceWith).equalsIgnoreCase("tex")) {
          // if the fileToReplace already has a TEX extension, assume it's already a compatible TEX file and doesn't need to be converted
          return fileToReplaceWith;
        }

        //
        //
        // If we're here, we want to write out a temp file that contains the converted TEX image
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
        // If we're here, we have a rendered image, so we want to convert it into TEX using Viewer_POD_POD6_TEX
        //
        //
        Viewer_POD_POD6_TEX converterPlugin = new Viewer_POD_POD6_TEX();

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
        //
        // EVERYTHING ELSE
        //
        return fileToReplaceWith;
      }
    }
    catch (Throwable t) {
      return fileToReplaceWith;
    }
  }

}
