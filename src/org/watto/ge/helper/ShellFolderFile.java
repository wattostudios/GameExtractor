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

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import org.watto.component.DirectoryListDrivesComboBox;
import sun.awt.shell.ShellFolder;

/**
**********************************************************************************************
This is a wrapper class for ShellFolder, so that it returns useful information for Game Extractor.
ShellFolder is a special type of File that is returned from the O/S for folders like "This PC", 
"Network", "Documents", etc., but it doesn't play nice with Game Extractor (eg getParent() gives
funny answers, getName() gets a system-like name rather than a human-friendly name). This class
aims to interface between ShellFolder and GameExtractor to present the correct information.
**********************************************************************************************
**/
public class ShellFolderFile extends File {

  /**  **/
  private static final long serialVersionUID = -8517116437547870943L;

  ShellFolder file = null;

  /** We can manually set a parent, so we can build an appropriate parent chain **/
  ShellFolderFile parent = null;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public ShellFolderFile(File file) {
    super(file.getPath());
    if (file instanceof ShellFolder) {
      this.file = (ShellFolder) file;
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public ShellFolderFile(ShellFolder file) {
    super(file.getPath());
    this.file = file;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public boolean canExecute() {
    if (file != null) {
      return file.canExecute();
    }
    return super.canExecute();
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public boolean canRead() {
    if (file != null) {
      return file.canRead();
    }
    return super.canRead();
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public boolean canWrite() {
    if (file != null) {
      return file.canWrite();
    }
    return super.canWrite();
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public int compareTo(File pathname) {
    if (file != null) {
      return file.compareTo(pathname);
    }
    return super.compareTo(pathname);
  }

  /**
  **********************************************************************************************
  Can't create these files
  **********************************************************************************************
  **/
  @Override
  public boolean createNewFile() throws IOException {
    return false;
  }

  /**
  **********************************************************************************************
  Can't delete these special folders!
  **********************************************************************************************
  **/
  @Override
  public boolean delete() {
    return false;
  }

  /**
  **********************************************************************************************
  Can't delete these special folders!
  **********************************************************************************************
  **/
  @Override
  public void deleteOnExit() {
    //
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public boolean equals(Object obj) {
    if (file != null) {
      return file.equals(obj);
    }
    return super.equals(obj);
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public boolean exists() {
    if (file != null) {
      return file.exists();
    }
    return super.exists();
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public File getAbsoluteFile() {
    if (file != null) {
      return file.getAbsoluteFile();
    }
    return super.getAbsoluteFile();
  }

  /**
  **********************************************************************************************
  Get the name (including the parent path, if it's been set)
  **********************************************************************************************
  **/
  @Override
  public String getAbsolutePath() {
    if (file != null) {
      // if the parent is set, we want to return the parent path as well
      String parentPath = buildParentString();
      if (parentPath == null) {
        return file.getDisplayName();
      }
      else {
        return parentPath + File.separatorChar + file.getDisplayName();
      }
    }
    return super.getAbsolutePath();
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public File getCanonicalFile() throws IOException {
    if (file != null) {
      return file.getCanonicalFile();
    }
    return super.getCanonicalFile();
  }

  /**
  **********************************************************************************************
  Get the name only
  **********************************************************************************************
  **/
  @Override
  public String getCanonicalPath() throws IOException {
    if (file != null) {
      return file.getDisplayName();
    }
    return super.getCanonicalPath();
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public long getFreeSpace() {
    if (file != null) {
      return file.getFreeSpace();
    }
    return super.getFreeSpace();
  }

  /**
  **********************************************************************************************
  Get a human-friendly name, not the technical name
  **********************************************************************************************
  **/
  @Override
  public String getName() {
    if (file != null) {
      return file.getDisplayName();
    }
    return super.getName();
  }

  /**
  **********************************************************************************************
  No parents (unless manually set)
  **********************************************************************************************
  **/
  @Override
  public String getParent() {
    if (parent != null) {
      return parent.buildParentString();
    }
    return null;
  }

  /**
  **********************************************************************************************
  If the parent is manually set, returns the parent + any parents down the chain
  **********************************************************************************************
  **/
  public String buildParentString() {
    if (parent != null) {
      String parentString = parent.buildParentString();
      if (parentString != null) {
        return parentString + File.separatorChar + parent.getName();
      }
      else {
        return parent.getName();
      }
    }
    return null;
  }

  /**
  **********************************************************************************************
  Gets a ShellFolderFile for a path. equivalent of <b>new File(path)</b> but it works for these
  special folders where that other method doesn't.
  **********************************************************************************************
  **/
  public static File getFileForPath(File directory) {
    File[] specialFolders = DirectoryListDrivesComboBox.getDrives();
    int numSpecial = specialFolders.length;

    // get the drive name only, from the source file
    String currentPath = directory.getPath();
    if (currentPath != null) {
      int slashPos = currentPath.indexOf(File.separatorChar);
      if (slashPos != -1) {
        currentPath = currentPath.substring(0, slashPos);
      }

      for (int i = 0; i < numSpecial; i++) {
        File specialFolder = specialFolders[i];
        if (specialFolder instanceof ShellFolderFile) {
          if (specialFolder.getName().equals(currentPath)) {
            // found it

            // Drill through from the special folder into the children until we find the actual folder we want
            String[] parents = null;
            try {
              parents = directory.getPath().split(File.separator);
            }
            catch (Throwable t) {
              parents = directory.getPath().split(File.separator + File.separator);
            }
            int parentCount = parents.length;

            for (int p = 1; p < parentCount; p++) { // start at 1, because we've already found 0 as the specialFolder
              String fileToMatch = parents[p];

              File[] children = specialFolder.listFiles();
              int numChildren = children.length;
              for (int c = 0; c < numChildren; c++) {
                File currentChild = children[c];
                if (currentChild.getName().equals(fileToMatch)) {
                  // found the match, move to the next parent
                  if (currentChild instanceof ShellFolder) {
                    ShellFolderFile currentShellChild = new ShellFolderFile(currentChild);
                    currentShellChild.setParent((ShellFolderFile) specialFolder);
                    currentChild = currentShellChild;
                  }
                  specialFolder = currentChild;
                  break;
                }
                else if (currentChild instanceof ShellFolder && ((ShellFolder) currentChild).getDisplayName().equals(fileToMatch)) {
                  // found the match, move to the next parent
                  if (currentChild instanceof ShellFolder) {
                    ShellFolderFile currentShellChild = new ShellFolderFile(currentChild);
                    currentShellChild.setParent((ShellFolderFile) specialFolder);
                    currentChild = currentShellChild;
                  }
                  specialFolder = currentChild;
                  break;
                }
              }

            }

            // If we got here, we found all (or most of) the path, so we'll load that
            return specialFolder;

          }
        }
      }
    }
    return directory;
  }

  /**
  **********************************************************************************************
  Manually set a parent 
  **********************************************************************************************
  **/
  public void setParent(ShellFolderFile newParent) {
    parent = newParent;
  }

  /**
  **********************************************************************************************
  No parents (unless manually set)
  **********************************************************************************************
  **/
  @Override
  public File getParentFile() {
    if (parent != null) {
      return parent;
    }
    return null;
  }

  /**
  **********************************************************************************************
  Get the name only
  **********************************************************************************************
  **/
  @Override
  public String getPath() {
    if (file != null) {
      return file.getDisplayName();
    }
    return super.getPath();
  }

  /**
  **********************************************************************************************
  Converts the special name to a real path
  **********************************************************************************************
  **/
  public String getPathSuper() {
    return super.getPath();
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public long getTotalSpace() {
    if (file != null) {
      return file.getTotalSpace();
    }
    return super.getTotalSpace();
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public long getUsableSpace() {
    if (file != null) {
      return file.getUsableSpace();
    }
    return super.getUsableSpace();
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public int hashCode() {
    if (file != null) {
      return file.hashCode();
    }
    return super.hashCode();
  }

  /**
  **********************************************************************************************
  Always absolute
  **********************************************************************************************
  **/
  @Override
  public boolean isAbsolute() {
    return true;
  }

  /**
  **********************************************************************************************
  It's a directory
  **********************************************************************************************
  **/
  @Override
  public boolean isDirectory() {
    return true;
  }

  /**
  **********************************************************************************************
  It's not a file
  **********************************************************************************************
  **/
  @Override
  public boolean isFile() {
    return false;
  }

  /**
  **********************************************************************************************
  It's not hidden
  **********************************************************************************************
  **/
  @Override
  public boolean isHidden() {
    return false;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public long lastModified() {
    if (file != null) {
      return file.lastModified();
    }
    return super.lastModified();
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public long length() {
    if (file != null) {
      return file.length();
    }
    return super.length();
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public String[] list() {
    try {
      if (file != null) {
        return file.list();
      }
      return super.list();
    }
    catch (Throwable t) {
      return new String[0];
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public String[] list(FilenameFilter filter) {
    try {
      if (file != null) {
        if (filter == null) {
          return file.list();
        }
        else {
          // listFiles(filter) doesn't work properly for ShellFolder, so just list all files and then manually filter
          return applyFilter(file.list(), filter);
        }
      }
      return super.list(filter);
    }
    catch (Throwable t) {
      return new String[0];
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public File[] listFiles() {
    try {
      if (file != null) {
        return file.listFiles();
      }
      return super.listFiles();
    }
    catch (Throwable t) {
      return new File[0];
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public File[] applyFilter(File[] files, FileFilter filter) {
    try {

      int fileCount = files.length;

      File[] foundFiles = new File[fileCount];
      int foundFilesCount = 0;
      for (int i = 0; i < fileCount; i++) {
        // apply the filter to the file
        File currentFile = files[i];
        if (filter.accept(currentFile)) {
          foundFiles[foundFilesCount] = currentFile;
          foundFilesCount++;
        }
      }

      if (foundFilesCount == fileCount) {
        // all files were valid
        return files;
      }
      else {
        // only some valid files
        File[] shrunkFiles = new File[foundFilesCount];
        System.arraycopy(foundFiles, 0, shrunkFiles, 0, foundFilesCount);
        return shrunkFiles;
      }

    }
    catch (Throwable t) {
      return new File[0];
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public File[] applyFilter(File[] files, FilenameFilter filter) {
    try {

      int fileCount = files.length;

      File[] foundFiles = new File[fileCount];
      int foundFilesCount = 0;
      for (int i = 0; i < fileCount; i++) {
        // apply the filter to the file
        File currentFile = files[i];
        if (filter.accept(this.file, currentFile.getName())) {
          foundFiles[foundFilesCount] = currentFile;
          foundFilesCount++;
        }
      }

      if (foundFilesCount == fileCount) {
        // all files were valid
        return files;
      }
      else {
        // only some valid files
        File[] shrunkFiles = new File[foundFilesCount];
        System.arraycopy(foundFiles, 0, shrunkFiles, 0, foundFilesCount);
        return shrunkFiles;
      }

    }
    catch (Throwable t) {
      return new File[0];
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public String[] applyFilter(String[] files, FilenameFilter filter) {
    try {

      int fileCount = files.length;

      String[] foundFiles = new String[fileCount];
      int foundFilesCount = 0;
      for (int i = 0; i < fileCount; i++) {
        // apply the filter to the file
        String currentFile = files[i];
        if (filter.accept(this.file, new File(currentFile).getName())) {
          foundFiles[foundFilesCount] = currentFile;
          foundFilesCount++;
        }
      }

      if (foundFilesCount == fileCount) {
        // all files were valid
        return files;
      }
      else {
        // only some valid files
        String[] shrunkFiles = new String[foundFilesCount];
        System.arraycopy(foundFiles, 0, shrunkFiles, 0, foundFilesCount);
        return shrunkFiles;
      }

    }
    catch (Throwable t) {
      return new String[0];
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public File[] listFiles(FileFilter filter) {
    try {
      if (file != null) {
        if (filter == null) {
          return file.listFiles();
        }
        else {
          // listFiles(filter) doesn't work properly for ShellFolder, so just list all files and then manually filter
          return applyFilter(file.listFiles(), filter);
        }
      }
      return super.listFiles(filter);
    }
    catch (Throwable t) {
      return new File[0];
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public File[] listFiles(FilenameFilter filter) {
    try {
      if (file != null) {
        if (filter == null) {
          return file.listFiles();
        }
        else {
          // listFiles(filter) doesn't work properly for ShellFolder, so just list all files and then manually filter
          return applyFilter(file.listFiles(), filter);
        }
      }
      return super.listFiles(filter);
    }
    catch (Throwable t) {
      return new File[0];
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public boolean mkdir() {
    if (file != null) {
      return file.mkdir();
    }
    return super.mkdir();
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public boolean mkdirs() {
    if (file != null) {
      return file.mkdirs();
    }
    return super.mkdirs();
  }

  /**
  **********************************************************************************************
  Can't rename
  **********************************************************************************************
  **/
  @Override
  public boolean renameTo(File dest) {
    return false;
  }

  /**
  **********************************************************************************************
  Not executable
  **********************************************************************************************
  **/
  @Override
  public boolean setExecutable(boolean executable, boolean ownerOnly) {
    return false;
  }

  /**
  **********************************************************************************************
  Not executable
  **********************************************************************************************
  **/
  @Override
  public boolean setExecutable(boolean executable) {
    return false;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public boolean setLastModified(long time) {
    if (file != null) {
      return file.setLastModified(time);
    }
    return super.setLastModified(time);
  }

  /**
  **********************************************************************************************
  Not read only
  **********************************************************************************************
  **/
  @Override
  public boolean setReadOnly() {
    return false;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public boolean setReadable(boolean readable, boolean ownerOnly) {
    if (file != null) {
      return file.setReadable(readable, ownerOnly);
    }
    return super.setReadable(readable, ownerOnly);
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public boolean setReadable(boolean readable) {
    if (file != null) {
      return file.setReadable(readable);
    }
    return super.setReadable(readable);
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public boolean setWritable(boolean writable, boolean ownerOnly) {
    if (file != null) {
      return file.setWritable(writable, ownerOnly);
    }
    return super.setWritable(writable, ownerOnly);
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public boolean setWritable(boolean writable) {
    if (file != null) {
      return file.setWritable(writable);
    }
    return super.setWritable(writable);
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public Path toPath() {
    if (file != null) {
      return file.toPath();
    }
    return super.toPath();
  }

  /**
  **********************************************************************************************
  Get the name only
  **********************************************************************************************
  **/
  @Override
  public String toString() {
    if (file != null) {
      return file.getDisplayName();
    }
    return super.toString();
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public URI toURI() {
    if (file != null) {
      return file.toURI();
    }
    return super.toURI();
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @SuppressWarnings("deprecation")
  @Override
  public URL toURL() throws MalformedURLException {
    if (file != null) {
      return file.toURL();
    }
    return super.toURL();
  }

}
