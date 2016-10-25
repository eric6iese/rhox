/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rhox.exec;

import java.io.File;
import java.lang.ProcessBuilder.Redirect;
import java.nio.file.Path;

/**
 * Just an Alias with additional factories for ProcessBuilder.Redirect
 */
public class ProcessRedirect {

    public static final Redirect INHERIT = Redirect.INHERIT;

    public static final Redirect PIPE = Redirect.PIPE;

    public static Redirect from(String file) {
        return from(new File(file));
    }

    public static Redirect from(Path file) {
        return from(file.toFile());
    }

    public static Redirect from(File file) {
        return Redirect.from(file);
    }

    public static Redirect to(String file) {
        return to(new File(file));
    }

    public static Redirect to(Path file) {
        return to(file.toFile());
    }

    public static Redirect to(File file) {
        return Redirect.to(file);
    }

    public static Redirect appendTo(String file) {
        return appendTo(new File(file));
    }

    public static Redirect appendTo(Path file) {
        return appendTo(file.toFile());
    }

    public static Redirect appendTo(File file) {
        return Redirect.appendTo(file);
    }

}
