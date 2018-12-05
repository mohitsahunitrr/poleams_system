package com.precisionhawk.poleamsv0dot0.dao;

import com.precisionhawk.poleamsv0dot0.domain.Pole;
import com.precisionhawk.poleamsv0dot0.bean.PoleSearchParameters;
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
