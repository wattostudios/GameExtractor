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
public class Plugin_G2A_G2APACK extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_G2A_G2APACK() {

    super("G2A_G2APACK", "G2A_G2APACK");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Nioh");
    setExtensions("g2a_pack", "tmg_pack", "eff", "tdpack", "col", "mdl", "trr", "g1e_pack", "g1m_pack", "g1h_pack", "headpack", "g1copack"); // MUST BE LOWER CASE
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
      String header = fm.readString(8);
      if (header.equals("EFFRESPK") || header.equals("COLRESPK") || header.equals("MDLRESPK") || header.equals("MDLTEXPK") || header.equals("TRRRESPK") || header.equals("G1E_PACK") || header.equals("G1M_PACK") || header.equals("G1H_PACK") || header.equals("G2A_PACK") || header.equals("HEADPACK") || header.equals("G1COPACK") || header.substring(0, 6).equals("TDPACK")) {
        rating += 50;
      }

      fm.skip(8);

      long arcSize = fm.getLength();

      // Archive Size
      if (FieldValidator.checkEquals(fm.readInt(), arcSize)) {
        rating += 5;
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

      //ExporterPlugin exporter = Exporter_ZLib.getInstance();

      // RESETTING GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 8 - Header (G2A_PACK)
      // 4 - Version
      // 4 - Header Length (48)
      // 4 - Archive Length
      fm.skip(20);

      // 4 - Number of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // 4 - Unknown
      // 4 - Unknown
      fm.skip(8);

      // 4 - Offsets Directory Offset
      int offsetDirOffset = fm.readInt();
      FieldValidator.checkOffset(offsetDirOffset, arcSize);

      // 4 - Lengths Directory Offset
      int lengthDirOffset = fm.readInt();
      FieldValidator.checkOffset(lengthDirOffset, arcSize);

      // 4 - Extras Directory Offset
      // 4 - null

      fm.seek(offsetDirOffset);

      long[] offsets = new long[numFiles];
      for (int i = 0; i < numFiles; i++) {
        // 4 - File Offset
        int offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        offsets[i] = offset;
      }

      fm.seek(lengthDirOffset);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {

        long offset = offsets[i];

        // 4 - File Length
        int length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        String filename = Resource.generateFilename(i);

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
    else if (headerInt1 == 1194475871) {
      return "g2a";
    }
    else if (headerInt1 == 1194480479) {
      return "g2s";
    }
    else if (headerInt1 == 1263814219) {
      return "kftk";
    }
    else if (headerInt1 == 1194413407) {
      return "g1m";
    }
    else if (headerInt1 == 1194411341) {
      return "g1em";
    }
    else if (headerInt1 == 1194411608) {
      return "g1fx";
    }
    else if (headerInt1 == 1329803591 && headerInt2 == 1262698832) {
      return "g1copack";
    }
    else if (headerInt1 == 1598894407 && headerInt2 == 1262698832) {
      return "g1m_pack";
    }
    else if (headerInt1 == 1598370119 && headerInt2 == 1262698832) {
      return "g1e_pack";
    }
    else if (headerInt1 == 1145128264) {
      return "head";
    }
    else if (headerInt1 == 1194410831) {
      return "g1co";
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
