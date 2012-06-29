/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.iiop.naming;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.omg.CosNaming.NameComponent;
import org.omg.CosNaming.NamingContextPackage.InvalidName;

/**
 * convenience class that represents a name
 *
 * @author Stuart Douglas
 */
public class Name {

    private final NameComponent[] nameComponents;
    private final NameComponent[] ctxName;
    private final NameComponent baseName;

    public Name(final NameComponent[] nameComponents) {
        this.nameComponents = nameComponents;
        this.ctxName = new NameComponent[nameComponents.length - 1];
        for (int i = 0; i < nameComponents.length - 1; ++i) {
            ctxName[i] = nameComponents[i];
        }
        baseName = nameComponents[nameComponents.length - 1];
    }

    public Name(final NameComponent nameComponent) {
        this.nameComponents = new NameComponent[1];
        this.nameComponents[0] = nameComponent;
        this.ctxName = new NameComponent[nameComponents.length - 1];
        for (int i = 0; i < nameComponents.length - 1; ++i) {
            ctxName[i] = nameComponents[i];
        }
        baseName = nameComponents[nameComponents.length - 1];
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final Name name = (Name) o;

        if (!Arrays.equals(nameComponents, name.nameComponents)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return nameComponents != null ? Arrays.hashCode(nameComponents) : 0;
    }

    public Name ctxName() {
        return new Name(ctxName);
    }

    public org.omg.CosNaming.NameComponent baseNameComponent() {
        return baseName;
    }

    public NameComponent[] components() {
        return nameComponents;
    }

    public static org.omg.CosNaming.NameComponent[] toName(String sn) throws org.omg.CosNaming.NamingContextPackage.InvalidName {
        if (sn == null || sn.length() == 0 || sn.startsWith("/"))
            throw new InvalidName();

        List<NameComponent> ret = new ArrayList<NameComponent>();

        int start = 0;
        int i;
        for (i = 0; i < sn.length(); i++) {
            if (sn.charAt(i) == '/' && sn.charAt(i - 1) != '\\') {
                if (i - start == 0)
                    throw new InvalidName();
                ret.add(toNameComponent(sn.substring(start, i)));
                start = i + 1;
            }
        }
        if (start < i)
            ret.add(toNameComponent(sn.substring(start, i)));
        return ret.toArray(new NameComponent[ret.size()]);
    }

    private static org.omg.CosNaming.NameComponent toNameComponent(String sn) throws org.omg.CosNaming.NamingContextPackage.InvalidName {
        final StringBuilder id = new StringBuilder();
        final StringBuilder kind = new StringBuilder();
        StringBuilder current = id;
        for (int i = 0; i < sn.length(); i++) {
            char c = sn.charAt(i);
            if (c == '\\') {
                i++;
                if (i >= sn.length()) {
                    throw new InvalidName(sn);
                }
                c = sn.charAt(i);
            } else if (c == '.') {
                if (current == kind) {
                    throw new InvalidName(sn);
                }
                current = kind;
            }
            current.append(c);
        }
        return (new org.omg.CosNaming.NameComponent(id.toString(), kind.toString()));
    }

    public static String toString(org.omg.CosNaming.NameComponent[] name) throws org.omg.CosNaming.NamingContextPackage.InvalidName {
        if (name == null || name.length == 0)
            throw new org.omg.CosNaming.NamingContextPackage.InvalidName(Arrays.toString(name));

        final StringBuilder ret = new StringBuilder();
        for (int i = 0; i < name.length; i++) {
            if (i > 0) {
                ret.append("/");
            }

            if (name[i].id.length() > 0) {
                ret.append(escape(name[i].id));
            }

            if (name[i].kind.length() > 0 ||
                    name[i].id.length() == 0) {
                ret.append(".");
            }

            if (name[i].kind.length() > 0) {
                ret.append(escape(name[i].kind));
            }
        }
        return ret.toString();
    }

    private static String escape(String s) {
        final StringBuilder ret = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            final char c = s.charAt(i);
            if (c == '/' ||
                    c == '\\' ||
                    c == '.') {
                ret.append('\\');
            }
            ret.append(c);
        }
        return ret.toString();
    }
}
