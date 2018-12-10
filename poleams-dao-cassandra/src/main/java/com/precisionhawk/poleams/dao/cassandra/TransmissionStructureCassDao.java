package com.precisionhawk.poleams.dao.cassandra;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.SimpleStatement;
import com.precisionhawk.ams.dao.DaoException;
import com.precisionhawk.ams.dao.cassandra.AbstractCassandraDao;
import com.precisionhawk.ams.dao.cassandra.StatementBuilder;
import com.precisionhawk.ams.util.CollectionsUtilities;
import com.precisionhawk.poleams.bean.TransmissionStructureSearchParams;
import com.precisionhawk.poleams.dao.TransmissionStructureDao;
import com.precisionhawk.poleams.domain.TransmissionStructure;
import java.util.List;

/**
 *
 * @author pchapman
 */
public class TransmissionStructureCassDao extends AbstractCassandraDao implements TransmissionStructureDao {
    protected static final String STATEMENTS_MAPS = "com/precisionhawk/poleams/dao/cassandra/TransmissionStructure_Statements.yaml";

    private static final String COL_ID = "id";
    private static final String COL_NAME = "name";
    private static final String COL_SERIAL_NUM = "serial_num";
    private static final String COL_SITE_ID = "site_id";
    private static final String COL_TYPE = "type";
    private static final String COL_STRUCT_NUM = "struct_num";
    
    private static final int PARAM_DEL_ID = 0;
    
    private static final int PARAM_INS_SITE_ID = 0;
    private static final int PARAM_INS_NAME = 1;
    private static final int PARAM_INS_SERIAL_NUM = 2;
    private static final int PARAM_INS_TYPE = 3;
    private static final int PARAM_INS_STRUCT_NUM = 4;
    private static final int PARAM_INS_OBJ_JSON = 5;
    private static final int PARAM_INS_ID = 6;
    
    private static final int PARAM_UPD_SITE_ID = 0;
    private static final int PARAM_UPD_NAME = 1;
    private static final int PARAM_UPD_SERIAL_NUM = 2;
    private static final int PARAM_UPD_TYPE = 3;
    private static final int PARAM_UPD_STRUCT_NUM = 4;
    private static final int PARAM_UPD_OBJ_JSON = 5;
    private static final int PARAM_UPD_ID = 6;

    @Override
    protected String statementsMapsPath() {
        return STATEMENTS_MAPS;
    }
    
    @Override
    public boolean insert(TransmissionStructure struct) throws DaoException {
        ensureExists(struct, "Transmission structure is required");
        ensureExists(struct.getId(), "Transmission structure ID is required");
        ensureExists(struct.getSiteId(), "Site ID is required");
        
        TransmissionStructure s = retrieve(struct.getId());
        if (s == null) {
            StatementBuilder stmt = new StatementBuilder(getStatementsMaps().getInsertStmt());
            stmt = stmt.setParameter(PARAM_INS_ID, struct.getId());
            stmt = stmt.setParameter(PARAM_INS_NAME, struct.getName());
            stmt = stmt.setParameter(PARAM_INS_OBJ_JSON, serializeObject(struct));
            stmt = stmt.setParameter(PARAM_INS_SERIAL_NUM, struct.getSerialNumber());
            stmt = stmt.setParameter(PARAM_INS_SITE_ID, struct.getSiteId());
            stmt = stmt.setParameter(PARAM_INS_TYPE, struct.getType());
            stmt = stmt.setParameter(PARAM_INS_STRUCT_NUM, struct.getStructureNumber());
            ResultSet rs = getSession().execute(stmt.build());
            return rs.wasApplied();
        } else {
            return false;
        }
    }

    @Override
    public boolean update(TransmissionStructure struct) throws DaoException {
        ensureExists(struct, "Transmission structure is required");
        ensureExists(struct.getId(), "Transmission structure ID is required");
        ensureExists(struct.getSiteId(), "Site ID is required");
        
        TransmissionStructure s = retrieve(struct.getId());
        if (s == null) {
            return false;
        } else {
            StatementBuilder stmt = new StatementBuilder(getStatementsMaps().getUpdateStmt());
            stmt = stmt.setParameter(PARAM_UPD_ID, struct.getId());
            stmt = stmt.setParameter(PARAM_UPD_NAME, struct.getName());
            stmt = stmt.setParameter(PARAM_UPD_OBJ_JSON, serializeObject(struct));
            stmt = stmt.setParameter(PARAM_UPD_SERIAL_NUM, struct.getSerialNumber());
            stmt = stmt.setParameter(PARAM_UPD_SITE_ID, struct.getSiteId());
            stmt = stmt.setParameter(PARAM_UPD_TYPE, struct.getType());
            stmt = stmt.setParameter(PARAM_UPD_STRUCT_NUM, struct.getStructureNumber());
            ResultSet rs = getSession().execute(stmt.build());
            return rs.wasApplied();
        }
    }

    @Override
    public boolean delete(String id) throws DaoException {
        ensureExists(id, "Transmission structure ID is required");
        
        TransmissionStructure s = retrieve(id);
        if (s == null) {
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
    public TransmissionStructure retrieve(String id) throws DaoException {
        ensureExists(id, "Transmission structure ID is required");
        StatementBuilder stmt = new StatementBuilder()
                .withSqlTemplate(getStatementsMaps().getSelectTemplate())
                .addEquals(COL_ID, id);
        return CollectionsUtilities.firstItemIn(selectObjects(TransmissionStructure.class, stmt.build(), 0));
    }

    @Override
    public List<TransmissionStructure> search(TransmissionStructureSearchParams params) throws DaoException {
        StatementBuilder stmt = new StatementBuilder()
                .withSqlTemplate(getStatementsMaps().getSelectTemplate())
                .addEqualsConditionally(COL_NAME, params.getName())
                .addEqualsConditionally(COL_SERIAL_NUM, params.getSerialNumber())
                .addEqualsConditionally(COL_SITE_ID, params.getSiteId())
                .addEqualsConditionally(COL_TYPE, params.getType())
                .addEqualsConditionally(COL_STRUCT_NUM, params.getStructureNumber())
                ;
        if (stmt.hasWhereClause()) {
            return selectObjects(TransmissionStructure.class, stmt.build(), 0);
        } else {
            throw new DaoException("Search parameters are required.");
        }
    }

}
