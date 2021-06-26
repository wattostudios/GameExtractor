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
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.io.FileManipulator;
import org.watto.io.converter.ByteConverter;
import org.watto.io.converter.IntConverter;
import org.watto.io.converter.LongConverter;
import org.watto.io.converter.ShortConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_MBUNDLE_BPLIST00 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_MBUNDLE_BPLIST00() {

    super("MBUNDLE_BPLIST00", "MBUNDLE_BPLIST00");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Samurai Shodown NeoGeo Collection");
    setExtensions("mbundle"); // MUST BE LOWER CASE
    setPlatforms("PC");

    // MUST BE LOWER CASE !!!
    //setFileTypes(new FileType("txt", "Text Document", FileType.TYPE_DOCUMENT),
    //             new FileType("bmp", "Bitmap Image", FileType.TYPE_IMAGE)
    //             );

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
      if (fm.readString(8).equals("bplist00")) {
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

      fm.seek(arcSize - 24);

      // 8 - Number of Files
      int numFiles = (int) LongConverter.convertBig(fm.readBytes(8));
      FieldValidator.checkNumFiles(numFiles);

      // 8 - ID Directory Offset [+8]
      long endOfData = LongConverter.convertBig(fm.readBytes(8)) + 8;
      FieldValidator.checkOffset(endOfData, arcSize);

      // 8 - Offsets Directory Offset
      long dirOffset = LongConverter.convertBig(fm.readBytes(8));
      FieldValidator.checkOffset(dirOffset, arcSize);

      fm.seek(endOfData);

      // 2 - Unknown
      fm.skip(2);

      // 4 - Number of Filenames
      boolean doSkipCheck = true;
      int numFilenames = IntConverter.changeFormat(fm.readInt());
      if (numFilenames == 0) {
        fm.relativeSeek(endOfData);
        fm.skip(1);
        doSkipCheck = false;
        numFilenames = numFiles / 2;
      }
      FieldValidator.checkNumFiles(numFilenames);

      int numEntries = (int) (dirOffset - endOfData - 6) / 4;
      if (numEntries == 0) {
        doSkipCheck = false;
      }
      else {
        FieldValidator.checkNumFiles(numEntries);
      }
      int[] ids = new int[numEntries];
      for (int i = 0; i < numEntries; i++) {
        // 4 - File ID
        int id = IntConverter.changeFormat(fm.readInt());
        ids[i] = id;
        //System.out.println(id);
      }

      fm.seek(dirOffset);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {

        // 4 - File Offset
        long offset = IntConverter.unsign(IntConverter.changeFormat(fm.readInt()));
        FieldValidator.checkOffset(offset, arcSize);

        String filename = Resource.generateFilename(i);

        //path,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, filename, offset);

        TaskProgressManager.setValue(i);
      }

      calculateFileSizes(resources, endOfData);

      // Now go and work out the filename values
      fm.getBuffer().setBufferSize(64);

      String previousName = "";
      String[] names = new String[numFilenames];
      int currentID = numFilenames;
      int expectedID = numFilenames;
      int currentName = 0;
      for (int i = 0; i < numFilenames; i++) {
        // first, check for IDs out of sequence
        if (doSkipCheck) {
          int checkID = ids[currentID];
          if (checkID != expectedID) {
            // skip this filename - it's in the wrong order
            currentID++;
            continue;
          }
        }
        currentID++;
        expectedID++;

        Resource resource = resources[i];
        long offset = resource.getOffset();

        fm.seek(offset);

        if (ShortConverter.changeFormat(fm.readShort()) == 24338) {
          // full filename

          // 4 - Filename Length
          fm.skip(4);

          int filenameLength = (int) (resource.getLength() - 6);
          FieldValidator.checkFilenameLength(filenameLength);

          // X - Filename
          previousName = fm.readString(filenameLength);

          //System.out.println(previousName);
        }
        else {
          // filename re-use
          fm.relativeSeek(offset);

          // 1 - Re-use amount
          int reuseAmount = ByteConverter.unsign(fm.readByte()) >> 4;

          int filenameLength = (int) (resource.getLength() - 1);
          FieldValidator.checkFilenameLength(filenameLength);

          // X - Filename
          int keepCount = previousName.length() - reuseAmount;
          previousName = previousName.substring(0, keepCount) + fm.readString(filenameLength);
          //System.out.println(previousName);
        }

        names[currentName] = previousName;
        currentName++;
      }

      // Now go and set the filenames on the files
      int realNumFiles = numFiles - numFilenames - 1; // -1 because the last file is the ID directory
      Resource[] realResources = new Resource[realNumFiles];
      int currentFile = 0;

      for (int i = numFilenames; i < numFiles - 1; i++) { // -1 because the last file is the ID directory
        Resource resource = resources[i];

        // set the filename
        if (numFilenames != 0) {
          String name = names[currentFile];
          resource.setName(name);
          resource.setOriginalName(name);
        }

        // skip the 6-byte file header
        resource.setOffset(resource.getOffset() + 6);
        long length = resource.getLength() - 6;
        resource.setLength(length);
        resource.setDecompressedLength(length);

        realResources[currentFile] = resource;
        currentFile++;
      }

      resources = realResources;

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

    /*
    if (headerInt1 == 2037149520) {
      return "js";
    }
    */

    return null;
  }

}
