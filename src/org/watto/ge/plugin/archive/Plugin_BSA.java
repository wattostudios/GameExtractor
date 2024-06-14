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
import org.watto.Language;
import org.watto.Settings;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.io.FileManipulator;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_BSA extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_BSA() {

    super("BSA", "BSA");

    //         read write replace rename
    setProperties(true, false, true, true);

    setExtensions("bsa");
    setGames("Terminator: Future Shock",
        "Battlespire",
        "Elder Scrolls: Arena");
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

      // Number Of Files
      int numFiles = fm.readShort();
      if (FieldValidator.checkNumFiles(numFiles)) {
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

      // 2 - numFiles
      int numFiles = fm.readShort();
      FieldValidator.checkNumFiles(numFiles);

      long arcSize = fm.getLength();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      long dirOffset = (int) fm.getLength() - (numFiles * 18);

      int currentPos = 2;
      for (int i = 0; i < numFiles; i++) {
        fm.seek(dirOffset);
        // 14 - Filename
        String filename = fm.readNullString(14);
        FieldValidator.checkFilename(filename);

        // 4 - fileLength
        long length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        dirOffset = (int) fm.getOffset();

        long offset = currentPos;

        currentPos += length;

        //path,id,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, filename, offset, length);

        TaskProgressManager.setValue(currentPos);
      }

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

      int numFiles = resources.length;
      TaskProgressManager.setMaximum(numFiles);

      FileManipulator fm = new FileManipulator(path, true);
      FileManipulator src = new FileManipulator(new File(Settings.getString("CurrentArchive")), false);

      // 2 - numFiles
      fm.writeShort((short) numFiles);
      src.skip(2);

      // 4 - Unknown
      fm.writeBytes(src.readBytes(4));

      src.close();

      TaskProgressManager.setMessage(Language.get("Progress_WritingDirectory"));
      for (int i = 0; i < numFiles; i++) {
        long length = resources[i].getDecompressedLength();

        // 14 - Filename (null)
        fm.writeNullString(resources[i].getName());

        // 4 - File Length
        fm.writeInt((int) length);

      }

      TaskProgressManager.setMessage(Language.get("Progress_WritingFiles"));
      write(resources, fm);

      fm.close();

    }
    catch (Throwable t) {
      logError(t);
    }
  }

}