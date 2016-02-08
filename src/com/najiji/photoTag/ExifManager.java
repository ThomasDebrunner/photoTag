package com.najiji.photoTag;

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.imaging.ImageReadException;
import org.apache.commons.imaging.ImageWriteException;
import org.apache.commons.imaging.Imaging;
import org.apache.commons.imaging.common.ImageMetadata;
import org.apache.commons.imaging.common.RationalNumber;
import org.apache.commons.imaging.formats.jpeg.JpegImageMetadata;
import org.apache.commons.imaging.formats.jpeg.exif.ExifRewriter;
import org.apache.commons.imaging.formats.tiff.TiffField;
import org.apache.commons.imaging.formats.tiff.TiffImageMetadata;
import org.apache.commons.imaging.formats.tiff.constants.ExifTagConstants;
import org.apache.commons.imaging.formats.tiff.constants.GpsTagConstants;
import org.apache.commons.imaging.formats.tiff.constants.TiffTagConstants;
import org.apache.commons.imaging.formats.tiff.taginfos.TagInfo;
import org.apache.commons.imaging.formats.tiff.write.TiffOutputDirectory;
import org.apache.commons.imaging.formats.tiff.write.TiffOutputSet;
import org.apache.commons.imaging.util.IoUtils;

public class ExifManager {
   
	private static SimpleDateFormat exifFormat = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss");
	
	
    /**
     * This example illustrates how to add/update EXIF metadata in a JPEG file.
     * 
     * @param jpegImageFile
     *            A source image file.
     * @param dst
     *            The output file.
     * @throws IOException
     * @throws ImageReadException
     * @throws ImageWriteException
     */
    public static void changeExifDate(final File jpegImageFile, Date date)
            throws IOException, ImageReadException, ImageWriteException {
        OutputStream os = null;
        boolean canThrow = false;
        try {
            TiffOutputSet outputSet = null;

            // note that metadata might be null if no metadata is found.
            final ImageMetadata metadata = Imaging.getMetadata(jpegImageFile);
            final JpegImageMetadata jpegMetadata = (JpegImageMetadata) metadata;
            if (null != jpegMetadata) {
                // note that exif might be null if no Exif metadata is found.
                final TiffImageMetadata exif = jpegMetadata.getExif();

                if (exif != null) {
                    // TiffImageMetadata class is immutable (read-only).
                    // TiffOutputSet class represents the Exif data to write.
                    //
                    // Usually, we want to update existing Exif metadata by
                    // changing
                    // the values of a few fields, or adding a field.
                    // In these cases, it is easiest to use getOutputSet() to
                    // start with a "copy" of the fields read from the image.
                    outputSet = exif.getOutputSet();
                }
            }

            // if file does not contain any exif metadata, we create an empty
            // set of exif metadata. Otherwise, we keep all of the other
            // existing tags.
            if (outputSet == null) {
                outputSet = new TiffOutputSet();
            }

            {
                // Example of how to add a field/tag to the output set.
                //
                // Note that you should first remove the field/tag if it already
                // exists in this directory, or you may end up with duplicate
                // tags. See above.
                //
                // Certain fields/tags are expected in certain Exif directories;
                // Others can occur in more than one directory (and often have a
                // different meaning in different directories).
                //
                // TagInfo constants often contain a description of what
                // directories are associated with a given tag.
                //
                final TiffOutputDirectory exifDirectory = outputSet
                        .getOrCreateExifDirectory();
                // make sure to remove old value if present (this method will
                // not fail if the tag does not exist).
                exifDirectory.removeField(TiffTagConstants.TIFF_TAG_DATE_TIME);
                exifDirectory.add(TiffTagConstants.TIFF_TAG_DATE_TIME, exifFormat.format(date));
                exifDirectory.removeField(ExifTagConstants.EXIF_TAG_DATE_TIME_ORIGINAL);
                exifDirectory.add(ExifTagConstants.EXIF_TAG_DATE_TIME_ORIGINAL, exifFormat.format(date));
                exifDirectory.removeField(ExifTagConstants.EXIF_TAG_DATE_TIME_DIGITIZED);
                exifDirectory.add(ExifTagConstants.EXIF_TAG_DATE_TIME_DIGITIZED, exifFormat.format(date));
            }

            {
                // Example of how to add/update GPS info to output set.

                // New York City
                final double longitude = -74.0; // 74 degrees W (in Degrees East)
                final double latitude = 40 + 43 / 60.0; // 40 degrees N (in Degrees
                // North)

                outputSet.setGPSInDegrees(longitude, latitude);
            }

            // printTagValue(jpegMetadata, TiffConstants.TIFF_TAG_DATE_TIME);

            os = new FileOutputStream(jpegImageFile);
            os = new BufferedOutputStream(os);

            new ExifRewriter().updateExifMetadataLossless(jpegImageFile, os,
                    outputSet);

            canThrow = true;
        } finally {
            IoUtils.closeQuietly(canThrow, os);
        }
    }
    
    public static Date getExifDate(final File file) throws ImageReadException,
    IOException {
		// get all metadata stored in EXIF format (ie. from JPEG or TIFF).
		final ImageMetadata metadata = Imaging.getMetadata(file);
		
		// System.out.println(metadata);
		
		if (metadata instanceof JpegImageMetadata) {
		    final JpegImageMetadata jpegMetadata = (JpegImageMetadata) metadata;
		
		    // print out various interesting EXIF tags.
		   final TiffField field = jpegMetadata.findEXIFValueWithExactMatch(TiffTagConstants.TIFF_TAG_DATE_TIME);
		   
		   if(field == null) {
			   return null;
		   }
		   else {
			   String stringDate = field.getStringValue();
			   try {
				   return exifFormat.parse(stringDate);
			} catch (ParseException e) {
				e.printStackTrace();
			}
		   }
		}
		return null;
	}
    
    
    
    
    
    
    public static void printExifTags(final File file) throws ImageReadException,
    IOException {
		// get all metadata stored in EXIF format (ie. from JPEG or TIFF).
		final ImageMetadata metadata = Imaging.getMetadata(file);
		
		// System.out.println(metadata);
		
		if (metadata instanceof JpegImageMetadata) {
		    final JpegImageMetadata jpegMetadata = (JpegImageMetadata) metadata;
		
		    // Jpeg EXIF metadata is stored in a TIFF-based directory structure
		    // and is identified with TIFF tags.
		    // Here we look for the "x resolution" tag, but
		    // we could just as easily search for any other tag.
		    //
		    // see the TiffConstants file for a list of TIFF tags.
		
		    System.out.println("file: " + file.getPath());
		
		    // print out various interesting EXIF tags.
		    printTagValue(jpegMetadata, TiffTagConstants.TIFF_TAG_XRESOLUTION);
		    printTagValue(jpegMetadata, TiffTagConstants.TIFF_TAG_DATE_TIME);
		    printTagValue(jpegMetadata, ExifTagConstants.EXIF_TAG_DATE_TIME_ORIGINAL);
		    printTagValue(jpegMetadata, ExifTagConstants.EXIF_TAG_DATE_TIME_DIGITIZED);
		    printTagValue(jpegMetadata, ExifTagConstants.EXIF_TAG_ISO);
		    printTagValue(jpegMetadata, ExifTagConstants.EXIF_TAG_SHUTTER_SPEED_VALUE);
		    printTagValue(jpegMetadata, ExifTagConstants.EXIF_TAG_APERTURE_VALUE);
		    printTagValue(jpegMetadata, ExifTagConstants.EXIF_TAG_BRIGHTNESS_VALUE);
		    printTagValue(jpegMetadata, GpsTagConstants.GPS_TAG_GPS_LATITUDE_REF);
		    printTagValue(jpegMetadata, GpsTagConstants.GPS_TAG_GPS_LATITUDE);
		    printTagValue(jpegMetadata, GpsTagConstants.GPS_TAG_GPS_LONGITUDE_REF);
		    printTagValue(jpegMetadata, GpsTagConstants.GPS_TAG_GPS_LONGITUDE);
		    System.out.println();
		
		}
	}
		
	private static void printTagValue(final JpegImageMetadata jpegMetadata, final TagInfo tagInfo) {
		final TiffField field = jpegMetadata.findEXIFValueWithExactMatch(tagInfo);
		if (field == null) {
		    System.out.println(tagInfo.name + ": " + "Not Found.");
		} else {
		    System.out.println(tagInfo.name + ": "
		            + field.getValueDescription());
		}
	}

}