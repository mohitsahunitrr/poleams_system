package com.precisionhawk.poleamsv0dot0.dao;

import com.precisionhawk.poleamsv0dot0.bean.SubStationSearchParameters;
import com.precisionhawk.poleamsv0dot0.domain.SubStation;
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
