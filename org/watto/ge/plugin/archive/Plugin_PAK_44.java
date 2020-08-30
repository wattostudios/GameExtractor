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
import org.watto.component.WSPluginManager;
import org.watto.datatype.FileType;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.io.FileManipulator;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_PAK_44 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_PAK_44() {

    super("PAK_44", "Node-Webkit Archive");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("15 Seconds",
        "80's Style",
        "A Salem Witch Trial",
        "Absoloot",
        "Adva-lines",
        "Airscape: The Fall of Gravity",
        "Among the Dead",
        "BalanCity",
        "BLOK DROP NEO",
        "Bloodbath Kavkaz",
        "Brute",
        "Bye-Bye, Wacky Planet",
        "Camp Sunshine",
        "Carnage in Space: Ignition",
        "Cat Doesn't Like Banana",
        "Cave Adventures",
        "Crappy Day",
        "Dangerous Skies: 80's Edition",
        "Days Under Custody",
        "Don't Tax Me, Bro!",
        "Donut Hunter",
        "Dragon Boar and Lady Rabbit",
        "Dreaming Sarah",
        "Evil Come",
        "Fairyland: Blackberry Warrior",
        "Fairyland: Chronicle",
        "Fairyland: Power Dice",
        "Frank the Miner",
        "Freezeer",
        "Fruit Crush",
        "G-Dino's Jungle Adventure",
        "Gardener The Ripper",
        "Gone Fireflies",
        "Grave Prosperity: Redux Part 1",
        "Grimm & Tonic",
        "Guinea-Pig",
        "Happy Santa",
        "Hellphobia",
        "Hexopods",
        "In The Fighting",
        "Injured By Space",
        "Inner Space",
        "Invasion of Barbarians",
        "Inverted",
        "Last Fort",
        "Leon's Crusade",
        "Life Beetle",
        "Liveza: Death of the Earth",
        "Lost in the Forest",
        "Lost with Dinosaurs",
        "Lucky Panda",
        "Magic and Challenge RPG",
        "Megatronic Void",
        "Mountain Troll",
        "Murazu",
        "My Personal Angel",
        "NeonGalaxy Wars",
        "Next Up Hero",
        "Ochkarik",
        "One Star",
        "OneScreen Solar Sails",
        "Pilferer",
        "Pixel War",
        "President Pig",
        "Rainbow Rage Squad",
        "Reflection of Mine",
        "RKN: Roskomnadzor Banned the Internet",
        "Samoliotik",
        "Sentience: The Android's Tale",
        "Slay.one",
        "Sliver-Sclicker",
        "Smart Mummy",
        "Space Girls",
        "Squeezone",
        "SUPER ROBO MOUSE",
        "The Last Vampire",
        "The Lost Wizard",
        "The Next Penelope",
        "The Theodore Adventures",
        "Thick Light",
        "Uriel's Chasm 2",
        "V Nekotorom Tsarstve",
        "Weird Dungeon Explorer: Run Away",
        "Weird Dungeon Explorer: Wave Beat",
        "World of Tea",
        "Wunderwaffe",
        "ZAMBI 2 KIL");
    setExtensions("pak"); // MUST BE LOWER CASE
    setPlatforms("PC");

    setCanScanForFileTypes(true);

    // MUST BE LOWER CASE !!!
    setFileTypes(new FileType("woff2", "Web Open Font", FileType.TYPE_OTHER),
        new FileType("pexe", "Portable Executable", FileType.TYPE_OTHER));

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

      // 4 - Version? (4)
      if (fm.readInt() == 4) {
        rating += 5;
      }

      // 4 - Number of Files
      if (FieldValidator.checkNumFiles(fm.readInt())) {
        rating += 5;
      }

      // 1 - Unknown (1)
      if (fm.readByte() == 1) {
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
  If an archive doesn't have filenames stored in it, the scanner can come here to try to work out
  what kind of file a Resource is. This method allows the plugin to provide additional plugin-specific
  extensions, which will be tried before any standard extensions.
  @return null if no extension can be determined, or the extension if one can be found
  **********************************************************************************************
  **/
  @Override
  public String guessFileExtension(Resource resource, byte[] headerBytes, int headerInt1, int headerInt2, int headerInt3, short headerShort1, short headerShort2, short headerShort3, short headerShort4, short headerShort5, short headerShort6) {

    if (headerInt1 == 2037149520) {
      return "js";
    }
    else if (headerInt1 == 843468663) {
      return "woff2";
    }
    else if (headerInt1 == 1163412816) {
      return "pexe";
    }

    int headerByte1 = headerBytes[0];

    if (headerByte1 == 123 || headerByte1 == 60 || headerByte1 == 47 || headerByte1 == 64 || headerByte1 == 10 || headerByte1 == 40 || headerByte1 == 34 || headerByte1 == 39) { // { or < or / or @ or <space> or ( or " or '
      return "txt";
    }
    else if (new String(new byte[] { headerBytes[0], headerBytes[1], headerBytes[2] }).equals("var")) {
      return "txt";
    }

    return null;
  }

  /**
   **********************************************************************************************
   Provide hints to the previewer so that certain document types are displayed appropriately
   **********************************************************************************************
   **/
  @Override
  public ViewerPlugin previewHint(Resource resource) {
    String extension = resource.getExtension();
    if (extension.equalsIgnoreCase("js")) {
      return (ViewerPlugin) WSPluginManager.getPlugin("Viewer", "TXT");
    }
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

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 4 - Version? (4)
      fm.skip(4);

      // 4 - Number of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // 1 - Unknown (1)
      fm.skip(1);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {
        // 2 - File ID?
        fm.skip(2);

        // 4 - File Offset
        int offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        String filename = Resource.generateFilename(i);

        //path,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, filename, offset);

        TaskProgressManager.setValue(i);
      }

      calculateFileSizes(resources, arcSize);

      fm.close();

      return resources;

    }
    catch (Throwable t) {
      logError(t);
      return null;
    }
  }

}
