
package org.watto.ge.plugin.archive;

import java.io.File;
import org.watto.Language;
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
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_WAD_WAD3 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_WAD_WAD3() {

    super("WAD_WAD3", "Quake 3D WAD Image Package");

    //         read write replace rename
    setProperties(true, true, true, true);

    setExtensions("wad");
    setGames("Quake",
        "Quake 2",
        "Half-Life",
        "Counter-Strike",
        "Counter-Strike: Condition Zero",
        "Day of Defeat",
        "Half-Life: Blue Shift",
        "Half-Life: Deathmatch Classic",
        "Half-Life: Opposing Force",
        "Ricochet",
        "Team Fortress Classic");
    setPlatforms("PC");

    setFileTypes("unk", "Unknown File",
        "mip", "MIP Texture",
        "fnt", "Font File");

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
      if (fm.readString(4).equals("WAD3")) {
        rating += 50;
      }

      // Number Of Files
      if (FieldValidator.checkNumFiles(fm.readInt())) {
        rating += 5;
      }

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
  
  **********************************************************************************************
  **/
  @Override
  public Resource[] read(File path) {
    try {

      addFileTypes();

      FileManipulator fm = new FileManipulator(path, false);
      // 4 - Header
      fm.skip(4);

      // 4 - Number Of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      long arcSize = fm.getLength();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // 4 - Directory Offset
      long dirOffset = fm.readInt();
      FieldValidator.checkOffset(dirOffset, arcSize);

      fm.seek(dirOffset);

      for (int i = 0; i < numFiles; i++) {
        // 4 - Data Offset
        long offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - Raw File Length
        long length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - Compressed File Length
        fm.skip(4);

        // 1 Byte - File Type
        int fileType = fm.readByte();

        // 1 Byte - Compression Type

        // 2 - Padding
        fm.skip(3);

        // 16 - Filename
        String filename = fm.readNullString(16);
        FieldValidator.checkFilename(filename);

        //String type = "" + fileType;
        if (fileType == 66) {
          filename += ".unk";
        }
        else if (fileType == 67) {
          filename += ".mip";
        }
        else if (fileType == 70) {
          filename += ".fnt";
        }
        else {
          filename += ".unk";
        }

        //path,id,name,offset,length,decompLength,exporter
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
  
  **********************************************************************************************
  **/
  @Override
  public void write(Resource[] resources, File path) {
    try {

      FileManipulator fm = new FileManipulator(path, true);

      int numFiles = resources.length;
      TaskProgressManager.setMaximum(numFiles);

      // 4 - Header
      fm.writeString("WAD3");

      // 4 - Number Of Files
      fm.writeInt(numFiles);

      TaskProgressManager.setMessage(Language.get("Progress_PerformingCalculations"));
      int totalLengthOfData = 0;
      int totalLengthOfHeader = 12;
      for (int i = 0; i < numFiles; i++) {
        if (resources[i].getDecompressedLength() >= 0) {
          totalLengthOfData += resources[i].getDecompressedLength();
        }
      }

      //DirOffset-4
      fm.writeInt(totalLengthOfData + totalLengthOfHeader);

      TaskProgressManager.setMessage(Language.get("Progress_WritingFiles"));
      write(resources, fm);

      TaskProgressManager.setMessage(Language.get("Progress_WritingDirectory"));
      int currentPos = totalLengthOfHeader;
      for (int i = 0; i < numFiles; i++) {
        String name = resources[i].getName();
        long decompLength = resources[i].getDecompressedLength();
        long length = resources[i].getLength();
        String ext = "";

        if (name.indexOf(".") > -1) {
          ext = name.substring(name.lastIndexOf(".") + 1);
          name = name.substring(0, name.lastIndexOf("."));
        }

        int fileType = 0;
        if (ext.equals("unk")) {
          fileType = 66;
        }
        else if (ext.equals("mip")) {
          fileType = 67;
        }
        else if (ext.equals("fnt")) {
          fileType = 70;
        }

        // 4 - Data Offset
        fm.writeInt(currentPos);

        // 4 - Raw File Length
        fm.writeInt((int) decompLength);

        // 4 - Compressed File Length
        fm.writeInt((int) length);

        // 1 Byte - File Type
        fm.writeByte(fileType);

        // 1 Byte - Compression Type
        fm.writeByte(0);

        // 2 - Padding
        fm.writeShort((short) 0);

        // 16 - Filename
        fm.writeNullString(name, 16);

        currentPos += length;
      }

      fm.close();

    }
    catch (Throwable t) {
      logError(t);
    }
  }

}