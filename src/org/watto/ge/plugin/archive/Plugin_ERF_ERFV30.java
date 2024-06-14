/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2022 wattostudios
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
import org.getopt.util.hash.FNV164;
import org.watto.ErrorLogger;
import org.watto.Settings;
import org.watto.datatype.FileType;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.exporter.Exporter_Deflate;
import org.watto.io.FileManipulator;
import org.watto.io.buffer.ByteBuffer;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_ERF_ERFV30 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_ERF_ERFV30() {

    super("ERF_ERFV30", "ERF_ERFV30");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Dragon Age 2");
    setExtensions("erf"); // MUST BE LOWER CASE
    setPlatforms("PC");

    // MUST BE LOWER CASE !!!
    setFileTypes(new FileType("msh", "Model Mesh", FileType.TYPE_MODEL));

    setTextPreviewExtensions("ldf"); // LOWER CASE

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
      if (fm.readUnicodeString(8).equals("ERF V3.0")) {
        rating += 50;
      }

      fm.skip(4);

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

  HashMap<String, String> hashMap = null;

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

      ExporterPlugin exporter = Exporter_Deflate.getInstance();

      // RESETTING GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 16 - Header ("ERF V3.0") (unicode)
      fm.skip(16);

      // 4 - Filename Directory Length (or null)
      int filenameDirLength = fm.readInt();
      FieldValidator.checkLength(filenameDirLength, arcSize);

      // 4 - Number of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // 4 - Flags (0x000000F0 = Encryption, 0xE0000000 = Compression)
      int flags = fm.readInt();

      int compression = (flags >> 29) & 0x7;
      int encryption = (flags >> 4) & 0xF;

      if (encryption != 0) {
        ErrorLogger.log("[ERF_ERFV30] Encryption Type = " + encryption);
      }

      // 4 - Module ID (for password lookups when encryption is used)
      // 16 - MD5 of the Password (as a String)
      fm.skip(20);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);
      TaskProgressManager.setIndeterminate(true);

      // FILENAME DIRECTORY
      byte[] nameBytes = fm.readBytes(filenameDirLength);
      FileManipulator nameFM = new FileManipulator(new ByteBuffer(nameBytes));

      // See if we have a file with the filenames in it, and if so, we need to read them in
      if (hashMap == null) {
        hashMap = new HashMap<String, String>(279049); // 279049 = the number of filenames in the external file
        File hashFile = new File(Settings.get("HashesDirectory") + File.separatorChar + "ERF_ERFV30" + File.separatorChar + "filenames.txt");
        if (hashFile.exists()) {
          int hashFileLength = (int) hashFile.length();

          FNV164 hashgen = new FNV164();

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
      }

      TaskProgressManager.setIndeterminate(false);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {
        // 4 - Filename Offset (-1 means no filename) (relative to the start of the Filename Directory)
        int filenameOffset = fm.readInt();

        // 8 - FNV64 hash of lowercased file name, including path and extension
        long filenameHash = fm.readLong();
        String hash = Long.toHexString(filenameHash);

        // 4 - FNV32 hash of lowercased file extension
        fm.skip(4);

        // 4 - File Offset
        int offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - Compressed File Length
        int length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - Decompressed File Length
        int decompLength = fm.readInt();
        FieldValidator.checkLength(decompLength);

        String filename = hashMap.get(hash);
        if (filename == null) {
          if (filenameOffset != -1) {
            nameFM.seek(filenameOffset);

            // X - Filename
            // 1 - null Filename Terminator
            filename = nameFM.readNullString();
          }
          else {
            filename = Resource.generateFilename(i);
          }
        }

        if (compression == 1) {
          // deflate with 1-byte header instead of 2
          offset += 1;
          length -= 1;
        }

        //path,name,offset,length,decompLength,exporter
        Resource resource = new Resource(path, filename, offset, length, decompLength);

        if (compression == 1 || compression == 7) {
          resource.setExporter(exporter);
        }

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

    if (headerInt1 == 541476423) {
      return "gff"; // we don't read enough bytes to be able to determine what type, because that's in bytes 13-16
    }

    return null;
  }

}
