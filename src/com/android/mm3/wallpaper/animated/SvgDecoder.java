package com.android.mm3.wallpaper.animated;

import android.graphics.*;
import android.graphics.drawable.PictureDrawable;
import android.util.Log;
import android.util.Xml;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Vector;
import java.util.ArrayList;
import java.util.HashMap;


public class SvgDecoder extends Decoder {

	public static final String TAG = "SvgDecoder";
	
	protected Vector<SVGOld> frames = new Vector<SVGOld>(); // frames read from current file
	protected int frameCount = 1;
	
	
	protected SVG frame = new SVG();
	
	// structural elements
	public static final String TAG_SVG                   = "svg";
	public static final String TAG_SVG_G                 = "g";
	public static final String TAG_SVG_DEFS              = "defs";
	public static final String TAG_SVG_SYMBOL            = "symbol";
	public static final String TAG_SVG_USE               = "use";
	public static final String TAG_SVG_A                 = "a";
	public static final String TAG_SVG_GLYPH             = "glyph";

	// descriptive elements
	public static final String TAG_SVG_TITLE             = "title";
	public static final String TAG_SVG_DESC              = "desc";
	public static final String TAG_SVG_METADATA          = "metadata";
	
	// shape elements
	public static final String TAG_SVG_CIRCLE            = "circle";
	public static final String TAG_SVG_PATH              = "path";
	public static final String TAG_SVG_ELLIPSE           = "ellipse";
	public static final String TAG_SVG_LINE              = "line";
	public static final String TAG_SVG_POLYGON           = "polygon";
	public static final String TAG_SVG_POLYLINE          = "polyline";
	public static final String TAG_SVG_RECT              = "rect";
	
	// gradient elements
	public static final String TAG_SVG_LINEAR_GRADIENT   = "linearGradient";
	public static final String TAG_SVG_RADIAL_GRADIENT   = "radialGradient";
	
	// animation elements
	public static final String TAG_SVG_ANIMATE           = "animate";
	public static final String TAG_SVG_ANIMATE_COLOR     = "animateColor";
	public static final String TAG_SVG_ANIMATE_MONITOR   = "animateMotion";
	public static final String TAG_SVG_ANIMATE_TRANSFORM = "animateTransform";
	public static final String TAG_SVG_SET               = "set";
	
	// other
	public static final String TAG_SVG_STOP              = "stop";
	public static final String TAG_SVG_ALT_GLYPH_DEF     = "altGlyphDef";
	public static final String TAG_SVG_COLOR_PROFILE     = "color-profile";
	public static final String TAG_SVG_CURSOR            = "cursor";
	public static final String TAG_SVG_FONT              = "font";
	public static final String TAG_SVG_FONT_FACE         = "font-face";
	public static final String TAG_SVG_FOREIGN_OBJECT    = "foreignObject";
	public static final String TAG_SVG_IMAGE             = "image";
	public static final String TAG_SVG_MARKER            = "marker";
	public static final String TAG_SVG_MASK              = "mask";
	public static final String TAG_SVG_PATTERN           = "pattern";
	public static final String TAG_SVG_SCRIPT            = "script";
	public static final String TAG_SVG_STYLE             = "style";
	public static final String TAG_SVG_SWITCH            = "switch";
	public static final String TAG_SVG_VIEW              = "view";
	public static final String TAG_SVG_MISSING_GLYPH     = "missing-glyph";

	// filter primitive element
	public static final String TAG_SVG_FILTER            = "filter";
	public static final String TAG_SVG_FE_BLEND          = "feBlend";
	public static final String TAG_SVG_FE_COLOR_MATRIX   = "feColorMatrix";
	public static final String TAG_SVG_FE_COMPOSITE      = "feComposite";
	public static final String TAG_SVG_FE_FLOOD          = "feFlood";
	public static final String TAG_SVG_FE_GAUSSIAN_BLUR  = "feGaussianBlur";
	public static final String TAG_SVG_FE_IMAGE          = "feImage";
	public static final String TAG_SVG_FE_MERGE          = "feMerge";
	public static final String TAG_SVG_FE_MORPHOLOGY     = "feMorphology";
	public static final String TAG_SVG_FE_OFFSET         = "feOffset";
	public static final String TAG_SVG_FE_TITLE          = "feTile";
	public static final String TAG_SVG_FE_TURBULENCE     = "feTurbulence";
	public static final String TAG_SVG_FE_DISTANT_LIGHT  = "feDistantLight";
	public static final String TAG_SVG_FE_POINT_LIGHT    = "fePointLight";
	public static final String TAG_SVG_FE_SPOT_LIGHT     = "feSpotLight";
	public static final String TAG_SVG_FE_COMPONENT_TRANSFER = "feComponentTransfer";
	public static final String TAG_SVG_FE_CONVOLVE_MATRIX    = "feConvolveMatrix";
	public static final String TAG_SVG_FE_DEFFUSE_LIGHTING   = "feDiffuseLighting";
	public static final String TAG_SVG_FE_DISPLACEMENT_MAP   = "feDisplacementMap";
	public static final String TAG_SVG_FE_SPECULAR_LIGHTING  = "feSpecularLighting";
	
	// text content element
	public static final String TAG_SVG_TEXT              = "text";
	public static final String TAG_SVG_ALT_GLYPH         = "altGlyph";
	public static final String TAG_SVG_TEXT_PATH         = "textPath";
	public static final String TAG_SVG_TREF              = "tref";
	public static final String TAG_SVG_TSPAN             = "tspan";

	public static final String ns = null;
	
	
	protected static final HashSet<String> tags = new HashSet<String>();
	static {
		tags.add(SvgDecoder.TAG_SVG);
		tags.add(SvgDecoder.TAG_SVG_G);
		tags.add(SvgDecoder.TAG_SVG_DEFS);
		tags.add(SvgDecoder.TAG_SVG_SYMBOL);
		tags.add(SvgDecoder.TAG_SVG_USE);
		tags.add(SvgDecoder.TAG_SVG_A);
		tags.add(SvgDecoder.TAG_SVG_GLYPH);
		tags.add(SvgDecoder.TAG_SVG_TITLE);
		tags.add(SvgDecoder.TAG_SVG_DESC);
		tags.add(SvgDecoder.TAG_SVG_METADATA);
		tags.add(SvgDecoder.TAG_SVG_CIRCLE);
		tags.add(SvgDecoder.TAG_SVG_PATH);
		tags.add(SvgDecoder.TAG_SVG_ELLIPSE);
		tags.add(SvgDecoder.TAG_SVG_LINE);
		tags.add(SvgDecoder.TAG_SVG_POLYGON);
		tags.add(SvgDecoder.TAG_SVG_POLYLINE);
		tags.add(SvgDecoder.TAG_SVG_RECT);
		tags.add(SvgDecoder.TAG_SVG_LINEAR_GRADIENT);
		tags.add(SvgDecoder.TAG_SVG_RADIAL_GRADIENT);
		tags.add(SvgDecoder.TAG_SVG_ANIMATE);
		tags.add(SvgDecoder.TAG_SVG_ANIMATE_COLOR);
		tags.add(SvgDecoder.TAG_SVG_ANIMATE_MONITOR);
		tags.add(SvgDecoder.TAG_SVG_ANIMATE_TRANSFORM);
		tags.add(SvgDecoder.TAG_SVG_SET);
		tags.add(SvgDecoder.TAG_SVG_ALT_GLYPH_DEF);
		tags.add(SvgDecoder.TAG_SVG_COLOR_PROFILE);
		tags.add(SvgDecoder.TAG_SVG_CURSOR);
		tags.add(SvgDecoder.TAG_SVG_FONT);
		tags.add(SvgDecoder.TAG_SVG_FONT_FACE);
		tags.add(SvgDecoder.TAG_SVG_FOREIGN_OBJECT);
		tags.add(SvgDecoder.TAG_SVG_IMAGE);
		tags.add(SvgDecoder.TAG_SVG_MARKER);
		tags.add(SvgDecoder.TAG_SVG_MASK);
		tags.add(SvgDecoder.TAG_SVG_PATTERN);
		tags.add(SvgDecoder.TAG_SVG_SCRIPT);
		tags.add(SvgDecoder.TAG_SVG_STYLE);
		tags.add(SvgDecoder.TAG_SVG_SWITCH);
		tags.add(SvgDecoder.TAG_SVG_VIEW);
		tags.add(SvgDecoder.TAG_SVG_MISSING_GLYPH);
		tags.add(SvgDecoder.TAG_SVG_FILTER);
		tags.add(SvgDecoder.TAG_SVG_FE_BLEND);
		tags.add(SvgDecoder.TAG_SVG_FE_COLOR_MATRIX);
		tags.add(SvgDecoder.TAG_SVG_FE_COMPOSITE);
		tags.add(SvgDecoder.TAG_SVG_FE_FLOOD);
		tags.add(SvgDecoder.TAG_SVG_FE_GAUSSIAN_BLUR);
		tags.add(SvgDecoder.TAG_SVG_FE_IMAGE);
		tags.add(SvgDecoder.TAG_SVG_FE_MERGE);
		tags.add(SvgDecoder.TAG_SVG_FE_MORPHOLOGY);
		tags.add(SvgDecoder.TAG_SVG_FE_OFFSET);
		tags.add(SvgDecoder.TAG_SVG_FE_TITLE);
		tags.add(SvgDecoder.TAG_SVG_FE_TURBULENCE);
		tags.add(SvgDecoder.TAG_SVG_FE_DISTANT_LIGHT);
		tags.add(SvgDecoder.TAG_SVG_FE_POINT_LIGHT);
		tags.add(SvgDecoder.TAG_SVG_FE_SPOT_LIGHT);
		tags.add(SvgDecoder.TAG_SVG_FE_COMPONENT_TRANSFER);
		tags.add(SvgDecoder.TAG_SVG_FE_CONVOLVE_MATRIX);
		tags.add(SvgDecoder.TAG_SVG_FE_DEFFUSE_LIGHTING);
		tags.add(SvgDecoder.TAG_SVG_FE_DISPLACEMENT_MAP);
		tags.add(SvgDecoder.TAG_SVG_FE_SPECULAR_LIGHTING);
		tags.add(SvgDecoder.TAG_SVG_TEXT);
		tags.add(SvgDecoder.TAG_SVG_ALT_GLYPH);
		tags.add(SvgDecoder.TAG_SVG_TEXT_PATH);
		tags.add(SvgDecoder.TAG_SVG_TREF);
		tags.add(SvgDecoder.TAG_SVG_TSPAN);
		tags.add(SvgDecoder.TAG_SVG_STOP);
	}
	
    protected boolean isValidTag(String tag) {
		return tags.contains(tag);
	}
    
    public void parse(InputStream in) {
        try {
            XmlPullParser parser = Xml.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(in, null);
            parser.nextTag();
            parse(parser);
            frame.init();
        } catch (Exception e){
        	Log.e(TAG, e.getMessage());
        } finally {
            try {
				in.close();
			} catch (Exception e) {
				Log.e(TAG, e.getMessage());
			}
        }
    }
    
    public void draw(Canvas c) {
    	frame.draw(c);
    }

    private void parse(XmlPullParser parser) throws XmlPullParserException, IOException {
    	parseElements(SvgDecoder.TAG_SVG, frame, parser);
    }
    
	protected void parseTag(String tag, SVGElement element, XmlPullParser parser) throws XmlPullParserException, IOException {
		SVGElement e = getElementByTag(tag);
		e.setName(tag);
		parseElements(tag, e, parser);
		//e.setData(readText(parser));
		element.addElement(e);
		if(tag.equalsIgnoreCase(SvgDecoder.TAG_SVG_G)) {
		} else {
			//skip(parser);
		}
	}
    
    private void skip(XmlPullParser parser) throws XmlPullParserException, IOException {
        if (parser.getEventType() != XmlPullParser.START_TAG) {
            throw new IllegalStateException();
        }
        int depth = 1;
        while (depth != 0) {
            switch (parser.next()) {
            case XmlPullParser.END_TAG:
                depth--;
                break;
            case XmlPullParser.START_TAG:
                depth++;
                break;
            }
        }
    }
 
    protected String readTagValue(String tag, XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, ns, tag);
        String result = readText(parser);
        parser.require(XmlPullParser.END_TAG, ns, tag);
        return result;
    }

    private String readText(XmlPullParser parser) throws IOException, XmlPullParserException {
        String result = null;
        if (parser.next() == XmlPullParser.TEXT) {
            result = parser.getText();
            parser.nextTag();
        }
        return result;
    }
    
	protected void parseAttrs(SVGElement element, XmlPullParser parser) throws XmlPullParserException, IOException {
    	int num = parser.getAttributeCount();
    	for(int i = 0; i < num; i++) {
    		String name = parser.getAttributeName(i);
    		String value = parser.getAttributeValue(i);
    		element.setAttr(name, value);
    	}
	}

    private void parseElements(String tag, SVGElement element, XmlPullParser parser) throws XmlPullParserException, IOException {
    	parser.require(XmlPullParser.START_TAG, SvgDecoder.ns, tag);
    	parseAttrs(element, parser);
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            // Starts by looking for the entry tag
            if (isValidTag(name)) {
            	parseTag(name, element, parser);
            } else {
                skip(parser);
            }
        }  
    }
    
    private SVGElement getElementByTag(String tag) {
    	SVGElement ret = null;
		if(tag.equalsIgnoreCase(SvgDecoder.TAG_SVG_G)) {
			ret = new SVGTagG();
		} else if(tag.equalsIgnoreCase(SvgDecoder.TAG_SVG_PATH)) {
			ret = new SVGTagPath();
		} else if(tag.equalsIgnoreCase(SvgDecoder.TAG_SVG_RECT)) {
			ret = new SVGTagRect();
		} else if(tag.equalsIgnoreCase(SvgDecoder.TAG_SVG_CIRCLE)) {
			ret = new SVGTagCircle();
		} else if(tag.equalsIgnoreCase(SvgDecoder.TAG_SVG_ELLIPSE)) {
			ret = new SVGTagEllipse();
		} else {
			ret = new SVGElement();
		}
		return ret;
    }
    
	public Shader getGradientById(String id) {
		return frame.getGradientById(id);
	}
	
	public int getWidth() {
		return 0; //frame.getGradientById(id);
	}

	public int getHeight() {
		return 0; //frame.getGradientById(id);
	}

    
    public static Float strToFloat(String str) {
        try {
        	return Float.parseFloat(str);
        } catch (Exception nfe) {
            return null;
        }
    }

    public static Float attrToFloat(String str, Float bound) {
        try {
        	boolean percent = false;
        	String v = str;
            if (v.endsWith("px")) {
                v = v.substring(0, v.length() - 2);
            }
    		else if(v.endsWith("%")) {
    			v = v.substring(0, v.length() - 1);
    			percent = true;
    		}
            Float ret = Float.parseFloat(v);
            if(percent) {
            	ret = ret * bound / 100;
            }

        	return ret;
        } catch (Exception nfe) {
            return null;
        }
    }
    
    
    public class SVGTagCircle extends SVGFigure{

    	private Float centerX = null;
    	private Float centerY = null;
    	private Float radius = null;


    	public SVGTagCircle() {
			// "circle"
			setName(SvgDecoder.TAG_SVG_CIRCLE);
		}

    	@Override
    	public void init() {
    		Float widthcanvas = (float)getWidth();
    		Float heightcanvas = (float)getHeight();
    		String centerXstr = getAttr("cx");
    		this.centerX = SvgDecoder.attrToFloat(centerXstr, widthcanvas);
    		String centerYstr = getAttr("cy");
    		this.centerY = SvgDecoder.attrToFloat(centerYstr, heightcanvas);
    		String radiusstr = getAttr("r");
    		this.radius = SvgDecoder.attrToFloat(radiusstr, widthcanvas);
			super.init();
		}

		@Override
    	public void drawData(Canvas c) {
			if(this.centerX != null && this.centerY != null && this.radius != null) {
				if(this.paintFill != null) {
					c.drawCircle(this.centerX, this.centerY, this.radius, this.paintFill);
				}
				if(this.paintStroke != null) {
					c.drawCircle(this.centerX, this.centerY, this.radius, this.paintStroke);
				}
			}
    	}
    }


    public class SVGTagEllipse extends SVGFigure{

    	private RectF rect = null;

    	public SVGTagEllipse() {
			// "ellipse"
			setName(SvgDecoder.TAG_SVG_ELLIPSE);
		}

    	@Override
    	public void init() {
    		Float widthcanvas = (float)getWidth();
    		Float heightcanvas = (float)getHeight();
    		String centerXstr = getAttr("cx");
    		Float centerX = SvgDecoder.attrToFloat(centerXstr, widthcanvas);
    		String centerYstr = getAttr("cy");
    		Float centerY = SvgDecoder.attrToFloat(centerYstr, heightcanvas);
    		String radiusXstr = getAttr("rx");
    		Float radiusX = SvgDecoder.attrToFloat(radiusXstr, widthcanvas);
    		String radiusYstr = getAttr("ry");
    		Float radiusY = SvgDecoder.attrToFloat(radiusYstr, heightcanvas);
    		if (centerX != null && centerY != null && radiusX != null && radiusY != null) {
    			this.rect = new RectF();
    			this.rect.set(centerX - radiusX, centerY - radiusY, centerX + radiusX, centerY + radiusY);
    		}

			super.init();
		}

		@Override
    	public void drawData(Canvas c) {
			if(this.rect != null) {
				if(this.paintFill != null) {
					c.drawOval(this.rect, this.paintFill);
				}
				if(this.paintStroke != null) {
					c.drawOval(this.rect, this.paintStroke);
				}
			}
    	}
    }

    
    public class SVGTagRect extends SVGFigure{
    	private Float x = null;
    	private Float y = null;
    	private Float width = null;
    	private Float height = null;

    	public SVGTagRect() {
			// "rect"
			setName(SvgDecoder.TAG_SVG_RECT);
		}

    	@Override
    	public void init() {
    		Float widthcanvas = (float)getWidth();
    		Float heightcanvas = (float)getHeight();
    		String strx = getAttr("x");
            this.x = SvgDecoder.attrToFloat(strx, widthcanvas);
            if (this.x == null) {
            	this.x = 0f;
            }
    		String stry = getAttr("y");
    		this.y = SvgDecoder.attrToFloat(stry, heightcanvas);
            if (this.y == null) {
            	this.y = 0f;
            }
    		String strwidth = getAttr("width");
    		this.width = SvgDecoder.attrToFloat(strwidth, widthcanvas);
    		String strheight = getAttr("height");
    		this.height = SvgDecoder.attrToFloat(strheight, heightcanvas);
			final String d = getAttr("d");
			super.init();
		}

		@Override
    	public void drawData(Canvas c) {
			if(this.paintFill != null) {
				c.drawRect(this.x, this.y,
						   this.x + this.width, 
						   this.y + this.height, 
						   this.paintFill);
			}
			if(this.paintStroke != null) {
				c.drawRect(this.x, this.y, 
						   this.x + this.width, 
						   this.y + this.height, 
						   this.paintFill);
			}
    	}
    }

    
	
    public class SVGTagPath extends SVGFigure{
    	private Path path = null;

    	public SVGTagPath() {
			// "path"
			setName(SvgDecoder.TAG_SVG_PATH);
		}

    	@Override
    	public void init() {
			final String d = getAttr("d");
			if(d != null) {
				this.path = doPath(d);
			}
			super.init();
		}

		@Override
    	public void drawData(Canvas c) {
			if(this.path != null) {
				if(this.paintFill != null) {
					c.drawPath(this.path, this.paintFill);
				}
				if(this.paintStroke != null) {
					c.drawPath(this.path, this.paintStroke);
				}
			}
    	}
    }

    public class SVGFigure extends SVGElement{
    	
    	protected boolean display = true;

    	protected Matrix matrix = null;
    	protected Paint paintFill = null;
    	protected Paint paintStroke = null;
    	protected StyleSet styles = null;

		@Override
    	public void init() {
			initParams();
			super.init();
		}
		
		protected void initParams() {
            final String transform = getAttr("transform");
            if(transform != null) {
            	this.matrix = parseTransform(transform);
            }
            final String styleAttr = getAttr("style");
            if(styleAttr != null) {
            	this.styles = new StyleSet(styleAttr);
            }
            
            if ("none".equals(getAttr("display"))) {
            	this.display = false;
            }
            
            this.paintFill = getFillPaint();
            this.paintStroke = getStrokePaint();
		}
		
		private String getStyleAttr(String name) {
            if(this.styles != null) {
            	return styles.getStyle(name);
            } else {
                return getAttr(name);
            }
		}
		
		protected Paint getFillPaint() {
			Paint paint = new Paint();
			paint.setAntiAlias(true);
            
            Shader shader = null;
            String fillString = getStyleAttr("fill");
            if (fillString != null && fillString.startsWith("url(#")) {
                String id = fillString.substring("url(#".length(), fillString.length() - 1);
                shader = getGradientById(id);
            }
            paint.setShader(shader);
            paint.setStyle(Paint.Style.FILL);
            if (fillString != null && !fillString.startsWith("#")) {
                try {
                	Integer color = Integer.parseInt(fillString.substring(1), 16);
                    int c = (0xFFFFFF & color) | 0xFF000000;
                    paint.setColor(c);
                    String opacitystr = getStyleAttr("opacity");
                    Float opacity = SvgDecoder.strToFloat(opacitystr);

                    if (opacity == null) {
                        opacity = SvgDecoder.strToFloat(getStyleAttr("fill-opacity"));
                    }
                    if (opacity == null) {
                    	paint.setAlpha(255);
                    } else {
                    	paint.setAlpha((int) (255 * opacity));
                    }
                } catch (NumberFormatException nfe) {
                	paint.setColor(0xFF000000);
                }
            }
			return paint;
		}

		protected Paint getStrokePaint() {
			Paint paint = new Paint();
			paint.setAntiAlias(true);
			Integer color = null;
			String strokeString = getStyleAttr("stroke");
            if (strokeString != null && !strokeString.startsWith("#")) {
                try {
                	color = Integer.parseInt(strokeString.substring(1), 16);
                } catch (NumberFormatException nfe) {
                }
            }
            if (color != null) {
                int c = (0xFFFFFF & color) | 0xFF000000;
                paint.setColor(c);
                String opacitystr = getStyleAttr("opacity");
                Float opacity = SvgDecoder.strToFloat(opacitystr);

                if (opacity == null) {
                    opacity = SvgDecoder.strToFloat(getStyleAttr("stroke-opacity"));
                }
                if (opacity == null) {
                	paint.setAlpha(255);
                } else {
                	paint.setAlpha((int) (255 * opacity));
                }
                // Check for other stroke attributes
                Float width = SvgDecoder.strToFloat(getStyleAttr("stroke-width"));
                // Set defaults

                if (width != null) {
                    paint.setStrokeWidth(width);
                }
                String linecap = getStyleAttr("stroke-linecap");
                if ("round".equals(linecap)) {
                    paint.setStrokeCap(Paint.Cap.ROUND);
                } else if ("square".equals(linecap)) {
                    paint.setStrokeCap(Paint.Cap.SQUARE);
                } else if ("butt".equals(linecap)) {
                    paint.setStrokeCap(Paint.Cap.BUTT);
                }
                String linejoin = getStyleAttr("stroke-linejoin");
                if ("miter".equals(linejoin)) {
                    paint.setStrokeJoin(Paint.Join.MITER);
                } else if ("round".equals(linejoin)) {
                    paint.setStrokeJoin(Paint.Join.ROUND);
                } else if ("bevel".equals(linejoin)) {
                    paint.setStrokeJoin(Paint.Join.BEVEL);
                }
                paint.setStyle(Paint.Style.STROKE);
                return paint;
            }
			return null;
		}

		
		@Override
    	public void draw(Canvas c) {
			if(!this.display) {
				return;
			}

			if(this.matrix != null) {
                c.save();
                c.concat(this.matrix);
			}
			
			drawData(c);
			
			if(this.matrix != null) {
                c.restore();
			}
			super.draw(c);
		}

    	public void drawData(Canvas c) {
    	}

		
    }

	
    public class SVGTagG extends SVGElement{
    	private boolean display = true;
    	
		public SVGTagG() {
			setName(SvgDecoder.TAG_SVG_G);
		}

		@Override
    	public void init() {
            if ("none".equals(getAttr("display"))) {
            	this.display = false;
            }
			
			super.init();
		}

		@Override
    	public void draw(Canvas c) {
			if(!this.display) {
				return;
			}

			super.draw(c);
		}
    }
    
    
    public class SVG extends SVGElement{
		public SVG() {
			setName(SvgDecoder.TAG_SVG);
		}
		
		public Shader getGradientById(String id) {
			return null;
		}
		
		@Override
    	public void init() {
			super.init();
		}
		
		@Override
    	public void draw(Canvas c) {
			super.draw(c);
		}
    }
    
    public class SVGElement {
    	private HashMap<String,String> attrs = new HashMap<String,String>();
    	private Vector<SVGElement> elements = new Vector<SVGElement>();
    	private String data = null;
    	private String name = null;
    	
    	public String getAttr(String attr) {
    		return this.attrs.get(attr);
    	}
    	
    	public void setAttr(String attr, String value) {
    		this.attrs.put(attr, value);
    	}
    	
    	public void addElement(SVGElement element) {
    		this.elements.add(element);
    	}
    	
    	public void setData(String data) {
    		this.data = data;
    	}
    	
    	public String getData() {
    		return this.data;
    	}
    	
    	public void setName(String name) {
    		this.name = name;
    	}
    	
    	public String getName() {
    		return this.name;
    	}
    	
    	public void init() {
    		int size = this.elements.size();
    		for(int i = 0; i < size; i++) {
    			this.elements.get(i).init();
    		}
    	}

    	public void draw(Canvas c) {
    		int size = this.elements.size();
    		for(int i = 0; i < size; i++) {
    			this.elements.get(i).draw(c);
    		}
    	}
    }
    
    
    



    public Path parsePath(String pathString) {
        return doPath(pathString);
    }
	
	public int read(InputStream is, int width, int height) {
		Log.w(TAG, "read input stream");
		Log.w(TAG, "");		
		frames.add(parse(is, 0, 0, width, height, true));
		frameCount = frames.size();
		Log.w(TAG, "read end");
		return frameCount;
	}

	public Picture getFramePicture(int n) {
		if (frameCount <= 0)
			return null;
		n = n % frameCount;
		return ((SVGOld) frames.elementAt(n)).getPicture();
	}

	public int getFrameCount() {
		return frameCount;
	}

	public int getDelay(int n) {
		return 100;
	}
	
	
    private SVGOld parse(InputStream in, Integer searchColor, Integer replaceColor, int width, int height, boolean whiteMode) throws SVGParseException {
		Log.w(TAG, "parse start");
        try {
	        SAXParserFactory spf = SAXParserFactory.newInstance();
	        SAXParser sp = spf.newSAXParser();
	        XMLReader xr = sp.getXMLReader();
 			final Picture picture = new Picture();
	        SVGHandler handler = new SVGHandler(picture, width, height);
	        InputSource is = new InputSource(in);
	        handler.setColorSwap(searchColor, replaceColor);
	        handler.setWhiteMode(whiteMode);
	        xr.setContentHandler(handler);
 			xr.parse(is);
 			SVGOld result = new SVGOld(picture, handler.bounds);
	        // Skip bounds if it was an empty pic
            if (!Float.isInfinite(handler.limits.top)) {
                result.setLimits(handler.limits);
            }
			Log.w(TAG, "parse end");
            return result;
        } catch (Exception e) {
			e.printStackTrace();
			Log.w(TAG, "exception - "+e.getMessage());
            throw new SVGParseException(e);
        }
    }

    private NumberParse parseNumbers(String s) {
        //Util.debug("Parsing numbers from: '" + s + "'");
        int n = s.length();
        int p = 0;
        ArrayList<Float> numbers = new ArrayList<Float>();
        boolean skipChar = false;
        for (int i = 1; i < n; i++) {
            if (skipChar) {
                skipChar = false;
                continue;
            }
            char c = s.charAt(i);
            switch (c) {
                // This ends the parsing, as we are on the next element
                case 'M':
                case 'm':
                case 'Z':
                case 'z':
                case 'L':
                case 'l':
                case 'H':
                case 'h':
                case 'V':
                case 'v':
                case 'C':
                case 'c':
                case 'S':
                case 's':
                case 'Q':
                case 'q':
                case 'T':
                case 't':
                case 'a':
                case 'A':
                case ')': {
                    String str = s.substring(p, i);
                    if (str.trim().length() > 0) {
                        //Util.debug("  Last: " + str);
                        Float f = Float.parseFloat(str);
                        numbers.add(f);
                    }
                    p = i;
                    return new NumberParse(numbers, p);
                }
                case '\n':
                case '\t':
                case ' ':
                case ',':
                case '-': {
                    String str = s.substring(p, i);
                    // Just keep moving if multiple whitespace
                    if (str.trim().length() > 0) {
                        //Util.debug("  Next: " + str);
                        Float f = Float.parseFloat(str);
                        numbers.add(f);
                        if (c == '-') {
                            p = i;
                        } else {
                            p = i + 1;
                            skipChar = true;
                        }
                    } else {
                        p++;
                    }
                    break;
                }
            }
        }
        String last = s.substring(p);
        if (last.length() > 0) {
            //Util.debug("  Last: " + last);
            try {
                numbers.add(Float.parseFloat(last));
            } catch (NumberFormatException nfe) {
                // Just white-space, forget it
            }
            p = s.length();
        }
        return new NumberParse(numbers, p);
    }

    private Matrix parseTransform(String s) {
        if (s.startsWith("matrix(")) {
            NumberParse np = parseNumbers(s.substring("matrix(".length()));
            if (np.numbers.size() == 6) {
                Matrix matrix = new Matrix();
                matrix.setValues(new float[]{
                        // Row 1
                        np.numbers.get(0),
                        np.numbers.get(2),
                        np.numbers.get(4),
                        // Row 2
                        np.numbers.get(1),
                        np.numbers.get(3),
                        np.numbers.get(5),
                        // Row 3
                        0,
                        0,
                        1,
                });
                return matrix;
            }
        } else if (s.startsWith("translate(")) {
            NumberParse np = parseNumbers(s.substring("translate(".length()));
            if (np.numbers.size() > 0) {
                float tx = np.numbers.get(0);
                float ty = 0;
                if (np.numbers.size() > 1) {
                    ty = np.numbers.get(1);
                }
                Matrix matrix = new Matrix();
                matrix.postTranslate(tx, ty);
                return matrix;
            }
        } else if (s.startsWith("scale(")) {
            NumberParse np = parseNumbers(s.substring("scale(".length()));
            if (np.numbers.size() > 0) {
                float sx = np.numbers.get(0);
                float sy = 0;
                if (np.numbers.size() > 1) {
                    sy = np.numbers.get(1);
                }
                Matrix matrix = new Matrix();
                matrix.postScale(sx, sy);
                return matrix;
            }
        } else if (s.startsWith("skewX(")) {
            NumberParse np = parseNumbers(s.substring("skewX(".length()));
            if (np.numbers.size() > 0) {
                float angle = np.numbers.get(0);
                Matrix matrix = new Matrix();
                matrix.postSkew((float) Math.tan(angle), 0);
                return matrix;
            }
        } else if (s.startsWith("skewY(")) {
            NumberParse np = parseNumbers(s.substring("skewY(".length()));
            if (np.numbers.size() > 0) {
                float angle = np.numbers.get(0);
                Matrix matrix = new Matrix();
                matrix.postSkew(0, (float) Math.tan(angle));
                return matrix;
            }
        } else if (s.startsWith("rotate(")) {
            NumberParse np = parseNumbers(s.substring("rotate(".length()));
            if (np.numbers.size() > 0) {
                float angle = np.numbers.get(0);
                float cx = 0;
                float cy = 0;
                if (np.numbers.size() > 2) {
                    cx = np.numbers.get(1);
                    cy = np.numbers.get(2);
                }
                Matrix matrix = new Matrix();
                matrix.postTranslate(cx, cy);
                matrix.postRotate(angle);
                matrix.postTranslate(-cx, -cy);
                return matrix;
            }
        }
        return null;
    }

    /**
     * This is where the hard-to-parse paths are handled.
     * Uppercase rules are absolute positions, lowercase are relative.
     * Types of path rules:
     * <p/>
     * <ol>
     * <li>M/m - (x y)+ - Move to (without drawing)
     * <li>Z/z - (no params) - Close path (back to starting point)
     * <li>L/l - (x y)+ - Line to
     * <li>H/h - x+ - Horizontal ine to
     * <li>V/v - y+ - Vertical line to
     * <li>C/c - (x1 y1 x2 y2 x y)+ - Cubic bezier to
     * <li>S/s - (x2 y2 x y)+ - Smooth cubic bezier to (shorthand that assumes the x2, y2 from previous C/S is the x1, y1 of this bezier)
     * <li>Q/q - (x1 y1 x y)+ - Quadratic bezier to
     * <li>T/t - (x y)+ - Smooth quadratic bezier to (assumes previous control point is "reflection" of last one w.r.t. to current point)
     * </ol>
     * <p/>
     * Numbers are separate by whitespace, comma or nothing at all (!) if they are self-delimiting, (ie. begin with a - sign)
     *
     * @param s the path string from the XML
     */
    private Path doPath(String s) {
        int n = s.length();
        ParserHelper ph = new ParserHelper(s, 0);
        ph.skipWhitespace();
        Path p = new Path();
        float lastX = 0;
        float lastY = 0;
        float lastX1 = 0;
        float lastY1 = 0;
        float subPathStartX = 0;
        float subPathStartY = 0;
        char prevCmd = 0;
        while (ph.pos < n) {
            char cmd = s.charAt(ph.pos);
            switch (cmd) {
                case '-':
                case '+':
                case '0':
                case '1':
                case '2':
                case '3':
                case '4':
                case '5':
                case '6':
                case '7':
                case '8':
                case '9':
                    if (prevCmd == 'm' || prevCmd == 'M') {
                        cmd = (char) (((int) prevCmd) - 1);
                        break;
                    } else if (prevCmd == 'c' || prevCmd == 'C') {
                        cmd = prevCmd;
                        break;
                    } else if (prevCmd == 'l' || prevCmd == 'L') {
                        cmd = prevCmd;
                        break;
                    }
                default: {
                    ph.advance();
                    prevCmd = cmd;
                }
            }

            boolean wasCurve = false;
            switch (cmd) {
                case 'M':
                case 'm': {
                    float x = ph.nextFloat();
                    float y = ph.nextFloat();
                    if (cmd == 'm') {
                        subPathStartX += x;
                        subPathStartY += y;
                        p.rMoveTo(x, y);
                        lastX += x;
                        lastY += y;
                    } else {
                        subPathStartX = x;
                        subPathStartY = y;
                        p.moveTo(x, y);
                        lastX = x;
                        lastY = y;
                    }
                    break;
                }
                case 'Z':
                case 'z': {
                    p.close();
                    p.moveTo(subPathStartX, subPathStartY);
                    lastX = subPathStartX;
                    lastY = subPathStartY;
                    lastX1 = subPathStartX;
                    lastY1 = subPathStartY;
                    wasCurve = true;
                    break;
                }
                case 'L':
                case 'l': {
                    float x = ph.nextFloat();
                    float y = ph.nextFloat();
                    if (cmd == 'l') {
                        p.rLineTo(x, y);
                        lastX += x;
                        lastY += y;
                    } else {
                        p.lineTo(x, y);
                        lastX = x;
                        lastY = y;
                    }
                    break;
                }
                case 'H':
                case 'h': {
                    float x = ph.nextFloat();
                    if (cmd == 'h') {
                        p.rLineTo(x, 0);
                        lastX += x;
                    } else {
                        p.lineTo(x, lastY);
                        lastX = x;
                    }
                    break;
                }
                case 'V':
                case 'v': {
                    float y = ph.nextFloat();
                    if (cmd == 'v') {
                        p.rLineTo(0, y);
                        lastY += y;
                    } else {
                        p.lineTo(lastX, y);
                        lastY = y;
                    }
                    break;
                }
                case 'C':
                case 'c': {
                    wasCurve = true;
                    float x1 = ph.nextFloat();
                    float y1 = ph.nextFloat();
                    float x2 = ph.nextFloat();
                    float y2 = ph.nextFloat();
                    float x = ph.nextFloat();
                    float y = ph.nextFloat();
                    if (cmd == 'c') {
                        x1 += lastX;
                        x2 += lastX;
                        x += lastX;
                        y1 += lastY;
                        y2 += lastY;
                        y += lastY;
                    }
                    p.cubicTo(x1, y1, x2, y2, x, y);
                    lastX1 = x2;
                    lastY1 = y2;
                    lastX = x;
                    lastY = y;
                    break;
                }
                case 'S':
                case 's': {
                    wasCurve = true;
                    float x2 = ph.nextFloat();
                    float y2 = ph.nextFloat();
                    float x = ph.nextFloat();
                    float y = ph.nextFloat();
                    if (cmd == 's') {
                        x2 += lastX;
                        x += lastX;
                        y2 += lastY;
                        y += lastY;
                    }
                    float x1 = 2 * lastX - lastX1;
                    float y1 = 2 * lastY - lastY1;
                    p.cubicTo(x1, y1, x2, y2, x, y);
                    lastX1 = x2;
                    lastY1 = y2;
                    lastX = x;
                    lastY = y;
                    break;
                }
                case 'A':
                case 'a': {
                    float rx = ph.nextFloat();
                    float ry = ph.nextFloat();
                    float theta = ph.nextFloat();
                    int largeArc = (int) ph.nextFloat();
                    int sweepArc = (int) ph.nextFloat();
                    float x = ph.nextFloat();
                    float y = ph.nextFloat();
                    drawArc(p, lastX, lastY, x, y, rx, ry, theta, largeArc, sweepArc);
                    lastX = x;
                    lastY = y;
                    break;
                }
            }
            if (!wasCurve) {
                lastX1 = lastX;
                lastY1 = lastY;
            }
            ph.skipWhitespace();
        }
        return p;
    }

    private void drawArc(Path p, float lastX, float lastY, float x, float y, float rx, float ry, float theta, int largeArc, int sweepArc) {
        // todo - not implemented yet, may be very hard to do using Android drawing facilities.
    }

    private NumberParse getNumberParseAttr(String name, Attributes attributes) {
        int n = attributes.getLength();
        for (int i = 0; i < n; i++) {
            if (attributes.getLocalName(i).equals(name)) {
                return parseNumbers(attributes.getValue(i));
            }
        }
        return null;
    }

    private String getStringAttr(String name, Attributes attributes) {
        int n = attributes.getLength();
        for (int i = 0; i < n; i++) {
            if (attributes.getLocalName(i).equals(name)) {
                return attributes.getValue(i);
            }
        }
        return null;
    }

    private Float getFloatAttr(String name, Attributes attributes) {
        return getFloatAttr(name, attributes, null);
    }

    private Float getFloatAttr(String name, Attributes attributes, Float defaultValue) {
        String v = getStringAttr(name, attributes);
        if (v == null) {
            return defaultValue;
        } else {
            if (v.endsWith("px")) {
                v = v.substring(0, v.length() - 2);
            }
			else if(v.endsWith("%")) {
				v = v.substring(0, v.length() - 1);
			}
//            Log.d(TAG, "Float parsing '" + name + "=" + v + "'");
            return Float.parseFloat(v);
        }
    }

    private Integer getHexAttr(String name, Attributes attributes) {
        String v = getStringAttr(name, attributes);
        //Util.debug("Hex parsing '" + name + "=" + v + "'");
        if (v == null) {
            return null;
        } else {
            try {
                return Integer.parseInt(v.substring(1), 16);
            } catch (NumberFormatException nfe) {
                // todo - parse word-based color here
                return null;
            }
        }
    }
	
	public class SVGOld {

		private Picture picture;

		private RectF bounds;

		private RectF limits = null;

		SVGOld(Picture picture, RectF bounds) {
			this.picture = picture;
			this.bounds = bounds;
		}

		void setLimits(RectF limits) {
			this.limits = limits;
		}

		public PictureDrawable createPictureDrawable() {
			return new PictureDrawable(picture);
		}

		public Picture getPicture() {
			return picture;
		}

		public RectF getBounds() {
			return bounds;
		}

		public RectF getLimits() {
			return limits;
		}
	}

    private class NumberParse {
        private ArrayList<Float> numbers;
        private int nextCmd;

        public NumberParse(ArrayList<Float> numbers, int nextCmd) {
            this.numbers = numbers;
            this.nextCmd = nextCmd;
        }

        public int getNextCmd() {
            return nextCmd;
        }

        public float getNumber(int index) {
            return numbers.get(index);
        }

    }
	
	public class SVGParseException extends RuntimeException {

		public SVGParseException(String s) {
			super(s);
		}

		public SVGParseException(String s, Throwable throwable) {
			super(s, throwable);
		}

		public SVGParseException(Throwable throwable) {
			super(throwable);
		}
	}
	

    private class Gradient {
        String id;
        String xlink;
        boolean isLinear;
        float x1, y1, x2, y2;
        float x, y, radius;
        ArrayList<Float> positions = new ArrayList<Float>();
        ArrayList<Integer> colors = new ArrayList<Integer>();
        Matrix matrix = null;

        public Gradient createChild(Gradient g) {
            Gradient child = new Gradient();
            child.id = g.id;
            child.xlink = id;
            child.isLinear = g.isLinear;
            child.x1 = g.x1;
            child.x2 = g.x2;
            child.y1 = g.y1;
            child.y2 = g.y2;
            child.x = g.x;
            child.y = g.y;
            child.radius = g.radius;
            child.positions = positions;
            child.colors = colors;
            child.matrix = matrix;
            if (g.matrix != null) {
                if (matrix == null) {
                    child.matrix = g.matrix;
                } else {
                    Matrix m = new Matrix(matrix);
                    m.preConcat(g.matrix);
                    child.matrix = m;
                }
            }
            return child;
        }
    }

    private class StyleSet {
        HashMap<String, String> styleMap = new HashMap<String, String>();

        private StyleSet(String string) {
            String[] styles = string.split(";");
            for (String s : styles) {
                String[] style = s.split(":");
                if (style.length == 2) {
                    styleMap.put(style[0], style[1]);
                }
            }
        }

        public String getStyle(String name) {
            return styleMap.get(name);
        }
    }

    private class Properties {
        StyleSet styles = null;
        Attributes atts;

        private Properties(Attributes atts) {
            this.atts = atts;
            String styleAttr = getStringAttr("style", atts);
            if (styleAttr != null) {
                styles = new StyleSet(styleAttr);
            }
        }

        public String getAttr(String name) {
            String v = null;
            if (styles != null) {
                v = styles.getStyle(name);
            }
            if (v == null) {
                v = getStringAttr(name, atts);
            }
            return v;
        }

        public String getString(String name) {
            return getAttr(name);
        }

        public Integer getHex(String name) {
            String v = getAttr(name);
            if (v == null || !v.startsWith("#")) {
                return null;
            } else {
                try {
                    return Integer.parseInt(v.substring(1), 16);
                } catch (NumberFormatException nfe) {
                    // todo - parse word-based color here
                    return null;
                }
            }
        }

        public Float getFloat(String name, float defaultValue) {
            Float v = getFloat(name);
            if (v == null) {
                return defaultValue;
            } else {
                return v;
            }
        }

        public Float getFloat(String name) {
            String v = getAttr(name);
            if (v == null) {
                return null;
            } else {
                try {
                    return Float.parseFloat(v);
                } catch (NumberFormatException nfe) {
                    return null;
                }
            }
        }
    }
	
	public class ParserHelper {

		private char current;
		private CharSequence s;
		public int pos;
		private int n;

		public ParserHelper(CharSequence s, int pos) {
			this.s = s;
			this.pos = pos;
			n = s.length();
			current = s.charAt(pos);
		}

		private char read() {
			if (pos < n) {
				pos++;
			}
			if (pos == n) {
				return '\0';
			} else {
				return s.charAt(pos);
			}
		}

		public void skipWhitespace() {
			while (pos < n) {
				if (Character.isWhitespace(s.charAt(pos))) {
					advance();
				} else {
					break;
				}
			}
		}

		public void skipNumberSeparator() {
			while (pos < n) {
				char c = s.charAt(pos);
				switch (c) {
					case ' ':
					case ',':
					case '\n':
					case '\t':
						advance();
						break;
					default:
						return;
				}
			}
		}

		public void advance() {
			current = read();
		}

		/**
		 * Parses the content of the buffer and converts it to a float.
		 */
		public float parseFloat() {
			int     mant     = 0;
			int     mantDig  = 0;
			boolean mantPos  = true;
			boolean mantRead = false;

			int     exp      = 0;
			int     expDig   = 0;
			int     expAdj   = 0;
			boolean expPos   = true;

			switch (current) {
				case '-':
					mantPos = false;
					// fallthrough
				case '+':
					current = read();
			}

			m1: switch (current) {
				default:
					return Float.NaN;

				case '.':
					break;

				case '0':
					mantRead = true;
					l: for (;;) {
						current = read();
						switch (current) {
							case '1': case '2': case '3': case '4':
							case '5': case '6': case '7': case '8': case '9':
								break l;
							case '.': case 'e': case 'E':
								break m1;
							default:
								return 0.0f;
							case '0':
						}
					}

				case '1': case '2': case '3': case '4':
				case '5': case '6': case '7': case '8': case '9':
					mantRead = true;
					l: for (;;) {
						if (mantDig < 9) {
							mantDig++;
							mant = mant * 10 + (current - '0');
						} else {
							expAdj++;
						}
						current = read();
						switch (current) {
							default:
								break l;
							case '0': case '1': case '2': case '3': case '4':
							case '5': case '6': case '7': case '8': case '9':
						}
					}
			}

			if (current == '.') {
				current = read();
				m2: switch (current) {
					default:
					case 'e': case 'E':
						if (!mantRead) {
							reportUnexpectedCharacterError( current );
							return 0.0f;
						}
						break;

					case '0':
						if (mantDig == 0) {
							l: for (;;) {
								current = read();
								expAdj--;
								switch (current) {
									case '1': case '2': case '3': case '4':
									case '5': case '6': case '7': case '8': case '9':
										break l;
									default:
										if (!mantRead) {
											return 0.0f;
										}
										break m2;
									case '0':
								}
							}
						}
					case '1': case '2': case '3': case '4':
					case '5': case '6': case '7': case '8': case '9':
						l: for (;;) {
							if (mantDig < 9) {
								mantDig++;
								mant = mant * 10 + (current - '0');
								expAdj--;
							}
							current = read();
							switch (current) {
								default:
									break l;
								case '0': case '1': case '2': case '3': case '4':
								case '5': case '6': case '7': case '8': case '9':
							}
						}
				}
			}

			switch (current) {
				case 'e': case 'E':
					current = read();
					switch (current) {
						default:
							reportUnexpectedCharacterError( current );
							return 0f;
						case '-':
							expPos = false;
						case '+':
							current = read();
							switch (current) {
								default:
									reportUnexpectedCharacterError( current );
									return 0f;
								case '0': case '1': case '2': case '3': case '4':
								case '5': case '6': case '7': case '8': case '9':
							}
						case '0': case '1': case '2': case '3': case '4':
						case '5': case '6': case '7': case '8': case '9':
					}

					en: switch (current) {
						case '0':
							l: for (;;) {
								current = read();
								switch (current) {
									case '1': case '2': case '3': case '4':
									case '5': case '6': case '7': case '8': case '9':
										break l;
									default:
										break en;
									case '0':
								}
							}

						case '1': case '2': case '3': case '4':
						case '5': case '6': case '7': case '8': case '9':
							l: for (;;) {
								if (expDig < 3) {
									expDig++;
									exp = exp * 10 + (current - '0');
								}
								current = read();
								switch (current) {
									default:
										break l;
									case '0': case '1': case '2': case '3': case '4':
									case '5': case '6': case '7': case '8': case '9':
								}
							}
					}
				default:
			}

			if (!expPos) {
				exp = -exp;
			}
			exp += expAdj;
			if (!mantPos) {
				mant = -mant;
			}

			return buildFloat(mant, exp);
		}

		private void reportUnexpectedCharacterError(char c) {
			throw new RuntimeException("Unexpected char '" + c + "'.");
		}

		/**
		 * Computes a float from mantissa and exponent.
		 */
		public float buildFloat(int mant, int exp) {
			if (exp < -125 || mant == 0) {
				return 0.0f;
			}

			if (exp >=  128) {
				return (mant > 0)
					? Float.POSITIVE_INFINITY
					: Float.NEGATIVE_INFINITY;
			}

			if (exp == 0) {
				return mant;
			}

			if (mant >= (1 << 26)) {
				mant++;  // round up trailing bits if they will be dropped.
			}

			return (float) ((exp > 0) ? mant * pow10[exp] : mant / pow10[-exp]);
		}

		/**
		 * Array of powers of ten. Using double instead of float gives a tiny bit more precision.
		 */
		private double[] pow10 = new double[128];

	    {
			for (int i = 0; i < pow10.length; i++) {
				pow10[i] = Math.pow(10, i);
			}
		}

		public float nextFloat() {
			skipWhitespace();
			float f = parseFloat();
			skipNumberSeparator();
			return f;
		}
	}

    private class SVGHandler extends DefaultHandler {

        Picture picture;
        Canvas canvas;
        Paint paint;
        // Scratch rect (so we aren't constantly making new ones)
        RectF rect = new RectF();
        RectF bounds = null;
        RectF limits = new RectF(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY);

        Integer searchColor = null;
        Integer replaceColor = null;

        boolean whiteMode = false;

        boolean pushed = false;
		
		int width = 0;
		int height = 0;

        HashMap<String, Shader> gradientMap = new HashMap<String, Shader>();
        HashMap<String, Gradient> gradientRefMap = new HashMap<String, Gradient>();
        Gradient gradient = null;

        private SVGHandler(Picture picture) {
            this.picture = picture;
            paint = new Paint();
            paint.setAntiAlias(true);
        }
		
		private SVGHandler(Picture picture, int width, int height) {
            this.picture = picture;
            paint = new Paint();
            paint.setAntiAlias(true);
			this.width = width;
			this.height = height;
        }
		

        public void setColorSwap(Integer searchColor, Integer replaceColor) {
            this.searchColor = searchColor;
            this.replaceColor = replaceColor;
        }

        public void setWhiteMode(boolean whiteMode) {
            this.whiteMode = whiteMode;
        }

        @Override
        public void startDocument() throws SAXException {
            // Set up prior to parsing a doc
        }

        @Override
        public void endDocument() throws SAXException {
            // Clean up after parsing a doc
        }

        private boolean doFill(Properties atts, HashMap<String, Shader> gradients) {
            if ("none".equals(atts.getString("display"))) {
                return false;
            }
            if (whiteMode) {
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(0xFFFFFFFF);
                return true;
            }
            String fillString = atts.getString("fill");
            if (fillString != null && fillString.startsWith("url(#")) {
                // It's a gradient fill, look it up in our map
                String id = fillString.substring("url(#".length(), fillString.length() - 1);
                Shader shader = gradients.get(id);
                if (shader != null) {
                    //Util.debug("Found shader!");
                    paint.setShader(shader);
                    paint.setStyle(Paint.Style.FILL);
                    return true;
                } else {
                    //Util.debug("Didn't find shader!");
                    return false;
                }
            } else {
                paint.setShader(null);
                Integer color = atts.getHex("fill");
                if (color != null) {
                    doColor(atts, color, true);
                    paint.setStyle(Paint.Style.FILL);
                    return true;
                } else if (atts.getString("fill") == null && atts.getString("stroke") == null) {
                    // Default is black fill
                    paint.setStyle(Paint.Style.FILL);
                    paint.setColor(0xFF000000);
                    return true;
                }
            }
            return false;
        }

        private boolean doStroke(Properties atts) {
            if (whiteMode) {
                // Never stroke in white mode
                return false;
            }
            if ("none".equals(atts.getString("display"))) {
                return false;
            }
            Integer color = atts.getHex("stroke");
            if (color != null) {
                doColor(atts, color, false);
                // Check for other stroke attributes
                Float width = atts.getFloat("stroke-width");
                // Set defaults

                if (width != null) {
                    paint.setStrokeWidth(width);
                }
                String linecap = atts.getString("stroke-linecap");
                if ("round".equals(linecap)) {
                    paint.setStrokeCap(Paint.Cap.ROUND);
                } else if ("square".equals(linecap)) {
                    paint.setStrokeCap(Paint.Cap.SQUARE);
                } else if ("butt".equals(linecap)) {
                    paint.setStrokeCap(Paint.Cap.BUTT);
                }
                String linejoin = atts.getString("stroke-linejoin");
                if ("miter".equals(linejoin)) {
                    paint.setStrokeJoin(Paint.Join.MITER);
                } else if ("round".equals(linejoin)) {
                    paint.setStrokeJoin(Paint.Join.ROUND);
                } else if ("bevel".equals(linejoin)) {
                    paint.setStrokeJoin(Paint.Join.BEVEL);
                }
                paint.setStyle(Paint.Style.STROKE);
                return true;
            }
            return false;
        }

        private Gradient doGradient(boolean isLinear, Attributes atts) {
            Gradient gradient = new Gradient();
            gradient.id = getStringAttr("id", atts);
            gradient.isLinear = isLinear;
            if (isLinear) {
                gradient.x1 = getFloatAttr("x1", atts, 0f);
                gradient.x2 = getFloatAttr("x2", atts, 0f);
                gradient.y1 = getFloatAttr("y1", atts, 0f);
                gradient.y2 = getFloatAttr("y2", atts, 0f);
            } else {
                gradient.x = getFloatAttr("cx", atts, 0f);
                gradient.y = getFloatAttr("cy", atts, 0f);
                gradient.radius = getFloatAttr("r", atts, 0f);
            }
            String transform = getStringAttr("gradientTransform", atts);
            if (transform != null) {
                gradient.matrix = parseTransform(transform);
            }
            String xlink = getStringAttr("href", atts);
            if (xlink != null) {
                if (xlink.startsWith("#")) {
                    xlink = xlink.substring(1);
                }
                gradient.xlink = xlink;
            }
            return gradient;
        }

        private void doColor(Properties atts, Integer color, boolean fillMode) {
            int c = (0xFFFFFF & color) | 0xFF000000;
            if (searchColor != null && searchColor.intValue() == c) {
                c = replaceColor;
            }
            paint.setColor(c);
            Float opacity = atts.getFloat("opacity");
            if (opacity == null) {
                opacity = atts.getFloat(fillMode ? "fill-opacity" : "stroke-opacity");
            }
            if (opacity == null) {
                paint.setAlpha(255);
            } else {
                paint.setAlpha((int) (255 * opacity));
            }
        }

        private boolean hidden = false;
        private int hiddenLevel = 0;
        private boolean boundsMode = false;

        private void doLimits(float x, float y) {
            if (x < limits.left) {
                limits.left = x;
            }
            if (x > limits.right) {
                limits.right = x;
            }
            if (y < limits.top) {
                limits.top = y;
            }
            if (y > limits.bottom) {
                limits.bottom = y;
            }
        }

        private void doLimits(float x, float y, float width, float height) {
            doLimits(x, y);
            doLimits(x + width, y + height);
        }

        private void doLimits(Path path) {
            path.computeBounds(rect, false);
            doLimits(rect.left, rect.top);
            doLimits(rect.right, rect.bottom);
        }

        private void pushTransform(Attributes atts) {
            final String transform = getStringAttr("transform", atts);
            pushed = transform != null;
            if (pushed) {
                final Matrix matrix = parseTransform(transform);
                canvas.save();
                canvas.concat(matrix);
            }
        }

        private void popTransform() {
            if (pushed) {
                canvas.restore();
            }
        }

        @Override
        public void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException {
            // Reset paint opacity
			Log.w(TAG, "startElement start");
            paint.setAlpha(255);
            // Ignore everything but rectangles in bounds mode
			Log.w(TAG, "localName: "+localName);			
            if (boundsMode) {
                if (localName.equals("rect")) {
                    Float x = getFloatAttr("x", atts);
                    if (x == null) {
                        x = 0f;
                    }
                    Float y = getFloatAttr("y", atts);
                    if (y == null) {
                        y = 0f;
                    }
                    Float width = getFloatAttr("width", atts);
                    Float height = getFloatAttr("height", atts);
                    bounds = new RectF(x, y, x + width, y + width);
                }
                return;
            }
            if (localName.equals("svg")) {
				Float widthf = getFloatAttr("width", atts);
				Float heightf = getFloatAttr("height", atts);
				if(widthf != null && heightf != null) {
                	int width = (int) Math.ceil(widthf);
                	int height = (int) Math.ceil(heightf);
                	canvas = picture.beginRecording(width, height);
				}
				else {
                	canvas = picture.beginRecording(this.width, this.height);
				}
            } else if (localName.equals("defs")) {
                // Ignore
            } else if (localName.equals("linearGradient")) {
                gradient = doGradient(true, atts);
            } else if (localName.equals("radialGradient")) {
                gradient = doGradient(false, atts);
            } else if (localName.equals("stop")) {
                if (gradient != null) {
                    float offset = getFloatAttr("offset", atts);
                    String styles = getStringAttr("style", atts);
                    int color = 0;
                    if(styles!=null) {
	                    StyleSet styleSet = new StyleSet(styles);
	                    String colorStyle = styleSet.getStyle("stop-color");
	                    color = getColor(colorStyle);
	                    String opacityStyle = styleSet.getStyle("stop-opacity");
	                    if (opacityStyle != null) {
	                        float alpha = Float.parseFloat(opacityStyle);
	                        int alphaInt = Math.round(255 * alpha);
	                        color |= (alphaInt << 24);
	                    } else {
	                        color |= 0xFF000000;
	                    }
                    }
                    gradient.positions.add(offset);
                    gradient.colors.add(color);
                }
            } else if (localName.equals("g")) {
                // Check to see if this is the "bounds" layer
                if ("bounds".equalsIgnoreCase(getStringAttr("id", atts))) {
                    boundsMode = true;
                }
                if (hidden) {
                    hiddenLevel++;
                    //Util.debug("Hidden up: " + hiddenLevel);
                }
                // Go in to hidden mode if display is "none"
                if ("none".equals(getStringAttr("display", atts))) {
                    if (!hidden) {
                        hidden = true;
                        hiddenLevel = 1;
                        //Util.debug("Hidden up: " + hiddenLevel);
                    }
                }
            } else if (!hidden && localName.equals("rect")) {
                Float x = getFloatAttr("x", atts);
                if (x == null) {
                    x = 0f;
                }
                Float y = getFloatAttr("y", atts);
                if (y == null) {
                    y = 0f;
                }
                Float width = getFloatAttr("width", atts);
                Float height = getFloatAttr("height", atts);
                pushTransform(atts);
                Properties props = new Properties(atts);
                if (doFill(props, gradientMap)) {
                    doLimits(x, y, width, height);
                    canvas.drawRect(x, y, x + width, y + height, paint);
                }
                if (doStroke(props)) {
                    canvas.drawRect(x, y, x + width, y + height, paint);
                }
                popTransform();
            } else if (!hidden && localName.equals("line")) {
                Float x1 = getFloatAttr("x1", atts);
                Float x2 = getFloatAttr("x2", atts);
                Float y1 = getFloatAttr("y1", atts);
                Float y2 = getFloatAttr("y2", atts);
                Properties props = new Properties(atts);
                if (doStroke(props)) {
                    pushTransform(atts);
                    doLimits(x1, y1);
                    doLimits(x2, y2);
                    canvas.drawLine(x1, y1, x2, y2, paint);
                    popTransform();
                }
            } else if (!hidden && localName.equals("circle")) {
                Float centerX = getFloatAttr("cx", atts);
                Float centerY = getFloatAttr("cy", atts);
                Float radius = getFloatAttr("r", atts);
                if (centerX != null && centerY != null && radius != null) {
                    pushTransform(atts);
                    Properties props = new Properties(atts);
                    if (doFill(props, gradientMap)) {
                        doLimits(centerX - radius, centerY - radius);
                        doLimits(centerX + radius, centerY + radius);
                        canvas.drawCircle(centerX, centerY, radius, paint);
                    }
                    if (doStroke(props)) {
                        canvas.drawCircle(centerX, centerY, radius, paint);
                    }
                    popTransform();
                }
            } else if (!hidden && localName.equals("ellipse")) {
                Float centerX = getFloatAttr("cx", atts);
                Float centerY = getFloatAttr("cy", atts);
                Float radiusX = getFloatAttr("rx", atts);
                Float radiusY = getFloatAttr("ry", atts);
                if (centerX != null && centerY != null && radiusX != null && radiusY != null) {
                    pushTransform(atts);
                    Properties props = new Properties(atts);
                    rect.set(centerX - radiusX, centerY - radiusY, centerX + radiusX, centerY + radiusY);
                    if (doFill(props, gradientMap)) {
                        doLimits(centerX - radiusX, centerY - radiusY);
                        doLimits(centerX + radiusX, centerY + radiusY);
                        canvas.drawOval(rect, paint);
                    }
                    if (doStroke(props)) {
                        canvas.drawOval(rect, paint);
                    }
                    popTransform();
                }
            } else if (!hidden && (localName.equals("polygon") || localName.equals("polyline"))) {
                NumberParse numbers = getNumberParseAttr("points", atts);
                if (numbers != null) {
                    Path p = new Path();
                    ArrayList<Float> points = numbers.numbers;
                    if (points.size() > 1) {
                        pushTransform(atts);
                        Properties props = new Properties(atts);
                        p.moveTo(points.get(0), points.get(1));
                        for (int i = 2; i < points.size(); i += 2) {
                            float x = points.get(i);
                            float y = points.get(i + 1);
                            p.lineTo(x, y);
                        }
                        // Don't close a polyline
                        if (localName.equals("polygon")) {
                            p.close();
                        }
                        if (doFill(props, gradientMap)) {
                            doLimits(p);
                            canvas.drawPath(p, paint);
                        }
                        if (doStroke(props)) {
                            canvas.drawPath(p, paint);
                        }
                        popTransform();
                    }
                }
            } else if (!hidden && localName.equals("path")) {
                Path p = doPath(getStringAttr("d", atts));
                pushTransform(atts);
                Properties props = new Properties(atts);
                if (doFill(props, gradientMap)) {
                    doLimits(p);
                    canvas.drawPath(p, paint);
                }
                if (doStroke(props)) {
                    canvas.drawPath(p, paint);
                }
                popTransform();
            } else if (!hidden) {
                Log.d(TAG, "UNRECOGNIZED SVG COMMAND: " + localName);
            }
			Log.w(TAG, "startElement end");
        }
		
		public int getColor(String color) {
			int ret = Color.BLACK;
			if (color != null) {
				if (color.startsWith("#")) {
					ret = Integer.parseInt(color.substring(1), 16);
				} else if (color.equalsIgnoreCase("black")) {
					ret = Color.BLACK;
				} else if (color.equalsIgnoreCase("white")) {
					ret = Color.WHITE;
				} else if (color.equalsIgnoreCase("blue")) {
					ret = Color.BLUE;
				} else if (color.equalsIgnoreCase("yellow")) {
					ret = Color.YELLOW;
				} else if (color.equalsIgnoreCase("red")) {
					ret = Color.RED;
				} else if (color.equalsIgnoreCase("green")) {
					ret = Color.GREEN;
				} else if (color.equalsIgnoreCase("gray")) {
					ret = Color.GRAY;
				} else {
					ret = Integer.parseInt(color, 16);
				}
			}
			return ret;			
		}

        @Override
        public void characters(char ch[], int start, int length) {
            // no-op
        }

        @Override
        public void endElement(String namespaceURI, String localName, String qName)
                throws SAXException {
            if (localName.equals("svg")) {
                picture.endRecording();
            } else if (localName.equals("linearGradient")) {
                if (gradient.id != null) {
                    if (gradient.xlink != null) {
                        Gradient parent = gradientRefMap.get(gradient.xlink);
                        if (parent != null) {
                            gradient = parent.createChild(gradient);
                        }
                    }
                    int[] colors = new int[gradient.colors.size()];
                    for (int i = 0; i < colors.length; i++) {
                        colors[i] = gradient.colors.get(i);
                    }
                    float[] positions = new float[gradient.positions.size()];
                    for (int i = 0; i < positions.length; i++) {
                        positions[i] = gradient.positions.get(i);
                    }
                    if (colors.length == 0) {
                        Log.d("BAD", "BAD");
                    }
                    LinearGradient g = new LinearGradient(gradient.x1, gradient.y1, gradient.x2, gradient.y2, colors, positions, Shader.TileMode.CLAMP);
                    if (gradient.matrix != null) {
                        g.setLocalMatrix(gradient.matrix);
                    }
                    gradientMap.put(gradient.id, g);
                    gradientRefMap.put(gradient.id, gradient);
                }
            } else if (localName.equals("radialGradient")) {
                if (gradient.id != null) {
                    if (gradient.xlink != null) {
                        Gradient parent = gradientRefMap.get(gradient.xlink);
                        if (parent != null) {
                            gradient = parent.createChild(gradient);
                        }
                    }
                    int[] colors = new int[gradient.colors.size()];
                    for (int i = 0; i < colors.length; i++) {
                        colors[i] = gradient.colors.get(i);
                    }
                    float[] positions = new float[gradient.positions.size()];
                    for (int i = 0; i < positions.length; i++) {
                        positions[i] = gradient.positions.get(i);
                    }
                    if (gradient.xlink != null) {
                        Gradient parent = gradientRefMap.get(gradient.xlink);
                        if (parent != null) {
                            gradient = parent.createChild(gradient);
                        }
                    }
                    RadialGradient g = new RadialGradient(gradient.x, gradient.y, gradient.radius, colors, positions, Shader.TileMode.CLAMP);
                    if (gradient.matrix != null) {
                        g.setLocalMatrix(gradient.matrix);
                    }
                    gradientMap.put(gradient.id, g);
                    gradientRefMap.put(gradient.id, gradient);
                }
            } else if (localName.equals("g")) {
                if (boundsMode) {
                    boundsMode = false;
                }
                // Break out of hidden mode
                if (hidden) {
                    hiddenLevel--;
                    //Util.debug("Hidden down: " + hiddenLevel);
                    if (hiddenLevel == 0) {
                        hidden = false;
                    }
                }
                // Clear gradient map
                gradientMap.clear();
            }
        }
    }
}
