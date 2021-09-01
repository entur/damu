package no.entur.damu.util;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Date;

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


    public static Date toGtfsDate(LocalDateTime netexDate) {
        return Date.from(netexDate.atZone(ZoneId.systemDefault()).toInstant());
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
