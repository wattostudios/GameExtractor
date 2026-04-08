/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2026 wattostudios
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

import org.watto.datatype.Archive;
import org.watto.datatype.FileType;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.helper.PaletteManager;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.resource.Resource_WAV_RawAudio;
import org.watto.io.FileManipulator;
import org.watto.io.converter.IntConverter;
import org.watto.io.converter.ShortConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_IEF_INDX extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_IEF_INDX() {

    super("IEF_INDX", "IEF_INDX");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Awesome Animated Monster Maker");
    setExtensions("ief"); // MUST BE LOWER CASE
    setPlatforms("PC");

    // MUST BE LOWER CASE !!!
    setFileTypes(new FileType("snip", "SNIP Audio", FileType.TYPE_AUDIO),
        new FileType("pixl", "Pixel Image", FileType.TYPE_IMAGE),
        new FileType("pltt", "Color Palette", FileType.TYPE_PALETTE));

    //setTextPreviewExtensions("colours", "rat", "screen", "styles"); // LOWER CASE

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

      // Header
      if (fm.readString(8).equals("INDXmeta")) {
        rating += 50;
      }

      long arcSize = fm.getLength();

      // Header Length
      if (FieldValidator.checkLength(IntConverter.changeFormat(fm.readInt()), arcSize)) {
        rating += 5;
      }

      // Number Of Folders
      if (FieldValidator.checkNumFiles(ShortConverter.changeFormat(fm.readShort()))) {
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
      PaletteManager.clear(); // clear any palettes loaded from a previous archive (for the Viewer plugins)

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 4 - Header (INDX)
      // 4 - Header (meta)
      // 4 - Header Length
      fm.skip(12);

      // 2 - Number of Folders
      int numFolders = ShortConverter.changeFormat(fm.readShort());
      FieldValidator.checkNumFiles(numFolders);

      // 2 - Unknown
      // 2 - Unknown
      // 4 - Header (INDX)
      // 4 - Header (????)
      fm.skip(12);

      int[] folderOffsets = new int[numFolders];
      for (int f = 0; f < numFolders; f++) {
        // 2 - null
        // 4 - Folder Data Length
        fm.skip(6);

        // 4 - Folder Data Offset
        int folderOffset = IntConverter.changeFormat(fm.readInt());
        FieldValidator.checkOffset(folderOffset, arcSize);
        folderOffsets[f] = folderOffset;

        // 4 - Unknown
        fm.skip(4);
      }

      int numFiles = Archive.getMaxFiles();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(arcSize);

      // Loop through directory
      int realNumFiles = 0;
      for (int f = 0; f < numFolders; f++) {
        //System.out.println(folderOffsets[f]);
        fm.seek(folderOffsets[f]);

        // 4 - Header (INDX)
        // 4 - Header (vari)
        // 4 - Folder Entry Length
        fm.skip(12);

        // 2 - Number of Files in this Folder (not including padding)
        int numFilesInFolder = ShortConverter.changeFormat(fm.readShort());
        FieldValidator.checkNumFiles(numFilesInFolder + 1); // allow empty folders

        // 2 - Number of Entries in this Folder (including padding)
        int numEntries = ShortConverter.changeFormat(fm.readShort());
        FieldValidator.checkNumFiles(numEntries);

        // 2 - Unknown
        // 4 - Header (DATA)
        fm.skip(6);

        // 4 - File Type String
        String fileType = fm.readNullString(4);

        int numFilesRemaining = numFilesInFolder;
        for (int i = 0; i < numEntries; i++) {

          // 2 - null
          fm.skip(2);

          // 4 - File Data Length
          int length = IntConverter.changeFormat(fm.readInt());
          FieldValidator.checkLength(length, arcSize);

          // 4 - File Data Offset
          int offset = IntConverter.changeFormat(fm.readInt());
          FieldValidator.checkOffset(offset, arcSize);

          // 4 - null
          fm.skip(4);

          if (length == 0 && offset == 0) {
            // empty file entry
            continue;
          }

          String filename = Resource.generateFilename(realNumFiles) + "." + fileType;

          //path,name,offset,length,decompLength,exporter
          resources[realNumFiles] = new Resource(path, filename, offset, length);

          realNumFiles++;

          numFilesRemaining--;

          TaskProgressManager.setValue(offset);

          if (numFilesRemaining <= 0) {
            break; // found all the files in this folder, move on to the next one
          }
        }

      }

      resources = resizeResources(resources, realNumFiles);

      numFiles = realNumFiles;

      // now read the filenames
      fm.getBuffer().setBufferSize(128);

      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];

        long startOffset = resource.getOffset();
        fm.seek(startOffset);

        // X - Filename
        // 1 - null Filename Terminator
        String filename = fm.readNullString();

        // X - Other data to relative offset 1352

        // X - File Data
        //long endOffset = fm.getOffset();
        long endOffset = startOffset + 1352;

        long length = resource.getLength() - (endOffset - startOffset);

        resource.setOffset(endOffset);
        resource.setLength(length);
        resource.setDecompressedLength(length);

        if (!filename.equals("Unnamed Chunk") && !filename.equals("")) {
          filename += "." + resource.getExtension();

          resource.setName(filename);
          resource.setOriginalName(filename);
        }

        if (resource.getExtension().equalsIgnoreCase("snip")) {
          // Audio file (8bit signed 22050)
          Resource_WAV_RawAudio wavResource = new Resource_WAV_RawAudio(path, filename, endOffset, length);
          /*
          wavResource.copyFrom(resource);
          
          filename += ".wav";
          
          wavResource.setName(filename);
          wavResource.setOriginalName(filename);
          */

          wavResource.setAudioProperties(22050, 8, 1, false);
          resources[i] = wavResource;
        }

      }

      fm.close();

      return resources;

    }
    catch (Throwable t) {
      logError(t);
      return null;
    }
  }

}
