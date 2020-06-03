
package org.watto.ge.plugin.archive;

import java.io.File;
import org.watto.Language;
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
import org.watto.io.converter.StringConverter;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_SCW extends ArchivePlugin {

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Plugin_SCW() {

    super("SCW", "Lord Of The Rings SCW");

    //         read write replace rename
    setProperties(true, true, true, false);

    setExtensions("scw");
    setGames("Lord Of The Rings: The Battle For Middle Earth",
        "Lord Of The Rings: The Fellowship Of The Ring",
        "Lord Of The Rings: The Two Towers",
        "Lord Of The Rings: Return Of The King");
    setPlatforms("PC");

    setFileTypes("ctrl", "Control",
        "shoc", "3D Data Chunk",
        "padd", "Blank Padding",
        "sono", "Sound Chunk",
        "fill", "Blank Filler",
        "stoc", "Table Of Contents",
        "swvr", "File Loader");

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

      Resource[] resources = new Resource[Archive.getMaxFiles(4)];

      int i = 0;

      long arcSize = fm.getLength();

      TaskProgressManager.setMaximum(arcSize);

      while (fm.getOffset() < path.length()) {
        //System.out.println(fm.getOffset());
        // 4 - Type Code
        String type = StringConverter.reverse(fm.readString(4));

        if (type.equals("FILL")) {
          long paddingSize = 65536 - (fm.getOffset() % 65536);
          if (paddingSize < 65536) {
            fm.skip(paddingSize);
          }
          //System.out.println((fm.getOffset())%65536);
        }
        else {
          // 4 - Length
          long length = fm.readInt() - 8;
          if (length == -8) {
            fm.seek(arcSize);
          }
          else {
            FieldValidator.checkLength(length, arcSize);

            String filename = Resource.generateFilename(i) + "." + type;

            // X - File Data
            long offset = (int) fm.getOffset();
            fm.skip(length);

            //path,id,name,offset,length,decompLength,exporter
            resources[i] = new Resource(path, filename, offset, length);
            TaskProgressManager.setValue(offset);
            i++;
          }
        }
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

      TaskProgressManager.setMessage(Language.get("Progress_WritingFiles"));
      for (int i = 0; i < numFiles; i++) {
        String name = resources[i].getName();

        String type = name.substring(name.lastIndexOf(".") + 1);
        name = name.substring(0, name.lastIndexOf("."));
        long length = resources[i].getDecompressedLength();

        // 4 - Type Code
        fm.writeNullString(StringConverter.reverse(type), 4);

        // 4 - Length
        fm.writeInt((int) length + 8);

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