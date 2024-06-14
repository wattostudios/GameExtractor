/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2024 wattostudios
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

import org.watto.Language;
import org.watto.component.WSPluginManager;
import org.watto.datatype.Archive;
import org.watto.datatype.FileType;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.io.FileManipulator;
import org.watto.io.converter.IntConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_AWB_AFS2_2 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_AWB_AFS2_2() {

    super("AWB_AFS2_2", "AWB_AFS2_2");

    //         read write replace rename
    setProperties(true, true, false, false);

    setGames("eFootball 24");
    setExtensions("awb"); // MUST BE LOWER CASE
    setPlatforms("PC");

    // MUST BE LOWER CASE !!!
    setFileTypes(new FileType("hca", "HCA Audio", FileType.TYPE_AUDIO));

    //setTextPreviewExtensions("colours", "rat", "screen", "styles"); // LOWER CASE

    setCanScanForFileTypes(true);

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
      String header = fm.readString(4);
      if (header.equals("AFS2")) {
        rating += 50;

        fm.skip(4);

        // NumFiles
        if (FieldValidator.checkNumFiles(fm.readInt())) {
          rating += 5;
        }
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

      int numFiles = Archive.getMaxFiles();
      int realNumFiles = 0;

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      while (fm.getOffset() < arcSize) {

        // 4 - Header (@UTF)
        String header = fm.readString(4);

        if (header.equals("@UTF")) {
          // 4 - Block Length [+8] (first block = Archive Length)
          int length = IntConverter.changeFormat(fm.readInt());

          // 4 - Unknown
          // 4 - Unknown
          fm.skip(8);

          // 4 - Block Data Length
          if (realNumFiles == 0) {
            length = IntConverter.changeFormat(fm.readInt());
          }
          else {
            fm.skip(4);
            length -= 16;
          }

          // 4 - null
          fm.skip(4);

          // X - Block Data
          long offset = fm.getOffset();

          FieldValidator.checkLength(length, arcSize);

          String filename = Resource.generateFilename(realNumFiles) + ".utf";

          //path,name,offset,length,decompLength,exporter
          resources[realNumFiles] = new Resource(path, filename, offset, length);

          TaskProgressManager.setValue(offset);
          realNumFiles++;

          fm.skip(length);

          // X - null Padding to a multiple of 32? bytes
          fm.skip(calculatePadding(fm.getOffset(), 32));

        }
        else if (header.equals("AFS2")) {
          long dataOffset = fm.getOffset() - 4;

          // 4 - Unknown
          fm.skip(4);

          // 4 - Number of Files
          int localNumFiles = fm.readInt();
          FieldValidator.checkNumFiles(localNumFiles);

          // 4 - Padding Length (32)
          fm.skip(4);

          // for each file
          // 4 - File ID (incremental from 0)
          fm.skip(localNumFiles * 4);

          // for each file
          long offset = fm.readInt() + dataOffset;
          offset += calculatePadding(offset, 32);
          for (int i = 0; i < localNumFiles; i++) {
            // 4 - File Offset (relative to the start of the Directory) [+ padding to a 32-block multiple]
            long nextOffset = fm.readInt() + dataOffset;

            long length = nextOffset - offset;
            FieldValidator.checkLength(length, arcSize);

            String filename = Resource.generateFilename(realNumFiles);

            //path,name,offset,length,decompLength,exporter
            resources[realNumFiles] = new Resource(path, filename, offset, length);

            TaskProgressManager.setValue(offset);
            realNumFiles++;

            offset = nextOffset += calculatePadding(nextOffset, 32);
          }

          fm.seek(offset);
          fm.skip(calculatePadding(fm.getOffset(), 32));
        }
        else {
          //ErrorLogger.log("[ACB_UTF] Unknown Header Tag " + header + " at offset" + (fm.getOffset() - 4));
          fm.skip(28); // 32-4, as we already read the 4-byte header
        }
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
  If an archive doesn't have filenames stored in it, the scanner can come here to try to work out
  what kind of file a Resource is. This method allows the plugin to provide additional plugin-specific
  extensions, which will be tried before any standard extensions.
  @return null if no extension can be determined, or the extension if one can be found
  **********************************************************************************************
  **/
  @Override
  public String guessFileExtension(Resource resource, byte[] headerBytes, int headerInt1, int headerInt2, int headerInt3, short headerShort1, short headerShort2, short headerShort3, short headerShort4, short headerShort5, short headerShort6) {

    String extension = resource.getExtension();
    if (extension != null && !extension.equals("")) {
      return extension; // keep the extension we already have (utf)
    }

    if (headerInt1 == 4277064) {
      return "hca";
    }

    return null;
  }

  /**
   **********************************************************************************************
   Provide hints to the previewer so that certain document types are displayed appropriately
   **********************************************************************************************
   **/
  @Override
  public ViewerPlugin previewHint(Resource resource) {
    String extension = resource.getExtension();
    if (extension.equalsIgnoreCase("hca")) {
      return (ViewerPlugin) WSPluginManager.getPlugin("Viewer", "VGMSTREAM_Audio_WAV_RIFF");
    }
    return null;
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

      long offset = 16 + (numFiles * 4) + (numFiles * 4) + 4;
      long arcSize = offset + calculatePadding(offset, 32);
      for (int i = 0; i < numFiles; i++) {
        arcSize += resources[i].getDecompressedLength();

        if (i == numFiles - 1) {
          // no padding at the end
        }
        else {
          // all other files have padding
          arcSize += calculatePadding(arcSize, 32);
        }
      }

      // Write Header Data

      // 4 - Header (AFS2)
      fm.writeString("AFS2");

      // 4 - Unknown (263170)
      fm.writeInt(263170);

      // 4 - Number of Files
      fm.writeInt(numFiles);

      // 4 - Padding Length (32)
      fm.writeInt(32);

      for (int i = 0; i < numFiles; i++) {
        // 4 - File ID (incremental from 0)
        fm.writeInt(i);
      }

      // Write Directory
      TaskProgressManager.setMessage(Language.get("Progress_WritingDirectory"));
      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];
        long decompLength = resource.getDecompressedLength();

        // 4 - File Offset [+ padding to a 32-block multiple]
        fm.writeInt((int) offset);

        offset += calculatePadding(offset, 32) + decompLength;
      }

      // 4 - Archive Length
      fm.writeInt(arcSize);

      // X - null Padding to a multiple of 32 bytes
      int paddingSize = calculatePadding(fm.getOffset(), 32);
      for (int p = 0; p < paddingSize; p++) {
        fm.writeByte(0);
      }

      // Write Files
      TaskProgressManager.setMessage(Language.get("Progress_WritingFiles"));
      for (int i = 0; i < resources.length; i++) {
        // X - File Data
        write(resources[i], fm);

        // X - null Padding to a multiple of 32 bytes
        if (i == numFiles - 1) {
          // no padding at the end of the archive
        }
        else {
          paddingSize = calculatePadding(fm.getOffset(), 32);
          for (int p = 0; p < paddingSize; p++) {
            fm.writeByte(0);
          }
        }

        TaskProgressManager.setValue(i);
      }

      fm.close();

    }
    catch (Throwable t) {
      logError(t);
    }
  }

}
