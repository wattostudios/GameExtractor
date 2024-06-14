/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2023 wattostudios
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
import org.watto.ge.plugin.exporter.Exporter_ZLib;
import org.watto.io.FileManipulator;
import org.watto.io.buffer.ByteBuffer;
import org.watto.io.converter.IntConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_VFS_VFS2 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_VFS_VFS2() {

    super("VFS_VFS2", "VFS_VFS2");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Digimon Rumble Arena");
    setExtensions("vfs"); // MUST BE LOWER CASE
    setPlatforms("PS1");

    // MUST BE LOWER CASE !!!
    setFileTypes(new FileType("tim", "TIM Image Archive", FileType.TYPE_ARCHIVE),
        new FileType("vb", "VAG Audio", FileType.TYPE_AUDIO));

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

      // Header
      if (fm.readString(4).equals("VFS2")) {
        rating += 50;
      }

      // Number Of Files
      if (FieldValidator.checkNumFiles(fm.readInt())) {
        rating += 5;
      }

      long arcSize = fm.getLength();

      // Directory Offset
      if (FieldValidator.checkOffset(fm.readInt() * 2048, arcSize)) {
        rating += 5;
      }

      // Directory Length
      if (FieldValidator.checkLength(fm.readInt() - 2048, arcSize)) {
        rating += 5;
      }

      // null
      if (fm.readLong() == 0) {
        rating += 5;
      }

      return rating;

    }
    catch (Throwable t) {
      return 0;
    }
  }

  int realNumFiles = 0;

  int dataOffset = 0;

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

      Exporter_ZLib exporter = Exporter_ZLib.getInstance();

      // RESETTING GLOBAL VARIABLES
      realNumFiles = 0;

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 4 - Header (VFS2)
      fm.skip(4);

      // 4 - Number of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // 4 - File Data Offset [*2048]
      dataOffset = fm.readInt() * 2048;
      FieldValidator.checkOffset(dataOffset, arcSize);

      // 4 - Compressed Directory Length [-2048]
      int compDirLength = fm.readInt() - 2048;
      FieldValidator.checkLength(compDirLength, arcSize);

      int decompDirLength = compDirLength * 10; // guess

      // 2032 - null Padding to a multiple of 2048 bytes
      fm.skip(2032);

      // X - Compressed Directory Data (ZLib)
      byte[] dirBytes = new byte[decompDirLength];
      int decompWritePos = 0;
      exporter.open(fm, compDirLength, decompDirLength);

      for (int b = 0; b < decompDirLength; b++) {
        if (exporter.available()) { // make sure we read the next bit of data, if required
          dirBytes[decompWritePos++] = (byte) exporter.read();
        }
      }

      // get the filenames from the decompressed directory
      int filenameDirLength = decompDirLength - (numFiles * 12);
      byte[] filenameBytes = new byte[filenameDirLength];
      System.arraycopy(dirBytes, numFiles * 12, filenameBytes, 0, filenameDirLength);

      // open the decompressed data for processing
      FileManipulator dirFM = new FileManipulator(new ByteBuffer(dirBytes));

      FileManipulator nameFM = new FileManipulator(new ByteBuffer(filenameBytes));

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      readDirectory(path, dirFM, nameFM, "", resources, arcSize);

      resources = resizeResources(resources, realNumFiles);

      nameFM.close();

      dirFM.close();

      // now go through and detect compressed files
      numFiles = realNumFiles;

      fm.getBuffer().setBufferSize(8);
      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];

        fm.seek(resource.getOffset());

        if (fm.readString(4).equals("ZP00")) {
          // compressed

          int decompLength = fm.readInt();
          FieldValidator.checkLength(decompLength);

          resource.setOffset(fm.getOffset());
          resource.setLength(resource.getLength() - 8);
          resource.setDecompressedLength(decompLength);
          resource.setExporter(exporter);
        }
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
   *
   **********************************************************************************************
   **/

  public void readDirectory(File path, FileManipulator fm, FileManipulator nameFM, String dirName, Resource[] resources, long arcSize) {
    try {

      int numEntries = 1;

      while (numEntries > 0) {

        // 4 - File Offset [*2048] (relative to the start of the file data)
        int offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - File Length
        int length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 3 - Filename Offset (relative to the start of the filename directory)
        byte[] nameBytes = new byte[] { fm.readByte(), fm.readByte(), fm.readByte(), 0 };
        int nameOffset = IntConverter.convertLittle(nameBytes);

        // 1 - Flags
        int flags = fm.readByte();

        if (flags == 8) {
          // sub-directory
          nameFM.relativeSeek(nameOffset);

          String name = nameFM.readNullString();
          FieldValidator.checkFilename(name);

          if (name.equals(".")) {
            numEntries = length;
          }
          else if (name.equals("..")) {
            // don't care about the parent
          }
          else {
            // a sub-directory

            // we can skip to it now and read it, because it's all in memory anyway, so we're not skipping around on disk, it'll be quick

            long thisOffset = fm.getOffset();
            fm.relativeSeek(offset);
            readDirectory(path, fm, nameFM, dirName + name + File.separatorChar, resources, arcSize);
            fm.relativeSeek(thisOffset);
          }

        }
        else {
          // file

          offset = (offset * 2048) + dataOffset;

          nameFM.relativeSeek(nameOffset);

          String name = nameFM.readNullString();
          FieldValidator.checkFilename(name);

          String filename = dirName + name;

          //path,name,offset,length,decompLength,exporter
          resources[realNumFiles] = new Resource(path, filename, offset, length);
          realNumFiles++;

          TaskProgressManager.setValue(realNumFiles);
        }

        numEntries--;
      }

    }
    catch (Throwable t) {
      logError(t);
      return;
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
