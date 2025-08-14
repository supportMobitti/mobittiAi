package org.mobitti.helpers;

import java.text.SimpleDateFormat;
import java.util.Date;

public class DateTimeUtils {

    public  static SimpleDateFormat dateTimeDbFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm");


    public static String getCurrentDateTimeDb() {
        return dateTimeDbFormatter.format(new Date());
    }
}
