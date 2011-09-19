/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.cmp.jdbc.metadata;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import org.jboss.as.cmp.jdbc.JDBCAbstractQueryCommand;

/**
 * Class which holds all the information about read-ahead settings.
 * It loads its data from standardjbosscmp-jdbc.xml and jbosscmp-jdbc.xml
 *
 * @author <a href="mailto:on@ibis.odessa.ua">Oleg Nitz</a>
 * @author <a href="mailto:alex@jboss.org">Alexey Loubyansky</a>
 * @version $Revision: 81030 $
 */
public class JDBCReadAheadMetaData {
    public static final JDBCReadAheadMetaData DEFAULT = new JDBCReadAheadMetaData();

    /**
     * Don't read ahead.
     */
    private static final byte NONE = 0;

    /**
     * Read ahead when some entity is being loaded (lazily, good for all queries).
     */
    private static final byte ON_LOAD = 1;

    /**
     * Read ahead during "find" (not lazily, the best for queries with small result set).
     */
    private static final byte ON_FIND = 2;

    private static final List<String> STRATEGIES = Arrays.asList("none", "on-load", "on-find");

    /**
     * The strategy of reading ahead, one of {@link #NONE}, {@link #ON_LOAD}, {@link #ON_FIND}.
     */
    private byte strategy;

    /**
     * The page size of the read ahead buffer
     */
    private int pageSize;

    /**
     * The name of the load group to eager load.
     */
    private String eagerLoadGroup;

    /**
     * a list of left-join
     */
    private List<JDBCLeftJoinMetaData> leftJoinList;

    /**
     * Is read ahead strategy is none.
     */
    public boolean isNone() {
        return (strategy == NONE);
    }

    /**
     * Is the read ahead stratey on-load
     */
    public boolean isOnLoad() {
        return (strategy == ON_LOAD);
    }

    /**
     * Is the read ahead stratey on-find
     */
    public boolean isOnFind() {
        return (strategy == ON_FIND);
    }

    /**
     * Gets the read ahead page size.
     */
    public int getPageSize() {
        return pageSize;
    }

    /**
     * Gets the eager load group.
     */
    public String getEagerLoadGroup() {
        return eagerLoadGroup;
    }

    public List<JDBCLeftJoinMetaData> getLeftJoins() {
        return leftJoinList;
    }

    /**
     * Returns a string describing this JDBCReadAheadMetaData.
     *
     * @return a string representation of the object
     */
    public String toString() {
        return "[JDBCReadAheadMetaData :" +
                " strategy=" + STRATEGIES.get(strategy) +
                ", pageSize=" + pageSize +
                ", eagerLoadGroup=" + eagerLoadGroup +
                ", left-join" + leftJoinList + "]";
    }

    public void setStrategy(final String strategy) {
        this.strategy = (byte) STRATEGIES.indexOf(strategy);
    }

    public void setPageSize(final int pageSize) {
        this.pageSize = pageSize;
    }


    public void setEagerLoadGroup(String eagerLoadGroup) {
        this.eagerLoadGroup = eagerLoadGroup;
    }

    public void setLeftJoins(final List<JDBCLeftJoinMetaData> leftJoins) {
        this.leftJoinList = leftJoins;
    }
}
