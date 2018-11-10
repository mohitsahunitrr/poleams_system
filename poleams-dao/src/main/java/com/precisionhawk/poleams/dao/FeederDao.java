package com.precisionhawk.poleams.dao;

import com.precisionhawk.ams.dao.DaoException;
import com.precisionhawk.poleams.bean.FeederSearchParams;
import com.precisionhawk.poleams.domain.Feeder;
import java.util.List;

/**
 *
 * @author Philip A. Chapman
 */
public interface FeederDao {
        
    boolean insert(Feeder substation) throws DaoException;
    
    boolean update(Feeder substation) throws DaoException;

    boolean delete(String id) throws DaoException;
    
    Feeder retrieve(String id) throws DaoException;
    
    List<Feeder> search(FeederSearchParams params) throws DaoException;

    List<Feeder> retrieveAll() throws DaoException;
}
