
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
import org.watto.io.FileManipulator;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_GLM extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_GLM() {

    super("GLM", "GLM");

    //         read write replace rename
    setProperties(true, true, true, true);

    setGames("Auto Assault");
    setExtensions("glm");
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

      //ExporterPlugin exporter = Exporter_ZLib.getInstance();

      // RESETTING GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      fm.seek(arcSize - 4);

      // 4 - Directory Offset (field at the end of the archive)
      long dirOffset = fm.readInt();
      FieldValidator.checkOffset(dirOffset, arcSize);

      fm.seek(dirOffset + 8); // +8 to skip the header

      // 8 - Header (CHNKBLXX)
      // skipped above

      // 4 - Filename Directory Offset
      int filenameDirOffset = fm.readInt();
      FieldValidator.checkOffset(filenameDirOffset, arcSize);

      // 4 - Filename Directory Length
      fm.skip(4);

      // 4 - Number Of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      long[] filenameOffsets = new long[numFiles];
      for (int i = 0; i < numFiles; i++) {
        // 4 - File Offset
        long offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - File Length
        long length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - File Length
        long decompLength = fm.readInt();
        FieldValidator.checkLength(decompLength, arcSize);

        // 4 - Filename Offset (relative to the start of the filename directory)
        long filenameOffset = fm.readInt() + filenameDirOffset;
        FieldValidator.checkOffset(filenameOffset, arcSize);
        filenameOffsets[i] = filenameOffset;

        // 6 - null
        fm.skip(6);

        //path,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, "", offset, length);

        TaskProgressManager.setValue(i);
      }

      // get the filenames
      for (int i = 0; i < numFiles; i++) {
        fm.seek(filenameOffsets[i]);

        // X - Filename
        // 1 - null Filename Terminator
        String filename = fm.readNullString();
        FieldValidator.checkFilename(filename);
        resources[i].setName(filename);
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

      // Write Files
      TaskProgressManager.setMessage(Language.get("Progress_WritingFiles"));
      write(resources, fm);

      int filenameDirOffset = (int) fm.getOffset();

      // Write Filename Directory
      TaskProgressManager.setMessage(Language.get("Progress_WritingDirectory"));
      int[] filenameOffsets = new int[numFiles];
      int filenameOffset = 0;
      for (int i = 0; i < numFiles; i++) {
        String filename = resources[i].getName();

        filenameOffsets[i] = filenameOffset;

        // X - Filename (null)
        fm.writeNullString(filename);

        filenameOffset += filename.length() + 1;
      }

      int dirOffset = (int) fm.getOffset();
      int filenameDirLength = dirOffset - filenameDirOffset;

      // 8 - Header (CHNKBLXX)
      fm.writeString("CHNKBLXX");

      // 4 - Filename Directory Offset
      fm.writeInt(filenameDirOffset);

      // 4 - Filename Directory Length
      fm.writeInt(filenameDirLength);

      // 4 - Number Of Files
      fm.writeInt(numFiles);

      // Write Directory
      long offset = 0;
      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];
        int decompLength = (int) resource.getDecompressedLength();

        // 4 - File Offset
        fm.writeInt((int) offset);

        // 4 - File Length
        fm.writeInt(decompLength);

        // 4 - File Length
        fm.writeInt(decompLength);

        // 4 - Filename Offset (relative to the start of the filename directory)
        fm.writeInt(filenameOffsets[i]);

        // 6 - null
        fm.writeInt(0);
        fm.writeShort((short) 0);

        offset += decompLength;
      }

      // 4 - null
      fm.writeInt(0);

      // 4 - File Directory Offset
      fm.writeInt(dirOffset);

      fm.close();

    }
    catch (Throwable t) {
      logError(t);
    }
  }

}
