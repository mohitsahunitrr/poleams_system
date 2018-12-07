package com.precisionhawk.poleams.dao;

import com.precisionhawk.ams.bean.AssetInspectionSearchParams;
import com.precisionhawk.ams.dao.DaoException;
import com.precisionhawk.poleams.domain.PoleInspection;
import java.util.List;

/**
 *
 * @author Philip A. Chapman
 */
public interface PoleInspectionDao {
        
    boolean insert(PoleInspection poleInspection) throws DaoException;
    
    boolean update(PoleInspection poleInspection) throws DaoException;

    boolean delete(String id) throws DaoException;
    
    PoleInspection retrieve(String id) throws DaoException;
    
    List<PoleInspection> search(AssetInspectionSearchParams params) throws DaoException;
}
