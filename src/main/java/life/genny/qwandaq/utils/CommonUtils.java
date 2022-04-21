package life.genny.qwandaq.utils;

import java.lang.reflect.Array;
import java.util.List;

import life.genny.qwandaq.utils.callbacks.FIGetStringCallBack;

/**
 * A few Common Utils to use throughout Genny.
 * 
 * @author Bryn
 * @author Jasper
 */
public class CommonUtils {

    /**
     * String Array Builder to get Stringified arrays of custom components
     */
    public static class StringArrayBuilder<T> {

        /**
         * Get a JSON style array of objects. Pass a callback for custom values. Will default to {@link Object#toString()} otherwise
         * @param list - list to get array of
         * @param stringCallback - callback to use to retrieve a string value of the object
         * @return a JSON style array of objects, where each item is the value returned from stringCallback
         */
        public String getArrayString(List<T> list, FIGetStringCallBack<T> stringCallback) {
            String result = "";
            for(T object : list) {
                result += "\"" + stringCallback.getString(object) + "\",";
            }
            return "[" + result.substring(0, result.length() - 1) + "]";
        }

        /**
         * Get a JSON style array of objects. Pass a callback for custom values. Will default to {@link Object#toString()} otherwise
         * @param list - list to get array of
         * @param stringCallback - callback to use to retrieve a string value of the object
         * @return a JSON style array of objects, where each item is the value returned from stringCallback
         */
        public String getArrayString(T[] array, FIGetStringCallBack<T> stringCallback) {
            String result = "";
            for(T object : array) {
                result += "\"" + stringCallback.getString(object) + "\",";
            }
            return "[" + result.substring(0, result.length() - 1) + "]";
        }
    }
}
