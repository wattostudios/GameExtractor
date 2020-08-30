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
import java.io.FileNotFoundException;
import org.watto.ErrorLogger;
import org.watto.Language;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.helper.Unity3DHelper;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.exporter.Exporter_Custom_FSB5_ProcessWithinArchive;
import org.watto.ge.plugin.exporter.Exporter_LZ4;
import org.watto.ge.plugin.resource.Resource_Unity3D_TEX;
import org.watto.io.FileManipulator;
import org.watto.io.FilenameSplitter;
import org.watto.io.buffer.ByteBuffer;
import org.watto.io.converter.IntConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_ASSETS_17 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_ASSETS_17() {

    super("ASSETS_17", "Unity3D Engine Resource (Version 17)");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("1775: Rebellion",
        "1812: The Invasion of Canada",
        "28 Waves Later",
        "8bit Invasion",
        "9 Balls",
        "A Penny For Some Motivation",
        "A Plot Story",
        "A Roll-Back Story",
        "Achievement Clicker 2019",
        "Achievement Clicker",
        "Achievement Printer Part 1",
        "Adventures of Heroes",
        "AER Memories Of Old",
        "Aftercharge",
        "Again",
        "Air Combat",
        "Akuatica",
        "Alien Invaders",
        "aMAZE 3D",
        "Angel's Love",
        "Anodyne 2",
        "Ape Out",
        "Army of Squirrels",
        "Attack Of Insects",
        "Attrition: Tactical Fronts",
        "Aztez",
        "Bad North",
        "BAE 2",
        "BAE",
        "BARRIER X",
        "BATALJ",
        "Battle Chef Brigade",
        "Beach Restaurant",
        "BeanVR",
        "Beyond the Wall",
        "Bitcoin Clicker",
        "Blacksmith",
        "Blind Mind",
        "Blitzkrieg 3",
        "Block Sport",
        "Blonde Driver",
        "Blood of Old",
        "Blood of Old: The Rise To Greatness",
        "Bloody Boobs",
        "Board Battlefield",
        "Bob the Cube",
        "BoneBone: Rise of the Deathlord",
        "BOOBS BATTLEGROUND",
        "Boobserman",
        "BOXVR",
        "Braveland",
        "Broke Protocol: Online City",
        "Build Wars",
        "Bullet VR",
        "Bunker 58",
        "Bunny Adventure",
        "Burly Men at Sea",
        "Business Clicker",
        "Carcassonne",
        "Caves!",
        "Chaos Reborn",
        "Cherry Kiss Strip Mahjong Solitaire",
        "Chroma Squad",
        "City Builder",
        "Clatter",
        "Click and Manage Tycoon",
        "Cloudborn",
        "Cludbugz's Twisted Magic",
        "Combat Raccoon",
        "Comedy Night",
        "Commands and Colors: The Great War",
        "Construct: Escape The System",
        "Cookies vs Claus",
        "Country Park",
        "Crankies Workshop: Bozzbot Assembly",
        "Crankies Workshop: Grizzbot Assembly 2",
        "Crankies Workshop: Grizzbot Assembly",
        "Crankies Workshop: Lerpbot Assembly",
        "Crankies Workshop: Whirlbot Assembly",
        "Crankies Workshop: Zazzbot Assembly",
        "Crazy Alien",
        "Crazy Oafish Ultra Blocks Big Sale",
        "Crazy Pirate",
        "CrazyCar",
        "Creativerse",
        "Cross And Crush",
        "Cryptochain",
        "Cube Color",
        "Cube Link",
        "Dad's Co-worker",
        "Daddy",
        "Deconstructor",
        "Deep Blue",
        "Deep Ones",
        "Defend The Planet",
        "Demon Robot Runner",
        "Depth Siege Atlantis",
        "Destination Dungeon: Crypts of Warthallow",
        "Destination Dungeons: Catacombs of Dreams",
        "Digit Daze",
        "Digital Dungeon",
        "Disco Zombie Rampage 2",
        "Do Not Feed The Monkeys",
        "Dolphins-Cyborgs And Open Space",
        "Don't Stand Out",
        "Dots",
        "Double Stretch",
        "Douche Bag",
        "Dr. Pills",
        "Dracula's Library",
        "Dragon Hunter",
        "Draw It!",
        "Draw_Love",
        "Drawkanoid",
        "Dumbass Drivers",
        "Dungeon Creepster",
        "Dungeon Escape",
        "Dungeon Rushers",
        "Dungeons Of The Dead",
        "Energy Nodes",
        "Enter The Gungeon",
        "Eon Fleet",
        "Epic Roll",
        "Equalizer",
        "Escape From Here",
        "Euclidean",
        "Everything",
        "Evolo.Evolution",
        "Faeria",
        "Fairy Escape",
        "Falling Words",
        "Far Cnight",
        "FarCraft",
        "First Telegram War",
        "Fitzzle Mighty Bears",
        "Fitzzle Precious Dolphins",
        "Fitzzle Wise Owls",
        "Five Keys to Exit",
        "Five Rooms",
        "Flat Kingdom",
        "Flotus",
        "Fluffy Friends 2",
        "Fluffy Friends",
        "Flux8",
        "Fly The Plane",
        "Food Hunter",
        "For The King",
        "Fortune 499",
        "Freaky Awesome",
        "Freedom Defender",
        "Fruit Soduko 2",
        "Fruit Soduko 3",
        "Galaxy 3D Space Defender",
        "Game of Life",
        "Gangs of Space",
        "Ghost Killer",
        "Ghoulboy",
        "Glass Masquerade",
        "GNOG",
        "Goblins Keep Coming: Tower Defense",
        "Gold Digger Maze",
        "Golden Fever",
        "Golf Extreme",
        "Gone Home",
        "GoNNER",
        "Goodbye My King",
        "Grandpa",
        "Grape Jelly",
        "Grav Blazer",
        "Gravity Light",
        "Gravity Puzzles",
        "Greeng 2D Dungeon",
        "Guess Who?",
        "Guns And Robots",
        "Guns of Icarus Online",
        "Gwent",
        "H.I.S.T.O.R.Y T.O.R.C.H.K.A 2",
        "H0ST",
        "Halcyon 6: Lightspeed Edition",
        "HandyCopter",
        "Hanse: The Hanseatic League",
        "Hardcore Survival",
        "HecatoncheirStory",
        "Hentai 3018",
        "Hentai Strike",
        "Hentai Tentacle Bicycle Race",
        "Heroes Arena",
        "Heroes Of The Offworld Arena",
        "Hidden Folks",
        "Hitch Hiker: First Ride",
        "Hitori",
        "Hoo-Boy",
        "Horace",
        "I Want Cookies",
        "I'm Not A Monster",
        "Icons: Combat Arena",
        "If You Know What I Mean",
        "Immersion",
        "In Game Adventure: Legend Of Monsters",
        "In Memory of TITAN",
        "Inca Blocks",
        "Inca Marbles",
        "Infected Battlegrounds",
        "Infinite Road",
        "Inner Space",
        "Insects Runner",
        "Instant Death",
        "It's Quiz Time",
        "JASEM Just Another Shooter with Electronic Music",
        "Jotun",
        "Joumee The Hedgehog",
        "Jump Stop",
        "Jump To The Circle",
        "Jurassic City Walk",
        "Kaleido Chaos",
        "Karma. Incarnation 1",
        "Katie",
        "Keep Talking and Nobody Explodes",
        "Keeplanet",
        "Keyboard Sports",
        "Kicking Kittens: Putin Saves The World",
        "Kingdom Defense",
        "Kingdom: New Lands",
        "Knife Flipping",
        "Knights Of Hearts",
        "Labyrinths of Atlantis",
        "Last Alive",
        "Last Day Of June",
        "Last Encounter",
        "Lauren's Visit",
        "Let's Be Architects",
        "Let's Zig Zag",
        "Lethis: Daring Discoverers",
        "LGBT vs Russia Battlegrounds",
        "Lightning: D-Day",
        "Limiter",
        "Lost in Space",
        "Lost in the Dungeon",
        "Lost",
        "Lucius Demake",
        "MagiCats Builder",
        "Match Point",
        "Math Problem Challenge",
        "Math Speed Challenge",
        "Maze Of Pain",
        "Me And Dungeons",
        "Meadow",
        "Might and Mayhem",
        "Mighty Gemstones",
        "MineSweep",
        "Mini Golf Coop",
        "Mini Metro",
        "Minion Masters",
        "Mission - Wolf",
        "Molecule: A Chemical Challenge",
        "Moonlighter",
        "Morendar: Goblin Slayer",
        "Moss Destruction",
        "Mr Jezko",
        "Muay Thai Fighting",
        "Murderous Pursuits",
        "Mutazione",
        "Mutual Secret",
        "My Car",
        "My Coloring Book: Food and Beverage",
        "MyTD",
        "Need For Gowna",
        "Neko Navy",
        "Neon Void Runner",
        "Next Up Hero",
        "Nice Way",
        "Night Fly",
        "Ninja Way",
        "Nuts! The Battle of the Bulge",
        "Object Cleaning",
        "Obscurity",
        "Odysseus Kosmos and his Robot Quest: Episode 1",
        "Offworld Trading Company",
        "Orb The Ball",
        "Orwell",
        "Outcast",
        "OutSplit",
        "Overcooked",
        "Overfall",
        "Overhead",
        "Pain Train 2",
        "Pain Train PainPocalypse",
        "Pain Train",
        "Paint Skills",
        "Pankapu",
        "Paradox Wrench",
        "Park The Car",
        "Phantasma",
        "Phantom Soldier",
        "PIDO1",
        "Pinball 2018",
        "Pinstripe",
        "Pixel Traffic: Circle Rush",
        "Pixel Traffic: Highway Racing",
        "Pixel Traffic: Risky Bridge",
        "Pixel Worlds",
        "Pizza Titan Ultra",
        "Porcunipine",
        "Poultry Panic",
        "Primal Reign",
        "Project Highrise",
        "Psi Cards",
        "Psychedelic Platformer",
        "Punch Club",
        "Puzzle Puppers",
        "Pylow",
        "QLORB 2",
        "QLORB",
        "Quiet City",
        "R-COIL",
        "Rabbit And The Moon",
        "Race The Sun",
        "Range Ball",
        "Reach Me",
        "Reflex",
        "Retro Space Shooter",
        "Return to Planet X",
        "Ripple Effect",
        "Rise Up",
        "Road Doom",
        "Robo Do It",
        "Robo-orders",
        "Robocraft",
        "Robot Fighting",
        "Robot Warriors",
        "RoBros",
        "rOt 2",
        "rOt",
        "Round Ways",
        "Royal Casino: Video Poker",
        "Run, My Little Pixel",
        "Russian World Cup Battlegrounds",
        "Saint Kotar: The Yellow Mask",
        "SAWKOBAN",
        "Scalpers: Turtle & the Moonshine Gang",
        "School of Horror",
        "scram",
        "Screw-Nut",
        "Sea Battle: Through The Ages",
        "Secret of Harrow Manor",
        "Sentinels of the Multiverse",
        "Serial Cleaner",
        "Shark Attack Deathmatch 2",
        "Shield Impact",
        "SIG",
        "Sinister Zombies",
        "Sky Jump",
        "Sky Road",
        "Skyhill",
        "SkyTime",
        "Slabo",
        "Slime-san",
        "Slippingcers",
        "Sludge Life",
        "Smash It",
        "Sniper Squad Mission",
        "Solar Battle Glargaz",
        "Sos I Pie Sos",
        "Soulblight",
        "Space Escape",
        "Space Force",
        "Space Launch Engineer",
        "Space Mining",
        "Spirits of Xanadu",
        "Splash Blast Panic",
        "Spreadstorm",
        "SQR2",
        "SQR3",
        "Squares",
        "Star Shield Down",
        "STARBO",
        "Stayin Alive",
        "Steel Arena: Robot War",
        "Stefanos Sizzilin Pizza Pie",
        "Step Sisters: Episode 1",
        "Step Sisters: Episode 2",
        "Stick Fight: The Game",
        "Stickman Fighting",
        "Stickman In The Portal",
        "Stickman: Killer of Apples",
        "Stories Untold",
        "Stranger Things 3",
        "Subnautica",
        "Subsurface Circular",
        "Sudoku Universe",
        "Sudoku",
        "Super Golf 2018",
        "Super GTR Racing",
        "Super Steampunk Pinball 2D",
        "Sure Footing",
        "Surviving in the Forest",
        "Sweater OK!",
        "Swipe Fruit Smash",
        "Symbiotic Overload",
        "Tank Game",
        "Tanki X",
        "Teddy Floppy Ear: Kayaking",
        "Tempest",
        "Tempo Wizard",
        "Tesla Roadster Going To Mars",
        "The Art Of Knuckle Sandwich",
        "The Battle for the Hut",
        "The Best of Magicats",
        "The Elder Scrolls: Legends",
        "The Escapists 2",
        "The First Thrust Of God",
        "The God Paradox",
        "The Greater Good",
        "The Haunting of Billy",
        "The Last Hope",
        "The Last Mission",
        "The Little Acre",
        "The Messenger",
        "The Momo Game",
        "The Mooseman",
        "The Mystery Of Woolley Mountain",
        "The New Girl",
        "The Ninja Path",
        "The Tritan Initiative",
        "Throne of Lies",
        "Timberman",
        "Tiny Echo",
        "Totally Accurate Battlegrounds",
        "Totally Accurate Battle Simulator",
        "Totally Reliable Delivery Service",
        "Travildorn",
        "Trials of The Illuminati: Animated Christmas Time Jigsaws",
        "Trio",
        "Trivia Vault: 1980's Trivia 2",
        "Trivia Vault: 1980's Trivia",
        "Trivia Vault: Art Trivia",
        "Trivia Vault: Auto Racing Trivia",
        "Trivia Vault: Baseball Trivia",
        "Trivia Vault: Basketball Trivia",
        "Trivia Vault: Boxing Trivia",
        "Trivia Vault: Business Trivia",
        "Trivia Vault: Celebrity Trivia",
        "Trivia Vault: Classic Rock Trivia 2",
        "Trivia Vault: Classic Rock Trivia",
        "Trivia Vault: Football Trivia",
        "Trivia Vault: Health Trivia Deluxe",
        "Trivia Vault: Mini Mixed Trivia 2",
        "Trivia Vault: Mini Mixed Trivia 3",
        "Trivia Vault: Mini Mixed Trivia 4",
        "Trivia Vault: Mini Mixed Trivia",
        "Trivia Vault: Mixed Trivia",
        "Trivia Vault: Olympics Trivia",
        "Trivia Vault: Science & History Trivia 2",
        "Trivia Vault: Science & History Trivia",
        "Trivia Vault: Super Heroes Trivia 2",
        "Trivia Vault: Super Heroes Trivia",
        "Trivia Vault: Technology Trivia Deluxe",
        "Trivia Vault: Video Game Trivia Deluxe",
        "Twickles",
        "Uganda Know De Way",
        "Ukrainian Ball In Search Of Gas",
        "Unblock: The Parking",
        "Unbroken Warrior",
        "Undertaker's",
        "Unit 4",
        "Unite Cell",
        "Unturned",
        "Uurnog",
        "Vampires!",
        "Vault Of Honor",
        "Viki Spotter: The Farm",
        "Viking's Drakkars",
        "Viridi",
        "Void Cube Runner",
        "WAR_WAR_WAR: Smiles vs Ghosts",
        "Warp Rider",
        "Watergate Xtreme",
        "When They Arrived",
        "White Noise 2",
        "Woodlands",
        "World Inside Out",
        "Wrestlers Without Boundaries",
        "XORPLE",
        "Yatsumitsu Fists of Wrath",
        "Yooka Laylee");

    setExtensions("assets"); // MUST BE LOWER CASE
    setPlatforms("PC");

    setFileTypes(Unity3DHelper.getFileTypes());

  }

  int[] fileTypeMapping = new int[1500]; // assuming a maximum of 1500 classes in a single file

  int numFileTypeMappings = 0;

  /**
   **********************************************************************************************
   Decompressed an archive, where the whole archive is compressed.
   Reads the compressed block information first, then processes the compressed blocks themselves.
   Writes the output to a file with the same name, but with "_ge_decompressed" at the end of it.
   The decompressed file contains the same Unreal header as the compressed file, so you can open
   the decompressed file in GE directly, without needing to re-decompress anything.
   If the decompressed file already exists, we use that, we don't re-decompress.
   **********************************************************************************************
   **/
  public FileManipulator decompressLZ4Archive(FileManipulator fm, int firstOffset, int[] compLengths, int[] decompLengths) {
    try {
      // Build a new "_ge_decompressed" archive file in the current directory
      File origFile = fm.getFile();

      String pathOnly = FilenameSplitter.getDirectory(origFile);
      String filenameOnly = FilenameSplitter.getFilename(origFile);
      String extensionOnly = FilenameSplitter.getExtension(origFile);

      File decompFile = new File(pathOnly + File.separatorChar + filenameOnly + "_ge_decompressed" + "." + extensionOnly);
      //File decompFile = new File(pathOnly + File.separatorChar + outputFilename);
      if (decompFile.exists()) {
        // we've already decompressed this file before - open and return it
        return new FileManipulator(decompFile, false);
      }

      FileManipulator decompFM = new FileManipulator(decompFile, true);

      TaskProgressManager.setMessage(Language.get("Progress_DecompressingArchive")); // progress bar
      TaskProgressManager.setIndeterminate(true);

      Exporter_LZ4 exporter = Exporter_LZ4.getInstance();

      long currentOffset = firstOffset;

      int numBlocks = decompLengths.length;
      //boolean adjustedForPadding = false;
      for (int b = 0; b < numBlocks; b++) {
        // go to the right place in the file (just in case of an overshoot form a previous block)
        fm.seek(currentOffset);

        int decompBytesRemaining = decompLengths[b]; // so we can cut it short if there's an overshoot in the available()

        if (decompLengths[b] == compLengths[b]) {
          // this block isn't compressed - copy it raw
          while (decompBytesRemaining > 0) {
            decompFM.writeByte(fm.readByte());
            decompBytesRemaining--;
          }
        }
        else {

          exporter.open(fm, decompLengths[b], decompLengths[b]);

          while (exporter.available() && decompBytesRemaining != 0) {
            decompFM.writeByte(exporter.read());
            decompBytesRemaining--;
          }

          /*
          if (b == 0 && decompBytesRemaining == decompLengths[b]) {
          // we weren't able to decompress at all! Maybe we need to see if the offset is a multiple of 2 bytes, and adjust
          if (!adjustedForPadding) {
            adjustedForPadding = true;
            currentOffset = firstOffset + 1;
            b--; // because it'll get ++ added by the loop
            continue;
          }
          }
          */

          while (decompBytesRemaining > 0) { // if it's cut short for some reason, padd out to the right size
            decompFM.writeByte(0);
            decompBytesRemaining--;
          }
        }

        currentOffset += compLengths[b];
      }

      // Force-write out the decompressed file to write it to disk, then change the buffer to read-only.
      decompFM.close();

      if (decompFile.length() <= 0) {
        // didn't decompress, so just return the original archive
        decompFile.delete();
        decompFile = origFile;
      }

      decompFM = new FileManipulator(decompFile, false);

      TaskProgressManager.setIndeterminate(false);
      TaskProgressManager.setMessage(Language.get("Progress_ReadingArchive")); // progress bar

      return decompFM;
    }
    catch (Throwable t) {
      ErrorLogger.log(t);
      return null;
    }
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

        fm.skip(4); // for the next field
      }
      else if (FieldValidator.checkExtension(fm, "resource")) {
        // clicked on a referenced archive - check to see if the real ASSETS file exists
        File dirFile = getDirectoryFile(fm.getFile(), "assets");
        if (dirFile != null && dirFile.exists()) {
          rating += 50; // higher than 25, as have confirmed both an ASSETS file AND a RESOURCE file - quite unique
          return rating; // otherwise the remaining checks will throw Exceptions and kill this plugin as a candidate
        }
      }
      else if (FieldValidator.checkExtension(fm, "ress")) { // "resS", but all lowercase for the comparison
        // clicked on a referenced archive - check to see if the real ASSETS file exists
        String pathName = fm.getFilePath();
        int pathNameLength = pathName.length();
        if (pathNameLength > 5) {
          File dirFile = new File(pathName.substring(0, pathNameLength - 5));
          if (dirFile != null && dirFile.exists()) {
            rating += 50; // higher than 25, as have confirmed both an ASSETS file AND a RESOURCE file - quite unique
            return rating; // otherwise the remaining checks will throw Exceptions and kill this plugin as a candidate
          }
        }
      }
      else if (fm.readString(4).equals("Unit")) {
        // No extension, so maybe a UnityFS file?

        // 8 - Header ("UnityFS" + null)
        String headerString = fm.readString(3);
        int headerByte = fm.readByte();
        if (headerString.equals("yFS") && headerByte == 0) {
          rating += 50;
        }

        // 4 - Version Number (6) (BIG ENDIAN)
        if (IntConverter.changeFormat(fm.readInt()) == 6) {
          rating += 5;
        }

        // X - General Version String (5.x.x)
        if (fm.readString(2).equals("5.")) {
          rating += 5;
        }

        return rating;
      }
      else if (FilenameSplitter.getExtension(fm.getFile()).equals("")) {
        // no extension, like one of the "level" files
        rating += 20;

        fm.skip(4); // for the next field
      }

      // 4 - Filename Directory Offset [+14 + VersionStringLength] (BIG ENDIAN)
      //fm.skip(4); // already skipped in the check above

      long arcSize = fm.getLength();

      // 4 - Size of Assets file (BIG ENDIAN)
      if (IntConverter.changeFormat(fm.readInt()) == arcSize) {
        rating += 5;
      }

      // 4 - Version (17) (BIG ENDIAN)
      if (IntConverter.changeFormat(fm.readInt()) == 17) {
        rating += 5;
      }

      // 4 - Filename Directory Offset (BIG ENDIAN)
      if (FieldValidator.checkOffset(IntConverter.changeFormat(fm.readInt()), arcSize)) {
        rating += 5;
      }

      // 4 - null
      fm.skip(4);

      // X - Version String (5.6.0p1)
      // 1 - null Terminator
      if (fm.readString(2).equals("5.")) {
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
  @SuppressWarnings("unused")
  @Override
  public Resource[] read(File path) {
    try {

      // First up, if they clicked on the "resource" or the "resS" file, point to the ASSETS file instead
      String extension = FilenameSplitter.getExtension(path);
      if (extension.equals("resource")) {
        // clicked on a referenced archive - check to see if the real ASSETS file exists
        File dirFile = getDirectoryFile(path, "assets");
        if (dirFile != null && dirFile.exists()) {
          path = dirFile;
        }
      }
      else if (extension.equals("resS")) {
        // clicked on a referenced archive - check to see if the real ASSETS file exists
        String pathName = path.getAbsolutePath();
        int pathNameLength = pathName.length();
        if (pathNameLength > 5) {
          File dirFile = new File(pathName.substring(0, pathNameLength - 5));
          if (dirFile != null && dirFile.exists()) {
            path = dirFile;
          }
        }
      }
      // Now we know we're pointing to the ASSETS file

      fileTypeMapping = new int[1500]; // assuming a maximum of 1500 classes in a single file
      numFileTypeMappings = 0;

      // NOTE - Compressed files MUST know their DECOMPRESSED LENGTH
      //      - Uncompressed files MUST know their LENGTH

      addFileTypes();

      //ExporterPlugin exporter = Exporter_ZLib.getInstance();

      // RESETTING GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      boolean unityFS = false;
      int relativeOffset = 0;
      int relativeDataOffset = 0;
      if (fm.readString(7).equals("UnityFS")) {
        // a UnityFS file - skip over the header stuff, to reach the real file data
        unityFS = true;

        // 8 - Header ("UnityFS" + null)
        // 4 - Version Number (6) (BIG ENDIAN)
        fm.skip(5);

        // X - General Version String (5.x.x)
        // 1 - null Terminator
        fm.readNullString();

        // X - Version String (5.5.2f1)
        // 1 - null Terminator
        fm.readNullString();

        // 4 - null
        fm.skip(4);

        // 4 - Archive Length (BIG ENDIAN)
        long arcLength = IntConverter.unsign(IntConverter.changeFormat(fm.readInt()));
        FieldValidator.checkLength(arcLength, arcSize);

        // 4 - Compressed Data Header Size (File Data Offset [+46]) (BIG ENDIAN)
        int compDataHeaderSize = IntConverter.changeFormat(fm.readInt());
        FieldValidator.checkLength(compDataHeaderSize, arcSize);

        relativeOffset = compDataHeaderSize + 46;
        FieldValidator.checkOffset(relativeOffset, arcSize);

        // 4 - Decompressed Data Header Size (BIG ENDIAN)
        int decompDataHeaderSize = IntConverter.changeFormat(fm.readInt());
        FieldValidator.checkLength(decompDataHeaderSize);

        // 4 - Compression Flags (BIG ENDIAN) (&3=LZ4, &1=LZMA, &80=DirectoryAtEnd, &40=EntryInfoPresent)
        int flags = IntConverter.changeFormat(fm.readInt());

        if ((flags & 128) == 128) {
          // the directory is at the end of the file
          fm.seek(arcLength - compDataHeaderSize);
        }

        if ((flags & 3) == 3) {
          // LZ4 Compression
          byte[] dirBytes = new byte[decompDataHeaderSize];
          int decompWritePos = 0;
          Exporter_LZ4 exporter = Exporter_LZ4.getInstance();
          exporter.open(fm, compDataHeaderSize, decompDataHeaderSize);

          for (int b = 0; b < decompDataHeaderSize; b++) {
            if (exporter.available()) { // make sure we read the next bit of data, if required
              dirBytes[decompWritePos++] = (byte) exporter.read();
            }
          }

          // open the decompressed data for processing
          FileManipulator fmDir = new FileManipulator(new ByteBuffer(dirBytes));

          // 16 - Unknown
          fmDir.skip(16);

          // 4 - Number of Storage Blocks
          int numBlocks = IntConverter.changeFormat(fmDir.readInt());
          FieldValidator.checkNumFiles(numBlocks);

          int[] blockDecompLengths = new int[numBlocks];
          int[] blockCompLengths = new int[numBlocks];
          for (int b = 0; b < numBlocks; b++) {
            // 4 - Decomp Block Size
            int blockDecompLength = IntConverter.changeFormat(fmDir.readInt());
            blockDecompLengths[b] = blockDecompLength;

            // 4 - Comp Block Size
            int blockCompLength = IntConverter.changeFormat(fmDir.readInt());
            blockCompLengths[b] = blockCompLength;

            // 2 - Block Flags
            fmDir.skip(2);
            //System.out.println("Block " + b + " with comp length " + blockCompLength + " and decomp length " + blockDecompLength);
          }

          long currentOffset = fm.getOffset();

          // Decompress the file from the blocks
          FileManipulator decompFM = decompressLZ4Archive(fm, (int) currentOffset, blockCompLengths, blockDecompLengths);
          if (decompFM == null) {
            return null; // couldn't decompress the file for some reason
          }

          // 4 - Number of Bundle Entries
          int numEntries = IntConverter.changeFormat(fmDir.readInt());
          FieldValidator.checkNumFiles(numEntries);

          TaskProgressManager.setMessage(Language.get("Progress_SplittingArchive")); // progress bar
          TaskProgressManager.setIndeterminate(true);

          long initialOffset = fm.getOffset();
          // now we want to split the decompressed file into separate files for each entry
          File[] splitFiles = new File[numEntries];
          for (int e = 0; e < numEntries; e++) {
            // 8 - Offset
            //long entryOffset = LongConverter.changeFormat(fmDir.readLong());
            fmDir.skip(4);
            long entryOffset = IntConverter.unsign(IntConverter.changeFormat(fmDir.readInt()));
            //FieldValidator.checkOffset(entryOffset, arcSize);

            // 8 - Decomp Length
            //long entryLength = LongConverter.changeFormat(fmDir.readLong());
            fmDir.skip(4);
            long entryLength = IntConverter.changeFormat(fmDir.readInt());
            //FieldValidator.checkLength(entryLength, arcSize);

            // 4 - Flags
            int entryFlags = IntConverter.changeFormat(fmDir.readInt());

            // X - Name
            String entryName = fmDir.readNullString();

            //System.out.println("Entry " + entryName + " at offset " + entryOffset + " with length " + entryLength + " and flags " + entryFlags);

            if (numEntries == 1) {
              splitFiles[e] = decompFM.getFile();
              continue; // don't bother splitting if it's only 1 file anyway
            }

            // Build a new split file in the current directory, with the entry name
            File origFile = fm.getFile();
            String pathOnly = FilenameSplitter.getDirectory(origFile);

            File splitFile = new File(pathOnly + File.separatorChar + entryName);
            splitFiles[e] = splitFile;

            if (splitFile.exists()) {
              // we've already split this file before - don't split it again
            }
            else {
              // do the split
              FileManipulator fmSplit = new FileManipulator(splitFile, true);

              decompFM.seek(entryOffset);
              for (int b = 0; b < entryLength; b++) {
                fmSplit.writeByte(decompFM.readByte());
              }

              fmSplit.close();
            }

          }

          fm.close(); // close the original archive
          decompFM.close(); // close the decompressed file
          fmDir.close(); // close the decompressed directory

          // now we want to open each split file, one at a time, and use that to build the archive
          Resource[] resources = new Resource[0];
          int numResources = 0;

          TaskProgressManager.setIndeterminate(false);
          TaskProgressManager.setMessage(Language.get("Progress_ReadingArchive")); // progress bar

          for (int e = 0; e < numEntries; e++) {
            // read the archive
            Resource[] splitResources = read(splitFiles[e]);
            if (splitResources == null) {
              continue;
            }

            // resize the existing array
            int numSplitResources = splitResources.length;
            int newArraySize = numResources + numSplitResources;
            Resource[] oldResources = resources;
            resources = new Resource[newArraySize];

            // add the split resources to the end of the array
            System.arraycopy(oldResources, 0, resources, 0, numResources);
            System.arraycopy(splitResources, 0, resources, numResources, numSplitResources);
            numResources = newArraySize;
          }

          return resources; // stop early, as we've already done all the reading

          /*
          fm = decompFM; // now we're going to read from the decompressed file instead
          
          arcSize = fm.getLength(); // use the arcSize of the decompressed file (for checking offsets etc)
          path = fm.getFile(); // So the resources are stored against the decompressed file
          
          // Now we have a normal archive - we don't need to set relative offsets or anything funny like that
          relativeOffset = 0;
          //unityFS = false; // NEED THIS TO BE TRUE, TO READ THE NESTED ENTRIES IN THE HEADER!
          
          // Now we're reading the first 2 fields of the normal unity file
          
          // 4 - Data Directory Offset [+14 + VersionStringLength] (BIG ENDIAN)
          fm.skip(4);
          
          // 4 - Size of Assets file (BIG ENDIAN)
          relativeDataOffset = IntConverter.changeFormat(fm.readInt()) + relativeOffset;
          //FieldValidator.checkOffset(relativeDataOffset, arcSize + 1);// +1 to allow for UnityFS files where the data is all inline
           */

        }
        else if ((flags & 3) == 1) {
          // LZMA Compression
          ErrorLogger.log("[Plugin_ASSETS_17] LZMA Compression Not Implemented");
          return null;
        }
        else {
          // no compression

          // X - Other Stuff
          // X - Unity Archive
          fm.seek(relativeOffset);

          // Now we're reading the first 2 fields of the normal unity file

          // 4 - Data Directory Offset [+14 + VersionStringLength] (BIG ENDIAN)
          fm.skip(4);

          // 4 - Size of Assets file (BIG ENDIAN)
          relativeDataOffset = IntConverter.changeFormat(fm.readInt()) + relativeOffset;
          //FieldValidator.checkOffset(relativeDataOffset, arcSize + 1);// +1 to allow for UnityFS files where the data is all inline
        }

      }
      else {
        fm.skip(1);
      }

      // 4 - Data Directory Offset [+14 + VersionStringLength] (BIG ENDIAN)
      // 4 - Size of Assets file (BIG ENDIAN)
      // 4 - Version Number (also Number Of Small Offsets) (17) (BIG ENDIAN)
      fm.skip(4); // already read 7 bytes in the check above, and 1 byte afterwards

      // 4 - Data Directory Offset (BIG ENDIAN)
      int dirOffset = IntConverter.changeFormat(fm.readInt()) + relativeOffset; // +relativeOffset to account for the UnityFS header, in those files
      FieldValidator.checkOffset(dirOffset, arcSize);

      // 4 - null
      fm.skip(4);

      // X - Version String (5.6.0p1)
      // 1 - null Terminator
      fm.readNullString();

      // 4 - Unknown
      // 1 - null
      fm.skip(5);

      // 4 - Number of Bases?
      int numBases = fm.readInt();
      FieldValidator.checkNumFiles(numBases);

      // bring out the variables from the loop below
      int numFiles = 0;
      Resource[] resources = new Resource[0];

      // this boolean, while, try is so we can come back and try again, in case this file was extracted from a unityFS and it has nested property details
      boolean repeatCheck = true;
      boolean forcedUnityFS = false;
      long startOffset = fm.getOffset();

      int skipLarge = 35;
      int skipSmall = 19;
      int baseToCheck = 114;
      boolean skipAltered = false;
      boolean forcedLargeSkips = false;

      while (repeatCheck) {
        fm.seek(startOffset);

        repeatCheck = false;

        try {

          // BASES DIRECTORY
          // for each Base...
          for (int b = 0; b < numBases; b++) {
            // 4 - ID Number?
            int baseID = fm.readInt();
            if (baseID == baseToCheck) {
              // 35 - Base Name (encrypted)
              fm.skip(skipLarge);
            }
            else {
              // 19 - Base Name (encrypted)
              fm.skip(skipSmall);
            }

            if (unityFS) {
              // read all the nested property data

              // 4 - Number of Entries
              int numEntries = fm.readInt();
              try {
                FieldValidator.checkNumFiles(numEntries);
              }
              catch (Throwable t) {
                if (forcedUnityFS) {
                  if (!skipAltered) {

                    // If we've gone through loop without unityFS, then we've gone through with unityFS forced, then try option 3 which is some different sized skips for the encrypted names
                    skipAltered = true;
                    repeatCheck = true;

                    skipLarge -= 3;
                    skipSmall -= 3;
                    baseToCheck = -1;

                    unityFS = false; // so this + the throw below will trigger a re-loop
                    throw t;
                  }
                  else if (skipAltered) {
                    // option 4 - the small skip is 16 bytes and the large is 32 bytes. Assume we did a small skip (16) and we want to see
                    // about the large skip instead. We've already read 4 bytes for the check above, read another 12 then try numEntries again.
                    fm.skip(12);
                    numEntries = fm.readInt();
                    FieldValidator.checkNumFiles(numEntries);
                    /*
                    forcedLargeSkips = true;  
                    
                    skipSmall = skipLarge;
                    
                    unityFS = false; // so this + the throw below will trigger a re-loop
                    throw t;
                    */
                  }

                }
              }

              // 4 - Filename Directory Length
              //System.out.println(fm.getOffset());
              int filenameDirLength = fm.readInt();
              FieldValidator.checkLength(filenameDirLength, arcSize);

              //for (int e = 0; e < numEntries; e++) {
              // 2 - Unknown
              // 2 - Unknown
              // 2 - Unknown
              // 2 - Unknown
              // 2 - Unknown
              // 2 - Unknown
              // 4 - Flags? (-1/1/4/16)
              // 4 - ID (incremental from 0)
              // 4 - Unknown
              //}
              fm.skip(numEntries * 24);

              // X - Filename Directory
              fm.skip(filenameDirLength);

            }

            // map the bases to the file types
            /*
            if (baseID == 28) {
            fileType_TEX = b;
            }
            else if (baseID == 83) {
            fileType_SND = b;
            }
            else if (baseID == 4) {
            fileType_REF = b;
            }
            else if (baseID == 21) {
            fileType_MAT = b;
            }
            else if (baseID == 1) {
            fileType_DIR = b;
            }
            else if (baseID == 23) {
            fileType_MATREF = b;
            }
            else if (baseID == 33) {
            fileType_MSHREF = b;
            }
            else if (baseID == 43) {
            fileType_MSH = b;
            }
            else if (baseID == 48) {
            fileType_SHADER = b;
            }
            else if (baseID == 49) {
            fileType_TXT = b;
            }
            else if (baseID == 74) {
            fileType_ANI = b;
            }
            else if (baseID == 115) {
            fileType_SCRIPT = b;
            }
            else if (baseID == 128) {
            fileType_TTF = b;
            }
            else if (baseID == 150) {
            fileType_BIN = b;
            }
            else if (baseID == 152) {
            fileType_OGM = b;
            }
            else if (baseID == 156) {
            fileType_TER = b;
            }
            else if (baseID == 184) {
            fileType_SBAM = b;
            }
            else if (baseID == 194) {
            fileType_TES = b;
            }
            else if (baseID == 89) {
            fileType_CTX = b;
            }
            */
            /*
            if (baseID == 1) {
              fileType_1 = b;
            }
            else if (baseID == 4) {
              fileType_4 = b;
            }
            else if (baseID == 12) {
              fileType_12 = b;
            }
            else if (baseID == 15) {
              fileType_15 = b;
            }
            else if (baseID == 20) {
              fileType_20 = b;
            }
            else if (baseID == 21) {
              fileType_21 = b;
            }
            else if (baseID == 23) {
              fileType_23 = b;
            }
            else if (baseID == 26) {
              fileType_26 = b;
            }
            else if (baseID == 28) {
              fileType_28 = b;
            }
            else if (baseID == 33) {
              fileType_33 = b;
            }
            else if (baseID == 43) {
              fileType_43 = b;
            }
            else if (baseID == 48) {
              fileType_48 = b;
            }
            else if (baseID == 49) {
              fileType_49 = b;
            }
            else if (baseID == 54) {
              fileType_54 = b;
            }
            else if (baseID == 65) {
              fileType_65 = b;
            }
            else if (baseID == 74) {
              fileType_74 = b;
            }
            else if (baseID == 76) {
              fileType_76 = b;
            }
            else if (baseID == 82) {
              fileType_82 = b;
            }
            else if (baseID == 83) {
              fileType_83 = b;
            }
            else if (baseID == 84) {
              fileType_84 = b;
            }
            else if (baseID == 89) {
              fileType_89 = b;
            }
            else if (baseID == 96) {
              fileType_96 = b;
            }
            else if (baseID == 102) {
              fileType_102 = b;
            }
            else if (baseID == 111) {
              fileType_111 = b;
            }
            else if (baseID == 115) {
              fileType_115 = b;
            }
            else if (baseID == 128) {
              fileType_128 = b;
            }
            else if (baseID == 134) {
              fileType_134 = b;
            }
            else if (baseID == 135) {
              fileType_135 = b;
            }
            else if (baseID == 136) {
              fileType_136 = b;
            }
            else if (baseID == 137) {
              fileType_137 = b;
            }
            else if (baseID == 150) {
              fileType_150 = b;
            }
            else if (baseID == 152) {
              fileType_152 = b;
            }
            else if (baseID == 153) {
              fileType_153 = b;
            }
            else if (baseID == 156) {
              fileType_156 = b;
            }
            else if (baseID == 184) {
              fileType_184 = b;
            }
            else if (baseID == 194) {
              fileType_194 = b;
            }
            */
            fileTypeMapping[b] = baseID;
          }
          numFileTypeMappings = numBases;

          // 4 - Number of Files
          numFiles = fm.readInt();
          //if (arcSize > 200000000) { // Unturned - big file
          FieldValidator.checkNumFiles(numFiles / 20);
          //}
          //else {
          //  FieldValidator.checkNumFiles(numFiles);
          //}

          // 0-3 - null to a multiple of 4 bytes
          fm.skip(calculatePadding(fm.getOffset() - relativeOffset, 4));

          resources = new Resource[numFiles];
          TaskProgressManager.setMaximum(numFiles);

          // FILES DIRECTORY
          // for each file (20 bytes per entry)
          for (int i = 0; i < numFiles; i++) {
            // 4 - ID Number (incremental from 1)
            fm.skip(4);

            // 4 - null
            int null1 = fm.readInt();

            // 4 - File Offset (relative to the start of the Filename Directory) - points to the FilenameLength field
            int offset = fm.readInt() + dirOffset;

            // 4 - File Size
            int length = fm.readInt();

            // 4 - File Type Code
            int fileTypeCode = fm.readInt();

            if (skipAltered) {
              // 8 - unknown
              fm.skip(8);
            }

            if (unityFS) {
              if (fileTypeCode == 0 && (length < 0 || offset < 0)) {
                // abrupt end of the directory
                numFiles = i;
                resources = resizeResources(resources, numFiles);
                break;
              }
            }
            // else, do checking as per normal
            FieldValidator.checkOffset(offset, arcSize + 1); // +1 to allow for empty files at the end of the archive
            FieldValidator.checkLength(length, arcSize);

            //String fileType = convertFileType(fileTypeCode);
            String fileType = null;
            if (fileTypeCode < 0) {
              for (int c = 0; c < numFileTypeMappings; c++) {
                if (fileTypeMapping[c] == fileTypeCode) {
                  fileType = Unity3DHelper.getFileExtension(c);
                  break;
                }
              }
              if (fileType == null) {
                fileType = "." + fileTypeCode;
              }
            }
            else {
              if (unityFS) {
                fileType = Unity3DHelper.getFileExtension(fileTypeCode);
              }
              else {
                int mapping = fileTypeMapping[fileTypeCode];
                if (mapping < 0) {
                  fileType = Unity3DHelper.getFileExtension(fileTypeCode);
                }
                else {
                  fileType = Unity3DHelper.getFileExtension(mapping);
                }
              }
            }

            /*
            try {
            Integer.parseInt(fileType.substring(1));
            // output the details for further analysis
            System.out.println(null1 + "\t" + offset + "\t" + length + "\t" + fileTypeCode + "\t" + fileTypeCodeSmall + "\t" + unknownSmall + "\t" + null2);
            }
            catch (Throwable t) {
            }
            */

            //path,name,offset,length,decompLength,exporter
            resources[i] = new Resource(path, fileType, offset, length);

            TaskProgressManager.setValue(i);
          }

        }
        catch (Throwable t) {
          // try again, reading as if it IS a unityFS file
          if (!unityFS) {
            unityFS = true;
            forcedUnityFS = true;
            repeatCheck = true;
          }
          else {
            // if it was already being read as a unityFS file, we really do want to throw the exception
            throw t;
          }
        }

      } // end of repeatCheck while()

      if (forcedUnityFS) {
        unityFS = false; // reset so the remaining values are read properly
      }

      //
      // In this loop...
      // * Get the filenames for each file
      // * Detect all the Type1 Resources
      // * If a SND or TEX Resource has its data in an external archive, point to it instead
      // * Sets the exporter for the SND file, so that we can analyse and preview the audio
      //
      TaskProgressManager.setValue(0);

      ExporterPlugin exporterFSB = Exporter_Custom_FSB5_ProcessWithinArchive.getInstance();

      Resource[] type1Resources = new Resource[numFiles];
      int numType1Resources = 0;

      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];

        String fileType = resource.getName();
        if (fileType.equals(".GameObject")) {
          // not a real file - just a folder structure or something
          // store it for analysis further down

          type1Resources[numType1Resources] = resource;
          numType1Resources++;

          continue;
        }

        // Go to the data offset
        long offset = resource.getOffset();
        fm.seek(offset);

        // 4 - Filename Length
        int filenameLength = fm.readInt();
        if (filenameLength <= 0) {
          resource.setName(Resource.generateFilename(i) + fileType);
          continue;
        }
        FieldValidator.checkFilenameLength(filenameLength);

        // X - Filename
        String filename = fm.readString(filenameLength);
        FieldValidator.checkFilename(filename);

        // 0-3 - null Padding to 4 bytes
        int paddingSize = calculatePadding(filenameLength, 4);
        fm.skip(paddingSize);

        resource.setName(filename + fileType);

        long realOffset = fm.getOffset();
        long realSize = resource.getLength() - (realOffset - offset);

        if (fileType.equals(".TextAsset")) {
          // 4 - File Size
          realSize = fm.readInt();
          realOffset = fm.getOffset();
        }
        else if (fileType.equals(".AudioClip")) {
          try {
            // 4 - Unknown (0/1)
            // 4 - Number of Channels? (1/2)
            // 4 - Sample Rate? (44100)
            // 4 - Bitrate? (16)
            // 4 - Unknown
            // 4 - null
            // 4 - null
            // 2 - Unknown (1)
            // 2 - Unknown (1)
            fm.skip(32);

            // 4 - External Archive Filename Length
            int externalFilenameLength = fm.readInt();
            FieldValidator.checkFilenameLength(externalFilenameLength);

            // X - External Archive Filename
            String externalFilename = fm.readString(externalFilenameLength);
            FieldValidator.checkFilename(externalFilename);

            // 0-3 - null Padding to 4 bytes
            int externalPaddingSize = calculatePadding(externalFilenameLength, 4);
            fm.skip(externalPaddingSize);

            // Check that the filename is a valid File in the FileSystem
            File externalArchive = new File(resource.getSource().getParent() + File.separatorChar + externalFilename);
            if (!unityFS && !externalArchive.exists()) {
              if (forcedUnityFS) {
                // see if we can find the archive name locally
                externalFilename = FilenameSplitter.getFilenameAndExtension(externalFilename);
                externalArchive = new File(resource.getSource().getParent() + File.separatorChar + externalFilename);
                if (!externalArchive.exists()) {
                  throw new FileNotFoundException("External resource " + externalFilename + " could not be found");
                }
              }
              else {
                throw new FileNotFoundException("External resource " + externalFilename + " could not be found");
              }
            }
            else if (unityFS) {
              // UnityFS files contain the file data after the end of the unity data
              externalArchive = path;
            }

            long externalArcSize = externalArchive.length();

            // 4 - File Offset
            int extOffset = fm.readInt();
            if (unityFS) {
              extOffset += relativeDataOffset;
            }
            FieldValidator.checkOffset(extOffset, externalArcSize);

            // 4 - null
            fm.skip(4);

            // 4 - File Length
            int extSize = fm.readInt();
            FieldValidator.checkLength(extSize, externalArcSize);

            // 4 - null
            // 4 - Unknown (1)

            // Now that we reached the end successfully, set the externalArchive on the Resource (the other fields set down further)
            resource.setSource(externalArchive);
            realOffset = extOffset;
            realSize = extSize;

            resource.setExporter(exporterFSB);

            //System.out.println("Setting External Resource " + externalFilename + " on Resource " + resource.getName());
          }
          catch (Throwable t) {
            // not in an external archive, or some other error
            ErrorLogger.log(t);
          }
        }
        else if (fileType.equals(".Texture2D")) {
          try {
            // 4 - Width/Height? (1024)
            int imageWidth = fm.readInt();

            // 4 - Width/Height? (1024/512)
            int imageHeight = fm.readInt();

            if (imageWidth == 4 && imageHeight == 0) {
              // these 2 fields were an 8-byte header, so need to read the real width/height next (GWENT game)

              // 4 - Width/Height? (1024)
              imageWidth = fm.readInt();

              // 4 - Width/Height? (1024/512)
              imageHeight = fm.readInt();
            }

            // 4 - File Size
            int imageFileSize = fm.readInt();

            // 4 - Image Format Code
            int imageFormat = fm.readInt();

            // 4 - Mipmap Count
            int mipmapCount = fm.readInt();

            if (resource.getLength() - imageFileSize > 0) {
              // This file is in the existing archive, not a separate archive file

              // 4 - Unknown
              // 4 - Unknown
              // 4 - Unknown
              // 4 - Unknown
              // 4 - Unknown
              // 4 - Unknown
              // 4 - Unknown
              // 4 - Unknown
              // 4 - Unknown
              for (int p = 0; p < 64; p += 4) { // a little loop to account for variable-sized padding
                // 4 - File Size
                realSize = fm.readInt();

                if (realSize == imageFileSize) {
                  // found the matching file size value
                  break;
                }
              }

              FieldValidator.checkLength(realSize, arcSize);

              realOffset = fm.getOffset();

              // Convert the Resource into a Resource_Unity3D_TEX
              Resource oldResource = resource;
              resource = new Resource_Unity3D_TEX();
              resource.copyFrom(oldResource); // copy the data from the old Resource to the new Resource
              resources[i] = resource; // stick the new Resource in the array, overwriting the old Resource

              // Set the image-specific properties on the new Resource
              Resource_Unity3D_TEX castResource = (Resource_Unity3D_TEX) resource;
              castResource.setImageWidth(imageWidth);
              castResource.setImageHeight(imageHeight);
              castResource.setFormatCode(imageFormat);
              castResource.setMipmapCount(mipmapCount);

              //System.out.println("This is an internal resource --> Resource " + resource.getName());
            }
            else {
              // This file is in an external archive, not the current one

              // 4 - Unknown (256)
              // 4 - Unknown (1)
              // 4 - Unknown (2)
              // 4 - Unknown (2/1)
              // 4 - Unknown (1/0)
              // 4 - null
              // 4 - Unknown (1)
              // 4 - null
              // 4 - Unknown (1)
              // 4 - null

              fm.skip(40);
              /*
              String output = "";
              
              for (int f = 0; f < 10; f++) {
                output += "Field" + (f + 1) + "=" + fm.readInt() + "\t";
              }
              System.out.println("Width=" + imageWidth + "\tHeight=" + imageHeight + "\tType=" + imageFormat + "\tMipMaps=" + mipmapCount + "\tFileSize=" + imageFileSize + "\t" + output + resource.getName());
              */

              // 4 - File Offset
              int extOffset = fm.readInt();

              // 4 - File Length
              int extSize = fm.readInt();
              if (extSize != imageFileSize) {
                extOffset = extSize;
                extSize = fm.readInt();

                if (extSize != imageFileSize) {
                  extOffset = extSize;
                  extSize = fm.readInt();

                  if (extSize != imageFileSize) {
                    extOffset = extSize;
                    extSize = fm.readInt();
                  }
                }
              }

              // 4 - External Archive Filename Length
              int externalFilenameLength = fm.readInt();
              try {
                FieldValidator.checkFilenameLength(externalFilenameLength);
              }
              catch (Throwable t) {
                extOffset = extSize;
                extSize = externalFilenameLength;

                externalFilenameLength = fm.readInt();
                FieldValidator.checkFilenameLength(externalFilenameLength);
              }

              // X - External Archive Filename
              String externalFilename = fm.readString(externalFilenameLength);
              FieldValidator.checkFilename(externalFilename);

              // 0-3 - null Padding to 4 bytes
              int externalPaddingSize = calculatePadding(externalFilenameLength, 4);
              fm.skip(externalPaddingSize);

              // Check that the filename is a valid File in the FileSystem
              File externalArchive = new File(resource.getSource().getParent() + File.separatorChar + externalFilename);
              //if (!externalArchive.exists()) {
              //  throw new FileNotFoundException("External resource " + externalFilename + " could not be found");
              //}
              if (!unityFS && !externalArchive.exists()) {
                if (forcedUnityFS) {
                  // see if we can find the archive name locally
                  externalFilename = FilenameSplitter.getFilenameAndExtension(externalFilename);
                  externalArchive = new File(resource.getSource().getParent() + File.separatorChar + externalFilename);
                  if (!externalArchive.exists()) {
                    throw new FileNotFoundException("External resource " + externalFilename + " could not be found");
                  }
                }
                else {
                  throw new FileNotFoundException("External resource " + externalFilename + " could not be found");
                }
              }
              else if (unityFS) {
                // UnityFS files contain the file data after the end of the unity data
                externalArchive = path;
              }

              if (unityFS) {
                extOffset += relativeDataOffset;
              }

              // Now check the offsets and sizes
              long externalArcSize = externalArchive.length();

              FieldValidator.checkOffset(extOffset, externalArcSize);
              FieldValidator.checkLength(extSize, externalArcSize);

              // Now that we reached the end successfully, set the externalArchive on the Resource (the other fields set down further)
              resource.setSource(externalArchive);
              realOffset = extOffset;
              realSize = extSize;

              // Convert the Resource into a Resource_Unity3D_TEX
              Resource oldResource = resource;
              resource = new Resource_Unity3D_TEX();
              resource.copyFrom(oldResource); // copy the data from the old Resource to the new Resource
              resources[i] = resource; // stick the new Resource in the array, overwriting the old Resource

              // Set the image-specific properties on the new Resource
              Resource_Unity3D_TEX castResource = (Resource_Unity3D_TEX) resource;
              castResource.setImageWidth(imageWidth);
              castResource.setImageHeight(imageHeight);
              castResource.setFormatCode(imageFormat);
              castResource.setMipmapCount(mipmapCount);

              //System.out.println("Setting External Resource " + externalFilename + " on Resource " + resource.getName());
            }
          }
          catch (Throwable t) {
            // not in an external archive, or some other error
            ErrorLogger.log(t);
          }
        }
        else if (fileType.equals(".Cubemap")) {
          try {
            // 4 - Unknown (32)
            // 4 - Unknown (32)
            // 4 - Unknown (1392)
            // 4 - Unknown (24)
            // 4 - Unknown (6)
            // 4 - null
            // 4 - Unknown (6)
            // 4 - Unknown (2)
            // 4 - Unknown (2)
            // 4 - null
            // 4 - null
            // 4 - Unknown (1)
            // 4 - null
            // 4 - null
            // 4 - null
            fm.skip(60);

            // 4 - File Offset
            int extOffset = fm.readInt();

            // 4 - File Length
            int extSize = fm.readInt();

            // 4 - External Archive Filename Length
            int externalFilenameLength = fm.readInt();
            FieldValidator.checkFilenameLength(externalFilenameLength);

            // X - External Archive Filename
            String externalFilename = fm.readString(externalFilenameLength);
            FieldValidator.checkFilename(externalFilename);

            // Check that the filename is a valid File in the FileSystem
            File externalArchive = new File(resource.getSource().getParent() + File.separatorChar + externalFilename);
            if (!externalArchive.exists()) {
              throw new FileNotFoundException("External resource " + externalFilename + " could not be found");
            }

            long externalArcSize = externalArchive.length();

            // Now that we reached the end successfully, set the externalArchive on the Resource (the other fields set down further)
            FieldValidator.checkOffset(extOffset, externalArcSize);
            FieldValidator.checkLength(extSize, externalArcSize);

            resource.setSource(externalArchive);
            realOffset = extOffset;
            realSize = extSize;

            //System.out.println("Setting External Resource " + externalFilename + " on Resource " + resource.getName());
          }
          catch (Throwable t) {
            // not in an external archive, or some other error
            ErrorLogger.log(t);
          }
        }

        resource.setOffset(realOffset);
        resource.setLength(realSize);
        resource.setDecompressedLength(realSize);
        TaskProgressManager.setValue(i);
      }

      //
      // In this loop...
      // * Go through all the Type 1 Resources and use them to set the folder names on the referenced Resources
      //
      TaskProgressManager.setValue(0);

      if (!unityFS && !forcedUnityFS) {
        for (int r = 0; r < numType1Resources; r++) {
          Resource resource = type1Resources[r];

          // Go to the data offset
          long offset = resource.getOffset();
          fm.seek(offset);

          // 4 - Number of Referenced Files
          int numRefFiles = fm.readInt();
          FieldValidator.checkNumFiles(numRefFiles);

          // for each referenced file...
          Resource[] refResources = new Resource[numRefFiles];
          for (int i = 0; i < numRefFiles; i++) {
            // 4 - null
            fm.skip(4);

            // 4 - File ID of Referenced File
            int refFileID = fm.readInt() - 1; // -1 because fileID numbers start at 1, not 0
            FieldValidator.checkRange(refFileID, 0, numFiles);
            refResources[i] = resources[refFileID];

            // 4 - null
            fm.skip(4);
          }

          // 4 - null
          fm.skip(4);

          // 4 - Folder Name Length
          int folderNameLength = fm.readInt();
          if (folderNameLength != 0) {
            FieldValidator.checkFilenameLength(folderNameLength);

            // X - Folder Name
            String folderName = fm.readString(folderNameLength);
            FieldValidator.checkFilename(folderName);

            // Set the folder name for each referenced file
            for (int i = 0; i < numRefFiles; i++) {
              Resource refResource = refResources[i];

              // Otherwise for other refType files (and for the ref files as well), set the folder name on the file itself.
              //System.out.println("Setting folder name " + folderName + " on Resource " + refResource.getName());
              refResource.setName(folderName + "\\" + refResource.getName());
            }
          }

          TaskProgressManager.setValue(r);
        }
      }

      //
      // In this loop...
      // * Remove all the Type1 Resources from the File List --> they're not real files
      // * Clear all the renames/replaced flags on the Resource, caused by setting the name, changing the file size, etc.
      // * Set the forceNotAdded flag on the Resources to override the "added" icons
      //
      int realNumFiles = numFiles - numType1Resources;
      Resource[] realResources = new Resource[realNumFiles];
      int realArrayPos = 0;
      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];
        if (resource.getExtension().equals("GameObject")) {
          continue;
        }

        resource.setReplaced(false);
        resource.setOriginalName(resource.getName());
        resource.forceNotAdded(true);

        realResources[realArrayPos] = resource;
        realArrayPos++;
      }

      //  TEMP ONLY. If we uncomment the above, we need to remove this line...
      //Resource[] realResources = resources;

      /*
      // FOR ANALYSIS, READ THROUGH ALL THE *.1, *.4, *.114 FILES AND PRINT OUT THEIR DETAILS
      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];
        String extension = resource.getExtension();
        if (extension.equals("1")) {
          //analyse1File(resource, fm, resources);
        }
        else if (extension.equals("4")) {
          //analyse4File(resource, fm, resources);
        }
        else if (extension.equals("23")) {
          //analyse23File(resource, fm, resources);
        }
        else if (extension.equals("33")) {
          //analyse33File(resource, fm, resources);
        }
        else if (extension.equals("114")) {
          //analyse114File(resource, fm, resources);
        }
      }
      */

      fm.close();

      //return resources;
      return realResources;

    }
    catch (Throwable t) {
      logError(t);
      return null;
    }
  }

}
