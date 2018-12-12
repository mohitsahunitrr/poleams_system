/*
 * All rights reserved.
 */

package com.precisionhawk.poleams.dao;

import com.precisionhawk.ams.bean.InspectionEventSearchParams;
import com.precisionhawk.ams.dao.DaoException;
import com.precisionhawk.poleams.domain.InspectionEvent;
import java.util.List;

/**
 *
 * @author <a href="mailto:pchapman@pcsw.us">Philip A. Chapman</a>
 */
public interface InspectionEventDao {
    
    boolean delete(String id) throws DaoException;

    InspectionEvent retrieve(String id) throws DaoException;
    
    Long count(InspectionEventSearchParams searchBean) throws DaoException;
    
    List<InspectionEvent> search(InspectionEventSearchParams searchBean) throws DaoException;
    
    boolean insert(InspectionEvent report) throws DaoException;
    
    boolean update(InspectionEvent report) throws DaoException;

}
