package uk.ac.kcl.testexecutionlisteners;


import org.springframework.test.context.TestContext;
import org.springframework.test.context.support.AbstractTestExecutionListener;
import uk.ac.kcl.it.SqlServerTestUtils;
import uk.ac.kcl.it.TestUtils;

/**
 * Created by rich on 03/06/16.
 */
public class SqlServerBasicTestExecutionListener extends AbstractTestExecutionListener {

    public SqlServerBasicTestExecutionListener(){}

    @Override
    public void beforeTestClass(TestContext testContext) {
        SqlServerTestUtils sqlServerTestUtils =
                testContext.getApplicationContext().getBean(SqlServerTestUtils.class);
        sqlServerTestUtils.createJobRepository();
        sqlServerTestUtils.createBasicInputTable();
        sqlServerTestUtils.createBasicOutputTable();
        TestUtils testUtils =
                testContext.getApplicationContext().getBean(TestUtils.class);
        testUtils.insertDataIntoBasicTable("dbo.tblInputDocs",true);
    }

}