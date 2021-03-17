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
import org.watto.component.WSPluginManager;
import org.watto.datatype.Archive;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.ge.plugin.exporter.Exporter_XOR_RepeatingKey;
import org.watto.io.FileManipulator;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_PKG_PKGFILE extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_PKG_PKGFILE() {

    super("PKG_PKGFILE", "PKG_PKGFILE");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Order of War");
    setExtensions("pkg"); // MUST BE LOWER CASE
    setPlatforms("PC");

    //setFileTypes(new FileType("txt", "Text Document", FileType.TYPE_DOCUMENT),
    //             new FileType("bmp", "Bitmap Image", FileType.TYPE_IMAGE)
    //             );

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

      // 42 - Header (Unicode String "PKG_FILE_VERSION:0004")
      String header = fm.readUnicodeString(21);
      if (header.equals("PKG_FILE_VERSION:0004") || header.equals("PKG_FILE_VERSION:0005")) {
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
   Provide hints to the previewer so that certain document types are displayed appropriately
   **********************************************************************************************
   **/
  @Override
  public ViewerPlugin previewHint(Resource resource) {
    String extension = resource.getExtension();
    if (extension.equalsIgnoreCase("bat") || extension.equalsIgnoreCase("lua") || extension.equalsIgnoreCase("fx") || extension.equalsIgnoreCase("inc") || extension.equalsIgnoreCase("msd") || extension.equalsIgnoreCase("sdr") || extension.equalsIgnoreCase("xyz")) {
      return (ViewerPlugin) WSPluginManager.getPlugin("Viewer", "TXT");
    }
    return null;
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

      //ExporterPlugin exporter = Exporter_Default.getInstance();

      // RESETTING GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 42 - Header (Unicode String "PKG_FILE_VERSION:0004")
      String header = fm.readUnicodeString(21);

      int dirLength = 0;
      boolean encrypted = false;
      //int[] baseXorKey = new int[] { 0x5c, 0x7b, 0xe3, 0xea, 0xec, 0xac, 0x5e, 0xb8, 0x6d, 0x0b, 0xd4, 0xcd, 0xce, 0x85, 0x34, 0xea, 0x80, 0x56, 0x52, 0x86, 0x23, 0x68, 0x6a, 0x83, 0x24, 0x0b, 0xeb, 0xee, 0x4e, 0xcf, 0x15, 0x0f, 0x38, 0xbc, 0x15, 0x09, 0x79, 0xb9, 0x76, 0xa3, 0x9c, 0x85, 0xc6, 0xe1, 0xdd, 0x9c, 0xfb, 0x4d, 0xb6, 0xc3 };
      int[] baseXorKey = new int[] { 92, 123, 227, 234, 236, 172, 94, 184, 109, 11, 212, 205, 206, 133, 52, 234, 128, 86, 82, 134, 35, 104, 106, 131, 36, 11, 235, 238, 78, 207, 21, 15, 56, 188, 21, 9, 121, 185, 118, 163, 156, 133, 198, 225, 221, 156, 251, 77, 182, 195 };

      if (header.equals("PKG_FILE_VERSION:0005")) {
        // skip to the end and read the footer
        fm.seek(arcSize - 12);

        // 4 - Directory Length
        dirLength = fm.readInt() - 12;
        FieldValidator.checkLength(dirLength, arcSize);

        // 4 - Directory Offset
        int dirOffset = fm.readInt();
        FieldValidator.checkOffset(dirOffset, arcSize);

        // 4 - Encryption Flag
        if ((fm.readInt() & 0x100) == 0x100) {
          encrypted = true;
        }

        // skip to the directory
        fm.seek(dirOffset);
      }
      else {

        // skip to the end and read the footer
        fm.seek(arcSize - 8);

        // 4 - Directory Length
        dirLength = fm.readInt() - 8;
        FieldValidator.checkLength(dirLength, arcSize);

        // 4 - Directory Offset
        int dirOffset = fm.readInt();
        FieldValidator.checkOffset(dirOffset, arcSize);

        // skip to the directory
        fm.seek(dirOffset);
      }

      int numFiles = Archive.getMaxFiles();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(dirLength);

      // Loop through directory
      int realNumFiles = 0;
      int dirPos = 0;
      while (dirPos < dirLength) {

        // 4 - Entry Length
        int entryLength = fm.readInt();
        FieldValidator.checkLength(entryLength, arcSize);

        int filenameLength = (entryLength - 16) / 2;
        FieldValidator.checkFilenameLength(filenameLength);

        // 4 - File Offset
        int offset = fm.readInt();

        // 4 - File Length
        int length = fm.readInt();

        if (offset == -1 && length == -1) {
          // empty file/directory/something
          fm.skip(entryLength - 12);

          dirPos += entryLength;
          continue;
        }
        FieldValidator.checkOffset(offset, arcSize);
        FieldValidator.checkLength(length, arcSize);

        // 4 - Hash?
        fm.skip(4);

        // X - Filename (Unicode String --> length is EntryLength - 16)
        String filename = fm.readUnicodeString(filenameLength);
        FieldValidator.checkFilename(filename);

        //path,name,offset,length,decompLength,exporter
        Resource resource = new Resource(path, filename, offset, length);
        if (encrypted) {
          /*
          int[] xorKey = new int[50];
          int xorStartPos = (offset - 14) % 50;
          if (xorStartPos == 0) {
            xorKey = baseXorKey;
          }
          else {
            System.arraycopy(baseXorKey, xorStartPos, xorKey, 0, 50 - xorStartPos);
            System.arraycopy(baseXorKey, 0, xorKey, 50 - xorStartPos, xorStartPos);
          }
          if (filename.equals("user/profile.xml")) {
            System.out.println("BOO");
          }
          */
          int[] xorKey = baseXorKey;
          int xorStartPos = offset % 50;
          Exporter_XOR_RepeatingKey exporter = new Exporter_XOR_RepeatingKey(xorKey);
          exporter.setCurrentKeyPos(xorStartPos);
          resource.setExporter(exporter);
        }
        resources[realNumFiles] = resource;
        realNumFiles++;

        TaskProgressManager.setValue(dirPos);
        dirPos += entryLength;
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

}
