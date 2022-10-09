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
import org.watto.ErrorLogger;
import org.watto.datatype.FileType;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.exporter.Exporter_Deflate;
import org.watto.io.FileManipulator;
import org.watto.io.converter.IntConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_PKG_8 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_PKG_8() {

    super("PKG_8", "PKG_8");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("World of Warships");
    setExtensions("pkg"); // MUST BE LOWER CASE
    setPlatforms("PC");

    setFileTypes(new FileType("geometry", "Geometry Mesh", FileType.TYPE_MODEL));

    setTextPreviewExtensions("atlas", "unbound", "settings", "ubersettings", "font", "def", "pubkey"); // LOWER CASE

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

      ExporterPlugin exporterDeflate = Exporter_Deflate.getInstance();

      // RESETTING GLOBAL VARIABLES

      long arcSize = (int) path.length();

      // The IDX files are NOT in the same location as the PKG files. The IDX files are in ../bin/<largest_number>/idx/ relative to the PKG files.
      File sourcePath = null;
      try {
        File idxPath = new File(path.getParentFile().getParentFile().getAbsolutePath() + File.separatorChar + "bin");
        if (idxPath.exists() && idxPath.isDirectory()) {
          // find the largest number in this path
          int largestNumber = 0;
          File[] files = idxPath.listFiles();
          int numFilesInPath = files.length;
          for (int f = 0; f < numFilesInPath; f++) {
            try {
              int number = Integer.parseInt(files[f].getName());
              if (number > largestNumber) {
                largestNumber = number;
              }
            }
            catch (Throwable t) {
            }
          }
          if (largestNumber != 0) {
            idxPath = new File(idxPath.getAbsolutePath() + File.separatorChar + largestNumber + File.separatorChar + "idx");
            if (idxPath.exists() && idxPath.isDirectory()) {
              // found the directory, now try to find the IDX file

              String filename = path.getName();
              int underscorePos = filename.lastIndexOf('_');
              if (underscorePos > 0) {
                filename = filename.substring(0, underscorePos) + ".idx";

                File idxFile = new File(idxPath.getAbsolutePath() + File.separatorChar + filename);
                if (idxFile.exists()) {
                  // found the IDX file
                  sourcePath = idxFile;
                }
              }
            }
          }
        }
        if (sourcePath == null) {
          // see if the file is in the same directory as the PKG file, as a fallback
          String filename = path.getName();
          int underscorePos = filename.lastIndexOf('_');
          if (underscorePos > 0) {
            filename = filename.substring(0, underscorePos) + ".idx";

            File idxFile = new File(path.getParentFile().getAbsolutePath() + File.separatorChar + filename);
            if (idxFile.exists()) {
              // found the IDX file
              sourcePath = idxFile;
            }
          }
        }
        if (sourcePath == null) {
          // see if the file is in the same directory as the PKG file (with the same name as the PKG as well), as a fallback
          sourcePath = getDirectoryFile(path, "idx");
        }
      }
      catch (Throwable t) {
        ErrorLogger.log(t);
      }

      if (sourcePath == null) {
        ErrorLogger.log("[PKG_8] Couldn't find matching IDX file for the PKG file");
        return null;
      }

      FileManipulator fm = new FileManipulator(sourcePath, false);

      // 4 - Header (ISFP)
      // 4 - Unknown (2) (BIG ENDIAN)
      // 8 - Unknown
      fm.skip(16);

      // 4 - Number of Entries in Directory 1 and Filename Directory
      int numFilenames = fm.readInt();
      FieldValidator.checkNumFiles(numFilenames / 3);

      // 4 - Number of Files in Directory 2
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles / 3);

      // 8 - Number of Archive Files (1/2)
      int numArchives = fm.readInt();
      FieldValidator.checkNumFiles(numArchives);
      fm.skip(4);

      // 8 - Directory 1 Offset [+16] (40)
      long dir1Offset = fm.readLong() + 16;

      // 8 - Directory 2 Offset [+16]
      long dir2Offset = fm.readLong() + 16;

      // 8 - Footer Offset [+16]
      long footerOffset = fm.readLong() + 16;
      fm.seek(footerOffset);

      String pkgBasePath = path.getParent();

      File[] pkgFiles = new File[numArchives];
      int[] pkgNameLengths = new int[numArchives];
      long[] pkgHashes = new long[numArchives];

      for (int a = 0; a < numArchives; a++) {
        // 8 - PKG Filename Length (including null terminator)
        int nameLength = fm.readInt() - 1;
        FieldValidator.checkFilenameLength(nameLength);
        pkgNameLengths[a] = nameLength;

        fm.skip(4);

        // 8 - Unknown (24)
        fm.skip(8);

        // 8 - PKG Filename Hash
        long nameHash = fm.readLong();
        pkgHashes[a] = nameHash;
      }

      // for each archive file
      for (int a = 0; a < numArchives; a++) {
        // X - PKG Filename
        String pkgName = fm.readString(pkgNameLengths[a]);

        // 1 - null PKG Filename Terminator
        fm.skip(1);

        File pkgFile = new File(pkgBasePath + File.separatorChar + pkgName);
        if (!pkgFile.exists()) {
          ErrorLogger.log("[PKG_8] PKG file could not be found: " + pkgName);
          return null;
        }
        pkgFiles[a] = pkgFile;
      }

      fm.seek(dir1Offset);

      // Loop through directory
      int[] nameLengths = new int[numFilenames];
      long[] nameHashes = new long[numFilenames];
      for (int i = 0; i < numFilenames; i++) {
        // 8 - Filename Length (including null terminator)
        int filenameLength = fm.readInt() - 1;
        FieldValidator.checkFilenameLength(filenameLength);
        nameLengths[i] = filenameLength;
        fm.skip(4);

        // 8 - Unknown
        fm.skip(8);

        // 8 - Filename Hash
        long filenameHash = fm.readLong();
        nameHashes[i] = filenameHash;

        // 8 - Unknown
        fm.skip(8);
      }

      HashMap<Long, String> nameLookup = new HashMap<Long, String>(numFilenames);

      for (int i = 0; i < numFilenames; i++) {
        // X - Filename
        String filename = fm.readString(nameLengths[i]);

        // 1 - null Filename Terminator
        fm.skip(1);

        nameLookup.put(nameHashes[i], filename);
      }

      fm.seek(dir2Offset);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      File currentPkgFile = null;
      long currentArcHash = 0;
      for (int i = 0; i < numFiles; i++) {
        // 8 - Filename Hash (matches to one of the entries in Directory 1)
        long filenameHash = fm.readLong();

        // 8 - PKG Filename Hash (matches to one of the entries in the Footer)
        long arcHash = fm.readLong();
        if (arcHash != currentArcHash) {
          currentPkgFile = null;
          for (int a = 0; a < numArchives; a++) {
            if (pkgHashes[a] == arcHash) {
              currentArcHash = arcHash;
              currentPkgFile = pkgFiles[a];
              arcSize = currentPkgFile.length();
              break;
            }
          }
        }
        if (currentPkgFile == null) {
          ErrorLogger.log("[PKG_8] Couldn't find PKG file for hash " + arcHash);
        }

        // 8 - File Offset
        long offset = fm.readLong();
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - Unknown (0/5)
        fm.skip(4);

        // 4 - Unknown (0/1)
        int compressionFlag = fm.readInt();

        // 4 - Compressed File Length
        long length = IntConverter.unsign(fm.readInt());
        FieldValidator.checkLength(length, arcSize);

        // 4 - Unknown
        fm.skip(4);

        // 8 - Decompressed File Length
        long decompLength = fm.readLong();
        FieldValidator.checkLength(decompLength);

        // find the filename matching the hash
        String filename = nameLookup.get(filenameHash);
        if (filename == null) {
          filename = Resource.generateFilename(i);
        }

        //path,name,offset,length,decompLength,exporter
        Resource resource = new Resource(currentPkgFile, filename, offset, length, decompLength);
        resource.forceNotAdded(true);
        if (compressionFlag == 1) {
          resource.setExporter(exporterDeflate);
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

}
