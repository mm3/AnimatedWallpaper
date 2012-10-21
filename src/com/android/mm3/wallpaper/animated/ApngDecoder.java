package com.android.mm3.wallpaper.animated;

import java.awt.*;
import java.io.*;
import java.util.*;
import android.graphics.*;

import java.util.zip.CRC32;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

	/**
	 * TO-DO
	 */
	public class ApngDecoder extends Decoder
	{
		/** TO-DO */
		public static final int acTL = 0x6163544C;
		/** TO-DO */
		public static final int fcTL = 0x6663544C;
		/** TO-DO */
		public static final int fdAT = 0x66644154;
		
		public static final int BITMASK		= 2;
		public static final int OPAQUE 		= 1;
		public static final int TRANSLUCENT 	= 3;

		private static final PngConfig DEFAULT_CONFIG =
		new PngConfig.Builder().readLimit(PngConfig.READ_EXCEPT_DATA).build();

	    private final PngConfig config;
		private final List chunks = new ArrayList();
		private final List frames = new ArrayList();
		private final Map frameData = new HashMap();
		private final List defaultImageData = new ArrayList();

		private Rectangle headerBounds;
		private boolean animated;
		private boolean sawData;
		private boolean useDefaultImage;
		private int numFrames;
		private int numPlays;

		
		private final Map props = new HashMap();
    	private boolean read = false;
	
    	
    	
		/**
		 * TO-DO
		 */
		public ApngDecoder ()
		{
			this.config = DEFAULT_CONFIG;
		}

		/**
		 * TO-DO
		 */
	//	public AnimatedPngImage(PngConfig config)
	//	{
	//		super(new PngConfig.Builder(config).readLimit(PngConfig.READ_EXCEPT_DATA).build());
	//	}
		
		public static class Dimension {
			public Dimension(int width, int height) {
				this.width = width;
				this.height = height;
			}
			int width = 0;
			int height = 0;
			
		}

		private void reset()
		{
			animated = sawData = useDefaultImage = false;
			chunks.clear();
			frames.clear();
			frameData.clear();
			defaultImageData.clear();
		}

		/**
		 * TO-DO
		 */
		public boolean isAnimated()
		{
			assertRead();
			return animated;
		}

		/**
		 * TO-DO
		 */
		public int getNumFrames()
		{
			assertRead();
			return frames.size();
		}

		/**
		 * TO-DO
		 */
		public int getNumPlays()
		{
			assertRead();
			return animated ? numPlays : 1;
		}

		/**
		 * TO-DO
		 */
		public Bitmap getFrame(int index)
		{
			return null; //getFrameControl(index);
		}
		
		public FrameControl getFrameControl(int index) {
			assertRead();
			return (FrameControl)frames.get(index);			
		}

		/**
		 * TO-DO
		 */
		public boolean isClearRequired()
		{
			assertRead();
			if (!animated)
				return false;
			FrameControl first = getFrameControl(0);
			return (first.getBlend() == FrameControl.BLEND_OVER) ||
				!first.getBounds().equals(new Rectangle(getWidth(), getHeight()));
		}

		/**
		 * TO-DO
		 */
		public Bitmap[] readAllFrames(InputStream in)
		throws IOException
		{
			read(in);
			Bitmap[] images = new Bitmap[getNumFrames()];
			for (int i = 0; i < images.length; i++)
				images[i] = readFrame(in, getFrameControl(i));
			return images;
		}

		// TO-DO: make sure that file is what we read before?
		// TO-DO: make sure that frame control is from this image?
		/**
		 * TO-DO
		 */
		public Bitmap readFrame(InputStream in, FrameControl frame)
		throws IOException
		{
			assertRead();
			if (frame == null)
				return readImage(in, defaultImageData, new Dimension(getWidth(), getHeight()));
			return readImage(in, (List)frameData.get(frame), frame.getBounds().getSize());
		}

		private Bitmap readImage(InputStream in, List data, Dimension size)
		throws IOException
		{
			try {
				return createImage(in, size);
			} finally {
				in.close();
			}
		}

		private void assertRead()
		{
			if (frames.isEmpty())
				throw new IllegalStateException("Image has not been read");
		}

		protected void readChunk(int type, DataInput in, long offset, int length)
		throws IOException
		{
			switch (type) {
				case PngConstants.IEND:
					validate();
					superreadChunk(type, in, offset, length);
					break;

				case PngConstants.IHDR:
					reset();
					superreadChunk(type, in, offset, length);
					headerBounds = new Rectangle(getWidth(), getHeight());
					break;

				case acTL:
					RegisteredChunks.checkLength(type, length, 8);
					if (sawData)
						error("acTL cannot appear after IDAT");
					animated = true;
					if ((numFrames = in.readInt()) <= 0)
						error("Invalid frame count: " + numFrames);
					if ((numPlays = in.readInt()) < 0)
						error("Invalid play count: " + numPlays);
					break;

				case fcTL:
					RegisteredChunks.checkLength(type, length, 26);
					add(in.readInt(), readFrameControl(in));
					break;

				case fdAT:
					if (!sawData)
						error("fdAT chunks cannot appear before IDAT");
					add(in.readInt(), new FrameData(offset + 4, length - 4));
					break;

				case PngConstants.IDAT:
					sawData = true;
					defaultImageData.add(new FrameData(offset, length));
					break;

				default:
					superreadChunk(type, in, offset, length);
			}
		}

		protected boolean isMultipleOK(int type)
		{
			switch (type) {
				case fcTL:
				case fdAT:
					return true;
			}
			return superisMultipleOK(type);
		}

		private void add(int seq, Object chunk)
		throws IOException
		{
			if (chunks.size() != seq ||
				(seq == 0 && !(chunk instanceof FrameControl)))
				error("APNG chunks out of order");
			chunks.add(chunk);
		}

		private static void error(String message)
		throws IOException
		{
			throw new IOException(message);
		}

		private FrameControl readFrameControl(DataInput in)
		throws IOException
		{
			int w = in.readInt();
			int h = in.readInt();
			Rectangle bounds = new Rectangle(in.readInt(), in.readInt(), w, h);
			if (!sawData) {
				if (!chunks.isEmpty())
					error("Multiple fcTL chunks are not allowed before IDAT");
				if (!bounds.equals(headerBounds))
					error("Default image frame must match IHDR bounds");
				useDefaultImage = true;
			}
			if (!headerBounds.contains(bounds))
				error("Frame bounds must fall within IHDR bounds");

			int delayNum = in.readUnsignedShort();
			int delayDen = in.readUnsignedShort();
			if (delayDen == 0)
				delayDen = 100;

			int disposeOp = in.readByte();
			switch (disposeOp) {
				case FrameControl.DISPOSE_NONE:
				case FrameControl.DISPOSE_BACKGROUND:
					break;
				case FrameControl.DISPOSE_PREVIOUS:
					if (chunks.isEmpty())
						disposeOp = FrameControl.DISPOSE_BACKGROUND;
					break;
				default:
					error("Unknown APNG dispose op " + disposeOp);
			}

			int blendOp = in.readByte();
			switch (blendOp) {
				case FrameControl.BLEND_OVER:
				case FrameControl.BLEND_SOURCE:
					break;
				default:
					error("Unknown APNG blend op " + blendOp);
			}
			return new FrameControl(bounds, (float)delayNum / delayDen, disposeOp, blendOp);
		}

		private void validate()
		throws IOException
		{
			if (!animated) {
				frames.add(null);
				return;
			}
			try {
				if (chunks.isEmpty())
					error("Found zero frames");

				List list = null;
				for (int i = 0; i < chunks.size(); i++) {
					Object chunk = chunks.get(i);
					if (chunk instanceof FrameControl) {
						frames.add(chunk);
						frameData.put(chunk, list = new ArrayList());
					} else {
						list.add(chunk);
					}
				}

				if (frames.size() != numFrames)
					error("Found " + frames.size() + " frames, expected " + numFrames);

				if (useDefaultImage)
					((List)frameData.get(frames.get(0))).addAll(defaultImageData);

				for (int i = 0; i < frames.size(); i++) {
					if (((List)frameData.get(frames.get(i))).isEmpty())
						error("Missing data for frame");
				}
				chunks.clear();

			} catch (IOException e) {
				animated = false;
				throw e;
			}
		}
	
		
/////////////////////////============== PngImage start ==================//////////////////

    public PngConfig getConfig()
    {
        return config;
    }
	
		
	public Bitmap read(InputStream in, boolean close)
    throws IOException
    {
        if (in == null)
            throw new NullPointerException("InputStream is null");
        this.read = true;
        props.clear();

        int readLimit = config.getReadLimit();
        Bitmap image = null;
        StateMachine machine = new StateMachine(this);
        try {
            PngInputStream pin = new PngInputStream(in);
            Set seen = new HashSet();
            while (machine.getState() != StateMachine.STATE_END) {
                int type = pin.startChunk();
                machine.nextState(type);
                try {
                    if (type == PngConstants.IDAT) {
                        switch (readLimit) {
							case PngConfig.READ_UNTIL_DATA:
								return null;
							case PngConfig.READ_EXCEPT_DATA:
								break;
							default:
								ImageDataInputStream data = new ImageDataInputStream(pin, machine);
								image = createImage(data, new Dimension(getWidth(), getHeight()));
								while ((type = machine.getType()) == PngConstants.IDAT) {
									skipFully(data, pin.getRemaining());
								}
                        }
                    }
                    if (!isMultipleOK(type) && !seen.add(Integers.valueOf(type)))
                        throw new IOException("Multiple " + PngConstants.getChunkName(type) + " chunks are not allowed");
                    try {
                        readChunk(type, pin, pin.getOffset(), pin.getRemaining());
                    } catch (IOException e) {
                        throw e;
                    }
                    skipFully(pin, pin.getRemaining());
                    if (type == PngConstants.IHDR && readLimit == PngConfig.READ_HEADER)
                        return null;
                } catch (IOException exception) {
                        throw exception;
                }
                pin.endChunk(type);
            }
            return image;
        } finally {
            if (close)
                in.close();
        }
    }

    protected Bitmap createImage(InputStream in, Dimension size)
    throws IOException
    {
    	return BitmapFactory.decodeStream(in);
    	//throw new IOException("Could not implemented ImageFactory.createImage(this, in, size);");
///////// !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!    	
        //return ImageFactory.createImage(this, in, size);
    }

    protected boolean handlePass(Bitmap image, int pass)
    {
        return true;
    }

    protected boolean handleProgress(Bitmap image, float pct)
    {
        return true;
    }

    protected void handleWarning(IOException e)
    throws IOException
    {
        if (config.getWarningsFatal())
            throw e;
    }

    public int getWidth()
    {
        return getInt(PngConstants.WIDTH);
    }

    public int getHeight()
    {
        return getInt(PngConstants.HEIGHT);
    }

    public int getBitDepth()
    {
        return getInt(PngConstants.BIT_DEPTH);
    }

    public boolean isInterlaced()
    {
        return getInt(PngConstants.INTERLACE) != PngConstants.INTERLACE_NONE;
    }

    public int getColorType()
    {
        return getInt(PngConstants.COLOR_TYPE);
    }

    public int getTransparency()
    {
        int colorType = getColorType();
        return (colorType == PngConstants.COLOR_TYPE_RGB_ALPHA ||
			colorType == PngConstants.COLOR_TYPE_GRAY_ALPHA ||
			props.containsKey(PngConstants.TRANSPARENCY) ||
			props.containsKey(PngConstants.PALETTE_ALPHA)) ?
            TRANSLUCENT : OPAQUE;
    }

    public int getSamples()
    {
        switch (getColorType()) {
			case PngConstants.COLOR_TYPE_GRAY_ALPHA: return 2;
			case PngConstants.COLOR_TYPE_RGB:        return 3;
			case PngConstants.COLOR_TYPE_RGB_ALPHA:  return 4;
        }
        return 1;
    }

    public float getGamma()
    {
        assertRead();
        if (props.containsKey(PngConstants.GAMMA))
            return ((Number)getProperty(PngConstants.GAMMA, Number.class, true)).floatValue();
        return config.getDefaultGamma();
    }

    public short[] getGammaTable()
    {
        assertRead();
        return createGammaTable(getGamma(),
                                config.getDisplayExponent(),
                                getBitDepth() == 16 && !config.getReduce16());
    }

    static short[] createGammaTable(float gamma, float displayExponent, boolean large)
    {
        int size = 1 << (large ? 16 : 8);
        short[] gammaTable = new short[size];
        double decodingExponent = 1d / ((double)gamma * (double)displayExponent);
        for (int i = 0; i < size; i++)
            gammaTable[i] = (short)(Math.pow((double)i / (size - 1), decodingExponent) * (size - 1));
        return gammaTable;
    }

    public Color getBackground()
    {
        int[] background = (int[])getProperty(PngConstants.BACKGROUND, int[].class, false);
        if (background == null)
            return null;
        switch (getColorType()) {
			case PngConstants.COLOR_TYPE_PALETTE:
				byte[] palette = (byte[])getProperty(PngConstants.PALETTE, byte[].class, true);
				int index = background[0] * 3;
				Color colorp = new Color();
				colorp.rgb(0xFF & palette[index + 0], 
						 0xFF & palette[index + 1], 
						 0xFF & palette[index + 2]);
				return colorp;
			case PngConstants.COLOR_TYPE_GRAY:
			case PngConstants.COLOR_TYPE_GRAY_ALPHA:
				int gray = background[0] * 255 / ((1 << getBitDepth()) - 1);
				Color colorg = new Color();
				colorg.rgb(gray, gray, gray);
				return colorg;
			default:
				if (getBitDepth() == 16) {
					Color color = new Color();
					color.rgb(background[0] >> 8, background[1] >> 8, background[2] >> 8);
					return color;
				} else {
					Color color = new Color();
					color.rgb(background[0], background[1], background[2]);
					return color;
				}
        }
    }

    public Object getProperty(String name)
    {
        assertRead();
        return props.get(name);
    }

    Object getProperty(String name, Class type, boolean required)
    {
        assertRead();
        Object value = props.get(name);
        if (value == null) {
            if (required)
                throw new IllegalStateException("Image is missing property \"" + name + "\"");
        } else if (!type.isAssignableFrom(value.getClass())) {
            throw new IllegalStateException("Property \"" + name + "\" has type " + value.getClass().getName() + ", expected " + type.getName());
        }
        return value;
    }

    private int getInt(String name)
    {
        return ((Number)getProperty(name, Number.class, true)).intValue();
    }

    public Map getProperties()
    {
        return props;
    }

    public TextChunk getTextChunk(String key)
    {
        List list = (List)getProperty(PngConstants.TEXT_CHUNKS, List.class, false);
        if (key != null && list != null) {
            for (Iterator it = list.iterator(); it.hasNext();) {
                // TO-DO: check list value type before cast?
                TextChunk chunk = (TextChunk)it.next();
                if (chunk.getKeyword().equals(key))
                    return chunk;
            }
        }
        return null;
    }

    protected void superreadChunk(int type, DataInput in, long offset, int length)
    throws IOException
    {
        if (type == PngConstants.IDAT)
            return;
        if (config.getReadLimit() == PngConfig.READ_EXCEPT_METADATA && PngConstants.isAncillary(type)) {
            switch (type) {
				case PngConstants.gAMA:
				case PngConstants.tRNS:
					break;
				default:
					return;
            }
        }
        RegisteredChunks.read(type, in, length, this);
    }

    protected boolean superisMultipleOK(int type)
    {
        switch (type) {
			case PngConstants.IDAT:
			case PngConstants.sPLT:
			case PngConstants.iTXt:
			case PngConstants.tEXt:
			case PngConstants.zTXt:
				return true;
        }
        return false;
    }

    private void superassertRead()
    {
        if (!read)
            throw new IllegalStateException("Image has not been read");
    }

    private static void skipFully(InputStream in, long n) throws IOException {
        while (n > 0) {
            long amt = in.skip(n);
            if (amt == 0) {
                // Force a blocking read to avoid infinite loop
                if (in.read() == -1) {
                    throw new EOFException();
                }
                n--;
            } else {
                n -= amt;
            }
        }
    }
	
	public class FrameControl
	{
		/** TO-DO */
		public static final int DISPOSE_NONE = 0;
		/** TO-DO */
		public static final int DISPOSE_BACKGROUND = 1;
		/** TO-DO */
		public static final int DISPOSE_PREVIOUS = 2;

		/** TO-DO */
		public static final int BLEND_SOURCE = 0;
		/** TO-DO */
		public static final int BLEND_OVER = 1;

		private final Rectangle bounds;
		private final float delay;
		private final int dispose;
		private final int blend;

		FrameControl(Rectangle bounds, float delay, int dispose, int blend)
		{
			this.bounds = bounds;
			this.delay = delay;
			this.dispose = dispose;
			this.blend = blend;
		}

		/**
		 * TO-DO
		 */
		public Rectangle getBounds()
		{
			return new Rectangle(bounds);
		}

		/**
		 * TO-DO
		 */
		public float getDelay()
		{
			return delay;
		}

		/**
		 * TO-DO
		 */
		public int getDispose()
		{
			return dispose;
		}

		/**
		 * TO-DO
		 */
		public int getBlend()
		{
			return blend;
		}

		public String toString()
		{
			return "FrameControl{bounds=" + bounds + ",delay=" + delay +
				",dispose=" + dispose + ",blend=" + blend + "}";
		}
	}
	
	public static class Rectangle
	{

		public int x;
		public int y;
		public int width;
		public int height;

		public Rectangle(int width, int height) {
			this.x = 0;
			this.y = 0;
			this.width = width;
			this.height = height;
		}

		public boolean contains(Rectangle bounds) {
			Rect reg = new Rect(this.x, this.y, this.width, this.height);
			Rect reg1 = new Rect(bounds.x, bounds.y, bounds.width, bounds.height);
			return reg.contains(reg1);
		}

		public Dimension getSize() {
			// TO-DO Auto-generated method stub
			return new Dimension(this.width, this.height);
		}

		public Rectangle(int x, int y, int w, int h) {
			this.x = x;
			this.y = y;
			this.width = w;
			this.height = h;
		}

		public Rectangle(Rectangle sourceRegion) {
			this.x = sourceRegion.x;
			this.y = sourceRegion.y;
			this.width = sourceRegion.width;
			this.height = sourceRegion.height;
		}
		
	}
	
	
	class FrameDataInputStream
	extends InputStream
	{
		private final RandomAccessFile file;
		private final Iterator it;
		private InputStream in;

		public FrameDataInputStream(File file, List frameData)
		throws IOException
		{
			this.file = new RandomAccessFile(file, "r");
			this.it = frameData.iterator();
			advance();
		}

		public void close()
		throws IOException
		{
			if (in != null) {
				in.close();
				in = null;
				while (it.hasNext())
					it.next();
			}
			file.close();
		}

		private void advance()
		throws IOException
		{
			if (in != null)
				in.close();
			in = null;
			if (it.hasNext()) {
				// TO-DO: enable streaming
				FrameData data = (FrameData)it.next();
				file.seek(data.getOffset());
				byte[] bytes = new byte[data.getLength()];
				file.readFully(bytes);
				in = new ByteArrayInputStream(bytes);
			}
		}

		public int available()
		throws IOException
		{
			if (in == null)
				return 0;
			return in.available();
		}

		public boolean markSupported()
		{
			return false;
		}

		public int read()
		throws IOException
		{
			if (in == null)
				return -1;
			int result = in.read();
			if (result == -1) {
				advance();
				return read();
			}
			return result;
		}

		public int read(byte[] b, int off, int len)
		throws IOException
		{
			if (in == null)
				return -1;
			int result = in.read(b, off, len);
			if (result == -1) {
				advance();
				return read(b, off, len);
			}
			return result;
		}

		public long skip(long n)
		throws IOException
		{
			if (in == null)
				return 0;
			long result = in.skip(n);
			if (result != 0)
				return result;
			if (read() == -1)
				return 0;
			return 1;
		}
	}
	
	class FrameData
	{
		private final long offset;
		private final int length;

		public FrameData(long offset, int length)
		{
			this.offset = offset;
			this.length = length;
		}

		public long getOffset()
		{
			return offset;
		}

		public int getLength()
		{
			return length;
		}
	}	

    public static class PngConstants
	{

		/**
		 * Returns {@code true} if the given chunk type has the ancillary bit set
		 * (the first letter is lowercase).
		 * An ancillary chunk is once which is not strictly necessary
		 * in order to meaningfully display the contents of the file.
		 * @param chunkType the chunk type
		 * @return whether the chunk type ancillary bit is set
		 */
		public static boolean isAncillary(int chunkType)
		{
			return ((chunkType & 0x20000000) != 0);
		}

		/**
		 * Returns {@code true} if the given chunk type has the private bit set
		 * (the second letter is lowercase).
		 * All unregistered chunk types should have this bit set.
		 * @param chunkType the chunk type
		 * @return whether the chunk type private bit is set
		 */
		public static boolean isPrivate(int chunkType)
		{
			return ((chunkType & 0x00200000) != 0);
		}

		/**
		 * Returns {@code true} if the given chunk type has the reserved bit set
		 * (the third letter is lowercase).
		 * The meaning of this bit is currently undefined, but reserved for future use.
		 * Images conforming to the current version of the PNG specification must
		 * not have this bit set.
		 * @param chunkType the chunk type
		 * @return whether the chunk type reserved bit is set
		 */
		public static boolean isReserved(int chunkType)
		{
			return ((chunkType & 0x00002000) != 0);
		}

		/**
		 * Returns {@code true} if the given chunk type has the safe-to-copy bit set
		 * (the fourth letter is lowercase).
		 * Chunks marked as safe-to-copy may be copied to a modified PNG file
		 * whether or not the software recognizes the chunk type.
		 * @param chunkType the chunk type
		 * @return whether the chunk safe-to-copy bit is set
		 */
		public static boolean isSafeToCopy(int chunkType)
		{
			return ((chunkType & 0x00000020) != 0);
		}

		/**
		 * Returns the four-character ASCII name corresponding to the given
		 * chunk type. For example, {@code PngConstants.getChunkName(PngConstants.IHDR)} will
		 * return {@code "IHDR"}.
		 * @param chunkType the chunk type
		 * @return the four-character ASCII chunk name
		 */
		public static String getChunkName(int chunkType)
		{
			return ("" + 
                (char)((chunkType >>> 24) & 0xFF) + 
                (char)((chunkType >>> 16) & 0xFF) + 
                (char)((chunkType >>>  8) & 0xFF) + 
                (char)((chunkType       ) & 0xFF));
		}

		/**
		 * Returns the chunk type corresponding to the given four-character
		 * ASCII chunk name.
		 * @param chunkName the four-character ASCII chunk name
		 * @return the chunk type
		 * @throws NullPointerException if {@code name} is null
		 * @throws IndexOutOfBoundsException if {@code name} has less than four characters
		 */
		public static int getChunkType(String chunkName)
		{
			return ((((int)chunkName.charAt(0) & 0xFF) << 24) | 
                (((int)chunkName.charAt(1) & 0xFF) << 16) | 
                (((int)chunkName.charAt(2) & 0xFF) <<  8) | 
                (((int)chunkName.charAt(3) & 0xFF)      ));
		}

		/** Eight byte magic number that begins all PNG images */
		public static final long SIGNATURE = 0x89504E470D0A1A0AL;

		/** Image header */
		public static final int IHDR = 0x49484452;
		/** Palette */
		public static final int PLTE = 0x504c5445;
		/** Image data */
		public static final int IDAT = 0x49444154;
		/** Image trailer */
		public static final int IEND = 0x49454e44;

		/** Background color */
		public static final int bKGD = 0x624b4744;
		/** Primary chromaticities */
		public static final int cHRM = 0x6348524d;
		/** Image gamma */
		public static final int gAMA = 0x67414d41;
		/** Palette histogram */
		public static final int hIST = 0x68495354;
		/** Embedded ICC profile */
		public static final int iCCP = 0x69434350;
		/** International textual data */
		public static final int iTXt = 0x69545874;
		/** Physical pixel dimensions */
		public static final int pHYs = 0x70485973;
		/** Significant bits */
		public static final int sBIT = 0x73424954;
		/** Suggested palette */
		public static final int sPLT = 0x73504c54;
		/** Standard RGB color space */
		public static final int sRGB = 0x73524742;
		/** Textual data */
		public static final int tEXt = 0x74455874;
		/** Image last-modification time */
		public static final int tIME = 0x74494d45;
		/** Transparency */
		public static final int tRNS = 0x74524e53;
		/** Compressed textual data */
		public static final int zTXt = 0x7a545874;

		/** Image offset */
		public static final int oFFs = 0x6f464673;
		/** Calibration of pixel values */
		public static final int pCAL = 0x7043414c;
		/** Physical scale of image subject */
		public static final int sCAL = 0x7343414c;
		/** GIF Graphic Control Extension */
		public static final int gIFg = 0x67494667;
		/** GIF Application Extension */
		public static final int gIFx = 0x67494678;
		/** Indicator of Stereo Image */
		public static final int sTER = 0x73544552;


		/** {@link #IHDR IHDR}: Bit depth */
		public static final String BIT_DEPTH = "bit_depth";
		/** {@link #IHDR IHDR}: Color type */
		public static final String COLOR_TYPE = "color_type";
		/** {@link #IHDR IHDR}: Compression method */
		public static final String COMPRESSION = "compression";
		/** {@link #IHDR IHDR}: Filter method */
		public static final String FILTER = "filter";
		/** {@link #gAMA gAMA}: Gamma */
		public static final String GAMMA = "gamma";
		/** {@link #IHDR IHDR}: Width */
		public static final String WIDTH = "width";
		/** {@link #IHDR IHDR}: Height */
		public static final String HEIGHT = "height";
		/** {@link #IHDR IHDR}: Interlace method */
		public static final String INTERLACE = "interlace";
		/** {@link #PLTE PLTE}: Palette entries */
		public static final String PALETTE = "palette";
		/** {@link #PLTE PLTE}: Palette alpha */
		public static final String PALETTE_ALPHA = "palette_alpha";
		/** {@link #tRNS tRNS}: Transparency samples */
		public static final String TRANSPARENCY = "transparency";
		/** {@link #bKGD bKGD}: Background samples */
		public static final String BACKGROUND = "background_rgb";
		/** {@link #pHYs pHYs}: Pixels per unit, X axis */
		public static final String PIXELS_PER_UNIT_X = "pixels_per_unit_x";
		/** {@link #pHYs pHYs}: Pixels per unit, Y axis */
		public static final String PIXELS_PER_UNIT_Y = "pixels_per_unit_y";
		/** {@link #sRGB sRGB}: Rendering intent */
		public static final String RENDERING_INTENT = "rendering_intent";
		/** {@link #sBIT sBIT}: Significant bits */
		public static final String SIGNIFICANT_BITS = "significant_bits";
		/** {@link #tEXt tEXt}/{@link #zTXt zTXt}/{@link #iTXt iTXt}: List of {@linkplain TextChunk text chunks} */
		public static final String TEXT_CHUNKS = "text_chunks";
		/** {@link #tIME tIME}: Image last-modification time */
		public static final String TIME = "time";
		/** {@link #pHYs pHYs}: Unit specifier */
		public static final String UNIT = "unit";
		/** {@link #cHRM cHRM}: Chromaticity */
		public static final String CHROMATICITY = "chromaticity";
		/** {@link #iCCP iCCP}: ICC profile */
		public static final String ICC_PROFILE = "icc_profile";
		/** {@link #iCCP iCCP}: ICC profile name */
		public static final String ICC_PROFILE_NAME = "icc_profile_name";
		/** {@link #hIST hIST}: Palette histogram */
		public static final String HISTOGRAM = "histogram";
		/** {@link #sPLT sPLT}: List of {@linkplain SuggestedPalette suggested palettes} */
		public static final String SUGGESTED_PALETTES = "suggested_palettes";

		/** {@link #gIFg gIFg}: GIF disposal method */
		public static final String GIF_DISPOSAL_METHOD = "gif_disposal_method";
		/** {@link #gIFg gIFg}: GIF user input flag */
		public static final String GIF_USER_INPUT_FLAG = "gif_user_input_flag";
		/** {@link #gIFg gIFg}: GIF delay time (hundredths of a second) */
		public static final String GIF_DELAY_TIME = "gif_delay_time";
		/** {@link #sCAL sCAL}: Unit for physical scale of image subject */
		public static final String SCALE_UNIT = "scale_unit";
		/** {@link #sCAL sCAL}: Physical width of pixel */
		public static final String PIXEL_WIDTH = "pixel_width";
		/** {@link #sCAL sCAL}: Physical height of pixel */
		public static final String PIXEL_HEIGHT = "pixel_height";
		/** {@link #oFFs oFFs}: Unit for image offset */
		public static final String POSITION_UNIT = "position_unit";
		/** {@link #sTER sTER}: Indicator of stereo image */
		public static final String STEREO_MODE = "stereo_mode";

		/** {@link #IHDR IHDR}: Grayscale color type */
		public static final int COLOR_TYPE_GRAY = 0;
		/** {@link #IHDR IHDR}: Grayscale+alpha color type */
		public static final int COLOR_TYPE_GRAY_ALPHA = 4;
		/** {@link #IHDR IHDR}: Palette color type */
		public static final int COLOR_TYPE_PALETTE = 3;
		/** {@link #IHDR IHDR}: RGB color type */
		public static final int COLOR_TYPE_RGB = 2;
		/** {@link #IHDR IHDR}: RGBA color type */
		public static final int COLOR_TYPE_RGB_ALPHA = 6;

		/** {@link #IHDR IHDR}: No interlace */
		public static final int INTERLACE_NONE = 0;
		/** {@link #IHDR IHDR}: Adam7 interlace */
		public static final int INTERLACE_ADAM7 = 1;

		/** {@link #IHDR IHDR}: Adaptive filtering */
		public static final int FILTER_BASE = 0;

		/** {@link #IHDR IHDR}: Deflate/inflate compression */
		public static final int COMPRESSION_BASE = 0;  

		/** {@link #pHYs pHYs}: Unit is unknown */
		public static final int UNIT_UNKNOWN = 0;
		/** {@link #pHYs pHYs}: Unit is the meter */
		public static final int UNIT_METER = 1;

		/** {@link #sRGB sRGB}: Perceptual rendering intent */
		public static final int SRGB_PERCEPTUAL = 0;
		/** {@link #sRGB sRGB}: Relative colorimetric rendering intent */
		public static final int SRGB_RELATIVE_COLORIMETRIC = 1;
		/** {@link #sRGB sRGB}: Saturation rendering intent */
		public static final int SRGB_SATURATION_PRESERVING = 2;
		/** {@link #sRGB sRGB}: Absolute colormetric rendering intent */
		public static final int SRGB_ABSOLUTE_COLORIMETRIC = 3;

		/** {@link #oFFs oFFs}: Image X position */
		public static final String POSITION_X = "position_x";
		/** {@link #oFFs oFFs}: Image Y position */
		public static final String POSITION_Y = "position_y";
		/** {@link #oFFs oFFs}: Unit is the pixel (true dimensions unspecified) */
		public static final int POSITION_UNIT_PIXEL = 0;
		/** {@link #oFFs oFFs}: Unit is the micrometer (10^-6 meter) */
		public static final int POSITION_UNIT_MICROMETER = 1;

		/** {@link #sCAL sCAL}: Unit is the meter */
		public static final int SCALE_UNIT_METER = 1;
		/** {@link #sCAL sCAL}: Unit is the radian */
		public static final int SCALE_UNIT_RADIAN = 2;

		/** {@link #sTER sTER}: Cross-fuse layout */
		public static final int STEREO_MODE_CROSS = 0;
		/** {@link #sTER sTER}: Diverging-fuse layout */
		public static final int STEREO_MODE_DIVERGING = 1;
	}
	
    static class Integers
    {
        public static Integer valueOf(int i)
        {
            switch (i) {
            case 0: return INT_0;
            case 1: return INT_1;
            case 2: return INT_2;
            case 3: return INT_3;
            case 4: return INT_4;
            case 5: return INT_5;
            case 6: return INT_6;
            case 7: return INT_7;
            case 8: return INT_8;
            default:
                return new Integer(i);
            }
        }

        private static final Integer INT_0 = new Integer(0);
        private static final Integer INT_1 = new Integer(1);
        private static final Integer INT_2 = new Integer(2);
        private static final Integer INT_3 = new Integer(3);
        private static final Integer INT_4 = new Integer(4);
        private static final Integer INT_5 = new Integer(5);
        private static final Integer INT_6 = new Integer(6);
        private static final Integer INT_7 = new Integer(7);
        private static final Integer INT_8 = new Integer(8);
    }	
	static class RegisteredChunks
{
    private static final TimeZone TIME_ZONE = TimeZone.getTimeZone("UTC");
    private static final String ISO_8859_1 = "ISO-8859-1";
    private static final String US_ASCII = "US-ASCII";
    private static final String UTF_8 = "UTF-8";

    public static boolean read(int type, DataInput in, int length, ApngDecoder png)
    throws IOException
    {
        Map props = png.getProperties();
        switch (type) {
        case PngConstants.IHDR: read_IHDR(in, length, props); break;
        case PngConstants.IEND: checkLength(PngConstants.IEND, length, 0); break;
        case PngConstants.PLTE: read_PLTE(in, length, props, png); break;
        case PngConstants.bKGD: read_bKGD(in, length, props, png); break;
        case PngConstants.tRNS: read_tRNS(in, length, props, png); break;
        case PngConstants.sBIT: read_sBIT(in, length, props, png); break;
        case PngConstants.hIST: read_hIST(in, length, props, png); break;
        case PngConstants.sPLT: read_sPLT(in, length, props, png); break;
        case PngConstants.cHRM: read_cHRM(in, length, props); break;
        case PngConstants.gAMA: read_gAMA(in, length, props); break;
        case PngConstants.iCCP: read_iCCP(in, length, props); break;
        case PngConstants.pHYs: read_pHYs(in, length, props); break;
        case PngConstants.sRGB: read_sRGB(in, length, props); break;
        case PngConstants.tIME: read_tIME(in, length, props); break;
        case PngConstants.gIFg: read_gIFg(in, length, props); break;
        case PngConstants.oFFs: read_oFFs(in, length, props); break;
        case PngConstants.sCAL: read_sCAL(in, length, props); break;
        case PngConstants.sTER: read_sTER(in, length, props); break;
            // case PngConstants.gIFx: read_gIFx(in, length, props); break;
        case PngConstants.iTXt:
        case PngConstants.tEXt:
        case PngConstants.zTXt:
            readText(type, in, length, props, png);
            break;
        default:
            return false;
        }
        return true;
    }

    private static void read_IHDR(DataInput in, int length, Map props)
    throws IOException
    {
        checkLength(PngConstants.IHDR, length, 13);
        int width = in.readInt();
        int height = in.readInt();
        if (width <= 0 || height <= 0)
            throw new IOException("Bad image size: " + width + "x" + height);

        byte bitDepth = in.readByte();
        switch (bitDepth) {
        case 1: case 2: case 4: case 8: case 16: break;
        default: throw new IOException("Bad bit depth: " + bitDepth);
        }

        byte[] sbits = null;
        int colorType = in.readUnsignedByte();
        switch (colorType) {
        case PngConstants.COLOR_TYPE_RGB:
        case PngConstants.COLOR_TYPE_GRAY: 
            break;
        case PngConstants.COLOR_TYPE_PALETTE: 
            if (bitDepth == 16)
                throw new IOException("Bad bit depth for color type " + colorType + ": " + bitDepth);
            break;
        case PngConstants.COLOR_TYPE_GRAY_ALPHA: 
        case PngConstants.COLOR_TYPE_RGB_ALPHA: 
            if (bitDepth <= 4)
                throw new IOException("Bad bit depth for color type " + colorType + ": " + bitDepth);
            break;
        default:
            throw new IOException("Bad color type: " + colorType);
        }

        int compression = in.readUnsignedByte();
        if (compression != PngConstants.COMPRESSION_BASE) 
            throw new IOException("Unrecognized compression method: " + compression);

        int filter = in.readUnsignedByte();
        if (filter != PngConstants.FILTER_BASE)
            throw new IOException("Unrecognized filter method: " + filter);

        int interlace = in.readUnsignedByte();
        switch (interlace) {
        case PngConstants.INTERLACE_NONE:
        case PngConstants.INTERLACE_ADAM7:
            break;
        default:
            throw new IOException("Unrecognized interlace method: " + interlace);
        }

        props.put(PngConstants.WIDTH, Integers.valueOf(width));
        props.put(PngConstants.HEIGHT, Integers.valueOf(height));
        props.put(PngConstants.BIT_DEPTH, Integers.valueOf(bitDepth));
        props.put(PngConstants.INTERLACE, Integers.valueOf(interlace));
        props.put(PngConstants.COMPRESSION, Integers.valueOf(compression));
        props.put(PngConstants.FILTER, Integers.valueOf(filter));
        props.put(PngConstants.COLOR_TYPE, Integers.valueOf(colorType));
    }

    private static void read_PLTE(DataInput in, int length, Map props, ApngDecoder png)
    throws IOException
    {
        if (length == 0)
            throw new IOException("PLTE chunk cannot be empty");
        if (length % 3 != 0)
            throw new IOException("PLTE chunk length indivisible by 3: " + length);
        int size = length / 3;
        if (size > 256)
            throw new IOException("Too many palette entries: " + size);
        switch (png.getColorType()) {
        case PngConstants.COLOR_TYPE_PALETTE:
            if (size > (2 << (png.getBitDepth() - 1)))
                throw new IOException("Too many palette entries: " + size);
            break;
        case PngConstants.COLOR_TYPE_GRAY:
        case PngConstants.COLOR_TYPE_GRAY_ALPHA:
            throw new IOException("PLTE chunk found in grayscale image");
        }
        byte[] palette = new byte[length];
        in.readFully(palette);
        props.put(PngConstants.PALETTE, palette);
    }

    private static void read_tRNS(DataInput in, int length, Map props, ApngDecoder png)
    throws IOException
    {
        switch (png.getColorType()) {
        case PngConstants.COLOR_TYPE_GRAY:
            checkLength(PngConstants.tRNS, length, 2);
            props.put(PngConstants.TRANSPARENCY, new int[]{ in.readUnsignedShort() });
            break;
        case PngConstants.COLOR_TYPE_RGB:
            checkLength(PngConstants.tRNS, length, 6);
            props.put(PngConstants.TRANSPARENCY, new int[]{
                in.readUnsignedShort(),
                in.readUnsignedShort(),
                in.readUnsignedShort(),
            });
            break;
        case PngConstants.COLOR_TYPE_PALETTE:
            int paletteSize = ((byte[])png.getProperty(PngConstants.PALETTE, byte[].class, true)).length / 3;
            if (length > paletteSize)
                throw new IOException("Too many transparency palette entries (" + length + " > " + paletteSize + ")");
            byte[] trans = new byte[length];
            in.readFully(trans);
            props.put(PngConstants.PALETTE_ALPHA, trans);
            break;
        default:
            throw new IOException("tRNS prohibited for color type " + png.getColorType());
        }
    }

    private static void read_bKGD(DataInput in, int length, Map props, ApngDecoder png)
    throws IOException
    {
        int[] background;
        switch (png.getColorType()) {
        case PngConstants.COLOR_TYPE_PALETTE:
            checkLength(PngConstants.bKGD, length, 1);
            background = new int[]{ in.readUnsignedByte() };
            break;
        case PngConstants.COLOR_TYPE_GRAY:
        case PngConstants.COLOR_TYPE_GRAY_ALPHA:
            checkLength(PngConstants.bKGD, length, 2);
            background = new int[]{ in.readUnsignedShort() };
            break;
        default:
            // truecolor
            checkLength(PngConstants.bKGD, length, 6);
            background = new int[]{
                in.readUnsignedShort(),
                in.readUnsignedShort(),
                in.readUnsignedShort(),
            };
        }
        props.put(PngConstants.BACKGROUND, background);
    }

    private static void read_cHRM(DataInput in, int length, Map props)
    throws IOException
    {
        checkLength(PngConstants.cHRM, length, 32);
        float[] array = new float[8];
        for (int i = 0; i < 8; i++)
            array[i] = in.readInt() / 100000f;
        if (!props.containsKey(PngConstants.CHROMATICITY))
            props.put(PngConstants.CHROMATICITY, array);
    }

    private static void read_gAMA(DataInput in, int length, Map props)
    throws IOException
    {
        checkLength(PngConstants.gAMA, length, 4);
        int gamma = in.readInt();
        if (gamma == 0)
            throw new IOException("Meaningless zero gAMA chunk value");
        if (!props.containsKey(PngConstants.RENDERING_INTENT))
            props.put(PngConstants.GAMMA, new Float(gamma / 100000f));
    }

    private static void read_hIST(DataInput in, int length, Map props, ApngDecoder png)
    throws IOException
    {
        // TO-DO: ensure it is divisible by three
        int paletteSize = ((byte[])png.getProperty(PngConstants.PALETTE, byte[].class, true)).length / 3;
        checkLength(PngConstants.hIST, length, paletteSize * 2);
        int[] array = new int[paletteSize];
        for (int i = 0; i < paletteSize; i++)
            array[i] = in.readUnsignedShort();
        props.put(PngConstants.HISTOGRAM, array);
    }

    private static void read_iCCP(DataInput in, int length, Map props)
    throws IOException
    {
        String name = readKeyword(in, length);
        byte[] data = readCompressed(in, length - name.length() - 1, true);
        props.put(PngConstants.ICC_PROFILE_NAME, name);
        props.put(PngConstants.ICC_PROFILE, data);
    }

    private static void read_pHYs(DataInput in, int length, Map props)
    throws IOException
    {
        checkLength(PngConstants.pHYs, length, 9);
        int pixelsPerUnitX = in.readInt();
        int pixelsPerUnitY = in.readInt();
        int unit = in.readUnsignedByte();
        if (unit != PngConstants.UNIT_UNKNOWN && unit != PngConstants.UNIT_METER)
            throw new IOException("Illegal pHYs chunk unit specifier: " + unit);
        props.put(PngConstants.PIXELS_PER_UNIT_X, Integers.valueOf(pixelsPerUnitX));
        props.put(PngConstants.PIXELS_PER_UNIT_Y, Integers.valueOf(pixelsPerUnitY));
        props.put(PngConstants.UNIT, Integers.valueOf(unit));
    }

    private static void read_sBIT(DataInput in, int length, Map props, ApngDecoder png)
    throws IOException
    {
        boolean paletted = png.getColorType() == PngConstants.COLOR_TYPE_PALETTE;
        int count = paletted ? 3 : png.getSamples();
        checkLength(PngConstants.sBIT, length, count);
        int depth = paletted ? 8 : png.getBitDepth();
        byte[] array = new byte[count];
        for (int i = 0; i < count; i++) {
            byte bits = in.readByte();
            if (bits <= 0 || bits > depth)
                throw new IOException("Illegal sBIT sample depth");
            array[i] = bits;
        }
        props.put(PngConstants.SIGNIFICANT_BITS, array);
    }

    private static void read_sRGB(DataInput in, int length, Map props)
    throws IOException
    {
        checkLength(PngConstants.sRGB, length, 1);
        int intent = in.readByte();
        props.put(PngConstants.RENDERING_INTENT, Integers.valueOf(intent));
        props.put(PngConstants.GAMMA, new Float(0.45455));
        props.put(PngConstants.CHROMATICITY, new float[]{
            0.3127f, 0.329f, 0.64f, 0.33f, 0.3f, 0.6f, 0.15f, 0.06f,
        });
    }

    private static void read_tIME(DataInput in, int length, Map props)
    throws IOException
    {
        checkLength(PngConstants.tIME, length, 7);
        Calendar cal = Calendar.getInstance(TIME_ZONE);
        cal.set(in.readUnsignedShort(),
                check(in.readUnsignedByte(), 1, 12, "month") - 1,
                check(in.readUnsignedByte(), 1, 31, "day"),
                check(in.readUnsignedByte(), 0, 23, "hour"),
                check(in.readUnsignedByte(), 0, 59, "minute"),
                check(in.readUnsignedByte(), 0, 60, "second"));
        props.put(PngConstants.TIME, cal.getTime());
    }

    private static int check(int value, int min, int max, String field)
    throws IOException
    {
        if (value < min || value > max)
            throw new IOException("tIME " + field + " value " + value +
                                   " is out of bounds (" + min + "-" + max + ")");
        return value;
    }

    private static void read_sPLT(DataInput in, int length, Map props, ApngDecoder png)
    throws IOException
    {
        String name = readKeyword(in, length);
        int sampleDepth = in.readByte();
        if (sampleDepth != 8 && sampleDepth != 16)
            throw new IOException("Sample depth must be 8 or 16");
        
        length -= (name.length() + 2);
        if ((length % ((sampleDepth == 8) ? 6 : 10)) != 0)
            throw new IOException("Incorrect sPLT data length for given sample depth");
        byte[] bytes = new byte[length];
        in.readFully(bytes);

        List palettes = (List)png.getProperty(PngConstants.SUGGESTED_PALETTES, List.class, false);
        if (palettes == null)
            props.put(PngConstants.SUGGESTED_PALETTES, palettes = new ArrayList());
        for (Iterator it = palettes.iterator(); it.hasNext();) {
            if (name.equals(((SuggestedPalette)it.next()).getName()))
                throw new IOException("Duplicate suggested palette name " + name);
        }
        palettes.add(new SuggestedPalette(name, sampleDepth, bytes));
    }

    private static void readText(int type, DataInput in, int length, Map props, ApngDecoder png)
    throws IOException
    {
        byte[] bytes = new byte[length];
        in.readFully(bytes);
        DataInputStream data = new DataInputStream(new ByteArrayInputStream(bytes));

        String keyword = readKeyword(data, length);
        String enc = ISO_8859_1;
        boolean compressed = false;
        boolean readMethod = true;
        String language = null;
        String translated = null;
        switch (type) {
        case PngConstants.tEXt:
            break;
        case PngConstants.zTXt:
            compressed = true;
            break;
        case PngConstants.iTXt:
            enc = UTF_8;
            int flag = data.readByte();
            int method = data.readByte();
            if (flag == 1) {
                compressed = true;
                readMethod = false;
                if (method != 0)
                    throw new IOException("Unrecognized " + PngConstants.getChunkName(type) + " compression method: " + method);
            } else if (flag != 0) {
                throw new IOException("Illegal " + PngConstants.getChunkName(type) + " compression flag: " + flag);
            }
            language = readString(data, data.available(), US_ASCII);
            // TO-DO: split language on hyphens, check that each component is 1-8 characters
            translated = readString(data, data.available(), UTF_8);
            // TO-DO: check for line breaks?
        }

        String text;
        if (compressed) {
            text = new String(readCompressed(data, data.available(), readMethod), enc);
        } else {
            text = new String(bytes, bytes.length - data.available(), data.available(), enc);
        }
        if (text.indexOf('\0') >= 0)
            throw new IOException("Text value contains null");
        List chunks = (List)png.getProperty(PngConstants.TEXT_CHUNKS, List.class, false);
        if (chunks == null)
            props.put(PngConstants.TEXT_CHUNKS, chunks = new ArrayList());
        chunks.add(new TextChunk(keyword, text, language, translated, type));
    }

    private static void read_gIFg(DataInput in, int length, Map props)
    throws IOException
    {
        checkLength(PngConstants.gIFg, length, 4);
        int disposalMethod = in.readUnsignedByte();
        int userInputFlag = in.readUnsignedByte();
        int delayTime = in.readUnsignedShort();
        props.put(PngConstants.GIF_DISPOSAL_METHOD, Integers.valueOf(disposalMethod));
        props.put(PngConstants.GIF_USER_INPUT_FLAG, Integers.valueOf(userInputFlag));
        props.put(PngConstants.GIF_DELAY_TIME, Integers.valueOf(delayTime));
    }

    private static void read_oFFs(DataInput in, int length, Map props)
    throws IOException
    {
        checkLength(PngConstants.oFFs, length, 9);
        int x = in.readInt();
        int y = in.readInt();
        int unit = in.readByte();
        if (unit != PngConstants.POSITION_UNIT_PIXEL &&
            unit != PngConstants.POSITION_UNIT_MICROMETER)
            throw new IOException("Illegal oFFs chunk unit specifier: " + unit);
        props.put(PngConstants.POSITION_X, Integers.valueOf(x));
        props.put(PngConstants.POSITION_Y, Integers.valueOf(y));
        props.put(PngConstants.POSITION_UNIT, Integers.valueOf(unit));
    }

    private static void read_sCAL(DataInput in, int length, Map props)
    throws IOException
    {
        byte[] bytes = new byte[length];
        in.readFully(bytes);
        DataInputStream data = new DataInputStream(new ByteArrayInputStream(bytes));
        
        int unit = data.readByte();
        if (unit != PngConstants.SCALE_UNIT_METER && unit != PngConstants.SCALE_UNIT_RADIAN)
            throw new IOException("Illegal sCAL chunk unit specifier: " + unit);
        double width = readFloatingPoint(data, data.available());
        double height = readFloatingPoint(data, data.available());
        if (width <= 0 || height <= 0)
            throw new IOException("sCAL measurements must be >= 0");
        props.put(PngConstants.SCALE_UNIT, Integers.valueOf(unit));
        props.put(PngConstants.PIXEL_WIDTH, new Double(width));
        props.put(PngConstants.PIXEL_HEIGHT, new Double(height));
    }

    private static void read_sTER(DataInput in, int length, Map props)
    throws IOException
    {
        checkLength(PngConstants.sTER, length, 1);
        int mode = in.readByte();
        switch (mode) {
        case PngConstants.STEREO_MODE_CROSS:
        case PngConstants.STEREO_MODE_DIVERGING:
            props.put(PngConstants.STEREO_MODE, Integers.valueOf(mode));
            break;
        default:
            throw new IOException("Unknown sTER mode: " + mode);
        }
    }

    public static void checkLength(int chunk, int length, int correct)
    throws IOException
    {
        if (length != correct)
            throw new IOException("Bad " + PngConstants.getChunkName(chunk) + " chunk length: " + length + " (expected " + correct + ")");
    }

    private static byte[] readCompressed(DataInput in, int length, boolean readMethod)
    throws IOException
    {
        if (readMethod) {
            int method = in.readByte();
            if (method != 0)
                throw new IOException("Unrecognized compression method: " + method);
            length--;
        }
        byte[] data = new byte[length];
        in.readFully(data);
        byte[] tmp = new byte[0x1000];
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Inflater inf = new Inflater();
        inf.reset();
        inf.setInput(data, 0, length);
        try {
            while (!inf.needsInput()) {
                out.write(tmp, 0, inf.inflate(tmp));
            }
        } catch (DataFormatException e) {
            throw new IOException("Error reading compressed data", e);
        }
        return out.toByteArray();
    }

    private static String readString(DataInput in, int limit, String enc)
    throws IOException
    {
        return new String(readToNull(in, limit), enc);
    }

    private static String readKeyword(DataInput in, int limit)
    throws IOException
    {
        String keyword = readString(in, limit, ISO_8859_1);
        if (keyword.length() == 0 || keyword.length() > 79)
            throw new IOException("Invalid keyword length: " + keyword.length());
        return keyword;
    }

    // TO-DO: performance
    private static byte[] readToNull(DataInput in, int limit)
    throws IOException
    {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (int i = 0; i < limit; i++) {
            int c = in.readUnsignedByte();
            if (c == 0)
                return out.toByteArray();
            out.write(c);
        }
        return out.toByteArray();
    }

    private static double readFloatingPoint(DataInput in, int limit)
    throws IOException
    {
        String s = readString(in, limit, "US-ASCII");
        int e = Math.max(s.indexOf('e'), s.indexOf('E'));
        double d = Double.valueOf(s.substring(0, (e < 0 ? s.length() : e))).doubleValue();
        if (e >= 0)
            d *= Math.pow(10d, Double.valueOf(s.substring(e + 1)).doubleValue());
        return d;
    }
}

	public static class TextChunk
	{
	    private final String keyword;
	    private final String text;
	    private final String language;
	    private final String translated;
	    private final int type;
	    
	    public TextChunk(String keyword, String text, String language, String translated, int type)
	    {
	        this.keyword = keyword;
	        this.text = text;
	        this.language = language;
	        this.translated = translated;
	        this.type = type;
	    }
	    
	    public String getKeyword(){return keyword;}
	    public String getTranslatedKeyword(){return translated;}
	    public String getLanguage(){return language;}
	    public String getText(){return text;}
	    public int getType(){return type;}
	}
	
	public static class SuggestedPalette
	{
	    private final String name;
	    private final int sampleDepth;
	    private final byte[] bytes;
	    private final int entrySize;
	    private final int sampleCount;
	        
	    public SuggestedPalette(String name, int sampleDepth, byte[] bytes)
	    {
	        this.name = name;
	        this.sampleDepth = sampleDepth;
	        this.bytes = bytes;
	        entrySize = (sampleDepth == 8) ? 6 : 10;
	        sampleCount = bytes.length / entrySize;
	    }

	    public String getName()
	    {
	        return name;
	    }
	        
	    public int getSampleCount()
	    {
	        return sampleCount;
	    }
	        
	    public int getSampleDepth()
	    {
	        return sampleDepth;
	    }

	    public void getSample(int index, short[] pixel)
	    {
	        int from = index * entrySize;
	        if (sampleDepth == 8) {
	            for (int j = 0; j < 4; j++) {
	                int a = 0xFF & bytes[from++];
	                pixel[j] = (short)a;
	            }
	        } else {
	            for (int j = 0; j < 4; j++) {
	                int a = 0xFF & bytes[from++];
	                int b = 0xFF & bytes[from++];
	                pixel[j] = (short)((a << 8) | b);
	            }
	        }
	    }
	        
	    public int getFrequency(int index)
	    {
	        int from = (index + 1) * entrySize - 2;
	        int a = 0xFF & bytes[from];
	        int b = 0xFF & bytes[from + 1];
	        return ((a << 8) | b);
	    }
	}
	
	
	
	public class StateMachine
	{
	    public static final int STATE_START = 0;
	    public static final int STATE_SAW_IHDR = 1;
	    public static final int STATE_SAW_IHDR_NO_PLTE = 2;
	    public static final int STATE_SAW_PLTE = 3;
	    public static final int STATE_IN_IDAT = 4;
	    public static final int STATE_AFTER_IDAT = 5;
	    public static final int STATE_END = 6;

	    private ApngDecoder png;
	    private int state = STATE_START;
	    private int type;

	    public StateMachine(ApngDecoder png)
	    {
	        this.png = png;
	    }

	    public int getState()
	    {
	        return state;
	    }

	    public int getType()
	    {
	        return type;
	    }

	    public void nextState(int type)
	    throws IOException
	    {
	        state = nextState(png, state, this.type = type);
	    }
	        
	    private int nextState(ApngDecoder png, int state, int type)
	    throws IOException
	    {
	        for (int i = 0; i < 4; i++) {
	            int c = 0xFF & (type >>> (8 * i));
	            if (c < 65 || (c > 90 && c < 97) || c > 122)
	                throw new IOException("Corrupted chunk type: 0x" + Integer.toHexString(type));
	        }
	        if (PngConstants.isPrivate(type) && !PngConstants.isAncillary(type))
	            throw new IOException("Private critical chunk encountered: " + PngConstants.getChunkName(type));
	        switch (state) {
	        case STATE_START:
	            if (type == PngConstants.IHDR)
	                return STATE_SAW_IHDR;
	            throw new IOException("IHDR chunk must be first chunk");
	        case STATE_SAW_IHDR:
	        case STATE_SAW_IHDR_NO_PLTE:
	            switch (type) {
	            case PngConstants.PLTE:
	                return STATE_SAW_PLTE;
	            case PngConstants.IDAT:
	                errorIfPaletted(png);
	                return STATE_IN_IDAT;
	            case PngConstants.bKGD:
	                return STATE_SAW_IHDR_NO_PLTE;
	            case PngConstants.tRNS:
	                errorIfPaletted(png);
	                return STATE_SAW_IHDR_NO_PLTE;
	            case PngConstants.hIST:
	                throw new IOException("PLTE must precede hIST");
	            }
	            return state;
	        case STATE_SAW_PLTE:
	            switch (type) {
	            case PngConstants.cHRM:
	            case PngConstants.gAMA:
	            case PngConstants.iCCP:
	            case PngConstants.sBIT:
	            case PngConstants.sRGB:
	                throw new IOException(PngConstants.getChunkName(type) + " cannot appear after PLTE");
	            case PngConstants.IDAT:
	                return STATE_IN_IDAT;
	            case PngConstants.IEND:
	                throw new IOException("Required data chunk(s) not found");
	            }
	            return STATE_SAW_PLTE;
	        default: // STATE_IN_IDAT, STATE_AFTER_IDAT
	            switch (type) {
	            case PngConstants.PLTE:
	            case PngConstants.cHRM:
	            case PngConstants.gAMA:
	            case PngConstants.iCCP:
	            case PngConstants.sBIT:
	            case PngConstants.sRGB:
	            case PngConstants.bKGD:
	            case PngConstants.hIST:
	            case PngConstants.tRNS:
	            case PngConstants.pHYs:
	            case PngConstants.sPLT:
	            case PngConstants.oFFs:
	            case PngConstants.pCAL:
	            case PngConstants.sCAL:
	            case PngConstants.sTER:
	                throw new IOException(PngConstants.getChunkName(type) + " cannot appear after IDAT");
	            case PngConstants.IEND:
	                return STATE_END;
	            case PngConstants.IDAT:
	                if (state == STATE_IN_IDAT)
	                    return STATE_IN_IDAT;
	                throw new IOException("IDAT chunks must be consecutive");
	            }
	            return STATE_AFTER_IDAT;
	        }
	    }

	    private void errorIfPaletted(ApngDecoder png)
	    throws IOException
	    {
	        if (png.getColorType() == PngConstants.COLOR_TYPE_PALETTE)
	            throw new IOException("Required PLTE chunk not found");
	    }
	}
	class ImageDataInputStream
	extends InputStream
	{
	    private final PngInputStream in;
	    private final StateMachine machine;
	    private final byte[] onebyte = new byte[1];
	    private boolean done;

	    public ImageDataInputStream(PngInputStream in, StateMachine machine)
	    {
	        this.in = in;
	        this.machine = machine;
	    }
	    
	    public int read()
	    throws IOException
	    {
	        return (read(onebyte, 0, 1) == -1) ? -1 : 0xFF & onebyte[0];
	    }

	    public int read(byte[] b, int off, int len)
	    throws IOException
	    {
	        if (done)
	            return -1;
	        try {
	            int total = 0;
	            while ((total != len) && !done) {
	                while ((total != len) && in.getRemaining() > 0) {
	                    int amt = Math.min(len - total, in.getRemaining());
	                    in.readFully(b, off + total, amt);
	                    total += amt;
	                }
	                if (in.getRemaining() <= 0) {
	                    in.endChunk(machine.getType());
	                    machine.nextState(in.startChunk());
	                    done = machine.getType() != PngConstants.IDAT;
	                }
	            }
	            return total;
	        } catch (EOFException e) {
	            done = true;
	            return -1;
	        }
	    }
	}
	
	final class PngInputStream
	extends InputStream
	implements DataInput
	{
	    private final CRC32 crc = new CRC32();
	    private final InputStream in;
	    private final DataInputStream data;
	    private final byte[] tmp = new byte[0x1000];
	    private long total;
	    private int length;
	    private int left;

	    public PngInputStream(InputStream in)
	    throws IOException
	    {
	        this.in = in;
	        data = new DataInputStream(this);
	        left = 8;
	        long sig = readLong();
	        if (sig != PngConstants.SIGNATURE) {
	            throw new IOException("Improper signature, expected 0x" +
	                                   Long.toHexString(PngConstants.SIGNATURE) + ", got 0x" +
	                                   Long.toHexString(sig));
	        }
	        total += 8;
	    }

	    public int startChunk()
	    throws IOException
	    {
	        left = 8; // length, type
	        length = readInt();
	        if (length < 0)
	            throw new IOException("Bad chunk length: " + (0xFFFFFFFFL & length));
	        crc.reset();
	        int type = readInt();
	        left = length;
	        total += 8;
	        return type;
	    }
	    
	    public int endChunk(int type)
	    throws IOException
	    {
	        if (getRemaining() != 0)
	            throw new IOException(PngConstants.getChunkName(type) + " read " + (length - left) + " bytes, expected " + length);
	        left = 4;
	        int actual = (int)crc.getValue();
	        int expect = readInt();
	        if (actual != expect)
	            throw new IOException("Bad CRC value for " + PngConstants.getChunkName(type) + " chunk");
	        total += length + 4;
	        return actual;
	    }

	    ////////// count/crc InputStream methods //////////

	    public int read()
	    throws IOException
	    {
	        if (left == 0)
	            return -1;
	        int result = in.read();
	        if (result != -1) {
	            crc.update(result);
	            left--;
	        }
	        return result;
	    }
	    
	    public int read(byte[] b, int off, int len)
	    throws IOException
	    {
	        if (len == 0)
	            return 0;
	        if (left == 0)
	            return -1;
	        int result = in.read(b, off, Math.min(left, len));
	        if (result != -1) {
	            crc.update(b, off, result);
	            left -= result;
	        }
	        return result;
	    }

	    public long skip(long n)
	    throws IOException
	    {
	        int result = read(tmp, 0, (int)Math.min(tmp.length, n));
	        return (result < 0) ? 0 : result;
	    }

	    public void close()
	    {
	        throw new UnsupportedOperationException("do not close me");
	    }
	    
	    ////////// DataInput methods we implement directly //////////

	    public boolean readBoolean()
	    throws IOException
	    {
	        return readUnsignedByte() != 0;
	    }

	    public int readUnsignedByte()
	    throws IOException
	    {
	        int a = read();
	        if (a < 0)
	            throw new EOFException();
	        return a;
	    }

	    public byte readByte()
	    throws IOException
	    {
	        return (byte)readUnsignedByte();
	    }

	    public int readUnsignedShort()
	    throws IOException
	    {
	        int a = read();
	        int b = read();
	        if ((a | b) < 0)
	            throw new EOFException();
	        return (a << 8) + (b << 0);
	    }

	    public short readShort()
	    throws IOException
	    {
	        return (short)readUnsignedShort();
	    }

	    public char readChar()
	    throws IOException
	    {
	        return (char)readUnsignedShort();
	    }

	    public int readInt()
	    throws IOException
	    {
	        int a = read();
	        int b = read();
	        int c = read();
	        int d = read();
	        if ((a | b | c | d) < 0)
	            throw new EOFException();
	        return ((a << 24) + (b << 16) + (c << 8) + (d << 0));
	    }

	    public long readLong()
	    throws IOException
	    {
	        return ((0xFFFFFFFFL & readInt()) << 32) | (0xFFFFFFFFL & readInt());
	    }

	    public float readFloat()
	    throws IOException
	    {
	        return Float.intBitsToFloat(readInt());
	    }

	    public double readDouble()
	    throws IOException
	    {
	        return Double.longBitsToDouble(readLong());
	    }
	    
	    ////////// DataInput methods we delegate //////////

	    public void readFully(byte[] b)
	    throws IOException
	    {
	        data.readFully(b, 0, b.length);
	    }
	    
	    public void readFully(byte[] b, int off, int len)
	    throws IOException
	    {
	        data.readFully(b, off, len);
	    }

	    public int skipBytes(int n)
	    throws IOException
	    {
	        return data.skipBytes(n);
	    }

	    public String readLine()
	    throws IOException
	    {
	        return data.readLine();
	    }

	    public String readUTF()
	    throws IOException
	    {
	        return data.readUTF();
	    }

	    ////////// PNG-specific methods //////////

	    /**
	     * Returns the number of bytes of chunk data that the
	     * {@link PngConstants#read} method implementation is required to read.
	     * Use {@link #skipBytes} to skip the data.
	     * @return the number of bytes in the chunk remaining to be read
	     */
	    public int getRemaining()
	    {
	        return left;
	    }

	    public long getOffset()
	    {
	        return total;
	    }    
	}
	
	
	final public static class PngConfig
	{
	    /** Read the entire image */
	    public static final int READ_ALL = 0;
	    /** Read only the header chunk */
	    public static final int READ_HEADER = 1;
	    /** Read all the metadata up to the image data */
	    public static final int READ_UNTIL_DATA = 2;
	    /** Read the entire image, skipping over the image data */
	    public static final int READ_EXCEPT_DATA = 3;
	    /** Read the entire image, skipping over all non-critical chunks except tRNS and gAMA */
	    public static final int READ_EXCEPT_METADATA = 4;

	    final int readLimit;
	    final float defaultGamma;
	    final float displayExponent;
	    final boolean warningsFatal;
	    final boolean progressive;
	    final boolean reduce16;
	    final boolean gammaCorrect;
	    final Rectangle sourceRegion;
	    final int[] subsampling;
	    final boolean convertIndexed;

	    PngConfig(Builder builder)
	    {
	        this.readLimit = builder.readLimit;
	        this.defaultGamma = builder.defaultGamma;
	        this.displayExponent = builder.displayExponent;
	        this.warningsFatal = builder.warningsFatal;
	        this.progressive = builder.progressive;
	        this.reduce16 = builder.reduce16;
	        this.gammaCorrect = builder.gammaCorrect;
	        this.sourceRegion = builder.sourceRegion;
	        this.subsampling = builder.subsampling;
	        this.convertIndexed = builder.convertIndexed;
	        
	        boolean subsampleOn = getSourceXSubsampling() != 1 || getSourceYSubsampling() != 1;
	        if (progressive && (subsampleOn || getSourceRegion() != null))
	            throw new IllegalStateException("Progressive rendering cannot be used with source regions or subsampling");
	    }

	    /**
	     * Builder class used to construct {@link PngConfig PngConfig} instances.
	     * Each "setter" method returns an reference to the instance to enable
	     * chaining multiple calls.
	     * Call {@link #build} to construct a new {@code PngConfig} instance
	     * using the current {@code Builder} settings. Example:
	     * <pre>PngConfig config = new PngConfig.Builder()
	     *        .readLimit(PngConfig.READ_EXCEPT_METADATA)
	     *        .warningsFatal(true)
	     *        .build();</pre>
	     */
	    final public static class Builder
	    {
	        private static final int[] DEFAULT_SUBSAMPLING = { 1, 1, 0, 0 };

	        int readLimit = READ_ALL;
	        float defaultGamma = 0.45455f;
	        float displayExponent = 2.2f;
	        boolean warningsFatal;
	        boolean progressive;
	        boolean reduce16 = true;
	        boolean gammaCorrect = true;
	        Rectangle sourceRegion;
	        int[] subsampling = DEFAULT_SUBSAMPLING;
	        boolean convertIndexed;

	        /**
	         * Create a new builder using default values.
	         */
	        public Builder()
	        {
	        }

	        /**
	         * Create a builder using values from the given configuration.
	         * @param cfg the configuration to copy
	         */
	        public Builder(PngConfig cfg)
	        {
	            this.readLimit = cfg.readLimit;
	            this.defaultGamma = cfg.defaultGamma;
	            this.displayExponent = cfg.displayExponent;
	            this.warningsFatal = cfg.warningsFatal;
	            this.progressive = cfg.progressive;
	            this.reduce16 = cfg.reduce16;
	            this.gammaCorrect = cfg.gammaCorrect;
	            this.subsampling = cfg.subsampling;
	        }

	        public PngConfig build()
	        {
	            return new PngConfig(this);
	        }
	        
	        public Builder reduce16(boolean reduce16)
	        {
	            this.reduce16 = reduce16;
	            return this;
	        }

	        public Builder defaultGamma(float defaultGamma)
	        {
	            this.defaultGamma = defaultGamma;
	            return this;
	        }

	        public Builder displayExponent(float displayExponent)
	        {
	            this.displayExponent = displayExponent;
	            return this;
	        }

	        public Builder gammaCorrect(boolean gammaCorrect)
	        {
	            this.gammaCorrect = gammaCorrect;
	            return this;
	        }

	        public Builder progressive(boolean progressive)
	        {
	            this.progressive = progressive;
	            return this;
	        }
	        
	        public Builder readLimit(int readLimit)
	        {
	            this.readLimit = readLimit;
	            return this;
	        }

	        public Builder warningsFatal(boolean warningsFatal)
	        {
	            this.warningsFatal = warningsFatal;
	            return this;
	        }

	        public Builder sourceRegion(Rectangle sourceRegion)
	        {
	            if (sourceRegion != null) {
	                if (sourceRegion.x < 0 ||
	                    sourceRegion.y < 0 ||
	                    sourceRegion.width <= 0 ||
	                    sourceRegion.height <= 0)
	                    throw new IllegalArgumentException("invalid source region: " + sourceRegion);
	                this.sourceRegion = new Rectangle(sourceRegion);
	            } else {
	                this.sourceRegion = null;
	            }
	            return this;
	        }

	        public Builder sourceSubsampling(int xsub, int ysub, int xoff, int yoff)
	        {
	            if (xsub <= 0 || ysub <= 0 ||
	                xoff < 0 || xoff >= xsub ||
	                yoff < 0 || yoff >= ysub)
	                throw new IllegalArgumentException("invalid subsampling values");
	            subsampling = new int[]{ xsub, ysub, xoff, yoff };
	            return this;
	        }

	        public Builder convertIndexed(boolean convertIndexed)
	        {
	            this.convertIndexed = convertIndexed;
	            return this;
	        }
	    }

	    public boolean getConvertIndexed()
	    {
	        return convertIndexed;
	    }

	    public boolean getReduce16()
	    {
	        return reduce16;
	    }

	    public float getDefaultGamma()
	    {
	        return defaultGamma;
	    }

	    public boolean getGammaCorrect()
	    {
	        return gammaCorrect;
	    }

	    public boolean getProgressive()
	    {
	        return progressive;
	    }

	    public float getDisplayExponent()
	    {
	        return displayExponent;
	    }
	    
	    public int getReadLimit()
	    {
	        return readLimit;
	    }

	    public boolean getWarningsFatal()
	    {
	        return warningsFatal;
	    }

	    public Rectangle getSourceRegion()
	    {
	        return (sourceRegion != null) ? new Rectangle(sourceRegion) : null;
	    }

	    public int getSourceXSubsampling()
	    {
	        return subsampling[0];
	    }

	    public int getSourceYSubsampling()
	    {
	        return subsampling[1];
	    }

	    public int getSubsamplingXOffset()
	    {
	        return subsampling[2];
	    }

	    public int getSubsamplingYOffset()
	    {
	        return subsampling[3];
	    }
	}
	/*	
	
	public static class ImageFactory
	{
	    private static short[] GAMMA_TABLE_45455 =
	    		ApngDecoder.createGammaTable(0.45455f, 2.2f, false);
	    private static short[] GAMMA_TABLE_100000 =
	    		ApngDecoder.createGammaTable(1f, 2.2f, false);

	    public static Bitmap createImage(ApngDecoder png, InputStream in)
	    throws IOException
	    {
	        return createImage(png, in, new Dimension(png.getWidth(), png.getHeight()));
	    }

	    // width and height are overridable for APNG
	    public static Bitmap createImage(ApngDecoder png, InputStream in, Dimension size)
	    throws IOException
	    {
	        PngConfig config = png.getConfig();

	        int width     = size.width;
	        int height    = size.height;
	        int bitDepth  = png.getBitDepth();
	        int samples   = png.getSamples();
	        boolean interlaced = png.isInterlaced();

	        boolean indexed = isIndexed(png);
	        boolean convertIndexed = indexed && config.getConvertIndexed();
	        short[] gammaTable = config.getGammaCorrect() ? getGammaTable(png) : null;
	        ColorModel dstColorModel = createColorModel(png, gammaTable, convertIndexed);

	        int dstWidth = width;
	        int dstHeight = height;
	        Rectangle sourceRegion = config.getSourceRegion();
	        if (sourceRegion != null) {
	            if (!new Rectangle(dstWidth, dstHeight).contains(sourceRegion))
	                throw new IllegalStateException("Source region " + sourceRegion + " falls outside of " +
	                                                width + "x" + height + " image");
	            dstWidth = sourceRegion.width;
	            dstHeight = sourceRegion.height;
	        }

	        Destination dst;
	        int xsub = config.getSourceXSubsampling();
	        int ysub = config.getSourceYSubsampling();
	        if (xsub != 1 || ysub != 1) {
	            int xoff = config.getSubsamplingXOffset();
	            int yoff = config.getSubsamplingYOffset();
	            int subw = calcSubsamplingSize(dstWidth, xsub, xoff, 'X');
	            int subh = calcSubsamplingSize(dstHeight, ysub, yoff, 'Y');
	            WritableRaster raster = dstColorModel.createCompatibleWritableRaster(subw, subh);
	            dst = new SubsamplingDestination(raster, width, xsub, ysub, xoff, yoff);
	        } else {
	            dst = new RasterDestination(dstColorModel.createCompatibleWritableRaster(dstWidth, dstHeight), width);
	        }
	        if (sourceRegion != null)
	            dst = new SourceRegionDestination(dst, sourceRegion);


	        // Destination dst = createDestination(config, dstColorModel, size, interlaced);
	        BufferedImage image = new BufferedImage(dstColorModel, dst.getRaster(), false, null);

	        PixelProcessor pp = null;
	        if (!indexed) {
	            int[] trans = (int[])png.getProperty(PngConstants.TRANSPARENCY, int[].class, false);
	            int shift = (bitDepth == 16 && config.getReduce16()) ? 8 : 0;
	            if (shift != 0 || trans != null || gammaTable != null) {
	                if (gammaTable == null)
	                    gammaTable = getIdentityTable(bitDepth - shift);
	                if (trans != null) {
	                    pp = new TransGammaPixelProcessor(dst, gammaTable, trans, shift);
	                } else {
	                    pp = new GammaPixelProcessor(dst, gammaTable, shift);
	                }
	            }
	        }
	        if (convertIndexed) {
	            IndexColorModel srcColorModel = (IndexColorModel)createColorModel(png, gammaTable, false);
	            dst = new ConvertIndexedDestination(dst, width, srcColorModel, (ComponentColorModel)dstColorModel);
	        }

	        if (pp == null)
	            pp = new BasicPixelProcessor(dst, samples);
	        if (config.getProgressive() && interlaced && !convertIndexed)
	            pp = new ProgressivePixelProcessor(dst, pp, width, height);
	        pp = new ProgressUpdater(png, image, pp);

	        InflaterInputStream inflate = new InflaterInputStream(in, new Inflater(), 0x1000);
	        Defilterer d = new Defilterer(inflate, bitDepth, samples, width, pp);
	        
	        // TO-DO: if not progressive, initialize to fully transparent?
	        boolean complete;
	        if (interlaced) {
	            complete =
	                d.defilter(0, 0, 8, 8, (width + 7) / 8, (height + 7) / 8) &&
	                png.handlePass(image, 0) &&
	                d.defilter(4, 0, 8, 8, (width + 3) / 8, (height + 7) / 8) &&
	                png.handlePass(image, 1) &&
	                d.defilter(0, 4, 4, 8, (width + 3) / 4, (height + 3) / 8) &&
	                png.handlePass(image, 2) &&
	                d.defilter(2, 0, 4, 4, (width + 1) / 4, (height + 3) / 4) &&
	                png.handlePass(image, 3) && 
	                d.defilter(0, 2, 2, 4, (width + 1) / 2, (height + 1) / 4) &&
	                png.handlePass(image, 4) &&
	                d.defilter(1, 0, 2, 2, width / 2, (height + 1) / 2) &&
	                png.handlePass(image, 5) &&
	                d.defilter(0, 1, 1, 2, width, height / 2) &&
	                png.handlePass(image, 6);
	        } else {
	            complete =
	                d.defilter(0, 0, 1, 1, width, height) &&
	                png.handlePass(image, 0);
	        }
	        // TO-DO: handle complete?
	        dst.done();
	        return image;
	    }

	    private static short[] getGammaTable(PngImage png)
	    {
	        PngConfig config = png.getConfig();
	        if ((png.getBitDepth() != 16 || config.getReduce16()) &&
	            config.getDisplayExponent() == 2.2f) {
	            float gamma = png.getGamma();
	            if (gamma == 0.45455f)
	                return GAMMA_TABLE_45455;
	            if (gamma == 1f)
	                return GAMMA_TABLE_100000;
	        }
	        return png.getGammaTable();
	    }

	    private static int calcSubsamplingSize(int len, int sub, int off, char desc)
	    {
	        int size = (len - off + sub - 1) / sub;
	        if (size == 0)
	            throw new IllegalStateException("Source " + desc + " subsampling " + sub + ", offset " + off +
	                                            " is invalid for image dimension " + len);
	        return size;
	    }

	    private static boolean isIndexed(ApngDecoder png)
	    {
	        int colorType = png.getColorType();
	        return colorType == PngConstants.COLOR_TYPE_PALETTE ||
	            (colorType == PngConstants.COLOR_TYPE_GRAY && png.getBitDepth() < 16);
	    }

	    private static ColorModel createColorModel(ApngDecoder png, short[] gammaTable, boolean convertIndexed)
	    throws IOException
	    {
	        Map props = png.getProperties();
	        int colorType = png.getColorType();
	        int bitDepth = png.getBitDepth();
	        int outputDepth = (bitDepth == 16 && png.getConfig().getReduce16()) ? 8 : bitDepth;

	        if (isIndexed(png) && !convertIndexed) {
	            byte[] r, g, b;
	            if (colorType == PngConstants.COLOR_TYPE_PALETTE) {
	                byte[] palette = (byte[])png.getProperty(PngConstants.PALETTE, byte[].class, true);
	                int paletteSize = palette.length / 3;
	                r = new byte[paletteSize];
	                g = new byte[paletteSize];
	                b = new byte[paletteSize];
	                for (int i = 0, p = 0; i < paletteSize; i++) {
	                    r[i] = palette[p++];
	                    g[i] = palette[p++];
	                    b[i] = palette[p++];
	                }
	                applyGamma(r, gammaTable);
	                applyGamma(g, gammaTable);
	                applyGamma(b, gammaTable);
	            } else {
	                int paletteSize = 1 << bitDepth;
	                r = g = b = new byte[paletteSize];
	                for (int i = 0; i < paletteSize; i++) {
	                    r[i] = (byte)(i * 255 / (paletteSize - 1));
	                }
	                applyGamma(r, gammaTable);
	            }
	            if (props.containsKey(PngConstants.PALETTE_ALPHA)) {
	                byte[] trans = (byte[])png.getProperty(PngConstants.PALETTE_ALPHA, byte[].class, true);
	                byte[] a = new byte[r.length];
	                Arrays.fill(a, trans.length, r.length, (byte)0xFF);
	                System.arraycopy(trans, 0, a, 0, trans.length);
	                return new IndexColorModel(outputDepth, r.length, r, g, b, a);
	            } else {
	                int trans = -1;
	                if (props.containsKey(PngConstants.TRANSPARENCY))
	                    trans = ((int[])png.getProperty(PngConstants.TRANSPARENCY, int[].class, true))[0];
	                return new IndexColorModel(outputDepth, r.length, r, g, b, trans);
	            }
	        } else {
	            int dataType = (outputDepth == 16) ?
	                DataBuffer.TYPE_USHORT : DataBuffer.TYPE_BYTE;
	            int colorSpace =
	                (colorType == PngConstants.COLOR_TYPE_GRAY ||
	                 colorType == PngConstants.COLOR_TYPE_GRAY_ALPHA) ?
	                ColorSpace.CS_GRAY :
	                ColorSpace.CS_sRGB;
	            int transparency = png.getTransparency();
	            // TO-DO: cache/enumerate color models?
	            return new ComponentColorModel(ColorSpace.getInstance(colorSpace),
	                                           null,
	                                           transparency != ApngDecoder.OPAQUE,
	                                           false,
	                                           transparency,
	                                           dataType);
	        }
	    }

	     private static void applyGamma(byte[] palette, short[] gammaTable)
	     {
	         if (gammaTable != null) {
	             for (int i = 0; i < palette.length; i++)
	                 palette[i] = (byte)gammaTable[0xFF & palette[i]];
	         }
	     }
	    
	    private static short[] getIdentityTable(int bitDepth)
	    {
	        // TO-DO: cache identity tables?
	        int size = 1 << bitDepth;
	        short[] table = new short[size];
	        for (int i = 0; i < size; i++)
	            table[i] = (short)i;
	        return table;
	    }
	}

	abstract class Destination
	{
	    abstract public void setPixels(int x, int y, int w, int[] pixels); // TO-DO: change to setRow(int y, int w, int[] pixels)
	    abstract public void setPixel(int x, int y, int[] pixel);
	    abstract public void getPixel(int x, int y, int[] pixel); // used only by ProgressivePixelProcessor
	    abstract public WritableRaster getRaster();
	    abstract public int getSourceWidth();
	    abstract public void done();
	}
	
	abstract class PixelProcessor
	{
	    abstract public boolean process(int[] row, int xOffset, int xStep, int yStep, int y, int width);
	}
	
	 public class BasicPixelProcessor
	extends PixelProcessor
	{
	    protected final Destination dst;
	    protected final int samples;
	    
	    public BasicPixelProcessor(Destination dst, int samples)
	    {
	        this.dst = dst;
	        this.samples = samples;
	    }
	    
	    public boolean process(int[] row, int xOffset, int xStep, int yStep, int y, int width)
	    {
	        if (xStep == 1) {
	            dst.setPixels(xOffset, y, width, row);
	        } else {
	            int dstX = xOffset;
	            for (int index = 0, total = samples * width; index < total; index += samples) {
	                for (int i = 0; i < samples; i++)
	                    row[i] = row[index + i];
	                dst.setPixel(dstX, y, row);
	                dstX += xStep;
	            }
	        }
	        return true;
	    }
	}
	
	 public static class Defilterer
	{
	    private final InputStream in;
	    private final int width;
	    private final int bitDepth;
	    private final int samples;
	    private final PixelProcessor pp;
	    private final int bpp;
	    private final int[] row;

	    public Defilterer(InputStream in, int bitDepth, int samples, int width, PixelProcessor pp)
	    {
	        this.in = in;
	        this.bitDepth = bitDepth;
	        this.samples = samples;
	        this.width = width;
	        this.pp = pp;
	        bpp = Math.max(1, (bitDepth * samples) >> 3);
	        row = new int[samples * width];
	    }

	    public boolean defilter(int xOffset, int yOffset,
	                            int xStep, int yStep,
	                            int passWidth, int passHeight)
	    throws IOException
	    {
	        if (passWidth == 0 || passHeight == 0)
	            return true;

	        int bytesPerRow = (bitDepth * samples * passWidth + 7) / 8;
	        boolean isShort = bitDepth == 16;
	        WritableRaster passRow = createInputRaster(bitDepth, samples, width);
	        DataBuffer dbuf = passRow.getDataBuffer();
	        byte[] byteData = isShort ? null : ((DataBufferByte)dbuf).getData();
	        short[] shortData = isShort ? ((DataBufferUShort)dbuf).getData() : null;
	        
	        int rowSize = bytesPerRow + bpp;
	        byte[] prev = new byte[rowSize];
	        byte[] cur = new byte[rowSize];

	        for (int srcY = 0, dstY = yOffset; srcY < passHeight; srcY++, dstY += yStep) {
	            int filterType = in.read();
	            if (filterType == -1)
	                throw new EOFException("Unexpected end of image data");
	            readFully(in, cur, bpp, bytesPerRow);
	            defilter(cur, prev, bpp, filterType);
	            if (isShort) {
	                for (int c = 0, i = bpp; i < rowSize; c++, i += 2)
	                    shortData[c] = (short)((cur[i] << 8) | (0xFF & cur[i + 1]));
	            } else {
	                System.arraycopy(cur, bpp, byteData, 0, bytesPerRow);
	            }
	            passRow.getPixels(0, 0, passWidth, 1, row);
	            if (!pp.process(row, xOffset, xStep, yStep, dstY, passWidth))
	                return false;
	            byte[] tmp = cur;
	            cur = prev;
	            prev = tmp;
	        }
	        return true;
	    }

	    private static void defilter(byte[] cur, byte[] prev, int bpp, int filterType)
	    throws IOException
	    {
	        int rowSize = cur.length;
	        int xc, xp;
	        switch (filterType) {
	        case 0: // None
	            break;
	        case 1: // Sub
	            for (xc = bpp, xp = 0; xc < rowSize; xc++, xp++)
	                cur[xc] = (byte)(cur[xc] + cur[xp]);
	            break;
	        case 2: // Up
	            for (xc = bpp; xc < rowSize; xc++)
	                cur[xc] = (byte)(cur[xc] + prev[xc]);
	            break;
	        case 3: // Average
	            for (xc = bpp, xp = 0; xc < rowSize; xc++, xp++)
	                cur[xc] = (byte)(cur[xc] + ((0xFF & cur[xp]) + (0xFF & prev[xc])) / 2);
	            break;
	        case 4: // Paeth
	            for (xc = bpp, xp = 0; xc < rowSize; xc++, xp++) {
	                byte L = cur[xp];
	                byte u = prev[xc];
	                byte nw = prev[xp];
	                int a = 0xFF & L; //  inline byte->int
	                int b = 0xFF & u; 
	                int c = 0xFF & nw; 
	                int p = a + b - c;
	                int pa = p - a; if (pa < 0) pa = -pa; // inline Math.abs
	                int pb = p - b; if (pb < 0) pb = -pb; 
	                int pc = p - c; if (pc < 0) pc = -pc;
	                int result;
	                if (pa <= pb && pa <= pc) {
	                    result = a;
	                } else if (pb <= pc) {
	                    result = b;
	                } else {
	                    result = c;
	                }
	                cur[xc] = (byte)(cur[xc] + result);
	            }
	            break;
	        default:
	            throw new IOException("Unrecognized filter type " + filterType);
	        }
	    }

	    private static int[][] bandOffsets = {
	        null,
	        { 0 },
	        { 0, 1 },
	        { 0, 1, 2 },
	        { 0, 1, 2, 3 },
	    };

	    private static WritableRaster createInputRaster(int bitDepth, int samples, int width)
	    {
	        int rowSize = (bitDepth * samples * width + 7) / 8;
	        Point origin = new Point(0, 0);
	        if ((bitDepth < 8) && (samples == 1)) {
	            DataBuffer dbuf = new DataBufferByte(rowSize);
	            return Raster.createPackedRaster(dbuf, width, 1, bitDepth, origin);
	        } else if (bitDepth <= 8) {
	            DataBuffer dbuf = new DataBufferByte(rowSize);
	            return Raster.createInterleavedRaster(dbuf, width, 1, rowSize, samples,
	                                                  bandOffsets[samples], origin);
	        } else {
	            DataBuffer dbuf = new DataBufferUShort(rowSize / 2);
	            return Raster.createInterleavedRaster(dbuf, width, 1, rowSize / 2, samples,
	                                                  bandOffsets[samples], origin);
	        }
	    }

	    private static void readFully(InputStream in, byte[] b, int off, int len)
	    throws IOException
	    {
	        int total = 0;
	        while (total < len) {
	            int result = in.read(b, off + total, len - total);
	            if (result == -1)
	                throw new EOFException("Unexpected end of image data");
	            total += result;
	        }
	    }
	}
	
	final public static class GammaPixelProcessor
	extends BasicPixelProcessor
	{
	    final private short[] gammaTable;
	    final private int shift;
	    final private int samplesNoAlpha;
	    final private boolean hasAlpha;
	    final private boolean shiftAlpha;
	    
	    public GammaPixelProcessor(Destination dst, short[] gammaTable, int shift)
	    {
	        super(dst, dst.getRaster().getNumBands());
	        this.gammaTable = gammaTable;
	        this.shift = shift;
	        hasAlpha = samples % 2 == 0;
	        samplesNoAlpha = hasAlpha ? samples - 1 : samples; // don't change alpha channel
	        shiftAlpha = hasAlpha && shift > 0;
	    }
	    
	    public boolean process(int[] row, int xOffset, int xStep, int yStep, int y, int width)
	    {
	        int total = samples * width;
	        for (int i = 0; i < samplesNoAlpha; i++)
	            for (int index = i; index < total; index += samples)
	                row[index] = 0xFFFF & gammaTable[row[index] >> shift];
	        if (shiftAlpha)
	            for (int index = samplesNoAlpha; index < total; index += samples)
	                row[index] >>= shift;
	        return super.process(row, xOffset, xStep, yStep, y, width);
	    }
	}
	
	final public static class TransGammaPixelProcessor
	extends BasicPixelProcessor
	{
	    final private short[] gammaTable;
	    final private int[] trans;
	    final private int shift;
	    final private int max;
	    final private int samplesNoAlpha;
	    final private int[] temp;
	    
	    public TransGammaPixelProcessor(Destination dst, short[] gammaTable, int[] trans, int shift)
	    {
	        super(dst, dst.getRaster().getNumBands());
	        this.gammaTable = gammaTable;
	        this.trans = trans;
	        this.shift = shift;
	        max = gammaTable.length - 1;
	        samplesNoAlpha = samples - 1;
	        temp = new int[samples * dst.getSourceWidth()];
	    }
	    
	    public boolean process(int[] row, int xOffset, int xStep, int yStep, int y, int width)
	    {
	        int total = width * samplesNoAlpha;
	        for (int i1 = 0, i2 = 0; i1 < total; i1 += samplesNoAlpha, i2 += samples) {
	            boolean opaque = false;
	            for (int j = 0; j < samplesNoAlpha; j++) {
	                int sample = row[i1 + j];
	                opaque = opaque || (sample != trans[j]);
	                temp[i2 + j] = 0xFFFF & gammaTable[sample >> shift];
	            }
	            temp[i2 + samplesNoAlpha] = opaque ? max : 0;
	        }
	        return super.process(temp, xOffset, xStep, yStep, y, width);
	    }
	}
	
	class RasterDestination
	extends Destination
	{
	    protected final WritableRaster raster;
	    protected final int sourceWidth;
	    public RasterDestination(WritableRaster raster, int sourceWidth) { this.raster = raster; this.sourceWidth = sourceWidth; }
 	    public void setPixels(int x, int y, int w, int[] pixels) { raster.setPixels(x, y, w, 1, pixels); }
 	    public void setPixel(int x, int y, int[] pixel) { raster.setPixel(x, y, pixel); }
 	    public void getPixel(int x, int y, int[] pixel) { raster.getPixel(x, y, pixel); }
 	    public WritableRaster getRaster() { return raster; }
	    public void done() { }    
	}
	
*/	
}
