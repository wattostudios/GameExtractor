
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
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.exporter.Exporter_Custom_CA2;
import org.watto.ge.plugin.exporter.Exporter_ZLib;
import org.watto.io.FileManipulator;
import org.watto.io.FilenameSplitter;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_CAB extends ArchivePlugin {

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Plugin_CAB() {

    super("CAB", "CAB");

    //         read write replace rename
    setProperties(true, false, true, true);

    setGames("Forza MotorSport");
    setExtensions("cab", "ca2");
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

      // Check for the cab file
      if (FieldValidator.checkExtension(fm, extensions)) {
        rating += 25;

        // Header
        if (fm.readString(4).equals(new String(new byte[] { (byte) 170, (byte) 170, (byte) 192, (byte) 192 }))) {
          rating += 50;
        }

        fm.skip(4);

        // Number Of Files
        if (FieldValidator.checkNumFiles(fm.readInt())) {
          rating += 5;
        }

        // Version (1)
        if (fm.readInt() == 1) {
          rating += 5;
        }

        // Archive Size
        if (fm.readInt() == fm.getLength()) {
          rating += 5;
        }

        return rating;
      }
      // check for the ca2 file
      else if (FilenameSplitter.getExtension(fm.getFile()).equals(extensions[1])) {
        rating += 25;

        // ZLib compression header
        if (fm.readString(1).equals("x")) {
          rating += 5;
        }

        return rating;

      }

      return 0;

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

      ExporterPlugin exporter = Exporter_ZLib.getInstance();
      addFileTypes();

      // RESETTING THE GLOBAL VARIABLES

      if (path.getName().indexOf("." + extensions[1]) > 0) {
        //extract the cab file first
        ExporterPlugin ca2Exporter = Exporter_Custom_CA2.getInstance();
        FileManipulator extDir = new FileManipulator(new File("temp" + File.separator + "ca2_decompressed.dat"), true);
        String dirName = extDir.getFilePath();
        Resource directory = new Resource(path, dirName, 0, (int) path.length(), (int) path.length() * 20);

        ca2Exporter.extract(directory, extDir);

        extDir.close();

        path = new File(dirName);

        // important for repacking!
        Settings.set("CurrentArchive", path.getAbsolutePath());
      }

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 4 - Header? (170 170 192 192)
      // 4 - Unknown (15)
      fm.skip(8);

      // 4 - Number Of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // 4 - Version (1)
      // 4 - Archive Size
      // 4 - Decompressed Archive Size?
      fm.skip(12);

      // 4 - Filename Directory Length
      int filenameDirLength = fm.readInt();
      FieldValidator.checkLength(filenameDirLength, arcSize);

      // 4 - Filename Directory Length
      // 16 - null
      // 4 - Unknown (1)
      // 254 - null

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      /*
       * fm.skip(278 + (numFiles*24));
       *
       * // Loop through directory (FILENAMES) String[] names = new String[numFiles]; for(int
       * i=0;i<numFiles;i++){ // X - Filename // 1 - null Filename Terminator names[i] =
       * fm.readNullString(); //System.out.println(names[i]);
       * FieldValidator.checkFilename(names[i]); }
       */

      fm.seek(306);

      // Loop through directory
      int filenameRelOffset = 306 + (numFiles * 24);
      int[] filenameOffsets = new int[numFiles];
      for (int i = 0; i < numFiles; i++) {
        // 4 - File Offset
        long offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - Compressed File Size
        long length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - Decompressed File Size
        int decompLength = fm.readInt();

        // 4 - Filename Offset (relative to the start of the filename directory)
        int filenameOffset = fm.readInt() + filenameRelOffset;
        FieldValidator.checkOffset(filenameOffset, arcSize);
        filenameOffsets[i] = filenameOffset;

        // 4 - null
        // 4 - Unknown
        fm.skip(8);

        String filename = "";

        //path,id,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, filename, offset, length, decompLength);
        if (decompLength != length) {
          resources[i].setExporter(exporter);
        }

        TaskProgressManager.setValue(i);
      }

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

  **********************************************************************************************
  **/
  @Override
  public void write(Resource[] resources, File path) {
    try {

      ExporterPlugin exporter = Exporter_ZLib.getInstance();

      FileManipulator fm = new FileManipulator(path, true);
      FileManipulator src = new FileManipulator(new File(Settings.getString("CurrentArchive")), false);

      int numFiles = resources.length;

      // Calculations
      TaskProgressManager.setMessage(Language.get("Progress_PerformingCalculations"));

      int headerLength = 306 + (numFiles * 25);
      int filenameLength = 0;
      for (int i = 0; i < numFiles; i++) {
        filenameLength += resources[i].getNameLength();
      }

      int openingLength = headerLength + filenameLength;

      TaskProgressManager.setMaximum(numFiles);

      TaskProgressManager.setMessage(Language.get("Progress_WritingFiles"));
      // go to the start of the file data
      fm.setLength(openingLength);
      fm.seek(openingLength);
      long[] compressedLengths = write(exporter, resources, fm);
      // go back and write the directory
      fm.seek(0);

      // Write Header Data

      // 4 - Header? (170 170 192 192)
      // 4 - Unknown (15)
      // 4 - Number Of Files
      // 4 - Version (1)
      fm.writeBytes(src.readBytes(16));

      // 4 - Archive Size
      src.skip(4);
      fm.writeInt((int) fm.getLength());

      // 4 - Decompressed Archive Size?
      fm.writeBytes(src.readBytes(4));

      // 4 - Filename Directory Length
      // 4 - Filename Directory Length
      src.skip(8);
      fm.writeInt(filenameLength);
      fm.writeInt(filenameLength);

      // 16 - null
      // 2 - Unknown (1)
      // 256 - Archive Directory (null)
      fm.writeBytes(src.readBytes(274));

      long offset = openingLength;

      TaskProgressManager.setMessage(Language.get("Progress_WritingDirectory"));
      int filenameOffset = 0;
      for (int i = 0; i < numFiles; i++) {
        Resource fd = resources[i];
        long length = fd.getDecompressedLength();

        // 4 - File Offset
        src.skip(4);
        fm.writeInt((int) offset);

        // 4 - Compressed File Size
        src.skip(4);
        fm.writeInt((int) compressedLengths[i]);

        // 4 - Decompressed File Size
        src.skip(4);
        fm.writeInt((int) length);

        // 4 - Unknown
        fm.writeInt(filenameOffset);
        filenameOffset += fd.getName().length() + 1;

        // 4 - null
        // 4 - Unknown
        fm.writeBytes(src.readBytes(8));

        offset += compressedLengths[i];
      }

      for (int i = 0; i < numFiles; i++) {
        Resource fd = resources[i];
        String name = fd.getName();

        // X - Filename (null)
        fm.writeNullString(name);
      }

      src.close();

      String[] possibleValues = { "cab Archive", "ca2 Archive" };
      String selectedValue = (String) javax.swing.JOptionPane.showInputDialog(null, "What archive type do you wish to make?", "Archive Type", javax.swing.JOptionPane.INFORMATION_MESSAGE, null, possibleValues, possibleValues[0]);
      //String selectedValue = new CheckboxDialog(ge).showOptionDialog("Game Version","What game should this archive be compatable with?",possibleValues);
      if (selectedValue.equals("cab Archive")) {
        fm.close();
        return;
      }

      // else, compress the entire archive

      File oldPath = fm.getFile();

      String filePath = path.getAbsolutePath();
      int cabOffset = filePath.lastIndexOf(".cab");
      if (cabOffset > 0) {
        path = new File(filePath.substring(0, cabOffset) + ".ca2");
      }
      else {
        path = new File(filePath + ".ca2");
      }

      fm.close();
      fm = new FileManipulator(path, true);

      // ZLib compress the full archive
      String dirName = fm.getFilePath();
      Resource directory = new Resource(oldPath, dirName, 0, (int) oldPath.length());

      ExporterPlugin ca2Exporter = Exporter_Custom_CA2.getInstance();
      ca2Exporter.pack(directory, fm);

      fm.close();

    }
    catch (Throwable t) {
      logError(t);
    }
  }

}