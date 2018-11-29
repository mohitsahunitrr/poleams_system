package com.precisionhawk.poleams.dao;

import com.precisionhawk.ams.bean.AssetInspectionSearchParams;
import com.precisionhawk.ams.dao.DaoException;
import com.precisionhawk.poleams.domain.TransmissionStructureInspection;
import java.util.List;

/**
 *
 * @author Philip A. Chapman
 */
public interface TransmissionStructureInspectionDao {
        
    boolean insert(TransmissionStructureInspection inspection) throws DaoException;
    
    boolean update(TransmissionStructureInspection inspection) throws DaoException;

    boolean delete(String id) throws DaoException;
    
    TransmissionStructureInspection retrieve(String id) throws DaoException;
    
    List<TransmissionStructureInspection> search(AssetInspectionSearchParams params) throws DaoException;
}
