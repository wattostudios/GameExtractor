/*
 * $RCSfile: RandomAccessIO.java,v $
 * $Revision: 1.1 $
 * $Date: 2005/02/11 05:02:16 $
 * $State: Exp $
 *
 * Interface:           RandomAccessIO.java
 *
 * Description:         Interface definition for random access I/O.
 *
 *
 *
 * COPYRIGHT:
 *
 * This software module was originally developed by Raphaël Grosbois and
 * Diego Santa Cruz (Swiss Federal Institute of Technology-EPFL); Joel
 * Askelöf (Ericsson Radio Systems AB); and Bertrand Berthelot, David
 * Bouchard, Félix Henry, Gerard Mozelle and Patrice Onno (Canon Research
 * Centre France S.A) in the course of development of the JPEG2000
 * standard as specified by ISO/IEC 15444 (JPEG 2000 Standard). This
 * software module is an implementation of a part of the JPEG 2000
 * Standard. Swiss Federal Institute of Technology-EPFL, Ericsson Radio
 * Systems AB and Canon Research Centre France S.A (collectively JJ2000
 * Partners) agree not to assert against ISO/IEC and users of the JPEG
 * 2000 Standard (Users) any of their rights under the copyright, not
 * including other intellectual property rights, for this software module
 * with respect to the usage by ISO/IEC and Users of this software module
 * or modifications thereof for use in hardware or software products
 * claiming conformance to the JPEG 2000 Standard. Those intending to use
 * this software module in hardware or software products are advised that
 * their use may infringe existing patents. The original developers of
 * this software module, JJ2000 Partners and ISO/IEC assume no liability
 * for use of this software module or modifications thereof. No license
 * or right to this software module is granted for non JPEG 2000 Standard
 * conforming products. JJ2000 Partners have full right to use this
 * software module for his/her own purpose, assign or donate this
 * software module to any third party and to inhibit third parties from
 * using this software module for non JPEG 2000 Standard conforming
 * products. This copyright notice must be included in all copies or
 * derivative works of this software module.
 *
 * Copyright (c) 1999/2000 JJ2000 Partners.
 */

package jj2000.j2k.io;

import java.io.EOFException;
import java.io.IOException;

/**
 * This abstract class defines the interface to perform random access I/O. It
 * implements the <tt>BinaryDataInput</tt> and <tt>BinaryDataOutput</tt>
 * interfaces so that binary data input/output can be performed.
 *
 * <P>This interface supports streams of up to 2 GB in length.
 *
 * @see BinaryDataInput
 * @see BinaryDataOutput
 * */
public interface RandomAccessIO {

    /**
     * Closes the I/O stream. Prior to closing the stream, any buffered data
     * (at the bit and byte level) should be written.
     *
     * @exception IOException If an I/O error ocurred.
     * */
    public void close() throws IOException;

    /**
     * Returns the current position in the stream, which is the position from
     * where the next byte of data would be read. The first byte in the stream
     * is in position <tt>0</tt>.
     *
     * @return The offset of the current position, in bytes.
     *
     * @exception IOException If an I/O error ocurred.
     * */
    public int getPos() throws IOException;

    /**
     * Returns the current length of the stream, in bytes, taking into account
     * any buffering.
     *
     * @return The length of the stream, in bytes.
     *
     * @exception IOException If an I/O error ocurred.
     * */
    public int length() throws IOException;

    /**
     * Moves the current position for the next read or write operation to
     * offset. The offset is measured from the beginning of the stream. The
     * offset may be set beyond the end of the file, if in write mode. Setting
     * the offset beyond the end of the file does not change the file
     * length. The file length will change only by writing after the offset
     * has been set beyond the end of the file.
     *
     * @param off The offset where to move to.
     *
     * @exception EOFException If in read-only and seeking beyond EOF.
     *
     * @exception IOException If an I/O error ocurred.
     * */
    public void seek(int off) throws IOException;

    /**
     * Reads a byte of data from the stream. Prior to reading, the stream is
     * realigned at the byte level.
     *
     * @return The byte read, as an int.
     *
     * @exception EOFException If the end-of file was reached.
     *
     * @exception IOException If an I/O error ocurred.
     * */
    public int read() throws EOFException, IOException;

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
    public void readFully(byte b[], int off, int len) throws IOException;

    /**
     * Writes a byte to the stream. Prior to writing, the stream is realigned
     * at the byte level.
     *
     * @param b The byte to write. The lower 8 bits of <tt>b</tt> are
     * written.
     *
     * @exception IOException If an I/O error ocurred.
     * */
    public void write(int b) throws IOException;

    /**
     * Should read a signed byte (i.e., 8 bit) from the input.
     * reading, the input should be realigned at the byte level.
     *
     * @return The next byte-aligned signed byte (8 bit) from the
     * input.
     *
     * @exception EOFException If the end-of file was reached before
     * getting all the necessary data.
     *
     * @exception IOException If an I/O error ocurred.
     *
     *
     * */
    public byte readByte() throws EOFException, IOException;

    /**
     * Should read an unsigned byte (i.e., 8 bit) from the input. It is
     * returned as an <tt>int</tt> since Java does not have an
     * unsigned byte type. Prior to reading, the input should be
     * realigned at the byte level.
     *
     * @return The next byte-aligned unsigned byte (8 bit) from the
     * input, as an <tt>int</tt>.
     *
     * @exception EOFException If the end-of file was reached before
     * getting all the necessary data.
     *
     * @exception IOException If an I/O error ocurred.
     *
     *
     *
     */
    public int readUnsignedByte() throws EOFException, IOException;

    /**
     * Should read a signed short (i.e., 16 bit) from the input. Prior to
     * reading, the input should be realigned at the byte level.
     *
     * @return The next byte-aligned signed short (16 bit) from the
     * input.
     *
     * @exception EOFException If the end-of file was reached before
     * getting all the necessary data.
     *
     * @exception IOException If an I/O error ocurred.
     *
     *
     *
     */
    public short readShort() throws EOFException, IOException;

    /**
     * Should read an unsigned short (i.e., 16 bit) from the input. It is
     * returned as an <tt>int</tt> since Java does not have an
     * unsigned short type. Prior to reading, the input should be
     * realigned at the byte level.
     *
     * @return The next byte-aligned unsigned short (16 bit) from the
     * input, as an <tt>int</tt>.
     *
     * @exception EOFException If the end-of file was reached before
     * getting all the necessary data.
     *
     * @exception IOException If an I/O error ocurred.
     *
     *
     *
     */
    public int readUnsignedShort() throws EOFException, IOException;

    /**
     * Should read a signed int (i.e., 32 bit) from the input. Prior to
     * reading, the input should be realigned at the byte level.
     *
     * @return The next byte-aligned signed int (32 bit) from the
     * input.
     *
     * @exception EOFException If the end-of file was reached before
     * getting all the necessary data.
     *
     * @exception IOException If an I/O error ocurred.
     *
     *
     *
     */
    public int readInt() throws EOFException, IOException;

    /**
     * Should read an unsigned int (i.e., 32 bit) from the input. It is
     * returned as a <tt>long</tt> since Java does not have an
     * unsigned short type. Prior to reading, the input should be
     * realigned at the byte level.
     *
     * @return The next byte-aligned unsigned int (32 bit) from the
     * input, as a <tt>long</tt>.
     *
     * @exception EOFException If the end-of file was reached before
     * getting all the necessary data.
     *
     * @exception IOException If an I/O error ocurred.
     *
     *
     *
     */
    public long readUnsignedInt() throws EOFException, IOException;

    /**
     * Should read a signed long (i.e., 64 bit) from the input. Prior to
     * reading, the input should be realigned at the byte level.
     *
     * @return The next byte-aligned signed long (64 bit) from the
     * input.
     *
     * @exception EOFException If the end-of file was reached before
     * getting all the necessary data.
     *
     * @exception IOException If an I/O error ocurred.
     *
     *
     *
     */
    public long readLong() throws EOFException, IOException;

    /**
     * Should read an IEEE single precision (i.e., 32 bit)
     * floating-point number from the input. Prior to reading, the
     * input should be realigned at the byte level.
     *
     * @return The next byte-aligned IEEE float (32 bit) from the
     * input.
     *
     * @exception EOFException If the end-of file was reached before
     * getting all the necessary data.
     *
     * @exception IOException If an I/O error ocurred.
     *
     *
     *
     */
    public float readFloat() throws EOFException, IOException;

    /**
     * Should read an IEEE double precision (i.e., 64 bit)
     * floating-point number from the input. Prior to reading, the
     * input should be realigned at the byte level.
     *
     * @return The next byte-aligned IEEE double (64 bit) from the
     * input.
     *
     * @exception EOFException If the end-of file was reached before
     * getting all the necessary data.
     *
     * @exception IOException If an I/O error ocurred.
     *
     *
     *
     */
    public double readDouble() throws EOFException, IOException;

    /**
     * Returns the endianess (i.e., byte ordering) of the implementing
     * class. Note that an implementing class may implement only one
     * type of endianness or both, which would be decided at creatiuon
     * time.
     *
     * @return Either <tt>EndianType.BIG_ENDIAN</tt> or
     * <tt>EndianType.LITTLE_ENDIAN</tt>
     *
     * @see EndianType
     *
     *
     *
     */
    public int getByteOrdering();

    /**
     * Skips <tt>n</tt> bytes from the input. Prior to skipping, the
     * input should be realigned at the byte level.
     *
     * @param n The number of bytes to skip
     *
     * @exception EOFException If the end-of file was reached before
     * all the bytes could be skipped.
     *
     * @exception IOException If an I/O error ocurred.
     *
     *
     *
     */
    public int skipBytes(int n)throws EOFException, IOException;

    /**
     * Should write the byte value of <tt>v</tt> (i.e., 8 least
     * significant bits) to the output. Prior to writing, the output
     * should be realigned at the byte level.
     *
     * <P>Signed or unsigned data can be written. To write a signed
     * value just pass the <tt>byte</tt> value as an argument. To
     * write unsigned data pass the <tt>int</tt> value as an argument
     * (it will be automatically casted, and only the 8 least
     * significant bits will be written).
     *
     * @param v The value to write to the output
     *
     * @exception IOException If an I/O error ocurred.
     *
     *
     *
     */
    public void writeByte(int v) throws IOException;

    /**
     * Should write the short value of <tt>v</tt> (i.e., 16 least
     * significant bits) to the output. Prior to writing, the output
     * should be realigned at the byte level.
     *
     * <P>Signed or unsigned data can be written. To write a signed
     * value just pass the <tt>short</tt> value as an argument. To
     * write unsigned data pass the <tt>int</tt> value as an argument
     * (it will be automatically casted, and only the 16 least
     * significant bits will be written).
     *
     * @param v The value to write to the output
     *
     * @exception IOException If an I/O error ocurred.
     *
     *
     *
     */
    public void writeShort(int v) throws IOException;

    /**
     * Should write the int value of <tt>v</tt> (i.e., the 32 bits) to
     * the output. Prior to writing, the output should be realigned at
     * the byte level.
     *
     * @param v The value to write to the output
     *
     * @exception IOException If an I/O error ocurred.
     *
     *
     *
     */
    public void writeInt(int v) throws IOException;

    /**
     * Should write the long value of <tt>v</tt> (i.e., the 64 bits)
     * to the output. Prior to writing, the output should be realigned
     * at the byte level.
     *
     * @param v The value to write to the output
     *
     * @exception IOException If an I/O error ocurred.
     *
     *
     *
     */
    public void writeLong(long v) throws IOException;

    /**
     * Should write the IEEE float value <tt>v</tt> (i.e., 32 bits) to
     * the output. Prior to writing, the output should be realigned at
     * the byte level.
     *
     * @param v The value to write to the output
     *
     * @exception IOException If an I/O error ocurred.
     *
     *
     *
     */
    public void writeFloat(float v) throws IOException;

    /**
     * Should write the IEEE double value <tt>v</tt> (i.e., 64 bits)
     * to the output. Prior to writing, the output should be realigned
     * at the byte level.
     *
     * @param v The value to write to the output
     *
     * @exception IOException If an I/O error ocurred.
     *
     *
     *
     */
    public void writeDouble(double v) throws IOException;

    /**
     * Write an array of bytes
     * NOTE this method was not in original RandomAccessIO interface, but
     * was only defined on BufferedRandomAccessFile. Moved to interface
     * for flexibility.
     */
    public void write(byte[] buf, int off, int len) throws IOException;

    /**
     * Any data that has been buffered must be written, and the stream should
     * be realigned at the byte level.
     *
     * @exception IOException If an I/O error ocurred.
     *
     *
     */
    public void flush() throws IOException;

}
