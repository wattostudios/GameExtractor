/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2024 wattostudios
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

import org.watto.Language;
import org.watto.Settings;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.exporter.Exporter_Custom_NeoXZLib;
import org.watto.ge.plugin.exporter.Exporter_Default;
import org.watto.ge.plugin.exporter.Exporter_ZLib;
import org.watto.io.FileManipulator;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_NPK_NXPK extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_NPK_NXPK() {

    super("NPK_NXPK", "NPK_NXPK");

    //         read write replace rename
    setProperties(true, false, true, false);

    setGames("Identity V");
    setExtensions("npk"); // MUST BE LOWER CASE
    setPlatforms("PC");

    //setFileTypes(new FileType("txt", "Text Document", FileType.TYPE_DOCUMENT),
    //             new FileType("bmp", "Bitmap Image", FileType.TYPE_IMAGE)
    //             );

    setTextPreviewExtensions("neox", "fxgroup"); // LOWER CASE

    // There is some kind of issue with the decompression that we can't work around, encryption or something.
    // This plugin was working prior to the game Update 2.0 in April 2023.
    //setEnabled(false); 
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
      if (fm.readString(4).equals("NXPK")) {
        rating += 50;
      }

      // Number Of Files
      if (FieldValidator.checkNumFiles(fm.readInt() / 5)) {
        rating += 5;
      }

      String arcName = fm.getFile().getName();
      if (arcName.contains(".part.") && arcName.contains("_0001")) {
        rating += 5;
      }
      else {
        fm.skip(12);

        long arcSize = fm.getLength();

        // Directory Offset
        if (FieldValidator.checkOffset(fm.readInt(), arcSize)) {
          rating += 5;
        }
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

      ExporterPlugin exporterZLib = Exporter_ZLib.getInstance();

      // RESETTING GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 4 - Header (NXPK)
      fm.skip(4);

      // 4 - Number of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles / 5);

      // 4 - Version 1 (0)
      // 4 - Version 2 (0)
      // 4 - Version 3 (1)
      fm.skip(12);

      // 4 - Directory Offset
      long dirOffset = fm.readInt();
      FieldValidator.checkOffset(dirOffset, arcSize);

      fm.seek(dirOffset);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {

        // 4 - Filename Hash
        fm.skip(4);

        // 4 - File Offset
        long offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - Compressed File Length
        int length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - Decompressed File Length
        int decompLength = fm.readInt();
        FieldValidator.checkLength(decompLength);

        // 4 - Compressed CRC
        fm.skip(4);

        // 4 - Decompressed CRC
        int decompCRC = fm.readInt();

        // 2 - Compression Flag (1=ZLib Compression)
        int compressionFlags = fm.readShort();

        // 2 - Encryption Flag (3=Encryption)
        int encryptionFlags = fm.readShort();

        String filename = Resource.generateFilename(i);

        if (encryptionFlags == 3) {
          // Encrypted
          if ((compressionFlags & 1) == 1) {
            // ZLib compression

            //path,name,offset,length,decompLength,exporter
            Resource resource = new Resource(path, filename, offset, length, decompLength, new Exporter_Custom_NeoXZLib(length, decompLength, decompCRC, true));
            resources[i] = resource;
          }
          else {
            // No compression

            //path,name,offset,length,decompLength,exporter
            Resource resource = new Resource(path, filename, offset, length, decompLength, new Exporter_Custom_NeoXZLib(length, decompLength, decompCRC, false));
            resource.addProperty("DecompressedCRC", decompCRC);
            resources[i] = resource;
          }
        }
        else {
          // Not Encrypted
          if ((compressionFlags & 1) == 1) {
            // ZLib compression

            //path,name,offset,length,decompLength,exporter
            resources[i] = new Resource(path, filename, offset, length, decompLength, exporterZLib);
          }
          else {
            // No compression

            //path,name,offset,length,decompLength,exporter
            resources[i] = new Resource(path, filename, offset, length, decompLength);
          }
        }

        TaskProgressManager.setValue(i);
      }

      fm.close();

      return resources;

    }
    catch (

    Throwable t) {
      logError(t);
      return null;
    }
  }

  /**
   **********************************************************************************************
   * Writes an [archive] File with the contents of the Resources. The archive is written using
   * data from the initial archive - it isn't written from scratch.
   **********************************************************************************************
   **/
  @Override
  public void replace(Resource[] resources, File path) {
    try {

      FileManipulator fm = new FileManipulator(path, true);
      FileManipulator src = new FileManipulator(new File(Settings.getString("CurrentArchive")), false);

      int numFiles = resources.length;
      TaskProgressManager.setMaximum(numFiles);

      // Calculations
      TaskProgressManager.setMessage(Language.get("Progress_PerformingCalculations"));

      long dirOffset = 24;
      for (int i = 0; i < numFiles; i++) {
        dirOffset += resources[i].getLength();
      }

      // Write Header Data

      // 4 - Header (NXPK)
      // 4 - Number of Files
      // 4 - Version 1 (0)
      // 4 - Version 2 (0)
      // 4 - Version 3 (1)
      fm.writeBytes(src.readBytes(20));

      // 4 - Directory Offset
      int srcDirOffset = src.readInt();
      fm.writeInt(dirOffset);

      src.seek(srcDirOffset);

      ExporterPlugin exporterDefault = Exporter_Default.getInstance();

      // Write Files
      TaskProgressManager.setMessage(Language.get("Progress_WritingFiles"));
      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];

        // If the file is replaced, want to read it in using the Default exporter.
        // If the file isn't replaced, we want to keep it compressed (if it's compressed), so use the Default exporter as well.

        ExporterPlugin oldExporter = resource.getExporter();
        resource.setExporter(exporterDefault);

        write(resource, fm);
        TaskProgressManager.setValue(i);

        resource.setExporter(oldExporter);
      }

      // Write Directory
      TaskProgressManager.setMessage(Language.get("Progress_WritingDirectory"));
      long offset = 24;
      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];
        long length = resource.getLength();

        // 4 - Filename Hash
        fm.writeBytes(src.readBytes(4));

        // 4 - File Offset
        fm.writeInt(offset);
        src.skip(4);

        // 4 - Compressed File Length
        // 4 - Decompressed File Length
        if (resource.isReplaced()) {
          fm.writeInt(length);
          fm.writeInt(length);
          src.skip(8);
        }
        else {
          fm.writeBytes(src.readBytes(8));
        }

        // 4 - Compressed CRC
        // 4 - Decompressed CRC
        fm.writeBytes(src.readBytes(8));

        // 4 - Flags
        if (resource.isReplaced()) {
          fm.writeInt(0);
          src.skip(4);
        }
        else {
          fm.writeBytes(src.readBytes(4));
        }

        offset += length;
      }

      src.close();
      fm.close();

    }
    catch (Throwable t) {
      logError(t);
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

    //System.out.println(resource.getName());

    if (headerInt1 == 1263686734) {
      return "ntrk";
    }
    else if (headerInt1 == 1347241037) {
      return "mdmp";
    }
    else if (headerInt1 == 1397311314) {
      return "gis";
    }
    else if (headerInt2 == 1146375250) {
      return "rltd";
    }
    else if (headerInt2 == 1145979222) {
      return "vand";
    }
    else if (headerInt2 == 1331185230) {
      return "nfxo";
    }
    else if (headerInt1 == 55727696) {
      return "pvr";
    }
    else if (headerInt1 == 1481919403) {
      return "ktx";
    }
    else if (headerInt1 == -1144487884) { // according to https://github.com/zhouhang95/neox_tools/blob/master/extractor.py
      return "mesh";
    }
    else if (headerInt1 == 67324752) {
      return "zip";
    }
    else if (headerInt1 == 1751607628) {
      return "LightProbeEditorFile";
    }
    else if (headerInt1 == 1414743380) {
      return "test";
    }
    else if (headerInt1 == 1297302868) {
      return "tesm";
    }
    else if (headerInt1 == 1397315139) {
      return "cvis";
    }
    else if (headerInt1 == 7563638) {
      return "vis";
    }
    else if (headerBytes[0] == 60) { // XML (raw)
      return "xml";
    }
    else if (headerInt1 == 1019198447) { // XML (unicode)
      return "xml";
    }
    else if (headerShort1 == 2573 || headerShort1 == 12079 || headerShort1 == 10799 || headerShort1 == 3451 || headerInt1 == 544501353 || headerInt1 == 543582499 || headerInt1 == 1970435187 || headerInt1 == 1920234593 || headerInt1 == 1886216563 || headerInt1 == 1852205347 || headerInt1 == 1718185589 || headerInt1 == 1717920803 || headerInt1 == 1668180259 || headerInt1 == 1667592816 || headerInt1 == 1634692198 || headerInt1 == 1230196560 || headerInt1 == 1129858388 || headerInt1 == 1684631414 || headerInt1 == 1852404597 || headerInt1 == 1954047267 || headerInt1 == 1954047348 || headerInt1 == 1684433187 || headerInt1 == 1835103008 || headerInt1 == 1870225772 || headerInt1 == 2037539190) {
      return "txt";
    }

    // some kind of script or something, from this point on
    /*
    else if (headerInt1 > 1597300000 && headerInt1 < 2054900000) {
      return "mesh";
    }
    else if (headerInt1 > 6780000 && headerInt1 < 7960000) {
      return "mesh";
    }
    else if (headerInt1 == 812083568 || headerInt1 == 892759396) {
      return "mesh";
    }
    */
    /*
    else if (headerInt1 == 1597322600) {
      return "h55";
    }
    else if (headerInt1 == 1600286069) {
      return "umb";
    }
    else if (headerInt1 == 1600417381) {
      return "end";
    }
    else if (headerInt1 == 1600481636) {
      return "die";
    }
    else if (headerInt1 == 1600484213) {
      return "use";
    }
    else if (headerInt1 == 1600612708) {
      return "dig";
    }
    else if (headerInt1 == 1601069414) {
      return "fan";
    }
    else if (headerInt1 == 1601070448) {
      return "pen";
    }
    else if (headerInt1 == 1601072994) {
      return "bon";
    }
    else if (headerInt1 == 1601074546) {
      return "run";
    }
    else if (headerInt1 == 1601202546) {
      return "rip";
    }
    else if (headerInt1 == 1601462627) {
      return "cat";
    }
    else if (headerInt1 == 1601463655) {
      return "get";
    }
    else if (headerInt1 == 1601463664) {
      return "pet";
    }
    else if (headerInt1 == 1601463667) {
      return "set";
    }
    else if (headerInt1 == 1601464439) {
      return "wht";
    }
    else if (headerInt1 == 1601464680) {
      return "hit";
    }
    else if (headerInt1 == 1601464691) {
      return "sit";
    }
    else if (headerInt1 == 1601467747) {
      return "cut";
    }
    else if (headerInt1 == 1601659251) {
      return "saw";
    }
    else if (headerInt1 == 1601662828) {
      return "low";
    }
    else if (headerInt1 == 1601793126) {
      return "fly";
    }
    else if (headerInt1 == 1601793890) {
      return "boy";
    }
    else if (headerInt1 == 1601794152) {
      return "hpy";
    }
    else if (headerInt1 == 1601794672) {
      return "pry";
    }
    else if (headerInt1 == 1601861753) {
      return "yxz";
    }
    else if (headerInt1 == 1633903986) {
      return "recall";
    }
    else if (headerInt1 == 1633907557) {
      return "escape";
    }
    else if (headerInt1 == 1634038370) {
      return "break";
    }
    else if (headerInt1 == 1634038371) {
      return "create";
    }
    else if (headerInt1 == 1634038388) {
      return "treat";
    }
    else if (headerInt1 == 1634165104) {
      return "pagan";
    }
    else if (headerInt1 == 1634496627) {
      return "splash";
    }
    else if (headerInt1 == 1634754918) {
      return "fapai";
    }
    else if (headerInt1 == 1634755954) {
      return "repair";
    }
    else if (headerInt1 == 1634953572) {
      return "disappear";
    }
    else if (headerInt1 == 1635020389) {
      return "entangled";
    }
    else if (headerInt1 == 1635020658) {
      return "rotate";
    }
    else if (headerInt1 == 1635021921) {
      return "attack";
    }
    else if (headerInt1 == 1635151465) {
      return "invalid";
    }
    else if (headerInt1 == 1635412332) {
      return "lizard";
    }
    else if (headerInt1 == 1650553447) {
      return "grab";
    }
    // MORE HERE...
    */

    return null;
  }

}
