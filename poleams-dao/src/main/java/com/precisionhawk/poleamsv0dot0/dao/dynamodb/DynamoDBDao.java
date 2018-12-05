package com.precisionhawk.poleamsv0dot0.dao.dynamodb;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.model.ListTablesResult;
import javax.annotation.PostConstruct;
import javax.inject.Inject;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Philip A. Chapman
 */
public abstract class DynamoDBDao {
    
    protected final Logger LOGGER = LoggerFactory.getLogger(getClass());
    
    @Inject protected AmazonDynamoDB client;
    protected DynamoDB db;
    @Inject protected ObjectMapper mapper;
    protected Table table;
    
    abstract protected void createTable();
    
    abstract protected String getTableName();
    
    @PostConstruct
    public void init() {
        db = new DynamoDB(client);
        ListTablesResult result = client.listTables();
        if (!result.getTableNames().contains(getTableName())) {
            createTable();
        }
        table = db.getTable(getTableName());
    }
}
