App42_CustomCode_Sample
=======================

Writing Your Custom Code
========================
1. Download and Export the Sample Project in your  Eclipse IDE.
2. Open Java File MyCustomCode.java inside com.shephertz.app42.paas.customcode.sample folder and modify the execute method of it.
   You can write your business logic inside  this method. If You want to Rename/change the package and class name you are free to do so.
3. Use App42 Logging Service to do the logging inside your custom code. These logs would be available to you inside AppHQ console.

Deploying Your Custom Code
==========================
1. You can either deploy your custom code through API or can use ant script available in root folder of this sample project.
2. To deploy your custom code through Ant script, run < ant deploy > from command line inside your root folder. Before running this command modify           build.properties to enter your APIKey and SecretKey for the same.
3. Once you run <ant deploy> command, your will ask to enter the name for custom code to be deployed on App42 Cloud. Default name is MyCustomCode. If you get    BUILD SUCCESSFUL message after entering the name, your App is deployed on App42 Cloud. Otherwise you will get an appropriate exception message.
4. To deploy your custom code through API, you have to call deployJarFile method on CustomCodeService API by passing name and jar file location. 

Run your Custom Code
====================
1. You can run your deployed custom code through API available in various SDKs using method runJavaCode in CustomCodeService. This method accepte JSON Object and name of deployed service and returns JSON response. JSON response will contain the value that your returned from your custom code. 
2. For your ease, you can also run your deployed custom code through above steps using ant command line available in this sample using command <ant run>. You will be asked to enter the name of deployed custom code, aftre entering the name you will be able to see the response obtained from your deployed custom code.
3. If you are deploying through ant command line, you have to give the value of input JSON inside build.properties in <jsonRequestBody> property value.

