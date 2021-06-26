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

import java.util.Collection;
import java.util.Hashtable;


/***********************************************************************************************
A group of <code>WSPlugin</code>s of a given <code>type</code>
***********************************************************************************************/
public class WSPluginGroup {

  /** The <code>WSPlugin</code>s in this group **/
  Hashtable<String,WSPlugin> plugins = new Hashtable<String,WSPlugin>(25);

  /** The type of plugins in this group **/
  String type = "";


  /***********************************************************************************************
  Creates an empty group for <code>WSPlugin</code>s of the given <code>type</code>
  @param type the type of <code>WSPlugin</code>s in this group
  ***********************************************************************************************/
  public WSPluginGroup(String type){
    this.type = type;
  }


  /***********************************************************************************************
  Adds a <code>WSPlugin</code> to this group
  @param plugin the <code>WSPlugin</code> to add
  ***********************************************************************************************/
  public void addPlugin(WSPlugin plugin){
    if (!plugin.isEnabled()) {
      return; // does not allow disabled plugins
    }

    plugins.put(plugin.getCode(),plugin);
  }


  /***********************************************************************************************
  Gets the <code>WSPlugin</code> in this group with the given <code>code</code> value
  @param code the text code of the <code>WSPlugin</code> to retrieve
  @return the <code>WSPlugin</code> with the text <code>code</code>, or <b>null</b> if no
          <code>WSPlugin</code> could be found with the text <code>code</code>
  ***********************************************************************************************/
  public WSPlugin getPlugin(String code){
    return plugins.get(code);
  }


  /***********************************************************************************************
  Gets the number of <code>plugins</code> in this group
  @return the <code>pluginCount</code>
  ***********************************************************************************************/
  public int getPluginCount(){
    return plugins.size();
  }


  /***********************************************************************************************
  Gets all the <code>WSPlugin</code>s in this group
  @return all the <code>WSPlugin</code>s in this group
  ***********************************************************************************************/
  public WSPlugin[] getPlugins(){
    Collection<WSPlugin> pluginValues = plugins.values();

    WSPlugin[] pluginArray = new WSPlugin[pluginValues.size()];
    pluginValues.toArray(pluginArray);
    return pluginArray;
  }


  /***********************************************************************************************
  Gets the <code>type</code> of <code>WSPlugin</code>s in this group
  @return the <code>type</code>
  ***********************************************************************************************/
  public String getType(){
    return type;
  }


  /***********************************************************************************************
  Whether there is a <code>WSPlugin</code> in this group with the given text <code>code</code>?
  @param code the text code of the <code>WSPlugin</code> to look for
  @return <b>true</b> if a <code>WSPlugin</code> with the text <code>code</code> exists<br />
          <b>false</b> is no <code>WSPlugin</code> with the text <code>code</code> was found
  ***********************************************************************************************/
  public boolean hasPlugin(String code){
    return plugins.containsKey(code);
  }


  /***********************************************************************************************
  Removes a <code>WSPlugin</code> from this group
  @param code the text code of the <code>WSPlugin</code> to remove
  ***********************************************************************************************/
  public void removePlugin(String code){
    plugins.remove(code);
  }


  /***********************************************************************************************
  Removes a <code>WSPlugin</code> from this group
  @param plugin the <code>WSPlugin</code> to remove
  ***********************************************************************************************/
  public void removePlugin(WSPlugin plugin){
    plugins.remove(plugin.getCode());
  }


  /***********************************************************************************************
  Gets the <code>type</code> of <code>WSPlugin</code>s in this group
  @return the <code>type</code>
  ***********************************************************************************************/
  public String toString(){
    return type;
  }
}