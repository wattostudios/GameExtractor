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
import org.watto.ge.helper.PaletteManager;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.exporter.Exporter_Explode;
import org.watto.ge.plugin.exporter.Exporter_Explode_CompressedSizeOnly;
import org.watto.io.FileManipulator;
import org.watto.io.converter.ByteConverter;
import org.watto.io.converter.IntConverter;
import org.watto.io.converter.ShortConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_RES_LG extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_RES_LG() {

    super("RES_LG", "RES_LG");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("System Shock",
        "System Shock: Enhanced Edition",
        "British Open Championship Golf");
    setExtensions("res");
    setPlatforms("PC");

    // MUST BE LOWER CASE !!!
    setFileTypes(new FileType("voc", "VOC Audio", FileType.TYPE_AUDIO),
        new FileType("image", "Texture Image", FileType.TYPE_IMAGE));

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
      if (fm.readString(14).equals("LG Res File v2")) {
        rating += 50;
      }

      // Unknown (26)
      if (fm.readInt() == 1706509) {
        rating += 5;
      }

      fm.skip(106);

      long arcSize = fm.getLength();

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

      ExporterPlugin exporterExplode = Exporter_Explode.getInstance();

      // RESETTING GLOBAL VARIABLES
      PaletteManager.clear();

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 16 - Header (LG Res File v2 + (byte)13,10)
      // 4 - Unknown (26)
      // 104 - null
      fm.skip(124);

      // 4 - Directory Offset
      int dirOffset = fm.readInt();
      FieldValidator.checkOffset(dirOffset, arcSize);

      fm.seek(dirOffset);

      // 2 - Number Of Files
      short numFiles = fm.readShort();
      FieldValidator.checkNumFiles(numFiles);

      // 4 - First File Offset
      int offset = fm.readInt();
      FieldValidator.checkOffset(offset);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      boolean[] chunkedFiles = new boolean[numFiles];
      for (int i = 0; i < numFiles; i++) {
        // 2 - File ID (incremental)
        int fileID = fm.readShort();

        // 4 - File Length
        byte[] decompLengthBytes = fm.readBytes(4);
        int compressionType = ByteConverter.unsign(decompLengthBytes[3]);
        decompLengthBytes[3] = 0;
        int decompLength = IntConverter.convertLittle(decompLengthBytes);
        FieldValidator.checkLength(decompLength);

        // 3 - File Length
        // 1 - File Type ID? (17=Movie, 7=Creative Voice File, 2=Image)
        byte[] lengthBytes = fm.readBytes(4);
        int fileType = ByteConverter.unsign(lengthBytes[3]);
        lengthBytes[3] = 0;
        int length = IntConverter.convertLittle(lengthBytes);
        FieldValidator.checkLength(length, arcSize);

        String extension;
        if (fileType == 0) {
          extension = ".bin";
        }
        else if (fileType == 1) {
          extension = ".string";
        }
        else if (fileType == 2) {
          extension = ".image";
        }
        else if (fileType == 3) {
          extension = ".font";
        }
        else if (fileType == 4) {
          extension = ".anim";
        }
        else if (fileType == 5) {
          extension = ".pall";
        }
        else if (fileType == 6) {
          extension = ".shadtab";
        }
        else if (fileType == 7) {
          extension = ".voc";
        }
        else if (fileType == 8) {
          extension = ".shape";
        }
        else if (fileType == 9) {
          extension = ".pict";
        }
        else if (fileType == 10) {
          extension = ".b2extern";
        }
        else if (fileType == 11) {
          extension = ".b2reloc";
        }
        else if (fileType == 12) {
          extension = ".b2code";
        }
        else if (fileType == 13) {
          extension = ".b2header";
        }
        else if (fileType == 14) {
          extension = ".b2resrvd";
        }
        else if (fileType == 15) {
          extension = ".obj3d";
        }
        else if (fileType == 16) {
          extension = ".stencil";
        }
        else if (fileType == 17) {
          extension = ".movie";
        }
        else if (fileType == 18) {
          extension = ".rect";
        }
        else if (fileType == 19) {
          extension = ".palette512";
        }
        else if (fileType == 48) {
          extension = ".map";
        }
        else {
          extension = "." + fileType;
        }

        String filename = Resource.generateFilename(i) + extension;

        int chunked = compressionType & 2;
        compressionType &= 253;

        FieldValidator.checkOffset(offset, arcSize);

        /*
        if (compressionType != 0) {
          System.out.println(compressionType + " for file at offset " + offset);
        }
        */

        if (chunked == 2) {
          // a chunked file
          chunkedFiles[i] = true;
        }
        else {
          // single (non-chunked) file
          chunkedFiles[i] = false;
        }

        if (compressionType == 1) { // 1=LZW_LG (https://github.com/smiRaphi/UniPyX/blob/8770cdaa5104d26ef704349ada7cc430b9fc1a05/lib/file.py#L316)
          // compressed
          //path,name,offset,length,decompLength,exporter
          resources[i] = new Resource(path, filename, offset, length, decompLength);
        }
        else if (compressionType == 32) { // 32=Explode
          // compressed
          //path,name,offset,length,decompLength,exporter
          resources[i] = new Resource(path, filename, offset, length, decompLength, exporterExplode);
        }
        else {
          // uncompressed
          //path,name,offset,length,decompLength,exporter
          resources[i] = new Resource(path, filename, offset, length, decompLength);
        }

        resources[i].addProperty("FileID", fileID);

        TaskProgressManager.setValue(i);

        offset += length;

        offset += calculatePadding(offset, 4);
      }

      // now go through and work out the chunks
      fm.getBuffer().setBufferSize(64); // small quick reads

      for (int i = 0; i < numFiles; i++) {
        if (chunkedFiles[i]) {
          Resource resource = resources[i];

          ExporterPlugin exporter = resource.getExporter();
          if (exporter instanceof Exporter_Explode) {
            exporter = Exporter_Explode_CompressedSizeOnly.getInstance();
          }

          long baseOffset = resource.getOffset();
          fm.seek(baseOffset);

          // 2 - Number of Chunks
          int numChunks = ShortConverter.unsign(fm.readShort());

          if (numChunks == 0) {
            //System.out.println(fm.getOffset());
            resource.setOffset(baseOffset + 6); // 2 (read above) + 4 for the file size
            resource.setExporter(exporter);
            continue;
          }

          long[] chunkOffsets = new long[numChunks];
          long[] chunkLengths = new long[numChunks];
          for (int c = 0; c < numChunks; c++) {
            // 4 - Chunk Offset (relative to the start of the file data for this file)
            long chunkOffset = baseOffset + fm.readInt();
            //FieldValidator.checkOffset(chunkOffset, arcSize);
            chunkOffsets[c] = chunkOffset;

            if (c != 0) {
              chunkLengths[c - 1] = chunkOffset - chunkOffsets[c - 1];
            }

          }

          // 4 - Total Length
          chunkLengths[numChunks - 1] = (baseOffset + fm.readInt()) - chunkOffsets[numChunks - 1];

          // X - Chunk Data
          //BlockExporterWrapper blockExporter = new BlockExporterWrapper(exporter, chunkOffsets, chunkLengths, chunkLengths);
          //resource.setExporter(blockExporter);

          resource.setOffset(chunkOffsets[0]);
          resource.setExporter(exporter);
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
