
package org.watto.ge.plugin.archive;

import java.io.File;
import org.watto.task.TaskProgressManager;
import org.watto.datatype.Archive;
import org.watto.datatype.Resource;
import org.watto.datatype.SplitChunkResource;
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
import org.watto.ge.plugin.exporter.Exporter_Custom_VFS;
import org.watto.io.FileManipulator;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_VFS extends ArchivePlugin {

  long relOffset = 0;
  int realNumFiles = 0;
  int[][] clusterMap;
  int clusterSize = 4096;
  int windowSize = 50000;

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Plugin_VFS() {

    super("VFS", "VFS");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("UFO Aftershock");
    setExtensions("vfs");
    setPlatforms("PC");

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

      fm.skip(4);

      // Cluster Size
      if (fm.readInt() == 4096) {
        rating += 5;
      }

      fm.skip(4);

      // Max Number Of Root Files
      if (fm.readInt() == 64) {
        rating += 5;
      }

      // null
      if (fm.readInt() == 0) {
        rating += 5;
      }

      // Max Filename Length
      if (fm.readInt() == 64) {
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
      ExporterPlugin exporter = Exporter_Custom_VFS.getInstance();

      // RESETTING THE GLOBAL VARIABLES
      realNumFiles = 0;

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = (int) fm.getLength();

      // 4 - Version (1.0 - float)
      fm.skip(4);

      // 4 - Cluster Size (4096)
      clusterSize = fm.readInt();

      // 4 - Number Of Clusters
      int numClusters = fm.readInt();

      // 4 - Maximum Number Of Root Files (64)
      int maxRootFiles = fm.readInt();

      // 4 - null
      // 4 - Maximum Filename Length (64)
      fm.skip(8);

      // 4 - Window Size (50000 - for compressed data)
      windowSize = fm.readInt();

      // 16 - Checksum (MD5 - calculated from offset 44 to EOF)
      // 4 - Maximum Description Length (256)
      // 256 - Description (null)
      // 4 - Number Of Used Clusters?
      fm.skip(280);

      clusterMap = new int[numClusters][2];
      for (int i = 0; i < numClusters; i++) {
        // 4 - Usage (0=unused, 1=used)
        int used = fm.readInt();

        // 4 - Next Cluster Number (-1 for the last clusted in the group)
        int next = fm.readInt();

        clusterMap[i] = new int[] { used, next };
      }

      int numFiles = Archive.getMaxFiles();

      Resource[] resources = new Resource[numFiles];

      TaskProgressManager.setMaximum(numFiles);

      relOffset = fm.getOffset() + (maxRootFiles * 88);

      // Loop through directory
      for (int i = 0; i < maxRootFiles; i++) {
        // 64 - Filename (null)
        String filename = fm.readNullString(64);
        if (filename.length() == 0) {
          // End of the root directory
          i = maxRootFiles;
          break;
        }

        FieldValidator.checkFilename(filename);

        // 4 - Unknown
        fm.skip(4);

        // 4 - Type (1=file, 2=directory, 9=compressed file)
        int type = fm.readInt();

        // 4 - Padding (-1)
        fm.skip(4);

        // 4 - Start Cluster
        int startCluster = fm.readInt() - 1;

        // 4 - Compressed File Length
        long length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - Decompressed File Length (0 if not compressed)
        int decompLength = fm.readInt();
        FieldValidator.checkLength(decompLength);

        int numFileClusters = (int) (length / clusterSize);
        int extraLength = (int) (length % clusterSize);
        if (extraLength != 0) {
          numFileClusters++;
        }

        long[] offsets = new long[numFileClusters];
        long[] lengths = new long[numFileClusters];

        int curClusterNum = startCluster;
        int curEntryPos = 0;
        while (curClusterNum >= 0) {
          int[] clusterInfo = clusterMap[curClusterNum];
          if (clusterInfo[0] == 1) {
            offsets[curEntryPos] = relOffset + ((curClusterNum) * clusterSize);
            lengths[curEntryPos] = clusterSize;
            curEntryPos++;
          }
          curClusterNum = clusterInfo[1] - 1;
        }

        if (extraLength != 0) {
          lengths[numFileClusters - 1] = extraLength;
        }

        if (type == 2) {
          // directory

          long curPos = fm.getOffset();
          readDirectory(path, fm, resources, filename + "\\", offsets, lengths);
          fm.seek(curPos);
        }
        else {
          // file

          //path,id,name,offset,length,decompLength,exporter
          resources[realNumFiles] = new SplitChunkResource(path, filename, offsets, lengths);
          resources[realNumFiles].setExporter(exporter);

          TaskProgressManager.setValue(offsets[i]);
          realNumFiles++;
        }
      }

      resources = resizeResources(resources, realNumFiles);

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
  public void readDirectory(File path, FileManipulator fm, Resource[] resources, String dirName, long[] dirOffsets, long[] dirLengths) throws Exception {
    long arcSize = fm.getLength();
    ExporterPlugin exporter = Exporter_Custom_VFS.getInstance();

    long dirLength = dirLengths[0];
    boolean fmChanged = false;
    if (dirOffsets.length > 1) {
      // extract the directory to the computer, and open a new FM to it
      FileManipulator dirTemp = new FileManipulator(new File("temp" + File.separator + "VFSTempDirectory.txt"), true);

      for (int i = 0; i < dirOffsets.length; i++) {
        fm.seek(dirOffsets[i]);
        dirTemp.writeBytes(fm.readBytes((int) dirLengths[i]));
      }

      dirLength = fm.getLength();
      fmChanged = true;
      fm = dirTemp;
    }
    else {
      fm.seek(dirOffsets[0]);
    }

    int numFiles = (int) (dirLength / 88);

    // Loop through directory
    for (int i = 0; i < numFiles; i++) {
      // 64 - Filename (null)
      String filename = fm.readNullString(64);
      if (filename.length() == 0) {
        // End of the root directory
        i = numFiles;
        break;
      }

      FieldValidator.checkFilename(filename);
      filename = dirName + filename;

      // 4 - Unknown
      fm.skip(4);

      // 4 - Type (1=file, 2=directory, 9=compressed file)
      int type = fm.readInt();

      // 4 - Padding (-1)
      fm.skip(4);

      // 4 - Start Cluster
      int startCluster = fm.readInt() - 1;

      // 4 - Compressed File Length
      long length = fm.readInt();
      FieldValidator.checkLength(length, arcSize);

      // 4 - Decompressed File Length (0 if not compressed)
      int decompLength = fm.readInt();
      FieldValidator.checkLength(decompLength);

      int numFileClusters = (int) (length / clusterSize);
      int extraLength = (int) (length % clusterSize);
      if (extraLength != 0) {
        numFileClusters++;
      }

      //System.out.println(numFileClusters);

      long[] offsets = new long[numFileClusters];
      long[] lengths = new long[numFileClusters];

      int curClusterNum = startCluster;
      int curEntryPos = 0;
      while (curClusterNum >= 0) {
        int[] clusterInfo = clusterMap[curClusterNum];
        if (clusterInfo[0] == 1) {
          offsets[curEntryPos] = relOffset + ((curClusterNum) * clusterSize);
          lengths[curEntryPos] = clusterSize;
          curEntryPos++;
        }
        curClusterNum = clusterInfo[1] - 1;
      }

      if (extraLength != 0) {
        lengths[numFileClusters - 1] = extraLength;
      }

      if (type == 2) {
        // directory

        if (length != 0) {
          long curPos = fm.getOffset();
          readDirectory(path, fm, resources, filename + "\\", offsets, lengths);
          fm.seek(curPos);
        }
      }
      else {
        // file

        if (length == 0) {
          lengths = new long[] { 0 };
          offsets = new long[] { 0 };
        }

        //path,id,name,offset,length,decompLength,exporter
        //System.out.println(realNumFiles + " - " + filename + " - " + length);

        resources[realNumFiles] = new SplitChunkResource(path, filename, offsets, lengths);
        if (type == 9) {
          // compressed
          resources[realNumFiles].setExporter(exporter);
        }

        TaskProgressManager.setValue(realNumFiles);
        realNumFiles++;
      }

      /*
       * if (type == 2){ int curPos = fm.getOffset(); readDirectory(path,fm,resources,dirName +
       * filename+"\\",length,startCluster); fm.seek(curPos); } else { int numFileClusters =
       * length/clusterSize; int extraLength = length%clusterSize; if (extraLength != 0){
       * numFileClusters++; }
       *
       * int[] offsets = new int[numFileClusters]; int[] lengths = new int[numFileClusters];
       *
       * int curClusterNum = startCluster; int curEntryPos = 0; while (curClusterNum != -1){
       * int[] clusterInfo = clusterMap[curClusterNum]; if (clusterInfo[0] == 1){
       * offsets[curEntryPos] = relOffset + ((curClusterNum)*clusterSize); lengths[curEntryPos] =
       * clusterSize; curEntryPos++; } curClusterNum = clusterInfo[1]; }
       *
       * if (extraLength != 0){ lengths[numFileClusters-1] = extraLength; }
       *
       * //path,id,name,offset,length,decompLength,exporter resources[realNumFiles] = new
       * SplitChunkResource(path,filename,offsets,lengths);
       *
       * TaskProgressManager.setValue(offsets[i]); realNumFiles++; }
       */

    }

    if (fmChanged) {
      // closes the temp dir file if one was made
      fm.close();
    }

  }

}
