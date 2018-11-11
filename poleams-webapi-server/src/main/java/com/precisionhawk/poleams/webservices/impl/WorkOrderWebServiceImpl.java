package com.precisionhawk.poleams.webservices.impl;

import com.precisionhawk.ams.bean.WorkOrderSearchParams;
import com.precisionhawk.ams.dao.DaoException;
import com.precisionhawk.ams.dao.WorkOrderDao;
import com.precisionhawk.ams.domain.WorkOrder;
import com.precisionhawk.ams.webservices.AbstractWebService;
import com.precisionhawk.poleams.webservices.WorkOrderWebService;
import java.util.List;
import java.util.UUID;
import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.InternalServerErrorException;

/**
 *
 * @author pchapman
 */
@Named
public class WorkOrderWebServiceImpl extends AbstractWebService implements WorkOrderWebService {
    
    @Inject private WorkOrderDao dao;

    @Override
    public WorkOrder retrieveById(String authToken, String orderNumber) {
        ensureExists(orderNumber, "Work order number is required.");
        try {
            return dao.retrieveById(orderNumber);
        } catch (DaoException ex) {
            throw new InternalServerErrorException(String.format("Error retrieving work order for order number %s.", orderNumber), ex);
        }
    }

    @Override
    public List<WorkOrder> search(String authToken, WorkOrderSearchParams searchBean) {
        ensureExists(searchBean, "Search parameters are required.");
        if (searchBean.hasCriteria()) {
            try {
                return dao.search(searchBean);
            } catch (DaoException ex) {
                throw new InternalServerErrorException("Error searching for work orders.");
            }
        } else {
            throw new BadRequestException("Search parameters are required.");
        }
    }

    @Override
    public WorkOrder create(String authToken, WorkOrder workOrder) {
        ensureExists(workOrder, "Work order is required.");
        if (workOrder.getOrderNumber() == null) {
            workOrder.setOrderNumber(UUID.randomUUID().toString().split("-")[0]);
        }
        try {
            if (dao.insert(workOrder)) {
                return workOrder;
            } else {
                throw new BadRequestException(String.format("The work order %s already exists.", workOrder.getOrderNumber()));
            }
        } catch (DaoException ex) {
            throw new InternalServerErrorException("Error storing new work order", ex);
        }
    }

    @Override
    public void delete(String authToken, String orderNumber) {
        ensureExists(orderNumber, "Work order number is required.");
        try {
            dao.delete(orderNumber);
        } catch (DaoException ex) {
            throw new InternalServerErrorException(String.format("Error deleting work order for order number %s.", orderNumber), ex);
        }
    }

    @Override
    public void update(String authToken, WorkOrder workOrder) {
        ensureExists(workOrder, "Work order is required.");
        ensureExists(workOrder.getOrderNumber(), "Work order number is required.");
        if (workOrder.getOrderNumber() == null) {
            workOrder.setOrderNumber(UUID.randomUUID().toString().split("-")[0]);
        }
        try {
            if (!dao.update(workOrder)) {
                throw new BadRequestException(String.format("The work order %s already exists.", workOrder.getOrderNumber()));
            }
        } catch (DaoException ex) {
            throw new InternalServerErrorException("Error storing new work order", ex);
        }
    }    
}
