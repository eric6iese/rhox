/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.evermind.scriptmaster.jacob;

import com.jacob.com.Dispatch;
import jdk.nashorn.api.scripting.JSObject;

/**
 *
 * @author giese
 */
public class JsCom {

    public static JSObject connect(String name) {
        return new ComJsObject(name, new Dispatch(name), null);
    }
}
