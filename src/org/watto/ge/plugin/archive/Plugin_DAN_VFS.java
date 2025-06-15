/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2024 wattostudios
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
import java.nio.charset.Charset;

import org.watto.datatype.Archive;
import org.watto.datatype.FileType;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.exporter.Exporter_Custom_VAG_Audio;
import org.watto.ge.plugin.exporter.Exporter_LZSS;
import org.watto.io.FileManipulator;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_DAN_VFS extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_DAN_VFS() {

    super("DAN_VFS", "DAN_VFS");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("R.A.D. Robotic Alchemic Drive");
    setExtensions("dan", "dy"); // MUST BE LOWER CASE
    setPlatforms("PS2");

    // MUST BE LOWER CASE !!!
    setFileTypes(new FileType("bd", "VAG Audio", FileType.TYPE_AUDIO));

    setTextPreviewExtensions("bat"); // LOWER CASE

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
      if (fm.readString(4).equals("VFS" + (byte) 0)) {
        rating += 50;
      }

      // Number Of Files
      if (FieldValidator.checkNumFiles(fm.readInt())) {
        rating += 5;
      }

      if (fm.readInt() == 16) {
        rating += 5;
      }

      long arcSize = fm.getLength();

      // Directory Length
      if (FieldValidator.checkLength(fm.readInt(), arcSize)) {
        rating += 5;
      }

      return rating;

    }
    catch (Throwable t) {
      return 0;
    }
  }

  int realNumFiles = 0;

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

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 4 - Header ("VFS" + null)
      fm.skip(4);

      // 4 - Number of Folders
      int numFolders = fm.readInt();
      FieldValidator.checkNumFiles(numFolders);

      // 4 - Entry Length (16)
      // 4 - Length of Header and Folder Directory (not including padding)
      fm.skip(8);

      int numFiles = Archive.getMaxFiles();
      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(arcSize);

      realNumFiles = 0;

      int[] folderOffsets = new int[numFolders];
      int[] numFilesInFolders = new int[numFolders];
      for (int f = 0; f < numFolders; f++) {
        // 4 - Folder Name Offset (relative to the start of this entry)
        fm.skip(4);

        // 4 - Number of Files in this Folder
        int numFilesInFolder = fm.readInt();
        FieldValidator.checkNumFiles(numFilesInFolder);

        // 4 - File Entries Offset
        int dirOffset = fm.readInt();
        FieldValidator.checkOffset(dirOffset, arcSize);

        // 4 - Unknown
        fm.skip(4);

        folderOffsets[f] = dirOffset;
        numFilesInFolders[f] = numFilesInFolder;
      }

      String[] names = new String[numFolders];
      for (int f = 0; f < numFolders; f++) {

        // X - Filename (null)
        String filename = fm.readNullString();
        FieldValidator.checkFilename(filename);

        names[f] = filename;
      }

      for (int f = 0; f < numFolders; f++) {
        fm.seek(folderOffsets[f]);

        String folderName = names[f];
        int numFilesInFolder = numFilesInFolders[f];

        readDirectory(path, fm, arcSize, folderName, numFilesInFolder, resources);
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
   
   **********************************************************************************************
   **/
  public void readDirectory(File path, FileManipulator fm, long arcSize, String folderName, int numFilesInFolder, Resource[] resources) {
    try {

      boolean charsetOK = false;
      if (Charset.isSupported("MS932")) {
        charsetOK = true;
      }

      ExporterPlugin exporter = Exporter_LZSS.getInstance();
      ExporterPlugin exporterVAG = Exporter_Custom_VAG_Audio.getInstance();

      int[] compLengths = new int[numFilesInFolder];
      int[] decompLengths = new int[numFilesInFolder];
      int[] offsets = new int[numFilesInFolder];

      // Loop through directory
      for (int i = 0; i < numFilesInFolder; i++) {

        // 4 - Compressed File Length
        int length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - File Offset
        int offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - Filename Offset (relative to the start of this entry)
        fm.skip(4);

        // 4 - Decompressed File Length (or null if not compressed)
        int decompLength = fm.readInt();
        FieldValidator.checkLength(decompLength);

        compLengths[i] = length;
        decompLengths[i] = decompLength;
        offsets[i] = offset;

        TaskProgressManager.setValue(offset);
      }

      String[] names = new String[numFilesInFolder];
      for (int i = 0; i < numFilesInFolder; i++) {
        // X - Filename (null)
        //String filename = fm.readNullString();
        //FieldValidator.checkFilename(filename);

        byte[] filenameBytes = new byte[128];
        byte byteVal = fm.readByte();
        int outPos = 0;
        boolean badChar = false;
        while (byteVal != 0) {
          filenameBytes[outPos] = byteVal;
          outPos++;
          if (byteVal >= 128 || byteVal < 0) {
            badChar = true;
          }
          byteVal = fm.readByte();
        }
        byte[] substringBytes = new byte[outPos];
        System.arraycopy(filenameBytes, 0, substringBytes, 0, outPos);

        String filename = null;
        if (charsetOK) {
          filename = new String(substringBytes, "MS932");
        }
        else {
          if (badChar) {
            // found characters that aren't in the encoding, so need to create a new filename
            // the extension is OK though, so grab that first, then generate the new filename
            filename = new String(substringBytes);
            int dotPos = filename.lastIndexOf('.');
            if (dotPos > 0) {
              filename = Resource.generateFilename(realNumFiles + i) + filename.substring(dotPos);
            }
            else {
              filename = Resource.generateFilename(realNumFiles + i);
            }
          }
          else {
            // still OK, just use as-is, all english characters
            filename = new String(substringBytes);
          }
        }

        names[i] = filename;
      }

      for (int i = 0; i < numFilesInFolder; i++) {
        int offset = offsets[i];
        int length = compLengths[i];
        int decompLength = decompLengths[i];
        String filename = names[i];

        filename = folderName + filename;

        if (decompLength == 0) {
          // not compressed

          //path,name,offset,length,decompLength,exporter
          resources[realNumFiles] = new Resource(path, filename, offset, length);
        }
        else {
          // compressed

          offset += 4; // the first 4 bytes of the file are the decompressed length
          // don't need to do length -= 4 because the length already doesn't consider these 4 bytes

          //path,name,offset,length,decompLength,exporter
          resources[realNumFiles] = new Resource(path, filename, offset, length, decompLength, exporter);
        }

        if (filename.endsWith("BD")) {
          resources[realNumFiles].setExporter(exporterVAG);
        }

        realNumFiles++;

      }

    }
    catch (

    Throwable t) {
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

    /*
    if (headerInt1 == 2037149520) {
      return "js";
    }
    */

    return null;
  }

}
