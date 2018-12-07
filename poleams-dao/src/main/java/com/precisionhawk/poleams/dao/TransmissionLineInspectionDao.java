package com.precisionhawk.poleams.dao;

import com.precisionhawk.ams.bean.SiteInspectionSearchParams;
import com.precisionhawk.ams.dao.DaoException;
import com.precisionhawk.poleams.domain.TransmissionLineInspection;
import java.util.List;

/**
 *
 * @author Philip A. Chapman
 */
public interface TransmissionLineInspectionDao {
        
    boolean insert(TransmissionLineInspection inspection) throws DaoException;
    
    boolean update(TransmissionLineInspection inspection) throws DaoException;

    boolean delete(String id) throws DaoException;
    
    TransmissionLineInspection retrieve(String id) throws DaoException;
    
    List<TransmissionLineInspection> search(SiteInspectionSearchParams params) throws DaoException;
}
