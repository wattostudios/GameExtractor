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

import java.util.Enumeration;
import java.util.Hashtable;


/***********************************************************************************************
The central repository for accessing <code>WSComponent</code>s
***********************************************************************************************/
public class ComponentRepository {

  /** The components in the repository **/
  static Hashtable<String,WSComponent> components = new Hashtable<String,WSComponent>(50);


  /***********************************************************************************************
  Constructor
  ***********************************************************************************************/
  public ComponentRepository(){}


  /***********************************************************************************************
  Adds a <code>component</code> into the repository
  @param component the <code>WSComponent</code> to add
  @return the code used to access the <code>component</code>
  ***********************************************************************************************/
  public static String add(WSComponent component){
    String code = component.getCode();
    if (code == null || code.equals("")) {
      code = "" + component.hashCode();
      component.setCode(code);
    }
    components.put(code,component);
    return code;
  }


  /***********************************************************************************************
  Requests focus for the <code>WSComponent</code> with the given <code>code</code>
  @param code the code of the <code>WSComponent</code>
  @return the <code>WSComponent</code>
  ***********************************************************************************************/
  public static WSComponent focus(String code){
    WSComponent component = get(code);
    component.setFocus(true);
    return component;
  }


  /***********************************************************************************************
  Gets the <code>WSComponent</code> with the given <code>code</code>
  @param code the code of the <code>WSComponent</code>
  @return the <code>WSComponent</code>
  ***********************************************************************************************/
  public static WSComponent get(String code){
    return components.get(code);
  }


  /***********************************************************************************************
  Is there a <code>WSComponent</code> with the given <code>code</code>?
  @param code the code of the <code>WSComponent</code>
  @return <b>true</b> if there is a <code>WSComponent</code> with the <code>code</code><br />
          <b>false</b> if no <code>WSComponent</code> exists with the <code>code</code>
  ***********************************************************************************************/
  public static boolean has(String code){
    return components.containsKey(code);
  }


  /***********************************************************************************************
  Writes all the <code>WSComponent</code>s and their codes to the command prompt
  ***********************************************************************************************/
  public static void outputComponentList(){
    Enumeration<String> keys = components.keys();
    Enumeration<WSComponent> values = components.elements();

    while (keys.hasMoreElements() && values.hasMoreElements()) {
      String key = (String)keys.nextElement();
      String value = values.nextElement().getClass().getName();

      System.out.println(key + " : " + value);
    }
  }


  /***********************************************************************************************
  Removes the <code>WSComponent</code> with the given <code>code</code>
  @param code the code of the <code>WSComponent</code>
  @return the <code>WSComponent</code> that was removed
  ***********************************************************************************************/
  public static WSComponent remove(String code){
    return components.remove(code);
  }
}