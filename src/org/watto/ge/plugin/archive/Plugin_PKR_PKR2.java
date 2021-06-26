
package org.watto.ge.plugin.archive;

import java.io.File;
import org.watto.Language;
import org.watto.task.TaskProgressManager;
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

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_PKR_PKR2 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_PKR_PKR2() {

    super("PKR_PKR2", "PKR_PKR2");

    //         read write replace rename
    setProperties(true, true, true, true);

    setExtensions("pkr");
    setGames("Tony Hawk: Pro Skater 2");
    setPlatforms("PC");

    setFileTypes("fnt", "Font File",
        "hed", "CD Data",
        "hep", "CD Data",
        "het", "CD Data",
        "wad", "CD Data",
        "nt", "Initialisation File",
        "prk", "Park File",
        "psh", "Person Setup",
        "rec", "Replay File",
        "sbl", "Soundblaster Settings",
        "sfx", "Sound FX",
        "tag", "Playstation to PC Data",
        "tdf", "Text Definition",
        "ts", "Trick Script");

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
      if (fm.readString(4).equals("PKR2")) {
        rating += 50;
      }

      fm.skip(4);

      // Number Of Directories
      if (FieldValidator.checkNumFiles(fm.readInt())) {
        rating += 5;
      }

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

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public Resource[] read(File path) {
    try {

      addFileTypes();

      FileManipulator fm = new FileManipulator(path, false);

      // 4 - Header
      // 4 - Version
      fm.skip(8);

      // 4 - Number of Directories
      int numDirectories = fm.readInt();
      FieldValidator.checkNumFiles(numDirectories);

      // 4 - Number of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      long arcSize = fm.getLength();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      int currentFile = 0;
      int arraypos = 16;
      for (int i = 0; i < numDirectories; i++) {
        // 32 - Directory Name
        fm.seek(arraypos);
        String directoryName = fm.readNullString(32);
        //FieldValidator.checkFilename(directoryName);

        // 4 - Offset To Directory
        int curDirOffset = fm.readInt();
        FieldValidator.checkOffset(curDirOffset, arcSize);

        // 4 - Number Of Files
        int curNumFiles = fm.readInt();
        FieldValidator.checkNumFiles(curNumFiles);

        // LOOP THROUGH THE ENTRIES IN EACH DIRECTORY
        fm.seek(curDirOffset);
        int readLength = 0;
        for (int j = 0; j < curNumFiles; j++) {

          // 32 - File Name
          String filename = directoryName + fm.readNullString(32);
          FieldValidator.checkFilename(filename);

          // 4 - Unknown (-2)
          fm.skip(4);

          // 4 - Data Offset
          long offset = fm.readInt();
          FieldValidator.checkOffset(offset, arcSize);

          // 4 - Raw File Length
          long length = fm.readInt();
          FieldValidator.checkLength(length, arcSize);

          // 4 - Compressed File Length
          fm.skip(4);

          //path,id,name,offset,length,decompLength,exporter
          resources[currentFile] = new Resource(path, filename, offset, length);

          currentFile++;
          TaskProgressManager.setValue(readLength);
          readLength += length;
        }

        arraypos += 40;// Go to next directory
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
  
  **********************************************************************************************
  **/
  @Override
  public void write(Resource[] resources, File path) {
    try {

      FileManipulator fm = new FileManipulator(path, true);

      int numFiles = resources.length;
      TaskProgressManager.setMaximum(numFiles);

      //Header-4
      fm.writeString("PKR2");

      //Version-4
      fm.writeInt((int) 1);

      TaskProgressManager.setMessage(Language.get("Progress_PerformingCalculations"));
      int numDirectories = 0;
      String[] dirNames = new String[1000];
      int[] dirNumFiles = new int[1000];
      for (int i = 0; i < numFiles; i++) {
        String name = resources[i].getName();
        int lastDirSlash = name.lastIndexOf("/") + 1;
        if (lastDirSlash >= 0) {
          if (numDirectories == 0) {
            dirNames[numDirectories] = name.substring(0, lastDirSlash);
            dirNumFiles[numDirectories] = 1;
            numDirectories++;
          }
          else {
            if (dirNames[numDirectories - 1].equals(name.substring(0, lastDirSlash))) {
              // Directory already exists
              dirNumFiles[numDirectories - 1] = dirNumFiles[numDirectories - 1] + 1;
            }
            else {
              dirNames[numDirectories] = name.substring(0, lastDirSlash);
              dirNumFiles[numDirectories] = 1;
              numDirectories++;
            }
          }
        }
      }

      //NumDirectories-4
      fm.writeInt((int) numDirectories);

      //numFiles-4
      fm.writeInt((int) numFiles);

      TaskProgressManager.setMessage(Language.get("Progress_WritingDirectory"));
      long dirOffset = 16 + (numDirectories * 40);
      for (int i = 0; i < numDirectories; i++) {
        //DirectoryName-32
        fm.writeNullString(dirNames[i], 32);

        //DirectoryOffset-4
        fm.writeInt((int) dirOffset);

        //DirectoryNumberOfFiles-4
        fm.writeInt((int) dirNumFiles[i]);

        dirOffset += (dirNumFiles[i] * 48);
      }

      int fileEntryOffset = 16 + (numDirectories * 40) + (numFiles * 48);
      for (int i = 0; i < numFiles; i++) {
        String name = resources[i].getName();
        long length = resources[i].getDecompressedLength();

        //FileName-32
        int lastDirSlash = name.lastIndexOf("/") + 1;
        if (lastDirSlash >= 0) {
          name = name.substring(lastDirSlash, name.length());
        }
        fm.writeNullString(name, 32);

        //UnknownFiller-4 (-2)
        fm.writeInt((int) -2);

        //Data Offset-4
        fm.writeInt((int) fileEntryOffset);

        //Length-4
        fm.writeInt((int) length);

        //CompressedFileLength-4
        fm.writeInt((int) length);

        fileEntryOffset += length;
      }

      TaskProgressManager.setMessage(Language.get("Progress_WritingFiles"));
      write(resources, fm);

      fm.close();

    }
    catch (Throwable t) {
      logError(t);
    }
  }

}