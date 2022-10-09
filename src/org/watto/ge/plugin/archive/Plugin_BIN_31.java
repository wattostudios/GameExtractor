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
import org.watto.Language;
import org.watto.datatype.FileType;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.exporter.BlockVariableExporterWrapper;
import org.watto.ge.plugin.exporter.Exporter_Custom_VAG_Audio;
import org.watto.ge.plugin.exporter.Exporter_Default;
import org.watto.ge.plugin.resource.Resource_WAV_RawAudio;
import org.watto.io.FileManipulator;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_BIN_31 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_BIN_31() {

    super("BIN_31", "BIN_31");

    //         read write replace rename
    setProperties(true, true, false, false);

    setGames("Driver 2");
    setExtensions("bin"); // MUST BE LOWER CASE
    setPlatforms("PSX");

    // MUST BE LOWER CASE !!!
    setFileTypes(new FileType("xm", "XM Audio", FileType.TYPE_AUDIO));

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

      // First File Offset
      if (FieldValidator.checkNumFiles((fm.readInt() / 4) - 1)) {
        rating += 5;
      }

      // Second File Offset
      if (FieldValidator.checkOffset(fm.readInt())) {
        rating += 5;
      }

      rating += 1; // so it's slightly more preferred over BIN_12, as this one will throw out better than BIN_12 will, if it's not the right format

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

      int currentOffset = fm.readInt();
      FieldValidator.checkOffset(currentOffset, arcSize);

      int numFiles = (currentOffset / 4) - 1;
      FieldValidator.checkNumFiles(numFiles);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {

        // 4 - File Offset
        int nextOffset = fm.readInt();
        FieldValidator.checkOffset(nextOffset, arcSize + 1);

        int offset = currentOffset;

        int length = nextOffset - currentOffset;
        FieldValidator.checkLength(length, arcSize);

        currentOffset = nextOffset;

        String filename = Resource.generateFilename(i);

        //path,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, filename, offset, length);

        TaskProgressManager.setValue(i);
      }

      // Now, the files come in pairs - the first is the start of the XM audio file, the second is a set of VAG-compressed audio files.
      // Lets see if we can join them together.

      ExporterPlugin exporterVAG = new Exporter_Custom_VAG_Audio(false);

      int realNumFiles = numFiles / 2;
      Resource[] realResources = new Resource[realNumFiles];
      realNumFiles = 0;
      for (int i = 0; i < numFiles; i += 2) {
        Resource resource = resources[i];
        Resource vagResource = resources[i + 1];

        long vagOffset = vagResource.getOffset();
        long vagLength = vagResource.getLength();

        fm.seek(vagOffset);

        // 4 - Number of Blocks
        int numBlocks = fm.readInt();
        FieldValidator.checkNumFiles(numBlocks);

        long[] blockOffsets = new long[numBlocks + 1];
        long[] blockLengths = new long[numBlocks + 1];
        ExporterPlugin[] blockExporters = new ExporterPlugin[numBlocks + 1];

        blockOffsets[0] = resource.getOffset();
        blockLengths[0] = resource.getLength();
        blockExporters[0] = Exporter_Default.getInstance();

        vagOffset += 4 + (numBlocks * 16);

        for (int b = 0; b < numBlocks; b++) {
          // 4 - Block Offset (relative to the start of the file data for this file)
          long offset = vagOffset + fm.readInt();
          FieldValidator.checkOffset(offset, arcSize);

          // 4 - Block Length
          int length = fm.readInt();
          FieldValidator.checkLength(length, vagLength);

          // 4 - null
          // 4 - Frequency (44100)
          fm.skip(8);

          blockOffsets[b + 1] = offset;
          blockLengths[b + 1] = length;
          blockExporters[b + 1] = exporterVAG;
        }

        BlockVariableExporterWrapper blockExporter = new BlockVariableExporterWrapper(blockExporters, blockOffsets, blockLengths, blockLengths);

        Resource_WAV_RawAudio realResource = new Resource_WAV_RawAudio(resource.getSource(), resource.getName(), resource.getOffset(), resource.getLength());
        realResource.setExporter(blockExporter);
        realResource.setAudioProperties(44100, 8, 1);
        realResources[realNumFiles] = realResource;
        realNumFiles++;
      }

      fm.close();

      //return resources;
      return realResources;

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

      // Write Directory
      TaskProgressManager.setMessage(Language.get("Progress_WritingDirectory"));
      long offset = (numFiles * 4) + 4;
      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];
        long decompLength = resource.getDecompressedLength();

        // 4 - File Offset
        fm.writeInt((int) offset);

        offset += decompLength;
      }

      fm.writeInt(offset);

      // Write Files
      TaskProgressManager.setMessage(Language.get("Progress_WritingFiles"));
      write(resources, fm);

      fm.close();

    }
    catch (Throwable t) {
      logError(t);
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

    if (headerInt1 == 1702131781) {
      return "xm";
    }

    return null;
  }

}
