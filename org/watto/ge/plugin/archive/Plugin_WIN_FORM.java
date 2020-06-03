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
import org.watto.datatype.Archive;
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
public class Plugin_WIN_FORM extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_WIN_FORM() {

    super("WIN_FORM", "WIN_FORM");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("!Dead Pixels Adventure!",
        "10 Second Ninja X",
        "12 is Better Than 6",
        "Ace of Protectors",
        "All Guns On Deck",
        "ANIMALITY",
        "Anime Fight in the Arena of Death",
        "Anykey Simulator",
        "Battle for Enlor",
        "Big Journey to Home",
        "Call of Bitcoin",
        "Chowderchu",
        "Clickdraw Clicker",
        "Colonumbers",
        "Crab Dub",
        "Daddy's Gone A-Hunting",
        "Daily Run",
        "Dead Dust",
        "Dear Leader",
        "Drill Arena",
        "Endorlight",
        "Epic PVP Castles",
        "Fran Bow",
        "Galactic Lords",
        "Galaxia Conquestum",
        "God Vs Zombies",
        "Golden Dungeons",
        "Half Past Impossible",
        "Heavy Dreams",
        "HellCat",
        "Hurricane Ship Ghost",
        "Keatz: The Lonely Bird",
        "Knight Club",
        "Laggerjack",
        "Laserium",
        "Learn to Drive on Moto Wars",
        "Memoranda",
        "Monster Slayers",
        "Ninja from Hell vs. Reptiloids",
        "No Turning Back: The Pixel Art Action-Adventure Roguelike",
        "NOGIBATOR",
        "Othello 2018",
        "Outer Space",
        "Outrunner 2",
        "PHAT STACKS",
        "Pixel Gladiator",
        "PlatONIR",
        "Ranger in Spider's Den",
        "Robot Chase",
        "Ruthless Safari",
        "See No Evil",
        "SEGFAULT",
        "Shake Your Money Simulator 2016",
        "Skyraine",
        "Snail Racer EXTREME",
        "Snake Eyes Dungeon",
        "Spoiler Alert",
        "Starship Annihilator",
        "Stealth Bastard Deluxe",
        "Super Duper Flying Genocide 2017",
        "Super GunWorld 2",
        "The Big Elk",
        "The Friends of Ringo Ishikawa",
        "The Mutton Horn: Jump Jump!",
        "The Mystery of Devils House",
        "The Orb Chambers 2",
        "The President",
        "The Rare Nine",
        "The Story Goes On",
        "Torture Chamber",
        "Trolley Gold",
        "unBorn",
        "Vicky Saves the Big Dumb World",
        "Violent Vectors",
        "Void Source",
        "Volstead",
        "Vzerthos: Heir of Thunder",
        "Win The Game: Do It!",
        "Wuppo",
        "Your Car Shooter",
        "Z55Z");
    setExtensions("win"); // MUST BE LOWER CASE
    setPlatforms("PC");

    // MUST BE LOWER CASE !!!
    setFileTypes(new FileType("txtr", "Texture Archive", FileType.TYPE_ARCHIVE),
        new FileType("audo", "Audio Archive", FileType.TYPE_ARCHIVE));

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
      if (fm.readString(4).equals("FORM")) {
        rating += 50;
      }

      long arcSize = fm.getLength();

      // Archive Size
      if (FieldValidator.checkEquals(fm.readInt() + 8, arcSize)) {
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

      // 4 - Header (FORM)
      // 4 - Archive Length [+8]
      fm.skip(8);

      int numFiles = Archive.getMaxFiles();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(arcSize);

      // Loop through directory
      int realNumFiles = 0;
      while (fm.getOffset() < arcSize) {
        // 4 - Header/File Type
        String fileType = fm.readString(4);

        // 4 - File Length
        int length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // X - File Data
        long offset = fm.getOffset();
        fm.skip(length);

        String filename = Resource.generateFilename(realNumFiles) + "." + fileType;

        //path,name,offset,length,decompLength,exporter
        resources[realNumFiles] = new Resource(path, filename, offset, length);

        TaskProgressManager.setValue(offset);
        realNumFiles++;
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
