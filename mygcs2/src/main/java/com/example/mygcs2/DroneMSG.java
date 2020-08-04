package com.example.mygcs2;

public class DroneMSG {
    public static String MSG_YES                   = "Yes";
    public static String MSG_NO                    = "No";
    public static String MSG_CANCLED               = "Cancled";
    public static String MSG_LAUNCHMOTER           = "모터를 가동합니다.";
    public static String MSG_TAKINGOFF             = "기체가 상승합니다.";
    public static String MSG_GUIDEMODEON           = "확인하시면 가이드모드로 전환 후 기체가 이동합니다.";
    public static String MSG_OVERNUMBER            = "10 이상 고도를 높일 수 없습니다.";
    public static String MSG_UNDERNUMBER           = "3 이하 고도를 낮출 수 없습니다.";
    public static String MSG_GUIDEMODEEND          = "목적지에 도착하였습니다.";

    public static String ERR_UNABLEARM             = "Unable to arm vehicle.";
    public static String ERR_UNABLETAKEOFF         = "Unable to Take-Off vehicle.";
    public static String ERR_UNABLELANDING         = "Unable to land the vehicle.";
    public static String ERR_UNCONNECTED           = "Connect to a drone first.";
    public static String ERR_ARMING_TIMEOUT        = "Arming operation timed out.";

    public static String ALERT_LAUNCHMOTER         = "* 모터가동";
    public static String ALERT_TAKEOFF             = "* 이륙완료";
    public static String ALERT_LANDING             = "* 착륙완료";
    public static String ALERT_GUIDEMODESTART      = "* 기체가 목표지점으로 이동합니다.";
    public static String ALERT_GUIDEMODEEND        = "* 기체가 목표지점에 도착했습니다.";
}
