
package org.watto.ge.plugin.archive;

import java.io.File;
import java.util.Hashtable;
import org.watto.datatype.Resource;
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
import org.watto.ge.plugin.exporter.Exporter_Custom_DAT_DBPF;
import org.watto.io.FileManipulator;
import org.watto.io.converter.HexConverter;
import org.watto.io.converter.IntConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_PACKAGE_DBPF extends ArchivePlugin {

  Hashtable<String, String> typeCodes = new Hashtable<String, String>();

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Plugin_PACKAGE_DBPF() {

    super("PACKAGE_DBPF", "The Sims 2 Package");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("The Sims 2");
    setExtensions("package");
    setPlatforms("PC");

    //columns = new String[]{"Filename","Size (Kb)","Size (Bytes)","Extension","Type","Data Offset","Compressed","File ID","Source File","Type ID","Group ID","Instance ID 1","Instance ID 2"};
    //columnTypes = new Class[]{String.class, Integer.class, Integer.class, String.class, String.class, Integer.class, Boolean.class,Integer.class,String.class,String.class,String.class,String.class,String.class};

    setFileTypes("ui", "User Interface",
        "wgra", "Wall Graph",
        "ltxt", "Lot or Tutorial Description",
        "6tx", "Texture Resource",
        "spx", "Speex Audio",
        "xa", "XA Audio",
        "5sc", "Scene Node",
        "3ary", "3D Array",
        "bcon", "Simantics Behaviour Constant",
        "bhav", "Simantics Behaviour Script",
        "cats", "Catalog String",
        "cige", "Image Link",
        "ctss", "Catalog String Set",
        "dgrp", "Drawgroup",
        "face", "Face Properties",
        "famh", "Family Data",
        "fami", "Family Information",
        "fcns", "Global Tuning Values",
        "fwav", "Audio Reference",
        "glob", "Semi-Globals",
        "hous", "House Data",
        "5tm", "Material Definitions",
        "wrld", "World Database",
        "tmap", "Lot or Terrain Texture Map",
        "5cs", "Cinematic Scene",
        "ngbh", "Neighborhood Data",
        "nref", "Name Reference",
        "nmap", "Name Map",
        "objd", "Object Data",
        "objf", "Object Functions",
        "objm", "Object Metadata",
        "palt", "Image Color Palette",
        "pers", "Current Sim Status",
        "posi", "Edith Stack Script",
        "ptbp", "Package Toolkit",
        "simi", "Sim Information",
        "slot", "Object Slot",
        "spr2", "Image Sprites",
        "str", "Text Strings",
        "tatt", "Unknown",
        "tprp", "Edith Simantics Behaviour Labels",
        "trcn", "BCON Labels",
        "tree", "Edith Flowchart Trees",
        "ttab", "Pie Menu Functions",
        "ttas", "Pie Menu Strings",
        "xmto", "Material Object",
        "xobj", "Unknown",
        "5el", "Environment Cube Lighting",
        "2ary", "2D Array",
        "mobjt", "Main Lot Objects",
        "5gn", "Geometric Node",
        "wlay", "Wall Layer",
        "famt", "Family Ties",
        "pmap", "Predictive Map",
        "sfx", "Sound Effects",
        "pdat", "Sim Data and Information",
        "fpst", "Fence Post Layer",
        "5lf", "Linear Fog Lighting",
        "5ds", "Draw State Lighting",
        "5gd", "Geometric Data Container",
        "skin", "Sim Skins and Clothing",
        "nid", "Neighbourhood ID",
        "stxr", "Surface Texture",
        "tssg", "TSSG System",
        "5al", "Ambient Light",
        "5dl", "Directional Light",
        "5pl", "Point Light",
        "5sl", "Spotlight",
        "smap", "String Map",
        "vert", "Vertex Layer",
        "srel", "Sim Relations",
        "lxnr", "Facial Structure",
        "matshad", "Maxis Material Shader",
        "wfr", "Wants and Fears",
        "5cr", "Base Object File",
        "dir", "DBDF Directory",
        "fx", "Effects Resource Tree",
        "6li", "Level Information",
        "objt", "Singular Lot Object",
        "5an", "Animation",
        "5sh", "Shape");

    loadTypeCodes();

  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public String determineExtension(String typeCode) {
    try {

      String extension = typeCodes.get(typeCode);
      if (extension == null) {
        return ".unk";
      }
      else {
        return extension;
      }

    }
    catch (Throwable t) {
      logError(t);
      return ".unk";
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

      // Header
      if (fm.readString(4).equals("DBPF")) {
        rating += 50;
      }

      fm.skip(32);

      // Number Of Files
      int numFiles = fm.readInt();
      if (FieldValidator.checkNumFiles(numFiles)) {
        rating += 5;
      }

      // Directory Offset
      long dirOffset = fm.readInt();
      if (FieldValidator.checkOffset(dirOffset, (int) fm.getLength())) {
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
  public void loadTypeCodes() {

    typeCodes.put("00000000", ".ui");
    typeCodes.put("0A284D0B", ".wgra");
    typeCodes.put("0BF999E7", ".ltxt");
    typeCodes.put("0C7E9A76", ".jpg");
    typeCodes.put("0C900FDB", ".unk");
    typeCodes.put("1C4A276C", ".6tx");
    typeCodes.put("2026960B", ".mp3"); // MP3 SPX XA
    typeCodes.put("25232B11", ".5sc");
    typeCodes.put("2A51171B", ".3ary");
    typeCodes.put("2C310F46", ".unk");
    typeCodes.put("42434F4E", ".bcon");
    typeCodes.put("42484156", ".bhav");
    typeCodes.put("424D505F", ".bmp");
    typeCodes.put("43415453", ".cats");
    typeCodes.put("43494745", ".cige");
    typeCodes.put("43545353", ".ctss");
    typeCodes.put("44475250", ".dgrp");
    typeCodes.put("46414345", ".face");
    typeCodes.put("46414d68", ".famh");
    typeCodes.put("46414D49", ".fami");
    typeCodes.put("46434E53", ".fcns");
    typeCodes.put("46574156", ".fwav");
    typeCodes.put("474C4F42", ".glob");
    typeCodes.put("484F5553", ".hous");
    typeCodes.put("49596978", ".5tm");
    typeCodes.put("49FF7D76", ".wrld");
    typeCodes.put("4B58975B", ".tmap");
    typeCodes.put("4D51F042", ".5cs");
    typeCodes.put("4D533EDD", ".jpg");
    typeCodes.put("4E474248", ".ngbh");
    typeCodes.put("4E524546", ".nref");
    typeCodes.put("4E6D6150", ".nmap");
    typeCodes.put("4F424A44", ".objd");
    typeCodes.put("4F424A66", ".objf");
    typeCodes.put("4F626A4D", ".objm");
    typeCodes.put("50414C54", ".palt");
    typeCodes.put("50455253", ".pers");
    typeCodes.put("504F5349", ".posi");
    typeCodes.put("50544250", ".ptbp");
    typeCodes.put("53494D49", ".simi");
    typeCodes.put("534C4F54", ".slot");
    typeCodes.put("53505232", ".spr2");
    typeCodes.put("53545223", ".str");
    typeCodes.put("54415454", ".tatt");
    typeCodes.put("54505250", ".tprp");
    typeCodes.put("5452434E", ".trcn");
    typeCodes.put("54524545", ".tree");
    typeCodes.put("54544142", ".ttab");
    typeCodes.put("54544173", ".ttas");
    typeCodes.put("584D544F", ".xmto");
    typeCodes.put("584F424A", ".xobj");
    typeCodes.put("6A97042F", ".5el");
    typeCodes.put("6B943B43", ".2ary");
    typeCodes.put("6C589723", ".unk");
    typeCodes.put("6F626A74", ".mobjt");
    typeCodes.put("7B1ACFCD", ".unk");
    typeCodes.put("7BA3838C", ".5gn");
    typeCodes.put("856DDBAC", ".png"); //BMP TGA PNG
    typeCodes.put("8A84D7B0", ".wlay");
    typeCodes.put("8B0C79D6", ".unk");
    typeCodes.put("8C3CE95A", ".jpg");
    typeCodes.put("8C870743", ".famt");
    typeCodes.put("8CC0A14B", ".pmap");
    typeCodes.put("8DB5E4C2", ".sfx");
    typeCodes.put("9D796DB4", ".unk");
    typeCodes.put("AACE2EFB", ".pdat");
    typeCodes.put("AB4BA572", ".fpst");
    typeCodes.put("AB9406AA", ".unk");
    typeCodes.put("ABCB5DA4", ".2ary");
    typeCodes.put("ABD0DC63", ".unk");
    typeCodes.put("AC06A66F", ".5lf");
    typeCodes.put("AC06A676", ".5ds");
    typeCodes.put("AC4F8687", ".5gd");
    typeCodes.put("AC506764", ".skin");
    typeCodes.put("AC8A7A2E", ".nid");
    typeCodes.put("ACE46235", ".stxr");
    typeCodes.put("BA353CE1", ".tssg");
    typeCodes.put("C9C81B9B", ".5al");
    typeCodes.put("C9C81BA3", ".5dl");
    typeCodes.put("C9C81BA9", ".5pl");
    typeCodes.put("C9C81BAD", ".5sl");
    typeCodes.put("CAC4FC40", ".smap");
    typeCodes.put("CB4387A1", ".vert");
    typeCodes.put("CC2A6A34", ".unk");
    typeCodes.put("CC364C2A", ".srel");
    typeCodes.put("CC8A6A69", ".unk");
    typeCodes.put("CCCEF852", ".lxnr");
    typeCodes.put("CD7FE87A", ".matshad");
    typeCodes.put("CD95548E", ".wfr");
    typeCodes.put("E519C933", ".5cr");
    typeCodes.put("E86B1EEF", ".dir");
    typeCodes.put("EA5118B0", ".fx");
    typeCodes.put("EC44BDDC", ".unk");
    typeCodes.put("ED534136", ".6li");
    typeCodes.put("FA1C39F7", ".objt");
    typeCodes.put("FB00791E", ".5an");
    typeCodes.put("FC6EB1F7", ".5sh");

  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  @Override
  @SuppressWarnings("unused")
  public Resource[] read(File path) {
    try {

      addFileTypes();

      FileManipulator fm = new FileManipulator(path, false);
      long arcSize = fm.getLength();

      // 4 Bytes - Header
      // 4 Bytes - Version (1)
      // 16 Bytes - Blank
      // 4 Bytes - Creation Date
      // 4 Bytes - Modification Date
      // 4 Bytes - Unknown (7)
      fm.skip(36);

      // 4 Bytes - Number Of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // 4 Bytes - Directory Offset
      long dirOffset = fm.readInt();
      FieldValidator.checkOffset(dirOffset, arcSize);

      // 4 Bytes - Directory Size
      // 48 Bytes - Unknown

      fm.seek(dirOffset);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {
        // 4 Bytes - Type ID
        byte[] type_b = fm.readBytes(4);
        int typeID = IntConverter.convertLittle(type_b);
        String typeHex = HexConverter.convertBig(type_b).toString();

        // 4 Bytes - Group ID
        String groupHex = HexConverter.changeFormat(fm.readHex(4)).toString();

        // 4 Bytes - Instance ID
        byte[] instance_b = fm.readBytes(4);
        int instanceID = IntConverter.convertLittle(instance_b);
        String instanceHex = HexConverter.convertBig(instance_b).toString();

        // 4 Bytes - Unknown ID
        String unknownHex = HexConverter.changeFormat(fm.readHex(4)).toString();

        // 4 Bytes - Data Offset
        long offset = fm.readInt();
        FieldValidator.checkOffset(offset);

        // 4 Bytes - File Size
        long length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        String filename = Resource.generateFilename(i);

        String ext = determineExtension(typeHex);

        if (typeHex.equals("856DDBAC")) {
          int currentPos = (int) fm.getOffset();
          fm.seek(offset);
          String header = fm.readString(2);
          if (header.equals("PN")) {
            ext = ".png";
          }
          else if (header.equals("BM")) {
            ext = ".bmp";
          }
          else if (header.equals(new String(new byte[] { (byte) 255, (byte) 216 }))) {
            ext = ".jpg";
          }
          else {
            fm.skip(7);
            header = fm.readString(6);

            if (header.indexOf("PN") >= 0) {
              ext = ".png";
            }
            else if (header.indexOf("BM") >= 0) {
              ext = ".bmp";
            }
            else if (header.indexOf(new String(new byte[] { (byte) 255, (byte) 216 })) >= 0) {
              ext = ".jpg";
            }
            else {
              ext = ".tga";
            }
          }
          fm.seek(currentPos);
        }
        else if (typeHex.equals("2026960B")) {
          int currentPos = (int) fm.getOffset();
          fm.seek(offset);
          String header = fm.readString(2);
          if (header.equals("XA")) {
            ext = ".xa";
          }
          else if (header.equals("SP")) {
            ext = ".spx";
          }
          else if (header.equals("UT")) {
            ext = ".utm";
          }
          else if (header.equals("RI")) {
            ext = ".wav";
          }
          else if (header.equals(new String(new byte[] { (byte) 255, (byte) 251 }))) {
            ext = ".mp3";
          }
          else {
            fm.skip(7);
            header = fm.readString(6);

            if (header.indexOf("XA") >= 0) {
              ext = ".xa";
            }
            else if (header.indexOf("SP") >= 0) {
              ext = ".spx";
            }
            else if (header.indexOf("UT") >= 0) {
              ext = ".utm";
            }
            else if (header.indexOf("RI") >= 0) {
              ext = ".wav";
            }
            else if (header.indexOf(new String(new byte[] { (byte) 255, (byte) 251 })) >= 0) {
              ext = ".mp3";
            }
            else {
              ext = ".unk";
            }
          }
          fm.seek(currentPos);
        }

        filename += ext;

        //path,id,name,offset,length,decompLength,exporter
        //resources[i] = new Resource_PACKAGE_DBPF(path,instanceID,filename,offset,length,true,typeHex,groupHex,instanceHex,unknownHex);
        resources[i] = new Resource(path, filename, offset, length);

        TaskProgressManager.setValue(i);
      }

      ExporterPlugin exporter = Exporter_Custom_DAT_DBPF.getInstance();

      for (int i = 0; i < numFiles; i++) {
        fm.seek(resources[i].getOffset());
        long length = fm.readInt();
        if (length == resources[i].getLength()) {
          //System.out.println(length + " - " + resources[i].getLength());
          fm.skip(1);
          byte[] decompLengthBytes = fm.readBytes(4);
          decompLengthBytes[0] = 0;
          int decompLength = IntConverter.convertBig(decompLengthBytes);
          resources[i].setDecompressedLength(decompLength);
          resources[i].setExporter(exporter);
          //System.out.println(decompLength);
        }
        else {
          //System.out.println(length + " - " + resources[i].getLength());
          //fm.skip(1);
          //System.out.println(fm.readInt());
        }
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
