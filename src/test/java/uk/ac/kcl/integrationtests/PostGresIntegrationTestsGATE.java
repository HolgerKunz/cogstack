/* 
 * Copyright 2016 King's College London, Richard Jackson <richgjackson@gmail.com>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.kcl.integrationtests;

import uk.ac.kcl.batch.JobConfiguration;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import javax.sql.DataSource;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.UnexpectedJobExecutionException;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.launch.JobParametersNotFoundException;
import org.springframework.batch.core.launch.NoSuchJobException;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import uk.ac.kcl.batch.BatchConfigurer;
import uk.ac.kcl.batch.io.DbLineFixerIOConfiguration;
import uk.ac.kcl.batch.GateConfiguration;
import uk.ac.kcl.batch.io.GateIOConfiguration;

/**
 *
 * @author rich
 */
@RunWith(SpringJUnit4ClassRunner.class)
@TestPropertySource("classpath:postgres_test_config_gate.properties")
@ContextConfiguration(classes = {
    JobConfiguration.class,
    BatchConfigurer.class,
    GateConfiguration.class,
    GateIOConfiguration.class},
        loader = AnnotationConfigContextLoader.class)
public class PostGresIntegrationTestsGATE {

    final static Logger logger = Logger.getLogger(PostGresIntegrationTestsGATE.class);

    @Autowired
    @Qualifier("sourceDataSource")
    public DataSource sourceDataSource;

    @Autowired
    @Qualifier("targetDataSource")
    public DataSource jdbcTargetDocumentFinder;

    private JdbcTemplate sourceTemplate;
    private JdbcTemplate targetTemplate;
    private ResourceDatabasePopulator rdp = new ResourceDatabasePopulator();
    private Resource dropTablesResource;
    private Resource makeTablesResource;


    @Before
    public void initTemplates() {
        sourceTemplate = new JdbcTemplate(sourceDataSource);
        targetTemplate = new JdbcTemplate(jdbcTargetDocumentFinder);
    }

    @After
    public void dropDb() {
        //sourceTemplate.execute("DROP TABLE tblInputDocs");
        //targetTemplate.execute("DROP TABLE tblOutputDocs");
    }


    @Autowired
    JobOperator jobOperator;

    @Ignore
    @Test
    public void postgresGatePipelineTest() {
        initPostgresGateTable();
        initPostGresJobRepository();
        insertTestXHTMLForGate(sourceDataSource, false);

        try {
            jobOperator.startNextInstance("gateJob");
        } catch (NoSuchJobException | JobParametersNotFoundException | JobRestartException | JobExecutionAlreadyRunningException | JobInstanceAlreadyCompleteException | UnexpectedJobExecutionException | JobParametersInvalidException ex) {
            java.util.logging.Logger.getLogger(PostGresIntegrationTestsGATE.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    
    private void initPostgresGateTable() {
////        for postgres
        sourceTemplate.execute("DROP TABLE IF EXISTS tblInputDocs");
        sourceTemplate.execute("CREATE TABLE tblInputDocs"
                + "( ID  SERIAL PRIMARY KEY"
                + ", srcColumnFieldName text "
                + ", srcTableName text "
                + ", primaryKeyFieldName text "
                + ", primaryKeyFieldValue text "
                + ", binaryFieldName text "
                + ", updateTime text "
                + ", xhtml text )");

        targetTemplate.execute("DROP TABLE IF EXISTS tblOutputDocs");
        targetTemplate.execute("CREATE TABLE tblOutputDocs "
                + "( ID  SERIAL PRIMARY KEY"
                + ", srcColumnFieldName text "
                + ", srcTableName text "
                + ", primaryKeyFieldName text "
                + ", primaryKeyFieldValue text "
                + ", binaryFieldName text "
                + ", updateTime text "
                + ", gatejson text )");


    }
    
    
    
    private void initPostGresJobRepository(){
        dropTablesResource = new ClassPathResource("org/springframework/batch/core/schema-drop-postgresql.sql");
        makeTablesResource = new ClassPathResource("org/springframework/batch/core/schema-postgresql.sql");
        rdp.addScript(dropTablesResource);
        rdp.addScript(makeTablesResource);
        rdp.execute(jdbcTargetDocumentFinder);        
    }
      

    
    private void insertTestXHTMLForGate(DataSource ds, boolean includeGateBreaker) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(ds);
        int docCount = 10;
        byte[] bytes = null;
        try {
            bytes = IOUtils.toByteArray(getClass().getClassLoader().getResourceAsStream("xhtml_test"));
        } catch (IOException ex) {
            java.util.logging.Logger.getLogger(PostGresIntegrationTestsGATE.class.getName()).log(Level.SEVERE, null, ex);
        }
        String xhtmlString = new String(bytes, StandardCharsets.UTF_8);

        String sql = "INSERT INTO tblInputDocs "
                + "( srcColumnFieldName"
                + ", srcTableName"
                + ", primaryKeyFieldName"
                + ", primaryKeyFieldValue"
                + ", updateTime"
                + ", xhtml"
                + ") VALUES (?,?,?,?,?,?)";
        for (int ii = 0; ii < docCount; ii++) {
            jdbcTemplate.update(sql, "fictionalColumnFieldName","fictionalTableName","fictionalPrimaryKeyFieldName", ii,null,  xhtmlString);
            
        }
        //see what happens with a really long document...
        if (includeGateBreaker) {
            try {
                bytes = IOUtils.toByteArray(getClass().getClassLoader().getResourceAsStream("gate_breaker.txt"));
                xhtmlString = new String(bytes, StandardCharsets.UTF_8);
                jdbcTemplate.update(sql, "fictionalColumnFieldName", "fictionalTableName", "fictionalPrimaryKeyFieldName", docCount, null, xhtmlString);
            } catch (IOException ex) {
                java.util.logging.Logger.getLogger(PostGresIntegrationTestsGATE.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
}
