package com.netcompss.ffmpeg4android;

import android.util.Log;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ProgressCalculator {

    private static final int DURATION_OF_CURRENT_WAIT_INDEX_LIMIT = 12;

    private int durationOfCurrentWaitIndex = 0;
    private String durationOfCurrent;
    private long lastVklogSize = -1;
    private int vkLogNoChangeCounter = 0;
    private SimpleDateFormat simpleDateFormat;
    private long timeRef = -1;
    private int prevProgress = 0;
    private String vkLogPath = null;

    public ProgressCalculator(String vkLogPathIn) {
        vkLogPath = vkLogPathIn;
        simpleDateFormat = new SimpleDateFormat("HH:mm:ss.SS");
        try {
            Date ref = simpleDateFormat.parse("00:00:00.00");
            ref.setYear(112);
            timeRef = ref.getTime();
        } catch (ParseException e) {
            Log.w(Prefs.TAG, "failed to set timeRef");
        }
    }

    public void initCalcParamsForNextInter() {
        Log.i(Prefs.TAG, "initCalcParamsForNextInter");
        lastVklogSize = -1;
        vkLogNoChangeCounter = 0;
        durationOfCurrent = null;

    }

    public int calcProgress() {
        return calcProgress(1);
    }


    public int calcProgress(int durationMultiplyer) {
        //Log.i(Prefs.TAG, "========calc progress======= " + durationMultiplyer);
        int progress = 0;
        if (durationOfCurrent == null) {
            String dur = GeneralUtils.getDutationFromVCLogRandomAccess(vkLogPath);
            Log.d(Prefs.TAG, "dur: " + dur);
            if (dur == null || dur.equals("") || dur.equals("null")) {
                Log.i(Prefs.TAG, "dur is not good, not setting ");
                if (durationOfCurrentWaitIndex < DURATION_OF_CURRENT_WAIT_INDEX_LIMIT) {
                    Log.i(Prefs.TAG, "waiting for real duration, going out of calcProgress with 0");
                    durationOfCurrentWaitIndex++;
                    return 0;
                } else {
                    Log.i(Prefs.TAG, "durationOfCurrentWaitIndex is equal to: " + DURATION_OF_CURRENT_WAIT_INDEX_LIMIT + " reseting.");
                    durationOfCurrentWaitIndex = 0;
                    Log.i(Prefs.TAG, "setting fake Prefs.durationOfCurrent");

                    durationOfCurrent = "00:03:00.00";
                    Log.w(Prefs.TAG, "setting fake Prefs.durationOfCurrent (Cant get from file): " + durationOfCurrent);

                }
            } else {
                durationOfCurrent = GeneralUtils.getDutationFromVCLogRandomAccess(vkLogPath);
                Log.i(Prefs.TAG, "duration: " + durationOfCurrent + " \nTranscoding...");
            }
        }


        if (durationOfCurrent != null) {

            long currentVkLogSize = -1;
            currentVkLogSize = GeneralUtils.getVKLogSizeRandomAccess(vkLogPath);
            //Log.d(Prefs.TAG, "currentVkLogSize: " + currentVkLogSize + " lastVklogSize: " + lastVklogSize);

            if (currentVkLogSize > lastVklogSize) {
                lastVklogSize = currentVkLogSize;
                vkLogNoChangeCounter = 0;
            } else {
                //Log.w(Prefs.TAG, "Looks like Vk log is not increasing in size");
                vkLogNoChangeCounter++;
            }


            String currentTimeStr = GeneralUtils.readLastTimeFromVKLogUsingRandomAccess(vkLogPath);
            //Log.d(Prefs.TAG, "currentTimeStr: " + currentTimeStr);
            if (currentTimeStr.equals("exit")) {
                Log.d(Prefs.TAG, "============Found one of the exit tokens in the log============");
                return 100;
            } else if (currentTimeStr.equals("error") && prevProgress == 0) {
                Log.d(Prefs.TAG, "============Found error in the log============");
                return 100;
            } else if (vkLogNoChangeCounter > 16) {
                Log.e(Prefs.TAG, "VK log is not changing in size, and no exit token found");
                return 100;
            }
            try {
                Date durationDate = simpleDateFormat.parse(durationOfCurrent);
                Date currentTimeDate = simpleDateFormat.parse(currentTimeStr);
                currentTimeDate.setYear(112);
                durationDate.setYear(112);
                //Log.d(Prefs.TAG, " durationDate: " + durationDate + " currentTimeDate: " + currentTimeDate);

                long durationLong = durationDate.getTime() - timeRef;
                if (durationMultiplyer != 1) {
                    //Log.i(Prefs.TAG, "====durationMultiplyer is not 1, handling===");
                    //Log.i(Prefs.TAG, "durationLong before: " + durationLong);
                    durationLong = durationLong * durationMultiplyer;
                    //Log.i(Prefs.TAG, "durationLong after: " + durationLong);
                }
                long currentTimeLong = currentTimeDate.getTime() - timeRef;
                //Log.d(Prefs.TAG, " durationLong: " + durationLong + " currentTimeLong: " + currentTimeLong + " diff: " + (durationLong - currentTimeLong));
                progress = Math.round(((float) currentTimeLong / durationLong) * 100);
                if (progress >= 100) {
                    Log.w(Prefs.TAG, "progress is 100, but can't find exit in the log, probably fake progress, still running...");
                    progress = 99;
                }
                prevProgress = progress;


            } catch (ParseException e) {
                Log.w(Prefs.TAG, e.getMessage());
            }
        }

        return progress;
    }


}
