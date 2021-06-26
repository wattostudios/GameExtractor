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
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.exporter.BlockExporterWrapper;
import org.watto.ge.plugin.exporter.Exporter_ZLib_CompressedSizeOnly;
import org.watto.io.FileManipulator;
import org.watto.io.converter.IntConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_DAT_25 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_DAT_25() {

    super("DAT_25", "DAT_25");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Juiced");
    setExtensions("dat");
    setPlatforms("PC", "PS2");

    // MUST BE LOWER CASE !!!
    //setFileTypes(new FileType("txt", "Text Document", FileType.TYPE_DOCUMENT),
    //             new FileType("bmp", "Bitmap Image", FileType.TYPE_IMAGE)
    //             );

    setTextPreviewExtensions("jetspline", "material", "atmosphere", "car", "actionmap", "mod", "dynamicobject", "script", "effect", "surfaceeffect", "collection", "collisionobject"); // LOWER CASE

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

      // Number Of Files
      if (FieldValidator.checkNumFiles(fm.readInt())) {
        rating += 5;
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
  
  **********************************************************************************************
  **/
  @Override
  public Resource[] read(File path) {
    try {

      //ExporterPlugin exporter = Exporter_Custom_DAT_25.getInstance();
      ExporterPlugin exporter = Exporter_ZLib_CompressedSizeOnly.getInstance();
      addFileTypes();

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 4 - Number Of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // 4 - Directory Offset
      long dirOffset = fm.readInt();
      FieldValidator.checkOffset(dirOffset, arcSize);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      fm.seek(dirOffset);

      // Loop through directory
      int[] partCount = new int[numFiles];
      boolean[] compressed = new boolean[numFiles];
      for (int i = 0; i < numFiles; i++) {
        // 4 - File ID
        fm.skip(4);

        // 4 - File Offset
        long offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - Number of File Parts
        int numParts = fm.readInt();
        FieldValidator.checkNumFiles(numParts + 1); // +1 to allow 0 length files
        partCount[i] = numParts;

        // 4 - Compressed File Size
        long length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - Decompressed File Size
        int decompLength = fm.readInt();
        FieldValidator.checkLength(decompLength);

        String filename = Resource.generateFilename(i);

        if (length == 0) {
          // Not compressed in parts - a raw file
          compressed[i] = false;

          length = decompLength;

          //path,id,name,offset,length,decompLength,exporter
          resources[i] = new Resource(path, filename, offset, length);

        }
        else {
          // compressed in parts
          compressed[i] = true;

          //path,id,name,offset,length,decompLength,exporter
          resources[i] = new Resource(path, filename, offset, length, decompLength);
        }

        TaskProgressManager.setValue(i);
      }

      // Now read all the compressed parts
      fm.getBuffer().setBufferSize(64);

      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];
        fm.seek(resource.getOffset());

        if (!compressed[i]) {
          String name = resource.getName();

          fm.skip(16);

          // 4 - Header
          byte[] headerBytes = fm.readBytes(4);
          int headerInt = IntConverter.convertLittle(headerBytes);
          if (headerInt == 1883717974) {
            name += ".vag";
          }
          else if (headerBytes[0] == 120) {
            name += ".dat";
          }

          resource.setName(name);
          resource.setOriginalName(name);

          continue;
        }

        int numParts = partCount[i];

        //System.out.println(fm.getOffset() + "\t" + numParts);

        long[] partOffsets = new long[numParts];
        long[] partLengths = new long[numParts];

        for (int p = 0; p < numParts; p++) {
          // 4 - File Part Offset
          int partOffset = fm.readInt();
          FieldValidator.checkOffset(partOffset, arcSize);
          partOffsets[p] = partOffset;

          // 4 - File Part Length
          int partLength = fm.readInt();
          FieldValidator.checkLength(partLength, arcSize);
          partLengths[p] = partLength;
        }

        BlockExporterWrapper blockExporter = new BlockExporterWrapper(exporter, partOffsets, partLengths, partLengths);
        resource.setExporter(blockExporter);
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

    // internal DAT files ... keep them as DAT
    String extension = resource.getExtension();
    if (extension.equals("dat")) {
      return "dat";
    }
    else if (extension.equals("vag")) {
      return "vag";
    }

    // guess other files
    if (headerInt1 == 1243626299 && headerInt2 == 1394635877 && headerInt3 == 1852402800) {
      return "jetspline";
    }
    else if (headerInt1 == 1702125901 && headerInt2 == 1818323314) {
      return "material";
    }
    else if (headerInt1 == 1734689371) {
      if (headerInt2 == 1950445161) {
        return "atmosphere";
      }
      else if (headerInt2 == 1631809129) {
        return "car";
      }
      else if (headerInt2 == 1665232489) {
        return "actionmap";
      }
      else if (headerInt2 == 1867345513 || headerInt2 == 1293971049) {
        return "mod";
      }
      else if (headerInt2 == 2034527849) {
        return "dynamicobject";
      }
      else if (headerInt2 == 1715826281) {
        return "effect";
      }
      else if (headerInt2 == 1968402025) {
        return "surfaceeffect";
      }
      else if (headerInt2 == 1866690153) {
        if (headerInt3 == 1667591276) {
          return "collection";
        }
        else if (headerInt3 == 1936288876) {
          return "collisionobject";
        }
        else {
          return "script";
        }
      }
      else {
        return "script";
      }
    }
    else if (headerInt1 == 827477587) {
      return "snr1";
    }

    else if (headerShort1 == 26997) {
      return "ui";
    }

    else if (headerShort1 == 2573 || headerShort1 == 3387 || headerShort1 == 8251 || headerShort1 == 15163) {
      return "script";
    }

    else if (headerShort1 == -257) {
      return "txt";
    }

    return null;
  }
}
