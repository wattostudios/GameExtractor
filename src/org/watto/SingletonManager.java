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

package org.watto;

import java.util.Hashtable;

/***********************************************************************************************
 * A singleton class that maintains a list of non-singleton classes. Used to provide a singleton
 * interface to normal classes by mapping them to a codeword.
 ***********************************************************************************************/
public class SingletonManager {

  /** the singleton objects **/
  static Hashtable<String, Object> singletons = new Hashtable<String, Object>();

  /***********************************************************************************************
   * Adds a <code>object</code>
   * @param code a unique codeword for this <code>object</code>
   * @param object the object to add
   ***********************************************************************************************/
  public static void add(String code, Object object) {
    singletons.put(code, object);
  }

  /***********************************************************************************************
   * Gets the object mapped to the <code>code</code>word
   * @param code the codeword for the object to get
   * @return the object
   ***********************************************************************************************/
  public static Object get(String code) {
    return singletons.get(code);
  }

  /***********************************************************************************************
   * Whether there is a singleton <code>Object</code> mapped to the <code>code</code>word or not
   * @param code the codeword for the object to get
   * @return <b>true</b> if there is a singleton <code>Object</code> mapped to the
   *         <code>code</code>word<br />
   *         <b>false</b> if there is no singleton <code>Object</code> mapped to the
   *         <code>code</code>word
   ***********************************************************************************************/
  public static boolean has(String code) {
    return singletons.containsKey(code);
  }

  /***********************************************************************************************
   * Removes a <code>object</code>
   * @param code the codeword for the object to remove
   ***********************************************************************************************/
  public static void remove(String code) {
    singletons.remove(code);
  }

  /***********************************************************************************************
   * Sets a singleton <code>object</code>
   * @param code a unique codeword for this <code>object</code>
   * @param object the object to set
   ***********************************************************************************************/
  public static void set(String code, Object object) {
    singletons.put(code, object);
  }
}