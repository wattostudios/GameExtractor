
package org.watto.ge.plugin.archive;

import java.io.File;
import org.watto.Language;
import org.watto.Settings;
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
public class Plugin_DAT_10 extends ArchivePlugin {

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Plugin_DAT_10() {

    super("DAT_10", "DAT_10");

    //         read write replace rename
    setProperties(true, false, true, true);

    setExtensions("dat");
    setGames("Indycar Racing");
    setPlatforms("PC");

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

      fm.skip(4);

      long arcSize = fm.getLength();

      // File Length
      long length = fm.readInt();
      if (FieldValidator.checkLength(length, arcSize)) {
        rating += 5;
      }

      // Compressed File Length
      int compLength = fm.readInt();
      if (FieldValidator.checkLength(compLength, arcSize) && compLength < length) {
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

      int numFiles = Archive.getMaxFiles(4);//guess

      long arcSize = fm.getLength();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(arcSize);

      int i = 0;
      int readLength = 0;
      boolean hasNext = true;
      while (hasNext) {
        // 2 - Unknown
        // 2 - Unknown
        fm.skip(4);

        // 4 - Raw File Length
        long length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - Compressed File Length
        int compLength = fm.readInt();
        FieldValidator.checkLength(compLength);

        // 11 - Filename
        String filename = fm.readNullString(11);
        if (filename.length() == 0) {
          hasNext = false;
        }
        else {
          FieldValidator.checkFilename(filename);

          // 4 - Unknown
          fm.skip(4);

          //path,id,name,offset,length,decompLength,exporter
          resources[i] = new Resource(path, filename, 0, compLength);

          TaskProgressManager.setValue(readLength);
          readLength += length;
          i++;
        }
      }

      long offset = (int) fm.getOffset();
      for (int j = 0; j < i; j++) {
        resources[j].setOffset(offset);
        offset += resources[j].getLength();
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
  @SuppressWarnings("unused")
  public void write(Resource[] resources, File path) {
    try {

      int numFiles = resources.length;
      TaskProgressManager.setMaximum(numFiles);

      FileManipulator fm = new FileManipulator(path, true);
      FileManipulator src = new FileManipulator(new File(Settings.getString("CurrentArchive")), false);

      // Loop 1 - Directory
      long offset = (27 * numFiles);

      TaskProgressManager.setMessage(Language.get("Progress_WritingDirectory"));
      for (int i = 0; i < numFiles; i++) {
        long length = resources[i].getDecompressedLength();

        // 2 - Unknown
        // 2 - Unknown
        fm.writeBytes(src.readBytes(4));

        // 4 - Raw File Length
        // 4 - Compressed File Length
        src.skip(8);
        fm.writeInt((int) length);
        fm.writeInt((int) length);

        // 11 - Filename
        fm.writeNullString(resources[i].getName(), 11);
        src.skip(11);

        // 4 - Unknown
        fm.writeBytes(src.readBytes(4));

        offset += length;
      }

      for (int i = 0; i < 27; i++) {
        fm.writeByte(0);
      }

      TaskProgressManager.setMessage(Language.get("Progress_WritingFiles"));
      write(resources, fm);

      src.close();

      fm.close();

    }
    catch (Throwable t) {
      logError(t);
    }
  }

}