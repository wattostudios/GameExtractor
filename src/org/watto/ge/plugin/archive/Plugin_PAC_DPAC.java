
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

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_PAC_DPAC extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_PAC_DPAC() {

    super("PAC_DPAC", "PAC_DPAC");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Rumble Roses");
    setExtensions("pac");
    setPlatforms("PC");

    setFileTypes("pac", "Index Data?",
        "bpe", "Unknown File",
        "tim2", "Image");

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
      String header = fm.readString(4);
      if (header.equals("DPAC") || header.equals("PAC ")) {
        rating += 50;
      }

      fm.skip(10);

      // null padding to 2048
      if (fm.readInt() == 0) {
        rating += 5;
      }
      if (fm.readInt() == 0) {
        rating += 5;
      }
      if (fm.readInt() == 0) {
        rating += 5;
      }
      if (fm.readInt() == 0) {
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

      int numFiles = Archive.getMaxFiles(4);

      long arcSize = fm.getLength();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // 4 - Header
      if (fm.readString(4).equals("DPAC")) {
        fm.seek(16384);
      }
      else {
        fm.seek(0);
      }

      // Loop through directory
      int realNumFiles = 0;
      while (fm.getOffset() < arcSize) {
        // X - Optional Null Padding
        int nullByte = fm.readByte();
        while (nullByte == 0) {
          nullByte = fm.readByte();
          if (fm.getOffset() >= arcSize) {
            resources = resizeResources(resources, realNumFiles);
            return resources;
          }
        }

        // 4 - Extension
        String ext = ((char) nullByte) + fm.readString(3);

        long length = 0;
        long offset = 0;

        if (ext.equals("PAC ")) {
          // 4 - Number of 8-byte fields
          length = fm.readInt() * 8;
          FieldValidator.checkLength(length, arcSize);

          // X - File Data
          offset = (int) fm.getOffset();
          fm.skip(length);

          ext = "pac";
        }
        else if (ext.equals("BPE ")) {
          // 4 - Version Number? (256)
          fm.skip(4);

          // 4 - Compressed Size
          length = fm.readInt();
          FieldValidator.checkLength(length, arcSize);

          // 4 - Decompressed Size?
          fm.skip(4);

          // X - File Data
          offset = (int) fm.getOffset();
          fm.skip(length);

          ext = "bpe";
        }
        else if (ext.equals("TIM2")) {
          // 2 - Unknown (4)
          // 2 - Unknown (1)
          // 8 - null
          fm.skip(12);

          // 4 - Image Data Length [-4]
          length = fm.readInt() - 4;
          FieldValidator.checkLength(length, arcSize);

          // X - Image Data
          offset = (int) fm.getOffset();
          fm.skip(length);

          ext = "tim2";
        }
        else if (ext.substring(0, 2).equals("; ")) {
          // 8232 - File Data
          length = 8236;
          FieldValidator.checkLength(length, arcSize);

          offset = (int) fm.getOffset() - 4;
          fm.skip(8232);

          ext = "unk";
        }
        else if (ext.substring(0, 2).equals(((char) 9) + ";")) {
          // 4080 - File Data
          length = 4080;
          FieldValidator.checkLength(length, arcSize);

          offset = (int) fm.getOffset() - 4;
          fm.skip(length - 4);

          ext = "unk";
        }

        else {
          //System.out.println(ext + " at " + fm.getOffset());
          // stop at this point, and return all read resources
          // uncompatable files will return null, which then tries other plugins
          if (realNumFiles == 0) {
            return null;
          }
          resources = resizeResources(resources, realNumFiles);
          return resources;
        }

        String filename = Resource.generateFilename(realNumFiles) + "." + ext;

        //path,id,name,offset,length,decompLength,exporter
        resources[realNumFiles] = new Resource(path, filename, offset, length);

        TaskProgressManager.setValue(offset);
        realNumFiles++;
      }

      fm.close();

      resources = resizeResources(resources, realNumFiles);

      return resources;

    }
    catch (Throwable t) {
      logError(t);
      return null;
    }
  }

}
