/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2025 wattostudios
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
import org.watto.ge.plugin.exporter.Exporter_Encryption_XXTEA;
import org.watto.ge.plugin.exporter.Exporter_ZLib;
import org.watto.ge.plugin.exporter.Exporter_ZLib_XXTEA;
import org.watto.io.FileManipulator;
import org.watto.io.buffer.ByteBuffer;
import org.watto.task.TaskProgressManager;
import org.xxtea.XXTEA;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_OBB_3 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_OBB_3() {

    super("OBB_3", "OBB_3");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Littlest Pet Shop");
    setExtensions("obb"); // MUST BE LOWER CASE
    setPlatforms("Android");

    // MUST BE LOWER CASE !!!
    setFileTypes(new FileType("pvr", "PVR Image", FileType.TYPE_IMAGE),
        new FileType("rk", "RK Mesh", FileType.TYPE_MODEL));

    setTextPreviewExtensions("bak", "cg", "dae", "frag", "glsl", "hlsl", "inl", "manifest", "particle", "pem", "rkgs", "rkm", "rkps", "rkvs", "sprite", "sql", "vsh", "xib"); // LOWER CASE

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

      // Number Of Files
      if (FieldValidator.checkNumFiles(fm.readInt())) {
        rating += 5;
      }

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

      byte[] key = new byte[] { 79, (byte) 148, 50, 1, (byte) 161, 91, 2, 0, 79, (byte) 148, 50, 1, (byte) 181, (byte) 136, (byte) 153, 0 };

      ExporterPlugin exporterZLib = Exporter_ZLib.getInstance();
      ExporterPlugin encryptionXXTEA = new Exporter_Encryption_XXTEA(key);
      ExporterPlugin exporterZLibXXTEA = new Exporter_ZLib_XXTEA(key);

      // RESETTING GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 4 - Number Of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // 4 - Directory Offset
      int dirOffset = fm.readInt();
      FieldValidator.checkOffset(dirOffset, arcSize);

      fm.seek(dirOffset);

      // Decrypt the directory
      int dirLength = (int) arcSize - dirOffset;
      byte[] dirBytes = fm.readBytes(dirLength);
      dirBytes = XXTEA.decrypt(dirBytes, key);

      fm.close();

      //FileManipulator tempFM = new FileManipulator(new File("c:\\out.tmp"), true);
      //tempFM.writeBytes(dirBytes);
      //tempFM.close();

      fm = new FileManipulator(new ByteBuffer(dirBytes));

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {

        // 64 - Filename
        String filename = fm.readNullString(64);
        FieldValidator.checkFilename(filename);

        // 64 - Directory
        String directory = fm.readNullString(64);
        //FieldValidator.checkFilename(directory);

        filename = directory + filename;

        // 4 - File Offset
        int offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - Decompressed Length
        int decompLength = fm.readInt();
        FieldValidator.checkLength(decompLength);

        // 4 - Compressed Length
        int length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - Encrypted Length
        int encryptedLength = fm.readInt();
        FieldValidator.checkLength(encryptedLength);

        // 4 - Timestamp
        // 16 - Checksum (MD5)
        // 4 - Unknown
        fm.skip(24);

        if (encryptedLength != 0) {
          if (length == decompLength) {
            // encrypted only

            //path,name,offset,length,decompLength,exporter
            resources[i] = new Resource(path, filename, offset, length, decompLength, encryptionXXTEA);
          }
          else {
            // encrypted and compressed

            //path,name,offset,length,decompLength,exporter
            resources[i] = new Resource(path, filename, offset, length, decompLength, exporterZLibXXTEA);
          }
        }
        else {
          if (length == decompLength) {
            // raw
            resources[i] = new Resource(path, filename, offset, length);
          }
          else {
            // compressed only

            //path,name,offset,length,decompLength,exporter
            resources[i] = new Resource(path, filename, offset, length, decompLength, exporterZLib);
          }
        }

        TaskProgressManager.setValue(i);
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
