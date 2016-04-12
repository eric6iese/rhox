package com.rhox.combridge;

import com.sun.jna.WString;
import static com.sun.jna.platform.win32.COM.COMBindingBaseObject.LOCALE_USER_DEFAULT;
import com.sun.jna.platform.win32.COM.COMException;

import com.sun.jna.platform.win32.OleAuto;
import com.sun.jna.platform.win32.Variant.VARIANT;
import com.sun.jna.platform.win32.COM.COMLateBindingObject;
import com.sun.jna.platform.win32.COM.COMUtils;
import com.sun.jna.platform.win32.COM.IDispatch;
import com.sun.jna.platform.win32.Guid;
import com.sun.jna.platform.win32.OaIdl.DISPID;
import com.sun.jna.platform.win32.OaIdl.DISPIDByReference;
import com.sun.jna.platform.win32.WinNT.HRESULT;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A wrapper for the JNA-specific functionality which is required for the
 * combridge.
 */
final class Dispatcher extends COMLateBindingObject {

    /**
     * Internal Cache which stores all display ids (and types) after an element
     * has been resolved.
     */
    private final Map<String, DISPID> idCache = new ConcurrentHashMap<>();

    Dispatcher(String name) {
        super(name, false);
    }

    Dispatcher(IDispatch dispatch) {
        super(dispatch);
    }

    /**
     * Returns the COM-Invocation ID for a given name, which is required for all
     * other methods of this class.<br/>
     * If an id is found, then it will be cached after the first call.
     *
     * @throws COMException if the calculation of the id failed, for example
     * because no mapping exists
     */
    public DISPID getId(String name) {
        return idCache.computeIfAbsent(name, n -> {
            // variable declaration
            WString[] ptName = {new WString(n)};
            DISPIDByReference pdispID = new DISPIDByReference();

            // Get DISPID for name passed...
            IDispatch pDisp = getIDispatch();
            HRESULT hr = pDisp.GetIDsOfNames(new Guid.REFIID.ByValue(Guid.IID_NULL), ptName, 1,
                    LOCALE_USER_DEFAULT, pdispID);

            COMUtils.checkRC(hr);
            return pdispID.getValue();
        });
    }

    public VARIANT get(DISPID id) {
        VARIANT.ByReference result = new VARIANT.ByReference();
        call(OleAuto.DISPATCH_PROPERTYGET, result, id, null);
        return result;
    }

    public void set(DISPID id, VARIANT value) {
        call(OleAuto.DISPATCH_PROPERTYPUT, null, id, new VARIANT[]{value});
    }

    public VARIANT call(DISPID id, VARIANT... args) {
        VARIANT.ByReference result = new VARIANT.ByReference();
        call(OleAuto.DISPATCH_METHOD, result, id, args);
        return result;
    }

    private void call(int nType, VARIANT.ByReference pvResult, DISPID dispId, VARIANT[] pArgs) {
        IDispatch pDisp = getIDispatch();
        super.oleMethod(nType, pvResult, pDisp, dispId, pArgs);
    }

}
