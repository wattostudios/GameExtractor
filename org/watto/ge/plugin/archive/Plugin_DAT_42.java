
package org.watto.ge.plugin.archive;

import java.io.File;
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
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_DAT_42 extends ArchivePlugin {

  int numNames = 0;

  int readEntries = 0;

  int currentResource = 0;

  boolean dieNow = false;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_DAT_42() {

    super("DAT_42", "DAT_42");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Transformers: The Game");
    setExtensions("dat"); // MUST BE LOWER CASE
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

      fm.skip(4);

      long arcSize = fm.getLength();

      // Directory Length
      if (FieldValidator.checkLength(fm.readInt(), arcSize)) {
        rating += 5;
      }

      // null
      if (fm.readLong() == 0) {
        rating += 5;
      }

      fm.skip(2000);

      // null
      if (fm.readLong() == 0) {
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

      //ExporterPlugin exporter = Exporter_ZLib.getInstance();

      // RESETTING GLOBAL VARIABLES
      numNames = 0;
      readEntries = 0;
      dieNow = false;

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 4 - Directory Offset?
      fm.skip(4);

      // 4 - Directory Length
      int dirLength = fm.readInt();
      FieldValidator.checkLength(dirLength, arcSize);

      long dirOffset = arcSize - dirLength;
      FieldValidator.checkOffset(dirOffset);

      fm.seek(dirOffset);

      // 4 - Unknown
      fm.skip(4);

      // 4 - Number Of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      long offset = 2048;

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {
        // 4 - Unknown Length/Offset
        fm.skip(4);

        // 4 - Compressed File Length
        long length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - Decompressed File Length
        long decompLength = fm.readInt();
        FieldValidator.checkLength(decompLength);

        // 4 - Compression Flag (0=Uncompressed/2=Compressed)
        fm.skip(4);

        String filename = Resource.generateFilename(i);

        //path,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, filename, offset, length, decompLength);

        offset += length;

        int paddingSize = 2048 - (int) (length % 2048);
        if (paddingSize != 2048) {
          offset += paddingSize;
        }

        TaskProgressManager.setValue(i);
      }

      // read the filenames

      // 4 - Number Of Filenames
      numNames = fm.readInt();
      FieldValidator.checkNumFiles(numNames);

      dirOffset = fm.getOffset();

      fm.skip(numNames * 8 + 4); // skip over the offsets etc.

      String[] names = new String[numNames];
      for (int i = 0; i < numNames; i++) {
        // X - Filename
        // 1 - null Filename Terminator
        names[i] = fm.readNullString();
      }

      fm.seek(dirOffset);

      readEntries = 0;
      currentResource = 0;

      while (readEntries < numNames) {
        readEntry(fm, "", resources, names, 0);
      }

      /*
       * int numParents = 0; String[] parents = new String[10]; String parentName = "";
       * 
       * boolean justChangedDirectory = false; String prevExtension = "";
       * 
       * for (int i=0;i<numNames;i++){ // X - Filename // 1 - null Filename Terminator String
       * name = fm.readNullString(); if (name.indexOf('.') > 0){ // file if (justChangedDirectory
       * && ! prevExtension.equals("")){ if (!
       * name.substring(name.indexOf('.')+1).equals(prevExtension)){ for (int
       * n=0;n<numParents;n++){ parents[n] = parents[n+1]; } if (numParents > 0){ numParents--; }
       * } } prevExtension = name.substring(name.indexOf('.')+1);
       * 
       * System.out.println(parentName + name); justChangedDirectory = false; } else { //
       * directory if (justChangedDirectory){ numParents++; } parents[numParents] = name;
       * 
       * parentName = ""; for (int n=0;n<=numParents;n++){ parentName += parents[n] + "\\"; }
       * System.out.println(parentName); justChangedDirectory = true; } }
       */

      fm.close();

      if (dieNow) {
        return null;
      }

      return resources;

    }
    catch (Throwable t) {
      logError(t);
      return null;
    }
  }

  public void readEntry(FileManipulator fm, String parentName, Resource[] resources, String[] names, int depth) {
    if (depth >= 50) {
      dieNow = true;
      return; // because some very rare files were causing stack overflows here (when the file wasn't even valid for this plugin)
    }
    try {

      // 2 - File ID (if this entry is a directory, this value is the last entry in this sub-directory)
      int lastFile = fm.readShort();

      // 2 - Unknown
      // 4 - Filename Offset (relative to the start of the filenames directory)
      fm.skip(6);

      if (lastFile <= 0) {
        // file
        //System.out.println("adding file " + names[readEntries] + " to " + parentName);
        resources[currentResource].setName(parentName + names[readEntries]);
        currentResource++;
        readEntries++;
      }
      else {
        // directory

        String dirName = parentName + names[readEntries];
        if (!dirName.equals("")) {
          dirName += "\\";
        }
        readEntries++;

        //System.out.println(parentName + names[readEntries-1] + " has " + (lastFile-readEntries) + " files");
        while (readEntries <= lastFile && !dieNow) {
          //System.out.println("Reading into " + parentName + names[readEntries-1] + " entry " + (readEntries));
          readEntry(fm, dirName, resources, names, depth + 1);
        }
      }

    }
    catch (Throwable t) {
      logError(t);
    }
  }

}
