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
import java.security.MessageDigest;
import java.util.HashMap;
import org.watto.Settings;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.exporter.Exporter_Custom_RSDK_RSDK;
import org.watto.io.FileManipulator;
import org.watto.io.converter.IntConverter;
import org.watto.io.converter.ShortConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_RSDK_RSDK extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_RSDK_RSDK() {

    super("RSDK_RSDK", "RSDK_RSDK");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Sonic Mania");
    setExtensions("rsdk"); // MUST BE LOWER CASE
    setPlatforms("PC");

    // MUST BE LOWER CASE !!!
    //setFileTypes(new FileType("txt", "Text Document", FileType.TYPE_DOCUMENT),
    //             new FileType("bmp", "Bitmap Image", FileType.TYPE_IMAGE)
    //             );

    //setTextPreviewExtensions("colours", "rat", "screen", "styles"); // LOWER CASE

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
      if (fm.readString(4).equals("RSDK")) {
        rating += 25;
      }
      if (fm.readString(2).equals("v5")) {
        rating += 25;
      }

      // Number Of Files
      try {
        if (FieldValidator.checkNumFiles(fm.readShort())) {
          rating += 5;
        }
      }
      catch (Throwable t) {
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

      ExporterPlugin exporter = Exporter_Custom_RSDK_RSDK.getInstance();

      // RESETTING GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 4 - Header (RSDK)
      // 2 - Version String (v5)
      fm.skip(6);

      // 2 - Number of Files
      int numFiles = ShortConverter.unsign(fm.readShort());
      FieldValidator.checkNumFiles(numFiles);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // See if we have a file with the filenames in it, and if so, we need to read them in so the decryption works properly
      HashMap<String, String> hashMap = new HashMap<String, String>(numFiles);
      File hashFile = new File(Settings.get("HashesDirectory") + File.separatorChar + "RSDK_RSDK" + File.separatorChar + path.getName() + ".txt");
      if (hashFile.exists()) {
        int hashFileLength = (int) hashFile.length();

        MessageDigest md5gen = MessageDigest.getInstance("MD5");

        FileManipulator hashFM = new FileManipulator(hashFile, false);
        while (hashFM.getOffset() < hashFileLength) {
          String name = hashFM.readLine();
          if (name.equals("")) {
            break; // EOF
          }
          byte[] md5 = endianSwap(md5gen.digest(name.toLowerCase().getBytes("UTF-8")));
          hashMap.put(new String(md5), name);
        }
        hashFM.close();
      }

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {
        // 16 - Hash
        //Hex hash = fm.readHex(16);
        //System.out.println(hash);
        //fm.skip(16);
        byte[] hash = fm.readBytes(16);

        // 4 - File Offset
        int offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 3 - File Length
        // 1 - Encryption Flag (128=encrypted, 0=normal)
        byte[] lengthBytes = fm.readBytes(4);
        boolean encrypted = lengthBytes[3] < 0;
        lengthBytes[3] &= 63;
        int length = IntConverter.convertLittle(lengthBytes);
        FieldValidator.checkLength(length, arcSize);

        String filename = hashMap.get(new String(hash));
        if (filename == null) {
          filename = Resource.generateFilename(i);
        }

        //path,name,offset,length,decompLength,exporter
        Resource resource = new Resource(path, filename, offset, length, length);
        if (encrypted) {
          resource.setExporter(exporter);
        }
        resources[i] = resource;

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
  Does endian swapping for every 4 byte block
  **********************************************************************************************
  **/
  public byte[] endianSwap(byte[] bytes) {
    int byteLength = bytes.length;

    byte[] outBytes = new byte[byteLength];
    for (int i = 0; i < byteLength; i += 4) {
      outBytes[i + 3] = bytes[i];
      outBytes[i + 2] = bytes[i + 1];
      outBytes[i + 1] = bytes[i + 2];
      outBytes[i] = bytes[i + 3];
    }

    return outBytes;
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
