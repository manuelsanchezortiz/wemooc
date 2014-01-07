package com.tls.liferaylms.test.unit;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;

import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.util.List;

import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

import com.tls.liferaylms.test.SeleniumTestCase;
import com.tls.liferaylms.test.util.CheckPage;
import com.tls.liferaylms.test.util.Context;
import com.tls.liferaylms.test.util.CourseActivityMenu;
import com.tls.liferaylms.test.util.GetPage;
import com.tls.liferaylms.test.util.Login;
import com.tls.liferaylms.test.util.Sleep;
import com.tls.liferaylms.test.util.TestProperties;

public class Bc_CreateActivity extends SeleniumTestCase {

	@Test
	public void createActivity() throws Exception {
		Login login = new Login(driver, Context.getTeacherUser(), Context.getTeacherPass(), Context.getBaseUrl());
		
		if(login.isLogin())
			login.logout();
		
		boolean teacherLogin = login.login();
		assertTrue("Error login teacher",teacherLogin);
		
		if(teacherLogin){
			try{
				GetPage.getPage(driver, Context.getCoursePage(), "/reto");
				
				changeEditMode();
				
				Sleep.sleep(2000);
	
				WebElement activityPortlet = getElement(By.id("p_p_id_moduleportlet_WAR_liferaylmsportlet_"));
				assertNotNull("Not Activity portlet found", activityPortlet);
				
				if (driver instanceof JavascriptExecutor) {
				    ((JavascriptExecutor)driver).executeScript("javascript:_moduleportlet_WAR_liferaylmsportlet_openPopup();");
				}

				Sleep.sleep(1000);
	
				driver.switchTo().frame(0);
				
				WebElement title = getElement(By.id("_moduleportlet_WAR_liferaylmsportlet_title_es_ES"));
				assertNotNull("Not Activity title found", title);
				title.sendKeys("Module "+Context.getCourseId());
	
				WebElement form = getElement(By.id("_moduleportlet_WAR_liferaylmsportlet_addmodule"));
				assertNotNull("Not form activity found", form);
				form.submit();

				Sleep.sleep(2000);

				driver.switchTo().activeElement();

				
				WebElement closethick = getElement(By.className("aui-button-input-cancel"));
				assertNotNull("Not close popoup", closethick);
				closethick.click();

				GetPage.getPage(driver, Context.getCoursePage(), "/reto");
				
				changeEditMode();
				
				Sleep.sleep(2000);
				
				//Add activities
				WebElement newactivity = getElement(By.className("newactivity"));
				assertNotNull("" +
						"", newactivity);
				
				WebElement aNew = getElement(newactivity,By.tagName("a"));
				assertNotNull("Not aNewnewactivity button", aNew);
				aNew.click();

				Sleep.sleep(2000);
				
				driver.switchTo().frame(0);

				WebElement activityList = getElement(By.className("activity-list"));
				assertNotNull("Not Activity list find", activityList);
				
				List<WebElement> lis = getElements(activityList, By.tagName("li"));
				
				assertTrue("Poor activities... ",lis.size()>6);
				
				for(int i=0;i<lis.size();i++){
					GetPage.getPage(driver, Context.getCoursePage(), "/reto");
					
					changeEditMode();

					Sleep.sleep(2000);
					
					newactivity = getElement(By.className("newactivity"));
					assertNotNull("Not newactivity button", newactivity);

					aNew = getElement(newactivity,By.tagName("a"));
					assertNotNull("Not aNewnewactivity button", aNew);
					aNew.click();

					Sleep.sleep(2000);
					
					driver.switchTo().frame(0);

					activityList = getElement(By.className("activity-list"));
					assertNotNull("Not Activity list find", activityList);
					
					lis = getElements(activityList, By.tagName("li"));
					
					assertTrue("Poor activities... ",lis.size()>6);

					WebElement a = getElement(lis.get(i),By.tagName("a"));
					a.click();

					Sleep.sleep(2000);
					
					WebElement titleAct = getElement(By.id("_lmsactivitieslist_WAR_liferaylmsportlet_title_es_ES"));
					assertNotNull("Title activity not find", titleAct);
					String prop = null;
					switch(i){
						case 0:
							prop = "act.test";
							break;
						case 1:
							prop = "act.ext";
							break;
						case 2:
							prop = "act.p2p";
							break;
						case 3:
							prop = "act.enc";
							break;
						case 4:
							prop = "act.pres";
							break;
						case 5:
							prop = "act.desa";
							break;
						case 6:
							prop = "act.media";
							break;
						case 7:
							prop = "act.eval";
							break;
						case 8:
							prop = "act.scorm";
							break;
					}
					titleAct.sendKeys(TestProperties.get(prop)+" "+Context.getCourseId());
					sendCkEditorJS(driver,prop);
					
					if(i==8){
						assertTrue("Error creating SCORM to SCORM activity",createScorm());
					}else if(i==2){
						WebElement numVal = getElement(By.id("_lmsactivitieslist_WAR_liferaylmsportlet_numValidaciones"));
						numVal.clear();
						numVal.sendKeys("1");
						
						Sleep.sleep(200000000);
					}
					
					form = getElement(By.id("_lmsactivitieslist_WAR_liferaylmsportlet_fm"));
					assertNotNull("Not form activity found", form);
					form.submit();

					Sleep.sleep(2000);

					GetPage.getPage(driver, Context.getCoursePage(), "/reto");
					
					//Chequeamos el estado de la actividad
					String param = TestProperties.get(prop);					
					WebElement liActive = CourseActivityMenu.findElementActivityMenu(driver,param);
					assertNotNull("Not found activity created", liActive);
					
					List<WebElement> asActive = getElements(liActive, By.tagName("a"));
					assertEquals("Not Edit portlet found", 1,asActive.size());
					
					//Put activity in context
					String idenfier = asActive.get(0).getText();					
					
					//asActive.get(0).click();

					Context.getActivities().put(idenfier, driver.getCurrentUrl());
					
					
					//assertEquals("Errors in activity"+param, 0, CheckPage.check(driver));
					
					
					//Editamos la actividad
					changeEditMode();
					
					liActive =  CourseActivityMenu.findElementActivityMenu(driver,param);
					assertNotNull("Not found activity created", liActive);

					asActive = getElements(liActive, By.tagName("a"));
					assertEquals("Not Edit portlet found", 5,asActive.size());
					if(getLog().isInfoEnabled())getLog().info("Enlaces::"+asActive.size());

					asActive.get(1).click();
					Sleep.sleep(2000);

					switch(i){
						case 0:
							assertTrue("Error creating test",createTest());
							break;
						case 1:
							assertTrue("Error creating specific data to Ext activity",createExt());
							break;
						case 3:
							assertTrue("Error creating dato to Poll activity",createPoll());
							break;
					}

					GetPage.getPage(driver, Context.getCoursePage(), "/reto");
						
				}
				
			}catch(Exception e){
				e.printStackTrace();
			}
		}
	}
	
	private void changeEditMode(){
		WebElement editPortlet = getElement(By.id("p_p_id_changeEditingMode_WAR_liferaylmsportlet_"));
		assertNotNull("Not Edit portlet found", editPortlet);
		
		List<WebElement> inputs = getElements(editPortlet,By.tagName("input"));
		assertEquals("Not Edit portlet found", 1,inputs.size());
		inputs.get(0).click();
	}
	
	private void sendCkEditorJS(WebDriver driver,String prop){
		if (driver instanceof JavascriptExecutor) {
			StringBuilder sb = new StringBuilder("javascript:CKEDITOR.instances['_lmsactivitieslist_WAR_liferaylmsportlet_description'].setData('<p>");
			sb.append(TestProperties.get(prop));
			sb.append(" ");
			sb.append(Context.getCourseId());
			sb.append("</p>');");
		    ((JavascriptExecutor)driver).executeScript(sb.toString());
		}
	}
	
	private void sendCkEditorJSId(WebDriver driver,String msg,String id){
		if (driver instanceof JavascriptExecutor) {
			StringBuilder sb = new StringBuilder("javascript:CKEDITOR.instances['");
			sb.append(id);
			sb.append("'].setData('<p>");
			sb.append(msg);
			sb.append(" ");
			sb.append(Context.getCourseId());
			sb.append("</p>');");
		    ((JavascriptExecutor)driver).executeScript(sb.toString());
		}
	}
	
	private void executeJS(String script){
		if (driver instanceof JavascriptExecutor) {
			((JavascriptExecutor)driver).executeScript(script);
		}
	}
	
	private boolean createTest(){
		driver.switchTo().frame(0);

		//Ordenable
		WebElement botonera = getElement(By.className("acticons"));
		assertNotNull("Not found botonera", botonera);
		
		List<WebElement> aHrefs = getElements(botonera,By.tagName("a"));
		assertTrue("Not menu actions finds",aHrefs.size()>0);
		aHrefs.get(0).click();
		
		driver.switchTo().defaultContent();
		driver.switchTo().frame(0);
		
		WebElement bt_new = getElement(By.className("bt_new"));
		assertNotNull("Not found button bt_new", bt_new);
		WebElement aNew = getElement(bt_new,By.tagName("a"));
		assertNotNull("Not found button aNew", aNew);
		aNew.click();
		
		Sleep.sleep(1000);
		
		WebElement typeId = getElement(By.id("_execactivity_WAR_liferaylmsportlet_typeId"));
		assertNotNull("Not found select typeId", typeId);
		
		Select select = new Select(typeId);
		select.selectByValue("5");
		
		sendCkEditorJSId(driver,TestProperties.get("act.test.5"),"_execactivity_WAR_liferaylmsportlet_text");
		
		WebElement submit = getElement(By.className("aui-button-input-submit"));
		assertNotNull("Not submit found", submit);
		//Doubleclick
		try{
			submit.click();
			submit.click();
		}catch(Exception e){}
		
		Sleep.sleep(1000);
		
		executeJS("javascript:_execactivity_WAR_liferaylmsportlet_divVisibility('addNewQuestion', this);");
		
		for(int i=0;i<10;i++){
			sendCkEditorJSId(driver,TestProperties.get("act.test.5.text")+" "+i,"_execactivity_WAR_liferaylmsportlet_answer");
			WebElement form = getElement(By.id("_execactivity_WAR_liferaylmsportlet_afm"));
			assertNotNull("Not form submit response", form);
			form.submit();
			
			Sleep.sleep(1000);
		}
		
		WebElement breturn = getElement(By.id("_execactivity_WAR_liferaylmsportlet_TabsBack"));
		assertNotNull("Not breturn found", breturn);
		breturn.click();
		try{
			breturn.click();
		}catch(Exception e){}

		Sleep.sleep(1000);
		//Arrastrar
		
		//Button edit
		bt_new = getElement(By.className("bt_new"));
		assertNotNull("Not found button bt_new", bt_new);
		aNew = getElement(bt_new,By.tagName("a"));
		assertNotNull("Not found button aNew", aNew);
		aNew.click();

		Sleep.sleep(1000);
		
		typeId = getElement(By.id("_execactivity_WAR_liferaylmsportlet_typeId"));
		assertNotNull("Not found select typeId", typeId);
		
		select = new Select(typeId);
		select.selectByValue("4");

		sendCkEditorJSId(driver,TestProperties.get("act.test.4"),"_execactivity_WAR_liferaylmsportlet_text");
		
		submit = getElement(By.className("aui-button-input-submit"));
		assertNotNull("Not submit found", submit);
		//Doubleclick
		try{
			submit.click();
			submit.click();
		}catch(Exception e){}

		Sleep.sleep(1000);

		executeJS("javascript:_execactivity_WAR_liferaylmsportlet_divVisibility('addNewQuestion', this);");
		
		for(int i=0;i<10;i++){
			sendCkEditorJSId(driver,TestProperties.get("act.test.4.text")+" "+i,"_execactivity_WAR_liferaylmsportlet_answer");
			WebElement form = getElement(By.id("_execactivity_WAR_liferaylmsportlet_afm"));
			assertNotNull("Not form submit response", form);
			if(i==0){
				WebElement check = getElement(By.id("_execactivity_WAR_liferaylmsportlet_correctCheckbox"));
				check.click();
			}
			
			form.submit();
			
			Sleep.sleep(2000);
		}
		
		breturn = getElement(By.id("_execactivity_WAR_liferaylmsportlet_TabsBack"));
		assertNotNull("Not breturn found", breturn);
		breturn.click();

		Sleep.sleep(1000);
		//Fill space
		bt_new = getElement(By.className("bt_new"));
		assertNotNull("Not found button bt_new", bt_new);
		aNew = getElement(bt_new,By.tagName("a"));
		assertNotNull("Not found button aNew", aNew);
		aNew.click();

		Sleep.sleep(1000);
		
		typeId = getElement(By.id("_execactivity_WAR_liferaylmsportlet_typeId"));
		assertNotNull("Not found select typeId", typeId);
		
		select = new Select(typeId);
		select.selectByValue("3");

		sendCkEditorJSId(driver,TestProperties.get("act.test.3"),"_execactivity_WAR_liferaylmsportlet_text");

		submit = getElement(By.className("aui-button-input-submit"));
		assertNotNull("Not submit found", submit);
		//Doubleclick
		submit.click();
		try{
			submit.click();
		}catch(Exception e){}

		Sleep.sleep(1000);

		WebElement newitem = getElement(By.className("newitem2"));
		assertNotNull("Not found submit for answer", newitem);
		WebElement a = getElement(newitem,By.tagName("a"));
		assertNotNull("Not found a for answer", a);
		a.click();
		
		sendCkEditorJSId(driver,TestProperties.get("act.test.3.text"),"_execactivity_WAR_liferaylmsportlet_answer");

		Sleep.sleep(3000);
		
		WebElement mainAdd = getElement(By.id("addNewQuestion"));
		
		submit = getElement(mainAdd, By.className("aui-button-input-submit"));
		assertNotNull("Not submit found", submit);
		//Doubleclick
		
		try{
			submit.click();
			submit.click();
		}catch(Exception e){}

		Sleep.sleep(1000);
		
		breturn = getElement(By.id("_execactivity_WAR_liferaylmsportlet_TabsBack"));
		assertNotNull("Not breturn found", breturn);
		breturn.click();

		Sleep.sleep(1000);
		//Free text
		bt_new = getElement(By.className("bt_new"));
		assertNotNull("Not found button bt_new", bt_new);
		aNew = getElement(bt_new,By.tagName("a"));
		assertNotNull("Not found button aNew", aNew);
		aNew.click();

		Sleep.sleep(1000);
		
		typeId = getElement(By.id("_execactivity_WAR_liferaylmsportlet_typeId"));
		assertNotNull("Not found select typeId", typeId);
		
		select = new Select(typeId);
		select.selectByValue("2");

		sendCkEditorJSId(driver,TestProperties.get("act.test.2"),"_execactivity_WAR_liferaylmsportlet_text");

		submit = getElement(By.className("aui-button-input-submit"));
		assertNotNull("Not submit found", submit);
		//Doubleclick
		try{
			submit.click();
			submit.click();
		}catch(Exception e){}

		Sleep.sleep(2000);

		//This line not work 
		//executeJS("javascript:_execactivity_WAR_liferaylmsportlet_divVisibility('addNewQuestion', this);");

		//WebElement form = getElement(By.id("_execactivity_WAR_liferaylmsportlet_afm"));
		//assertNotNull("Not form submit response", form);
		//form.submit();
		
		WebElement include = getElement(By.id("includeSolution"));
		assertNotNull("Not found inclide for free text", include);
		include.click();

		Sleep.sleep(1000);
		
		WebElement tac =getElement(By.className("container-textarea"));
		assertNotNull("Not found textarea container", tac);
		
		WebElement ta =getElement(tac,By.tagName("textarea"));
		assertNotNull("Not found textarea container", ta);
		
		ta.clear();
		ta.sendKeys(TestProperties.get("act.test.3.answer"));
		

		WebElement solution = getElement(By.id("solution"));
		
		submit = getElement(solution,By.className("aui-button-input-submit"));
		assertNotNull("Not submit found", submit);
		//Doubleclick
		try{
			submit.click();
			submit.click();
		}catch(Exception e){}
		
		Sleep.sleep(1000);

		breturn = getElement(By.id("_execactivity_WAR_liferaylmsportlet_TabsBack"));
		assertNotNull("Not breturn found", breturn);
		breturn.click();

		Sleep.sleep(1000);
		//Multiple answer
		bt_new = getElement(By.className("bt_new"));
		assertNotNull("Not found button bt_new", bt_new);
		aNew = getElement(bt_new,By.tagName("a"));
		assertNotNull("Not found button aNew", aNew);
		aNew.click();

		Sleep.sleep(1000);
		
		typeId = getElement(By.id("_execactivity_WAR_liferaylmsportlet_typeId"));
		assertNotNull("Not found select typeId", typeId);

		select = new Select(typeId);
		select.selectByValue("1");

		sendCkEditorJSId(driver,TestProperties.get("act.test.1"),"_execactivity_WAR_liferaylmsportlet_text");

		submit = getElement(By.className("aui-button-input-submit"));
		assertNotNull("Not submit found", submit);
		//Doubleclick
		try{
			submit.click();
			submit.click();
		}catch(Exception e){}

		Sleep.sleep(1000);

		executeJS("javascript:_execactivity_WAR_liferaylmsportlet_divVisibility('addNewQuestion', this);");

		for(int i=0;i<10;i++){
			sendCkEditorJSId(driver,TestProperties.get("act.test.1.text")+" "+i,"_execactivity_WAR_liferaylmsportlet_answer");
			Sleep.sleep(1000);
			if(i==0){
				WebElement check = getElement(By.id("_execactivity_WAR_liferaylmsportlet_correctCheckbox"));
				WebDriverWait wait = new WebDriverWait(driver, 300);
				wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("_execactivity_WAR_liferaylmsportlet_correctCheckbox")));
				check.click();
			}
			WebElement form = getElement(By.id("_execactivity_WAR_liferaylmsportlet_afm"));
			assertNotNull("Not form submit response", form);
			
			form.submit();
			
			Sleep.sleep(2000);
		}

		breturn = getElement(By.id("_execactivity_WAR_liferaylmsportlet_TabsBack"));
		assertNotNull("Not breturn found", breturn);
		breturn.click();

		Sleep.sleep(1000);
		//Options
		bt_new = getElement(By.className("bt_new"));
		assertNotNull("Not found button bt_new", bt_new);
		aNew = getElement(bt_new,By.tagName("a"));
		assertNotNull("Not found button aNew", aNew);
		aNew.click();

		Sleep.sleep(1000);
		
		typeId = getElement(By.id("_execactivity_WAR_liferaylmsportlet_typeId"));
		assertNotNull("Not found select typeId", typeId);

		select = new Select(typeId);
		select.selectByValue("0");

		sendCkEditorJSId(driver,TestProperties.get("act.test.0"),"_execactivity_WAR_liferaylmsportlet_text");

		submit = getElement(By.className("aui-button-input-submit"));
		assertNotNull("Not submit found", submit);
		//Doubleclick
		try{
			submit.click();
			submit.click();
		}catch(Exception e){}

		Sleep.sleep(1000);

		executeJS("javascript:_execactivity_WAR_liferaylmsportlet_divVisibility('addNewQuestion', this);");

		for(int i=0;i<10;i++){
			sendCkEditorJSId(driver,TestProperties.get("act.test.0.text")+" "+i,"_execactivity_WAR_liferaylmsportlet_answer");
			WebElement form = getElement(By.id("_execactivity_WAR_liferaylmsportlet_afm"));
			assertNotNull("Not form submit response", form);
			
			if(i==0){
				WebElement check = getElement(By.id("_execactivity_WAR_liferaylmsportlet_correctCheckbox"));
				check.click();
			}
			
			form.submit();
			
			Sleep.sleep(2000);
		}

		breturn = getElement(By.id("_execactivity_WAR_liferaylmsportlet_TabsBack"));
		assertNotNull("Not breturn found", breturn);
		breturn.click();
		
		return true;
	}
	
	private boolean createExt(){
		driver.switchTo().frame(0);

		openColapsables();
		
		WebElement youtube = getElement(By.id("_lmsactivitieslist_WAR_liferaylmsportlet_youtubecode"));
		youtube.sendKeys(TestProperties.get("act.ext.youtube"));
		
		WebElement form = getElement(By.id("_lmsactivitieslist_WAR_liferaylmsportlet_fm"));
		form.submit();
		
		return true;
	}
	
	private boolean createPoll(){
		driver.switchTo().frame(0);
		
		WebElement botonera = getElement(By.className("acticons"));
		assertNotNull("Not found botonera", botonera);
		
		List<WebElement> aHrefs = getElements(botonera,By.tagName("a"));
		assertTrue("Not menu actions finds",aHrefs.size()>0);
		aHrefs.get(0).click();
		
		driver.switchTo().defaultContent();
		driver.switchTo().frame(0);
		
		//3th a
		List<WebElement> as = getElements(By.tagName("a"));
		assertNotNull("Not found 'as' in Poll edit", as);
		assertTrue("Numbers of 'as' in Poll edit not matched",as.size()>=4);
		
		as.get(2).click();

		driver.switchTo().defaultContent();
		driver.switchTo().frame(0);

		//File f = new File("resources"+File.separator+"encuesta.csv");
		File f = new File("docroot"+File.separator+"WEB-INF"+File.separator+"classes"+File.separator+"resources"+File.separator+"encuesta.csv");
		WebElement upload = getElement(By.id("_surveyactivity_WAR_liferaylmsportlet_fileName"));
		upload.sendKeys(f.getAbsolutePath());
		
		WebElement submit = getElement(By.className("aui-button-input-submit"));
		assertNotNull("Not submit found", submit);
		
		//Doubleclick
		try{
			submit.click();
		}catch(Exception e){}

		Sleep.sleep(2000);
		
		return true;
	}
	
	private boolean createScorm(){
		WebElement scromSearch= getElement(By.id("_lmsactivitieslist_WAR_liferaylmsportlet_searchEntry"));
		assertNotNull("Not scorm button search found", scromSearch);
		scromSearch.click();

		Sleep.sleep(1000);
		
		driver.switchTo().defaultContent();
		driver.switchTo().frame(0);
		
		driver.switchTo().frame(driver.findElement(By.id("_lmsactivitieslist_WAR_liferaylmsportlet_finder")));

		System.out.println("--------------------------------------------------------------------------------");
		System.out.println(driver.getPageSource());
		System.out.println("--------------------------------------------------------------------------------");

		WebElement submit = getElement(By.className("aui-button-input-submit"));
		assertNotNull("Not submit found", submit);
		submit.click();
		
		WebElement scorm = getElement(By.id("_lmsactivitieslist_WAR_liferaylmsportlet_assetEntriesSearchContainer_col-2_row-1"));
		if(scorm!=null){
			WebElement a = getElement(scorm,By.tagName("a"));
			a.click();
		}else{
			if(getLog().isInfoEnabled())getLog().info("Error no SCORM found");
		}

		driver.switchTo().defaultContent();
		driver.switchTo().frame(0);
		
		return true;
	}
	
	private static void setClipboardData(String string) {
	      StringSelection stringSelection = new StringSelection(string);
	      java.awt.Toolkit.getDefaultToolkit().getSystemClipboard().setContents(stringSelection, null);      
	}
	
	private void openColapsables(){
		List<WebElement> pcontainer = getElements(By.className("lfr-panel-titlebar"));
		assertNotNull("Not found pcontainer", pcontainer);
		
		for(WebElement we : pcontainer){
			if(getLog().isInfoEnabled())getLog().info("Click::"+we.getText());
			try{
				we.click();
			}catch(Exception e){}
			List<WebElement> spans = getElements(we,By.tagName("span"));
			if(spans!=null){
				for(WebElement span : spans){
					try{
						span.click();
					}catch(Exception e){}
				}
			}
			List<WebElement> divs = getElements(we,By.tagName("div"));
			if(spans!=null){
				for(WebElement div : divs){
					try{
						div.click();
					}catch(Exception e){}
				}
			}
			Sleep.sleep(1000);
		}
	}
}
