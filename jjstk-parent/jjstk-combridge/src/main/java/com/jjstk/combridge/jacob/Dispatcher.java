package com.jjstk.combridge.jacob;

import com.sun.jna.platform.win32.COM.COMBindingBaseObject;
import java.util.NoSuchElementException;

import com.sun.jna.platform.win32.OleAuto;
import com.sun.jna.platform.win32.Variant.VARIANT;
import com.sun.jna.platform.win32.COM.COMLateBindingObject;
import com.sun.jna.platform.win32.COM.IDispatch;
import java.util.Date;

/**
 * A wrapper for the JNA-specific functionality which is required for the
 * combridge.
 */
final class Dispatcher extends COMLateBindingObject {

    Dispatcher(String name) {
        super(name, false);
    }

    Dispatcher(VARIANT variant) {
        super((IDispatch) ((VARIANT) variant).getValue());
    }

    public VARIANT get(String member) {
        VARIANT.ByReference result = new VARIANT.ByReference();
        oleMethod(OleAuto.DISPATCH_PROPERTYGET, result, getIDispatch(), member);
        return result;
    }

    public void set(String member, Object value) {
        VARIANT v = var(value);
        this.setProperty(member, getIDispatch(), v);
    }

    public VARIANT invoke(String method, Object... args) {
        VARIANT[] vars = new VARIANT[args.length];
        for (int i = 0; i < vars.length; i++) {
            vars[i] = var(args[i]);
        }
        if (vars.length == 0) {
            return super.invoke(method);
        } else {
            return super.invoke(method, vars);
        }
    }

    /**
     * Creates a Variant out of a java object, applying implicit conversions
     * where necessary.
     */
    private static VARIANT var(Object in) {
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
}
