
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
public class Plugin_FFL_RFFL extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_FFL_RFFL() {

    super("FFL_RFFL", "FFL_RFFL");

    //         read write replace rename
    setProperties(true, true, true, true);

    setExtensions("ffl");
    setGames("Alien vs Predator");
    setPlatforms("PC");

    setFileTypes("rim", "Texture Image");

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
      if (fm.readString(4).equals("RFFL")) {
        rating += 50;
      }

      // null
      if (fm.readInt() == 0) {
        rating += 5;
      }

      long arcSize = fm.getLength();

      // Directory Length
      if (FieldValidator.checkLength(fm.readInt(), arcSize)) {
        rating += 5;
      }

      // Body Length
      if (FieldValidator.checkLength(fm.readInt(), arcSize)) {
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
      // 4 - Blank
      fm.skip(8);

      // 4 - Number Of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      long arcSize = fm.getLength();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // 4 - Directory Length
      int dirLength = fm.readInt();
      FieldValidator.checkLength(dirLength, arcSize);

      // 4 - Body Length
      fm.skip(4);

      int readLength = 0;
      for (int i = 0; i < numFiles; i++) {
        // 4 - Data Offset
        long offset = dirLength + 20 + fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - File Length
        long length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // X - Filename (multiples of 4)
        String filename = fm.readNullString();
        FieldValidator.checkFilename(filename);

        if ((filename.length() + 1) % 4 != 0) { //corrects to a multiple of 4 bytes
          for (int j = 0; j < 4 - ((filename.length() + 1) % 4); j++) {
            fm.skip(1);
          }
        }

        //path,id,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, filename, offset, length);

        TaskProgressManager.setValue(readLength);
        readLength += length;
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

      // 4 - Header
      fm.writeString("RFFL");

      // 4 - Blank
      fm.writeInt((int) 0);

      // 4 - Number Of Files
      fm.writeInt((int) numFiles);

      TaskProgressManager.setMessage(Language.get("Progress_PerformingCalculations"));
      int totalLengthOfData = 0;
      int totalLengthOfDirectory = 0;
      for (int i = 0; i < numFiles; i++) {
        if (resources[i].getLength() >= 0) {
          long fileLength = resources[i].getDecompressedLength();
          if ((4 - (fileLength % 4)) != 0) {
            totalLengthOfData += fileLength + (4 - (fileLength % 4));
          }
          else {
            totalLengthOfData += fileLength;
          }
          int filenameLength = resources[i].getName().length();
          if (((filenameLength + 1) % 4) != 0) {
            totalLengthOfDirectory += 8 + filenameLength + 1 + (4 - ((filenameLength + 1) % 4));
          }
          else {
            totalLengthOfDirectory += 8 + filenameLength + 1;
          }
        }
      }
      //System.out.println(totalLengthOfDirectory);

      // 4 - Directory Length
      fm.writeInt((int) totalLengthOfDirectory);

      // 4 - Body Length
      fm.writeInt((int) totalLengthOfData);

      TaskProgressManager.setMessage(Language.get("Progress_WritingDirectory"));
      int currentPos = 0;
      for (int i = 0; i < numFiles; i++) {
        String name = resources[i].getName();
        long length = resources[i].getDecompressedLength();

        // 4 - Data Offset
        fm.writeInt((int) currentPos);

        // 4 - File Length
        fm.writeInt((int) length);

        // X - Filename (multiples of 4)
        fm.writeNullString(name);

        if ((name.length() + 1) % 4 != 0) { //corrects to a multiple of 4 bytes
          for (int j = 0; j < 4 - ((name.length() + 1) % 4); j++) {
            fm.writeByte(1);// Anything other than null !!!
          }
        }

        currentPos += length;

        // FILE LENGTH MUST BE A MULTIPLE OF 4 (PADDED USING NULL BYTES)
        if (length % 4 != 0) { //corrects to a multiple of 4 bytes
          for (int j = 0; j < 4 - (length % 4); j++) {
            currentPos++;
          }
        }

      }

      //write(resources, fm);

      TaskProgressManager.setMessage(Language.get("Progress_WritingFiles"));
      for (int i = 0; i < numFiles; i++) {

        write(resources[i], fm);

        TaskProgressManager.setValue(i);

        long length = resources[i].getLength();

        // FILE LENGTH MUST BE A MULTIPLE OF 4 (PADDED USING NULL BYTES)
        if (length % 4 != 0) { //corrects to a multiple of 4 bytes
          for (int j = 0; j < 4 - (length % 4); j++) {
            fm.writeByte(0);
          }
        }

      }

      fm.close();

    }
    catch (Throwable t) {
      logError(t);
    }
  }

}