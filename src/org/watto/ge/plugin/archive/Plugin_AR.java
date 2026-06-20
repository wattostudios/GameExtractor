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

import org.watto.datatype.FileType;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.io.FileManipulator;
import org.watto.io.converter.ByteConverter;
import org.watto.io.converter.IntConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_AR extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_AR() {

    super("AR", "AR");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Street Racing Syndicate", "Big Mutha Truckers");
    setExtensions("ar"); // MUST BE LOWER CASE
    setPlatforms("PC", "PS2", "XBox");

    setFileTypes(new FileType("arc", "ARC Archive", FileType.TYPE_ARCHIVE));

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

      File thisFile = fm.getFile();
      if (thisFile.getName().equalsIgnoreCase("archive.ar")) {
        rating += 25;
      }

      String basePath = thisFile.getParentFile().getAbsolutePath() + File.separatorChar;
      if (new File(basePath + "CDFILES.DAT").exists() || new File(basePath + "cdfiles.dat").exists()) {
        rating += 24; // 24 so that it doesn't match unless something else also matches
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

      long arcSize = path.length();

      String basePath = path.getParentFile().getAbsolutePath() + File.separatorChar;
      File dirFile = new File(basePath + "CDFILES.DAT");
      if (!dirFile.exists()) {
        dirFile = new File(basePath + "cdfiles.dat");
      }
      if (!dirFile.exists()) {
        return null; // can't find the cdfiles.dat file
      }

      // Now for XBox, the files are split as archive0.ar, archive1.ar, etc.
      // So need to find all the archive files, as well as their lengths
      String arcFilename = path.getAbsolutePath();
      int dotPos = arcFilename.lastIndexOf('.');
      if (dotPos <= 1) {
        return null;
      }
      String arcExtension = arcFilename.substring(dotPos);
      arcFilename = arcFilename.substring(0, dotPos - 1);

      File[] arcFiles = new File[10];
      long[] arcSizes = new long[10];
      int numArcFiles = 0;
      if (new File(arcFilename + 0 + arcExtension).exists()) {
        // multiple files
        for (int i = 0; i < 10; i++) {
          File arcFile = new File(arcFilename + i + arcExtension);
          if (arcFile.exists() && arcFile.isFile()) {
            arcFiles[numArcFiles] = arcFile;
            arcSizes[numArcFiles] = arcFile.length();
            numArcFiles++;
          }
        }

      }
      else {
        // single file
        numArcFiles = 1;
        arcFiles[0] = path;
        arcSizes[0] = arcSize;
      }

      FileManipulator fm = new FileManipulator(dirFile, false);

      // 4 - Header (file)
      fm.skip(4);

      // 4 - Version (3)
      int version = fm.readInt();

      // 4 - Unknown
      fm.skip(4);

      // 4 - Unknown (4)
      int unknown1 = fm.readInt();
      // 4 - Unknown (4)
      int unknown2 = fm.readInt();
      // 4 - null
      int unknown3 = fm.readInt();
      // 4 - null
      int unknown4 = fm.readInt();

      // 4 - Number of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // 4 - Unknown (12)
      fm.skip(4);

      // 4 - Padding Multiple (2048)
      int paddingMultiple = fm.readInt();
      FieldValidator.checkLength(paddingMultiple, arcSize);

      // 4 - Number of Names
      int numNames = fm.readInt();
      FieldValidator.checkNumFiles(numNames);

      // 4 - Unknown
      fm.skip(4);

      // X - Unknown
      int skipSize = unknown1 + unknown2 + unknown3 * 4 + unknown4;
      fm.skip(skipSize);

      // 12 - Archive Filename ("\ARCHIVE.AR" + null)
      fm.readNullString();

      fm.skip(calculatePadding(fm.getOffset(), 4));

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      //System.out.println("Offsets at " + fm.getOffset());

      // Get ready at the first archive (in case there's multiple of them)
      int currentArcFile = 0;
      path = arcFiles[currentArcFile];
      arcSize = arcSizes[currentArcFile];

      // Loop through directory
      long[] offsets = new long[numFiles];
      for (int i = 0; i < numFiles; i++) {

        // 4 - File Offset
        long offset = ((long) fm.readInt()) * paddingMultiple;
        FieldValidator.checkOffset(offset, arcSize);
        offsets[i] = offset;

        if (i != 0 && offset == 0) {
          currentArcFile++;
          path = arcFiles[currentArcFile];
          arcSize = arcSizes[currentArcFile];
        }

        TaskProgressManager.setValue(i);
      }

      //System.out.println("Lengths at " + fm.getOffset());

      // Get ready at the first archive again, as we're storing the Resources here (in case there's multiple of them)
      currentArcFile = 0;
      path = arcFiles[currentArcFile];
      arcSize = arcSizes[currentArcFile];

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {
        // 4 - File Length
        long length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        long offset = offsets[i];

        if (i != 0 && offset == 0) {
          currentArcFile++;
          path = arcFiles[currentArcFile];
          arcSize = arcSizes[currentArcFile];
        }

        String filename = Resource.generateFilename(i);

        //path,name,offset,length,decompLength,exporter
        Resource resource = new Resource(path, filename, offset, length);
        resource.forceNotAdded(true);
        resources[i] = resource;

        TaskProgressManager.setValue(i);
      }

      //System.out.println("Recon at " + fm.getOffset());

      // Loop through directory
      long[] reconstructionOffsets = new long[numNames];
      for (int i = 0; i < numNames; i++) {
        // 4 - Offset into Reconstruction Directory for this file (relative to the start of the Reconstruction Directory)
        int offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);
        reconstructionOffsets[i] = offset;
      }

      // Read The Flags Directory
      int[] ids = new int[numNames];
      int[] idTypes = new int[numNames];
      for (int i = 0; i < numNames; i++) {
        byte[] flagBytes = fm.readBytes(4);
        if (flagBytes[3] == 32) {
          continue;
        }

        if (flagBytes[3] == 0) {
          // Unknown
          continue;
        }

        int id = IntConverter.convertLittle(new byte[] { flagBytes[0], flagBytes[1], flagBytes[2], 0 });

        if (idTypes[id] == 64) {
          // File Overwrite
          continue;
        }

        ids[id] = i;
        idTypes[id] = flagBytes[3];
      }

      String[] filenames = null;
      if (version == 1) {
        long relativeNameOffset = fm.getOffset();

        filenames = new String[numNames];
        for (int i = 0; i < numNames; i++) {
          fm.relativeSeek(relativeNameOffset + reconstructionOffsets[i]);

          // X - Filename
          // 1 - null Filename Terminator
          String name = fm.readNullString();
          filenames[i] = name;
        }
      }
      else {
        // SKIP THE NULLS DIRECTORY
        fm.skip((numNames * 4));

        //System.out.println("Names Offsets at " + fm.getOffset());

        // 4 - Number of Names
        int realNumNames = fm.readInt();
        FieldValidator.checkNumFiles(realNumNames);

        // 4 - Name Directory Length
        int nameDirLength = fm.readInt();
        FieldValidator.checkLength(nameDirLength, arcSize);

        long relativeNameOffset = fm.getOffset() + (realNumNames * 4);
        long reconstructionDirOffset = relativeNameOffset + nameDirLength;

        long[] nameOffsets = new long[realNumNames];
        for (int i = 0; i < realNumNames; i++) {
          // 4 - Name Offset (relative to the start of the Names Directory)
          nameOffsets[i] = relativeNameOffset + fm.readInt();
        }

        //System.out.println("Names at " + fm.getOffset());

        String[] names = new String[realNumNames];
        for (int i = 0; i < realNumNames; i++) {
          fm.relativeSeek(nameOffsets[i]);

          // X - Partial Name
          // 1 - null Name Terminator
          String name = fm.readNullString();
          names[i] = name;
        }

        // Read the name reconstructions
        filenames = new String[numNames];
        for (int i = 0; i < numNames; i++) {
          fm.relativeSeek(reconstructionDirOffset + reconstructionOffsets[i]);

          String filename = "";

          //long nameOffset = fm.getOffset();

          int currentByte = ByteConverter.unsign(fm.readByte());
          while (currentByte != 0) {
            int nameIndex = 0;

            if (currentByte >= 128) {
              nameIndex = ((currentByte - 128) << 8) | ByteConverter.unsign(fm.readByte());
            }
            else {
              nameIndex = currentByte;
            }

            nameIndex--; // name indexes actually start at 1, in this directory, because 0 means "end of entry" so can't be used as an index

            filename += names[nameIndex];

            // read the next byte
            currentByte = ByteConverter.unsign(fm.readByte());
          }

          //Resource resource = resources[i];
          //resource.setName(filename);
          //resource.setOriginalName(filename);

          filenames[i] = filename;
        }
      }

      Resource[] oldResources = resources;
      resources = new Resource[numNames];

      // Assign the names to the files (and exclude all the "old" files that have been overwritten)
      int realNumFiles = 0;
      for (int i = 0; i < numFiles; i++) {
        int id = ids[i];

        if (idTypes[i] != 64) {
          continue;
        }

        String filename = filenames[id];//Resource.generateFilename(realNumFiles);

        Resource resource = oldResources[i];
        resource.setName(filename);
        resource.setOriginalName(filename);

        resources[realNumFiles] = resource;
        realNumFiles++;
      }

      fm.close();

      if (realNumFiles < numNames) {
        resources = resizeResources(resources, realNumFiles);
      }

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

    if (headerInt1 == 1128485441) {
      return "arc"; // ARCC
    }
    else if (headerInt1 == 811689068) {
      return "lda"; // lda0
    }
    else if (headerInt1 == 1634037875) {
      return "spe"; // spea
    }
    else if (headerInt1 == 1633971827) {
      return "snd"; // snda
    }

    return null;
  }

}
