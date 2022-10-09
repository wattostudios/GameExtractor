/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2020 wattostudios
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
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.exporter.Exporter_ZLib_CompressedSizeOnly;
import org.watto.io.FileManipulator;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_ARC_ARC_2 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_ARC_ARC_2() {

    super("ARC_ARC_2", "Capcom ARC Archive");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Lost Planet",
        "Mega Man X Legacy Collection");
    setExtensions("arc");
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

      // 4 - Header (ARC + null)
      if (fm.readString(4).equals("ARC" + (char) 0)) {
        rating += 50;
      }

      // 2 - Version (7)
      if (fm.readShort() == 7) {
        rating += 5;
      }

      // 2 - Number Of Files
      if (FieldValidator.checkNumFiles(fm.readShort())) {
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

      ExporterPlugin exporter = Exporter_ZLib_CompressedSizeOnly.getInstance();

      // RESETTING GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 4 - Header (ARC + null)
      // 2 - Version (7)
      fm.skip(6);

      // 2 - Number Of Files
      short numFiles = fm.readShort();
      FieldValidator.checkNumFiles(numFiles);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {
        // 64 - Filename (null terminated)
        String filename = fm.readNullString(64);
        FieldValidator.checkFilename(filename);

        // 4 - File Type ID
        int fileType = fm.readInt();
        if (fileType == 1018003574) {
          filename += ".tex";
        }
        else if (fileType == 1156029294) {
          filename += ".sdl";
        }
        else if (fileType == 667850998) {
          filename += ".rtx";
        }
        else if (fileType == 883475283 || fileType == 2047053644 || fileType == 1815824644 || fileType == 1471865736 || fileType == 864821228 || fileType == 1414542962 || fileType == 776455971 || fileType == 222029749 || fileType == 1696625072 || fileType == 731104429) {
          filename += ".xfs";
        }
        else if (fileType == 1010633733) {
          filename += ".png";
        }
        else if (fileType == 1437137716) {
          filename += ".anm";
        }
        else if (fileType == 272743838) {
          filename += ".mod";
        }
        else if (fileType == 592277764) {
          filename += ".cdf";
        }
        else if (fileType == 66773634) {
          filename += ".efa";
        }
        else if (fileType == 553864336) {
          filename += ".arcs";
        }
        else if (fileType == 697601978) {
          filename += ".dnrs";
        }
        else if (fileType == 1813848179) {
          filename += ".sreq";
        }
        else if (fileType == 867062535) {
          filename += ".spac";
        }
        else if (fileType == 1210801045) {
          filename += ".esl";
        }
        else if (fileType == 155808719) {
          filename += ".scst";
        }
        else if (fileType == 873417209) {
          filename += ".sdst";
        }
        else if (fileType == 941734221) {
          filename += ".ogg";
        }
        else if (fileType == 131436703) {
          filename += ".strq";
        }
        else if (fileType == 1289707753) {
          filename += ".msg";
        }
        else if (fileType == 1384607967) {
          filename += ".efs";
        }
        else if (fileType == 298329479) {
          filename += ".obja";
        }
        else if (fileType == 490057515) {
          filename += ".hit";
        }
        else if (fileType == 329180445) {
          filename += ".lmt";
        }
        else if (fileType == 1592916818) {
          filename += ".lcm";
        }
        else if (fileType == 956357328) {
          filename += ".sbc";
        }
        else if (fileType == 1023439125) {
          filename += ".wed";
        }
        else if (fileType == 1513942406) {
          filename += ".rrd";
        }
        else if (fileType == 1909891284) {
          filename += ".osf";
        }
        else if (fileType == 1311932796) {
          filename += ".bfx";
        }
        else if (fileType == 1412813435) {
          filename += ".havok";
        }
        else if (fileType == 1212519942) {
          filename += ".seq0";
        }
        else if (fileType == 641763256) {
          filename += ".fca";
        }
        else if (fileType == 1332739548) {
          filename += ".fcp";
        }
        else if (fileType == 1917712505) { // MEGAMAN
          filename += ".wav";
        }
        else if (fileType == 516048707) { // MEGAMAN
          filename += ".ctex";
        }
        else if (fileType == 638138255) { // MEGAMAN
          filename += ".cof";
        }
        else if (fileType == 606035435) { // MEGAMAN
          filename += ".tex";
        }
        else if (fileType == 96840727) { // MEGAMAN
          filename += ".pat";
        }
        else if (fileType == 1061689397) { // MEGAMAN
          filename += ".rlst";
        }
        else if (fileType == 466372966) { // MEGAMAN
          filename += ".srqr";
        }
        else if (fileType == 366445307) { // MEGAMAN
          filename += ".sbkr";
        }
        else if (fileType == 1637677031) { // MEGAMAN
          filename += ".col";
        }
        else if (fileType == 1242424190) { // MEGAMAN
          filename += ".dat4";
        }
        else if (fileType == 288493190) { // MEGAMAN
          filename += ".omp";
        }
        else if (fileType == 1807858375) { // MEGAMAN
          filename += ".ocl";
        }
        else if (fileType == 590226060) { // MEGAMAN
          filename += ".revr";
        }
        else if (fileType == 1126422586) { // MEGAMAN
          filename += ".xfs";
        }
        else if (fileType == 2013850128) { // MEGAMAN
          filename += ".rtx";
        }
        else if (fileType == 408118762) { // MEGAMAN
          filename += ".emp";
        }
        else if (fileType == 105528024) { // MEGAMAN
          filename += ".xml";
        }
        else if (fileType == 377338879) { // MEGAMAN
          filename += ".stqr";
        }
        else {
          // Unknown file type
          System.out.println("Hint: " + fileType + " = " + filename);
        }

        // 4 - Compressed Length
        long length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - Unknown
        fm.skip(4);

        // 4 - File Offset
        long offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        //path,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, filename, offset, length, length, exporter);

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
