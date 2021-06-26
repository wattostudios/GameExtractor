
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
import org.watto.io.converter.IntConverter;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_LBM_FORM extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_LBM_FORM() {

    super("LBM_FORM", "Generic IFF Archive");

    //         read write replace rename
    setProperties(true, true, true, false);

    setExtensions("lbm", "vqa", "bbm", "iff");
    setGames("Allan Border Cricket",
        "Medieval Total War",
        "Command And Conquer",
        "Steel Panthers",
        "Ultima 7",
        "Evasive Action",
        "AFL 98",
        "Project IGI: I'm Going In");
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

      // Header
      if (fm.readString(4).equals("FORM")) {
        rating += 50;
      }

      // Archive Size
      if (fm.readInt() == fm.getLength()) {
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

      // 4 - Header (FORM)
      // 4 - Length Of Archive
      // 4 - name/extension?
      fm.skip(12);

      int numFiles = Archive.getMaxFiles(4);//guess

      long arcSize = fm.getLength();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(arcSize);

      int i = 0;
      while (fm.getOffset() < fm.getLength() - 4) {
        // 4 - Type
        String fileExt = fm.readString(4);
        if (fileExt.charAt(0) == (char) 0) {
          fileExt = fileExt.substring(1, 4) + fm.readString(1);
        }

        // 4 - Length (BIG ENDIAN!)
        long length = IntConverter.changeFormat(fm.readInt());
        FieldValidator.checkLength(length, arcSize);

        // X - File Data
        long offset = (int) fm.getOffset();
        fm.skip(length);

        String filename = Resource.generateFilename(i) + "." + fileExt;

        //path,id,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, filename, offset, length);

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

      TaskProgressManager.setMessage(Language.get("Progress_PerformingCalculations"));
      long arcSize = (numFiles * 8) + 12;
      for (int i = 0; i < numFiles; i++) {
        arcSize += resources[i].getDecompressedLength();
      }

      // 4 - Header (FORM)
      fm.writeString("FORM");

      // 4 - Length Of Archive
      fm.writeInt((int) arcSize);

      // 4 - name/extension?
      fm.writeString("FORM");

      TaskProgressManager.setMessage(Language.get("Progress_WritingFiles"));
      for (int i = 0; i < numFiles; i++) {
        Resource fd = resources[i];
        long length = fd.getDecompressedLength();
        String ext = fd.getExtension();

        int extlength = ext.length();

        if (extlength > 4) {
          ext = ext.substring(0, 4);
        }

        while (extlength < 4) {
          fm.writeByte((char) 0);
          extlength++;
        }

        // 4 - Type
        fm.writeString(ext);

        // 4 - Length
        fm.writeInt(IntConverter.convertBig((int) length));

        // X - File
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