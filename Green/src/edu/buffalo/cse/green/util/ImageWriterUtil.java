/* This file is part of Green.
 *
 * Copyright (C) 2005 The Research Foundation of State University of New York
 * All Rights Under Copyright Reserved, The Research Foundation of S.U.N.Y.
 * 
 * Green is free software, licensed under the terms of the Eclipse
 * Public License, version 1.0.  The license is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package edu.buffalo.cse.green.util;

import static edu.buffalo.cse.green.preferences.PreferenceInitializer.P_COLOR_COMPARTMENT_BORDER;
import static edu.buffalo.cse.green.preferences.PreferenceInitializer.P_COLOR_NOTE;
import static edu.buffalo.cse.green.preferences.PreferenceInitializer.P_COLOR_NOTE_BORDER;
import static edu.buffalo.cse.green.preferences.PreferenceInitializer.P_COLOR_NOTE_TEXT;
import static edu.buffalo.cse.green.preferences.PreferenceInitializer.P_COLOR_REL_ARROW_FILL;
import static edu.buffalo.cse.green.preferences.PreferenceInitializer.P_COLOR_REL_LINE;
import static edu.buffalo.cse.green.preferences.PreferenceInitializer.P_COLOR_REL_TEXT;
import static edu.buffalo.cse.green.preferences.PreferenceInitializer.P_COLOR_TYPE_BORDER;
import static edu.buffalo.cse.green.preferences.PreferenceInitializer.P_COLOR_TYPE_BORDER_HIDDENR;
import static edu.buffalo.cse.green.preferences.PreferenceInitializer.P_COLOR_TYPE_TEXT;
import static edu.buffalo.cse.green.preferences.PreferenceInitializer.P_COLOR_UML;

import java.awt.image.BufferedImage;
import java.awt.image.ColorConvertOp;
import java.awt.image.ColorModel;
import java.awt.image.DirectColorModel;
import java.awt.image.IndexColorModel;
import java.awt.image.WritableRaster;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.SWTGraphics;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Drawable;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;

import edu.buffalo.cse.green.PlugIn;
/**
 * Converts from images (Draw2D) to picture file formats.
 * 
 * @author evertwoo
 */
public class ImageWriterUtil {
	public static final int FORMAT_BMP = 0;
	public static final int FORMAT_GIF = 2;
	public static final int FORMAT_JPG = 4;
	public static final int FORMAT_PNG = 5;
	public static final int FORMAT_TIF = 6;
	private static RGB[] GRAYSCALE_COLORS;
	private static RGB[] GREEN_COLORS;
	private static final int GREEN_COLOR_BACKGROUND;

	static {
		GRAYSCALE_COLORS = new RGB[256];
		for (int c = 0; c < GRAYSCALE_COLORS.length; ++c) {
			GRAYSCALE_COLORS[c] = new RGB(c, c, c);
		}

		// set up the palette to contain at least the UML colors
		Set<RGB> basicColorSet = new HashSet<RGB>();
		basicColorSet.add(getRGB(ColorConstants.listBackground));
		basicColorSet.add(getRGB(PlugIn.getColorPreference(P_COLOR_UML)));
		basicColorSet.add(getRGB(PlugIn.getColorPreference(P_COLOR_TYPE_BORDER)));
		basicColorSet.add(getRGB(PlugIn.getColorPreference(P_COLOR_TYPE_BORDER_HIDDENR)));
		basicColorSet.add(getRGB(PlugIn.getColorPreference(P_COLOR_TYPE_TEXT)));
		basicColorSet.add(getRGB(PlugIn.getColorPreference(P_COLOR_COMPARTMENT_BORDER)));
		basicColorSet.add(getRGB(PlugIn.getColorPreference(P_COLOR_NOTE)));
		basicColorSet.add(getRGB(PlugIn.getColorPreference(P_COLOR_NOTE_BORDER)));
		basicColorSet.add(getRGB(PlugIn.getColorPreference(P_COLOR_NOTE_TEXT)));
		basicColorSet.add(getRGB(PlugIn.getColorPreference(P_COLOR_REL_ARROW_FILL)));
		basicColorSet.add(getRGB(PlugIn.getColorPreference(P_COLOR_REL_LINE)));
		basicColorSet.add(getRGB(PlugIn.getColorPreference(P_COLOR_REL_TEXT)));

		GREEN_COLORS = new RGB[256];
		basicColorSet.toArray(GREEN_COLORS);

		// Make the rest of the colors greyscale
		int startIndexBase = basicColorSet.size();
		for (int c = startIndexBase; c < GREEN_COLORS.length; ++c) {
			int value = (c - startIndexBase) * GREEN_COLORS.length
					/ (GREEN_COLORS.length - startIndexBase);
			GREEN_COLORS[c] = new RGB(value, value, value);
		}
		
		GREEN_COLOR_BACKGROUND = ImageWriterUtil.getPaletteIndex(255, 255, 255,
				GREEN_COLORS);
	}

	private ImageWriterUtil() {}

	/**
	 * Writes a figure to a device.
	 * 
	 * @param figure - The figure
	 * @param dest - The destination to draw to.
	 * @param format - The format to draw in.
	 * @param backgroundColor - The background color.
	 */
	public static void writeFigureToDeviceContext(
			IFigure figure,
			Drawable dest,
			int format,
			RGB backgroundColor) {
		// Derived from org.eclipse.draw2d.BufferedGrapicsSource
		SWTGraphics destGraphics = new SWTGraphics(new GC(dest));
		figure.setBackgroundColor(new Color(null, backgroundColor));
		destGraphics.translate(figure.getBounds().getTopLeft().negate());
		figure.paint(destGraphics);
	}

	/**
	 * Loads a figure.
	 * 
	 * @param figure - The figure.
	 * @param imageIO - The loader.
	 * @param format - The format.
	 */
	public static void writeFigureToLoader(
			IFigure figure,
			ImageLoader imageIO,
			int format) {
		writeFigureToLoader(figure, imageIO, format,
				GREEN_COLORS[GREEN_COLOR_BACKGROUND]);
	}

	/**
	 * Loads a figure.
	 * 
	 * @param figure - The figure.
	 * @param imageIO - The loader.
	 * @param format - The format.
	 * @param backgroundColor - The background color.
	 */
	public static void writeFigureToLoader(
			IFigure figure,
			ImageLoader imageIO,
			int format,
			RGB backgroundColor) {
		Rectangle figureBounds = figure.getBounds();
		imageIO.logicalScreenWidth = figureBounds.width;
		imageIO.logicalScreenHeight = figureBounds.height;

		Image destImage = new Image(null, figureBounds.width,
				figureBounds.height);

		writeFigureToDeviceContext(figure, destImage, format, backgroundColor);

		if (format == ImageWriterUtil.FORMAT_GIF) { // GIF only supports 8 bits
			// per pixel
			ImageData destImageData = ImageWriterUtil
					.getImageDataInEightBitColor(destImage.getImageData());
			imageIO.data = new ImageData[] {
				destImageData };
			imageIO.backgroundPixel = GREEN_COLOR_BACKGROUND;
		} else {
			imageIO.data = new ImageData[] {
				destImage.getImageData() };
		}
		imageIO.repeatCount = 0;

	}

	/**
	 * Loads an image.
	 * 
	 * @param image - The image.
	 * @param imageIO - The loader.
	 * @param format - The format.
	 */
	public static void writeImageToLoader(
			Image image,
			ImageLoader imageIO,
			int format) {
		writeImageToLoader(image, imageIO, format, -1);
	}

	/**
	 * Loads an image.
	 * 
	 * @param image - The image.
	 * @param imageIO - The loader.
	 * @param format - The format.
	 * @param backgroundColorIndex - The background color's index in the
	 * palette.
	 */
	public static void writeImageToLoader(
			Image image,
			ImageLoader imageIO,
			int format,
			int backgroundColorIndex) {
		org.eclipse.swt.graphics.Rectangle imageBounds = image.getBounds();
		imageIO.logicalScreenWidth = imageBounds.width;
		imageIO.logicalScreenHeight = imageBounds.height;

		if (format == ImageWriterUtil.FORMAT_GIF) { // GIF only supports 8 bits
			// per pixel
			ImageData imageData = ImageWriterUtil
					.getImageDataInEightBitColor(image.getImageData());
			imageIO.data = new ImageData[] {
				imageData };
			imageIO.backgroundPixel = backgroundColorIndex;
		} else {
			imageIO.data = new ImageData[] {
				image.getImageData() };
		}
		imageIO.repeatCount = 0;
	}

	/**
	 * Converts an image to 8-bit color.
	 * 
	 * @param destImageData - The image data.
	 * @return The converted image.
	 */
	protected static ImageData getImageDataInEightBitColor(
			ImageData destImageData) {
		if(System.getProperty("os.name").equals("Linux")){
			Image origImage = new Image(Display.getDefault (), destImageData);
			ImageData myd=origImage.getImageData();
			BufferedImage ga=convertToAWT(myd);
			BufferedImage gn=convert8(ga);
			ImageData output=convertToSWT(gn);	
			return output;
		}else{
		ImageData destImageDataGrayscale = new ImageData(destImageData.width,
				destImageData.height, 8, new PaletteData(GREEN_COLORS));

		Image origImage = new Image(Display.getDefault (), destImageData);
		Image gifImage = new Image(Display.getDefault (), destImageDataGrayscale);
		GC gifGC = new GC (gifImage);
		gifGC.drawImage(origImage, 0, 0);
		gifGC.dispose();		
		
		return gifImage.getImageData();
		}
	}
	
	public static BufferedImage convert8(BufferedImage src) { //image4j convert8
	    BufferedImage dest = new BufferedImage(
	        src.getWidth(), src.getHeight(),
	        BufferedImage.TYPE_BYTE_INDEXED
	        );
	    ColorConvertOp cco = new ColorConvertOp(
	        src.getColorModel().getColorSpace(),
	        dest.getColorModel().getColorSpace(),
	        null
	        );
	    cco.filter(src, dest);
	    return dest;
	}
	static BufferedImage convertToAWT(ImageData data) {
		ColorModel colorModel = null;
		PaletteData palette = data.palette;
		if (palette.isDirect) {
			colorModel = new DirectColorModel(data.depth, palette.redMask, palette.greenMask, palette.blueMask);
			BufferedImage bufferedImage = new BufferedImage(colorModel, colorModel.createCompatibleWritableRaster(data.width, data.height), false, null);
			for (int y = 0; y < data.height; y++) {
				for (int x = 0; x < data.width; x++) {
					int pixel = data.getPixel(x, y);
					RGB rgb = palette.getRGB(pixel);
					bufferedImage.setRGB(x, y,  rgb.red << 16 | rgb.green << 8 | rgb.blue);
				}
			}
			return bufferedImage;
		} else {
			RGB[] rgbs = palette.getRGBs();
			byte[] red = new byte[rgbs.length];
			byte[] green = new byte[rgbs.length];
			byte[] blue = new byte[rgbs.length];
			for (int i = 0; i < rgbs.length; i++) {
				RGB rgb = rgbs[i];
				red[i] = (byte)rgb.red;
				green[i] = (byte)rgb.green;
				blue[i] = (byte)rgb.blue;
			}
			if (data.transparentPixel != -1) {
				colorModel = new IndexColorModel(data.depth, rgbs.length, red, green, blue, data.transparentPixel);
			} else {
				colorModel = new IndexColorModel(data.depth, rgbs.length, red, green, blue);
			}
			BufferedImage bufferedImage = new BufferedImage(colorModel, colorModel.createCompatibleWritableRaster(data.width, data.height), false, null);
			WritableRaster raster = bufferedImage.getRaster();
			int[] pixelArray = new int[1];
			for (int y = 0; y < data.height; y++) {
				for (int x = 0; x < data.width; x++) {
					int pixel = data.getPixel(x, y);
					pixelArray[0] = pixel;
					raster.setPixel(x, y, pixelArray);
				}
			}
			return bufferedImage;
		}
	}

    /**
     * Converts a buffered image to SWT <code>ImageData</code>.
     *
     * @param bufferedImage  the buffered image (<code>null</code> not
     *         permitted).
     *
     * @return The image data.
     */
    public static ImageData convertToSWT(BufferedImage bufferedImage) {
        if (bufferedImage.getColorModel() instanceof DirectColorModel) {
            DirectColorModel colorModel
                    = (DirectColorModel) bufferedImage.getColorModel();
            PaletteData palette = new PaletteData(colorModel.getRedMask(),
                    colorModel.getGreenMask(), colorModel.getBlueMask());
            ImageData data = new ImageData(bufferedImage.getWidth(),
                    bufferedImage.getHeight(), colorModel.getPixelSize(),
                    palette);
            WritableRaster raster = bufferedImage.getRaster();
            int[] pixelArray = new int[3];
            for (int y = 0; y < data.height; y++) {
                for (int x = 0; x < data.width; x++) {
                    raster.getPixel(x, y, pixelArray);
                    int pixel = palette.getPixel(new RGB(pixelArray[0],
                            pixelArray[1], pixelArray[2]));
                    data.setPixel(x, y, pixel);
                }
            }
            return data;
        }
        else if (bufferedImage.getColorModel() instanceof IndexColorModel) {
            IndexColorModel colorModel = (IndexColorModel)
                    bufferedImage.getColorModel();
            int size = colorModel.getMapSize();
            byte[] reds = new byte[size];
            byte[] greens = new byte[size];
            byte[] blues = new byte[size];
            colorModel.getReds(reds);
            colorModel.getGreens(greens);
            colorModel.getBlues(blues);
            RGB[] rgbs = new RGB[size];
            for (int i = 0; i < rgbs.length; i++) {
                rgbs[i] = new RGB(reds[i] & 0xFF, greens[i] & 0xFF,
                        blues[i] & 0xFF);
            }
            PaletteData palette = new PaletteData(rgbs);
            ImageData data = new ImageData(bufferedImage.getWidth(),
                    bufferedImage.getHeight(), colorModel.getPixelSize(),
                    palette);
            data.transparentPixel = colorModel.getTransparentPixel();
            WritableRaster raster = bufferedImage.getRaster();
            int[] pixelArray = new int[1];
            for (int y = 0; y < data.height; y++) {
                for (int x = 0; x < data.width; x++) {
                    raster.getPixel(x, y, pixelArray);
                    data.setPixel(x, y, pixelArray[0]);
                }
            }
            return data;
        }
        return null;
    }


	/**
	 * @param color - The color.
	 * @param colorPalette - The palette.
	 * @return The index of the color in the palette.
	 */
	public static int getPaletteIndex(int color, RGB[] colorPalette) {
		return getPaletteIndex((color & 0xFF), ((color >> 8) & 0xFF),
				((color >> 16) & 0xFF), colorPalette);
	}

	/**
	 * @param r - The red value of the color.
	 * @param g - The green value of the color.
	 * @param b - The blue value of the color.
	 * @param colorPalette - The palette.
	 * @return The index of the color in the palette.
	 */
	public static int getPaletteIndex(int r, int g, int b, RGB[] colorPalette) {
		int minDifference = 256 * 3; // some huge difference
		int indexMinDiff = -1;
		for (int c = 0; c < colorPalette.length; c++) {
			int difference = Math.abs(colorPalette[c].red - r)
					+ Math.abs(colorPalette[c].green - g)
					+ Math.abs(colorPalette[c].blue - b);
			if (difference < minDifference) {
				indexMinDiff = c;
				minDifference = difference;
				if (difference == 0) {
					break;
				}
			}
		}
		return indexMinDiff;
	}

	/**
	 * @param color - The color.
	 * @return The <code>RGB</code> representation of the given
	 * <code>Color</code>.
	 */
	private static RGB getRGB(Color color) {
		return new RGB(color.getRed(), color.getGreen(), color.getBlue());
	}
}
