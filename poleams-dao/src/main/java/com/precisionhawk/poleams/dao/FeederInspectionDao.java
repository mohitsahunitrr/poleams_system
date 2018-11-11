package com.precisionhawk.poleams.dao;

import com.precisionhawk.ams.bean.SiteInspectionSearchParams;
import com.precisionhawk.ams.dao.DaoException;
import com.precisionhawk.poleams.domain.FeederInspection;
import java.util.List;

/**
 *
 * @author Philip A. Chapman
 */
public interface FeederInspectionDao {
        
    boolean insert(FeederInspection poleInspection) throws DaoException;
    
    boolean update(FeederInspection poleInspection) throws DaoException;

    boolean delete(String id) throws DaoException;
    
    FeederInspection retrieve(String id) throws DaoException;
    
    List<FeederInspection> search(SiteInspectionSearchParams params) throws DaoException;
}
