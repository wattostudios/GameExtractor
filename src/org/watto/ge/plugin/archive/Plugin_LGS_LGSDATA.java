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
import org.watto.ErrorLogger;
import org.watto.Language;
import org.watto.Settings;
import org.watto.component.WSPluginException;
import org.watto.datatype.Archive;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.exporter.Exporter_Deflate;
import org.watto.io.FileManipulator;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_LGS_LGSDATA extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_LGS_LGSDATA() {

    super("LGS_LGSDATA", "LGS_LGSDATA");

    //         read write replace rename
    setProperties(true, false, true, false);

    setGames("Weird War");
    setExtensions("lgs"); // MUST BE LOWER CASE
    setPlatforms("PC");

    // MUST BE LOWER CASE !!!
    //setFileTypes(new FileType("txt", "Text Document", FileType.TYPE_DOCUMENT),
    //             new FileType("bmp", "Bitmap Image", FileType.TYPE_IMAGE)
    //             );

    setTextPreviewExtensions("adf", "snd"); // LOWER CASE

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
      if (fm.readString(8).equals("LGS Data")) {
        rating += 50;
      }

      return rating;

    }
    catch (Throwable t) {
      return 0;
    }
  }

  boolean hasShortHeader = false;

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

      ExporterPlugin exporter = Exporter_Deflate.getInstance();

      // RESETTING GLOBAL VARIABLES
      hasShortHeader = false;

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 8 - Header ("LGS Data")
      // 2 - null
      // 4 - Unknown
      // 12 - null
      fm.skip(26);

      // 4 - Root Directory Name Length
      int rootLength = fm.readInt();
      try {
        FieldValidator.checkFilenameLength(rootLength);
      }
      catch (Throwable t) {
        fm.relativeSeek(0);

        // back to the beginning
        rootLength = 0;

        hasShortHeader = true; // for Replacing
      }

      // X - Root Directory Name
      fm.skip(rootLength);

      //
      // PKZIP FILE FROM HERE ON
      //

      int numFiles = Archive.getMaxFiles();
      Resource[] resources = new Resource[numFiles];

      TaskProgressManager.setMaximum(arcSize);

      int realNumFiles = 0;
      while (fm.getOffset() < arcSize) {
        // 2 - Header (PK)
        fm.skip(2);

        // 4 - Entry Type (1311747 = File Entry)
        int entryType = fm.readInt();
        if (entryType == 1311747 || entryType == 1631854675) { // added 1631854675 for file AnimDesc.lgs
          // File Entry

          // 2 - Unknown (2)
          fm.skip(2);

          // 2 - Compression Method
          short compType = fm.readShort();

          // 8 - Checksum?
          fm.skip(8);

          // 4 - Compressed File Size
          int length = fm.readInt();
          FieldValidator.checkLength(length, arcSize);

          // 4 - Decompressed File Size
          int decompLength = fm.readInt();
          FieldValidator.checkLength(decompLength);

          // 2 - Filename Length
          int filenameLength = fm.readShort();
          FieldValidator.checkFilenameLength(filenameLength);

          // 2 - Extra Data Length
          int extraLength = fm.readShort();
          FieldValidator.checkLength(extraLength, arcSize);

          // X - Filename
          String filename = fm.readString(filenameLength);

          // X - Extra Data
          fm.skip(extraLength);

          // X - File Data
          long offset = fm.getOffset();
          fm.skip(length);

          if (compType == 0) {
            // uncompressed

            //path,name,offset,length,decompLength,exporter
            resources[realNumFiles] = new Resource(path, filename, offset, length);
          }
          else {
            // compressed - probably Deflate

            //path,name,offset,length,decompLength,exporter
            resources[realNumFiles] = new Resource(path, filename, offset, length, decompLength, exporter);
          }
          realNumFiles++;

          TaskProgressManager.setValue(offset);

        }
        else if (entryType == 513 || entryType == 1638913 || entryType == 1311233) {
          // Directory Entry

          // 2 - Unknown (20)
          // 2 - Unknown (2)
          // 2 - Unknown (8)
          // 8 - Checksum?
          // 4 - Compressed File Size
          // 4 - Decompressed File Size
          fm.skip(22);

          // 4 - Filename Length
          int filenameLength = fm.readShort();
          fm.skip(2);
          FieldValidator.checkFilenameLength(filenameLength);

          // 10 - null
          // 4 - File Offset (points to PK for this file in the directory)
          fm.skip(14);

          // X - Filename
          fm.skip(filenameLength);

        }
        else if (entryType == 656387) {
          // Directory Entry (Short) (or sometimes a file)

          // 2 - Unknown (20)
          fm.skip(2);

          // 2 - Unknown (2)
          short compType = fm.readShort();

          // 8 - Checksum?
          fm.skip(8);

          // 4 - Compressed File Size
          int length = fm.readInt();
          FieldValidator.checkLength(length, arcSize);

          // 4 - Decompressed File Size
          int decompLength = fm.readInt();
          FieldValidator.checkLength(decompLength);

          // 4 - Filename Length
          int filenameLength = fm.readShort();
          FieldValidator.checkFilenameLength(filenameLength);

          // 2 - Extra Data Length
          int extraLength = fm.readShort();
          FieldValidator.checkLength(extraLength, arcSize);

          // X - Filename
          String filename = fm.readString(filenameLength);

          // X - Extra Data
          fm.skip(extraLength);

          // X - File Data
          if (length != 0) {
            long offset = fm.getOffset();
            fm.skip(length);

            if (compType == 0) {
              // uncompressed

              //path,name,offset,length,decompLength,exporter
              resources[realNumFiles] = new Resource(path, filename, offset, length);
            }
            else {
              // compressed - probably Deflate

              //path,name,offset,length,decompLength,exporter
              resources[realNumFiles] = new Resource(path, filename, offset, length, decompLength, exporter);
            }
            realNumFiles++;

            TaskProgressManager.setValue(offset);
          }

        }
        else if (entryType == 1541) {
          // EOF Entry

          // 2 - null
          // 8 - Checksum?
          // 4 - Length Of File Data (archive size excluding the directory)
          // 2 - null
          fm.skip(16);
        }
        else if (entryType == 17491) {
          // Footer of LGS File

          // 16 - Unknown
          fm.skip(16);
        }
        else {
          // bad header
          String errorMessage = "[LGS_LGSDATA]: Manual read: Unknown entry type " + entryType + " at offset " + (fm.getOffset() - 6);
          if (realNumFiles >= 5) {
            // we found a number of files, so lets just return them, it might be a "prematurely-short" archive.
            ErrorLogger.log(errorMessage);
            break;
          }
          else {
            throw new WSPluginException(errorMessage);
          }
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
   * Writes an [archive] File with the contents of the Resources. The archive is written using
   * data from the initial archive - it isn't written from scratch.
   **********************************************************************************************
   **/
  @Override
  public void replace(Resource[] resources, File path) {
    try {

      FileManipulator fm = new FileManipulator(path, true);
      FileManipulator src = new FileManipulator(new File(Settings.getString("CurrentArchive")), false);

      long arcSize = src.getLength();

      int numFiles = resources.length;
      TaskProgressManager.setMaximum(numFiles);

      // Write Header Data

      if (hasShortHeader) {
        // Short Header
      }
      else {
        // Long Header

        // 8 - Header ("LGS Data")
        // 2 - null
        // 4 - Unknown
        // 12 - null
        fm.writeBytes(src.readBytes(26));

        // 4 - Root Directory Name Length
        int rootLength = src.readInt();
        fm.writeInt(rootLength);

        // X - Root Directory Name
        fm.writeBytes(src.readBytes(rootLength));
      }

      // Write Directory and Files
      TaskProgressManager.setMessage(Language.get("Progress_WritingFiles"));
      int currentFile = 0;
      long dirOffset = 0;
      while (src.getOffset() < arcSize) {

        // 2 - Header (PK)
        fm.writeBytes(src.readBytes(2));

        // 4 - Entry Type (1311747 = File Entry)
        int entryType = src.readInt();
        fm.writeInt(entryType);

        if (entryType == 1311747 || entryType == 656387 || entryType == 1631854675) { // added 1631854675 for file AnimDesc.lgs
          // File Entry
          Resource resource = resources[currentFile];
          currentFile++;

          // 2 - Unknown (2)
          fm.writeBytes(src.readBytes(2));

          // 2 - Compression Method
          short compType = src.readShort();
          if (resource.isReplaced()) {
            // uncompressed
            fm.writeShort(0);
          }
          else {
            // leave as is
            fm.writeShort(compType);
          }

          // 8 - Checksum?
          fm.writeBytes(src.readBytes(8));

          // 4 - Compressed File Size
          // 4 - Decompressed File Size
          int srcLength = src.readInt();
          int srcDecompLength = src.readInt();

          if (entryType == 656387 && srcLength == 0) {
            // entry directory entry
            fm.writeInt(srcLength);
            fm.writeInt(srcDecompLength);
          }
          else {

            if (resource.isReplaced()) {
              // replaced (and uncompressed)
              int length = (int) resource.getLength();

              fm.writeInt(length);
              fm.writeInt(length);
            }
            else {
              // leave as is
              fm.writeInt(srcLength);
              fm.writeInt(srcDecompLength);
            }
          }

          // 2 - Filename Length
          int srcFilenameLength = src.readShort();
          fm.writeShort(srcFilenameLength);

          // 2 - Extra Data Length
          int srcExtraLength = src.readShort();
          fm.writeShort(srcExtraLength);

          // X - Filename
          fm.writeBytes(src.readBytes(srcFilenameLength));

          // X - Extra Data
          fm.writeBytes(src.readBytes(srcExtraLength));

          // X - File Data
          if (srcLength != 0) {
            if (resource.isReplaced()) {
              // Replaced
              src.skip(srcLength);

              write(resource, fm);
            }
            else {
              // copy file as is
              fm.writeBytes(src.readBytes(srcLength));
            }
          }
          else if (entryType == 656387 && srcLength == 0) {
            // empty directory entry
            currentFile--; // we didn't store this in the array when reading in, so we haven't actually processed this file, so go back 1
          }

          TaskProgressManager.setValue(currentFile);

        }
        else if (entryType == 513 || entryType == 1638913 || entryType == 1311233) {
          // Directory Entry
          if (dirOffset == 0) {
            dirOffset = fm.getOffset();
          }

          // 2 - Unknown (20)
          // 2 - Unknown (2)
          // 2 - Unknown (8)
          // 8 - Checksum?
          // 4 - Compressed File Size
          // 4 - Decompressed File Size
          fm.writeBytes(src.readBytes(22));

          // 2 - Filename Length
          int srcFilenameLength = src.readShort();
          fm.writeShort(srcFilenameLength);

          // 2 - Unknown
          // 10 - null
          // 4 - File Offset (points to PK for this file in the directory)
          fm.writeBytes(src.readBytes(16));

          // X - Filename
          fm.writeBytes(src.readBytes(srcFilenameLength));

        }
        else if (entryType == 1541) {
          // EOF Entry

          // 2 - null
          // 8 - Checksum?
          // 4 - Length Of File Data (archive size excluding the directory)
          // 2 - null
          fm.writeBytes(src.readBytes(16));
        }
        else if (entryType == 17491) {
          // Footer of LGS File

          // 10 - Unknown
          fm.writeBytes(src.readBytes(10));

          // 4 - Directory Offset
          fm.writeInt((int) dirOffset);
          src.skip(4);

          // 2 - null
          fm.writeBytes(src.readBytes(2));
        }
        else {
          // bad header
          String errorMessage = "[LGS_LGSDATA]: Manual read: Unknown entry type " + entryType + " at offset " + (src.getOffset() - 6);
          throw new WSPluginException(errorMessage);
        }

      }

      src.close();
      fm.close();

    }
    catch (Throwable t) {
      logError(t);
    }
  }

}
