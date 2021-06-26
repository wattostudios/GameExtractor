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

package org.watto;

import org.watto.task.TaskManager;

/***********************************************************************************************
A singleton class that forwards all requests to <code>SingletonManager</code> to do the dirty
work, but all <i>get</i> methods are typecast to return objects used by this specific program.
***********************************************************************************************/
public class TypecastSingletonManager {

  /***********************************************************************************************
  Adds a <code>object</code>
  @param code a unique codeword for this <code>object</code>
  @param object the object to add
  ***********************************************************************************************/
  public static void add(String code, Object object) {
    SingletonManager.add(code, object);
  }

  /***********************************************************************************************
  Gets the object mapped to the <code>code</code>word
  @param code the codeword for the object to get
  @return the object
  ***********************************************************************************************/
  public static Object get(String code) {
    return SingletonManager.get(code);
  }

  /***********************************************************************************************
  Gets the <code>code</code> object as a <code>RecentFilesManager</code>
  @param code the code for the object to get
  @return the <code>RecentFilesManager</code>
  ***********************************************************************************************/
  public static RecentFilesManager getRecentFilesManager(String code) {
    return (RecentFilesManager) (SingletonManager.get(code));
  }

  /***********************************************************************************************
  Gets the <code>code</code> object as a <code>TaskManager</code>
  @param code the code for the object to get
  @return the <code>TaskManager</code>
  ***********************************************************************************************/
  public static TaskManager getTaskManager(String code) {
    return (TaskManager) (SingletonManager.get(code));
  }

  /***********************************************************************************************
  Whether there is a singleton <code>Object</code> mapped to the <code>code</code>word or not
  @param code the codeword for the object to get
  @return <b>true</b>  if there is a singleton <code>Object</code> mapped to the <code>code</code>word<br />
          <b>false</b> if there is no singleton <code>Object</code> mapped to the <code>code</code>word
  ***********************************************************************************************/
  public static boolean has(String code) {
    return SingletonManager.has(code);
  }

  /***********************************************************************************************
  Removes a <code>object</code>
  @param code the codeword for the object to remove
  ***********************************************************************************************/
  public static void remove(String code) {
    SingletonManager.remove(code);
  }

  /***********************************************************************************************
  Sets a singleton <code>object</code>
  @param code a unique codeword for this <code>object</code>
  @param object the object to set
  ***********************************************************************************************/
  public static void set(String code, Object object) {
    SingletonManager.set(code, object);
  }
}