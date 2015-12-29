package com.jjstk.combridge;

import com.sun.jna.WString;
import com.sun.jna.platform.win32.COM.COMBindingBaseObject;
import static com.sun.jna.platform.win32.COM.COMBindingBaseObject.LOCALE_USER_DEFAULT;
import java.util.NoSuchElementException;

import com.sun.jna.platform.win32.OleAuto;
import com.sun.jna.platform.win32.Variant.VARIANT;
import com.sun.jna.platform.win32.COM.COMLateBindingObject;
import com.sun.jna.platform.win32.COM.COMUtils;
import com.sun.jna.platform.win32.COM.IDispatch;
import com.sun.jna.platform.win32.Guid;
import com.sun.jna.platform.win32.OaIdl.DISPID;
import com.sun.jna.platform.win32.OaIdl.DISPIDByReference;
import com.sun.jna.platform.win32.WinNT.HRESULT;
import java.util.Date;

/**
 * A wrapper for the JNA-specific functionality which is required for the
 * combridge.
 */
final class Dispatcher extends COMLateBindingObject {

    Dispatcher(String name) {
        super(name, false);
    }

    Dispatcher(IDispatch dispatch) {
        super(dispatch);
    }

    Dispatcher(VARIANT variant) {
        this((IDispatch) ((VARIANT) variant).getValue());
    }

    public VARIANT get(String member) {
        DISPID id = getId(member);
        return get(id);
    }

    public void set(String member, Object value) {
        DISPID id = getId(member);
        set(id, value);
    }

    public VARIANT invoke(String method, Object... args) {
        DISPID id = getId(method);
        return invoke(method, args);
    }

    /**
     * Returns the COM-Invocation ID for a given name. This call is part of any
     * of the other 3 in order to know how to invoke a method.<br/>
     * It can be used for a single call in order to improve performance or just
     * to check if a method is available at all.
     */
    public DISPID getId(String name) {
        // variable declaration
        WString[] ptName = new WString[]{new WString(name)};
        DISPIDByReference pdispID = new DISPIDByReference();

        // Get DISPID for name passed...
        IDispatch pDisp = getIDispatch();
        HRESULT hr = pDisp.GetIDsOfNames(new Guid.REFIID.ByValue(Guid.IID_NULL), ptName, 1,
                LOCALE_USER_DEFAULT, pdispID);

        COMUtils.checkRC(hr);
        return pdispID.getValue();
    }

    public VARIANT get(DISPID id) {
        VARIANT.ByReference result = new VARIANT.ByReference();
        call(OleAuto.DISPATCH_PROPERTYGET, result, id, null);
        return result;
    }

    public void set(DISPID id, Object value) {
        VARIANT v = var(value);
        VARIANT[] vars = new VARIANT[]{v};
        call(OleAuto.DISPATCH_PROPERTYPUT, null, id, vars);
    }

    public VARIANT invoke(DISPID id, Object... args) {
        VARIANT[] vars = new VARIANT[args.length];
        for (int i = 0; i < vars.length; i++) {
            vars[i] = var(args[i]);
        }
        VARIANT.ByReference result = new VARIANT.ByReference();
        call(OleAuto.DISPATCH_METHOD, result, id, vars);
        return result;
    }

    private void call(int nType, VARIANT.ByReference pvResult, DISPID dispId, VARIANT[] pArgs) {
        IDispatch pDisp = getIDispatch();
        super.oleMethod(nType, pvResult, pDisp, dispId, pArgs);
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
