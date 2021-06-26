
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
import org.watto.ge.plugin.exporter.Exporter_LZSS_Old;
import org.watto.io.FileManipulator;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_ARK_2 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_ARK_2() {

    super("ARK_2", "ARK_2");

    //         read write replace rename
    setProperties(true, false, true, false);

    //allowImplicitReplacing = true;

    setGames("MotoGP Ultimate Racing Technology 3");
    setExtensions("ark");
    setPlatforms("PC");

    setFileTypes("cmp", "Compressed File",
        "nml", "Non-Compressed File");

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

      // null
      if (fm.readInt() == 0) {
        rating += 5;
      }

      long arcSize = fm.getLength();

      // First File Offset
      if (FieldValidator.checkOffset(fm.readInt() + 8, arcSize)) {
        rating += 5;
      }

      // Number Of Files
      if (FieldValidator.checkNumFiles(fm.readInt())) {
        rating += 5;
      }

      fm.skip(4);

      // null
      if (fm.readInt() == 0) {
        rating += 5;
      }

      // First File Offset
      if (FieldValidator.checkOffset(fm.readInt() + 8, arcSize)) {
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
      ExporterPlugin exporter = Exporter_LZSS_Old.getInstance();

      // RESETTING THE GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = (int) fm.getLength();

      // 4 - null
      // 4 - First File Offset [+8]
      fm.skip(8);

      // 4 - Number Of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // 4 - Unknown (16)
      // 4 - null
      // 4 - First File Offset [+8]
      fm.skip(12);

      Resource[] resources = new Resource[numFiles];

      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {
        // 4 - Compression Tag? (0=Decompressed 1=Compressed)
        int compressed = fm.readInt();

        // 4 - File Offset
        long offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - File Length
        long length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - Hash?
        fm.skip(4);

        String ext = "cmp";
        if (compressed == 0) {
          ext = "nml";
        }

        String filename = Resource.generateFilename(i) + "." + ext;

        //path,id,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, filename, offset, length, length);

        if (compressed != 0) {
          resources[i].setExporter(exporter);
        }

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
  @SuppressWarnings("unused")
  public void write(Resource[] resources, File path) {
    try {

      ExporterPlugin exporter = Exporter_LZSS_Old.getInstance();

      FileManipulator fm = new FileManipulator(path, true);
      FileManipulator src = new FileManipulator(new File(Settings.getString("CurrentArchive")), false);

      int numFiles = resources.length;
      TaskProgressManager.setMaximum(numFiles);

      // 4 - null
      // 4 - First File Offset [+8]
      // 4 - Number Of Files
      // 4 - Unknown (16)
      // 4 - null
      // 4 - First File Offset [+8]
      fm.writeBytes(src.readBytes(24));

      long offset = (numFiles * 16) + 24;

      fm.setLength(offset);
      fm.seek(offset);

      // Write Files
      TaskProgressManager.setMessage(Language.get("Progress_WritingFiles"));
      long[] compressedLengths = write(exporter, resources, fm);

      fm.seek(24);

      TaskProgressManager.setMessage(Language.get("Progress_WritingDirectory"));
      for (int i = 0; i < numFiles; i++) {
        Resource fd = resources[i];
        long length = fd.getDecompressedLength();
        String name = fd.getName();

        // 4 - Compression Tag (0=Decompressed 1=Compressed)
        // 4 - File Offset
        // 4 - File Length
        src.skip(12);
        fm.writeInt(1);
        fm.writeInt((int) offset);
        fm.writeInt((int) compressedLengths[i]);

        // 4 - Hash?
        fm.writeBytes(src.readBytes(4));

        offset += compressedLengths[i];
      }

      src.close();

      fm.close();

    }
    catch (Throwable t) {
      logError(t);
    }
  }

}
