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

import org.watto.Language;
import org.watto.datatype.FileType;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.io.FileManipulator;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_PAK_89 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_PAK_89() {

    super("PAK_89", "PAK_89");

    //         read write replace rename
    setProperties(true, true, false, true);

    setGames("RTL Ski Jumping 2001",
        "RTL Skispringen 2002");
    setExtensions("pak"); // MUST BE LOWER CASE
    setPlatforms("PC");

    // MUST BE LOWER CASE !!!
    setFileTypes(new FileType("bma", "Bitmap Image", FileType.TYPE_IMAGE),
        new FileType("bmc", "Bitmap Image", FileType.TYPE_IMAGE),
        new FileType("bmp", "Bitmap Image", FileType.TYPE_IMAGE),
        new FileType("es", "Mesh and Texture Archive", FileType.TYPE_ARCHIVE));

    setTextPreviewExtensions("bat"); // LOWER CASE

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

      long arcSize = fm.getLength();

      int length1 = fm.readInt();
      if (FieldValidator.checkLength(length1, arcSize)) {
        rating += 5;
      }

      fm.skip(4);

      int paddedLength = length1 + calculatePadding(length1, 2048);
      if (fm.readInt() == paddedLength) {
        rating += 5;
      }

      int length2 = fm.readInt();
      if (FieldValidator.checkLength(length2, arcSize)) {
        rating += 5;
      }

      if (length1 == length2) {
        rating += 5;
      }

      if (length1 == 0 || length2 == 0) {
        rating -= 10; // to reduce false positives
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

      // work out the max number of files
      fm.skip(4);

      int numFiles = (fm.readInt() * 2048) / 64;
      FieldValidator.checkNumFiles(numFiles);

      fm.relativeSeek(0);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      int realNumFiles = 0;
      for (int i = 0; i < numFiles; i++) {

        // 4 - File Length (not including padding)
        int length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - File Offset [*2048]
        int offset = fm.readInt() * 2048;
        FieldValidator.checkOffset(offset, arcSize);

        if (length == 0 || offset == 0) {
          // padding at the end of the directory
          break;
        }

        // 4 - File Length (including Padding)
        // 4 - File Length (not including padding)
        fm.skip(8);

        // 48 - Filename (null terminated, filled with nulls)
        byte[] filenameBytes = fm.readBytes(48);
        int filenameLength = 48;
        for (int b = 47; b > 0; b--) {
          if (filenameBytes[b] != 0) {
            filenameLength = b + 1;
            break;
          }
        }
        byte[] shortFilenameBytes = new byte[filenameLength];
        System.arraycopy(filenameBytes, 0, shortFilenameBytes, 0, filenameLength);
        String filename = new String(shortFilenameBytes, "Cp1252");
        FieldValidator.checkFilename(filename);

        //path,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, filename, offset, length);
        realNumFiles++;

        TaskProgressManager.setValue(i);
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

      // Calculations
      TaskProgressManager.setMessage(Language.get("Progress_PerformingCalculations"));

      long offset = numFiles * 64;
      int dirPadding = calculatePadding(offset, 2048);
      offset += dirPadding;

      // Write Directory
      TaskProgressManager.setMessage(Language.get("Progress_WritingDirectory"));
      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];
        long decompLength = resource.getDecompressedLength();

        // 4 - File Length (not including padding)
        fm.writeInt((int) decompLength);

        // 4 - File Offset [*2048]
        fm.writeInt(offset / 2048);

        // 4 - File Length (including Padding)
        long paddedLength = decompLength + calculatePadding(decompLength, 2048);
        fm.writeInt((int) paddedLength);

        // 4 - File Length (not including padding)
        fm.writeInt((int) decompLength);

        // 48 - Filename (null terminated, filled with nulls)
        //fm.writeNullString(resource.getName(), 48);
        String filename = resource.getName();
        byte[] filenameBytes = filename.getBytes("Cp1252");
        int filenameLength = filenameBytes.length;
        if (filenameLength > 48) {
          filenameLength = 48;
        }
        for (int b = 0; b < filenameLength; b++) {
          fm.writeByte(filenameBytes[b]);
        }
        for (int p = filenameLength; p < 48; p++) {
          fm.writeByte(0); // padding
        }

        offset += paddedLength;
      }

      // X - null Padding to a multiple of 2048 bytes
      for (int p = 0; p < dirPadding; p++) {
        fm.writeByte(0);
      }

      // Write Files
      TaskProgressManager.setMessage(Language.get("Progress_WritingFiles"));
      write(resources, fm, 2048);

      //ExporterPlugin exporter = new Exporter_ZLib();
      //long[] compressedLengths = write(exporter,resources,fm);

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
