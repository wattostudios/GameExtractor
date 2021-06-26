
package org.watto.ge.plugin.archive;

import java.io.File;
import java.util.zip.InflaterInputStream;
import org.watto.Language;
import org.watto.Settings;
import org.watto.component.WSPluginException;
import org.watto.component.WSPopup;
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
import org.watto.io.stream.ManipulatorInputStream;
import org.watto.io.stream.ManipulatorOutputStream;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_SDF_RYS_2 extends ArchivePlugin {

  boolean doDecompress = false;

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Plugin_SDF_RYS_2() {

    super("SDF_RYS_2", "SDF_RYS_2");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("World In Conflict");
    setExtensions("sdf");
    setPlatforms("PC");

    //setFileTypes("","",
    //             "",""
    //             );

  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  @SuppressWarnings("resource")
  public FileManipulator decompressFile(FileManipulator fm) throws WSPluginException {
    if (doDecompress || Settings.getBoolean("Popup_DecompressArchive_Show")) {
      String ok = WSPopup.showConfirm("DecompressArchive", true);
      if (!ok.equals(WSPopup.BUTTON_YES)) {
        throw new WSPluginException("User chose not to decompress archive.");
      }
    }

    doDecompress = true;

    // create a temporary file
    File tempFile = new File(Settings.get("TempDirectory") + File.separator + "SDF_RYS_2_Dir_Decompressed");
    FileManipulator outfm = new FileManipulator(tempFile, true, 100000);

    ManipulatorInputStream inStream = new ManipulatorInputStream(fm);
    ManipulatorOutputStream outStream = new ManipulatorOutputStream(outfm);

    TaskProgressManager.setMessage(Language.get("Progress_DecompressingArchive"));

    InflaterInputStream inflater = new InflaterInputStream(inStream);
    try {
      int byteValue = inflater.read();
      while (inflater.available() >= 1) {
        outStream.write(byteValue);
        byteValue = inflater.read();
      }
    }
    catch (Throwable t) {
    }

    TaskProgressManager.setMessage(Language.get("Progress_ReadingArchive"));

    return outfm;
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
      if (fm.readString(4).equals("RYS" + (byte) 10)) {
        rating += 50;
      }

      long arcSize = fm.getLength();

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
      doDecompress = false;

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 3 - Header (RYS)
      // 1 - Version (10)
      fm.skip(4);

      // 4 - Entries Directory Offset
      long dirOffset = fm.readInt();
      FieldValidator.checkOffset(dirOffset, arcSize);

      fm.seek(dirOffset);

      // 4 - Decompressed Directory Length
      // 4 - Compressed Directory Length (if byte4 = 64, the directory is compressed)
      fm.skip(7);
      boolean compressed = false;
      if (fm.readByte() == 64) {
        compressed = true;
      }

      FileManipulator dirFM = fm;
      if (compressed) {
        // decompress
        // open the decompressed file
        dirFM = decompressFile(fm);
      }

      // 4 - Number Of Files
      int numFiles = dirFM.readInt();
      FieldValidator.checkNumFiles(numFiles);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // skip over this directory
      dirFM.skip(numFiles * 8);

      if (compressed) {
        // close the decompressed file
        dirFM.close();
      }

      dirFM = fm;

      // 4 - Unknown
      // 4 - Unknown
      compressed = false;
      if (fm.readInt() == 4 && fm.readInt() == 1) {
        // second dir is compressed
        compressed = true;

        // 4 - Decompressed Directory Length
        // 4 - Compressed Directory Length (if byte4 = 64, the directory is compressed)
        // 4 - Unknown
        // 4 - Decompressed Directory Length?
        // 4 - Unknown (if byte4 = 64, the directory is compressed)
        fm.skip(20);

        // decompress
        // open the decompressed file
        dirFM = decompressFile(fm);
      }
      else {
        // not decompressed

        // 4 - Unknown Length
        // 4 - Hash?
        // 4 - Unknown Length
        // 4 - Hash?
        // 4 - Unknown Length
        // 4 - Hash?
        // 4 - Unknown Length
        fm.skip(28);
      }

      for (int i = 0; i < numFiles; i++) {
        // 4 - Hash?
        dirFM.skip(4);

        // 4 - Length?
        long decompLength = dirFM.readInt();
        FieldValidator.checkLength(decompLength);

        // 4 - Length?
        long length = dirFM.readInt();
        FieldValidator.checkLength(length, arcSize);

        if (decompLength < length) {
          System.out.println("SDF_RYS_2 lengths in wrong order");
        }

        // 4 - File Offset
        long offset = dirFM.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - Unknown
        dirFM.skip(4);

        // X - Filename
        // 1 - null Filename Terminator
        String filename = dirFM.readNullString();
        FieldValidator.checkFilename(filename);

        // 0-3 - Padding (byte 255) to a multiple of 4 bytes
        int paddingSize = 4 - ((filename.length() + 1) % 4);
        if (paddingSize != 4) {
          dirFM.skip(paddingSize);
        }

        //path,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, filename, offset, length, decompLength);

        if (decompLength != length) {
          resources[i].setExporter(exporter);
        }

        TaskProgressManager.setValue(i);
      }

      if (compressed) {
        // close the decompressed file
        dirFM.close();
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
