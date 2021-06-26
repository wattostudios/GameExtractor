
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
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.exporter.Exporter_Custom_MHTML_Base64;
import org.watto.ge.plugin.exporter.Exporter_Custom_MHTML_QuotedPrintable;
import org.watto.io.FileManipulator;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_MHTML extends ArchivePlugin {

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Plugin_MHTML() {

    super("MHTML", "MHTML Webpage Archive");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Microsoft Internet Explorer");
    setExtensions("mht", "mhtml");
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
      if (fm.readString(5).equals("From:")) {
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
      long arcSize = fm.getLength();

      // First find the boundary locator
      String boundary = null;
      while (boundary == null && fm.getOffset() < arcSize) {
        String line = fm.readLine();

        int boundaryIndex = line.indexOf("boundary=\"");
        if (boundaryIndex > 0) {
          int boundaryEnd = line.lastIndexOf("\"");
          if (boundaryEnd > 0) {
            boundary = "--" + line.substring(boundaryIndex + 10, boundaryEnd);
          }
        }
      }

      if (fm.getOffset() >= arcSize || boundary == null) {
        return null;
      }

      int realNumFiles = 0;

      // Setup storage arrays
      Resource[] resources = new Resource[Archive.getMaxFiles()];

      // now locate each of the files
      boolean lookingForFilename = false;
      boolean lookingForFileEnd = false;
      String filename = "";
      String exportType = "";
      long offset = 0;

      TaskProgressManager.setMaximum(arcSize);

      while (fm.getOffset() < arcSize) {
        long beforeOffset = fm.getOffset();
        String line = fm.readLine();

        if (lookingForFilename) {
          if (line.indexOf("Content-Transfer-Encoding: ") == 0) {
            exportType = line.substring(27).trim();
          }
          if (line.indexOf("Content-Location: ") == 0) {
            filename = line.substring(18).trim();

            fm.readLine();
            offset = fm.getOffset();

            lookingForFilename = false;
            lookingForFileEnd = true;
          }
        }
        else if (lookingForFileEnd) {
          if (line.indexOf(boundary) == 0) {
            // found the end of the file, cause it is the start of the next file

            long length = beforeOffset - offset;

            ExporterPlugin exporter;
            if (exportType.equals("base64")) {
              // images etc.
              exporter = Exporter_Custom_MHTML_Base64.getInstance();
              //exporter = Exporter_Custom_MHTML_QuotedPrintable.getInstance();
            }
            else {
              // text - "quoted-printable"
              exporter = Exporter_Custom_MHTML_QuotedPrintable.getInstance();
            }

            //filename = FileManipulator.removeNonFilename(filename);

            //path,id,name,offset,length,decompLength,exporter
            resources[realNumFiles] = new Resource(path, filename, offset, length, length, exporter);
            realNumFiles++;

            lookingForFilename = true;
            lookingForFileEnd = false;

            TaskProgressManager.setValue(beforeOffset);
          }
        }
        else {
          if (line.indexOf(boundary) == 0) {
            // found the first file
            lookingForFilename = true;
            lookingForFileEnd = false;
          }
        }
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
