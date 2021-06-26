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

package org.watto.ge.script;

import org.watto.xml.XMLNode;
import org.watto.xml.XMLReader;

/**
**********************************************************************************************
A MexCom3 ScriptNode for the wrapper XML
**********************************************************************************************
**/
public class ScriptNode_MexCom3_XMLWrapper extends ScriptNode {

  String[] extensions = new String[0];

  String[] games = new String[0];

  String[] platforms = new String[0];

  String author = "WATTO (http://www.watto.org/extract)";

  String version = "1.0";

  /**
   **********************************************************************************************
   Checks the syntax of this command for any errors.
   return <b>null</b> if no errors, otherwise returns the error message
   **********************************************************************************************
   **/
  public String checkSyntax() {
    return null;
  }

  /**
  **********************************************************************************************
  Constructor
  **********************************************************************************************
  **/
  public ScriptNode_MexCom3_XMLWrapper(String line) {
    XMLNode node = XMLReader.read(line + "</bms>");

    String nodeExtensions = node.getAttribute("ext");
    if (nodeExtensions.indexOf('\'') >= 0) {
      // split up the extensions
      extensions = nodeExtensions.split("', '");
      extensions[0] = extensions[0].replaceAll("'", "");
      extensions[extensions.length - 1] = extensions[extensions.length - 1].replaceAll("'", "");
    }
    else {
      this.extensions = new String[] { nodeExtensions };
    }

    String nodeGames = node.getAttribute("games");
    if (nodeGames.indexOf('\'') >= 0) {
      // split up the games
      games = nodeGames.split("', '");
      games[0] = games[0].replaceAll("'", "");
      games[games.length - 1] = games[games.length - 1].replaceAll("'", "");
    }
    else {
      this.games = new String[] { nodeGames };
    }

    String nodePlatforms = node.getAttribute("platforms");
    if (nodePlatforms.indexOf('\'') >= 0) {
      // split up the platforms
      platforms = nodePlatforms.split("', '");
      platforms[0] = platforms[0].replaceAll("'", "");
      platforms[platforms.length - 1] = platforms[platforms.length - 1].replaceAll("'", "");
    }
    else {
      this.platforms = new String[] { nodePlatforms };
    }

    this.author = node.getAttribute("author");
    this.version = node.getAttribute("version");

    /*
    System.out.println(author);
    System.out.println(version);
    
    for (int i=0;i<games.length;i++){
      System.out.println(games[i]);
      }
    
    for (int i=0;i<extensions.length;i++){
      System.out.println(extensions[i]);
      }
    
    for (int i=0;i<platforms.length;i++){
      System.out.println(platforms[i]);
      }
    */

  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public String getAuthor() {
    return author;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public String[] getExtensions() {
    return extensions;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public String[] getGames() {
    return games;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public String[] getPlatforms() {
    return platforms;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public String getVersion() {
    return version;
  }

  /**
  **********************************************************************************************
  Runs the command
  **********************************************************************************************
  **/
  @Override
  public void run() {
    runChildren();
  }

}