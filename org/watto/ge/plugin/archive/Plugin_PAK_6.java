/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2020 wattostudios
 *
 * License Information:
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License
 * published by the Free Software Foundation; either version 2 of the License, or (at your option) any later versions. This
 * program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranties
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License at http://www.gnu.org for more
 * details. For further information on this application, refer to the authors' website.
 */

package org.watto.ge.plugin.archive;

import java.io.File;
import org.watto.datatype.Archive;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.exporter.Exporter_ZLib_CompressedSizeOnly;
import org.watto.io.FileManipulator;
import org.watto.io.FilenameSplitter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_PAK_6 extends ArchivePlugin {

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Plugin_PAK_6() {

    super("PAK_6", "PAK_6");

    //         read write replace rename
    setProperties(true, false, false, false);

    setExtensions("pak");
    setGames("Carmageddon: TDR 2000");
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

      getDirectoryFile(fm.getFile(), "dir");
      rating += 25;

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

      ExporterPlugin exporter = Exporter_ZLib_CompressedSizeOnly.getInstance();

      long arcSize = (int) path.length();

      File sourcePath = getDirectoryFile(path, "dir");

      FileManipulator fm = new FileManipulator(sourcePath, false);

      int numFiles = Archive.getMaxFiles(4);//guess

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(arcSize);

      int i = 0;
      int readLength = 0;
      //String nameRepeat = "";
      while (fm.getOffset() < fm.getLength()) {
        // X - Special filename method
        // THIS IS NOT QUITE RIGHT - SOMEHOW IT WORKS BACKWARDS AND FORWARDS ON
        // REPEATED STRINGS, POSSIBLY LIKE THE SLIDING WINDOW IN LZ COMPRESSION.
        /*
        String filename = "" + (char) fm.readByte();
        int filename_b = fm.readByte();
        
        if (filename_b == -64) {
          // remember the first bit of the name
          nameRepeat = filename;
          filename = "";
          while (filename_b == -64) {
            nameRepeat += (char) fm.readByte();
            filename_b = fm.readByte();
          }
        }
        
        while (filename_b != 8 && filename_b != 136) {
          // determine the remaining bit of the name
          filename += (char) fm.readByte();
          filename_b = fm.readByte();
        }
        
        filename = nameRepeat + filename;
        
        if (("" + filename.charAt(0)).equals(".")) {
          filename = Resource.generateFilename(i) + filename;
        }
        */

        /*
        String filename = "";
        
        String filenameChar = fm.readString(1);
        int filenameByte = fm.readByte();
        
        boolean filenameInitialized = false;
        
        while (filenameByte != 8) { // 8 = end of filename
          if (filenameByte == -64) {
            // repeat
            if (!filenameInitialized && !filename.equals("")) {
              filename = nameRepeat;
              filename += filenameChar;
            }
            else {
              nameRepeat = filename;
              filename += filenameChar;
            }
          }
          else {
            if (!filenameInitialized) {
              filename = nameRepeat;
            }
            filename += filenameChar;
          }
          filenameChar = fm.readString(1);
          filenameByte = fm.readByte();
        
          filenameInitialized = true;
        }
        // add the last char to the filename
        filename += filenameChar;
        */

        // Who cares - just generate a filename and apply the right file extension - that'll do
        String filename = "";

        String filenameChar = fm.readString(1);
        int filenameByte = fm.readByte();
        while ((filenameByte & 8) != 8) { // 8 = end of filename
          filename += filenameChar;

          filenameChar = fm.readString(1);
          filenameByte = fm.readByte();
        }
        filename += filenameChar;

        String fileExtension = FilenameSplitter.getExtension(filename);
        filename = Resource.generateFilename(i);
        if (!fileExtension.equals("")) {
          filename += "." + fileExtension;
        }

        // 4 - Offset
        long offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize + 1);

        // 4 - Length
        long length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        //path,id,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, filename, offset, length);

        TaskProgressManager.setValue(readLength);
        readLength += length;
        i++;
      }

      resources = resizeResources(resources, i);

      fm.close();

      // now open the actual PAK file and see whether the files are compressed or not
      fm = new FileManipulator(path, false);

      fm.getBuffer().setBufferSize(9);

      for (int j = 0; j < i; j++) {
        Resource resource = resources[j];

        fm.seek(resource.getOffset() + 8);

        if (fm.readString(1).equals("x")) {
          resource.setOffset(resource.getOffset() + 8);
          resource.setLength(resource.getLength() - 8);
          resource.setDecompressedLength(resource.getDecompressedLength() - 8);
          resource.setExporter(exporter);
        }
      }

      fm.close();

      return resources;

    }
    catch (Throwable t) {
      logError(t);
      return null;
    }
  }

}