package com.precisionhawk.poleams.dao;

import com.precisionhawk.ams.dao.DaoException;
import com.precisionhawk.poleams.bean.TransmissionLineSearchParams;
import com.precisionhawk.poleams.domain.TransmissionLine;
import java.util.List;

/**
 *
 * @author Philip A. Chapman
 */
public interface TransmissionLineDao {
        
    boolean insert(TransmissionLine line) throws DaoException;
    
    boolean update(TransmissionLine line) throws DaoException;

    boolean delete(String id) throws DaoException;
    
    TransmissionLine retrieve(String id) throws DaoException;
    
    List<TransmissionLine> search(TransmissionLineSearchParams params) throws DaoException;

    List<TransmissionLine> retrieveAll() throws DaoException;
}
