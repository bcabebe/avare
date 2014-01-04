/*
Copyright (c) 2012, Apps4Av Inc. (apps4av.com) 
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
    *     * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
    *
    *     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package com.ds.avare.utils;

import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;

import com.ds.avare.LocationView;
import com.ds.avare.storage.Preferences;

/***
 * Object to handle all of the text on the top two lines of the screen
 * along with their configured content
 * 
 * @author Ron
 *
 */
public class InfoLines {

	// Simple class to encapsulate a filed location on the screen
	public class InfoLineFieldLoc {
		int mRowIdx;
		int mFieldIdx;
		
		private InfoLineFieldLoc(int aRowIdx, int aFieldIdx) 
		{
			mRowIdx = aRowIdx;
			mFieldIdx = aFieldIdx;
		}
	}
	
	// Dynamic data fields related items
    int mDisplayWidth;			// Horizontal display size
    int mDisplayOrientation;	// portrait or landscape
    int mFieldPosX[];			// X positions of the fields left edges
    int mFieldLines[][];		// Configuration/content of the status lines

    Preferences mPref;			// How to fetch preferences
    LocationView mLocationView;	// Link back to the location view

    // Constants to indicate the display orientation
    static final int ID_DO_LANDSCAPE = 0;
    static final int ID_DO_PORTRAIT  = 1;

    // To add new display fields, take the ID_FLD_MAX value, and adjust MAX up by 1. 
    // ID_FLD_MAX must always be the highest, an ID_FLD_NUL the lowest
    // Ensure that the string-array "TextFieldOptions" is update with the new entry
    // in the proper order
    static final int ID_FLD_NUL = 0;
    static final int ID_FLD_GMT = 1;
    static final int ID_FLD_LT  = 2;
    static final int ID_FLD_SPD = 3;
    static final int ID_FLD_HDG = 4;
    static final int ID_FLD_BRG = 5;
    static final int ID_FLD_DST = 6;
    static final int ID_FLD_DIS = 7;
    static final int ID_FLD_ETE = 8;
    static final int ID_FLD_ETA = 9;
    static final int ID_FLD_MSL = 10;
    static final int ID_FLD_AGL = 11;
    static final int ID_FLD_HOB = 12;
    static final int ID_FLD_VSI = 13;
    static final int ID_FLD_MAX = 14;

    /***
     * Construct this object passing in the LocationView that
     * did the creation
     * 
     * @param locationView
     */
    public InfoLines(LocationView locationView) 
    {
    	mLocationView = locationView;
    	mPref = mLocationView.getPref();
    	
        mFieldLines = new int[4][ID_FLD_MAX];	// 2 Lines for portrait, 2 for landscape
        String rowFormats = mPref.getRowFormats();	// One config string for all 4 lines
        String strRows[] = rowFormats.split(" ");	// Split the string to get each row
        for(int rowIdx = 0; rowIdx < strRows.length; rowIdx++) {
	        String arFields[] = strRows[rowIdx].split(",");		// Split the row string to get each field
	        for(int idx = 0; idx < arFields.length; idx++) {
	        	mFieldLines[rowIdx][idx] = Integer.parseInt(arFields[idx]);
	        }
        }
    }

    /***
     * A caller wishes to change the type of the display field.
     * 
     * @param infoLineFieldLoc what field to change
     * @param nType what type to set it to
     */
    public void setFieldType(InfoLineFieldLoc infoLineFieldLoc, int nType) 
    {
    	if(rangeCheck(infoLineFieldLoc) == true) {	// Make sure field in range
    		mFieldLines[infoLineFieldLoc.mRowIdx][infoLineFieldLoc.mFieldIdx] = nType;
    		mPref.setRowFormats(buildConfigString());	// Save to storage
    	}
    }
    
	/***
	 * Return the type ID of the field in question
	 *  
	 * @param infoLineFieldLoc
	 * @return
	 */
    public int getFieldType(InfoLineFieldLoc infoLineFieldLoc) 
    {
    	if(rangeCheck(infoLineFieldLoc) == true)
    		return mFieldLines[infoLineFieldLoc.mRowIdx][infoLineFieldLoc.mFieldIdx];
    	return ID_FLD_NUL;
    }

    boolean rangeCheck(InfoLineFieldLoc iLFL)
    {
    	if((iLFL.mRowIdx < 0) || (iLFL.mRowIdx >= mFieldLines.length))
    		return false;
    	if((iLFL.mFieldIdx < 0) || (iLFL.mFieldIdx >= mFieldLines[iLFL.mRowIdx].length))
    		return false;
        return true;
    }

    /***
     * Is there a field to display at the indicated location. To figure this out we
     * need the X/Y of the location along with the paint object (which determines text size)
     * 
     * @param aPaint
     * @param posX
     * @param posY
     * @return an InfoLineFieldLoc object which identifies the field or null
     */
    public InfoLineFieldLoc findField(Paint aPaint, float posX, float posY)
    {
        if(posY > aPaint.getTextSize() * 2) {
    		return null;
        }
        
    	// Did we tap on Row0 or Row1 ?
    	int nRowIdx = 0;
    	if(posY > aPaint.getTextSize()) {
    		nRowIdx = 1;
    	}

    	// Make the adjustment here in case we are in PORTRAIT display mode
    	if(mDisplayOrientation == ID_DO_PORTRAIT) {
    		nRowIdx += 2;
    	}
    	
    	// Find out what field we tapped over
    	int nFieldIdx =  mFieldPosX.length - 1;
    	for(int idx = 1; idx < mFieldPosX.length; idx++) {
    		if(mFieldPosX[idx] > posX) {
    			nFieldIdx = idx - 1;
    			break;
    		}
    	}

    	return new InfoLineFieldLoc(nRowIdx, nFieldIdx);
    }

    /***
     * This method draws the top two lines of the display.
     * 
     * @param canvas
     * @param aPaint
     * @param errorStatus
     * @param aTextColor
     * @param aTextColorOpposite
     * @param aShadow
     */
    public void drawCornerTextsDynamic(Canvas canvas, Paint aPaint, String errorStatus, int aTextColor, int aTextColorOpposite, int aShadow)
    {
    	// If the screen width has changed since the last time, we need to recalc
    	// the positions of the fields
		resizeFields(aPaint, mLocationView.getDisplayWidth());
    	
		float textSize = aPaint.getTextSize();
		
    	// Draw the shadowed background on the top 2 lines if we are configured to do so
        if(mPref.shouldShowBackground()) {
        	aPaint.setShadowLayer(0, 0, 0, 0);
        	aPaint.setColor(aTextColorOpposite);
        	aPaint.setAlpha(0x7f);
            canvas.drawRect(0, 0, mDisplayWidth, textSize * 2 + aShadow, aPaint);
            aPaint.setAlpha(0xff);
        }
        aPaint.setShadowLayer(aShadow, aShadow, aShadow, Color.BLACK);

        // White text that is left aligned
        aPaint.setTextAlign(Align.LEFT);
        aPaint.setColor(aTextColor);

        // Lines 0/1 are for landscape, 2/3 for portrait
        int nStartLine = 0;
        if (mDisplayOrientation == ID_DO_PORTRAIT)
        	nStartLine = 2;
        
        for(int row = 0; row < 2; row++) {
	        for(int idx = 0, max = mFieldPosX.length; idx < max; idx++) {
	        	canvas.drawText(getDisplayFieldValue(mFieldLines[nStartLine + row][idx]), mFieldPosX[idx], textSize * (1 + row), aPaint);
	        }
        }
        
        // Now check for an error message. That will overwrite some of the fields on the screen
        // If we have an error message, that has priority over everything else
        if(errorStatus != null) {
        	aPaint.setTextAlign(Align.RIGHT);
        	aPaint.setColor(Color.RED);
            canvas.drawText(errorStatus,
                    mDisplayWidth, textSize * 2, aPaint);
        }
    }

    /***
     * Calculate the quantity and size of that we can display with the given display width and paint.
     * 
     * @param aPaint
     * @param aDisplayWidth
     */
    private void resizeFields(Paint aPaint, int aDisplayWidth)
    {
    	// If the size did not change, then we don't need to do any work
    	if(mDisplayWidth == aDisplayWidth)
    		return;
    	
        // Set our copy of what the display width is.
        mDisplayWidth = aDisplayWidth;
        
    	// Set if we are in portrait or landscape mode. This determines what
    	// status lines we draw
    	mDisplayOrientation = mPref.getOrientation().contains("Landscape") ? ID_DO_LANDSCAPE : ID_DO_PORTRAIT; 
    	
        // Fetch the NULL field to figure out how large it is
        String strField = getDisplayFieldValue(ID_FLD_NUL) + " ";
        float charWidths[] = new float[strField.length()];
        aPaint.getTextWidths(strField, charWidths);
        int fieldWidth = ((int)charWidths[0] * strField.length());
        
        // Now we can determine the max fields per line we can display
        int maxFieldsPerLine = mDisplayWidth / fieldWidth;

        // There might be leftover space. Divide it so that it pads between
        // the fields
        int nLeftoverSpace = mDisplayWidth - maxFieldsPerLine * fieldWidth;
        int nPadding = nLeftoverSpace / (maxFieldsPerLine - 1);
        
        // Now calculate the horizontal position of each field
        int nRightShift = nPadding;
        mFieldPosX = new int[maxFieldsPerLine];
        mFieldPosX[0] = 0;
        for(int idx = 1, max = mFieldPosX.length; idx < max; idx++){
        	mFieldPosX[idx] = mFieldPosX[idx - 1] +  fieldWidth + nRightShift;

        	// If this is the last field then make it right justified
        	if(idx == max - 1) {
        		mFieldPosX[idx] = mDisplayWidth - fieldWidth + (int)charWidths[0];
        	}
        	
        	// Adjust the padding between this and the next field
        	if(nLeftoverSpace > nPadding) {
        		nLeftoverSpace -= nPadding;
        		nRightShift = nPadding;
        	} else {
        		nRightShift = nLeftoverSpace;
        		nLeftoverSpace = 0;
        	}
        }

    }
    
    /***
     * Return a string that represents the value of the desired field
     * @param aField which field is being requested
     * @return string display value for that field
     */
    private String getDisplayFieldValue(int aField)
    {
	    String dspText = "     ";
	    switch(aField) {
	    	default:
		    case ID_FLD_NUL: {
		    	dspText = "     ";
		    	break;
		    }
		    
	    	case ID_FLD_VSI: {
	    		dspText = String.format(Locale.getDefault(), "%+05.0f", mLocationView.getVSI());
	    		break;
	    	}
	    	
	    	case ID_FLD_SPD: {
	    		dspText = String.format(Locale.getDefault(), "%3.0f%s", mLocationView.getGpsParams().getSpeed(), 
	    				Preferences.speedConversionUnit);
	            break;
	    	}
	
	    	case ID_FLD_HOB: {
	    		if(mLocationView.getStorageService() != null) {
	    			dspText = "" + mLocationView.getStorageService().getFlightTimer().getValue();
	    		}
	    		break;
	    	}
	    	
	    	case ID_FLD_HDG: {
	    		dspText = " " + Helper.correctConvertHeading(Math.round((
	    				Helper.getMagneticHeading(mLocationView.getGpsParams().getBearing(), 
	    						mLocationView.getGpsParams().getDeclinition())))) + '\u00B0';
	    		break;
	    	}
	    	
	    	case ID_FLD_BRG: {
	    		if(mLocationView.getStorageService() != null) {
		    		if(mLocationView.getStorageService().getDestination() != null) {
		    			dspText = " " + Helper.correctConvertHeading(Math.round((
		    					Helper.getMagneticHeading(
		    					mLocationView.getStorageService().getDestination().getBearing(), 
		    					mLocationView.getGpsParams().getDeclinition())))) + '\u00B0';
		    		}
	    		}
	    		break;
	    	}
	    	
	    	case ID_FLD_DST: {
	    		if(mLocationView.getStorageService() != null) {
		    		if(mLocationView.getStorageService().getDestination() != null) {
		    			dspText = "  " + mLocationView.getStorageService().getDestination().getID();
		    		}
	    		}
	    		break;
	    	}
	    	
	    	case ID_FLD_DIS: {
	    		if(mLocationView.getStorageService() != null) {
		    		if(mLocationView.getStorageService().getDestination() != null) {
		        		dspText = String.format(Locale.getDefault(), "%3.0f%s", 
		        				mLocationView.getStorageService().getDestination().getDistance(), 
		        				Preferences.distanceConversionUnit);
		    		}
	    		}
	    		break;
	    	}
	    	
	    	case ID_FLD_ETE: {
	    		if(mLocationView.getStorageService() != null) {
		    		if(mLocationView.getStorageService().getDestination() != null) {
		    			dspText = "" + mLocationView.getStorageService().getDestination().getEte();
		    		}
	    		}
	    		break;
	    	}
	    	
	    	case ID_FLD_ETA: {
	    		if(mLocationView.getStorageService() != null) {
		    		if(mLocationView.getStorageService().getDestination() != null) {
		    			dspText = "" + mLocationView.getStorageService().getDestination().getEta();
		    		}
	    		}
	    		break;
	    	}
	    	
	    	case ID_FLD_LT: {
	    		Calendar localTime = Calendar.getInstance();
	    		dspText = String.format(Locale.getDefault(), "%02d:%02d", 
	    				localTime.get(Calendar.HOUR_OF_DAY), 
	    				localTime.get(Calendar.MINUTE));
	    		break;
	    	}
	    	
	    	case ID_FLD_GMT: {
	    		Calendar localTime = Calendar.getInstance();
	    		localTime.setTimeZone(TimeZone.getTimeZone("UTC"));
	    		dspText = String.format(Locale.getDefault(), "%02d:%02d", 
	    				localTime.get(Calendar.HOUR_OF_DAY), 
	    				localTime.get(Calendar.MINUTE));
	    		break;
	    	}
	    	
	    	case ID_FLD_MSL: {
	    		double alt = mLocationView.getGpsParams().getAltitude();
	    		dspText = String.format(Locale.getDefault(), getAglMslFmtString(alt), alt);
	    		break;
	    	}
	    	
	    	case ID_FLD_AGL: {
	    		double dAGL = 0;
	    		double dElev = mLocationView.getElev();
	    		if (dElev > 0)
	    			dAGL = mLocationView.getGpsParams().getAltitude() - dElev;
	    		dspText = String.format(Locale.getDefault(), getAglMslFmtString(dAGL), dAGL);
	    		break;
	    	}
	    }
	    return dspText;
    }
    
    /***
     * Return a format string for the value passed in
     * @param value 
     * @return
     */
    private String getAglMslFmtString(double value)
    {
		String fmtString = "";
		if(value >= 9999)
			fmtString = "%05.0f";
		else if(value >= 1000)
			fmtString = "%4.0f'";
		else if(value >= 100)
			fmtString = "%03.0fft";
		return fmtString;
    }

    /***
     * Create the configuration string for the current settings and save
     * it to the shared preferences area
     * 
     * @return string that represents the current format of the fields
     */
    private String buildConfigString()
    {
    	String rowFormats = "";
    	for(int rowIdx = 0, rowMax = mFieldLines.length; rowIdx < rowMax; rowIdx++) {
    		for(int fldIdx = 0, fldMax = mFieldLines[rowIdx].length; fldIdx < fldMax; fldIdx++) {
    			rowFormats += mFieldLines[rowIdx][fldIdx];
    			if(fldIdx < fldMax - 1) {
    				rowFormats += ',';
    			}
    		}
    		if(rowIdx < rowMax - 1) {
    			rowFormats += ' ';
    		}
    	}
    	return rowFormats;
    }
}
