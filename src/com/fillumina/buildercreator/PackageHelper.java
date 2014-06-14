package com.fillumina.buildercreator;

import java.util.ArrayList;
import java.util.List;
import javax.lang.model.element.VariableElement;

/**
 *
 * @author Francesco Illuminati <fillumina@gmail.com>
 */
class PackageHelper {

    /** Removes packages from class names (it manages generics too).
     * i.e.
     * {@code java.lang.String -> String}
     * and
     * {@code java.util.List<java.lang.String> -> List<String>}.
     */
    static String removePackages(VariableElement element) {
        final String fullName = element.asType().toString();
        final List<String> list = new ArrayList<>();
        int idx = 0, counter = 0;
        for (char c : fullName.toCharArray()) {
            switch (c) {
                case ',':
                case '<':
                case '>':
                    list.add(fullName.substring(idx, counter));
                    list.add(String.valueOf(c));
                    idx = counter + 1;
                    break;
            }
            counter++;
        }
        if (list.isEmpty()) {
            return removePackage(fullName);
        }
        StringBuilder buf = new StringBuilder();
        for (String s : list) {
            if ("<>,".contains(s)) {
                buf.append(s);
            } else {
                buf.append(removePackage(s));
            }
        }
        return buf.toString();
    }

    /** Removes the package from a single class name (don't manage generics). */
    static String removePackage(String fullname) {
        int lastIndexOfPoint = fullname.lastIndexOf('.');
        if (lastIndexOfPoint == -1) {
            return fullname;
        }
        return fullname.substring(lastIndexOfPoint + 1, fullname.length());
    }

}
