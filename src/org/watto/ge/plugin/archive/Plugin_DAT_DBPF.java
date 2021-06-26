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
import org.watto.Settings;
import org.watto.component.WSPluginManager;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.ge.plugin.exporter.Exporter_Default;
import org.watto.ge.plugin.exporter.Exporter_REFPACK;
import org.watto.io.FileManipulator;
import org.watto.io.converter.IntConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_DAT_DBPF extends ArchivePlugin {

  boolean debugMode = false;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_DAT_DBPF() {

    super("DAT_DBPF", "Maxis DBPF - Original");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("The Sims Online",
        "Simcity 4",
        "The Sims 2");
    setExtensions("dat", "package", "sc4lot", "sc4");
    setPlatforms("PC");

    // The Sims 2
    setFileTypes("2ary", "2D Array",
        "3ary", "3D Array",
        "3dmd", "3D Model",
        "5al", "Ambient Light",
        "5an", "Animation",
        "5cr", "Base Object File",
        "5cs", "Cinematic Scene",
        "5dl", "Directional Light",
        "5ds", "Draw State Lighting",
        "5el", "Environment Cube Lighting",
        "5gd", "Geometric Data Container",
        "5gn", "Geometric Node",
        "5lf", "Linear Fog Lighting",
        "5pl", "Point Light",
        "5sc", "Scene Node",
        "5sh", "Shape",
        "5sl", "Spotlight",
        "5tm", "Material Definitions",
        "6li", "Level Information",
        "6tx", "Texture Resource",
        "ab", "Audio Bank",
        "atc", "Animation Texture Collection",
        "avp", "Animation Viewpoints",
        "bcon", "Simantics Behaviour Constant",
        "bhav", "Simantics Behaviour Script",
        "cats", "Catalog String",
        "cige", "Image Link",
        "cqzb", "CQZB File",
        "ctss", "Catalog String Set",
        "cursor", "Cursor",
        "dat", "DAT Archive",
        "dgrp", "Drawgroup",
        "dir", "DBDF Directory",
        "ealt", "Edge Altitude",
        "eqz", "Sound Equilizer",
        "eqzb", "EQZB File",
        "face", "Face Properties",
        "famh", "Family Data",
        "fami", "Family Information",
        "famt", "Family Ties",
        "fcns", "Global Tuning Values",
        "fnt", "Settings",
        "fpst", "Fence Post Layer",
        "fwav", "Audio Reference",
        "fx", "Effects Resource Tree",
        "glob", "Semi-Globals",
        "hit", "Hit List",
        "hous", "House Data",
        "inf", "File Information",
        "kbd", "Keyboard Accelerators",
        "lot", "Lot Definition",
        "ltxt", "Lot or Tutorial Description",
        "lua", "LUA Script",
        "lxnr", "Facial Structure",
        "mad", "Mad Video",
        "matshad", "Maxis Material Shader",
        "mobjt", "Main Lot Objects",
        "ngbh", "Neighborhood Data",
        "nhtr", "Neighborhood Terrain",
        "nhvw", "Neighborhood View",
        "nid", "Neighborhood ID",
        "nmap", "Name Map",
        "nref", "Name Reference",
        "objd", "Object Data",
        "objf", "Object Functions",
        "objm", "Object Metadata",
        "objt", "Singular Lot Object",
        "palt", "Image Color Palette",
        "pdat", "Sim Data and Information",
        "pers", "Current Sim Status",
        "pmap", "Predictive Map",
        "pool", "Pool Surface",
        "pops", "Popup Tracker",
        "posi", "Edith Stack Script",
        "ptbp", "Package Toolkit",
        "pth", "Simcity 4 Path",
        "roof", "Roof",
        "rul", "Rules",
        "set", "Sound Settings",
        "sfx", "Sound Effects",
        "shpi", "SHPI Image",
        "shpid", "SHPI Data",
        "simi", "Sim Information",
        "skin", "Sim Skins and Clothing",
        "slot", "Object Slot",
        "smap", "String Map",
        "spr2", "Image Sprites",
        "spx", "Speex Audio",
        "srel", "Sim Relations",
        "ss", "Sound Script",
        "str", "Text Strings",
        "stxr", "Surface Texture",
        "tatt", "Unknown",
        "tkd", "TKDT File",
        "tkd2", "TKD2 File",
        "tkd2d", "TKD2 Data",
        "tmap", "Lot or Terrain Texture Map",
        "tmp", "Temporary File",
        "tprp", "Edith Simantics Behaviour Labels",
        "trcn", "BCON Labels",
        "tree", "Edith Flowchart Trees",
        "trk", "Track Definitions",
        "tssg", "TSSG System",
        "ttab", "Pie Menu Functions",
        "ttas", "Pie Menu Strings",
        "ui", "User Interface",
        "utm", "UTM Audio",
        "utxt", "Unicode Text",
        "vert", "Vertex Layer",
        "wfr", "Wants and Fears",
        "wgra", "Wall Graph",
        "wlay", "Wall Layer",
        "wrld", "World Database",
        "xa", "XA Audio",
        "xmto", "Material Object",
        "xobj", "Unknown");

  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public String determineExtension(int typeID) {

    if (typeID == 0) {
      return ".ui";
    }
    else if (typeID == 170413323) {
      return ".wgra";
    }
    else if (typeID == 200907239) {
      return ".ltxt";
    }
    else if (typeID == 209623670) {
      return ".jpg";
    }
    else if (typeID == 210767835) {
      return ".pool";
    }
    else if (typeID == 474621804) {
      return ".6tx";
    }
    else if (typeID == 539399691) {
      return ".mp3";
    }
    else if (typeID == 623061777) {
      return ".5sc";
    }
    else if (typeID == 709957403) {
      return ".3ary";
    }
    else if (typeID == 741412678) {
      return ".pops";
    }
    else if (typeID == 1111707470) {
      return ".bcon";
    }
    else if (typeID == 1112031574) {
      return ".bhav";
    }
    else if (typeID == 1112363103) {
      return ".bmp";
    }
    else if (typeID == 1128354899) {
      return ".cats";
    }
    else if (typeID == 1128875845) {
      return ".cige";
    }
    else if (typeID == 1129599827) {
      return ".ctss";
    }
    else if (typeID == 1145524816) {
      return ".dgrp";
    }
    else if (typeID == 1178682181) {
      return ".face";
    }
    else if (typeID == 1178684776) {
      return ".famh";
    }
    else if (typeID == 1178684745) {
      return ".fami";
    }
    else if (typeID == 1178816083) {
      return ".fcns";
    }
    else if (typeID == 1180123478) {
      return ".fwav";
    }
    else if (typeID == 1196183362) {
      return ".glob";
    }
    else if (typeID == 1213158739) {
      return ".hous";
    }
    else if (typeID == 1230596472) {
      return ".5tm";
    }
    else if (typeID == 1241480566) {
      return ".wrld";
    }
    else if (typeID == 1264097115) {
      return ".tmap";
    }
    else if (typeID == 1297215554) {
      return ".5cs";
    }
    else if (typeID == 1297301213) {
      return ".jpg";
    }
    else if (typeID == 1313292872) {
      return ".ngbh";
    }
    else if (typeID == 1314014534) {
      return ".nref";
    }
    else if (typeID == 1315791184) {
      return ".nmap";
    }
    else if (typeID == 1329744452) {
      return ".objd";
    }
    else if (typeID == 1329744486) {
      return ".objf";
    }
    else if (typeID == 1331849805) {
      return ".objm";
    }
    else if (typeID == 1346456660) {
      return ".palt";
    }
    else if (typeID == 1346720339) {
      return ".pers";
    }
    else if (typeID == 1347375945) {
      return ".posi";
    }
    else if (typeID == 1347699280) {
      return ".ptbp";
    }
    else if (typeID == 1397312841) {
      return ".simi";
    }
    else if (typeID == 1397509972) {
      return ".slot";
    }
    else if (typeID == 1397772850) {
      return ".spr2";
    }
    else if (typeID == 1398034979) {
      return ".str";
    }
    else if (typeID == 1413567572) {
      return ".tatt";
    }
    else if (typeID == 1414550096) {
      return ".tprp";
    }
    else if (typeID == 1414677326) {
      return ".trcn";
    }
    else if (typeID == 1414677829) {
      return ".tree";
    }
    else if (typeID == 1414807874) {
      return ".ttab";
    }
    else if (typeID == 1414807923) {
      return ".ttas";
    }
    else if (typeID == 1481462863) {
      return ".xmto";
    }
    else if (typeID == 1481589322) {
      return ".xobj";
    }
    else if (typeID == 1788281903) {
      return ".5el";
    }
    else if (typeID == 1804876611) {
      return ".2ary";
    }
    else if (typeID == 1817745187) {
      return ".lot";
    }
    else if (typeID == 1868720756) {
      return ".mobjt";
    }
    else if (typeID == 2074313612) {
      return ".5gn";
    }
    else if (typeID == -2056397908) {
      return ".png";
    }
    else if (typeID == -1971005520) {
      return ".wlay";
    }
    else if (typeID == -1942165158) {
      return ".jpg";
    }
    else if (typeID == -1937307837) {
      return ".famt";
    }
    else if (typeID == -1933532853) {
      return ".pmap";
    }
    else if (typeID == -1917459262) {
      return ".sfx";
    }
    else if (typeID == -1429328133) {
      return ".pdat";
    }
    else if (typeID == -1421105806) {
      return ".fpst";
    }
    else if (typeID == -1416362326) {
      return ".roof";
    }
    else if (typeID == -1412735580) {
      return ".2ary";
    }
    else if (typeID == -1412375453) {
      return ".nhtr";
    }
    else if (typeID == -1408850321) {
      return ".5lf";
    }
    else if (typeID == -1408850314) {
      return ".5ds";
    }
    else if (typeID == -1404074361) {
      return ".5gd";
    }
    else if (typeID == -1404016796) {
      return ".skin";
    }
    else if (typeID == -1400210898) {
      return ".nid";
    }
    else if (typeID == -1394318795) {
      return ".stxr";
    }
    else if (typeID == -1170916127) {
      return ".tssg";
    }
    else if (typeID == -909632613) {
      return ".5al";
    }
    else if (typeID == -909632605) {
      return ".5dl";
    }
    else if (typeID == -909632599) {
      return ".5pl";
    }
    else if (typeID == -909632595) {
      return ".5sl";
    }
    else if (typeID == -893060032) {
      return ".smap";
    }
    else if (typeID == -884766815) {
      return ".vert";
    }
    else if (typeID == -868856790) {
      return ".srel";
    }
    else if (typeID == -858851246) {
      return ".lxnr";
    }
    else if (typeID == -847255430) {
      return ".matshad";
    }
    else if (typeID == -845851506) {
      return ".wfr";
    }
    else if (typeID == -451294925) {
      return ".5cr";
    }
    else if (typeID == -395632913) {
      return ".dir";
    }
    else if (typeID == -363784016) {
      return ".fx";
    }
    else if (typeID == -331039268) {
      return ".nhvw";
    }
    else if (typeID == -313310922) {
      return ".6li";
    }
    else if (typeID == -98813449) {
      return ".objt";
    }
    else if (typeID == -83855074) {
      return ".5an";
    }
    else if (typeID == -59854345) {
      return ".5sh";
    }
    else if (typeID == 1567860241) {
      return ".trk";
    }
    else if (typeID == 87304289) {
      return ".cqzb";
    }
    else if (typeID == 162385269) {
      return ".avp";
    }
    else if (typeID == 176885360) {
      return ".mad";
    }
    else if (typeID == 694581495) {
      return ".pth";
    }
    else if (typeID == 698733036) {
      return ".atc";
    }
    else if (typeID == 1523640343) {
      return ".3dmd";
    }
    else if (typeID == 1697917002) {
      return ".eqzb";
    }
    else if (typeID == 2058686020) {
      return ".shpi";
    }
    else if (typeID == 2065354701) {
      return ".hit";
    }
    else if (typeID == -1977318727) {
      return ".png";
    }
    else if (typeID == -1652986444) {
      return ".trk2";
    }
    else if (typeID == -1562127053) {
      return ".kbd";
    }
    else if (typeID == -1445105676) {
      return ".ealt";
    }
    else if (typeID == -905806117) {
      return ".inf";
    }
    else if (typeID == -899423581) {
      return ".lua";
    }
    else if (typeID == 1784380405) {
      return ".dat"; // DBPF
    }
    else if (typeID == 173789003) {
      return ".rul";
    }
    else if (typeID == -1436798652) {
      return ".cursor";
    }
    else if (typeID == 193823258) {
      return ".trk";
    }

    if (debugMode) {
      return "." + typeID;
    }
    else {
      return ".unknown";
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
      }

      // 4 - Header
      if (fm.readString(4).equals("DBPF")) {
        rating += 50;
      }

      // 4 - Version (1)
      if (fm.readInt() == 1) {
        rating += 5;
      }

      fm.skip(28);

      // Number Of Files
      if (FieldValidator.checkNumFiles(fm.readInt())) {
        rating += 5;
      }

      long arcSize = fm.getLength();

      // Directory Offset
      if (FieldValidator.checkOffset(fm.readInt(), arcSize)) {
        rating += 5;
      }

      // Directory Length
      if (FieldValidator.checkLength(fm.readInt(), arcSize)) {
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
  
  **********************************************************************************************
  **/
  @Override
  @SuppressWarnings({ "static-access" })
  public Resource[] read(File path) {
    try {

      addFileTypes();

      debugMode = Settings.getBoolean("DebugMode");

      //ExporterPlugin exporter = Exporter_Custom_DAT_DBPF.getInstance();
      Exporter_Default exporterDefault = Exporter_Default.getInstance();
      Exporter_REFPACK exporterRefPack = Exporter_REFPACK.getInstance();
      exporterRefPack.setSkipHeaders(true);

      FileManipulator fm = new FileManipulator(path, false);
      long arcSize = fm.getLength();

      // 4 - Header (DBPF)
      // 4 - Version (1)
      // 16 - null
      // 4 - Archive ID?
      // 4 - Archive Type
      // 4 - Unknown (7)
      fm.skip(36);

      // 4 - Number Of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // 4 - Directory Offset
      int dirOffset = fm.readInt();
      FieldValidator.checkOffset(dirOffset, arcSize);

      // 4 - Directory Size
      // 48 - null
      fm.seek(dirOffset);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      int[] typeIDs = new int[numFiles];
      int[] offsets = new int[numFiles];
      int[] lengths = new int[numFiles];
      for (int i = 0; i < numFiles; i++) {
        // 4 - Type ID
        int typeID = fm.readInt();
        typeIDs[i] = typeID;
        //System.out.println((i + 1) + "\t" + typeID);
        // 4 - Group ID
        // 4 - Instance ID
        fm.skip(8);

        // 4 - File Offset
        int offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);
        offsets[i] = offset;

        // 4 - File Length
        int length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);
        lengths[i] = length;

        TaskProgressManager.setValue(i);
      }

      String jpegHeader = new String(new byte[] { (byte) 255, (byte) 216 });
      String mp3Header = new String(new byte[] { (byte) 255, (byte) 251 });

      fm.getBuffer().setBufferSize(15); // quick reads
      fm.seek(0);

      // Now set the file extension and determine the compression
      for (int i = 0; i < numFiles; i++) {
        int typeID = typeIDs[i];
        int offset = offsets[i];
        int length = lengths[i];

        int decompLength = length;
        ExporterPlugin exporter = exporterDefault;

        String filename = Resource.generateFilename(i);
        String extension = determineExtension(typeID);

        // check for compression
        fm.seek(offset);

        // 4 - Compressed Length
        // 2 - Compression Type (10FB)
        // 3 - Decompressed Length
        if (fm.readInt() == length) {
          short compType = fm.readShort();
          if (compType == -1264) {
            // REFPACK compression

            //byte[] decompLengthBytes = fm.readBytes(4);
            //decompLengthBytes[0] = 0;
            byte[] decompLengthBytes = new byte[] { 0, fm.readByte(), fm.readByte(), fm.readByte() };
            decompLength = IntConverter.convertBig(decompLengthBytes);

            offset += 9; // skip the refpack headers
            length -= 9;

            exporter = exporterRefPack;
          }
          else {
            // not really compressed?
            //ErrorLogger.log("[DAT_DBPF] Unknown Compression Type: " + compType);

            offset += 4; // skip the size header
            length -= 4;
            decompLength = length;
          }

        }

        // check for variations to the TypeID/extension

        if (typeID == -2056397908) {
          fm.relativeSeek(offset); // should be pretty quick, as we're not actually moving and re-reading, just going back to where we were to start with
          String header = fm.readString(6);

          if (header.indexOf("PN") >= 0) {
            extension = ".png";
          }
          else if (header.indexOf("BM") >= 0) {
            extension = ".bmp";
          }
          else if (header.indexOf(jpegHeader) >= 0) {
            extension = ".jpg";
          }
          else if (header.indexOf("SH") >= 0) {
            extension = ".shpi";
          }
          else {
            extension = ".tga";
          }
        }
        else if (typeID == 539399691) {
          fm.relativeSeek(offset); // should be pretty quick, as we're not actually moving and re-reading, just going back to where we were to start with

          String header = fm.readString(6);

          if (header.indexOf("XA") >= 0) {
            extension = ".xa";
          }
          else if (header.indexOf("SP") >= 0) {
            extension = ".spx";
          }
          else if (header.indexOf("UT") >= 0) {
            extension = ".utm";
          }
          else if (header.indexOf("RI") >= 0) {
            extension = ".wav";
          }
          else if (header.indexOf(mp3Header) >= 0) {
            extension = ".mp3";
          }
          else {
            extension = ".utxt";
          }
        }
        else if (typeID == 0) {
          fm.relativeSeek(offset); // should be pretty quick, as we're not actually moving and re-reading, just going back to where we were to start with
          String header = fm.readString(6);

          if (header.indexOf("AB") >= 0) {
            extension = ".ab";
          }
          else {
            extension = ".ui";
          }

        }

        filename += extension;

        //path,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, filename, offset, length, decompLength, exporter);

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
   Provide hints to the previewer so that certain document types are displayed appropriately
   **********************************************************************************************
   **/
  @Override
  public ViewerPlugin previewHint(Resource resource) {
    String extension = resource.getExtension();
    if (extension.equalsIgnoreCase("lua") || extension.equalsIgnoreCase("kbd") || extension.equalsIgnoreCase("pth") || extension.equalsIgnoreCase("ui") || extension.equalsIgnoreCase("rul")) {
      return (ViewerPlugin) WSPluginManager.getPlugin("Viewer", "TXT");
    }
    return null;
  }

}
