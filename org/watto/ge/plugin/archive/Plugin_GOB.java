
package org.watto.ge.plugin.archive;

import java.io.File;
import org.watto.Language;
import org.watto.Settings;
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
import org.watto.io.FileManipulator;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_GOB extends ArchivePlugin {

  // NOTE - THIS FORMAT DOES HAVE A WRITE METHOD, BUT IT IS DISABLED FOR EVERYONE BECAUSE I HAVE
  // NO WAY TO TEST THAT IT WORKS. THE WRITE METHOD WAS REQUESTED BY SOMEONE THEREFORE IT
  // EXISTS, BUT THATS IT - I DON'T EXPECT IT TO WORK AT ALL UNTIL I CAN GET HOLD OF THE FULL
  // ARCHIVE. PLUS, I THINK THE READ CODE THAT MrMouse WROTE IS WRONG TOO - I THINK IT SOMEHOW
  // STORES MULTIPLE DIRECTORIES, NOT JUST A SINGLE DIRECTORY.

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Plugin_GOB() {

    super("GOB", "GOB");

    //         read write replace rename
    setProperties(true, false, false, false);

    setExtensions("gob");
    setGames("EA Cricket 2004");
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

      long arcSize = fm.getLength();
      if (arcSize - 2043 < 0) {
        return 0;
      }

      fm.seek(arcSize - 2043);

      // Number Of Files
      if (FieldValidator.checkNumFiles(fm.readInt())) {
        rating += 5;
      }

      // Directory Offset
      if (FieldValidator.checkOffset(fm.readInt(), arcSize)) {
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

      int end = (int) fm.getLength() - 2047;
      fm.seek(end);

      // 4 - Unknown
      fm.skip(4);

      // 4 - numFiles
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      long arcSize = fm.getLength();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // 4 - Directory Offset
      long dirOffset = end - fm.readInt() - (24 * numFiles);
      fm.seek(dirOffset);

      int readLength = 0;
      for (int i = 0; i < numFiles; i++) {
        // 16 - MD5 Hash
        fm.skip(16);

        // 4 - Data Offset
        long offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - Raw File Length
        long length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        String filename = Resource.generateFilename(i);

        //path,id,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, filename, offset, length);

        TaskProgressManager.setValue(readLength);
        readLength += length;
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

      FileManipulator fm = new FileManipulator(path, true);
      int numFiles = resources.length;
      TaskProgressManager.setMaximum(numFiles);

      FileManipulator src = new FileManipulator(new File(Settings.getString("CurrentArchive")), false);

      // getting to the correct spot in the source file
      int end = (int) src.getLength() - 2055;
      src.seek(end);
      long dirOffset = end - src.readInt() - (24 * numFiles);
      src.seek(dirOffset);

      TaskProgressManager.setMessage(Language.get("Progress_WritingFiles"));
      write(resources, fm);

      TaskProgressManager.setMessage(Language.get("Progress_WritingDirectory"));
      long offset = 0;
      for (int i = 0; i < numFiles; i++) {
        long length = resources[i].getDecompressedLength();

        // 16 - MD5 Hash
        fm.writeBytes(src.readBytes(16));

        // 4 - Offset
        // 4 - Length
        fm.writeInt((int) offset);
        fm.writeInt((int) length);
        src.skip(8);

        offset += length;
      }

      int bytesRemaining = (int) (src.getLength() - src.getOffset());
      fm.writeBytes(src.readBytes(bytesRemaining));

      src.close();

      fm.close();

    }
    catch (Throwable t) {
      logError(t);
    }
  }

}