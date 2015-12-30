/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jjstk.combridge;

import com.sun.jna.platform.win32.COM.COMBindingBaseObject;
import com.sun.jna.platform.win32.COM.COMException;
import com.sun.jna.platform.win32.COM.IDispatch;
import com.sun.jna.platform.win32.OaIdl;
import com.sun.jna.platform.win32.Variant.VARIANT;
import java.util.Date;
import java.util.NoSuchElementException;

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
        // Primitives
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

        // Other default Variant types
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
        if (in instanceof Number) {
            Number n = (Number) in;
            return new VARIANT(n.longValue());
        }
        if (in instanceof COMBindingBaseObject) {
            COMBindingBaseObject obj = (COMBindingBaseObject) in;
            return new VARIANT(obj.getIDispatch());
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

    public static RuntimeException newException(String operation, COMException e) {
        OaIdl.EXCEPINFO info = e.getExcepInfo();
        long error = info.wCode.longValue();
        if (error == 0L) {
            error = info.scode.longValue();
        }
        String source = info.bstrSource.getValue();
        String description = info.bstrDescription.getValue();
        return new UnsupportedOperationException(
                String.format("%s failed! ErrorCode: %s, Source: %s, Message: %s", operation, error, source, description));
    }

    /**
     * Hidden utility cons.
     */
    private Variants() {
    }
}
