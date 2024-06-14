package jj2000.j2k.io;

import java.io.IOException;

/**
 * A view on an existing RandomAccessIO which can be used to
 * limit the length of data that can be read.
 *
 * @author http://bfo.com
 */
public abstract class AbstractRandomAccessIO implements RandomAccessIO {

    public byte readByte() throws IOException {
        return (byte)read();
    }
    public int readUnsignedByte() throws IOException {
        return read();
    }
    public short readShort() throws IOException {
        return (short)((read()<<8) | read());
    }
    public int readUnsignedShort() throws IOException {
        return ((read()<<8) | read());
    }
    public int readInt() throws IOException {
        return (read()<<24) | (read()<<16) | (read()<<8) | read();
    }
    public long readUnsignedInt() throws IOException {
        return readInt() & 0xFFFFFFFFl;
    }
    public long readLong() throws IOException {
        return (readUnsignedInt()<<32) | readUnsignedInt();
    }
    public float readFloat() throws IOException {
        return Float.intBitsToFloat(readInt());
    }
    public double readDouble() throws IOException {
        return Double.longBitsToDouble(readLong());
    }
    public int getByteOrdering() {
        return EndianType.BIG_ENDIAN;
    }
    public int skipBytes(int n) throws IOException {
        seek(getPos() + n);
        return n;
    }
    public void writeByte(int v) throws IOException {
        write(v);
    }
    public void writeShort(int v) throws IOException {
        write(v>>8);
        write(v);
    }
    public void writeInt(int v) throws IOException {
        write(v>>24);
        write(v>>16);
        write(v>>8);
        write(v);
    }
    public void writeLong(long v) throws IOException {
        writeInt((int)(v>>32));
        writeInt((int)v);
    }
    public void writeFloat(float v) throws IOException {
        writeInt(Float.floatToIntBits(v));
    }
    public void writeDouble(double v) throws IOException {
        writeLong(Double.doubleToLongBits(v));
    }
    public void write(byte[] buf, int off, int len) throws IOException {
        for (int i=0;i<len;i++) {
            write(buf[off + i]);
        }
    }

}
