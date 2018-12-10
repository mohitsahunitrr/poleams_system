package com.precisionhawk.poleams.dao.cassandra;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.SimpleStatement;
import com.precisionhawk.ams.dao.DaoException;
import com.precisionhawk.ams.dao.cassandra.AbstractCassandraDao;
import com.precisionhawk.ams.dao.cassandra.StatementBuilder;
import com.precisionhawk.ams.util.CollectionsUtilities;
import com.precisionhawk.poleams.bean.TransmissionLineSearchParams;
import com.precisionhawk.poleams.dao.TransmissionLineDao;
import com.precisionhawk.poleams.domain.TransmissionLine;
import java.util.List;

/**
 *
 * @author pchapman
 */
public class TransmissionLineCassDao extends AbstractCassandraDao implements TransmissionLineDao {
    protected static final String STATEMENTS_MAPS = "com/precisionhawk/poleams/dao/cassandra/TransmissionLine_Statements.yaml";

    private static final String COL_LINE_NUM = "line_num";
    private static final String COL_ID = "id";
    private static final String COL_ORG_ID = "org_id";
    private static final String COL_SITE_NAME = "site_name";
    
    private static final int PARAM_DEL_ID = 0;
    
    private static final int PARAM_INS_ORG_ID = 0;
    private static final int PARAM_INS_SITE_NAME = 1;
    private static final int PARAM_INS_LINE_NUM = 2;
    private static final int PARAM_INS_OBJ_JSON = 3;
    private static final int PARAM_INS_ID = 4;
    
    private static final int PARAM_UPD_ORG_ID = 0;
    private static final int PARAM_UPD_SITE_NAME = 1;
    private static final int PARAM_UPD_LINE_NUM = 2;
    private static final int PARAM_UPD_OBJ_JSON = 3;
    private static final int PARAM_UPD_ID = 4;

    @Override
    protected String statementsMapsPath() {
        return STATEMENTS_MAPS;
    }

    @Override
    public boolean insert(TransmissionLine site) throws DaoException {
        ensureExists(site, "Transmission Line is required");
        ensureExists(site.getId(), "Transmission Line ID is required");
        ensureExists(site.getLineNumber(), "Transmission Line number is required");
        ensureExists(site.getOrganizationId(), "Organization ID is required");
        
        TransmissionLine l = retrieve(site.getId());
        if (l == null) {    
            StatementBuilder stmt = new StatementBuilder(getStatementsMaps().getInsertStmt());
            stmt = stmt.setParameter(PARAM_INS_LINE_NUM, site.getLineNumber());
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
    public boolean update(TransmissionLine site) throws DaoException {
        ensureExists(site, "Transmission Line is required");
        ensureExists(site.getId(), "Transmission Line ID is required");
        ensureExists(site.getLineNumber(), "Transmission Line number is required");
        ensureExists(site.getOrganizationId(), "Organization ID is required");
        
        TransmissionLine l = retrieve(site.getId());
        if (l == null) {
            return false;
        } else {
            StatementBuilder stmt = new StatementBuilder(getStatementsMaps().getUpdateStmt());
            stmt = stmt.setParameter(PARAM_UPD_LINE_NUM, site.getLineNumber());
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
        ensureExists(id, "Transmission Line ID is required");
        
        TransmissionLine l = retrieve(id);
        if (l == null) {
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
    public TransmissionLine retrieve(String id) throws DaoException {
        ensureExists(id, "Transmission Line ID is required");
        StatementBuilder stmt = new StatementBuilder()
                .withSqlTemplate(getStatementsMaps().getSelectTemplate())
                .addEquals(COL_ID, id);
        return CollectionsUtilities.firstItemIn(selectObjects(TransmissionLine.class, stmt.build(), 0));
    }

    @Override
    public List<TransmissionLine> search(TransmissionLineSearchParams params) throws DaoException {
        StatementBuilder stmt = new StatementBuilder()
                .withSqlTemplate(getStatementsMaps().getSelectTemplate())
                .addEqualsConditionally(COL_LINE_NUM, params.getLineNumber())
                .addEqualsConditionally(COL_SITE_NAME, params.getName())
                .addEqualsConditionally(COL_ORG_ID, params.getOrganizationId());
        if (stmt.hasWhereClause()) {
            return selectObjects(TransmissionLine.class, stmt.build(), 0);
        } else {
            throw new DaoException("Search parameters are required.");
        }
    }

    @Override
    public List<TransmissionLine> retrieveAll() throws DaoException {
        StatementBuilder stmt = new StatementBuilder()
                .withSqlTemplate(getStatementsMaps().getSelectTemplate());
        return selectObjects(TransmissionLine.class, stmt.build(), 0);
    }
    
}
