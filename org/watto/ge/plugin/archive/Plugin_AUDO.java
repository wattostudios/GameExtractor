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
import org.watto.io.FileManipulator;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_AUDO extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_AUDO() {

    super("AUDO", "AUDO");

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
        "Aviation Hurricane Storm",
        "Battle for Enlor",
        "Big Journey to Home",
        "Call of Bitcoin",
        "Chowderchu",
        "Crab Dub",
        "Daddy's Gone A-Hunting",
        "Dead Dust",
        "Dear Leader",
        "Detective Case and the Clown Bot: Murder In the Hotel Lisbon",
        "Digital Resistance",
        "Drill Arena",
        "Dungeon Souls",
        "Endorlight",
        "Epic PVP Castles",
        "Fran Bow",
        "Fruit Pop II",
        "Galactic Lords",
        "Galaxia Conquestum",
        "Golden Dungeons",
        "HackyZack",
        "Half Past Impossible",
        "Heavy Dreams",
        "Hurricane Ship Ghost",
        "Keatz: The Lonely Bird",
        "Laggerjack",
        "Laserium",
        "Learn to Drive on Moto Wars",
        "Love",
        "Minit",
        "Monster Slayers",
        "Ninja from Hell vs. Reptiloids",
        "No Turning Back: The Pixel Art Action-Adventure Roguelike",
        "Nogibator",
        "Outer Space",
        "Outrunner 2",
        "Overture",
        "PHAT STACKS",
        "Pixel Gladiator",
        "PlatONIR",
        "Ranger in Spider's Den",
        "Rising Lords",
        "Robot Chase",
        "Ruthless Safari",
        "See No Evil",
        "SEGFAULT",
        "Shake Your Money Simulator 2016 ",
        "Skyraine",
        "Snail Racer EXTREME",
        "Snake Eyes Dungeon",
        "Sniper Tanks",
        "Spoiler Alert",
        "Squidlit",
        "Stealth Bastard Deluxe",
        "Super GunWorld 2 ",
        "Teenager vs. Tropical Mutants",
        "The Big Elk",
        "The Friends of Ringo Ishikawa",
        "The Mutton Horn: Jump Jump! ",
        "The Mystery of Devils House",
        "The Orb Chambers 2",
        "The President",
        "The Rare Nine",
        "The Story Goes On",
        "Tormentor X Punisher",
        "Torture Chamber",
        "Trolley Gold",
        "unBorn",
        "Vicky Saves the Big Dumb World",
        "Violent Vectors",
        "Volstead",
        "Vzerthos: Heir of Thunder",
        "Win The Game: Do It! ",
        "Wuppo",
        "Your Car Shooter",
        "Z55Z");
    setExtensions("audo"); // MUST BE LOWER CASE
    setPlatforms("PC");

    // MUST BE LOWER CASE !!!
    //setFileTypes(new FileType("txt", "Text Document", FileType.TYPE_DOCUMENT),
    //             new FileType("bmp", "Bitmap Image", FileType.TYPE_IMAGE)
    //             );

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
  Extracts a STRG resource and then gets the Strings from it
  **********************************************************************************************
  **/
  public String[] extractStrings(File path, String prefix) {
    /*
    try {
      // try to find the STRG resource that matches this one
      String directory = FilenameSplitter.getDirectory(path);
      String filenameOnly = FilenameSplitter.getFilename(path);
    
      File strgFile = new File(directory + File.separatorChar + filenameOnly + ".STRG");
      if (!strgFile.exists()) {
        // no matching STRG file found
        return null;
      }
    
      Resource strgResource = new Resource(strgFile, 0, strgFile.length());
    
      ByteBuffer buffer = new ByteBuffer((int) strgResource.getLength());
      FileManipulator fm = new FileManipulator(buffer);
      strgResource.extract(fm);
    
      fm.seek(0); // back to the beginning of the byte array
    
      // 4 - Number of Strings
      int numStrings = fm.readInt();
      FieldValidator.checkNumFiles(numStrings / 4);
    
      // numStrings*4 - Unknown
      //fm.skip(numStrings * 4);
      int[] ids = new int[numStrings];
      byte[][] idBytes = new byte[numStrings][4];
      for (int i = 0; i < numStrings; i++) {
    
        byte[] bytes = fm.readBytes(4);
        ids[i] = IntConverter.convertLittle(bytes);
        idBytes[i] = bytes;
      }
    
      String[] strings = new String[numStrings];
      int numMatches = 0;
      for (int i = 0; i < numStrings; i++) {
        // 4 - String Length (not including the terminator)
        int nameLength = fm.readInt();
        FieldValidator.checkLength(nameLength);
    
        // X - String
        String name = fm.readString(nameLength);
        if (name.startsWith(prefix) && name.indexOf('.') > 0) {
          strings[numMatches] = name;
          numMatches++;
          System.out.println(idBytes[i][0] + "\t" + idBytes[i][1] + "\t" + idBytes[i][2] + "\t" + idBytes[i][3] + "\t" + i + "\t" + name);
          //System.out.println(ids[i] + "\t" + name);
        }
    
        // 1 - null String Terminator
        fm.skip(1);
      }
    
      String[] oldStrings = strings;
      strings = new String[numMatches];
      System.arraycopy(oldStrings, 0, strings, 0, numMatches);
    
      fm.close();
    
      return strings;
    }
    catch (Throwable t) {
      logError(t);
      return null;
    }
    */
    return null;
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

      FileManipulator fm = new FileManipulator(path, false, 4); // small quick reads

      long arcSize = fm.getLength();

      // 4 - Number Of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // numFiles*4 - Unknown
      fm.skip(numFiles * 4);
      /*
      int[] ids = new int[numFiles];
      for (int i = 0; i < numFiles; i++) {
        //ids[i] = fm.readInt();
        byte[] bytes = fm.readBytes(4);
        ids[i] = IntConverter.convertLittle(bytes);
        System.out.println(bytes[0] + "\t" + bytes[1] + "\t" + bytes[2] + "\t" + bytes[3]);
      }
      */

      String[] names = null;
      int numNames = 0;

      names = extractStrings(path, "snd");
      if (names != null) {
        numNames = names.length;
      }

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {

        // 4 - File Length
        int length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // X - File Data
        long offset = fm.getOffset();
        fm.skip(length);

        // 0-3 - null Padding to a multiple of 4 bytes
        fm.skip(calculatePadding(length, 4));

        String filename = Resource.generateFilename(i);
        if (names != null && i < numNames) {
          filename = names[i];
        }

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

    /*
    if (headerInt1 == 2037149520) {
      return "js";
    }
    */

    return null;
  }

}
