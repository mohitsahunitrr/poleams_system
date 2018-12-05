package com.precisionhawk.poleams.dao;

import com.precisionhawk.poleams.bean.SubStationSearchParameters;
import com.precisionhawk.poleams.domain.SubStation;
import java.util.List;

/**
 *
 * @author Philip A. Chapman
 */
public interface SubStationDao {
        
    boolean insert(SubStation substation) throws DaoException;
    
    boolean update(SubStation substation) throws DaoException;

    boolean delete(String id) throws DaoException;
    
    SubStation retrieve(String id) throws DaoException;
    
    List<SubStation> search(SubStationSearchParameters params) throws DaoException;

    List<SubStation> retrieveAll() throws DaoException;
}
