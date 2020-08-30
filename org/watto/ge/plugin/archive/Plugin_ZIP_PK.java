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
import java.io.FileOutputStream;
import java.text.DateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import org.watto.ErrorLogger;
import org.watto.Language;
import org.watto.component.WSPluginException;
import org.watto.component.WSTableColumn;
import org.watto.datatype.Archive;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.exporter.Exporter_Deflate;
import org.watto.ge.plugin.exporter.Exporter_ZIP;
import org.watto.ge.plugin.resource.Resource_ZIP_PK;
import org.watto.io.FileManipulator;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_ZIP_PK extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_ZIP_PK() {

    super("ZIP_PK", "ZIP Archive");

    //         read write replace rename
    setProperties(true, true, true, true);

    setGames("Generic Zip Archive",
        "18 Wheels Of Steel: Across America",
        "18 Wheels Of Steel: Convoy",
        "18 Wheels Of Steel: Haulin",
        "18 Wheels Of Steel: Pedal To The Metal",
        "Achievement Collector: Cat",
        "Advanced Strategic Command",
        "Against Rome",
        "Age of Conquest 3",
        "Alpha Black Zero",
        "American History Lux",
        "American McGee's Alice",
        "Ankh: Heart Of Osiris",
        "Arena Wars",
        "Audiosurf",
        "AuroraRL",
        "Baikonur Space",
        "Battlefield 2",
        "Battlefield 2142",
        "Blitzkrieg 2",
        "Blitzkrieg",
        "Blocktality",
        "Braid",
        "Brain Sanity",
        "Broken Sword 2.5",
        "Brothers Pilots 4",
        "Buzz! The Great Music Quiz",
        "Call of Duty 2",
        "Call of Duty 4: Modern Warfare",
        "Call of Duty",
        "Call of Juarez",
        "Carnivores: Cityscape",
        "Celestial Impact",
        "Cellblock Squadrons",
        "Ceville",
        "Chrome SpecForce",
        "Chrono Rage",
        "Citizen Abel: Gravity Bone",
        "Civilization: Call To Power",
        "Code of Honor: The French Foreign Legion",
        "Crime Life: Gang Wars",
        "Crysis",
        "Dark Sector",
        "Darkstar One",
        "Deadly Dozen 2",
        "Deadly Dozen",
        "Deer Drive",
        "Defiance",
        "Desperados: Wanted Dead or Alive",
        "Dethkarz",
        "Dirty Little Helper '98",
        "Doom 2",
        "Doom 3",
        "Doomsday",
        "Dracula Twins",
        "Duke Nukem Manhatten Project",
        "Dungeons",
        "El Airplane",
        "Empire Earth 2",
        "Empire Earth 3",
        "Enemy Territory: Quake Wars",
        "Euro Truck Simulator",
        "Faces Of War",
        "Falcon 4",
        "Fallout Tactics",
        "Far Cry",
        "Fergus The Fly",
        "Fire Starter",
        "Freedom Fighters",
        "Freedom Force vs The 3rd Reich",
        "Freelancer",
        "Frontline: Fields of Thunder",
        "Galactic Command: Echo Squad",
        "Gardener The Ripper",
        "GP500",
        "Granado Espada",
        "Great Battles of WWII - Stalingrad",
        "Guinea-Pig",
        "Hard Truck: 18 Wheels Of Steel",
        "Heavy Metal FAKK 2",
        "Hellhog XP",
        "Hello Pollution",
        "Heroes of Might and Magic 5",
        "Heroes of Newerth",
        "Hexus",
        "Hitman 2: Silent Assasin",
        "Hitman: Blood Money",
        "Hitman: Codename 47",
        "Hitman: Contracts",
        "Hot Rod American Street Drag",
        "House Of The Dead 3",
        "Hoyle Board Games 2005",
        "Hoyle Card Games 2005",
        "Hunting Unlimited 3",
        "I Was An Atomic Mutant",
        "Imperial Glory",
        "Indecision",
        "Iron Grip: Warlord",
        "Itch",
        "Jack Keane",
        "Jane Angel: Templar Mystery",
        "Jedi Academy",
        "Jedi Knight",
        "Jedi Knight: Mysteries of The Sith",
        "Jedi Outcast",
        "Keepsake",
        "Kings Bounty: The Legend",
        "Kitty Run",
        "Kong",
        "Law And Order 3: Justice Is Served",
        "League of Legends",
        "Line Of Sight: Vietnam",
        "Lionheart: Legacy Of The Crusader",
        "Master of Orion 3",
        "Maximus XV",
        "MDK2",
        "Medal Of Honor: Allied Assult",
        "Men Of War",
        "Men of War: Assault Squad",
        "Metal Gear Solid",
        "Microsoft Flight Simulator 2004",
        "Minions Of Mirth",
        "Monte Christo",
        "Moonshine Runners",
        "MotorM4X",
        "Neighbours From Hell 2",
        "Neighbours From Hell",
        "Nexuiz",
        "OneScreen Solar Sails",
        "Open Arena",
        "Outfront",
        "Packmania 2",
        "Paradise Cracked",
        "Perimeter",
        "Pirates: Battle For The Caribbean",
        "Praetorians",
        "Prey",
        "PURE",
        "Pusher",
        "Puzzle Kingdoms",
        "Quake 3",
        "Quake 4",
        "Red Ocean",
        "Reflexive Arcade",
        "Return To Castle Wolfenstein",
        "Revenant",
        "Richard Burns Rally",
        "Ricochet Xtreme",
        "Ricochet",
        "RIP 3: The Last Hero",
        "S.W.A.T 3",
        "Sabotain",
        "Sacred 2",
        "Savage 2: A Tortured Soul",
        "Savage",
        "Serious Sam 2",
        "Serious Sam",
        "Shadow Warrior",
        "Shadowbane: Throne Of Oblivion",
        "Shark: Hunting The Great White",
        "Singles: Flirt Up Your Life",
        "Slave Zero",
        "Smokin' Guns",
        "Soldier Of Fortune 2",
        "Soldiers Of Anarchy",
        "Soldiers: Heroes Of World War 2",
        "Space Exploration",
        "Space Girls",
        "Space Trader",
        "Star Trek: Elite Force 2",
        "Star Wolves",
        "Starcon 2",
        "StepMania",
        "Stranger",
        "Sudden Strike 3",
        "Supreme Commander",
        "Swarm",
        "Sword Of The Stars",
        "System Shock 2",
        "Team Factor",
        "Terminator 3",
        "Terrorist Takedown: Covert Operations",
        "Test Drive 5",
        "Test Drive 6",
        "Test Drive Off-Road 3",
        "The Age of Decadence",
        "The Chosen",
        "The Chronicles of Emerland Solitaire",
        "The Fall: Last Days Of Gaia",
        "The Talos Principle",
        "The Witness",
        "Thief 2: The Metal Age",
        "Thief: The Dark Age",
        "Tom Clancy's H.A.W.X",
        "ToolsMedia",
        "Torchlight",
        "TrackMania Nations Forever",
        "TrackMania Next",
        "Transport Tycoon",
        "Tremulous",
        "Tribes 2",
        "True Combat: Elite",
        "UFO: Alien Invasion",
        "Universal Combat: A World Apart",
        "Uplink",
        "Urban Brawl: Action Doom 2",
        "Urban Terror",
        "Urquan Masters",
        "Vampire: The Masquerade",
        "VoidExpanse",
        "Warsow",
        "Warzone 2100",
        "Winamp",
        "Windows Media Player",
        "Wolfenstein: Enemy Territory",
        "World Of Padman",
        "World War Z",
        "Worms Crazy Golf",
        "X-Men Legends 2",
        "XPand Rally",
        "XS Mark",
        "Zoo Tycoon 2: Endangered Species",
        "Zoo Tycoon");
    setExtensions("zip", "pk3", "pk4", "zipfs", "crf", "ztd");
    setPlatforms("PC", "Android");

  }

  /**
   **********************************************************************************************
   * Gets a blank resource of this type, for use when adding resources
   **********************************************************************************************
   **/
  @Override
  public Resource getBlankResource(File file, String name) {
    return new Resource_ZIP_PK(file, name);
  }

  /**
   **********************************************************************************************
   * Gets all the columns
   **********************************************************************************************
   **/
  @Override
  public WSTableColumn[] getColumns() {

    // used codes: a,c,C,d,D,E,F,i,I,N,O,P,r,R,S,z,Z
    //                                          code,languageCode,class,editable,sortable
    WSTableColumn crcColumn = new WSTableColumn("CRC", 'X', String.class, false, true);
    WSTableColumn timeColumn = new WSTableColumn("Time", 'T', String.class, false, true);

    return getDefaultColumnsWithAppended(crcColumn, timeColumn);
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public Object getColumnValue(Resource res, char code) {
    if (res instanceof Resource_ZIP_PK) {
      Resource_ZIP_PK resource = (Resource_ZIP_PK) res;

      if (code == 'X') {
        return Long.toHexString(resource.getCRC());
      }
      else if (code == 'T') {
        return DateFormat.getInstance().format(new Date(resource.getTime()));
      }
    }

    return super.getColumnValue(res, code);
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
      if (fm.readString(2).equals("PK")) {
        rating += 50;
      }
      else {
        return 0;
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
  @SuppressWarnings("rawtypes")
  @Override
  public Resource[] read(File path) {
    try {

      ExporterPlugin exporter = Exporter_ZIP.getInstance();
      addFileTypes();

      ZipFile zipArchive = new ZipFile(path);

      int numFiles = zipArchive.size();

      Resource[] resources = new Resource[numFiles];

      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      int i = 0;
      Enumeration zipdir = zipArchive.entries();
      while (zipdir.hasMoreElements()) {
        ZipEntry zippedFile = (ZipEntry) zipdir.nextElement();
        if (!zippedFile.isDirectory()) {

          String filename = zippedFile.getName();
          long length = zippedFile.getCompressedSize();
          long decompLength = zippedFile.getSize();
          long crc = zippedFile.getCrc();
          long time = zippedFile.getTime();

          //path,id,name,offset,length,decompLength,exporter
          resources[i] = new Resource_ZIP_PK(path, filename, 0, length, decompLength, exporter, crc, time);

          TaskProgressManager.setValue(i);
          i++;
        }
      }

      resources = resizeResources(resources, i);

      zipArchive.close();

      return resources;

    }
    catch (Throwable t) {
      logError(t);
      return readManually(path);
    }
  }

  /**
  **********************************************************************************************
  Try reading the ZIP file manually, to work around some MALFORMED errors and so forth
  **********************************************************************************************
  **/
  public Resource[] readManually(File path) {
    try {

      // NOTE - Compressed files MUST know their DECOMPRESSED LENGTH
      //      - Uncompressed files MUST know their LENGTH

      addFileTypes();

      ExporterPlugin exporter = Exporter_Deflate.getInstance();

      // RESETTING GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      int numFiles = Archive.getMaxFiles();
      Resource[] resources = new Resource[numFiles];

      TaskProgressManager.setMaximum(arcSize);

      int realNumFiles = 0;
      while (fm.getOffset() < arcSize) {
        // 2 - Header (PK)
        fm.skip(2);

        // 4 - Entry Type (1311747 = File Entry)
        int entryType = fm.readInt();
        if (entryType == 1311747) {
          // File Entry

          // 2 - Unknown (2)
          fm.skip(2);

          // 2 - Compression Method
          short compType = fm.readShort();

          // 8 - Checksum?
          fm.skip(8);

          // 4 - Compressed File Size
          int length = fm.readInt();
          FieldValidator.checkLength(length, arcSize);

          // 4 - Decompressed File Size
          int decompLength = fm.readInt();
          FieldValidator.checkLength(decompLength);

          // 2 - Filename Length
          int filenameLength = fm.readShort();
          FieldValidator.checkFilenameLength(filenameLength);

          // 2 - Extra Data Length
          int extraLength = fm.readShort();
          FieldValidator.checkLength(extraLength, arcSize);

          // X - Filename
          String filename = fm.readString(filenameLength);

          // X - Extra Data
          fm.skip(extraLength);

          // X - File Data
          long offset = fm.getOffset();
          fm.skip(length);

          if (compType == 0) {
            // uncompressed

            //path,name,offset,length,decompLength,exporter
            resources[realNumFiles] = new Resource(path, filename, offset, length);
          }
          else {
            // compressed - probably Deflate

            //path,name,offset,length,decompLength,exporter
            resources[realNumFiles] = new Resource(path, filename, offset, length, decompLength, exporter);
          }
          realNumFiles++;

          TaskProgressManager.setValue(offset);

        }
        else if (entryType == 513) {
          // Directory Entry

          // 2 - Unknown (20)
          // 2 - Unknown (2)
          // 2 - Unknown (8)
          // 8 - Checksum?
          // 4 - Compressed File Size
          // 4 - Decompressed File Size
          fm.skip(22);

          // 4 - Filename Length
          int filenameLength = fm.readShort();
          fm.skip(2);
          FieldValidator.checkFilenameLength(filenameLength);

          // 10 - null
          // 4 - File Offset (points to PK for this file in the directory)
          fm.skip(14);

          // X - Filename
          fm.skip(filenameLength);

        }
        else if (entryType == 656387) {
          // Directory Entry (Short) (or sometimes a file)

          // 2 - Unknown (20)
          fm.skip(2);

          // 2 - Unknown (2)
          short compType = fm.readShort();

          // 8 - Checksum?
          fm.skip(8);

          // 4 - Compressed File Size
          int length = fm.readInt();
          FieldValidator.checkLength(length, arcSize);

          // 4 - Decompressed File Size
          int decompLength = fm.readInt();
          FieldValidator.checkLength(decompLength);

          // 4 - Filename Length
          int filenameLength = fm.readShort();
          fm.skip(2);
          FieldValidator.checkFilenameLength(filenameLength);

          // X - Filename
          String filename = fm.readString(filenameLength);

          // X - File Data
          if (length != 0) {
            long offset = fm.getOffset();
            fm.skip(length);

            if (compType == 0) {
              // uncompressed

              //path,name,offset,length,decompLength,exporter
              resources[realNumFiles] = new Resource(path, filename, offset, length);
            }
            else {
              // compressed - probably Deflate

              //path,name,offset,length,decompLength,exporter
              resources[realNumFiles] = new Resource(path, filename, offset, length, decompLength, exporter);
            }
            realNumFiles++;

            TaskProgressManager.setValue(offset);
          }

        }
        else if (entryType == 1541) {
          // EOF Entry

          // 2 - null
          // 8 - Checksum?
          // 4 - Length Of File Data (archive size excluding the directory)
          // 2 - null
          fm.skip(16);
        }
        else {
          // bad header
          String errorMessage = "[ZIP_PK]: Manual read: Unknown entry type " + entryType + " at offset " + (fm.getOffset() - 6);
          if (realNumFiles >= 5) {
            // we found a number of files, so lets just return them, it might be a "prematurely-short" archive.
            ErrorLogger.log(errorMessage);
            break;
          }
          else {
            throw new WSPluginException(errorMessage);
          }
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
  @Override
  public void setColumnValue(Resource res, char code, Object value) {
    try {
      if (res instanceof Resource_ZIP_PK) {
        Resource_ZIP_PK resource = (Resource_ZIP_PK) res;

        if (code == 'X') {
          resource.setCRC(Long.parseLong(value.toString(), 16));
          return;
        }
        else if (code == 'T') {
          resource.setTime(DateFormat.getInstance().parse(value.toString()).getTime());
          return;
        }
      }
    }
    catch (Throwable t) {
    }

    super.setColumnValue(res, code, value);
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public void write(Resource[] resources, File path) {
    try {

      // 2.0 - this doesn't actually work???
      //if (path.exists()){
      //  path = FileBuffer.checkFilename(path);
      //  FileBuffer.makeDirectory(path.getAbsolutePath());
      //  }

      ZipOutputStream outputStream = new ZipOutputStream(new FileOutputStream(path));

      int numFiles = resources.length;
      TaskProgressManager.setMaximum(numFiles);

      TaskProgressManager.setMessage(Language.get("Progress_WritingFiles"));
      for (int i = 0; i < numFiles; i++) {

        ZipEntry cpZipEntry = new ZipEntry(resources[i].getName());

        outputStream.putNextEntry(cpZipEntry);
        resources[i].extract(outputStream);
        outputStream.closeEntry();

        TaskProgressManager.setValue(i);
      }

      outputStream.finish();
      outputStream.close();

    }
    catch (Throwable t) {
      logError(t);
    }
  }

}
