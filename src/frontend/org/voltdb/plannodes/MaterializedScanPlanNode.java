/* This file is part of VoltDB.
 * Copyright (C) 2008-2013 VoltDB Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with VoltDB.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.voltdb.plannodes;

import org.json_voltpatches.JSONException;
import org.json_voltpatches.JSONObject;
import org.json_voltpatches.JSONStringer;
import org.voltdb.catalog.Cluster;
import org.voltdb.catalog.Database;
import org.voltdb.compiler.DatabaseEstimates;
import org.voltdb.compiler.ScalarValueHints;
import org.voltdb.expressions.AbstractExpression;
import org.voltdb.expressions.VectorValueExpression;
import org.voltdb.types.PlanNodeType;

/**
 * Used for SQL-IN that are accelerated with indexes.
 * A MaterializedScanPlanNode is created from the list part
 * of the SQL-IN-LIST. It is inner-joined with NLIJ to another
 * table to make the SQL-IN fast.
 *
 */
public class MaterializedScanPlanNode extends AbstractPlanNode {

    protected AbstractExpression m_rowData;

    public enum Members {
        TABLE_DATA;
    }

    public MaterializedScanPlanNode() {
        super();
    }

    @Override
    public PlanNodeType getPlanNodeType() {
        return PlanNodeType.MATERIALIZEDSCAN;
    }

    public void setRowData(AbstractExpression rowData) {
        assert(rowData instanceof VectorValueExpression);
        m_rowData = rowData;
    }

    public AbstractExpression getTableData() {
        return m_rowData;
    }

    /**
     * Accessor for flag marking the plan as guaranteeing an identical result/effect
     * when "replayed" against the same database state, such as during replication or CL recovery.
     * @return false
     */
    @Override
    public boolean isOrderDeterministic() {
        return true;
    }

    @Override
    public void computeCostEstimates(long childOutputTupleCountEstimate, Cluster cluster, Database db, DatabaseEstimates estimates, ScalarValueHints[] paramHints) {
        // assume constant cost. Most of the cost of the SQL-IN will be measured by the NLIJ that is always paired with this element
        m_estimatedProcessedTupleCount = 1;
        m_estimatedOutputTupleCount = 1;
    }

    @Override
    protected String explainPlanForNode(String indent) {
        return "MATERIALIZED SCAN of SQL-IN-LIST";
    }

    @Override
    public void resolveColumnIndexes() {
        // MaterializedScanPlanNodes have no children
        assert(m_children.size() == 0);
    }

    @Override
    public void toJSONString(JSONStringer stringer) throws JSONException {
        super.toJSONString(stringer);

        stringer.key(Members.TABLE_DATA.name());
        stringer.object();
        assert(m_rowData != null);
        m_rowData.toJSONString(stringer);
        stringer.endObject();
    }

    @Override
    protected void loadFromJSONObject(JSONObject obj, Database db) throws JSONException {
        helpLoadFromJSONObject(obj, db);

        assert(!obj.isNull(Members.TABLE_DATA.name()));
        JSONObject rowDataObj = obj.getJSONObject(Members.TABLE_DATA.name());
        m_rowData = AbstractExpression.fromJSONObject(rowDataObj, db);
    }
}