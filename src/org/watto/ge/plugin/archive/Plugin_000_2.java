/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2022 wattostudios
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
import java.util.Arrays;
import org.watto.Language;
import org.watto.datatype.FileType;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.helper.PaletteManager;
import org.watto.ge.helper.ResourceSorter_Offset;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.io.FileManipulator;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_000_2 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_000_2() {

    super("000_2", "DreamForge Intertainment ### Archive");

    //         read write replace rename
    setProperties(true, true, false, false);

    setGames("WarWind",
        "Sanitarium");
    setExtensions("000", "001", "002", "003", "004", "005", "006", "007", "008", "009", "010", "011", "012", "013", "014", "015", "016", "017", "018", "104", "204", "304");
    setPlatforms("PC");

    setCanScanForFileTypes(true);

    // MUST BE LOWER CASE !!!
    setFileTypes(new FileType("palette", "Color Palette", FileType.TYPE_PALETTE),
        new FileType("d3gr", "D3GR Image", FileType.TYPE_IMAGE));

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

      // Number Of Files
      int numFiles = fm.readInt();
      if (FieldValidator.checkNumFiles(numFiles)) {
        rating += 5;
      }

      // First File Offset
      int firstOffset = fm.readInt();
      int calcOffset = ((numFiles * 4) + 4);
      if (firstOffset == calcOffset || firstOffset == (calcOffset + 4)) {
        rating += 10;
      }
      else if (firstOffset == 0) {
        rating += 5;
      }
      else {
        rating = 0;
      }

      String filename = fm.getFile().getName();
      if (filename.startsWith("RES.") || filename.startsWith("res.")) {
        rating += 10;
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

      PaletteManager.clear(); // clear the color palettes before we load new ones into it.

      addFileTypes();

      FileManipulator fm = new FileManipulator(path, false);

      // 4 - Number Of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      long arcSize = fm.getLength();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      ResourceSorter_Offset[] sorter = new ResourceSorter_Offset[numFiles];

      int realNumFiles = 0;
      for (int i = 0; i < numFiles; i++) {
        // 4 - File Offset
        int offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        if (offset == 0) {
          continue;
        }

        String filename = Resource.generateFilename(realNumFiles);

        //path,id,name,offset,length,decompLength,exporter
        Resource resource = new Resource(path, filename, offset);

        resources[realNumFiles] = resource;
        sorter[realNumFiles] = new ResourceSorter_Offset(resource);

        realNumFiles++;

        TaskProgressManager.setValue(i);
      }

      fm.close();

      if (realNumFiles < numFiles) {
        resources = resizeResources(resources, realNumFiles);

        ResourceSorter_Offset[] oldSorter = sorter;
        sorter = new ResourceSorter_Offset[realNumFiles];
        System.arraycopy(oldSorter, 0, sorter, 0, realNumFiles);
      }

      numFiles = realNumFiles;

      // Sort the files in order of Offset, before calculating the file sizes
      Arrays.sort(sorter);

      for (int i = 0; i < numFiles - 1; i++) {
        Resource resource = sorter[i].getResource();
        Resource nextResource = sorter[i + 1].getResource();

        int length = (int) (nextResource.getOffset() - resource.getOffset());
        resource.setLength(length);
        resource.setDecompressedLength(length);
      }

      Resource lastResource = sorter[numFiles - 1].getResource();
      int length = (int) (arcSize - lastResource.getOffset());
      lastResource.setLength(length);
      lastResource.setDecompressedLength(length);

      return resources;

    }
    catch (Throwable t) {
      logError(t);
      return null;
    }
  }

  /**
   **********************************************************************************************
   * Writes an [archive] File with the contents of the Resources
   **********************************************************************************************
   **/
  @Override
  public void write(Resource[] resources, File path) {
    try {

      FileManipulator fm = new FileManipulator(path, true);
      int numFiles = resources.length;
      TaskProgressManager.setMaximum(numFiles);

      // Write Header Data

      // 4 - Number Of Files
      fm.writeInt(numFiles);

      // Write Directory
      TaskProgressManager.setMessage(Language.get("Progress_WritingDirectory"));
      long offset = 4 + (numFiles * 4);
      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];
        long decompLength = resource.getDecompressedLength();

        // 4 - File Offset
        fm.writeInt((int) offset);

        offset += decompLength;
      }

      // Write Files
      TaskProgressManager.setMessage(Language.get("Progress_WritingFiles"));
      write(resources, fm);

      fm.close();

    }
    catch (Throwable t) {
      logError(t);
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

    if (headerInt1 == 1380397892) {
      if ((headerBytes[5] & 32) == 32) {
        return "palette";
      }
      return "d3gr";
    }

    return null;
  }

}
