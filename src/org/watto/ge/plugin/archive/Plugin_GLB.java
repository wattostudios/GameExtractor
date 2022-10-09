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
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.exporter.Exporter_Custom_GLB;
import org.watto.io.FileManipulator;
import org.watto.io.FilenameSplitter;
import org.watto.io.converter.ByteConverter;
import org.watto.io.converter.IntConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_GLB extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_GLB() {

    super("GLB", "GLB");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Raptor: Call of the Shadows");
    setExtensions("glb"); // MUST BE LOWER CASE
    setPlatforms("PC");

    // MUST BE LOWER CASE !!!
    //setFileTypes(new FileType("txt", "Text Document", FileType.TYPE_DOCUMENT),
    //             new FileType("bmp", "Bitmap Image", FileType.TYPE_IMAGE)
    //             );

    setTextPreviewExtensions("err", "dix", "diz"); // LOWER CASE

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
      if (fm.readInt() == 164731748) {
        rating += 50;
      }

      return rating;

    }
    catch (Throwable t) {
      return 0;
    }
  }

  byte[] key = new byte[0];

  int keyPos = 0;

  int previousByte = 0;

  /**
   **********************************************************************************************
  
   **********************************************************************************************
   **/

  public byte[] decryptBytes(FileManipulator fm, int numBytes) {
    byte[] bytes = fm.readBytes(numBytes);
    for (int b = 0; b < numBytes; b++) {
      bytes[b] = decryptByte(bytes[b]);
    }
    return bytes;
  }

  /**
   **********************************************************************************************
  
   **********************************************************************************************
   **/

  public byte decryptByte(byte currentByte) {
    // 1. Subtract the character value from the current position in the encryption key (i.e. if the current position is 0, subtract 0x33, the character code for "3")
    int decryptedByte = ByteConverter.unsign(currentByte) - key[keyPos];

    // 2. Advance the position in the encryption key by one (i.e. go to the next letter)
    keyPos++;

    // 3. If the end of the encryption key has been reached, go back to the first character
    if (keyPos >= key.length) {
      keyPos = 0;
    }

    // 4. Subtract the value of the previous byte read (note the previous byte *read*, not the decrypted version of that byte)
    decryptedByte -= previousByte;

    // 5. Logical AND with 0xFF to limit the result to 0-255
    decryptedByte &= 255;

    // 6. This byte is now decoded, move on to the next
    previousByte = currentByte;

    return (byte) decryptedByte;
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

      // RESETTING GLOBAL VARIABLES

      key = new byte[] { 51, 50, 55, 54, 56, 71, 76, 66 }; //"32768GLB"
      keyPos = 25 % 8;
      previousByte = key[keyPos];

      ExporterPlugin exporter = new Exporter_Custom_GLB(key);

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 4 - null
      // 4 - Number of Files
      byte[] headerBytes = fm.readBytes(8);
      for (int b = 0; b < 8; b++) {
        headerBytes[b] = decryptByte(headerBytes[b]);
      }

      int numFiles = IntConverter.convertLittle(new byte[] { headerBytes[4], headerBytes[5], headerBytes[6], headerBytes[7] });
      FieldValidator.checkNumFiles(numFiles);

      // 4 - null
      // 16 - null
      fm.skip(20);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      String previousFilename = "";
      int filenameIncrement = 1;

      for (int i = 0; i < numFiles; i++) {
        // Reset the encryption key
        keyPos = 25 % 8;
        previousByte = key[keyPos];

        // 4 - Encryption Flag (0=not encrypted, 1=encrypted)
        byte[] encryptionBytes = decryptBytes(fm, 4);
        int encryptionFlag = IntConverter.convertLittle(encryptionBytes);

        // 4 - File Offset
        byte[] offsetBytes = decryptBytes(fm, 4);
        int offset = IntConverter.convertLittle(offsetBytes);
        FieldValidator.checkOffset(offset, arcSize + 1); // +1 to allow empty files at the end of the archive

        // 4 - File Length
        byte[] lengthBytes = decryptBytes(fm, 4);
        int length = IntConverter.convertLittle(lengthBytes);
        FieldValidator.checkLength(length, arcSize);

        // 16 - Filename (null)
        byte[] filenameBytes = decryptBytes(fm, 16);
        String filename = "";
        for (int b = 0; b < 16; b++) {
          byte currentByte = filenameBytes[b];
          if (currentByte == 0) {
            break;
          }
          filename += (char) currentByte;
        }
        //FieldValidator.checkFilename(filename);

        int underscorePos = filename.lastIndexOf('_');
        if (underscorePos > 0) {
          filename = filename.substring(0, underscorePos) + "." + filename.substring(underscorePos + 1);
        }

        if (filename.length() <= 0 || filename.equals(previousFilename)) {
          String extension = FilenameSplitter.getExtension(previousFilename);
          filename = FilenameSplitter.getFilename(previousFilename) + "_" + filenameIncrement;
          filenameIncrement++;
          if (extension.length() > 0) {
            filename += "." + extension;
          }
        }
        else {
          previousFilename = filename;
          filenameIncrement = 1;
        }

        //path,name,offset,length,decompLength,exporter
        if (encryptionFlag == 1) {
          // encrypted
          resources[i] = new Resource(path, filename, offset, length, length, exporter);
        }
        else {
          // normal file
          resources[i] = new Resource(path, filename, offset, length);
        }

        TaskProgressManager.setValue(i);
      }

      int realNumFiles = numFiles;
      resources = resizeResources(resources, realNumFiles);
      calculateFileSizes(resources, arcSize);

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
