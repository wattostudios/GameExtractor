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

import org.watto.Language;
import org.watto.Settings;
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
public class Plugin_ARC_15 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_ARC_15() {

    super("ARC_15", "ARC_15");

    //         read write replace rename
    setProperties(true, false, true, false);

    setGames("The Urbz: Sims in the City");
    setExtensions("arc"); // MUST BE LOWER CASE
    setPlatforms("GameCube");

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

      long arcSize = fm.getLength();

      // Directory Offset
      if (FieldValidator.checkOffset(fm.readInt(), arcSize)) {
        rating += 5;
      }

      // 4 - Unknown (65536)
      if (fm.readInt() == 65536) {
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

      // 4 - Details Directory Offset
      int dirOffset = fm.readInt();
      FieldValidator.checkOffset(dirOffset, arcSize);

      // 4 - Unknown (65536)
      // 4 - Hash/CRC?
      // 4 - Hash/CRC?

      fm.seek(dirOffset);

      // 4 - Number Of Files
      int numFiles = IntConverter.changeFormat(fm.readInt());
      FieldValidator.checkNumFiles(numFiles);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      setCanScanForFileTypes(false);

      String extension = null;
      String arcName = path.getName().toLowerCase();
      if (arcName.equals("animatio.arc")) {
        extension = ".ani";
      }
      else if (arcName.equals("audiostr.arc")) {
        extension = ".aud";
      }
      else if (arcName.equals("characte.arc")) {
        extension = ".chr";
      }
      else if (arcName.equals("datasets.arc")) {
        extension = ".set";
      }
      else if (arcName.equals("edithtre.arc")) {
        extension = ".edt";
      }
      else if (arcName.equals("emitters.arc")) {
        extension = ".emt";
      }
      else if (arcName.equals("flashes.arc")) {
        extension = ".fls";
      }
      else if (arcName.equals("fonts.arc")) {
        extension = ".fnt";
      }
      else if (arcName.equals("levels.arc")) {
        extension = ".lvl";
      }
      else if (arcName.equals("models.arc")) {
        extension = ".mdl";
      }
      else if (arcName.equals("movies.arc")) {
        extension = ".xmv";
      }
      else if (arcName.equals("quickdat.arc")) {
        extension = ".dat";
      }
      else if (arcName.equals("samples.arc")) {
        extension = ".vox";
      }
      else if (arcName.equals("shaders.arc")) {
        extension = ".shd";
      }
      else if (arcName.equals("textures.arc")) {
        extension = ".txfl";
      }
      else if (arcName.equals("binaries.arc")) {
        // do nothing - already contains file extensions
      }
      else {
        setCanScanForFileTypes(true);
      }

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {
        // 4 - Hash?
        fm.skip(4);

        // 4 - File Offset
        int offset = IntConverter.changeFormat(fm.readInt());
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - File Length
        int length = IntConverter.changeFormat(fm.readInt());
        FieldValidator.checkLength(length, arcSize);

        // X - Filename
        // 1 - null Filename Terminator
        String filename = fm.readNullString();
        FieldValidator.checkFilename(filename);

        if (extension != null) {
          filename += extension;
        }

        // 8 - Hash/CRC? (maybe TGI like in The Sims?)
        fm.skip(8);

        //path,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, filename, offset, length);

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

      // For this archive, lets read in all the source details first, store the items we need, then we'll rebuild the archive.
      // This is partly because we want to see whether the archive has padding or not.

      TaskProgressManager.setMessage(Language.get("Progress_PerformingCalculations"));

      // 4 - Details Directory Offset
      int srcDirOffset = src.readInt();

      // 4 - Unknown (65536)
      // 4 - Hash/CRC?
      // 4 - Hash/CRC?
      byte[] headerBytes = src.readBytes(12);

      src.seek(srcDirOffset);

      // 4 - Number Of Files
      src.skip(4);

      int[] fileHashes1 = new int[numFiles];
      long[] fileHashes2 = new long[numFiles];
      String[] names = new String[numFiles];
      int paddingSize = 0;

      for (int i = 0; i < numFiles; i++) {
        // 4 - Hash?
        fileHashes1[i] = src.readInt();

        // 4 - File Offset
        if (i == 0) {
          int firstOffset = src.readInt();
          if (firstOffset != 16) {
            paddingSize = firstOffset;
          }
        }
        else {
          src.skip(4);
        }

        // 4 - File Length
        src.skip(4);

        // X - Filename
        // 1 - null Filename Terminator
        names[i] = src.readNullString();

        // 8 - Hash/CRC? (maybe TGI like in The Sims?)
        fileHashes2[i] = src.readLong();
      }

      // Now we have the padding size, and all the hash details that we need.
      // So, lets build the new archive.

      // Calculations
      TaskProgressManager.setMessage(Language.get("Progress_PerformingCalculations"));

      long dirOffset = 16;
      if (paddingSize != 0) {
        dirOffset = paddingSize;
      }
      for (int i = 0; i < numFiles; i++) {
        dirOffset += resources[i].getDecompressedLength();
        if (paddingSize != 0) {
          dirOffset += calculatePadding(dirOffset, paddingSize);
        }
      }

      // Write Header Data

      // 4 - Details Directory Offset
      fm.writeInt(dirOffset);

      // 4 - Unknown (65536)
      // 4 - Hash/CRC?
      // 4 - Hash/CRC?
      fm.writeBytes(headerBytes);

      // [OPTIONAL] X - Padding to a multiple of 4096 bytes
      for (int p = 16; p < paddingSize; p++) { // if paddingSize == 0, this loop won't run
        fm.writeByte(0);
      }

      // Write Files
      long[] offsets = new long[numFiles];
      TaskProgressManager.setMessage(Language.get("Progress_WritingFiles"));
      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];

        offsets[i] = fm.getOffset();

        // X - File Data
        write(resource, fm);

        if (paddingSize != 0) {
          // [OPTIONAL] X - Padding to a multiple of 4096 bytes
          int padding = calculatePadding(resource.getDecompressedLength(), paddingSize);
          for (int p = 0; p < padding; p++) {
            fm.writeByte(0);
          }
        }

        TaskProgressManager.setValue(i);
      }

      // Write Directory
      TaskProgressManager.setMessage(Language.get("Progress_WritingDirectory"));

      // 4 - Number of Files
      fm.writeInt(IntConverter.changeFormat(numFiles));

      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];
        long length = resource.getDecompressedLength();

        // 4 - Hash?
        fm.writeInt(fileHashes1[i]);

        // 4 - File Offset
        fm.writeInt(IntConverter.changeFormat((int) offsets[i]));

        // 4 - File Length
        fm.writeInt(IntConverter.changeFormat((int) length));

        // X - Filename
        fm.writeString(names[i]);

        // 1 - null Filename Terminator
        fm.writeByte(0);

        // 8 - Hash/CRC? (maybe TGI like in The Sims?)
        fm.writeLong(fileHashes2[i]);

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

    if (headerInt1 == 1415071308) {
      return "txfl";
    }

    return null;
  }

}
