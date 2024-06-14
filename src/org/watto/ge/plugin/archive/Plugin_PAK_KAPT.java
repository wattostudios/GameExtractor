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

import org.watto.ErrorLogger;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.exporter.Exporter_ZLib;
import org.watto.ge.plugin.exporter.Exporter_ZLibX;
import org.watto.io.FileManipulator;
import org.watto.io.buffer.ByteBuffer;
import org.watto.io.converter.ByteConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_PAK_KAPT extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_PAK_KAPT() {

    super("PAK_KAPT", "PAK_KAPT");

    // read write replace rename
    setProperties(true, false, false, false);

    setGames("MorphX");
    setExtensions("pak"); // MUST BE LOWER CASE
    setPlatforms("PC");

    // MUST BE LOWER CASE !!!
    // setFileTypes(new FileType("txt", "Text Document", FileType.TYPE_DOCUMENT),
    // new FileType("bmp", "Bitmap Image", FileType.TYPE_IMAGE)
    // );

    setTextPreviewExtensions("mel", "map", "mdf", "wga", "fx", "mmh", "mmp", "mmt", "ps", "vs", "anims", "model", "skins", "psys"); // LOWER CASE

    // setCanScanForFileTypes(true);

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
      if (fm.readString(4).equals("KAPT")) {
        rating += 50;
      }

      if (fm.readInt() == 2) {
        rating += 5;
      }

      // Number Of Files
      if (FieldValidator.checkNumFiles(fm.readInt())) {
        rating += 5;
      }

      long arcSize = fm.getLength();

      // Directory Length
      if (FieldValidator.checkLength(fm.readInt())) {
        rating += 5;
      }

      // Directory Length
      if (FieldValidator.checkLength(fm.readInt(), arcSize)) {
        rating += 5;
      }

      return rating;

    } catch (Throwable t) {
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
      // - Uncompressed files MUST know their LENGTH

      addFileTypes();

      ExporterPlugin exporter = Exporter_ZLib.getInstance();

      // RESETTING GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 4 - Header (KAPT)
      // 4 - Unknown (2)
      fm.skip(8);

      // 4 - Number of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // 4 - Decompressed Directory Length
      int decompDirLength = fm.readInt();
      FieldValidator.checkLength(decompDirLength);

      // 4 - Compressed Directory Length
      int compDirLength = fm.readInt();
      FieldValidator.checkLength(compDirLength, arcSize);

      // 4 - Unknown
      fm.skip(4);

      // X - Compressed Directory (ZLibX Compression)
      byte[] compFilenameDirBytes = fm.readBytes(compDirLength);

      FileManipulator decompNameFM = new FileManipulator(new ByteBuffer(compFilenameDirBytes));

      byte[] decompNameDirBytes = new byte[decompDirLength];
      int decompNameWritePos = 0;
      Exporter_ZLibX exporterZLibX = Exporter_ZLibX.getInstance();
      exporterZLibX.open(decompNameFM, compDirLength, decompDirLength);
      exporterZLibX.setContinueUntilDecompLength(true);

      for (int b = 0; b < decompDirLength; b++) {
        if (exporterZLibX.available()) { // make sure we read the next bit of data, if required
          decompNameDirBytes[decompNameWritePos++] = (byte) exporterZLibX.read();
        }
      }

      decompNameFM.close();
      decompNameFM = new FileManipulator(new ByteBuffer(decompNameDirBytes));

      // 4 - Decompressed Directory Length
      decompDirLength = fm.readInt();
      FieldValidator.checkLength(decompDirLength);

      // 4 - Compressed Directory Length
      compDirLength = fm.readInt();
      FieldValidator.checkLength(compDirLength, arcSize);

      // X - Compressed Directory (ZLibX Compression)
      byte[] compDirBytes = fm.readBytes(compDirLength);

      long relOffset = fm.getOffset();

      FileManipulator decompFM = new FileManipulator(new ByteBuffer(compDirBytes));

      byte[] decompDirBytes = new byte[decompDirLength];
      int decompWritePos = 0;
      exporterZLibX = new Exporter_ZLibX();
      exporterZLibX.open(decompFM, compDirLength, decompDirLength);
      exporterZLibX.setContinueUntilDecompLength(true);

      for (int b = 0; b < decompDirLength; b++) {
        if (exporterZLibX.available()) { // make sure we read the next bit of data, if required
          decompDirBytes[decompWritePos++] = (byte) exporterZLibX.read();
        }
      }

      decompFM.close();

      // open the decompressed data for processing
      fm.close();
      fm = new FileManipulator(new ByteBuffer(decompDirBytes));

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {

        // 4 - Decompressed Length
        int decompLength = fm.readInt();
        FieldValidator.checkLength(decompLength);

        // 4 - Compressed Length
        int length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - File Offset (relative to the start of the File Data)
        long offset = fm.readInt() + relOffset;
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - Filename Offset (relative to the start of the decompressed filename
        // directory)
        int filenameOffset = fm.readInt();
        FieldValidator.checkOffset(filenameOffset);

        // 4 - Filename Length
        int filenameLength = fm.readInt();
        FieldValidator.checkFilenameLength(filenameLength);

        decompNameFM.relativeSeek(filenameOffset);
        byte[] filenameBytes = decompNameFM.readBytes(filenameLength);

        /*
        int xorKey = archiveXorKey;
        xorKey ^= ByteConverter.unsign(filenameBytes[0]);
        
        for (int fb = 0; fb < filenameLength; fb++) {
          filenameBytes[fb] = (byte) (filenameBytes[fb] ^ (byte) xorKey);
          xorKey += 2;
          if (xorKey >= 256) {
            xorKey -= 256;
          }
        }
        */

        // First try with the "dot" and a 3-character file extension 
        int dotPos = filenameLength - 4;
        int xorKey = ByteConverter.unsign(filenameBytes[dotPos]) ^ 46;
        xorKey -= (dotPos * 2);
        if (xorKey < 0) {
          xorKey = 256 + xorKey;
        }

        byte[] newFilenameBytes = new byte[filenameLength];

        boolean badFilename = false;
        //int origXorKey = xorKey;
        for (int fb = 0; fb < filenameLength; fb++) {
          byte currentByte = (byte) (filenameBytes[fb] ^ (byte) xorKey);
          if (currentByte < 32) { // ie we only want between 32-127
            // not an ASCII character
            badFilename = true;
          }
          newFilenameBytes[fb] = currentByte;
          xorKey += 2;
          if (xorKey >= 256) {
            xorKey -= 256;
          }
        }

        if (badFilename) {
          badFilename = false;
          // Try with the "dot" and a 2-character file extension
          dotPos = filenameLength - 3;
          xorKey = ByteConverter.unsign(filenameBytes[dotPos]) ^ 46;
          xorKey -= (dotPos * 2);
          if (xorKey < 0) {
            xorKey = 256 + xorKey;
          }

          for (int fb = 0; fb < filenameLength; fb++) {
            byte currentByte = (byte) (filenameBytes[fb] ^ (byte) xorKey);
            if (currentByte < 32) { // ie we only want between 32-127
              // not an ASCII character
              badFilename = true;
            }
            newFilenameBytes[fb] = currentByte;
            xorKey += 2;
            if (xorKey >= 256) {
              xorKey -= 256;
            }
          }
        }

        if (badFilename) {
          badFilename = false;
          // Try with the "dot" and a 1-character file extension
          dotPos = filenameLength - 2;
          xorKey = ByteConverter.unsign(filenameBytes[dotPos]) ^ 46;
          xorKey -= (dotPos * 2);
          if (xorKey < 0) {
            xorKey = 256 + xorKey;
          }

          for (int fb = 0; fb < filenameLength; fb++) {
            byte currentByte = (byte) (filenameBytes[fb] ^ (byte) xorKey);
            if (currentByte < 32) { // ie we only want between 32-127
              // not an ASCII character
              badFilename = true;
            }
            newFilenameBytes[fb] = currentByte;
            xorKey += 2;
            if (xorKey >= 256) {
              xorKey -= 256;
            }
          }
        }

        if (badFilename) {
          badFilename = false;
          // Try with the "dot" and a 4-character file extension
          dotPos = filenameLength - 5;
          xorKey = ByteConverter.unsign(filenameBytes[dotPos]) ^ 46;
          xorKey -= (dotPos * 2);
          if (xorKey < 0) {
            xorKey = 256 + xorKey;
          }

          for (int fb = 0; fb < filenameLength; fb++) {
            byte currentByte = (byte) (filenameBytes[fb] ^ (byte) xorKey);
            if (currentByte < 32) { // ie we only want between 32-127
              // not an ASCII character
              badFilename = true;
            }
            newFilenameBytes[fb] = currentByte;
            xorKey += 2;
            if (xorKey >= 256) {
              xorKey -= 256;
            }
          }
        }

        if (badFilename) {
          badFilename = false;
          // Try with the "dot" and a 5-character file extension
          dotPos = filenameLength - 6;
          xorKey = ByteConverter.unsign(filenameBytes[dotPos]) ^ 46;
          xorKey -= (dotPos * 2);
          if (xorKey < 0) {
            xorKey = 256 + xorKey;
          }

          for (int fb = 0; fb < filenameLength; fb++) {
            byte currentByte = (byte) (filenameBytes[fb] ^ (byte) xorKey);
            if (currentByte < 32) { // ie we only want between 32-127
              // not an ASCII character
              badFilename = true;
            }
            newFilenameBytes[fb] = currentByte;
            xorKey += 2;
            if (xorKey >= 256) {
              xorKey -= 256;
            }
          }
        }

        /*
        if (badFilename) {
          badFilename = false;
          // Try with the "dot" and a 6-character file extension
          dotPos = filenameLength - 7;
          xorKey = ByteConverter.unsign(filenameBytes[dotPos]) ^ 46;
          xorKey -= (dotPos * 2);
          if (xorKey < 0) {
            xorKey = 256 + xorKey;
          }
        
          for (int fb = 0; fb < filenameLength; fb++) {
            byte currentByte = (byte) (filenameBytes[fb] ^ (byte) xorKey);
            if (currentByte < 32) { // ie we only want between 32-127
              // not an ASCII character
              badFilename = true;
            }
            newFilenameBytes[fb] = currentByte;
            xorKey += 2;
            if (xorKey >= 256) {
              xorKey -= 256;
            }
          }
        }
        
        if (badFilename) {
          badFilename = false;
          // Try with the "dot" and a 7-character file extension
          dotPos = filenameLength - 8;
          xorKey = ByteConverter.unsign(filenameBytes[dotPos]) ^ 46;
          xorKey -= (dotPos * 2);
          if (xorKey < 0) {
            xorKey = 256 + xorKey;
          }
        
          for (int fb = 0; fb < filenameLength; fb++) {
            byte currentByte = (byte) (filenameBytes[fb] ^ (byte) xorKey);
            if (currentByte < 32) { // ie we only want between 32-127
              // not an ASCII character
              badFilename = true;
            }
            newFilenameBytes[fb] = currentByte;
            xorKey += 2;
            if (xorKey >= 256) {
              xorKey -= 256;
            }
          }
        }*/

        /*
        if (badFilename) {
          badFilename = false;
        
          if (lowerPath.equals("models.pak")) {
            xorKey = ByteConverter.unsign(filenameBytes[0]) ^ 109; // "m"odel
          }
        
          for (int fb = 0; fb < filenameLength; fb++) {
            byte currentByte = (byte) (filenameBytes[fb] ^ (byte) xorKey);
            if (currentByte < 32) { // ie we only want between 32-127
              // not an ASCII character
              badFilename = true;
            }
            newFilenameBytes[fb] = currentByte;
            xorKey += 2;
            if (xorKey >= 256) {
              xorKey -= 256;
            }
          }
        }*/

        String filename = new String(newFilenameBytes);
        //System.out.println(origXorKey + "\t" + filename);

        // 4 - Compression Flag (0=uncompressed, 1=Zlib Compression)
        int compFlag = fm.readInt();

        // 8 - Hash?
        fm.skip(8);

        //filename = Resource.generateFilename(i);

        if (compFlag == 0) {
          // path,name,offset,length,decompLength,exporter
          resources[i] = new Resource(path, filename, offset, length);
        } else if (compFlag == 1) {
          // path,name,offset,length,decompLength,exporter
          resources[i] = new Resource(path, filename, offset, length, decompLength, exporter);
        } else {
          ErrorLogger.log("[PAK_KAPT] Unknown Compression Flag: " + compFlag);
        }

        TaskProgressManager.setValue(i);
      }

      decompNameFM.close();

      fm.close();

      return resources;

    } catch (Throwable t) {
      logError(t);
      return null;
    }
  }

  /**
   **********************************************************************************************
   * If an archive doesn't have filenames stored in it, the scanner can come here
   * to try to work out what kind of file a Resource is. This method allows the
   * plugin to provide additional plugin-specific extensions, which will be tried
   * before any standard extensions.
   * 
   * @return null if no extension can be determined, or the extension if one can
   *         be found
   **********************************************************************************************
   **/
  @Override
  public String guessFileExtension(Resource resource, byte[] headerBytes, int headerInt1, int headerInt2, int headerInt3, short headerShort1, short headerShort2, short headerShort3, short headerShort4, short headerShort5, short headerShort6) {

    /*
     * if (headerInt1 == 2037149520) { return "js"; }
     */

    return null;
  }

}
