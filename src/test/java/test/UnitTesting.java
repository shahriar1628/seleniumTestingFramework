package test;

import org.hamcrest.Matcher;
import org.junit.Test;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import test.coreModule.*;
import test.driver.DriverFactory;
import test.keywordScripts.UIBase;

import test.objectLocator.ObjectLocatorDataStorage;
import test.objectLocator.OrRead;
import test.objectLocator.WebObjectSearchType;
import test.utility.LogMessage;
import test.utility.PropertyConfig;
import test.utility.ReadExcel;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import java.lang.System.* ;
import static org.junit.Assert.*;
import org.hamcrest.collection.IsMapContaining;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static test.utility.PropertyConfig.*;

public class UnitTesting {

    @Test
    public void readExcelsheetTest(){
        ClassLoader classLoader = getClass().getClassLoader();
        ReadExcel readExcel = new ReadExcel(classLoader.getResource("modules/sample.xlsx").getPath());
        List<Map> records = readExcel.read("TC001_TC050");
        assertTrue(records.size() > 1 );
        Map  data = records.get(0);
        assertTrue(data.containsKey(TC_ID));
        assertTrue("001".equals( (String) data.get(PropertyConfig.TC_ID)));

    }
    @Test
    public void orReadTesting(){
        OrRead orRead  = new OrRead("Common.Login.txtUserName");
        Map records = orRead.getOrFromSheet();
        assertTrue(records.size() > 1 );
        int size = records.size();
        assertTrue(records.containsKey("Common.Login.txtUserName") );
        assertTrue(records.containsKey("Common.Login.txtPassword") );
         orRead  = new OrRead("Common.Login.txtUserNameFake");
         records = orRead.getOrFromSheet();
        assertTrue(records.size() == size);
        assertFalse(records.containsKey("Common.Login.txtUserNameFake"));
    }
    @Test
    public void objectLocatorStorageTesting(){
        ObjectLocatorDataStorage objectLocatorDataStorage = new ObjectLocatorDataStorage();
        try {

            long start = System.currentTimeMillis();
            Map record = objectLocatorDataStorage.getObjectLocator("Common.Login.txtUserName");
            long finish = System.currentTimeMillis();
            long timeElapsed1 = finish - start;
            start = System.currentTimeMillis();
             record = objectLocatorDataStorage.getObjectLocator("Common.Login.txtUserName");
             finish = System.currentTimeMillis();
            long timeElapsed2 = finish - start;
            assertTrue(timeElapsed2 < timeElapsed1);
            start = System.currentTimeMillis();
            record = objectLocatorDataStorage.getObjectLocator("Common.Login.txtPassword");
            finish = System.currentTimeMillis();
            long timeElapsed3 = finish - start;
            assertTrue(timeElapsed3 == timeElapsed2);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    @Test
    public void testingInvokeMethod() {

        WebDriver driver = null ;
        ExecuteTests executeTests = new ExecuteTests(driver);
         String test = "test" ;
        Object[] object = new Object[]{test};
        LogMessage logMessage = executeTests.invokeMethod("UIBase","test_click",1,object);
        assertFalse(!logMessage.isPassed().booleanValue());
        assertTrue(logMessage.getLogMessage().equals(test));
    }
    @Test
    public void testinCreateTestPlan() {
        MainController mc = new MainController();
        TestPlan testPlan =  mc.createTestPlan() ;
        List<TestModule> modules = testPlan.getAllTesModules() ;
        for(TestModule md : modules){
            assertTrue(md.getModuleName().equals("sample"));
        }
    }
    @Test
    public void testingCreateTestPlanAndModule() {
        MainController mc = new MainController();
        TestPlan testPlan = mc.createTestPlanAndModule();
        List<TestModule> modules = testPlan.getAllTesModules() ;
        for(TestModule md : modules){
            List<TestSuite> tsc = md.getAllTestSuits();
            for(TestSuite ts : tsc){
                List<TestCase> tcs = ts.getAllTestCases();
                for(TestCase tc: tcs){
                  assertTrue(tc.getTestCaseNumber().equals("001"));
                }
            }
        }
    }


}


