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

import org.watto.datatype.FileType;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.exporter.Exporter_ZLib_CompressedSizeOnly;
import org.watto.io.FileManipulator;
import org.watto.io.buffer.ByteBuffer;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_GTX_TEXT extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_GTX_TEXT() {

    super("GTX_TEXT", "GTX_TEXT");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Armobiles");
    setExtensions("gtx");
    setPlatforms("PC");

    setFileTypes(new FileType("gtx_tex", "Texture Image", FileType.TYPE_IMAGE));

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
      if (fm.readString(4).equals("TEXT")) {
        rating += 50;
      }

      fm.skip(8);

      long arcSize = fm.getLength();

      // Directory Offset
      if (FieldValidator.checkOffset(fm.readInt(), arcSize)) {
        rating += 5;
      }

      // Number Of Files
      if (FieldValidator.checkNumFiles(fm.readInt())) {
        rating += 5;
      }

      // Number Of Files
      if (FieldValidator.checkNumFiles(fm.readInt())) {
        rating += 5;
      }

      fm.skip(4);

      // Directory Offset
      if (FieldValidator.checkOffset(fm.readInt(), arcSize)) {
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
      ExporterPlugin exporter = Exporter_ZLib_CompressedSizeOnly.getInstance();

      // RESETTING THE GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = (int) fm.getLength();

      // 4 - Header (TEXT)
      // 4 - Unknown (401)
      // 4 - Hash?
      // 4 - Mipmaps Directory Offset
      fm.skip(16);

      // 4 - Number Of Mipmaps
      int numMipmaps = fm.readInt();
      FieldValidator.checkNumFiles(numMipmaps);

      // 4 - Number Of Images (where each image is comprised of multiple mipmaps)
      int numImages = fm.readInt();
      FieldValidator.checkNumFiles(numImages);

      // 4 - Filename Directory Length
      int filenameDirLength = fm.readInt();
      FieldValidator.checkLength(filenameDirLength, arcSize);

      // 4 - Mipmaps Directory Offset
      long mipmapDirOffset = fm.readInt();
      FieldValidator.checkOffset(mipmapDirOffset, arcSize);

      // 4 - Images Directory Offset
      int imageDirOffset = fm.readInt();
      FieldValidator.checkOffset(imageDirOffset, arcSize);

      // 4 - Filename Directory Offset
      int filenameDirOffset = fm.readInt();
      FieldValidator.checkOffset(filenameDirOffset, arcSize);

      // Read the filename directory into a FM
      fm.seek(filenameDirOffset);
      byte[] filenameBytes = fm.readBytes(filenameDirLength);

      FileManipulator nameFM = new FileManipulator(new ByteBuffer(filenameBytes));

      // read the mipmaps
      fm.seek(mipmapDirOffset);

      int[] compressions = new int[numMipmaps];
      int[] offsets = new int[numMipmaps];
      int[] lengths = new int[numMipmaps];

      for (int i = 0; i < numMipmaps; i++) {
        // 1 - Compression Flag (0=Not Compressed, 1=ZLib Compression, 2=JPEG Compression)
        int compression = fm.readByte();
        compressions[i] = compression;

        // 3 - Unknown
        fm.skip(3);

        // 4 - File Offset
        int offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);
        offsets[i] = offset;

        // 4 - File Length
        int length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);
        lengths[i] = length;
      }

      fm.seek(imageDirOffset);

      Resource[] resources = new Resource[numImages];

      TaskProgressManager.setMaximum(numImages);
      // read the images (we only keep the largest mipmap of each one)      
      for (int i = 0; i < numImages; i++) {

        // 4 - Unknown
        // 4 - Unknown
        // 4 - Hash?
        fm.skip(12);

        // 2 - Largest Mipmap Width
        short width = fm.readShort();
        FieldValidator.checkWidth(width);

        // 2 - Largest Mipmap Height
        short height = fm.readShort();
        FieldValidator.checkHeight(height);

        // 4 - Filename Offset (relative to the start of the Filename Directory)
        int filenameOffset = fm.readInt();
        FieldValidator.checkOffset(filenameOffset, filenameDirLength);

        // 4 - First Mipmap That Belongs To This Image
        int mipmapID = fm.readInt();
        FieldValidator.checkRange(mipmapID, 0, numMipmaps);

        int offset = offsets[mipmapID];
        int length = lengths[mipmapID];
        int compression = compressions[mipmapID];

        nameFM.seek(filenameOffset);

        // X - Filename
        // 1 - null Filename Terminator
        String filename = nameFM.readNullString();
        FieldValidator.checkFilename(filename);

        Resource resource;

        if (compression == 2) {
          // JPEG Compression (leave as a raw file, just put a JPG on the end of the filename)
          //filename += ".jpg";
          filename += ".gtx_tex";

          //path,id,name,offset,length,decompLength,exporter
          resource = new Resource(path, filename, offset, length);

          resource.addProperty("ImageFormat", "JPEG");
        }
        else if (compression == 1) {
          // ZLib Compression
          filename += ".gtx_tex";

          //path,id,name,offset,length,decompLength,exporter
          resource = new Resource(path, filename, offset, length, length, exporter);

          resource.addProperty("ImageFormat", "BGRA");
        }
        else {
          // not compressed
          filename += ".gtx_tex";

          //path,id,name,offset,length,decompLength,exporter
          resource = new Resource(path, filename, offset, length);

          resource.addProperty("ImageFormat", "BGRA");
        }

        resource.addProperty("Width", width);
        resource.addProperty("Height", height);

        resources[i] = resource;

        TaskProgressManager.setValue(i);
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
