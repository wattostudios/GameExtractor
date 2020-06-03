////////////////////////////////////////////////////////////////////////////////////////////////
//                                                                                            //
//                                       WATTO STUDIOS                                        //
//                             Java Code, Programs, and Software                              //
//                                    http://www.watto.org                                    //
//                                                                                            //
//                           Copyright (C) 2004-2010  WATTO Studios                           //
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

package org.watto.component;

import org.watto.Settings;
import org.watto.event.WSEvent;
import org.watto.event.WSEventableInterface;
import org.watto.xml.XMLNode;
import org.watto.xml.XMLReader;

/***********************************************************************************************
 * A Recent Files Menu GUI <code>Component</code>
 ***********************************************************************************************/

public class WSRecentFileMenu extends WSMenu implements WSEventableInterface {

  /** serialVersionUID */
  private static final long serialVersionUID = 1L;

  /***********************************************************************************************
   * Constructor for extended classes only
   ***********************************************************************************************/
  public WSRecentFileMenu() {
    super();
  }

  /***********************************************************************************************
   * Builds a <code>WSPanel</code> and sets the properties from the <code>node</code>
   * @param node the <code>XMLNode</code> used to construct the <code>WSPanel</code>
   ***********************************************************************************************/
  public WSRecentFileMenu(XMLNode node) {
    super();
    toComponent(node);
    registerEvents();
    setInitialised(true); // so events aren't triggered until after setting the initial properties
  }

  /***********************************************************************************************
   * Adds the Recent Files from the <code>Settings</code> to the menu
   ***********************************************************************************************/
  public void addMenuItems() {
    int numRecentFiles = Settings.getInt("NumberOfRecentFiles");

    for (int i = 1; i <= numRecentFiles; i++) { // start at 1 because the 1st recent file is #1, not #0
      String recentFile = Settings.getString("RecentFile" + i);

      if (recentFile == null || recentFile.equals("")) {
        // no more recent files
        break;
      }
      else {
        // add a recent file to the menu
        WSRecentFileMenuItem item = new WSRecentFileMenuItem(XMLReader.read("<WSRecentFileMenuItem code=\"" + recentFile + "\" />"));
        //item.addActionListener(new WSMenuableListener(this));

        if (i < 10) {
          item.setMnemonic(WSHelper.parseMnemonic("I"));
          item.setAccelerator(WSHelper.parseShortcut("ctrl " + i));
        }

        add(item);
      }

    }
  }

  /***********************************************************************************************
   * Rebuild the menu when a recent file is added
   ***********************************************************************************************/
  @Override
  public boolean onEvent(Object source, WSEvent event, int type) {
    if (type == WSEvent.RECENT_FILES_CHANGED) {
      rebuild();
      return true;
    }

    return false;
  }

  /***********************************************************************************************
   * Rebuilds the menu
   ***********************************************************************************************/
  public void rebuild() {
    removeAll();
    addMenuItems();
  }

  /***********************************************************************************************
   * Builds this <code>WSComponent</code> from the properties of the <code>node</code>
   * @param node the <code>XMLNode</code> that describes the <code>WSComponent</code> to
   *        construct
   ***********************************************************************************************/
  @Override
  public void toComponent(XMLNode node) {
    // Sets the generic properties of this component
    WSHelper.setAttributes(node, this);

    if (node.getAttribute("code") == null) {
      setCode("RecentFileMenu");
    }

    //ComponentRepository.add(this);
    setIcons();

    addMenuItems();

  }

  /***********************************************************************************************
   * Constructs an <code>XMLNode</code> representation of this <code>WSComponent</code>
   * @return the <code>XMLNode</code> representation of this <code>WSComponent</code>
   ***********************************************************************************************/
  @Override
  public XMLNode toXML() {
    XMLNode node = WSHelper.toXML(this);
    return node;
  }

}