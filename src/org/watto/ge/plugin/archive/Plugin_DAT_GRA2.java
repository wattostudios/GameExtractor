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
import java.util.HashMap;
import org.getopt.util.hash.FNV1a32;
import org.watto.Language;
import org.watto.Settings;
import org.watto.datatype.FileType;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.exporter.Exporter_Default;
import org.watto.ge.plugin.exporter.Exporter_ZLib;
import org.watto.io.FileManipulator;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_DAT_GRA2 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_DAT_GRA2() {

    super("DAT_GRA2", "DAT_GRA2");

    //         read write replace rename
    setProperties(true, true, false, false);

    setGames("Legend of Grimrock",
        "Legend of Grimrock 2");
    setExtensions("dat"); // MUST BE LOWER CASE
    setPlatforms("PC");

    // MUST BE LOWER CASE !!!
    setFileTypes(new FileType("model", "3D Mesh", FileType.TYPE_MODEL));

    //setTextPreviewExtensions("colours", "rat", "screen", "styles"); // LOWER CASE

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

      // Header
      if (fm.readString(4).equals("GRA2")) {
        rating += 50;
      }

      // Number Of Files
      if (FieldValidator.checkNumFiles(fm.readInt())) {
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

      ExporterPlugin exporter = Exporter_ZLib.getInstance();

      // RESETTING GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 4 - Header (GRA2)
      fm.skip(4);

      // 4 - Number Of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // See if we have a file with the filenames in it, and if so, read it in
      HashMap<String, String> hashMap = new HashMap<String, String>(numFiles);
      File hashFile = new File(Settings.get("HashesDirectory") + File.separatorChar + "DAT_GRA2" + File.separatorChar + path.getName() + ".txt");
      if (!hashFile.exists()) {
        hashFile = new File(Settings.get("HashesDirectory") + File.separatorChar + "DAT_GRA2" + File.separatorChar + "default.txt");
      }
      if (hashFile.exists()) {
        int hashFileLength = (int) hashFile.length();

        FNV1a32 hashgen = new FNV1a32();

        FileManipulator hashFM = new FileManipulator(hashFile, false);
        while (hashFM.getOffset() < hashFileLength) {
          String name = hashFM.readLine();
          if (name.equals("")) {
            break; // EOF
          }
          hashgen.init(name);
          long hash = hashgen.getHash();

          //System.out.println(Long.toHexString(hash));

          hashMap.put(Long.toHexString(hash), name);
        }
        hashFM.close();
      }

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {

        // 4 - Filename Hash
        int hashInt = fm.readInt();
        String hash = Integer.toHexString(hashInt);

        // 4 - File Offset
        int offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - Compressed File Length
        int length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - Decompressed File Length
        int decompLength = fm.readInt();
        FieldValidator.checkLength(decompLength);

        // 2 - Compression Flag? (1=compressed, 0=uncompressed)
        short compressionFlag = fm.readShort();

        // 2 - Encryption Flag? (1=encrypted, 0=unencrypted)
        short encryptionFlag = fm.readShort();

        if (compressionFlag == 0) {
          length = decompLength;
        }

        String filename = hashMap.get(hash);
        if (filename == null) {
          //System.out.println("Mising hash for " + hash);
          filename = Resource.generateFilename(i);
        }

        if (compressionFlag == 1 && encryptionFlag == 0) {
          //path,name,offset,length,decompLength,exporter

          offset += 4;
          length -= 4;

          Resource resource = new Resource(path, filename, offset, length, decompLength, exporter);
          resource.addProperty("FilenameHash", hashInt);
          resource.addProperty("Encrypted", encryptionFlag);
          resources[i] = resource;
        }
        else {
          Resource resource = new Resource(path, filename, offset, length, decompLength);
          resource.addProperty("FilenameHash", hashInt);
          resource.addProperty("Encrypted", encryptionFlag);
          resources[i] = resource;
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
   * Writes an [archive] File with the contents of the Resources
   **********************************************************************************************
   **/
  @Override
  public void write(Resource[] resources, File path) {
    try {

      FileManipulator fm = new FileManipulator(path, true);
      int numFiles = resources.length;
      TaskProgressManager.setMaximum(numFiles);

      // Write Header Data

      // 4 - Header (GRA2)
      fm.writeString("GRA2");

      // 4 - Number Of Files
      fm.writeInt(numFiles);

      // Write Directory
      TaskProgressManager.setMessage(Language.get("Progress_WritingDirectory"));
      long offset = 8 + (numFiles * 20);
      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];

        // use the filename hash, if we have it
        int filenameHash = 0;
        try {
          filenameHash = Integer.parseInt(resource.getProperty("FilenameHash"));
        }
        catch (Throwable t) {
        }

        if (filenameHash == 0 || resource.isRenamed()) {
          // calculate the hash for the filename
          FNV1a32 hashgen = new FNV1a32();
          hashgen.init(resource.getName());
          filenameHash = (int) hashgen.getHash();
        }

        // 4 - Filename Hash
        fm.writeInt(filenameHash);

        // 4 - File Offset
        fm.writeInt((int) offset);

        // 4 - Compressed File Length
        // 4 - Decompressed File Length
        if (resource.isCompressed()) {
          // compressed
          long decompLength = resource.getDecompressedLength();
          long compLength = resource.getLength() + 4; // +4 for the compression header

          if (resource.getExporter() instanceof Exporter_Default) {
            // it's compressed, and encrypted, so we just leave it as a raw file
            compLength -= 4;
          }

          fm.writeInt((int) compLength);
          fm.writeInt((int) decompLength);

          offset += compLength;
        }
        else {
          // not compressed, or is a replaced file
          long decompLength = resource.getDecompressedLength();

          fm.writeInt((int) 0);
          fm.writeInt((int) decompLength);

          offset += decompLength;
        }

        // 2 - Compression Flag? (1=compressed, 0=uncompressed)
        if (resource.isCompressed()) {
          fm.writeShort(1);
        }
        else {
          fm.writeShort(0);
        }

        // 2 - Encryption Flag? (1=encrypted, 0=unencrypted)
        if (!resource.isReplaced()) {
          int encryptionFlag = 0;

          try {
            encryptionFlag = Integer.parseInt(resource.getProperty("Encrypted"));
          }
          catch (Throwable t) {
          }

          fm.writeShort(encryptionFlag);
        }
        else {
          fm.writeShort(0);
        }

      }

      // Write Files
      TaskProgressManager.setMessage(Language.get("Progress_WritingFiles"));
      ExporterPlugin exporterDefault = Exporter_Default.getInstance();
      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];

        if (resource.isCompressed()) {
          if (!(resource.getExporter() instanceof Exporter_Default)) {
            // if the file is encrypted, it's just stored as a raw file (including this encrypted 4-byte header), so skip it 

            // 4 - Decompressed File Length
            long decompLength = resource.getDecompressedLength();
            fm.writeInt((int) decompLength);
          }
        }

        ExporterPlugin originalExporter = resource.getExporter();
        if (!resource.isReplaced()) {
          resource.setExporter(exporterDefault); // set to the default exporter for fast writes (just does a byte-to-byte copy)
        }

        write(resource, fm);

        resource.setExporter(originalExporter);

        TaskProgressManager.setValue(i);
      }

      //ExporterPlugin exporter = new Exporter_ZLib();
      //long[] compressedLengths = write(exporter,resources,fm);

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
    if (extension != null && !extension.equals("")) {
      return extension;
    }

    if (headerInt1 == 1296649793) {
      return "animation";
    }
    else if (headerInt1 == 827081805) {
      return "model";
    }
    else if (headerInt1 == -130304 || headerInt1 == -64768) {
      return "d3d9_shader";
    }
    else if (headerInt1 == 21646363) {
      return "lua"; // grimrock 2 unencrypted lua script
    }
    else if (headerShort1 == 11565 || headerInt1 == 1768318308 || headerInt1 == 1852205347 || headerInt1 == 2037539190 || headerInt1 == 1092628271 || headerInt1 == 1634692198 || headerInt1 == 1717920803 || headerInt1 == 1919252003) {
      return "shader"; // either fsh or vsh, probably
    }
    else if (headerShort3 == 2625) {
      return "lua"; // encrypted lua script
    }
    else if (headerInt1 == 655360) {
      return "tga";
    }

    return null;
  }

}
