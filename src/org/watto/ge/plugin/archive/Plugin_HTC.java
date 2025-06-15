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
import org.watto.datatype.Archive;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.exporter.Exporter_XOR_RepeatingKey;
import org.watto.io.FileManipulator;
import org.watto.io.buffer.XORRepeatingKeyBufferWrapper;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_HTC extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_HTC() {

    super("HTC", "HTC");

    //         read write replace rename
    setProperties(true, true, false, false);

    setGames("To Serve And Command");
    setExtensions("htc"); // MUST BE LOWER CASE
    setPlatforms("PC");

    // MUST BE LOWER CASE !!!
    //setFileTypes(new FileType("txt", "Text Document", FileType.TYPE_DOCUMENT),
    //             new FileType("bmp", "Bitmap Image", FileType.TYPE_IMAGE)
    //             );

    setTextPreviewExtensions("dat"); // LOWER CASE

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

      fm.skip(1);

      if (fm.readByte() == 90) {
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

      FileManipulator fm = new FileManipulator(path, false);

      int[] key = new int[] { 97, 90, 107, 107, 82, 105, 109, 118, 67, 103, 118, 53, 97, 36, 57, 33 };

      XORRepeatingKeyBufferWrapper xorBuffer = new XORRepeatingKeyBufferWrapper(fm.getBuffer(), key);
      fm.setBuffer(xorBuffer);

      long arcSize = fm.getLength();

      // find the first offset, so we know where the directory ends
      // 2 - Filename Length
      short firstFilenameLength = fm.readShort();
      FieldValidator.checkFilenameLength(firstFilenameLength);

      // X - Filename
      fm.skip(firstFilenameLength);

      // 8 - File Offset
      long dataOffset = fm.readLong();
      FieldValidator.checkOffset(dataOffset, arcSize);

      // back to the start now, ready to read the directory properly
      fm.relativeSeek(0);
      xorBuffer.setCurrentKeyPos(0); // need to reset the key position, as the seek() won't do this

      int numFiles = Archive.getMaxFiles();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(dataOffset);

      // Loop through directory
      int realNumFiles = 0;
      while (fm.getOffset() < dataOffset) {

        // 2 - Filename Length
        short filenameLength = fm.readShort();
        FieldValidator.checkFilenameLength(filenameLength);

        // X - Filename
        String filename = fm.readString(filenameLength);
        System.out.println(filename);

        // 8 - File Offset
        long offset = fm.readLong();
        FieldValidator.checkOffset(offset, arcSize);

        // 8 - File Length
        long length = fm.readLong();
        FieldValidator.checkLength(length, arcSize);

        // store the exporter, including the offset int the key to start this file
        int keyPos = (int) offset % 16; // the repeating key is 16 bytes long
        Exporter_XOR_RepeatingKey exporter = new Exporter_XOR_RepeatingKey(key, keyPos);

        //path,name,offset,length,decompLength,exporter
        resources[realNumFiles] = new Resource(path, filename, offset, length, length, exporter);
        realNumFiles++;

        TaskProgressManager.setValue(fm.getOffset());
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
   * Writes an [archive] File with the contents of the Resources
   **********************************************************************************************
   **/
  @Override
  public void write(Resource[] resources, File path) {
    try {

      FileManipulator fm = new FileManipulator(path, true);
      int numFiles = resources.length;
      TaskProgressManager.setMaximum(numFiles);

      // set the XOR buffer - as long as we write without seeking, it'll keep repeating the key as we write.
      int[] key = new int[] { 97, 90, 107, 107, 82, 105, 109, 118, 67, 103, 118, 53, 97, 36, 57, 33 };
      XORRepeatingKeyBufferWrapper xorBuffer = new XORRepeatingKeyBufferWrapper(fm.getBuffer(), key);
      fm.setBuffer(xorBuffer);

      // Calculations
      TaskProgressManager.setMessage(Language.get("Progress_PerformingCalculations"));

      long directorySize = 0;
      long archiveSize = 0;
      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];
        directorySize += 18 + resource.getNameLength();
        archiveSize += resource.getDecompressedLength();
      }
      archiveSize += directorySize;

      // Write Directory
      TaskProgressManager.setMessage(Language.get("Progress_WritingDirectory"));
      long offset = directorySize;
      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];
        long decompLength = resource.getDecompressedLength();

        String filename = resource.getName();

        // 2 - Filename Length
        fm.writeShort(filename.length());

        // X - Filename
        fm.writeString(filename);

        // 8 - File Offset
        fm.writeLong(offset);

        // 8 - File Length
        fm.writeLong(decompLength);

        offset += decompLength;
      }

      // Write Files
      TaskProgressManager.setMessage(Language.get("Progress_WritingFiles"));
      write(resources, fm);

      // footer
      int archivePaddingSize = calculatePadding(archiveSize, 16);
      for (int i = 0; i < archivePaddingSize; i++) {
        fm.writeByte(0);
      }

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

    /*
    if (headerInt1 == 2037149520) {
      return "js";
    }
    */

    return null;
  }

}
