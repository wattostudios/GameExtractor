
package org.watto.ge.plugin.archive;

import java.io.File;
import org.watto.component.WSPluginException;
import org.watto.task.TaskProgressManager;
import org.watto.datatype.Archive;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
////////////////////////////////////////////////////////////////////////////////////////////////
//                                                                                            //
//                                       GAME EXTRACTOR                                       //
//                               Extensible Game Archive Editor                               //
//                                http://www.watto.org/extract                                //
//                                                                                            //
//                           Copyright (C) 2002-2009  WATTO Studios                           //
//                                                                                            //
// This program is free software; you can redistribute it and/or modify it under the terms of //
// the GNU General Public License published by the Free Software Foundation; either version 2 //
// of the License, or (at your option) any later versions. This program is distributed in the //
// hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranties //
// of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License //
// at http://www.gnu.org for more details. For updates and information about this program, go //
// to the WATTO Studios website at http://www.watto.org or email watto@watto.org . Thanks! :) //
//                                                                                            //
////////////////////////////////////////////////////////////////////////////////////////////////
import org.watto.io.FileManipulator;
import org.watto.io.converter.IntConverter;
import org.watto.io.converter.ShortConverter;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_LIF_LIFF extends ArchivePlugin {

  int realNumFiles = 0;
  int relOffset = 0;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_LIF_LIFF() {

    super("LIF_LIFF", "LIF_LIFF");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Lego Digital Designer");
    setExtensions("lif"); // MUST BE LOWER CASE
    setPlatforms("PC");

    //setFileTypes("","",
    //             "",""
    //             );

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
      if (fm.readString(4).equals("LIFF")) {
        rating += 50;
      }

      // null
      if (fm.readInt() == 0) {
        rating += 5;
      }

      long arcSize = fm.getLength();

      // Archive Size
      if (FieldValidator.checkEquals(fm.readInt(), arcSize)) {
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
      realNumFiles = 0;
      relOffset = 84;

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 4 - Header (LIFF)
      // 4 - null
      // 4 - Archive Length
      // 2 - Unknown (2)
      // 4 - Hash?

      // ARCHIVE ENTRY
      // 2 - Entry Tag (1)
      // 2 - Entry Type (1=archive info)
      // 4 - null
      // 4 - Archive Length (including these 20 bytes of header) [+18]
      // 4 - null
      // 4 - Hash?

      // UNKNOWN ENTRY
      // 2 - Entry Tag (1)
      // 2 - Entry Type (2=unknown info)
      // 4 - null
      // 4 - Entry Length (26) (including these 20 bytes of header)
      // 4 - Unknown (1)
      // 4 - Hash?
      // 2 - Unknown (1)
      // 4 - null

      // FILE DATA ENTRY
      // 2 - Entry Tag (1)
      // 2 - Entry Type (3=file data info)
      // 4 - null
      fm.skip(72);

      // 4 - File Data Length (including these 20 bytes of header)
      int dirOffset = IntConverter.changeFormat(fm.readInt()) + 64;
      FieldValidator.checkOffset(dirOffset, arcSize);

      // 4 - null
      // 4 - Hash?
      fm.seek(dirOffset);

      int numFiles = Archive.getMaxFiles();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // 2 - Entry Tag (1)
      // 2 - Entry Type (5=directory)
      // 4 - null
      // 4 - Directory Length
      // 4 - Unknown (1)
      // 4 - Hash?

      // 2 - Entry Type (1=directory, 2=file)
      // 4 - Unknown (0=root, 5=file, 7=directory)
      // X - File/Directory Name (Unicode) (root entry has a single null byte here)
      // 1 - null File/Directory Name Terminator
      // 4 - null
      // 4 - Unknown (20)

      fm.skip(36);

      // 4 - Number Of Files In Directory
      int numDirFiles = IntConverter.changeFormat(fm.readInt());
      FieldValidator.checkNumFiles(numDirFiles);

      for (int i = 0; i < numDirFiles; i++) {
        readDirectory(fm, path, resources, "", arcSize);
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
  public void readDirectory(FileManipulator fm, File path, Resource[] resources, String dirName, long arcSize) throws Exception {
    // 2 - Entry Type (1=directory, 2=file)
    int entryType = ShortConverter.changeFormat(fm.readShort());

    // 4 - Unknown (0=root, 5=file, 7=directory)
    fm.skip(4);

    // X - File/Directory Name (Unicode) (root entry has a single null byte here)
    String filename = fm.readNullUnicodeString();
    char[] chars = filename.toCharArray(); // need to swap the byte order because it is big endian
    for (int i = 0; i < chars.length; i++) {
      chars[i] = (char) (((chars[i] & 0xFF) << 8) | ((chars[i] & 0xFF00) >> 8));
      //System.out.println((chars[i]&0xFF) + " - " + ((chars[i]&0xFF00)>>8));
    }
    filename = new String(chars);
    FieldValidator.checkFilename(filename);

    // 1 - null File/Directory Name Terminator

    // 4 - null
    fm.skip(4);

    if (entryType == 1) {
      // directory

      // 4 - Unknown (20)
      fm.skip(4);

      // 4 - Number Of Files In Directory
      int numDirFiles = IntConverter.changeFormat(fm.readInt());
      FieldValidator.checkNumFiles(numDirFiles);

      relOffset += 20;

      String newDirName = dirName + filename + "\\";
      for (int i = 0; i < numDirFiles; i++) {
        readDirectory(fm, path, resources, newDirName, arcSize);
      }

    }
    else if (entryType == 2) {
      // file

      // 4 - File Length
      int length = IntConverter.changeFormat(fm.readInt()) - 20;
      FieldValidator.checkLength(length, arcSize);

      // 24 - Checksum?
      fm.skip(24);

      int offset = relOffset + 20;
      FieldValidator.checkOffset(offset, arcSize);

      //path,name,offset,length,decompLength,exporter
      resources[realNumFiles] = new Resource(path, dirName + filename, offset, length);

      TaskProgressManager.setValue(realNumFiles);
      realNumFiles++;

      relOffset += length + 20;
    }
    else {
      throw new WSPluginException("Invalid entry type");
    }

  }

}
