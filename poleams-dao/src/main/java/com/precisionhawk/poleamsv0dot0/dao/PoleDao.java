package com.precisionhawk.poleams.dao;

import com.precisionhawk.poleams.domain.Pole;
import com.precisionhawk.poleams.bean.PoleSearchParameters;
import java.util.List;

/**
 *
 * @author Philip A. Chapman
 */
public interface PoleDao {
        
    boolean insert(Pole pole) throws DaoException;
    
    boolean update(Pole pole) throws DaoException;

    boolean delete(String id) throws DaoException;
    
    Pole retrieve(String id) throws DaoException;
    
    List<Pole> search(PoleSearchParameters params) throws DaoException;
}
