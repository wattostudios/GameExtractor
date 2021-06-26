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

package org.watto.ge.helper;

import org.watto.component.WSTableColumn;
import org.watto.datatype.Archive;
import org.watto.datatype.Resource;
import org.watto.ge.plugin.ArchivePlugin;

/**
**********************************************************************************************
THIS IS ENHANCED SUCH THAT, IF THERE ARE 2 EQUAL VALUES IN A COLUMN, IT WILL SORT BY THE FILEPATH
**********************************************************************************************
**/

public class FileListSorter {

  static char sortColumnCode = ' ';
  static ArchivePlugin readPlugin = null;
  static boolean ascending = true;

  static char filePathCode = 'P';

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public static int compareBooleans(Resource row1, Resource row2) {
    boolean b1 = ((Boolean) readPlugin.getColumnValue(row1, sortColumnCode)).booleanValue();
    boolean b2 = ((Boolean) readPlugin.getColumnValue(row2, sortColumnCode)).booleanValue();

    if ((b2 != b1) && b2) {
      if (ascending) {
        return -1;
      }
      return 1;
    }
    else if (b1 == b2) {
      return compareFilePaths(row1, row2);
    }
    else {
      if (ascending) {
        return 1;
      }
      return -1;
    }
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public static int compareFilePaths(Resource row1, Resource row2) {
    String s1 = ((String) readPlugin.getColumnValue(row1, filePathCode)).toLowerCase();
    String s2 = ((String) readPlugin.getColumnValue(row2, filePathCode)).toLowerCase();

    int result = s1.compareTo(s2);

    if (result < 0) {
      if (ascending) {
        return -1;
      }
      return 1;
    }
    else {
      if (ascending) {
        return 1;
      }
      return -1;
    }
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public static int compareIntegers(Resource row1, Resource row2) {
    int i1 = ((Integer) readPlugin.getColumnValue(row1, sortColumnCode)).intValue();
    int i2 = ((Integer) readPlugin.getColumnValue(row2, sortColumnCode)).intValue();

    if (i1 < i2) {
      if (ascending) {
        return -1;
      }
      return 1;
    }
    else if (i1 == i2) {
      return compareFilePaths(row1, row2);
    }
    else {
      if (ascending) {
        return 1;
      }
      return -1;
    }
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public static int compareLongs(Resource row1, Resource row2) {
    long i1 = ((Long) readPlugin.getColumnValue(row1, sortColumnCode)).longValue();
    long i2 = ((Long) readPlugin.getColumnValue(row2, sortColumnCode)).longValue();

    if (i1 < i2) {
      if (ascending) {
        return -1;
      }
      return 1;
    }
    else if (i1 == i2) {
      return compareFilePaths(row1, row2);
    }
    else {
      if (ascending) {
        return 1;
      }
      return -1;
    }
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public static int compareStrings(Resource row1, Resource row2) {
    String s1 = ((String) readPlugin.getColumnValue(row1, sortColumnCode)).toLowerCase();
    String s2 = ((String) readPlugin.getColumnValue(row2, sortColumnCode)).toLowerCase();

    int result = s1.compareTo(s2);

    if (result < 0) {
      if (ascending) {
        return -1;
      }
      return 1;
    }
    else if (result == 0) {
      return compareFilePaths(row1, row2);
    }
    else {
      if (ascending) {
        return 1;
      }
      return -1;
    }
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public static char getSortColumnCode() {
    return sortColumnCode;
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public static Resource[] sort(Resource[] resources, WSTableColumn column) {
    return sort(resources, column, true);
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  @SuppressWarnings("rawtypes")
  public static Resource[] sort(Resource[] resources, WSTableColumn column, boolean toggleAscending) {

    readPlugin = Archive.getReadPlugin();

    // determine whether to sort ascending or descending
    if (toggleAscending && sortColumnCode == column.getCharCode()) {
      ascending = !ascending;
    }
    else {
      sortColumnCode = column.getCharCode();
      ascending = true;
    }

    Class sortType = column.getType();

    if (sortType == String.class) {
      sortStrings(resources.clone(), resources, 0, resources.length);
    }
    else if (sortType == Integer.class) {
      sortIntegers(resources.clone(), resources, 0, resources.length);
    }
    else if (sortType == Long.class) {
      sortLongs(resources.clone(), resources, 0, resources.length);
    }
    else if (sortType == Boolean.class) {
      sortBooleans(resources.clone(), resources, 0, resources.length);
    }

    return resources;

  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public static Resource[] sort(WSTableColumn column) {

    return sort(column, true);

  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public static Resource[] sort(WSTableColumn column, boolean toggleAscending) {

    Resource[] resources = Archive.getResources();
    return sort(resources, column, toggleAscending);

  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public static void sortBooleans(Resource[] from, Resource[] to, int low, int high) {
    if (high - low < 2) {
      return;
    }
    int middle = (low + high) / 2;

    sortBooleans(to, from, low, middle);
    sortBooleans(to, from, middle, high);

    int p = low;
    int q = middle;

    if (high - low >= 4 && compareBooleans(from[middle - 1], from[middle]) <= 0) {
      for (int i = low; i < high; i++) {
        to[i] = from[i];
      }
      return;
    }

    for (int i = low; i < high; i++) {
      if (q >= high || (p < middle && compareBooleans(from[p], from[q]) <= 0)) {
        to[i] = from[p++];
      }
      else {
        to[i] = from[q++];
      }
    }
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public static void sortIntegers(Resource[] from, Resource[] to, int low, int high) {
    if (high - low < 2) {
      return;
    }
    int middle = (low + high) / 2;

    sortIntegers(to, from, low, middle);
    sortIntegers(to, from, middle, high);

    int p = low;
    int q = middle;

    if (high - low >= 4 && compareIntegers(from[middle - 1], from[middle]) <= 0) {
      for (int i = low; i < high; i++) {
        to[i] = from[i];
      }
      return;
    }

    for (int i = low; i < high; i++) {
      if (q >= high || (p < middle && compareIntegers(from[p], from[q]) <= 0)) {
        to[i] = from[p++];
      }
      else {
        to[i] = from[q++];
      }
    }
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public static void sortLongs(Resource[] from, Resource[] to, int low, int high) {
    if (high - low < 2) {
      return;
    }
    int middle = (low + high) / 2;

    sortLongs(to, from, low, middle);
    sortLongs(to, from, middle, high);

    int p = low;
    int q = middle;

    if (high - low >= 4 && compareLongs(from[middle - 1], from[middle]) <= 0) {
      for (int i = low; i < high; i++) {
        to[i] = from[i];
      }
      return;
    }

    for (int i = low; i < high; i++) {
      if (q >= high || (p < middle && compareLongs(from[p], from[q]) <= 0)) {
        to[i] = from[p++];
      }
      else {
        to[i] = from[q++];
      }
    }
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public static void sortStrings(Resource[] from, Resource[] to, int low, int high) {
    if (high - low < 2) {
      return;
    }
    int middle = (low + high) / 2;

    sortStrings(to, from, low, middle);
    sortStrings(to, from, middle, high);

    int p = low;
    int q = middle;

    if (high - low >= 4 && compareStrings(from[middle - 1], from[middle]) <= 0) {
      for (int i = low; i < high; i++) {
        to[i] = from[i];
      }
      return;
    }

    for (int i = low; i < high; i++) {
      if (q >= high || (p < middle && compareStrings(from[p], from[q]) <= 0)) {
        to[i] = from[p++];
      }
      else {
        to[i] = from[q++];
      }
    }
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public FileListSorter() {
  }

}
