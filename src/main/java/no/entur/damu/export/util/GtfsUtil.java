package no.entur.damu.export.util;

import java.time.LocalTime;

public final class GtfsUtil {

    private GtfsUtil() {
    }

    /**
     * Return the number of seconds since midnight for this local time.
     *
     * @param netexTime
     * @return
     */
    public static int toGtfsTime(LocalTime netexTime) {
        return netexTime.toSecondOfDay();
    }

    /**
     * Return the number of seconds since midnight for this local time plus the number of seconds (positive or negative) corresponding to the day offset.
     * @param netexTime
     * @param dayOffset
     * @return
     */
    public static int toGtfsTimeWithDayOffset(LocalTime netexTime, int dayOffset) {
        return toGtfsTime(netexTime) + dayOffset * 60 * 60 * 24;
    }

    public static String toGtfsId(String neptuneId, String prefix, boolean keepOriginal) {
        if (keepOriginal) {
            return neptuneId;
        } else {
            String[] tokens = neptuneId.split(":");
            if (tokens[0].equals(prefix))
                return tokens[2];
            else
                return tokens[0] + "." + tokens[2];
        }
    }
}
