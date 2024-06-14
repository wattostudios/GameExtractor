/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2024 wattostudios
 *
 * License Information:
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License
 * published by the Free Software Foundation; either version 2 of the License, or (at your option) any later versions. This
 * program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranties
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License at http://www.gnu.org for more
 * details. For further information on this application, refer to the authors' website.
 */

package jj2000.j2k.io;

import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import org.watto.io.FileManipulator;

/**
 * This class defines a Buffered Random Access File.  It implements the
 * <tt>BinaryDataInput</tt> and <tt>BinaryDataOutput</tt> interfaces so that
 * binary data input/output can be performed. This class is abstract since no
 * assumption is done about the byte ordering type (little Endian, big
 * Endian). So subclasses will have to implement methods like
 * <tt>readShort()</tt>, <tt>writeShort()</tt>, <tt>readFloat()</tt>, ...
 *
 * <P><tt>BufferedRandomAccessFile</tt> (BRAF for short) is a
 * <tt>RandomAccessFile</tt> containing an extra buffer. When the BRAF is
 * accessed, it checks if the requested part of the file is in the buffer or
 * not. If that is the case, the read/write is done on the buffer. If not, the
 * file is uppdated to reflect the current status of the buffer and the file
 * is then accessed for a new buffer containing the requested byte/bit.
 *
 * @see RandomAccessIO
 * @see BinaryDataOutput
 * @see BinaryDataInput
 * @see BEBufferedRandomAccessFile
 * */
public abstract class BufferedFileManipulator extends AbstractRandomAccessIO implements EndianType {

    /**
     * The name of the current file
     * */
    private String fileName;

    /**
     * Whether the opened file is read only or not (defined by the constructor
     * arguments)
     * */
    private boolean isReadOnly = true;

    /**
     * The RandomAccessFile associated with the buffer
     * */
    private FileManipulator theFile;

    /**
     * Buffer of bytes containing the part of the file that is currently being
     * accessed
     * */
    protected byte[] byteBuffer;

    /**
     * Boolean keeping track of whether the byte buffer has been changed since
     * it was read.
     * */
    protected boolean byteBufferChanged;

    /**
     * The current offset of the buffer (which will differ from the offset of
     * the file)
     * */
    protected int offset;

    /**
     * The current position in the byte-buffer
     * */
    protected int pos;

    /**
     * The maximum number of bytes that can be read from the buffer
     * */
    protected int maxByte;

    /**
     * Whether the end of the file is in the current buffer or not
     * */
    protected boolean isEOFInBuffer;

    /* The endianess of the class */
    protected int byteOrdering;

    /**
     * Constructor. Always needs a size for the buffer.
     *
     * @param file The file associated with the buffer
     *
     * @param mode "r" for read, "rw" or "rw+" for read and write mode ("rw+"
     *             opens the file for update whereas "rw" removes it
     *             before. So the 2 modes are different only if the file
     *             already exists).
     *
     * @param bufferSize The number of bytes to buffer
     *
     * @exception java.io.IOException If an I/O error ocurred.
     * */
    protected BufferedFileManipulator(File file,
               String mode,
               int bufferSize) throws IOException{

  fileName = file.getName();
  if(mode.equals("rw") || mode.equals("rw+")){ // mode read / write
      isReadOnly = false;
      if(mode.equals("rw")){ // mode read / (over)write
    if(file.exists()) // Output file already exists
        file.delete();
      }
      mode = "rw";
  }
  
  if (mode == "rw") {
    theFile=new FileManipulator(file,true);
  }else {
    theFile=new FileManipulator(file,false);
  }
  byteBuffer=new byte[bufferSize];
  readNewBuffer(0);
    }
    
    
    /**
     * Constructor. Always needs a size for the buffer.
     *
     * @param file The file associated with the buffer
     *
     * @param mode "r" for read, "rw" or "rw+" for read and write mode ("rw+"
     *             opens the file for update whereas "rw" removes it
     *             before. So the 2 modes are different only if the file
     *             already exists).
     *
     * @param bufferSize The number of bytes to buffer
     *
     * @exception java.io.IOException If an I/O error ocurred.
     * */
    protected BufferedFileManipulator(FileManipulator file) throws IOException{

  fileName = file.getFile().getName();
  
      isReadOnly = true;
      theFile = file;

  byteBuffer=new byte[file.getBuffer().getBufferSize()];
  readNewBuffer(0);
    }

    /**
     * Constructor. Uses the default value for the byte-buffer
     * size (512 bytes).
     *
     * @param file The file associated with the buffer
     *
     * @param mode "r" for read, "rw" or "rw+" for read and write mode
     *             ("rw+" opens the file for update whereas "rw" removes
     *             it before. So the 2 modes are different only if the
     *             file already exists).
     *
     * @exception java.io.IOException If an I/O error ocurred.
     * */
    protected BufferedFileManipulator(File file,
				       String mode ) throws IOException{

	this(file, mode, 512);
    }

    /**
     * Constructor. Always needs a size for the buffer.
     *
     * @param name The name of the file associated with the buffer
     *
     * @param mode "r" for read, "rw" or "rw+" for read and write mode
     *             ("rw+" opens the file for update whereas "rw" removes
     *             it before. So the 2 modes are different only if the
     *             file already exists).
     *
     * @param bufferSize The number of bytes to buffer
     *
     * @exception java.io.IOException If an I/O error ocurred.
     * */
    protected BufferedFileManipulator(String name,
				       String mode,
				       int bufferSize) throws IOException{
	this(new File(name), mode, bufferSize);
    }

    /**
     * Constructor. Uses the default value for the byte-buffer
     * size (512 bytes).
     *
     * @param name The name of the file associated with the buffer
     *
     * @param mode "r" for read, "rw" or "rw+" for read and write mode
     *             ("rw+" opens the file for update whereas "rw" removes
     *             it before. So the 2 modes are different only if the
     *             file already exists).
     *
     * @exception java.io.IOException If an I/O error ocurred.
     * */
    protected BufferedFileManipulator(String name,
				       String mode ) throws IOException{

	this(name, mode, 512);
    }

    /**
     * Reads a new buffer from the file. If there has been any
     * changes made since the buffer was read, the buffer is
     * first written to the file.
     *
     * @param off The offset where to move to.
     *
     * @exception java.io.IOException If an I/O error ocurred.
     * */
    protected final void readNewBuffer(int off) throws IOException{

	/* If the buffer have changed. We need to write it to
	 * the file before reading a new buffer.
	 */
	if(byteBufferChanged){
	    flush();
	}
        // Don't allow to seek beyond end of file if reading only
        if (isReadOnly && off > theFile.getLength()) {
            throw new EOFException();
        }
        // Set new offset
	offset = off;

        theFile.seek(offset);

	//maxByte = theFile.read(byteBuffer,0,byteBuffer.length);
        
        maxByte = byteBuffer.length;
        int remainingBytes = (int)theFile.getRemainingLength();
        if (remainingBytes < maxByte) {
          maxByte = remainingBytes;
        }
        byte[] newBuffer = theFile.readBytes(maxByte);
        System.arraycopy(newBuffer, 0, byteBuffer, 0, maxByte);
        
	pos=0;

	if(maxByte<byteBuffer.length){ // Not enough data in input file.
	    isEOFInBuffer = true;
	    if(maxByte==-1){
		maxByte++;
	    }
	}else{
	    isEOFInBuffer = false;
	}
    }

    /**
     * Closes the buffered random access file
     *
     * @exception java.io.IOException If an I/O error ocurred.
     * */
    public void close() throws IOException{
	/* If the buffer has been changed, it need to be saved before
	 * closing
	 */
	flush();
	byteBuffer = null; // Release the byte-buffer reference
	theFile.close();
    }

    /**
     * Returns the current offset in the file
     * */
    public int getPos(){
	return (offset+pos);
    }

    /**
     * Returns the current length of the stream, in bytes, taking into
     * account any buffering.
     *
     * @return The length of the stream, in bytes.
     *
     * @exception java.io.IOException If an I/O error ocurred.
     * */
    public int length() throws IOException{
	int len;

	len = (int)theFile.getLength();

	// If the position in the buffer is not past the end of the file,
	// the length of theFile is the length of the stream
	if( (offset+maxByte)<=len ){
	    return(len);
	}
	else{ // If not, the file is extended due to the buffering
	    return (offset+maxByte);
	}
    }

    /**
     * Moves the current position to the given offset at which the
     * next read or write occurs. The offset is measured from the
     * beginning of the stream.
     *
     * @param off The offset where to move to.
     *
     * @exception EOFException If in read-only and seeking beyond EOF.
     *
     * @exception java.io.IOException If an I/O error ocurred.
     * */
    public void seek(int off) throws IOException{
	/* If the new offset is within the buffer, only the pos value needs
	 * to be modified. Else, the buffer must be moved. */
	if( (off>=offset)&&(off<(offset+byteBuffer.length)) ){
            if (isReadOnly && isEOFInBuffer && off > offset+maxByte) {
                // We are seeking beyond EOF in read-only mode!
                throw new EOFException();
            }
	    pos = off-offset;
	}
	else{
	    readNewBuffer(off);
	}
    }

    /**
     * Reads an unsigned byte of data from the stream. Prior to reading, the
     * stream is realigned at the byte level.
     *
     * @return The byte read.
     *
     * @exception java.io.IOException If an I/O error ocurred.
     *
     * @exception java.io.EOFException If the end of file was reached
     * */
    public final int read() throws IOException, EOFException{
	if(pos<maxByte){ // The byte can be read from the buffer
	    // In Java, the bytes are always signed.
            return (byteBuffer[pos++]&0xFF);
	}
	else if(isEOFInBuffer){ // EOF is reached
            pos = maxByte+1; // Set position to EOF
	    throw new EOFException();
	}
	else { // End of the buffer is reached
	    readNewBuffer(offset+pos);
	    return read();
	}
    }

    /**
     * Reads up to len bytes of data from this file into an array of
     * bytes. This method reads repeatedly from the stream until all the bytes
     * are read. This method blocks until all the bytes are read, the end of
     * the stream is detected, or an exception is thrown.
     *
     * @param b The buffer into which the data is to be read. It must be long
     * enough.
     *
     * @param off The index in 'b' where to place the first byte read.
     *
     * @param len The number of bytes to read.
     *
     * @exception EOFException If the end-of file was reached before
     * getting all the necessary data.
     *
     * @exception IOException If an I/O error ocurred.
     * */
    public final void readFully(byte b[], int off, int len)
        throws IOException {
        int clen; // current length to read
        while (len > 0) {
            // There still is some data to read
            if (pos<maxByte) { // We can read some data from buffer
                clen = maxByte-pos;
                if (clen > len) clen = len;
                System.arraycopy(byteBuffer,pos,b,off,clen);
                pos += clen;
                off += clen;
                len -= clen;
            }
            else if (isEOFInBuffer) {
                pos = maxByte+1; // Set position to EOF
                throw new EOFException();
            }
            else { // Buffer empty => get more data
                readNewBuffer(offset+pos);
            }
        }
    }

    /**
     * Writes a byte to the stream. Prior to writing, the stream is
     * realigned at the byte level.
     *
     * @param b The byte to write. The lower 8 bits of <tt>b</tt> are
     * written.
     *
     * @exception java.io.IOException If an I/O error ocurred.
     * */
    public final void write(int b) throws IOException{
	// As long as pos is less than the length of the buffer we can write
	// to the buffer. If the position is after the buffer a new buffer is
	// needed
	if(pos<byteBuffer.length){
	    if(isReadOnly)
		throw new IOException("File is read only");
	    byteBuffer[pos]=(byte)b;
	    if(pos>=maxByte){
		maxByte=pos+1;
	    }
	    pos++;
	    byteBufferChanged =true;
	}
	else{
	    readNewBuffer(offset+pos);
	    write(b);
	}
    }

    /**
     * Writes aan array of bytes to the stream. Prior to writing, the stream is
     * realigned at the byte level.
     *
     * @param b The array of bytes to write.
     *
     * @param offset The first byte in b to write
     *
     * @param length The number of bytes from b to write
     *
     * @exception java.io.IOException If an I/O error ocurred.
     * */
    public final void write(byte[] b, int offset, int length)
        throws IOException{
        int i,stop;
        stop = offset+length;
        if(stop > b.length)
            throw new ArrayIndexOutOfBoundsException(b.length);
        for(i=offset ; i<stop ; i++){
            write(b[i]);
        }
    }

    /**
     * Any data that has been buffered must be written (including
     * buffering at the bit level), and the stream should be realigned
     * at the byte level.
     *
     * @exception java.io.IOException If an I/O error ocurred.
     * */
    public final void flush() throws IOException{
        if(byteBufferChanged){
	    theFile.seek(offset);
	    
	    //theFile.writeBytes(byteBuffer,0,maxByte);
	    byte[] bytes = new byte[maxByte];
	    System.arraycopy(byteBuffer, 0, bytes, 0, maxByte);
	    theFile.writeBytes(bytes);
	    
	    byteBufferChanged = false;
        }
    }

    /**
     * Returns the endianess (i.e., byte ordering) of the implementing
     * class. Note that an implementing class may implement only one
     * type of endianness or both, which would be decided at creation
     * time.
     *
     * @return Either <tt>EndianType.BIG_ENDIAN</tt> or
     * <tt>EndianType.LITTLE_ENDIAN</tt>
     *
     * @see EndianType
     * */
    public int getByteOrdering(){
	return byteOrdering;
    }

    /**
     * Returns a string of information about the file
     * */
    public String toString(){
	return "BufferedRandomAccessFile: "+fileName+" ("+
	    ((isReadOnly)?"read only":"read/write")+
	    ")";
    }
}
