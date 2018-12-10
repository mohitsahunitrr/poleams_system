package com.precisionhawk.poleams.dao.cassandra;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.SimpleStatement;
import com.precisionhawk.ams.dao.DaoException;
import com.precisionhawk.ams.dao.cassandra.AbstractCassandraDao;
import com.precisionhawk.ams.dao.cassandra.StatementBuilder;
import com.precisionhawk.ams.util.CollectionsUtilities;
import com.precisionhawk.poleams.bean.PoleSearchParams;
import com.precisionhawk.poleams.dao.PoleDao;
import com.precisionhawk.poleams.domain.Pole;
import java.util.List;

/**
 *
 * @author pchapman
 */
public class PoleCassDao extends AbstractCassandraDao implements PoleDao {
    protected static final String STATEMENTS_MAPS = "com/precisionhawk/poleams/dao/cassandra/Pole_Statements.yaml";

    private static final String COL_ID = "id";
    private static final String COL_NAME = "name";
    private static final String COL_SERIAL_NUM = "serial_num";
    private static final String COL_SITE_ID = "site_id";
    private static final String COL_TYPE = "type";
    private static final String COL_UTILITY_ID = "utility_id";
    
    private static final int PARAM_DEL_ID = 0;
    
    private static final int PARAM_INS_SITE_ID = 0;
    private static final int PARAM_INS_NAME = 1;
    private static final int PARAM_INS_SERIAL_NUM = 2;
    private static final int PARAM_INS_TYPE = 3;
    private static final int PARAM_INS_UTILITY_ID = 4;
    private static final int PARAM_INS_OBJ_JSON = 5;
    private static final int PARAM_INS_ID = 6;
    
    private static final int PARAM_UPD_SITE_ID = 0;
    private static final int PARAM_UPD_NAME = 1;
    private static final int PARAM_UPD_SERIAL_NUM = 2;
    private static final int PARAM_UPD_TYPE = 3;
    private static final int PARAM_UPD_UTILITY_ID = 4;
    private static final int PARAM_UPD_OBJ_JSON = 5;
    private static final int PARAM_UPD_ID = 6;

    @Override
    protected String statementsMapsPath() {
        return STATEMENTS_MAPS;
    }
    
    @Override
    public boolean insert(Pole pole) throws DaoException {
        ensureExists(pole, "Pole is required");
        ensureExists(pole.getId(), "Pole ID is required");
        ensureExists(pole.getSiteId(), "Site ID is required");
        
        Pole p = retrieve(pole.getId());
        if (p == null) {
            StatementBuilder stmt = new StatementBuilder(getStatementsMaps().getInsertStmt());
            stmt = stmt.setParameter(PARAM_INS_ID, pole.getId());
            stmt = stmt.setParameter(PARAM_INS_NAME, pole.getName());
            stmt = stmt.setParameter(PARAM_INS_OBJ_JSON, serializeObject(pole));
            stmt = stmt.setParameter(PARAM_INS_SERIAL_NUM, pole.getSerialNumber());
            stmt = stmt.setParameter(PARAM_INS_SITE_ID, pole.getSiteId());
            stmt = stmt.setParameter(PARAM_INS_TYPE, pole.getType());
            stmt = stmt.setParameter(PARAM_INS_UTILITY_ID, pole.getUtilityId());
            ResultSet rs = getSession().execute(stmt.build());
            return rs.wasApplied();
        } else {
            return false;
        }
    }

    @Override
    public boolean update(Pole pole) throws DaoException {
        ensureExists(pole, "Pole is required");
        ensureExists(pole.getId(), "Pole ID is required");
        ensureExists(pole.getSiteId(), "Site ID is required");
        
        Pole p = retrieve(pole.getId());
        if (p == null) {
            return false;
        } else {
            StatementBuilder stmt = new StatementBuilder(getStatementsMaps().getUpdateStmt());
            stmt = stmt.setParameter(PARAM_UPD_ID, pole.getId());
            stmt = stmt.setParameter(PARAM_UPD_NAME, pole.getName());
            stmt = stmt.setParameter(PARAM_UPD_OBJ_JSON, serializeObject(pole));
            stmt = stmt.setParameter(PARAM_UPD_SERIAL_NUM, pole.getSerialNumber());
            stmt = stmt.setParameter(PARAM_UPD_SITE_ID, pole.getSiteId());
            stmt = stmt.setParameter(PARAM_UPD_TYPE, pole.getType());
            stmt = stmt.setParameter(PARAM_UPD_UTILITY_ID, pole.getUtilityId());
            ResultSet rs = getSession().execute(stmt.build());
            return rs.wasApplied();
        }
    }

    @Override
    public boolean delete(String id) throws DaoException {
        ensureExists(id, "Pole ID is required");
        
        Pole p = retrieve(id);
        if (p == null) {
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
    public Pole retrieve(String id) throws DaoException {
        ensureExists(id, "Pole ID is required");
        StatementBuilder stmt = new StatementBuilder()
                .withSqlTemplate(getStatementsMaps().getSelectTemplate())
                .addEquals(COL_ID, id);
        return CollectionsUtilities.firstItemIn(selectObjects(Pole.class, stmt.build(), 0));
    }

    @Override
    public List<Pole> search(PoleSearchParams params) throws DaoException {
        StatementBuilder stmt = new StatementBuilder()
                .withSqlTemplate(getStatementsMaps().getSelectTemplate())
                .addEqualsConditionally(COL_NAME, params.getName())
                .addEqualsConditionally(COL_SERIAL_NUM, params.getSerialNumber())
                .addEqualsConditionally(COL_SITE_ID, params.getSiteId())
                .addEqualsConditionally(COL_TYPE, params.getType())
                .addEqualsConditionally(COL_UTILITY_ID, params.getUtilityId())
                ;
        if (stmt.hasWhereClause()) {
            return selectObjects(Pole.class, stmt.build(), 0);
        } else {
            throw new DaoException("Search parameters are required.");
        }
    }

}
