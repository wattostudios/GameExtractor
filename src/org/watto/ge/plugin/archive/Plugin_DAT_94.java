/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2022 wattostudios
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
import org.watto.ge.plugin.exporter.Exporter_LZ4_Framed;
import org.watto.io.FileManipulator;
import org.watto.io.converter.ByteConverter;
import org.watto.io.converter.IntConverter;
import org.watto.io.converter.LongConverter;
import org.watto.io.converter.ShortConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_DAT_94 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_DAT_94() {

    super("DAT_94", "DAT_94");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("The Surge 2");
    setExtensions("dat"); // MUST BE LOWER CASE
    setPlatforms("PC");

    //setFileTypes(new FileType("txt", "Text Document", FileType.TYPE_DOCUMENT),
    //             new FileType("bmp", "Bitmap Image", FileType.TYPE_IMAGE)
    //             );

    //setTextPreviewExtensions("colours", "rat", "screen", "styles"); // LOWER CASE

    //setCanScanForFileTypes(true);

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

      // find the TOC file for this archive
      String filename = fm.getFile().getName();
      int underscorePos = filename.lastIndexOf('_');
      if (underscorePos > 0) {
        filename = filename.substring(0, underscorePos) + ".toc";
        filename = fm.getFile().getParentFile().getAbsolutePath() + File.separatorChar + filename;
        if (new File(filename).exists()) {
          rating += 25;
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

      ExporterPlugin exporter = Exporter_LZ4_Framed.getInstance();

      // RESETTING GLOBAL VARIABLES

      long arcSize = (int) path.length();

      // find the TOC file for this archive
      File sourcePath = null;
      String basePath = "";

      String tocFilename = path.getName();
      int underscorePos = tocFilename.lastIndexOf('_');
      if (underscorePos > 0) {
        basePath = tocFilename.substring(0, underscorePos);
        basePath = path.getParentFile().getAbsolutePath() + File.separatorChar + basePath;

        tocFilename = basePath + ".toc";
        sourcePath = new File(tocFilename);
        if (!new File(tocFilename).exists()) {
          return null; // TOC not found
        }
      }

      // find all the archives that this TOC reads over
      basePath += "_";
      int numArchives = 0;

      File[] archiveFiles = new File[100]; // 100 max (guess)
      long[] archiveLengths = new long[100];
      for (int i = 0; i < 100; i++) {
        String arcPath = basePath + i + ".dat";
        File arcFile = new File(arcPath);

        if (!arcFile.exists()) {
          break;
        }

        archiveFiles[numArchives] = arcFile;
        archiveLengths[numArchives] = arcFile.length();
        numArchives++;
      }

      // Now process the TOC
      FileManipulator fm = new FileManipulator(sourcePath, false);

      // 8 - Checksum?
      // 12 - null
      // 4 - TOC File Length [+24] (LITTLE ENDIAN)

      // 4 - Unknown
      // 4 - null
      // 4 - Unknown (1)
      // 2 - Unknown
      fm.skip(38);

      // 2 - Filename Length (18)
      int descLength = ShortConverter.changeFormat(fm.readShort());
      FieldValidator.checkRange(descLength, 0, 255);//guess

      // X - Filename (ResourcePackageTOC)
      fm.skip(descLength);

      // 0-3 - null Padding to a multiple of 4 bytes
      fm.skip(calculatePadding(descLength, 4));

      // 4 - Unknown
      // 4 - null
      // 4 - Unknown (1)
      // 2 - Unknown
      fm.skip(14);

      // 2 - Filename Length (20)
      descLength = ShortConverter.changeFormat(fm.readShort());
      FieldValidator.checkRange(descLength, 0, 255);//guess

      // X - Filename (ResourcePackageTOC64)
      fm.skip(descLength);

      // 0-3 - null Padding to a multiple of 4 bytes
      fm.skip(calculatePadding(descLength, 4));

      // 8 - Unknown
      // 2 - Unknown
      fm.skip(10);

      // 2 - Archive Name Length
      descLength = ShortConverter.changeFormat(fm.readShort());
      FieldValidator.checkRange(descLength, 0, 255);//guess

      // X - Archive Name
      fm.skip(descLength);

      // 0-3 - null Padding to a multiple of 4 bytes
      fm.skip(calculatePadding(descLength, 4));

      // 2 - Unknown
      fm.skip(2);

      // 4 - Number of Files
      int numFiles = IntConverter.changeFormat(fm.readInt());
      FieldValidator.checkNumFiles(numFiles);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {
        // 4 - Unknown
        // 4 - null
        // 4 - Unknown (1)
        // 2 - Unknown
        fm.skip(14);

        // 2 - Filename Length
        short filenameLength = ShortConverter.changeFormat(fm.readShort());
        FieldValidator.checkFilenameLength(filenameLength);

        // X - Filename
        String filename = fm.readString(filenameLength);

        // 0-3 - null Padding to a multiple of 4 bytes
        fm.skip(calculatePadding(filenameLength, 4));

        // 8 - File Offset
        byte[] offsetBytes = fm.readBytes(8);
        long offset = LongConverter.convertBig(offsetBytes);

        // 4 - Compressed File Length
        long length = IntConverter.changeFormat(fm.readInt());

        // 4 - Decompressed File Length
        long decompLength = IntConverter.changeFormat(fm.readInt());
        FieldValidator.checkLength(decompLength);

        // 4 - Unknown
        // 4 - Unknown
        fm.skip(8);

        // 1 - Unknown (3)
        int fileType = fm.readByte();
        String extension = null;
        if (fileType == 1) {
          extension = ".mesh"; // Fledge Mesh V103
        }
        else if (fileType == 3) {
          extension = ".dds";
        }
        else if (fileType == 4) {
          extension = ".nsx"; //(NSX_MESH)
        }
        else if (fileType == 6) {
          extension = ".nav";
        }
        else if (fileType == 7) {
          extension = ".anim"; // SkeletalAnimationSet
        }
        else if (fileType == 10) {
          extension = ".phys"; // Breakables etc (PhysX)
        }
        else if (fileType == 12) {
          extension = ".lang";
        }
        else if (fileType == 15) {
          extension = ".morph"; // fmorph 
        }
        else {
          extension = "." + fileType;
        }
        filename += extension;

        // 1 - Archive Number
        int arcNumber = ByteConverter.unsign(fm.readByte());
        FieldValidator.checkRange(arcNumber, 0, numArchives);

        path = archiveFiles[arcNumber];
        arcSize = archiveLengths[arcNumber];

        FieldValidator.checkOffset(offset, arcSize);
        FieldValidator.checkLength(length, arcSize);

        //path,name,offset,length,decompLength,exporter
        Resource resource = new Resource(path, filename, offset, length, decompLength, exporter);
        resource.forceNotAdded(true);
        resources[i] = resource;

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

    /*
    if (headerInt1 == 2037149520) {
      return "js";
    }
    */

    return null;
  }

}
