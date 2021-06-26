
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
import org.watto.ge.plugin.exporter.Exporter_ZLib;
import org.watto.io.FileManipulator;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_BAG_4 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_BAG_4() {

    super("BAG_4", "BAG_4");

    //         read write replace rename
    setProperties(true, true, true, true);

    setGames("Austerlitz");
    setExtensions("bag");
    setPlatforms("PC");

    //setFileTypes("","",
    //             "",""
    //             );

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
      if (FieldValidator.checkNumFiles(fm.readInt())) {
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

      ExporterPlugin exporter = Exporter_ZLib.getInstance();

      // RESETTING GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 4 - Number Of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      int relOffset = numFiles * 272 + 4;

      for (int i = 0; i < numFiles; i++) {

        // 260 - Filename (null terminated, filled with byte 205)
        String filename = fm.readNullString(260);
        FieldValidator.checkFilename(filename);

        // 4 - File Length
        long length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - File Offset (relative to the start of the file data)
        long offset = fm.readInt() + relOffset;
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - Decompressed Length
        long decompLength = fm.readInt();
        FieldValidator.checkLength(decompLength);

        //path,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, filename, offset, length, decompLength, exporter);

        TaskProgressManager.setValue(i);
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
   * Writes an [archive] File with the contents of the Resources
   **********************************************************************************************
   **/
  @Override
  public void write(Resource[] resources, File path) {
    try {

      FileManipulator fm = new FileManipulator(path, true);
      int numFiles = resources.length;
      TaskProgressManager.setMaximum(numFiles);

      // write the file data first, then come back and write the directory once we know the compressed lengths
      long dataOffset = numFiles * 272 + 4;
      fm.setLength(dataOffset);
      fm.seek(dataOffset);

      // Write Files
      TaskProgressManager.setMessage(Language.get("Progress_WritingFiles"));

      ExporterPlugin exporter = Exporter_ZLib.getInstance();
      long[] compressedLengths = write(exporter, resources, fm);

      // now come back and write the directory
      fm.seek(0);

      // Write Header Data

      // 4 - Number Of Files
      fm.writeInt((int) numFiles);

      // Write Directory
      TaskProgressManager.setMessage(Language.get("Progress_WritingDirectory"));
      long offset = 0;
      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];
        long decompLength = resource.getDecompressedLength();
        long compLength = compressedLengths[i];
        String name = resource.getName();

        // 260 - Filename (null terminated, filled with byte 205)
        if (name.length() > 259) {
          name = name.substring(0, 259);
        }
        fm.writeNullString(name);
        int paddingSize = 259 - name.length();
        for (int p = 0; p < paddingSize; p++) {
          fm.writeByte(205);
        }

        // 4 - File Length
        fm.writeInt((int) compLength);

        // 4 - File Offset (relative to the start of the file data)
        fm.writeInt((int) offset);

        // 4 - Decompressed Length
        fm.writeInt((int) decompLength);

        offset += compLength;
      }

      fm.close();

    }
    catch (Throwable t) {
      logError(t);
    }
  }

}
