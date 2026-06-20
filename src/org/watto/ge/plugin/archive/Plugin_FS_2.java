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
import java.util.HashMap;

import org.watto.Language;
import org.watto.Settings;
import org.watto.datatype.Archive;
import org.watto.datatype.FileType;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.io.FileManipulator;
import org.watto.io.converter.ByteConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_FS_2 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_FS_2() {

    super("FS_2", "FS_2");

    //         read write replace rename
    setProperties(true, false, true, false);

    setGames("Disney's Hercules", "Disney's Tarzan");
    setExtensions("fs", "fsd");
    setPlatforms("PC");

    // MUST BE LOWER CASE !!!
    setFileTypes(new FileType("esf", "ESF Audio", FileType.TYPE_AUDIO),
        new FileType("egf", "EGF Image", FileType.TYPE_IMAGE),
        new FileType("tex", "TEX Image Archive", FileType.TYPE_ARCHIVE));

    setCanScanForFileTypes(true);

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

      fm.skip(4);

      long arcSize = fm.getLength();

      // First File Offset
      if (FieldValidator.checkOffset(fm.readInt(), arcSize)) {
        rating += 5;
      }

      // First File Length
      if (FieldValidator.checkLength(fm.readInt(), arcSize)) {
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

      // RESETTING THE GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = (int) fm.getLength();

      // See if we have a file with the filenames in it, and if so, we need to read them in so the decryption works properly
      HashMap<Integer, String> hashMap = new HashMap<Integer, String>(1500); // set initial size
      File hashFile = new File(Settings.get("HashesDirectory") + File.separatorChar + "FS_2" + File.separatorChar + "filenames.txt");
      if (hashFile.exists()) {
        int hashFileLength = (int) hashFile.length();

        FileManipulator hashFM = new FileManipulator(hashFile, false);
        while (hashFM.getOffset() < hashFileLength) {
          String name = hashFM.readLine();
          if (name.equals("")) {
            break; // EOF
          }

          byte[] nameBytes = name.getBytes();
          int numBytes = nameBytes.length;

          int o = 0;
          int shft = 0;
          int lng = 0;

          for (int h = 0; h < numBytes; h++) {
            o += ByteConverter.unsign(nameBytes[h]) << shft;
            shft += 8;
            if (shft > 24) {
              shft = 0;
            }
            lng += 1;
          }

          int hash = (o + lng);// & 0xFFFFFFFF;
          //System.out.println(hash + "\t" + name);

          if (name.startsWith("T:\\")) {
            name = name.substring(3);
          }

          hashMap.put(hash, name);
        }
        hashFM.close();
      }

      int numFiles = Archive.getMaxFiles();

      Resource[] resources = new Resource[numFiles];

      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      int realNumFiles = 0;
      boolean endOfDir = false;
      while (!endOfDir) {
        // 4 - Hash
        int hash = fm.readInt();

        // 4 - File Offset
        long offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - File Length
        long length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        if (offset == 0 && length == 0) {
          endOfDir = true;
          if (realNumFiles < 5) {
            return null; /// probably some other format
          }
        }
        else {
          String filename = hashMap.get(hash);
          if (filename == null || filename.length() <= 0) {
            filename = Resource.generateFilename(realNumFiles);
          }

          //path,id,name,offset,length,decompLength,exporter
          resources[realNumFiles] = new Resource(path, filename, offset, length);

          TaskProgressManager.setValue(realNumFiles);
          realNumFiles++;
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

      int numFiles = resources.length;
      TaskProgressManager.setMaximum(numFiles);

      // Calculations
      TaskProgressManager.setMessage(Language.get("Progress_PerformingCalculations"));

      // lets find the earliest offset, and store the hashes as we go...
      int[] hashes = new int[numFiles];
      int earliestOffset = (int) src.getLength();

      for (int i = 0; i < numFiles; i++) {
        // 4 - Filename Hash
        int hash = src.readInt();
        hashes[i] = hash;

        // 4 - File Offset
        int offset = src.readInt();
        if (offset < earliestOffset) {
          earliestOffset = offset;
        }

        // 4 - File Length
        src.skip(4);
      }

      // Write Directory
      TaskProgressManager.setMessage(Language.get("Progress_WritingDirectory"));
      long offset = earliestOffset;
      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];
        long length = resource.getDecompressedLength();

        // 4 - Filename Hash
        fm.writeInt(hashes[i]);

        // 4 - File Offset
        fm.writeInt(offset);

        // 4 - File Length
        fm.writeInt(length);

        offset += length;
      }

      // Padding between dir and file data
      int padding = earliestOffset - (numFiles * 12);
      for (int p = 0; p < padding; p++) {
        fm.writeByte(0);
      }

      // Write Files
      TaskProgressManager.setMessage(Language.get("Progress_WritingFiles"));
      write(resources, fm);

      src.close();
      fm.close();

    }
    catch (Throwable t) {
      logError(t);
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

    String extension = resource.getExtension();
    if (extension != null && extension.length() > 0) {
      return extension;
    }

    if (headerInt1 == 38160197) {
      return "EGF";
    }
    else if (headerInt1 == 1114133) {
      return "GEO";
    }
    else if (headerInt1 == 5) {
      return "TEX";
    }

    return null;
  }

}
