/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2026 wattostudios
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

import org.watto.Language;
import org.watto.datatype.Archive;
import org.watto.datatype.FileType;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.io.FileManipulator;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_OFF extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_OFF() {

    super("OFF", "OFF");

    //         read write replace rename
    setProperties(true, true, true, false);

    setExtensions("dat", "mad");
    setGames("Actua Soccer",
        "Actua Soccer 3");
    setPlatforms("PC");

    // MUST BE LOWER CASE !!!
    setFileTypes(new FileType("tim", "TIM Texture Image", FileType.TYPE_IMAGE),
        new FileType("timcol", "TIM Texture Archive", FileType.TYPE_ARCHIVE));

    //setTextPreviewExtensions("colours", "rat", "screen", "styles"); // LOWER CASE

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

      getDirectoryFile(fm.getFile(), "off");
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

      long arcSize = (int) path.length();

      File sourcePath = getDirectoryFile(path, "off");

      FileManipulator fm = new FileManipulator(sourcePath, false);

      int numFiles = Archive.getMaxFiles(4);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(arcSize);

      int readLength = 0;
      int i = 0;
      while (fm.getOffset() < fm.getLength()) {
        // 4 - Data Offset
        long offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);
        //System.out.println(offset + " of " + arcSize);

        // 4 - Raw File Length
        long length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        String filename = Resource.generateFilename(i);

        //path,id,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, filename, offset, length);

        TaskProgressManager.setValue(readLength);
        readLength += length;

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
   * Writes an [archive] File with the contents of the Resources
   **********************************************************************************************
   **/
  @Override
  public void write(Resource[] resources, File path) {
    try {

      int numFiles = resources.length;
      TaskProgressManager.setMaximum(numFiles);

      // WRITE THE FILE THAT CONTAINS THE DIRECTORY
      File dirPath = getDirectoryFile(path, "off", false);
      FileManipulator fm = new FileManipulator(dirPath, true);

      // Write Directory
      TaskProgressManager.setMessage(Language.get("Progress_WritingDirectory"));
      long offset = 0;
      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];
        long decompLength = resource.getDecompressedLength();

        // 4 - File Offset
        fm.writeInt((int) offset);

        // 4 - File Length
        fm.writeInt((int) decompLength);

        offset += decompLength;
      }

      fm.close();

      // WRITE THE FILE THAT CONTAINS THE DATA
      fm = new FileManipulator(path, true);

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

    if (headerInt1 == 16) {
      return "tim";
    }
    else if (headerInt1 != 0 && (headerInt1 + headerInt2 == headerInt3)) {
      return "timcol";
    }
    return null;
  }

}