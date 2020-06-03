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
public class Plugin_ERF_ERFV10 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_ERF_ERFV10() {

    super("ERF_ERFV10", "ERF_ERFV10");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Star Wars: Knights Of The Old Republic",
        "Star Wars: Knights Of The Old Republic 2: The Sith Lords");
    setExtensions("erf"); // MUST BE LOWER CASE
    setPlatforms("PC");

    // MUST BE LOWER CASE !!!
    setFileTypes(new FileType("1da", "1D Array", FileType.TYPE_OTHER),
        new FileType("2da", "2D Array", FileType.TYPE_OTHER),
        new FileType("4pc", "Texture", FileType.TYPE_IMAGE),
        new FileType("amp", "Brightening Control", FileType.TYPE_OTHER),
        new FileType("are", "Area", FileType.TYPE_OTHER),
        new FileType("art", "Area Environment Settings", FileType.TYPE_OTHER),
        new FileType("bic", "Character", FileType.TYPE_OTHER),
        new FileType("bif", "Game Resource Data", FileType.TYPE_OTHER),
        new FileType("bip", "Lipsync", FileType.TYPE_OTHER),
        new FileType("bmu", "MP3 Audio with Header", FileType.TYPE_AUDIO),
        new FileType("btc", "Creature Blueprint", FileType.TYPE_OTHER),
        new FileType("btd", "Door Blueprint", FileType.TYPE_OTHER),
        new FileType("bte", "Encounter Blueprint", FileType.TYPE_OTHER),
        new FileType("btg", "Random Item Generator Blueprint", FileType.TYPE_OTHER),
        new FileType("bti", "Item Blueprint", FileType.TYPE_OTHER),
        new FileType("btm", "Merchant Blueprint", FileType.TYPE_OTHER),
        new FileType("btp", "Placeable Object Blueprint", FileType.TYPE_OTHER),
        new FileType("bts", "Sound Blueprint", FileType.TYPE_OTHER),
        new FileType("btt", "Trigger Blueprint", FileType.TYPE_OTHER),
        new FileType("btw", "Waypoint Blueprint", FileType.TYPE_OTHER),
        new FileType("cam", "Campaign", FileType.TYPE_OTHER),
        new FileType("ccs", "NWScript Compiled Script", FileType.TYPE_OTHER),
        new FileType("css", "NWScript Source", FileType.TYPE_OTHER),
        new FileType("cut", "Cutscene", FileType.TYPE_OTHER),
        new FileType("cwa", "Crowd Attributes", FileType.TYPE_OTHER),
        new FileType("dft", "Default Values", FileType.TYPE_DOCUMENT),
        new FileType("dlg", "Conversation", FileType.TYPE_OTHER),
        new FileType("dtf", "Default Values", FileType.TYPE_OTHER),
        new FileType("dwk", "Door Walkmesh", FileType.TYPE_OTHER),
        new FileType("erf", "Module Resources", FileType.TYPE_OTHER),
        new FileType("fac", "Faction", FileType.TYPE_OTHER),
        new FileType("fnt", "Font", FileType.TYPE_OTHER),
        new FileType("fsm", "Finite State Machine", FileType.TYPE_OTHER),
        new FileType("gff", "Generic File Format", FileType.TYPE_OTHER),
        new FileType("gic", "Game Instance Comments", FileType.TYPE_OTHER),
        new FileType("git", "Game Instance File", FileType.TYPE_OTHER),
        new FileType("gui", "Graphical User Interface", FileType.TYPE_OTHER),
        new FileType("hak", "Resource Archive", FileType.TYPE_OTHER),
        new FileType("hex", "Hex Grid", FileType.TYPE_OTHER),
        new FileType("ifo", "Module Info File", FileType.TYPE_OTHER),
        new FileType("itp", "Tile Palette File", FileType.TYPE_OTHER),
        new FileType("jrl", "Journal", FileType.TYPE_OTHER),
        new FileType("ka", "Karma", FileType.TYPE_OTHER),
        new FileType("key", "Game Resource Index", FileType.TYPE_OTHER),
        new FileType("lip", "Lipsync", FileType.TYPE_OTHER),
        new FileType("ltr", "Letter-Combo Probability", FileType.TYPE_OTHER),
        new FileType("lua", "LUA Script", FileType.TYPE_OTHER),
        new FileType("luc", "Compiled LUA Script", FileType.TYPE_OTHER),
        new FileType("lyt", "Room Layout", FileType.TYPE_OTHER),
        new FileType("mab", "Binary Material", FileType.TYPE_OTHER),
        new FileType("mat", "Material", FileType.TYPE_OTHER),
        new FileType("mdb", "Geometry Model", FileType.TYPE_OTHER),
        new FileType("mdl", "Model", FileType.TYPE_OTHER),
        new FileType("mdx", "Geometry Model Mesh", FileType.TYPE_OTHER),
        new FileType("mdx2", "Geometry Model Mesh", FileType.TYPE_OTHER),
        new FileType("mod", "Module", FileType.TYPE_OTHER),
        new FileType("mve", "Video", FileType.TYPE_VIDEO),
        new FileType("ncs", "NWScript Compiled Script", FileType.TYPE_OTHER),
        new FileType("ndb", "Script Debugger File", FileType.TYPE_OTHER),
        new FileType("nss", "NWScript Source", FileType.TYPE_DOCUMENT),
        new FileType("plt", "Packed Layer Texture", FileType.TYPE_IMAGE),
        new FileType("pth", "Path Finder", FileType.TYPE_OTHER),
        new FileType("ptm", "Plot Manager", FileType.TYPE_OTHER),
        new FileType("ptt", "Plot Wizard Blueprint", FileType.TYPE_OTHER),
        new FileType("pwk", "Placeable Object Walkmesh", FileType.TYPE_OTHER),
        new FileType("qdb", "Quest Database", FileType.TYPE_OTHER),
        new FileType("qst", "Quest", FileType.TYPE_OTHER),
        new FileType("qst2", "Quest", FileType.TYPE_OTHER),
        new FileType("res", "Generic File Format", FileType.TYPE_OTHER),
        new FileType("rim", "Module Resources", FileType.TYPE_OTHER),
        new FileType("sav", "Game Save", FileType.TYPE_OTHER),
        new FileType("set", "Tileset", FileType.TYPE_IMAGE),
        new FileType("spt", "SpeedTree", FileType.TYPE_OTHER),
        new FileType("ssf", "Sound Set File", FileType.TYPE_OTHER),
        new FileType("tex", "Texture", FileType.TYPE_IMAGE),
        new FileType("tlk", "Talk Table", FileType.TYPE_OTHER),
        new FileType("tpc", "Texture", FileType.TYPE_IMAGE),
        new FileType("ttf", "True Type Font", FileType.TYPE_OTHER),
        new FileType("txb", "Texture", FileType.TYPE_IMAGE),
        new FileType("txb2", "Texture", FileType.TYPE_IMAGE),
        new FileType("txi", "Texture Info", FileType.TYPE_OTHER),
        new FileType("utc", "Creature Blueprint", FileType.TYPE_OTHER),
        new FileType("utd", "Door Blueprint", FileType.TYPE_OTHER),
        new FileType("ute", "Encounter Blueprint", FileType.TYPE_OTHER),
        new FileType("utg", "Random Item Generator Blueprint", FileType.TYPE_OTHER),
        new FileType("uti", "Item Blueprint", FileType.TYPE_OTHER),
        new FileType("utm", "Merchant Blueprint", FileType.TYPE_OTHER),
        new FileType("utp", "Placeable Object Blueprint", FileType.TYPE_OTHER),
        new FileType("uts", "Sound Blueprint", FileType.TYPE_OTHER),
        new FileType("utt", "Trigger Blueprint", FileType.TYPE_OTHER),
        new FileType("utw", "Waypoint Blueprint", FileType.TYPE_OTHER),
        new FileType("vis", "Room Visibilities", FileType.TYPE_OTHER),
        new FileType("wfx", "Woot Effect", FileType.TYPE_OTHER),
        new FileType("wok", "Walkmesh", FileType.TYPE_OTHER),
        new FileType("xmv", "XBox Video", FileType.TYPE_VIDEO));

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

      // 8 - Header ("ERF V1.0")
      if (fm.readString(8).equals("ERF V1.0")) {
        rating += 50;
      }

      fm.skip(8);

      // 4 - Number of Files
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
   Provide hints to the previewer so that certain document types are displayed appropriately
   **********************************************************************************************
   **/
  @Override
  public ViewerPlugin previewHint(Resource resource) {
    String extension = resource.getExtension();
    if (extension.equalsIgnoreCase("cns") || extension.equalsIgnoreCase("mao") || extension.equalsIgnoreCase("nss") || extension.equalsIgnoreCase("ldf")) {
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

      // 8 - Header ("ERF V1.0")
      // 8 - null
      fm.skip(16);

      // 4 - Number of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // 4 - Filename Directory Offset (160)
      int filenameDirOffset = fm.readInt();
      FieldValidator.checkOffset(filenameDirOffset, arcSize);

      // 4 - Filename Directory Offset (160)
      fm.skip(4);

      // 4 - Details Directory Offset
      int dirOffset = fm.readInt();
      FieldValidator.checkOffset(dirOffset, arcSize);

      // 4 - Unknown (103)
      // 4 - Unknown (309)
      // 120 - null
      fm.seek(filenameDirOffset);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      String[] names = new String[numFiles];
      for (int i = 0; i < numFiles; i++) {
        // 16 - Filename (null terminated, filled with nulls)
        String filename = fm.readNullString(16);
        FieldValidator.checkFilename(filename);

        // 4 - File ID (incremental from 0)
        fm.skip(4);

        // 2 - File Type
        short fileType = fm.readShort();
        if (fileType == 1) {
          filename += ".bmp";
        }
        else if (fileType == 2) {
          filename += ".mve";
        }
        else if (fileType == 3) {
          filename += ".tga";
        }
        else if (fileType == 4) {
          filename += ".wav";
        }
        else if (fileType == 6) {
          filename += ".plt";
        }
        else if (fileType == 7) {
          filename += ".ini";
        }
        else if (fileType == 8) {
          filename += ".bmu";
        }
        else if (fileType == 9) {
          filename += ".mpg";
        }
        else if (fileType == 10) {
          filename += ".txt";
        }
        else if (fileType == 11) {
          filename += ".wma";
        }
        else if (fileType == 12) {
          filename += ".wmv";
        }
        else if (fileType == 13) {
          filename += ".xmv";
        }
        else if (fileType == 2000) {
          filename += ".plh";
        }
        else if (fileType == 2001) {
          filename += ".tex";
        }
        else if (fileType == 2002) {
          filename += ".mdl";
        }
        else if (fileType == 2003) {
          filename += ".thg";
        }
        else if (fileType == 2005) {
          filename += ".fnt";
        }
        else if (fileType == 2007) {
          filename += ".lua";
        }
        else if (fileType == 2008) {
          filename += ".slt";
        }
        else if (fileType == 2009) {
          filename += ".nss";
        }
        else if (fileType == 2010) {
          filename += ".ncs";
        }
        else if (fileType == 2011) {
          filename += ".mod";
        }
        else if (fileType == 2012) {
          filename += ".are";
        }
        else if (fileType == 2013) {
          filename += ".set";
        }
        else if (fileType == 2014) {
          filename += ".ifo";
        }
        else if (fileType == 2015) {
          filename += ".bic";
        }
        else if (fileType == 2016) {
          filename += ".wok";
        }
        else if (fileType == 2017) {
          filename += ".2da";
        }
        else if (fileType == 2018) {
          filename += ".tlk";
        }
        else if (fileType == 2022) {
          filename += ".txi";
        }
        else if (fileType == 2023) {
          filename += ".git";
        }
        else if (fileType == 2024) {
          filename += ".bti";
        }
        else if (fileType == 2025) {
          filename += ".uti";
        }
        else if (fileType == 2026) {
          filename += ".btc";
        }
        else if (fileType == 2027) {
          filename += ".utc";
        }
        else if (fileType == 2029) {
          filename += ".dlg";
        }
        else if (fileType == 2030) {
          filename += ".itp";
        }
        else if (fileType == 2031) {
          filename += ".btt";
        }
        else if (fileType == 2032) {
          filename += ".utt";
        }
        else if (fileType == 2033) {
          filename += ".dds";
        }
        else if (fileType == 2034) {
          filename += ".bts";
        }
        else if (fileType == 2035) {
          filename += ".uts";
        }
        else if (fileType == 2036) {
          filename += ".ltr";
        }
        else if (fileType == 2037) {
          filename += ".gff";
        }
        else if (fileType == 2038) {
          filename += ".fac";
        }
        else if (fileType == 2039) {
          filename += ".bte";
        }
        else if (fileType == 2040) {
          filename += ".ute";
        }
        else if (fileType == 2041) {
          filename += ".btd";
        }
        else if (fileType == 2042) {
          filename += ".utd";
        }
        else if (fileType == 2043) {
          filename += ".btp";
        }
        else if (fileType == 2044) {
          filename += ".utp";
        }
        else if (fileType == 2045) {
          filename += ".dft";
        }
        else if (fileType == 2046) {
          filename += ".gic";
        }
        else if (fileType == 2047) {
          filename += ".gui";
        }
        else if (fileType == 2048) {
          filename += ".css";
        }
        else if (fileType == 2049) {
          filename += ".ccs";
        }
        else if (fileType == 2050) {
          filename += ".btm";
        }
        else if (fileType == 2051) {
          filename += ".utm";
        }
        else if (fileType == 2052) {
          filename += ".dwk";
        }
        else if (fileType == 2053) {
          filename += ".pwk";
        }
        else if (fileType == 2054) {
          filename += ".btg";
        }
        else if (fileType == 2055) {
          filename += ".utg";
        }
        else if (fileType == 2056) {
          filename += ".jrl";
        }
        else if (fileType == 2057) {
          filename += ".sav";
        }
        else if (fileType == 2058) {
          filename += ".utw";
        }
        else if (fileType == 2059) {
          filename += ".4pc";
        }
        else if (fileType == 2060) {
          filename += ".ssf";
        }
        else if (fileType == 2061) {
          filename += ".hak";
        }
        else if (fileType == 2062) {
          filename += ".nwm";
        }
        else if (fileType == 2063) {
          filename += ".bik";
        }
        else if (fileType == 2064) {
          filename += ".ndb";
        }
        else if (fileType == 2065) {
          filename += ".ptm";
        }
        else if (fileType == 2066) {
          filename += ".ptt";
        }
        else if (fileType == 2067) {
          filename += ".ncm";
        }
        else if (fileType == 2068) {
          filename += ".mfx";
        }
        else if (fileType == 2069) {
          filename += ".mat";
        }
        else if (fileType == 2070) {
          filename += ".mdb";
        }
        else if (fileType == 2071) {
          filename += ".say";
        }
        else if (fileType == 2072) {
          filename += ".ttf";
        }
        else if (fileType == 2073) {
          filename += ".ttc";
        }
        else if (fileType == 2074) {
          filename += ".cut";
        }
        else if (fileType == 2075) {
          filename += ".ka";
        }
        else if (fileType == 2076) {
          filename += ".jpg";
        }
        else if (fileType == 2077) {
          filename += ".ico";
        }
        else if (fileType == 2078) {
          filename += ".ogg";
        }
        else if (fileType == 2079) {
          filename += ".spt";
        }
        else if (fileType == 2080) {
          filename += ".spw";
        }
        else if (fileType == 2081) {
          filename += ".wfx";
        }
        else if (fileType == 2082) {
          filename += ".ugm";
        }
        else if (fileType == 2083) {
          filename += ".qdb";
        }
        else if (fileType == 2084) {
          filename += ".qst";
        }
        else if (fileType == 2085) {
          filename += ".npc";
        }
        else if (fileType == 2086) {
          filename += ".cpn";
        }
        else if (fileType == 2087) {
          filename += ".utx";
        }
        else if (fileType == 2088) {
          filename += ".mmd";
        }
        else if (fileType == 2089) {
          filename += ".smm";
        }
        else if (fileType == 2090) {
          filename += ".uta";
        }
        else if (fileType == 2091) {
          filename += ".mde";
        }
        else if (fileType == 2092) {
          filename += ".mdv";
        }
        else if (fileType == 2093) {
          filename += ".mda";
        }
        else if (fileType == 2094) {
          filename += ".mba";
        }
        else if (fileType == 2095) {
          filename += ".oct";
        }
        else if (fileType == 2096) {
          filename += ".bfx";
        }
        else if (fileType == 2097) {
          filename += ".pdb";
        }
        else if (fileType == 2099) {
          filename += ".pvs";
        }
        else if (fileType == 2100) {
          filename += ".cfx";
        }
        else if (fileType == 2101) {
          filename += ".luc";
        }
        else if (fileType == 2103) {
          filename += ".prb";
        }
        else if (fileType == 2104) {
          filename += ".cam";
        }
        else if (fileType == 2105) {
          filename += ".vds";
        }
        else if (fileType == 2106) {
          filename += ".bin";
        }
        else if (fileType == 2107) {
          filename += ".wob";
        }
        else if (fileType == 2108) {
          filename += ".api";
        }
        else if (fileType == 2110) {
          filename += ".png";
        }
        else if (fileType == 3000) {
          filename += ".lyt";
        }
        else if (fileType == 3001) {
          filename += ".vis";
        }
        else if (fileType == 3002) {
          filename += ".rim";
        }
        else if (fileType == 3003) {
          filename += ".pth";
        }
        else if (fileType == 3004) {
          filename += ".lip";
        }
        else if (fileType == 3005) {
          filename += ".bwm";
        }
        else if (fileType == 3006) {
          filename += ".txb";
        }
        else if (fileType == 3007) {
          filename += ".tpc";
        }
        else if (fileType == 3008) {
          filename += ".mdx";
        }
        else if (fileType == 3009) {
          filename += ".rsv";
        }
        else if (fileType == 3010) {
          filename += ".sig";
        }
        else if (fileType == 3011) {
          filename += ".mab";
        }
        else if (fileType == 3012) {
          filename += ".qst2";
        }
        else if (fileType == 3013) {
          filename += ".sto";
        }
        else if (fileType == 3015) {
          filename += ".hex";
        }
        else if (fileType == 3016) {
          filename += ".mdx2";
        }
        else if (fileType == 3017) {
          filename += ".txb2";
        }
        else if (fileType == 3022) {
          filename += ".fsm";
        }
        else if (fileType == 3023) {
          filename += ".art";
        }
        else if (fileType == 3024) {
          filename += ".amp";
        }
        else if (fileType == 3025) {
          filename += ".cwa";
        }
        else if (fileType == 3028) {
          filename += ".bip";
        }
        else if (fileType == 4000) {
          filename += ".mdb2";
        }
        else if (fileType == 4001) {
          filename += ".mda2";
        }
        else if (fileType == 4002) {
          filename += ".spt2";
        }
        else if (fileType == 4003) {
          filename += ".gr2";
        }
        else if (fileType == 4004) {
          filename += ".fxa";
        }
        else if (fileType == 4005) {
          filename += ".fxe";
        }
        else if (fileType == 4007) {
          filename += ".jpg2";
        }
        else if (fileType == 4008) {
          filename += ".pwc";
        }
        else if (fileType == 9996) {
          filename += ".1da";
        }
        else if (fileType == 9997) {
          filename += ".erf";
        }
        else if (fileType == 9998) {
          filename += ".bif";
        }
        else if (fileType == 9999) {
          filename += ".key";
        }
        else {
          filename += "." + fileType;
        }

        names[i] = filename;

        // 2 - null
        fm.skip(2);
      }

      fm.seek(dirOffset);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {
        // 4 - File Offset
        int offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - File Length
        int length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        String filename = names[i];

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

}
