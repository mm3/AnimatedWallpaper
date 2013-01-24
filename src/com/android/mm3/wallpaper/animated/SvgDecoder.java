package com.android.mm3.wallpaper.animated;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Picture;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.Log;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Vector;
import java.util.ArrayList;
import java.util.HashMap;


public class SvgDecoder extends Decoder {

	public static final String TAG = "SvgDecoder";
	
	protected int frameCount = 1;
	protected SVG frame = new SVG(SvgDecoder.TAG_SVG, null);
	
	private int width = 0;
	private int height = 0;
	
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
    
	public void setWidthHeight(int width, int height) {
    	this.width = width;
    	this.height = height;
    }
    
    @Override
	public int getWidth() {
    	int localWidth = frame.getWidth();
    	if(localWidth == 0) {
    		localWidth = this.width;
    	}
		return localWidth;
	}

    @Override
	public int getHeight() {
    	int localHeight = frame.getHeight();
    	if(localHeight == 0) {
    		localHeight = this.height;
    	}
		return localHeight;
	}
	
	public Picture getFramePicture(int n) {
		if (frameCount <= 0)
			return null;
		n = n % frameCount;
		Picture p = new Picture();
		Canvas c = p.beginRecording(getWidth(), getHeight());
		frame.draw(c);
		p.endRecording();
		return p;
	}

	public int getFrameCount() {
		return frameCount;
	}

	public int getDelay(int n) {
		return frame.getDelay();
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
		SVGElement e = getElementByTag(tag, element);
		parseElements(tag, e, parser);
		//e.setData(readText(parser));
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
                    return new NumberParse(numbers);
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
        return new NumberParse(numbers);
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
    private Path parsePath(String s) {
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
    
    private SVGElement getElementByTag(String tag, SVGElement parent) {
    	SVGElement ret = null;
		if(tag.equalsIgnoreCase(SvgDecoder.TAG_SVG_G)) {
			ret = new SVGTagG(tag, parent);
		} else if(tag.equalsIgnoreCase(SvgDecoder.TAG_SVG_PATH)) {
			ret = new SVGTagPath(tag, parent);
		} else if(tag.equalsIgnoreCase(SvgDecoder.TAG_SVG_RECT)) {
			ret = new SVGTagRect(tag, parent);
		} else if(tag.equalsIgnoreCase(SvgDecoder.TAG_SVG_CIRCLE)) {
			ret = new SVGTagCircle(tag, parent);
		} else if(tag.equalsIgnoreCase(SvgDecoder.TAG_SVG_ELLIPSE)) {
			ret = new SVGTagEllipse(tag, parent);
		} else if(tag.equalsIgnoreCase(SvgDecoder.TAG_SVG_LINE)) {
			ret = new SVGTagLine(tag, parent);
		} else if(tag.equalsIgnoreCase(SvgDecoder.TAG_SVG_POLYLINE)) {
			ret = new SVGTagPolyline(tag, parent);
		} else if(tag.equalsIgnoreCase(SvgDecoder.TAG_SVG_POLYGON)) {
			ret = new SVGTagPolygon(tag, parent);
		} else if(tag.equalsIgnoreCase(SvgDecoder.TAG_SVG_LINEAR_GRADIENT)) {
			ret = new SVGTagLinearGradient(tag, parent);
		} else if(tag.equalsIgnoreCase(SvgDecoder.TAG_SVG_RADIAL_GRADIENT)) {
			ret = new SVGTagRadialGradient(tag, parent);
		} else if(tag.equalsIgnoreCase(SvgDecoder.TAG_SVG_STOP)) {
			ret = new SVGTagStop(tag, parent);
		} else if(tag.equalsIgnoreCase(SvgDecoder.TAG_SVG_ANIMATE)) {
			ret = new SVGTagAnimate(tag, parent);
		} else {
			ret = new SVGElement(tag, parent);
		}
		return ret;
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
    
	public static int getColor(String color) {
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

    public class SVGTagLine extends SVGFigure{

    	private Float x1 = null;
    	private Float x2 = null;
    	private Float y1 = null;
    	private Float y2 = null;

    	public SVGTagLine(String tag, SVGElement parent) {
			// "line"
    		super(SvgDecoder.TAG_SVG_LINE, parent);
		}

    	@Override
    	public void init() {
    		Float widthcanvas = (float)getWidth();
    		Float heightcanvas = (float)getHeight();
    		this.x1 = getFloatAttr("x1", widthcanvas);
    		this.x2 = getFloatAttr("x2", widthcanvas);
    		this.y1 = getFloatAttr("y1", heightcanvas);
    		this.y2 = getFloatAttr("y2", heightcanvas);
			super.init();
		}

		@Override
    	public void drawData(Canvas c) {
			if(this.x1 != null && this.x2 != null && this.y1 != null && this.y2 != null) {
				if(this.paintStroke != null) {
					c.drawLine(this.x1, this.y1, this.x2, this.y2, this.paintStroke);
				}
			}
    	}
    }

    public class SVGTagPolyline extends SVGFigure{

    	private Path path = null;

    	public SVGTagPolyline(String tag, SVGElement parent) {
			// "polyline"
    		super(SvgDecoder.TAG_SVG_POLYLINE, parent);
		}

    	@Override
    	public void init() {
            NumberParse numbers = null;
            String pointstr = getAttr("points");
            if(pointstr != null) {
            	numbers = parseNumbers("points");
            }
            if (numbers != null) {
                Path p = new Path();
                ArrayList<Float> points = numbers.numbers;
                if (points.size() > 1) {
                    p.moveTo(points.get(0), points.get(1));
                    for (int i = 2; i < points.size(); i += 2) {
                        float x = points.get(i);
                        float y = points.get(i + 1);
                        p.lineTo(x, y);
                    }
                    this.path = p;
                }
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

    
    public class SVGTagPolygon extends SVGFigure{

    	private Path path = null;

    	public SVGTagPolygon(String tag, SVGElement parent) {
			// "polygon"
    		super(SvgDecoder.TAG_SVG_POLYGON, parent);
		}

    	@Override
    	public void init() {
            NumberParse numbers = null;
            String pointstr = getAttr("points");
            if(pointstr != null) {
            	numbers = parseNumbers("points");
            }
            if (numbers != null) {
                Path p = new Path();
                ArrayList<Float> points = numbers.numbers;
                if (points.size() > 1) {
                    p.moveTo(points.get(0), points.get(1));
                    for (int i = 2; i < points.size(); i += 2) {
                        float x = points.get(i);
                        float y = points.get(i + 1);
                        p.lineTo(x, y);
                    }
                    p.close();
                    this.path = p;
                }
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

    
    public class SVGTagCircle extends SVGFigure{

    	private Float centerX = null;
    	private Float centerY = null;
    	private Float radius = null;

    	public SVGTagCircle(String tag, SVGElement parent) {
			// "circle"
    		super(SvgDecoder.TAG_SVG_CIRCLE, parent);
		}

    	@Override
    	public void init() {
    		Float widthcanvas = (float)getWidth();
    		Float heightcanvas = (float)getHeight();
    		this.centerX = getFloatAttr("cx", widthcanvas);
    		this.centerY = getFloatAttr("cy", heightcanvas);
    		this.radius = getFloatAttr("r", widthcanvas);
			super.init();
		}

		@Override
    	public void drawData(Canvas c) {
			if(this.radius != 0f) {
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

    	public SVGTagEllipse(String tag, SVGElement parent) {
			// "ellipse"
    		super(SvgDecoder.TAG_SVG_ELLIPSE, parent);
		}

    	@Override
    	public void init() {
    		Float widthcanvas = (float)getWidth();
    		Float heightcanvas = (float)getHeight();
    		Float centerX = getFloatAttr("cx", widthcanvas);
    		Float centerY = getFloatAttr("cy", heightcanvas);
    		Float radiusX = getFloatAttr("rx", widthcanvas);
    		Float radiusY = getFloatAttr("ry", heightcanvas);
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
    	private RectF bounds = null;

    	public SVGTagRect(String tag, SVGElement parent) {
			// "rect"
    		super(SvgDecoder.TAG_SVG_RECT, parent);
		}
    	
    	public RectF getBounds() {
    		return this.bounds;
    	}

    	@Override
    	public void init() {
    		Float widthcanvas = (float)getWidth();
    		Float heightcanvas = (float)getHeight();
            this.x = getFloatAttr("x", widthcanvas);
    		this.y = getFloatAttr("y", heightcanvas);
    		this.width = getFloatAttr("width", widthcanvas);
    		this.height = getFloatAttr("height", heightcanvas);
    		this.bounds = new RectF(this.x, this.y, this.x + this.width, this.y + this.width);
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

    	public SVGTagPath(String tag, SVGElement parent) {
			// "path"
    		super(SvgDecoder.TAG_SVG_PATH, parent);
		}

    	@Override
    	public void init() {
			final String d = getAttr("d");
			if(d != null) {
				this.path = parsePath(d);
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

		public SVGFigure(String tag, SVGElement parent) {
			super(tag, parent);
		}

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
			paint.setAlpha(255);
            
            Shader shader = null;
            String fillString = getStyleAttr("fill");
            if (fillString != null && fillString.startsWith("url(#")) {
                String id = fillString.substring("url(#".length(), fillString.length() - 1);
                SVGElement root = getRoot();
                if(root instanceof SVG) {
                    shader = ((SVG)root).getGradientById(id);
                }
            }
            paint.setShader(shader);
            paint.setStyle(Paint.Style.FILL);
            if (fillString != null && fillString.startsWith("#")) {
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
			paint.setAlpha(255);
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

    public class SVGTagLinearGradient extends SVGGradient{
    	
		public SVGTagLinearGradient(String tag, SVGElement parent) {
			// "linearGradient"
			super(SvgDecoder.TAG_SVG_LINEAR_GRADIENT, parent);
		}

		@Override
    	public void init() {
			super.init();
			
    		Float widthcanvas = (float)getWidth();
    		Float heightcanvas = (float)getHeight();
    		
            Gradient gradient = new Gradient();
            gradient.id = getAttr("id");
            gradient.isLinear = true;
            gradient.x1 = getFloatAttr("x1", widthcanvas);
            gradient.x2 = getFloatAttr("x2", widthcanvas);
            gradient.y1 = getFloatAttr("y1", heightcanvas);
            gradient.y2 = getFloatAttr("y2", heightcanvas);
            String transform = getAttr("gradientTransform");
            if (transform != null) {
                gradient.matrix = parseTransform(transform);
            }
            String xlink = getAttr("href");
            if (xlink != null) {
                if (xlink.startsWith("#")) {
                    xlink = xlink.substring(1);
                }
                gradient.xlink = xlink;
            }

			int size = getElementsSize();
    		for(int i = 0; i < size; i++) {
    			SVGElement e = getElement(i);
    			if(e instanceof SVGTagStop) {
    				float offset = ((SVGTagStop)e).getOffset();
    				int color = ((SVGTagStop)e).getColor();
                    gradient.positions.add(offset);
                    gradient.colors.add(color);
    			}
    		}

            this.gradientRef = gradient;
            
            if (gradient.id != null) {
                if (gradient.xlink != null) {
                	Gradient parent = null;
                    SVGElement root = getRoot();
                    if(root instanceof SVG) {
                    	parent = ((SVG)root).getGradientRefById(gradient.xlink);
                    }
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
                	Gradient parent = null;
                    SVGElement root = getRoot();
                    if(root instanceof SVG) {
                    	parent = ((SVG)root).getGradientRefById(gradient.xlink);
                    }
                    if (parent != null) {
                        gradient = parent.createChild(gradient);
                    }
                }
                LinearGradient g = new LinearGradient(gradient.x1, gradient.y1, gradient.x2, gradient.y2, colors, positions, Shader.TileMode.CLAMP);
                if (gradient.matrix != null) {
                    g.setLocalMatrix(gradient.matrix);
                }
                this.gradientAnd = g;
            }
		}
    }

    public class SVGTagRadialGradient extends SVGGradient{
    	
		public SVGTagRadialGradient(String tag, SVGElement parent) {
			// "radialGradient"
			super(SvgDecoder.TAG_SVG_RADIAL_GRADIENT, parent);
		}

		@Override
    	public void init() {
			super.init();

			Float widthcanvas = (float)getWidth();
    		Float heightcanvas = (float)getHeight();

            Gradient gradient = new Gradient();
            gradient.id = getAttr("id");
            gradient.isLinear = false;
   			gradient.x = getFloatAttr("cx", widthcanvas);
   			gradient.y = getFloatAttr("cy", heightcanvas);
   			gradient.radius = getFloatAttr("r", widthcanvas);
   			
   			if(gradient.radius == 0f) {
   				return;
   			}
   			
            String transform = getAttr("gradientTransform");
            if (transform != null) {
                gradient.matrix = parseTransform(transform);
            }
            String xlink = getAttr("href");
            if (xlink != null) {
                if (xlink.startsWith("#")) {
                    xlink = xlink.substring(1);
                }
                gradient.xlink = xlink;
            }
            
			int size = getElementsSize();
    		for(int i = 0; i < size; i++) {
    			SVGElement e = getElement(i);
    			if(e instanceof SVGTagStop) {
    				float offset = ((SVGTagStop)e).getOffset();
    				int color = ((SVGTagStop)e).getColor();
                    gradient.positions.add(offset);
                    gradient.colors.add(color);
    			}
    		}
            
            this.gradientRef = gradient;
            
            if (gradient.id != null) {
                if (gradient.xlink != null) {
                	Gradient parent = null;
                    SVGElement root = getRoot();
                    if(root instanceof SVG) {
                    	parent = ((SVG)root).getGradientRefById(gradient.xlink);
                    }
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
                	Gradient parent = null;
                    SVGElement root = getRoot();
                    if(root instanceof SVG) {
                    	parent = ((SVG)root).getGradientRefById(gradient.xlink);
                    }
                    if (parent != null) {
                        gradient = parent.createChild(gradient);
                    }
                }
                RadialGradient g = new RadialGradient(gradient.x, gradient.y, gradient.radius, colors, positions, Shader.TileMode.CLAMP);
                if (gradient.matrix != null) {
                    g.setLocalMatrix(gradient.matrix);
                }
                this.gradientAnd = g;
            }
		}
    }

    
    public class SVGGradient extends SVGElement{
    	protected Gradient gradientRef = null;
    	protected Shader gradientAnd = null;
    	
		public SVGGradient(String tag, SVGElement parent) {
			super(tag, parent);
		}
		
		public Shader getGradient(String id) {
			if(this.gradientRef != null && this.gradientRef.id != null && this.gradientRef.id.equals(id)) {
				return this.gradientAnd;
			} 
			return null;
		}
		
		public Gradient getGradientRef(String id) {
			if(this.gradientRef != null && this.gradientRef.id != null && this.gradientRef.id.equals(id)) {
				return this.gradientRef;
			} 
			return null;
		}
    }

    public class SVGTagAnimate extends SVGElement{

    	public SVGTagAnimate(String tag, SVGElement parent) {
			// "animate"
			super(SvgDecoder.TAG_SVG_ANIMATE, parent);
		}

		@Override
    	public void init() {
			String attributeName = getAttr("attributeName");
			String from = getAttr("from");
			String to = getAttr("to");
			String dur = getAttr("dur");
			String repeatCount = getAttr("repeatCount");
			super.init();
		}
    }

    
    public class SVGTagStop extends SVGElement{
    	private float offset = 0f;
    	private int color = 0;

    	public SVGTagStop(String tag, SVGElement parent) {
			// "stop"
			super(SvgDecoder.TAG_SVG_STOP, parent);
		}
    	
    	public float getOffset() {
    		return this.offset;
    	}
    	
    	public int getColor() {
    		return this.color;
    	}

		@Override
    	public void init() {
            this.offset = getFloatAttr("offset", 0f);
            String styles = getAttr("style");
            this.color = 0;
            if(styles!=null) {
                StyleSet styleSet = new StyleSet(styles);
                String colorStyle = styleSet.getStyle("stop-color");
                this.color = SvgDecoder.getColor(colorStyle);
                String opacityStyle = styleSet.getStyle("stop-opacity");
                if (opacityStyle != null) {
                    float alpha = Float.parseFloat(opacityStyle);
                    int alphaInt = Math.round(255 * alpha);
                    this.color |= (alphaInt << 24);
                } else {
                	this.color |= 0xFF000000;
                }
            }
			super.init();
		}
    }
    
    public class SVGTagG extends SVGElement{
    	private boolean display = true;
    	private boolean boundsMode = true;
    	
		public SVGTagG(String tag, SVGElement parent) {
			// "g"
			super(SvgDecoder.TAG_SVG_G, parent);
		}
		
		public boolean inBoundsMode() {
			return this.boundsMode;
		}

		@Override
    	public void init() {
            if ("none".equals(getAttr("display"))) {
            	this.display = false;
            }
            
            if ("bounds".equalsIgnoreCase(getAttr("id"))) {
                boundsMode = true;
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
    	
    	private int width = 0;
    	private int height = 0;
    	
		public SVG(String tag, SVGElement parent) {
			super(SvgDecoder.TAG_SVG, parent);
		}
		
		public Shader getGradientById(String id) {
			SVGElement ret = searchGradientById(id, this);
			if(ret != null) {
    			if(ret instanceof SVGGradient) {
    				return ((SVGGradient)ret).getGradient(id);
    			}
			}
			return null;
		}
		
		public Gradient getGradientRefById(String id) {
			SVGElement ret = searchGradientById(id, this);
			if(ret != null) {
    			if(ret instanceof SVGGradient) {
    				return ((SVGGradient)ret).getGradientRef(id);
    			}
			}
			return null;
		}
		
		private SVGElement searchGradientById(String id, SVGElement element) {
			int size = element.getElementsSize();
    		for(int i = 0; i < size; i++) {
    			SVGElement e = element.getElement(i);
    			if(e instanceof SVGGradient) {
    				if(((SVGGradient)e).getGradientRef(id) != null) {
    					return e;
    				}
    			}
    			SVGElement ret = searchGradientById(id, e);
    			if(ret != null) {
    				return ret;
    			}
    		}
    		return null;
		}
		
		public int getWidth() {
			return this.width;
		}
		
		public int getHeight() {
			return this.height;
		}
		
		public int getDelay() {
			SVGElement animation = searchAnimation(this);
			if(animation != null) {
				return 300;
			}
			return Integer.MAX_VALUE;
		}

		private SVGElement searchAnimation(SVGElement element) {
			int size = element.getElementsSize();
    		for(int i = 0; i < size; i++) {
    			SVGElement e = element.getElement(i);
    			if(e instanceof SVGGradient) {
   					return e;
    			}
    			SVGElement ret = searchAnimation(e);
    			if(ret != null) {
    				return ret;
    			}
    		}
    		return null;
		}

		@Override
    	public void init() {
			Float widthf = getFloatAttr("width", 0f);
			Float heightf = getFloatAttr("height", 0f);
			if(widthf != null && heightf != null) {
            	this.width = (int) Math.ceil((double)widthf);
            	this.height = (int) Math.ceil((double)heightf);
			}
			
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
    	private SVGElement parent = null;
    	private String data = null;
    	private String name = null;
    	
		public SVGElement(String tag, SVGElement parent) {
			this.name = tag;
			this.parent = parent;
			if(this.parent != null) {
				this.parent.addElement(this);
			}
		}
		
		public SVGElement getRoot() {
			if(this.parent != null) {
				return this.parent.getRoot();
			} else {
				return this;
			}
		}

		public SVGElement getParent() {
			return this.parent;
		}
    	
    	public String getAttr(String attr) {
    		return this.attrs.get(attr);
    	}
    	
    	public Float getFloatAttr(String attr, Float bound) {
    		String str = getAttr(attr);
    		if(str != null) {
    			return SvgDecoder.attrToFloat(str, bound);
    		} 
    		return 0f;
    	}
    	
    	
    	public void setAttr(String attr, String value) {
    		this.attrs.put(attr, value);
    	}
    	
    	public void addElement(SVGElement element) {
    		this.elements.add(element);
    	}
    	
    	public int getElementsSize() {
    		return this.elements.size();
    	}
    	
    	public SVGElement getElement(int i) {
    		return this.elements.get(i);
    	}
    	
    	public void setData(String data) {
    		this.data = data;
    	}
    	
    	public String getData() {
    		return this.data;
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
    
    private class NumberParse {
        private ArrayList<Float> numbers;

        public NumberParse(ArrayList<Float> numbers) {
            this.numbers = numbers;
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
}
