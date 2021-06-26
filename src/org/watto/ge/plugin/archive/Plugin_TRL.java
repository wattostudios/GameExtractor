
package org.watto.ge.plugin.archive;

import java.io.File;
import org.watto.component.WSPluginException;
import org.watto.task.TaskProgressManager;
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
import org.watto.ge.plugin.PluginAssistant_TRL_DirEntry;
import org.watto.io.FileManipulator;

/**
 **********************************************************************************************
 *
 * =====
 *
 * This is a very difficult and complicated plugin that attempts to read the Microsoft Compound
 * Document File Format. It does work, from what I can tell, however the short sector table might
 * not be working correctly (thus all short files have incorrect offsets etc.)
 *
 * =====
 **********************************************************************************************
 *
 **/
public class Plugin_TRL extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_TRL() {

    super("TRL", "TRL");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Learn To Speak French");
    setExtensions("trl", "lts");
    setPlatforms("PC");

  }

  /**
   **********************************************************************************************
   * Renames the files under this branch
   **********************************************************************************************
   **/
  public void expandBranch(PluginAssistant_TRL_DirEntry[] dirEntry, int curDID, String dirName) {
    if (curDID == -1) {
      ////System.out.println("Ignoring DID " + curDID);
      return;
    }

    PluginAssistant_TRL_DirEntry curDirEntry = dirEntry[curDID];
    ////System.out.println("filename - " + curDirEntry.getFilename());
    String newDirName = dirName + curDirEntry.getFilename();
    curDirEntry.setFilename(newDirName);

    expandBranch(dirEntry, curDirEntry.getLeftNode(), newDirName);
    expandBranch(dirEntry, curDirEntry.getRightNode(), newDirName);
    //expandBranch(dirEntry,curDirEntry.getRootNode(),"");

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

      /*
       *
       * // Header if (fm.readString(4).equals("")){ rating += 50; }
       *
       * long arcSize = fm.getLength();
       *
       * // Archive Size if (fm.readInt() == arcSize){ rating += 5; }
       *
       * // Number Of Files if (FieldValidator.checkNumFiles(fm.readInt())){ rating += 5; }
       *
       */

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
  @SuppressWarnings("unused")
  public Resource[] read(File path) {
    try {

      // NOTE - Compressed files MUST know their DECOMPRESSED LENGTH
      //      - Uncompressed files MUST know their LENGTH

      addFileTypes();

      // RESETTING THE GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = (int) fm.getLength();

      // 8 - Header (208,207,17,224,161,177,26,225)
      // 16 - Unique File ID (null)
      // 2 - Revision Number (62)
      // 2 - Version Number (3)
      // 2 - Byte Order Mark ((byte)(254,255) = little endian)
      fm.skip(30);

      // 2 - Sector Size (2^value) (9=512)
      int sectorSize = (int) Math.pow(2, fm.readShort());

      // 2 - Short Sector Size (2^value) (6=64)
      int shortSectorSize = (int) Math.pow(2, fm.readShort());

      // 10 - null
      fm.skip(10);

      // 4 - Number Of Sectors in the sector allocation table (256)
      int numSectors = fm.readInt();
      ////System.out.println("numSectors - " + numSectors);

      // 4 - SID of the first sector in the directory stream (1)
      int firstSectorSID = fm.readInt();
      ////System.out.println("firstSectorSID - " + firstSectorSID);

      // 4 - null
      fm.skip(4);

      // 4 - Minimum size of a standard stream (4096) (streams smaller are short-streams)
      int streamSize = fm.readInt();

      // 4 - SID of the first sector of the short-sector allocation table (200)
      int firstShortSectorSID = fm.readInt();
      ////System.out.println("firstShortSectorSID - " + firstShortSectorSID);

      // 4 - Number Of Sectors in the short-sector allocation table (37)
      int numShortSectors = fm.readInt();
      ////System.out.println("numShortSectors - " + numShortSectors);

      // 4 - SID of the first sector of the master allocation table
      int firstMasterSectorSID = fm.readInt();
      ////System.out.println("firstMasterSectorSID - " + firstMasterSectorSID);

      // 4 - Number Of Sectors in the master allocation table (2)
      int numMasterSectors = fm.readInt();
      ////System.out.println("numMasterSectors - " + numMasterSectors);

      int maxNumMasterSIDs = numMasterSectors * (sectorSize / 4) + 109;
      int maxNumSectorSIDs = numSectors * (sectorSize / 4);
      int maxNumShortSectorSIDs = numShortSectors * (sectorSize / 4);

      ////System.out.println("maxNumMasterSIDs - " + maxNumMasterSIDs);
      int[] masterSIDs = new int[maxNumMasterSIDs];

      // read initial Master Sector Table in the Header
      ////System.out.println("============READING MASTER TABLES============");
      int numToRead = 109;
      if (numSectors < 109) {
        numToRead = maxNumMasterSIDs;
      }

      ////System.out.println("numToRead - " + numToRead);

      for (int i = 0; i < numToRead; i++) {
        // 4 - SID
        masterSIDs[i] = fm.readInt();
      }

      int numMasterSIDs = numToRead;

      int numRemaining = maxNumMasterSIDs - 109;
      if (numRemaining > 0) {
        // jump to the next sector and read more SIDs
        int firstMasterSectorSIDOffset = 512 + (firstMasterSectorSID * sectorSize);

        fm.seek(firstMasterSectorSIDOffset);

        int insertIndex = 109;
        while (numRemaining > 0) {

          ////System.out.println("numRemaining - " + numRemaining);

          numToRead = sectorSize / 4 - 1;
          if (numRemaining < numToRead) {
            numToRead = numRemaining;
          }

          for (int i = 0; i < numToRead; i++) {
            // 4 - SID
            masterSIDs[insertIndex] = fm.readInt();
            numMasterSIDs++;
            insertIndex++;
          }

          // 4 - Next Master Sector SID
          int nextMasterSectorID = 512 + (fm.readInt() * sectorSize);
          fm.seek(nextMasterSectorID);
          numRemaining -= (sectorSize / 4);
        }

      }

      ////System.out.println("numMasterSIDs - " + numMasterSIDs);

      // ** By now, we should have all the SIDs from the Master Table!

      // Now we read the MSAT and generate the SAT table
      ////System.out.println("============READING SAT TABLES============");
      int[] sectorSIDs = new int[maxNumSectorSIDs];
      int insertIndex = 0;
      for (int i = 0; i < numMasterSIDs; i++) {
        // read through each of the MSAT IDs 1 at a time, go to the appropriate offset, and read all entries
        int curMasterSID = masterSIDs[i];

        if (curMasterSID == -1) {
          // skip
        }
        else {
          int SATOffset = 512 + (curMasterSID * sectorSize);
          ////System.out.println("SATOffset - " + SATOffset);
          fm.seek(SATOffset);

          for (int j = 0; j < sectorSize / 4; j++) {
            // 4 - Sector SID
            sectorSIDs[insertIndex] = fm.readInt();
            insertIndex++;
          }
        }
      }

      // Now we read the MSAT and generate the Short-SAT table
      ////System.out.println("============READING SHORT-SAT TABLES============");

      int[] shortSectorSIDs = new int[maxNumShortSectorSIDs];
      insertIndex = 0;

      int nextShortSectorSID = firstShortSectorSID;
      while (nextShortSectorSID != -2 && nextShortSectorSID != -1) {
        // go to the sector
        int shortSATOffset = 512 + (nextShortSectorSID * sectorSize);
        ////System.out.println("shortSATOffset - " + shortSATOffset);
        fm.seek(shortSATOffset);

        for (int j = 0; j < sectorSize / 4; j++) {
          // 4 - Short Sector SID
          shortSectorSIDs[insertIndex] = fm.readInt();
          insertIndex++;
        }

        nextShortSectorSID = sectorSIDs[nextShortSectorSID];
      }

      // Now we want to read the Directory Sector Table
      // First, determine the sectors used for the directory...
      int[] dirSectors = new int[maxNumMasterSIDs];
      int numDirSectors = 0;

      ////System.out.println("============DETERMINING DIRECTORY SECTORS============");
      int nextDirSectorSID = firstSectorSID;
      while (nextDirSectorSID != -2 && nextDirSectorSID != -1) {
        ////System.out.println("nextDirSectorSID - " + nextDirSectorSID);
        dirSectors[numDirSectors] = nextDirSectorSID;
        //nextDirSectorSID = masterSIDs[nextDirSectorSID];
        nextDirSectorSID = sectorSIDs[nextDirSectorSID];
        numDirSectors++;
        ////System.out.println("==");
      }

      // Now read the directory
      int numEntriesPerSector = sectorSize / 128;
      int maxNumEntries = numDirSectors * numEntriesPerSector;

      ////System.out.println("maxNumEntries - " + maxNumEntries);

      PluginAssistant_TRL_DirEntry[] dirEntry = new PluginAssistant_TRL_DirEntry[maxNumEntries];
      int numDirEntries = 0;

      ////System.out.println("============READING DIRECTORY ENTRIES============");

      // go to each dir sector and read the entries
      for (int i = 0; i < numDirSectors; i++) {
        // go to the dir sector
        fm.seek(512 + (dirSectors[i] * sectorSize));

        // read all the entries in this sector
        for (int j = 0; j < numEntriesPerSector; j++) {
          // 64 - Filename (null-terminated) (unicode text)
          String filename = fm.readUnicodeString(32);

          // 2 - Filename Length (including null)
          int filenameLength = (fm.readShort() / 2) - 1;
          if (filenameLength <= 0) {
            filename = "";
          }
          else {
            filename = filename.substring(0, filenameLength);
          }
          ////System.out.println("filename - " + filename);

          // 1 - Entry Type (0=Empty, 1=Directory, 2=File, 5=Root Directory)
          int entryType = fm.readByte();
          ////System.out.println("entryType - " + entryType);

          // 1 - Node Color (0=Red, 1=Black)
          fm.skip(1);

          // 4 - DID of the left child node (-1 for no node)
          int leftNodeDID = fm.readInt();
          ////System.out.println("leftNode - " + leftNodeDID);

          // 4 - DID of the right child node (-1 for no node)
          int rightNodeDID = fm.readInt();
          ////System.out.println("rightNode - " + rightNodeDID);

          // 4 - DID of the root child node (-1 for no node)
          int rootNodeDID = fm.readInt();
          ////System.out.println("rootNode - " + rootNodeDID);

          // 16 - Unique ID (can be null)
          // 4 - Flags
          // 8 - Creation Timestamp
          // 8 - Modification Timestamp
          fm.skip(36);

          // 4 - SID of the first or short sector (File), SID of the first sector in short-stream (Root)
          int fileSID = fm.readInt();
          ////System.out.println("fileSID - " + fileSID);

          // 4 - File Length (File), Size of the short-stream container (Root)
          long length = fm.readInt();
          FieldValidator.checkLength(length, arcSize);
          ////System.out.println("length - " + length);

          // 4 - null
          fm.skip(4);

          dirEntry[numDirEntries] = new PluginAssistant_TRL_DirEntry(filename, entryType, leftNodeDID, rightNodeDID, rootNodeDID, fileSID, length);
          numDirEntries++;
          ////System.out.println("==");
        }
      }

      ////System.out.println("============NAVIGATING THE DIRECTORY TREE============");

      // Now construct the Red-Black tree using the DID information
      PluginAssistant_TRL_DirEntry curDirEntry = dirEntry[0];
      if (curDirEntry.getEntryType() != 5) {
        // Error
        throw new WSPluginException("Expected the root node!");
      }
      else {
        expandBranch(dirEntry, curDirEntry.getLeftNode(), "");
        expandBranch(dirEntry, curDirEntry.getRightNode(), "");
        //expandBranch(dirEntry,curDirEntry.getRootNode(),"");
      }

      int numFiles = numDirEntries;
      int realNumFiles = 0;

      Resource[] resources = new Resource[numFiles];

      TaskProgressManager.setMaximum(numFiles);

      ////System.out.println("============FINDING THE FILES============");
      // Loop through directory
      for (int i = 0; i < numFiles; i++) {
        curDirEntry = dirEntry[i];

        if (curDirEntry.getEntryType() == 2) {
          // File

          String filename = curDirEntry.getFilename();

          // determine the offsets for this file
          long length = curDirEntry.getLength();
          if (length <= streamSize) {
            // short stream - check this against the specs!!!
            //long offset = 512 + (curDirEntry.getFileSID()*sectorSize);
            //long offset = 512 + (shortSectorSIDs[curDirEntry.getFileSID()]*sectorSize);

            ////System.out.println("SHORT STREAM");
            ////System.out.println("  filename - " + filename);
            ////System.out.println("  FileSID - " + curDirEntry.getFileSID());
            //////System.out.println("  Short Sector SID - " + shortSectorSIDs[curDirEntry.getFileSID()]);
            //////System.out.println("  offset - " + offset);
            ////System.out.println("  length - " + length);

            int numStreams = (int) (length / sectorSize);
            if (length % sectorSize > 0) {
              numStreams++;
            }

            long[] lengths = new long[numStreams];
            long[] offsets = new long[numStreams];

            int nextFileSID = curDirEntry.getFileSID();
            for (int j = 0; j < numStreams; j++) {
              ////System.out.println("  nextFileSID - " + nextFileSID);
              // navigate through the SIDs for this file
              offsets[j] = 512 + (nextFileSID * sectorSize);
              lengths[j] = sectorSize;

              ////System.out.println("  offset " + j + " - " + offsets[j]);
              ////System.out.println("  length " + j + " - " + lengths[j]);

              if (j != numStreams - 1) {
                //nextFileSID = masterSIDs[nextFileSID];
                nextFileSID = shortSectorSIDs[nextFileSID];
              }
              ////System.out.println("  ==");
            }

            if (length % sectorSize > 0) {
              // setting the length of the last piece
              lengths[numStreams - 1] = length % sectorSize;
            }

            //////System.out.println("  Test Range - " + masterSIDs[curDirEntry.getFileSID()]);

            //path,id,name,offset,length,decompLength,exporter
            resources[realNumFiles] = new SplitChunkResource(path, filename, offsets, lengths);
            realNumFiles++;
            ////System.out.println("==");
          }
          else {
            // long stream
            int numStreams = (int) (length / sectorSize);
            if (length % sectorSize > 0) {
              numStreams++;
            }

            ////System.out.println("LONG STREAM");
            ////System.out.println("  filename - " + filename);
            ////System.out.println("  numStreams - " + numStreams);
            ////System.out.println("  length - " + length);

            long[] lengths = new long[numStreams];
            long[] offsets = new long[numStreams];

            int nextFileSID = curDirEntry.getFileSID();
            for (int j = 0; j < numStreams; j++) {
              ////System.out.println("  nextFileSID - " + nextFileSID);
              // navigate through the SIDs for this file
              offsets[j] = 512 + (nextFileSID * sectorSize);
              lengths[j] = sectorSize;

              ////System.out.println("  offset " + j + " - " + offsets[j]);
              ////System.out.println("  length " + j + " - " + lengths[j]);

              if (j != numStreams - 1) {
                //nextFileSID = masterSIDs[nextFileSID];
                nextFileSID = sectorSIDs[nextFileSID];
              }
              ////System.out.println("  ==");
            }

            if (length % sectorSize > 0) {
              // setting the length of the last piece
              lengths[numStreams - 1] = length % sectorSize;
            }

            //path,id,name,offset,length,decompLength,exporter
            resources[realNumFiles] = new SplitChunkResource(path, filename, offsets, lengths);
            realNumFiles++;
            ////System.out.println("==");
          }
        }
        else {
          // Directory - skip cause already dealt with
        }

        TaskProgressManager.setValue(i);

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

}
