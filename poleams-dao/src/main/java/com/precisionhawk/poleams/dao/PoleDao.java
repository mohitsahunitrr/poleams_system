package com.precisionhawk.poleams.dao;

import com.precisionhawk.ams.dao.DaoException;
import com.precisionhawk.poleams.domain.Pole;
import com.precisionhawk.poleams.bean.PoleSearchParams;
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
    
    List<Pole> search(PoleSearchParams params) throws DaoException;
}
