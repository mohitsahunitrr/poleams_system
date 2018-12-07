package com.precisionhawk.poleams.dao;

import com.precisionhawk.ams.dao.DaoException;
import com.precisionhawk.poleams.domain.TransmissionStructure;
import com.precisionhawk.poleams.bean.TransmissionStructureSearchParams;
import java.util.List;

/**
 *
 * @author Philip A. Chapman
 */
public interface TransmissionStructureDao {
        
    boolean insert(TransmissionStructure structure) throws DaoException;
    
    boolean update(TransmissionStructure structure) throws DaoException;

    boolean delete(String id) throws DaoException;
    
    TransmissionStructure retrieve(String id) throws DaoException;
    
    List<TransmissionStructure> search(TransmissionStructureSearchParams params) throws DaoException;
}
