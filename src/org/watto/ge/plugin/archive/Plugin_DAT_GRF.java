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

import org.watto.datatype.Archive;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.exporter.Exporter_ZLib;
import org.watto.io.FileManipulator;
import org.watto.io.buffer.ByteBuffer;
import org.watto.io.converter.ByteConverter;
import org.watto.io.converter.IntConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_DAT_GRF extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_DAT_GRF() {

    super("DAT_GRF", "DAT_GRF");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Fashion Assistant",
        "CSI: NY");
    setExtensions("dat"); // MUST BE LOWER CASE
    setPlatforms("PC");

    // MUST BE LOWER CASE !!!
    //setFileTypes(new FileType("txt", "Text Document", FileType.TYPE_DOCUMENT),
    //             new FileType("bmp", "Bitmap Image", FileType.TYPE_IMAGE)
    //             );

    setTextPreviewExtensions("anim", "imagesrc", "spec", "std-imagesrc", "theme", "ui-imagesrc", "template", "emitter"); // LOWER CASE

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

      // 8 - Header ("GRF" + (byte)5 + "GRF" + (byte)1)
      if (fm.readInt() == 88494663) {
        rating += 25;
      }

      long arcSize = fm.getLength();

      int header2 = fm.readInt();
      if (header2 == 21385799) {
        rating += 25;

        // Directory Offset
        if (FieldValidator.checkOffset(fm.readInt(), arcSize)) {
          rating += 5;
        }
      }
      else if (header2 == 1196574209) {
        rating += 25;

        // Directory Offset
        if (FieldValidator.checkOffset(IntConverter.changeFormat(fm.readInt()), arcSize)) {
          rating += 5;
        }
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

      // 8 - Header ("GRF" + (byte)5 + "GRF" + (byte)1)
      fm.skip(4);
      boolean endianSwap = false;
      if (fm.readInt() == 1196574209) { // header was actually ("GRF" + (byte)5,1 + "FRG")
        endianSwap = true;
      }

      // 4 - Directory Offset
      int dirOffset = fm.readInt();
      if (endianSwap) {
        dirOffset = IntConverter.changeFormat(dirOffset);
      }
      FieldValidator.checkOffset(dirOffset, arcSize);

      fm.seek(dirOffset);

      int dirLength = (int) (arcSize - dirOffset);
      FieldValidator.checkLength(dirLength);

      int dirDecompLength = dirLength * 10; // guess

      // X - Compressed Directory Data (ZLib)
      byte[] dirBytes = new byte[dirDecompLength];
      int decompWritePos = 0;
      Exporter_ZLib exporter = Exporter_ZLib.getInstance();
      exporter.open(fm, dirLength, dirDecompLength);

      for (int b = 0; b < dirDecompLength; b++) {
        if (exporter.available()) { // make sure we read the next bit of data, if required
          dirBytes[decompWritePos++] = (byte) exporter.read();
        }
        else {
          dirDecompLength = b - 1;
          break; // EOF
        }
      }

      // open the decompressed data for processing
      fm.close();

      //FileManipulator tempFM = new FileManipulator(new File("C:\\temp.txt"), true);
      //tempFM.writeBytes(dirBytes);
      //tempFM.close();

      fm = new FileManipulator(new ByteBuffer(dirBytes));

      /*
      // 4 - Unknown
      while (fm.getOffset() < dirDecompLength) {
        if (fm.readByte() == 0) {
          if (fm.readByte() == 0) {
            if (fm.readByte() == 0) {
              if (fm.readByte() == 0) {
                // found the first file
                fm.seek(fm.getOffset() - 4);
                found = true;
                break;
              }
            }
          }
        }
      }
      */

      fm.seek(0);

      if (endianSwap) {
        // Look for "EIR"
        while (fm.getOffset() < dirDecompLength) {
          if (fm.readByte() == 1) {
            if (fm.readByte() == 69) {
              if (fm.readByte() == 73) {
                if (fm.readByte() == 82) {
                  // found the first file
                  fm.seek(fm.getOffset() - 9);
                  break;
                }
              }
            }
          }
        }
      }
      else {
        // Look for "RIE"
        while (fm.getOffset() < dirDecompLength) {
          if (fm.readByte() == 82) {
            if (fm.readByte() == 73) {
              if (fm.readByte() == 69) {
                if (fm.readByte() == 1) {
                  // found the first file
                  fm.seek(fm.getOffset() - 9);
                  break;
                }
              }
            }
          }
        }
      }

      int numFiles = Archive.getMaxFiles();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(arcSize);

      // Loop through directory
      int realNumFiles = 0;
      long[] offsets = new long[numFiles];

      if (endianSwap) {
        while (fm.getOffset() < dirDecompLength) {
          //System.out.println(fm.getOffset());
          // 1 - null
          // 4 - File ID (incremental from 0)
          // 4 - Entry Header? ((byte)1 + "EIR")
          // 4 - String Header ((byte)1 + "rtS")
          fm.skip(13);

          // 4 - Filename Length
          int filenameLength = IntConverter.changeFormat(fm.readInt());
          FieldValidator.checkFilenameLength(filenameLength);

          // X - Filename
          String filename = fm.readString(filenameLength);

          // 4 - File Offset
          int offset = IntConverter.changeFormat(fm.readInt());
          FieldValidator.checkOffset(offset, arcSize);
          offsets[realNumFiles] = offset;

          // 4 - Decompressed File Length
          int decompLength = IntConverter.changeFormat(fm.readInt());
          FieldValidator.checkLength(decompLength, arcSize);

          // 1 - Unknown (1)
          fm.skip(1);

          //path,name,offset,length,decompLength,exporter
          resources[realNumFiles] = new Resource(path, filename, offset, decompLength, decompLength, exporter);
          realNumFiles++;

          TaskProgressManager.setValue(offset);
        }
      }
      else {
        while (fm.getOffset() < dirDecompLength) {
          //System.out.println(fm.getOffset());
          // 1 - null
          // 4 - File ID (incremental from 0)
          // 4 - Entry Header? ("RIE" + (byte)1)
          // 1 - Unknown (18)
          fm.skip(10);

          // 1 - Filename Length
          int filenameLength = ByteConverter.unsign(fm.readByte());

          // X - Filename
          String filename = fm.readString(filenameLength);

          // 4 - File Offset
          int offset = fm.readInt();
          FieldValidator.checkOffset(offset, arcSize);
          offsets[realNumFiles] = offset;

          // 4 - Decompressed File Length
          int decompLength = fm.readInt();
          FieldValidator.checkLength(decompLength, arcSize);

          // 1 - Unknown (1)
          fm.skip(1);

          //path,name,offset,length,decompLength,exporter
          resources[realNumFiles] = new Resource(path, filename, offset, decompLength, decompLength, exporter);
          realNumFiles++;

          TaskProgressManager.setValue(offset);
        }
      }

      resources = resizeResources(resources, realNumFiles);

      offsets[realNumFiles] = dirOffset;

      numFiles = realNumFiles;

      // go through and set the compressed file lengths
      for (int i = 0; i < numFiles; i++) {
        resources[i].setLength(offsets[i + 1] - offsets[i]);
      }

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
