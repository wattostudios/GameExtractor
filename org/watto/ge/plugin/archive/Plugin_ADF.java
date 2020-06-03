
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
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.exporter.Exporter_Custom_ADF;
import org.watto.io.FileManipulator;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_ADF extends ArchivePlugin {

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Plugin_ADF() {

    super("ADF", "Grand Theft Auto MP3 Audio");

    //         read write replace rename
    setProperties(true, true, true, false);

    setExtensions("adf");
    setGames("GTA3", "GTA Vice City");
    setPlatforms("PC");

    setFileTypes("mp3", "MP3 Audio File");

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

      ExporterPlugin exporter = Exporter_Custom_ADF.getInstance();
      addFileTypes();

      FileManipulator fm = new FileManipulator(path, false);

      Resource[] resources = new Resource[1];

      //path,id,name,offset,length,decompLength,exporter
      resources[0] = new Resource(path, path.getName() + ".mp3", 0, fm.getLength(), fm.getLength(), exporter);

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

      ExporterPlugin exporter = Exporter_Custom_ADF.getInstance();

      FileManipulator fm = new FileManipulator(path, true);

      TaskProgressManager.setMaximum(1);

      TaskProgressManager.setMessage(Language.get("Progress_WritingFiles"));
      write(exporter, resources[0], fm);

      fm.close();

    }
    catch (Throwable t) {
      logError(t);
    }
  }

}