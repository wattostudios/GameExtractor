/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2021 wattostudios
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
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.exporter.Exporter_Default;
import org.watto.ge.plugin.exporter.MultiFileBlockExporterWrapper;
import org.watto.io.FileManipulator;
import org.watto.io.converter.IntConverter;
import org.watto.task.TaskProgressManager;

/**
 **********************************************************************************************
 * THIS PLUGIN READS OVER A BUNCH OF FILES THAT MAKE A SINGLE ARCHIVE. THE FILENAMES ARE NAMED IN
 * HEX - ie 0, 1, ... A, B, etc. IT IS POSSIBLE FOR A FILE TO BE HALF IN 1 SPAN AND HALF IN THE
 * NEXT - this should be covered by the plugin OK.
 **********************************************************************************************
 **/
public class Plugin_NoExt_1 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_NoExt_1() {

    super("NoExt_1", "NoExt_1");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("ESPN NFL 2K5",
        "ESPN NFL 2K8");
    setExtensions("");
    setPlatforms("XBox",
        "PS2");

    setCanScanForFileTypes(true);

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
      else {
        return 0;
      }

      // Number Of Files
      if (FieldValidator.checkNumFiles(fm.readInt())) {
        rating += 5;
      }

      // null
      if (fm.readInt() == 0) {
        rating += 5;
      }

      // Number header blocks
      if (fm.readInt() == 16) {
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
   * Reads an [archive] File into the Resources
   **********************************************************************************************
   **/
  @Override
  public Resource[] read(File path) {
    try {

      // NOTE - Compressed files MUST know their DECOMPRESSED LENGTH
      //      - Uncompressed files MUST know their LENGTH

      addFileTypes();

      // RESETTING THE GLOBAL VARIABLES
      ExporterPlugin exporterDefault = Exporter_Default.getInstance();

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 4 - Number Of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // 4 - null
      // 4 - Number Of Header Blocks (16)
      // for each header block
      // 4 - Unknown (256000/1/188164)
      // 80 - null Padding to offset 156
      fm.skip(152);

      Resource[] resources = new Resource[numFiles];

      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      // stuff for spanning over multiple files
      long relOffset = 0;
      String[] sourceFilenames = new String[] { "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "A", "B", "C", "D", "E", "F" };
      int sourceFilenamePos = 1;

      int realNumFiles = 0;
      for (int i = 0; i < numFiles; i++) {
        // 4 - Hash?
        fm.skip(4);

        // 4 - File Length?
        long length = fm.readInt();
        //FieldValidator.checkLength(length,arcSize);

        // 4 - File Offset [*2048]
        long offset = IntConverter.unsign(fm.readInt());
        offset = (offset * 2048) - relOffset;

        if (offset >= arcSize) {
          //break;
          relOffset += arcSize;

          path = new File(path.getParent() + File.separator + sourceFilenames[sourceFilenamePos]);
          sourceFilenamePos++;

          arcSize = path.length();

          offset -= arcSize;
        }
        //FieldValidator.checkOffset(offset,arcSize);

        String filename = Resource.generateFilename(i);

        // see if the file spans over multiple archives 
        long remainingArcLength = arcSize - offset;
        if (remainingArcLength < length) {
          // spanned - use a MultiBlockExporter for storing both pieces

          File[] blockFiles = new File[2];
          blockFiles[0] = path;
          blockFiles[1] = new File(path.getParent() + File.separator + sourceFilenames[sourceFilenamePos]);

          long[] blockOffsets = new long[] { offset, 0 };
          long[] blockLengths = new long[] { remainingArcLength, length - remainingArcLength };

          MultiFileBlockExporterWrapper blockExporter = new MultiFileBlockExporterWrapper(exporterDefault, blockFiles, blockOffsets, blockLengths, blockLengths);

          Resource resource = new Resource(path, filename, offset, length, length, blockExporter);
          resource.forceNotAdded(true);
          resources[i] = resource;
        }
        else {
          // not spanned

          //path,id,name,offset,length,decompLength,exporter
          Resource resource = new Resource(path, filename, offset, length);
          resource.forceNotAdded(true);
          resources[i] = resource;
        }

        TaskProgressManager.setValue(i);
        realNumFiles++;
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

  /**
  **********************************************************************************************
  If an archive doesn't have filenames stored in it, the scanner can come here to try to work out
  what kind of file a Resource is. This method allows the plugin to provide additional plugin-specific
  extensions, which will be tried before any standard extensions.
  @return null if no extension can be determined, or the extension if one can be found
  **********************************************************************************************
  **/
  @Override
  public String guessFileExtension(Resource resource, byte[] headerBytes, int headerInt1, int headerInt2, int headerInt3, short headerShort1, short headerShort2, short headerShort3, short headerShort4, short headerShort5, short headerShort6) {

    if (headerInt1 == 1096040772) {
      return "data";
    }
    else if (headerInt1 == 1112757569) {
      return "ausb";
    }
    else if (headerInt1 == 1162756947) {
      return "scne";
    }
    else if (headerInt1 == 1212371027) {
      return "spch";
    }
    else if (headerInt1 == 1279415379) {
      return "stbl";
    }
    else if (headerInt1 == 1329878337) {
      return "audo";
    }
    else if (headerInt1 == 1380139331) {
      return "cacr";
    }
    else if (headerInt1 == 1381259348) {
      //resource.setOffset(resource.getOffset() + 48);
      return "txtr";
    }
    else if (headerInt1 == 1397445197) {
      return "mrks";
    }
    else if (headerInt1 == 1413698116) {
      return "drct";
    }
    else if (headerInt1 == 1413829448) {
      return "hset";
    }
    else if (headerInt1 == 1413829460) {
      return "tset";
    }
    else if (headerInt1 == 1414418246) {
      return "font";
    }
    else if (headerInt1 == 1431191885) {
      return "manu";
    }
    else if (headerInt1 == 1481918792) {
      return "hitx";
    }
    else if (headerInt1 == 1749177427) {
      return "stbh";
    }
    else if (headerInt1 == 1750352196) {
      return "dath";
    }
    else if (headerInt1 == 1850960200) {
      return "hisn";
    }

    else if (headerInt1 == 1163087433) {
      return "txt";
    }

    return null;
  }

}
