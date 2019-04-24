package com.precisionhawk.poleams.dao;

import com.precisionhawk.ams.bean.ComponentInspectionSearchParams;
import com.precisionhawk.ams.dao.DaoException;
import com.precisionhawk.ams.domain.ComponentInspection;
import java.util.List;

/**
 *
 * @author Philip A. Chapman
 */
public interface ComponentInspectionDao {
        
    boolean insert(ComponentInspection poleInspection) throws DaoException;
    
    boolean update(ComponentInspection poleInspection) throws DaoException;

    boolean delete(String id) throws DaoException;
    
    ComponentInspection retrieve(String id) throws DaoException;
    
    List<ComponentInspection> search(ComponentInspectionSearchParams params) throws DaoException;
}
