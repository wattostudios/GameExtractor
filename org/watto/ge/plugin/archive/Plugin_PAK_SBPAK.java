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
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.exporter.Exporter_XOR;
import org.watto.io.FileManipulator;
import org.watto.io.FilenameSplitter;
import org.watto.io.buffer.ByteBuffer;
import org.watto.io.buffer.XORBufferWrapper;
import org.watto.io.converter.IntConverter;
import org.watto.io.converter.StringConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_PAK_SBPAK extends ArchivePlugin {

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Plugin_PAK_SBPAK() {

    super("PAK_SBPAK", "PAK_SBPAK");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("ArcaniA: Gothic 4",
        "Helldorado");
    setExtensions("pak"); // MUST BE LOWER CASE
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

      // 13 - Header ("SBPAK V 1.0" + (byte)13,10)
      if (fm.readString(11).equals("SBPAK V 1.0")) {
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

      // Everything _EXCEPT_ for the Archive Header is XOR with (byte)182.
      FileManipulator origFM = new FileManipulator(path, false);
      FileManipulator fm = new FileManipulator(new XORBufferWrapper(origFM.getBuffer(), 182));

      long arcSize = fm.getLength();

      // 13 - Header ("SBPAK V 1.0" + (byte)13,10)
      // 1 - null Header Terminator
      // 2 - Unknown (64)
      // 4 - null
      // 4 - Unknown
      // 4 - Unknown
      // 4 - Unknown (65536)
      // 4 - Unknown
      fm.skip(36);

      // 4 - Number of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // 4 - Unknown (57)
      fm.skip(4);

      // 4 - File Data Offset
      int fileDataOffset = fm.readInt();
      FieldValidator.checkOffset(fileDataOffset, arcSize);

      // 4 - Archive Length
      fm.skip(4);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      int filenameDirOffset = (int) (fm.getOffset() + (numFiles * 16));

      // Loop through directory
      int[] filenameOffsets = new int[numFiles];
      for (int i = 0; i < numFiles; i++) {
        // 4 - File Length
        int length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - File Offset (relative to the start of the File Data)
        int offset = fm.readInt() + fileDataOffset;
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - Filename Offset (relative to the start of the Filename Directory)
        //int filenameOffset = fm.readInt();
        byte[] filenameOffsetBytes = new byte[] { fm.readByte(), fm.readByte(), fm.readByte(), 0 };
        fm.skip(1);

        int filenameOffset = IntConverter.convertLittle(filenameOffsetBytes);
        FieldValidator.checkOffset(filenameOffset, arcSize);
        filenameOffsets[i] = filenameOffset;

        // 2 - Unknown (30)
        // 2 - Unknown
        fm.skip(4);

        //path,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, "", offset, length);

        TaskProgressManager.setValue(i);
      }

      // decrypt the filenames
      // NOTE: WE USE origFM HERE BECAUSE WE APPLY OUR OWN SPECIAL DECRYPTION!!!
      //origFM.seek(filenameDirOffset);
      fm.seek(filenameDirOffset);

      int filenameDirLength = fileDataOffset - filenameDirOffset;
      //byte[] filenameBytes = origFM.readBytes(filenameDirLength);
      byte[] filenameBytes = fm.readBytes(filenameDirLength);

      int xorPos = filenameDirLength;
      for (int i = 0; i < filenameDirLength; i++) {
        int currentByte = filenameBytes[i];
        currentByte ^= 0xc4;

        /*
        // uncomment the following 5 lines if you get wrong filenames!
        if ((xorPos & 1) == 1) {
          currentByte ^= 0x6e;   // 'n'   you could modify these values setting your language
        }
        else {
          currentByte ^= 0x65;   // 'e'   you could modify these values setting your language
        }
        */

        currentByte += xorPos;
        xorPos -= 1;
        filenameBytes[i] = (byte) currentByte;
      }

      /*
      // work out the filename lengths
      int[] filenameLengths = new int[numFiles];
      System.arraycopy(filenameOffsets, 0, filenameLengths, 0, numFiles);
      Arrays.sort(filenameLengths);

      int[] sortedFilenameOffsets = new int[numFiles];
      System.arraycopy(filenameLengths, 0, sortedFilenameOffsets, 0, numFiles);

      for (int i = 0; i < numFiles - 1; i++) {
        int filenameLength = filenameLengths[i + 1] - filenameLengths[i];
        if (filenameLength == -1) {
          filenameLength = 0;
        }
        else {
          FieldValidator.checkLength(filenameLength, arcSize);
        }

        filenameLengths[i] = filenameLength;
      }

      int lastFilenameLength = filenameDirLength - filenameLengths[numFiles - 1];
      FieldValidator.checkLength(lastFilenameLength, arcSize);
      filenameLengths[numFiles - 1] = lastFilenameLength;

      // set the filenames
      String parentDirectory = "";
      String nullString = new String(new byte[] { 0 });
      for (int i = 0; i < numFiles; i++) {
        int filenameOffset = filenameOffsets[i];
        int filenameLength = filenameLengths[i];

        if (filenameOffset != sortedFilenameOffsets[i]) {
          int arrayPos = Arrays.binarySearch(sortedFilenameOffsets, filenameOffset);
          if (arrayPos >= 0) {
            filenameLength = filenameLengths[arrayPos];
          }
        }

        // X - Filename
        String filename;
        if (filenameLength == 0) {
          filename = Resource.generateFilename(i);
        }
        else {
          filenameOffset += 2;
          filenameLength -= 3;

          byte[] thisFilenameBytes = new byte[filenameLength];
          System.arraycopy(filenameBytes, filenameOffset, thisFilenameBytes, 0, filenameLength);
          filename = new String(thisFilenameBytes);
          filename = StringConverter.reverse(filename);
        }

        int dirPos = filename.indexOf(nullString);
        if (dirPos > 0) {
          parentDirectory = filename.substring(0, dirPos);
          filename = filename.substring(dirPos + 1);
        }

        if (!parentDirectory.equals("")) {
          filename = parentDirectory + "\\" + filename;
        }


        Resource resource = resources[i];
        resource.setName(filename);
        resource.setOriginalName(filename);
      }
      */

      Exporter_XOR exporter = new Exporter_XOR(182);

      FileManipulator nameFM = new FileManipulator(new ByteBuffer(filenameBytes));
      for (int i = 0; i < numFiles; i++) {
        int filenameOffset = filenameOffsets[i];
        nameFM.seek(filenameOffset + 2);

        // X - Filename
        String filename = StringConverter.reverse(nameFM.readNullString());

        Resource resource = resources[i];
        resource.setName(filename);
        resource.setOriginalName(filename);

        if (!FilenameSplitter.getExtension(filename).equalsIgnoreCase("bik")) {
          // except for BIK files, all other file data is XOR'd with (byte)182;
          resource.setExporter(exporter);
        }

      }
      nameFM.close();

      fm.close();
      origFM.close();

      return resources;

    }
    catch (Throwable t) {
      logError(t);
      return null;
    }
  }

}
