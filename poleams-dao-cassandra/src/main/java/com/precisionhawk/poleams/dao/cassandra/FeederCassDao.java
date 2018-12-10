package com.precisionhawk.poleams.dao.cassandra;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.SimpleStatement;
import com.precisionhawk.ams.dao.DaoException;
import com.precisionhawk.ams.dao.cassandra.AbstractCassandraDao;
import com.precisionhawk.ams.dao.cassandra.StatementBuilder;
import com.precisionhawk.ams.util.CollectionsUtilities;
import com.precisionhawk.poleams.bean.FeederSearchParams;
import com.precisionhawk.poleams.dao.FeederDao;
import com.precisionhawk.poleams.domain.Feeder;
import java.util.List;

/**
 *
 * @author pchapman
 */
public class FeederCassDao extends AbstractCassandraDao implements FeederDao {
    protected static final String STATEMENTS_MAPS = "com/precisionhawk/poleams/dao/cassandra/Feeder_Statements.yaml";

    private static final String COL_FEEDER_NUM = "feeder_num";
    private static final String COL_ID = "id";
    private static final String COL_ORG_ID = "org_id";
    private static final String COL_SITE_NAME = "site_name";
    
    private static final int PARAM_DEL_ID = 0;
    
    private static final int PARAM_INS_ORG_ID = 0;
    private static final int PARAM_INS_SITE_NAME = 1;
    private static final int PARAM_INS_FEEDER_NUM = 2;
    private static final int PARAM_INS_OBJ_JSON = 3;
    private static final int PARAM_INS_ID = 4;
    
    private static final int PARAM_UPD_ORG_ID = 0;
    private static final int PARAM_UPD_SITE_NAME = 1;
    private static final int PARAM_UPD_FEEDER_NUM = 2;
    private static final int PARAM_UPD_OBJ_JSON = 3;
    private static final int PARAM_UPD_ID = 4;

    @Override
    protected String statementsMapsPath() {
        return STATEMENTS_MAPS;
    }

    @Override
    public boolean insert(Feeder site) throws DaoException {
        ensureExists(site, "Feeder is required");
        ensureExists(site.getId(), "Feeder ID is required");
        ensureExists(site.getFeederNumber(), "Feeder number is required");
        ensureExists(site.getName(), "Feeder name is required");
        ensureExists(site.getOrganizationId(), "Organization ID is required");
        
        Feeder f = retrieve(site.getId());
        if (f == null) {    
            StatementBuilder stmt = new StatementBuilder(getStatementsMaps().getInsertStmt());
            stmt = stmt.setParameter(PARAM_INS_FEEDER_NUM, site.getFeederNumber());
            stmt = stmt.setParameter(PARAM_INS_ID, site.getId());
            stmt = stmt.setParameter(PARAM_INS_OBJ_JSON, serializeObject(site));
            stmt = stmt.setParameter(PARAM_INS_ORG_ID, site.getOrganizationId());
            stmt = stmt.setParameter(PARAM_INS_SITE_NAME, site.getName());
            ResultSet rs = getSession().execute(stmt.build());
            return rs.wasApplied();
        } else {
            return false;
        }
    }

    @Override
    public boolean update(Feeder site) throws DaoException {
        ensureExists(site, "Feeder is required");
        ensureExists(site.getId(), "Feeder ID is required");
        ensureExists(site.getFeederNumber(), "Feeder number is required");
        ensureExists(site.getName(), "Feeder name is required");
        ensureExists(site.getOrganizationId(), "Organization ID is required");
        
        Feeder f = retrieve(site.getId());
        if (f == null) {
            return false;
        } else {
            StatementBuilder stmt = new StatementBuilder(getStatementsMaps().getUpdateStmt());
            stmt = stmt.setParameter(PARAM_UPD_FEEDER_NUM, site.getFeederNumber());
            stmt = stmt.setParameter(PARAM_UPD_ID, site.getId());
            stmt = stmt.setParameter(PARAM_UPD_OBJ_JSON, serializeObject(site));
            stmt = stmt.setParameter(PARAM_UPD_ORG_ID, site.getId());
            stmt = stmt.setParameter(PARAM_UPD_SITE_NAME, site.getName());
            ResultSet rs = getSession().execute(stmt.build());
            return rs.wasApplied();
        }
    }

    @Override
    public boolean delete(String id) throws DaoException {
        ensureExists(id, "Feeder ID is required");
        
        Feeder f = retrieve(id);
        if (f == null) {
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
    public Feeder retrieve(String id) throws DaoException {
        ensureExists(id, "Feeder ID is required");
        StatementBuilder stmt = new StatementBuilder()
                .withSqlTemplate(getStatementsMaps().getSelectTemplate())
                .addEquals(COL_ID, id);
        return CollectionsUtilities.firstItemIn(selectObjects(Feeder.class, stmt.build(), 0));
    }

    @Override
    public List<Feeder> search(FeederSearchParams params) throws DaoException {
        StatementBuilder stmt = new StatementBuilder()
                .withSqlTemplate(getStatementsMaps().getSelectTemplate())
                .addEqualsConditionally(COL_FEEDER_NUM, params.getFeederNumber())
                .addEqualsConditionally(COL_SITE_NAME, params.getName())
                .addEqualsConditionally(COL_ORG_ID, params.getOrganizationId());
        if (stmt.hasWhereClause()) {
            return selectObjects(Feeder.class, stmt.build(), 0);
        } else {
            throw new DaoException("Search parameters are required.");
        }
    }

    @Override
    public List<Feeder> retrieveAll() throws DaoException {
        StatementBuilder stmt = new StatementBuilder()
                .withSqlTemplate(getStatementsMaps().getSelectTemplate());
        return selectObjects(Feeder.class, stmt.build(), 0);
    }
    
}
