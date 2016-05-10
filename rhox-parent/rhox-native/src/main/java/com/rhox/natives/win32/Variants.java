/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rhox.natives.win32;

import com.sun.jna.IntegerType;
import com.sun.jna.platform.win32.COM.COMBindingBaseObject;
import com.sun.jna.platform.win32.COM.COMException;
import com.sun.jna.platform.win32.COM.IDispatch;
import com.sun.jna.platform.win32.OaIdl;
import com.sun.jna.platform.win32.Variant.VARIANT;
import com.sun.jna.platform.win32.WTypes;
import java.util.Date;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.StringJoiner;

/**
 * Utility methods for converting variants to native JavaObjects.
 */
final class Variants {

    /**
     * Creates a Variant out of a java object, applying implicit conversions
     * where necessary.
     */
    public static VARIANT to(Object in) {
        if (in instanceof VARIANT) {
            return (VARIANT) in;
        }
        // Native Variant Constructors
        if (in instanceof Boolean) {
            return new VARIANT((boolean) in);
        }
        if (in instanceof Byte) {
            return new VARIANT((byte) in);
        }
        if (in instanceof Short) {
            return new VARIANT((short) in);
        }
        if (in instanceof Integer) {
            return new VARIANT((int) in);
        }
        if (in instanceof Long) {
            return new VARIANT((long) in);
        }
        if (in instanceof Character) {
            return new VARIANT((char) in);
        }
        if (in instanceof Float) {
            return new VARIANT((float) in);
        }
        if (in instanceof Double) {
            return new VARIANT((double) in);
        }
        if (in instanceof Date) {
            return new VARIANT((Date) in);
        }
        if (in instanceof String) {
            return new VARIANT((String) in);
        }
        if (in instanceof IDispatch) {
            return new VARIANT((IDispatch) in);
        }

        // Custom Mappings (destructuring)
        if (in instanceof CharSequence) {
            return new VARIANT(in.toString());
        }
        if (in instanceof COMBindingBaseObject) {
            return new VARIANT(((COMBindingBaseObject) in).getIDispatch());
        }
        if (in instanceof ComDispatch) {
            return new VARIANT(((ComDispatch) in).getDispatcher().getIDispatch());
        }
        if (in instanceof ComObject) {
            return new VARIANT(((ComObject) in).getDispatcher().getIDispatch());
        }
        throw new NoSuchElementException("Unkown type: " + in + "!");
    }

    /**
     * Converts any number of java values into an array of variants.
     */
    public static VARIANT[] toArray(Object[] values) {
        VARIANT[] vars = new VARIANT[values.length];
        for (int i = 0; i < vars.length; i++) {
            vars[i] = to(values[i]);
        }
        return vars;
    }

    /**
     * This method is nearly identical to getValue, with only one exception: If
     * the result is not a primitive Type but an IDispatch, then it will be
     * returned as a new dispatcher instead.<br/>
     * Later on, even more conversions might be possible, but for now none of
     * them are planned.
     */
    public static Object from(VARIANT variant) {
        Object value = variant.getValue();
        if (value instanceof IDispatch) {
            return new Dispatcher((IDispatch) value);
        }
        return value;
    }

    /**
     * Wraps the native ComException into something which is simpler to read...
     */
    public static RuntimeException newException(String operation, COMException e) {
        OaIdl.EXCEPINFO info = e.getExcepInfo();
        if (info == null) {
            throw new UnsupportedOperationException(e);
        }
        long error = getValue(info.wCode);
        if (error == 0L) {
            error = getValue(info.scode);
        }
        String source = getValue(info.bstrSource);
        String description = getValue(info.bstrDescription);
        return new UnsupportedOperationException(
                String.format("%s failed! ErrorCode: %s, Source: %s, Message: %s", operation, error, source, description));
    }

    private static long getValue(IntegerType word) {
        return word == null ? 0L : word.longValue();
    }

    private static String getValue(WTypes.BSTR bstr) {
        return bstr == null ? null : bstr.getValue();
    }

    /**
     * Creates a human-readable signature from the current node name and the
     * given parameters (used in invoke).
     */
    public static String toSignature(Object... args) {
        StringJoiner joiner = new StringJoiner(",", "(", ")");
        for (Object arg : args) {
            String v;
            if (arg instanceof CharSequence) {
                v = "'" + arg + "'";
            } else {
                v = Objects.toString(arg);
            }
            joiner.add(v);
        }
        return joiner.toString();
    }

    /**
     * Hidden utility cons.
     */
    private Variants() {
    }
}
