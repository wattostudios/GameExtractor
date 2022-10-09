/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2021 wattostudios
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
import org.watto.ErrorLogger;
import org.watto.datatype.FileType;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.io.FileManipulator;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_LNK_K300 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_LNK_K300() {

    super("LNK_K300", "LNK_K300");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Nioh");
    setExtensions("lnk"); // MUST BE LOWER CASE
    setPlatforms("PC");

    // MUST BE LOWER CASE !!!
    setFileTypes(new FileType("g1t", "G1T Image Archive", FileType.TYPE_ARCHIVE),
        new FileType("eff", "EFF Archive", FileType.TYPE_ARCHIVE),
        new FileType("tdpack", "TD Archive", FileType.TYPE_ARCHIVE),
        new FileType("tmg_pack", "TMG Archive", FileType.TYPE_ARCHIVE),
        new FileType("g2a_pack", "G2A Archive", FileType.TYPE_ARCHIVE),
        new FileType("g1e_pack", "G1E Archive", FileType.TYPE_ARCHIVE),
        new FileType("g1m_pack", "G1M Archive", FileType.TYPE_ARCHIVE),
        new FileType("g1h_pack", "G1H Archive", FileType.TYPE_ARCHIVE),
        new FileType("g1copack", "G1CO Archive", FileType.TYPE_ARCHIVE),
        new FileType("col", "COL Archive", FileType.TYPE_ARCHIVE),
        new FileType("mdl", "MDL Archive", FileType.TYPE_ARCHIVE),
        new FileType("trr", "TRR Archive", FileType.TYPE_ARCHIVE));

    //setTextPreviewExtensions("colours", "rat", "screen", "styles"); // LOWER CASE

    setCanScanForFileTypes(true);

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
      if (fm.readString(4).equals("K300")) {
        rating += 50;
      }

      fm.skip(4);

      // Number Of Files
      if (FieldValidator.checkNumFiles((int) fm.readLong())) {
        rating += 5;
      }

      long arcSize = fm.getLength();

      // Archive Size
      if (FieldValidator.checkEquals(fm.readLong(), arcSize)) {
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

      // See if we can read in the filenames from the external file
      String[] names = null;
      try {
        String basePath = path.getParent() + File.separatorChar;

        String arcNumber = path.getName();
        arcNumber = arcNumber.substring(arcNumber.length() - 6, arcNumber.length() - 4);

        String namesPath = basePath + "lfm_order_" + arcNumber + ".bin";
        File namesFile = new File(namesPath);
        if (namesFile.exists()) {
          // open it and ready the names
          FileManipulator fm = new FileManipulator(namesFile, false);

          long arcSize = namesFile.length();

          // 4 - Header (LFMO)
          // 4 - Number of Source Files (1)
          fm.skip(8);

          // 4 - Number of Filenames
          int numFilenames = fm.readInt();
          FieldValidator.checkNumFiles(numFilenames);

          // 4 - Source Offsets Directory Offset (32)
          fm.skip(4);

          // 4 - Filename Offsets Directory Offset (40)
          int nameDirOffset = fm.readInt();
          FieldValidator.checkOffset(nameDirOffset, arcSize);

          // 4 - Name Directory Offset
          // 4 - Unknown
          // 4 - null
          fm.seek(nameDirOffset);

          int[] nameOffsets = new int[numFilenames];
          for (int i = 0; i < numFilenames; i++) {
            // 4 - null
            // 4 - File ID (incremental from 0)
            fm.skip(8);

            // 4 - Filename Offset
            int filenameOffset = fm.readInt();
            FieldValidator.checkOffset(filenameOffset, arcSize);

            nameOffsets[i] = filenameOffset;
          }

          names = new String[numFilenames];

          for (int i = 0; i < numFilenames; i++) {
            fm.relativeSeek(nameOffsets[i]);

            // X - Filename
            // 1 - null Filenameame Terminator
            String filename = fm.readNullString();
            FieldValidator.checkFilename(filename);
            names[i] = filename;
          }

          fm.close();
        }
      }
      catch (Throwable t) {
        // don't worry about names
        names = null;
      }

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 4 - Header (K300)
      // 4 - null
      fm.skip(8);

      // 8 - Number of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);
      fm.skip(4);

      // 8 - Archive Length
      // 8 - File Data Offset
      fm.skip(16);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {

        // 8 - File Offset
        long offset = fm.readLong();
        FieldValidator.checkOffset(offset, arcSize);

        // 8 - File Length
        long length = fm.readLong();
        FieldValidator.checkLength(length, arcSize);

        // 8 - File Length
        long length2 = fm.readLong();

        if (length != length2) {
          ErrorLogger.log(length + " vs " + length2);
        }

        // 8 - null
        fm.skip(8);

        // X - Filename (null)
        String filename = names[i];

        if (filename.startsWith("/")) {
          filename = filename.substring(1);
        }

        //path,name,offset,length,decompLength,exporter
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
  If an archive doesn't have filenames stored in it, the scanner can come here to try to work out
  what kind of file a Resource is. This method allows the plugin to provide additional plugin-specific
  extensions, which will be tried before any standard extensions.
  @return null if no extension can be determined, or the extension if one can be found
  **********************************************************************************************
  **/
  @Override
  public String guessFileExtension(Resource resource, byte[] headerBytes, int headerInt1, int headerInt2, int headerInt3, short headerShort1, short headerShort2, short headerShort3, short headerShort4, short headerShort5, short headerShort6) {

    String extension = resource.getExtension();
    if (extension != null && !extension.equals("")) {
      return extension;
    }

    if (headerInt1 == 1194415175) {
      return "g1t";
    }
    else if (headerInt1 == 1263555379) {
      return "kps3";
    }
    else if (headerInt1 == 1598108231 && headerInt2 == 1262698832) {
      return "g2a_pack";
    }
    else if (headerInt1 == 1598508372 && headerInt2 == 1262698832) {
      return "tmg_pack";
    }
    else if (headerInt1 == 1380337221 && headerInt2 == 1263555397) {
      return "eff";
    }
    else if (headerInt1 == 1263555423) {
      return "kps";
    }
    else if (headerInt1 == 1194413663) {
      return "g1n";
    }
    else if (headerInt1 == 1380729933 && headerInt2 == 1263555397) {
      return "mdl";
    }
    else if (headerInt1 == 1380732739 && headerInt2 == 1263555397) {
      return "col";
    }
    else if (headerInt1 == 1381126740 && headerInt2 == 1263555397) {
      return "trr";
    }
    else if (headerInt1 == 1313162067) {
      return "scen";
    }
    else if (headerInt1 == 1263747916) {
      return "lcsk";
    }
    else if (headerInt1 == 1263750228) {
      return "tlsk";
    }
    else if (headerInt1 == 1381192779) {
      return "ktsr";
    }
    else if (headerInt1 == 1094927684) {
      return "decal";
    }
    else if (headerInt1 == 1163282770) {
      return "river";
    }
    else if (headerInt1 == 1396789831) {
      return "grass";
    }
    else if (headerInt1 == 1297301836) {
      return "lasmap";
    }
    else if (headerInt1 == 1095779412) {
      return "tdpack";
    }
    else if (headerInt1 == 1145918036) {
      return "trmd";
    }
    else if (headerInt1 == 1397509983) {
      return "slo";
    }
    else if (headerInt1 == 0 && headerInt2 == 0 && headerInt3 == 0) {
      return "spkg";
    }

    return null;
  }

}
