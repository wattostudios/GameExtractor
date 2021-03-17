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
import org.watto.datatype.FileType;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.exporter.Exporter_ZLib;
import org.watto.io.FileManipulator;
import org.watto.io.buffer.ByteBuffer;
import org.watto.io.converter.ByteConverter;
import org.watto.io.converter.IntConverter;
import org.watto.io.converter.ShortConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_P_DFPF extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_P_DFPF() {

    super("P_DFPF", "P_DFPF");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Brutal Legend",
        "Costume Quest",
        "Headlander");
    setExtensions("~p"); // MUST BE LOWER CASE
    setPlatforms("PC");

    //setEnabled(false); // Too complex to be worried about it

    setFileTypes(new FileType("texture", "Texture Image", FileType.TYPE_IMAGE));

    setTextPreviewExtensions("prototyperesource"); // LOWER CASE

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

      getDirectoryFile(fm.getFile(), "~h");
      rating += 25;

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

      ExporterPlugin exporter = Exporter_ZLib.getInstance();

      // RESETTING GLOBAL VARIABLES

      long arcSize = (int) path.length();

      File sourcePath = getDirectoryFile(path, "~h");
      FileManipulator fm = new FileManipulator(sourcePath, false);

      long dirSize = fm.getLength();

      // 4 - Header (dfpf)
      fm.skip(4);

      // 4 - Version? (5) (LITTLE ENDIAN)
      int version = fm.readByte();

      // 4 - null
      fm.skip(7);

      // 4 - Type Names Directory Offset (2048)
      int typeNamesDirOffset = IntConverter.changeFormat(fm.readInt());
      FieldValidator.checkOffset(typeNamesDirOffset, dirSize);

      // 4 - null
      fm.skip(4);

      // 4 - Filename Directory Offset
      int filenameDirOffset = IntConverter.changeFormat(fm.readInt());
      FieldValidator.checkOffset(filenameDirOffset, dirSize);

      // 4 - Number of Types
      int numTypes = IntConverter.changeFormat(fm.readInt());
      FieldValidator.checkNumFiles(numTypes);

      // 4 - Filename Directory Length
      int filenameDirLength = IntConverter.changeFormat(fm.readInt());
      FieldValidator.checkLength(filenameDirLength, dirSize);

      // 4 - Number of Files
      int numFiles = IntConverter.changeFormat(fm.readInt());
      FieldValidator.checkNumFiles(numFiles);

      // 4 - Unknown
      // 12 - null
      // 4 - Unknown
      // 4 - null
      fm.skip(24);

      // 4 - Details Directory Offset
      int detailsDirOffset = IntConverter.changeFormat(fm.readInt());
      FieldValidator.checkOffset(detailsDirOffset, dirSize);

      // 4 - null
      // 4 - Padding Directory Offset
      // 4 - null
      // 4 - Header File Length [+16]
      // 4 - Unknown (1)
      // 4 - Unknown
      // 0-2047 - null Padding to a multiple of 2048 bytes
      fm.seek(typeNamesDirOffset);

      String[] types = new String[numTypes];
      for (int i = 0; i < numTypes; i++) {
        // 4 - Type Name Length (including null terminator)
        int typeNameLength = IntConverter.changeFormat(fm.readInt());

        // X - Type Name
        // 1 - null Type Name Terminator
        String type = fm.readNullString(typeNameLength);
        types[i] = type;

        // 4 - Unknown
        // 4 - Hash?
        // 4 - null
        fm.skip(12);
      }

      fm.seek(filenameDirOffset);

      // Loop through directory
      /*
       * String[] filenames = new String[numFiles];
      while (dirPos < filenameDirLength) {
        // X - Filename (null)
        String filename = fm.readNullString();
        FieldValidator.checkFilename(filename);
      
        dirPos += filename.length() + 1;
      
        filenames[numNames] = filename;
        numNames++;
      }
      */

      byte[] nameDirBytes = fm.readBytes(filenameDirLength);
      FileManipulator nameFM = new FileManipulator(new ByteBuffer(nameDirBytes));

      fm.seek(detailsDirOffset);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      if (version == 2) {
        // VERSION 2

        for (int i = 0; i < numFiles; i++) {
          // 3 - Decompressed File Length
          byte[] decompLengthBytes = new byte[] { 0, fm.readByte(), fm.readByte(), fm.readByte() };
          int decompLength = IntConverter.convertBig(decompLengthBytes) + 36;

          // 3 - Unknown
          byte[] next3bytes = new byte[] { 0, fm.readByte(), fm.readByte(), fm.readByte() };

          // 2 - Compressed File Length (Version 2)
          byte[] lengthBytes = new byte[] { 0, (byte) (next3bytes[3] & 7), fm.readByte(), fm.readByte() };
          int length = IntConverter.convertBig(lengthBytes) >> 1;

          // 3 - File Offset
          byte[] offsetBytes = new byte[] { 0, fm.readByte(), fm.readByte(), fm.readByte() };
          int offset = IntConverter.convertBig(offsetBytes) << 5;

          // 1 - null
          fm.skip(1);

          // 3 - Filename Offset
          // 1 - File Type and Compression Type
          byte[] last4bytes = fm.readBytes(4);

          byte[] nameOffsetBytes = new byte[] { 0, last4bytes[0], last4bytes[1], last4bytes[2] };
          int nameOffset = IntConverter.convertBig(nameOffsetBytes) >> 3;

          int fileType = ShortConverter.convertBig(new byte[] { (byte) (last4bytes[2] & 7), last4bytes[3] }) >> 4;

          int compressionType = last4bytes[3] & 15;

          // Do the checks
          FieldValidator.checkOffset(offset, arcSize);
          FieldValidator.checkLength(length, arcSize);
          FieldValidator.checkLength(decompLength);

          //String filename = filenames[i];
          nameFM.seek(nameOffset);
          String filename = nameFM.readNullString();
          //String filename = Resource.generateFilename(i);

          filename += "." + types[fileType];

          if (compressionType == 4) {
            // ZLib

            //path,name,offset,length,decompLength,exporter
            resources[i] = new Resource(path, filename, offset, length, decompLength, exporter);
          }
          else {
            // No compression?

            //path,name,offset,length,decompLength,exporter
            resources[i] = new Resource(path, filename, offset, length, decompLength);
          }

          TaskProgressManager.setValue(i);
        }
      }
      else if (version == 6) {
        // VERSION 6

        for (int i = 0; i < numFiles; i++) {
          // 4 - Decompressed File Length
          int decompLength = IntConverter.changeFormat(fm.readInt());

          // 2 - Filename Offset
          int nameOffset = ShortConverter.unsign(ShortConverter.changeFormat(fm.readShort()));

          nameOffset |= ((decompLength & 31) << 16);
          decompLength >>= 5;

          FieldValidator.checkRange(nameOffset, 0, filenameDirLength);

          // 2 - Flags
          fm.skip(2);

          // 3 - File Offset
          byte[] offsetBytes = new byte[] { 0, fm.readByte(), fm.readByte(), fm.readByte() };
          int offset = IntConverter.convertBig(offsetBytes) << 7;

          // 1 - extraOffset
          int extraOffset = ByteConverter.unsign(fm.readByte()) >> 1;
          offset += extraOffset;

          // 4 - Compressed File Length
          //int length = IntConverter.changeFormat(fm.readInt());
          byte[] lengthBytes = new byte[] { 0, fm.readByte(), fm.readByte(), fm.readByte() };
          int length = IntConverter.convertBig(lengthBytes);

          // 1 - File Type and Compression Type
          int lastByte = ByteConverter.unsign(fm.readByte());

          int fileType = lastByte >> 2;

          int compressionType = lastByte & 3;

          // Do the checks
          FieldValidator.checkOffset(offset, arcSize);
          FieldValidator.checkLength(length, arcSize);
          FieldValidator.checkLength(decompLength);

          //String filename = filenames[i];
          nameFM.seek(nameOffset);
          String filename = nameFM.readNullString();
          //String filename = Resource.generateFilename(i);

          filename += "." + types[fileType];

          if (compressionType == 2) {
            // ZLib

            //path,name,offset,length,decompLength,exporter
            resources[i] = new Resource(path, filename, offset, length, decompLength, exporter);
          }
          else {
            // No compression?

            //path,name,offset,length,decompLength,exporter
            resources[i] = new Resource(path, filename, offset, length, decompLength);
          }

          TaskProgressManager.setValue(i);
        }
      }
      else {
        // VERSION 5

        for (int i = 0; i < numFiles; i++) {
          // 3 - Decompressed File Length
          byte[] decompLengthBytes = new byte[] { 0, fm.readByte(), fm.readByte(), fm.readByte() };
          int decompLength = IntConverter.convertBig(decompLengthBytes) + 36;

          // 3 - Filename Offset
          byte[] nameOffsetBytes = new byte[] { 0, fm.readByte(), fm.readByte(), fm.readByte() };
          int nameOffset = IntConverter.convertBig(nameOffsetBytes) >> 3;

          // 2 - Flags (Version 5)
          fm.skip(2);

          // 3 - File Offset
          byte[] offsetBytes = new byte[] { 0, fm.readByte(), fm.readByte(), fm.readByte() };
          int offset = IntConverter.convertBig(offsetBytes) << 5;

          // 1 - null
          fm.skip(1);

          // 3 - Compressed File Length (Version 5) (version 2 = Unknown)
          // 1 - File Type and Compression Type
          byte[] last4bytes = fm.readBytes(4);

          byte[] lengthBytes = new byte[] { 0, last4bytes[0], last4bytes[1], (byte) (last4bytes[2] & 240) };
          int length = IntConverter.convertBig(lengthBytes) >> 4;

          int fileType = ShortConverter.convertBig(new byte[] { (byte) (last4bytes[2] & 7), last4bytes[3] }) >> 5;

          int compressionType = last4bytes[3] & 15;

          // Do the checks
          FieldValidator.checkOffset(offset, arcSize);
          FieldValidator.checkLength(length, arcSize);
          FieldValidator.checkLength(decompLength);

          //String filename = filenames[i];
          nameFM.seek(nameOffset);
          String filename = nameFM.readNullString();
          //String filename = Resource.generateFilename(i);

          filename += "." + types[fileType];

          if (compressionType == 8) {
            // ZLib

            //path,name,offset,length,decompLength,exporter
            resources[i] = new Resource(path, filename, offset, length, decompLength, exporter);
          }
          else if (compressionType == 4) {
            // ???

            //path,name,offset,length,decompLength,exporter
            resources[i] = new Resource(path, filename, offset, length, decompLength);
          }
          else {
            // No compression?

            //path,name,offset,length,decompLength,exporter
            resources[i] = new Resource(path, filename, offset, length, decompLength);
          }

          TaskProgressManager.setValue(i);
        }
      }

      nameFM.close();
      fm.close();

      return resources;

    }
    catch (Throwable t) {
      logError(t);
      return null;
    }
  }

}
