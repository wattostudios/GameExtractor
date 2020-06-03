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
import java.util.Arrays;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.io.FileManipulator;
import org.watto.io.converter.ByteConverter;
import org.watto.io.converter.IntConverter;
import org.watto.io.converter.ShortConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_DAT_58 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_DAT_58() {

    super("DAT_58", "DAT_58");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Rival Realms");
    setExtensions("dat"); // MUST BE LOWER CASE
    setPlatforms("PC");

    // MUST BE LOWER CASE !!!
    //setFileTypes(new FileType("txt", "Text Document", FileType.TYPE_DOCUMENT),
    //             new FileType("bmp", "Bitmap Image", FileType.TYPE_IMAGE)
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

      // check that this is IMAGES.DAT or IMOFFS.DAT
      if (fm.getFile().getName().equalsIgnoreCase("IMAGES.DAT")) {
        rating += 25;
      }
      String path = fm.getFile().getParentFile().getAbsolutePath() + File.separator + "IMOFFS.DAT";
      if (new File(path).exists()) {
        rating += 25;
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

      // 4 - Number Of Files
      int numFiles = fm.readInt() * 10;
      FieldValidator.checkNumFiles(numFiles);

      // Now switch to IMOFFS.DAT to read the details
      String offsPath = fm.getFile().getParentFile().getAbsolutePath() + File.separator + "IMOFFS.DAT";
      File offsFile = new File(offsPath);
      if (!(offsFile.exists())) {
        return null;
      }

      fm.close();
      fm = new FileManipulator(offsFile, false);
      long dirSize = fm.getLength();

      // read the first group to work out how many groups there are
      // 4 - Offset to Group Details (in the Details Directory)
      int firstGroupOffset = fm.readInt();
      int numGroups = firstGroupOffset / 4;
      FieldValidator.checkNumFiles(numGroups);
      int[] groups = new int[numGroups];
      groups[0] = firstGroupOffset;

      int realNumGroups = 0;
      int previousOffset = firstGroupOffset;
      for (int i = 1; i < numGroups; i++) { // i=1, as we have already read the first one
        // 4 - Offset to Group Details (in the Details Directory)
        int offset = fm.readInt();
        if (offset == 0) {
          continue; // ignore nulls
        }
        if (offset == previousOffset) {
          continue; // ignore duplicate group offsets
        }
        FieldValidator.checkOffset(offset, dirSize);

        groups[realNumGroups] = offset;
        realNumGroups++;

        previousOffset = offset;
      }

      //Resource[] resources = new Resource[numFiles];
      int[] imageOffsets = new int[numFiles];
      TaskProgressManager.setMaximum(realNumGroups);

      int lastOffset = 0; // the end offset for the last real file in the archive. For calculating file sizes

      // now loop through each group
      int realNumFiles = 0;
      for (int i = 0; i < realNumGroups; i++) {
        fm.seek(groups[i]);

        // 1 - Number of Sub-Groups
        int numSubGroups = ByteConverter.unsign(fm.readByte());

        // 1 - Unknown
        int unknownI1 = ByteConverter.unsign(fm.readByte());

        // 1 - Image Width? (for each image in this group)
        int width = ByteConverter.unsign(fm.readByte());

        // 1 - Image Height? (for each image in this group)
        int height = ByteConverter.unsign(fm.readByte());

        System.out.println("Group " + (i + 1) + " of " + realNumGroups + " at offset " + (fm.getOffset() - 4));
        System.out.println("Num Sub Groups=" + numSubGroups + "\tunknownI1=" + unknownI1 + "\twidth=" + width + "\theight=" + height);

        // loop through each sub-group
        for (int j = 0; j < numSubGroups; j++) {
          // 1 - Number of Primary Images in this sub-group
          int numPrimary = ByteConverter.unsign(fm.readByte());
          // 1 - Number of Secondary Images in this sub-group
          int numSecondary = ByteConverter.unsign(fm.readByte());

          System.out.println("    Sub Group " + (j + 1) + " of " + numSubGroups + " at offset " + (fm.getOffset() - 2));
          System.out.println("    Num Primary=" + numPrimary + "\tNum Secondary=" + numSecondary);

          int numImagesTotal = numPrimary * numSecondary;

          // loop through each image
          for (int k = 0; k < numImagesTotal; k++) {
            System.out.println("        Image " + (k + 1) + " of " + numImagesTotal);

            // 3 - File Offset (in IMAGES.DAT file)
            byte[] offsetBytes = new byte[] { fm.readByte(), fm.readByte(), fm.readByte(), 0 };
            int offset = IntConverter.convertLittle(offsetBytes);
            FieldValidator.checkOffset(offset, arcSize);

            // 1 - Unknown
            int unknownK1 = ByteConverter.unsign(fm.readByte());

            // 2 - File Length
            int length = ShortConverter.unsign(fm.readShort());

            // 1 - Unknown
            int unknownK2 = ByteConverter.unsign(fm.readByte());

            // 1 - Unknown
            int unknownK3 = ByteConverter.unsign(fm.readByte());

            //String filename = Resource.generateFilename(realNumFiles);

            //path,name,offset,length,decompLength,exporter
            //resources[realNumFiles] = new Resource(path, filename, offset);

            if (offset == 0 && length == 0) {
              System.out.println("            Skipping null offset");
              continue;
            }

            if (unknownK1 != 0) {
              System.out.println("        OTHER FILE " + unknownK1 + "\tOffset=" + offset + "\tMaybeLength=" + length + "\tunknownK1=" + unknownK1 + "\tunknownK2=" + unknownK2 + "\tunknownK2=" + unknownK3);
              continue;
            }

            System.out.println("        Offset=" + offset + "\tMaybeLength=" + length + "\tunknownK1=" + unknownK1 + "\tunknownK2=" + unknownK2 + "\tunknownK2=" + unknownK3);

            int endOfFile = offset + length;
            if (endOfFile > lastOffset) {
              lastOffset = endOfFile;
            }

            imageOffsets[realNumFiles] = offset;
            realNumFiles++;
          }

        }

        TaskProgressManager.setValue(i);
      }

      numFiles = realNumFiles;

      // Sort the offsets
      Arrays.sort(imageOffsets, 0, numFiles); // only sort the realNumFiles amount, not all the unfilled nulls at the end of the array

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Go through, remove any duplicates, and create the Resources
      realNumFiles = 0;
      previousOffset = -1;
      for (int i = 0; i < numFiles; i++) {
        int offset = imageOffsets[i];
        if (offset == previousOffset) {
          continue; // no duplicates
        }

        String filename = Resource.generateFilename(realNumFiles);

        //path,name,offset,length,decompLength,exporter
        resources[realNumFiles] = new Resource(path, filename, offset);
        realNumFiles++;

        previousOffset = offset;

        TaskProgressManager.setValue(i);
      }

      resources = resizeResources(resources, realNumFiles);

      calculateFileSizes(resources, lastOffset);

      fm.close();

      return resources;

    }
    catch (Throwable t) {
      logError(t);
      return null;
    }
  }

}
