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
import java.util.Hashtable;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.exporter.Exporter_Default;
import org.watto.ge.plugin.exporter.Exporter_REFPACK;
import org.watto.ge.plugin.exporter.Exporter_ZLib;
import org.watto.io.FileManipulator;
import org.watto.io.converter.HexConverter;
import org.watto.io.converter.IntConverter;
import org.watto.io.converter.LongConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_PACKAGE_DBPF_2 extends ArchivePlugin {

  Hashtable<String, String> typeCodes = new Hashtable<String, String>();

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_PACKAGE_DBPF_2() {

    super("PACKAGE_DBPF_2", "PACKAGE_DBPF_2");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("The Sims 3",
        "The Sims 4");
    setExtensions("package", "world");
    setPlatforms("PC");

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
        return "." + typeCode;
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

      // 4 - Header (DBPF)
      if (fm.readString(4).equals("DBPF")) {
        rating += 50;
      }

      // 4 - Version Major (2)
      if (fm.readInt() == 2) {
        rating += 5;
      }

      // 4 - Version Minor (1)
      if (fm.readInt() == 1) {
        rating += 5;
      }

      fm.skip(24);

      // 4 - Number Of Files
      if (FieldValidator.checkNumFiles(fm.readInt() / 3)) {
        rating += 5;
      }

      fm.skip(4);

      // 4 - Directory Length
      if (FieldValidator.checkLength(fm.readInt(), fm.getLength())) {
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

    typeCodes.put("00AE6C67", ".skcon.bone");
    typeCodes.put("00B2D882", ".img.dds");
    typeCodes.put("00B552EA", ".tree.spe");
    typeCodes.put("015A1849", ".geom");
    typeCodes.put("0166038C", ".nmap");
    typeCodes.put("01661233", ".scene.modl");
    typeCodes.put("01A527DB", ".mm.aud");
    typeCodes.put("01D0E6FB", ".vbuf");
    typeCodes.put("01D0E70F", ".ibuf");
    typeCodes.put("01D0E723", ".vrtf");
    typeCodes.put("01D0E75D", ".scene.matd");
    typeCodes.put("01D0E76B", ".scene.skin");
    typeCodes.put("01D10F34", ".scene.mlod");
    typeCodes.put("01EEF63A", ".mm.aud");
    typeCodes.put("02019972", ".scene.mtst");
    typeCodes.put("021D7E8C", ".scene.spt2");
    typeCodes.put("0229684B", ".vbuf");
    typeCodes.put("0229684F", ".ibuf");
    typeCodes.put("022B756C", ".worlddesc");
    typeCodes.put("025C90A6", ".css");
    typeCodes.put("025C95B6", ".layo.xml");
    typeCodes.put("025ED6F4", ".simo");
    typeCodes.put("029E333B", ".voce.voicemix");
    typeCodes.put("02C9EFF2", ".mixr.audmix");
    typeCodes.put("02D5DF13", ".jazz");
    typeCodes.put("02DC343F", ".objk.objkey");
    typeCodes.put("033260E3", ".tkmk.trackmask");
    typeCodes.put("0333406C", ".xml");
    typeCodes.put("033A1435", ".txtc.compositor");
    typeCodes.put("0341ACC9", ".txtf.fabric");
    typeCodes.put("034AEECB", ".casp.caspart");
    typeCodes.put("0354796A", ".tone.skintone");
    typeCodes.put("03555BA8", ".tone.hairtone");
    typeCodes.put("0355E0A6", ".bond.bonedelta");
    typeCodes.put("0358B08A", ".face.faceblend");
    typeCodes.put("03B33DDF", ".itun.xml");
    typeCodes.put("03B4C61D", ".lite.light");
    typeCodes.put("03D843C2", ".cche.cacheentry");
    typeCodes.put("03D86EA4", ".detl");
    typeCodes.put("0418FE2A", ".cfen.fence");
    typeCodes.put("044AE110", ".comp.xml");
    typeCodes.put("046A7235", ".lotloc");
    typeCodes.put("0498DA7E", ".globlotid");
    typeCodes.put("049CA4CD", ".cstr.stairs");
    typeCodes.put("04A09283", ".stairloc");
    typeCodes.put("04A4D951", ".worlddetail");
    typeCodes.put("04AC5D93", ".cprx.proxyprod");
    typeCodes.put("04B30669", ".cttl.terraintool");
    typeCodes.put("04C58103", ".cral.railing");
    typeCodes.put("04D82D90", ".cmru.cachemru");
    typeCodes.put("04ED4BB2", ".ctpt.terrainpaint");
    typeCodes.put("04EE6ABB", ".terraintex");
    typeCodes.put("04F3CC01", ".cfir.fireplace");
    typeCodes.put("04F51033", ".sbno.binoutfit");
    typeCodes.put("04F66BCC", ".firegroup");
    typeCodes.put("04F88964", ".sime.simexport");
    typeCodes.put("051DF2DD", ".cbln.compblend");
    typeCodes.put("053A3E7B", ".xml");
    typeCodes.put("05512255", ".roomindex");
    typeCodes.put("0563919E", ".wallfloor");
    typeCodes.put("0580A2CD", ".snap.png");
    typeCodes.put("0580A2CE", ".snap.png");
    typeCodes.put("0580A2CF", ".snap.png");
    typeCodes.put("0589DC44", ".thumb.png");
    typeCodes.put("0589DC45", ".thumb.png");
    typeCodes.put("0589DC46", ".thumb.png");
    typeCodes.put("0591B1AF", ".upst.usercastpreset");
    typeCodes.put("05B17698", ".thumb.png");
    typeCodes.put("05B17699", ".thumb.png");
    typeCodes.put("05B1769A", ".thumb.png");
    typeCodes.put("05B1B524", ".thumb.png");
    typeCodes.put("05B1B525", ".thumb.png");
    typeCodes.put("05B1B526", ".thumb.png");
    typeCodes.put("05CD4BB3", ".worldroute");
    typeCodes.put("05DA8AF6", ".wbnd");
    typeCodes.put("05ED1226", ".refs.references");
    typeCodes.put("05FF6BA4", ".2ary.bnry");
    typeCodes.put("0604ABDA", ".dmtr.dreamtree");
    typeCodes.put("060B390C", ".cwat.water");
    typeCodes.put("062853A8", ".famd.household");
    typeCodes.put("062C8204", ".bbln.filen");
    typeCodes.put("06302271", ".cinf.color");
    typeCodes.put("063261DA", ".hinf.haircolor");
    typeCodes.put("06326213", ".obci.objcolor");
    typeCodes.put("0668F628", ".worlddetails");
    typeCodes.put("0668F630", ".worldlotcount");
    typeCodes.put("0668F635", ".twni.png");
    typeCodes.put("0668F639", ".twnp.imgpath");
    typeCodes.put("067CAA11", ".bgeo.blendgeom");
    typeCodes.put("06B981ED", ".objs");
    typeCodes.put("06CE4804", ".meta");
    typeCodes.put("06DC847E", ".terrainpaint");
    typeCodes.put("07046B39", ".simindex");
    typeCodes.put("073FAA07", ".s3sa");
    typeCodes.put("07CD07EC", ".frontdoor");
    typeCodes.put("0A36F07A", ".ccfp.fountain");
    typeCodes.put("0C07456D", ".coll");
    typeCodes.put("0C37A5B5", ".look.lookuptab");
    typeCodes.put("0C772E27", ".loot.xml");
    typeCodes.put("0D338A3A", ".jpg");
    typeCodes.put("11E32896", ".roofdef");
    typeCodes.put("16B17A6C", ".thumbexp");
    typeCodes.put("1709627D", ".h2om");
    typeCodes.put("1A8506C5", ".xml");
    typeCodes.put("1B25A024", ".xml");
    typeCodes.put("1F886EAD", ".ini");
    typeCodes.put("220557DA", ".stbl");
    typeCodes.put("25796DCA", ".font");
    typeCodes.put("2653E3C8", ".thumb.png");
    typeCodes.put("2653E3C9", ".thumb.png");
    typeCodes.put("2653E3CA", ".thumb.png");
    typeCodes.put("26978421", ".cur");
    typeCodes.put("276CA4B9", ".font");
    typeCodes.put("27C01D95", ".xml");
    typeCodes.put("2D4284F0", ".thumb.png");
    typeCodes.put("2D4284F1", ".thumb.png");
    typeCodes.put("2D4284F2", ".thumb.png");
    typeCodes.put("2E75C764", ".icon.png");
    typeCodes.put("2E75C765", ".icon.png");
    typeCodes.put("2E75C766", ".icon.png");
    typeCodes.put("2E75C767", ".icon.png");
    typeCodes.put("2F7D0002", ".imag.png");
    typeCodes.put("2F7D0004", ".imag.png");
    typeCodes.put("2F7D0008", ".uitx.uitexture");
    typeCodes.put("312E7545", ".wall");
    typeCodes.put("316C78F2", ".cfnd.foundation");
    typeCodes.put("319E4F1D", ".objd.object");
    typeCodes.put("32C83095", ".thumbnailvalid");
    typeCodes.put("342778A7", ".faceoverlay");
    typeCodes.put("3453CF95", ".dds");
    typeCodes.put("35A33E29", ".worlddesc");
    typeCodes.put("376840D7", ".avi");
    typeCodes.put("3A65AF29", ".minf.makeup");
    typeCodes.put("3C1AF1F2", ".png");
    typeCodes.put("3C2A8647", ".jpg");
    typeCodes.put("3D8632D0", ".terrainpaint");
    typeCodes.put("3EAAA87C", ".ldnb");
    typeCodes.put("4115F9D5", ".xml");
    typeCodes.put("4D1A5589", ".objn");
    typeCodes.put("515CA4CD", ".cwal.wall");
    typeCodes.put("54372472", ".tsnp.png");
    typeCodes.put("545AC67A", ".data");
    typeCodes.put("5B282D45", ".png");
    typeCodes.put("5DE9DBA0", ".thumb.png");
    typeCodes.put("5DE9DBA1", ".thumb.png");
    typeCodes.put("5DE9DBA2", ".thumb.png");
    typeCodes.put("6017E896", ".buff.xml");
    typeCodes.put("626F60CC", ".thumb.png");
    typeCodes.put("626F60CD", ".thumb.png");
    typeCodes.put("626F60CE", ".thumb.png");
    typeCodes.put("628A788F", ".activehouse");
    typeCodes.put("62ECC59A", ".gfx");
    typeCodes.put("63A33EA7", ".scene.anim");
    typeCodes.put("6B20C4F3", ".clip.animation");
    typeCodes.put("6B6D837D", ".snap.png");
    typeCodes.put("6B6D837E", ".snap.png");
    typeCodes.put("6B6D837F", ".snap.png");
    typeCodes.put("6F40796A", ".wrpr");
    typeCodes.put("71BDB8A2", ".fashion");
    typeCodes.put("72683C15", ".stpr.sktonepreset");
    typeCodes.put("736884F1", ".vpxy");
    typeCodes.put("73E93EEB", ".manifest.xml");
    typeCodes.put("7672F0C5", ".dependency");
    typeCodes.put("76BCF80C", ".trim");
    typeCodes.put("78C8BCE4", ".wmrf");
    typeCodes.put("8070223D", ".audt.audtun");
    typeCodes.put("81CA1A10", ".mtbl");
    typeCodes.put("82B43584", ".checksum");
    typeCodes.put("8EAF13DE", ".rig.grannyrig");
    typeCodes.put("8FFB80F6", ".ads.dds");
    typeCodes.put("9063660D", ".texturemap");
    typeCodes.put("9063660E", ".road");
    typeCodes.put("9151E6BC", ".cwst.wallstyle");
    typeCodes.put("91EDBD3E", ".crst.roofstyle");
    typeCodes.put("94C5D14A", ".sigr.signature");
    typeCodes.put("99D98089", ".xml");
    typeCodes.put("9AFE47F5", ".xml");
    typeCodes.put("9C925813", ".png");
    typeCodes.put("A1FF2FC4", ".jpg");
    typeCodes.put("A576C2E7", ".xml");
    typeCodes.put("A5F9FE18", ".socialroute");
    typeCodes.put("A8D58BE5", ".skil.xml");
    typeCodes.put("AE39399F", ".worldgeom");
    typeCodes.put("B1CC1AF6", ".mm.vid");
    typeCodes.put("B3C438F0", ".sav");
    typeCodes.put("B4DD716B", ".inv.inventory");
    typeCodes.put("B52F5055", ".fbln.blendunit");
    typeCodes.put("B61DE6B4", ".xml");
    typeCodes.put("B6C8B6A0", ".dds");
    typeCodes.put("BA856C78", ".dds");
    typeCodes.put("BDD82221", ".auev");
    typeCodes.put("C0DB5AE7", ".objd");
    typeCodes.put("C202C770", ".aud.xml");
    typeCodes.put("C582D2FB", ".xml");
    typeCodes.put("CB5FDDC7", ".trait.xml");
    typeCodes.put("CD9DE247", ".png");
    typeCodes.put("CF84EC98", ".statemachine");
    typeCodes.put("CF9A4ACE", ".mdlr.modular");
    typeCodes.put("D063545B", ".ldes.lotdesc");
    typeCodes.put("D3044521", ".scene.rslt");
    typeCodes.put("D382BF57", ".scene.ftpt");
    typeCodes.put("D4D9FBE5", ".ptrn.patternlist");
    typeCodes.put("D84E7FC5", ".icon.png");
    typeCodes.put("D84E7FC6", ".icon.png");
    typeCodes.put("D84E7FC7", ".icon.png");
    typeCodes.put("D9BD0909", ".worldname");
    typeCodes.put("DC37E964", ".text");
    typeCodes.put("DD3223A7", ".buff.xml");
    typeCodes.put("DEA2951C", ".petb.breed");
    typeCodes.put("E231B3D8", ".objmod.xml");
    typeCodes.put("E882D22F", ".interact.xml");
    typeCodes.put("EA5118B0", ".swb.effects");
    typeCodes.put("F0FF5598", ".cam");
    typeCodes.put("F1EDBD86", ".crmt.roofmatrl");
    typeCodes.put("F3A38370", ".ngmp.guidmap");
    typeCodes.put("F609FD60", ".worldpack");
    typeCodes.put("FCEAB65B", ".thumb.png");

    /*
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
    */

  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @SuppressWarnings({ "unused", "static-access" })
  @Override
  public Resource[] read(File path) {
    try {

      addFileTypes();

      FileManipulator fm = new FileManipulator(path, false);
      long arcSize = fm.getLength();

      //ExporterPlugin exporterDBPF = Exporter_Custom_DAT_DBPF.getInstance();
      Exporter_REFPACK exporterRefPack = Exporter_REFPACK.getInstance();
      exporterRefPack.setSkipHeaders(true);
      ExporterPlugin exporterZLib = Exporter_ZLib.getInstance();
      ExporterPlugin exporterDefault = Exporter_Default.getInstance();

      // 4 - Header (DBPF)
      // 4 - Version Major (2)
      // 4 - Version Minor (1)
      // 24 - null
      fm.skip(36);

      // 8 - Number Of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles / 3);

      fm.skip(4);

      // 8 - Directory Length
      long dirLength = fm.readLong();
      FieldValidator.checkOffset(dirLength, arcSize);

      // 8 - null
      // 4 - Unknown (3)
      fm.skip(12);

      // 8 - Directory Offset
      long dirOffset = fm.readLong();
      FieldValidator.checkOffset(dirOffset, arcSize);

      // 8 - null
      // 8 - Unknown (65535)
      // 8 - null
      fm.seek(dirOffset);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // 4 - Directory Format
      int dirFormat = fm.readInt();

      if (dirFormat == 0) {
        // Loop through directory
        for (int i = 0; i < numFiles; i++) {
          // 4 - Type ID
          byte[] type_b = fm.readBytes(4);
          int typeID = IntConverter.convertLittle(type_b);
          String typeHex = HexConverter.convertBig(type_b).toString();

          // 4 - Group ID
          String groupHex = HexConverter.changeFormat(fm.readHex(4)).toString();

          // 8 - Instance ID
          byte[] instance_b = fm.readBytes(8);
          long instanceID = LongConverter.convertLittle(instance_b);
          String instanceHex = HexConverter.convertBig(instance_b).toString();

          // 4 - File Offset
          int offset = fm.readInt();
          FieldValidator.checkOffset(offset);

          // 4 - Compressed File Length [&0x7fffffff]
          int length = fm.readInt() & 0x7fffffff;
          FieldValidator.checkLength(length, arcSize);

          // 4 - Decompressed File Length
          int decompLength = fm.readInt();
          FieldValidator.checkLength(decompLength);

          // 2 - Compression Flag (23106=Compressed, -32/0=Uncompressed, -1=REFPACK)
          short compression = fm.readShort();
          ExporterPlugin exporter = exporterDefault;
          if (compression == 23106) {
            exporter = exporterZLib;
          }
          else if (compression == -1) {
            exporter = exporterRefPack;
            offset += 5;
            length -= 5;
          }

          // 2 - Unknown (1)
          short unknown = fm.readShort();

          //System.out.println(i + "\t" + typeHex + "\t" + groupHex);

          String filename = Resource.generateFilename(i);

          String ext = determineExtension(typeHex);

          filename += ext;

          //path,id,name,offset,length,decompLength,exporter
          resources[i] = new Resource(path, filename, offset, length, decompLength, exporter);

          TaskProgressManager.setValue(i);
        }
      }
      else if (dirFormat == 2) {
        // 4 - null
        fm.skip(4);

        // Loop through directory
        for (int i = 0; i < numFiles; i++) {
          // 4 - Type ID
          byte[] type_b = fm.readBytes(4);
          int typeID = IntConverter.convertLittle(type_b);
          String typeHex = HexConverter.convertBig(type_b).toString();

          // 4 - Group ID
          String groupHex = HexConverter.changeFormat(fm.readHex(4)).toString();

          // 4 - Instance ID
          byte[] instance_b = fm.readBytes(4);
          long instanceID = IntConverter.convertLittle(instance_b);
          String instanceHex = HexConverter.convertBig(instance_b).toString();

          // 4 - File Offset
          int offset = fm.readInt();
          FieldValidator.checkOffset(offset);

          // 4 - Compressed File Length [&0x7fffffff]
          int length = fm.readInt() & 0x7fffffff;
          FieldValidator.checkLength(length, arcSize);

          // 4 - Decompressed File Length
          int decompLength = fm.readInt();
          FieldValidator.checkLength(decompLength);

          // 2 - Compression Flag (23106=Compressed, -32=Uncompressed)
          short compression = fm.readShort();
          ExporterPlugin exporter = exporterDefault;
          if (compression == 23106) {
            exporter = exporterZLib;
          }
          else if (compression == -1) {
            exporter = exporterRefPack;
            offset += 5;
            length -= 5;
          }

          // 2 - Unknown (1)
          short unknown = fm.readShort();

          //System.out.println(i + "\t" + typeHex + "\t" + groupHex);

          String filename = Resource.generateFilename(i);

          String ext = determineExtension(typeHex);

          filename += ext;

          //path,id,name,offset,length,decompLength,exporter
          resources[i] = new Resource(path, filename, offset, length, decompLength, exporter);

          TaskProgressManager.setValue(i);
        }
      }
      else {
        System.out.println("[PACKAGE_DBPF_2]: Unknown Directory Format: " + dirFormat);
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
