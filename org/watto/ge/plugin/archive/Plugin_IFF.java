
package org.watto.ge.plugin.archive;

import java.io.File;
import org.watto.Language;
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
import org.watto.ge.plugin.resource.Resource_FileID;
import org.watto.io.FileManipulator;
import org.watto.io.converter.IntConverter;
import org.watto.io.converter.ShortConverter;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_IFF extends ArchivePlugin {

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Plugin_IFF() {

    super("IFF", "The Sims IFF Object");

    //         read write replace rename
    setProperties(true, true, true, true);

    setExtensions("iff", "wll", "flr", "spf");
    setGames("The Sims",
        "The Sims Online");
    setPlatforms("PC");

    setFileTypes("bmp_", "Bitmap Image",
        "slot", "Slot Data",
        "bcon", "Behaviour Constants",
        "fwav", "Sound Link",
        "bhav", "Behaviour",
        "ttas", "Pie Menu Strings",
        "ttab", "Pie Menu Choices",
        "str#", "Text String",
        "objf", "Object File",
        "fams", "Family Strings",
        "dgrp", "Drawing Group",
        "palt", "Color Palette",
        "spr2", "Image Sprite",
        "ctss", "Catalog Strings",
        "objd", "Object Data",
        "rsmp", "Resource Map",
        "xxxx", "Filler Data",
        "carr", "Career Data",
        "fbmp", "Bitmap Image",
        "glob", "Global Data",
        "hous", "House Data",
        "objm", "Object Model",
        "objt", "Object Table",
        "simi", "Sims Index",
        "spr#", "Image Sprite",
        "thmb", "Thumbnail Image",
        "uchr", "User Character",
        "walm", "Wall Map");

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
      if (fm.readString(60).equals("IFF FILE 2.5:TYPE FOLLOWED BY SIZE" + (char) 0 + " JAMIE DOORNBOS & MAXIS 1")) {
        rating += 50;
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

      // 60 - Header

      // 4 - Offset To Resource Map
      // THIS FORMAT READS BIG ENDIAN!

      int numFiles = Archive.getMaxFiles(4);

      long arcSize = fm.getLength();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(arcSize);

      fm.skip(64);
      int i = 0;
      while (fm.getOffset() < path.length()) {
        // 4 - Type Code
        String typeCode = fm.readString(4);

        // 4 - Length
        long length = IntConverter.changeFormat(fm.readInt()) - 76;
        if (length < -10 && !typeCode.equals("XXXX")) {
          throw new WSPluginException("Bad File Length");
        }

        // 2 - File ID
        int fileID = ShortConverter.changeFormat(fm.readShort());

        // 2 - Flags
        fm.skip(2);

        // 64 - Filename
        String filename = fm.readNullString(64);

        if (filename.equals("")) {
          filename = Resource.generateFilename(i);
        }

        filename += "." + typeCode;

        // X - File Data
        long offset = (int) fm.getOffset();
        fm.skip(length);

        //path,id,name,offset,length,decompLength,exporter
        resources[i] = new Resource_FileID(path, fileID, filename, offset, length);

        TaskProgressManager.setValue(offset);
        i++;

      }

      resources = resizeResources(resources, i);

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

      // 60 - Header
      fm.writeString("IFF FILE 2.5:TYPE FOLLOWED BY SIZE");
      fm.writeByte(0);
      fm.writeString(" JAMIE DOORNBOS & MAXIS 1");

      // 4 - Offset To Resource Map
      fm.writeInt(IntConverter.convertBig(0));

      TaskProgressManager.setMessage(Language.get("Progress_WritingFiles"));
      for (int i = 0; i < numFiles; i++) {
        String name = resources[i].getName();

        String typeCode = name.substring(name.lastIndexOf(".") + 1);
        if (typeCode.length() < 4) {
          int difference = 4 - typeCode.length();
          for (int j = 0; j < difference; j++) {
            typeCode += " ";
          }
        }
        else if (typeCode.length() > 4) {
          typeCode = typeCode.substring(0, 4);
        }

        name = name.substring(0, name.lastIndexOf("."));
        long length = resources[i].getDecompressedLength();
        short fileID = (short) -1;
        if (resources[i] instanceof Resource_FileID) {
          fileID = (short) ((Resource_FileID) resources[i]).getID();
        }

        // 4 - Type Code
        fm.writeString(typeCode);

        // 4 - Length
        fm.writeInt(IntConverter.convertBig((int) length + 76));

        // 2 - File ID
        fm.writeInt(ShortConverter.convertBig(fileID));

        // 2 - Flags
        fm.writeInt(ShortConverter.convertBig((short) 0));

        // 64 - Filename
        fm.writeNullString(name, 64);

        // X - File Data

        write(resources[i], fm);

        TaskProgressManager.setValue(i);
      }

      fm.close();

    }
    catch (Throwable t) {
      logError(t);
    }
  }

}