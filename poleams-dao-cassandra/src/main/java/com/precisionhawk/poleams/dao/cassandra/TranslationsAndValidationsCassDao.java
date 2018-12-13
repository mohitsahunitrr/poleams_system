package com.precisionhawk.poleams.dao.cassandra;

import com.precisionhawk.ams.bean.orgconfig.OrgFieldTranslations;
import com.precisionhawk.ams.bean.orgconfig.OrgFieldValidations;
import com.precisionhawk.ams.dao.DaoException;
import com.precisionhawk.ams.dao.TranslationsAndValidationsDao;
import com.precisionhawk.ams.dao.cassandra.AbstractCassandraDao;
import java.util.List;

/**
 *
 * @author pchapman
 */
public class TranslationsAndValidationsCassDao extends AbstractCassandraDao implements TranslationsAndValidationsDao {

    //TODO:
    
    @Override
    protected String statementsMapsPath() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public List<OrgFieldTranslations> loadOrgTranslations(String orgId, String lang, String country) throws DaoException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean storeOrgTranslations(OrgFieldTranslations translations) throws DaoException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public OrgFieldValidations loadOrgValidations(String orgId) throws DaoException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean storeOrgValidations(OrgFieldValidations translations) throws DaoException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
}
