
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
public class Plugin_FAR_FAR extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_FAR_FAR() {

    super("FAR_FAR", "The Sims FAR");

    //         read write replace rename
    setProperties(true, true, true, true);

    setGames("The Sims", "The Sims Online");
    setExtensions("far");
    setPlatforms("PC");

    setFileTypes("ffn", "Font File",
        "iff", "IFF Object",
        "xa", "XA Sound",
        "bcf", "BCF Animation",
        "bmf", "BMF Animation",
        "cfp", "CFP Animation",
        "cmx", "CMX Character File",
        "spf", "SPF Object",
        "skn", "Character Skin",
        "ndx", "Index File",
        "flr", "Floor Image",
        "wll", "Wall Image",
        "hit", "Sound File",
        "fsc", "Sound File",
        "hot", "Sound File",
        "hsm", "Sound File",
        "stx", "STX Object",
        "otf", "Object Tuning");

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
      if (fm.readString(8).equals("FAR!byAZ")) {
        rating += 50;
      }

      // Version (1)
      if (fm.readInt() == 1) {
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
      long arcSize = fm.getLength();

      // 8 - Header (FAR!byAZ)
      // 4 - Version (1)
      fm.skip(12);

      // 4 - Directory Offset
      long dirOffset = fm.readInt();
      FieldValidator.checkOffset(dirOffset, arcSize);

      // 4 (From Directory Offset) - Number Of Files
      fm.seek(dirOffset);
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      boolean TSOformat = false;
      for (int i = 0; i < numFiles; i++) {
        // 4 - Raw File Size
        long length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - Compressed File Size
        fm.skip(4);

        // 4 - Data Offset
        long offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - Filename Length
        int filenameLength = 0;
        if (TSOformat == false) {
          filenameLength = fm.readInt();
          if (filenameLength > 1000) {
            TSOformat = true;
            fm.seek(dirOffset + 16);
            filenameLength = fm.readShort();
          }
        }
        else {
          filenameLength = fm.readShort();
        }
        FieldValidator.checkFilenameLength(filenameLength);

        // X - Filename
        String filename = fm.readString(filenameLength);
        FieldValidator.checkFilename(filename);

        //path,id,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, filename, offset, length);

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
  
  **********************************************************************************************
  **/
  @Override
  public void write(Resource[] resources, File path) {
    try {

      FileManipulator fm = new FileManipulator(path, true);
      int numFiles = resources.length;
      TaskProgressManager.setMaximum(numFiles);

      // Calculations
      TaskProgressManager.setMessage(Language.get("Progress_PerformingCalculations"));
      int directoryOffset = 16;
      for (int i = 0; i < numFiles; i++) {
        directoryOffset += resources[i].getDecompressedLength();
      }

      // Write Header Data

      // 8 - Header (FAR!byAZ)
      fm.writeString("FAR!byAZ");

      // 4 - Version (1)
      fm.writeInt((int) 1);

      // 4 - Directory Offset
      fm.writeInt((int) directoryOffset);

      // Write Files
      TaskProgressManager.setMessage(Language.get("Progress_WritingFiles"));
      write(resources, fm);

      // Write Directory
      TaskProgressManager.setMessage(Language.get("Progress_WritingDirectory"));

      // 4 - Number Of Files
      fm.writeInt((int) numFiles);

      long lengthByteSize = 4;
      String[] possibleValues = { "The Sims", "The Sims Online" };
      String selectedValue = (String) javax.swing.JOptionPane.showInputDialog(null, "What game should this archive be compatable with?", "Game Version", javax.swing.JOptionPane.INFORMATION_MESSAGE, null, possibleValues, possibleValues[0]);
      //String selectedValue = new CheckboxDialog(ge).showOptionDialog("Game Version","What game should this archive be compatable with?",possibleValues);
      if (selectedValue.equals("The Sims Online")) {
        lengthByteSize = 2;
      }

      long offset = 16;
      for (int i = 0; i < numFiles; i++) {
        String name = resources[i].getName();
        long length = resources[i].getDecompressedLength();

        // 4 - Raw File Size
        fm.writeInt((int) length);

        // 4 - Compressed File Size
        fm.writeInt((int) length);

        // 4 - Data Offset
        fm.writeInt((int) offset);

        // 4 -Filename Length
        if (lengthByteSize == 4) {
          fm.writeInt((int) name.length());
        }
        else {
          fm.writeShort((short) (short) name.length());
        }

        // X - Filename
        fm.writeString(name);

        offset += length;
      }

      fm.close();

    }
    catch (Throwable t) {
      logError(t);
    }
  }

}
