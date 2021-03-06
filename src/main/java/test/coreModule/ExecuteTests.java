package test.coreModule;

import org.openqa.selenium.WebDriver;
import test.Log.CreateLog;
import test.Log.LogMessage;
import test.beforeTest.LeaseCreateAndSearch;
import test.beforeTest.PropertyCreateAndSearch;
import test.beforeTest.SpaceCreateAndSearch;
import test.beforeTest.TestData;
import test.keywordScripts.UIBase;
import test.keywordScripts.UtilKeywordScript;
import test.utility.PropertyConfig;
import test.utility.ReadExcel;

import javax.swing.text.StyledEditorKit;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ExecuteTests {

    private WebDriver webDriver;
    public ExecuteTests(WebDriver driver) {
        webDriver = driver ;
    }
    public void readAndExecute(String fileName,String sheetName) {
        ClassLoader classLoader = getClass().getClassLoader();
        long start = System.currentTimeMillis();
        ReadExcel readExcel = new ReadExcel(classLoader.getResource("modules/" + fileName + ".xlsx").getPath());
        List<Map> records = readExcel.read(sheetName);
        CreateLog createLog = new CreateLog("sampleReport");
        createLog.createLogger("sample test");
        for(Map map : records) {
            ArrayList<Object> objects = new ArrayList<Object>();
           // objects.add(webDriver);
            String actionName = (String) map.get(PropertyConfig.ACTION);
            String objectLocators = (String) map.get(PropertyConfig.OBJECT_LOCATORS);
            String testData = (String) map.get(PropertyConfig.TEST_DATA);
            String executionFlag = (String) map.get(PropertyConfig.EXECUTION_FLAG);
            String critical = (String) map.get(PropertyConfig.CRITICAL);
            int numberOfParams = 0;

            if ( null == executionFlag || !executionFlag.toLowerCase().equals("yes"))
                continue;
            if(null != objectLocators && ! objectLocators.isEmpty()) {
                objects.add(objectLocators);
                numberOfParams++;
            }
            if(null != testData && ! testData.isEmpty()){
                objects.add(testData);
                numberOfParams++;
            }
            LogMessage logMessage =  invokeMethod(actionName.split("\\.")[0],actionName.split("\\.")[1],numberOfParams,objects.toArray());
            createLog.writeLog("sample test",logMessage.getLogMessage(),logMessage.isPassed());
        }

    }
    public List<LogMessage> executeTest(TestCase testCase) {
        List<LogMessage> logMessages = new ArrayList<LogMessage>();
        try {
            UIBase uiBase = new UIBase(webDriver);
            ClassLoader classLoader = getClass().getClassLoader();
            long start = System.currentTimeMillis();
            List<TestStep> testSteps = testCase.getAllTestSteps();
            testCase.setPassed(true);
            TestData testData = TestData.getInstance();
            testData.setDriver(webDriver);
            for (TestStep testStep : testSteps) {
                //every step either create data or remove data or prequisite or normal step, if execultion flag is not set, it will not be executed
                Boolean executionFlag = testStep.isExecutionFlagOn();
                if (!executionFlag) {
                    testStep.setPassed(true);
                    logMessages.add(new LogMessage(true, testStep.getTestStepDescription() + " --" + testStep.getFieldName() + "(Skipped)"));
                    continue;
                }
                if (isItPrequisite(testStep.getAction())) {
                    logMessages.add(new LogMessage(true, "Prerequisite started : " + testStep.getTestStepDescription()));
                    List<LogMessage> preqLogMessages = runPrequisite(testCase, testStep);
                    logMessages.addAll(preqLogMessages);
                    testCase.setPassed(preqLogMessages.stream().allMatch(logMessage -> logMessage.isPassed()));
                    if (!testCase.isPassed()) {
                        logMessages.add(new LogMessage(false, "Prerequisite not fullfiled : "));
                        return logMessages;
                    } else {
                        logMessages.add(new LogMessage(true, "Prerequisite fullfiled"));
                    }
                } else if (isItCreate(testStep.getAction().toUpperCase())) {
                    logMessages.addAll(testData.runPrequisites(testCase.getTestCaseNumber(), testStep.getTestData()));
                } else if (isItDelete(testStep.getAction().toUpperCase())) {
                    if(null==testStep.getTestData() || testStep.getTestData().isEmpty() || testStep.getTestData().toLowerCase().contains("property"))
                    {
                        PropertyCreateAndSearch propertyCreateAndSearch=new PropertyCreateAndSearch(webDriver);
                        List<Map> propertyDatas=testData.getData("Property",testCase.getTestCaseNumber());
                        for(Map propertyData:propertyDatas){
                            logMessages.add(propertyCreateAndSearch.deleteProperty((String)propertyData.get("propertyName"), (String) propertyData.get("propertyCode")));
                        }
                        continue;
                    }
                    if(null==testStep.getTestData() || testStep.getTestData().isEmpty() || testStep.getTestData().toLowerCase().contains("lease"))
                    {
                        LeaseCreateAndSearch leaseCreateAndSearch=new LeaseCreateAndSearch(webDriver);
                        List<Map> leaseDatas=testData.getData("Lease",testCase.getTestCaseNumber());
                        logMessages.addAll(leaseCreateAndSearch.deleteLeases((String)leaseDatas.get(0).get("propertyName"),(String)leaseDatas.get(0).get("propertyCode"),leaseDatas));
                    }
                    if(null==testStep.getTestData() || testStep.getTestData().isEmpty() || testStep.getTestData().toLowerCase().contains("space")){
                        SpaceCreateAndSearch spaceCreateAndSearch=new SpaceCreateAndSearch(webDriver);
                        List<Map> spaceDatas=testData.getData("Space",testCase.getTestCaseNumber());
                        logMessages.addAll(spaceCreateAndSearch.deleteSpace((String)spaceDatas.get(0).get("propertyName"),(String)spaceDatas.get(0).get("propertyCode"),spaceDatas));
                    }
                } else {
                    logMessages.add(runSingleStep(testStep, testCase));
                    if (!testStep.isPassed() && testStep.isCritical()) {
                        testCase.setPassed(false);
                        return logMessages;
                    }
                }
            }
            testCase.setPassed(logMessages.stream().allMatch(logMessage -> logMessage.isPassed()));
            return logMessages;
        }catch (Exception e){
            return logMessages;
        }
    }


    public LogMessage runSingleStep(TestStep testStep,TestCase testCase) {
        try {

            ArrayList<Object> objects = new ArrayList<Object>();
            // objects.add(webDriver);
            String actionName = testStep.getAction();
            String objectLocators = testStep.getObjectLocator();
            testStep.setTestData(updateTestData(testCase.getTestCaseNumber(),testStep.getTestData(),testCase.getPreqData()));
            String testData = testStep.getTestData();
            Boolean executionFlag = testStep.isExecutionFlagOn();
            Boolean pageRefresh = testStep.isRefreshPageOn();
            Boolean critical = testStep.isCritical();
            String[] logMeessageFromXL=null;
            int numberOfParams = 0;

            if (! executionFlag) {
                testStep.setPassed(true);
                return new LogMessage(true,testStep.getTestStepDescription() + " --" + testStep.getFieldName() + "(Skipped)");
                //logMessage.setSkippedTrue();
            }
            if(null != objectLocators && ! objectLocators.isEmpty()) {
                objects.add(objectLocators);
                numberOfParams++;
            }
            if(null != testData && ! testData.isEmpty()){
                objects.add(testData);
                numberOfParams++;
            }
            UtilKeywordScript.delay(PropertyConfig.SHORT_WAIT_TIME_SECONDS);
            LogMessage logMessage =  invokeMethod(actionName.split("\\.")[0],actionName.split("\\.")[1],numberOfParams,objects.toArray());
            if(null!=testStep.getLogMessage() && !testStep.getLogMessage().equals("")) {
                logMeessageFromXL = testStep.getLogMessage().split("\\?");
                if (logMessage.isPassed() && logMeessageFromXL.length == 1) {
                    logMessage.setLogMessage(logMeessageFromXL[0]);
                } else if (logMeessageFromXL[0].equals("")) {
                    if(!logMessage.isPassed())
                        logMessage.setLogMessage(logMeessageFromXL[1]);
                    else
                        logMessage.setLogMessage(testStep.getTestStepDescription() + " --" + testStep.getFieldName() + "--" + logMessage.getLogMessage());
                } else if (logMeessageFromXL.length == 2) {
                    if (logMessage.isPassed())
                        logMessage.setLogMessage(logMeessageFromXL[0]);
                    else
                        logMessage.setLogMessage(logMeessageFromXL[1]);
                }else
                    logMessage.setLogMessage(testStep.getTestStepDescription() + " --" + testStep.getFieldName() + "--" + logMessage.getLogMessage());
            }else
                logMessage.setLogMessage(testStep.getTestStepDescription() + " --" + testStep.getFieldName() + "--" + logMessage.getLogMessage());

            testStep.setPassed(logMessage.isPassed());

            if (pageRefresh){
                new UIBase(webDriver).WaitingForPageLoad();
            }
            return  logMessage ;
        } catch (Exception ex) {
            ex.printStackTrace();
            testStep.setPassed(false);
            return new LogMessage(false, "exception occured running one test step " + ex.getMessage()) ;
        }

    }

    public LogMessage invokeMethod(String className,String methodName,int numberOfParams,Object[] object) {
        try {

            Class<?> callingClass = Class.forName("test.keywordScripts." + className);
            Method callingMethod ;
            if(numberOfParams == 0)
                callingMethod = callingClass.getDeclaredMethod(methodName);
            else if(numberOfParams == 1)
                callingMethod = callingClass.getDeclaredMethod(methodName,String.class );
            else if(numberOfParams == 2)
                callingMethod = callingClass.getDeclaredMethod(methodName,String.class,String.class);
            else
                return new LogMessage(false, "number of parameter exceeds");
            Constructor<?> constructor = callingClass.getConstructor(WebDriver.class);
            LogMessage logMessage = (LogMessage) callingMethod.invoke(constructor.newInstance(webDriver),object);
            return logMessage;
        } catch(Exception ex) {
               ex.printStackTrace();
               return  new LogMessage(false,"exception occured");
        }
    }

    public List<LogMessage> runPrequisite(TestCase testCase,TestStep testStep) {
        try {
           Optional<TestCase> prerequiste =  new PreRequiste().getPrequisiteTestCase(testStep.getFieldName().split(":")[0],testStep.getFieldName().split(":")[1]) ;
           if(!prerequiste.isPresent()) {
               return new ArrayList<LogMessage>()
               {{
                   add(new LogMessage(false,"no valid prequisite found for " + testStep.getFieldName()));
               }};
           }
           new UtilKeywordScript(webDriver).redirectHomePage() ;
           TestCase preqTCase = prerequiste.get() ;
           preqTCase.setTestCaseNumber(testCase.getTestCaseNumber());
           preqTCase.setPreqData(UtilKeywordScript.jsonStringToMap(testStep.getTestData()));
           List<LogMessage> preqLogMessages =  executeTest(preqTCase) ;
            new UtilKeywordScript(webDriver).redirectHomePage() ;
            return preqLogMessages ;
        } catch (Exception ex) {
            return new ArrayList<LogMessage>()
            {{
                add(new LogMessage(false,"exception occured handling prerequiste " + ex.getMessage()));
            }};
        }
    }


    private Boolean isItPrequisite(String action) {
        if (action.equals(PropertyConfig.PREREQ_COMMAND)) return true;
        return false;
    }
    private Boolean isItCreate(String action){
        if (action.equals(PropertyConfig.CREATE_COMMAND)) return true;
        return false;
    }
    private Boolean isItDelete(String action){
        if (action.equals(PropertyConfig.DELETE_COMMAND)) return true;
        return false;
    }
    public String updateTestData(String testCaseId,String testData, Optional<Map> utilData){
        try{
            if(Optional.ofNullable(testData).orElse("").equals(""))
                return testData ;
            String finalTestData="";
            String[] splitTestDatas=testData.split(",");
            TestData prerequisiteTestData = TestData.getInstance();
            prerequisiteTestData.setDriver(webDriver);
            if(splitTestDatas.length>0)
            {
                for(String splitTestData:splitTestDatas){
                    if(splitTestData != "" && splitTestData.charAt(0)=='$'){
                        splitTestData=splitTestData.substring(1);
                        String storeValue = TestPlan.getInstance().getStoreData(splitTestData) ;
                        if(null != storeValue && !storeValue.isEmpty())
                            finalTestData = finalTestData + storeValue ;
                        else {
                            String[] testDataDetails = splitTestData.split("_");
                            List<Map> datas = prerequisiteTestData.getData(testDataDetails[0].toUpperCase(), testCaseId);
                            String indexValue = utilData.isPresent() ? Optional.ofNullable((String) utilData.get().get(testDataDetails[0])).orElse("") : "";
                            if (indexValue != "")
                                testDataDetails[2] = indexValue;
                            if (testDataDetails.length == 2) {
                                Map data = datas.get(0);
                                finalTestData = finalTestData + (String) data.get(testDataDetails[1]);

                            } else if (testDataDetails.length == 3) {
                                Map data = datas.get(Integer.parseInt(testDataDetails[2]));
                                finalTestData = finalTestData + (String) data.get(testDataDetails[1]);
                            }
                        }
                    }
                    else
                        finalTestData=finalTestData+splitTestData;
                    finalTestData=finalTestData+",";

                }
                finalTestData=finalTestData.replaceAll(",$", "");
            }
            return finalTestData;
        }
        catch (Exception ex){
            ex.printStackTrace();
            return testData;
        }
    }
}
