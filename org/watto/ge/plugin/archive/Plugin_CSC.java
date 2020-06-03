
package org.watto.ge.plugin.archive;

import java.io.File;
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
import org.watto.io.converter.ByteConverter;
import org.watto.io.converter.IntConverter;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_CSC extends ArchivePlugin {

  int numDirectories = 0;

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Plugin_CSC() {

    super("CSC", "CSC");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Deus Ex: Invisible War",
        "Thief 3: Deadly Shadows");
    setExtensions("csc"); // MUST BE LOWER CASE
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

      // Main Type
      if (fm.readShort() == 13) {
        rating += 5;
      }

      // Sub Type
      int subType = fm.readShort();
      if (subType == 2 || subType == 5) {
        rating += 5;
      }

      // Number Of Files
      if (FieldValidator.checkNumFiles(fm.readInt())) {
        rating += 5;
      }

      fm.skip(1);

      long arcSize = fm.getLength();

      // Directory Offset
      if (FieldValidator.checkOffset(fm.readInt(), arcSize)) {
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
  @SuppressWarnings("unused")
  public Resource[] read(File path) {
    try {

      // NOTE - Compressed files MUST know their DECOMPRESSED LENGTH
      //      - Uncompressed files MUST know their LENGTH

      addFileTypes();

      //ExporterPlugin exporter = Exporter_ZLib.getInstance();

      // RESETTING GLOBAL VARIABLES
      numDirectories = 0;

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 2 - Main Type (13)
      // 2 - Sub Type (2=SchemaMetafile, 5=SchemaMetafile_HardDrive)
      fm.skip(4);

      // 4 - Number Of Files
      int numFiles = fm.readInt();
      //FieldValidator.checkNumFiles(numFiles);
      numFiles = Archive.getMaxFiles();

      // 1 - Unknown (1)
      fm.skip(1);

      // 4 - Directory Offset
      int dirOffset = fm.readInt();
      FieldValidator.checkOffset(dirOffset, arcSize);

      fm.seek(dirOffset);

      /*
       * // 1 - Number Of Directories and Sub-Directories numDirectories =
       * ByteConverter.unsign(fm.readByte());
       *
       *
       * while (numDirectories > 0){ readDirectory(fm,""); }
       *
       *
       *
       * // 2 - Number Of Blocks Entries short numBlocks = fm.readShort();
       * FieldValidator.checkNumFiles(numBlocks);
       *
       * for (int i=0;i<numBlocks;i++){
       *
       * // 1 - Number Of Blocks (2/3/4) int numInnerBlocks =
       * ByteConverter.unsign(fm.readByte());
       *
       * // for each block // 4 - Unknown // 1 - null
       *
       * // 4 - Unknown fm.skip(numInnerBlocks*5 + 4); }
       *
       *
       *
       * // 2 - Number Of Name Entries short numNames = fm.readShort();
       * FieldValidator.checkNumFiles(numNames);
       *
       * for (int i=0;i<numNames;i++){ // 4 - Filename Length int filenameLength = fm.readInt();
       * FieldValidator.checkFilenameLength(filenameLength);
       *
       * // X - Filename String filename = fm.readString(filenameLength);
       *
       * // 4 - Unknown fm.skip(4); }
       */

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      int realNumFiles = 0;
      long bestOffset = arcSize;
      while (fm.getOffset() < bestOffset) {
        // 1 - null
        // 4 - Unknown
        // 4 - Unknown
        // 16 - null
        // 4 - Unknown (BIG ENDIAN)
        // 1 - Unknown
        fm.skip(30);

        // 4 - Entry Type (BIG ENDIAN)
        int entryType = IntConverter.changeFormat(fm.readInt());

        // 4 - Unknown (0/1/2)
        int numPadding = fm.readInt();

        // 6 - null
        fm.skip(6);

        // 1 - Unknown
        fm.skip(1);

        // 1 - Number Of Entries In Group
        int numEntries = ByteConverter.unsign(fm.readByte());

        boolean created = false;
        for (int j = 0; j < numEntries; j++) {
          //System.out.println(fm.getOffset());

          // 4 - Filename Length
          int filenameLength = fm.readInt();
          FieldValidator.checkFilenameLength(filenameLength);

          // X - Filename
          String filename = fm.readString(filenameLength);

          //System.out.println(filename);

          // 4 - File Offset (XOR byte 4 with the number 64) (if fileLength == 0, this is a Hash, not a fileOffset?)
          byte[] offsetBytes = fm.readBytes(4);

          // 4 - File Length
          long length = fm.readInt();
          FieldValidator.checkLength(length, arcSize);

          // 4 - Unknown (null for file)
          int check1 = fm.readInt();

          // 4 - Unknown (null for file)
          int check2 = fm.readInt();

          long offset = fm.getOffset();

          for (int l = 0; l < 5; l++) {
            // 4 - String Length
            int textLength = fm.readInt();
            FieldValidator.checkLength(textLength, arcSize);

            // X - String
            fm.skip(textLength);
          }

          // work out the offset
          if (check1 == 0 && check2 == 0 && length > 0) {
            if ((offsetBytes[3] & 160) == 160) {
              //System.out.println("skipping 160 " + filename + " at " + fm.getOffset());
              continue;
            }
            else if ((offsetBytes[3] & 128) == 128) {
              //System.out.println("skipping 128 " + filename + " at " + fm.getOffset());
              continue;
            }
            else if ((offsetBytes[3] & 96) == 96) {
              //System.out.println("skipping 128 " + filename + " at " + fm.getOffset());
              continue;
            }

            offsetBytes[3] &= 31;
            offset = IntConverter.convertLittle(offsetBytes);
            FieldValidator.checkOffset(offset, arcSize);

            if (offset < bestOffset) {
              if (offset > dirOffset) {
                bestOffset = offset;
              }
              else {
                //System.out.println(filename + " at " + fm.getOffset());
              }
            }

          }
          else {
            length = fm.getOffset() - offset;
          }

          //path,name,offset,length,decompLength,exporter
          resources[realNumFiles] = new Resource(path, filename, offset, length);
          realNumFiles++;

          TaskProgressManager.setValue(realNumFiles);

        }
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
   * Reads an [archive] File into the Resources
   **********************************************************************************************
   **/
  public void readDirectory(FileManipulator fm, String parent) throws Exception {
    // 4 - Filename Length
    int filenameLength = fm.readInt();
    FieldValidator.checkFilenameLength(filenameLength);

    // X - Filename
    String filename = fm.readString(filenameLength);

    if (filename.length() > 1 && filename.charAt(0) == '+') {
      // directory
      numDirectories--;

      // 1 - Number Of Sub-directories in this Directory
      int numDirs = ByteConverter.unsign(fm.readByte());

      // 1 - Number Of File in this Directory
      int numFiles = ByteConverter.unsign(fm.readByte());

      for (int i = 0; i < numFiles; i++) {
        readDirectory(fm, parent + filename + "\\");
      }

      for (int i = 0; i < numDirs; i++) {
        readDirectory(fm, parent + filename + "\\");
      }
    }
  }

}
