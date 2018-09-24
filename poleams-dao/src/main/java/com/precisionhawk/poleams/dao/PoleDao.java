package com.precisionhawk.poleams.dao;

import com.precisionhawk.poleams.bean.PoleSearchParameters;
import com.precisionhawk.poleams.domain.poledata.PoleData;
import java.util.List;

/**
 *
 * @author Philip A. Chapman
 */
public interface PoleDao {
        
    boolean insert(PoleData pole) throws DaoException;
    
    boolean update(PoleData pole) throws DaoException;

    boolean delete(String id) throws DaoException;
    
    PoleData retrieve(String id) throws DaoException;
    
    List<PoleData> search(PoleSearchParameters params) throws DaoException;
}
