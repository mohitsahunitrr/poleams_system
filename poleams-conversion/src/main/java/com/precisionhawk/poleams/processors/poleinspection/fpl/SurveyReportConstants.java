package com.precisionhawk.poleams.processors.poleinspection.fpl;

import java.awt.Point;

/**
 *
 * @author Philip A Chapman
 */
public interface SurveyReportConstants {
    
    static final int COL_FPL_ID = 0;
    static final int COL_POLE_NUM_1 = 1;
    static final int COL_POLE_TYPE = 2;
    static final int COL_POLE_OWNER = 3;
    static final int COL_POLE_ACCESS = 4;
    // Utility Pole Owner - Action Taken = 5
    static final int COL_POLE_HEIGHT = 6;
    static final int COL_POLE_CLASS = 7;
    static final int COL_POLE_SPAN_1_FRAMING = 8;
    static final int[] COL_POLE_SPAN_LEN = {9, 10, 11, 12};
    static final int COL_POLE_SPAN_2_FRAMING = 13;
    static final int COL_POLE_EQUIP_TYPE = 14;
    static final int COL_POLE_EQUIP_QUAN = 15;
    static final int COL_POLE_STREETLIGHT = 16;
    static final int[] COL_POLE_RISER_TYPE = {17, 18};
    static final int COL_POLE_CATV_ATTCHMNT_CNT = 19;
    static final int COL_POLE_CATV_TOTAL_SIZE = 20;
    static final int[] COL_POLE_CATV_ATTCHMNT_HEIGHT = {21, 23, 25, 27, 29, 31};
    static final int[] COL_POLE_CATV_ATTCHMNT_DIAM =   {22, 24, 26, 28, 30, 32};
    static final int COL_POLE_TELCO_ATTCHMNT_CNT = 33;
    static final int COL_POLE_TELCO_TOTAL_SIZE = 34;
    static final int[] COL_POLE_TELCO_ATTCHMNT_HEIGHT = {35, 37, 39, 41, 43, 45};
    static final int[] COL_POLE_TELCO_ATTCHMNT_DIAM =  {36, 38, 40, 42, 44, 46};
    static final int COL_POLE_NUM_PHASES = 47;
    static final int COL_POLE_PRIMARY_WIRE_TYPE = 48;
    static final int COL_POLE_NEUTRAL_WIRE_TYPE = 49;
    static final int COL_POLE_OPEN_WIRE_TYPE = 50;
    static final int COL_POLE_OPEN_WIRE_COUNT = 51;
    static final int COL_POLE_MULTIPLEX_TYPE = 52;
    static final int COL_POLE_SWITCH_NUM = 53;
    static final int COL_POLE_TLN_COORD = 54;
    static final int COL_POLE_LAT = 55;
    static final int COL_POLE_LON = 56;
    static final int[] COL_GUY_ASSOC =    {57, 61, 65, 69, 73, 77};
    static final int[] COL_GUY_LEAD_LEN = {58, 62, 66, 70, 74, 78};
    static final int[] COL_GUY_DIAM =     {59, 63, 67, 71, 75, 79};
    static final int[] COL_GUY_BEARING =  {60, 64, 68, 72, 76, 80};
    static final int COL_POLE_NUM_2 = 81;
    static final int COL_POLE_CONTRACTOR_COMMENTS = 82;
//    static final int COL_CURRENT_WIND_RATING = 83;
    static final int COL_CURRENT_PROPOSED_WORK = 84;
    static final int COL_CURRENT_ESTIMATED_COST = 85;
    static final int COL_CURRENT_NEW_WIND_RATING = 86;
    static final int COL_LAT_LONG_DELTA = 87;
    static final int COL_HORIZONTAL_POLE_LOADING = 88;
    static final Point FEEDER_HARDENING_LVL = new Point(13, 1);
    static final Point FEEDER_NAME = new Point(5, 1);
    static final Point FEEDER_NUM = new Point(1, 1);
    static final Point FEEDER_WIND_ZONE = new Point(8, 1);
    static final int FIRST_POLE_ROW = 4;
    static final String SURVEY_SHEET = "Survey Data";

}
