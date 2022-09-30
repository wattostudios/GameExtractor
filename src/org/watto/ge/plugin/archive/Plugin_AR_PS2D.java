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
import org.watto.datatype.FileType;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.exporter.Exporter_Custom_VAG_Audio;
import org.watto.io.FileManipulator;
import org.watto.io.FilenameSplitter;
import org.watto.io.buffer.ByteBuffer;
import org.watto.io.converter.IntConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_AR_PS2D extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_AR_PS2D() {

    super("AR_PS2D", "AR_PS2D");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Big Mutha Truckers");
    setExtensions("ar"); // MUST BE LOWER CASE
    setPlatforms("PS2");

    setFileTypes(new FileType("snd", "VAG Audio", FileType.TYPE_AUDIO),
        new FileType("spd", "VAG Audio", FileType.TYPE_AUDIO));

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

      String baseDir = fm.getFile().getParentFile().getAbsolutePath();
      String dirFile = baseDir + File.separatorChar + "CDFILES.DAT";

      if (new File(dirFile).exists()) {
        rating += 25;
      }
      else {
        return 0;
      }

      // Header
      if (fm.readString(4).equals("PS2D")) {
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

      ExporterPlugin exporter = Exporter_Custom_VAG_Audio.getInstance();

      // RESETTING GLOBAL VARIABLES

      long arcSize = (int) path.length();

      String baseDir = path.getParentFile().getAbsolutePath();
      String dirFile = baseDir + File.separatorChar + "CDFILES.DAT";
      File sourcePath = new File(dirFile);

      if (!sourcePath.exists()) {
        return null;
      }

      long dirFileSize = (int) sourcePath.length();

      FileManipulator fm = new FileManipulator(sourcePath, false);

      // 4 - Header ("file")
      // 4 - Version? (1)
      // 4 - Unknown
      // 4 - Unknown (1)
      // 4 - Unknown (3)
      // 4 - Unknown (1)
      // 4 - Unknown (2)
      fm.skip(28);

      // 4 - Number of Files #1
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // 4 - Unknown (11)
      // 4 - Unknown (8)
      fm.skip(8);

      // 4 - Number of Names
      int numNames = fm.readInt();
      FieldValidator.checkNumFiles(numNames);

      // 4 - Filename Directory Length
      int dirLength = fm.readInt();
      FieldValidator.checkLength(dirLength, dirFileSize);

      // 4 - Unknown
      // 4 - null
      // 2 - Unknown
      fm.skip(10);

      // 10 - Archive Filename (ARCHIV.AR)
      // 1 - null Filename Terminator
      fm.readNullString();

      long baseOffset = fm.getOffset();

      // read the filename directory
      long filenameDirOffset = baseOffset + (numFiles * 8) + (numNames * 8);
      FieldValidator.checkOffset(filenameDirOffset, dirFileSize);

      fm.relativeSeek(filenameDirOffset);

      byte[] nameBytes = fm.readBytes(dirLength);
      FileManipulator nameFM = new FileManipulator(new ByteBuffer(nameBytes));

      fm.relativeSeek(baseOffset);

      // read the offsets
      int[] offsets = new int[numFiles];
      for (int i = 0; i < numFiles; i++) {
        // 4 - File Offset [*8]
        int offset = fm.readInt() * 8;
        FieldValidator.checkOffset(offset, arcSize);
        offsets[i] = offset;
      }

      // read the file lengths
      int[] lengths = new int[numFiles];
      for (int i = 0; i < numFiles; i++) {
        // 4 - File Length
        int length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);
        lengths[i] = length;
      }

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // read the filenames
      String[] names = new String[numNames];
      for (int i = 0; i < numNames; i++) {
        // 4 - Filename Offset (relative to the start of the filename directory)
        int filenameOffset = fm.readInt();
        FieldValidator.checkOffset(filenameOffset, dirLength);

        // X - Filename (null)
        nameFM.seek(filenameOffset);
        String filename = nameFM.readNullString();
        FieldValidator.checkFilename(filename);

        names[i] = filename;
      }

      // Read the filename IDs
      String[] filenames = new String[numFiles];

      for (int i = 0; i < numNames; i++) {

        // 3 - File ID that this Name belongs to
        // 1 - Entry Type (0=Archive Name, 64=Filename)
        byte[] fileIDBytes = fm.readBytes(4);

        int entryType = fileIDBytes[3];
        fileIDBytes[3] = 0;

        if (entryType == 64) {
          // a file

          int fileID = IntConverter.convertLittle(fileIDBytes);
          FieldValidator.checkRange(fileID, 0, numFiles);

          filenames[fileID] = names[i];
        }
      }

      // now create the Resources
      for (int i = 0; i < numFiles; i++) {
        long offset = offsets[i];
        long length = lengths[i];
        String filename = filenames[i];

        String extension = FilenameSplitter.getExtension(filename).toLowerCase();
        if (extension.equals("snd") || extension.equals("spd")) {
          // VAG audio

          //path,name,offset,length,decompLength,exporter
          resources[i] = new Resource(path, filename, offset, length, length, exporter);
        }
        else {
          // any other file

          //path,name,offset,length,decompLength,exporter
          resources[i] = new Resource(path, filename, offset, length);
        }

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

    /*
    if (headerInt1 == 2037149520) {
      return "js";
    }
    */

    return null;
  }

}
