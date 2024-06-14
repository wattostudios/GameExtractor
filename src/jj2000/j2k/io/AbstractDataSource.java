package jj2000.j2k.io;

import jj2000.j2k.image.*;
import jj2000.j2k.*;
import java.awt.image.*;
import java.awt.Point;

/**
 * An abstract instance of BlkImgDataSrc. Implementations need only
 * implement the "loadTile" method, which sets the "buf", "offset" and "scanline"
 * variables.
 *
 * @author http://bfo.com
 */
public abstract class AbstractDataSource implements BlkImgDataSrc {

    protected /*final*/ int w, h, numc, bpc, nomtw, nomth, numx, numy;
    protected int tx, ty, tw, th;
    protected byte[] buf;
    protected int scanline, offset;

    /**
     * Create a new AbstractDataSource from the specified BufferedImage
     * @param img the image source
     * @param tilesize the tile size (suggest 256)
     */
    public static AbstractDataSource newInstance(BufferedImage img, int tilesize) {
        int w = img.getWidth();
        int h = img.getHeight();
        int numc = img.getColorModel().getNumColorComponents();
        int bpc = img.getColorModel().getComponentSize()[0];
        AbstractDataSource s = new AbstractDataSource() {
            protected void loadTile(int x, int y, int w, int h) {
                offset = y * scanline + x * this.numc;
            }
        };
        s.initialize(w, h, numc, bpc, tilesize);
        s.scanline = (w * numc * bpc + 7) >> 3;
        s.buf = new byte[s.scanline * h];
        Raster raster = img.getData();
        for (int y=0;y<h;y++) {
            for (int x=0;x<w;x++) {
                for (int z=0;z<numc;z++) {
                    s.buf[y*s.scanline + x*numc + z] = (byte)raster.getSample(x, y, z);
                }
            }
        }
        return s;
    }

    /**
     * Create a new AbstractDataSource from the specified byte array and parameters
     * @param w the image width
     * @param h the image height
     * @param numc the number of components in the image
     * @param bpc the number of bits per component in the image
     * @param tilesize the tile size (suggest 256)
     * @param buf the byte buffer containing the image data in normal component-interleaved order
     * @param bufoffset the index into the buffer of component (0,0)
     */
    public static AbstractDataSource newInstance(int w, int h, int numc, int bpc, int tilesize, byte[] buf, final int bufoffset) {
        AbstractDataSource s = new AbstractDataSource() {
            protected void loadTile(int x, int y, int w, int h) {
                offset = bufoffset + y * scanline + x * numc;
            }
        };
        s.initialize(w, h, numc, bpc, tilesize);
        s.scanline = (w * numc * bpc + 7) >> 3;
        s.buf = buf;
        return s;
    }

    protected void initialize(int w, int h, int numc, int bpc, int tilesize) {
        this.w = w;
        this.h = h;
        this.numc = numc;
        this.bpc = bpc;
        this.nomtw = Math.min(tilesize, w);
        this.nomth = Math.min(tilesize, h);
        this.numx = (w + nomtw - 1)  / nomtw;
        this.numy = (h + nomth - 1)  / nomth;
        tw = nomtw;
        th = nomth;
        tx = ty = -1;
    }

    /**
     * This method should be implemented by subclasses.
     * It should set:
     * 1. the "buf" variable to a byte array containing the image data
     * 2. the "offset" variable to the offset into that array of point (x,y)
     * 3. the "scanline" variable to the number of bytes per horizontal scanline
     */
    protected abstract void loadTile(int x, int y, int w, int h);

    public DataBlk getInternCompData(DataBlk blk, int c) {
        if (blk.getDataType() != DataBlk.TYPE_INT) {
            blk = new DataBlkInt(blk.ulx, blk.uly, blk.w, blk.h);
        }
        DataBlkInt blki = (DataBlkInt)blk;
        if (blki.data == null || blki.data.length != blk.w * blk.h) {
            blki.data = new int[blk.w * blk.h];
        }
        int tx0 = tx * nomtw;
        int ty0 = ty * nomth;
//        System.out.println("Here: tx="+tx+"x"+ty+" tw="+tw+"x"+th+" c="+c+" blkxy="+blk.ulx+"x"+blk.uly+" blkwh="+blk.w+"x"+blk.h+" sl="+scanline+" tt="+tx0+"x"+ty0);
//        System.out.println("T: offset="+offset+" txy="+tx0+"x"+ty0+" bxy="+blk.ulx+"x"+blk.uly);
        for (int y=0;y<blk.h;y++) {
            int i = offset + (blk.uly - ty0)*scanline + (blk.ulx - tx0)*numc + c;
            int o = blki.offset + y*tw;
            for (int x=0;x<blk.w;x++) {
                blki.data[o] = (buf[i] & 0xFF) - 128;
                i += numc;
                o++;
            }
        }
        blk.progressive = false;
        return blk;
    }

    public DataBlk getCompData(DataBlk blk, int c) {
        throw new IllegalStateException();
    }

    public int getFixedPoint(int c) {
        return 0;
    }

    public int getTileWidth() {
        return tw;
    }

    public int getTileHeight() {
        return th;
    }

    public int getTileCompWidth(int t, int c) {
        return tw;
    }

    public int getTileCompHeight(int t, int c) {
        return th;
    }

    public void setTile(int x, int y) {
        if (x != tx || y != ty) {
            if (x < 0 || y < 0 || x >= numx || y >= numy) {
                throw new IllegalArgumentException("Tile "+x+"x"+y+" out of bounds");
            }
            tx = x;
            ty = y;
            tw = Math.min(nomtw, w - (tx * nomtw));
            th = Math.min(nomth, h - (ty * nomth));
            loadTile(tx * nomtw, ty * nomth, tw, th);
        }
    }

    public void nextTile() {
        int x = tx, y = ty;
        if (++x == numx) {
            x = 0;
            if (++y == numy) {
                throw new NoNextElementException();
            }
        }
        setTile(x, y);
    }

    public Point getTile(Point co) {
        if (co == null) {
            return new Point(tx, ty);
        } else {
            co.x = tx;
            co.y = ty;
            return co;
        }
    }

    public Point getNumTiles(Point co) {
        if (co == null) {
            return new Point(numx, numy);
        } else {
            co.x = numx;
            co.y = numy;
            return co;
        }
    }

    public int getNumComps() {
        return numc;
    }

    public int getNumTiles() {
        return numx * numy;
    }

    public int getTileGridXOffset() {
        return 0;
    }

    public int getTileGridYOffset() {
        return 0;
    }

    public int getTileIdx() {
        return ty * numx + tx;
    }

    public int getCompULX(int c) {
        return tx * nomtw;
    }

    public int getCompULY(int c) {
        return ty * nomth;
    }

    public int getImgWidth() {
        return w;
    }

    public int getImgHeight() {
        return h;
    }

    public int getCompImgWidth(int c) {
        return w;
    }

    public int getCompImgHeight(int c) {
        return h;
    }

    public int getTilePartULX() {
        return 0;
    }

    public int getTilePartULY() {
        return 0;
    }

    public int getCompSubsX(int c) {
        return 1;
    }

    public int getCompSubsY(int c) {
        return 1;
    }

    public int getNomRangeBits(int c) {
        return bpc;
    }

    public int getImgULX() {
        return 0;
    }

    public int getImgULY() {
        return 0;
    }

    public int getNomTileWidth() {
        return nomtw;
    }

    public int getNomTileHeight() {
        return nomth;
    }
}
