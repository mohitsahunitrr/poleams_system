package com.precisionhawk.poleams.dao.cassandra;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.SimpleStatement;
import com.precisionhawk.ams.bean.AssetInspectionSearchParams;
import com.precisionhawk.ams.dao.DaoException;
import com.precisionhawk.ams.dao.cassandra.AbstractCassandraDao;
import com.precisionhawk.ams.dao.cassandra.StatementBuilder;
import com.precisionhawk.ams.util.CollectionsUtilities;
import com.precisionhawk.poleams.dao.TransmissionStructureInspectionDao;
import com.precisionhawk.poleams.domain.TransmissionStructureInspection;
import java.util.List;

/**
 *
 * @author pchapman
 */
public class TransmissionStructureInspectionCassDao extends AbstractCassandraDao implements TransmissionStructureInspectionDao {
    protected static final String STATEMENTS_MAPS = "com/precisionhawk/poleams/dao/cassandra/TransmissionStructureInspection_Statements.yaml";

    private static final String COL_ASSET_ID = "asset_id";
    private static final String COL_ID = "id";
    private static final String COL_ORD_NUM = "ord_num";
    private static final String COL_SITE_ID = "site_id";
    private static final String COL_SITE_INSP_ID = "site_insp_id";
    private static final String COL_STATUS = "status";
    
    private static final int PARAM_DEL_ID = 0;
    
    private static final int PARAM_INS_SITE_ID = 0;
    private static final int PARAM_INS_ORD_NUM = 1;
    private static final int PARAM_INS_SITE_INSP_ID = 2;
    private static final int PARAM_INS_ASSET_ID = 3;
    private static final int PARAM_INS_STATUS = 4;
    private static final int PARAM_INS_OBJ_JSON = 5;
    private static final int PARAM_INS_ID = 6;
    
    private static final int PARAM_UPD_SITE_ID = 0;
    private static final int PARAM_UPD_ORD_NUM = 1;
    private static final int PARAM_UPD_SITE_INSP_ID = 2;
    private static final int PARAM_UPD_ASSET_ID = 3;
    private static final int PARAM_UPD_STATUS = 4;
    private static final int PARAM_UPD_OBJ_JSON = 5;
    private static final int PARAM_UPD_ID = 6;

    @Override
    protected String statementsMapsPath() {
        return STATEMENTS_MAPS;
    }
    
    @Override
    public boolean insert(TransmissionStructureInspection insp) throws DaoException {
        ensureExists(insp, "Transmission Structure Inspection is required");
        ensureExists(insp.getId(), "Transmission Structure Inspection ID is required");
        ensureExists(insp.getOrderNumber(), "Order number is required");
        ensureExists(insp.getSiteId(), "Transmission Line ID is required");
        
        TransmissionStructureInspection i = retrieve(insp.getId());
        if (i == null) {
            StatementBuilder stmt = new StatementBuilder(getStatementsMaps().getInsertStmt());
            stmt = stmt.setParameter(PARAM_INS_ASSET_ID, insp.getAssetId());
            stmt = stmt.setParameter(PARAM_INS_ID, insp.getId());
            stmt = stmt.setParameter(PARAM_INS_OBJ_JSON, serializeObject(insp));
            stmt = stmt.setParameter(PARAM_INS_ORD_NUM, insp.getOrderNumber());
            stmt = stmt.setParameter(PARAM_INS_SITE_ID, insp.getSiteId());
            stmt = stmt.setParameter(PARAM_INS_SITE_INSP_ID, insp.getSiteInspectionId());
            stmt = stmt.setParameter(PARAM_INS_STATUS, insp.getStatus());
            ResultSet rs = getSession().execute(stmt.build());
            return rs.wasApplied();
        } else {
            return false;
        }
    }

    @Override
    public boolean update(TransmissionStructureInspection insp) throws DaoException {
        ensureExists(insp, "Transmission Structure Inspection is required");
        ensureExists(insp.getId(), "Transmission Structure Inspection ID is required");
        ensureExists(insp.getOrderNumber(), "Order number is required");
        ensureExists(insp.getSiteId(), "Transmission Line ID is required");
        
        TransmissionStructureInspection i = retrieve(insp.getId());
        if (i == null) {        
            return false;
        } else {
            StatementBuilder stmt = new StatementBuilder(getStatementsMaps().getUpdateStmt());
            stmt = stmt.setParameter(PARAM_UPD_ASSET_ID, insp.getAssetId());
            stmt = stmt.setParameter(PARAM_UPD_ID, insp.getId());
            stmt = stmt.setParameter(PARAM_UPD_OBJ_JSON, serializeObject(insp));
            stmt = stmt.setParameter(PARAM_UPD_ORD_NUM, insp.getOrderNumber());
            stmt = stmt.setParameter(PARAM_UPD_SITE_ID, insp.getSiteId());
            stmt = stmt.setParameter(PARAM_UPD_SITE_INSP_ID, insp.getSiteInspectionId());
            stmt = stmt.setParameter(PARAM_UPD_STATUS, insp.getStatus());
            ResultSet rs = getSession().execute(stmt.build());
            return rs.wasApplied();
        }
    }

    @Override
    public boolean delete(String id) throws DaoException {
        ensureExists(id, "Transmission Structure Inspection ID is required");
        
        TransmissionStructureInspection i = retrieve(id);
        if (i == null) {
            return false;
        } else {
            Object[] values = new Object[1];
            values[PARAM_DEL_ID] = id;
            SimpleStatement stmt = new SimpleStatement(getStatementsMaps().getDeleteStmt(), values);
            ResultSet rs = getSession().execute(stmt);
            return rs.wasApplied();
        }
    }

    @Override
    public TransmissionStructureInspection retrieve(String id) throws DaoException {
        ensureExists(id, "Transmission Structure Inspection ID is required");
        StatementBuilder stmt = new StatementBuilder()
                .withSqlTemplate(getStatementsMaps().getSelectTemplate())
                .addEquals(COL_ID, id);
        return CollectionsUtilities.firstItemIn(selectObjects(TransmissionStructureInspection.class, stmt.build(), 0));
    }

    @Override
    public List<TransmissionStructureInspection> search(AssetInspectionSearchParams params) throws DaoException {
        StatementBuilder stmt = new StatementBuilder()
                .withSqlTemplate(getStatementsMaps().getSelectTemplate())
                .addEqualsConditionally(COL_ASSET_ID, params.getAssetId())
                .addEqualsConditionally(COL_ORD_NUM, params.getOrderNumber())
                .addEqualsConditionally(COL_SITE_ID, params.getSiteId())
                .addEqualsConditionally(COL_SITE_INSP_ID, params.getSiteInspectionId())
                .addEqualsConditionally(COL_STATUS, params.getStatus())
                ;
        if (stmt.hasWhereClause()) {
            return selectObjects(TransmissionStructureInspection.class, stmt.build(), 0);
        } else {
            throw new DaoException("Search parameters are required.");
        }
    }

}
