package com.android.mm3.wallpaper.animated;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.zip.CRC32;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Color;

/**
 * A PNGDecoder. The slick PNG decoder is based on this class :)
 * 
 * @author Matthias Mann
 */

public class PngDecoder {

    public enum Format {
        ALPHA(1, true),
        LUMINANCE(1, false),
        LUMINANCE_ALPHA(2, true),
        RGB(3, false),
        RGBA(4, true),
        BGRA(4, true),
        ABGR(4, true),
        ARGB(4, true);

        final int numComponents;
        final boolean hasAlpha;

        private Format(int numComponents, boolean hasAlpha) {
            this.numComponents = numComponents;
            this.hasAlpha = hasAlpha;
        }

        public int getNumComponents() {
            return numComponents;
        }

        public boolean isHasAlpha() {
            return hasAlpha;
        }
    }

    private static final byte[] SIGNATURE = {(byte)137, 80, 78, 71, 13, 10, 26, 10};

    private static final int IHDR = 0x49484452;
    private static final int PLTE = 0x504C5445;
    private static final int tRNS = 0x74524E53;
    private static final int IDAT = 0x49444154;
    private static final int IEND = 0x49454E44;
    private static final int acTL = 0x6163544C;
    private static final int fcTL = 0x6663544C;
    private static final int fdAT = 0x66644154;
    
    private static final byte COLOR_GREYSCALE = 0;
    private static final byte COLOR_TRUECOLOR = 2;
    private static final byte COLOR_INDEXED = 3;
    private static final byte COLOR_GREYALPHA = 4;
    private static final byte COLOR_TRUEALPHA = 6;  
    
    private final InputStream input;
    private final CRC32 crc;
    private final byte[] buffer;
    
    private int chunkLength;
    private int chunkType;
    private int chunkRemaining;
    
    private boolean animated = false;
    private int numFrames = 0;
    private int numPlays = 0;
    
    private int width;
    private int height;
    private int bitdepth;
    private int colorType;
    private int bytesPerPixel;
    private byte[] palette;
    private byte[] paletteA;
    private byte[] transPixel;
    
    public PngDecoder(InputStream input) throws IOException {
        this.input = input;
        this.crc = new CRC32();
        this.buffer = new byte[4096];
        
        readFully(buffer, 0, SIGNATURE.length);
        if(!checkSignature(buffer)) {
            throw new IOException("Not a valid PNG file");
        }
        
        searchIDAT: for(;;) {
            openChunk();
            switch (chunkType) {
            case IHDR:
            	readIHDR();
            	break;
            case IDAT:
                break searchIDAT;
            case PLTE:
                readPLTE();
                break;
            case tRNS:
                readtRNS();
                break;
            case acTL:
            	readacTL();
            	break;
            case fcTL:
            	readfcTL();
            	break;
            case fdAT:
            	readfdAT();
            	break;
            case IEND:
            	readIEND();
            	break;
            }
            closeChunk();
        }

        if(colorType == COLOR_INDEXED && palette == null) {
            throw new IOException("Missing PLTE chunk");
        }
    }

    public int getHeight() {
        return height;
    }

    public int getWidth() {
        return width;
    }
    
    /**
     * Checks if the image has a real alpha channel.
     * This method does not check for the presence of a tRNS chunk.
     *
     * @return true if the image has an alpha channel
     * @see #hasAlpha()
     */
    public boolean hasAlphaChannel() {
        return colorType == COLOR_TRUEALPHA || colorType == COLOR_GREYALPHA;
    }

    /**
     * Checks if the image has transparency information either from
     * an alpha channel or from a tRNS chunk.
     * 
     * @return true if the image has transparency
     * @see #hasAlphaChannel()
     * @see #overwriteTRNS(byte, byte, byte)
     */
    public boolean hasAlpha() {
        return hasAlphaChannel() ||
                paletteA != null || transPixel != null;
    }
    
    public boolean isRGB() {
        return colorType == COLOR_TRUEALPHA ||
                colorType == COLOR_TRUECOLOR ||
                colorType == COLOR_INDEXED;
    }

    /**
     * Overwrites the tRNS chunk entry to make a selected color transparent.
     * <p>This can only be invoked when the image has no alpha channel.</p>
     * <p>Calling this method causes {@link #hasAlpha()} to return true.</p>
     *
     * @param r the red component of the color to make transparent
     * @param g the green component of the color to make transparent
     * @param b the blue component of the color to make transparent
     * @throws UnsupportedOperationException if the tRNS chunk data can't be set
     * @see #hasAlphaChannel() 
     */
    public void overwriteTRNS(byte r, byte g, byte b) {
        if(hasAlphaChannel()) {
            throw new UnsupportedOperationException("image has an alpha channel");
        }
        byte[] pal = this.palette;
        if(pal == null) {
            transPixel = new byte[] { 0, r, 0, g, 0, b };
        } else {
            paletteA = new byte[pal.length/3];
            for(int i=0,j=0 ; i<pal.length ; i+=3,j++) {
                if(pal[i] != r || pal[i+1] != g || pal[i+2] != b) {
                    paletteA[j] = (byte)0xFF;
                }
            }
        }
    }

    /**
     * Computes the implemented format conversion for the desired format.
     *
     * @param fmt the desired format
     * @return format which best matches the desired format
     * @throws UnsupportedOperationException if this PNG file can't be decoded
     */
    public Format decideTextureFormat(Format fmt) {
        switch (colorType) {
        case COLOR_TRUECOLOR:
            switch (fmt) {
            case ABGR:
            case ARGB:
            case RGBA:
            case BGRA:
            case RGB: return fmt;
            default: return Format.RGB;
            }
        case COLOR_TRUEALPHA:
            switch (fmt) {
            case ABGR:
            case ARGB:
            case RGBA:
            case BGRA:
            case RGB: return fmt;
            default: return Format.RGBA;
            }
        case COLOR_GREYSCALE:
            switch (fmt) {
            case LUMINANCE:
            case ALPHA: return fmt;
            default: return Format.LUMINANCE;
            }
        case COLOR_GREYALPHA:
            return Format.LUMINANCE_ALPHA;
        case COLOR_INDEXED:
            switch (fmt) {
            case ABGR:
            case ARGB:
            case RGBA:
            case BGRA: return fmt;
            default: return Format.RGBA;
            }
        default:
            throw new UnsupportedOperationException("Not yet implemented");
        }
    }

    /**
     * Decodes the image into the Bitmap.
     *
     * @throws IOException if a read or data error occurred
     * @throws IllegalArgumentException if the start position of a line falls outside the buffer
     * @throws UnsupportedOperationException if the image can't be decoded into the desired format
     */
    public Bitmap decodeBitmap() throws IOException {
        final int lineSize = ((width * bitdepth + 7) / 8) * bytesPerPixel;
        int offset = 0;
        byte[] curLine = new byte[lineSize+1];
        byte[] prevLine = new byte[lineSize+1];
        byte[] palLine = (bitdepth < 8) ? new byte[width+1] : null;
        
        int[] dest = new int[width * height];
        
        final Inflater inflater = new Inflater();
        try {
            for(int y=0 ; y<height ; y++) {
                readChunkUnzip(inflater, curLine, 0, curLine.length);
                unfilter(curLine, prevLine);

                switch (colorType) {
                case COLOR_TRUECOLOR:
                    {
	                    if(transPixel != null) {
	                        byte tr = transPixel[1];
	                        byte tg = transPixel[3];
	                        byte tb = transPixel[5];
	                        for(int i=1,n=curLine.length ; i<n ; i+=3) {
	                            byte r = curLine[i];
	                            byte g = curLine[i+1];
	                            byte b = curLine[i+2];
	                            byte a = (byte)0xFF;
	                            if(r==tr && g==tg && b==tb) {
	                                a = 0;
	                            }
	                            dest[offset++] = Color.argb(a, r, g, b);
	                        }
	                    } else {
	                        for(int i=1,n=curLine.length ; i<n ; i+=3) {
	                        	dest[offset++] = Color.argb((byte)0xFF, curLine[i], curLine[i+1], curLine[i+2]);
	                        }
	                    }
                    }
                    break;
                case COLOR_TRUEALPHA:
                    {
	                    for(int i=1,n=curLine.length ; i<n ; i+=4) {
	                    	dest[offset++] = Color.argb(curLine[i+3], curLine[i], curLine[i+1], curLine[i+2]);
	                    }
                    }
                    break;
                case COLOR_GREYSCALE:
                    {
	                    for(int i=1,n=curLine.length ; i<n ; i+=1) {
	                    	dest[offset++] = curLine[i] * 0x00010101;
	                    }
                    }
                    break;
                case COLOR_GREYALPHA:
	                {
	                    for(int i=1,n=curLine.length ; i<n ; i+=2) {
	                    	dest[offset++] = curLine[i] * 0x00010101 + (curLine[i+1] << 6);
	                    }
	                }
                    break;
                case COLOR_INDEXED:
                	{
	                    switch(bitdepth) {
	                        case 8: palLine = curLine; break;
	                        case 4: expand4(curLine, palLine); break;
	                        case 2: expand2(curLine, palLine); break;
	                        case 1: expand1(curLine, palLine); break;
	                        default: throw new UnsupportedOperationException("Unsupported bitdepth for this image");
	                    }
	                    if(paletteA != null) {
	                        for(int i=1,n=curLine.length ; i<n ; i+=1) {
	                            int idx = curLine[i] & 255;
	                            byte r = palette[idx*3 + 0];
	                            byte g = palette[idx*3 + 1];
	                            byte b = palette[idx*3 + 2];
	                            byte a = paletteA[idx];
	                            dest[offset++] = Color.argb(a, r, g, b);
	                        }
	                    } else {
	                        for(int i=1,n=curLine.length ; i<n ; i+=1) {
	                            int idx = curLine[i] & 255;
	                            byte r = palette[idx*3 + 0];
	                            byte g = palette[idx*3 + 1];
	                            byte b = palette[idx*3 + 2];
	                            byte a = (byte)0xFF;
	                            dest[offset++] = Color.argb(a, r, g, b);
	                        }
	                    }
                	}
                    break;
                default:
                    throw new UnsupportedOperationException("Not yet implemented");
                }

                byte[] tmp = curLine;
                curLine = prevLine;
                prevLine = tmp;
            }
        } finally {
            inflater.end();
        }
        return Bitmap.createBitmap(dest, width, height, Config.ARGB_8888);
    }

    private void expand4(byte[] src, byte[] dst) {
        for(int i=1,n=dst.length ; i<n ; i+=2) {
            int val = src[1 + (i >> 1)] & 255;
            switch(n-i) {
                default: dst[i+1] = (byte)(val & 15);
                case 1:  dst[i  ] = (byte)(val >> 4);
            }
        }
    }

    private void expand2(byte[] src, byte[] dst) {
        for(int i=1,n=dst.length ; i<n ; i+=4) {
            int val = src[1 + (i >> 2)] & 255;
            switch(n-i) {
                default: dst[i+3] = (byte)((val     ) & 3);
                case 3:  dst[i+2] = (byte)((val >> 2) & 3);
                case 2:  dst[i+1] = (byte)((val >> 4) & 3);
                case 1:  dst[i  ] = (byte)((val >> 6)    );
            }
        }
    }

    private void expand1(byte[] src, byte[] dst) {
        for(int i=1,n=dst.length ; i<n ; i+=8) {
            int val = src[1 + (i >> 3)] & 255;
            switch(n-i) {
                default: dst[i+7] = (byte)((val     ) & 1);
                case 7:  dst[i+6] = (byte)((val >> 1) & 1);
                case 6:  dst[i+5] = (byte)((val >> 2) & 1);
                case 5:  dst[i+4] = (byte)((val >> 3) & 1);
                case 4:  dst[i+3] = (byte)((val >> 4) & 1);
                case 3:  dst[i+2] = (byte)((val >> 5) & 1);
                case 2:  dst[i+1] = (byte)((val >> 6) & 1);
                case 1:  dst[i  ] = (byte)((val >> 7)    );
            }
        }
    }
    
    private void unfilter(byte[] curLine, byte[] prevLine) throws IOException {
        switch (curLine[0]) {
            case 0: // none
                break;
            case 1:
                unfilterSub(curLine);
                break;
            case 2:
                unfilterUp(curLine, prevLine);
                break;
            case 3:
                unfilterAverage(curLine, prevLine);
                break;
            case 4:
                unfilterPaeth(curLine, prevLine);
                break;
            default:
                throw new IOException("invalide filter type in scanline: " + curLine[0]);
        }
    }
    
    private void unfilterSub(byte[] curLine) {
        final int bpp = this.bytesPerPixel;
        for(int i=bpp+1,n=curLine.length ; i<n ; ++i) {
            curLine[i] += curLine[i-bpp];
        }
    }
    
    private void unfilterUp(byte[] curLine, byte[] prevLine) {
        final int bpp = this.bytesPerPixel;
        for(int i=1,n=curLine.length ; i<n ; ++i) {
            curLine[i] += prevLine[i];
        }
    }
    
    private void unfilterAverage(byte[] curLine, byte[] prevLine) {
        final int bpp = this.bytesPerPixel;
        
        int i;
        for(i=1 ; i<=bpp ; ++i) {
            curLine[i] += (byte)((prevLine[i] & 0xFF) >>> 1);
        }
        for(int n=curLine.length ; i<n ; ++i) {
            curLine[i] += (byte)(((prevLine[i] & 0xFF) + (curLine[i - bpp] & 0xFF)) >>> 1);
        }
    }
    
    private void unfilterPaeth(byte[] curLine, byte[] prevLine) {
        final int bpp = this.bytesPerPixel;
        
        int i;
        for(i=1 ; i<=bpp ; ++i) {
            curLine[i] += prevLine[i];
        }
        for(int n=curLine.length ; i<n ; ++i) {
            int a = curLine[i - bpp] & 255;
            int b = prevLine[i] & 255;
            int c = prevLine[i - bpp] & 255;
            int p = a + b - c;
            int pa = p - a; if(pa < 0) pa = -pa;
            int pb = p - b; if(pb < 0) pb = -pb;
            int pc = p - c; if(pc < 0) pc = -pc;
            if(pa<=pb && pa<=pc)
                c = a;
            else if(pb<=pc)
                c = b;
            curLine[i] += (byte)c;
        }
    }
      
    private void readIHDR() throws IOException {
        checkChunkLength(13);
        readChunk(buffer, 0, 13);
        width = readInt(buffer, 0);
        height = readInt(buffer, 4);
        bitdepth = buffer[8] & 255;
        colorType = buffer[9] & 255;
        
        switch (colorType) {
        case COLOR_GREYSCALE:
            if(bitdepth != 8) {
                throw new IOException("Unsupported bit depth: " + bitdepth);
            }
            bytesPerPixel = 1;
            break;
        case COLOR_GREYALPHA:
            if(bitdepth != 8) {
                throw new IOException("Unsupported bit depth: " + bitdepth);
            }
            bytesPerPixel = 2;
            break;
        case COLOR_TRUECOLOR:
            if(bitdepth != 8) {
                throw new IOException("Unsupported bit depth: " + bitdepth);
            }
            bytesPerPixel = 3;
            break;
        case COLOR_TRUEALPHA:
            if(bitdepth != 8) {
                throw new IOException("Unsupported bit depth: " + bitdepth);
            }
            bytesPerPixel = 4;
            break;
        case COLOR_INDEXED:
            switch(bitdepth) {
            case 8:
            case 4:
            case 2:
            case 1:
                bytesPerPixel = 1;
                break;
            default:
                throw new IOException("Unsupported bit depth: " + bitdepth);
            }
            break;
        default:
            throw new IOException("unsupported color format: " + colorType);
        }
        
        if(buffer[10] != 0) {
            throw new IOException("unsupported compression method");
        }
        if(buffer[11] != 0) {
            throw new IOException("unsupported filtering method");
        }
        if(buffer[12] != 0) {
            throw new IOException("unsupported interlace method");
        }
    }

    private void readPLTE() throws IOException {
        int paletteEntries = chunkLength / 3;
        if(paletteEntries < 1 || paletteEntries > 256 || (chunkLength % 3) != 0) {
            throw new IOException("PLTE chunk has wrong length");
        }
        palette = new byte[paletteEntries*3];
        readChunk(palette, 0, palette.length);
    }

    private void readtRNS() throws IOException {
        switch (colorType) {
        case COLOR_GREYSCALE:
            checkChunkLength(2);
            transPixel = new byte[2];
            readChunk(transPixel, 0, 2);
            break;
        case COLOR_TRUECOLOR:
            checkChunkLength(6);
            transPixel = new byte[6];
            readChunk(transPixel, 0, 6);
            break;
        case COLOR_INDEXED:
            if(palette == null) {
                throw new IOException("tRNS chunk without PLTE chunk");
            }
            paletteA = new byte[palette.length/3];
            Arrays.fill(paletteA, (byte)0xFF);
            readChunk(paletteA, 0, paletteA.length);
            break;
        default:
            // just ignore it
        }
    }
    
    private void readacTL() throws IOException {
        checkChunkLength(8);
        readChunk(buffer, 0, 8);
    	animated = true;
    	numFrames = readInt(buffer, 0);
    	numPlays = readInt(buffer, 4);
    }


    private void readfcTL() throws IOException {
        checkChunkLength(26);
        readChunk(buffer, 0, 26);
        int sequence_number = readInt(buffer, 0);
    	int ch_width        = readInt(buffer, 4);
    	int ch_height       = readInt(buffer, 8);
    	int x_offset        = readInt(buffer, 12);
    	int y_offset        = readInt(buffer, 16);
    	int delay_num       = readShort(buffer, 20);
    	int delay_den       = readShort(buffer, 22);
    	int dispose_op      = buffer[24];
    	int blend_op        = buffer[25];
    }
    
    private void readIEND() throws IOException {
    }
    
    private void readfdAT() throws IOException {
        checkChunkLength(26);
        readChunk(buffer, 0, 4);
        int sequence_number = readInt(buffer, 0);
    }
    
    private void closeChunk() throws IOException {
        if(chunkRemaining > 0) {
            // just skip the rest and the CRC
            skip(chunkRemaining + 4);
        } else {
            readFully(buffer, 0, 4);
            int expectedCrc = readInt(buffer, 0);
            int computedCrc = (int)crc.getValue();
            if(computedCrc != expectedCrc) {
                throw new IOException("Invalid CRC");
            }
        }
        chunkRemaining = 0;
        chunkLength = 0;
        chunkType = 0;
    }
    
    private void openChunk() throws IOException {
        readFully(buffer, 0, 8);
        chunkLength = readInt(buffer, 0);
        chunkType = readInt(buffer, 4);
        chunkRemaining = chunkLength;
        crc.reset();
        crc.update(buffer, 4, 4);   // only chunkType
    }
    
    private void openChunk(int expected) throws IOException {
        openChunk();
        if(chunkType != expected) {
            throw new IOException("Expected chunk: " + Integer.toHexString(expected));
        }
    }

    private void checkChunkLength(int expected) throws IOException {
        if(chunkLength != expected) {
            throw new IOException("Chunk has wrong size");
        }
    }
    
    private int readChunk(byte[] buffer, int offset, int length) throws IOException {
        if(length > chunkRemaining) {
            length = chunkRemaining;
        }
        readFully(buffer, offset, length);
        crc.update(buffer, offset, length);
        chunkRemaining -= length;
        return length;
    }

    private void refillInflater(Inflater inflater) throws IOException {
        while(chunkRemaining == 0) {
            closeChunk();
            openChunk(IDAT);
        }
        int read = readChunk(buffer, 0, buffer.length);
        inflater.setInput(buffer, 0, read);
    }
    
    private void readChunkUnzip(Inflater inflater, byte[] buffer, int offset, int length) throws IOException {
        assert(buffer != this.buffer);
        try {
            do {
                int read = inflater.inflate(buffer, offset, length);
                if(read <= 0) {
                    if(inflater.finished()) {
                        throw new EOFException();
                    }
                    if(inflater.needsInput()) {
                        refillInflater(inflater);
                    } else {
                        throw new IOException("Can't inflate " + length + " bytes");
                    }
                } else {
                    offset += read;
                    length -= read;
                }
            } while(length > 0);
        } catch (DataFormatException ex) {
            throw (IOException)(new IOException("inflate error").initCause(ex));
        }
    }

    private void readFully(byte[] buffer, int offset, int length) throws IOException {
        do {
            int read = input.read(buffer, offset, length);
            if(read < 0) {
                throw new EOFException();
            }
            offset += read;
            length -= read;
        } while(length > 0);
    }
    
    private int readInt(byte[] buffer, int offset) {
        return
                ((buffer[offset  ]      ) << 24) |
                ((buffer[offset+1] & 255) << 16) |
                ((buffer[offset+2] & 255) <<  8) |
                ((buffer[offset+3] & 255)      );
    }

    private int readShort(byte[] buffer, int offset) {
        return 
                ((buffer[offset  ]      ) << 8) |
                ((buffer[offset+1] & 255)      );
    }

    private void skip(long amount) throws IOException {
        while(amount > 0) {
            long skipped = input.skip(amount);
            if(skipped < 0) {
                throw new EOFException();
            }
            amount -= skipped;
        }
    }
    
    private static boolean checkSignature(byte[] buffer) {
        for(int i=0 ; i<SIGNATURE.length ; i++) {
            if(buffer[i] != SIGNATURE[i]) {
                return false;
            }
        }
        return true;
    }
}
