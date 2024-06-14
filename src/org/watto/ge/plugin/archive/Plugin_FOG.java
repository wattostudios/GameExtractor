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
import org.watto.Settings;
import org.watto.datatype.Archive;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.io.FileManipulator;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_FOG extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_FOG() {

    super("FOG", "FOG");

    //         read write replace rename
    setProperties(true, false, true, false);

    setGames("Syphon Filter");
    setExtensions("fog"); // MUST BE LOWER CASE
    setPlatforms("PS1");

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
      if (fm.readInt() == -2147483647) {
        rating += 50;
      }

      long arcSize = fm.getLength();

      // 4 - Archive Length (not including Footer) [*2048]
      if (FieldValidator.checkLength(fm.readInt() * 2048, arcSize)) {
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

      long arcSize = fm.getLength();

      // 4 - Unknown (1,0,0,128)
      // 4 - Archive Length (not including Footer) [*2048]
      // 8 - null
      fm.skip(16);

      int numFiles = Archive.getMaxFiles();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(arcSize);

      // Loop through directory
      int realNumFiles = 0;
      while (fm.getOffset() < arcSize) {

        // 16 - Filename (null terminated, filled with (byte)205)
        /*
        byte[] filenameBytes = fm.readBytes(16);
        for (int p = 0; p < 16; p++) {
          if (ByteConverter.unsign(filenameBytes[p]) == 205) {
            if (p == 0) {
              filenameBytes = new byte[0];
              break;
            }
            byte[] oldFilenameBytes = filenameBytes;
            filenameBytes = new byte[p - 1];
            System.arraycopy(oldFilenameBytes, 0, filenameBytes, 0, p - 1);
            break;
          }
        }
        if (filenameBytes.length <= 0) {
          // padding - end of directory
          break;
        }
        String filename = new String(filenameBytes);
        */
        String filename = fm.readNullString(16);
        if (filename.length() <= 0 || filename.charAt(0) == (char) 205) {
          // padding - end of directory
          break;
        }
        FieldValidator.checkFilename(filename);

        // 4 - File Offset
        int offset = fm.readInt() * 2048;
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - File Length
        int length = fm.readInt() * 2048;
        FieldValidator.checkLength(length, arcSize);

        //path,name,offset,length,decompLength,exporter
        resources[realNumFiles] = new Resource(path, filename, offset, length);
        realNumFiles++;

        TaskProgressManager.setValue(offset);
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

      long archiveSize = 16 + (numFiles * 24);
      archiveSize += calculatePadding(archiveSize, 2048);

      for (int i = 0; i < numFiles; i++) {
        archiveSize += resources[i].getDecompressedLength();
        archiveSize += calculatePadding(archiveSize, 2048);
      }

      archiveSize /= 2048;

      // Write Header Data
      // 4 - Unknown (1,0,0,128)
      fm.writeBytes(src.readBytes(4));

      // 4 - Archive Length (not including Footer) [*2048]
      long srcArchiveLength = src.readInt() * 2048;
      fm.writeInt(archiveSize);

      // 8 - null
      fm.writeBytes(src.readBytes(8));

      // Write Directory
      TaskProgressManager.setMessage(Language.get("Progress_WritingDirectory"));
      long offset = 16 + (numFiles * 24);
      offset += calculatePadding(offset, 2048);
      offset /= 2048;

      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];
        long length = resource.getDecompressedLength();

        // 16 - Filename (null terminated, filled with (byte)205)
        String filename = resource.getName();
        if (filename.length() > 16) {
          filename = filename.substring(0, 16);
        }
        fm.writeString(filename);
        int filenamePadding = 16 - filename.length();
        if (filenamePadding >= 1) {
          fm.writeByte(0);
          filenamePadding--;
          for (int p = 0; p < filenamePadding; p++) {
            fm.writeByte((byte) 205);
          }
        }

        // 4 - File Offset [*2048]
        fm.writeInt(offset);

        // 4 - File Length (including padding) [*2048]
        if (length % 2048 == 0) {
          length /= 2048;
        }
        else {
          length = (length / 2048) + 1;
        }
        fm.writeInt(length);

        offset += length;
      }

      int dirPadding = 16 + (numFiles * 24);
      dirPadding = calculatePadding(dirPadding, 2048);
      for (int p = 0; p < dirPadding; p++) {
        fm.writeByte((byte) 205);
      }

      // Write Files
      TaskProgressManager.setMessage(Language.get("Progress_WritingFiles"));
      for (int i = 0; i < resources.length; i++) {
        // X - File Data
        Resource resource = resources[i];
        write(resource, fm);
        TaskProgressManager.setValue(i);

        // X - Padding (byte 205) to a multiple of 2048 bytes
        int padding = calculatePadding(resource.getDecompressedLength(), 2048);
        for (int t = 0; t < padding; t++) {
          fm.writeByte((byte) 205);
        }
      }

      // X - Footer Padding (byte 205)
      int footerPadding = (int) (src.getLength() - srcArchiveLength);
      for (int p = 0; p < footerPadding; p++) {
        fm.writeByte((byte) 205);
      }

      src.close();
      fm.close();

    }
    catch (Throwable t) {
      logError(t);
    }
  }

}
