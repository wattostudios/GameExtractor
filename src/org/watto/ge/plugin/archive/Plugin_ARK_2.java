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
import java.util.HashMap;

import org.watto.Language;
import org.watto.Settings;
import org.watto.datatype.FileType;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.exporter.Exporter_Default;
import org.watto.ge.plugin.exporter.Exporter_LZSS_Old;
import org.watto.io.FileManipulator;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_ARK_2 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_ARK_2() {

    super("ARK_2", "ARK_2");

    //         read write replace rename
    setProperties(true, false, true, false);

    //allowImplicitReplacing = true;

    setGames("MotoGP: Ultimate Racing Technology",
        "MotoGP: Ultimate Racing Technology 2",
        "MotoGP: Ultimate Racing Technology 3");
    setExtensions("ark");
    setPlatforms("PC");

    // MUST BE LOWER CASE !!!
    setFileTypes(new FileType("tex", "Texture Image", FileType.TYPE_IMAGE),
        new FileType("msg", "Language File", FileType.TYPE_DOCUMENT),
        new FileType("vso", "Vertex Shader", FileType.TYPE_OTHER),
        new FileType("pso", "Pixel Shader", FileType.TYPE_OTHER),
        new FileType("font", "Font", FileType.TYPE_OTHER),
        new FileType("mesh", "Mesh", FileType.TYPE_MODEL));

    setTextPreviewExtensions("params"); // LOWER CASE

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

      // null
      if (fm.readInt() == 0) {
        rating += 5;
      }

      long arcSize = fm.getLength();

      // First File Offset
      if (FieldValidator.checkOffset(fm.readInt() + 8, arcSize)) {
        rating += 5;
      }

      // Number Of Files
      if (FieldValidator.checkNumFiles(fm.readInt())) {
        rating += 5;
      }

      fm.skip(4);

      // null
      if (fm.readInt() == 0) {
        rating += 5;
      }

      // First File Offset
      if (FieldValidator.checkOffset(fm.readInt() + 8, arcSize)) {
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
      ExporterPlugin exporter = Exporter_LZSS_Old.getInstance();

      // RESETTING THE GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = (int) fm.getLength();

      // 4 - null
      // 4 - First File Offset [+8]
      fm.skip(8);

      // 4 - Number Of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // 4 - Unknown (16)
      // 4 - null
      // 4 - First File Offset [+8]
      fm.skip(12);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // See if we have a file with the filenames in it, and if so, we need to read them in so the decryption works properly
      HashMap<String, String> hashMap = new HashMap<String, String>(numFiles);
      File hashFile = new File(Settings.get("HashesDirectory") + File.separatorChar + "ARK_2" + File.separatorChar + "filenames.txt");
      if (hashFile.exists()) {
        int hashFileLength = (int) hashFile.length();

        char equalSign = '=';

        FileManipulator hashFM = new FileManipulator(hashFile, false);
        while (hashFM.getOffset() < hashFileLength) {
          String name = hashFM.readLine();
          if (name.equals("")) {
            break; // EOF
          }

          int equalPos = name.indexOf(equalSign);
          if (equalPos <= 0) {
            break; // EOF
          }

          String hash = name.substring(0, equalPos);
          String filename = name.substring(equalPos + 1);

          hashMap.put(hash, filename);
        }
        hashFM.close();
      }

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {
        // 4 - Compression Tag? (0=Decompressed 1=Compressed)
        int compressed = fm.readInt();

        // 4 - File Offset
        long offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - File Length
        long length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - Hash?
        //fm.skip(4);
        String hash = fm.readHex(4).toString();
        /*
        System.out.println(hash);
        
        String ext = "cmp";
        if (compressed == 0) {
          ext = "nml";
        }
        
        String filename = Resource.generateFilename(i) + "." + ext;
        */
        String filename = hashMap.get(hash);
        if (filename == null) {
          filename = Resource.generateFilename(i) + "-" + hash;
        }

        if (compressed == 0) {
          // raw file

          //path,id,name,offset,length,decompLength,exporter
          resources[i] = new Resource(path, filename, offset, length, length);
        }
        else {
          // Compressed

          //path,id,name,offset,length,decompLength,exporter
          resources[i] = new Resource(path, filename, offset, length, length * 5, exporter);
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
  
  **********************************************************************************************
  **/
  @Override
  @SuppressWarnings("unused")
  public void write(Resource[] resources, File path) {
    try {

      ExporterPlugin exporter = Exporter_LZSS_Old.getInstance();

      FileManipulator fm = new FileManipulator(path, true);
      FileManipulator src = new FileManipulator(new File(Settings.getString("CurrentArchive")), false);

      int numFiles = resources.length;
      TaskProgressManager.setMaximum(numFiles);

      // 4 - null
      // 4 - First File Offset [+8]
      // 4 - Number Of Files
      // 4 - Unknown (16)
      // 4 - null
      // 4 - First File Offset [+8]
      fm.writeBytes(src.readBytes(24));

      long offset = (numFiles * 16) + 24;

      TaskProgressManager.setMessage(Language.get("Progress_WritingDirectory"));
      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];
        long length = resource.getLength();

        if (resource.isReplaced()) {
          // replaced

          // 4 - Compression Tag (0=Decompressed 1=Compressed)
          fm.writeInt(0);
          src.skip(4);

          // 4 - File Offset
          fm.writeInt((int) offset);
          src.skip(4);

          // 4 - File Length
          fm.writeInt(length);
          src.skip(4);

          // 4 - Hash
          fm.writeBytes(src.readBytes(4));

          offset += length;

        }
        else {
          // same as in original archive

          // 4 - Compression Tag (0=Decompressed 1=Compressed)
          fm.writeBytes(src.readBytes(4));

          // 4 - File Offset
          fm.writeInt((int) offset);
          src.skip(4);

          // 4 - File Length
          // 4 - Hash
          fm.writeBytes(src.readBytes(8));

          offset += length;
        }

      }

      ExporterPlugin exporterDefault = Exporter_Default.getInstance();

      // Write Files
      TaskProgressManager.setMessage(Language.get("Progress_WritingFiles"));
      //long[] compressedLengths = write(exporter, resources, fm);
      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];

        if (resource.isReplaced()) {
          // replaced
          write(resource, fm);
        }
        else {
          // same as in original archive
          ExporterPlugin origExporter = resource.getExporter();
          resource.setExporter(exporterDefault);
          write(resource, fm);
          resource.setExporter(origExporter);
        }

        TaskProgressManager.setValue(i);
      }

      src.close();

      fm.close();

    }
    catch (Throwable t) {
      logError(t);
    }
  }

}
