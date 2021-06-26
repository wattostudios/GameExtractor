/*
	LhaFile.java
	
	package		lha
	class		LhaFile
	
	Copyright (c) 2001-2008 Nobuyasu SUEHIRO All Rights Reserved.
 */

package net.sourceforge.lhadecompressor;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

/**
 * This class is used to read a lha file.<br>
 * <p>
 * <strong>SAMPLE CODE:</strong> read all data from a lha file.<br>
 * 
 * <pre>
 * LzhFile file = new LzhFile(&quot;sample.lzh&quot;);
 * for (Iterator iter = file.entryIterator(); e.hasNext();) {
 * 	InputStream in = file.getInputStream((LzhEntry) e.next());
 * 
 * }
 * </pre>
 * 
 * </p>
 * 
 * @author Nobuyasu SUEHIRO <nosue@users.sourceforge.net>
 */
public class LhaFile {

  private RandomAccessFile raf;

  private String encoding;

  private String name;

  @SuppressWarnings("rawtypes")
  private List entryList;

  @SuppressWarnings("rawtypes")
  private Hashtable entryMap;

  private int size;

  private long pos;

  /**
   * Opens a lha file for reading.
   * 
   * @param file
   *            the lha file
   * @throws LhaException
   *             if a lha format error has occurred
   * @throws IOException
   *             if an I/O error has occurred
   */
  public LhaFile(File file) throws LhaException, IOException {
    this(file.getPath(), LhaEntry.HD_STR_ENCODING);
  }

  /**
   * Opens a lha file for reading.
   * 
   * @param name
   *            the name of lha file
   * @throws LhaException
   *             if a lha format error has occurred
   * @throws IOException
   *             if an I/O error has occurred
   */
  public LhaFile(String name) throws LhaException, IOException {
    this(name, LhaEntry.HD_STR_ENCODING);
  }

  /**
   * Opens a lha file for reading.
   * 
   * @param file
   *            the lha file
   * @param encoding
   *            character encoding name
   * @throws LhaException
   *             if a lha format error has occurred
   * @throws IOException
   *             if an I/O error has occurred
   */
  public LhaFile(File file, String encoding) throws LhaException, IOException {
    this(file.getPath(), encoding);
  }

  /**
   * Opens a lha file for reading.
   * 
   * @param name
   *            the name of lha file
   * @param encoding
   *            character encoding name
   * @throws LhaException
   *             if a lha format error has occurred
   * @throws IOException
   *             if an I/O error has occurred
   */
  @SuppressWarnings("rawtypes")
  public LhaFile(String name, String encoding) throws LhaException, IOException {
    this.raf = new RandomAccessFile(name, "r");
    this.encoding = encoding;
    this.name = name;
    this.entryList = new ArrayList();
    this.entryMap = new Hashtable();

    makeEntryMap();
  }

  /**
   * Returns the lha file entry for the specified name, or null if not found.
   * 
   * @param name
   *            the name of the entry
   * @return the lha file entry, or null if not found
   */
  public LhaEntry getEntry(String name) {
    return ((LhaEntry) entryMap.get(name));
  }

  /**
   * Returns the lha file entry for the specified index, or null if not found.
   * 
   * @param index
   *            the index of the entry
   * @return the lha file entry, or null if not found
   */
  public LhaEntry getEntry(int index) {
    return ((LhaEntry) entryList.get(index));
  }

  /**
   * Returns an input stream for reading the contents of the specified lha
   * file entry.
   * 
   * @param entry
   *            the lha file entry
   * @return the input stream for reading the contents of the specified lha
   *         file entry
   * @throws IOException
   *             if an I/O error has occurred
   */
  public InputStream getInputStream(LhaEntry entry) throws IOException {
    return (new LhaDecoderInputStream(new LhaFileInputStream(this, entry), entry));
  }

  /**
   * Returns the path name of the lha file.
   * 
   * @return the path name of the lha file
   */
  public String getName() {
    return (name);
  }

  /**
   * Returns an iterator of the lha file entries
   * 
   * @return an iterator of the lha file entries
   */
  @SuppressWarnings("rawtypes")
  public Iterator entryIterator() {
    return (entryList.iterator());
  }

  /**
   * Reads from the current lha entry into an array of bytes.
   */
  private int read(long pos, byte b[], int off, int len) throws IOException {
    if (pos != this.pos)
      raf.seek(pos);

    int n = raf.read(b, off, len);
    if (n > 0)
      this.pos = pos + n;

    return (n);
  }

  /**
   * Reads from the current lha entry.
   */
  private int read(long pos) throws IOException {
    if (pos != this.pos)
      raf.seek(pos);

    int n = raf.read();
    if (n > 0)
      this.pos = pos + 1;

    return (n);
  }

  /**
   * Returns the number of entries in the lha file.
   * 
   * @return the number of entries in the lha file
   */
  public int size() {
    return (size);
  }

  /**
   * Closes the lha file
   * 
   * @throws IOException
   *             if an I/O error has occured
   */
  public void close() throws IOException {
    if (raf != null) {
      raf.close();
      raf = null;
    }
  }

  /**
   * Make entry map in lha file.
   * 
   * @throws LhaException
   *             if a lha format error has occurred
   * @throws IOException
   *             if an I/O error has occured
   */
  @SuppressWarnings("unchecked")
  private void makeEntryMap() throws LhaException, IOException {
    LhaEntryReader hr = new LhaEntryReader(encoding) {

      protected int _read(byte[] b) throws IOException {
        return (raf.read(b));
      }
    };

    size = 0;
    while (true) {
      LhaEntry e = hr.readHeader();

      if (e == null)
        break;

      e.setOffset(raf.getFilePointer());
      entryList.add(e);
      entryMap.put(e.getFile().getPath(), e);
      ++size;

      int skipSize = (int) e.getCompressedSize();
      if (raf.skipBytes(skipSize) != skipSize)
        throw (new LhaException("Lha header is broken"));
    }
  }

  /*
   * Inner class implementing the input stream used to read a lha file entry.
   */
  private static class LhaFileInputStream extends InputStream {

    private LhaFile file;

    private long pos;

    private long count;

    public LhaFileInputStream(LhaFile file, LhaEntry entry) {
      if ((file == null) || (entry == null))
        throw (new NullPointerException());

      this.file = file;
      this.pos = entry.getOffset();
      this.count = entry.getCompressedSize();
    }

    public int read(byte b[], int off, int len) throws IOException, LhaException {
      if (count == 0)
        return (-1);

      if (len > count) {
        if (Integer.MAX_VALUE < count) {
          len = Integer.MAX_VALUE;
        }
        else {
          len = (int) count;
        }
      }

      len = file.read(pos, b, off, len);
      if (len == -1)
        throw (new LhaException("premature EOF"));

      pos += len;
      count -= len;

      return (len);
    }

    public int read() throws IOException, LhaException {
      if (count == 0)
        return (-1);

      int n = file.read(pos);
      if (n == -1)
        throw (new LhaException("premature EOF"));

      ++pos;
      --count;

      return (n);
    }
  }
}