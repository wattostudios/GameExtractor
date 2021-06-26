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

package org.watto.io.stream.datatype;

/***********************************************************************************************

***********************************************************************************************/
public class ZLibXTree {

  /* table of code length counts */
  int[] table = new int[16]; // unsigned short

  /* code -> symbol translation table */
  int[] trans = new int[288]; // unsigned short

  /***********************************************************************************************
  
  ***********************************************************************************************/
  public ZLibXTree() {
  }

  /***********************************************************************************************
  
  ***********************************************************************************************/
  public int[] getTable() {
    return table;
  }

  /***********************************************************************************************
  
  ***********************************************************************************************/
  public int getTableValue(int position) {
    return table[position];
  }

  /***********************************************************************************************
  
  ***********************************************************************************************/
  public int[] getTrans() {
    return trans;
  }

  /***********************************************************************************************
  
  ***********************************************************************************************/
  public int getTransValue(int position) {
    return trans[position];
  }

  /***********************************************************************************************
  
  ***********************************************************************************************/
  public void setTable(int[] table) {
    this.table = table;
  }

  /***********************************************************************************************
  
  ***********************************************************************************************/
  public void setTableValue(int position, int value) {
    table[position] = value;
  }

  /***********************************************************************************************
  
  ***********************************************************************************************/
  public void setTrans(int[] trans) {
    this.trans = trans;
  }

  /***********************************************************************************************
  
  ***********************************************************************************************/
  public void setTransValue(int position, int value) {
    trans[position] = value;
  }

}