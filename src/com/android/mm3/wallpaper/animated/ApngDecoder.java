package com.android.mm3.wallpaper.animated;


	import java.awt.*;
	import java.io.*;
	import java.util.*;
	import java.util.List;
    import android.graphics.*;

	/**
	 * TODO
	 */
	public class ApngDecoder extends Decoder
	{
		/** TODO */
		public static final int acTL = 0x6163544C;
		/** TODO */
		public static final int fcTL = 0x6663544C;
		/** TODO */
		public static final int fdAT = 0x66644154;

//		private static final PngConfig DEFAULT_CONFIG =
//		new PngConfig.Builder().readLimit(PngConfig.READ_EXCEPT_DATA).build();

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
		 * TODO
		 */
		public ApngDecoder ()
		{
			//super(DEFAULT_CONFIG);
		}

		/**
		 * TODO
		 */
	//	public AnimatedPngImage(PngConfig config)
	//	{
	//		super(new PngConfig.Builder(config).readLimit(PngConfig.READ_EXCEPT_DATA).build());
	//	}

		private void reset()
		{
			animated = sawData = useDefaultImage = false;
			chunks.clear();
			frames.clear();
			frameData.clear();
			defaultImageData.clear();
		}

		/**
		 * TODO
		 */
		public boolean isAnimated()
		{
			assertRead();
			return animated;
		}

		/**
		 * TODO
		 */
		public int getNumFrames()
		{
			assertRead();
			return frames.size();
		}

		/**
		 * TODO
		 */
		public int getNumPlays()
		{
			assertRead();
			return animated ? numPlays : 1;
		}

		/**
		 * TODO
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
		 * TODO
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
		 * TODO
		 */
		public BufferedImage[] readAllFrames(File file)
		throws IOException
		{
			read(file);
			BufferedImage[] images = new BufferedImage[getNumFrames()];
			for (int i = 0; i < images.length; i++)
				images[i] = readFrame(file, getFrame(i));
			return images;
		}

		// TODO: make sure that file is what we read before?
		// TODO: make sure that frame control is from this image?
		/**
		 * TODO
		 */
		public BufferedImage readFrame(File file, FrameControl frame)
		throws IOException
		{
			assertRead();
			if (frame == null)
				return readImage(file, defaultImageData, new Dimension(getWidth(), getHeight()));
			return readImage(file, (List)frameData.get(frame), frame.getBounds().getSize());
		}

		private BufferedImage readImage(File file, List data, Dimension size)
		throws IOException
		{
			FrameDataInputStream in = new FrameDataInputStream(file, data);
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
		throws PngException
		{
			if (chunks.size() != seq ||
				(seq == 0 && !(chunk instanceof FrameControl)))
				error("APNG chunks out of order");
			chunks.add(chunk);
		}

		private static void error(String message)
		throws PngException
		{
			throw new PngException(message);
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
		
		
	public BufferedImage read(InputStream in, boolean close)
    throws IOException
    {
        if (in == null)
            throw new NullPointerException("InputStream is null");
        this.read = true;
        props.clear();

        int readLimit = config.getReadLimit();
        BufferedImage image = null;
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
                        throw new PngException("Multiple " + PngConstants.getChunkName(type) + " chunks are not allowed",
                                               !PngConstants.isAncillary(type));
                    try {
                        readChunk(type, pin, pin.getOffset(), pin.getRemaining());
                    } catch (PngException e) {
                        throw e;
                    } catch (IOException e) {
                        throw new PngException("Malformed " + PngConstants.getChunkName(type) + " chunk", e,
                                               !PngConstants.isAncillary(type));
                    }
                    skipFully(pin, pin.getRemaining());
                    if (type == PngConstants.IHDR && readLimit == PngConfig.READ_HEADER)
                        return null;
                } catch (PngException exception) {
                    if (exception.isFatal())
                        throw exception;
                    skipFully(pin, pin.getRemaining());
                    handleWarning(exception);
                }
                pin.endChunk(type);
            }
            return image;
        } finally {
            if (close)
                in.close();
        }
    }

    protected BufferedImage createImage(InputStream in, Dimension size)
    throws IOException
    {
        return ImageFactory.createImage(this, in, size);
    }

    protected boolean handlePass(BufferedImage image, int pass)
    {
        return true;
    }

    protected boolean handleProgress(BufferedImage image, float pct)
    {
        return true;
    }

    protected void handleWarning(PngException e)
    throws PngException
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
				return new Color(0xFF & palette[index + 0], 
								 0xFF & palette[index + 1], 
								 0xFF & palette[index + 2]);
			case PngConstants.COLOR_TYPE_GRAY:
			case PngConstants.COLOR_TYPE_GRAY_ALPHA:
				int gray = background[0] * 255 / ((1 << getBitDepth()) - 1);
				return new Color(gray, gray, gray);
			default:
				if (getBitDepth() == 16) {
					return new Color(background[0] >> 8, background[1] >> 8, background[2] >> 8);
				} else {
					return new Color(background[0], background[1], background[2]);
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
                // TODO: check list value type before cast?
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
		/** TODO */
		public static final int DISPOSE_NONE = 0;
		/** TODO */
		public static final int DISPOSE_BACKGROUND = 1;
		/** TODO */
		public static final int DISPOSE_PREVIOUS = 2;

		/** TODO */
		public static final int BLEND_SOURCE = 0;
		/** TODO */
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
		 * TODO
		 */
		public Rectangle getBounds()
		{
			return new Rectangle(bounds);
		}

		/**
		 * TODO
		 */
		public float getDelay()
		{
			return delay;
		}

		/**
		 * TODO
		 */
		public int getDispose()
		{
			return dispose;
		}

		/**
		 * TODO
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
	
	class Rectangle 
	extends RectF
	{
		
	}
	
	public class PngException
	extends IOException
	{
		
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
				// TODO: enable streaming
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
            throw new PngException("Bad image size: " + width + "x" + height, true);

        byte bitDepth = in.readByte();
        switch (bitDepth) {
        case 1: case 2: case 4: case 8: case 16: break;
        default: throw new PngException("Bad bit depth: " + bitDepth, true);
        }

        byte[] sbits = null;
        int colorType = in.readUnsignedByte();
        switch (colorType) {
        case PngConstants.COLOR_TYPE_RGB:
        case PngConstants.COLOR_TYPE_GRAY: 
            break;
        case PngConstants.COLOR_TYPE_PALETTE: 
            if (bitDepth == 16)
                throw new PngException("Bad bit depth for color type " + colorType + ": " + bitDepth, true);
            break;
        case PngConstants.COLOR_TYPE_GRAY_ALPHA: 
        case PngConstants.COLOR_TYPE_RGB_ALPHA: 
            if (bitDepth <= 4)
                throw new PngException("Bad bit depth for color type " + colorType + ": " + bitDepth, true);
            break;
        default:
            throw new PngException("Bad color type: " + colorType, true);
        }

        int compression = in.readUnsignedByte();
        if (compression != PngConstants.COMPRESSION_BASE) 
            throw new PngException("Unrecognized compression method: " + compression, true);

        int filter = in.readUnsignedByte();
        if (filter != PngConstants.FILTER_BASE)
            throw new PngException("Unrecognized filter method: " + filter, true);

        int interlace = in.readUnsignedByte();
        switch (interlace) {
        case PngConstants.INTERLACE_NONE:
        case PngConstants.INTERLACE_ADAM7:
            break;
        default:
            throw new PngException("Unrecognized interlace method: " + interlace, true);
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
                throw new PngException("Too many palette entries: " + size, true);
            break;
        case PngConstants.COLOR_TYPE_GRAY:
        case PngConstants.COLOR_TYPE_GRAY_ALPHA:
            throw new PngException("PLTE chunk found in grayscale image", false);
        }
        byte[] palette = new byte[length];
        in.readFully(palette);
        props.put(PngConstants.PALETTE, palette);
    }

    private static void read_tRNS(DataInput in, int length, Map props, PngImage png)
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
                throw new PngException("Too many transparency palette entries (" + length + " > " + paletteSize + ")", true);
            byte[] trans = new byte[length];
            in.readFully(trans);
            props.put(PngConstants.PALETTE_ALPHA, trans);
            break;
        default:
            throw new PngException("tRNS prohibited for color type " + png.getColorType(), true);
        }
    }

    private static void read_bKGD(DataInput in, int length, Map props, PngImage png)
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
            throw new PngException("Meaningless zero gAMA chunk value", false);
        if (!props.containsKey(PngConstants.RENDERING_INTENT))
            props.put(PngConstants.GAMMA, new Float(gamma / 100000f));
    }

    private static void read_hIST(DataInput in, int length, Map props, PngImage png)
    throws IOException
    {
        // TODO: ensure it is divisible by three
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
            throw new PngException("Illegal pHYs chunk unit specifier: " + unit, false);
        props.put(PngConstants.PIXELS_PER_UNIT_X, Integers.valueOf(pixelsPerUnitX));
        props.put(PngConstants.PIXELS_PER_UNIT_Y, Integers.valueOf(pixelsPerUnitY));
        props.put(PngConstants.UNIT, Integers.valueOf(unit));
    }

    private static void read_sBIT(DataInput in, int length, Map props, PngImage png)
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
                throw new PngException("Illegal sBIT sample depth", false);
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
    throws PngException
    {
        if (value < min || value > max)
            throw new PngException("tIME " + field + " value " + value +
                                   " is out of bounds (" + min + "-" + max + ")", false);
        return value;
    }

    private static void read_sPLT(DataInput in, int length, Map props, PngImage png)
    throws IOException
    {
        String name = readKeyword(in, length);
        int sampleDepth = in.readByte();
        if (sampleDepth != 8 && sampleDepth != 16)
            throw new PngException("Sample depth must be 8 or 16", false);
        
        length -= (name.length() + 2);
        if ((length % ((sampleDepth == 8) ? 6 : 10)) != 0)
            throw new PngException("Incorrect sPLT data length for given sample depth", false);
        byte[] bytes = new byte[length];
        in.readFully(bytes);

        List palettes = (List)png.getProperty(PngConstants.SUGGESTED_PALETTES, List.class, false);
        if (palettes == null)
            props.put(PngConstants.SUGGESTED_PALETTES, palettes = new ArrayList());
        for (Iterator it = palettes.iterator(); it.hasNext();) {
            if (name.equals(((SuggestedPalette)it.next()).getName()))
                throw new PngException("Duplicate suggested palette name " + name, false);
        }
        palettes.add(new SuggestedPaletteImpl(name, sampleDepth, bytes));
    }

    private static void readText(int type, DataInput in, int length, Map props, PngImage png)
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
                    throw new PngException("Unrecognized " + PngConstants.getChunkName(type) + " compression method: " + method, false);
            } else if (flag != 0) {
                throw new PngException("Illegal " + PngConstants.getChunkName(type) + " compression flag: " + flag, false);
            }
            language = readString(data, data.available(), US_ASCII);
            // TODO: split language on hyphens, check that each component is 1-8 characters
            translated = readString(data, data.available(), UTF_8);
            // TODO: check for line breaks?
        }

        String text;
        if (compressed) {
            text = new String(readCompressed(data, data.available(), readMethod), enc);
        } else {
            text = new String(bytes, bytes.length - data.available(), data.available(), enc);
        }
        if (text.indexOf('\0') >= 0)
            throw new PngException("Text value contains null", false);
        List chunks = (List)png.getProperty(PngConstants.TEXT_CHUNKS, List.class, false);
        if (chunks == null)
            props.put(PngConstants.TEXT_CHUNKS, chunks = new ArrayList());
        chunks.add(new TextChunkImpl(keyword, text, language, translated, type));
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
            throw new PngException("Illegal oFFs chunk unit specifier: " + unit, false);
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
            throw new PngException("Illegal sCAL chunk unit specifier: " + unit, false);
        double width = readFloatingPoint(data, data.available());
        double height = readFloatingPoint(data, data.available());
        if (width <= 0 || height <= 0)
            throw new PngException("sCAL measurements must be >= 0", false);
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
            throw new PngException("Unknown sTER mode: " + mode, false);
        }
    }

    public static void checkLength(int chunk, int length, int correct)
    throws PngException
    {
        if (length != correct)
            throw new PngException("Bad " + PngConstants.getChunkName(chunk) + " chunk length: " + length + " (expected " + correct + ")", true);
    }

    private static byte[] readCompressed(DataInput in, int length, boolean readMethod)
    throws IOException
    {
        if (readMethod) {
            int method = in.readByte();
            if (method != 0)
                throw new PngException("Unrecognized compression method: " + method, false);
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
            throw new PngException("Error reading compressed data", e, false);
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
            throw new PngException("Invalid keyword length: " + keyword.length(), false);
        return keyword;
    }

    // TODO: performance
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
	
	
}
