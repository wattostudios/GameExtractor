
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
import org.watto.io.StringHelper;
import org.watto.io.converter.IntConverter;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_HIP_HIPA extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_HIP_HIPA() {

    super("HIP_HIPA", "HIP_HIPA");

    //         read write replace rename
    setProperties(true, false, false, false);

    setExtensions("hip", "hop");
    setGames("The Incredibles",
        "Spongebob Squarepants: The Movie");
    setPlatforms("PC", "XBox");

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
      if (fm.readString(4).equals("HIPA")) {
        rating += 50;
      }

      // null
      if (IntConverter.changeFormat(fm.readInt()) == 0) {
        rating += 5;
      }

      // PACK Header
      if (fm.readString(4).equals("PACK")) {
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

      long arcSize = fm.getLength();

      int numFiles = Archive.getMaxFiles(4);
      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(arcSize);

      // 4 - Archive Header (HIPA)
      // 4 - null
      fm.skip(8);

      // 4 - Type Code
      String typeCode = fm.readString(4);
      while (!typeCode.equals("DICT")) {
        if (fm.getOffset() >= arcSize) {
          throw new WSPluginException("Missing Directory");
        }

        // 4 - Length Of Section
        int sectionLength = IntConverter.changeFormat(fm.readInt());
        FieldValidator.checkLength(sectionLength, arcSize);

        fm.skip(sectionLength);
        typeCode = fm.readString(4);
      }

      // 4 - Directory Header (DICT) // FOUND ABOVE

      // 4 - Directory Length
      fm.skip(4);

      // 4 - Table Of Contents Header (ATOC)
      typeCode = fm.readString(4);
      while (!typeCode.equals("ATOC")) {
        if (fm.getOffset() >= arcSize) {
          throw new WSPluginException("Missing Directory");
        }

        // 4 - Length Of Section
        int sectionLength = IntConverter.changeFormat(fm.readInt());
        FieldValidator.checkLength(sectionLength, arcSize);

        fm.skip(sectionLength);
        typeCode = fm.readString(4);
      }

      // 4 - Table Of Contents Length
      fm.skip(4);

      // 4 - Information Header (AINF)
      typeCode = fm.readString(4);
      while (!typeCode.equals("AHDR")) {
        // FINDS THE FIRST FILE HEADER
        if (fm.getOffset() >= arcSize) {
          throw new WSPluginException("Missing Directory");
        }

        // 4 - Length Of Section
        int sectionLength = IntConverter.changeFormat(fm.readInt());
        FieldValidator.checkLength(sectionLength, arcSize);

        fm.skip(sectionLength);
        typeCode = fm.readString(4);
      }

      int realNumFiles = 0;
      while (typeCode.equals("AHDR")) {
        // 4 - File Entry Header (AHDR)
        // 4 - File Entry Length
        // 4 - Unknown
        fm.skip(8); // first field is already found

        // 4 - File Type Code (filled to length with (byte)32 - "spaces")
        String ext = StringHelper.readTerminatedString(fm.getBuffer(), (byte) 32, 4);

        // 4 - File Offset?
        long offset = IntConverter.changeFormat(fm.readInt());
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - File Length?
        long length = IntConverter.changeFormat(fm.readInt());
        FieldValidator.checkLength(length, arcSize);

        // 4 - File Type ID? (12=SCRP)
        // 4 - Unknown (2)
        // 4 - File Details Header (ADBG)
        fm.skip(12);

        // 4 - File Details Header Length
        int filenameLength = IntConverter.changeFormat(fm.readInt()) - 8;

        // 4 - null
        fm.skip(4);

        // X - Filename (null)
        // X - null Padding (use the FileDetailsHeaderLength to determine the size of this field)
        String filename = fm.readNullString(filenameLength) + "." + ext.toLowerCase();

        // 4 - Unknown (Hash or something?)
        fm.skip(4);

        //path,id,name,offset,length,decompLength,exporter
        resources[realNumFiles] = new Resource(path, filename, offset, length);
        //resources[realNumFiles] = new Resource(path,filename,offset);
        realNumFiles++;

        TaskProgressManager.setValue(offset);

        typeCode = fm.readString(4);
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

}