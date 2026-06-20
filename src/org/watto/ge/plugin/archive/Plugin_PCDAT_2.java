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
import org.watto.io.converter.IntConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_PCDAT_2 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_PCDAT_2() {

    super("PCDAT_2", "PCDAT_2");

    //         read write replace rename
    setProperties(true, false, true, false);

    setGames("Conflict: Desert Storm");
    setExtensions("dat"); // MUST BE LOWER CASE
    setPlatforms("GameCube");

    // MUST BE LOWER CASE !!!
    setFileTypes(new FileType("loc", "Language Archive", FileType.TYPE_ARCHIVE),
        new FileType("csv", "CSV Spreadsheet", FileType.TYPE_DOCUMENT),
        new FileType("dat_texarc", "Texture Archive", FileType.TYPE_ARCHIVE));

    setTextPreviewExtensions("ver", "csv"); // LOWER CASE

    // WE ONLY TURN THIS ON, in read(), IF SOME FILES DON'T HAVE PROPER FILENAMES
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

      fm.skip(4);

      long arcSize = fm.getLength();

      // First File Offset
      if (FieldValidator.checkOffset(IntConverter.changeFormat(fm.readInt()), arcSize)) {
        rating += 5;
      }

      // First File Length
      if (FieldValidator.checkLength(IntConverter.changeFormat(fm.readInt()), arcSize)) {
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

      //ExporterPlugin exporter = Exporter_ZLib.getInstance();

      // RESETTING GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      int numFiles = Archive.getMaxFiles();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(arcSize);

      // See if we have a file with the filenames in it, and if so, we need to read them in so the decryption works properly
      HashMap<Integer, String> hashMap = new HashMap<Integer, String>(numFiles);
      File hashFile = new File(Settings.get("HashesDirectory") + File.separatorChar + "PCDAT" + File.separatorChar + "filenames.txt");
      if (hashFile.exists()) {
        int hashFileLength = (int) hashFile.length();

        FileManipulator hashFM = new FileManipulator(hashFile, false);
        while (hashFM.getOffset() < hashFileLength) {
          String name = hashFM.readLine();
          if (name.equals("")) {
            break; // EOF
          }

          int separatorPos = name.indexOf(' ');
          if (separatorPos <= 0) {
            continue; // no separator between hash and filename
          }

          String hash = name.substring(0, separatorPos);
          name = name.substring(separatorPos + 1);

          // Convert the unsigned hash to a signed hash
          Integer intHash = Integer.parseUnsignedInt(hash);

          hashMap.put(intHash, name);
        }
        hashFM.close();
      }

      // Loop through directory
      int realNumFiles = 0;
      boolean missingFilenames = false;
      while (fm.getOffset() < 49152) {

        // 4 - Hash
        int hash = IntConverter.changeFormat(fm.readInt());

        // 4 - File Offset
        int offset = IntConverter.changeFormat(fm.readInt());
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - File Length
        int length = IntConverter.changeFormat(fm.readInt());
        FieldValidator.checkLength(length, arcSize);

        if (offset == 0 && length == 0) {
          break; // end of directory
        }

        //String filename = Resource.generateFilename(realNumFiles);
        String filename = hashMap.get(hash);
        if (filename == null) {
          filename = Resource.generateFilename(realNumFiles);
          missingFilenames = true;
        }

        //path,name,offset,length,decompLength,exporter
        resources[realNumFiles] = new Resource(path, filename, offset, length);
        realNumFiles++;

        TaskProgressManager.setValue(offset);
      }

      if (realNumFiles == 1) { // not a valid archive
        return null;
      }

      resources = resizeResources(resources, realNumFiles);

      if (missingFilenames) {
        setCanScanForFileTypes(true); // only scan for file types IF we have filenames that are missing
      }
      else {
        setCanScanForFileTypes(false);
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

      // Get the first offset
      src.skip(4);
      long firstOffset = IntConverter.changeFormat(src.readInt());
      src.relativeSeek(0);

      boolean padding = true; // GameCube padded to 32 bytes

      // Calculations
      TaskProgressManager.setMessage(Language.get("Progress_PerformingCalculations"));

      // Write Header Data

      // Write Directory
      TaskProgressManager.setMessage(Language.get("Progress_WritingDirectory"));
      long offset = firstOffset;
      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];
        long length = resource.getDecompressedLength();

        // 4 - Hash
        fm.writeBytes(src.readBytes(4));

        // 4 - File Offset
        fm.writeInt(IntConverter.changeFormat((int) offset));
        src.skip(4);

        // 4 - File Length
        fm.writeInt(IntConverter.changeFormat((int) length));
        src.skip(4);

        offset += length;

        if (padding) {
          offset += calculatePadding(offset, 32);
        }
      }

      // padding to the first file offset
      int paddingLength = (int) (firstOffset - fm.getOffset());
      for (int p = 0; p < paddingLength; p++) {
        fm.writeByte(0);
      }

      // Write Files
      TaskProgressManager.setMessage(Language.get("Progress_WritingFiles"));
      for (int i = 0; i < resources.length; i++) {
        Resource resource = resources[i];
        long length = resource.getDecompressedLength();

        write(resource, fm);

        if (padding) {
          paddingLength = calculatePadding(length, 32);

          for (int p = 0; p < paddingLength; p++) {
            fm.writeByte(0);
          }
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

    if (headerInt1 == 1245859653) {
      return "EVO"; // EOBJ
    }
    else if (headerInt1 == 1718054249) {
      return "IMG"; // imgf
    }
    else if (headerInt1 == 131072) {
      return "TGA";
    }
    else if (headerInt1 == 131072) {
      return "RFX"; //REFX 
    }
    else if (headerInt1 == 2 || headerInt1 == 33554432) {
      return "PRB"; // a mesh?
    }
    else if (headerInt1 == 1178750284) {
      return "lmbf";
    }
    else if (headerInt1 == 1196118081) {
      return "apkg";
    }
    else if (headerInt1 == 1380275744 || headerInt1 == 1397900630) {
      return "ver";
    }
    else if (headerInt1 == 0 && (headerInt2 > 0 && headerInt2 < 50) && (headerInt3 > 0 && headerInt3 < 50)) {
      return "loc"; // language archive
    }
    else if (headerInt1 == 1 && headerInt2 == 80) {
      return "facetalk";
    }
    else if (headerInt3 == 4 || headerInt3 == 8) {
      return "dat_texarc"; // potentially a texture archive (PS2)
    }
    else {
      // if the first 12 bytes are all ascii, it might be a text file
      boolean ascii = true;
      for (int b = 0; b < 12; b++) {
        byte currentByte = headerBytes[b];
        if (currentByte >= 32 && currentByte <= 126) {
          // ascii
        }
        else if (currentByte == 9 || currentByte == 10 || currentByte == 13) {
          // ascii (tab, new line)
        }
        else {
          ascii = false; // found a non-ascii character
          break;
        }
      }
      if (ascii) {
        return "csv";
      }
    }

    return null;
  }

}
