package com.precisionhawk.poleams.dao;

import com.precisionhawk.ams.dao.DaoException;
import com.precisionhawk.ams.domain.Component;
import com.precisionhawk.ams.bean.ComponentSearchParams;
import java.util.List;

/**
 *
 * @author Philip A. Chapman
 */
public interface ComponentDao {
    
    long count(ComponentSearchParams params) throws DaoException;
        
    boolean insert(Component comp) throws DaoException;
    
    boolean update(Component comp) throws DaoException;

    boolean delete(String id) throws DaoException;
    
    Component retrieve(String id) throws DaoException;
    
    List<Component> search(ComponentSearchParams params) throws DaoException;
}
