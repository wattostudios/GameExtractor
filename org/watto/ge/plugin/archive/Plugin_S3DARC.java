
package org.watto.ge.plugin.archive;

import java.io.File;
import org.watto.Settings;
import org.watto.component.WSPluginException;
import org.watto.component.WSPopup;
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
public class Plugin_S3DARC extends ArchivePlugin {

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Plugin_S3DARC() {

    super("S3DARC", "S3DARC");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("TimeShift");
    setExtensions("s3darc"); // MUST BE LOWER CASE
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

      // Length 1
      if (fm.readInt() == 4) {
        rating += 5;
      }

      // Length 2
      if (fm.readInt() == 12) {
        rating += 5;
      }

      fm.skip(12);

      // Length 1
      if (fm.readInt() == 4) {
        rating += 5;
      }

      // Length 2
      if (fm.readInt() == 12) {
        rating += 5;
      }

      fm.skip(12);

      long arcSize = fm.getLength();

      // Directory Offset
      if (FieldValidator.checkOffset(fm.readInt(), arcSize)) {
        rating += 4;
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

      // BLOCK 1
      // 4 - Decompressed Length (4)
      // 4 - Compressed Length (12)
      // 12 - Compressed Data (ZLib)

      // when decompressed
      // 4 - Unknown (1)

      // BLOCK 2
      // 4 - Decompressed Length (4)
      // 4 - Compressed Length (12)
      // 12 - Compressed Data (ZLib)

      // when decompressed
      // 4 - Unknown (0)
      fm.skip(40);

      // 4 - Directory Block Offset
      int dirOffset = fm.readInt();
      FieldValidator.checkOffset(dirOffset, arcSize);

      fm.seek(dirOffset);

      // 4 - Decompressed Length
      int dirDecompressedLength = fm.readInt();
      FieldValidator.checkLength(dirDecompressedLength);

      // 4 - Compressed Length
      int dirLength = fm.readInt();
      FieldValidator.checkLength(dirLength, arcSize);

      dirOffset += 8;

      // check ZLib header
      if (!fm.readString(1).equals("x")) {
        throw new WSPluginException("Compression tag missing");
      }

      fm.close();

      // ask - do you want to decompress?
      if (Settings.getBoolean("Popup_DecompressArchive_Show")) {
        String ok = WSPopup.showConfirm("DecompressArchive", true);
        if (!ok.equals(WSPopup.BUTTON_YES)) {
          throw new WSPluginException("User chose not to decompress archive.");
        }
      }

      // Decompress the file
      File tempFile = new File(Settings.get("TempDirectory") + File.separator + "S3DARC_Decompressed");
      FileManipulator outfm = new FileManipulator(tempFile, true, 100000);
      tempFile = outfm.getFile();

      String dirName = tempFile.getPath();
      Resource directory = new Resource(path, dirName, dirOffset, dirLength, dirDecompressedLength);

      exporter.extract(directory, outfm);

      outfm.close();

      // Open the decompressed file
      fm = new FileManipulator(tempFile, false);

      // 4 - Number Of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {
        // 4 - Filename Length
        int filenameLength = fm.readInt();
        FieldValidator.checkFilenameLength(filenameLength);

        // X - Filename
        String filename = fm.readString(filenameLength);
        filename = filename.replace('#', '/');

        // 4 - File Type ID? (0/2/5/6/7/8)
        int fileType = fm.readInt();
        if (fileType == 6) {
          filename += ".pict";
        }

        // 4 - File Offset
        int offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - Hash?
        fm.skip(4);

        // 4 - Decompressed Length
        int decompLength = fm.readInt();
        FieldValidator.checkLength(decompLength);

        // 4 - Compressed Length
        int length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

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

}
