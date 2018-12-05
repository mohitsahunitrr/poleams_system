package com.precisionhawk.poleamsv0dot0.dao;

import com.precisionhawk.poleamsv0dot0.bean.PoleInspectionSearchParameters;
import com.precisionhawk.poleamsv0dot0.domain.PoleInspection;
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
    
    List<PoleInspection> search(PoleInspectionSearchParameters params) throws DaoException;
}
